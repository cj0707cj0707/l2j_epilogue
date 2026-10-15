package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnCreatureZoneExit implements IBaseEvent {
   private final Creature _creature;
   private final ZoneType _zone;

   public OnCreatureZoneExit(Creature creature, ZoneType zone) {
      this._creature = creature;
      this._zone = zone;
   }

   public Creature getCreature() {
      return this._creature;
   }

   public ZoneType getZone() {
      return this._zone;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CREATURE_ZONE_EXIT;
   }
}
