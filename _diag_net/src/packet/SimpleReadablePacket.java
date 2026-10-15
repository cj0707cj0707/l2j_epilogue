package org.l2jmobius.commons.network.packet;

import java.nio.charset.StandardCharsets;

public class SimpleReadablePacket {
   private final byte[] _bytes;
   private int _position = 0;

   public SimpleReadablePacket(byte[] bytes) {
      this._bytes = bytes;
   }

   public boolean readBoolean() {
      return this.readByte() != 0;
   }

   public String readString() {
      StringBuilder result = new StringBuilder();

      int charId;
      try {
         while ((charId = this.readShort()) != 0) {
            result.append((char)charId);
         }
      } catch (Exception var3) {
      }

      return result.toString();
   }

   public String readSizedString() {
      String result = "";

      try {
         result = new String(this.readBytes(this.readShort() * 2), StandardCharsets.UTF_16LE);
      } catch (Exception var3) {
      }

      return result;
   }

   public byte[] readBytes(int length) {
      if (length >= 0 && length <= this._bytes.length - this._position) {
         byte[] result = new byte[length];

         for (int i = 0; i < length; i++) {
            result[i] = this._bytes[this._position++];
         }

         return result;
      } else {
         throw new ArrayIndexOutOfBoundsException();
      }
   }

   public byte[] readBytes(byte[] array) {
      for (int i = 0; i < array.length; i++) {
         array[i] = this._bytes[this._position++];
      }

      return array;
   }

   public int readByte() {
      return this._bytes[this._position++] & 0xFF;
   }

   public int readShort() {
      return this._bytes[this._position++] & 0xFF | (this._bytes[this._position++] & 0xFF) << 8;
   }

   public int readInt() {
      return this._bytes[this._position++] & 0xFF
         | (this._bytes[this._position++] & 0xFF) << 8
         | (this._bytes[this._position++] & 0xFF) << 16
         | (this._bytes[this._position++] & 0xFF) << 24;
   }

   public long readLong() {
      return this._bytes[this._position++] & 255
         | (this._bytes[this._position++] & 255L) << 8
         | (this._bytes[this._position++] & 255L) << 16
         | (this._bytes[this._position++] & 255L) << 24
         | (this._bytes[this._position++] & 255L) << 32
         | (this._bytes[this._position++] & 255L) << 40
         | (this._bytes[this._position++] & 255L) << 48
         | (this._bytes[this._position++] & 255L) << 56;
   }

   public float readFloat() {
      return Float.intBitsToFloat(this.readInt());
   }

   public double readDouble() {
      return Double.longBitsToDouble(this.readLong());
   }

   public int getRemainingLength() {
      return this._bytes.length - this._position;
   }

   public int getLength() {
      return this._bytes.length;
   }
}
