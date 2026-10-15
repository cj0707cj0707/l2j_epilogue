package org.l2jmobius.commons.network.packet;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SimpleWritablePacket {
   private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
   private final int _initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(this.getClass(), 8);
   private byte[] _data;
   private byte[] _sendableBytes;
   private int _position = 2;

   protected SimpleWritablePacket() {
      this._data = new byte[this._initialSize];
   }

   public void write(byte value) {
      if (this._position < 65535) {
         if (this._position == this._data.length) {
            this._data = Arrays.copyOf(this._data, this._data.length * 2);
         }

         this._data[this._position++] = value;
      } else {
         throw new IndexOutOfBoundsException("Packet data exceeded the raw data size limit of 65533!");
      }
   }

   public void writeBoolean(boolean value) {
      this.writeByte(value ? 1 : 0);
   }

   public void writeString(String text) {
      if (text != null) {
         this.writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
      }

      this.writeShort(0);
   }

   public void writeSizedString(String text) {
      if (text != null) {
         this.writeShort(text.length());
         this.writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
      } else {
         this.writeShort(0);
      }
   }

   public void writeBytes(byte[] array) {
      for (int i = 0; i < array.length; i++) {
         this.write(array[i]);
      }
   }

   public void writeByte(int value) {
      this.write((byte)(value & 0xFF));
   }

   public void writeByte(boolean value) {
      this.writeByte(value ? 1 : 0);
   }

   public void writeShort(int value) {
      this.write((byte)(value & 0xFF));
      this.write((byte)(value >> 8 & 0xFF));
   }

   public void writeShort(boolean value) {
      this.writeShort(value ? 1 : 0);
   }

   public void writeInt(int value) {
      this.write((byte)(value & 0xFF));
      this.write((byte)(value >> 8 & 0xFF));
      this.write((byte)(value >> 16 & 0xFF));
      this.write((byte)(value >> 24 & 0xFF));
   }

   public void writeInt(boolean value) {
      this.writeInt(value ? 1 : 0);
   }

   public void writeLong(long value) {
      this.write((byte)(value & 255L));
      this.write((byte)(value >> 8 & 255L));
      this.write((byte)(value >> 16 & 255L));
      this.write((byte)(value >> 24 & 255L));
      this.write((byte)(value >> 32 & 255L));
      this.write((byte)(value >> 40 & 255L));
      this.write((byte)(value >> 48 & 255L));
      this.write((byte)(value >> 56 & 255L));
   }

   public void writeLong(boolean value) {
      this.writeLong(value ? 1L : 0L);
   }

   public void writeFloat(float value) {
      this.writeInt(Float.floatToRawIntBits(value));
   }

   public void writeDouble(double value) {
      this.writeLong(Double.doubleToRawLongBits(value));
   }

   public void write() {
   }

   public synchronized byte[] getSendableBytes() {
      if (this._sendableBytes == null) {
         if (this._position == 2) {
            this.write();
            if (this._position > this._initialSize) {
               MAXIMUM_PACKET_SIZE.put(this.getClass(), Math.min(this._position, 65535));
            }
         }

         if (this._position > 2) {
            this._sendableBytes = Arrays.copyOf(this._data, this._position);
            this._sendableBytes[0] = (byte)(this._position & 0xFF);
            this._sendableBytes[1] = (byte)(this._position >> 8 & 65535);
         }
      }

      return this._sendableBytes;
   }

   public int getLength() {
      return this._position;
   }
}
