package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharDeleteFail extends ServerPacket {
   public static final int REASON_DELETION_FAILED = 1;
   public static final int REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER = 2;
   public static final int REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED = 3;
   private final int _error;

   public CharDeleteFail(int errorCode) {
      this._error = errorCode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHARACTER_DELETE_FAIL.writeId(this, buffer);
      buffer.writeInt(this._error);
   }
}
