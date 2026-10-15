package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnAttackableHate implements IBaseEvent {
   private final Attackable _npc;
   private final Player _player;
   private final boolean _isSummon;

   public OnAttackableHate(Attackable npc, Player player, boolean isSummon) {
      this._npc = npc;
      this._player = player;
      this._isSummon = isSummon;
   }

   public Attackable getNpc() {
      return this._npc;
   }

   public Player getPlayer() {
      return this._player;
   }

   public boolean isSummon() {
      return this._isSummon;
   }

   @Override
   public EventType getType() {
      return EventType.ON_NPC_HATE;
   }
}
