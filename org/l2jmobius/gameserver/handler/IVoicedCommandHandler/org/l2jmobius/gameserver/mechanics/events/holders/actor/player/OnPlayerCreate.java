package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.network.GameClient;

public class OnPlayerCreate implements IBaseEvent {
   private final Player _player;
   private final int _objectId;
   private final String _name;
   private final GameClient _client;

   public OnPlayerCreate(Player player, int objectId, String name, GameClient client) {
      this._player = player;
      this._objectId = objectId;
      this._name = name;
      this._client = client;
   }

   public Player getPlayer() {
      return this._player;
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
      return EventType.ON_PLAYER_CREATE;
   }
}
