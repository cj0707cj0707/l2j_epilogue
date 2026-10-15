package org.l2jmobius.gameserver.mechanics.script;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

public class QuestState {
   protected static final Logger LOGGER = Logger.getLogger(QuestState.class.getName());
   private static final String COND_VAR = "cond";
   private static final String RESTART_VAR = "restartTime";
   private static final String MEMO_VAR = "memoState";
   private static final String MEMO_EX_VAR = "memoStateEx";
   private static final int RESET_HOUR = 6;
   private static final int RESET_MINUTES = 30;
   private final String _questName;
   private final Player _player;
   private byte _state;
   private int _cond = 0;
   private Map<String, String> _vars;
   private boolean _isExitQuestOnCleanUp = false;

   public QuestState(Quest quest, Player player, byte state) {
      this._questName = quest.getName();
      this._player = player;
      this._state = state;
      player.setQuestState(this);
   }

   public String getQuestName() {
      return this._questName;
   }

   public Quest getQuest() {
      return ScriptManager.getInstance().getScript(this._questName);
   }

   public Player getPlayer() {
      return this._player;
   }

   public byte getState() {
      return this._state;
   }

   public boolean isCreated() {
      return this._state == 0;
   }

   public boolean isStarted() {
      return this._state == 1;
   }

   public boolean isCompleted() {
      return this._state == 2;
   }

   public void setState(byte state) {
      this.setState(state, true);
   }

   public void setState(byte state, boolean saveInDb) {
      if (this._state != state) {
         boolean newQuest = this.isCreated();
         this._state = state;
         if (saveInDb) {
            if (newQuest) {
               Quest.createQuestInDb(this);
            } else {
               Quest.updateQuestInDb(this);
            }
         }

         this._player.sendPacket(new QuestList(this._player));
      }
   }

   public void setInternal(String variable, String value) {
      if (this._vars == null) {
         this._vars = new HashMap<>();
      }

      if (value == null) {
         this._vars.put(variable, "");
      } else {
         if ("cond".equals(variable)) {
            try {
               this._cond = Integer.parseInt(value);
            } catch (Exception var4) {
            }
         }

         this._vars.put(variable, value);
      }
   }

   public void set(String variable, int value) {
      this.set(variable, Integer.toString(value));
   }

   public void set(String variable, String value) {
      if (this._vars == null) {
         this._vars = new HashMap<>();
      }

      String newValue = value;
      if (value == null) {
         newValue = "";
      }

      String old = this._vars.put(variable, newValue);
      if (old != null) {
         Quest.updateQuestVarInDb(this, variable, newValue);
      } else {
         Quest.createQuestVarInDb(this, variable, newValue);
      }

      if ("cond".equals(variable)) {
         try {
            int previousVal = 0;

            try {
               previousVal = Integer.parseInt(old);
            } catch (Exception var9) {
            }

            int newCond = 0;

            try {
               newCond = Integer.parseInt(newValue);
            } catch (Exception var8) {
            }

            this._cond = newCond;
            this.setCond(newCond, previousVal);
         } catch (Exception var10) {
            LOGGER.log(
               Level.WARNING,
               this._player.getName()
                  + ", "
                  + this._questName
                  + " cond ["
                  + newValue
                  + "] is not an integer.  Value stored, but no packet was sent: "
                  + var10.getMessage(),
               (Throwable)var10
            );
         }
      }
   }

   private void setCond(int cond, int old) {
      if (cond != old) {
         int completedStateFlags = 0;
         if (cond >= 3 && cond <= 31) {
            completedStateFlags = this.getInt("__compltdStateFlags");
         } else {
            this.unset("__compltdStateFlags");
         }

         if (completedStateFlags == 0) {
            if (cond > old + 1) {
               completedStateFlags = -2147483647;
               completedStateFlags |= (1 << old) - 1;
               completedStateFlags |= 1 << cond - 1;
               this.set("__compltdStateFlags", String.valueOf(completedStateFlags));
            }
         } else if (cond < old) {
            completedStateFlags &= (1 << cond) - 1;
            if (completedStateFlags == (1 << cond) - 1) {
               this.unset("__compltdStateFlags");
            } else {
               completedStateFlags |= -2147483647;
               this.set("__compltdStateFlags", String.valueOf(completedStateFlags));
            }
         } else {
            completedStateFlags |= 1 << cond - 1;
            this.set("__compltdStateFlags", String.valueOf(completedStateFlags));
         }

         this._player.sendPacket(new QuestList(this._player));
         Quest q = this.getQuest();
         if (!q.isCustomQuest() && cond > 0) {
            this._player.sendPacket(new ExShowQuestMark(q.getId()));
         }
      }
   }

