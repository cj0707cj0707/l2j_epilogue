package org.l2jmobius.gameserver.network;

import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class Encryption {
   private static final int KEY_LENGTH = 16;
   private static final int BYTE_MASK = 255;
   private static final int NIBBLE_MASK = 15;
   private static final int SHIFT_8 = 8;
   private static final int SHIFT_16 = 16;
   private static final int SHIFT_24 = 24;
   private static final int OFFSET_INDEX = 8;
   private final byte[] _inKey = new byte[16];
   private final byte[] _outKey = new byte[16];
   private final int[] _inKeyMasked = new int[16];
   private final int[] _outKeyMasked = new int[16];
   private boolean _isEnabled;

   public void setKey(byte[] key) {
      if (key != null && key.length >= 16) {
         System.arraycopy(key, 0, this._inKey, 0, 16);
         System.arraycopy(key, 0, this._outKey, 0, 16);

         for (int i = 0; i < 16; i++) {
            int v = key[i] & 255;
            this._inKeyMasked[i] = v;
            this._outKeyMasked[i] = v;
         }
      } else {
         throw new IllegalArgumentException("Encryption key must be at least 16 bytes.");
      }
   }

   public void encrypt(WriteBuffer data, int offset, int size) {
      if (!this._isEnabled) {
         this._isEnabled = true;
      } else if (size > 0) {
         int[] key = this._outKeyMasked;
         int prev = 0;

         for (int i = 0; i < size; i++) {
            int raw = data.readByte(offset + i) & 255;
            prev ^= raw ^ key[i & 15];
            data.writeByte(offset + i, (byte)prev);
         }

         advanceOffset(this._outKey, this._outKeyMasked, size);
      }
   }

   public void decrypt(ReadBuffer data, int offset, int size) {
      if (this._isEnabled) {
         if (size > 0) {
            int[] key = this._inKeyMasked;
            int last = 0;

            for (int i = 0; i < size; i++) {
               int enc = data.readByte(offset + i) & 255;
               data.writeByte(offset + i, (byte)(enc ^ key[i & 15] ^ last));
               last = enc;
            }

            advanceOffset(this._inKey, this._inKeyMasked, size);
         }
      }
   }

   private static void advanceOffset(byte[] key, int[] keyMasked, int size) {
      int old = key[8] & 255;
      old |= (key[9] & 255) << 8;
      old |= (key[10] & 255) << 16;
      old |= (key[11] & 255) << 24;
      old += size;
      byte b0 = (byte)(old & 0xFF);
      byte b1 = (byte)(old >> 8 & 0xFF);
      byte b2 = (byte)(old >> 16 & 0xFF);
      byte b3 = (byte)(old >> 24 & 0xFF);
      key[8] = b0;
      key[9] = b1;
      key[10] = b2;
      key[11] = b3;
      keyMasked[8] = b0 & 255;
      keyMasked[9] = b1 & 255;
      keyMasked[10] = b2 & 255;
      keyMasked[11] = b3 & 255;
   }
}
