package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

public class FortSiegeGuardManager {
   private static final Logger LOGGER = Logger.getLogger(FortSiegeGuardManager.class.getName());
   private final Fort _fort;
   private final Map<Integer, List<Spawn>> _siegeGuards = new HashMap<>();

   public FortSiegeGuardManager(Fort fort) {
      this._fort = fort;
   }

   public void spawnSiegeGuard() {
      try {
         List<Spawn> monsterList = this._siegeGuards.get(this._fort.getResidenceId());
         if (monsterList != null) {
            for (Spawn spawnDat : monsterList) {
               spawnDat.doSpawn(false);
               if (spawnDat.getRespawnDelay() == 0) {
                  spawnDat.stopRespawn();
               } else {
                  spawnDat.startRespawn();
               }
            }
         }
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, "Error spawning siege guards for fort " + this._fort.getName() + ":" + var4.getMessage(), (Throwable)var4);
      }
   }

   public void unspawnSiegeGuard() {
      try {
         List<Spawn> monsterList = this._siegeGuards.get(this._fort.getResidenceId());
         if (monsterList != null) {
            for (Spawn spawnDat : monsterList) {
               spawnDat.stopRespawn();
               if (spawnDat.getLastSpawn() != null) {
                  spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
               }
            }
         }
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, "Error unspawning siege guards for fort " + this._fort.getName() + ":" + var4.getMessage(), (Throwable)var4);
      }
   }

   void loadSiegeGuard() {
      this._siegeGuards.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?");
      ) {
         int fortId = this._fort.getResidenceId();
         ps.setInt(1, fortId);

         try (ResultSet rs = ps.executeQuery()) {
            List<Spawn> siegeGuardSpawns = new ArrayList<>();

            while (rs.next()) {
               Spawn spawn = new Spawn(rs.getInt("npcId"));
               spawn.setAmount(1);
               spawn.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawn.setHeading(rs.getInt("heading"));
               spawn.setRespawnDelay(rs.getInt("respawnDelay"));
               spawn.setLocationId(0);
               siegeGuardSpawns.add(spawn);
            }

            this._siegeGuards.put(fortId, siegeGuardSpawns);
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Error loading siege guard for fort " + this._fort.getName() + ": " + var13.getMessage(), (Throwable)var13);
      }
   }

   public Fort getFort() {
      return this._fort;
   }

   public Map<Integer, List<Spawn>> getSiegeGuardSpawn() {
      return this._siegeGuards;
   }
}
