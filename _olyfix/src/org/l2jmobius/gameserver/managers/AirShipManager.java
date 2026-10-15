package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ExAirShipTeleportList;
import org.l2jmobius.gameserver.util.StatSet;

public class AirShipManager {
   private static final Logger LOGGER = Logger.getLogger(AirShipManager.class.getName());
   private static final String LOAD_DB = "SELECT * FROM airships";
   private static final String ADD_DB = "INSERT INTO airships (owner_id,fuel) VALUES (?,?)";
   private static final String UPDATE_DB = "UPDATE airships SET fuel=? WHERE owner_id=?";
   private CreatureTemplate _airShipTemplate = null;
   private final Map<Integer, StatSet> _airShipsInfo = new HashMap<>();
   private final Map<Integer, AirShip> _airShips = new HashMap<>();
   private final Map<Integer, AirShipManager.AirShipTeleportList> _teleports = new HashMap<>();

   protected AirShipManager() {
      StatSet npcDat = new StatSet();
      npcDat.set("npcId", 9);
      npcDat.set("level", 0);
      npcDat.set("jClass", "boat");
      npcDat.set("baseShldDef", 0);
      npcDat.set("baseShldRate", 0);
      npcDat.set("baseAccCombat", 38);
      npcDat.set("baseEvasRate", 38);
      npcDat.set("baseCritRate", 38);
      npcDat.set("collisionRadius", 0);
      npcDat.set("collisionHeight", 0);
      npcDat.set("sex", "male");
      npcDat.set("type", "");
      npcDat.set("baseAtkRange", 0);
      npcDat.set("baseMpMax", 0);
      npcDat.set("baseCpMax", 0);
      npcDat.set("rewardExp", 0);
      npcDat.set("rewardSp", 0);
      npcDat.set("basePAtk", 0);
      npcDat.set("baseMAtk", 0);
      npcDat.set("basePAtkSpd", 0);
      npcDat.set("aggroRange", 0);
      npcDat.set("baseMAtkSpd", 0);
      npcDat.set("rhand", 0);
      npcDat.set("lhand", 0);
      npcDat.set("armor", 0);
      npcDat.set("baseWalkSpd", 0);
      npcDat.set("baseRunSpd", 0);
      npcDat.set("name", "AirShip");
      npcDat.set("baseHpMax", 50000);
      npcDat.set("baseHpReg", 0.003F);
      npcDat.set("baseMpReg", 0.003F);
      npcDat.set("basePDef", 100);
      npcDat.set("baseMDef", 100);
      this._airShipTemplate = new CreatureTemplate(npcDat);
      this.load();
   }

   public AirShip getNewAirShip(int x, int y, int z, int heading) {
      AirShip airShip = new AirShip(this._airShipTemplate);
      airShip.setHeading(heading);
      airShip.setXYZInvisible(x, y, z);
      airShip.spawnMe();
      airShip.getStat().setMoveSpeed(280.0F);
      airShip.getStat().setRotationSpeed(2000);
      return airShip;
   }

   public AirShip getNewAirShip(int x, int y, int z, int heading, int ownerId) {
      StatSet info = this._airShipsInfo.get(ownerId);
      if (info == null) {
         return null;
      } else {
         AirShip airShip = this._airShips.get(ownerId);
         if (airShip != null) {
            airShip.refreshId();
         } else {
            airShip = new ControllableAirShip(this._airShipTemplate, ownerId);
            this._airShips.put(ownerId, airShip);
            airShip.setMaxFuel(600);
            airShip.setFuel(info.getInt("fuel"));
            airShip.getStat().setMoveSpeed(280.0F);
            airShip.getStat().setRotationSpeed(2000);
         }

         airShip.setHeading(heading);
         airShip.setXYZInvisible(x, y, z);
         airShip.spawnMe();
         return airShip;
      }
   }

   public void removeAirShip(AirShip ship) {
      if (ship.getOwnerId() != 0) {
         this.storeInDb(ship.getOwnerId());
         StatSet info = this._airShipsInfo.get(ship.getOwnerId());
         if (info != null) {
            info.set("fuel", ship.getFuel());
         }
      }
   }

