# 13. Удалённая регистрация на олимпиаду: `/oly`, `//oly`, `.oly`

> Правка от 14.09.2026. Задача: добавить команду, которой игроки могли бы регистрироваться
> на участие во **внеклассовой** (бесклассовой, class-irrelevant 1x1) олимпиаде дистанционно,
> без подхода к Grand Olympiad Manager.
> **Статус: установлен** (патч ядра — в `libs\GameServer.jar`, при остановленном GS; скрипты
> компилируются на старте). В игре проверку выполняет администратор — план проверки в
> `_oly_reg\README.md`.

## Как пользоваться

Ввод в чат (любой из префиксов): `/oly` — регистрация; `//oly` — то же (клиент шлёт пакет
`SendBypassBuildCmd`, ядро вызывает `admin_oly`); `.oly` — voiced-вариант.

| Команда | Действие |
|---|---|
| `oly` | регистрация на бесклассовый бой (`Olympiad.registerNoble(player, false)`) |
| `oly cancel` | отмена регистрации (`Olympiad.unRegisterNoble(player)`) |
| `oly status` | период, очки, победы/поражения, размер листа ожидания, «в листе ли ты» |

Вся валидация — штатная, ядром (ноблес, базовый класс, ≥5 очков, инвентарь ≤80%, не в матче,
период активен, не в ивенте): сообщения об отказе идентичны кнопкам у NPC-менеджера, поскольку
вызывается тот же публичный API, что и в bypass `OlympiadNoble 4/1`
(`entity.actor.instance.OlympiadManager`).

## Правки

| Слой | Файл | Что сделано |
|---|---|---|
| Скрипт | `handlers\chat\commands\voiced\Oly.java` (новый) | voiced-хендлер `oly`; статическая точка входа `Oly.dispatch(player, params)` используется всеми тремя префиксами |
| Скрипт | `handlers\chat\commands\admin\AdminOly.java` (новый) | обработчик `admin_oly` для `//oly`, делегирует в `Oly.dispatch` |
| Скрипт | `handlers\MasterHandler.java` | импорты + регистрация `AdminOly.class` (admin-секция) и `Oly.class` (voiced-секция) |
| Скрипт | `handlers\chat\channels\ChatGeneral.java` | алиас: текст `/oly ...` / `//oly ...` из обычного чата уходит в voiced-хендлер; прочий `/...`-текст broadcastится как раньше (перехватывается только команда `oly`) |
| Конфиг | `game\config\AdminCommands.xml` | `<admin command="oly" description="..." accessLevel="0" />` (без префикса, как принято в файле) |
| Ядро | `org\l2jmobius\gameserver\handler\AdminCommandHandler.java` | в `onCommand` гейт `player.isGM()` заменён на `player.isGM() \|\| AdminData.getInstance().hasAccess(command, player.getAccessLevel())` |

## Почему был нужен патч ядра (важно для будущих команд)

- `//команда`, набранная в чате, приходит пакетом **`SendBypassBuildCmd`**: клиент отрезает
  `//`, ядро вызывает `AdminCommandHandler.onCommand(player, "admin_" + текст, true)`.
  Прежний метод начинался с `if (player.isGM())` — обычные игроки отсекались молча.
- Патч пускает не-ГМа дальше только если `AdminData.hasAccess(command, accessLevel)` — то есть
  команда **явно** объявлена в `AdminCommands.xml` с уровнем, доступным игроку (точно или по
  цепочке childAccess в `AccessLevels.xml`). Все 469 существующих команд имеют accessLevel
  10–100, поэтому для них патч — no-op; единственная «игроковая» команда — `oly` (0).
- Цепочки childAccess: 10→0, 20→10→0, 30→20→10→0 и т.д. — все ГМ-уровни (10…100) тоже проходят
  к accessLevel-0 командам; забаненный (-1) не проходит (`hasChildAccess(0)` = false).
- `RequestBypassToServer` (bypass `admin_*` из HTML-панелей) здесь ни при чём: он используется
  ГМ-меню и по-прежнему требует кэш HTML-действий; геймплейно `//oly` идёт мимо него.

## Механика загрузки (справка)

- Скрипты `handlers\*` исключены в `Scripts.xml`, но `GameServer` явно исполняет
  `ScriptEngine.executeScript(ScriptEngine.MASTER_HANDLER_FILE)` — регистрация новых хендлеров
  идёт через `MasterHandler.main()`; сами файлы классов компилируются на старте
  (sourcepath `data/scripts`).
- voiced-команды разбираются только в `ChatGeneral` (`.` и теперь `/`, `//` c точным совпадением
  `oly`); другие каналы (Shout/Trade/Whisper) команд не разбирают — как и раньше для `.`.

## Сборка/верификация и откат

- Ядро: `javac` (LibericaJDK 25) против `libs\GameServer.jar` — без ошибок; класс подменён
  в JAR (`jar uf`); декомпиляция из обновлённого JAR подтверждает правку (`_oly_reg\_verify_out`).
- Скрипты: тест-компиляция всего `data\scripts\handlers` (420 файлов) против JAR — без ошибок
  (`_oly_reg\scriptbuild`).
- Бэкап/откат: `_oly_reg\GameServer.jar.pre-olyreg` → вернуть в `libs\GameServer.jar` при
  остановленном GS; исходник патча — `_oly_reg\src\...`. Подробности — `_oly_reg\README.md`.

## Границы/замечания

- Команда работает **только на бесклассовые** бои (классовые — по-прежнему только через
  менеджера). Расширить просто: `Oly.dispatch` → `Olympiad.registerNoble(player, true)`.
- Во время наблюдения за боем (`inObserverMode`) регистрация отсекается скриптом
  (у NPC-менеджера такой ситуации не бывает).
- Дуалбоксы: ограничение «один участник олимпиады на IP» (`DualboxCheck.ini`) применяется
  штатной системой при подборе матча, команда его не обходит.
