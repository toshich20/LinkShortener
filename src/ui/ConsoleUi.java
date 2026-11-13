package ui;

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
    private final Scanner scanner = new Scanner(System.in);
    private UUID currentUserUuid;

    public ConsoleUi(LinkService linkService) {
        this.linkService = linkService;
        this.currentUserUuid = UUID.randomUUID();
    }

    public void start() {
        System.out.println("=== Сервис сокращения ссылок PROMO IT ===");
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
                    case "0" -> {
                        running = false;
                        System.out.println("Завершение работы...");
                    }
                    default -> System.out.println("Неизвестная команда. Выберите пункт от 0 до 6.");
                }
            } catch (LinkNotFoundException | LinkExpiredException |
                     LimitExceededException | AccessDeniedException |
                     IllegalArgumentException e) {
                // Перехватываем бизнес-ошибки и выводим их текст без падения программы
                System.out.println("\n[ОШИБКА]: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[НЕПРЕДВИДЕННАЯ ОШИБКА]: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n-----------------------------------------");
        System.out.println("Текущий пользователь (UUID): " + currentUserUuid);
        System.out.println("1. Сократить ссылку");
        System.out.println("2. Перейти по короткой ссылке");
        System.out.println("3. Мои ссылки");
        System.out.println("4. Изменить лимит переходов");
        System.out.println("5. Удалить ссылку");
        System.out.println("6. Сменить пользователя (ввести другой UUID или создать новый)");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    private void handleOpenLink() {
        System.out.print("Введите короткий код ссылки: ");
        String shortCode = scanner.nextLine().trim();

        // Если ссылка протухла или не найдена, openLink сам выбросит исключение,
        // и мы сразу попадем в catch в методе start()
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
        System.out.print("Введите длинный URL (с http:// или https://): ");
        String url = scanner.nextLine().trim();

        System.out.print("Введите лимит переходов (или нажмите Enter для значения по умолчанию): ");
        String limitInput = scanner.nextLine().trim();
        Long customLimit = limitInput.isEmpty() ? null : Long.parseLong(limitInput);

        System.out.print("Введите время жизни в часах (или нажмите Enter для значения по умолчанию): ");
        String ttlInput = scanner.nextLine().trim();
        Long customTtl = ttlInput.isEmpty() ? null : Long.parseLong(ttlInput);

        ShortLink created = linkService.createLink(url, currentUserUuid, customTtl, customLimit);
        System.out.println("Ссылка создана! Короткий код: " + created.getShortCode());
    }

    private void handleListMyLinks() {
        List<ShortLink> myLinks = linkService.getUserLinks(currentUserUuid);

        if (myLinks.isEmpty()) {
            System.out.println("У вас пока нет созданных ссылок.");
            return;
        }

        System.out.println("\nВаши ссылки:");
        for (ShortLink link : myLinks) {
            System.out.printf("- Код: %s | URL: %s | Клики: %d/%d | Истекает: %s%n",
                    link.getShortCode(),
                    link.getLongUrl(),
                    link.getClickCounter(),
                    link.getMaxClicks(),
                    link.getExpiresAt());
        }
    }

    private void handleUpdateLimit() {
        System.out.print("Введите короткий код ссылки: ");
        String shortCode = scanner.nextLine().trim();

        System.out.print("Введите новый лимит переходов: ");
        String limitInput = scanner.nextLine().trim();
        long newLimit = Long.parseLong(limitInput);

        linkService.updateClickLimit(shortCode, newLimit, currentUserUuid);
        System.out.println("Лимит переходов для ссылки " + shortCode + " успешно обновлен до " + newLimit + ".");
    }

    private void handleDeleteLink() {
        System.out.print("Введите короткий код ссылки для удаления: ");
        String shortCode = scanner.nextLine().trim();

        linkService.deleteLink(shortCode, currentUserUuid);
        System.out.println("Ссылка " + shortCode + " успешно удалена.");
    }

    private void handleSwitchUser() {
        System.out.print("Введите существующий UUID (или нажмите Enter для создания нового пользователя): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            currentUserUuid = UUID.randomUUID();
            System.out.println("Создан новый пользователь с UUID: " + currentUserUuid);
        } else {
            currentUserUuid = UUID.fromString(input);
            System.out.println("Вы переключились на пользователя: " + currentUserUuid);
        }
    }
}