package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill.1;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

public class Skill {
   private static final Logger LOGGER = Logger.getLogger(Skill.class.getName());
   private final int _id;
   private final int _level;
   private final int _displayId;
   private final int _displayLevel;
   private final String _name;
   private final SkillOperateType _operateType;
   private final int _magic;
   private final TraitType _traitType;
   private final boolean _staticReuse;
   private final int _mpConsume;
   private final int _mpInitialConsume;
   private final int _mpPerChanneling;
   private final int _hpConsume;
   private final int _itemConsumeCount;
   private final int _itemConsumeId;
   private final int _castRange;
   private final int _effectRange;
   private final boolean _isAbnormalInstant;
   private final int _abnormalLevel;
   private final AbnormalType _abnormalType;
   private final int _abnormalTime;
   private AbnormalVisualEffect[] _abnormalVisualEffects = null;
   private AbnormalVisualEffect[] _abnormalVisualEffectsSpecial = null;
   private AbnormalVisualEffect[] _abnormalVisualEffectsEvent = null;
   private final boolean _stayAfterDeath;
   private final boolean _stayOnSubclassChange;
   private final boolean _isRecoveryHerb;
   private int _refId;
   private final int _hitTime;
   private final int _coolTime;
   private final int _reuseHashCode;
   private final int _reuseDelay;
   private final TargetType _targetType;
   private final int _feed;
   private final double _power;
   private final double _pvpPower;
   private final double _pvePower;
   private final int _magicLevel;
   private final int _lvlBonusRate;
   private final int _activateRate;
   private final int _minChance;
   private final int _maxChance;
   private final int _blowChance;
   private final int _affectRange;
   private final int[] _affectLimit = new int[2];
   private final boolean _nextActionIsAttack;
   private final boolean _removedOnAnyActionExceptMove;
   private final boolean _removedOnDamage;
   private final boolean _blockedInOlympiad;
   private final byte _element;
   private final int _elementPower;
   private final BaseStat _basicProperty;
   private final boolean _overhit;
   private final int _minPledgeClass;
   private final int _chargeConsume;
   private final int _soulMaxConsume;
   private final boolean _isHeroSkill;
   private final boolean _isGMSkill;
   private final boolean _isSevenSigns;
   private final int _baseCritRate;
   private final boolean _directHpDmg;
   private final int _effectPoint;
   private List<Condition> _preCondition;
   private List<Condition> _itemPreCondition;
   private List<FuncTemplate> _funcTemplates;
   private final Map<EffectScope, List<AbstractEffect>> _effectLists = new EnumMap<>(EffectScope.class);
   private final FlyType _flyType;
   private final int _flyRadius;
   private final float _flyCourse;
   private final boolean _isDebuff;
   private final String _attribute;
   private final boolean _ignoreShield;
   private final boolean _isSuicideAttack;
   private final boolean _canBeDispeled;
   private final boolean _isClanSkill;
   private final boolean _excludedFromCheck;
   private final boolean _simultaneousCast;
   private final String _icon;
   private volatile Byte[] _effectTypes;
   private final int _channelingSkillId;
   private final int _channelingTickInitialDelay;
   private final int _channelingTickInterval;
   private final boolean _isPvPOnly;

