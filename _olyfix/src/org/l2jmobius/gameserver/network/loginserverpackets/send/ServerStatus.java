package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class ServerStatus extends SimpleWritablePacket {
   private static final int PACKET_ID = 6;
   public static final int ON = 1;
   public static final int OFF = 0;
   public static final int SERVER_AGE_ALL = 0;
   public static final int SERVER_AGE_15 = 15;
   public static final int SERVER_AGE_18 = 18;
   public static final int SERVER_NORMAL = 1;
   public static final int SERVER_RELAX = 2;
   public static final int SERVER_TEST = 4;
   public static final int SERVER_NOLABEL = 8;
   public static final int SERVER_CREATION_RESTRICTED = 16;
   public static final int SERVER_EVENT = 32;
   public static final int SERVER_FREE = 64;
   public static final int STATUS_AUTO = 0;
   public static final int STATUS_GOOD = 1;
   public static final int STATUS_NORMAL = 2;
   public static final int STATUS_FULL = 3;
   public static final int STATUS_DOWN = 4;
   public static final int STATUS_GM_ONLY = 5;
   public static final int SERVER_LIST_STATUS = 1;
   public static final int SERVER_TYPE = 2;
   public static final int SERVER_LIST_SQUARE_BRACKET = 3;
   public static final int MAX_PLAYERS = 4;
   public static final int SERVER_AGE = 5;
   public static final String[] STATUS_STRING = new String[]{"Auto", "Good", "Normal", "Full", "Down", "Gm Only"};
   private static final int INITIAL_PAIRS = 16;
   private int[] _packedPairs = new int[32];
   private int _pairCount = 0;

   public void addAttribute(int id, int value) {
      int nextSize = (this._pairCount + 1) * 2;
      if (nextSize > this._packedPairs.length) {
         int newLen = this._packedPairs.length << 1;

         while (newLen < nextSize) {
            newLen <<= 1;
         }

         int[] grown = new int[newLen];
         System.arraycopy(this._packedPairs, 0, grown, 0, this._packedPairs.length);
         this._packedPairs = grown;
      }

      int base = this._pairCount * 2;
      this._packedPairs[base] = id;
      this._packedPairs[base + 1] = value;
      this._pairCount++;
   }

   @Override
   public void write() {
      this.writeByte(6);
      this.writeInt(this._pairCount);

      for (int i = 0; i < this._pairCount; i++) {
         int base = i * 2;
         this.writeInt(this._packedPairs[base]);
         this.writeInt(this._packedPairs[base + 1]);
      }
   }
}
