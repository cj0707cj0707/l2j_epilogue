package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.network.GameClient;

public class OnPlayerDelete implements IBaseEvent {
   private final int _objectId;
   private final String _name;
   private final GameClient _client;

   public OnPlayerDelete(int objectId, String name, GameClient client) {
      this._objectId = objectId;
      this._name = name;
      this._client = client;
   }

   public int getObjectId() {
      return this._objectId;
   }

   public String getName() {
      return this._name;
   }

   public GameClient getClient() {
      return this._client;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_DELETE;
   }
}
