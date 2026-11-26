# Сервис сокращения ссылок (LinkShortener)

Консольное Java-приложение для сокращения ссылок с UUID-идентификацией пользователей, лимитом переходов, TTL, автоочисткой истёкших ссылок и уведомлениями. Проект реализован на чистой Java без фреймворков.

## Возможности

- **Сокращение URL**: длинный `http://`/`https://` URL преобразуется в короткий код формата `clck.ru/<код>`.
- **Уникальность для пользователей**: один и тот же URL, сокращённый разными пользователями, получает разные короткие коды.
- **Лимит переходов**: пользователь задаёт лимит при создании; после исчерпания переход блокируется и выводится уведомление.
- **TTL**: время жизни задаётся системой в `src/config.properties`, пользователь его не задаёт. Истёкшие ссылки блокируются при обращении и удаляются фоновым планировщиком.
- **UUID**: идентификатор создаётся при первом запросе на создание короткой ссылки или при ручном переключении пользователя.
- **Права владельца**: изменять лимит и удалять ссылку может только создатель.
- **Консольный редирект**: при переходе по короткому коду сервис пытается открыть исходный URL в браузере через `Desktop.browse`.

## Архитектура

```
src/
  config/AppConfig.java                  - чтение конфигурации
  config.properties                      - параметры по умолчанию
  domain/model/ShortLink.java            - модель ссылки
  domain/model/User.java                 - модель пользователя
  domain/exception/*                     - доменные исключения
  repository/LinkRepository.java         - контракт хранилища
  repository/InMemoryLinkRepository.java - потокобезопасное in-memory хранилище
  service/LinkService.java               - бизнес-логика
  service/ShortenerService.java          - генерация Base62-кода
  service/NotificationService.java       - уведомления
  service/CleanupScheduler.java          - фоновая очистка истёкших ссылок
  ui/ConsoleUi.java                      - CLI
  Main.java                              - composition root
test/
  service/LinkServiceTest.java           - JUnit 5 тесты
```

## Конфигурация

Файл: `src/config.properties`

```properties
app.base.url=clck.ru/
app.default.ttl.hours=24
app.default.click.limit=10
app.cleanup.interval.seconds=60
```

Все ключевые параметры читаются из конфигурации. При некорректном значении используется безопасное значение по умолчанию.

## Запуск

### IntelliJ IDEA

1. Открыть проект.
2. Запустить `src/Main.java`.

### Из терминала (javac/java)

```bash
mkdir -p build/classes
find src -name '*.java' | sort > build/srcfiles.txt
javac -d build/classes @build/srcfiles.txt
cp src/config.properties build/classes/config.properties
java -cp build/classes Main
```

## Тестирование

### Локальный скрипт

```bash
./scripts/test.sh
```

Скрипт компилирует `src` и `test`, использует JUnit jars из локального `~/.m2` и запускает тесты пакета `service`.

### GitHub Actions

Workflow `.github/workflows/tests.yml` компилирует проект на Java 21 и запускает JUnit Console Standalone на push/pull request.

## Команды CLI

- `1` — сократить ссылку. Ввод: длинный URL и опциональный лимит переходов. TTL задаётся системой.
- `2` — перейти по короткому коду/ссылке. При успехе открывает исходный URL в браузере.
- `3` — показать мои ссылки.
- `4` — изменить лимит переходов (только владелец).
- `5` — удалить ссылку (только владелец).
- `6` — сменить пользователя: ввести существующий UUID или создать новый.
- `7` — помощь.
- `0` — выход.

## Примеры сценариев ТЗ

- Два разных пользователя сокращают один URL — получают разные коды.
- Лимит переходов исчерпан — `openLink` блокирует переход и уведомляет.
- TTL истёк — ссылка недоступна, уведомление выводится, CleanupScheduler удаляет истёкшие записи.
- Чужой UUID не может изменить лимит или удалить ссылку.
