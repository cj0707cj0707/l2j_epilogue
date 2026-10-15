package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharCreateFail extends ServerPacket {
   public static final int REASON_CREATION_FAILED = 0;
   public static final int REASON_TOO_MANY_CHARACTERS = 1;
   public static final int REASON_NAME_ALREADY_EXISTS = 2;
   public static final int REASON_16_ENG_CHARS = 3;
   public static final int REASON_INCORRECT_NAME = 4;
   public static final int REASON_CREATE_NOT_ALLOWED = 5;
   public static final int REASON_CHOOSE_ANOTHER_SVR = 6;
   private final int _error;

   public CharCreateFail(int errorCode) {
      this._error = errorCode;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHARACTER_CREATE_FAIL.writeId(this, buffer);
      buffer.writeInt(this._error);
   }
}
