package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class BotReportTable {
   protected static final Logger LOGGER = Logger.getLogger(BotReportTable.class.getName());
   private static final int COLUMN_BOT_ID = 1;
   private static final int COLUMN_REPORTER_ID = 2;
   private static final int COLUMN_REPORT_TIME = 3;
   public static final int ATTACK_ACTION_BLOCK_ID = -1;
   public static final int TRADE_ACTION_BLOCK_ID = -2;
   public static final int PARTY_ACTION_BLOCK_ID = -3;
   public static final int ACTION_BLOCK_ID = -4;
   public static final int CHAT_BLOCK_ID = -5;
   private static final String SQL_LOAD_REPORTED_CHAR_DATA = "SELECT * FROM bot_reported_char_data";
   private static final String SQL_INSERT_REPORTED_CHAR_DATA = "INSERT INTO bot_reported_char_data VALUES (?,?,?)";
   private static final String SQL_CLEAR_REPORTED_CHAR_DATA = "DELETE FROM bot_reported_char_data";
   private Map<Integer, Long> _ipRegistry;
   private Map<Integer, BotReportTable.ReporterCharData> _charRegistry;
   private Map<Integer, BotReportTable.ReportedCharData> _reports;
   private Map<Integer, BotReportTable.PunishHolder> _punishments;

   protected BotReportTable() {
      if (GeneralConfig.BOTREPORT_ENABLE) {
         this._ipRegistry = new HashMap<>();
         this._charRegistry = new ConcurrentHashMap<>();
         this._reports = new ConcurrentHashMap<>();
         this._punishments = new ConcurrentHashMap<>();

         try {
            File punishments = new File("./config/BotReportPunishments.xml");
            if (!punishments.exists()) {
               throw new FileNotFoundException(punishments.getName());
            }

            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(punishments, new BotReportTable.PunishmentsLoader());
         } catch (Exception var3) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load punishments from /config/BotReportPunishments.xml", (Throwable)var3);
         }

         this.loadReportedCharData();
         this.scheduleResetPointTask();
      }
   }

   private void loadReportedCharData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement st = con.createStatement();
         ResultSet rset = st.executeQuery("SELECT * FROM bot_reported_char_data");
      ) {
         long lastResetTime = 0L;

         try {
            int hour = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[0]);
            int minute = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[1]);
            long currentTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.set(11, hour);
            calendar.set(12, minute);
            if (currentTime < calendar.getTimeInMillis()) {
               calendar.set(6, calendar.get(6) - 1);
            }

            lastResetTime = calendar.getTimeInMillis();
         } catch (Exception var15) {
         }

         while (rset.next()) {
            int botId = rset.getInt(1);
            int reporter = rset.getInt(2);
            long date = rset.getLong(3);
            BotReportTable.ReportedCharData existing = this._reports.get(botId);
            if (existing != null) {
               existing.addReporter(reporter, date);
            } else {
               BotReportTable.ReportedCharData rcd = new BotReportTable.ReportedCharData();
               rcd.addReporter(reporter, date);
               this._reports.put(rset.getInt(1), rcd);
            }

            if (date > lastResetTime) {
               BotReportTable.ReporterCharData rcd = this._charRegistry.get(reporter);
               if (rcd != null) {
                  rcd.setPoints(rcd.getPointsLeft() - 1);
               } else {
                  rcd = new BotReportTable.ReporterCharData();
                  rcd.setPoints(6);
                  this._charRegistry.put(reporter, rcd);
               }
            }
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._reports.size() + " bot reports");
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load reported char data!", (Throwable)var19);
      }
   }

   public void saveReportedCharData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement st = con.createStatement();
         PreparedStatement ps = con.prepareStatement("INSERT INTO bot_reported_char_data VALUES (?,?,?)");
      ) {
         st.execute("DELETE FROM bot_reported_char_data");

         for (Entry<Integer, BotReportTable.ReportedCharData> entrySet : this._reports.entrySet()) {
            for (int reporterId : entrySet.getValue()._reporters.keySet()) {
               ps.setInt(1, entrySet.getKey());
               ps.setInt(2, reporterId);
               ps.setLong(3, entrySet.getValue()._reporters.get(reporterId));
               ps.execute();
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Could not update reported char data in database!", (Throwable)var14);
      }
   }

   public boolean reportBot(Player reporter) {
      WorldObject target = reporter.getTarget();
      if (target == null) {
         return false;
      } else {
         Creature bot = target.asCreature();
         if ((bot.isPlayer() || bot.isFakePlayer())
            && (!bot.isFakePlayer() || bot.asNpc().getTemplate().getFakePlayerInfo().isTalkable())
            && target.getObjectId() != reporter.getObjectId()) {
            if (bot.isInsideZone(ZoneId.PEACE) || bot.isInsideZone(ZoneId.PVP)) {
               reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_A_CHARACTER_WHO_IS_IN_A_PEACE_ZONE_OR_A_BATTLEFIELD);
               return false;
            } else if (bot.isPlayer() && bot.asPlayer().isInOlympiadMode()) {
               reporter.sendPacket(
                  SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_YOU_CANNOT_MAKE_A_REPORT_WHILE_LOCATED_INSIDE_A_PEACE_ZONE_OR_A_BATTLEFIELD_WHILE_YOU_ARE_AN_OPPOSING_CLAN_MEMBER_DURING_A_CLAN_WAR_OR_WHILE_PARTICIPATING_IN_THE_OLYMPIAD
               );
               return false;
            } else if (bot.getClan() != null && reporter.getClan() != null && bot.getClan().isAtWarWith(reporter.getClan())) {
               reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_WHEN_A_CLAN_WAR_HAS_BEEN_DECLARED);
               return false;
            } else if (bot.isPlayer() && bot.asPlayer().getExp() == bot.asPlayer().getStat().getStartingExp()) {
               reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_A_CHARACTER_WHO_HAS_NOT_ACQUIRED_ANY_EXP_AFTER_CONNECTING);
               return false;
            } else {
               BotReportTable.ReportedCharData rcd = this._reports.get(bot.getObjectId());
               BotReportTable.ReporterCharData rcdRep = this._charRegistry.get(reporter.getObjectId());
               int reporterId = reporter.getObjectId();
               synchronized (this) {
                  if (this._reports.containsKey(reporterId)) {
                     reporter.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CANNOT_REPORT_OTHER_USERS);
                     return false;
                  }

                  int ip = hashIp(reporter);
                  if (!timeHasPassed(this._ipRegistry, ip)) {
                     reporter.sendPacket(
                        SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_THE_TARGET_HAS_ALREADY_BEEN_REPORTED_BY_EITHER_YOUR_CLAN_OR_ALLIANCE_OR_HAS_ALREADY_BEEN_REPORTED_FROM_YOUR_CURRENT_IP
                     );
                     return false;
                  }

                  if (rcd != null) {
                     if (rcd.alredyReportedBy(reporterId)) {
                        reporter.sendPacket(SystemMessageId.C1_PURCHASED_S3_S2_S);
                        return false;
                     }

                     if (!GeneralConfig.BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS && rcd.reportedBySameClan(reporter.getClan())) {
                        reporter.sendPacket(
                           SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_THE_TARGET_HAS_ALREADY_BEEN_REPORTED_BY_EITHER_YOUR_CLAN_OR_ALLIANCE_OR_HAS_ALREADY_BEEN_REPORTED_FROM_YOUR_CURRENT_IP
                        );
                        return false;
                     }
                  }

                  if (rcdRep != null) {
                     if (rcdRep.getPointsLeft() == 0) {
                        reporter.sendPacket(SystemMessageId.YOU_HAVE_USED_ALL_AVAILABLE_POINTS_POINTS_ARE_RESET_EVERYDAY_AT_NOON);
                        return false;
                     }

                     long reuse = System.currentTimeMillis() - rcdRep.getLastReporTime();
                     if (reuse < GeneralConfig.BOTREPORT_REPORT_DELAY) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.FIVE_YEARS_HAVE_PASSED_SINCE_THIS_CHARACTER_S_CREATION);
                        sm.addInt((int)(reuse / 60000L));
                        sm.addInt(rcdRep.getPointsLeft());
                        reporter.sendPacket(sm);
                        return false;
                     }
                  }

                  long curTime = System.currentTimeMillis();
                  if (rcd == null) {
                     rcd = new BotReportTable.ReportedCharData();
                     this._reports.put(bot.getObjectId(), rcd);
                  }

                  rcd.addReporter(reporterId, curTime);
                  if (rcdRep == null) {
                     rcdRep = new BotReportTable.ReporterCharData();
                  }

                  rcdRep.registerReport(curTime);
                  this._ipRegistry.put(ip, curTime);
                  this._charRegistry.put(reporterId, rcdRep);
               }

               SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_REPORTED_AS_A_BOT);
               sm.addString(bot.getName());
               reporter.sendPacket(sm);
               sm = new SystemMessage(SystemMessageId.YOU_HAVE_USED_A_REPORT_POINT_ON_C1_YOU_HAVE_S2_POINTS_REMAINING_ON_THIS_ACCOUNT);
               sm.addString(bot.getName());
               sm.addInt(rcdRep.getPointsLeft());
               reporter.sendPacket(sm);
               if (bot.isPlayer()) {
                  this.handleReport(bot.asPlayer(), rcd);
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   private void handleReport(Player bot, BotReportTable.ReportedCharData rcd) {
      this.punishBot(bot, this._punishments.get(rcd.getReportCount()));

      for (Entry<Integer, BotReportTable.PunishHolder> entry : this._punishments.entrySet()) {
         int key = entry.getKey();
         if (key < 0 && Math.abs(key) <= rcd.getReportCount()) {
            this.punishBot(bot, entry.getValue());
         }
      }
   }

   private void punishBot(Player bot, BotReportTable.PunishHolder ph) {
      if (ph != null) {
         ph._punish.applyEffects(bot, bot);
         if (ph._systemMessageId > -1) {
            SystemMessageId id = SystemMessageId.getSystemMessageId(ph._systemMessageId);
            if (id != null) {
               bot.sendPacket(id);
            }
         }
      }
   }

   void addPunishment(int neededReports, int skillId, int skillLevel, int sysMsg) {
      Skill sk = SkillData.getInstance().getSkill(skillId, skillLevel);
      if (sk != null) {
         this._punishments.put(neededReports, new BotReportTable.PunishHolder(sk, sysMsg));
      } else {
         LOGGER.warning(
            this.getClass().getSimpleName()
               + ": Could not add punishment for "
               + neededReports
               + " report(s): Skill "
               + skillId
               + "-"
               + skillLevel
               + " does not exist!"
         );
      }
   }

   void resetPointsAndSchedule() {
      synchronized (this._charRegistry) {
         for (BotReportTable.ReporterCharData rcd : this._charRegistry.values()) {
            rcd.setPoints(7);
         }
      }

      this.scheduleResetPointTask();
   }

   private void scheduleResetPointTask() {
      try {
         int hour = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[0]);
         int minute = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[1]);
         long currentTime = System.currentTimeMillis();
         Calendar calendar = Calendar.getInstance();
         calendar.set(11, hour);
         calendar.set(12, minute);
         if (calendar.getTimeInMillis() < currentTime) {
            calendar.add(6, 1);
         }

         ThreadPool.schedule(new BotReportTable.ResetPointTask(), calendar.getTimeInMillis() - currentTime);
      } catch (Exception var6) {
         ThreadPool.schedule(new BotReportTable.ResetPointTask(), 86400000L);
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": Could not properly schedule bot report points reset task. Scheduled in 24 hours.",
            (Throwable)var6
         );
      }
   }

   public static BotReportTable getInstance() {
      return BotReportTable.SingletonHolder.INSTANCE;
   }

   private static int hashIp(Player player) {
      String con = player.getClient().getIp();
      String[] rawByte = con.split("\\.");
      int[] rawIp = new int[4];

      for (int i = 0; i < 4; i++) {
         rawIp[i] = Integer.parseInt(rawByte[i]);
      }

      return rawIp[0] | rawIp[1] << 8 | rawIp[2] << 16 | rawIp[3] << 24;
   }

   private static boolean timeHasPassed(Map<Integer, Long> map, int objectId) {
      Long time = map.get(objectId);
      return time == null || System.currentTimeMillis() - time > GeneralConfig.BOTREPORT_REPORT_DELAY;
   }

   private class PunishHolder {
      final Skill _punish;
      final int _systemMessageId;

      public PunishHolder(Skill sk, int sysMsg) {
         Objects.requireNonNull(BotReportTable.this);
         super();
         this._punish = sk;
         this._systemMessageId = sysMsg;
      }
   }

   private class PunishmentsLoader extends DefaultHandler {
      PunishmentsLoader() {
         Objects.requireNonNull(BotReportTable.this);
         super();
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attr) {
         if (qName.equals("punishment")) {
            int reportCount = -1;
            int skillId = -1;
            int skillLevel = 1;
            int sysMessage = -1;

            try {
               reportCount = Integer.parseInt(attr.getValue("neededReportCount"));
               skillId = Integer.parseInt(attr.getValue("skillId"));
               String level = attr.getValue("skillLevel");
               String systemMessageId = attr.getValue("sysMessageId");
               if (level != null) {
                  skillLevel = Integer.parseInt(level);
               }

               if (systemMessageId != null) {
                  sysMessage = Integer.parseInt(systemMessageId);
               }
            } catch (Exception var11) {
               BotReportTable.LOGGER.warning("Problem with BotReportTable: " + var11.getMessage());
            }

            BotReportTable.this.addPunishment(reportCount, skillId, skillLevel, sysMessage);
         }
      }
   }

   private class ReportedCharData {
      Map<Integer, Long> _reporters;

      ReportedCharData() {
         Objects.requireNonNull(BotReportTable.this);
         super();
         this._reporters = new HashMap<>();
      }

      int getReportCount() {
         return this._reporters.size();
      }

      boolean alredyReportedBy(int objectId) {
         return this._reporters.containsKey(objectId);
      }

      void addReporter(int objectId, long reportTime) {
         this._reporters.put(objectId, reportTime);
      }

      boolean reportedBySameClan(Clan clan) {
         if (clan == null) {
            return false;
         } else {
            for (int reporterId : this._reporters.keySet()) {
               if (clan.isMember(reporterId)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private class ReporterCharData {
      private long _lastReport;
      private byte _reportPoints;

      ReporterCharData() {
         Objects.requireNonNull(BotReportTable.this);
         super();
         this._reportPoints = 7;
         this._lastReport = 0L;
      }

      void registerReport(long time) {
         this._reportPoints--;
         this._lastReport = time;
      }

      long getLastReporTime() {
         return this._lastReport;
      }

      byte getPointsLeft() {
         return this._reportPoints;
      }

      void setPoints(int points) {
         this._reportPoints = (byte)points;
      }
   }

   private class ResetPointTask implements Runnable {
      public ResetPointTask() {
         Objects.requireNonNull(BotReportTable.this);
         super();
      }

      @Override
      public void run() {
         BotReportTable.this.resetPointsAndSchedule();
      }
   }

   private static class SingletonHolder {
      static final BotReportTable INSTANCE = new BotReportTable();
   }
}
