# 01. Корневая структура `C:\EpilogueServer`

Это развёрнутая (deploy-)сборка сервера **L2J-Mobius Gracia Epilogue**. Исходников ядра здесь нет — ядро собрано в JAR-файлы, а вся «гибкая» часть (датапак, скрипты, конфиги) лежит рядом в виде текстовых файлов и правится напрямую.

```
C:\EpilogueServer\
├── game\          # GameServer — игровое ядро, датапак, конфиги, логи
├── login\         # LoginServer — авторизация, список серверов
├── libs\          # Скомпилированные JAR-файлы (ядро + зависимости)
├── db_installer\  # Установщик БД: SQL-скрипты создания схемы + дампы
├── backup\        # Сюда пишутся автоматические бэкапы БД (см. Database.ini)
├── images\        # Логотипы проекта L2J Mobius (splash, иконки)
├── structure\     # Эта документация
├── readme.txt     # Патчноуты хроники Gracia Epilogue (описание контента)
└── .vs\           # Служебная папка Visual Studio (следы открытия проекта)
```

## `game\` — GameServer

Главная папка. Всё игровое содержимое:

| Элемент | Назначение |
|---|---|
| `config\` | Все конфигурации ядра (INI/XML) + `Custom\` с кастомными модами. → [03-game-config.md](03-game-config.md) |
| `data\` | Датапак: XML-данные, HTML-диалоги, спавны, geodata, зоны. → [05-datapack-data.md](05-datapack-data.md) |
| `data\scripts\` | Java-скрипты: квесты, ИИ, инстансы, события, хендлеры. → [06-scripts-and-quests.md](06-scripts-and-quests.md) |
| `log\` | Логи сервера (см. [02-launching.md](02-launching.md)) |
| `GameServer.vbs` / `.sh` | Запуск (Windows GUI-консоль / Linux) |
| `GameServerTask.sh` | Linux: цикл «запуск → рестарт при коде 2» |
| `Search.sh` / `.vbs` | Утилита поиска по файлам сервера |
| `java.cfg` | Аргументы JVM |
| `console.cfg`, `log.cfg` | Уровень логирования консоли и файловые лог-хендлеры |

## `login\` — LoginServer

Авторизация аккаунтов и маршрутизация на игровые серверы. Подробнее → [04-login-server.md](04-login-server.md)

| Элемент | Назначение |
|---|---|
| `config\` | `Server.ini`, `Network.ini`, `Database.ini`, `Threads.ini`, `Interface.ini` |
| `data\` | `servername.xml` — список игровых серверов для клиента |
| `log\` | Логи |
| `LoginServer.vbs` / `.sh`, `LoginServerTask.sh` | Запуск |
| `AccountManager.sh` / `.vbs` | Консольный менеджер аккаунтов (создание/смена пароля/уровень доступа) |
| `GameServerRegister.sh` / `.vbs` | Регистрация GameServer (генерирует `hexid.txt`) |
| `banned_ip.cfg` | Забаненные IP-адреса |

## `libs\` — ядро и зависимости

| Файл | Назначение |
|---|---|
| `GameServer.jar` | Ядро игрового сервера (все пакеты `org.l2jmobius.gameserver.*`) |
| `LoginServer.jar` | Ядро логин-сервера |
| `HikariCP-7.0.2.jar` | Пул соединений к БД |
| `mysql-connector-j-9.5.0.jar` | JDBC-драйвер MySQL |
| `slf4j-api-2.0.17.jar`, `slf4j-simple-2.0.17.jar` | Фасад логирования (используется HikariCP) |

Оба сервера запускаются командой вида `java <аргументы из java.cfg> -jar ../libs/<Jar>.jar` — то есть **рабочей директорией должна быть `game\` (или `login\`)**, а не `libs\`.

## `db_installer\` — установка и дампы БД

| Элемент | Назначение |
|---|---|
| `sql\game\` | 116 SQL-скриптов создания таблиц игровой части (`characters.sql`, `items.sql`, ...) |
| `sql\login\` | 4 скрипта логин-части (`accounts.sql`, `gameservers.sql`, ...) |
| `DatabaseInstaller.jar` / `.sh` / `.vbs` | Интерактивный установщик: создаёт схему `l2jmobiusepilogue`, накатывает SQL и опционально автоматизацию |
| `config\` | Настройки установщика |
| `dumps\` | Дампы БД (`l2jmobiusepilogue_dump_05.09.2026_*.sql`) |

Подробнее → [07-database.md](07-database.md)

## `backup\` и `images\`

- `backup\` — путь из `game/config/Database.ini` (`BackupPath = ../backup/`): если включить `BackupDatabase = True`, сюда пишутся дампы MySQL перед рестартом/выключением.
- `images\` — логотипы L2J Mobius (`l2jmobius_*.png`, `splash.png`) — только оформление.

## `readme.txt`

Не технический readme, а **патчноуты хроник** от CT1 (Kamael) до Gracia Epilogue: что появилось в каждой хронике (почта, виталити, территориальные войны, сиды Грации, камалоки, Пайлаки и т.д.). Полезно как справочник по игровому контенту этой версии.
