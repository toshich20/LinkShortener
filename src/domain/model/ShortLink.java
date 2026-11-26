package domain.model;
import java.time.Duration;
import java.util.UUID;
import java.time.Instant;

public class ShortLink {
    private long  clickCounter;
    private long maxClicks;
    private final UUID ownerUuid;
    private final String longUrl;
    private final String shortCode;
    private final Instant createdAt;
    private Instant expiresAt;

    public ShortLink(String longUrl, long ttlHours, long maxClicks, UUID ownerUuid, String shortCode){
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plus(Duration.ofHours(ttlHours));
        this.longUrl = longUrl;
        this.maxClicks = maxClicks;
        this.ownerUuid = ownerUuid;
        this.clickCounter = 0;
        this.shortCode = shortCode;
    }
    public boolean isExpired() {
        return this.expiresAt.isBefore(Instant.now());
    }

    public boolean isClickLimitReached() {
        return this.clickCounter >= this.maxClicks;
    }

    //Геттеры
    public void incrementClicks() {
        this.clickCounter++;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public long getClickCounter() {
        return clickCounter;
    }

    public long getMaxClicks() {
        return maxClicks;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    //Сеттер для максимального количества кликов
    public void setMaxClicks(long maxClicks) {
        this.maxClicks = maxClicks;
    }
}
