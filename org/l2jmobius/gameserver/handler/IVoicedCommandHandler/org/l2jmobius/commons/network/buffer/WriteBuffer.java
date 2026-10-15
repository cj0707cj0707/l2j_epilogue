package org.l2jmobius.commons.network.buffer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.network.pool.ResourcePool;

public class WriteBuffer {
   private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
   private final ResourcePool _resourcePool;
   private final Class<?> _packetClass;
   private final int _initialSize;
   private ByteBuffer _buf;
   private int _position;
   private int _limit;

   public WriteBuffer(ResourcePool resourcePool, Class<?> packetClass) {
      this._resourcePool = resourcePool;
      this._packetClass = packetClass;
      this._initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(packetClass, resourcePool.getSegmentSize());
      this._buf = resourcePool.getBuffer(this._initialSize);
      this._limit = this._buf.capacity();
   }

   public WriteBuffer(byte[] cached, int cachedLength, ResourcePool resourcePool, Class<?> packetClass) {
      this._resourcePool = resourcePool;
      this._packetClass = packetClass;
      this._initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(packetClass, Math.max(cachedLength, resourcePool.getSegmentSize()));
      this._buf = resourcePool.getBuffer(Math.max(this._initialSize, cachedLength));
      this._buf.position(0);
      this._buf.put(cached, 0, cachedLength);
      this._buf.position(0);
      this._position = cachedLength;
      this._limit = cachedLength;
   }

   private void ensureSize(int required) {
      int capacity = this._buf.capacity();
      if (capacity < required) {
         int newSize = Math.max(required, (int)(capacity * 1.5));
         ByteBuffer newBuf = this._resourcePool.getBuffer(newSize);
         newBuf.position(0);
         this._buf.position(0).limit(Math.min(this._position, capacity));
         newBuf.put(this._buf);
         newBuf.position(0);
         this._resourcePool.recycleBuffer(this._buf);
         this._buf = newBuf;
      }
   }

   public void writeByte(byte value) {
      this.ensureSize(this._position + 1);
      this._buf.put(this._position++, value);
   }

   public void writeByte(int value) {
      this.writeByte((byte)value);
   }

   public void writeByte(boolean value) {
      this.writeByte((byte)(value ? 1 : 0));
   }

   public void writeBytes(byte... value) {
      if (value != null && value.length != 0) {
         this.ensureSize(this._position + value.length);
         this._buf.put(this._position, value, 0, value.length);
         this._position += value.length;
      }
   }

   public void writeShort(short value) {
      this.ensureSize(this._position + 2);
      this._buf.putShort(this._position, value);
      this._position += 2;
   }

   public void writeShort(int value) {
      this.writeShort((short)value);
   }

   public void writeShort(boolean value) {
      this.writeShort((short)(value ? 1 : 0));
   }

   public void writeInt(int value) {
      this.ensureSize(this._position + 4);
      this._buf.putInt(this._position, value);
      this._position += 4;
   }

   public void writeInt(boolean value) {
      this.writeInt(value ? 1 : 0);
   }

   public void writeLong(long value) {
      this.ensureSize(this._position + 8);
      this._buf.putLong(this._position, value);
      this._position += 8;
   }

   public void writeFloat(float value) {
      this.writeInt(Float.floatToRawIntBits(value));
   }

   public void writeDouble(double value) {
      this.writeLong(Double.doubleToRawLongBits(value));
   }

   public void writeChar(char value) {
      this.writeShort((short)value);
   }

   public void writeString(CharSequence text) {
      if (text == null) {
         this.writeChar('\u0000');
      } else {
         int len = text.length();
         this.ensureSize(this._position + (len << 1) + 2);

         for (int i = 0; i < len; i++) {
            this._buf.putShort(this._position, (short)text.charAt(i));
            this._position += 2;
         }

         this._buf.putShort(this._position, (short)0);
         this._position += 2;
      }
   }

   public void writeSizedString(CharSequence text) {
      if (text != null && !text.isEmpty()) {
         int len = text.length();
         this.ensureSize(this._position + 2 + (len << 1));
         this._buf.putShort(this._position, (short)len);
         this._position += 2;

         for (int i = 0; i < len; i++) {
            this._buf.putShort(this._position, (short)text.charAt(i));
            this._position += 2;
         }
      } else {
         this.writeShort(0);
      }
   }

   public void writeByte(int index, byte value) {
      this.ensureSize(index + 1);
      this._buf.put(index, value);
   }

   public void writeShort(int index, short value) {
      this.ensureSize(index + 2);
      this._buf.putShort(index, value);
   }

   public void writeInt(int index, int value) {
      this.ensureSize(index + 4);
      this._buf.putInt(index, value);
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

   public int position() {
      return this._position;
   }

   public void position(int pos) {
      this._position = pos;
   }

   public int limit() {
      return this._limit;
   }

   public void limit(int newLimit) {
      this.ensureSize(newLimit);
      this._limit = newLimit;
   }

   public void mark() {
      this._limit = this._position;
   }

   public ByteBuffer[] toByteBuffers() {
      if (this._limit > this._initialSize) {
         MAXIMUM_PACKET_SIZE.put(this._packetClass, Math.min(this._limit, 65535));
      }

      this._buf.position(0);
      this._buf.limit(this._limit);
      return new ByteBuffer[]{this._buf};
   }

   public byte[] toByteArray() {
      byte[] snapshot = new byte[this._limit];
      this._buf.get(0, snapshot, 0, this._limit);
      return snapshot;
   }

   public void releaseResources() {
      if (this._buf != null) {
         this._resourcePool.recycleBuffer(this._buf);
         this._buf = null;
      }

      this._position = 0;
      this._limit = 0;
   }
}
