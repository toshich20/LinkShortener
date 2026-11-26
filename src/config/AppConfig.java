package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = "config.properties";

    private final String baseUrl;
    private final long defaultTtlHours;
    private final long defaultClickLimit;
    private final long cleanupIntervalSeconds;

    public AppConfig() {
        this(loadFromClasspath());
    }

    public AppConfig(Properties properties) {
        Properties safeProperties = properties == null ? new Properties() : properties;
        this.baseUrl = readString(safeProperties, "app.base.url", "clck.ru/");
        this.defaultTtlHours = readPositiveLong(safeProperties, "app.default.ttl.hours", 24L);
        this.defaultClickLimit = readPositiveLong(safeProperties, "app.default.click.limit", 10L);
        this.cleanupIntervalSeconds = readPositiveLong(safeProperties, "app.cleanup.interval.seconds", 60L);
    }

    private static Properties loadFromClasspath() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Файл " + CONFIG_FILE + " не найден в classpath, используются значения по умолчанию.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла конфигурации: " + e.getMessage());
        }
        return properties;
    }

    private String readString(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private long readPositiveLong(Properties properties, String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                System.err.println("Некорректное значение " + key + ": должно быть больше 0. Используется значение по умолчанию " + defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            System.err.println("Некорректное значение " + key + ": " + value + ". Используется значение по умолчанию " + defaultValue);
            return defaultValue;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public long getDefaultTtlHours() {
        return defaultTtlHours;
    }

    public long getDefaultClickLimit() {
        return defaultClickLimit;
    }

    public long getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }
}
