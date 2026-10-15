package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

class OlympiadStadium {
   private static final Logger _log = Logger.getLogger(OlympiadStadium.class.getName());
   private boolean _freeToUse = true;
   private static DoorData _doorTable;
   private final int[] _coords = new int[3];
   private final int[] _doors = new int[2];
   private final List<Player> _spectators;

   public boolean isFreeToUse() {
      return this._freeToUse;
   }

   public void setStadiaBusy() {
      this._freeToUse = false;
   }

   public void setStadiaFree() {
      this._freeToUse = true;
   }

   public int[] getCoordinates() {
      return this._coords;
   }

   public int[] getDoorID() {
      return this._doors;
   }

   public OlympiadStadium(int x, int y, int z, int d1, int d2) {
      this._coords[0] = x;
      this._coords[1] = y;
      this._coords[2] = z;
      this._doors[0] = d1;
      this._doors[1] = d2;
      this._spectators = new CopyOnWriteArrayList<>();
   }

   public void openDoors() {
      _doorTable = DoorData.getInstance();

      try {
         _doorTable.getDoor(this.getDoorID()[0]).openMe();
         _doorTable.getDoor(this.getDoorID()[1]).openMe();
      } catch (Exception var2) {
         _log.log(Level.WARNING, "", (Throwable)var2);
      }
   }

   public void closeDoors() {
      _doorTable = DoorData.getInstance();

      try {
         _doorTable.getDoor(this.getDoorID()[0]).closeMe();
         _doorTable.getDoor(this.getDoorID()[1]).closeMe();
      } catch (Exception var2) {
         _log.log(Level.WARNING, "", (Throwable)var2);
      }
   }

   protected void addSpectator(int id, Player spec, boolean storeCoords) {
      Location loc = new Location(this.getCoordinates()[0] + 1200, this.getCoordinates()[1], this.getCoordinates()[2]);
      spec.enterOlympiadObserverMode(loc, id, storeCoords);
      this._spectators.add(spec);
   }

   protected List<Player> getSpectators() {
      return this._spectators;
   }

   protected void removeSpectator(Player spec) {
      if (this._spectators != null) {
         this._spectators.remove(spec);
      }
   }
}
