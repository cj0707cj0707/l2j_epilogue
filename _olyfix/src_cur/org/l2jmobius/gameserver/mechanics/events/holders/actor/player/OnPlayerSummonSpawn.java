package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerSummonSpawn implements IBaseEvent {
   private final Summon _summon;

   public OnPlayerSummonSpawn(Summon summon) {
      this._summon = summon;
   }

   public Summon getSummon() {
      return this._summon;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_SUMMON_SPAWN;
   }
}
