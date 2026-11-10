package service;

import java.security.SecureRandom;
import java.util.UUID;

public class ShortenerService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateShortCode(String originalUrl, UUID ownerUuid) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(randomIndex));
        }
        return sb.toString();
    }
}