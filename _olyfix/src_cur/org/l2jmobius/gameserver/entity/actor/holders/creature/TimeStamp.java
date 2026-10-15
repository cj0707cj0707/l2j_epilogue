package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class TimeStamp {
   private final int _id1;
   private final int _id2;
   private final long _reuse;
   private volatile long _stamp;
   private final int _group;

   public TimeStamp(Skill skill, long reuse, long systime) {
      this._id1 = skill.getId();
      this._id2 = skill.getLevel();
      this._reuse = reuse;
      this._stamp = systime > 0L ? systime : (reuse != 0L ? System.currentTimeMillis() + reuse : 0L);
      this._group = -1;
   }

   public TimeStamp(Item item, long reuse, long systime) {
      this._id1 = item.getId();
      this._id2 = item.getObjectId();
      this._reuse = reuse;
      this._stamp = systime > 0L ? systime : (reuse != 0L ? System.currentTimeMillis() + reuse : 0L);
      this._group = item.getSharedReuseGroup();
   }

   public long getStamp() {
      return this._stamp;
   }

   public int getItemId() {
      return this._id1;
   }

   public int getItemObjectId() {
      return this._id2;
   }

   public int getSkillId() {
      return this._id1;
   }

   public int getSkillLevel() {
      return this._id2;
   }

   public long getReuse() {
      return this._reuse;
   }

   public int getSharedReuseGroup() {
      return this._group;
   }

   public long getRemaining() {
      if (this._stamp == 0L) {
         return 0L;
      } else {
         long remainingTime = this._stamp - System.currentTimeMillis();
         if (remainingTime <= 0L) {
            this._stamp = 0L;
            return 0L;
         } else {
            return remainingTime;
         }
      }
   }

   public boolean hasNotPassed() {
      if (this._stamp == 0L) {
         return false;
      } else if (System.currentTimeMillis() >= this._stamp) {
         this._stamp = 0L;
         return false;
      } else {
         return true;
      }
   }
}
