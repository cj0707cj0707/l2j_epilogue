# 52. GM: вход без invul/invisible и очистка после просмотра Олимпиады

База: `99a1318b145f11b587f6a6acc43106d0fd18e78c`. Изменение относится к администратору/наблюдателю, не к боевой ротации ботов.

## Причины в поставленном коде

1. В `game/config/General.ini` уже было `GMStartupInvulnerable=False`, но оставались `GMStartupBuilderHide=True` и `GMStartupInvisible=True`. В настоящем `EnterWorld.runImpl` builder-hide вызывает `Player.setHiding(true)` и обходит отдельные startup-переключатели. `setHiding` устанавливает сразу silence, invul и invisible. Поэтому один False у Invulnerable не выключал неуязвимость при входе GM.
2. `Player.enterOlympiadObserverMode(Location,int,boolean)` штатно включает `setInvul(true)` и `setInvisible(true)`. В `leaveOlympiadObserverMode()` их выключение было внутри `if (!isGM())`. Обычный зритель очищался, а GM сохранял защиту и невидимость после возврата. Клиентский `RequestOlympiadObserverEnd` вызывает именно этот метод.

Это подтверждено конфигом и байткодом поставленного `libs/GameServer.jar`; живые логи/сессия пользователя не читались.

## Новое поведение

### Вход администратора

Все три настройки отключены:

```ini
GMStartupBuilderHide = False
GMStartupInvulnerable = False
GMStartupInvisible = False
```

Автоматический builder-hide больше не включает также silence как побочный эффект. Остальные параметры General.ini (геройская аура, права, списки GM, навыки и т.д.) не менялись. Ручные `//hide`, `//invul`, `//invisible` не запрещаются и не подавляются постоянным таймером.

### Выход из просмотра Олимпиады

Точечный core-патч меняет единственную GM-проверку в `Player.leaveOlympiadObserverMode`: обе существующие операции `setInvisible(false)` / `setInvul(false)` выполняются для GM точно так же, как для остальных зрителей. Предыдущее ручное включение этих флагов также не сохраняется при выходе из просмотра — это принудительная очистка по запросу, не восстановление старого GM-состояния.

- Во время просмотра персонаж **остаётся** скрытым и неуязвимым. Метод входа в наблюдение не менялся.
- Штатные снятие из spectator-списка, gameId=-1, observer=false, цель, ExOlympiadMode(0), instance0, возврат на сохранённые координаты, очистка lastLoc, AI idle и broadcastUserInfo сохранены. Снятие флагов происходит в прежнем месте перед итоговым broadcast.
- Изменён только олимпийский выход. Обычный `leaveObserverMode`, `setHiding`, `EnterWorld`, права GM и боевые методы остаются прежними. Вызов олимпийского выхода без назначенного gameId по-прежнему no-op: чужое игровое/ручное состояние вне просмотра не сбрасывается.
- Здесь invul/invisible — нативные административные/observer-флаги, не произвольные бафы навыков. Патч не снимает Celestial/Vengeance/Hide и другие реальные skill-effects и не отключает штатную кратковременную teleport/spawn-защиту. Во время самого телепорта `isInvul()` может быть true независимо от уже снятого observer-флага; это не залипший GM invul.
- Нет новых runtime-хелперов, событийных скриптов или фонового обхода игроков. Не затрагивает тренировочные защитные фазы, предбоевое снятие invul участника Олимпиады или уведомление о сопернике из51.

## Карта файлов и цепочка патчей

