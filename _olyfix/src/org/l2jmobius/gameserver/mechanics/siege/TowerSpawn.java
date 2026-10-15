package org.l2jmobius.gameserver.mechanics.siege;

import java.util.List;
import org.l2jmobius.gameserver.entity.Location;

public class TowerSpawn {
   private final int _npcId;
   private final Location _location;
   private List<Integer> _zoneList = null;
   private int _upgradeLevel = 0;

   public TowerSpawn(int npcId, Location location) {
      this._location = location;
      this._npcId = npcId;
   }

   public TowerSpawn(int npcId, Location location, List<Integer> zoneList) {
      this._location = location;
      this._npcId = npcId;
      this._zoneList = zoneList;
   }

   public int getId() {
      return this._npcId;
   }

   public Location getLocation() {
      return this._location;
   }

   public List<Integer> getZoneList() {
      return this._zoneList;
   }

   public void setUpgradeLevel(int level) {
      this._upgradeLevel = level;
   }

   public int getUpgradeLevel() {
      return this._upgradeLevel;
   }
}
