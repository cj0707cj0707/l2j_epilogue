package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class NoSummonFriendZone extends ZoneType {
   public NoSummonFriendZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
   }
}
