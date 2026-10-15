package org.l2jmobius.gameserver.mechanics.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.HitConditionBonusData;
import org.l2jmobius.gameserver.data.xml.KarmaLossData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.CastleZone;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.entity.zone.type.FortZone;
import org.l2jmobius.gameserver.entity.zone.type.MotherTreeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas.1;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncArmorSet;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncAtkAccuracy;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncAtkCritical;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncAtkEvasion;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncGatesMDefMod;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncGatesPDefMod;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncHenna;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMAtkCritical;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMAtkMod;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMAtkSpeed;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMDefMod;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMaxCpMul;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMaxHpMul;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMaxMpMul;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncMoveSpeed;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncPAtkMod;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncPAtkSpeed;
import org.l2jmobius.gameserver.mechanics.stats.functions.formulas.FuncPDefMod;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.MathUtil;

public class Formulas {
   private static final int HP_REGENERATE_PERIOD = 3000;
   public static final byte SHIELD_DEFENSE_FAILED = 0;
   public static final byte SHIELD_DEFENSE_SUCCEED = 1;
   public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2;
   private static final byte MELEE_ATTACK_RANGE = 40;
   public static final Set<Integer> FIXED_DEBUFF_RATE_PLAYERS = ConcurrentHashMap.newKeySet();
   public static int FIXED_DEBUFF_RATE_PERCENT = 25;

   public static int getRegeneratePeriod(Creature var0) {
      return var0.isDoor() ? 300000 : 3000;
   }

