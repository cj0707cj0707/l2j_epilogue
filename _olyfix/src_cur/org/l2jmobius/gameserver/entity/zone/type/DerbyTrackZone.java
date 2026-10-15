package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class DerbyTrackZone extends ZoneType {
   public DerbyTrackZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayable()) {
         creature.setInsideZone(ZoneId.MONSTER_TRACK, true);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayable()) {
         creature.setInsideZone(ZoneId.MONSTER_TRACK, false);
      }
   }
}
