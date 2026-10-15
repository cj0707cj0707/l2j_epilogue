package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

public class InitLS extends SimpleReadablePacket {
   private final int _rev;
   private final byte[] _key;

   public int getRevision() {
      return this._rev;
   }

   public byte[] getRSAKey() {
      return this._key;
   }

   public InitLS(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      this._rev = this.readInt();
      int size = this.readInt();
      this._key = this.readBytes(size);
   }
}
