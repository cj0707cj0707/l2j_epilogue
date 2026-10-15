package org.l2jmobius.gameserver.entity.item.holders;

import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;

public class DropProtection implements Runnable {
   private volatile boolean _isProtected = false;
   private Creature _owner = null;
   private ScheduledFuture<?> _task = null;
   private static final long PROTECTED_MILLIS_TIME = 15000L;

   @Override
   public synchronized void run() {
      this._isProtected = false;
      this._owner = null;
      this._task = null;
   }

   public boolean isProtected() {
      return this._isProtected;
   }

   public Creature getOwner() {
      return this._owner;
   }

   public synchronized boolean tryPickUp(Player actor) {
      return !this._isProtected || this._owner == actor || this._owner.getParty() != null && this._owner.getParty() == actor.getParty();
   }

   public boolean tryPickUp(Pet pet) {
      return this.tryPickUp(pet.getOwner());
   }

   public synchronized void unprotect() {
      if (this._task != null) {
         this._task.cancel(false);
      }

      this._isProtected = false;
      this._owner = null;
      this._task = null;
   }

   public synchronized void protect(Creature creature) {
      this.unprotect();
      this._isProtected = true;
      this._owner = creature;
      if (this._owner == null) {
         throw new NullPointerException("Trying to protect dropped item to null owner");
      } else {
         this._task = ThreadPool.schedule(this, 15000L);
      }
   }
}
