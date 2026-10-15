package org.l2jmobius.gameserver.entity.actor.enums.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;

public enum PlayerClass {
   FIGHTER(0, false, Race.HUMAN, null),
   WARRIOR(1, false, Race.HUMAN, FIGHTER),
   GLADIATOR(2, false, Race.HUMAN, WARRIOR),
   WARLORD(3, false, Race.HUMAN, WARRIOR),
   KNIGHT(4, false, Race.HUMAN, FIGHTER),
   PALADIN(5, false, Race.HUMAN, KNIGHT),
   DARK_AVENGER(6, false, Race.HUMAN, KNIGHT),
   ROGUE(7, false, Race.HUMAN, FIGHTER),
   TREASURE_HUNTER(8, false, Race.HUMAN, ROGUE),
   HAWKEYE(9, false, Race.HUMAN, ROGUE),
   MAGE(10, true, Race.HUMAN, null),
   WIZARD(11, true, Race.HUMAN, MAGE),
   SORCERER(12, true, Race.HUMAN, WIZARD),
   NECROMANCER(13, true, Race.HUMAN, WIZARD),
   WARLOCK(14, true, true, Race.HUMAN, WIZARD),
   CLERIC(15, true, Race.HUMAN, MAGE),
   BISHOP(16, true, Race.HUMAN, CLERIC),
   PROPHET(17, true, Race.HUMAN, CLERIC),
   ELVEN_FIGHTER(18, false, Race.ELF, null),
   ELVEN_KNIGHT(19, false, Race.ELF, ELVEN_FIGHTER),
   TEMPLE_KNIGHT(20, false, Race.ELF, ELVEN_KNIGHT),
   SWORDSINGER(21, false, Race.ELF, ELVEN_KNIGHT),
   ELVEN_SCOUT(22, false, Race.ELF, ELVEN_FIGHTER),
   PLAINS_WALKER(23, false, Race.ELF, ELVEN_SCOUT),
   SILVER_RANGER(24, false, Race.ELF, ELVEN_SCOUT),
   ELVEN_MAGE(25, true, Race.ELF, null),
   ELVEN_WIZARD(26, true, Race.ELF, ELVEN_MAGE),
   SPELLSINGER(27, true, Race.ELF, ELVEN_WIZARD),
   ELEMENTAL_SUMMONER(28, true, true, Race.ELF, ELVEN_WIZARD),
   ORACLE(29, true, Race.ELF, ELVEN_MAGE),
   ELDER(30, true, Race.ELF, ORACLE),
   DARK_FIGHTER(31, false, Race.DARK_ELF, null),
   PALUS_KNIGHT(32, false, Race.DARK_ELF, DARK_FIGHTER),
   SHILLIEN_KNIGHT(33, false, Race.DARK_ELF, PALUS_KNIGHT),
   BLADEDANCER(34, false, Race.DARK_ELF, PALUS_KNIGHT),
   ASSASSIN(35, false, Race.DARK_ELF, DARK_FIGHTER),
   ABYSS_WALKER(36, false, Race.DARK_ELF, ASSASSIN),
   PHANTOM_RANGER(37, false, Race.DARK_ELF, ASSASSIN),
   DARK_MAGE(38, true, Race.DARK_ELF, null),
   DARK_WIZARD(39, true, Race.DARK_ELF, DARK_MAGE),
   SPELLHOWLER(40, true, Race.DARK_ELF, DARK_WIZARD),
   PHANTOM_SUMMONER(41, true, true, Race.DARK_ELF, DARK_WIZARD),
   SHILLIEN_ORACLE(42, true, Race.DARK_ELF, DARK_MAGE),
   SHILLIEN_ELDER(43, true, Race.DARK_ELF, SHILLIEN_ORACLE),
   ORC_FIGHTER(44, false, Race.ORC, null),
   ORC_RAIDER(45, false, Race.ORC, ORC_FIGHTER),
   DESTROYER(46, false, Race.ORC, ORC_RAIDER),
   ORC_MONK(47, false, Race.ORC, ORC_FIGHTER),
   TYRANT(48, false, Race.ORC, ORC_MONK),
   ORC_MAGE(49, true, Race.ORC, null),
   ORC_SHAMAN(50, true, Race.ORC, ORC_MAGE),
   OVERLORD(51, true, Race.ORC, ORC_SHAMAN),
   WARCRYER(52, true, Race.ORC, ORC_SHAMAN),
   DWARVEN_FIGHTER(53, false, Race.DWARF, null),
   SCAVENGER(54, false, Race.DWARF, DWARVEN_FIGHTER),
   BOUNTY_HUNTER(55, false, Race.DWARF, SCAVENGER),
   ARTISAN(56, false, Race.DWARF, DWARVEN_FIGHTER),
   WARSMITH(57, false, Race.DWARF, ARTISAN),
   DUELIST(88, false, Race.HUMAN, GLADIATOR),
   DREADNOUGHT(89, false, Race.HUMAN, WARLORD),
   PHOENIX_KNIGHT(90, false, Race.HUMAN, PALADIN),
   HELL_KNIGHT(91, false, Race.HUMAN, DARK_AVENGER),
   SAGITTARIUS(92, false, Race.HUMAN, HAWKEYE),
   ADVENTURER(93, false, Race.HUMAN, TREASURE_HUNTER),
   ARCHMAGE(94, true, Race.HUMAN, SORCERER),
   SOULTAKER(95, true, Race.HUMAN, NECROMANCER),
   ARCANA_LORD(96, true, true, Race.HUMAN, WARLOCK),
   CARDINAL(97, true, Race.HUMAN, BISHOP),
   HIEROPHANT(98, true, Race.HUMAN, PROPHET),
   EVA_TEMPLAR(99, false, Race.ELF, TEMPLE_KNIGHT),
   SWORD_MUSE(100, false, Race.ELF, SWORDSINGER),
   WIND_RIDER(101, false, Race.ELF, PLAINS_WALKER),
   MOONLIGHT_SENTINEL(102, false, Race.ELF, SILVER_RANGER),
   MYSTIC_MUSE(103, true, Race.ELF, SPELLSINGER),
   ELEMENTAL_MASTER(104, true, true, Race.ELF, ELEMENTAL_SUMMONER),
   EVA_SAINT(105, true, Race.ELF, ELDER),
   SHILLIEN_TEMPLAR(106, false, Race.DARK_ELF, SHILLIEN_KNIGHT),
   SPECTRAL_DANCER(107, false, Race.DARK_ELF, BLADEDANCER),
   GHOST_HUNTER(108, false, Race.DARK_ELF, ABYSS_WALKER),
   GHOST_SENTINEL(109, false, Race.DARK_ELF, PHANTOM_RANGER),
   STORM_SCREAMER(110, true, Race.DARK_ELF, SPELLHOWLER),
   SPECTRAL_MASTER(111, true, true, Race.DARK_ELF, PHANTOM_SUMMONER),
   SHILLIEN_SAINT(112, true, Race.DARK_ELF, SHILLIEN_ELDER),
   TITAN(113, false, Race.ORC, DESTROYER),
   GRAND_KHAVATARI(114, false, Race.ORC, TYRANT),
   DOMINATOR(115, true, Race.ORC, OVERLORD),
   DOOMCRYER(116, true, Race.ORC, WARCRYER),
   FORTUNE_SEEKER(117, false, Race.DWARF, BOUNTY_HUNTER),
   MAESTRO(118, false, Race.DWARF, WARSMITH),
   MALE_SOLDIER(123, false, Race.KAMAEL, null),
   FEMALE_SOLDIER(124, false, Race.KAMAEL, null),
   TROOPER(125, false, Race.KAMAEL, MALE_SOLDIER),
   WARDER(126, false, Race.KAMAEL, FEMALE_SOLDIER),
   BERSERKER(127, false, Race.KAMAEL, TROOPER),
   MALE_SOULBREAKER(128, false, Race.KAMAEL, TROOPER),
   FEMALE_SOULBREAKER(129, false, Race.KAMAEL, WARDER),
   ARBALESTER(130, false, Race.KAMAEL, WARDER),
   DOOMBRINGER(131, false, Race.KAMAEL, BERSERKER),
   MALE_SOUL_HOUND(132, false, Race.KAMAEL, MALE_SOULBREAKER),
   FEMALE_SOUL_HOUND(133, false, Race.KAMAEL, FEMALE_SOULBREAKER),
   TRICKSTER(134, false, Race.KAMAEL, ARBALESTER),
   INSPECTOR(135, false, Race.KAMAEL, WARDER),
   JUDICATOR(136, false, Race.KAMAEL, INSPECTOR);

