package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.NpcStatMultipliersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.MathUtil;
import org.l2jmobius.gameserver.util.StatSet;

public class NpcTemplate extends CreatureTemplate {
   private static final Logger LOGGER = Logger.getLogger(NpcTemplate.class.getName());
   private int _id;
   private int _displayId;
   private byte _level;
   private String _type;
   private String _name;
   private boolean _usingServerSideName;
   private String _title;
   private boolean _usingServerSideTitle;
   private StatSet _parameters;
   private Sex _sex;
   private int _chestId;
   private int _rhandId;
   private int _lhandId;
   private int _weaponEnchant;
   private double _exp;
   private double _sp;
   private double _raidPoints;
   private boolean _unique;
   private boolean _attackable;
   private boolean _targetable;
   private boolean _talkable;
   private boolean _isQuestMonster;
   private boolean _undying;
   private boolean _showName;
   private boolean _randomWalk;
   private boolean _randomAnimation;
   private boolean _flying;
   private boolean _fakePlayer;
   private FakePlayerHolder _fakePlayerInfo;
   private boolean _canMove;
   private boolean _noSleepMode;
   private boolean _passableDoor;
   private boolean _hasSummoner;
   private boolean _canBeSown;
   private int _corpseTime;
   private AIType _aiType;
   private int _aggroRange;
   private int _clanHelpRange;
   private boolean _isChaos;
   private boolean _isAggressive;
   private int _soulShot;
   private int _spiritShot;
   private int _soulShotChance;
   private int _spiritShotChance;
   private int _minSkillChance;
   private int _maxSkillChance;
   private int _baseAttackAngle;
   private Map<Integer, Skill> _skills;
   private Map<AISkillScope, List<Skill>> _aiSkillLists;
   private Set<Integer> _clans;
   private Set<Integer> _ignoreClanNpcIds;
   private List<DropGroupHolder> _dropGroups;
   private List<DropHolder> _dropListDeath;
   private List<DropHolder> _dropListSpoil;
   private double _collisionRadiusGrown;
   private double _collisionHeightGrown;
   private final List<PlayerClass> _teachInfo = new ArrayList<>();

   public NpcTemplate(StatSet set) {
      super(set);
   }

