# 04. LoginServer (`login\`)

LoginServer — первый контакт клиента: авторизация аккаунтов, генерация сессий, список игровых серверов. После логина клиент переключается на GameServer (порт 7777).

## Структура папки

| Элемент | Назначение |
|---|---|
| `config\Server.ini` | Порты, безопасность, автосоздание аккаунтов, плановый рестарт |
| `config\Network.ini` | Тонкая настройка сети (пулы потоков пакетов, лимиты) |
| `config\Database.ini` | Подключение к MySQL (`l2jmobiusepilogue`) |
| `config\Threads.ini` | Размеры Scheduled/Instant пулов потоков |
| `config\Interface.ini` | GUI-консоль (EnableGUI, DarkTheme) |
| `data\servername.xml` | Соответствие ID → имя сервера для окна выбора сервера |
| `data\servername.xsd` | XML-схема для servername.xml |
| `banned_ip.cfg` | Статический список забаненных IP/подсетей (можно с датой истечения) |
| `log\` | Логи (формат как у GameServer, см. [02-launching.md](02-launching.md)) |
| `LoginServer.vbs` / `.sh` / `LoginServerTask.sh` | Запуск (см. [02-launching.md](02-launching.md)) |
| `AccountManager.vbs` / `.sh` | Консольный менеджер аккаунтов |
| `GameServerRegister.vbs` / `.sh` | Регистрация GameServer → генерирует `game\config\hexid.txt` |

## Ключевые настройки `config\Server.ini`

| Параметр | Значение | Смысл |
|---|---|---|
| `LoginserverHostname` | `0.0.0.0` | Адрес, на котором логин слушает клиентов |
| `LoginserverPort` | `2106` | Порт для игровых клиентов |
| `LoginHostname` | `127.0.0.1` | Адрес для подключений GameServer'ов |
| `LoginPort` | `9014` | Порт для GameServer'ов |
| `LoginTryBeforeBan` | `5` | Неверных паролей до бана IP |
| `LoginBlockAfterBan` | `900` | Секунд бана после превышения попыток |
| `AcceptNewGameServer` | `True` | Разрешить регистрацию новых GS без ручного добавления |
| `EnableFloodProtection` | `True` | Антифлуд подключений (`FastConnectionLimit`, `NormalConnectionTime`, `FastConnectionTime`, `MaxConnectionPerIP=50`) |
| `ShowLicence` | `True` | Экран лицензии после логина |
| `AutoCreateAccounts` | `True` | **Аккаунты создаются автоматически при первом входе клиента** |
| `LoginRestartSchedule` | `False` | Плановый рестарт логина (часы: `LoginRestartTime`) |

## База данных LoginServer

Только 4 таблицы (`db_installer\sql\login\`):

| Таблица | Содержимое |
|---|---|
| `accounts` | Аккаунты: логин, пароль (хэш), email, `accessLevel`, `lastIP`, `lastServer`, время создания/активности |
| `accounts_ipauth` | Белые/чёрные списки IP по аккаунту (IP-авторизация) |
| `account_data` | Серверные переменные аккаунта (запомненные настройки клиента) |
| `gameservers` | Зарегистрированные GameServer'ы: ID, hexid, хост |

Подробнее о БД → [07-database.md](07-database.md)

## `data\servername.xml`

Таблица ID → название сервера (реальные имена официальных серверов: 1 = Bartz, 2 = Sieghardt, ...). Этот сервер просит **ID 1 (Bartz)** — см. `game/config/Server.ini → RequestServerID = 1`. Имя, которое видит клиент в списке, берётся отсюда.

## Аккаунты

- Создание: автоматически при входе (AutoCreateAccounts=True), либо через `AccountManager.vbs` (интерактивное меню: создать/сменить пароль/удалить/уровень доступа).
- `accessLevel`: 0 — игрок; отрицательные — бан (значения определяются `game/config/AccessLevels.xml`); >0 — ГМ-уровни (команды — `game/config/AdminCommands.xml`).
- Баны по IP: через ГМ-команды в игре (таблица `punishments`) или статически в `banned_ip.cfg`.

## Регистрация GameServer

1. В `login\GameServerRegister.vbs` — запустить, выбрать ID (1 = Bartz).
2. LoginServer сохраняет hexid в `gameservers`, а инсталлер пишет файл `game\config\hexid.txt`.
3. GameServer при старте читает `hexid.txt` и подключается к `Server.ini → LoginHost:LoginPort`.
4. Если hexid потерян/не совпадает — GameServer не примется логином; повторить регистрацию.

## Связанные конфиги GameServer

- `game\config\Server.ini` — куда подключаться (`LoginHost = 127.0.0.1`, `LoginPort = 9014`), свой порт `GameserverPort = 7777`, `RequestServerID = 1`, лимит онлайна, разрешённые протоколы клиента (**152** — это протокол Gracia Epilogue), плановые рестарты.
- `game\config\default-ipconfig.xml` — автоопределение внешних/внутренних IP (для игры через интернет переименовывается в `ipconfig.xml` и правится).
