package org.l2jmobius.commons.network.pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

public class BufferPool {
   private final ArrayDeque<ByteBuffer> _buffers;
   private final ReentrantLock _lock = new ReentrantLock();
   private final int _bufferSize;
   private int _maxSize;

   public BufferPool(int maxSize, int bufferSize) {
      this._maxSize = maxSize;
      this._bufferSize = bufferSize;
      this._buffers = new ArrayDeque<>(maxSize);
   }

   public void initialize(float factor) {
      int amount = (int)Math.min((float)this._maxSize, this._maxSize * factor);
      this._lock.lock();

      try {
         for (int i = 0; i < amount; i++) {
            this._buffers.offer(ByteBuffer.allocateDirect(this._bufferSize).order(ByteOrder.LITTLE_ENDIAN));
         }
      } finally {
         this._lock.unlock();
      }
   }

   public ByteBuffer get() {
      this._lock.lock();

      ByteBuffer var1;
      try {
         var1 = this._buffers.poll();
      } finally {
         this._lock.unlock();
      }

      return var1;
   }

   public boolean recycle(ByteBuffer buffer) {
      this._lock.lock();

      boolean var2;
      try {
         if (this._buffers.size() >= this._maxSize) {
            return false;
         }

         this._buffers.offer(buffer.clear());
         var2 = true;
      } finally {
         this._lock.unlock();
      }

      return var2;
   }

   public void expandCapacity(float factor, int limit) {
      this._lock.lock();

      try {
         if (this._maxSize > limit) {
            return;
         }

         if (factor > 0.0F) {
            int amount = (int)(this._maxSize * factor);

            for (int i = 0; i < amount; i++) {
               this._buffers.offer(ByteBuffer.allocateDirect(this._bufferSize).order(ByteOrder.LITTLE_ENDIAN));
            }

            this._maxSize += amount;
         } else {
            this._maxSize *= 2;
         }
      } finally {
         this._lock.unlock();
      }
   }

   public int getMaxSize() {
      this._lock.lock();

      int var1;
      try {
         var1 = this._maxSize;
      } finally {
         this._lock.unlock();
      }

      return var1;
   }

   public boolean isFull() {
      this._lock.lock();

      boolean var1;
      try {
         var1 = this._buffers.size() >= this._maxSize;
      } finally {
         this._lock.unlock();
      }

      return var1;
   }

   public boolean isEmpty() {
      this._lock.lock();

      boolean var1;
      try {
         var1 = this._buffers.isEmpty();
      } finally {
         this._lock.unlock();
      }

      return var1;
   }

   @Override
   public String toString() {
      this._lock.lock();

      String var1;
      try {
         var1 = "Pool {maxSize=" + this._maxSize + ", bufferSize=" + this._bufferSize + ", currentSize=" + this._buffers.size() + "}";
      } finally {
         this._lock.unlock();
      }

      return var1;
   }
}