   @Override
   public void set(StatSet set) {
      super.set(set);
      this._id = set.getInt("id");
      this._displayId = set.getInt("displayId", this._id);
      this._level = set.getByte("level", (byte)70);
      this._type = set.getString("type", "Folk");
      this._name = set.getString("name", "");
      this._usingServerSideName = set.getBoolean("usingServerSideName", false);
      this._title = set.getString("title", "");
      this._usingServerSideTitle = set.getBoolean("usingServerSideTitle", false);
      this.setRace(set.getEnum("race", Race.class, Race.NONE));
      this._sex = set.getEnum("sex", Sex.class, Sex.ETC);
      this._chestId = set.getInt("chestId", 0);
      this._rhandId = set.getInt("rhandId", 0);
      this._lhandId = set.getInt("lhandId", 0);
      this._weaponEnchant = set.getInt("weaponEnchant", 0);
      this._exp = set.getDouble("exp", 0.0);
      this._sp = set.getDouble("sp", 0.0);
      this._raidPoints = set.getDouble("raidPoints", 0.0);
      this._unique = set.getBoolean("unique", false);
      this._attackable = set.getBoolean("attackable", true);
      this._targetable = set.getBoolean("targetable", true);
      this._talkable = set.getBoolean("talkable", true);
      this._isQuestMonster = this._title.contains("Quest");
      this._undying = set.getBoolean("undying", !this._type.equals("Monster") && !this._type.equals("RaidBoss") && !this._type.equals("GrandBoss"));
      this._showName = set.getBoolean("showName", true);
      this._randomWalk = set.getBoolean("randomWalk", !this._type.equals("Guard"));
      this._randomAnimation = set.getBoolean("randomAnimation", true);
      this._flying = set.getBoolean("flying", false);
      this._fakePlayer = set.getBoolean("fakePlayer", false);
      if (this._fakePlayer) {
         this._fakePlayerInfo = new FakePlayerHolder(set);
         if (CharInfoTable.getInstance().getIdByName(this._name) > 0) {
            LOGGER.info(
               this.getClass().getSimpleName() + ": Fake player id [" + this._id + "] conflict. A real player with name [" + this._name + "] already exists."
            );
         }
      }

      this._canMove = set.getDouble("baseWalkSpd", 1.0) <= 0.1 || set.getBoolean("canMove", true);
      this._noSleepMode = set.getBoolean("noSleepMode", false);
      this._passableDoor = set.getBoolean("passableDoor", false);
      this._hasSummoner = set.getBoolean("hasSummoner", false);
      this._canBeSown = set.getBoolean("canBeSown", false);
      this._corpseTime = set.getInt("corpseTime", NpcConfig.DEFAULT_CORPSE_TIME);
      this._aiType = set.getEnum("aiType", AIType.class, AIType.FIGHTER);
      int aggroValue = set.getInt("aggroRange", 0);
      if (this._type.equalsIgnoreCase("Monster") && aggroValue > NpcConfig.MAX_AGGRO_RANGE) {
         this._aggroRange = NpcConfig.MAX_AGGRO_RANGE;
      } else {
         this._aggroRange = aggroValue;
      }

      this._clanHelpRange = set.getInt("clanHelpRange", 0);
      this._isChaos = set.getBoolean("isChaos", false);
      this._isAggressive = set.getBoolean("isAggressive", true);
      this._soulShot = set.getInt("soulShot", 0);
      this._spiritShot = set.getInt("spiritShot", 0);
      this._soulShotChance = set.getInt("soulShotChance", 0);
      this._spiritShotChance = set.getInt("spiritShotChance", 0);
      this._minSkillChance = set.getInt("minSkillChance", 7);
      this._maxSkillChance = set.getInt("maxSkillChance", 15);
      this._baseAttackAngle = set.getInt("width", 120);
      this._collisionRadiusGrown = set.getDouble("collisionRadiusGrown", 0.0);
      this._collisionHeightGrown = set.getDouble("collisionHeightGrown", 0.0);
      if (NpcStatMultipliersConfig.ENABLE_NPC_STAT_MULTIPLIERS) {
         String var3 = this._type;
         switch (var3) {
            case "Monster":
               this._baseHpMax = (float)(this._baseHpMax * NpcStatMultipliersConfig.MONSTER_HP_MULTIPLIER);
               this._baseMpMax = (float)(this._baseMpMax * NpcStatMultipliersConfig.MONSTER_MP_MULTIPLIER);
               this._basePAtk = (int)(this._basePAtk * NpcStatMultipliersConfig.MONSTER_PATK_MULTIPLIER);
               this._baseMAtk = (int)(this._baseMAtk * NpcStatMultipliersConfig.MONSTER_MATK_MULTIPLIER);
               this._basePDef = (int)(this._basePDef * NpcStatMultipliersConfig.MONSTER_PDEF_MULTIPLIER);
               this._baseMDef = (int)(this._baseMDef * NpcStatMultipliersConfig.MONSTER_MDEF_MULTIPLIER);
               this._aggroRange = (int)(this._aggroRange * NpcStatMultipliersConfig.MONSTER_AGRRO_RANGE_MULTIPLIER);
               this._clanHelpRange = (int)(this._clanHelpRange * NpcStatMultipliersConfig.MONSTER_CLAN_HELP_RANGE_MULTIPLIER);
               break;
            case "RaidBoss":
            case "GrandBoss":
               this._baseHpMax = (float)(this._baseHpMax * NpcStatMultipliersConfig.RAIDBOSS_HP_MULTIPLIER);
               this._baseMpMax = (float)(this._baseMpMax * NpcStatMultipliersConfig.RAIDBOSS_MP_MULTIPLIER);
               this._basePAtk = (int)(this._basePAtk * NpcStatMultipliersConfig.RAIDBOSS_PATK_MULTIPLIER);
               this._baseMAtk = (int)(this._baseMAtk * NpcStatMultipliersConfig.RAIDBOSS_MATK_MULTIPLIER);
               this._basePDef = (int)(this._basePDef * NpcStatMultipliersConfig.RAIDBOSS_PDEF_MULTIPLIER);
               this._baseMDef = (int)(this._baseMDef * NpcStatMultipliersConfig.RAIDBOSS_MDEF_MULTIPLIER);
               this._aggroRange = (int)(this._aggroRange * NpcStatMultipliersConfig.RAIDBOSS_AGRRO_RANGE_MULTIPLIER);
               this._clanHelpRange = (int)(this._clanHelpRange * NpcStatMultipliersConfig.RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER);
               break;
            case "Guard":
               this._baseHpMax = (float)(this._baseHpMax * NpcStatMultipliersConfig.GUARD_HP_MULTIPLIER);
               this._baseMpMax = (float)(this._baseMpMax * NpcStatMultipliersConfig.GUARD_MP_MULTIPLIER);
               this._basePAtk = (int)(this._basePAtk * NpcStatMultipliersConfig.GUARD_PATK_MULTIPLIER);
               this._baseMAtk = (int)(this._baseMAtk * NpcStatMultipliersConfig.GUARD_MATK_MULTIPLIER);
               this._basePDef = (int)(this._basePDef * NpcStatMultipliersConfig.GUARD_PDEF_MULTIPLIER);
               this._baseMDef = (int)(this._baseMDef * NpcStatMultipliersConfig.GUARD_MDEF_MULTIPLIER);
               this._aggroRange = (int)(this._aggroRange * NpcStatMultipliersConfig.GUARD_AGRRO_RANGE_MULTIPLIER);
               this._clanHelpRange = (int)(this._clanHelpRange * NpcStatMultipliersConfig.GUARD_CLAN_HELP_RANGE_MULTIPLIER);
               break;
            case "Defender":
               this._baseHpMax = (float)(this._baseHpMax * NpcStatMultipliersConfig.DEFENDER_HP_MULTIPLIER);
               this._baseMpMax = (float)(this._baseMpMax * NpcStatMultipliersConfig.DEFENDER_MP_MULTIPLIER);
               this._basePAtk = (int)(this._basePAtk * NpcStatMultipliersConfig.DEFENDER_PATK_MULTIPLIER);
               this._baseMAtk = (int)(this._baseMAtk * NpcStatMultipliersConfig.DEFENDER_MATK_MULTIPLIER);
               this._basePDef = (int)(this._basePDef * NpcStatMultipliersConfig.DEFENDER_PDEF_MULTIPLIER);
               this._baseMDef = (int)(this._baseMDef * NpcStatMultipliersConfig.DEFENDER_MDEF_MULTIPLIER);
               this._aggroRange = (int)(this._aggroRange * NpcStatMultipliersConfig.DEFENDER_AGRRO_RANGE_MULTIPLIER);
               this._clanHelpRange = (int)(this._clanHelpRange * NpcStatMultipliersConfig.DEFENDER_CLAN_HELP_RANGE_MULTIPLIER);
         }
      }
   }