| Файл | Назначение |
|---|---|
| `game/config/General.ini` | Отключены GMStartupBuilderHide и GMStartupInvisible; GMStartupInvulnerable остаётся False. Комментарий объясняет совмещённое действие builder-hide. |
| `game/core-patches/observer-cleanup/InstallObserverCleanup.java` | Новый Java25 source-launcher: проверка исходного Player и предыдущей цепочки, замена только GM guard в одном методе, атомарная установка и проверенный backup; новых runtime-классов в JAR нет. |
| `game/core-patches/debuff-no-refresh/InstallDebuffGuard.java` | При marker четвёртого патча делегирует совместную проверку новому установщику, не пытаясь проверить изменённый payload как старый одиночный core. Прежнее поведение без нового marker сохранено. |
| `game/core-patches/arena-boundary/InstallArenaBoundary.java` | Такая же совместимая делегация проверки четвёртого патча; boundary-логика не меняется. |
| `game/core-patches/olympiad-diversity/InstallOlympiadDiversity.java` | Делегация совместной проверки после observer-cleanup; matchmaking-helper не меняется. |
| `game/InstallDebuffGuard.cmd` | Windows: четвёртая стадия после diversity, общий `log/debuff-guard-install.log`, ненулевой exit блокирует запуск. Оба VBS уже вызывают этот CMD и не изменены. |
| `game/GameServerTask.sh` | Linux: такой же порядок перед циклом запуска, fail-closed при ошибке. `GameServer.sh` не менялся. |
| `tests/fakeplayersai/ObserverCleanupNativeTest.java` | Настоящие native Player entry/exit/флаги/AccessLevel и удаление из настоящего stadium-списка; минимальные headless UI/teleport/target-фасады вместо клиента и мира. |
| `tests/fakeplayersai/observer_cleanup_tests.py` | Патчит только временную копию JAR; воспроизводит старую GM-ветку и проверяет новый результат, неизменность прочих методов, backup/idempotence/tamper/source checks и компиляцию датапака. |
| `structure/03-game-config.md`, `08-admin-and-mods.md`, `44-olympiad-random-registration-and-opponent-history.md`, `README.md`, этот файл | Актуальные настройки GM, назначение файлов и согласованная установка четвёртого патча. |

Порядок автоматической установки: **debuff-no-refresh → arena-boundary → olympiad-diversity → observer-cleanup**. Новый marker: `META-INF/l2j-observer-cleanup-v1.properties`. Он фиксирует полный payload (включая прежние markers), hashes Player/источника установщика; при повторной проверке также сверяются исходники всех трёх прежних runtime-хелперов. Все четыре входа установки после наличия нового marker проверяют единый итоговый core.

Исходный JAR до всех guards: SHA256 `8f27196ca9d74b82bbe9ad0d12d70da0196fa4e0d1bf23595267b724c79a23dd`. Дополнительно закреплён исходный Player.class: `f0022b223ece8287743efb89867559c39f9f4779d2af20b0b22d721fd2f1afc6`. Неизвестный/повреждённый core или несовпадение источников отвергаются до замены. Старые markers не переписываются для сокрытия несовпадения; добавляется новый общий marker. Резерв: `libs/GameServer.jar.pre-observer-cleanup-<первые12 символов SHA256 входного JAR>`.

## Проверки и границы подтверждения

`python3 tests/fakeplayersai/observer_cleanup_tests.py`: **3891 native-проверка на исходном ядре (воспроизведение GM-бага) и3891 на исправленном**, плюс проверки установки/байткода/компиляции ниже. Проверяет:

- Все22 стадиона, access levels0/30/70/100, четыре начальные комбинации invul/invisible: native вход сохраняет защиту; выход очищает оба флага для GM и не-GM. На исходном JAR тот же тест воспроизводит сохранение флагов именно у GM.
- Настоящее удаление из spectator-списка, сброс observer/gameId/lastLoc, возврат и UI-пакеты, переход между аренами с исходной точкой возврата, ручное управление вне наблюдения, сохранение независимых teleport/skill защит.
- Настоящий `GeneralConfig.load` читает три False; старый `setHiding(true)` воспроизводит совместное включение обоих флагов. Полный сетевой EnterWorld не запускается: его связь с настройками дополнительно проверяется по настоящему байткоду.
- В JAR изменён только Player.class и добавлен marker; disassembly всех прочих методов Player полностью совпадает, включая вход в оба вида наблюдения и обычный observer exit. Все custom/effect-скрипты компилируются против четырёхпатчевого core.
- Backup байт-в-байт, повторная установка через все4 entrypoints не меняет JAR, неизвестная база отвергается, повреждение Player/каждого прежнего helper/нового marker и изменение исходников блокируют установку без записи в core. Репозиторный JAR не меняется.

