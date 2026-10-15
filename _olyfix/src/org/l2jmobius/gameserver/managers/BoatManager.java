package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.StatSet;

public class BoatManager {
   private final Map<Integer, Boat> _boats = new ConcurrentHashMap<>();
   private final boolean[] _docksBusy = new boolean[3];
   public static final int TALKING_ISLAND = 1;
   public static final int GLUDIN_HARBOR = 2;
   public static final int RUNE_HARBOR = 3;

   public static BoatManager getInstance() {
      return BoatManager.SingletonHolder.INSTANCE;
   }

   protected BoatManager() {
      for (int i = 0; i < this._docksBusy.length; i++) {
         this._docksBusy[i] = false;
      }
   }

   public Boat getNewBoat(int boatId, int x, int y, int z, int heading) {
      if (!GeneralConfig.ALLOW_BOAT) {
         return null;
      } else {
         StatSet npcDat = new StatSet();
         npcDat.set("npcId", boatId);
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
         npcDat.set("baseHpMax", 50000);
         npcDat.set("baseHpReg", 0.003F);
         npcDat.set("baseMpReg", 0.003F);
         npcDat.set("basePDef", 100);
         npcDat.set("baseMDef", 100);
         Boat boat = new Boat(new CreatureTemplate(npcDat));
         boat.setHeading(heading);
         boat.setXYZInvisible(x, y, z);
         boat.spawnMe();
         this._boats.put(boat.getObjectId(), boat);
         return boat;
      }
   }

   public Boat getBoat(int boatId) {
      return this._boats.get(boatId);
   }

   public void dockShip(int h, boolean value) {
      try {
         this._docksBusy[h] = value;
      } catch (ArrayIndexOutOfBoundsException var4) {
      }
   }

   public boolean dockBusy(int h) {
      try {
         return this._docksBusy[h];
      } catch (ArrayIndexOutOfBoundsException var3) {
         return false;
      }
   }

   public void broadcastPacket(VehiclePathPoint point1, VehiclePathPoint point2, ServerPacket packet) {
      this.broadcastPacketsToPlayers(point1, point2, packet);
   }

   public void broadcastPackets(VehiclePathPoint point1, VehiclePathPoint point2, ServerPacket... packets) {
      this.broadcastPacketsToPlayers(point1, point2, packets);
   }

   private void broadcastPacketsToPlayers(VehiclePathPoint point1, VehiclePathPoint point2, ServerPacket... packets) {
      double radSq = (double)GeneralConfig.BOAT_BROADCAST_RADIUS * GeneralConfig.BOAT_BROADCAST_RADIUS;

      for (Player player : World.getPlayers()) {
         int px = player.getX();
         int py = player.getY();
         double dx1 = px - point1.getX();
         double dy1 = py - point1.getY();
         double dx1Sq = dx1 * dx1;
         double dy1Sq = dy1 * dy1;
         if (dx1Sq + dy1Sq < radSq) {
            for (ServerPacket p : packets) {
               player.sendPacket(p);
            }
         } else {
            double dx2 = px - point2.getX();
            double dy2 = py - point2.getY();
            double dx2Sq = dx2 * dx2;
            double dy2Sq = dy2 * dy2;
            if (dx2Sq + dy2Sq < radSq) {
               for (ServerPacket p : packets) {
                  player.sendPacket(p);
               }
            }
         }
      }
   }

   private static class SingletonHolder {
      protected static final BoatManager INSTANCE = new BoatManager();
   }
}
