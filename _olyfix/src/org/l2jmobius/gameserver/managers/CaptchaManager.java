package org.l2jmobius.gameserver.managers;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.captcha.Captcha;
import org.l2jmobius.gameserver.mechanics.captcha.ImageData;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeCrest;
import org.l2jmobius.gameserver.util.DDSConverter;
import org.l2jmobius.gameserver.util.MathUtil;

public class CaptchaManager {
   private static final Logger LOGGER = Logger.getLogger(CaptchaManager.class.getName());
   private static final Map<Integer, Integer> JAIL_COUNT = new ConcurrentHashMap<>();
   private static final Map<Integer, Integer> RETRIES = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> LAST_JAIL_TIME = new ConcurrentHashMap<>();
   private static final int MAX_JAIL_TIME = 1440;
   protected static Map<Integer, ImageData> IMAGES;
   protected static Map<Integer, Integer> MONSTER_COUNTER = new ConcurrentHashMap<>();
   protected static Map<Integer, Long> LAST_KILL_TIME = new ConcurrentHashMap<>();
   protected static Map<Integer, Future<?>> BEGIN_VALIDATION = new ConcurrentHashMap<>();
   protected static Map<Integer, CaptchaManager.PlayerData> VALIDATION = new ConcurrentHashMap<>();
   protected static int WINDOW_DELAY = 3;
   private static int USED_ID = 0;

   protected CaptchaManager() {
      if (CaptchaConfig.ENABLE_CAPTCHA) {
         IMAGES = Captcha.getInstance().createImageList();
         LOGGER.info(this.getClass().getSimpleName() + ": Enabled.");
      }
   }

   public void updateCounter(Creature player, Creature monster) {
      if (CaptchaConfig.ENABLE_CAPTCHA) {
         if (player instanceof Player && monster instanceof Monster) {
            Player killer = player.asPlayer();
            if (!AutoPlayConfig.ENABLE_AUTO_PLAY || !killer.isAutoPlaying()) {
               if (VALIDATION.get(killer.getObjectId()) == null) {
                  if (CaptchaConfig.KILL_COUNTER_RESET) {
                     long currentTime = System.currentTimeMillis();
                     long previousKillTime = LAST_KILL_TIME.getOrDefault(killer.getObjectId(), currentTime);
                     if (currentTime - previousKillTime > CaptchaConfig.KILL_COUNTER_RESET_TIME) {
                        MONSTER_COUNTER.put(killer.getObjectId(), 0);
                     }

                     LAST_KILL_TIME.put(killer.getObjectId(), currentTime);
                  }

                  int count = 1;
                  if (MONSTER_COUNTER.get(killer.getObjectId()) != null) {
                     count = MONSTER_COUNTER.get(killer.getObjectId()) + 1;
                  }

                  int next = Rnd.get(CaptchaConfig.KILL_COUNTER_RANDOMIZATION);
                  if (CaptchaConfig.KILL_COUNTER + next < count) {
                     this.validationTasks(killer);
                     MONSTER_COUNTER.remove(killer.getObjectId());
                  } else {
                     MONSTER_COUNTER.put(killer.getObjectId(), count);
                  }
               }
            }
         }
      }
   }

