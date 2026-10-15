package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ArenaZone extends ZoneType {
   public ArenaZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP)) {
         creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
      }

      creature.setInsideZone(ZoneId.PVP, true);
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.PVP, false);
      if (creature.isPlayer() && !creature.isInsideZone(ZoneId.PVP)) {
         creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
      }
   }
}
