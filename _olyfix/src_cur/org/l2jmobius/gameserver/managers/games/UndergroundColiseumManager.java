package org.l2jmobius.gameserver.managers.games;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCBestTeam;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCPoint;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCReward;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class UndergroundColiseumManager implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(UndergroundColiseumManager.class.getName());
   private final Map<Integer, UCArena> _arenas = new HashMap<>(5);
   private boolean _isStarted = false;
   private long _periodStartTime;
   private long _periodEndTime;
   private ScheduledFuture<?> _regTask = null;
   private final Map<Integer, UCBestTeam> _bestTeams = new HashMap<>(5);

   protected UndergroundColiseumManager() {
      this.load();
   }

   @Override
   public void load() {
      this._periodStartTime = GlobalVariablesManager.getInstance().getLong("UC_START_TIME", 0L);
      this._periodEndTime = GlobalVariablesManager.getInstance().getLong("UC_STOP_TIME", 0L);
      long curerntTime = System.currentTimeMillis();
      if (this._periodStartTime < curerntTime && this._periodEndTime < curerntTime) {
         this.generateNewDate();
      }

      this.parseDatapackFile("data/UndergroundColiseum.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._arenas.size() + " coliseum arenas.");
      if (this._periodStartTime < curerntTime && this._periodEndTime > curerntTime) {
         this.switchStatus(true);
      } else {
         long nextTime = this._periodStartTime - curerntTime;
         this._regTask = ThreadPool.schedule(new UndergroundColiseumManager.UCRegistrationTask(true), nextTime);
         LOGGER.info(this.getClass().getSimpleName() + ": Starts at " + TimeUtil.getDateTimeString(this._periodStartTime));
      }

      this.restoreBestTeams();
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("arena".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap map = d.getAttributes();
                  int id = Integer.parseInt(map.getNamedItem("id").getNodeValue());
                  int min_level = Integer.parseInt(map.getNamedItem("minLvl").getNodeValue());
                  int max_level = Integer.parseInt(map.getNamedItem("maxLvl").getNodeValue());
                  int curator = Integer.parseInt(map.getNamedItem("curator").getNodeValue());
                  UCArena arena = new UCArena(id, curator, min_level, max_level);
                  int index = 0;
                  int index2 = 0;

                  for (Node und = d.getFirstChild(); und != null; und = und.getNextSibling()) {
                     if ("tower".equalsIgnoreCase(und.getNodeName())) {
                        map = und.getAttributes();
                        int npcId = Integer.parseInt(map.getNamedItem("id").getNodeValue());
                        int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
                        int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
                        int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
                        UCTeam team = new UCTeam(index, arena, x, y, z, npcId);
                        arena.setUCTeam(index, team);
                        index++;
                     } else if ("spawn".equalsIgnoreCase(und.getNodeName())) {
                        map = und.getAttributes();
                        List<Door> doors = new ArrayList<>();
                        String doorList = map.getNamedItem("doors") != null ? map.getNamedItem("doors").getNodeValue() : "";
                        if (!doorList.isEmpty()) {
                           String[] doorSplint = doorList.split(",");

                           for (String doorId : doorSplint) {
                              Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
                              if (door != null) {
                                 doors.add(door);
                              }
                           }
                        }

                        int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
                        int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
                        int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
                        UCPoint point = new UCPoint(doors, new Location(x, y, z));
                        arena.setUCPoint(index2, point);
                        index2++;
                     } else if ("rewards".equalsIgnoreCase(und.getNodeName())) {
                        for (Node c = und.getFirstChild(); c != null; c = c.getNextSibling()) {
                           if ("item".equalsIgnoreCase(c.getNodeName())) {
                              int itemId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                              long amount = Long.parseLong(c.getAttributes().getNamedItem("amount").getNodeValue());
                              boolean useModifier = c.getAttributes().getNamedItem("useModifers") != null
                                 ? Boolean.parseBoolean(c.getAttributes().getNamedItem("useModifers").getNodeValue())
                                 : false;
                              arena.setReward(new UCReward(itemId, amount, useModifier));
                           }
                        }
                     }
                  }

                  this._arenas.put(arena.getId(), arena);
               }
            }
         }
      }
   }

   private void restoreBestTeams() {
      this._bestTeams.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT * FROM underground_coliseum ORDER BY arenaId");
         ResultSet rset = statement.executeQuery();
      ) {
         while (rset.next()) {
            int arenaId = rset.getInt("arenaId");
            String leader = rset.getString("leader");
            int wins = rset.getInt("wins");
            this._bestTeams.put(arenaId, new UCBestTeam(arenaId, leader, wins));
         }
      } catch (SQLException var13) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not load underground_coliseum table");
      } catch (Exception var14) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Error while initializing UndergroundColiseumManager: " + var14.getMessage());
      }
   }

   private void saveBestTeam(UCBestTeam team, boolean isNew) {
      if (isNew) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO underground_coliseum (`arenaId`, `leader`, `wins`) VALUES (?,?,?) ");
         ) {
            ps.setInt(1, team.getArenaId());
            ps.setString(2, team.getLeaderName());
            ps.setInt(3, team.getWins());
            ps.executeUpdate();
         } catch (SQLException var14) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Could not save underground_coliseum: " + var14.getMessage());
         }
      } else {
         try (Connection con = DatabaseFactory.getConnection()) {
            PreparedStatement stmt = con.prepareStatement("UPDATE underground_coliseum SET leader = ?, wins = ?  WHERE arenaId = ?");
            stmt.setInt(1, team.getArenaId());
            stmt.setInt(2, team.getWins());
            stmt.setInt(3, team.getArenaId());
            stmt.execute();
            stmt.close();
         } catch (Exception var11) {
            LOGGER.warning(
               this.getClass().getSimpleName() + ": could not clean status for underground_coliseum areanaId: " + team.getArenaId() + " in database!"
            );
         }
      }
   }

   public UCBestTeam getBestTeam(int arenaId) {
      return this._bestTeams.get(arenaId);
   }

   public void updateBestTeam(int arenaId, String name, int wins) {
      if (this._bestTeams.containsKey(arenaId)) {
         UCBestTeam team = this.getBestTeam(arenaId);
         if (team != null) {
            team.setLeader(name);
            team.setWins(wins);
            this.saveBestTeam(team, false);
         }
      } else {
         UCBestTeam team = new UCBestTeam(arenaId, name, wins);
         this._bestTeams.put(arenaId, team);
         this.saveBestTeam(team, true);
      }
   }

   private void generateNewDate() {
      SchedulingPattern timePattern = new SchedulingPattern(UndergroundColiseumConfig.UC_START_TIME);
      this._periodStartTime = timePattern.next(System.currentTimeMillis());
      this._periodEndTime = this._periodStartTime + UndergroundColiseumConfig.UC_TIME_PERIOD * 3600000;
      GlobalVariablesManager.getInstance().set("UC_START_TIME", this._periodStartTime);
      GlobalVariablesManager.getInstance().set("UC_STOP_TIME", this._periodEndTime);
   }

   public UCArena getArena(int id) {
      return this._arenas.get(id);
   }

   public void setStarted(boolean started) {
      this._isStarted = started;

      for (UCArena arena : this.getAllArenas()) {
         arena.switchStatus(started);
      }

      if (UndergroundColiseumConfig.UC_ALLOW_ANNOUNCE) {
         if (this._isStarted) {
            World.broadcastToAllOnlinePlayers("Underground Coliseum has started!");
         } else {
            World.broadcastToAllOnlinePlayers("Underground Coliseum has stopped!");
         }
      }
   }

   public boolean isStarted() {
      return this._isStarted;
   }

   public Collection<UCArena> getAllArenas() {
      return this._arenas.values();
   }

   private void switchStatus(boolean isStart) {
      if (this._regTask != null) {
         this._regTask.cancel(false);
         this._regTask = null;
      }

      this.setStarted(isStart);
      if (isStart) {
         long nextTime = this._periodEndTime - System.currentTimeMillis();
         this._regTask = ThreadPool.schedule(new UndergroundColiseumManager.UCRegistrationTask(false), nextTime);
         LOGGER.info(this.getClass().getSimpleName() + ": Ends at " + TimeUtil.getDateTimeString(this._periodEndTime));
      } else {
         this.generateNewDate();
         long nextTime = this._periodStartTime - System.currentTimeMillis();
         this._regTask = ThreadPool.schedule(new UndergroundColiseumManager.UCRegistrationTask(true), nextTime);
         LOGGER.info(this.getClass().getSimpleName() + ": Starts at " + TimeUtil.getDateTimeString(this._periodStartTime));
      }
   }

   public static UndergroundColiseumManager getInstance() {
      return UndergroundColiseumManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final UndergroundColiseumManager INSTANCE = new UndergroundColiseumManager();
   }

   public class UCRegistrationTask implements Runnable {
      private final boolean _status;

      public UCRegistrationTask(boolean status) {
         Objects.requireNonNull(UndergroundColiseumManager.this);
         super();
         this._status = status;
      }

      @Override
      public void run() {
         UndergroundColiseumManager.this.switchStatus(this._status);
      }
   }
}
