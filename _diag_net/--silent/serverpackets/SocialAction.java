package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SocialAction extends ServerPacket {
   public static final int LEVEL_UP = 2122;
   private final int _objectId;
   private final int _actionId;

   public SocialAction(int objectId, int actionId) {
      this._objectId = objectId;
      this._actionId = actionId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SOCIAL_ACTION.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._actionId);
   }

   public boolean canBeDropped(org.l2jmobius.gameserver.network.GameClient client) {
      return true;
   }
}
