package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class DailyResetManager {
   private static final Logger LOGGER = Logger.getLogger(DailyResetManager.class.getName());
   private static final int[] RESET_SKILLS = new int[]{2510, 22180};

   protected DailyResetManager() {
      long nextResetTime = TimeUtil.getNextTime(6, 30).getTimeInMillis();
      long currentTime = System.currentTimeMillis();
      Calendar calendar = Calendar.getInstance();
      calendar.set(11, 6);
      calendar.set(12, 30);
      calendar.set(13, 0);
      calendar.set(14, 0);
      long currentResetTime = calendar.getTimeInMillis();
      if (currentTime >= currentResetTime && GlobalVariablesManager.getInstance().getLong("DAILY_TASK_RESET", 0L) <= currentResetTime) {
         LOGGER.info(this.getClass().getSimpleName() + ": Daily task will run now.");
         this.onReset();
      } else {
         LOGGER.info(this.getClass().getSimpleName() + ": Next schedule at " + TimeUtil.getDateTimeString(nextResetTime) + ".");
      }

      long startDelay = Math.max(0L, nextResetTime - currentTime);
      ThreadPool.scheduleAtFixedRate(this::onReset, startDelay, 86400000L);
      ThreadPool.scheduleAtFixedRate(this::onSave, 1800000L, 1800000L);
      ThreadPool.scheduleAtFixedRate(this::resetRecommends, TimeUtil.getNextTime(13, 0).getTimeInMillis() - currentTime, 86400000L);
   }

   private void onReset() {
      LOGGER.info("Starting reset of daily tasks...");
      GlobalVariablesManager.getInstance().set("DAILY_TASK_RESET", System.currentTimeMillis());
      Calendar calendar = Calendar.getInstance();
      if (calendar.get(7) == 4) {
         this.clanLeaderApply();
      }

      this.resetDailySkills();
      if (EventDispatcher.getInstance().hasListener(EventType.ON_DAILY_RESET)) {
         EventDispatcher.getInstance().notifyEvent(new OnDailyReset());
      }

      for (Player player : World.getPlayers()) {
         player.getVariables().storeMe();
         player.getAccountVariables().storeMe();
      }

      LOGGER.info("Daily tasks reset completed.");
   }

   private void onSave() {
      GlobalVariablesManager.getInstance().storeMe();
      if (Olympiad.getInstance().inCompPeriod()) {
         Olympiad.getInstance().saveOlympiadStatus();
         LOGGER.info("Olympiad System: Data updated.");
      }

      SevenSigns.getInstance().saveSevenSignsStatus();
      if (!SevenSigns.getInstance().isSealValidationPeriod()) {
         SevenSignsFestival.getInstance().saveFestivalData(false);
      }

      LOGGER.info("SevenSigns: Data updated.");
   }

   private void clanLeaderApply() {
      for (Clan clan : ClanTable.getInstance().getClans()) {
         if (clan.getNewLeaderId() != 0) {
            ClanMember member = clan.getClanMember(clan.getNewLeaderId());
            if (member != null) {
               clan.setNewLeader(member);
            }
         }
      }

      LOGGER.info("Clan leaders have been updated.");
   }

   private void resetDailySkills() {
      try (Connection con = DatabaseFactory.getConnection()) {
         for (int skillId : RESET_SKILLS) {
            try (PreparedStatement ps = con.prepareStatement(
                  "DELETE FROM character_skills_save WHERE skill_id=? AND charId IN (SELECT charId FROM characters WHERE online = 0)"
               )) {
               ps.setInt(1, skillId);
               ps.execute();
            }
         }
      } catch (Exception var13) {
         LOGGER.severe(this.getClass().getSimpleName() + ": Could not reset daily skill reuse: " + var13);
      }

      for (int skillId : RESET_SKILLS) {
         Skill skill = SkillData.getInstance().getSkill(skillId, 1);
         if (skill != null) {
            for (Player player : World.getPlayers()) {
               if (player.hasSkillReuse(skill.getReuseHashCode())) {
                  player.removeTimeStamp(skill);
               }
            }
         }
      }

      LOGGER.info("Daily skill reuse cleaned.");
   }

   private void resetRecommends() {
      for (Player player : World.getPlayers()) {
         player.restartRecom();
         player.updateUserInfo();
      }
   }

   public static DailyResetManager getInstance() {
      return DailyResetManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final DailyResetManager INSTANCE = new DailyResetManager();
   }
}
