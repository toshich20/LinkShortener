import config.AppConfig;
import repository.InMemoryLinkRepository;
import repository.LinkRepository;
import service.CleanupScheduler;
import service.LinkService;
import service.NotificationService;
import service.ShortenerService;
import ui.ConsoleUi;

public class Main {
    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();

        LinkRepository linkRepository = new InMemoryLinkRepository();
        ShortenerService shortenerService = new ShortenerService();
        NotificationService notificationService = new NotificationService();

        LinkService linkService = new LinkService(
                linkRepository,
                shortenerService,
                notificationService,
                appConfig
        );

        CleanupScheduler cleanupScheduler = new CleanupScheduler(
                linkRepository,
                notificationService,
                appConfig
        );
        cleanupScheduler.start();

        ConsoleUi consoleUi = new ConsoleUi(linkService);
        try {
            consoleUi.start();
        } finally {
            cleanupScheduler.stop();
        }
    }
}