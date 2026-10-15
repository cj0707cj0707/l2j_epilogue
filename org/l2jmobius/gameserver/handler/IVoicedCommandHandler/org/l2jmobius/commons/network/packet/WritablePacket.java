package org.l2jmobius.commons.network.packet;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public abstract class WritablePacket<T extends Client<Connection<T>>> {
   private volatile boolean _broadcast;
   private byte[] _broadcastCacheBytes;
   private int _broadcastCacheLength;

   protected WritablePacket() {
   }

   public WriteBuffer writeData(T client) throws Exception {
      return this._broadcast ? this.writeDataWithCache(client) : this.writeDataToBuffer(client);
   }

   private synchronized WriteBuffer writeDataWithCache(T client) throws Exception {
      if (this._broadcastCacheBytes != null) {
         return new WriteBuffer(this._broadcastCacheBytes, this._broadcastCacheLength, client.getResourcePool(), this.getClass());
      } else {
         WriteBuffer buffer = this.writeDataToBuffer(client);
         this._broadcastCacheBytes = buffer.toByteArray();
         this._broadcastCacheLength = buffer.limit();
         return buffer;
      }
   }

   private WriteBuffer writeDataToBuffer(T client) throws Exception {
      WriteBuffer buffer = new WriteBuffer(client.getResourcePool(), this.getClass());
      buffer.position(2);
      if (this.write(client, buffer)) {
         buffer.mark();
         return buffer;
      } else {
         buffer.releaseResources();
         throw new Exception("WritablePacket.write() returned false for " + this.getClass().getSimpleName());
      }
   }

   public void writeHeader(WriteBuffer buffer, int header) {
      buffer.writeShort(0, (short)header);
   }

   public void sendInBroadcast() {
      this._broadcast = true;
   }

   public boolean canBeDropped(T client) {
      return false;
   }

   protected abstract boolean write(T var1, WriteBuffer var2);

   @Override
   public String toString() {
      return this.getClass().getSimpleName();
   }
}
