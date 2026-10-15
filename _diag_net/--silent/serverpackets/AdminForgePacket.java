package org.l2jmobius.gameserver.network.serverpackets;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class AdminForgePacket extends ServerPacket {
   private final List<AdminForgePacket.Part> _parts = new ArrayList<>();

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      for (AdminForgePacket.Part p : this._parts) {
         this.generate(p.b, p.str, buffer);
      }
   }

   public boolean generate(byte type, String value, WriteBuffer buffer) {
      if (type == 67 || type == 99) {
         buffer.writeByte(Integer.decode(value));
         return true;
      } else if (type == 68 || type == 100) {
         buffer.writeInt(Integer.decode(value));
         return true;
      } else if (type == 72 || type == 104) {
         buffer.writeShort(Integer.decode(value));
         return true;
      } else if (type == 70 || type == 102) {
         buffer.writeDouble(Double.parseDouble(value));
         return true;
      } else if (type == 83 || type == 115) {
         buffer.writeString(value);
         return true;
      } else if (type == 66 || type == 98 || type == 88 || type == 120) {
         buffer.writeBytes(new BigInteger(value).toByteArray());
         return true;
      } else if (type != 81 && type != 113) {
         return false;
      } else {
         buffer.writeLong(Long.decode(value));
         return true;
      }
   }

   public void addPart(byte b, String string) {
      this._parts.add(new AdminForgePacket.Part(b, string));
   }

   private static class Part {
      public byte b;
      public String str;

      public Part(byte bb, String string) {
         this.b = bb;
         this.str = string;
      }
   }
}
