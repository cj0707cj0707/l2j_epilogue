package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class FishingZone extends ZoneType {
   public FishingZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
   }

   @Override
   protected void onExit(Creature creature) {
   }

   public int getWaterZ() {
      return this.getZone().getHighZ();
   }
}
