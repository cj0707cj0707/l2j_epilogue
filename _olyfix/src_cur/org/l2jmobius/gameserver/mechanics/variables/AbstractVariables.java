package org.l2jmobius.gameserver.mechanics.variables;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.l2jmobius.gameserver.util.StatSet;

public abstract class AbstractVariables extends StatSet {
   private final AtomicBoolean _hasChanges = new AtomicBoolean(false);
   protected final Set<String> _added = new HashSet<>(4);
   protected final Set<String> _modified = new HashSet<>(4);
   protected final Set<String> _deleted = new HashSet<>(4);
   protected final Lock _saveLock = new ReentrantLock();

   protected AbstractVariables() {
      super(ConcurrentHashMap::new);
   }

   @Override
   public void set(String name, boolean value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, byte value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, short value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, int value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, long value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, float value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, double value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, String value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, Enum<?> value) {
      this.trackChange(name);
      super.set(name, value);
   }

   @Override
   public void set(String name, Object value) {
      this.trackChange(name);
      super.set(name, value);
   }

   protected void trackChange(String name) {
      this._saveLock.lock();

      try {
         this._hasChanges.compareAndSet(false, true);
         if (this.hasVariable(name)) {
            this._modified.add(name);
            this._deleted.remove(name);
         } else {
            this._added.add(name);
         }
      } finally {
         this._saveLock.unlock();
      }
   }

   public void set(String name, String value, boolean markAsChanged) {
      if (markAsChanged) {
         this.trackChange(name);
      }

      super.set(name, value);
   }

   public boolean hasVariable(String name) {
      return this.getSet().containsKey(name);
   }

   public boolean hasChanges() {
      return this._hasChanges.get();
   }

   public boolean compareAndSetChanges(boolean expect, boolean update) {
      return this._hasChanges.compareAndSet(expect, update);
   }

   @Override
   public void remove(String name) {
      this._saveLock.lock();

      try {
         this._hasChanges.compareAndSet(false, true);
         Object removed = this.getSet().remove(name);
         if (removed == null) {
            return;
         }

         if (!this._added.remove(name)) {
            this._modified.remove(name);
            this._deleted.add(name);
            return;
         }
      } finally {
         this._saveLock.unlock();
      }
   }

   protected void clearChangeTracking() {
      this._added.clear();
      this._modified.clear();
      this._deleted.clear();
   }
}
