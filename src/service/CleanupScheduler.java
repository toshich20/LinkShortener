package service;

import config.AppConfig;
import domain.model.ShortLink;
import repository.LinkRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CleanupScheduler {
    private final LinkRepository linkRepository;
    private final NotificationService notificationService;
    private final AppConfig appConfig;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CleanupScheduler(LinkRepository linkRepository,
                            NotificationService notificationService,
                            AppConfig appConfig) {
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
        this.appConfig = appConfig;
    }

    // Запуск фоновой службы
    public void start() {
        long interval = appConfig.getCleanupIntervalSeconds();

        // Запускает метод cleanup каждые N секунд
        scheduler.scheduleAtFixedRate(
                this::cleanup,
                0,            // Начальная задержка перед первым запуском (0 сек)
                interval,     // Период повторения
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void cleanup() {
        List<ShortLink> linkList = linkRepository.findAll();
        for (ShortLink link : linkList) {
            if (link.isExpired()) {
                String shortCode = link.getShortCode();
                linkRepository.deleteByCode(shortCode);
                notificationService.notifyLinkCleaned(shortCode, link.getLongUrl());
            }
        }
    }
}