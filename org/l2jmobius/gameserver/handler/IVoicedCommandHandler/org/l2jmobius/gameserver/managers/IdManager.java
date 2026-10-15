package org.l2jmobius.gameserver.managers;

import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.IdManagerConfig;
import org.l2jmobius.gameserver.util.PrimeCapacityAllocator;

public class IdManager {
   private static final Logger LOGGER = Logger.getLogger(IdManager.class.getName());
   private static final int TOTAL_ID_COUNT = IdManagerConfig.LAST_OBJECT_ID - IdManagerConfig.FIRST_OBJECT_ID;
   private BitSet _freeIds;
   private int _freeIdCount;
   private int _nextFreeId;
   private final Lock _lock = new ReentrantLock();

   public IdManager() {
      DatabaseIdManager.cleanDatabase();
      DatabaseIdManager.cleanCharacterStatus();
      DatabaseIdManager.cleanTimestamps();

      try {
         this._freeIds = new BitSet(PrimeCapacityAllocator.nextCapacity(IdManagerConfig.INITIAL_CAPACITY));
         this._freeIds.clear();
         this._freeIdCount = TOTAL_ID_COUNT;

         for (int usedObjectId : DatabaseIdManager.getUsedIds()) {
            int objectId = usedObjectId - IdManagerConfig.FIRST_OBJECT_ID;
            if (objectId >= 0) {
               this._freeIds.set(objectId);
               this._freeIdCount--;
            }
         }

         this._nextFreeId = this._freeIds.nextClearBit(0);
      } catch (Exception var4) {
         LOGGER.severe("IdManager: Could not be initialized properly: " + var4.getMessage());
      }

      LOGGER.info("IdManager: " + this._freeIds.size() + " ids available.");
   }

   private void increaseBitSetCapacity() {
      int currentSize = this._freeIds.size();
      int newSize = Math.min(PrimeCapacityAllocator.nextCapacity((int)(currentSize * IdManagerConfig.RESIZE_MULTIPLIER)), TOTAL_ID_COUNT);
      if (newSize > currentSize) {
         BitSet newBitSet = new BitSet(newSize);
         newBitSet.or(this._freeIds);
         this._freeIds = newBitSet;
         LOGGER.info("IdManager: Increased BitSet capacity to " + newSize);
      }
   }

   public int getNextId() {
      this._lock.lock();

      int var5;
      try {
         int newId = this._nextFreeId;
         this._freeIds.set(newId);
         this._freeIdCount--;
         double utilization = (double)(TOTAL_ID_COUNT - this._freeIdCount) / this._freeIds.size();
         if (utilization >= IdManagerConfig.RESIZE_THRESHOLD) {
            this.increaseBitSetCapacity();
            this._nextFreeId = this._freeIds.nextClearBit(0);
         }

         int nextFree = this._freeIds.nextClearBit(newId);
         if (nextFree < 0) {
            nextFree = this._freeIds.nextClearBit(0);
         }

         if (nextFree < 0) {
            if (this._freeIds.size() >= TOTAL_ID_COUNT) {
               throw new NullPointerException("IdManager: Ran out of valid ids.");
            }

            this.increaseBitSetCapacity();
            this._nextFreeId = this._freeIds.nextClearBit(0);
         }

         this._nextFreeId = nextFree;
         var5 = newId + IdManagerConfig.FIRST_OBJECT_ID;
      } finally {
         this._lock.unlock();
      }

      return var5;
   }

   public void releaseId(int objectId) {
      this._lock.lock();

      try {
         if (objectId - IdManagerConfig.FIRST_OBJECT_ID > -1) {
            this._freeIds.clear(objectId - IdManagerConfig.FIRST_OBJECT_ID);
            this._freeIdCount++;
         } else {
            LOGGER.warning("IdManager: Release objectID " + objectId + " failed (< " + IdManagerConfig.FIRST_OBJECT_ID + ")");
         }
      } finally {
         this._lock.unlock();
      }
   }

   public int getAvailableIdCount() {
      this._lock.lock();

      int var1;
      try {
         var1 = this._freeIdCount;
      } finally {
         this._lock.unlock();
      }

      return var1;
   }

   public static IdManager getInstance() {
      return IdManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final IdManager INSTANCE = new IdManager();
   }
}
