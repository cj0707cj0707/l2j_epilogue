package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SocialAction extends ServerPacket {
   public static final int LEVEL_UP = 2122;
   private final int _objectId;
   private final int _actionId;

   public SocialAction(int objectId, int actionId) {
      this._objectId = objectId;
      this._actionId = actionId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SOCIAL_ACTION.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._actionId);
   }

   public boolean canBeDropped(GameClient client) {
      return true;
   }
}
