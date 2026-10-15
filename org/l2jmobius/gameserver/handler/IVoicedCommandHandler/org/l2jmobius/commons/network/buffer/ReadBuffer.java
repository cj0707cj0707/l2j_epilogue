package org.l2jmobius.commons.network.buffer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ReadBuffer {
   private final ByteBuffer _buf;

   public ReadBuffer(ByteBuffer buffer) {
      this._buf = Objects.requireNonNull(buffer);
   }

   public static ReadBuffer of(ByteBuffer buffer) {
      return new ReadBuffer(buffer);
   }

   public char readChar() {
      return this._buf.getChar();
   }

   public byte readByte() {
      return this._buf.get();
   }

   public short readShort() {
      return this._buf.getShort();
   }

   public int readInt() {
      return this._buf.getInt();
   }

   public long readLong() {
      return this._buf.getLong();
   }

   public float readFloat() {
      return this._buf.getFloat();
   }

   public double readDouble() {
      return this._buf.getDouble();
   }

   public byte[] readBytes(int length) {
      if (length >= 0 && length <= this._buf.remaining()) {
         byte[] dst = new byte[length];
         this._buf.get(dst);
         return dst;
      } else {
         throw new BufferUnderflowException();
      }
   }

   public void readBytes(byte[] dst) {
      this._buf.get(dst);
   }

   public void readBytes(byte[] dst, int offset, int length) {
      this._buf.get(dst, offset, length);
   }

   public int remaining() {
      return this._buf.remaining();
   }

   public byte readByte(int index) {
      return this._buf.get(index);
   }

   public short readShort(int index) {
      return this._buf.getShort(index);
   }

   public int readInt(int index) {
      return this._buf.getInt(index);
   }

   public void writeByte(int index, byte value) {
      this._buf.put(index, value);
   }

   public void writeShort(int index, short value) {
      this._buf.putShort(index, value);
   }

   public void writeInt(int index, int value) {
      this._buf.putInt(index, value);
   }

   public int limit() {
      return this._buf.limit();
   }

   public void limit(int newLimit) {
      this._buf.limit(newLimit);
   }
}
