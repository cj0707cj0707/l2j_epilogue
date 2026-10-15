# 05. Датапак: `game\data\`

Датапак — всё игровое содержимое в виде текстовых файлов. Порядок загрузки управляется ядром; почти каждый XML валидируется схемой из `data\xsd\` (75 схем) через `xsi:noNamespaceSchemaLocation`.

## Обзор

| Каталог | Файлов | Назначение |
|---|---|---|
| `buylists\` | 749 .xml | Списки товаров обычных торговцев (BuyList). **Имя файла = id списка торговца** (`0000007.xml`). `<item id price count restock_delay/>` |
| `multisell\` | 371 (369 .xml + 2 .txt) | Multisell-обмены: `<item><ingredient id count/><production id count/></item>`. `custom\` — 172 кастомных файла серии `50039xxx` |
| `spawns\` | 192 .xml, 22 папки | Спавны мобов/NPC: `<spawn zone="..."><territory><node x y/><npc id count respawnDelay/>`. Папки по территориям (Aden, Giran, Catacombs, Castles, Gracia, Hellbound, Others = спавны по квадратам карты `16_12.xml` и т.д.) |
| `teleporters\` | 190 .xml, 7 папок | Телепорты NPC: `town\` (файл = npcId, `<teleport type="NORMAL"><location x y z feeCount/>`), `clanhall\`, `doorman\`, `dungeon\`, `fortress\`, `chamberlain\`, `others\` |
| `zones\` | 47 .xml + 1 txt | Зоны мира: `<zone name type="PeaceZone" shape="NPoly" minZ maxZ><node X Y/>`. Типы: peace/pvp/damage/water/swamp/tax/fishing, осадные (`castle_siege`, `fortress_siege`, `clan_hall_siege`), территориальные войны, арены (olympiad_stadium, krateis_cube, underground_coliseum), запреты (no_restart, no_bookmark, no_landing...), `documentation.txt` — описание форм Cuboid/NPoly/Polygon |
| `mapregion\` | 27 .xml | Регионы городов: точки возрождения `<respawnPoint X Y Z>`, banish-точки, привязка к замку/налогам (`<region name="aden_town" town castle="5" locId>`); спец.: `gm_regions.xml`, `monster_race.xml`, `rift_bitween_worlds.xml` |
| `instances\` | 125 .xml, 8 папок | Настройки инстанс-миров (таймеры, спавны, двери): SeedOfDestruction, CrystalCaverns, Kamaloka\ (19), PailakaRuneCastle (30), FortressDungeon (21), RimKamaloka (11), CastleDungeon (9), ChamberOfDelusion (6) |
| `stats\` | 1063 файла | Статы всего: см. раздел ниже |
| `html\` | 4010 файлов, 28 папок | Диалоги NPC (см. раздел ниже) |
| `lang\` | 6 файлов | Локализация — **только каркас греческого** (`el`): SystemMessage/NpcName/SendMessage локализации (пустые) + htm выбора языка |
| `xsd\` | 75 .xsd | XML-схемы валидации всех данных |
| `geodata\` | 1 файл | **ПУСТО!** Только `Readme.txt`. Бинарные геоданные не установлены — сервер работает без гео (или нужны отдельные файлы *.l2j) |
| `scripts\` | 17 391 файлов | Java-скрипты + диалоги квестов → отдельный документ [06-scripts-and-quests.md](06-scripts-and-quests.md) |

## Корневые XML `data\` (26 файлов)

| Файл | Что содержит |
|---|---|
| `CategoryData.xml` | Категории классов (`FIGHTER_GROUP` и др.) — группировка classId |
| `CursedWeapons.xml` | Проклятое оружие: Zariche (8190), Akamanah — dropRate, duration, stageKills |
| `DimensionalRift.xml` | Комнаты рифта: спавны мобов, задержки |
| `Doors.xml` | Все двери мира: id, HP, pDef, координаты узлов, статус по умолчанию |
| `ElementalAttributeData.xml` | Камни/кристаллы атрибутов (fire/water/...) |
| `EnchantItemData.xml` | Свитки заточки: targetGrade, maxEnchant |
| `EnchantItemGroups.xml` | Шансы заточки по уровням (`ARMOR_GROUP`: 0-2 = 100%...) |
| `EnchantItemOptions.xml` | Бонусы заточки: на каждый уровень предмета — option id |
| `EnchantSkillGroups.xml` | Заточка скиллов: стоимость adena/sp, шансы по уровням 76-85 |
| `FakePlayerChatData.xml` | Реплики фейк-игроков (мод FakePlayers) |
| `FenceData.xml` | Декоративные заборы |
| `InstanceNames.xml` | Имена инстансов (id → name) |
| `ItemAuctions.xml` | Аукционы предметов: расписание, лоты, стартовые ставки |
| `KrateisCube.xml` | Арены Kratei's Cube: уровни, менеджеры, двери, награды |
| `LevelUpCrystalData.xml` | Цепочки Life Crystal'ов |
| `MerchantPriceConfig.xml` | Налоговые зоны торговцев (baseTax, привязка к замкам) |
| `PetSkillData.xml` | Какие скиллы доступны петам/саммонам (npcId → skillId) |
| `PrimeShop.xml` | Премиум-магазин (товары за очки) |
| `Recipes.xml` | Все рецепты крафта: ingredients/production, MP, dwarven/common |
| `Routes.xml` | Маршруты движения NPC и фейк-игроков (стили back/cycle/conveyor/random; FPC-маршруты для npcId 80000+) |
| `SchemeBufferSkills.xml` | Скиллы scheme-буфера (цена, категории) — мод SchemeBuffer |
| `Seeds.xml` | Seed/Crop (манор) по замкам |
| `SellBuffData.xml` | Скиллы, разрешённые к продаже (мод SellBuffs) |
| `SkillLearn.xml` | NPC-учителя → классы, которым обучают |
| `StaticObjects.xml` | Статические объекты (карты городов на стенах и т.п.) |
| `UndergroundColiseum.xml` | Арены колизея: уровни, кураторы, награды |

## `stats\` — игровые характеристики

Корень: `enchantHPBonus.xml`, `hennaList.xml`, `hitConditionBonus.xml`, `statBonus.xml`.

| Подкаталог | Файлов | Содержимое |
|---|---|---|
| `items\` | 176 | Статы предметов, файлы-диапазоны id (`00000-00099.xml`); `custom\` — кастомные предметы |
| `npcs\` | 132 | Статы NPC: vitals, attack, defence, race, skillList, агрессия, дроп-группы |
| `skills\` | 83 | Все скиллы: `<skill id levels name>` + условия и эффекты; `custom\` — скиллы модов |
| `players\` | 223 | `classList.xml`, `experience.xml`, `experienceLoss.xml`, `initialEquipment.xml`, `initialShortcuts.xml`, `karmaLoss.xml`; `skillTrees\` — деревья скиллов (noble, pledge, subClass, transform, fishing...); `templates\` — шаблоны классов по ступеням (StartingClass\1stClass\2ndClass\3rdClass) |
| `augmentation\` | 253 | Аугментация: skillmap, шансы (retailchances), `options\` — 250 файлов опций |
| `transformations\` | 111 | Трансформации (статы/скиллы форм) |
| `pets\` | 49 | Статы петов (Wolf, Hatchling, Strider...) |
| `armorsets\` | 15 | Бонусы сетов брони по грейдам (+ PvP/клановые) |
| `cubics\` | 14 | Кубики |
| `fishing\` | 3 | Рыбы, рыбьи монстры, удочки |

## `html\` — диалоги NPC

- Формат: HTML с L2-тегами (`<html><body>...<a action="bypass -h npc_%objectId%_BuyList 7">...`).
- Имя файла обычно **= npcId** (`30006.htm`); многостраничные: `npcId-page.htm`.
- 28 папок по типам NPC: `default` (1141 файл), `admin` (233 — GM-панели), `CommunityBoard` (60+, включая `Custom\buffer`, `Custom\dropsearch`, `Custom\premium`), `merchant`, `teleporter`, `warehouse`, `villagemaster`, `trainer`, `common` (заточка, крафт...), `mods` (страницы модов, например `mods\SellBuffs\BuffMenu.html`), `siege`, `fortress`, `olympiad`, `seven_signs` и др.
- Корневые страницы: `npcdefault.htm`, `npcbusy.htm`, `noquest.htm`, `jail_in.htm`, `clanNotice.htm`.

## Форматы на примерах

**Buylist** (`buylists\0000007.xml`):
```xml
<list xmlns:xsi="..." xsi:noNamespaceSchemaLocation="../xsd/buylist.xsd">
	<item id="5900" count="1" price="10000" restock_delay="180" />