   public int getId() {
      return this._id;
   }

   public int getDisplayId() {
      return this._displayId;
   }

   public byte getLevel() {
      return this._level;
   }

   public String getType() {
      return this._type;
   }

   public boolean isType(String type) {
      return this._type.equalsIgnoreCase(type);
   }

   public String getName() {
      return this._name;
   }

   public boolean isUsingServerSideName() {
      return this._usingServerSideName;
   }

   public String getTitle() {
      return this._title;
   }

   public boolean isUsingServerSideTitle() {
      return this._usingServerSideTitle;
   }

   public StatSet getParameters() {
      return this._parameters;
   }

   public void setParameters(StatSet set) {
      this._parameters = set;
   }

   public Sex getSex() {
      return this._sex;
   }

   public int getChestId() {
      return this._chestId;
   }

   public int getRHandId() {
      return this._rhandId;
   }

   public int getLHandId() {
      return this._lhandId;
   }

   public int getWeaponEnchant() {
      return this._weaponEnchant;
   }

   public double getExp() {
      return this._exp;
   }

   public double getSP() {
      return this._sp;
   }

   public double getRaidPoints() {
      return this._raidPoints;
   }

   public boolean isUnique() {
      return this._unique;
   }

   public boolean isAttackable() {
      return this._attackable;
   }

