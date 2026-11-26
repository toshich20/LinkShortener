package service;

import config.AppConfig;
import domain.exception.AccessDeniedException;
import domain.exception.LimitExceededException;
import domain.exception.LinkExpiredException;
import domain.exception.LinkNotFoundException;
import domain.model.ShortLink;
import repository.LinkRepository;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class LinkService {
    private final LinkRepository linkRepository;
    private final ShortenerService shortenerService;
    private final NotificationService notificationService;
    private final AppConfig appConfig;

    public LinkService(LinkRepository linkRepository, ShortenerService shortenerService, NotificationService notificationService, AppConfig appConfig) {
        this.linkRepository = linkRepository;
        this.shortenerService = shortenerService;
        this.notificationService = notificationService;
        this.appConfig = appConfig;
    }

    public ShortLink createLink(String originalUrl, UUID ownerUuid, Long customClickLimit) {
        if (ownerUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть пустым");
        }
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Некорректный URL. Ссылка должна начинаться с http:// или https://");
        }

        long ttlHours = appConfig.getDefaultTtlHours();
        long maxClicks = (customClickLimit != null) ? customClickLimit : appConfig.getDefaultClickLimit();
        validatePositive("Лимит переходов", maxClicks);
        validatePositive("Время жизни ссылки", ttlHours);

        String shortCode;
        do {
            shortCode = shortenerService.generateShortCode(originalUrl, ownerUuid);
        } while (linkRepository.findByCode(shortCode).isPresent());

        ShortLink shortLink = new ShortLink(originalUrl, ttlHours, maxClicks, ownerUuid, shortCode);
        linkRepository.save(shortLink);
        return shortLink;
    }

    public ShortLink createLink(String originalUrl, UUID ownerUuid) {
        return createLink(originalUrl, ownerUuid, null);
    }

    public ShortLink createLink(String originalUrl, UUID ownerUuid, Long ignoredCustomTtlHours, Long customClickLimit) {
        return createLink(originalUrl, ownerUuid, customClickLimit);
    }

    boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String openLink(String shortCode) {
        ShortLink link = findByCodeOrThrow(shortCode);

        if (link.isExpired()) {
            notificationService.notifyExpired(link.getShortCode(), link.getLongUrl());
            throw new LinkExpiredException("Время жизни ссылки истекло. Ссылка недоступна");
        }

        if (link.isClickLimitReached()) {
            notificationService.notifyLimitReached(link.getShortCode(), link.getLongUrl());
            throw new LimitExceededException("Лимит переходов исчерпан. Ссылка недоступна");
        }

        link.incrementClicks();
        linkRepository.save(link);
        return link.getLongUrl();
    }

    private ShortLink getLinkAndCheckOwner(String shortCode, UUID ownerUuid) {
        ShortLink link = findByCodeOrThrow(shortCode);
        if (ownerUuid == null || !link.getOwnerUuid().equals(ownerUuid)) {
            throw new AccessDeniedException("У вас нет прав для управления этой ссылкой");
        }
        return link;
    }

    private ShortLink findByCodeOrThrow(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Короткий код не может быть пустым");
        }
        return linkRepository.findByCode(shortCode.trim())
                .orElseThrow(() -> new LinkNotFoundException(shortCode.trim()));
    }

    public void updateClickLimit(String shortCode, long newLimit, UUID userUuid) {
        validatePositive("Лимит переходов", newLimit);
        ShortLink link = getLinkAndCheckOwner(shortCode, userUuid);
        link.setMaxClicks(newLimit);
        linkRepository.save(link);
    }

    public void deleteLink(String shortCode, UUID userUuid) {
        ShortLink link = getLinkAndCheckOwner(shortCode, userUuid);
        linkRepository.deleteByCode(link.getShortCode());
    }

    public List<ShortLink> getUserLinks(UUID userUuid) {
        if (userUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть пустым");
        }
        return linkRepository.findByOwnerUuid(userUuid);
    }

    private void validatePositive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " должен быть больше 0");
        }
    }
}