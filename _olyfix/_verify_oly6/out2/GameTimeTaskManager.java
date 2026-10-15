package org.l2jmobius.gameserver.taskmanagers;

import java.util.Calendar;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.managers.DayNightSpawnManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class GameTimeTaskManager extends Thread {
   public static final int TICKS_PER_SECOND = 10;
   public static final int MILLIS_IN_TICK = 100;
   public static final int IG_DAYS_PER_DAY = 6;
   public static final int MILLIS_PER_IG_DAY = 14400000;
   public static final int SECONDS_PER_IG_DAY = 14400;
   public static final int TICKS_PER_IG_DAY = 144000;
   private final long _referenceTime;
   private boolean _isNight;
   private int _gameTicks;
   private int _gameTime;
   private int _gameHour;

   protected GameTimeTaskManager() {
      super("GameTimeTaskManager");
      super.setDaemon(true);
      super.setPriority(10);
      Calendar c = Calendar.getInstance();
      c.set(11, 0);
      c.set(12, 0);
      c.set(13, 0);
      c.set(14, 0);
      this._referenceTime = c.getTimeInMillis();
      super.start();
   }

   @Override
   public void run() {
      while (true) {
         this._gameTicks = (int)((System.currentTimeMillis() - this._referenceTime) / 100L);
         this._gameTime = this._gameTicks % 144000 / 100;
         this._gameHour = this._gameTime / 60;
         if (this._gameHour < 6 != this._isNight) {
            this._isNight = !this._isNight;

            for (Player player : World.getPlayers()) {
               if (player.getRace() == Race.DARK_ELF) {
                  Skill shadowSense = player.getKnownSkill(CommonSkill.SHADOW_SENSE.getId());
                  if (shadowSense != null) {
                     if (this._isNight) {
                        player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(shadowSense));
                        player.addSkill(shadowSense, false);
                     } else {
                        player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(shadowSense));
                        player.getEffectList().stopSkillEffects(SkillFinishType.NORMAL, shadowSense);
                     }
                  }
               }
            }

            ThreadPool.execute(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
            if (EventDispatcher.getInstance().hasListener(EventType.ON_DAY_NIGHT_CHANGE)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnDayNightChange(this._isNight), new ListenersContainer[0]);
            }
         }

         try {
            Thread.sleep(100L);
         } catch (InterruptedException var4) {
         }
      }
   }

   public boolean isNight() {
      return this._isNight;
   }

   public int getGameTicks() {
      return this._gameTicks;
   }

   public int getGameTime() {
      return this._gameTime;
   }

   public int getGameHour() {
      return this._gameHour;
   }

   public int getGameMinute() {
      return this._gameTime % 60;
   }

   public static org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager();
   }
}
