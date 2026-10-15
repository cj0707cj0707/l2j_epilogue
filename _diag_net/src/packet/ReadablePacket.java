package org.l2jmobius.commons.network.packet;

import java.nio.charset.StandardCharsets;
import org.l2jmobius.commons.network.buffer.ReadBuffer;

public abstract class ReadablePacket<T extends org.l2jmobius.commons.network.Client<org.l2jmobius.commons.network.Connection<T>>> implements Runnable {
   private ReadBuffer _buffer;
   private T _client;

   protected ReadablePacket() {
   }

   public void init(T client, ReadBuffer buffer) {
      this._client = client;
      this._buffer = buffer;
   }

   protected char readChar() {
      return this._buffer.readChar();
   }

   protected byte readByte() {
      return this._buffer.readByte();
   }

   protected int readUnsignedByte() {
      return Byte.toUnsignedInt(this._buffer.readByte());
   }

   protected boolean readBoolean() {
      return this._buffer.readByte() != 0;
   }

   protected byte[] readBytes(int length) {
      return this._buffer.readBytes(length);
   }

   protected void readBytes(byte[] dst) {
      this._buffer.readBytes(dst, 0, dst.length);
   }

   protected void readBytes(byte[] dst, int offset, int length) {
      this._buffer.readBytes(dst, offset, length);
   }

   protected short readShort() {
      return this._buffer.readShort();
   }

   protected int readInt() {
      return this._buffer.readInt();
   }

   protected long readLong() {
      return this._buffer.readLong();
   }

   protected float readFloat() {
      return this._buffer.readFloat();
   }

   protected double readDouble() {
      return this._buffer.readDouble();
   }

   protected String readString() {
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

   protected String readSizedString() {
      try {
         int charCount = this.readShort();
         if (charCount > 0) {
            return new String(this.readBytes(charCount * 2), StandardCharsets.UTF_16LE);
         }
      } catch (Exception var2) {
      }

      return "";
   }

   protected int remaining() {
      return this._buffer.remaining();
   }

   public T getClient() {
      return this._client;
   }

   public abstract boolean read();
}