Связанные регрессии прошли: `olympiad_diversity_tests.py` —3545 history/queue проверок и26276 Trance/orbit/boundary/mana; `cardinal_recovery_opponent_notice_tests.py` —691 recovery/notice проверка. Прежние три core guards и скрипты Олимпиады совместимы.

Проверки **offline**, без живой БД/сетевого клиента. Native observer-логика и setters исполняются, но реальное движение/сеть заменены узкими записывающими фасадами. Windows CMD/VBS на Linux не исполняются; проверены подключения/порядок и общие Java-установщики. После установки нужен live smoke.

## Установка при остановленном GameServer

1. Сохранить своё General.ini, launchers/core-patches и текущий `libs/GameServer.jar`.
2. Обновить **согласованную папку `game/core-patches/` целиком**, включая все3 обновлённых старых установщика и новый `observer-cleanup`, а также CMD и Task.sh. Не оставлять старый verifier рядом с четырёхпатчевым JAR — он отклонит новый payload.
3. В своём General.ini выставить только три указанных GMStartup-параметра в False; не затирать остальные production-настройки целым репозиторным ini.
4. Запустить обычным VBS/SH: цепочка установится/проверится до старта GameServer. Альтернатива вручную, Java25 JDK, из `game`, строго по порядку:

```sh
java --class-path . core-patches/debuff-no-refresh/InstallDebuffGuard.java ../libs/GameServer.jar core-patches/debuff-no-refresh/DebuffCategoryGuard.java
java --class-path . core-patches/arena-boundary/InstallArenaBoundary.java ../libs/GameServer.jar core-patches/arena-boundary/ArenaBoundaryGuard.java
java --class-path . core-patches/olympiad-diversity/InstallOlympiadDiversity.java ../libs/GameServer.jar core-patches/olympiad-diversity/OlympiadDiversity.java
java --class-path . core-patches/observer-cleanup/InstallObserverCleanup.java ../libs/GameServer.jar core-patches
```

Первичная установка изменяет локальный JAR атомарно и создаёт проверенный backup. В Git бинарный JAR **не заменялся**. Ни DB, ни XML навыков, ни registry ботов не меняются. Полный перезапуск обязателен; hot reload скриптов здесь недостаточен. Если запускать `java -jar` напрямую в обход launchers, предварительно обязательно выполнить установку вручную.

### Live smoke

- Войти GM70 и GM100: не назначаются постоянные invul/invisible/builder-hide, персонаж виден обычному игроку. Ручные GM-команды работают.
- Посмотреть Олимпиаду: во время просмотра GM скрыт/защищён. Выйти кнопкой клиента: возвращён в исходное место, observer выключен, invul/invisible флаги сняты; после штатной короткой teleport-защиты персонаж получает урон и виден другому игроку.
- Повторить просмотр, перейти между аренами, выйти; затем повторить обычным персонажем. Проверить отсутствие залипания и сохранение боёв/панели Олимпиады.
- Проверить `game/log/debuff-guard-install.log`: `Installed Olympiad observer cleanup v1` на первой установке либо `Olympiad observer cleanup v1 verified; all four patches intact.` на следующих запусках. При hash/source mismatch не обходить проверку: нужен согласованный комплект и соответствующий backup.

Откат: остановить GameServer; вернуть проверенный `.pre-observer-cleanup-*` JAR (в нём три прежних guards), предыдущие установщики/launchers и при необходимости свой General.ini из сохранённой копии. Нельзя откатывать только один verifier или только marker; не удалять markers вручную и не патчить работающий сервер.


## Уточнение55

Цепочка запуска теперь состоит из пяти патчей: новый olympiad-battle-timer следует за observer-cleanup. Старые installer делегируют проверку нового marker; обновлённый observer installer принимает verified legacy52 при обновлении. Поведение входа GM/выхода из наблюдения не менялось. См. [55](55-dominator-melee-control-shadow-step-and-90s-olympiad.md).
