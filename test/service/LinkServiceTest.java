package service;

import config.AppConfig;
import domain.exception.AccessDeniedException;
import domain.exception.LimitExceededException;
import domain.exception.LinkExpiredException;
import domain.exception.LinkNotFoundException;
import domain.model.ShortLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryLinkRepository;
import repository.LinkRepository;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LinkServiceTest {

    private LinkRepository linkRepository;
    private LinkService linkService;
    private NotificationService notificationService;
    private AppConfig appConfig;
    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        Properties properties = new Properties();
        properties.setProperty("app.base.url", "clck.ru/");
        properties.setProperty("app.default.ttl.hours", "24");
        properties.setProperty("app.default.click.limit", "10");
        properties.setProperty("app.cleanup.interval.seconds", "60");
        appConfig = new AppConfig(properties);
        linkRepository = new InMemoryLinkRepository();
        ShortenerService shortenerService = new ShortenerService();
        notificationService = new NotificationService();
        linkService = new LinkService(linkRepository, shortenerService, notificationService, appConfig);
        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
    }

    @Test
    void shouldGenerateUniqueCodesForDifferentUsers() {
        String url = "https://example.com";
        ShortLink link1 = linkService.createLink(url, user1);
        ShortLink link2 = linkService.createLink(url, user2);
        assertNotEquals(link1.getShortCode(), link2.getShortCode());
        assertEquals(user1, link1.getOwnerUuid());
        assertEquals(user2, link2.getOwnerUuid());
    }

    @Test
    void shouldGenerateDifferentCodesForSameUserAndSameUrl() {
        String url = "https://example.com";
        ShortLink link1 = linkService.createLink(url, user1);
        ShortLink link2 = linkService.createLink(url, user1);
        assertNotEquals(link1.getShortCode(), link2.getShortCode());
    }

    @Test
    void shouldThrowExceptionWhenClickLimitExceeded() {
        ShortLink link = linkService.createLink("https://example.com", user1, 2L);
        String code = link.getShortCode();
        assertEquals("https://example.com", linkService.openLink(code));
        assertEquals("https://example.com", linkService.openLink(code));
        assertThrows(LimitExceededException.class, () -> linkService.openLink(code));
    }

    @Test
    void shouldThrowExceptionWhenLinkIsExpired() {
        ShortLink link = linkService.createLink("https://example.com", user1, 10L);
        String code = link.getShortCode();
        setExpiresAt(link, Instant.now().minusSeconds(1));
        assertThrows(LinkExpiredException.class, () -> linkService.openLink(code));
    }

    @Test
    void shouldAllowOnlyOwnerToUpdateOrDeleteLink() {
        ShortLink link = linkService.createLink("https://example.com", user1);
        String code = link.getShortCode();

        assertThrows(AccessDeniedException.class, () -> linkService.updateClickLimit(code, 50L, user2));
        assertThrows(AccessDeniedException.class, () -> linkService.deleteLink(code, user2));

        assertDoesNotThrow(() -> linkService.updateClickLimit(code, 50L, user1));
        assertEquals(50L, linkRepository.findByCode(code).orElseThrow().getMaxClicks());

        assertDoesNotThrow(() -> linkService.deleteLink(code, user1));
        assertTrue(linkRepository.findByCode(code).isEmpty());
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("not-a-valid-url", user1));
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("ftp://example.com", user1));
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("", user1));
    }

    @Test
    void shouldUseDefaultsFromConfig() {
        ShortLink link = linkService.createLink("https://example.com", user1);
        assertEquals(appConfig.getDefaultClickLimit(), link.getMaxClicks());
        assertTrue(link.getExpiresAt().isAfter(Instant.now().plusSeconds(appConfig.getDefaultTtlHours() * 3600 - 10)));
    }

    @Test
    void shouldRejectInvalidClickLimits() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("https://example.com", user1, 0L));
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("https://example.com", user1, -1L));
        ShortLink link = linkService.createLink("https://example.com", user1, 1L);
        assertThrows(IllegalArgumentException.class, () -> linkService.updateClickLimit(link.getShortCode(), 0L, user1));
    }

    @Test
    void shouldReturnLinksOnlyForOwner() {
        linkService.createLink("https://a.com", user1);
        linkService.createLink("https://b.com", user1);
        linkService.createLink("https://c.com", user2);

        List<ShortLink> user1Links = linkService.getUserLinks(user1);
        List<ShortLink> user2Links = linkService.getUserLinks(user2);

        assertEquals(2, user1Links.size());
        assertEquals(1, user2Links.size());
        assertTrue(user1Links.stream().allMatch(l -> l.getOwnerUuid().equals(user1)));
    }

    @Test
    void shouldThrowWhenLinkNotFound() {
        assertThrows(LinkNotFoundException.class, () -> linkService.openLink("missing"));
        assertThrows(LinkNotFoundException.class, () -> linkService.deleteLink("missing", user1));
    }

    @Test
    void shouldRejectBlankShortCode() {
        assertThrows(IllegalArgumentException.class, () -> linkService.openLink(" "));
        assertThrows(IllegalArgumentException.class, () -> linkService.deleteLink("", user1));
    }

    @Test
    void shouldIncrementClickCounter() {
        ShortLink link = linkService.createLink("https://example.com", user1, 5L);
        linkService.openLink(link.getShortCode());
        linkService.openLink(link.getShortCode());
        ShortLink stored = linkRepository.findByCode(link.getShortCode()).orElseThrow();
        assertEquals(2, stored.getClickCounter());
    }

    @Test
    void shouldBlockWhenUpdatedLimitBelowUsedClicks() {
        ShortLink link = linkService.createLink("https://example.com", user1, 5L);
        linkService.openLink(link.getShortCode());
        linkService.openLink(link.getShortCode());
        linkService.updateClickLimit(link.getShortCode(), 2L, user1);
        assertThrows(LimitExceededException.class, () -> linkService.openLink(link.getShortCode()));
    }

    @Test
    void cleanupSchedulerShouldRemoveExpiredLinks() throws Exception {
        ShortLink expired = linkService.createLink("https://expired.com", user1, 1L);
        ShortLink active = linkService.createLink("https://active.com", user1, 1L);
        setExpiresAt(expired, Instant.now().minusSeconds(5));

        CleanupScheduler scheduler = new CleanupScheduler(linkRepository, notificationService, appConfig);
        java.lang.reflect.Method cleanup = CleanupScheduler.class.getDeclaredMethod("cleanup");
        cleanup.setAccessible(true);
        cleanup.invoke(scheduler);

        assertTrue(linkRepository.findByCode(expired.getShortCode()).isEmpty());
        assertTrue(linkRepository.findByCode(active.getShortCode()).isPresent());
    }

    @Test
    void appConfigShouldReadProperties() {
        Properties properties = new Properties();
        properties.setProperty("app.base.url", "short.test/");
        properties.setProperty("app.default.ttl.hours", "12");
        properties.setProperty("app.default.click.limit", "7");
        properties.setProperty("app.cleanup.interval.seconds", "30");
        AppConfig config = new AppConfig(properties);
        assertEquals("short.test/", config.getBaseUrl());
        assertEquals(12L, config.getDefaultTtlHours());
        assertEquals(7L, config.getDefaultClickLimit());
        assertEquals(30L, config.getCleanupIntervalSeconds());
    }

    @Test
    void appConfigShouldFallbackOnInvalidValues() {
        Properties properties = new Properties();
        properties.setProperty("app.default.ttl.hours", "-1");
        properties.setProperty("app.default.click.limit", "abc");
        properties.setProperty("app.cleanup.interval.seconds", "0");
        AppConfig config = new AppConfig(properties);
        assertEquals(24L, config.getDefaultTtlHours());
        assertEquals(10L, config.getDefaultClickLimit());
        assertEquals(60L, config.getCleanupIntervalSeconds());
    }

    private void setExpiresAt(ShortLink link, Instant instant) {
        try {
            java.lang.reflect.Field field = ShortLink.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(link, instant);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
