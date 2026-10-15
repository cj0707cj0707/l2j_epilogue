package org.l2jmobius.gameserver.network.clientpackets;

public class MoveWithDelta extends ClientPacket {
   private int _dx;
   private int _dy;
   private int _dz;

   @Override
   protected void readImpl() {
      this._dx = this.readInt();
      this._dy = this.readInt();
      this._dz = this.readInt();
   }

   @Override
   protected void runImpl() {
   }
}