   private final int _id;
   private final boolean _isMage;
   private final boolean _isSummoner;
   private final Race _race;
   private final PlayerClass _parentClass;
   private final Set<PlayerClass> _nextClasses = new HashSet<>(1);
   private static Map<Integer, PlayerClass> _classMap = new HashMap<>(values().length);

   private PlayerClass(int id, boolean isMage, Race race, PlayerClass parent) {
      this._id = id;
      this._isMage = isMage;
      this._isSummoner = false;
      this._race = race;
      this._parentClass = parent;
      if (this._parentClass != null) {
         this._parentClass.addNextClass(this);
      }
   }

   private PlayerClass(int id, boolean isMage, boolean isSummoner, Race race, PlayerClass parent) {
      this._id = id;
      this._isMage = isMage;
      this._isSummoner = isSummoner;
      this._race = race;
      this._parentClass = parent;
      if (this._parentClass != null) {
         this._parentClass.addNextClass(this);
      }
   }

   public static PlayerClass getPlayerClass(int id) {
      return _classMap.get(id);
   }

   public int getId() {
      return this._id;
   }

   public boolean isMage() {
      return this._isMage;
   }

   public boolean isSummoner() {
      return this._isSummoner;
   }

   public Race getRace() {
      return this._race;
   }

   public boolean childOf(PlayerClass id) {
      if (this._parentClass == null) {
         return false;
      } else {
         return this._parentClass == id ? true : this._parentClass.childOf(id);
      }
   }

   public boolean equalsOrChildOf(PlayerClass playerClass) {
      return this == playerClass || this.childOf(playerClass);
   }

   public int level() {
      return this._parentClass == null ? 0 : 1 + this._parentClass.level();
   }

   public PlayerClass getParent() {
      return this._parentClass;
   }

   public PlayerClass getRootClass() {
      return this._parentClass != null ? this._parentClass.getRootClass() : this;
   }

   public Set<PlayerClass> getNextClasses() {
      return this._nextClasses;
   }

   private void addNextClass(PlayerClass playerClass) {
      this._nextClasses.add(playerClass);
   }

   static {
      for (PlayerClass playerClass : values()) {
         _classMap.put(playerClass.getId(), playerClass);
      }
   }
}