   public boolean hasAirShipLicense(int ownerId) {
      return this._airShipsInfo.containsKey(ownerId);
   }

   public void registerLicense(int ownerId) {
      if (!this._airShipsInfo.containsKey(ownerId)) {
         StatSet info = new StatSet();
         info.set("fuel", 600);
         this._airShipsInfo.put(ownerId, info);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO airships (owner_id,fuel) VALUES (?,?)");
         ) {
            ps.setInt(1, ownerId);
            ps.setInt(2, info.getInt("fuel"));
            ps.executeUpdate();
         } catch (SQLException var11) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not add new airship license: " + var11.getMessage(), (Throwable)var11);
         } catch (Exception var12) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while initializing: " + var12.getMessage(), (Throwable)var12);
         }
      }
   }

   public boolean hasAirShip(int ownerId) {
      AirShip ship = this._airShips.get(ownerId);
      return ship != null && (ship.isSpawned() || ship.isTeleporting());
   }

   public void registerAirShipTeleportList(int dockId, int locationId, VehiclePathPoint[][] tp, int[] fuelConsumption) {
      if (tp.length == fuelConsumption.length) {
         this._teleports.put(dockId, new AirShipManager.AirShipTeleportList(locationId, fuelConsumption, tp));
      }
   }

   public void sendAirShipTeleportList(Player player) {
      if (player != null && player.isInAirShip()) {
         AirShip ship = player.getAirShip();
         if (ship.isCaptain(player) && ship.isInDock() && !ship.isMoving()) {
            int dockId = ship.getDockId();
            if (this._teleports.containsKey(dockId)) {
               AirShipManager.AirShipTeleportList all = this._teleports.get(dockId);
               player.sendPacket(new ExAirShipTeleportList(all.getLocation(), all.getRoute(), all.getFuel()));
            }
         }
      }
   }

   public VehiclePathPoint[] getTeleportDestination(int dockId, int index) {
      AirShipManager.AirShipTeleportList all = this._teleports.get(dockId);
      return all != null && index >= -1 && index < all.getRoute().length ? all.getRoute()[index + 1] : null;
   }

   public int getFuelConsumption(int dockId, int index) {
      AirShipManager.AirShipTeleportList all = this._teleports.get(dockId);
      return all != null && index >= -1 && index < all.getFuel().length ? all.getFuel()[index + 1] : 0;
   }

   private void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM airships");
      ) {
         while (rs.next()) {
            StatSet info = new StatSet();
            info.set("fuel", rs.getInt("fuel"));
            this._airShipsInfo.put(rs.getInt("owner_id"), info);
         }
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not load airships table: " + var12.getMessage(), (Throwable)var12);
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while initializing: " + var13.getMessage(), (Throwable)var13);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._airShipsInfo.size() + " private airships");
   }

   private void storeInDb(int ownerId) {
      StatSet info = this._airShipsInfo.get(ownerId);
      if (info != null) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE airships SET fuel=? WHERE owner_id=?");
         ) {
            ps.setInt(1, info.getInt("fuel"));
            ps.setInt(2, ownerId);
            ps.executeUpdate();
         } catch (SQLException var11) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not update airships table: " + var11.getMessage(), (Throwable)var11);
         } catch (Exception var12) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while save: " + var12.getMessage(), (Throwable)var12);
         }
      }
   }

   public static AirShipManager getInstance() {
      return AirShipManager.SingletonHolder.INSTANCE;
   }

   private static class AirShipTeleportList {
      private final int _location;
      private final int[] _fuel;
      private final VehiclePathPoint[][] _routes;

      public AirShipTeleportList(int locationValue, int[] fuelValue, VehiclePathPoint[][] routesValue) {
         this._location = locationValue;
         this._fuel = fuelValue;
         this._routes = routesValue;
      }

      public int getLocation() {
         return this._location;
      }

      public int[] getFuel() {
         return this._fuel;
      }

      public VehiclePathPoint[][] getRoute() {
         return this._routes;
      }
   }

   private static class SingletonHolder {
      protected static final AirShipManager INSTANCE = new AirShipManager();
   }
}