   public Skill(StatSet set) {
      this._id = set.getInt("skill_id");
      this._level = set.getInt("level");
      this._displayId = set.getInt("displayId", this._id);
      this._displayLevel = set.getInt("displayLevel", this._level);
      this._name = set.getString("name", "");
      this._operateType = (SkillOperateType)set.getEnum("operateType", SkillOperateType.class);
      this._magic = set.getInt("isMagic", 0);
      this._traitType = (TraitType)set.getEnum("trait", TraitType.class, TraitType.NONE);
      this._staticReuse = set.getBoolean("staticReuse", false);
      this._mpConsume = set.getInt("mpConsume", 0);
      this._mpInitialConsume = set.getInt("mpInitialConsume", 0);
      this._mpPerChanneling = set.getInt("mpPerChanneling", this._mpConsume);
      this._hpConsume = set.getInt("hpConsume", 0);
      this._itemConsumeCount = set.getInt("itemConsumeCount", 0);
      this._itemConsumeId = set.getInt("itemConsumeId", 0);
      this._castRange = set.getInt("castRange", -1);
      this._effectRange = set.getInt("effectRange", -1);
      this._abnormalLevel = set.getInt("abnormalLevel", 0);
      this._abnormalType = (AbnormalType)set.getEnum("abnormalType", AbnormalType.class, AbnormalType.NONE);
      int abnormalTime = set.getInt("abnormalTime", 0);
      if (PlayerConfig.ENABLE_MODIFY_SKILL_DURATION && PlayerConfig.SKILL_DURATION_LIST.containsKey(this._id) && this._operateType != SkillOperateType.T) {
         if (this._level < 100 || this._level > 140) {
            abnormalTime = (Integer)PlayerConfig.SKILL_DURATION_LIST.get(this._id);
         } else if (this._level >= 100 && this._level < 140) {
            abnormalTime += PlayerConfig.SKILL_DURATION_LIST.get(this._id);
         }
      }

      this._abnormalTime = abnormalTime;
      this._isAbnormalInstant = set.getBoolean("abnormalInstant", false);
      this.parseAbnormalVisualEffect(set.getString("abnormalVisualEffect", null));
      this._attribute = set.getString("attribute", "");
      this._stayAfterDeath = set.getBoolean("stayAfterDeath", false);
      this._stayOnSubclassChange = set.getBoolean("stayOnSubclassChange", true);
      this._hitTime = set.getInt("hitTime", 0);
      this._coolTime = set.getInt("coolTime", 0);
      this._isDebuff = set.getBoolean("isDebuff", false);
      this._isRecoveryHerb = set.getBoolean("isRecoveryHerb", false);
      this._feed = set.getInt("feed", 0);
      this._reuseHashCode = SkillData.getSkillHashCode(this._id, this._level);
      if (PlayerConfig.ENABLE_MODIFY_SKILL_REUSE && PlayerConfig.SKILL_REUSE_LIST.containsKey(this._id)) {
         this._reuseDelay = (Integer)PlayerConfig.SKILL_REUSE_LIST.get(this._id);
      } else {
         this._reuseDelay = set.getInt("reuseDelay", 0);
      }

      this._affectRange = set.getInt("affectRange", 0);
      String affectLimit = set.getString("affectLimit", null);
      if (affectLimit != null) {
         try {
            String[] valuesSplit = affectLimit.split("-");
            this._affectLimit[0] = Integer.parseInt(valuesSplit[0]);
            this._affectLimit[1] = Integer.parseInt(valuesSplit[1]);
         } catch (Exception var5) {
            throw new IllegalArgumentException("SkillId: " + this._id + " invalid affectLimit value: " + affectLimit + ", \"percent-percent\" required");
         }
      }

      this._targetType = (TargetType)set.getEnum("targetType", TargetType.class, TargetType.SELF);
      this._power = set.getFloat("power", 0.0F);
      this._pvpPower = set.getFloat("pvpPower", (float)this._power);
      this._pvePower = set.getFloat("pvePower", (float)this._power);
      this._magicLevel = set.getInt("magicLevel", 0);
      this._lvlBonusRate = set.getInt("lvlBonusRate", 0);
      this._activateRate = set.getInt("activateRate", -1);
      this._minChance = set.getInt("minChance", PlayerConfig.MIN_ABNORMAL_STATE_SUCCESS_RATE);
      this._maxChance = set.getInt("maxChance", PlayerConfig.MAX_ABNORMAL_STATE_SUCCESS_RATE);
      this._ignoreShield = set.getBoolean("ignoreShld", false);
      this._nextActionIsAttack = set.getBoolean("nextActionAttack", false);
      this._removedOnAnyActionExceptMove = set.getBoolean("removedOnAnyActionExceptMove", false);
      this._removedOnDamage = set.getBoolean("removedOnDamage", false);
      this._blockedInOlympiad = set.getBoolean("blockedInOlympiad", false);
      this._element = set.getByte("element", (byte)-1);
      this._elementPower = set.getInt("elementPower", 0);
      this._basicProperty = (BaseStat)set.getEnum("basicProperty", BaseStat.class, BaseStat.NONE);
      this._overhit = set.getBoolean("overHit", false);
      this._isSuicideAttack = set.getBoolean("isSuicideAttack", false);
      this._minPledgeClass = set.getInt("minPledgeClass", 0);
      this._chargeConsume = set.getInt("chargeConsume", 0);
      this._soulMaxConsume = set.getInt("soulMaxConsumeCount", 0);
      this._blowChance = set.getInt("blowChance", 0);
      this._isHeroSkill = SkillTreeData.getInstance().isHeroSkill(this._id, this._level);
      this._isGMSkill = SkillTreeData.getInstance().isGMSkill(this._id, this._level);
      this._isSevenSigns = this._id > 4360 && this._id < 4367;
      this._isClanSkill = SkillTreeData.getInstance().isClanSkill(this._id, this._level);
      this._baseCritRate = set.getInt("baseCritRate", 0);
      this._directHpDmg = set.getBoolean("dmgDirectlyToHp", false);
      this._effectPoint = set.getInt("effectPoint", 0);
      this._flyType = (FlyType)set.getEnum("flyType", FlyType.class, null);
      this._flyRadius = set.getInt("flyRadius", 0);
      this._flyCourse = set.getFloat("flyCourse", 0.0F);
      this._canBeDispeled = set.getBoolean("canBeDispeled", true);
      this._excludedFromCheck = set.getBoolean("excludedFromCheck", false);
      this._simultaneousCast = set.getBoolean("simultaneousCast", false);
      this._icon = set.getString("icon", "icon.skill0000");
      this._channelingSkillId = set.getInt("channelingSkillId", 0);
      this._channelingTickInterval = set.getInt("channelingTickInterval", 2) * 1000;
      this._channelingTickInitialDelay = set.getInt("channelingTickInitialDelay", this._channelingTickInterval / 1000) * 1000;
      this._isPvPOnly = set.getBoolean("isPvPOnly", false);
   }

