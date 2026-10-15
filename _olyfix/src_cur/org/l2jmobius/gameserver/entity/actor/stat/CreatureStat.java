package org.l2jmobius.gameserver.entity.actor.stat;

import java.util.Arrays;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Calculator;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;

public class CreatureStat {
   private static final int DIVINE_INSPIRATION = 1405;
   private final Creature _creature;
   private long _exp = 0L;
   private long _sp = 0L;
   private byte _level = 1;
   private final float[] _attackTraits = new float[TraitType.values().length];
   private final int[] _attackTraitsCount = new int[TraitType.values().length];
   private final float[] _defenceTraits = new float[TraitType.values().length];
   private final int[] _defenceTraitsCount = new int[TraitType.values().length];
   private final int[] _traitsInvul = new int[TraitType.values().length];
   private int _maxBuffCount = PlayerConfig.BUFFS_MAX_AMOUNT;
   private double _gmSpeedMultiplier = 1.0;

   public CreatureStat(Creature creature) {
      this._creature = creature;
      Arrays.fill(this._attackTraits, 1.0F);
      Arrays.fill(this._defenceTraits, 1.0F);
   }

   public double calcStat(Stat stat, double init) {
      return this.calcStat(stat, init, null, null);
   }

   public double calcStat(Stat stat, double initVal, Creature target, Skill skill) {
      double value = initVal;
      if (stat == null) {
         return initVal;
      } else {
         int id = stat.ordinal();
         Calculator calc = this._creature.getCalculators()[id];
         if (calc != null && calc.size() != 0) {
            Transform transform = this._creature.getTransformation();
            if (transform != null) {
               double val = transform.getStat(this._creature.asPlayer(), stat);
               if (val > 0.0) {
                  value = val;
               }
            }

            value = calc.calc(this._creature, target, skill, value);
            if (value <= 0.0) {
               switch (stat) {
                  case MAX_HP:
                  case MAX_MP:
                  case MAX_CP:
                  case MAGIC_DEFENCE:
                  case POWER_DEFENCE:
                  case POWER_ATTACK:
                  case MAGIC_ATTACK:
                  case POWER_ATTACK_SPEED:
                  case MAGIC_ATTACK_SPEED:
                  case SHIELD_DEFENCE:
                  case STAT_CON:
                  case STAT_DEX:
                  case STAT_INT:
                  case STAT_MEN:
                  case STAT_STR:
                  case STAT_WIT:
                     value = 1.0;
               }
            }

            return value;
         } else {
            return initVal;
         }
      }
   }

   public int getAccuracy() {
      return (int)Math.round(this.calcStat(Stat.ACCURACY_COMBAT, 0.0, null, null));
   }

   public Creature getActiveChar() {
      return this._creature;
   }

   public float getAttackSpeedMultiplier() {
      return (float)(1.1 * this.getPAtkSpd() / this._creature.getTemplate().getBasePAtkSpd());
   }

   public double getCriticalDmg(Creature target, double init) {
      return this.calcStat(Stat.CRITICAL_DAMAGE, init, target, null);
   }

   public int getCriticalHit(Creature target, Skill skill) {
      double val = this.calcStat(Stat.CRITICAL_RATE, this._creature.getTemplate().getBaseCritRate(), target, skill);
      val = Math.min(val, (double)PlayerConfig.MAX_PCRIT_RATE);
      return (int)(val + 0.5);
   }

   public int getCriticalHitPos(int base) {
      return (int)this.calcStat(Stat.CRITICAL_RATE_POS, base);
   }

   public int getEvasionRate(Creature target) {
      int val = (int)Math.round(this.calcStat(Stat.EVASION_RATE, 0.0, target, null));
      return Math.min(val, PlayerConfig.MAX_EVASION);
   }

   public long getExp() {
      return this._exp;
   }

   public void setExp(long value) {
      this._exp = value;
   }

   public byte getLevel() {
      return this._level;
   }

   public void setLevel(byte value) {
      this._level = value;
   }

   public int getMagicalAttackRange(Skill skill) {
      return skill != null ? (int)this.calcStat(Stat.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill) : this._creature.getTemplate().getBaseAttackRange();
   }

   public int getMaxCp() {
      return (int)this.calcStat(Stat.MAX_CP, this._creature.getTemplate().getBaseCpMax());
   }

   public int getMaxRecoverableCp() {
      return (int)this.calcStat(Stat.MAX_RECOVERABLE_CP, this.getMaxCp());
   }

   public int getMaxHp() {
      return (int)this.calcStat(Stat.MAX_HP, this._creature.getTemplate().getBaseHpMax());
   }

   public int getMaxRecoverableHp() {
      return (int)this.calcStat(Stat.MAX_RECOVERABLE_HP, this.getMaxHp());
   }