   public static Calculator[] getStdNPCCalculators() {
      Calculator[] var0 = new Calculator[Stat.NUM_STATS];
      var0[Stat.MAX_HP.ordinal()] = new Calculator();
      var0[Stat.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());
      var0[Stat.MAX_MP.ordinal()] = new Calculator();
      var0[Stat.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());
      var0[Stat.POWER_ATTACK.ordinal()] = new Calculator();
      var0[Stat.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());
      var0[Stat.MAGIC_ATTACK.ordinal()] = new Calculator();
      var0[Stat.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());
      var0[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
      var0[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());
      var0[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
      var0[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());
      var0[Stat.CRITICAL_RATE.ordinal()] = new Calculator();
      var0[Stat.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());
      var0[Stat.MCRITICAL_RATE.ordinal()] = new Calculator();
      var0[Stat.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());
      var0[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
      var0[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
      var0[Stat.EVASION_RATE.ordinal()] = new Calculator();
      var0[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
      var0[Stat.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
      var0[Stat.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());
      var0[Stat.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
      var0[Stat.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());
      var0[Stat.MOVE_SPEED.ordinal()] = new Calculator();
      var0[Stat.MOVE_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());
      return var0;
   }

   public static Calculator[] getStdDoorCalculators() {
      Calculator[] var0 = new Calculator[Stat.NUM_STATS];
      var0[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
      var0[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
      var0[Stat.EVASION_RATE.ordinal()] = new Calculator();
      var0[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
      var0[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
      var0[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
      var0[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
      var0[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
      return var0;
   }

   public static void addFuncsToNewCharacter(Creature var0) {
      if (var0.isPlayer()) {
         var0.addStatFunc(FuncMaxHpMul.getInstance());
         var0.addStatFunc(FuncMaxCpMul.getInstance());
         var0.addStatFunc(FuncMaxMpMul.getInstance());
         var0.addStatFunc(FuncPAtkMod.getInstance());
         var0.addStatFunc(FuncMAtkMod.getInstance());
         var0.addStatFunc(FuncPDefMod.getInstance());
         var0.addStatFunc(FuncMDefMod.getInstance());
         var0.addStatFunc(FuncAtkCritical.getInstance());
         var0.addStatFunc(FuncMAtkCritical.getInstance());
         var0.addStatFunc(FuncAtkAccuracy.getInstance());
         var0.addStatFunc(FuncAtkEvasion.getInstance());
         var0.addStatFunc(FuncPAtkSpeed.getInstance());
         var0.addStatFunc(FuncMAtkSpeed.getInstance());
         var0.addStatFunc(FuncMoveSpeed.getInstance());
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_STR));
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_DEX));
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_INT));
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_MEN));
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_CON));
         var0.addStatFunc(FuncHenna.getInstance(Stat.STAT_WIT));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_STR));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_DEX));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_INT));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_MEN));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_CON));
         var0.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_WIT));
      } else if (var0.isSummon()) {
         var0.addStatFunc(FuncMaxHpMul.getInstance());
         var0.addStatFunc(FuncMaxMpMul.getInstance());
         var0.addStatFunc(FuncPAtkMod.getInstance());
         var0.addStatFunc(FuncMAtkMod.getInstance());
         var0.addStatFunc(FuncPDefMod.getInstance());
         var0.addStatFunc(FuncMDefMod.getInstance());
         var0.addStatFunc(FuncAtkCritical.getInstance());
         var0.addStatFunc(FuncMAtkCritical.getInstance());
         var0.addStatFunc(FuncAtkAccuracy.getInstance());
         var0.addStatFunc(FuncAtkEvasion.getInstance());
         var0.addStatFunc(FuncMoveSpeed.getInstance());
         var0.addStatFunc(FuncPAtkSpeed.getInstance());
         var0.addStatFunc(FuncMAtkSpeed.getInstance());
      }
   }

   public static double calcHpRegen(Creature var0) {
      double var1 = var0.isPlayer() ? var0.asPlayer().getTemplate().getBaseHpRegen(var0.getLevel()) : var0.getTemplate().getBaseHpReg();
      double var3 = var0.isRaid() ? NpcConfig.RAID_HP_REGEN_MULTIPLIER : PlayerConfig.HP_REGEN_MULTIPLIER;
      double var5 = 0.0;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && var0.isChampion()) {
         var3 *= ChampionMonstersConfig.CHAMPION_HP_REGEN;
      }

      if (var0.isPlayer()) {
         Player var7 = var0.asPlayer();
         if (SevenSignsFestival.getInstance().isFestivalInProgress() && var7.isFestivalParticipant()) {
            var3 *= calcFestivalRegenModifier(var7);
         } else {
            double var8 = calcSiegeRegenModifier(var7);
            if (var8 > 0.0) {
               var3 *= var8;
            }
         }

         Clan var13 = var7.getClan();
         if (var7.isInsideZone(ZoneId.CLAN_HALL) && var13 != null && var13.getHideoutId() > 0) {
            ClanHallZone var9 = (ClanHallZone)ZoneManager.getInstance().getZone(var7, ClanHallZone.class);
            int var10 = var9 == null ? -1 : var9.getResidenceId();
            int var11 = var13.getHideoutId();
            if (var11 > 0 && var11 == var10) {
               ClanHall var12 = ClanHallTable.getInstance().getClanHallById(var11);
               if (var12 != null && var12.getFunction(3) != null) {
                  var3 *= 1.0 + var12.getFunction(3).getLevel() / 100.0;
               }
            }
         }

         if (var7.isInsideZone(ZoneId.CASTLE) && var13 != null && var13.getCastleId() > 0) {
            CastleZone var14 = (CastleZone)ZoneManager.getInstance().getZone(var7, CastleZone.class);
            int var17 = var14 == null ? -1 : var14.getResidenceId();
            int var20 = var13.getCastleId();
            if (var20 > 0 && var20 == var17) {
               Castle var22 = CastleManager.getInstance().getCastleById(var20);
               if (var22 != null && var22.getFunction(2) != null) {
                  var3 *= 1.0 + var22.getFunction(2).getLvl() / 100.0;
               }
            }
         }

         if (var7.isInsideZone(ZoneId.FORT) && var13 != null && var13.getFortId() > 0) {
            FortZone var15 = (FortZone)ZoneManager.getInstance().getZone(var7, FortZone.class);
            int var18 = var15 == null ? -1 : var15.getResidenceId();
            int var21 = var13.getFortId();
            if (var21 > 0 && var21 == var18) {
               Fort var23 = FortManager.getInstance().getFortById(var21);
               if (var23 != null && var23.getFunction(2) != null) {
                  var3 *= 1.0 + var23.getFunction(2).getLevel() / 100.0;
               }
            }
         }

         if (var7.isInsideZone(ZoneId.MOTHER_TREE)) {
            MotherTreeZone var16 = (MotherTreeZone)ZoneManager.getInstance().getZone(var7, MotherTreeZone.class);
            int var19 = var16 == null ? 0 : var16.getHpRegenBonus();
            var5 += var19;
         }

         if (var7.isSitting()) {
            var3 *= 1.5;
         } else if (!var7.isMoving()) {
            var3 *= 1.1;
         } else if (var7.isRunning()) {
            var3 *= 0.7;
         }

         var1 *= var0.getLevelMod() * BaseStat.CON.calcBonus(var0);
      } else if (var0.isPet()) {
         var1 = var0.asPet().getPetLevelData().getPetRegenHP() * NpcConfig.PET_HP_REGEN_MULTIPLIER;
      }

      return var0.calcStat(Stat.REGENERATE_HP_RATE, Math.max(1.0, var1), null, null) * var3 + var5;
   }

   public static double calcMpRegen(Creature var0) {
      double var1 = var0.isPlayer() ? var0.asPlayer().getTemplate().getBaseMpRegen(var0.getLevel()) : var0.getTemplate().getBaseMpReg();
      double var3 = var0.isRaid() ? NpcConfig.RAID_MP_REGEN_MULTIPLIER : PlayerConfig.MP_REGEN_MULTIPLIER;
      double var5 = 0.0;
      if (var0.isPlayer()) {
         Player var7 = var0.asPlayer();
         if (SevenSignsFestival.getInstance().isFestivalInProgress() && var7.isFestivalParticipant()) {
            var3 *= calcFestivalRegenModifier(var7);
         }

         if (var7.isInsideZone(ZoneId.MOTHER_TREE)) {
            MotherTreeZone var8 = (MotherTreeZone)ZoneManager.getInstance().getZone(var7, MotherTreeZone.class);
            int var9 = var8 == null ? 0 : var8.getMpRegenBonus();
            var5 += var9;
         }

         Clan var13 = var7.getClan();
         if (var7.isInsideZone(ZoneId.CLAN_HALL) && var13 != null && var13.getHideoutId() > 0) {
            ClanHallZone var14 = (ClanHallZone)ZoneManager.getInstance().getZone(var7, ClanHallZone.class);
            int var10 = var14 == null ? -1 : var14.getResidenceId();
            int var11 = var13.getHideoutId();
            if (var11 > 0 && var11 == var10) {
               ClanHall var12 = ClanHallTable.getInstance().getClanHallById(var11);
               if (var12 != null && var12.getFunction(4) != null) {
                  var3 *= 1.0 + var12.getFunction(4).getLevel() / 100.0;
               }
            }
         }

         if (var7.isInsideZone(ZoneId.CASTLE) && var13 != null && var13.getCastleId() > 0) {
            CastleZone var15 = (CastleZone)ZoneManager.getInstance().getZone(var7, CastleZone.class);
            int var17 = var15 == null ? -1 : var15.getResidenceId();
            int var19 = var13.getCastleId();
            if (var19 > 0 && var19 == var17) {
               Castle var21 = CastleManager.getInstance().getCastleById(var19);
               if (var21 != null && var21.getFunction(3) != null) {
                  var3 *= 1.0 + var21.getFunction(3).getLvl() / 100.0;
               }
            }
         }

         if (var7.isInsideZone(ZoneId.FORT) && var13 != null && var13.getFortId() > 0) {
            FortZone var16 = (FortZone)ZoneManager.getInstance().getZone(var7, FortZone.class);
            int var18 = var16 == null ? -1 : var16.getResidenceId();
            int var20 = var13.getFortId();
            if (var20 > 0 && var20 == var18) {
               Fort var22 = FortManager.getInstance().getFortById(var20);
               if (var22 != null && var22.getFunction(3) != null) {
                  var3 *= 1.0 + var22.getFunction(3).getLevel() / 100.0;
               }
            }
         }

         if (var7.isSitting()) {
            var3 *= 1.5;
         } else if (!var7.isMoving()) {
            var3 *= 1.1;
         } else if (var7.isRunning()) {
            var3 *= 0.7;
         }

         var1 *= var0.getLevelMod() * BaseStat.MEN.calcBonus(var0);
      } else if (var0.isPet()) {
         var1 = var0.asPet().getPetLevelData().getPetRegenMP() * NpcConfig.PET_MP_REGEN_MULTIPLIER;
      }

      return var0.calcStat(Stat.REGENERATE_MP_RATE, Math.max(1.0, var1), null, null) * var3 + var5;
   }

   public static double calcCpRegen(Player var0) {
      double var1 = var0.asPlayer().getTemplate().getBaseCpRegen(var0.getLevel()) * var0.getLevelMod() * BaseStat.CON.calcBonus(var0);
      double var3 = PlayerConfig.CP_REGEN_MULTIPLIER;
      if (var0.isSitting()) {
         var3 *= 1.5;
      } else if (!var0.isMoving()) {
         var3 *= 1.1;
      } else if (var0.isRunning()) {
         var3 *= 0.7;
      }

      return var0.calcStat(Stat.REGENERATE_CP_RATE, Math.max(1.0, var1), null, null) * var3;
   }

   public static double calcFestivalRegenModifier(Player var0) {
      int[] var1 = SevenSignsFestival.getInstance().getFestivalForPlayer(var0);
      int var2 = var1[1];
      if (var2 < 0) {
         return 0.0;
      } else {
         int var3 = var1[0];
         int[] var4;
         if (var3 == 2) {
            var4 = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[var2];
         } else {
            var4 = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[var2];
         }

         double var5 = var0.calculateDistance2D(var4[0], var4[1], 0);
         return 1.0 - var5 * 5.0E-4;
      }
   }

   public static double calcSiegeRegenModifier(Player var0) {
      if (var0 == null) {
         return 0.0;
      } else {
         Clan var1 = var0.getClan();
         if (var1 == null) {
            return 0.0;
         } else {
            Siege var2 = SiegeManager.getInstance().getSiege(var0.getX(), var0.getY(), var0.getZ());
            if (var2 != null && var2.isInProgress()) {
               SiegeClan var3 = var2.getAttackerClan(var1.getId());
               return var3 != null
                     && !var3.getFlag().isEmpty()
                     && LocationUtil.checkIfInRange(200, var0, (WorldObject)var3.getFlag().stream().findFirst().get(), true)
                  ? 1.5
                  : 0.0;
            } else {
               return 0.0;
            }
         }
      }
   }

   public static double calcBlowDamage(Creature var0, Creature var1, Skill var2, byte var3, boolean var4) {
      double var5 = var1.getPDef(var0);
      switch (var3) {
         case 1:
            var5 += var1.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      boolean var7 = var0.isPlayable() && var1.isPlayable();
      boolean var8 = var0.isPlayable() && var1.isAttackable();
      double var9 = var2.getPower(var7, var8);
      double var11 = 0.0;
      double var13 = var0.isBehind(var1) ? 1.2 : (var0.isInFrontOf(var1) ? 1.0 : 1.1);
      double var15 = var4 ? 1.458 : 1.0;
      double var17 = 1.0;
      if (var7) {
         var17 = var0.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
         var17 *= ClassBalanceConfig.PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         var5 *= var1.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         var5 *= ClassBalanceConfig.PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
      }

      if (var8) {
         var17 *= ClassBalanceConfig.PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
      }

      if (var0.isAttackable() && var1.isPlayable()) {
         var5 *= ClassBalanceConfig.PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
      }

      double var19 = 77.0 * (var9 + var0.getPAtk(var1) * var15) / var5;
      double var21 = var0.calcStat(Stat.CRITICAL_DAMAGE, 1.0, var1, var2);
      double var23 = (var0.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, var1, var2) - 1.0) / 2.0 + 1.0;
      double var25 = var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, var1, var2);
      double var27 = var0.getStat().calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0) * 6.1 * 77.0 / var5;
      double var29 = var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, var1, var2);
      double var31 = calcWeaponTraitBonus(var0, var1);
      double var33 = calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
      double var35 = calcAttributeBonus(var0, var1, var2);
      double var37 = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? var0.getRandomDamageMultiplier() : 1.0;
      double var39 = 1.0;
      if (var1.isAttackable() && !var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
         Player var41 = var0.asPlayer();
         if (var41 != null && var1.getLevel() - var41.getLevel() >= 2) {
            int var42 = var1.getLevel() - var41.getLevel() - 1;
            if (var42 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               var39 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               var39 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var42];
            }
         }
      }

      var11 = var19 * var21 * var23 * var25 * var13 * var17 + var27 + var29;
      var11 *= var31;
      var11 *= var33;
      var11 *= var35;
      var11 *= var37;
      var11 *= var39;
      return Math.max(var11, 1.0);
   }

   public static double calcBackstabDamage(Creature var0, Creature var1, Skill var2, byte var3, boolean var4) {
      double var5 = var1.getPDef(var0);
      switch (var3) {
         case 1:
            var5 += var1.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      boolean var7 = var0.isPlayable() && var1.isPlayer();
      boolean var8 = var0.isPlayable() && var1.isAttackable();
      double var9 = 0.0;
      double var11 = var0.isBehind(var1) ? 1.2 : (var0.isInFrontOf(var1) ? 1.0 : 1.1);
      double var13 = var4 ? 1.458 : 1.0;
      double var15 = 1.0;
      if (var7) {
         var15 = var0.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
         var15 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         var5 *= var1.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         var5 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
      }

      double var17 = 77.0 * (var2.getPower(var7, var8) + var0.getPAtk(var1)) / var5 * var13;
      double var19 = var0.calcStat(Stat.CRITICAL_DAMAGE, 1.0, var1, var2);
      double var21 = (var0.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, var1, var2) - 1.0) / 2.0 + 1.0;
      double var23 = var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, var1, var2);
      double var25 = var0.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0, var1, var2) * 6.1 * 77.0 / var5;
      double var27 = var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, var1, var2);
      double var29 = calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
      double var31 = calcAttributeBonus(var0, var1, var2);
      double var33 = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? var0.getRandomDamageMultiplier() : 1.0;
      double var35 = 1.0;
      if (var1.isAttackable() && !var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
         Player var37 = var0.asPlayer();
         if (var37 != null && var1.getLevel() - var37.getLevel() >= 2) {
            int var38 = var1.getLevel() - var37.getLevel() - 1;
            if (var38 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               var35 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               var35 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var38];
            }
         }
      }

      var9 = var17 * var19 * var21 * var23 * var11 * var15 + var25 + var27;
      var9 *= var29;
      var9 *= var31;
      var9 *= var33;
      var9 *= var35;
      return Math.max(var9, 1.0);
   }

   public static double calcPhysDam(Creature var0, Creature var1, Skill var2, byte var3, boolean var4, boolean var5) {
      boolean var6 = var0.isPlayable() && var1.isPlayable();
      boolean var7 = var0.isPlayable() && var1.isAttackable();
      double var8 = var0.isBehind(var1) ? 1.2 : (var0.isInFrontOf(var1) ? 1.0 : 1.1);
      double var10 = var0.getPAtk(var1);
      double var12 = var1.getPDef(var0);
      if (var6) {
         var12 *= var2 == null ? var1.calcStat(Stat.PVP_PHYSICAL_DEF, 1.0, null, null) : var1.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         var12 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
      }

      switch (var3) {
         case 1:
            if (!PlayerConfig.ALT_GAME_SHIELD_BLOCKS) {
               var12 += var1.getShldDef();
            }
         default:
            int var14 = var5 ? 2 : 1;
            var10 = var2 != null ? var10 * var14 + var2.getPower(var0, var1, var6, var7) : var10 * var14;
            if (var4) {
               var10 = 2.0
                  * var0.calcStat(Stat.CRITICAL_DAMAGE, 1.0, var1, var2)
                  * var0.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, var1, var2)
                  * var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, var1, null)
                  * (76.0 * var10 * var8 / var12);
               var10 += var0.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0, var1, var2) * 77.0 / var12;
               var10 += var1.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, var1, var2);
               if (var2 == null) {
                  if (var6) {
                     var10 *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
                  } else if (var7) {
                     var10 *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
                  }
               } else if (var6) {
                  var10 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
               } else if (var7) {
                  var10 *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
               }
            } else {
               var10 = 76.0 * var10 * var8 / var12;
            }

            var10 *= calcAttackTraitBonus(var0, var1);
            if (var2 == null && PlayerConfig.RANDOMIZE_AUTO_ATTACK_DAMAGE || var2 != null && PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE) {
               var10 *= var0.getRandomDamageMultiplier();
            }

            var10 *= var0.getRandomDamageMultiplier();
            if (var3 > 0 && PlayerConfig.ALT_GAME_SHIELD_BLOCKS) {
               var10 -= var1.getShldDef();
               if (var10 < 0.0) {
                  var10 = 0.0;
               }
            }

            if (var10 > 0.0 && var10 < 1.0) {
               var10 = 1.0;
            } else if (var10 < 0.0) {
               var10 = 0.0;
            }

            if (var6) {
               if (var2 == null) {
                  var10 *= var0.calcStat(Stat.PVP_PHYSICAL_DMG, 1.0, null, null);
                  var10 *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
               } else {
                  var10 *= var0.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
                  var10 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
               }
            }

            if (var2 != null) {
               var10 = var0.calcStat(Stat.PHYSICAL_SKILL_POWER, var10, null, null);
            }

            var10 *= calcAttributeBonus(var0, var1, var2);
            if (var1.isAttackable()) {
               Weapon var15 = var0.getActiveWeaponItem();
               if (var15 != null && (var15.getItemType() == WeaponType.BOW || var15.getItemType() == WeaponType.CROSSBOW)) {
                  if (var2 != null) {
                     var10 *= var0.calcStat(Stat.PVE_BOW_SKILL_DMG, 1.0, null, null);
                  } else {
                     var10 *= var0.calcStat(Stat.PVE_BOW_DMG, 1.0, null, null);
                  }
               } else {
                  var10 *= var0.calcStat(Stat.PVE_PHYSICAL_DMG, 1.0, null, null);
                  if (var0.isPlayable()) {
                     if (var2 == null) {
                        var10 *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
                     } else {
                        var10 *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
                     }
                  }
               }

               if (!var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
                  Player var16 = var0.asPlayer();
                  if (var16 != null && var1.getLevel() - var16.getLevel() >= 2) {
                     int var17 = var1.getLevel() - var16.getLevel() - 1;
                     if (var2 != null) {
                        if (var17 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                           var10 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
                        } else {
                           var10 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var17];
                        }
                     } else if (var4) {
                        if (var17 >= NpcConfig.NPC_CRIT_DMG_PENALTY.length) {
                           var10 *= NpcConfig.NPC_CRIT_DMG_PENALTY[NpcConfig.NPC_CRIT_DMG_PENALTY.length - 1];
                        } else {
                           var10 *= NpcConfig.NPC_CRIT_DMG_PENALTY[var17];
                        }
                     } else if (var17 >= NpcConfig.NPC_DMG_PENALTY.length) {
                        var10 *= NpcConfig.NPC_DMG_PENALTY[NpcConfig.NPC_DMG_PENALTY.length - 1];
                     } else {
                        var10 *= NpcConfig.NPC_DMG_PENALTY[var17];
                     }
                  }
               }
            }

            return var10;
         case 2:
            return 1.0;
      }
   }

   public static double calcMagicDam(Creature var0, Creature var1, Skill var2, byte var3, boolean var4, boolean var5, boolean var6) {
      double var7 = var1.getMDef(var0, var2);
      switch (var3) {
         case 1:
            var7 += var1.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      double var9 = var0.getMAtk(var1, var2);
      boolean var11 = var0.isPlayable() && var1.isPlayable();
      boolean var12 = var0.isPlayable() && var1.isAttackable();
      if (var11) {
         if (var2.isMagic()) {
            var7 *= var1.calcStat(Stat.PVP_MAGICAL_DEF, 1.0, null, null);
            var7 *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
         } else {
            var7 *= var1.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
            var7 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[var1.asPlayer().getPlayerClass().getId()];
         }
      }

      var9 *= var5 ? 4.0 : (var4 ? 2.0 : 1.0);
      double var13 = 91.0 * Math.sqrt(var9) / var7 * var2.getPower(var0, var1, var11, var12);
      if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(var0, var1, var2)) {
         if (var0.isPlayer()) {
            if (calcMagicSuccess(var0, var1, var2) && var1.getLevel() - var0.getLevel() <= 9) {
               if (var2.hasEffectType(EffectType.HP_DRAIN, new EffectType[0])) {
                  var0.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
               } else {
                  var0.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
               }

               var13 /= 2.0;
            } else {
               SystemMessage var15 = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
               var15.addString(var1.getName());
               var15.addSkillName(var2);
               var0.sendPacket(var15);
               var13 = 1.0;
            }
         }

         if (var1.isPlayer()) {
            SystemMessage var23 = var2.hasEffectType(EffectType.HP_DRAIN, new EffectType[0])
               ? new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN)
               : new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
            var23.addString(var0.getName());
            var1.sendPacket(var23);
         }
      } else if (var6) {
         var13 *= var0.isPlayer() && var1.isPlayer() ? 2.5 : 3.0;
         var13 *= var0.calcStat(Stat.MAGIC_CRIT_DMG, 1.0, null, null);
         if (var0.isPlayable()) {
            var13 *= var1.isPlayable()
               ? ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()]
               : ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         }
      }

      if (PlayerConfig.RANDOMIZE_MAGICAL_SKILL_DAMAGE) {
         var13 *= var0.getRandomDamageMultiplier();
      }

      if (var11) {
         Stat var24;
         if (var2.isMagic()) {
            var24 = Stat.PVP_MAGICAL_DMG;
            var13 *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         } else {
            var24 = Stat.PVP_PHYS_SKILL_DMG;
            var13 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         }

         var13 *= var0.calcStat(var24, 1.0, null, null);
      }

      var13 *= calcAttributeBonus(var0, var1, var2);
      if (var1.isAttackable()) {
         var13 *= var0.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
         Player var25 = var0.asPlayer();
         if (var25 != null) {
            var13 *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[var25.getPlayerClass().getId()];
            if (!var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY && var1.getLevel() - var25.getLevel() >= 2) {
               int var16 = var1.getLevel() - var25.getLevel() - 1;
               if (var16 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                  var13 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
               } else {
                  var13 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var16];
               }
            }
         }
      }

      return var13;
   }

   public static double calcMagicDam(Cubic var0, Creature var1, Skill var2, boolean var3, byte var4) {
      double var5 = var1.getMDef(var0.getOwner(), var2);
      switch (var4) {
         case 1:
            var5 += var1.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      int var7 = var0.getCubicPower();
      boolean var8 = var1.isPlayable();
      boolean var9 = var1.isAttackable();
      double var10 = 91.0 * ((var7 + var2.getPower(var8, var9)) / var5);
      Player var12 = var0.getOwner();
      if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(var12, var1, var2)) {
         if (calcMagicSuccess(var12, var1, var2) && var1.getLevel() - var2.getMagicLevel() <= 9) {
            if (var2.hasEffectType(EffectType.HP_DRAIN, new EffectType[0])) {
               var12.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
            } else {
               var12.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
            }

            var10 /= 2.0;
         } else {
            SystemMessage var13 = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
            var13.addString(var1.getName());
            var13.addSkillName(var2);
            var12.sendPacket(var13);
            var10 = 1.0;
         }

         if (var1.isPlayer()) {
            if (var2.hasEffectType(EffectType.HP_DRAIN, new EffectType[0])) {
               SystemMessage var16 = new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN);
               var16.addString(var12.getName());
               var1.sendPacket(var16);
            } else {
               SystemMessage var17 = new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
               var17.addString(var12.getName());
               var1.sendPacket(var17);
            }
         }
      } else if (var3) {
         var10 *= 3.0;
      }

      var10 *= calcAttributeBonus(var12, var1, var2);
      if (var1.isAttackable()) {
         var10 *= var12.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
         var10 *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[var12.getPlayerClass().getId()];
         if (!var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY && var1.getLevel() - var12.getLevel() >= 2) {
            int var18 = var1.getLevel() - var0.getOwner().getLevel() - 1;
            if (var18 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               var10 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               var10 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var18];
            }
         }
      }

      return var10;
   }

   public static boolean calcCrit(Creature var0, Creature var1) {
      return calcCrit(var0, var1, null);
   }

   public static boolean calcCrit(Creature var0, Creature var1, Skill var2) {
      double var3;
      if (var2 != null) {
         var3 = var2.getBaseCritRate() * 10 * BaseStat.STR.calcBonus(var0);
      } else {
         var3 = var0.getStat().calcStat(Stat.CRITICAL_RATE_POS, var0.getStat().getCriticalHit(var1, null), var1, var2);
      }

      boolean var5 = var0.isPlayable() && var1.isPlayable();
      if (var2 == null) {
         if (var5) {
            var3 *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         } else if (var0.isPlayable() && var1.isAttackable()) {
            var3 *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         }
      } else if (var5) {
         var3 *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
      } else if (var0.isPlayable() && var1.isAttackable()) {
         var3 *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
      }

      return var1.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE, var3, null, null) + var1.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE_ADD, 0.0, null, null)
         > Rnd.get(1000);
   }

   public static boolean calcMCrit(double var0) {
      return var0 > Rnd.get(1000);
   }

   public static boolean calcAtkBreak(Creature var0, double var1) {
      if (var0.isChanneling()) {
         return false;
      } else {
         double var3 = 0.0;
         if (PlayerConfig.ALT_GAME_CANCEL_CAST && var0.isCastingNow()) {
            var3 = 15.0;
         }

         if (PlayerConfig.ALT_GAME_CANCEL_BOW && var0.isAttackingNow()) {
            Weapon var5 = var0.getActiveWeaponItem();
            if (var5 != null && var5.getItemType() == WeaponType.BOW) {
               var3 = 15.0;
            }
         }

         if (!var0.isRaid() && !var0.isInvul() && !(var3 <= 0.0)) {
            var3 += Math.sqrt(13.0 * var1);
            var3 -= BaseStat.MEN.calcBonus(var0) * 100.0 - 100.0;
            double var9 = var0.calcStat(Stat.ATTACK_CANCEL, var3, null, null);
            var9 = Math.clamp(var9, 1.0, 99.0);
            return Rnd.get(100) < var9;
         } else {
            return false;
         }
      }
   }

   public static int calcPAtkSpd(Creature var0, Creature var1, double var2) {
      return var2 < 2.0 ? 2700 : (int)(470000.0 / var2);
   }

   public static int calcAtkSpd(Creature var0, Skill var1, double var2) {
      return var1.isMagic() ? (int)(var2 / var0.getMAtkSpd() * 333.0) : (int)(var2 / var0.getPAtkSpd() * 300.0);
   }

   public static boolean calcHitMiss(Creature var0, Creature var1) {
      int var2 = (80 + 2 * (var0.getAccuracy() - var1.getEvasionRate(var0))) * 10;
      var2 = (int)(var2 * HitConditionBonusData.getInstance().getConditionBonus(var0, var1));
      var2 = Math.max(var2, 200);
      var2 = Math.min(var2, 980);
      return var2 < Rnd.get(1000);
   }

   public static byte calcShldUse(Creature var0, Creature var1, Skill var2, boolean var3) {
      if (var2 != null && var2.ignoreShield()) {
         return 0;
      } else {
         ItemTemplate var4 = var1.getSecondaryWeaponItem();
         if (var4 != null && var4 instanceof Armor && ((Armor)var4).getItemType() != ArmorType.SIGIL) {
            double var5 = var1.calcStat(Stat.SHIELD_RATE, 0.0, var0, null) * BaseStat.DEX.calcBonus(var1);
            if (var5 <= 1.0E-6) {
               return 0;
            } else {
               int var7 = (int)var1.calcStat(Stat.SHIELD_DEFENCE_ANGLE, 0.0, null, null) + 120;
               if (var7 < 360 && Math.abs(var1.calculateDirectionTo(var0) - LocationUtil.convertHeadingToDegree(var1.getHeading())) > var7 / 2) {
                  return 0;
               } else {
                  byte var8 = 0;
                  Weapon var9 = var0.getActiveWeaponItem();
                  if (var9 != null && var9.getItemType() == WeaponType.BOW) {
                     var5 *= 1.3;
                  }

                  if (var5 > 0.0 && 100 - PlayerConfig.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100)) {
                     var8 = 2;
                  } else if (var5 > Rnd.get(100)) {
                     var8 = 1;
                  }

                  if (var3 && var1.isPlayer()) {
                     Player var10 = var1.asPlayer();
                     switch (var8) {
                        case 1:
                           var10.sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
                           break;
                        case 2:
                           var10.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
                     }
                  }

                  return var8;
               }
            }
         } else {
            return 0;
         }
      }
   }

   public static byte calcShldUse(Creature var0, Creature var1, Skill var2) {
      return calcShldUse(var0, var1, var2, true);
   }

   public static byte calcShldUse(Creature var0, Creature var1) {
      return calcShldUse(var0, var1, null, true);
   }

   public static boolean calcMagicAffected(Creature var0, Creature var1, Skill var2) {
      double var3 = 0.0;
      if (var2.isActive() && var2.hasNegativeEffect()) {
         var3 = var1.getMDef(var0, var2);
      }

      double var5 = 2.0 * var0.getMAtk(var1, var2) * calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
      double var7 = (var5 - var3) / (var5 + var3);
      if (var2.isDebuff() && var1.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, null, var2) > 0.0) {
         return false;
      } else {
         var7 += 0.5 * Rnd.nextGaussian();
         return var7 > 0.0;
      }
   }

   public static double calcLvlBonusMod(Creature var0, Creature var1, Skill var2) {
      int var3 = var2.getMagicLevel() > 0 ? var2.getMagicLevel() : var0.getLevel();
      double var4 = 1.0 + var2.getLvlBonusRate() / 100.0;
      double var6 = 1.0 + (var3 - var1.getLevel()) / 100.0;
      return var4 * var6;
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static boolean calcEffectSuccess(Creature var0, Creature var1, Skill var2) {
      if (!var1.isDoor() && !(var1 instanceof SiegeFlag) && !(var1 instanceof StaticObject)) {
         if (var2.isDebuff() && var1.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, var0, var2) > 0.0) {
            SystemMessage var22 = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
            var22.addString(var1.getName());
            var22.addSkillName(var2);
            var0.sendPacket(var22);
            return false;
         } else if (!var2.isDebuff() || !var0.isPlayer() || !FIXED_DEBUFF_RATE_PLAYERS.contains(var0.getObjectId())) {
            int var21 = var2.getActivateRate();
            if (var21 != -1 && var21 <= 99 && var2.getBasicProperty() != BaseStat.NONE) {
               int var4 = var2.getMagicLevel();
               if (var4 <= -1) {
                  var4 = var1.getLevel() + 3;
               }

               int var5 = 0;
               switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$stats$BaseStat[var2.getBasicProperty().ordinal()]) {
                  case 1:
                     var5 = var1.getSTR();
                     break;
                  case 2:
                     var5 = var1.getDEX();
                     break;
                  case 3:
                     var5 = var1.getCON();
                     break;
                  case 4:
                     var5 = var1.getINT();
                     break;
                  case 5:
                     var5 = var1.getMEN();
                     break;
                  case 6:
                     var5 = var1.getWIT();
               }

               double var6 = (var4 - var1.getLevel() + 3) * var2.getLvlBonusRate() + var21 + 30.0 - var5;
               double var8 = calcAttributeBonus(var0, var1, var2);
               double var10 = calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
               double var12 = 1.0 + var1.calcStat(var2.isDebuff() ? Stat.DEBUFF_VULN : Stat.BUFF_VULN, 1.0, null, null) / 100.0;
               double var14 = 1.0;
               if (var2.isMagic()) {
                  double var16 = var0.getMAtk(null, null);
                  double var18 = 0.0;
                  if (var0.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)) {
                     var18 = var16 * 3.0;
                  }

                  var18 += var16;
                  var18 = Math.sqrt(var18) / var1.getMDef(null, null) * 11.0;
                  var14 = var18;
               }

               double var23 = var6 * var8 * var10 * var14 * var12;
               double var26 = var10 > 0.0 ? Math.clamp(var23, (double)var2.getMinChance(), (double)var2.getMaxChance()) : 0.0;
               if (var26 <= Rnd.get(100)) {
                  SystemMessage var20 = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
                  var20.addString(var1.getName());
                  var20.addSkillName(var2);
                  var0.sendPacket(var20);
                  return false;
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else if (Rnd.get(100) < FIXED_DEBUFF_RATE_PERCENT) {
            return true;
         } else {
            SystemMessage var3 = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
            var3.addString(var1.getName());
            var3.addSkillName(var2);
            var0.sendPacket(var3);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean calcCubicSkillSuccess(Cubic var0, Creature var1, Skill var2, byte var3) {
      if (var2.isDebuff()) {
         if (var2.getPower() == -1.0) {
            return true;
         }

         if (var1.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, null, var2) > 0.0) {
            return false;
         }
      }

      if (var3 == 2) {
         return false;
      } else if (calcBuffDebuffReflection(var1, var2)) {
         return false;
      } else {
         int var4 = var2.getActivateRate();
         if (var4 != -1 && var4 <= 99 && var2.getBasicProperty() != BaseStat.NONE) {
            double var5 = var4 > 0 ? var4 : var2.getPower();
            double var7 = var2.getBasicProperty().calcBonus(var1);
            double var9 = var5 / var7;
            double var11 = calcGeneralTraitBonus(var0.getOwner(), var1, var2.getTraitType(), false);
            var9 *= var11;
            double var13 = calcLvlBonusMod(var0.getOwner(), var1, var2);
            var9 *= var13;
            double var15 = calcAttributeBonus(var0.getOwner(), var1, var2);
            var9 *= var15;
            double var17 = Math.clamp(var9, (double)var2.getMinChance(), (double)var2.getMaxChance());
            return Rnd.get(100) < var17;
         } else {
            return true;
         }
      }
   }

   public static boolean calcMagicSuccess(Creature var0, Creature var1, Skill var2) {
      if (var2.getPower() == -1.0) {
         return true;
      } else {
         double var3 = MathUtil.pow(
            1.3,
            var1.getLevel() - (PlayerConfig.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL && var2.getMagicLevel() > 0 ? var2.getMagicLevel() : var0.getLevel())
         );
         float var5 = 1.0F;
         if (var1.isAttackable() && !var1.isRaid() && !var1.isRaidMinion() && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_MAGIC_PENALTY) {
            Player var6 = var0.asPlayer();
            if (var6 != null && var1.getLevel() - var6.getLevel() >= 3) {
               int var7 = var1.getLevel() - var6.getLevel() - 2;
               if (var7 >= NpcConfig.NPC_SKILL_CHANCE_PENALTY.length) {
                  var5 = NpcConfig.NPC_SKILL_CHANCE_PENALTY[NpcConfig.NPC_SKILL_CHANCE_PENALTY.length - 1];
               } else {
                  var5 = NpcConfig.NPC_SKILL_CHANCE_PENALTY[var7];
               }
            }
         }

         double var9 = var1.calcStat(Stat.MAGIC_SUCCESS_RES, 1.0, null, var2);
         int var8 = 100 - Math.round((float)(var3 * var5 * var9));
         return Rnd.get(100) < var8;
      }
   }

   public static double calcManaDam(Creature var0, Creature var1, Skill var2, byte var3, boolean var4, boolean var5, boolean var6) {
      double var7 = var0.getMAtk(var1, var2);
      double var9 = var1.getMDef(var0, var2);
      boolean var11 = var0.isPlayable() && var1.isPlayable();
      boolean var12 = var0.isPlayable() && var1.isAttackable();
      double var13 = var1.getMaxMp();
      switch (var3) {
         case 1:
            var9 += var1.getShldDef();
         default:
            var7 *= var5 ? 4.0 : (var4 ? 2.0 : 1.0);
            double var15 = Math.sqrt(var7) * var2.getPower(var0, var1, var11, var12) * (var13 / 97.0) / var9;
            var15 *= calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
            if (var1.isAttackable()) {
               var15 *= var0.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
               Player var17 = var0.asPlayer();
               if (var17 != null) {
                  var15 *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[var17.getPlayerClass().getId()];
                  if (!var1.isRaid()
                     && !var1.isRaidMinion()
                     && var1.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY
                     && var1.getLevel() - var17.getLevel() >= 2) {
                     int var18 = var1.getLevel() - var17.getLevel() - 1;
                     if (var18 >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                        var15 *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
                     } else {
                        var15 *= NpcConfig.NPC_SKILL_DMG_PENALTY[var18];
                     }
                  }
               }
            }

            if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(var0, var1, var2)) {
               if (var0.isPlayer()) {
                  SystemMessage var21 = new SystemMessage(SystemMessageId.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2_S_MAGIC);
                  var21.addString(var1.getName());
                  var21.addString(var0.getName());
                  var0.sendPacket(var21);
                  var15 /= 2.0;
               }

               if (var1.isPlayer()) {
                  SystemMessage var22 = new SystemMessage(SystemMessageId.C1_WEAKLY_RESISTED_C2_S_MAGIC);
                  var22.addString(var1.getName());
                  var22.addString(var0.getName());
                  var1.sendPacket(var22);
               }
            }

            if (var6) {
               var15 *= 3.0;
               var0.sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
            }

            return var15;
         case 2:
            return 1.0;
      }
   }

   public static double calculateSkillResurrectRestorePercent(double var0, Creature var2) {
      if (var0 != 0.0 && var0 != 100.0) {
         double var3 = var0 * BaseStat.WIT.calcBonus(var2);
         if (var3 - var0 > 20.0) {
            var3 += 20.0;
         }

         var3 = Math.max(var3, var0);
         return Math.min(var3, 90.0);
      } else {
         return var0;
      }
   }

   public static boolean calcPhysicalSkillEvasion(Creature var0, Creature var1, Skill var2) {
      if (!var2.isMagic() && !var2.isDebuff()) {
         if (Rnd.get(100) < var1.calcStat(Stat.P_SKILL_EVASION, 0.0, null, var2)) {
            if (var0.isPlayer()) {
               SystemMessage var3 = new SystemMessage(SystemMessageId.C1_DODGES_THE_ATTACK);
               var3.addString(var1.getName());
               var0.asPlayer().sendPacket(var3);
            }

            if (var1.isPlayer()) {
               SystemMessage var4 = new SystemMessage(SystemMessageId.YOU_HAVE_AVOIDED_C1_S_ATTACK_2);
               var4.addString(var0.getName());
               var1.asPlayer().sendPacket(var4);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean calcSkillMastery(Creature var0, Skill var1) {
      if (!var0.isPlayer()) {
         return false;
      } else if (var1.isStatic()) {
         return false;
      } else {
         int var2 = (int)var0.getStat().calcStat(Stat.SKILL_CRITICAL, 0.0, null, null);
         if (var2 == 0) {
            return false;
         } else {
            double var3 = 0.0;
            switch (var2) {
               case 1:
                  var3 = BaseStat.STR.calcBonus(var0);
                  break;
               case 4:
                  var3 = BaseStat.INT.calcBonus(var0);
            }

            var3 *= var0.getStat().calcStat(Stat.SKILL_CRITICAL_PROBABILITY, 1.0, null, null);
            return Rnd.get(100) < var3 * ClassBalanceConfig.SKILL_MASTERY_CHANCE_MULTIPLIERS[var0.asPlayer().getPlayerClass().getId()];
         }
      }
   }

   public static double calcAttributeBonus(Creature var0, Creature var1, Skill var2) {
      int var3;
      int var4;
      if (var2 != null && var2.getElement() != -1) {
         var3 = var0.getAttackElementValue(var2.getElement()) + var2.getElementPower();
         var4 = var1.getDefenseElementValue(var2.getElement());
      } else {
         var3 = var0.getAttackElementValue(var0.getAttackElement());
         var4 = var1.getDefenseElementValue(var0.getAttackElement());
      }

      int var5 = var3 - var4;
      if (var5 > 0) {
         return Math.min(1.025 + Math.sqrt(MathUtil.pow(var5, 3) / 2.0) * 1.0E-4, 1.25);
      } else {
         return var5 < 0 ? Math.max(0.975 - Math.sqrt(MathUtil.pow(-var5, 3) / 2.0) * 1.0E-4, 0.75) : 1.0;
      }
   }

   public static void calcDamageReflected(Creature var0, Creature var1, Skill var2, boolean var3) {
      if (!var2.isMagic() && var2.getCastRange() <= 40) {
         double var4 = var1.calcStat(Stat.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0.0, var1, var2);
         if (Rnd.get(100) < var4) {
            if (var1.isPlayer()) {
               SystemMessage var6 = new SystemMessage(SystemMessageId.YOU_COUNTERED_C1_S_ATTACK);
               var6.addString(var0.getName());
               var1.sendPacket(var6);
            }

            if (var0.isPlayer()) {
               SystemMessage var8 = new SystemMessage(SystemMessageId.C1_IS_PERFORMING_A_COUNTERATTACK);
               var8.addString(var1.getName());
               var0.sendPacket(var8);
            }

            double var9 = var1.getPAtk(var0) * 10.0 * 70.0 / var0.getPDef(var1);
            var9 *= calcWeaponTraitBonus(var0, var1);
            var9 *= calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
            var9 *= calcAttributeBonus(var0, var1, var2);
            var0.reduceCurrentHp(var9, var1, var2);
            if (var3) {
               var0.reduceCurrentHp(var9, var1, var2);
            }
         }
      }
   }

   public static boolean calcBuffDebuffReflection(Creature var0, Skill var1) {
      if (var1.isDebuff() && var1.getActivateRate() != -1) {
         double var2 = var0.calcStat(var1.isMagic() ? Stat.REFLECT_SKILL_MAGIC : Stat.REFLECT_SKILL_PHYSIC, 0.0, null, var1);
         return var2 > Rnd.get(100);
      } else {
         return false;
      }
   }

   public static double calcFallDam(Creature var0, int var1) {
      return GeneralConfig.ENABLE_FALLING_DAMAGE && var1 >= 0 ? var0.calcStat(Stat.FALL, var1 * var0.getMaxHp() / 1000, null, null) : 0.0;
   }

   public static boolean calcBlowSuccess(Creature var0, Creature var1, Skill var2) {
      double var3 = BaseStat.DEX.calcBonus(var0);
      double var5 = var2.getBlowChance();
      double var7 = var0.isInFrontOf(var1) ? 1.0 : (var0.isBehind(var1) ? 2.0 : 1.5);
      double var9 = var5 * var3 * var7;
      double var11 = var0.calcStat(Stat.BLOW_RATE, var9, var1, null);
      return Rnd.get(100) < var11;
   }

   public static List<BuffInfo> calcStealEffects(Creature var0, int var1) {
      ArrayList var2 = new ArrayList(var1);
      ArrayList var3 = var0.getEffectList().hasBuffs() ? new ArrayList(var0.getEffectList().getBuffs()) : new ArrayList(1);
      if (var0.getEffectList().hasDances()) {
         var3.addAll(var0.getEffectList().getDances());
      }

      for (int var4 = var3.size() - 1; var4 >= 0 && var2.size() < var1; var4--) {
         BuffInfo var5 = (BuffInfo)var3.get(var4);
         if (var5.getSkill().canBeStolen()) {
            var2.add(var5);
         }
      }

      return var2;
   }

   public static List<BuffInfo> calcCancelEffects(Creature var0, Creature var1, Skill var2, String var3, int var4, int var5) {
      ArrayList var6 = new ArrayList(var5);
      switch (var3) {
         case "buff":
            int var9 = var2 == null ? var0.getLevel() : var2.getMagicLevel();
            double var10 = var1.calcStat(Stat.CANCEL_VULN, 0.0, var1, null);
            double var12 = var0.calcStat(Stat.CANCEL_PROF, 0.0, var1, null);
            double var14 = 1.0 + (var10 + var12) * -1.0 / 100.0;
            double var16 = var4 / var14;
            ArrayList var18 = var1.getEffectList().hasBuffs() ? new ArrayList(var1.getEffectList().getBuffs()) : new ArrayList(1);
            if (var1.getEffectList().hasDances()) {
               var18.addAll(var1.getEffectList().getDances());
            }

            for (int var22 = var18.size() - 1; var22 >= 0; var22--) {
               BuffInfo var23 = (BuffInfo)var18.get(var22);
               if (var23.getSkill().canBeStolen() && calcCancelSuccess(var23, var9, (int)var16, var2)) {
                  var6.add(var23);
                  if (var6.size() >= var5) {
                     return var6;
                  }
               }
            }
            break;
         case "debuff":
            ArrayList var19 = new ArrayList(var1.getEffectList().getDebuffs());

            for (int var20 = var19.size() - 1; var20 >= 0; var20--) {
               BuffInfo var21 = (BuffInfo)var19.get(var20);
               if (var21.getSkill().isDebuff() && var21.getSkill().canBeDispeled() && Rnd.get(100) <= var4) {
                  var6.add(var21);
                  if (var6.size() >= var5) {
                     break;
                  }
               }
            }
      }

      return var6;
   }

   public static boolean calcCancelSuccess(BuffInfo var0, int var1, int var2, Skill var3) {
      int var4 = (int)(var2 * (var0.getSkill().getMagicLevel() > 0 ? 1.0 + (var1 - var0.getSkill().getMagicLevel()) / 100.0 : 1.0));
      return Rnd.get(100) < Math.clamp((long)var4, var3.getMinChance(), var3.getMaxChance());
   }

   public static int calcEffectAbnormalTime(Creature var0, Creature var1, Skill var2) {
      int var3 = !var2.isPassive() && !var2.isToggle() ? var2.getAbnormalTime() : -1;
      if (var1 != null && var1.isServitor() && var2.isAbnormalInstant()) {
         var3 /= 2;
      }

      if (calcSkillMastery(var0, var2)) {
         var3 *= 2;
      }

      if (var0 != null && var1 != null && var2.isDebuff() && GeneralConfig.DEBUFF_DURATION_USES_RESISTS && var3 > 0) {
         double var4 = var2.getBasicProperty().calcBonus(var1);
         double var6 = calcGeneralTraitBonus(var0, var1, var2.getTraitType(), false);
         double var8 = calcLvlBonusMod(var0, var1, var2);
         double var10 = calcAttributeBonus(var0, var1, var2);
         var3 = (int)Math.ceil(Math.clamp(var3 * var6 * var8 * var10 / var4, var3 * 0.5, (double)var3));
      }

      return var3;
   }

   public static boolean calcProbability(double var0, Creature var2, Creature var3, Skill var4) {
      return Rnd.get(100)
         < (var4.getMagicLevel() + var0 - var3.getLevel() + 30.0 - var3.getINT())
            * calcAttributeBonus(var2, var3, var4)
            * calcGeneralTraitBonus(var2, var3, var4.getTraitType(), false);
   }

   public static int calculateKarmaLost(Player var0, long var1) {
      double var3 = KarmaLossData.getInstance().getModifier(var0.getLevel());
      return var1 > 0L ? (int)(Math.abs((float)var1 / RatesConfig.RATE_KARMA_LOST) / var3 / 30.0) : (int)(Math.abs(var1) / var3 / 30.0);
   }

   public static int calculateKarmaGain(int var0, boolean var1) {
      int var2 = 43200;
      if (var1) {
         var2 = (int)((var0 * 0.375 + 1.0) * 60.0 * 4.0) - 150;
         if (var2 > 10800) {
            return 10800;
         }
      }

      if (var0 < 99) {
         var2 = (int)((var0 * 0.5 + 1.0) * 60.0 * 12.0);
      } else if (var0 < 180) {
         var2 = (int)((var0 * 0.125 + 37.75) * 60.0 * 12.0);
      }

      return var2;
   }

   public static double calcGeneralTraitBonus(Creature var0, Creature var1, TraitType var2, boolean var3) {
      if (var2 == TraitType.NONE) {
         return 1.0;
      } else if (var1.getStat().isTraitInvul(var2)) {
         return 0.0;
      } else {
         switch (var2.getType()) {
            case 2:
               if (!var0.getStat().hasAttackTrait(var2) || !var1.getStat().hasDefenceTrait(var2)) {
                  return 1.0;
               }
               break;
            case 3:
               if (var3) {
                  return 1.0;
               }
               break;
            default:
               return 1.0;
         }

         double var4 = var0.getStat().getAttackTrait(var2) - var1.getStat().getDefenceTrait(var2) + 1.0;
         return Math.clamp(var4, 0.05, 2.0);
      }
   }

   public static double calcWeaponTraitBonus(Creature var0, Creature var1) {
      TraitType var2 = var0.getAttackType().getTraitType();
      double var3 = var1.getStat().getDefenceTraits()[var2.ordinal()] - 1.0;
      return 1.0 - var3;
   }

   public static double calcAttackTraitBonus(Creature var0, Creature var1) {
      double var2 = calcWeaponTraitBonus(var0, var1);
      if (var2 == 0.0) {
         return 0.0;
      } else {
         double var4 = 1.0;

         for (TraitType var9 : TraitType.values()) {
            if (var9.getType() == 2) {
               var4 *= calcGeneralTraitBonus(var0, var1, var9, true);
               if (var4 == 0.0) {
                  return 0.0;
               }
            }
         }

         return Math.clamp(var2 * var4, 0.05, 2.0);
      }
   }
}
