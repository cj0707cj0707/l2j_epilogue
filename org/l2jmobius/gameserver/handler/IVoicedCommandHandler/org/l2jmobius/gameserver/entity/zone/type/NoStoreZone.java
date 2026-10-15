package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class NoStoreZone extends ZoneType {
   public NoStoreZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.NO_STORE, true);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.NO_STORE, false);
      }
   }
}
