package org.l2jmobius.gameserver.network.clientpackets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.l2jmobius.gameserver.network.PacketLogger;

public class GameGuardReply extends ClientPacket {
   private static final byte[] VALID = new byte[]{-120, 64, 28, -89, -125, 66, -23, 21, -34, -61, 104, -10, 45, 35, -15, 63, -18, 104, 91, -59};
   private final byte[] _reply = new byte[8];

   @Override
   protected void readImpl() {
      this._reply[0] = this.readByte();
      this._reply[1] = this.readByte();
      this._reply[2] = this.readByte();
      this._reply[3] = this.readByte();
      this.readInt();
      this._reply[4] = this.readByte();
      this._reply[5] = this.readByte();
      this._reply[6] = this.readByte();
      this._reply[7] = this.readByte();
   }

   @Override
   protected void runImpl() {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA");
         byte[] result = md.digest(this._reply);
         if (Arrays.equals(result, VALID)) {
            this.getClient().setGameGuardOk(true);
         }
      } catch (NoSuchAlgorithmException var3) {
         PacketLogger.warning(this.getClass().getSimpleName() + ": " + var3.getMessage());
      }
   }
}