   public boolean isTargetable() {
      return this._targetable;
   }

   public boolean isTalkable() {
      return this._talkable;
   }

   public boolean isQuestMonster() {
      return this._isQuestMonster;
   }

   public boolean isUndying() {
      return this._undying;
   }

   public boolean isShowName() {
      return this._showName;
   }

   public boolean isRandomWalkEnabled() {
      return this._randomWalk && this.getParameters().getInt("MoveArounding", 1) != 0;
   }

   public boolean isRandomAnimationEnabled() {
      return this._randomAnimation;
   }

   public boolean isFlying() {
      return this._flying;
   }

   public boolean isFakePlayer() {
      return this._fakePlayer;
   }

   public FakePlayerHolder getFakePlayerInfo() {
      return this._fakePlayerInfo;
   }

   public boolean canMove() {
      return this._canMove;
   }

   public boolean isNoSleepMode() {
      return this._noSleepMode;
   }

   public boolean isPassableDoor() {
      return this._passableDoor;
   }

   public boolean hasSummoner() {
      return this._hasSummoner;
   }

   public boolean canBeSown() {
      return this._canBeSown;
   }

   public int getCorpseTime() {
      return this._corpseTime;
   }

   public AIType getAIType() {
      return this._aiType;
   }

   public int getAggroRange() {
      return this._aggroRange;
   }

   public int getClanHelpRange() {
      return this._clanHelpRange;
   }

   public boolean isChaos() {
      return this._isChaos;
   }

   public boolean isAggressive() {
      return this._isAggressive;
   }

   public int getSoulShot() {
      return this._soulShot;
   }

   public int getSpiritShot() {
      return this._spiritShot;
   }

   public int getSoulShotChance() {
      return this._soulShotChance;
   }

   public int getSpiritShotChance() {
      return this._spiritShotChance;
   }

   public int getMinSkillChance() {
      return this._minSkillChance;
   }

   public int getMaxSkillChance() {
      return this._maxSkillChance;
   }

   public int getBaseAttackAngle() {
      return this._baseAttackAngle;
   }

   @Override
   public Map<Integer, Skill> getSkills() {
      return this._skills;
   }

   public void setSkills(Map<Integer, Skill> skills) {
      this._skills = skills != null ? Collections.unmodifiableMap(skills) : Collections.emptyMap();
   }

   public List<Skill> getAISkills(AISkillScope aiSkillScope) {
      return this._aiSkillLists.getOrDefault(aiSkillScope, Collections.emptyList());
   }

   public void setAISkillLists(Map<AISkillScope, List<Skill>> aiSkillLists) {
      this._aiSkillLists = aiSkillLists != null ? Collections.unmodifiableMap(aiSkillLists) : Collections.emptyMap();
   }

   public Set<Integer> getClans() {
      return this._clans;
   }

   public void setClans(Set<Integer> clans) {
      this._clans = clans != null ? Collections.unmodifiableSet(clans) : null;
   }