   public void unset(String variable) {
      if (this._vars != null) {
         String old = this._vars.remove(variable);
         if (old != null) {
            if ("cond".equals(variable)) {
               this._cond = 0;
            }

            Quest.deleteQuestVarInDb(this, variable);
         }
      }
   }

   public String get(String variable) {
      return this._vars == null ? null : this._vars.get(variable);
   }

   public int getInt(String variable) {
      if (this._vars == null) {
         return 0;
      } else {
         String varStr = this._vars.get(variable);
         if (varStr != null && !varStr.isEmpty()) {
            int varInt = 0;

            try {
               varInt = Integer.parseInt(varStr);
            } catch (NumberFormatException var5) {
               LOGGER.log(
                  Level.INFO,
                  "Quest "
                     + this._questName
                     + ", method getInt("
                     + variable
                     + "), tried to parse a non-integer value ("
                     + varStr
                     + "). Char Id: "
                     + this._player.getObjectId(),
                  (Throwable)var5
               );
            }

            return varInt;
         } else {
            return 0;
         }
      }
   }

   public boolean isCond(int condition) {
      return this._cond == condition;
   }

   public void setCond(int value) {
      if (this.isStarted()) {
         this.set("cond", Integer.toString(value));
      }
   }

   public int getCond() {
      return this.isStarted() ? this._cond : 0;
   }

   public boolean isSet(String variable) {
      return this.get(variable) != null;
   }

   public void setCond(int value, boolean playQuestMiddle) {
      if (this.isStarted()) {
         this.set("cond", String.valueOf(value));
         if (playQuestMiddle) {
            this._player.sendPacket(QuestSound.ITEMSOUND_QUEST_MIDDLE.getPacket());
         }
      }
   }

   public void setMemoState(int value) {
      this.set("memoState", String.valueOf(value));
   }

   public int getMemoState() {
      return this.isStarted() ? this.getInt("memoState") : 0;
   }

   public boolean isMemoState(int memoState) {
      return this.getInt("memoState") == memoState;
   }

   public int getMemoStateEx(int slot) {
      return this.isStarted() ? this.getInt("memoStateEx" + slot) : 0;
   }

   public void setMemoStateEx(int slot, int value) {
      this.set("memoStateEx" + slot, String.valueOf(value));
   }

   public boolean isMemoStateEx(int slot, int memoStateEx) {
      return this.getMemoStateEx(slot) == memoStateEx;
   }

   public void addNotifyOfDeath(Creature creature) {
      if (creature.isPlayer()) {
         creature.asPlayer().addNotifyQuestOfDeath(this);
      }
   }

   public boolean isExitQuestOnCleanUp() {
      return this._isExitQuestOnCleanUp;
   }

   public void setIsExitQuestOnCleanUp(boolean isExitQuestOnCleanUp) {
      this._isExitQuestOnCleanUp = isExitQuestOnCleanUp;
   }

   public void startQuest() {
      if (this.isCreated() && !this.getQuest().isCustomQuest()) {
         this.set("cond", "1");
         this.setState((byte)1);
         this._player.sendPacket(QuestSound.ITEMSOUND_QUEST_ACCEPT.getPacket());
      }
   }

   public void exitQuest(QuestType type) {
      switch (type) {
         case DAILY:
            this.exitQuest(false);
            this.setRestartTime();
            break;
         default:
            this.exitQuest(type == QuestType.REPEATABLE);
      }
   }

   public void exitQuest(QuestType type, boolean playExitQuest) {
      this.exitQuest(type);
      if (playExitQuest) {
         this._player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
      }
   }

   public void exitQuest(boolean repeatable) {
      this._player.removeNotifyQuestOfDeath(this);
      if (this.isStarted()) {
         this.getQuest().removeRegisteredQuestItems(this._player);
         Quest.deleteQuestInDb(this, repeatable);
         if (repeatable) {
            this._player.delQuestState(this._questName);
            this._player.sendPacket(new QuestList(this._player));
         } else {
            this.setState((byte)2);
         }

         this._vars = null;
      }
   }

   public void exitQuest(boolean repeatable, boolean playExitQuest) {
      this.exitQuest(repeatable);
      if (playExitQuest) {
         this._player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
      }
   }

   public void showQuestionMark(int number) {
      this._player.sendPacket(new TutorialShowQuestionMark(number));
   }

   public void setRestartTime() {
      Calendar reDo = Calendar.getInstance();
      if (reDo.get(11) >= 6) {
         reDo.add(5, 1);
      }

      reDo.set(11, 6);
      reDo.set(12, 30);
      this.set("restartTime", String.valueOf(reDo.getTimeInMillis()));
   }

   public boolean isNowAvailable() {
      String val = this.get("restartTime");
      return val != null && Long.parseLong(val) <= System.currentTimeMillis();
   }
}
