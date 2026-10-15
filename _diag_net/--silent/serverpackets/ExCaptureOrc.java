package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExCaptureOrc extends ServerPacket {
   private static final byte[] _test = new byte[]{
      -28,
      -85,
      -114,
      -59,
      -23,
      -7,
      -122,
      123,
      -98,
      93,
      -125,
      20,
      5,
      -44,
      72,
      1,
      -51,
      -94,
      -115,
      -112,
      98,
      -116,
      -38,
      50,
      123,
      27,
      -121,
      109,
      8,
      -60,
      -31,
      86,
      -101,
      59,
      -61,
      64,
      -33,
      -24,
      -41,
      -31,
      -104,
      56,
      28,
      -91,
      -114,
      69,
      63,
      -14,
      94,
      28,
      89,
      -114,
      116,
      1,
      -98,
      -62,
      0,
      -107,
      -80,
      29,
      -121,
      -19,
      -100,
      -118
   };

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SEARCH_ORC.writeId(this, buffer);
      buffer.writeBytes(_test);
   }
}
