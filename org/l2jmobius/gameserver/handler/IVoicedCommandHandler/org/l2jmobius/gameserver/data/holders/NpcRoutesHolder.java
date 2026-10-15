package org.l2jmobius.gameserver.data.holders;

import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.interfaces.ILocational;

public class NpcRoutesHolder {
   private final Map<String, String> _correspondences = new HashMap<>();

   public void addRoute(String routeName, Location loc) {
      this._correspondences.put(this.getUniqueKey(loc), routeName);
   }

   public String getRouteName(Npc npc) {
      if (npc.getSpawn() != null) {
         String key = this.getUniqueKey(npc.getSpawn().getLocation());
         return this._correspondences.getOrDefault(key, "");
      } else {
         return "";
      }
   }

   private String getUniqueKey(ILocational loc) {
      return loc.getX() + "-" + loc.getY() + "-" + loc.getZ();
   }
}
