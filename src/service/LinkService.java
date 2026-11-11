package service;

import config.AppConfig;
import domain.exception.AccessDeniedException;
import domain.exception.LimitExceededException;
import domain.exception.LinkExpiredException;
import domain.exception.LinkNotFoundException;
import domain.model.ShortLink;
import repository.InMemoryLinkRepository;
import repository.LinkRepository;

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

    public ShortLink createLink(String originalUrl, UUID ownerUuid, Long customTtlHours, Long customClickLimit) {
        //Валидация входного URL
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Некорректный URL. Ссылка должна начинаться с http:// или https://");
        }

        //Определение лимитов (дефолты из AppConfig при null)
        long ttlHours = (customTtlHours != null) ? customTtlHours : appConfig.getDefaultTtlHours();
        long maxClicks = (customClickLimit != null) ? customClickLimit : appConfig.getDefaultClickLimit();
        //Защита от коллизий: генерируем код, пока не найдем свободный
        String shortCode;
        do {
            shortCode = shortenerService.generateShortCode(originalUrl, ownerUuid);
        } while (linkRepository.findByCode(shortCode).isPresent());

        //Создание и сохранение объекта ссылки
        ShortLink shortLink = new ShortLink(originalUrl, ttlHours, maxClicks, ownerUuid, shortCode);
        linkRepository.save(shortLink);

        return shortLink;
    }

    boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public String openLink(String shortcode) {
        //Находим ссылку
        ShortLink link = linkRepository.findByCode(shortcode)
                .orElseThrow(() -> new LinkNotFoundException(shortcode));

        //Проверяем не протухла ли она
        if (link.isExpired()) {
            notificationService.notifyExpired(link.getShortCode(), link.getLongUrl());
            throw new LinkExpiredException("Ссылка протухла)");
        }

        //Проверяем не исчерпан ли лимит переходов
        if (link.isClickLimitReached()) {
            notificationService.notifyLimitReached(link.getShortCode(), link.getLongUrl());
            throw new LimitExceededException("Исчерпан лимит переходов по ссылке");
        }

        //Увеличиваем счетчик переходов и сохраняем
        link.incrementClicks();
        linkRepository.save(link);

        //Вохвращаем оригинальный URL
        return link.getLongUrl();
    }

    private ShortLink getLinkAndCheckOwner(String shortCode, UUID ownerUuid) {
        ShortLink link = linkRepository.findByCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        if (!link.getOwnerUuid().equals(ownerUuid)) {
            throw new AccessDeniedException("У вас нет прав для управления этой ссылкой");
        }
        return link;
    }

    public void updateClickLimit(String shortCode, long newLimit, UUID userUuid) {
        if (newLimit <= 0) {
            throw new IllegalArgumentException("Лимит должен быть больше 0");
        }

        ShortLink link = getLinkAndCheckOwner(shortCode, userUuid);
        link.setMaxClicks(newLimit);
        linkRepository.save(link);
    }

    public void deleteLink(String shortCode, UUID userUuid) {
        getLinkAndCheckOwner(shortCode, userUuid);
        linkRepository.deleteByCode(shortCode);
    }

    public List<ShortLink> getUserLinks(UUID userUuid) {
        return linkRepository.findByOwnerUuid(userUuid);
    }
}