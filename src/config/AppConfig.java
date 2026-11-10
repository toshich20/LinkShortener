package config;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;


public class AppConfig {
    private static final String CONFIG_FILE = "config.properties";

    private final long defaultTtlHours;
    private final long defaultClickLimit;
    private final long cleanupIntervalSeconds;

    public AppConfig() {
        Properties properties = new Properties();

        //Читаем файл стримом
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Файл " + CONFIG_FILE + " не найден в resources, будут использоваться значения по умолчанию.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла конфигурации: " + e.getMessage());
        }

        //Парсим файл конфига и присваиваем значения
        this.defaultTtlHours = Long.parseLong(properties.getProperty("defaultTtlHours", "24"));
        this.defaultClickLimit = Long.parseLong(properties.getProperty("defaultClickLimit", "24"));
        this.cleanupIntervalSeconds = Long.parseLong(properties.getProperty("cleanupIntervalSeconds", "24"));
    }

    //Геттеры
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
