package service;

import config.AppConfig;
import domain.exception.AccessDeniedException;
import domain.exception.LimitExceededException;
import domain.exception.LinkExpiredException;
import domain.model.ShortLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryLinkRepository;
import repository.LinkRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LinkServiceTest {

    private LinkRepository linkRepository;
    private LinkService linkService;
    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig();
        linkRepository = new InMemoryLinkRepository();
        ShortenerService shortenerService = new ShortenerService();
        NotificationService notificationService = new NotificationService();

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
    }

    @Test
    void shouldThrowExceptionWhenClickLimitExceeded() {
        ShortLink link = linkService.createLink("https://example.com", user1, 24L, 2L);
        String code = link.getShortCode();

        // Первые 2 перехода должны пройти успешно
        assertEquals("https://example.com", linkService.openLink(code));
        assertEquals("https://example.com", linkService.openLink(code));

        // 3-й переход должен выбросить LimitExceededException
        assertThrows(LimitExceededException.class, () -> linkService.openLink(code));
    }

    @Test
    void shouldThrowExceptionWhenLinkIsExpired() {
        // Создаем ссылку с нулевым временем жизни (0 часов = истекает мгновенно)
        ShortLink link = linkService.createLink("https://example.com", user1, 0L, 10L);
        String code = link.getShortCode();

        assertThrows(LinkExpiredException.class, () -> linkService.openLink(code));
    }

    @Test
    void shouldAllowOnlyOwnerToUpdateOrDeleteLink() {
        ShortLink link = linkService.createLink("https://example.com", user1);
        String code = link.getShortCode();

        // Чужой пользователь (user2) получает отказ
        assertThrows(AccessDeniedException.class, () -> linkService.updateClickLimit(code, 50L, user2));
        assertThrows(AccessDeniedException.class, () -> linkService.deleteLink(code, user2));

        // Владелец (user1) успешно обновляет и удаляет
        assertDoesNotThrow(() -> linkService.updateClickLimit(code, 50L, user1));
        assertEquals(50L, linkRepository.findByCode(code).orElseThrow().getMaxClicks());

        assertDoesNotThrow(() -> linkService.deleteLink(code, user1));
        assertTrue(linkRepository.findByCode(code).isEmpty());
    }

    @Test
    void shouldThrowExceptionForInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createLink("not-a-valid-url", user1));
    }
}