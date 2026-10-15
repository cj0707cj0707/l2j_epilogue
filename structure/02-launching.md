# 02. Запуск, остановка и логи

## Как запускаются серверы

Оба сервера — Java-приложения из `libs\`, запускаемые с рабочей директорией `game\` / `login\`:

```
java <аргументы из java.cfg> -jar ../libs/GameServer.jar
java <аргументы из java.cfg> -jar ../libs/LoginServer.jar
```

### Windows: `GameServer.vbs` / `LoginServer.vbs`

1. Берут путь к Java из переменной окружения **`JAVA_HOME`** (обязательна!).
2. Читают **первую строку** `java.cfg` — аргументы JVM.
3. Читают `config\Interface.ini`: если `EnableGUI = true` — сервер работает в **GUI-окне**, иначе в консоли `cmd`.
4. Запускают `-jar ../libs/GameServer.jar`.
5. **Автоперезапуск:** если процесс завершился с кодом `2` (запрошен reboot из админ-команды) — сервер стартует снова в цикле. Иные коды выхода показывают окно ошибки.

Расшифровка кодов выхода (из `.vbs`): `0` — штатное выключение, `2` — рестарт, `1` — необработанное исключение/неверная версия Java, `137` — убит ОС (не хватило памяти), `143` — SIGTERM.

### Linux: `GameServerTask.sh`

Цикл `while :;` с тем же правилом: код `2` → перезапуск через 10 сек; иначе выход. Перед запуском ротирует `log/java0.log.0` и `log/stdout.log` в файлы с датой. `GameServer.sh` просто запускает `GameServerTask.sh` в фоне.

## Аргументы JVM (`java.cfg`)

```
# game/java.cfg
-server
-Dfile.encoding=UTF-8
-Djava.util.logging.manager=org.l2jmobius.log.ServerLogManager
-Dorg.slf4j.simpleLogger.log.com.zaxxer.hikari=warn
-XX:+UseZGC
-Xmx4g -Xms2g
```

| Параметр | Смысл |
|---|---|
| `-server` | Серверный режим JIT |
| `-Dfile.encoding=UTF-8` | Кодировка файлов/БД — **не менять** |
| `ServerLogManager` | Кастомный java.util.logging-менеджер |
| `-XX:+UseZGC` | Сборщик мусора ZGC (low-pause) |
| `-Xmx4g -Xms2g` | Heap: макс. 4 ГБ, старт 2 ГБ |

При изменении `java.cfg` правь **первую строку** файла (`.vbs` читает только её; `.sh` — весь `cat java.cfg`).

## Порядок старта (чистая установка)

1. Поднята MySQL, БД `l2jmobiusepilogue` создана инсталлером (→ [07-database.md](07-database.md)).
2. `login\LoginServer.vbs` — стартует LoginServer (порт **2106** для клиентов, **9014** для GameServer'ов).
3. При первой настройке: `login\GameServerRegister.vbs` — регистрирует GameServer на логине, создаёт `game\config\hexid.txt`.
4. `game\GameServer.vbs` — стартует GameServer (порт **7777**, настраивается в `config\Server.ini` → `GameserverPort`).
5. В консоли каждого сервера доступны команды `help` / `shutdown` / `restart` / `abort` и др.

## Консоль сервера

Помимо GUI-окна, сервер поддерживает консольные команды (зависят от ядра): `status`, `shutdown <sec>`, `restart <sec>`, `abort` (отмена шатдауна). Java-код повторного запуска — `exit code 2`.

## Логи (`game\log\`, `login\log\`)

Настройка — `game\log.cfg` (java.util.logging properties) и `console.cfg` (уровень вывода в консоль, по умолчанию `OFF`).

| Файл | Хендлер | Что пишет |
|---|---|---|
| `java0.log` … `java9.log` | FileHandler | Основной лог (CONFIG+), до 100 МБ × 20 файлов |
| `error0.log` … | ErrorLogHandler | Только ошибки/исключения |
| `chat0.log` | ChatLogHandler | Весь чат |
| `gmaudit0.log` | GMAuditLogHandler | Действия ГМ |
| `item0.log` | ItemLogHandler | Создание/передача предметов |
| `audit0.log` | AuditLogHandler | Аудит (крафт, транзакции) |
| `enchantitem0.log`, `enchantskill0.log` | Enchant* | Заточки предметов/скиллов |
| `olympiad.csv` | OlympiadLogHandler | Результаты олимпиады |
| `accounting0.log` | AccountingLogHandler | Входы/выходы аккаунтов |

Ротация: `%g`-счётчик, лимит 100 МБ, до 20 файлов на хендлер. Паттерн `java0.log` / `java0.log.lck` — `.lck` это lock-файл работающего процесса.

## Остановка

- Штатно: команда `shutdown` в консоли сервера (или GUI-кнопка) — сохраняет игроков, кланы, олимпиаду, при `BackupDatabase = True` делает дамп БД в `backup\`.
- `restart` — корректный рестарт (для `.vbs`/`Task.sh` = exit code 2, скрипт поднимет процесс заново).
- Проверять проблемы: сначала `log\error0.log`, затем `log\java0.log`.
