package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class HqZone extends ZoneType {
   public HqZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (!"castleId".equals(name) && !"fortId".equals(name) && !"clanHallId".equals(name) && !"territoryId".equals(name)) {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.HQ, true);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.HQ, false);
      }
   }
}
