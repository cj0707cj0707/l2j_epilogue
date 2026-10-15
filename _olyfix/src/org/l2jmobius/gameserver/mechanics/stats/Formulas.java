package org.l2jmobius.gameserver.mechanics.stats;

import java.util.ArrayList;
import java.util.List;
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

   public static int getRegeneratePeriod(Creature creature) {
      return creature.isDoor() ? 300000 : 3000;
   }

   public static Calculator[] getStdNPCCalculators() {
      Calculator[] std = new Calculator[Stat.NUM_STATS];
      std[Stat.MAX_HP.ordinal()] = new Calculator();
      std[Stat.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());
      std[Stat.MAX_MP.ordinal()] = new Calculator();
      std[Stat.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());
      std[Stat.POWER_ATTACK.ordinal()] = new Calculator();
      std[Stat.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());
      std[Stat.MAGIC_ATTACK.ordinal()] = new Calculator();
      std[Stat.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());
      std[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
      std[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());
      std[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
      std[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());
      std[Stat.CRITICAL_RATE.ordinal()] = new Calculator();
      std[Stat.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());
      std[Stat.MCRITICAL_RATE.ordinal()] = new Calculator();
      std[Stat.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());
      std[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
      std[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
      std[Stat.EVASION_RATE.ordinal()] = new Calculator();
      std[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
      std[Stat.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
      std[Stat.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());
      std[Stat.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
      std[Stat.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());
      std[Stat.MOVE_SPEED.ordinal()] = new Calculator();
      std[Stat.MOVE_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());
      return std;
   }

   public static Calculator[] getStdDoorCalculators() {
      Calculator[] std = new Calculator[Stat.NUM_STATS];
      std[Stat.ACCURACY_COMBAT.ordinal()] = new Calculator();
      std[Stat.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
      std[Stat.EVASION_RATE.ordinal()] = new Calculator();
      std[Stat.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
      std[Stat.POWER_DEFENCE.ordinal()] = new Calculator();
      std[Stat.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
      std[Stat.MAGIC_DEFENCE.ordinal()] = new Calculator();
      std[Stat.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
      return std;
   }

   public static void addFuncsToNewCharacter(Creature creature) {
      if (creature.isPlayer()) {
         creature.addStatFunc(FuncMaxHpMul.getInstance());
         creature.addStatFunc(FuncMaxCpMul.getInstance());
         creature.addStatFunc(FuncMaxMpMul.getInstance());
         creature.addStatFunc(FuncPAtkMod.getInstance());
         creature.addStatFunc(FuncMAtkMod.getInstance());
         creature.addStatFunc(FuncPDefMod.getInstance());
         creature.addStatFunc(FuncMDefMod.getInstance());
         creature.addStatFunc(FuncAtkCritical.getInstance());
         creature.addStatFunc(FuncMAtkCritical.getInstance());
         creature.addStatFunc(FuncAtkAccuracy.getInstance());
         creature.addStatFunc(FuncAtkEvasion.getInstance());
         creature.addStatFunc(FuncPAtkSpeed.getInstance());
         creature.addStatFunc(FuncMAtkSpeed.getInstance());
         creature.addStatFunc(FuncMoveSpeed.getInstance());
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_STR));
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_DEX));
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_INT));
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_MEN));
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_CON));
         creature.addStatFunc(FuncHenna.getInstance(Stat.STAT_WIT));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_STR));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_DEX));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_INT));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_MEN));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_CON));
         creature.addStatFunc(FuncArmorSet.getInstance(Stat.STAT_WIT));
      } else if (creature.isSummon()) {
         creature.addStatFunc(FuncMaxHpMul.getInstance());
         creature.addStatFunc(FuncMaxMpMul.getInstance());
         creature.addStatFunc(FuncPAtkMod.getInstance());
         creature.addStatFunc(FuncMAtkMod.getInstance());
         creature.addStatFunc(FuncPDefMod.getInstance());
         creature.addStatFunc(FuncMDefMod.getInstance());
         creature.addStatFunc(FuncAtkCritical.getInstance());
         creature.addStatFunc(FuncMAtkCritical.getInstance());
         creature.addStatFunc(FuncAtkAccuracy.getInstance());
         creature.addStatFunc(FuncAtkEvasion.getInstance());
         creature.addStatFunc(FuncMoveSpeed.getInstance());
         creature.addStatFunc(FuncPAtkSpeed.getInstance());
         creature.addStatFunc(FuncMAtkSpeed.getInstance());
      }
   }

   public static double calcHpRegen(Creature creature) {
      double init = creature.isPlayer() ? creature.asPlayer().getTemplate().getBaseHpRegen(creature.getLevel()) : creature.getTemplate().getBaseHpReg();
      double hpRegenMultiplier = creature.isRaid() ? NpcConfig.RAID_HP_REGEN_MULTIPLIER : PlayerConfig.HP_REGEN_MULTIPLIER;
      double hpRegenBonus = 0.0;
      if (ChampionMonstersConfig.CHAMPION_ENABLE && creature.isChampion()) {
         hpRegenMultiplier *= ChampionMonstersConfig.CHAMPION_HP_REGEN;
      }

      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
            hpRegenMultiplier *= calcFestivalRegenModifier(player);
         } else {
            double siegeModifier = calcSiegeRegenModifier(player);
            if (siegeModifier > 0.0) {
               hpRegenMultiplier *= siegeModifier;
            }
         }

         Clan clan = player.getClan();
         if (player.isInsideZone(ZoneId.CLAN_HALL) && clan != null && clan.getHideoutId() > 0) {
            ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
            int posChIndex = zone == null ? -1 : zone.getResidenceId();
            int clanHallIndex = clan.getHideoutId();
            if (clanHallIndex > 0 && clanHallIndex == posChIndex) {
               ClanHall clansHall = ClanHallTable.getInstance().getClanHallById(clanHallIndex);
               if (clansHall != null && clansHall.getFunction(3) != null) {
                  hpRegenMultiplier *= 1.0 + clansHall.getFunction(3).getLevel() / 100.0;
               }
            }
         }

         if (player.isInsideZone(ZoneId.CASTLE) && clan != null && clan.getCastleId() > 0) {
            CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
            int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
            int castleIndex = clan.getCastleId();
            if (castleIndex > 0 && castleIndex == posCastleIndex) {
               Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
               if (castle != null && castle.getFunction(2) != null) {
                  hpRegenMultiplier *= 1.0 + castle.getFunction(2).getLvl() / 100.0;
               }
            }
         }

         if (player.isInsideZone(ZoneId.FORT) && clan != null && clan.getFortId() > 0) {
            FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
            int posFortIndex = zone == null ? -1 : zone.getResidenceId();
            int fortIndex = clan.getFortId();
            if (fortIndex > 0 && fortIndex == posFortIndex) {
               Fort fort = FortManager.getInstance().getFortById(fortIndex);
               if (fort != null && fort.getFunction(2) != null) {
                  hpRegenMultiplier *= 1.0 + fort.getFunction(2).getLevel() / 100.0;
               }
            }
         }

         if (player.isInsideZone(ZoneId.MOTHER_TREE)) {
            MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
            int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
            hpRegenBonus += hpBonus;
         }

         if (player.isSitting()) {
            hpRegenMultiplier *= 1.5;
         } else if (!player.isMoving()) {
            hpRegenMultiplier *= 1.1;
         } else if (player.isRunning()) {
            hpRegenMultiplier *= 0.7;
         }

         init *= creature.getLevelMod() * BaseStat.CON.calcBonus(creature);
      } else if (creature.isPet()) {
         init = creature.asPet().getPetLevelData().getPetRegenHP() * NpcConfig.PET_HP_REGEN_MULTIPLIER;
      }

      return creature.calcStat(Stat.REGENERATE_HP_RATE, Math.max(1.0, init), null, null) * hpRegenMultiplier + hpRegenBonus;
   }

   public static double calcMpRegen(Creature creature) {
      double init = creature.isPlayer() ? creature.asPlayer().getTemplate().getBaseMpRegen(creature.getLevel()) : creature.getTemplate().getBaseMpReg();
      double mpRegenMultiplier = creature.isRaid() ? NpcConfig.RAID_MP_REGEN_MULTIPLIER : PlayerConfig.MP_REGEN_MULTIPLIER;
      double mpRegenBonus = 0.0;
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
            mpRegenMultiplier *= calcFestivalRegenModifier(player);
         }

         if (player.isInsideZone(ZoneId.MOTHER_TREE)) {
            MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
            int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
            mpRegenBonus += mpBonus;
         }

         Clan clan = player.getClan();
         if (player.isInsideZone(ZoneId.CLAN_HALL) && clan != null && clan.getHideoutId() > 0) {
            ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
            int posChIndex = zone == null ? -1 : zone.getResidenceId();
            int clanHallIndex = clan.getHideoutId();
            if (clanHallIndex > 0 && clanHallIndex == posChIndex) {
               ClanHall clansHall = ClanHallTable.getInstance().getClanHallById(clanHallIndex);
               if (clansHall != null && clansHall.getFunction(4) != null) {
                  mpRegenMultiplier *= 1.0 + clansHall.getFunction(4).getLevel() / 100.0;
               }
            }
         }

         if (player.isInsideZone(ZoneId.CASTLE) && clan != null && clan.getCastleId() > 0) {
            CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
            int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
            int castleIndex = clan.getCastleId();
            if (castleIndex > 0 && castleIndex == posCastleIndex) {
               Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
               if (castle != null && castle.getFunction(3) != null) {
                  mpRegenMultiplier *= 1.0 + castle.getFunction(3).getLvl() / 100.0;
               }
            }
         }

         if (player.isInsideZone(ZoneId.FORT) && clan != null && clan.getFortId() > 0) {
            FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
            int posFortIndex = zone == null ? -1 : zone.getResidenceId();
            int fortIndex = clan.getFortId();
            if (fortIndex > 0 && fortIndex == posFortIndex) {
               Fort fort = FortManager.getInstance().getFortById(fortIndex);
               if (fort != null && fort.getFunction(3) != null) {
                  mpRegenMultiplier *= 1.0 + fort.getFunction(3).getLevel() / 100.0;
               }
            }
         }

         if (player.isSitting()) {
            mpRegenMultiplier *= 1.5;
         } else if (!player.isMoving()) {
            mpRegenMultiplier *= 1.1;
         } else if (player.isRunning()) {
            mpRegenMultiplier *= 0.7;
         }

         init *= creature.getLevelMod() * BaseStat.MEN.calcBonus(creature);
      } else if (creature.isPet()) {
         init = creature.asPet().getPetLevelData().getPetRegenMP() * NpcConfig.PET_MP_REGEN_MULTIPLIER;
      }

      return creature.calcStat(Stat.REGENERATE_MP_RATE, Math.max(1.0, init), null, null) * mpRegenMultiplier + mpRegenBonus;
   }

   public static double calcCpRegen(Player player) {
      double init = player.asPlayer().getTemplate().getBaseCpRegen(player.getLevel()) * player.getLevelMod() * BaseStat.CON.calcBonus(player);
      double cpRegenMultiplier = PlayerConfig.CP_REGEN_MULTIPLIER;
      if (player.isSitting()) {
         cpRegenMultiplier *= 1.5;
      } else if (!player.isMoving()) {
         cpRegenMultiplier *= 1.1;
      } else if (player.isRunning()) {
         cpRegenMultiplier *= 0.7;
      }

      return player.calcStat(Stat.REGENERATE_CP_RATE, Math.max(1.0, init), null, null) * cpRegenMultiplier;
   }

   public static double calcFestivalRegenModifier(Player player) {
      int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(player);
      int festivalId = festivalInfo[1];
      if (festivalId < 0) {
         return 0.0;
      } else {
         int oracle = festivalInfo[0];
         int[] festivalCenter;
         if (oracle == 2) {
            festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
         } else {
            festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
         }

         double distToCenter = player.calculateDistance2D(festivalCenter[0], festivalCenter[1], 0);
         return 1.0 - distToCenter * 5.0E-4;
      }
   }

   public static double calcSiegeRegenModifier(Player player) {
      if (player == null) {
         return 0.0;
      } else {
         Clan clan = player.getClan();
         if (clan == null) {
            return 0.0;
         } else {
            Siege siege = SiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
            if (siege != null && siege.isInProgress()) {
               SiegeClan siegeClan = siege.getAttackerClan(clan.getId());
               return siegeClan != null
                     && !siegeClan.getFlag().isEmpty()
                     && LocationUtil.checkIfInRange(200, player, siegeClan.getFlag().stream().findFirst().get(), true)
                  ? 1.5
                  : 0.0;
            } else {
               return 0.0;
            }
         }
      }
   }

   public static double calcBlowDamage(Creature attacker, Creature target, Skill skill, byte shld, boolean ss) {
      double defence = target.getPDef(attacker);
      switch (shld) {
         case 1:
            defence += target.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      boolean isPvP = attacker.isPlayable() && target.isPlayable();
      boolean isPvE = attacker.isPlayable() && target.isAttackable();
      double power = skill.getPower(isPvP, isPvE);
      double damage = 0.0;
      double proximityBonus = attacker.isBehind(target) ? 1.2 : (attacker.isInFrontOf(target) ? 1.0 : 1.1);
      double ssboost = ss ? 1.458 : 1.0;
      double pvpBonus = 1.0;
      if (isPvP) {
         pvpBonus = attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
         pvpBonus *= ClassBalanceConfig.PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         defence *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         defence *= ClassBalanceConfig.PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
      }

      if (isPvE) {
         pvpBonus *= ClassBalanceConfig.PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
      }

      if (attacker.isAttackable() && target.isPlayable()) {
         defence *= ClassBalanceConfig.PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
      }

      double baseMod = 77.0 * (power + attacker.getPAtk(target) * ssboost) / defence;
      double criticalMod = attacker.calcStat(Stat.CRITICAL_DAMAGE, 1.0, target, skill);
      double criticalModPos = (attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, target, skill) - 1.0) / 2.0 + 1.0;
      double criticalVulnMod = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, target, skill);
      double criticalAddMod = attacker.getStat().calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0) * 6.1 * 77.0 / defence;
      double criticalAddVuln = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, target, skill);
      double weaponTraitMod = calcWeaponTraitBonus(attacker, target);
      double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
      double attributeMod = calcAttributeBonus(attacker, target, skill);
      double weaponMod = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? attacker.getRandomDamageMultiplier() : 1.0;
      double penaltyMod = 1.0;
      if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
         Player attackerPlayer = attacker.asPlayer();
         if (attackerPlayer != null && target.getLevel() - attackerPlayer.getLevel() >= 2) {
            int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
            if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               penaltyMod *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               penaltyMod *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
            }
         }
      }

      damage = baseMod * criticalMod * criticalModPos * criticalVulnMod * proximityBonus * pvpBonus + criticalAddMod + criticalAddVuln;
      damage *= weaponTraitMod;
      damage *= generalTraitMod;
      damage *= attributeMod;
      damage *= weaponMod;
      damage *= penaltyMod;
      return Math.max(damage, 1.0);
   }

   public static double calcBackstabDamage(Creature attacker, Creature target, Skill skill, byte shld, boolean ss) {
      double defence = target.getPDef(attacker);
      switch (shld) {
         case 1:
            defence += target.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      boolean isPvP = attacker.isPlayable() && target.isPlayer();
      boolean isPvE = attacker.isPlayable() && target.isAttackable();
      double damage = 0.0;
      double proximityBonus = attacker.isBehind(target) ? 1.2 : (attacker.isInFrontOf(target) ? 1.0 : 1.1);
      double ssboost = ss ? 1.458 : 1.0;
      double pvpBonus = 1.0;
      if (isPvP) {
         pvpBonus = attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
         pvpBonus *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         defence *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         defence *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
      }

      double baseMod = 77.0 * (skill.getPower(isPvP, isPvE) + attacker.getPAtk(target)) / defence * ssboost;
      double criticalMod = attacker.calcStat(Stat.CRITICAL_DAMAGE, 1.0, target, skill);
      double criticalModPos = (attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, target, skill) - 1.0) / 2.0 + 1.0;
      double criticalVulnMod = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, target, skill);
      double criticalAddMod = attacker.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0, target, skill) * 6.1 * 77.0 / defence;
      double criticalAddVuln = target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, target, skill);
      double generalTraitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
      double attributeMod = calcAttributeBonus(attacker, target, skill);
      double weaponMod = PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE ? attacker.getRandomDamageMultiplier() : 1.0;
      double penaltyMod = 1.0;
      if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
         Player attackerPlayer = attacker.asPlayer();
         if (attackerPlayer != null && target.getLevel() - attackerPlayer.getLevel() >= 2) {
            int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
            if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               penaltyMod *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               penaltyMod *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
            }
         }
      }

      damage = baseMod * criticalMod * criticalModPos * criticalVulnMod * proximityBonus * pvpBonus + criticalAddMod + criticalAddVuln;
      damage *= generalTraitMod;
      damage *= attributeMod;
      damage *= weaponMod;
      damage *= penaltyMod;
      return Math.max(damage, 1.0);
   }

   public static double calcPhysDam(Creature attacker, Creature target, Skill skill, byte shld, boolean crit, boolean ss) {
      boolean isPvP = attacker.isPlayable() && target.isPlayable();
      boolean isPvE = attacker.isPlayable() && target.isAttackable();
      double proximityBonus = attacker.isBehind(target) ? 1.2 : (attacker.isInFrontOf(target) ? 1.0 : 1.1);
      double damage = attacker.getPAtk(target);
      double defence = target.getPDef(attacker);
      if (isPvP) {
         defence *= skill == null ? target.calcStat(Stat.PVP_PHYSICAL_DEF, 1.0, null, null) : target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
         defence *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
      }

      switch (shld) {
         case 1:
            if (!PlayerConfig.ALT_GAME_SHIELD_BLOCKS) {
               defence += target.getShldDef();
            }
         default:
            int ssBoost = ss ? 2 : 1;
            damage = skill != null ? damage * ssBoost + skill.getPower(attacker, target, isPvP, isPvE) : damage * ssBoost;
            if (crit) {
               damage = 2.0
                  * attacker.calcStat(Stat.CRITICAL_DAMAGE, 1.0, target, skill)
                  * attacker.calcStat(Stat.CRITICAL_DAMAGE_POS, 1.0, target, skill)
                  * target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE, 1.0, target, null)
                  * (76.0 * damage * proximityBonus / defence);
               damage += attacker.calcStat(Stat.CRITICAL_DAMAGE_ADD, 0.0, target, skill) * 77.0 / defence;
               damage += target.calcStat(Stat.DEFENCE_CRITICAL_DAMAGE_ADD, 0.0, target, skill);
               if (skill == null) {
                  if (isPvP) {
                     damage *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
                  } else if (isPvE) {
                     damage *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
                  }
               } else if (isPvP) {
                  damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
               } else if (isPvE) {
                  damage *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
               }
            } else {
               damage = 76.0 * damage * proximityBonus / defence;
            }

            damage *= calcAttackTraitBonus(attacker, target);
            if (skill == null && PlayerConfig.RANDOMIZE_AUTO_ATTACK_DAMAGE || skill != null && PlayerConfig.RANDOMIZE_PHYSICAL_SKILL_DAMAGE) {
               damage *= attacker.getRandomDamageMultiplier();
            }

            damage *= attacker.getRandomDamageMultiplier();
            if (shld > 0 && PlayerConfig.ALT_GAME_SHIELD_BLOCKS) {
               damage -= target.getShldDef();
               if (damage < 0.0) {
                  damage = 0.0;
               }
            }

            if (damage > 0.0 && damage < 1.0) {
               damage = 1.0;
            } else if (damage < 0.0) {
               damage = 0.0;
            }

            if (isPvP) {
               if (skill == null) {
                  damage *= attacker.calcStat(Stat.PVP_PHYSICAL_DMG, 1.0, null, null);
                  damage *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
               } else {
                  damage *= attacker.calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0, null, null);
                  damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
               }
            }

            if (skill != null) {
               damage = attacker.calcStat(Stat.PHYSICAL_SKILL_POWER, damage, null, null);
            }

            damage *= calcAttributeBonus(attacker, target, skill);
            if (target.isAttackable()) {
               Weapon weapon = attacker.getActiveWeaponItem();
               if (weapon != null && (weapon.getItemType() == WeaponType.BOW || weapon.getItemType() == WeaponType.CROSSBOW)) {
                  if (skill != null) {
                     damage *= attacker.calcStat(Stat.PVE_BOW_SKILL_DMG, 1.0, null, null);
                  } else {
                     damage *= attacker.calcStat(Stat.PVE_BOW_DMG, 1.0, null, null);
                  }
               } else {
                  damage *= attacker.calcStat(Stat.PVE_PHYSICAL_DMG, 1.0, null, null);
                  if (attacker.isPlayable()) {
                     if (skill == null) {
                        damage *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
                     } else {
                        damage *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
                     }
                  }
               }

               if (!target.isRaid() && !target.isRaidMinion() && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY) {
                  Player attackerPlayer = attacker.asPlayer();
                  if (attackerPlayer != null && target.getLevel() - attackerPlayer.getLevel() >= 2) {
                     int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
                     if (skill != null) {
                        if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                           damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
                        } else {
                           damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
                        }
                     } else if (crit) {
                        if (levelDiff >= NpcConfig.NPC_CRIT_DMG_PENALTY.length) {
                           damage *= NpcConfig.NPC_CRIT_DMG_PENALTY[NpcConfig.NPC_CRIT_DMG_PENALTY.length - 1];
                        } else {
                           damage *= NpcConfig.NPC_CRIT_DMG_PENALTY[levelDiff];
                        }
                     } else if (levelDiff >= NpcConfig.NPC_DMG_PENALTY.length) {
                        damage *= NpcConfig.NPC_DMG_PENALTY[NpcConfig.NPC_DMG_PENALTY.length - 1];
                     } else {
                        damage *= NpcConfig.NPC_DMG_PENALTY[levelDiff];
                     }
                  }
               }
            }

            return damage;
         case 2:
            return 1.0;
      }
   }

   public static double calcMagicDam(Creature attacker, Creature target, Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit) {
      double mDef = target.getMDef(attacker, skill);
      switch (shld) {
         case 1:
            mDef += target.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      double mAtk = attacker.getMAtk(target, skill);
      boolean isPvP = attacker.isPlayable() && target.isPlayable();
      boolean isPvE = attacker.isPlayable() && target.isAttackable();
      if (isPvP) {
         if (skill.isMagic()) {
            mDef *= target.calcStat(Stat.PVP_MAGICAL_DEF, 1.0, null, null);
            mDef *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
         } else {
            mDef *= target.calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0, null, null);
            mDef *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
         }
      }

      mAtk *= bss ? 4.0 : (sps ? 2.0 : 1.0);
      double damage = 91.0 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker, target, isPvP, isPvE);
      if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill)) {
         if (attacker.isPlayer()) {
            if (calcMagicSuccess(attacker, target, skill) && target.getLevel() - attacker.getLevel() <= 9) {
               if (skill.hasEffectType(EffectType.HP_DRAIN)) {
                  attacker.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
               } else {
                  attacker.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
               }

               damage /= 2.0;
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
               sm.addString(target.getName());
               sm.addSkillName(skill);
               attacker.sendPacket(sm);
               damage = 1.0;
            }
         }

         if (target.isPlayer()) {
            SystemMessage sm = skill.hasEffectType(EffectType.HP_DRAIN)
               ? new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN)
               : new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
            sm.addString(attacker.getName());
            target.sendPacket(sm);
         }
      } else if (mcrit) {
         damage *= attacker.isPlayer() && target.isPlayer() ? 2.5 : 3.0;
         damage *= attacker.calcStat(Stat.MAGIC_CRIT_DMG, 1.0, null, null);
         if (attacker.isPlayable()) {
            damage *= target.isPlayable()
               ? ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()]
               : ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         }
      }

      if (PlayerConfig.RANDOMIZE_MAGICAL_SKILL_DAMAGE) {
         damage *= attacker.getRandomDamageMultiplier();
      }

      if (isPvP) {
         Stat stat;
         if (skill.isMagic()) {
            stat = Stat.PVP_MAGICAL_DMG;
            damage *= ClassBalanceConfig.PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         } else {
            stat = Stat.PVP_PHYS_SKILL_DMG;
            damage *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         }

         damage *= attacker.calcStat(stat, 1.0, null, null);
      }

      damage *= calcAttributeBonus(attacker, target, skill);
      if (target.isAttackable()) {
         damage *= attacker.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
         Player attackerPlayer = attacker.asPlayer();
         if (attackerPlayer != null) {
            damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
            if (!target.isRaid()
               && !target.isRaidMinion()
               && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY
               && target.getLevel() - attackerPlayer.getLevel() >= 2) {
               int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
               if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                  damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
               } else {
                  damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
               }
            }
         }
      }

      return damage;
   }

   public static double calcMagicDam(Cubic attacker, Creature target, Skill skill, boolean mcrit, byte shld) {
      double mDef = target.getMDef(attacker.getOwner(), skill);
      switch (shld) {
         case 1:
            mDef += target.getShldDef();
            break;
         case 2:
            return 1.0;
      }

      int mAtk = attacker.getCubicPower();
      boolean isPvP = target.isPlayable();
      boolean isPvE = target.isAttackable();
      double damage = 91.0 * ((mAtk + skill.getPower(isPvP, isPvE)) / mDef);
      Player owner = attacker.getOwner();
      if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill)) {
         if (calcMagicSuccess(owner, target, skill) && target.getLevel() - skill.getMagicLevel() <= 9) {
            if (skill.hasEffectType(EffectType.HP_DRAIN)) {
               owner.sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
            } else {
               owner.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
            }

            damage /= 2.0;
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
            sm.addString(target.getName());
            sm.addSkillName(skill);
            owner.sendPacket(sm);
            damage = 1.0;
         }

         if (target.isPlayer()) {
            if (skill.hasEffectType(EffectType.HP_DRAIN)) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_DRAIN);
               sm.addString(owner.getName());
               target.sendPacket(sm);
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RESISTED_C1_S_MAGIC);
               sm.addString(owner.getName());
               target.sendPacket(sm);
            }
         }
      } else if (mcrit) {
         damage *= 3.0;
      }

      damage *= calcAttributeBonus(owner, target, skill);
      if (target.isAttackable()) {
         damage *= owner.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
         damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[owner.getPlayerClass().getId()];
         if (!target.isRaid()
            && !target.isRaidMinion()
            && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY
            && target.getLevel() - owner.getLevel() >= 2) {
            int levelDiff = target.getLevel() - attacker.getOwner().getLevel() - 1;
            if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
               damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
            } else {
               damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
            }
         }
      }

      return damage;
   }

   public static boolean calcCrit(Creature attacker, Creature target) {
      return calcCrit(attacker, target, null);
   }

   public static boolean calcCrit(Creature attacker, Creature target, Skill skill) {
      double rate;
      if (skill != null) {
         rate = skill.getBaseCritRate() * 10 * BaseStat.STR.calcBonus(attacker);
      } else {
         rate = attacker.getStat().calcStat(Stat.CRITICAL_RATE_POS, attacker.getStat().getCriticalHit(target, null), target, skill);
      }

      boolean isPvP = attacker.isPlayable() && target.isPlayable();
      if (skill == null) {
         if (isPvP) {
            rate *= ClassBalanceConfig.PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         } else if (attacker.isPlayable() && target.isAttackable()) {
            rate *= ClassBalanceConfig.PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
         }
      } else if (isPvP) {
         rate *= ClassBalanceConfig.PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
      } else if (attacker.isPlayable() && target.isAttackable()) {
         rate *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[attacker.asPlayer().getPlayerClass().getId()];
      }

      return target.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE, rate, null, null)
            + target.getStat().calcStat(Stat.DEFENCE_CRITICAL_RATE_ADD, 0.0, null, null)
         > Rnd.get(1000);
   }

   public static boolean calcMCrit(double mRate) {
      return mRate > Rnd.get(1000);
   }

   public static boolean calcAtkBreak(Creature target, double dmg) {
      if (target.isChanneling()) {
         return false;
      } else {
         double init = 0.0;
         if (PlayerConfig.ALT_GAME_CANCEL_CAST && target.isCastingNow()) {
            init = 15.0;
         }

         if (PlayerConfig.ALT_GAME_CANCEL_BOW && target.isAttackingNow()) {
            Weapon wpn = target.getActiveWeaponItem();
            if (wpn != null && wpn.getItemType() == WeaponType.BOW) {
               init = 15.0;
            }
         }

         if (!target.isRaid() && !target.isInvul() && !(init <= 0.0)) {
            init += Math.sqrt(13.0 * dmg);
            init -= BaseStat.MEN.calcBonus(target) * 100.0 - 100.0;
            double rate = target.calcStat(Stat.ATTACK_CANCEL, init, null, null);
            rate = Math.clamp(rate, 1.0, 99.0);
            return Rnd.get(100) < rate;
         } else {
            return false;
         }
      }
   }

   public static int calcPAtkSpd(Creature attacker, Creature target, double rate) {
      return rate < 2.0 ? 2700 : (int)(470000.0 / rate);
   }

   public static int calcAtkSpd(Creature attacker, Skill skill, double skillTime) {
      return skill.isMagic() ? (int)(skillTime / attacker.getMAtkSpd() * 333.0) : (int)(skillTime / attacker.getPAtkSpd() * 300.0);
   }

   public static boolean calcHitMiss(Creature attacker, Creature target) {
      int chance = (80 + 2 * (attacker.getAccuracy() - target.getEvasionRate(attacker))) * 10;
      chance = (int)(chance * HitConditionBonusData.getInstance().getConditionBonus(attacker, target));
      chance = Math.max(chance, 200);
      chance = Math.min(chance, 980);
      return chance < Rnd.get(1000);
   }

   public static byte calcShldUse(Creature attacker, Creature target, Skill skill, boolean sendSysMsg) {
      if (skill != null && skill.ignoreShield()) {
         return 0;
      } else {
         ItemTemplate item = target.getSecondaryWeaponItem();
         if (item != null && item instanceof Armor && ((Armor)item).getItemType() != ArmorType.SIGIL) {
            double shldRate = target.calcStat(Stat.SHIELD_RATE, 0.0, attacker, null) * BaseStat.DEX.calcBonus(target);
            if (shldRate <= 1.0E-6) {
               return 0;
            } else {
               int degreeside = (int)target.calcStat(Stat.SHIELD_DEFENCE_ANGLE, 0.0, null, null) + 120;
               if (degreeside < 360
                  && Math.abs(target.calculateDirectionTo(attacker) - LocationUtil.convertHeadingToDegree(target.getHeading())) > degreeside / 2) {
                  return 0;
               } else {
                  byte shldSuccess = 0;
                  Weapon atWeapon = attacker.getActiveWeaponItem();
                  if (atWeapon != null && atWeapon.getItemType() == WeaponType.BOW) {
                     shldRate *= 1.3;
                  }

                  if (shldRate > 0.0 && 100 - PlayerConfig.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100)) {
                     shldSuccess = 2;
                  } else if (shldRate > Rnd.get(100)) {
                     shldSuccess = 1;
                  }

                  if (sendSysMsg && target.isPlayer()) {
                     Player enemy = target.asPlayer();
                     switch (shldSuccess) {
                        case 1:
                           enemy.sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
                           break;
                        case 2:
                           enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
                     }
                  }

                  return shldSuccess;
               }
            }
         } else {
            return 0;
         }
      }
   }

   public static byte calcShldUse(Creature attacker, Creature target, Skill skill) {
      return calcShldUse(attacker, target, skill, true);
   }

   public static byte calcShldUse(Creature attacker, Creature target) {
      return calcShldUse(attacker, target, null, true);
   }

   public static boolean calcMagicAffected(Creature actor, Creature target, Skill skill) {
      double defence = 0.0;
      if (skill.isActive() && skill.hasNegativeEffect()) {
         defence = target.getMDef(actor, skill);
      }

      double attack = 2.0 * actor.getMAtk(target, skill) * calcGeneralTraitBonus(actor, target, skill.getTraitType(), false);
      double d = (attack - defence) / (attack + defence);
      if (skill.isDebuff() && target.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, null, skill) > 0.0) {
         return false;
      } else {
         d += 0.5 * Rnd.nextGaussian();
         return d > 0.0;
      }
   }

   public static double calcLvlBonusMod(Creature attacker, Creature target, Skill skill) {
      int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
      double skillLevelBonusRateMod = 1.0 + skill.getLvlBonusRate() / 100.0;
      double lvlMod = 1.0 + (attackerLvl - target.getLevel()) / 100.0;
      return skillLevelBonusRateMod * lvlMod;
   }

   public static boolean calcEffectSuccess(Creature attacker, Creature target, Skill skill) {
      if (!target.isDoor() && !(target instanceof SiegeFlag) && !(target instanceof StaticObject)) {
         if (skill.isDebuff() && target.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, attacker, skill) > 0.0) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
            sm.addString(target.getName());
            sm.addSkillName(skill);
            attacker.sendPacket(sm);
            return false;
         } else {
            int activateRate = skill.getActivateRate();
            if (activateRate != -1 && activateRate <= 99 && skill.getBasicProperty() != BaseStat.NONE) {
               int magicLevel = skill.getMagicLevel();
               if (magicLevel <= -1) {
                  magicLevel = target.getLevel() + 3;
               }

               int targetBaseStat = 0;
               switch (skill.getBasicProperty()) {
                  case STR:
                     targetBaseStat = target.getSTR();
                     break;
                  case DEX:
                     targetBaseStat = target.getDEX();
                     break;
                  case CON:
                     targetBaseStat = target.getCON();
                     break;
                  case INT:
                     targetBaseStat = target.getINT();
                     break;
                  case MEN:
                     targetBaseStat = target.getMEN();
                     break;
                  case WIT:
                     targetBaseStat = target.getWIT();
               }

               double baseMod = (magicLevel - target.getLevel() + 3) * skill.getLvlBonusRate() + activateRate + 30.0 - targetBaseStat;
               double elementMod = calcAttributeBonus(attacker, target, skill);
               double traitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
               double buffDebuffMod = 1.0 + target.calcStat(skill.isDebuff() ? Stat.DEBUFF_VULN : Stat.BUFF_VULN, 1.0, null, null) / 100.0;
               double mAtkMod = 1.0;
               if (skill.isMagic()) {
                  double mAtk = attacker.getMAtk(null, null);
                  double val = 0.0;
                  if (attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)) {
                     val = mAtk * 3.0;
                  }

                  val += mAtk;
                  val = Math.sqrt(val) / target.getMDef(null, null) * 11.0;
                  mAtkMod = val;
               }

               double rate = baseMod * elementMod * traitMod * mAtkMod * buffDebuffMod;
               double finalRate = traitMod > 0.0 ? Math.clamp(rate, (double)skill.getMinChance(), (double)skill.getMaxChance()) : 0.0;
               if (finalRate <= Rnd.get(100)) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RESISTED_YOUR_S2);
                  sm.addString(target.getName());
                  sm.addSkillName(skill);
                  attacker.sendPacket(sm);
                  return false;
               } else {
                  return true;
               }
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, Skill skill, byte shld) {
      if (skill.isDebuff()) {
         if (skill.getPower() == -1.0) {
            return true;
         }

         if (target.calcStat(Stat.DEBUFF_IMMUNITY, 0.0, null, skill) > 0.0) {
            return false;
         }
      }

      if (shld == 2) {
         return false;
      } else if (calcBuffDebuffReflection(target, skill)) {
         return false;
      } else {
         int activateRate = skill.getActivateRate();
         if (activateRate != -1 && activateRate <= 99 && skill.getBasicProperty() != BaseStat.NONE) {
            double baseRate = activateRate > 0 ? activateRate : skill.getPower();
            double statMod = skill.getBasicProperty().calcBonus(target);
            double rate = baseRate / statMod;
            double resMod = calcGeneralTraitBonus(attacker.getOwner(), target, skill.getTraitType(), false);
            rate *= resMod;
            double lvlBonusMod = calcLvlBonusMod(attacker.getOwner(), target, skill);
            rate *= lvlBonusMod;
            double elementMod = calcAttributeBonus(attacker.getOwner(), target, skill);
            rate *= elementMod;
            double finalRate = Math.clamp(rate, (double)skill.getMinChance(), (double)skill.getMaxChance());
            return Rnd.get(100) < finalRate;
         } else {
            return true;
         }
      }
   }

   public static boolean calcMagicSuccess(Creature attacker, Creature target, Skill skill) {
      if (skill.getPower() == -1.0) {
         return true;
      } else {
         double lvlModifier = MathUtil.pow(
            1.3,
            target.getLevel()
               - (PlayerConfig.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL && skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel())
         );
         float targetModifier = 1.0F;
         if (target.isAttackable() && !target.isRaid() && !target.isRaidMinion() && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_MAGIC_PENALTY) {
            Player attackerPlayer = attacker.asPlayer();
            if (attackerPlayer != null && target.getLevel() - attackerPlayer.getLevel() >= 3) {
               int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 2;
               if (levelDiff >= NpcConfig.NPC_SKILL_CHANCE_PENALTY.length) {
                  targetModifier = NpcConfig.NPC_SKILL_CHANCE_PENALTY[NpcConfig.NPC_SKILL_CHANCE_PENALTY.length - 1];
               } else {
                  targetModifier = NpcConfig.NPC_SKILL_CHANCE_PENALTY[levelDiff];
               }
            }
         }

         double resModifier = target.calcStat(Stat.MAGIC_SUCCESS_RES, 1.0, null, skill);
         int rate = 100 - Math.round((float)(lvlModifier * targetModifier * resModifier));
         return Rnd.get(100) < rate;
      }
   }

   public static double calcManaDam(Creature attacker, Creature target, Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit) {
      double mAtk = attacker.getMAtk(target, skill);
      double mDef = target.getMDef(attacker, skill);
      boolean isPvP = attacker.isPlayable() && target.isPlayable();
      boolean isPvE = attacker.isPlayable() && target.isAttackable();
      double mp = target.getMaxMp();
      switch (shld) {
         case 1:
            mDef += target.getShldDef();
         default:
            mAtk *= bss ? 4.0 : (sps ? 2.0 : 1.0);
            double damage = Math.sqrt(mAtk) * skill.getPower(attacker, target, isPvP, isPvE) * (mp / 97.0) / mDef;
            damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
            if (target.isAttackable()) {
               damage *= attacker.calcStat(Stat.PVE_MAGICAL_DMG, 1.0, null, null);
               Player attackerPlayer = attacker.asPlayer();
               if (attackerPlayer != null) {
                  damage *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[attackerPlayer.getPlayerClass().getId()];
                  if (!target.isRaid()
                     && !target.isRaidMinion()
                     && target.getLevel() >= NpcConfig.MIN_NPC_LEVEL_DMG_PENALTY
                     && target.getLevel() - attackerPlayer.getLevel() >= 2) {
                     int levelDiff = target.getLevel() - attackerPlayer.getLevel() - 1;
                     if (levelDiff >= NpcConfig.NPC_SKILL_DMG_PENALTY.length) {
                        damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[NpcConfig.NPC_SKILL_DMG_PENALTY.length - 1];
                     } else {
                        damage *= NpcConfig.NPC_SKILL_DMG_PENALTY[levelDiff];
                     }
                  }
               }
            }

            if (PlayerConfig.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill)) {
               if (attacker.isPlayer()) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2_S_MAGIC);
                  sm.addString(target.getName());
                  sm.addString(attacker.getName());
                  attacker.sendPacket(sm);
                  damage /= 2.0;
               }

               if (target.isPlayer()) {
                  SystemMessage sm2 = new SystemMessage(SystemMessageId.C1_WEAKLY_RESISTED_C2_S_MAGIC);
                  sm2.addString(target.getName());
                  sm2.addString(attacker.getName());
                  target.sendPacket(sm2);
               }
            }

            if (mcrit) {
               damage *= 3.0;
               attacker.sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
            }

            return damage;
         case 2:
            return 1.0;
      }
   }

   public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster) {
      if (baseRestorePercent != 0.0 && baseRestorePercent != 100.0) {
         double restorePercent = baseRestorePercent * BaseStat.WIT.calcBonus(caster);
         if (restorePercent - baseRestorePercent > 20.0) {
            restorePercent += 20.0;
         }

         restorePercent = Math.max(restorePercent, baseRestorePercent);
         return Math.min(restorePercent, 90.0);
      } else {
         return baseRestorePercent;
      }
   }

   public static boolean calcPhysicalSkillEvasion(Creature creature, Creature target, Skill skill) {
      if (!skill.isMagic() && !skill.isDebuff()) {
         if (Rnd.get(100) < target.calcStat(Stat.P_SKILL_EVASION, 0.0, null, skill)) {
            if (creature.isPlayer()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_DODGES_THE_ATTACK);
               sm.addString(target.getName());
               creature.asPlayer().sendPacket(sm);
            }

            if (target.isPlayer()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_AVOIDED_C1_S_ATTACK_2);
               sm.addString(creature.getName());
               target.asPlayer().sendPacket(sm);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean calcSkillMastery(Creature actor, Skill sk) {
      if (!actor.isPlayer()) {
         return false;
      } else if (sk.isStatic()) {
         return false;
      } else {
         int val = (int)actor.getStat().calcStat(Stat.SKILL_CRITICAL, 0.0, null, null);
         if (val == 0) {
            return false;
         } else {
            double chance = 0.0;
            switch (val) {
               case 1:
                  chance = BaseStat.STR.calcBonus(actor);
                  break;
               case 4:
                  chance = BaseStat.INT.calcBonus(actor);
            }

            chance *= actor.getStat().calcStat(Stat.SKILL_CRITICAL_PROBABILITY, 1.0, null, null);
            return Rnd.get(100) < chance * ClassBalanceConfig.SKILL_MASTERY_CHANCE_MULTIPLIERS[actor.asPlayer().getPlayerClass().getId()];
         }
      }
   }

   public static double calcAttributeBonus(Creature attacker, Creature target, Skill skill) {
      int attackAttribute;
      int defenceAttribute;
      if (skill != null && skill.getElement() != -1) {
         attackAttribute = attacker.getAttackElementValue(skill.getElement()) + skill.getElementPower();
         defenceAttribute = target.getDefenseElementValue(skill.getElement());
      } else {
         attackAttribute = attacker.getAttackElementValue(attacker.getAttackElement());
         defenceAttribute = target.getDefenseElementValue(attacker.getAttackElement());
      }

      int diff = attackAttribute - defenceAttribute;
      if (diff > 0) {
         return Math.min(1.025 + Math.sqrt(MathUtil.pow(diff, 3) / 2.0) * 1.0E-4, 1.25);
      } else {
         return diff < 0 ? Math.max(0.975 - Math.sqrt(MathUtil.pow(-diff, 3) / 2.0) * 1.0E-4, 0.75) : 1.0;
      }
   }

   public static void calcDamageReflected(Creature attacker, Creature target, Skill skill, boolean crit) {
      if (!skill.isMagic() && skill.getCastRange() <= 40) {
         double chance = target.calcStat(Stat.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0.0, target, skill);
         if (Rnd.get(100) < chance) {
            if (target.isPlayer()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_COUNTERED_C1_S_ATTACK);
               sm.addString(attacker.getName());
               target.sendPacket(sm);
            }

            if (attacker.isPlayer()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_PERFORMING_A_COUNTERATTACK);
               sm.addString(target.getName());
               attacker.sendPacket(sm);
            }

            double counterdmg = target.getPAtk(attacker) * 10.0 * 70.0 / attacker.getPDef(target);
            counterdmg *= calcWeaponTraitBonus(attacker, target);
            counterdmg *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
            counterdmg *= calcAttributeBonus(attacker, target, skill);
            attacker.reduceCurrentHp(counterdmg, target, skill);
            if (crit) {
               attacker.reduceCurrentHp(counterdmg, target, skill);
            }
         }
      }
   }

   public static boolean calcBuffDebuffReflection(Creature target, Skill skill) {
      if (skill.isDebuff() && skill.getActivateRate() != -1) {
         double reflectChance = target.calcStat(skill.isMagic() ? Stat.REFLECT_SKILL_MAGIC : Stat.REFLECT_SKILL_PHYSIC, 0.0, null, skill);
         return reflectChance > Rnd.get(100);
      } else {
         return false;
      }
   }

   public static double calcFallDam(Creature creature, int fallHeight) {
      return GeneralConfig.ENABLE_FALLING_DAMAGE && fallHeight >= 0 ? creature.calcStat(Stat.FALL, fallHeight * creature.getMaxHp() / 1000, null, null) : 0.0;
   }

   public static boolean calcBlowSuccess(Creature creature, Creature target, Skill skill) {
      double dexMod = BaseStat.DEX.calcBonus(creature);
      double blowChance = skill.getBlowChance();
      double sideMod = creature.isInFrontOf(target) ? 1.0 : (creature.isBehind(target) ? 2.0 : 1.5);
      double baseRate = blowChance * dexMod * sideMod;
      double rate = creature.calcStat(Stat.BLOW_RATE, baseRate, target, null);
      return Rnd.get(100) < rate;
   }

   public static List<BuffInfo> calcStealEffects(Creature target, int amount) {
      List<BuffInfo> canceled = new ArrayList<>(amount);
      List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs()) : new ArrayList<>(1);
      if (target.getEffectList().hasDances()) {
         buffs.addAll(target.getEffectList().getDances());
      }

      for (int i = buffs.size() - 1; i >= 0 && canceled.size() < amount; i--) {
         BuffInfo info = buffs.get(i);
         if (info.getSkill().canBeStolen()) {
            canceled.add(info);
         }
      }

      return canceled;
   }

   public static List<BuffInfo> calcCancelEffects(Creature creature, Creature target, Skill skill, String slot, int rate, int max) {
      List<BuffInfo> canceled = new ArrayList<>(max);
      switch (slot) {
         case "buff":
            int cancelMagicLvl = skill == null ? creature.getLevel() : skill.getMagicLevel();
            double vuln = target.calcStat(Stat.CANCEL_VULN, 0.0, target, null);
            double prof = creature.calcStat(Stat.CANCEL_PROF, 0.0, target, null);
            double resMod = 1.0 + (vuln + prof) * -1.0 / 100.0;
            double finalRate = rate / resMod;
            List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs()) : new ArrayList<>(1);
            if (target.getEffectList().hasDances()) {
               buffs.addAll(target.getEffectList().getDances());
            }

            for (int ix = buffs.size() - 1; ix >= 0; ix--) {
               BuffInfo info = buffs.get(ix);
               if (info.getSkill().canBeStolen() && calcCancelSuccess(info, cancelMagicLvl, (int)finalRate, skill)) {
                  canceled.add(info);
                  if (canceled.size() >= max) {
                     return canceled;
                  }
               }
            }
            break;
         case "debuff":
            List<BuffInfo> debuffs = new ArrayList<>(target.getEffectList().getDebuffs());

            for (int i = debuffs.size() - 1; i >= 0; i--) {
               BuffInfo info = debuffs.get(i);
               if (info.getSkill().isDebuff() && info.getSkill().canBeDispeled() && Rnd.get(100) <= rate) {
                  canceled.add(info);
                  if (canceled.size() >= max) {
                     break;
                  }
               }
            }
      }

      return canceled;
   }

   public static boolean calcCancelSuccess(BuffInfo info, int cancelMagicLvl, int rate, Skill skill) {
      int chance = (int)(rate * (info.getSkill().getMagicLevel() > 0 ? 1.0 + (cancelMagicLvl - info.getSkill().getMagicLevel()) / 100.0 : 1.0));
      return Rnd.get(100) < Math.clamp((long)chance, skill.getMinChance(), skill.getMaxChance());
   }

   public static int calcEffectAbnormalTime(Creature caster, Creature target, Skill skill) {
      int time = !skill.isPassive() && !skill.isToggle() ? skill.getAbnormalTime() : -1;
      if (target != null && target.isServitor() && skill.isAbnormalInstant()) {
         time /= 2;
      }

      if (calcSkillMastery(caster, skill)) {
         time *= 2;
      }

      if (caster != null && target != null && skill.isDebuff() && GeneralConfig.DEBUFF_DURATION_USES_RESISTS && time > 0) {
         double statMod = skill.getBasicProperty().calcBonus(target);
         double resMod = calcGeneralTraitBonus(caster, target, skill.getTraitType(), false);
         double lvlBonusMod = calcLvlBonusMod(caster, target, skill);
         double elementMod = calcAttributeBonus(caster, target, skill);
         time = (int)Math.ceil(Math.clamp(time * resMod * lvlBonusMod * elementMod / statMod, time * 0.5, (double)time));
      }

      return time;
   }

   public static boolean calcProbability(double baseChance, Creature attacker, Creature target, Skill skill) {
      return Rnd.get(100)
         < (skill.getMagicLevel() + baseChance - target.getLevel() + 30.0 - target.getINT())
            * calcAttributeBonus(attacker, target, skill)
            * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
   }

   public static int calculateKarmaLost(Player player, long finalExp) {
      double karmaLossMod = KarmaLossData.getInstance().getModifier(player.getLevel());
      return finalExp > 0L
         ? (int)(Math.abs((float)finalExp / RatesConfig.RATE_KARMA_LOST) / karmaLossMod / 30.0)
         : (int)(Math.abs(finalExp) / karmaLossMod / 30.0);
   }

   public static int calculateKarmaGain(int pkCount, boolean isSummon) {
      int result = 43200;
      if (isSummon) {
         result = (int)((pkCount * 0.375 + 1.0) * 60.0 * 4.0) - 150;
         if (result > 10800) {
            return 10800;
         }
      }

      if (pkCount < 99) {
         result = (int)((pkCount * 0.5 + 1.0) * 60.0 * 12.0);
      } else if (pkCount < 180) {
         result = (int)((pkCount * 0.125 + 37.75) * 60.0 * 12.0);
      }

      return result;
   }

   public static double calcGeneralTraitBonus(Creature attacker, Creature target, TraitType traitType, boolean ignoreResistance) {
      if (traitType == TraitType.NONE) {
         return 1.0;
      } else if (target.getStat().isTraitInvul(traitType)) {
         return 0.0;
      } else {
         switch (traitType.getType()) {
            case 2:
               if (!attacker.getStat().hasAttackTrait(traitType) || !target.getStat().hasDefenceTrait(traitType)) {
                  return 1.0;
               }
               break;
            case 3:
               if (ignoreResistance) {
                  return 1.0;
               }
               break;
            default:
               return 1.0;
         }

         double result = attacker.getStat().getAttackTrait(traitType) - target.getStat().getDefenceTrait(traitType) + 1.0;
         return Math.clamp(result, 0.05, 2.0);
      }
   }

   public static double calcWeaponTraitBonus(Creature attacker, Creature target) {
      TraitType type = attacker.getAttackType().getTraitType();
      double result = target.getStat().getDefenceTraits()[type.ordinal()] - 1.0;
      return 1.0 - result;
   }

   public static double calcAttackTraitBonus(Creature attacker, Creature target) {
      double weaponTraitBonus = calcWeaponTraitBonus(attacker, target);
      if (weaponTraitBonus == 0.0) {
         return 0.0;
      } else {
         double weaknessBonus = 1.0;

         for (TraitType traitType : TraitType.values()) {
            if (traitType.getType() == 2) {
               weaknessBonus *= calcGeneralTraitBonus(attacker, target, traitType, true);
               if (weaknessBonus == 0.0) {
                  return 0.0;
               }
            }
         }

         return Math.clamp(weaponTraitBonus * weaknessBonus, 0.05, 2.0);
      }
   }
}
