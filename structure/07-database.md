# 07. База данных (`db_installer\`, MySQL)

## Общие сведения

| Параметр | Значение |
|---|---|
| СУБД | MySQL / MariaDB |
| Схема | **`l2jmobiusepilogue`** |
| Драйвер | `com.mysql.cj.jdbc.Driver` (mysql-connector-j 9.5.0) |
| Пул соединений | HikariCP 7.0.2 |
| Настройка GS | `game\config\Database.ini` (Login=root, пароль пустой, до 100 соединений) |
| Настройка LS | `login\config\Database.ini` (до 5 соединений) |
| URL | `jdbc:mysql://localhost/l2jmobiusepilogue?...` (utf-8, autoReconnect) |

## Структура `db_installer\`

| Элемент | Назначение |
|---|---|
| `sql\game\` | 116 SQL-файлов создания таблиц GameServer |
| `sql\login\` | 4 SQL-файла таблиц LoginServer |
| `DatabaseInstaller.jar` / `.vbs` / `.sh` | Интерактивный установщик (GUI/консоль): создаёт БД, накатывает таблицы; спрашивает про автоматизации |
| `config\Interface.ini` | GUI установщика (EnableGUI, DarkTheme) |
| `dumps\` | Дампы БД: `l2jmobiusepilogue_dump_05.09.2026_19-15.sql`, `..._19-21.sql` (структурные дампы на момент установки) |

## Таблицы LoginServer (`sql\login\`)

`accounts`, `accounts_ipauth`, `account_data`, `gameservers` — описание в [04-login-server.md](04-login-server.md).

## Таблицы GameServer (`sql\game\`) — по группам

### Аккаунты и персонажи
| Таблица | Содержимое |
|---|---|
| `characters` | Персонажи: имя, уровень, HP/MP/CP, координаты, exp/sp, karma, pvp/pk, внешность, подклассы и т.д. |
| `character_subclasses` | Подклассы |
| `character_skills`, `character_skills_save` | Изученные скиллы + сохранённые баффы/переключатели |
| `character_quests` | Прогресс квестов (имя квеста, var, value) |
| `character_shortcuts`, `character_macroses` | Панельки и макросы |
| `character_friends`, `character_contacts` | Друзья, контакты |
| `character_hennas` | Символы (henna) |
| `character_recipebook`, `character_recipeshoplist` | Рецепты |
| `character_instance_time` | Кулдауны инстансов |
| `character_item_reuse_save` | Кулдауны предметов |
| `character_tpbookmark` | Закладки телепорта |
| `character_variables` | Произвольные переменные персонажа (используются модами) |
| `character_premium_items` | Предметы премиума |
| `character_raid_points` | Очки рейд-боссов |
| `character_summons`, `character_summon_skills_save`, `character_pet_skills_save` | Питомцы/саммоны и их скиллы |
| `character_offline_play`, `character_offline_play_group`, `character_offline_trade`, `character_offline_trade_items` | Офлайн-режимы (торговля/игра) — моды OfflinePlay/OfflineTrade |
| `character_transmogs` | Трансмогрификация (мод Transmog) |
| `account_gsdata`, `account_premium` | Данные аккаунта на GS, премиум |

### Кланы, замки, крепости
| Таблица | Содержимое |
|---|---|
| `clan_data`, `clan_skills`, `clan_privs`, `clan_subpledges`, `clan_wars`, `clan_notices` | Кланы целиком |
| `crests` | Изображения клановых гербов |
| `castle`, `castle_doorupgrade`, `castle_functions`, `castle_manor_procure`, `castle_manor_production`, `castle_siege_guards`, `castle_trapupgrade` | Замки: владение, функции (буфы/деко), маноры, NPC-охрана, ловушки |
| `siege_clans` | Регистрации на осады |
| `fort`, `fortsiege_clans`, `fort_doorupgrade`, `fort_functions`, `fort_siege_guards`, `fort_spawnlist` | Крепости |
| `territories`, `territory_registrations`, `territory_spawnlist` | Территориальные войны |
| `clanhall`, `clanhall_functions`, `clanhall_siege_attackers`, `clanhall_siege_guards` | Клановые холлы |
| `siegable_clanhall`, `siegable_hall_flagwar_attackers`, `siegable_hall_flagwar_attackers_members` | Захватываемые холлы (Rainbow Springs и др.) |
| `rainbowsprings_attacker_list` | Атакующие Rainbow Springs |

### Предметы и торговля
| Таблица | Содержимое |
|---|---|
| `items` | Все предметы игроков (в инвентаре, на складе, у петов) |
| `itemsonground` | Предметы на земле |
| `item_attributes`, `item_elementals`, `item_variables` | Заточки атрибутами, элементы, переменные предметов |
| `auction`, `auction_bid`, `auction_watch` | Аукционы (продажа холлов/замков) |
| `item_auction`, `item_auction_bid` | Item Auction (редкие предметы) |
| `buylists` | Изменённые списки магазинов (если правились в игре) |
| `merchant_lease` | Аренда торговцев |
| `prime_shop_transactions` | Транзакции Prime Shop |
| `custom_mail` | Почта (мод CustomMailManager) |
| `messages` | Сообщения почты |
| `mods_wedding` | Свадьбы (мод Wedding) |
| `buffer_schemes` | Схемы баффера (мод SchemeBuffer) |

### Мир, боссы, события
| Таблица | Содержимое |
|---|---|
| `grandboss_data`, `grandboss_list` | Состояние гранд-боссов (Antharas, Baium, Valakas...) |
| `raidboss_spawnlist` | Спавны рейд-боссов |
| `z05_raidboss_spawnlist`, `z06_anais`, `z07_frintezza`, `z13_andreas_van_halter` | Специальные спавны (Zaken/Anais/Frintezza/Van Halter) |
| `seven_signs`, `seven_signs_festival`, `seven_signs_status` | Seven Signs |
| `cursed_weapons` | Проклятое оружие (Zariche/Akamanah) |
| `dimensional_rift` | Dimensional Rift |
| `fishing_championship` | Чемпионат рыбалки |
| `lottery` | Лотерея |
| `olympiad_data`, `olympiad_fights`, `olympiad_nobles`, `olympiad_nobles_eom` | Олимпиада |
| `heroes`, `heroes_diary` | Герои |
| `underground_coliseum` | Подземный колизей |
| `random_spawn`, `random_spawn_loc` | Рандомные спавны (эвенты) |
| `airships` | Дирижабли |
| `global_variables` | Глобальные переменные сервера |
| `announcements` | Объявления |
| `mdt_bets`, `mdt_history` | Monster Derby Track |
| `punishments` | Наказания (бан чата/игры) |
| `bot_reported_char_data` | Жалобы на ботов |
| `petitions` данные — `petition_feedback` | Петиции |
| `forums`, `posts`, `topic` | Форум клановых холлов |

## Установка / пересоздание БД

1. Запустить `db_installer\DatabaseInstaller.vbs` (или `.sh`).
2. Указать хост/логин/пароль MySQL — инсталлер создаст схему `l2jmobiusepilogue`.
3. Полная установка: все таблицы из `sql\game` + `sql\login` (опционально модификации/автоматизация).
4. После установки зарегистрировать GameServer (→ [04-login-server.md](04-login-server.md)).

## Резервные копии

- Автоматически: `game\config\Database.ini` → `BackupDatabase = True`, путь `BackupPath = ../backup/` (перед шатдауном/рестартом), `MySqlBinLocation` должен указывать на `mysql\bin` (сейчас `C:/xampp/mysql/bin/` — то есть MySQL установлен в составе XAMPP). Хранение ограничено по дням (`MaximumBackupDays`).
- Вручную: `mysqldump l2jmobiusepilogue > dump.sql` или через GUI (phpMyAdmin из XAMPP).
- Дампы в `dumps\` — снимки структуры после установки (05.09.2026).

## Полезные запросы

```sql
-- Сделать аккаунт ГМом (accessLevel 100+ смотрите в AccessLevels.xml)
UPDATE accounts SET accessLevel = 100 WHERE login = 'имя';
-- Забанить аккаунт
UPDATE accounts SET accessLevel = -100 WHERE login = 'имя';
-- Телепорт персонажа (x,y,z)
UPDATE characters SET x=..., y=..., z=... WHERE char_name = 'имя';
-- Все предметы персонажа
SELECT * FROM items WHERE owner_id = (SELECT charId FROM characters WHERE char_name='имя');
```
