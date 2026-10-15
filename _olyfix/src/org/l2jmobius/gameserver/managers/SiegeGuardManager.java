package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

public class SiegeGuardManager {
   private static final Logger LOGGER = Logger.getLogger(SiegeGuardManager.class.getName());
   private final Castle _castle;
   private final List<Spawn> _siegeGuardSpawn = new ArrayList<>();

   public SiegeGuardManager(Castle castle) {
      this._castle = castle;
   }

   public void addSiegeGuard(Player player, int npcId) {
      if (player != null) {
         this.addSiegeGuard(player.getX(), player.getY(), player.getZ(), player.getHeading(), npcId);
      }
   }

   public void addSiegeGuard(int x, int y, int z, int heading, int npcId) {
      this.saveSiegeGuard(x, y, z, heading, npcId, 0);
   }

   public void hireMerc(Player player, int npcId) {
      if (player != null) {
         this.hireMerc(player.getX(), player.getY(), player.getZ(), player.getHeading(), npcId);
      }
   }

   public void hireMerc(int x, int y, int z, int heading, int npcId) {
      this.saveSiegeGuard(x, y, z, heading, npcId, 1);
   }

   public void removeMerc(int npcId, int x, int y, int z) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("Delete From castle_siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1");
      ) {
         ps.setInt(1, npcId);
         ps.setInt(2, x);
         ps.setInt(3, y);
         ps.setInt(4, z);
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": Error deleting hired siege guard at " + x + "," + y + "," + z + ": " + var13.getMessage(),
            (Throwable)var13
         );
      }
   }

   public void removeMercs() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("Delete From castle_siege_guards Where castleId = ? And isHired = 1");
      ) {
         ps.setInt(1, this._castle.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": Error deleting hired siege guard for castle " + this._castle.getName() + ": " + var9.getMessage(),
            (Throwable)var9
         );
      }
   }

   public void spawnSiegeGuard() {
      try {
         int hiredCount = 0;
         int hiredMax = MercTicketManager.getInstance().getMaxAllowedMerc(this._castle.getResidenceId());
         boolean isHired = this._castle.getOwnerId() > 0;
         this.loadSiegeGuard();

         for (Spawn spawn : this._siegeGuardSpawn) {
            if (spawn != null) {
               spawn.init();
               if (isHired) {
                  spawn.stopRespawn();
                  if (++hiredCount > hiredMax) {
                     return;
                  }
               } else if (spawn.getRespawnDelay() == 0) {
                  spawn.stopRespawn();
               }
            }
         }
      } catch (Exception var6) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error spawning siege guards for castle " + this._castle.getName(), (Throwable)var6);
      }
   }

   public void unspawnSiegeGuard() {
      for (Spawn spawn : this._siegeGuardSpawn) {
         if (spawn != null && spawn.getLastSpawn() != null) {
            spawn.stopRespawn();
            spawn.getLastSpawn().deleteMe();
         }
      }

      this._siegeGuardSpawn.clear();
   }

   private void loadSiegeGuard() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_siege_guards Where castleId = ? And isHired = ?");
      ) {
         ps.setInt(1, this._castle.getResidenceId());
         ps.setInt(2, this._castle.getOwnerId() > 0 ? 1 : 0);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Spawn spawn = new Spawn(rs.getInt("npcId"));
               spawn.setAmount(1);
               spawn.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawn.setHeading(rs.getInt("heading"));
               spawn.setRespawnDelay(rs.getInt("respawnDelay"));
               spawn.setLocationId(0);
               this._siegeGuardSpawn.add(spawn);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": Error loading siege guard for castle " + this._castle.getName() + ": " + var12.getMessage(),
            (Throwable)var12
         );
      }
   }

   private void saveSiegeGuard(int x, int y, int z, int heading, int npcId, int isHire) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "Insert Into castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)"
         );
      ) {
         ps.setInt(1, this._castle.getResidenceId());
         ps.setInt(2, npcId);
         ps.setInt(3, x);
         ps.setInt(4, y);
         ps.setInt(5, z);
         ps.setInt(6, heading);
         ps.setInt(7, isHire == 1 ? 0 : 600);
         ps.setInt(8, isHire);
         ps.execute();
      } catch (Exception var15) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": Error adding siege guard for castle " + this._castle.getName() + ": " + var15.getMessage(),
            (Throwable)var15
         );
      }
   }

   public Castle getCastle() {
      return this._castle;
   }

   public List<Spawn> getSiegeGuardSpawn() {
      return this._siegeGuardSpawn;
   }
}
