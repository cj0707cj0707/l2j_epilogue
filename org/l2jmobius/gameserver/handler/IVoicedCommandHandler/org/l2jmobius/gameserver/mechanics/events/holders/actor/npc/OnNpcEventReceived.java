package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnNpcEventReceived implements IBaseEvent {
   private final String _eventName;
   private final Npc _sender;
   private final Npc _receiver;
   private final WorldObject _reference;

   public OnNpcEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference) {
      this._eventName = eventName;
      this._sender = sender;
      this._receiver = receiver;
      this._reference = reference;
   }

   public String getEventName() {
      return this._eventName;
   }

   public Npc getSender() {
      return this._sender;
   }

   public Npc getReceiver() {
      return this._receiver;
   }

   public WorldObject getReference() {
      return this._reference;
   }

   @Override
   public EventType getType() {
      return EventType.ON_NPC_EVENT_RECEIVED;
   }
}
