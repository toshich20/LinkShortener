package domain.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String shortCode) {
        super("Ссылка с кодом" + shortCode + "не найдена");
    }
}