   public TraitType getTraitType() {
      return this._traitType;
   }

   public byte getElement() {
      return this._element;
   }

   public int getElementPower() {
      return this._elementPower;
   }

   public TargetType getTargetType() {
      return this._targetType;
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public boolean isAOE() {
      switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$skill$targets$TargetType[this._targetType.ordinal()]) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
            return true;
         default:
            return false;
      }
   }

   public boolean isDamage() {
      return this.hasEffectType(EffectType.MAGICAL_ATTACK, EffectType.HP_DRAIN, EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK);
   }

   public boolean isOverhit() {
      return this._overhit;
   }

   public boolean isSuicideAttack() {
      return this._isSuicideAttack;
   }

   public double getPower(Creature creature, Creature target, boolean isPvP, boolean isPvE) {
      if (creature == null) {
         return this.getPower(isPvP, isPvE);
      } else if (this.hasEffectType(EffectType.DEATH_LINK)) {
         return this.getPower(isPvP, isPvE) * (-(creature.getCurrentHp() * 2.0 / creature.getMaxHp()) + 2.0);
      } else {
         return this.hasEffectType(EffectType.PHYSICAL_ATTACK_HP_LINK)
            ? this.getPower(isPvP, isPvE) * (-(target.getCurrentHp() * 2.0 / target.getMaxHp()) + 2.0)
            : this.getPower(isPvP, isPvE);
      }
   }

   public double getPower() {
      return this._power;
   }

   public double getPower(boolean isPvP, boolean isPvE) {
      return isPvE ? this._pvePower : (isPvP ? this._pvpPower : this._power);
   }

   public boolean isAbnormalInstant() {
      return this._isAbnormalInstant;
   }

   public AbnormalType getAbnormalType() {
      return this._abnormalType;
   }

   public int getAbnormalLevel() {
      return this._abnormalLevel;
   }

   public int getAbnormalTime() {
      return this._abnormalTime;
   }

   public AbnormalVisualEffect[] getAbnormalVisualEffects() {
      return this._abnormalVisualEffects;
   }

   public boolean hasAbnormalVisualEffects() {
      return this._abnormalVisualEffects != null && this._abnormalVisualEffects.length > 0;
   }

   public AbnormalVisualEffect[] getAbnormalVisualEffectsSpecial() {
      return this._abnormalVisualEffectsSpecial;
   }

   public boolean hasAbnormalVisualEffectsSpecial() {
      return this._abnormalVisualEffectsSpecial != null && this._abnormalVisualEffectsSpecial.length > 0;
   }

   public AbnormalVisualEffect[] getAbnormalVisualEffectsEvent() {
      return this._abnormalVisualEffectsEvent;
   }

   public boolean hasAbnormalVisualEffectsEvent() {
      return this._abnormalVisualEffectsEvent != null && this._abnormalVisualEffectsEvent.length > 0;
   }

   public int getMagicLevel() {
      return this._magicLevel;
   }

   public int getLvlBonusRate() {
      return this._lvlBonusRate;
   }

   public int getActivateRate() {
      return this._activateRate;
   }

   public int getMinChance() {
      return this._minChance;
   }

   public int getMaxChance() {
      return this._maxChance;
   }

   public boolean isRemovedOnAnyActionExceptMove() {
      return this._removedOnAnyActionExceptMove;
   }

   public boolean isRemovedOnDamage() {
      return this._removedOnDamage;
   }

   public boolean isBlockedInOlympiad() {
      return this._blockedInOlympiad;
   }

   public int getChannelingSkillId() {
      return this._channelingSkillId;
   }

   public boolean nextActionIsAttack() {
      return this._nextActionIsAttack;
   }

   public int getCastRange() {
      return this._castRange;
   }

   public int getEffectRange() {
      return this._effectRange;
   }

   public int getHpConsume() {
      return this._hpConsume;
   }

   public int getId() {
      return this._id;
   }

   public boolean isDebuff() {
      return this._isDebuff;
   }

   public boolean isRecoveryHerb() {
      return this._isRecoveryHerb;
   }

   public int getDisplayId() {
      return this._displayId;
   }

   public int getDisplayLevel() {
      return this._displayLevel;
   }

   public BaseStat getBasicProperty() {
      return this._basicProperty;
   }

   public int getItemConsumeCount() {
      return this._itemConsumeCount;
   }

   public int getItemConsumeId() {
      return this._itemConsumeId;
   }

   public int getLevel() {
      return this._level;
   }

   public boolean isPhysical() {
      return this._magic == 0;
   }

   public boolean isMagic() {
      return this._magic == 1;
   }

   public boolean isStatic() {
      return this._magic == 2;
   }

   public boolean isDance() {
      return this._magic == 3;
   }

   public boolean isStaticReuse() {
      return this._staticReuse;
   }

   public int getMpConsume() {
      return this._mpConsume;
   }

   public int getMpInitialConsume() {
      return this._mpInitialConsume;
   }

   public int getMpPerChanneling() {
      return this._mpPerChanneling;
   }

   public String getName() {
      return this._name;
   }

   public int getReuseDelay() {
      return this._reuseDelay;
   }

   public int getReuseHashCode() {
      return this._reuseHashCode;
   }

   public int getHitTime() {
      return this._hitTime;
   }

   public int getCoolTime() {
      return this._coolTime;
   }

   public int getAffectRange() {
      return this._affectRange;
   }

   public int getAffectLimit() {
      return this._affectLimit[0] + Rnd.get(this._affectLimit[1]);
   }

   public boolean isActive() {
      return this._operateType != null && this._operateType.isActive();
   }

   public boolean isPassive() {
      return this._operateType != null && this._operateType.isPassive();
   }

   public boolean isToggle() {
      return this._operateType != null && this._operateType.isToggle();
   }

   public boolean isContinuous() {
      return this._operateType != null && this._operateType.isContinuous() || this.isSelfContinuous();
   }

   public boolean isSelfContinuous() {
      return this._operateType != null && this._operateType.isSelfContinuous();
   }

   public boolean isChanneling() {
      return this._operateType != null && this._operateType.isChanneling();
   }

   public boolean isTransformation() {
      return this._abnormalType == AbnormalType.TRANSFORM;
   }

   public int getEffectPoint() {
      return this._effectPoint;
   }

   public boolean useSoulShot() {
      return this.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK);
   }

   public boolean useSpiritShot() {
      return this._magic == 1;
   }

   public boolean useFishShot() {
      return this.hasEffectType(EffectType.FISHING);
   }

   public int getMinPledgeClass() {
      return this._minPledgeClass;
   }

   public boolean isHeroSkill() {
      return this._isHeroSkill;
   }

   public boolean isGMSkill() {
      return this._isGMSkill;
   }

   public boolean is7Signs() {
      return this._isSevenSigns;
   }

   public boolean isHealingPotionSkill() {
      return this._abnormalType == AbnormalType.HP_RECOVER;
   }

   public int getChargeConsumeCount() {
      return this._chargeConsume;
   }

   public int getMaxSoulConsumeCount() {
      return this._soulMaxConsume;
   }

   public int getBaseCritRate() {
      return this._baseCritRate;
   }

   public boolean getDmgDirectlyToHP() {
      return this._directHpDmg;
   }

   public FlyType getFlyType() {
      return this._flyType;
   }

   public int getFlyRadius() {
      return this._flyRadius;
   }

   public float getFlyCourse() {
      return this._flyCourse;
   }

   public boolean isStayAfterDeath() {
      return this._stayAfterDeath;
   }

   public boolean isStayOnSubclassChange() {
      return this._stayOnSubclassChange;
   }

   public boolean hasNegativeEffect() {
      return this._effectPoint < 0 && this._targetType != TargetType.SELF;
   }

   public boolean checkCondition(Creature creature, WorldObject object, boolean itemOrWeapon) {
      if (!creature.isFakePlayer() && (!creature.isGM() || GeneralConfig.GM_SKILL_RESTRICTION)) {
         if (creature.isPlayer() && creature.asPlayer().isMounted() && this.hasNegativeEffect() && !MountEnabledSkillList.contains(this._id)) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
            sm.addSkillName(this._id);
            creature.sendPacket(sm);
            return false;
         } else {
            List<Condition> preCondition = itemOrWeapon ? this._itemPreCondition : this._preCondition;
            if (preCondition != null && !preCondition.isEmpty()) {
               Creature target = object instanceof Creature ? object.asCreature() : null;

               for (Condition cond : preCondition) {
                  if (!cond.test(creature, target, this)) {
                     String msg = cond.getMessage();
                     int msgId = cond.getMessageId();
                     if (msgId != 0) {
                        SystemMessage sm = new SystemMessage(msgId);
                        if (cond.isAddName()) {
                           sm.addSkillName(this._id);
                        }

                        creature.sendPacket(sm);
                     } else if (msg != null) {
                        creature.sendMessage(msg);
                     }

                     return false;
                  }
               }

               return true;
            } else {
               return true;
            }
         }
      } else {
         return true;
      }
   }

   public List<WorldObject> getTargetList(Creature creature, boolean onlyFirst) {
      Creature target = null;
      WorldObject objTarget = creature.getTarget();
      if (objTarget != null && objTarget.isCreature()) {
         target = objTarget.asCreature();
      }

      return this.getTargetList(creature, onlyFirst, target);
   }

   public List<WorldObject> getTargetList(Creature creature, boolean onlyFirst, Creature target) {
      ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(this.getTargetType());
      if (handler != null) {
         try {
            List<WorldObject> result = handler.getTargetList(this, creature, onlyFirst, target);
            if (creature != null && creature.isMonster() && !this.hasNegativeEffect() && result instanceof LinkedList) {
               result.removeIf(wo -> wo.isPlayable());
            }

            return result;
         } catch (Exception var6) {
            LOGGER.log(Level.WARNING, "Exception in Skill.getTargetList(): " + var6.getMessage(), (Throwable)var6);
         }
      }

      if (creature != null) {
         creature.sendMessage("Target type of skill is not currently handled.");
      }

      return Collections.emptyList();
   }

   public List<WorldObject> getTargetList(Creature creature) {
      return this.getTargetList(creature, false);
   }

   public WorldObject getFirstOfTargetList(Creature creature) {
      List<WorldObject> targets = this.getTargetList(creature, true);
      return targets.isEmpty() ? null : targets.get(0);
   }

   public static boolean checkForAreaOffensiveSkills(Creature caster, Creature target, Skill skill, boolean sourceInArena) {
      if (target != null && !target.isDead() && target != caster) {
         Player player = caster.asPlayer();
         Player targetPlayer = target.asPlayer();
         if (player != null) {
            if (targetPlayer != null) {
               if (targetPlayer == caster || targetPlayer == player) {
                  return false;
               }

               if (player.isAutoPlaying()) {
                  int targetMode = player.getAutoPlaySettings().getNextTargetMode();
                  if (targetMode == 1 || targetMode == 3) {
                     return false;
                  }
               }

               if (targetPlayer.inObserverMode()) {
                  return false;
               }

               if (skill.hasNegativeEffect()
                  && player.getSiegeState() > 0
                  && player.isInsideZone(ZoneId.SIEGE)
                  && player.getSiegeState() == targetPlayer.getSiegeState()
                  && player.getSiegeSide() == targetPlayer.getSiegeSide()) {
                  return false;
               }

               if (skill.hasNegativeEffect() && target.isInsideZone(ZoneId.PEACE)) {
                  return false;
               }

               Party party = player.getParty();
               Party targetParty = targetPlayer.getParty();
               if (party != null && targetParty != null) {
                  if (party.getLeaderObjectId() == targetParty.getLeaderObjectId()) {
                     return false;
                  }

                  if (party.isInCommandChannel() && party.getCommandChannel() == targetParty.getCommandChannel()) {
                     return false;
                  }
               }

               if (player.isOnEvent() && !player.isOnSoloEvent() && targetPlayer.isOnEvent() && player.getTeam() == target.getTeam()) {
                  return false;
               }

               if (player.isAutoPlaying() && (targetPlayer.getPvpFlag() == 0 || targetPlayer.getKarma() < 1)) {
                  int targetMode = player.getAutoPlaySettings().getNextTargetMode();
                  if (targetMode != 0 && targetMode != 2) {
                     return false;
                  }
               }

               if (!sourceInArena && (!targetPlayer.isInsideZone(ZoneId.PVP) || targetPlayer.isInsideZone(ZoneId.SIEGE))) {
                  if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId()) {
                     return false;
                  }

                  if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId()) {
                     return false;
                  }

                  if (!player.checkPvpSkill(targetPlayer, skill)) {
                     return false;
                  }
               }
            }
         } else if (targetPlayer == null && target.isAttackable() && caster.isAttackable()) {
            return false;
         }

         return GeoEngine.getInstance().canSeeTarget(caster, target);
      } else {
         return false;
      }
   }

   public static boolean addSummon(Creature caster, Player owner, int radius, boolean isDead) {
      return !owner.hasSummon() ? false : addCharacter(caster, owner.getSummon(), radius, isDead);
   }

   public static boolean addCharacter(Creature caster, Creature target, int radius, boolean isDead) {
      return isDead != target.isDead() ? false : radius <= 0 || LocationUtil.checkIfInRange(radius, caster, target, true);
   }

   public List<AbstractFunction> getStatFuncs(AbstractEffect effect, Creature creature) {
      if (this._funcTemplates != null && creature != null) {
         if (!creature.isPlayable() && !creature.isAttackable()) {
            return Collections.emptyList();
         } else {
            List<AbstractFunction> funcs = new ArrayList<>(this._funcTemplates.size());

            for (FuncTemplate t : this._funcTemplates) {
               AbstractFunction f = t.getFunc(creature, null, this, this);
               if (f != null) {
                  funcs.add(f);
               }
            }

            return funcs;
         }
      } else {
         return Collections.emptyList();
      }
   }

   public List<AbstractEffect> getEffects(EffectScope effectScope) {
      return this._effectLists.get(effectScope);
   }

   public boolean hasEffects(EffectScope effectScope) {
      List<AbstractEffect> effects = this._effectLists.get(effectScope);
      return effects != null && !effects.isEmpty();
   }

   public void applyEffectScope(EffectScope effectScope, BuffInfo info, boolean applyInstantEffects, boolean addContinuousEffects) {
      if (effectScope != null && this.hasEffects(effectScope)) {
         for (AbstractEffect effect : this.getEffects(effectScope)) {
            if (effect != null) {
               if (effect.isInstant()) {
                  if (applyInstantEffects && effect.calcSuccess(info.getEffector(), info.getEffected(), info.getSkill())) {
                     effect.onStart(info.getEffector(), info.getEffected(), info.getSkill());
                  }
               } else if (addContinuousEffects && effect.canStart(info.getEffector(), info.getEffected(), info.getSkill())) {
                  info.addEffect(effect);
               }
            }
         }
      }
   }

   public void applyEffects(Creature effector, Creature effected) {
      this.applyEffects(effector, effected, false, false, true, 0);
   }

   public void applyEffects(Creature effector, Creature effected, boolean instant, int abnormalTime) {
      this.applyEffects(effector, effected, false, false, instant, abnormalTime);
   }

   public void applyEffects(Creature effector, Creature effected, boolean self, boolean passive, boolean instant, int abnormalTime) {
      if (effected != null) {
         if (effector == effected || !this.hasNegativeEffect() || !effected.isInvul() && (!effector.isGM() || effector.getAccessLevel().canGiveDamage())) {
            if (!effected.isInvulAgainst(this._id, this._level)) {
               boolean addContinuousEffects = !passive
                  && (this._operateType.isToggle() || this._operateType.isContinuous() && Formulas.calcEffectSuccess(effector, effected, this));
               if (!self && !passive) {
                  BuffInfo info = new BuffInfo(effector, effected, this);
                  if (addContinuousEffects && abnormalTime > 0) {
                     info.setAbnormalTime(abnormalTime);
                  }

                  this.applyEffectScope(EffectScope.GENERAL, info, instant, addContinuousEffects);
                  EffectScope pvpOrPveEffectScope = effector.isPlayable() && effected.isAttackable()
                     ? EffectScope.PVE
                     : (effector.isPlayable() && effected.isPlayable() ? EffectScope.PVP : null);
                  this.applyEffectScope(pvpOrPveEffectScope, info, instant, addContinuousEffects);
                  this.applyEffectScope(EffectScope.CHANNELING, info, instant, addContinuousEffects);
                  if (addContinuousEffects) {
                     effected.getEffectList().add(info);
                  }
               }

               if (self) {
                  addContinuousEffects = !passive
                     && (
                        this._operateType.isToggle()
                           || (this._operateType.isContinuous() || this._operateType.isSelfContinuous())
                              && Formulas.calcEffectSuccess(effector, effector, this)
                     );
                  BuffInfo infox = new BuffInfo(effector, effector, this);
                  if (addContinuousEffects && abnormalTime > 0) {
                     infox.setAbnormalTime(abnormalTime);
                  }

                  this.applyEffectScope(EffectScope.SELF, infox, instant, addContinuousEffects);
                  if (addContinuousEffects && this.hasEffectType(EffectType.BUFF)) {
                     infox.getEffector().getEffectList().add(infox);
                  }
               }

               if (passive) {
                  BuffInfo infoxx = new BuffInfo(effector, effector, this);
                  this.applyEffectScope(EffectScope.PASSIVE, infoxx, false, true);
                  effector.getEffectList().add(infoxx);
               }
            }
         }
      }
   }

   public void activateSkill(Creature caster, Collection<WorldObject> targets) {
      this.activateSkill(caster, null, targets);
   }

   public void activateSkill(Cubic cubic, Collection<WorldObject> targets) {
      this.activateSkill(cubic.getOwner(), cubic, targets);
   }

   private void activateSkill(Creature caster, Cubic cubic, Collection<WorldObject> targets) {
      for (WorldObject obj : targets) {
         Creature target = obj.asCreature();
         if (Formulas.calcBuffDebuffReflection(target, this)) {
            this.applyEffects(target, caster, false, 0);
            BuffInfo info = new BuffInfo(caster, target, this);
            this.applyEffectScope(EffectScope.GENERAL, info, true, false);
            EffectScope pvpOrPveEffectScope = caster.isPlayable() && target.isAttackable()
               ? EffectScope.PVE
               : (caster.isPlayable() && target.isPlayable() ? EffectScope.PVP : null);
            this.applyEffectScope(pvpOrPveEffectScope, info, true, false);
            this.applyEffectScope(EffectScope.CHANNELING, info, true, false);
         } else {
            this.applyEffects(caster, target);
         }
      }

      if (this.hasEffects(EffectScope.SELF)) {
         if (caster.isAffectedBySkill(this._id)) {
            caster.stopSkillEffects(SkillFinishType.REMOVED, this._id);
         }

         this.applyEffects(caster, caster, true, false, true, 0);
      }

      if (cubic == null) {
         if (this.useSpiritShot()) {
            caster.setChargedShot(caster.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
         } else if (this.useSoulShot()) {
            caster.setChargedShot(ShotType.SOULSHOTS, false);
         }
      }

      if (this._isSuicideAttack) {
         caster.doDie(caster);
      }
   }

   public void attach(FuncTemplate f) {
      if (this._funcTemplates == null) {
         this._funcTemplates = new ArrayList<>(1);
      }

      this._funcTemplates.add(f);
   }

   public void addEffect(EffectScope effectScope, AbstractEffect effect) {
      List<AbstractEffect> effects = this._effectLists.get(effectScope);
      if (effects == null) {
         effects = new ArrayList<>(1);
         this._effectLists.put(effectScope, effects);
      }

      effects.add(effect);
   }

   public void attach(Condition c, boolean itemOrWeapon) {
      if (itemOrWeapon) {
         if (this._itemPreCondition == null) {
            this._itemPreCondition = new ArrayList<>();
         }

         this._itemPreCondition.add(c);
      } else {
         if (this._preCondition == null) {
            this._preCondition = new ArrayList<>();
         }

         this._preCondition.add(c);
      }
   }

   @Override
   public String toString() {
      return "Skill " + this._name + "(" + this._id + "," + this._level + ")";
   }

   public int getFeed() {
      return this._feed;
   }

   public int getReferenceItemId() {
      return this._refId;
   }

   public void setReferenceItemId(int value) {
      this._refId = value;
   }

   public String getAttributeName() {
      return this._attribute;
   }

   public int getBlowChance() {
      return this._blowChance;
   }

   public boolean ignoreShield() {
      return this._ignoreShield;
   }

   public boolean canBeDispeled() {
      return this._canBeDispeled;
   }

   public boolean canBeStolen() {
      return !this.isPassive()
         && !this.isToggle()
         && !this._isDebuff
         && !this._isHeroSkill
         && !this._isGMSkill
         && (!this.isStatic() || this.getId() == CommonSkill.CARAVANS_SECRET_MEDICINE.getId())
         && this._canBeDispeled
         && this.getId() != CommonSkill.SERVITOR_SHARE.getId();
   }

   public boolean isClanSkill() {
      return this._isClanSkill;
   }

   public boolean isExcludedFromCheck() {
      return this._excludedFromCheck;
   }

   public boolean isSimultaneousCast() {
      return this._simultaneousCast;
   }

   private void parseAbnormalVisualEffect(String abnormalVisualEffects) {
      if (abnormalVisualEffects != null) {
         String[] data = abnormalVisualEffects.split(";");
         List<AbnormalVisualEffect> avesEvent = null;
         List<AbnormalVisualEffect> avesSpecial = null;
         List<AbnormalVisualEffect> aves = null;

         for (String ave2 : data) {
            AbnormalVisualEffect ave = AbnormalVisualEffect.valueOf(ave2);
            if (ave != null) {
               if (ave.isEvent()) {
                  if (avesEvent == null) {
                     avesEvent = new ArrayList<>(1);
                  }

                  avesEvent.add(ave);
               } else if (ave.isSpecial()) {
                  if (avesSpecial == null) {
                     avesSpecial = new ArrayList<>(1);
                  }

                  avesSpecial.add(ave);
               } else {
                  if (aves == null) {
                     aves = new ArrayList<>(1);
                  }

                  aves.add(ave);
               }
            }
         }

         if (avesEvent != null) {
            this._abnormalVisualEffectsEvent = avesEvent.toArray(new AbnormalVisualEffect[0]);
         }

         if (avesSpecial != null) {
            this._abnormalVisualEffectsSpecial = avesSpecial.toArray(new AbnormalVisualEffect[0]);
         }

         if (aves != null) {
            this._abnormalVisualEffects = aves.toArray(new AbnormalVisualEffect[0]);
         }
      }
   }

   public boolean hasEffectType(EffectType effectType, EffectType... effectTypes) {
      if (this._effectTypes == null) {
         synchronized (this) {
            if (this._effectTypes == null) {
               Set<Byte> effectTypesSet = new HashSet<>();

               for (List<AbstractEffect> effectList : this._effectLists.values()) {
                  if (effectList != null) {
                     for (AbstractEffect effect : effectList) {
                        if (effect != null) {
                           effectTypesSet.add((byte)effect.getEffectType().ordinal());
                        }
                     }
                  }
               }

               Byte[] effectTypesArray = effectTypesSet.toArray(new Byte[0]);
               Arrays.sort((Object[])effectTypesArray);
               this._effectTypes = effectTypesArray;
            }
         }
      }

      if (Arrays.binarySearch(this._effectTypes, Byte.valueOf((byte)effectType.ordinal())) >= 0) {
         return true;
      } else {
         for (EffectType type : effectTypes) {
            if (Arrays.binarySearch(this._effectTypes, Byte.valueOf((byte)type.ordinal())) >= 0) {
               return true;
            }
         }

         return false;
      }
   }

   public String getIcon() {
      return this._icon;
   }

   public int getChannelingTickInterval() {
      return this._channelingTickInterval;
   }

   public int getChannelingTickInitialDelay() {
      return this._channelingTickInitialDelay;
   }

   public boolean isPvPOnly() {
      return this._isPvPOnly;
   }
}
