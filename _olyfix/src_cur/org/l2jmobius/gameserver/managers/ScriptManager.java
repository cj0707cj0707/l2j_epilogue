package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

public class ScriptManager {
   private static final Logger LOGGER = Logger.getLogger(ScriptManager.class.getName());
   private final Map<String, Quest> _quests = new ConcurrentHashMap<>();
   private final Map<String, Quest> _scripts = new ConcurrentHashMap<>();

   protected ScriptManager() {
   }

   public void reload(String questFolder) {
      Quest q = this.getScript(questFolder);
      if (q != null) {
         q.reload();
      }
   }

   public void reload(int questId) {
      Quest q = this.getQuest(questId);
      if (q != null) {
         q.reload();
      }
   }

   public void reloadAllScripts() {
      this.unloadAllScripts();
      LOGGER.info("Reloading all server scripts.");

      try {
         ScriptEngine.getInstance().executeScriptList();
      } catch (Exception var2) {
         LOGGER.log(Level.SEVERE, "Failed executing script list!", (Throwable)var2);
      }

      getInstance().report();
   }

   public void unloadAllScripts() {
      LOGGER.info("Unloading all server scripts.");

      for (Quest quest : this._quests.values()) {
         if (quest != null) {
            quest.unload(false);
         }
      }

      this._quests.clear();

      for (Quest script : this._scripts.values()) {
         if (script != null) {
            script.unload(false);
         }
      }

      this._scripts.clear();
   }

   public void report() {
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._quests.size() + " quests.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._scripts.size() + " scripts.");
   }

   public void save() {
      for (Quest quest : this._quests.values()) {
         quest.onSave();
      }

      for (Quest script : this._scripts.values()) {
         script.onSave();
      }
   }

   public Quest getScript(String name) {
      Quest quest = this._quests.get(name);
      return quest != null ? quest : this._scripts.get(name);
   }

   public Quest getQuest(int questId) {
      for (Quest q : this._quests.values()) {
         if (q.getId() == questId) {
            return q;
         }
      }

      return null;
   }

   public void addQuest(Quest quest) {
      if (quest == null) {
         throw new IllegalArgumentException("Quest argument cannot be null");
      } else {
         Quest old = this._quests.put(quest.getName(), quest);
         if (old != null) {
            old.unload();
            LOGGER.info("Replaced quest " + old.getName() + " (" + old.getId() + ") with a new version!");
         }

         if (DevelopmentConfig.SHOW_QUEST_LOAD_IN_LOGS) {
            String questName = quest.getName().contains("_") ? quest.getName().substring(quest.getName().indexOf(95) + 1) : quest.getName();
            LOGGER.info("Loaded quest " + StringUtil.separateWords(questName) + ".");
         }
      }
   }

   public boolean removeScript(Quest script) {
      if (this._quests.containsKey(script.getName())) {
         this._quests.remove(script.getName());
         return true;
      } else if (this._scripts.containsKey(script.getName())) {
         this._scripts.remove(script.getName());
         return true;
      } else {
         return false;
      }
   }

   public Map<String, Quest> getQuests() {
      return this._quests;
   }

   public boolean unload(Quest ms) {
      ms.onSave();
      return this.removeScript(ms);
   }

   public Map<String, Quest> getScripts() {
      return this._scripts;
   }

   public void addScript(Quest script) {
      Quest old = this._scripts.put(script.getClass().getSimpleName(), script);
      if (old != null) {
         old.unload();
         LOGGER.info("Replaced script " + old.getName() + " with a new version!");
      }

      if (DevelopmentConfig.SHOW_SCRIPT_LOAD_IN_LOGS) {
         LOGGER.info("Loaded script " + StringUtil.separateWords(script.getClass().getSimpleName()) + ".");
      }
   }

   public static ScriptManager getInstance() {
      return ScriptManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ScriptManager INSTANCE = new ScriptManager();
   }
}
