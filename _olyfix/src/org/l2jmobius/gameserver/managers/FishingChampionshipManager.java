package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Folk;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class FishingChampionshipManager {
   protected static final Logger LOGGER = Logger.getLogger(FishingChampionshipManager.class.getName());
   private static final String INSERT = "INSERT INTO fishing_championship(player_name,fish_length,rewarded) VALUES (?,?,?)";
   private static final String DELETE = "DELETE FROM fishing_championship";
   private static final String SELECT = "SELECT `player_name`, `fish_length`, `rewarded` FROM fishing_championship";
   private static final FishingChampionshipManager INSTANCE = new FishingChampionshipManager();
   protected long _enddate = 0L;
   protected final List<String> _playersName = new ArrayList<>();
   protected final List<String> _fishLength = new ArrayList<>();
   protected final List<String> _winPlayersName = new ArrayList<>();
   protected final List<String> _winFishLength = new ArrayList<>();
   protected final List<FishingChampionshipManager.Fisher> _tmpPlayers = new ArrayList<>();
   protected final List<FishingChampionshipManager.Fisher> _winPlayers = new ArrayList<>();
   protected double _minFishLength = 0.0;
   protected boolean _needRefresh = true;

   public static FishingChampionshipManager getInstance() {
      return INSTANCE;
   }

   private FishingChampionshipManager() {
      this.restoreData();
      this.refreshWinResult();
      this.recalculateMinLength();
      if (this._enddate <= System.currentTimeMillis()) {
         this._enddate = System.currentTimeMillis();
         new FishingChampionshipManager.finishChamp().run();
      } else {
         ThreadPool.schedule(new FishingChampionshipManager.finishChamp(), this._enddate - System.currentTimeMillis());
      }
   }

   protected void setEndOfChamp() {
      Calendar finishtime = Calendar.getInstance();
      finishtime.setTimeInMillis(this._enddate);
      finishtime.set(12, 0);
      finishtime.set(13, 0);
      finishtime.add(5, 6);
      finishtime.set(7, 3);
      finishtime.set(11, 19);
      this._enddate = finishtime.getTimeInMillis();
   }

   private void restoreData() {
      this._enddate = GlobalVariablesManager.getInstance().getLong("fishChampionshipEnd", 0L);

      try (Connection con = DatabaseFactory.getConnection()) {
         PreparedStatement statement = con.prepareStatement("SELECT `player_name`, `fish_length`, `rewarded` FROM fishing_championship");
         ResultSet rs = statement.executeQuery();

         while (rs.next()) {
            int rewarded = rs.getInt("rewarded");
            if (rewarded == 0) {
               this._tmpPlayers.add(new FishingChampionshipManager.Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), 0));
            } else if (rewarded > 0) {
               this._winPlayers.add(new FishingChampionshipManager.Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), rewarded));
            }
         }

         rs.close();
         statement.close();
      } catch (SQLException var7) {
         LOGGER.log(Level.WARNING, "FishingChampionshipManager: can't restore fishing championship info: " + var7.getMessage(), (Throwable)var7);
      }
   }

   public synchronized void newFish(Player pl, int lureId) {
      if (GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED) {
         double len = Rnd.get(60, 89) + Rnd.get(0, 1000) / 1000.0;
         if (lureId >= 8484 && lureId <= 8486) {
            len += Rnd.get(0, 3000) / 1000.0;
         }

         pl.sendPacket(new SystemMessage(SystemMessageId.YOU_CAUGHT_A_FISH_S1_IN_LENGTH).addString(String.valueOf(len)));
         if (this._tmpPlayers.size() < 5) {
            for (FishingChampionshipManager.Fisher fisher : this._tmpPlayers) {
               if (fisher.getName().equalsIgnoreCase(pl.getName())) {
                  if (fisher.getLength() < len) {
                     fisher.setLength(len);
                     pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
                     this.recalculateMinLength();
                  }

                  return;
               }
            }

            this._tmpPlayers.add(new FishingChampionshipManager.Fisher(pl.getName(), len, 0));
            pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
            this.recalculateMinLength();
         } else if (this._minFishLength < len) {
            for (FishingChampionshipManager.Fisher fisherx : this._tmpPlayers) {
               if (fisherx.getName().equalsIgnoreCase(pl.getName())) {
                  if (fisherx.getLength() < len) {
                     fisherx.setLength(len);
                     pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
                     this.recalculateMinLength();
                  }

                  return;
               }
            }

            FishingChampionshipManager.Fisher minFisher = null;
            double minLen = 99999.0;

            for (FishingChampionshipManager.Fisher fisherxx : this._tmpPlayers) {
               if (fisherxx.getLength() < minLen) {
                  minFisher = fisherxx;
                  minLen = fisherxx.getLength();
               }
            }

            this._tmpPlayers.remove(minFisher);
            this._tmpPlayers.add(new FishingChampionshipManager.Fisher(pl.getName(), len, 0));
            pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
            this.recalculateMinLength();
         }
      }
   }

   private void recalculateMinLength() {
      double minLen = 99999.0;

      for (FishingChampionshipManager.Fisher fisher : this._tmpPlayers) {
         if (fisher.getLength() < minLen) {
            minLen = fisher.getLength();
         }
      }

      this._minFishLength = minLen;
   }

   public long getTimeRemaining() {
      return (this._enddate - System.currentTimeMillis()) / 60000L;
   }

   public String getWinnerName(int par) {
      return this._winPlayersName.size() >= par ? this._winPlayersName.get(par - 1) : "None";
   }

   public String getCurrentName(int par) {
      return this._playersName.size() >= par ? this._playersName.get(par - 1) : "None";
   }

   public String getFishLength(int par) {
      return this._winFishLength.size() >= par ? this._winFishLength.get(par - 1) : "0";
   }

   public String getCurrentFishLength(int par) {
      return this._fishLength.size() >= par ? this._fishLength.get(par - 1) : "0";
   }

   public boolean isWinner(String playerName) {
      for (String name : this._winPlayersName) {
         if (name.equals(playerName)) {
            return true;
         }
      }

      return false;
   }

   public void getReward(Player pl) {
      for (FishingChampionshipManager.Fisher fisher : this._winPlayers) {
         if (fisher.getName().equalsIgnoreCase(pl.getName()) && fisher.getRewardType() != 2) {
            int rewardCnt = 0;

            for (int x = 0; x < this._winPlayersName.size(); x++) {
               if (this._winPlayersName.get(x).equalsIgnoreCase(pl.getName())) {
                  switch (x) {
                     case 0:
                        rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1;
                        break;
                     case 1:
                        rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2;
                        break;
                     case 2:
                        rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3;
                        break;
                     case 3:
                        rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4;
                        break;
                     case 4:
                        rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5;
                  }
               }
            }

            fisher.setRewardType(2);
            if (rewardCnt > 0) {
               pl.addItem(ItemProcessType.REWARD, GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM, rewardCnt, null, true);
               NpcHtmlMessage html = new NpcHtmlMessage();
               html.setFile(pl, "data/html/fisherman/championship/fish_event_reward001.htm");
               pl.sendPacket(html);
            }
         }
      }
   }

   public void showMidResult(Player pl) {
      NpcHtmlMessage html = new NpcHtmlMessage();
      if (this._needRefresh) {
         html.setFile(pl, "data/html/fisherman/championship/fish_event003.htm");
         pl.sendPacket(html);
         this.refreshResult();
         ThreadPool.schedule(new FishingChampionshipManager.needRefresh(), 60000L);
      } else {
         html.setFile(pl, "data/html/fisherman/championship/fish_event002.htm");
         String str = null;

         for (int x = 1; x <= 5; x++) {
            str = str + "<tr><td width=70 align=center>" + x + "</td>";
            str = str + "<td width=110 align=center>" + this.getCurrentName(x) + "</td>";
            str = str + "<td width=80 align=center>" + this.getCurrentFishLength(x) + "</td></tr>";
         }

         html.replace("%TABLE%", str);
         html.replace("%prizeItem%", ItemData.getInstance().getTemplate(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
         html.replace("%prizeFirst%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1));
         html.replace("%prizeTwo%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2));
         html.replace("%prizeThree%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3));
         html.replace("%prizeFour%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4));
         html.replace("%prizeFive%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5));
         pl.sendPacket(html);
      }
   }

   public void showChampScreen(Player pl, Folk npc) {
      NpcHtmlMessage html = new NpcHtmlMessage();
      html.setFile(pl, "data/html/fisherman/championship/fish_event001.htm");
      String str = null;

      for (int x = 1; x <= 5; x++) {
         str = str + "<tr><td width=70 align=center>" + x + "</td>";
         str = str + "<td width=110 align=center>" + this.getWinnerName(x) + "</td>";
         str = str + "<td width=80 align=center>" + this.getFishLength(x) + "</td></tr>";
      }

      html.replace("%TABLE%", str);
      html.replace("%prizeItem%", ItemData.getInstance().getTemplate(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
      html.replace("%prizeFirst%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1));
      html.replace("%prizeTwo%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2));
      html.replace("%prizeThree%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3));
      html.replace("%prizeFour%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4));
      html.replace("%prizeFive%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5));
      html.replace("%refresh%", String.valueOf(this.getTimeRemaining()));
      html.replace("%objectId%", String.valueOf(npc.getObjectId()));
      pl.sendPacket(html);
   }

   public void shutdown() {
      GlobalVariablesManager.getInstance().set("fishChampionshipEnd", this._enddate);

      try (Connection con = DatabaseFactory.getConnection()) {
         PreparedStatement statement = con.prepareStatement("DELETE FROM fishing_championship");
         statement.execute();
         statement.close();

         for (FishingChampionshipManager.Fisher fisher : this._winPlayers) {
            statement = con.prepareStatement("INSERT INTO fishing_championship(player_name,fish_length,rewarded) VALUES (?,?,?)");
            statement.setString(1, fisher.getName());
            statement.setDouble(2, fisher.getLength());
            statement.setInt(3, fisher.getRewardType());
            statement.execute();
            statement.close();
         }

         for (FishingChampionshipManager.Fisher fisher : this._tmpPlayers) {
            statement = con.prepareStatement("INSERT INTO fishing_championship(player_name,fish_length,rewarded) VALUES (?,?,?)");
            statement.setString(1, fisher.getName());
            statement.setDouble(2, fisher.getLength());
            statement.setInt(3, 0);
            statement.execute();
            statement.close();
         }
      } catch (SQLException var7) {
         LOGGER.log(Level.WARNING, "FishingChampionshipManager: can't update infos: " + var7.getMessage(), (Throwable)var7);
      }
   }

   private synchronized void refreshResult() {
      this._needRefresh = false;
      this._playersName.clear();
      this._fishLength.clear();

      for (int x = 0; x <= this._tmpPlayers.size() - 1; x++) {
         for (int y = 0; y <= this._tmpPlayers.size() - 2; y++) {
            FishingChampionshipManager.Fisher fisher1 = this._tmpPlayers.get(y);
            FishingChampionshipManager.Fisher fisher2 = this._tmpPlayers.get(y + 1);
            if (fisher1.getLength() < fisher2.getLength()) {
               this._tmpPlayers.set(y, fisher2);
               this._tmpPlayers.set(y + 1, fisher1);
            }
         }
      }

      for (int x = 0; x <= this._tmpPlayers.size() - 1; x++) {
         this._playersName.add(this._tmpPlayers.get(x).getName());
         this._fishLength.add(String.valueOf(this._tmpPlayers.get(x).getLength()));
      }
   }

   protected void refreshWinResult() {
      this._winPlayersName.clear();
      this._winFishLength.clear();

      for (int x = 0; x <= this._winPlayers.size() - 1; x++) {
         for (int y = 0; y <= this._winPlayers.size() - 2; y++) {
            FishingChampionshipManager.Fisher fisher1 = this._winPlayers.get(y);
            FishingChampionshipManager.Fisher fisher2 = this._winPlayers.get(y + 1);
            if (fisher1.getLength() < fisher2.getLength()) {
               this._winPlayers.set(y, fisher2);
               this._winPlayers.set(y + 1, fisher1);
            }
         }
      }

      for (int x = 0; x <= this._winPlayers.size() - 1; x++) {
         this._winPlayersName.add(this._winPlayers.get(x).getName());
         this._winFishLength.add(String.valueOf(this._winPlayers.get(x).getLength()));
      }
   }

   private class Fisher {
      private double _length;
      private final String _name;
      private int _reward;

      public Fisher(String name, double length, int rewardType) {
         Objects.requireNonNull(FishingChampionshipManager.this);
         super();
         this._name = name;
         this._length = length;
         this._reward = rewardType;
      }

      public void setLength(double value) {
         this._length = value;
      }

      public void setRewardType(int value) {
         this._reward = value;
      }

      public String getName() {
         return this._name;
      }

      public int getRewardType() {
         return this._reward;
      }

      public double getLength() {
         return this._length;
      }
   }

   private class finishChamp implements Runnable {
      protected finishChamp() {
         Objects.requireNonNull(FishingChampionshipManager.this);
         super();
      }

      @Override
      public void run() {
         FishingChampionshipManager.this._winPlayers.clear();

         for (FishingChampionshipManager.Fisher fisher : FishingChampionshipManager.this._tmpPlayers) {
            fisher.setRewardType(1);
            FishingChampionshipManager.this._winPlayers.add(fisher);
         }

         FishingChampionshipManager.this._tmpPlayers.clear();
         FishingChampionshipManager.this.refreshWinResult();
         FishingChampionshipManager.this.setEndOfChamp();
         FishingChampionshipManager.this.shutdown();
         FishingChampionshipManager.LOGGER.info("FishingChampionshipManager : new event period start.");
         ThreadPool.schedule(FishingChampionshipManager.this.new finishChamp(), FishingChampionshipManager.this._enddate - System.currentTimeMillis());
      }
   }

   private class needRefresh implements Runnable {
      protected needRefresh() {
         Objects.requireNonNull(FishingChampionshipManager.this);
         super();
      }

      @Override
      public void run() {
         FishingChampionshipManager.this._needRefresh = true;
      }
   }
}
