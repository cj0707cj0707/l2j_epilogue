package org.l2jmobius.commons.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.l2jmobius.commons.network.handler.ReadHandler;
import org.l2jmobius.commons.network.handler.WriteHandler;
import org.l2jmobius.commons.network.pool.ResourcePool;

public class Connection<T extends org.l2jmobius.commons.network.Client<org.l2jmobius.commons.network.Connection<T>>> {
   private final AsynchronousSocketChannel _channel;
   private final ReadHandler<T> _readHandler;
   private final WriteHandler<T> _writeHandler;
   private final org.l2jmobius.commons.network.ConnectionConfig _config;
   private volatile T _client;
   private final AtomicBoolean _closed = new AtomicBoolean();
   private ByteBuffer _readingBuffer;
   private ByteBuffer[] _writingBuffers;

   public Connection(
      AsynchronousSocketChannel channel, ReadHandler<T> readHandler, WriteHandler<T> writeHandler, org.l2jmobius.commons.network.ConnectionConfig config
   ) {
      this._channel = channel;
      this._readHandler = readHandler;
      this._writeHandler = writeHandler;
      this._config = config;
   }

   public void setClient(T client) {
      this._client = client;
   }

   public void read() {
      if (this._channel.isOpen()) {
         this._channel.read(this._readingBuffer, this._client, this._readHandler);
      }
   }

   public void readHeader() {
      if (this._channel.isOpen()) {
         this.recycleReadBuffer();
         this._readingBuffer = this._config.resourcePool.getHeaderBuffer();
         this.read();
      }
   }

   public void read(int size) {
      if (this._channel.isOpen()) {
         this._readingBuffer = this._config.resourcePool.recycleAndGetNew(this._readingBuffer, size);
         this.read();
      }
   }

   public ByteBuffer getReadingBuffer() {
      return this._readingBuffer;
   }

   public boolean write(ByteBuffer[] buffers) {
      if (!this._channel.isOpen()) {
         return false;
      } else {
         this._writingBuffers = buffers;
         this.write();
         return true;
      }
   }

   public void write() {
      if (this._channel.isOpen() && this._writingBuffers != null) {
         this._channel.write(this._writingBuffers, 0, this._writingBuffers.length, -1L, TimeUnit.MILLISECONDS, this._client, this._writeHandler);
      } else if (this._client != null) {
         this._client.finishWriting();
      }
   }

   public boolean releaseWritingBuffer() {
      ByteBuffer[] buffers = this._writingBuffers;
      if (buffers == null) {
         return false;
      } else {
         this._writingBuffers = null;

         for (ByteBuffer buf : buffers) {
            this._config.resourcePool.recycleBuffer(buf);
         }

         return true;
      }
   }

   public void close() {
      if (this._closed.compareAndSet(false, true)) {
         this.recycleReadBuffer();
         this.releaseWritingBuffer();

         try {
            if (this._channel.isOpen()) {
               this._channel.close();
            }
         } catch (IOException var5) {
         } finally {
            this._client = null;
         }
      }
   }

   public boolean isOpen() {
      return this._channel.isOpen();
   }

   public String getRemoteAddress() {
      try {
         InetSocketAddress remote = (InetSocketAddress)this._channel.getRemoteAddress();
         return remote.getAddress().getHostAddress();
      } catch (IOException var2) {
         return "";
      }
   }

   public ResourcePool getResourcePool() {
      return this._config.resourcePool;
   }

   public boolean dropPackets() {
      return this._config.dropPackets;
   }

   public int dropPacketThreshold() {
      return this._config.dropPacketThreshold;
   }

   private void recycleReadBuffer() {
      ByteBuffer buf = this._readingBuffer;
      if (buf != null) {
         this._readingBuffer = null;
         this._config.resourcePool.recycleBuffer(buf);
      }
   }
}
