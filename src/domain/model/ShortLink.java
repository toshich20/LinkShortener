package domain.model;
import java.util.UUID;
import java.time.Instant;

public class ShortLink {
    private int  clickCounter;
    private int maxClicks;
    private UUID ownerUuid;
    private String longURL;
    private String shortCode;
    private Instant createdAt;
    private Instant expiresAt;
}