   public int getMaxMp() {
      return (int)this.calcStat(Stat.MAX_MP, this._creature.getTemplate().getBaseMpMax());
   }

   public int getMaxRecoverableMp() {
      return (int)this.calcStat(Stat.MAX_RECOVERABLE_MP, this.getMaxMp());
   }

   public double getMAtk(Creature target, Skill skill) {
      float bonusAtk = 1.0F;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && this._creature.isChampion()) {
         bonusAtk = ChampionMonstersConfig.CHAMPION_ATK;
      }

      if (this._creature.isRaid()) {
         bonusAtk = (float)(bonusAtk * NpcConfig.RAID_MATTACK_MULTIPLIER);
      }

      return Math.min(this.calcStat(Stat.MAGIC_ATTACK, this._creature.getTemplate().getBaseMAtk() * bonusAtk, target, skill), (double)PlayerConfig.MAX_MATK);
   }

   public int getMAtkSpd() {
      float bonusSpdAtk = 1.0F;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && this._creature.isChampion()) {
         bonusSpdAtk = ChampionMonstersConfig.CHAMPION_SPD_ATK;
      }

      double val = this.calcStat(Stat.MAGIC_ATTACK_SPEED, this._creature.getTemplate().getBaseMAtkSpd() * bonusSpdAtk);
      val = Math.min(val, (double)PlayerConfig.MAX_MATK_SPEED);
      return (int)val;
   }

   public int getMCriticalHit(Creature target, Skill skill) {
      int val = (int)this.calcStat(Stat.MCRITICAL_RATE, 1.0, target, skill) * 10;
      if (skill != null && target != null && this._creature.isPlayable()) {
         if (target.isPlayable()) {
            val = (int)(val * ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[this._creature.asPlayer().getPlayerClass().getId()]);
         } else {
            val = (int)(val * ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[this._creature.asPlayer().getPlayerClass().getId()]);
         }
      }

      return Math.min(val, PlayerConfig.MAX_MCRIT_RATE);
   }

   public double getMDef(Creature target, Skill skill) {
      double defence = this._creature.getTemplate().getBaseMDef();
      if (this._creature.isRaid()) {
         defence *= NpcConfig.RAID_MDEFENCE_MULTIPLIER;
      }

      if (skill != null && target != null && target.isPlayable()) {
         if (skill.isMagic()) {
            defence *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
         } else {
            defence *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
         }
      }

      return this.calcStat(Stat.MAGIC_DEFENCE, defence, target, skill);
   }

   public int getCON() {
      return (int)this.calcStat(Stat.STAT_CON, this._creature.getTemplate().getBaseCON());
   }

   public int getDEX() {
      return (int)this.calcStat(Stat.STAT_DEX, this._creature.getTemplate().getBaseDEX());
   }

   public int getINT() {
      return (int)this.calcStat(Stat.STAT_INT, this._creature.getTemplate().getBaseINT());
   }

   public int getMEN() {
      return (int)this.calcStat(Stat.STAT_MEN, this._creature.getTemplate().getBaseMEN());
   }

   public int getSTR() {
      return (int)this.calcStat(Stat.STAT_STR, this._creature.getTemplate().getBaseSTR());
   }

   public int getWIT() {
      return (int)this.calcStat(Stat.STAT_WIT, this._creature.getTemplate().getBaseWIT());
   }

   public double getMovementSpeedMultiplier() {
      double baseSpeed;
      if (this._creature.isInsideZone(ZoneId.WATER)) {
         baseSpeed = this.getBaseMoveSpeed(this._creature.isRunning() ? MoveType.FAST_SWIM : MoveType.SLOW_SWIM);
      } else {
         baseSpeed = this.getBaseMoveSpeed(this._creature.isRunning() ? MoveType.RUN : MoveType.WALK);
      }

      return this.getMoveSpeed() * (1.0 / baseSpeed);
   }

   public void setGmSpeedMultiplier(double multipier) {
      this._gmSpeedMultiplier = multipier;
   }

   public double getRunSpeed() {
      double baseRunSpd = this._creature.isInsideZone(ZoneId.WATER) ? this.getSwimRunSpeed() : this.getBaseMoveSpeed(MoveType.RUN);
      return baseRunSpd <= 0.0 ? 0.0 : this.calcStat(Stat.MOVE_SPEED, baseRunSpd * this._gmSpeedMultiplier, null, null);
   }

   public double getWalkSpeed() {
      double baseWalkSpd = this._creature.isInsideZone(ZoneId.WATER) ? this.getSwimWalkSpeed() : this.getBaseMoveSpeed(MoveType.WALK);
      return baseWalkSpd <= 0.0 ? 0.0 : this.calcStat(Stat.MOVE_SPEED, baseWalkSpd * this._gmSpeedMultiplier);
   }

   public double getSwimRunSpeed() {
      double baseRunSpd = this.getBaseMoveSpeed(MoveType.FAST_SWIM);
      return baseRunSpd <= 0.0 ? 0.0 : this.calcStat(Stat.MOVE_SPEED, baseRunSpd * this._gmSpeedMultiplier, null, null);
   }

   public double getSwimWalkSpeed() {
      double baseWalkSpd = this.getBaseMoveSpeed(MoveType.SLOW_SWIM);
      return baseWalkSpd <= 0.0 ? 0.0 : this.calcStat(Stat.MOVE_SPEED, baseWalkSpd * this._gmSpeedMultiplier);
   }

   public double getBaseMoveSpeed(MoveType type) {
      return this._creature.getTemplate().getBaseMoveSpeed(type);
   }

   public double getMoveSpeed() {
      if (this._creature.isInsideZone(ZoneId.WATER)) {
         return this._creature.isRunning() ? this.getSwimRunSpeed() : this.getSwimWalkSpeed();
      } else {
         return this._creature.isRunning() ? this.getRunSpeed() : this.getWalkSpeed();
      }
   }

   public double getMReuseRate(Skill skill) {
      return this.calcStat(Stat.MAGIC_REUSE_RATE, 1.0, null, skill);
   }

   public double getPAtk(Creature target) {
      float bonusAtk = 1.0F;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && this._creature.isChampion()) {
         bonusAtk = ChampionMonstersConfig.CHAMPION_ATK;
      }

      if (this._creature.isRaid()) {
         bonusAtk = (float)(bonusAtk * NpcConfig.RAID_PATTACK_MULTIPLIER);
      }

      return Math.min(this.calcStat(Stat.POWER_ATTACK, this._creature.getTemplate().getBasePAtk() * bonusAtk, target, null), (double)PlayerConfig.MAX_PATK);
   }

   public double getPAtkSpd() {
      float bonusAtk = 1.0F;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && this._creature.isChampion()) {
         bonusAtk = ChampionMonstersConfig.CHAMPION_SPD_ATK;
      }

      return Math.round(this.calcStat(Stat.POWER_ATTACK_SPEED, this._creature.getTemplate().getBasePAtkSpd() * bonusAtk, null, null));
   }

   public double getPDef(Creature target) {
      if (!this._creature.isPlayable() || target == null) {
         return this.calcStat(
            Stat.POWER_DEFENCE,
            this._creature.isRaid()
               ? this._creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER
               : this._creature.getTemplate().getBasePDef(),
            target,
            null
         );
      } else {
         return target.isPlayable()
            ? ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[this._creature.asPlayer().getPlayerClass().getId()]
               * this.calcStat(
                  Stat.POWER_DEFENCE,
                  this._creature.isRaid()
                     ? this._creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER
                     : this._creature.getTemplate().getBasePDef(),
                  target,
                  null
               )
            : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[this._creature.asPlayer().getPlayerClass().getId()]
               * this.calcStat(
                  Stat.POWER_DEFENCE,
                  this._creature.isRaid()
                     ? this._creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER
                     : this._creature.getTemplate().getBasePDef(),
                  target,
                  null
               );
      }
   }

   public int getPhysicalAttackRange() {
      Transform transform = this._creature.getTransformation();
      int baseAttackRange;
      if (transform != null) {
         baseAttackRange = transform.getBaseAttackRange(this._creature.asPlayer());
      } else if (this._creature.getActiveWeaponItem() == null) {
         baseAttackRange = this._creature.getTemplate().getBaseAttackRange();
      } else {
         baseAttackRange = 0;
      }

      return (int)this.calcStat(Stat.POWER_ATTACK_RANGE, baseAttackRange, null, null);
   }

   public int getPhysicalAttackAngle() {
      return 0;
   }

   public double getWeaponReuseModifier(Creature target) {
      return this.calcStat(Stat.ATK_REUSE, 1.0, target, null);
   }

   public int getShldDef() {
      return (int)this.calcStat(Stat.SHIELD_DEFENCE, 0.0);
   }

   public long getSp() {
      return this._sp;
   }

   public void setSp(long value) {
      this._sp = value;
   }

   public int getMpConsume(Skill skill) {
      if (skill == null) {
         return 1;
      } else {
         double mpConsume = skill.getMpConsume();
         double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.0);
         if (skill.isDance() && PlayerConfig.DANCE_CONSUME_ADDITIONAL_MP && this._creature != null && this._creature.getDanceCount() > 0) {
            mpConsume += this._creature.getDanceCount() * nextDanceMpCost;
         }

         mpConsume = this.calcStat(Stat.MP_CONSUME, mpConsume, null, skill);
         if (skill.isDance()) {
            return (int)this.calcStat(Stat.DANCE_MP_CONSUME_RATE, mpConsume);
         } else {
            return skill.isMagic() ? (int)this.calcStat(Stat.MAGICAL_MP_CONSUME_RATE, mpConsume) : (int)this.calcStat(Stat.PHYSICAL_MP_CONSUME_RATE, mpConsume);
         }
      }
   }

   public int getMpInitialConsume(Skill skill) {
      return skill == null ? 1 : (int)this.calcStat(Stat.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
   }

   public byte getAttackElement() {
      Item weaponInstance = this._creature.getActiveWeaponInstance();
      if (weaponInstance != null && weaponInstance.getAttackElementType() >= 0) {
         return weaponInstance.getAttackElementType();
      } else {
         int tempVal = 0;
         int[] stats = new int[]{0, 0, 0, 0, 0, 0};
         byte returnVal = -2;
         stats[0] = (int)this.calcStat(Stat.FIRE_POWER, this._creature.getTemplate().getBaseFire());
         stats[1] = (int)this.calcStat(Stat.WATER_POWER, this._creature.getTemplate().getBaseWater());
         stats[2] = (int)this.calcStat(Stat.WIND_POWER, this._creature.getTemplate().getBaseWind());
         stats[3] = (int)this.calcStat(Stat.EARTH_POWER, this._creature.getTemplate().getBaseEarth());
         stats[4] = (int)this.calcStat(Stat.HOLY_POWER, this._creature.getTemplate().getBaseHoly());
         stats[5] = (int)this.calcStat(Stat.DARK_POWER, this._creature.getTemplate().getBaseDark());

         for (byte x = 0; x < 6; x++) {
            if (stats[x] > tempVal) {
               returnVal = x;
               tempVal = stats[x];
            }
         }

         return returnVal;
      }
   }

   public int getAttackElementValue(byte attackAttribute) {
      switch (attackAttribute) {
         case 0:
            return (int)this.calcStat(Stat.FIRE_POWER, this._creature.getTemplate().getBaseFire());
         case 1:
            return (int)this.calcStat(Stat.WATER_POWER, this._creature.getTemplate().getBaseWater());
         case 2:
            return (int)this.calcStat(Stat.WIND_POWER, this._creature.getTemplate().getBaseWind());
         case 3:
            return (int)this.calcStat(Stat.EARTH_POWER, this._creature.getTemplate().getBaseEarth());
         case 4:
            return (int)this.calcStat(Stat.HOLY_POWER, this._creature.getTemplate().getBaseHoly());
         case 5:
            return (int)this.calcStat(Stat.DARK_POWER, this._creature.getTemplate().getBaseDark());
         default:
            return 0;
      }
   }

   public int getDefenseElementValue(byte defenseAttribute) {
      switch (defenseAttribute) {
         case 0:
            return (int)this.calcStat(Stat.FIRE_RES, this._creature.getTemplate().getBaseFireRes());
         case 1:
            return (int)this.calcStat(Stat.WATER_RES, this._creature.getTemplate().getBaseWaterRes());
         case 2:
            return (int)this.calcStat(Stat.WIND_RES, this._creature.getTemplate().getBaseWindRes());
         case 3:
            return (int)this.calcStat(Stat.EARTH_RES, this._creature.getTemplate().getBaseEarthRes());
         case 4:
            return (int)this.calcStat(Stat.HOLY_RES, this._creature.getTemplate().getBaseHolyRes());
         case 5:
            return (int)this.calcStat(Stat.DARK_RES, this._creature.getTemplate().getBaseDarkRes());
         default:
            return (int)this._creature.getTemplate().getBaseElementRes();
      }
   }

   public float getAttackTrait(TraitType traitType) {
      return this._attackTraits[traitType.ordinal()];
   }

   public float[] getAttackTraits() {
      return this._attackTraits;
   }

   public boolean hasAttackTrait(TraitType traitType) {
      return this._attackTraitsCount[traitType.ordinal()] > 0;
   }

   public int[] getAttackTraitsCount() {
      return this._attackTraitsCount;
   }

   public float getDefenceTrait(TraitType traitType) {
      return this._defenceTraits[traitType.ordinal()];
   }

   public float[] getDefenceTraits() {
      return this._defenceTraits;
   }

   public boolean hasDefenceTrait(TraitType traitType) {
      return this._defenceTraitsCount[traitType.ordinal()] > 0;
   }

   public int[] getDefenceTraitsCount() {
      return this._defenceTraitsCount;
   }

   public boolean isTraitInvul(TraitType traitType) {
      return this._traitsInvul[traitType.ordinal()] > 0;
   }

   public int[] getTraitsInvul() {
      return this._traitsInvul;
   }

   public int getMaxBuffCount() {
      return this._maxBuffCount + this._creature.getSkillLevel(1405);
   }

   public void setMaxBuffCount(int buffCount) {
      this._maxBuffCount = buffCount;
   }
}
