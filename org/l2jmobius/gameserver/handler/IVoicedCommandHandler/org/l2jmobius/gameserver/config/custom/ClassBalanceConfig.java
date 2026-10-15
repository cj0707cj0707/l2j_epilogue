package org.l2jmobius.gameserver.config.custom;

import java.util.Arrays;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

public class ClassBalanceConfig {
   private static final String CLASS_BALANCE_CONFIG_FILE = "./config/Custom/ClassBalance.ini";
   public static float[] PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[137];
   public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[137];
   public static float[] PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[137];
   public static float[] PLAYER_HEALING_SKILL_MULTIPLIERS = new float[137];
   public static float[] SKILL_MASTERY_CHANCE_MULTIPLIERS = new float[137];
   public static float[] SKILL_REUSE_MULTIPLIERS = new float[137];
   public static float[] EXP_AMOUNT_MULTIPLIERS = new float[137];
   public static float[] SP_AMOUNT_MULTIPLIERS = new float[137];

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/ClassBalance.ini");
      Arrays.fill(PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pveMagicalSkillDamageMultipliers = config.getString("PveMagicalSkillDamageMultipliers", "").trim().split(";");
      if (pveMagicalSkillDamageMultipliers.length > 0) {
         for (String info : pveMagicalSkillDamageMultipliers) {
            String[] classInfo = info.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpMagicalSkillDamageMultipliers = config.getString("PvpMagicalSkillDamageMultipliers", "").trim().split(";");
      if (pvpMagicalSkillDamageMultipliers.length > 0) {
         for (String infox : pvpMagicalSkillDamageMultipliers) {
            String[] classInfo = infox.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pveMagicalSkillDefenceMultipliers = config.getString("PveMagicalSkillDefenceMultipliers", "").trim().split(";");
      if (pveMagicalSkillDefenceMultipliers.length > 0) {
         for (String infoxx : pveMagicalSkillDefenceMultipliers) {
            String[] classInfo = infoxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvpMagicalSkillDefenceMultipliers = config.getString("PvpMagicalSkillDefenceMultipliers", "").trim().split(";");
      if (pvpMagicalSkillDefenceMultipliers.length > 0) {
         for (String infoxxx : pvpMagicalSkillDefenceMultipliers) {
            String[] classInfo = infoxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pveMagicalSkillCriticalChanceMultipliers = config.getString("PveMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
      if (pveMagicalSkillCriticalChanceMultipliers.length > 0) {
         for (String infoxxxx : pveMagicalSkillCriticalChanceMultipliers) {
            String[] classInfo = infoxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pvpMagicalSkillCriticalChanceMultipliers = config.getString("PvpMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
      if (pvpMagicalSkillCriticalChanceMultipliers.length > 0) {
         for (String infoxxxxx : pvpMagicalSkillCriticalChanceMultipliers) {
            String[] classInfo = infoxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pveMagicalSkillCriticalDamageMultipliers = config.getString("PveMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
      if (pveMagicalSkillCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxx : pveMagicalSkillCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpMagicalSkillCriticalDamageMultipliers = config.getString("PvpMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
      if (pvpMagicalSkillCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxxx : pvpMagicalSkillCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalSkillDamageMultipliers = config.getString("PvePhysicalSkillDamageMultipliers", "").trim().split(";");
      if (pvePhysicalSkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxx : pvePhysicalSkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalSkillDamageMultipliers = config.getString("PvpPhysicalSkillDamageMultipliers", "").trim().split(";");
      if (pvpPhysicalSkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxx : pvpPhysicalSkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalSkillDefenceMultipliers = config.getString("PvePhysicalSkillDefenceMultipliers", "").trim().split(";");
      if (pvePhysicalSkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxx : pvePhysicalSkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalSkillDefenceMultipliers = config.getString("PvpPhysicalSkillDefenceMultipliers", "").trim().split(";");
      if (pvpPhysicalSkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxx : pvpPhysicalSkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalSkillCriticalChanceMultipliers = config.getString("PvePhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
      if (pvePhysicalSkillCriticalChanceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxx : pvePhysicalSkillCriticalChanceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalSkillCriticalChanceMultipliers = config.getString("PvpPhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
      if (pvpPhysicalSkillCriticalChanceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxx : pvpPhysicalSkillCriticalChanceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalSkillCriticalDamageMultipliers = config.getString("PvePhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
      if (pvePhysicalSkillCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxx : pvePhysicalSkillCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalSkillCriticalDamageMultipliers = config.getString("PvpPhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
      if (pvpPhysicalSkillCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxx : pvpPhysicalSkillCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalAttackDamageMultipliers = config.getString("PvePhysicalAttackDamageMultipliers", "").trim().split(";");
      if (pvePhysicalAttackDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxx : pvePhysicalAttackDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalAttackDamageMultipliers = config.getString("PvpPhysicalAttackDamageMultipliers", "").trim().split(";");
      if (pvpPhysicalAttackDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxx : pvpPhysicalAttackDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalAttackDefenceMultipliers = config.getString("PvePhysicalAttackDefenceMultipliers", "").trim().split(";");
      if (pvePhysicalAttackDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxx : pvePhysicalAttackDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalAttackDefenceMultipliers = config.getString("PvpPhysicalAttackDefenceMultipliers", "").trim().split(";");
      if (pvpPhysicalAttackDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxx : pvpPhysicalAttackDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalAttackCriticalChanceMultipliers = config.getString("PvePhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
      if (pvePhysicalAttackCriticalChanceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxx : pvePhysicalAttackCriticalChanceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalAttackCriticalChanceMultipliers = config.getString("PvpPhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
      if (pvpPhysicalAttackCriticalChanceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxx : pvpPhysicalAttackCriticalChanceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvePhysicalAttackCriticalDamageMultipliers = config.getString("PvePhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
      if (pvePhysicalAttackCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxx : pvePhysicalAttackCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpPhysicalAttackCriticalDamageMultipliers = config.getString("PvpPhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
      if (pvpPhysicalAttackCriticalDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxx : pvpPhysicalAttackCriticalDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pveBlowSkillDamageMultipliers = config.getString("PveBlowSkillDamageMultipliers", "").trim().split(";");
      if (pveBlowSkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxx : pveBlowSkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpBlowSkillDamageMultipliers = config.getString("PvpBlowSkillDamageMultipliers", "").trim().split(";");
      if (pvpBlowSkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxx : pvpBlowSkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pveBlowSkillDefenceMultipliers = config.getString("PveBlowSkillDefenceMultipliers", "").trim().split(";");
      if (pveBlowSkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxx : pveBlowSkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvpBlowSkillDefenceMultipliers = config.getString("PvpBlowSkillDefenceMultipliers", "").trim().split(";");
      if (pvpBlowSkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxx : pvpBlowSkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pveEnergySkillDamageMultipliers = config.getString("PveEnergySkillDamageMultipliers", "").trim().split(";");
      if (pveEnergySkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxx : pveEnergySkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1.0F);
      String[] pvpEnergySkillDamageMultipliers = config.getString("PvpEnergySkillDamageMultipliers", "").trim().split(";");
      if (pvpEnergySkillDamageMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : pvpEnergySkillDamageMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pveEnergySkillDefenceMultipliers = config.getString("PveEnergySkillDefenceMultipliers", "").trim().split(";");
      if (pveEnergySkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : pveEnergySkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1.0F);
      String[] pvpEnergySkillDefenceMultipliers = config.getString("PvpEnergySkillDefenceMultipliers", "").trim().split(";");
      if (pvpEnergySkillDefenceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : pvpEnergySkillDefenceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(PLAYER_HEALING_SKILL_MULTIPLIERS, 1.0F);
      String[] playerHealingSkillMultipliers = config.getString("PlayerHealingSkillMultipliers", "").trim().split(";");
      if (playerHealingSkillMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : playerHealingSkillMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               PLAYER_HEALING_SKILL_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(SKILL_MASTERY_CHANCE_MULTIPLIERS, 1.0F);
      String[] skillMasteryChanceMultipliers = config.getString("SkillMasteryChanceMultipliers", "").trim().split(";");
      if (skillMasteryChanceMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : skillMasteryChanceMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               SKILL_MASTERY_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(SKILL_REUSE_MULTIPLIERS, 1.0F);
      String[] skillReuseMultipliers = config.getString("SkillReuseMultipliers", "").trim().split(";");
      if (skillReuseMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : skillReuseMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               SKILL_REUSE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(EXP_AMOUNT_MULTIPLIERS, 1.0F);
      String[] expAmountMultipliers = config.getString("ExpAmountMultipliers", "").trim().split(";");
      if (expAmountMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : expAmountMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               EXP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }

      Arrays.fill(SP_AMOUNT_MULTIPLIERS, 1.0F);
      String[] spAmountMultipliers = config.getString("SpAmountMultipliers", "").trim().split(";");
      if (spAmountMultipliers.length > 0) {
         for (String infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : spAmountMultipliers) {
            String[] classInfo = infoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.trim().split("[*]");
            if (classInfo.length == 2) {
               String id = classInfo[0].trim();
               SP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(
                  classInfo[1].trim()
               );
            }
         }
      }
   }
}
