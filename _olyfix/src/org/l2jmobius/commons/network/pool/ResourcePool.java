package org.l2jmobius.commons.network.pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ResourcePool {
   private final ConcurrentSkipListMap<Integer, BufferPool> _sizedPools = new ConcurrentSkipListMap<>();
   private final ConcurrentHashMap<Integer, BufferPool> _exactPools = new ConcurrentHashMap<>();
   private volatile boolean _autoExpandCapacity = true;
   private volatile boolean _initBufferPools = false;
   private volatile float _initBufferPoolFactor = 0.0F;
   private volatile int _bufferSegmentSize = 64;

   public ByteBuffer getHeaderBuffer() {
      return this.getSizedBuffer(2);
   }

   public ByteBuffer getBuffer(int size) {
      return this.getSizedBuffer(this.determineBufferSize(size));
   }

   public ByteBuffer recycleAndGetNew(ByteBuffer buffer, int newSize) {
      int poolSize = this.determineBufferSize(newSize);
      if (buffer != null) {
         if (buffer.clear().limit() == poolSize) {
            return buffer.limit(newSize);
         }

         this.recycleBuffer(buffer);
      }

      return this.getSizedBuffer(poolSize).limit(newSize);
   }

   private ByteBuffer getSizedBuffer(int size) {
      Entry<Integer, BufferPool> entry = this._sizedPools.ceilingEntry(size);
      if (entry != null) {
         BufferPool pool = entry.getValue();
         if (this._autoExpandCapacity) {
            if (this._initBufferPools) {
               if (pool.isEmpty()) {
                  pool.expandCapacity(this._initBufferPoolFactor, pool.getMaxSize());
               }
            } else if (pool.isFull()) {
               pool.expandCapacity(this._initBufferPoolFactor, pool.getMaxSize());
            }
         }

         ByteBuffer buffer = pool.get();
         if (buffer != null) {
            return buffer;
         }
      }

      if (entry == null) {
         BufferPool poolx = new BufferPool(10, size);
         if (this._initBufferPools) {
            poolx.initialize(this._initBufferPoolFactor);
         }

         this._sizedPools.putIfAbsent(size, poolx);
         this._exactPools.putIfAbsent(size, this._sizedPools.get(size));
      }

      return ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
   }

   private int determineBufferSize(int size) {
      Entry<Integer, BufferPool> entry = this._sizedPools.ceilingEntry(size);
      if (entry != null) {
         return entry.getKey();
      } else {
         BufferPool pool = new BufferPool(10, size);
         if (this._initBufferPools) {
            pool.initialize(this._initBufferPoolFactor);
         }

         this._sizedPools.putIfAbsent(size, pool);
         this._exactPools.putIfAbsent(size, this._sizedPools.get(size));
         return size;
      }
   }

   public void recycleBuffer(ByteBuffer buffer) {
      if (buffer != null) {
         BufferPool pool = this._exactPools.get(buffer.capacity());
         if (pool != null) {
            pool.recycle(buffer);
         }
      }
   }

   public void addBufferPool(int bufferSize, BufferPool bufferPool) {
      this._sizedPools.putIfAbsent(bufferSize, bufferPool);
      this._exactPools.putIfAbsent(bufferSize, bufferPool);
   }

   public int bufferPoolSize() {
      return this._sizedPools.size();
   }

   public void initializeBuffers(boolean autoExpandCapacity, float initBufferPoolFactor) {
      this._autoExpandCapacity = autoExpandCapacity;
      this._initBufferPoolFactor = initBufferPoolFactor;
      this._initBufferPools = initBufferPoolFactor > 0.0F;
      if (this._initBufferPools) {
         this._sizedPools.values().forEach(pool -> pool.initialize(initBufferPoolFactor));
      }
   }

   public int getSegmentSize() {
      return this._bufferSegmentSize;
   }

   public void setBufferSegmentSize(int size) {
      this._bufferSegmentSize = size;
   }

   public String stats() {
      StringBuilder sb = new StringBuilder();

      for (BufferPool pool : this._sizedPools.values()) {
         sb.append(pool.toString()).append(System.lineSeparator());
      }

      return sb.toString();
   }
}
