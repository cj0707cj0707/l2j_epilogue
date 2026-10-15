package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnNpcMoveFinished implements IBaseEvent {
   private final Npc _npc;

   public OnNpcMoveFinished(Npc npc) {
      this._npc = npc;
   }

   public Npc getNpc() {
      return this._npc;
   }

   @Override
   public EventType getType() {
      return EventType.ON_NPC_MOVE_FINISHED;
   }
}
