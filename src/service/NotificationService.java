package service;

public class NotificationService {
    public void notifyLimitReached(String shortCode, String originalUrl) {
        System.out.println("[УВЕДОМЛЕНИЕ]\nЛимит переходов для " + shortCode + "(" + originalUrl + ") исчерпан. Ссылка больше недоступна");
    }
    public void notifyExpired(String shortCode, String originalUrl) {
        System.out.println("[УВЕДОМЛЕНИЕ]\nВремя жизни ссылки: " + shortCode + "(" + originalUrl + ") истекло. Ссылка больше недоступна");
    }
    public void notifyLinkCleaned(String shortCode, String originalUrl) {
        System.out.println("[УВЕДОМЛЕНИЕ]\nСсылка " + shortCode + "(" + originalUrl + ") удалена");
    }
}