   public boolean isClan(String clanName, String... clanNames) {
      Set<Integer> clans = this._clans;
      if (clans == null) {
         return false;
      } else {
         int clanId = NpcData.getInstance().getGenericClanId();
         if (clans.contains(clanId)) {
            return true;
         } else {
            clanId = NpcData.getInstance().getClanId(clanName);
            if (clans.contains(clanId)) {
               return true;
            } else {
               for (String name : clanNames) {
                  clanId = NpcData.getInstance().getClanId(name);
                  if (clans.contains(clanId)) {
                     return true;
                  }
               }

               return false;
            }
         }
      }
   }

   public boolean isClan(Set<Integer> clans) {
      Set<Integer> clanSet = this._clans;
      if (clanSet != null && clans != null) {
         int clanId = NpcData.getInstance().getGenericClanId();
         if (clanSet.contains(clanId)) {
            return true;
         } else {
            for (Integer id : clans) {
               if (clanSet.contains(id)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public Set<Integer> getIgnoreClanNpcIds() {
      return this._ignoreClanNpcIds;
   }

   public boolean hasIgnoreClanNpcIds() {
      return this._ignoreClanNpcIds != null;
   }

   public void setIgnoreClanNpcIds(Set<Integer> ignoreClanNpcIds) {
      this._ignoreClanNpcIds = ignoreClanNpcIds != null ? Collections.unmodifiableSet(ignoreClanNpcIds) : null;
   }

   public void removeDropGroups() {
      this._dropGroups = null;
   }

   public void removeDrops() {
      this._dropListDeath = null;
      this._dropListSpoil = null;
   }

   public void setDropGroups(List<DropGroupHolder> groups) {
      this._dropGroups = groups;
   }

   public void addDrop(DropHolder dropHolder) {
      if (this._dropListDeath == null) {
         this._dropListDeath = new ArrayList<>(1);
      }

      this._dropListDeath.add(dropHolder);
   }

   public void addSpoil(DropHolder dropHolder) {
      if (this._dropListSpoil == null) {
         this._dropListSpoil = new ArrayList<>(1);
      }

      this._dropListSpoil.add(dropHolder);
   }

   public List<DropGroupHolder> getDropGroups() {
      return this._dropGroups;
   }

   public List<DropHolder> getDropList() {
      return this._dropListDeath;
   }

   public List<DropHolder> getSpoilList() {
      return this._dropListSpoil;
   }

   public List<ItemHolder> calculateDrops(DropType dropType, Creature victim, Creature killer) {
      if (dropType == DropType.DROP) {
         List<ItemHolder> groupDrops = null;
         if (this._dropGroups != null) {
            groupDrops = this.calculateGroupDrops(victim, killer);
            if (groupDrops != null && victim.isMonster() && victim.asMonster().isSeeded()) {
               groupDrops.removeIf(i -> i.getId() != 57 && i.getId() != 6361 && i.getId() != 6362 && i.getId() != 6360);
            }
         }

         List<ItemHolder> ungroupedDrops = null;
         if (this._dropListDeath != null) {
            ungroupedDrops = this.calculateUngroupedDrops(dropType, victim, killer);
            if (ungroupedDrops != null && victim.isMonster() && victim.asMonster().isSeeded()) {
               ungroupedDrops.removeIf(i -> i.getId() != 57 && i.getId() != 6361 && i.getId() != 6362 && i.getId() != 6360);
            }
         }

         if (groupDrops != null && ungroupedDrops != null) {
            groupDrops.addAll(ungroupedDrops);
            ungroupedDrops.clear();
            return groupDrops;
         }

         if (groupDrops != null) {
            return groupDrops;
         }

         if (ungroupedDrops != null) {
            return ungroupedDrops;
         }
      } else if (dropType == DropType.SPOIL && this._dropListSpoil != null) {
         return this.calculateUngroupedDrops(dropType, victim, killer);
      }

      return null;
   }

   private List<ItemHolder> calculateGroupDrops(Creature victim, Creature killer) {
      int levelDifference = victim.getLevel() - killer.getLevel();
      double levelGapChanceToDropAdena = MathUtil.scaleToRange(
         (double)levelDifference,
         (double)(-RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE),
         (double)(-RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE),
         RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE,
         100.0
      );
      double levelGapChanceToDrop = MathUtil.scaleToRange(
         (double)levelDifference,
         (double)(-RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE),
         (double)(-RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE),
         RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE,
         100.0
      );
      List<ItemHolder> calculatedDrops = null;
      int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
      if (dropOccurrenceCounter > 0) {
         Player player = killer.asPlayer();
         List<ItemHolder> randomDrops = null;
         ItemHolder cachedItem = null;

         for (DropGroupHolder group : this._dropGroups) {
            double totalChance = 0.0;

            for (DropHolder dropItem : group.getDropList()) {
               int itemId = dropItem.getItemId();
               ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
               boolean champion = victim.isChampion();
               double rateChance = 1.0;
               if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null) {
                  rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId).floatValue();
                  if (champion && itemId == 57) {
                     rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
                  }

                  if (itemId == 57 && rateChance > 100.0) {
                     rateChance = 100.0;
                  }
               } else if (item.hasExImmediateEffect()) {
                  rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
               } else if (victim.isRaid()) {
                  rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
               } else {
                  rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1.0F);
               }

               if (player != null) {
                  if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus()) {
                     if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null) {
                        rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId).floatValue();
                     } else if (!item.hasExImmediateEffect() && !victim.isRaid()) {
                        rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
                     }
                  }

                  rateChance *= player.getStat().getBonusDropRateMultiplier();
               }

               if (rateChance == 1.0) {
                  totalChance += dropItem.getChance();
               } else {
                  totalChance = dropItem.getChance();
               }

               double groupItemChance = totalChance * (group.getChance() / 100.0) * rateChance;
               if (dropOccurrenceCounter == 0 && groupItemChance < 100.0 && randomDrops != null && calculatedDrops != null) {
                  if (rateChance == 1.0 && !randomDrops.isEmpty()) {
                     cachedItem = randomDrops.remove(0);
                     calculatedDrops.remove(cachedItem);
                  }

                  dropOccurrenceCounter = 1;
               }

               if (!(Rnd.nextDouble() * 100.0 > (dropItem.getItemId() == 57 ? levelGapChanceToDropAdena : levelGapChanceToDrop))) {
                  ItemHolder drop = this.calculateGroupDrop(group, dropItem, victim, killer, groupItemChance);
                  if (drop != null) {
                     if (randomDrops == null) {
                        randomDrops = new ArrayList<>(dropOccurrenceCounter);
                     }

                     if (calculatedDrops == null) {
                        calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
                     }

                     Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
                     if (itemChance != null) {
                        if (groupItemChance * itemChance.floatValue() < 100.0) {
                           dropOccurrenceCounter--;
                           if (rateChance == 1.0) {
                              randomDrops.add(drop);
                           }
                        }
                     } else if (groupItemChance < 100.0) {
                        dropOccurrenceCounter--;
                        if (rateChance == 1.0) {
                           randomDrops.add(drop);
                        }
                     }

                     calculatedDrops.add(drop);
                     if (rateChance == 1.0) {
                        break;
                     }
                  }
               }
            }
         }

         if (dropOccurrenceCounter > 0 && cachedItem != null && calculatedDrops != null) {
            calculatedDrops.add(cachedItem);
         }

         if (randomDrops != null) {
            randomDrops.clear();
            randomDrops = null;
         }

         if (victim.isChampion()
            && Rnd.get(100)
               < (
                  victim.getLevel() < killer.getLevel()
                     ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE
                     : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE
               )) {
            if (calculatedDrops == null) {
               calculatedDrops = new ArrayList<>();
            }

            if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS)) {
               calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
            }
         }
      }

      return calculatedDrops;
   }

   private List<ItemHolder> calculateUngroupedDrops(DropType dropType, Creature victim, Creature killer) {
      List<DropHolder> dropList = dropType == DropType.SPOIL ? this._dropListSpoil : this._dropListDeath;
      int levelDifference = victim.getLevel() - killer.getLevel();
      double levelGapChanceToDropAdena = MathUtil.scaleToRange(
         (double)levelDifference,
         (double)(-RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE),
         (double)(-RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE),
         RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE,
         100.0
      );
      double levelGapChanceToDrop = MathUtil.scaleToRange(
         (double)levelDifference,
         (double)(-RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE),
         (double)(-RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE),
         RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE,
         100.0
      );
      int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
      List<ItemHolder> calculatedDrops = null;
      List<ItemHolder> randomDrops = null;
      ItemHolder cachedItem = null;
      if (dropOccurrenceCounter > 0) {
         for (DropHolder dropItem : dropList) {
            if (dropOccurrenceCounter == 0 && dropItem.getChance() < 100.0 && randomDrops != null && calculatedDrops != null) {
               cachedItem = randomDrops.remove(0);
               calculatedDrops.remove(cachedItem);
               dropOccurrenceCounter = 1;
            }

            if (!(Rnd.nextDouble() * 100.0 > (dropItem.getItemId() == 57 ? levelGapChanceToDropAdena : levelGapChanceToDrop))) {
               ItemHolder drop = this.calculateUngroupedDrop(dropItem, victim, killer);
               if (drop != null) {
                  if (randomDrops == null) {
                     randomDrops = new ArrayList<>(dropOccurrenceCounter);
                  }

                  if (calculatedDrops == null) {
                     calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
                  }

                  Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
                  if (itemChance != null) {
                     if (dropItem.getChance() * itemChance.floatValue() < 100.0) {
                        dropOccurrenceCounter--;
                        randomDrops.add(drop);
                     }
                  } else if (dropItem.getChance() < 100.0) {
                     dropOccurrenceCounter--;
                     randomDrops.add(drop);
                  }

                  calculatedDrops.add(drop);
               }
            }
         }
      }

      if (dropOccurrenceCounter > 0 && cachedItem != null && calculatedDrops != null) {
         calculatedDrops.add(cachedItem);
      }

      if (randomDrops != null) {
         randomDrops.clear();
         randomDrops = null;
      }

      if (victim.isChampion()
         && Rnd.get(100)
            < (
               victim.getLevel() < killer.getLevel()
                  ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE
                  : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE
            )) {
         if (calculatedDrops == null) {
            calculatedDrops = new ArrayList<>();
         }

         if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS)) {
            calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
         }
      }

      return calculatedDrops;
   }

   private ItemHolder calculateGroupDrop(DropGroupHolder group, DropHolder dropItem, Creature victim, Creature killer, double chance) {
      int itemId = dropItem.getItemId();
      ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
      boolean champion = victim.isChampion();
      if (Rnd.nextDouble() * 100.0 < chance) {
         double rateAmount = 1.0;
         if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null) {
            rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId).floatValue();
            if (champion && itemId == 57) {
               rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
            }
         } else if (item.hasExImmediateEffect()) {
            rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
         } else if (victim.isRaid()) {
            rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
         } else {
            rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1.0F);
         }

         Player player = killer.asPlayer();
         if (player != null) {
            if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus()) {
               if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null) {
                  rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId).floatValue();
               } else if (!item.hasExImmediateEffect() && !victim.isRaid()) {
                  rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
               }
            }

            PlayerStat stat = player.getStat();
            rateAmount *= stat.getBonusDropAmountMultiplier();
            if (itemId == 57) {
               rateAmount *= stat.getBonusDropAdenaMultiplier();
            }
         }

         return new ItemHolder(itemId, (long)(Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
      } else {
         return null;
      }
   }

   private ItemHolder calculateUngroupedDrop(DropHolder dropItem, Creature victim, Creature killer) {
      switch (dropItem.getDropType()) {
         case DROP:
            int itemId = dropItem.getItemId();
            ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
            boolean champion = victim.isChampion();
            double rateChancex = 1.0;
            if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null) {
               rateChancex *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId).floatValue();
               if (champion && itemId == 57) {
                  rateChancex *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
               }

               if (itemId == 57 && rateChancex > 100.0) {
                  rateChancex = 100.0;
               }
            } else if (item.hasExImmediateEffect()) {
               rateChancex *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
            } else if (victim.isRaid()) {
               rateChancex *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
            } else {
               rateChancex *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1.0F);
            }

            Player playerx = killer.asPlayer();
            if (playerx != null) {
               if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && playerx.hasPremiumStatus()) {
                  if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null) {
                     rateChancex *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId).floatValue();
                  } else if (!item.hasExImmediateEffect() && !victim.isRaid()) {
                     rateChancex *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
                  }
               }

               rateChancex *= playerx.getStat().getBonusDropRateMultiplier();
            }

            if (Rnd.nextDouble() * 100.0 < dropItem.getChance() * rateChancex) {
               double rateAmount = 1.0;
               if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null) {
                  rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId).floatValue();
                  if (champion && itemId == 57) {
                     rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
                  }
               } else if (item.hasExImmediateEffect()) {
                  rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
               } else if (victim.isRaid()) {
                  rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
               } else {
                  rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1.0F);
               }

               if (playerx != null) {
                  if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && playerx.hasPremiumStatus()) {
                     if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null) {
                        rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId).floatValue();
                     } else if (!item.hasExImmediateEffect() && !victim.isRaid()) {
                        rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
                     }
                  }

                  PlayerStat stat = playerx.getStat();
                  rateAmount *= stat.getBonusDropAmountMultiplier();
                  if (itemId == 57) {
                     rateAmount *= stat.getBonusDropAdenaMultiplier();
                  }
               }

               return new ItemHolder(itemId, (long)(Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
            }
            break;
         case SPOIL:
            double rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
            Player player = killer.asPlayer();
            if (player != null) {
               if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus()) {
                  rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
               }

               rateChance *= player.getStat().getBonusSpoilRateMultiplier();
            }

            if (Rnd.nextDouble() * 100.0 < dropItem.getChance() * rateChance) {
               double rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
               if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player != null && player.hasPremiumStatus()) {
                  rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
               }

               return new ItemHolder(dropItem.getItemId(), (long)(Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
            }
      }

      return null;
   }

   public double getCollisionRadiusGrown() {
      return this._collisionRadiusGrown;
   }

   public double getCollisionHeightGrown() {
      return this._collisionHeightGrown;
   }

   public static boolean isAssignableTo(Class<?> subValue, Class<?> clazz) {
      if (clazz.isInterface()) {
         for (Class<?> interface1 : subValue.getInterfaces()) {
            if (clazz.getName().equals(interface1.getName())) {
               return true;
            }
         }
      } else {
         Class<?> sub = subValue;

         do {
            if (sub.getName().equals(clazz.getName())) {
               return true;
            }

            sub = sub.getSuperclass();
         } while (sub != null);
      }

      return false;
   }

   public static boolean isAssignableTo(Object obj, Class<?> clazz) {
      return isAssignableTo(obj.getClass(), clazz);
   }

   public boolean canTeach(PlayerClass playerClass) {
      return playerClass.level() == 3 ? this._teachInfo.contains(playerClass.getParent()) : this._teachInfo.contains(playerClass);
   }

   public List<PlayerClass> getTeachInfo() {
      return this._teachInfo;
   }

   public void addTeachInfo(List<PlayerClass> teachInfo) {
      this._teachInfo.addAll(teachInfo);
   }
}
