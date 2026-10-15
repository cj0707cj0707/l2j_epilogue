package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class LoginFail extends ServerPacket {
   public static final int NO_TEXT = 0;
   public static final int SYSTEM_ERROR_LOGIN_LATER = 1;
   public static final int PASSWORD_DOES_NOT_MATCH_THIS_ACCOUNT = 2;
   public static final int PASSWORD_DOES_NOT_MATCH_THIS_ACCOUNT2 = 3;
   public static final int ACCESS_FAILED_TRY_LATER = 4;
   public static final int INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT = 5;
   public static final int ACCESS_FAILED_TRY_LATER2 = 6;
   public static final int ACOUNT_ALREADY_IN_USE = 7;
   public static final int ACCESS_FAILED_TRY_LATER3 = 8;
   public static final int ACCESS_FAILED_TRY_LATER4 = 9;
   public static final int ACCESS_FAILED_TRY_LATER5 = 10;
   private final int _reason;

   public LoginFail(int reason) {
      this._reason = reason;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.LOGIN_FAIL.writeId(this, buffer);
      buffer.writeInt(this._reason);
   }
}
