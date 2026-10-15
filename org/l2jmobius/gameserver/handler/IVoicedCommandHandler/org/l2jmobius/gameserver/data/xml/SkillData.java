package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.DocumentSkill;

public class SkillData {
   private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
   private final Map<Integer, Skill> _skillsByHash = new ConcurrentHashMap<>();
   private final Map<Integer, Integer> _maxSkillLevels = new ConcurrentHashMap<>();
   private final Set<Integer> _enchantable = ConcurrentHashMap.newKeySet();
   private final List<File> _skillFiles = new ArrayList<>();
   private static int count = 0;

   protected SkillData() {
      this.processDirectory("data/stats/skills", this._skillFiles);
      if (GeneralConfig.CUSTOM_SKILLS_LOAD) {
         this.processDirectory("data/stats/skills/custom", this._skillFiles);
      }

      this.load();
   }

   private void processDirectory(String dirName, List<File> list) {
      File dir = new File(ServerConfig.DATAPACK_ROOT, dirName);
      if (!dir.exists()) {
         LOGGER.warning("Directory " + dir.getAbsolutePath() + " does not exist.");
      } else {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                  list.add(file);
               }
            }
         }
      }
   }

   private void load() {
      Map<Integer, Skill> temp = new ConcurrentHashMap<>();
      this.loadAllSkills(temp);
      this._skillsByHash.clear();
      this._skillsByHash.putAll(temp);
      this._maxSkillLevels.clear();
      this._enchantable.clear();

      for (Skill skill : this._skillsByHash.values()) {
         int skillId = skill.getId();
         int skillLevel = skill.getLevel();
         if (skillLevel > 99) {
            if (!this._enchantable.contains(skillId)) {
               this._enchantable.add(skillId);
            }
         } else {
            int maxLevel = this.getMaxLevel(skillId);
            if (skillLevel > maxLevel) {
               this._maxSkillLevels.put(skillId, skillLevel);
            }
         }
      }
   }

   public List<Skill> loadSkills(File file) {
      if (file == null) {
         LOGGER.warning("Skill file not found.");
         return null;
      } else {
         DocumentSkill doc = new DocumentSkill(file);
         doc.parse();
         return doc.getSkills();
      }
   }

   public void loadAllSkills(Map<Integer, Skill> allSkills) {
      if (ThreadConfig.THREADS_FOR_LOADING) {
         Collection<ScheduledFuture<?>> jobs = ConcurrentHashMap.newKeySet();

         for (File file : this._skillFiles) {
            jobs.add(ThreadPool.schedule(() -> {
               List<Skill> skillsx = this.loadSkills(file);
               if (skillsx != null) {
                  for (Skill skillx : skillsx) {
                     allSkills.put(getSkillHashCode(skillx), skillx);
                     count++;
                  }
               }
            }, 0L));
         }

         while (!jobs.isEmpty()) {
            for (ScheduledFuture<?> job : jobs) {
               if (job == null || job.isDone() || job.isCancelled()) {
                  jobs.remove(job);
               }
            }
         }
      } else {
         for (File file : this._skillFiles) {
            List<Skill> skills = this.loadSkills(file);
            if (skills == null) {
               return;
            }

            for (Skill skill : skills) {
               allSkills.put(getSkillHashCode(skill), skill);
               count++;
            }
         }
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + count + " Skill templates from XML files.");
   }

   public void reload() {
      this.load();
      SkillTreeData.getInstance().load();
   }

   public static int getSkillHashCode(Skill skill) {
      return getSkillHashCode(skill.getId(), skill.getLevel());
   }

   public static int getSkillHashCode(int skillId, int skillLevel) {
      return skillId * 1021 + skillLevel;
   }

   public Skill getSkill(int skillId, int level) {
      Skill result = this._skillsByHash.get(getSkillHashCode(skillId, level));
      if (result != null) {
         return result;
      } else {
         int maxLevel = this.getMaxLevel(skillId);
         if (maxLevel > 0 && level > maxLevel) {
            LOGGER.warning(
               StringUtil.concat(
                  this.getClass().getSimpleName(),
                  ": Call to unexisting skill level id: ",
                  String.valueOf(skillId),
                  " requested level: ",
                  String.valueOf(level),
                  " max level: ",
                  String.valueOf(maxLevel),
                  ".",
                  System.lineSeparator(),
                  TraceUtil.getStackTrace(new Exception())
               )
            );
            return this._skillsByHash.get(getSkillHashCode(skillId, maxLevel));
         } else {
            LOGGER.warning(
               StringUtil.concat(
                  this.getClass().getSimpleName(),
                  ": No skill info found for skill id ",
                  String.valueOf(skillId),
                  " and skill level ",
                  String.valueOf(level),
                  ".",
                  System.lineSeparator(),
                  TraceUtil.getStackTrace(new Exception())
               )
            );
            return null;
         }
      }
   }

   public int getMaxLevel(int skillId) {
      Integer maxLevel = this._maxSkillLevels.get(skillId);
      return maxLevel != null ? maxLevel : 0;
   }

   public boolean isEnchantable(int skillId) {
      return this._enchantable.contains(skillId);
   }

   public Skill[] getSiegeSkills(boolean addNoble, boolean hasCastle) {
      Skill[] result = new Skill[2 + (addNoble ? 1 : 0) + (hasCastle ? 2 : 0)];
      int i = 0;
      result[i++] = this._skillsByHash.get(getSkillHashCode(246, 1));
      result[i++] = this._skillsByHash.get(getSkillHashCode(247, 1));
      if (addNoble) {
         result[i++] = this._skillsByHash.get(getSkillHashCode(326, 1));
      }

      if (hasCastle) {
         result[i++] = this._skillsByHash.get(getSkillHashCode(844, 1));
         result[i++] = this._skillsByHash.get(getSkillHashCode(845, 1));
      }

      return result;
   }

   public static SkillData getInstance() {
      return SkillData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SkillData INSTANCE = new SkillData();
   }
}