</list>
```

**Multisell**:
```xml
<item>
	<ingredient count="2100" id="57" />   <!-- Adena -->
	<ingredient count="1" id="21" />      <!-- Shirt -->
	<production count="1" id="22" />      <!-- Leather Shirt -->
</item>
```

**Spawn** (`spawns\Aden\BlazingSwamp.xml`):
```xml
<list enabled="true" ...>
	<spawn zone="aden03_2417_01">
		<territory minZ="-2356" maxZ="-1856">
			<node x="143434" y="-25858" /> ...
		</territory>
		<npc id="21114" count="2" respawnDelay="45" /> <!-- Cursed Guardian -->
	</spawn>
</list>
```

## Замечания для эксплуатации

1. **Геоданные не установлены** (`geodata\` пуст) — движение/позиции считаются упрощённо; для полной геометрии нужно распаковать гео-файлы этой хроники в `data\geodata\` (см. `geodata\Readme.txt`).
2. Локализация не настроена (только пустой каркас `el`) — весь контент на английском.
3. Значительная часть контента кастомизирована: `multisell\custom`, `stats\items\custom`, `stats\skills\custom`, SchemeBuffer/SellBuff/FakePlayers данные — при обновлении датапака их легко потерять.
4. После правки почти любого XML нужен рестарт GameServer (часть данных перечитывается админ-командами reload).
5. Кастомные сервисные NPC Гирана добавлены отдельными файлами (тоже легко потерять при обновлении): статы `stats\npcs\custom\Enchanter.xml` (npcId 1004000), `stats\npcs\custom\SkillEnchanter.xml` (npcId 1005000) и `stats\npcs\custom\BlacksmithAug.xml` (npcId 1006000), спавны `spawns\Others\Enchanter.xml`, `spawns\Others\SkillEnchanter.xml` и `spawns\Others\BlacksmithAug.xml`. Скрипты диалогов — в `data\scripts\custom\` (→ [06-scripts-and-quests.md](06-scripts-and-quests.md)). Для Blacksmith Aug также добавлены кастомные augment-опции: `stats\augmentation\options\25000-25099.xml` и `stats\augmentation\options\25100-25199.xml` (id 25000+, свободный диапазон после retail-опций до 24949; в одном файле максимум 100 опций — ограничение xsd).

6. Четвёртый сервисный NPC Гирана — **Adena Giver** (npcId `1007000`, displayId `31960`): `stats\npcs\custom\AdenaGiver.xml` — шаблон NPC; `spawns\Others\AdenaGiver.xml` — спавн `(83185,148613,-3455)`, heading `16384`, на 64 единицы по X от Skill Enchanter. Скрипт `scripts\custom\AdenaGiver\AdenaGiver.java` выдаёт 100 000 000 адены по кнопке в community board; отдельный HTML и изменения БД не нужны. Подробности и установка → [16-giran-adena-giver.md](16-giran-adena-giver.md).
