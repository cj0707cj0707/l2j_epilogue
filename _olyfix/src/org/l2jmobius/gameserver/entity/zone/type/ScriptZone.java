package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class ScriptZone extends ZoneType {
   public ScriptZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      creature.setInsideZone(ZoneId.SCRIPT, true);
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.SCRIPT, false);
   }
}
