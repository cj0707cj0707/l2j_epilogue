package org.l2jmobius.gameserver.mechanics.sevensigns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.spawns.AutoSpawnHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SSQInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class SevenSigns {
   protected static final Logger LOGGER = Logger.getLogger(SevenSigns.class.getName());
   public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
   public static final int CABAL_NULL = 0;
   public static final int CABAL_DUSK = 1;
   public static final int CABAL_DAWN = 2;
   public static final int SEAL_NULL = 0;
   public static final int SEAL_AVARICE = 1;
   public static final int SEAL_GNOSIS = 2;
   public static final int SEAL_STRIFE = 3;
   public static final int PERIOD_COMP_RECRUITING = 0;
   public static final int PERIOD_COMPETITION = 1;
   public static final int PERIOD_COMP_RESULTS = 2;
   public static final int PERIOD_SEAL_VALIDATION = 3;
   public static final int PERIOD_START_HOUR = 18;
   public static final int PERIOD_START_MINS = 0;
   public static final int PERIOD_START_DAY = 2;
   public static final int PERIOD_MINOR_LENGTH = 900000;
   public static final int PERIOD_MAJOR_LENGTH = 603900000;
   public static final int RECORD_SEVEN_SIGNS_ID = 5707;
   public static final int RECORD_SEVEN_SIGNS_COST = 500;
   public static final int ORATOR_NPC_ID = 31094;
   public static final int PREACHER_NPC_ID = 31093;
   public static final int MAMMON_MERCHANT_ID = 31113;
   public static final int MAMMON_BLACKSMITH_ID = 31126;
   public static final int MAMMON_MARKETEER_ID = 31092;
   public static final int LILITH_NPC_ID = 25283;
   public static final int ANAKIM_NPC_ID = 25286;
   public static final int CREST_OF_DAWN_ID = 31170;
   public static final int CREST_OF_DUSK_ID = 31171;
   public static final int SEAL_STONE_BLUE_ID = 6360;
   public static final int SEAL_STONE_GREEN_ID = 6361;
   public static final int SEAL_STONE_RED_ID = 6362;
   protected static final int[] SEAL_STONE_IDS = new int[]{6360, 6361, 6362};
   public static final int SEAL_STONE_BLUE_VALUE = 3;
   public static final int SEAL_STONE_GREEN_VALUE = 5;
   public static final int SEAL_STONE_RED_VALUE = 10;
   public static final int BLUE_CONTRIB_POINTS = 3;
   public static final int GREEN_CONTRIB_POINTS = 5;
   public static final int RED_CONTRIB_POINTS = 10;
   private final Calendar _nextPeriodChange = Calendar.getInstance();
   protected int _activePeriod;
   protected int _currentCycle;
   protected double _dawnStoneScore;
   protected double _duskStoneScore;
   protected int _dawnFestivalScore;
   protected int _duskFestivalScore;
   protected int _compWinner;
   protected int _previousWinner;
   protected Calendar _lastSave = Calendar.getInstance();
   protected Map<Integer, StatSet> _signsPlayerData = new LinkedHashMap<>();
   private final Map<Integer, Integer> _signsSealOwners = new LinkedHashMap<>();
   private final Map<Integer, Integer> _signsDuskSealTotals = new LinkedHashMap<>();
   private final Map<Integer, Integer> _signsDawnSealTotals = new LinkedHashMap<>();
   private static final String LOAD_DATA = "SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score FROM seven_signs";
   private static final String LOAD_STATUS = "SELECT * FROM seven_signs_status WHERE id=0";
   private static final String INSERT_PLAYER = "INSERT INTO seven_signs (charId, cabal, seal) VALUES (?,?,?)";
   private static final String UPDATE_PLAYER = "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, ancient_adena_amount=?, contribution_score=? WHERE charId=?";
   private static final String UPDATE_STATUS = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, festival_cycle=?, accumulated_bonus0=?, accumulated_bonus1=?, accumulated_bonus2=?,accumulated_bonus3=?, accumulated_bonus4=?, date=? WHERE id=0";

   protected SevenSigns() {
      try {
         this.restoreSevenSignsData();
      } catch (Exception var3) {
         LOGGER.log(Level.SEVERE, "SevenSigns: Failed to load configuration: " + var3.getMessage(), (Throwable)var3);
      }

      LOGGER.info("SevenSigns: Currently in the " + this.getCurrentPeriodName() + " period!");
      this.initializeSeals();
      if (this.isSealValidationPeriod()) {
         if (this.getCabalHighestScore() == 0) {
            LOGGER.info("SevenSigns: The competition ended with a tie last week.");
         } else {
            LOGGER.info("SevenSigns: The " + getCabalName(this.getCabalHighestScore()) + " were victorious last week.");
         }
      } else if (this.getCabalHighestScore() == 0) {
         LOGGER.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week.");
      } else {
         LOGGER.info("SevenSigns: The " + getCabalName(this.getCabalHighestScore()) + " are in the lead this week.");
      }

      long milliToChange = 0L;
      if (this.isNextPeriodChangeInPast()) {
         LOGGER.info("SevenSigns: Next period change was in the past (server was offline), changing periods now!");
      } else {
         this.setCalendarForNextPeriodChange();
         milliToChange = this.getMilliToPeriodChange();
      }

      ThreadPool.schedule(new SevenSigns.SevenSignsPeriodChange(), milliToChange);
      LOGGER.info("SevenSigns: Next period begins in " + TimeUtil.formatDuration(milliToChange));
   }

   private boolean isNextPeriodChangeInPast() {
      Calendar lastPeriodChange = Calendar.getInstance();
      switch (this._activePeriod) {
         case 0:
         case 2:
            lastPeriodChange.setTimeInMillis(this._lastSave.getTimeInMillis() + 900000L);
            break;
         case 1:
         case 3:
            lastPeriodChange.set(7, 2);
            lastPeriodChange.set(11, 18);
            lastPeriodChange.set(12, 0);
            lastPeriodChange.set(13, 0);
            if (Calendar.getInstance().before(lastPeriodChange)) {
               lastPeriodChange.add(10, -168);
            }
      }

      return this._lastSave.getTimeInMillis() > 7L && this._lastSave.before(lastPeriodChange);
   }

   public void spawnSevenSignsNPC() {
      AutoSpawnHandler.AutoSpawnInstance merchantSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(31113, false);
      AutoSpawnHandler.AutoSpawnInstance blacksmithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(31126, false);
      AutoSpawnHandler.AutoSpawnInstance lilithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(25283, false);
      AutoSpawnHandler.AutoSpawnInstance anakimSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(25286, false);
      List<AutoSpawnHandler.AutoSpawnInstance> crestOfDawnSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(31170);
      List<AutoSpawnHandler.AutoSpawnInstance> crestOfDuskSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(31171);
      List<AutoSpawnHandler.AutoSpawnInstance> oratorSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(31094);
      List<AutoSpawnHandler.AutoSpawnInstance> preacherSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(31093);
      List<AutoSpawnHandler.AutoSpawnInstance> marketeerSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(31092);
      if (!this.isSealValidationPeriod() && !this.isCompResultsPeriod()) {
         AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
         AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);
         AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
         AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);

         for (AutoSpawnHandler.AutoSpawnInstance dawnCrest : crestOfDawnSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
         }

         for (AutoSpawnHandler.AutoSpawnInstance duskCrest : crestOfDuskSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
         }

         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : oratorSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
         }

         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : preacherSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
         }

         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : marketeerSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
         }
      } else {
         for (AutoSpawnHandler.AutoSpawnInstance spawnInst : marketeerSpawns) {
            AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
         }

         if (this.getSealOwner(2) == this.getCabalHighestScore() && this.getSealOwner(2) != 0) {
            if (!NpcConfig.ANNOUNCE_MAMMON_SPAWN) {
               blacksmithSpawn.setBroadcast(false);
            }

            if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(blacksmithSpawn.getObjectId(), true).isSpawnActive()) {
               AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, true);
            }

            for (AutoSpawnHandler.AutoSpawnInstance spawnInst : oratorSpawns) {
               if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive()) {
                  AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
               }
            }

            for (AutoSpawnHandler.AutoSpawnInstance spawnInstx : preacherSpawns) {
               if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInstx.getObjectId(), true).isSpawnActive()) {
                  AutoSpawnHandler.getInstance().setSpawnActive(spawnInstx, true);
               }
            }
         } else {
            AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);

            for (AutoSpawnHandler.AutoSpawnInstance spawnInstxx : oratorSpawns) {
               AutoSpawnHandler.getInstance().setSpawnActive(spawnInstxx, false);
            }

            for (AutoSpawnHandler.AutoSpawnInstance spawnInstxx : preacherSpawns) {
               AutoSpawnHandler.getInstance().setSpawnActive(spawnInstxx, false);
            }
         }

         if (this.getSealOwner(1) == this.getCabalHighestScore() && this.getSealOwner(1) != 0) {
            if (!NpcConfig.ANNOUNCE_MAMMON_SPAWN) {
               merchantSpawn.setBroadcast(false);
            }

            if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(merchantSpawn.getObjectId(), true).isSpawnActive()) {
               AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, true);
            }

            switch (this.getCabalHighestScore()) {
               case 1:
                  if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(anakimSpawn.getObjectId(), true).isSpawnActive()) {
                     AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, true);
                  }

                  AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);

                  for (AutoSpawnHandler.AutoSpawnInstance duskCrest : crestOfDuskSpawns) {
                     if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(duskCrest.getObjectId(), true).isSpawnActive()) {
                        AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, true);
                     }
                  }

                  for (AutoSpawnHandler.AutoSpawnInstance dawnCrest : crestOfDawnSpawns) {
                     AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
                  }
                  break;
               case 2:
                  if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(lilithSpawn.getObjectId(), true).isSpawnActive()) {
                     AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, true);
                  }

                  AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);

                  for (AutoSpawnHandler.AutoSpawnInstance dawnCrest : crestOfDawnSpawns) {
                     if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(dawnCrest.getObjectId(), true).isSpawnActive()) {
                        AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, true);
                     }
                  }

                  for (AutoSpawnHandler.AutoSpawnInstance duskCrestx : crestOfDuskSpawns) {
                     AutoSpawnHandler.getInstance().setSpawnActive(duskCrestx, false);
                  }
            }
         } else {
            AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
            AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
            AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);

            for (AutoSpawnHandler.AutoSpawnInstance dawnCrestx : crestOfDawnSpawns) {
               AutoSpawnHandler.getInstance().setSpawnActive(dawnCrestx, false);
            }

            for (AutoSpawnHandler.AutoSpawnInstance duskCrestx : crestOfDuskSpawns) {
               AutoSpawnHandler.getInstance().setSpawnActive(duskCrestx, false);
            }
         }
      }
   }

   public static SevenSigns getInstance() {
      return SevenSigns.SingletonHolder.INSTANCE;
   }

   public static long calcContributionScore(long blueCount, long greenCount, long redCount) {
      long contrib = blueCount * 3L;
      contrib += greenCount * 5L;
      return contrib + redCount * 10L;
   }

   public static long calcAncientAdenaReward(long blueCount, long greenCount, long redCount) {
      long reward = blueCount * 3L;
      reward += greenCount * 5L;
      return reward + redCount * 10L;
   }

   public static String getCabalShortName(int cabal) {
      switch (cabal) {
         case 1:
            return "dusk";
         case 2:
            return "dawn";
         default:
            return "No Cabal";
      }
   }

   public static String getCabalName(int cabal) {
      switch (cabal) {
         case 1:
            return "Revolutionaries of Dusk";
         case 2:
            return "Lords of Dawn";
         default:
            return "No Cabal";
      }
   }

   public static String getSealName(int seal, boolean shortName) {
      String sealName = !shortName ? "Seal of " : "";
      switch (seal) {
         case 1:
            sealName = sealName + "Avarice";
            break;
         case 2:
            sealName = sealName + "Gnosis";
            break;
         case 3:
            sealName = sealName + "Strife";
      }

      return sealName;
   }

   public int getCurrentCycle() {
      return this._currentCycle;
   }

   public int getCurrentPeriod() {
      return this._activePeriod;
   }

   private int getDaysToPeriodChange() {
      int numDays = this._nextPeriodChange.get(7) - 2;
      return numDays < 0 ? 0 - numDays : 7 - numDays;
   }

   public long getMilliToPeriodChange() {
      return this._nextPeriodChange.getTimeInMillis() - System.currentTimeMillis();
   }

   protected void setCalendarForNextPeriodChange() {
      switch (this._activePeriod) {
         case 0:
         case 2:
            this._nextPeriodChange.add(14, 900000);
            break;
         case 1:
         case 3:
            int daysToChange = this.getDaysToPeriodChange();
            if (daysToChange == 7) {
               if (this._nextPeriodChange.get(11) < 18) {
                  daysToChange = 0;
               } else if (this._nextPeriodChange.get(11) == 18 && this._nextPeriodChange.get(12) < 0) {
                  daysToChange = 0;
               }
            }

            if (daysToChange > 0) {
               this._nextPeriodChange.add(5, daysToChange);
            }

            this._nextPeriodChange.set(11, 18);
            this._nextPeriodChange.set(12, 0);
      }

      LOGGER.info("SevenSigns: Next period change set to " + this._nextPeriodChange.getTime());
   }

   public String getCurrentPeriodName() {
      String periodName = null;
      switch (this._activePeriod) {
         case 0:
            periodName = "Quest Event Initialization";
            break;
         case 1:
            periodName = "Competition (Quest Event)";
            break;
         case 2:
            periodName = "Quest Event Results";
            break;
         case 3:
            periodName = "Seal Validation";
      }

      return periodName;
   }

   public boolean isCompetitionPeriod() {
      return this._activePeriod == 1;
   }

   public boolean isSealValidationPeriod() {
      return this._activePeriod == 3;
   }

   public boolean isCompResultsPeriod() {
      return this._activePeriod == 2;
   }

   public boolean isDateInSealValidPeriod(Calendar date) {
      long nextPeriodChange = this.getMilliToPeriodChange();
      long nextQuestStart = 0L;
      long nextValidStart = 0L;
      long tillDate = date.getTimeInMillis() - System.currentTimeMillis();

      while (1209600000L < tillDate) {
         tillDate -= 1209600000L;
      }

      while (tillDate < 0L) {
         tillDate += 1209600000L;
      }

      switch (this._activePeriod) {
         case 0:
            nextValidStart = nextPeriodChange + 603900000L;
            nextQuestStart = nextValidStart + 603900000L + 900000L;
            break;
         case 1:
            nextValidStart = nextPeriodChange;
            nextQuestStart = nextPeriodChange + 603900000L + 900000L;
            break;
         case 2:
            nextQuestStart = nextPeriodChange + 603900000L;
            nextValidStart = nextQuestStart + 603900000L + 900000L;
            break;
         case 3:
            nextQuestStart = nextPeriodChange;
            nextValidStart = nextPeriodChange + 603900000L + 900000L;
      }

      return (nextQuestStart >= tillDate || tillDate >= nextValidStart)
         && (nextValidStart >= nextQuestStart || tillDate >= nextValidStart && nextQuestStart >= tillDate);
   }

   public int getCurrentScore(int cabal) {
      double totalStoneScore = this._dawnStoneScore + this._duskStoneScore;
      switch (cabal) {
         case 0:
            return 0;
         case 1:
            return Math.round((float)(this._duskStoneScore / ((float)totalStoneScore == 0.0F ? 1.0 : totalStoneScore)) * 500.0F) + this._duskFestivalScore;
         case 2:
            return Math.round((float)(this._dawnStoneScore / ((float)totalStoneScore == 0.0F ? 1.0 : totalStoneScore)) * 500.0F) + this._dawnFestivalScore;
         default:
            return 0;
      }
   }

   public double getCurrentStoneScore(int cabal) {
      switch (cabal) {
         case 0:
            return 0.0;
         case 1:
            return this._duskStoneScore;
         case 2:
            return this._dawnStoneScore;
         default:
            return 0.0;
      }
   }

   public int getCurrentFestivalScore(int cabal) {
      switch (cabal) {
         case 0:
            return 0;
         case 1:
            return this._duskFestivalScore;
         case 2:
            return this._dawnFestivalScore;
         default:
            return 0;
      }
   }

   public int getCabalHighestScore() {
      if (this.getCurrentScore(1) == this.getCurrentScore(2)) {
         return 0;
      } else {
         return this.getCurrentScore(1) > this.getCurrentScore(2) ? 1 : 2;
      }
   }

   public int getSealOwner(int seal) {
      return this._signsSealOwners.get(seal);
   }

   public int getSealProportion(int seal, int cabal) {
      if (cabal == 0) {
         return 0;
      } else {
         return cabal == 1 ? this._signsDuskSealTotals.get(seal) : this._signsDawnSealTotals.get(seal);
      }
   }

   public int getTotalMembers(int cabal) {
      int cabalMembers = 0;
      String cabalName = getCabalShortName(cabal);

      for (StatSet sevenDat : this._signsPlayerData.values()) {
         if (sevenDat.getString("cabal").equals(cabalName)) {
            cabalMembers++;
         }
      }

      return cabalMembers;
   }

   public int getPlayerStoneContrib(int objectId) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      if (currPlayer == null) {
         return 0;
      } else {
         int stoneCount = 0;
         stoneCount += currPlayer.getInt("red_stones");
         stoneCount += currPlayer.getInt("green_stones");
         return stoneCount + currPlayer.getInt("blue_stones");
      }
   }

   public int getPlayerContribScore(int objectId) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      return currPlayer == null ? 0 : currPlayer.getInt("contribution_score");
   }

   public int getPlayerAdenaCollect(int objectId) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      return currPlayer == null ? 0 : currPlayer.getInt("ancient_adena_amount");
   }

   public int getPlayerSeal(int objectId) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      return currPlayer == null ? 0 : currPlayer.getInt("seal");
   }

   public int getPlayerCabal(int objectId) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      if (currPlayer == null) {
         return 0;
      } else {
         String playerCabal = currPlayer.getString("cabal");
         if (playerCabal.equalsIgnoreCase("dawn")) {
            return 2;
         } else {
            return playerCabal.equalsIgnoreCase("dusk") ? 1 : 0;
         }
      }
   }

   protected void restoreSevenSignsData() {
      try (Connection con = DatabaseFactory.getConnection()) {
         try (
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery(
               "SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score FROM seven_signs"
            );
         ) {
            StatSet sevenDat = null;

            while (rs.next()) {
               int charObjId = rs.getInt("charId");
               sevenDat = new StatSet();
               sevenDat.set("charId", charObjId);
               sevenDat.set("cabal", rs.getString("cabal"));
               sevenDat.set("seal", rs.getInt("seal"));
               sevenDat.set("red_stones", rs.getInt("red_stones"));
               sevenDat.set("green_stones", rs.getInt("green_stones"));
               sevenDat.set("blue_stones", rs.getInt("blue_stones"));
               sevenDat.set("ancient_adena_amount", rs.getDouble("ancient_adena_amount"));
               sevenDat.set("contribution_score", rs.getDouble("contribution_score"));
               this._signsPlayerData.put(charObjId, sevenDat);
            }
         }

         try (
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM seven_signs_status WHERE id=0");
         ) {
            while (rs.next()) {
               this._currentCycle = rs.getInt("current_cycle");
               this._activePeriod = rs.getInt("active_period");
               this._previousWinner = rs.getInt("previous_winner");
               this._dawnStoneScore = rs.getDouble("dawn_stone_score");
               this._dawnFestivalScore = rs.getInt("dawn_festival_score");
               this._duskStoneScore = rs.getDouble("dusk_stone_score");
               this._duskFestivalScore = rs.getInt("dusk_festival_score");
               this._signsSealOwners.put(1, rs.getInt("avarice_owner"));
               this._signsSealOwners.put(2, rs.getInt("gnosis_owner"));
               this._signsSealOwners.put(3, rs.getInt("strife_owner"));
               this._signsDawnSealTotals.put(1, rs.getInt("avarice_dawn_score"));
               this._signsDawnSealTotals.put(2, rs.getInt("gnosis_dawn_score"));
               this._signsDawnSealTotals.put(3, rs.getInt("strife_dawn_score"));
               this._signsDuskSealTotals.put(1, rs.getInt("avarice_dusk_score"));
               this._signsDuskSealTotals.put(2, rs.getInt("gnosis_dusk_score"));
               this._signsDuskSealTotals.put(3, rs.getInt("strife_dusk_score"));
               this._lastSave.setTimeInMillis(rs.getLong("date"));
            }
         }
      } catch (SQLException var16) {
         LOGGER.log(Level.SEVERE, "SevenSigns: Unable to load Seven Signs data from database: " + var16.getMessage(), (Throwable)var16);
      }
   }

   public void saveSevenSignsData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, ancient_adena_amount=?, contribution_score=? WHERE charId=?"
         );
      ) {
         for (StatSet sevenDat : this._signsPlayerData.values()) {
            ps.setString(1, sevenDat.getString("cabal"));
            ps.setInt(2, sevenDat.getInt("seal"));
            ps.setInt(3, sevenDat.getInt("red_stones"));
            ps.setInt(4, sevenDat.getInt("green_stones"));
            ps.setInt(5, sevenDat.getInt("blue_stones"));
            ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
            ps.setDouble(7, sevenDat.getDouble("contribution_score"));
            ps.setInt(8, sevenDat.getInt("charId"));
            ps.addBatch();
         }

         ps.executeBatch();
      } catch (SQLException var9) {
         LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void saveSevenSignsData(int objectId) {
      StatSet sevenDat = this._signsPlayerData.get(objectId);
      if (sevenDat != null) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, ancient_adena_amount=?, contribution_score=? WHERE charId=?"
            );
         ) {
            ps.setString(1, sevenDat.getString("cabal"));
            ps.setInt(2, sevenDat.getInt("seal"));
            ps.setInt(3, sevenDat.getInt("red_stones"));
            ps.setInt(4, sevenDat.getInt("green_stones"));
            ps.setInt(5, sevenDat.getInt("blue_stones"));
            ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
            ps.setDouble(7, sevenDat.getDouble("contribution_score"));
            ps.setInt(8, sevenDat.getInt("charId"));
            ps.execute();
         } catch (SQLException var11) {
            LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + var11.getMessage(), (Throwable)var11);
         }
      }
   }

   public void saveSevenSignsStatus() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, festival_cycle=?, accumulated_bonus0=?, accumulated_bonus1=?, accumulated_bonus2=?,accumulated_bonus3=?, accumulated_bonus4=?, date=? WHERE id=0"
         );
      ) {
         ps.setInt(1, this._currentCycle);
         ps.setInt(2, this._activePeriod);
         ps.setInt(3, this._previousWinner);
         ps.setDouble(4, this._dawnStoneScore);
         ps.setInt(5, this._dawnFestivalScore);
         ps.setDouble(6, this._duskStoneScore);
         ps.setInt(7, this._duskFestivalScore);
         ps.setInt(8, this._signsSealOwners.get(1));
         ps.setInt(9, this._signsSealOwners.get(2));
         ps.setInt(10, this._signsSealOwners.get(3));
         ps.setInt(11, this._signsDawnSealTotals.get(1));
         ps.setInt(12, this._signsDawnSealTotals.get(2));
         ps.setInt(13, this._signsDawnSealTotals.get(3));
         ps.setInt(14, this._signsDuskSealTotals.get(1));
         ps.setInt(15, this._signsDuskSealTotals.get(2));
         ps.setInt(16, this._signsDuskSealTotals.get(3));
         ps.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());

         for (int i = 0; i < 5; i++) {
            ps.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
         }

         this._lastSave = Calendar.getInstance();
         ps.setLong(23, this._lastSave.getTimeInMillis());
         ps.execute();
      } catch (SQLException var9) {
         LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + var9.getMessage(), (Throwable)var9);
      }
   }

   protected void resetPlayerData() {
      for (StatSet sevenDat : this._signsPlayerData.values()) {
         sevenDat.set("cabal", "");
         sevenDat.set("seal", 0);
         sevenDat.set("contribution_score", 0);
      }
   }

   public int setPlayerInfo(int objectId, int chosenCabal, int chosenSeal) {
      StatSet currPlayerData = this._signsPlayerData.get(objectId);
      if (currPlayerData != null) {
         currPlayerData.set("cabal", getCabalShortName(chosenCabal));
         currPlayerData.set("seal", chosenSeal);
         this._signsPlayerData.put(objectId, currPlayerData);
      } else {
         currPlayerData = new StatSet();
         currPlayerData.set("charId", objectId);
         currPlayerData.set("cabal", getCabalShortName(chosenCabal));
         currPlayerData.set("seal", chosenSeal);
         currPlayerData.set("red_stones", 0);
         currPlayerData.set("green_stones", 0);
         currPlayerData.set("blue_stones", 0);
         currPlayerData.set("ancient_adena_amount", 0);
         currPlayerData.set("contribution_score", 0);
         this._signsPlayerData.put(objectId, currPlayerData);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO seven_signs (charId, cabal, seal) VALUES (?,?,?)");
         ) {
            ps.setInt(1, objectId);
            ps.setString(2, getCabalShortName(chosenCabal));
            ps.setInt(3, chosenSeal);
            ps.execute();
         } catch (SQLException var13) {
            LOGGER.log(Level.SEVERE, "SevenSigns: Failed to save data: " + var13.getMessage(), (Throwable)var13);
         }
      }

      if ("dawn".equals(currPlayerData.getString("cabal"))) {
         this._signsDawnSealTotals.put(chosenSeal, this._signsDawnSealTotals.get(chosenSeal) + 1);
      } else {
         this._signsDuskSealTotals.put(chosenSeal, this._signsDuskSealTotals.get(chosenSeal) + 1);
      }

      if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE) {
         this.saveSevenSignsStatus();
      }

      return chosenCabal;
   }

   public int getAncientAdenaReward(int objectId, boolean removeReward) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      int rewardAmount = currPlayer.getInt("ancient_adena_amount");
      currPlayer.set("red_stones", 0);
      currPlayer.set("green_stones", 0);
      currPlayer.set("blue_stones", 0);
      currPlayer.set("ancient_adena_amount", 0);
      if (removeReward) {
         this._signsPlayerData.put(objectId, currPlayer);
         if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE) {
            this.saveSevenSignsData(objectId);
            this.saveSevenSignsStatus();
         }
      }

      return rewardAmount;
   }

   public long addPlayerStoneContrib(int objectId, long blueCount, long greenCount, long redCount) {
      StatSet currPlayer = this._signsPlayerData.get(objectId);
      long contribScore = calcContributionScore(blueCount, greenCount, redCount);
      long totalContribScore = currPlayer.getLong("contribution_score") + contribScore;
      if (totalContribScore > FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB) {
         return -1L;
      } else {
         long totalAncientAdena = currPlayer.getLong("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
         currPlayer.set("red_stones", currPlayer.getInt("red_stones") + redCount);
         currPlayer.set("green_stones", currPlayer.getInt("green_stones") + greenCount);
         currPlayer.set("blue_stones", currPlayer.getInt("blue_stones") + blueCount);
         currPlayer.set("ancient_adena_amount", totalAncientAdena);
         currPlayer.set("contribution_score", totalContribScore);
         this._signsPlayerData.put(objectId, currPlayer);
         switch (this.getPlayerCabal(objectId)) {
            case 1:
               this._duskStoneScore += contribScore;
               break;
            case 2:
               this._dawnStoneScore += contribScore;
         }

         if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE) {
            this.saveSevenSignsData(objectId);
            this.saveSevenSignsStatus();
         }

         return contribScore;
      }
   }

   public void addFestivalScore(int cabal, int amount) {
      if (cabal == 1) {
         this._duskFestivalScore += amount;
         if (this._dawnFestivalScore >= amount) {
            this._dawnFestivalScore -= amount;
         }
      } else {
         this._dawnFestivalScore += amount;
         if (this._duskFestivalScore >= amount) {
            this._duskFestivalScore -= amount;
         }
      }
   }

   public void sendCurrentPeriodMsg(Player player) {
      switch (this._activePeriod) {
         case 0:
            player.sendPacket(SystemMessageId.SEVEN_SIGNS_PREPARATIONS_HAVE_BEGUN_FOR_THE_NEXT_QUEST_EVENT);
            break;
         case 1:
            player.sendPacket(
               SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_SPEAK_WITH_A_PRIEST_OF_DAWN_OR_DUSK_PRIESTESS_IF_YOU_WISH_TO_PARTICIPATE_IN_THE_EVENT
            );
            break;
         case 2:
            player.sendPacket(SystemMessageId.SEVEN_SIGNS_QUEST_EVENT_HAS_ENDED_RESULTS_ARE_BEING_TALLIED);
            break;
         case 3:
            player.sendPacket(SystemMessageId.SEVEN_SIGNS_THIS_IS_THE_SEAL_VALIDATION_PERIOD_A_NEW_QUEST_EVENT_PERIOD_BEGINS_NEXT_MONDAY);
      }
   }

   public void sendMessageToAll(SystemMessageId sysMsgId) {
      World.broadcastToAllOnlinePlayers(new SystemMessage(sysMsgId));
   }

   protected void initializeSeals() {
      for (Entry<Integer, Integer> e : this._signsSealOwners.entrySet()) {
         if (e.getValue() != 0) {
            if (this.isSealValidationPeriod()) {
               LOGGER.info("SevenSigns: The " + getCabalName(e.getValue()) + " have won the " + getSealName(e.getKey(), false) + ".");
            } else {
               LOGGER.info("SevenSigns: The " + getSealName(e.getKey(), false) + " is currently owned by " + getCabalName(e.getValue()) + ".");
            }
         } else {
            LOGGER.info("SevenSigns: The " + getSealName(e.getKey(), false) + " remains unclaimed.");
         }
      }
   }

   protected void resetSeals() {
      this._signsDawnSealTotals.put(1, 0);
      this._signsDawnSealTotals.put(2, 0);
      this._signsDawnSealTotals.put(3, 0);
      this._signsDuskSealTotals.put(1, 0);
      this._signsDuskSealTotals.put(2, 0);
      this._signsDuskSealTotals.put(3, 0);
   }

   protected void calcNewSealOwners() {
      for (Integer currSeal : this._signsDawnSealTotals.keySet()) {
         int newSealOwner;
         int prevSealOwner = this._signsSealOwners.get(currSeal);
         newSealOwner = 0;
         int dawnProportion = this.getSealProportion(currSeal, 2);
         int totalDawnMembers = this.getTotalMembers(2) == 0 ? 1 : this.getTotalMembers(2);
         int dawnPercent = Math.round((float)dawnProportion / totalDawnMembers * 100.0F);
         int duskProportion = this.getSealProportion(currSeal, 1);
         int totalDuskMembers = this.getTotalMembers(1) == 0 ? 1 : this.getTotalMembers(1);
         int duskPercent = Math.round((float)duskProportion / totalDuskMembers * 100.0F);
         label93:
         switch (prevSealOwner) {
            case 0:
               switch (this.getCabalHighestScore()) {
                  case 0:
                     newSealOwner = 0;
                     break label93;
                  case 1:
                     if (duskPercent >= 35) {
                        newSealOwner = 1;
                     } else {
                        newSealOwner = 0;
                     }
                     break label93;
                  case 2:
                     if (dawnPercent >= 35) {
                        newSealOwner = 2;
                     } else {
                        newSealOwner = 0;
                     }
                  default:
                     break label93;
               }
            case 1:
               switch (this.getCabalHighestScore()) {
                  case 0:
                     if (duskPercent >= 10) {
                        newSealOwner = 1;
                     } else {
                        newSealOwner = 0;
                     }
                     break label93;
                  case 1:
                     if (duskPercent >= 10) {
                        newSealOwner = 1;
                     } else {
                        newSealOwner = 0;
                     }
                     break label93;
                  case 2:
                     if (dawnPercent >= 35) {
                        newSealOwner = 2;
                     } else if (duskPercent >= 10) {
                        newSealOwner = 1;
                     } else {
                        newSealOwner = 0;
                     }
                  default:
                     break label93;
               }
            case 2:
               switch (this.getCabalHighestScore()) {
                  case 0:
                     if (dawnPercent >= 10) {
                        newSealOwner = 2;
                     } else {
                        newSealOwner = 0;
                     }
                     break;
                  case 1:
                     if (duskPercent >= 35) {
                        newSealOwner = 1;
                     } else if (dawnPercent >= 10) {
                        newSealOwner = 2;
                     } else {
                        newSealOwner = 0;
                     }
                     break;
                  case 2:
                     if (dawnPercent >= 10) {
                        newSealOwner = 2;
                     } else {
                        newSealOwner = 0;
                     }
               }
         }

         this._signsSealOwners.put(currSeal, newSealOwner);
         switch (currSeal) {
            case 1:
               if (newSealOwner == 2) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
               } else if (newSealOwner == 1) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
               }
               break;
            case 2:
               if (newSealOwner == 2) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
               } else if (newSealOwner == 1) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
               }
               break;
            case 3:
               if (newSealOwner == 2) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
               } else if (newSealOwner == 1) {
                  this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
               }

               CastleManager.getInstance().validateTaxes(newSealOwner);
         }
      }
   }

   protected void teleLosingCabalFromDungeons(String compWinner) {
      for (Player player : World.getPlayers()) {
         StatSet currPlayer = this._signsPlayerData.get(player.getObjectId());
         if (!this.isSealValidationPeriod() && !this.isCompResultsPeriod()) {
            if (!player.isGM() && player.isIn7sDungeon() && (currPlayer == null || !currPlayer.getString("cabal").isEmpty())) {
               player.teleToLocation(TeleportWhereType.TOWN);
               player.setIn7sDungeon(false);
               player.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
            }
         } else if (!player.isGM() && player.isIn7sDungeon() && (currPlayer == null || !currPlayer.getString("cabal").equals(compWinner))) {
            player.teleToLocation(TeleportWhereType.TOWN);
            player.setIn7sDungeon(false);
            player.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
         }
      }
   }

   public boolean checkIsDawnPostingTicket(int itemId) {
      if (itemId > 6114 && itemId < 6175) {
         return true;
      } else if (itemId > 6801 && itemId < 6812) {
         return true;
      } else if (itemId > 7997 && itemId < 8008) {
         return true;
      } else if (itemId > 7940 && itemId < 7951) {
         return true;
      } else if (itemId > 6294 && itemId < 6307) {
         return true;
      } else if (itemId > 6831 && itemId < 6834) {
         return true;
      } else {
         return itemId > 8027 && itemId < 8030 ? true : itemId > 7970 && itemId < 7973;
      }
   }

   public boolean checkIsRookiePostingTicket(int itemId) {
      if (itemId > 6174 && itemId < 6295) {
         return true;
      } else if (itemId > 6811 && itemId < 6832) {
         return true;
      } else {
         return itemId > 7950 && itemId < 7971 ? true : itemId > 8007 && itemId < 8028;
      }
   }

   public void giveCPMult(int strifeOwner) {
      for (Player player : World.getPlayers()) {
         int cabal = this.getPlayerCabal(player.getObjectId());
         if (cabal != 0) {
            if (cabal == strifeOwner) {
               player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
            } else {
               player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
            }
         }
      }
   }

   public void removeCPMult() {
      for (Player player : World.getPlayers()) {
         player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
         player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
      }
   }

   public boolean checkSummonConditions(Player player) {
      if (player == null) {
         return true;
      } else if (this.isSealValidationPeriod() && this.getSealOwner(3) == 2 && this.getPlayerCabal(player.getObjectId()) == 1) {
         player.sendPacket(SystemMessageId.DUE_TO_THE_AFFECTS_OF_THE_SEAL_OF_STRIFE_IT_IS_NOT_POSSIBLE_TO_SUMMON_AT_THIS_TIME);
         return true;
      } else {
         return false;
      }
   }

   protected class SevenSignsPeriodChange implements Runnable {
      protected SevenSignsPeriodChange() {
         Objects.requireNonNull(SevenSigns.this);
         super();
      }

      @Override
      public void run() {
         int periodEnded = SevenSigns.this._activePeriod++;
         switch (periodEnded) {
            case 0:
               SevenSignsFestival.getInstance().startFestivalManager();
               SevenSigns.this.sendMessageToAll(
                  SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_VISIT_A_PRIEST_OF_DAWN_OR_PRIESTESS_OF_DUSK_TO_PARTICIPATE_IN_THE_EVENT
               );
               break;
            case 1:
               SevenSigns.this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_ENDED_THE_NEXT_QUEST_EVENT_WILL_START_IN_ONE_WEEK);
               int compWinner = SevenSigns.this.getCabalHighestScore();
               SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
               SevenSignsFestival.getInstance().rewardHighestRanked();
               SevenSigns.this.calcNewSealOwners();
               switch (compWinner) {
                  case 1:
                     SevenSigns.this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_WON);
                     break;
                  case 2:
                     SevenSigns.this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_WON);
               }

               SevenSigns.this._previousWinner = compWinner;

               for (Castle castle : CastleManager.getInstance().getCastles()) {
                  castle.setTicketBuyCount(0);
               }
               break;
            case 2:
               SevenSigns.this.initializeSeals();
               SevenSigns.this.giveCPMult(SevenSigns.this.getSealOwner(3));
               SevenSigns.this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_BEGUN);
               SevenSigns.LOGGER
                  .info(
                     "SevenSigns: The "
                        + SevenSigns.getCabalName(SevenSigns.this._previousWinner)
                        + " have won the competition with "
                        + SevenSigns.this.getCurrentScore(SevenSigns.this._previousWinner)
                        + " points!"
                  );
               break;
            case 3:
               SevenSigns.this._activePeriod = 0;
               SevenSigns.this.sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_ENDED);
               SevenSigns.this.removeCPMult();
               SevenSigns.this.resetPlayerData();
               SevenSigns.this.resetSeals();
               SevenSigns.this._currentCycle++;
               SevenSignsFestival.getInstance().resetFestivalData(false);
               SevenSigns.this._dawnStoneScore = 0.0;
               SevenSigns.this._duskStoneScore = 0.0;
               SevenSigns.this._dawnFestivalScore = 0;
               SevenSigns.this._duskFestivalScore = 0;
         }

         SevenSigns.this.saveSevenSignsData();
         SevenSigns.this.saveSevenSignsStatus();
         SevenSigns.this.teleLosingCabalFromDungeons(SevenSigns.getCabalShortName(SevenSigns.this.getCabalHighestScore()));
         SSQInfo ss = new SSQInfo();
         World.broadcastToAllOnlinePlayers(ss);
         SevenSigns.this.spawnSevenSignsNPC();
         SevenSigns.LOGGER.info("SevenSigns: The " + SevenSigns.this.getCurrentPeriodName() + " period has begun!");
         SevenSigns.this.setCalendarForNextPeriodChange();
         SevenSigns.SevenSignsPeriodChange sspc = SevenSigns.this.new SevenSignsPeriodChange();
         ThreadPool.schedule(sspc, SevenSigns.this.getMilliToPeriodChange());
      }
   }

   private static class SingletonHolder {
      protected static final SevenSigns INSTANCE = new SevenSigns();
   }
}
