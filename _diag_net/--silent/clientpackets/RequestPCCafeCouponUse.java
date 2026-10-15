package org.l2jmobius.gameserver.network.clientpackets;

public class RequestPCCafeCouponUse extends ClientPacket {
   private String _str;

   @Override
   protected void readImpl() {
      this._str = this.readString();
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.PacketLogger.info("C5: RequestPCCafeCouponUse: S: " + this._str);
   }
}