   public void preValidationWindow(Player player) {
      RETRIES.put(player.getObjectId(), 0);
      NpcHtmlMessage html = new NpcHtmlMessage(1);
      StringBuilder sb = new StringBuilder();
      StringUtil.append(sb, "<html>");
      StringUtil.append(sb, "<title>Bot Protection</title>");
      StringUtil.append(sb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
      StringUtil.append(sb, "<br><br><font color=\"a2a0a2\">If such window appears it means server suspect,<br1>that you may using cheating software.</font>");
      StringUtil.append(
         sb, "<br><br><font color=\"b09979\">If given answer results are incorrect or no action is made character will be punished instantly.</font>"
      );
      StringUtil.append(
         sb,
         "<br><br><button value=\"CONTINUE\" action=\"bypass report_continue\" width=\"100\" height=\"27\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">"
      );
      StringUtil.append(sb, "</center></body>");
      StringUtil.append(sb, "</html>");
      html.setHtml(sb.toString());
      player.sendPacket(html);
   }

   private static void validationWindow(Player player) {
      CaptchaManager.PlayerData data = VALIDATION.get(player.getObjectId());
      NpcHtmlMessage html = new NpcHtmlMessage(1);
      StringBuilder sb = new StringBuilder();
      StringUtil.append(sb, "<html>");
      StringUtil.append(sb, "<title>Bot Protection</title>");
      StringUtil.append(sb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
      StringUtil.append(
         sb,
         "<br><br><font color=\"a2a0a2\">in order to prove you are a human being<br1>you've to</font> <font color=\"b09979\">enter the code from picture:</font>"
      );
      StringUtil.append(sb, "<br><br><img src=\"Crest.crest_" + ServerConfig.SERVER_ID + "_" + data.captchaId + "\" width=256 height=64></td></tr>");
      StringUtil.append(sb, "<br>");
      StringUtil.append(sb, "<br><br><edit var=\"answer\" width=110>");
      StringUtil.append(
         sb,
         "<br><button value=\"Confirm\" action=\"bypass -h report_ $answer\" width=\"100\" height=\"27\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br/>"
      );
      StringUtil.append(sb, "</center></body>");
      StringUtil.append(sb, "</html>");
      html.setHtml(sb.toString());
      player.sendPacket(html);
      QuestSound soundToPlay = QuestSound.SKILLSOUND_HORROR_1;
      player.sendPacket(soundToPlay.getPacket());
   }

   public void punishmentWindow(Player player) {
      NpcHtmlMessage html = new NpcHtmlMessage(1);
      StringBuilder sb = new StringBuilder();
      StringUtil.append(sb, "<html>");
      StringUtil.append(sb, "<title>Bot Protection</title>");
      StringUtil.append(sb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
      StringUtil.append(sb, "<br><br><font color=\"a2a0a2\">If such window appears, it means character haven't passed through prevention system.");
      StringUtil.append(sb, "<br><br><font color=\"b09979\">In such case character get moved to nearest town.</font>");
      StringUtil.append(sb, "</center></body>");
      StringUtil.append(sb, "</html>");
      html.setHtml(sb.toString());
      player.sendPacket(html);
   }

   public void validationTasks(Player player) {
      CaptchaManager.PlayerData data = new CaptchaManager.PlayerData();
      Captcha.getInstance().generateCaptcha(data, player);
      data.image = IMAGES.get(USED_ID).image;
      data.captchaId = IMAGES.get(USED_ID).captchaID;
      data.captchaText = IMAGES.get(USED_ID).captchaText;
      PledgeCrest packet = new PledgeCrest(data.captchaId, DDSConverter.convertToDDS(data.image).array());
      player.sendPacket(packet);
      USED_ID++;
      if (USED_ID == 998) {
         USED_ID = 0;
         ThreadPool.schedule(() -> IMAGES = Captcha.getInstance().createImageList(), 100L);
      }

      VALIDATION.put(player.getObjectId(), data);
      Future<?> newTask = ThreadPool.schedule(new CaptchaManager.ReportCheckTask(player), CaptchaConfig.VALIDATION_TIME * 1000);
      ThreadPool.schedule(new CaptchaManager.countdown(player, CaptchaConfig.VALIDATION_TIME), 0L);
      BEGIN_VALIDATION.put(player.getObjectId(), newTask);
   }

   protected void banPunishment(Player player) {
      VALIDATION.remove(player.getObjectId());
      BEGIN_VALIDATION.get(player.getObjectId()).cancel(true);
      BEGIN_VALIDATION.remove(player.getObjectId());
      switch (CaptchaConfig.PUNISHMENT) {
         case 0:
            player.stopMove(null);
            player.teleToLocation(TeleportWhereType.TOWN);
            this.punishmentWindow(player);
            QuestSound soundForTeleport = QuestSound.ITEMSOUND_QUEST_GIVEUP;
            player.sendPacket(soundForTeleport.getPacket());
            AdminData.getInstance().broadcastToGMs(new CreatureSay(null, ChatType.ALLIANCE, "Punishment", player.getName() + " has been teleported."));
            PacketLogger.warning(player + "[TELEPORT] Wrong Captcha maybe boting?");
            break;
         case 1:
            if (player.isOnline()) {
               Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
               AdminData.getInstance().broadcastToGMs(new CreatureSay(null, ChatType.ALLIANCE, "Punishment", player.getName() + " has been kicked."));
               PacketLogger.warning(player + "[KICK] Wrong Captcha maybe boting?");
               return;
            }
            break;
         case 2:
            this.applyJailPunishment(player);
            QuestSound soundForJail = QuestSound.ETCSOUND_ELROKI_SONG_FULL;
            player.sendPacket(soundForJail.getPacket());
            AdminData.getInstance().broadcastToGMs(new CreatureSay(null, ChatType.ALLIANCE, "Punishment", player.getName() + " has been jailed."));
            PacketLogger.warning(player + "[JAIL] Wrong Captcha maybe boting?");
            break;
         case 3:
            changeAccessLevel(player);
            AdminData.getInstance().broadcastToGMs(new CreatureSay(null, ChatType.ALLIANCE, "Punishment", player.getName() + " has been banned."));
            PacketLogger.warning(player + "[BAN] Wrong Captcha maybe boting?");
      }

      player.sendMessage("Unfortunately, code doesn't match.");
   }

   private int getPlayerJailCount(Player player) {
      return JAIL_COUNT.getOrDefault(player.getObjectId(), 0);
   }

   private void incrementPlayerJailCount(Player player) {
      JAIL_COUNT.put(player.getObjectId(), this.getPlayerJailCount(player) + 1);
   }

   private void applyJailPunishment(Player player) {
      if (player.isOnline()) {
         long currentTime = System.currentTimeMillis();
         int jailTime;
         if (CaptchaConfig.DOUBLE_JAIL_TIME) {
            long lastJailTime = LAST_JAIL_TIME.getOrDefault(player.getObjectId(), 0L);
            int previousJailCount = this.getPlayerJailCount(player);
            if (currentTime - lastJailTime >= TimeUnit.HOURS.toMillis(24L)) {
               jailTime = CaptchaConfig.JAIL_TIME;
            } else {
               jailTime = Math.min((int)MathUtil.pow(2.0, previousJailCount) * CaptchaConfig.JAIL_TIME, 1440);
            }

            this.incrementPlayerJailCount(player);
            ThreadPool.schedule(() -> player.sendMessage("Warning: Your next jail time will be increased."), 2000L);
         } else {
            jailTime = CaptchaConfig.JAIL_TIME;
         }

         long punishmentTime;
         if (jailTime == 0) {
            punishmentTime = 0L;
         } else {
            punishmentTime = currentTime + TimeUnit.MINUTES.toMillis(jailTime);
         }

         PunishmentTask punishmentTask = new PunishmentTask(
            player.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL, punishmentTime, "Bot Protection", "System"
         );
         PunishmentManager.getInstance().startPunishment(punishmentTask);
         if (jailTime != 0) {
            LAST_JAIL_TIME.put(player.getObjectId(), currentTime);
            if (CaptchaConfig.DOUBLE_JAIL_TIME) {
               LAST_JAIL_TIME.put(player.getObjectId(), currentTime);
            }
         }
      }
   }

   private static void changeAccessLevel(Player player) {
      if (player.isOnline()) {
         PunishmentTask punishmentTask = new PunishmentTask(
            player.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.BAN, 0L, "Bot Protection", "System"
         );
         PunishmentManager.getInstance().startPunishment(punishmentTask);
      }
   }

   public void analyseBypass(String command, Player player) {
      if (VALIDATION.containsKey(player.getObjectId())) {
         String params = command.substring(command.indexOf("_") + 1);
         if (params.startsWith("continue")) {
            validationWindow(player);
            VALIDATION.get(player.getObjectId()).firstWindow = false;
         } else {
            CaptchaManager.PlayerData data = VALIDATION.get(player.getObjectId());
            if (!params.trim().equalsIgnoreCase(data.captchaText)) {
               int retries = RETRIES.getOrDefault(player.getObjectId(), 0) + 1;
               RETRIES.put(player.getObjectId(), retries);
               if (retries >= CaptchaConfig.CAPTCHA_ATTEMPTS) {
                  this.banPunishment(player);
               } else {
                  player.sendMessage("Incorrect code. You have " + (CaptchaConfig.CAPTCHA_ATTEMPTS - retries) + " more attempt(s) left.");
                  validationWindow(player);
                  player.sendPacket(QuestSound.ITEMSOUND_BROKEN_KEY.getPacket());
               }
            } else {
               player.sendMessage("Congratulations, code matches!");
               VALIDATION.remove(player.getObjectId());
               BEGIN_VALIDATION.get(player.getObjectId()).cancel(true);
               BEGIN_VALIDATION.remove(player.getObjectId());
               player.sendPacket(QuestSound.ITEMSOUND_SOW_SUCCESS.getPacket());
            }
         }
      }
   }

   public void captchaSuccessfull(Player player) {
      if (VALIDATION.get(player.getObjectId()) != null) {
         VALIDATION.remove(player.getObjectId());
      }
   }

   public boolean isAlredyInReportMode(Player player) {
      return VALIDATION.get(player.getObjectId()) != null;
   }

   public static CaptchaManager getInstance() {
      return CaptchaManager.SingletonHolder.INSTANCE;
   }

   public class PlayerData {
      public boolean firstWindow;
      public BufferedImage image;
      public String captchaText;
      public int captchaId;

      public PlayerData() {
         Objects.requireNonNull(CaptchaManager.this);
         super();
         this.captchaText = "";
         this.captchaId = 0;
         this.firstWindow = true;
      }
   }

   private class ReportCheckTask implements Runnable {
      private final Player _player;

      public ReportCheckTask(Player player) {
         Objects.requireNonNull(CaptchaManager.this);
         super();
         this._player = player;
      }

      @Override
      public void run() {
         if (CaptchaManager.VALIDATION.get(this._player.getObjectId()) != null) {
            CaptchaManager.this.banPunishment(this._player);
         }
      }
   }

   private static class SingletonHolder {
      protected static final CaptchaManager INSTANCE = new CaptchaManager();
   }

   protected class countdown implements Runnable {
      private final Player _player;
      private final int _time;

      public countdown(Player player, int time) {
         Objects.requireNonNull(CaptchaManager.this);
         super();
         this._time = time;
         this._player = player;
      }

      @Override
      public void run() {
         if (this._player.isOnline()) {
            if (CaptchaManager.VALIDATION.containsKey(this._player.getObjectId())
               && CaptchaManager.VALIDATION.get(this._player.getObjectId()).firstWindow
               && this._time % CaptchaManager.WINDOW_DELAY == 0) {
               CaptchaManager.this.preValidationWindow(this._player);
            }

            switch (this._time) {
               case 1:
               case 2:
               case 3:
               case 4:
               case 5:
               case 10:
               case 30:
                  this._player.sendMessage(this._time + " second(s) to enter the code!");
                  break;
               case 60:
               case 120:
               case 180:
               case 240:
               case 300:
                  this._player.sendMessage(this._time / 60 + " minute(s) to enter the code.");
            }

            if (this._time > 1 && CaptchaManager.VALIDATION.containsKey(this._player.getObjectId())) {
               ThreadPool.schedule(CaptchaManager.this.new countdown(this._player, this._time - 1), 1000L);
            }
         }
      }
   }
}
