package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnAttackableAggroRangeEnter implements IBaseEvent {
   private final Npc _npc;
   private final Player _player;
   private final boolean _isSummon;

   public OnAttackableAggroRangeEnter(Npc npc, Player attacker, boolean isSummon) {
      this._npc = npc;
      this._player = attacker;
      this._isSummon = isSummon;
   }

   public Npc getNpc() {
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
      return EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER;
   }
}
