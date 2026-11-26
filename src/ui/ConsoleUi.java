package ui;

import config.AppConfig;
import domain.exception.AccessDeniedException;
import domain.exception.LimitExceededException;
import domain.exception.LinkExpiredException;
import domain.exception.LinkNotFoundException;
import domain.model.ShortLink;
import service.LinkService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleUi {
    private final LinkService linkService;
    private final AppConfig appConfig;
    private final Scanner scanner = new Scanner(System.in);
    private UUID currentUserUuid;
    private boolean userInitialized;

    public ConsoleUi(LinkService linkService, AppConfig appConfig) {
        this.linkService = linkService;
        this.appConfig = appConfig;
    }

    public void start() {
        System.out.println("=== Сервис сокращения ссылок ===");
        System.out.println("UUID создаётся при первом запросе на создание короткой ссылки.");
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleCreateLink();
                    case "2" -> handleOpenLink();
                    case "3" -> handleListMyLinks();
                    case "4" -> handleUpdateLimit();
                    case "5" -> handleDeleteLink();
                    case "6" -> handleSwitchUser();
                    case "7" -> printHelp();
                    case "0" -> {
                        running = false;
                        System.out.println("Завершение работы...");
                    }
                    default -> System.out.println("Неизвестная команда. Введите 7 для справки.");
                }
            } catch (LinkNotFoundException | LinkExpiredException |
                     LimitExceededException | AccessDeniedException |
                     IllegalArgumentException e) {
                System.out.println("\n[ОШИБКА]: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[НЕПРЕДВИДЕННАЯ ОШИБКА]: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n-----------------------------------------");
        System.out.println("Текущий пользователь (UUID): " + (userInitialized ? currentUserUuid : "будет создан при первом сокращении ссылки"));
        System.out.println("1. Сократить ссылку");
        System.out.println("2. Перейти по короткой ссылке");
        System.out.println("3. Мои ссылки");
        System.out.println("4. Изменить лимит переходов");
        System.out.println("5. Удалить ссылку");
        System.out.println("6. Сменить пользователя (ввести UUID или создать новый)");
        System.out.println("7. Помощь");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private void printHelp() {
        System.out.println("\nСправка:");
        System.out.println("- Короткая ссылка имеет формат: " + appConfig.getBaseUrl() + "<код>");
        System.out.println("- Для перехода введите только код или полную короткую ссылку.");
        System.out.println("- Время жизни ссылки задаётся системой из config.properties: " + appConfig.getDefaultTtlHours() + " ч.");
        System.out.println("- Лимит переходов по умолчанию: " + appConfig.getDefaultClickLimit() + ".");
        System.out.println("- Редактировать и удалять ссылки может только владелец (UUID).");
    }

    private void handleOpenLink() {
        System.out.print("Введите короткий код или короткую ссылку: ");
        String shortCode = normalizeShortCode(scanner.nextLine());

        String originalUrl = linkService.openLink(shortCode);
        System.out.println("Перенаправление на: " + originalUrl);

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(originalUrl));
            } catch (Exception e) {
                System.out.println("Не удалось автоматически открыть браузер: " + e.getMessage());
            }
        } else {
            System.out.println("Открытие браузера не поддерживается вашей системой. Скопируйте ссылку вручную.");
        }
    }

    private void handleCreateLink() {
        ensureUserInitialized();
        System.out.print("Введите длинный URL (с http:// или https://): ");
        String url = scanner.nextLine().trim();

        System.out.print("Введите лимит переходов (Enter — значение по умолчанию " + appConfig.getDefaultClickLimit() + "): ");
        Long customLimit = readOptionalPositiveLong("Лимит переходов");

        ShortLink created = linkService.createLink(url, currentUserUuid, customLimit);
        System.out.println("Ссылка создана!");
        System.out.println("Короткая ссылка: " + appConfig.getBaseUrl() + created.getShortCode());
        System.out.println("Код: " + created.getShortCode());
        System.out.println("TTL задаётся системой: " + appConfig.getDefaultTtlHours() + " ч.");
    }

    private void handleListMyLinks() {
        ensureUserInitialized();
        List<ShortLink> myLinks = linkService.getUserLinks(currentUserUuid);

        if (myLinks.isEmpty()) {
            System.out.println("У вас пока нет созданных ссылок.");
            return;
        }

        System.out.println("\nВаши ссылки:");
        for (ShortLink link : myLinks) {
            System.out.printf("- %s%s | URL: %s | Клики: %d/%d | Истекает: %s%n",
                    appConfig.getBaseUrl(),
                    link.getShortCode(),
                    link.getLongUrl(),
                    link.getClickCounter(),
                    link.getMaxClicks(),
                    link.getExpiresAt());
        }
    }

    private void handleUpdateLimit() {
        ensureUserInitialized();
        System.out.print("Введите короткий код или короткую ссылку: ");
        String shortCode = normalizeShortCode(scanner.nextLine());

        System.out.print("Введите новый лимит переходов: ");
        long newLimit = readRequiredPositiveLong("Лимит переходов");

        linkService.updateClickLimit(shortCode, newLimit, currentUserUuid);
        System.out.println("Лимит переходов для ссылки " + shortCode + " успешно обновлен до " + newLimit + ".");
    }

    private void handleDeleteLink() {
        ensureUserInitialized();
        System.out.print("Введите короткий код или короткую ссылку для удаления: ");
        String shortCode = normalizeShortCode(scanner.nextLine());

        linkService.deleteLink(shortCode, currentUserUuid);
        System.out.println("Ссылка " + shortCode + " успешно удалена.");
    }

    private void handleSwitchUser() {
        System.out.print("Введите существующий UUID (Enter — создать нового пользователя): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            currentUserUuid = UUID.randomUUID();
            userInitialized = true;
            System.out.println("Создан новый пользователь с UUID: " + currentUserUuid);
            return;
        }
        try {
            currentUserUuid = UUID.fromString(input);
            userInitialized = true;
            System.out.println("Вы переключились на пользователя: " + currentUserUuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный UUID. Пример: 123e4567-e89b-12d3-a456-426614174000");
        }
    }

    private void ensureUserInitialized() {
        if (!userInitialized) {
            currentUserUuid = UUID.randomUUID();
            userInitialized = true;
            System.out.println("Создан UUID пользователя: " + currentUserUuid);
        }
    }

    private String normalizeShortCode(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Короткий код не может быть пустым");
        }
        String baseUrl = appConfig.getBaseUrl();
        if (value.startsWith(baseUrl)) {
            value = value.substring(baseUrl.length());
        }
        if (value.startsWith("http://")) {
            value = value.substring("http://".length());
        }
        if (value.startsWith("https://")) {
            value = value.substring("https://".length());
        }
        if (value.startsWith(baseUrl)) {
            value = value.substring(baseUrl.length());
        }
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash < value.length() - 1) {
            value = value.substring(slash + 1);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Короткий код не может быть пустым");
        }
        return value;
    }

    private Long readOptionalPositiveLong(String fieldName) {
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        long value = parsePositiveLong(input, fieldName);
        return value;
    }

    private long readRequiredPositiveLong(String fieldName) {
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        return parsePositiveLong(input, fieldName);
    }

    private long parsePositiveLong(String input, String fieldName) {
        try {
            long value = Long.parseLong(input);
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " должен быть больше 0");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " должен быть целым числом больше 0");
        }
    }
}