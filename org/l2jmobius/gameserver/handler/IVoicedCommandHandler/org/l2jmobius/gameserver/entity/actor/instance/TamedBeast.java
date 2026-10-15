package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;

public class TamedBeast extends FeedableBeast {
   private int _foodSkillId;
   private static final int MAX_DISTANCE_FROM_HOME = 30000;
   private static final int MAX_DISTANCE_FROM_OWNER = 2000;
   private static final int MAX_DURATION = 1200000;
   private static final int DURATION_CHECK_INTERVAL = 60000;
   private static final int DURATION_INCREASE_INTERVAL = 20000;
   private static final int BUFF_INTERVAL = 5000;
   private int _remainingTime = 1200000;
   private int _homeX;
   private int _homeY;
   private int _homeZ;
   protected Player _owner;
   private Future<?> _buffTask = null;
   private Future<?> _durationCheckTask = null;
   protected boolean _isFreyaBeast;
   private Collection<Skill> _beastSkills = null;

   public TamedBeast(int npcTemplateId) {
      super(NpcData.getInstance().getTemplate(npcTemplateId));
      this.setInstanceType(InstanceType.TamedBeast);
      this.setHome(this);
   }

   public TamedBeast(int npcTemplateId, Player owner, int foodSkillId, int x, int y, int z) {
      super(NpcData.getInstance().getTemplate(npcTemplateId));
      this._isFreyaBeast = false;
      this.setInstanceType(InstanceType.TamedBeast);
      this.fullRestore();
      this.setOwner(owner);
      this.setFoodType(foodSkillId);
      this.setHome(x, y, z);
      this.spawnMe(x, y, z);
   }

   public TamedBeast(int npcTemplateId, Player owner, int food, int x, int y, int z, boolean isFreyaBeast) {
      super(NpcData.getInstance().getTemplate(npcTemplateId));
      this._isFreyaBeast = isFreyaBeast;
      this.setInstanceType(InstanceType.TamedBeast);
      this.fullRestore();
      this.setFoodType(food);
      this.setHome(x, y, z);
      this.spawnMe(x, y, z);
      this.setOwner(owner);
      if (isFreyaBeast) {
         this.getAI().setIntentionFollow(this._owner);
      }
   }

   public void onReceiveFood() {
      this._remainingTime += 20000;
      if (this._remainingTime > 1200000) {
         this._remainingTime = 1200000;
      }
   }

   public Location getHome() {
      return new Location(this._homeX, this._homeY, this._homeZ);
   }

   public void setHome(int x, int y, int z) {
      this._homeX = x;
      this._homeY = y;
      this._homeZ = z;
   }

   public void setHome(Creature c) {
      this.setHome(c.getX(), c.getY(), c.getZ());
   }

   public int getRemainingTime() {
      return this._remainingTime;
   }

   public void setRemainingTime(int duration) {
      this._remainingTime = duration;
   }

   public int getFoodType() {
      return this._foodSkillId;
   }

   public void setFoodType(int foodItemId) {
      if (foodItemId > 0) {
         this._foodSkillId = foodItemId;
         if (this._durationCheckTask != null) {
            this._durationCheckTask.cancel(true);
         }

         this._durationCheckTask = ThreadPool.scheduleAtFixedRate(new TamedBeast.CheckDuration(this), 60000L, 60000L);
      }
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         this.getAI().stopFollow();
         if (this._buffTask != null) {
            this._buffTask.cancel(true);
         }

         if (this._durationCheckTask != null) {
            this._durationCheckTask.cancel(true);
         }

         if (this._owner != null && this._owner.getTrainedBeasts() != null) {
            this._owner.getTrainedBeasts().remove(this);
         }

         this._buffTask = null;
         this._durationCheckTask = null;
         this._owner = null;
         this._foodSkillId = 0;
         this._remainingTime = 0;
         return true;
      }
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return !this._isFreyaBeast;
   }

   public boolean isFreyaBeast() {
      return this._isFreyaBeast;
   }

   public void addBeastSkill(Skill skill) {
      if (this._beastSkills == null) {
         this._beastSkills = ConcurrentHashMap.newKeySet();
      }

      this._beastSkills.add(skill);
   }

   public void castBeastSkills() {
      if (this._owner != null && this._beastSkills != null) {
         int delay = 100;

         for (Skill skill : this._beastSkills) {
            ThreadPool.schedule(new TamedBeast.buffCast(skill), delay);
            delay += 100 + skill.getHitTime();
         }

         ThreadPool.schedule(new TamedBeast.buffCast(null), delay);
      }
   }

   public Player getOwner() {
      return this._owner;
   }

   public void setOwner(Player owner) {
      if (owner != null) {
         this._owner = owner;
         this.setTitle(owner.getName());
         this.setShowSummonAnimation(true);
         this.broadcastPacket(new AbstractNpcInfo.NpcInfo(this, owner));
         owner.addTrainedBeast(this);
         this.getAI().startFollow(this._owner, 100);
         if (!this._isFreyaBeast) {
            int totalBuffsAvailable = 0;

            for (Skill skill : this.getTemplate().getSkills().values()) {
               if (skill.isContinuous() && !skill.isDebuff()) {
                  totalBuffsAvailable++;
               }
            }

            if (this._buffTask != null) {
               this._buffTask.cancel(true);
            }

            this._buffTask = ThreadPool.scheduleAtFixedRate(new TamedBeast.CheckOwnerBuffs(this, totalBuffsAvailable), 5000L, 5000L);
         }
      } else {
         this.deleteMe();
      }
   }

   public boolean isTooFarFromHome() {
      return !this.isInsideRadius3D(this._homeX, this._homeY, this._homeZ, 30000);
   }

   @Override
   public boolean deleteMe() {
      if (this._buffTask != null) {
         this._buffTask.cancel(true);
      }

      this._durationCheckTask.cancel(true);
      this.stopHpMpRegeneration();
      if (this._owner != null && this._owner.getTrainedBeasts() != null) {
         this._owner.getTrainedBeasts().remove(this);
      }

      this.setTarget(null);
      this._buffTask = null;
      this._durationCheckTask = null;
      this._owner = null;
      this._foodSkillId = 0;
      this._remainingTime = 0;
      return super.deleteMe();
   }

   public void onOwnerGotAttacked(Creature attacker) {
      if (this._owner == null || !this._owner.isOnline()) {
         this.deleteMe();
      } else if (!this._owner.isInsideRadius3D(this, 2000)) {
         this.getAI().startFollow(this._owner);
      } else if (!this._owner.isDead() && !this._isFreyaBeast) {
         if (!this.isCastingNow()) {
            float HPRatio = (float)this._owner.getCurrentHp() / this._owner.getMaxHp();
            if (HPRatio >= 0.8) {
               for (Skill skill : this.getTemplate().getSkills().values()) {
                  if (skill.isDebuff() && Rnd.get(3) < 1 && attacker != null && attacker.isAffectedBySkill(skill.getId())) {
                     this.sitCastAndFollow(skill, attacker);
                  }
               }
            } else if (HPRatio < 0.5) {
               int chance = 1;
               if (HPRatio < 0.25) {
                  chance = 2;
               }

               for (Skill skillx : this.getTemplate().getSkills().values()) {
                  if (Rnd.get(5) < chance
                     && skillx.hasEffectType(EffectType.CPHEAL, EffectType.HEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT)) {
                     this.sitCastAndFollow(skillx, this._owner);
                  }
               }
            }
         }
      }
   }

   protected void sitCastAndFollow(Skill skill, Creature target) {
      this.stopMove(null);
      this.broadcastPacket(new StopMove(this));
      this.getAI().setIntentionIdle();
      this.setTarget(target);
      this.doCast(skill);
      this.getAI().setIntentionFollow(this._owner);
   }

   @Override
   public void onAction(Player player, boolean interact) {
      if (player != null && this.canTarget(player)) {
         if (this != player.getTarget()) {
            player.setTarget(this);
         } else if (interact) {
            if (this.isAutoAttackable(player) && Math.abs(player.getZ() - this.getZ()) < 100) {
               player.getAI().setIntentionAttack(this);
            } else {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            }
         }
      }
   }

   private static class CheckDuration implements Runnable {
      private final TamedBeast _tamedBeast;

      CheckDuration(TamedBeast tamedBeast) {
         this._tamedBeast = tamedBeast;
      }

      @Override
      public void run() {
         int foodTypeSkillId = this._tamedBeast.getFoodType();
         Player owner = this._tamedBeast.getOwner();
         Item item = null;
         if (this._tamedBeast._isFreyaBeast) {
            item = owner.getInventory().getItemByItemId(foodTypeSkillId);
            if (item != null && item.getCount() >= 1L) {
               owner.destroyItem(ItemProcessType.DESTROY, item, 1L, this._tamedBeast, true);
               this._tamedBeast.broadcastPacket(new SocialAction(this._tamedBeast.getObjectId(), 3));
            } else {
               this._tamedBeast.deleteMe();
            }
         } else {
            this._tamedBeast.setRemainingTime(this._tamedBeast.getRemainingTime() - 60000);
            if (foodTypeSkillId == 2188) {
               item = owner.getInventory().getItemByItemId(6643);
            } else if (foodTypeSkillId == 2189) {
               item = owner.getInventory().getItemByItemId(6644);
            }

            if (item != null && item.getCount() >= 1L) {
               WorldObject oldTarget = owner.getTarget();
               owner.setTarget(this._tamedBeast);
               owner.callSkill(SkillData.getInstance().getSkill(foodTypeSkillId, 1), Collections.singletonList(this._tamedBeast));
               owner.setTarget(oldTarget);
            } else if (this._tamedBeast.getRemainingTime() < 900000) {
               this._tamedBeast.setRemainingTime(-1);
            }

            if (this._tamedBeast.getRemainingTime() <= 0) {
               this._tamedBeast.deleteMe();
            }
         }
      }
   }

   private class CheckOwnerBuffs implements Runnable {
      private final TamedBeast _tamedBeast;
      private final int _numBuffs;

      CheckOwnerBuffs(TamedBeast tamedBeast, int numBuffs) {
         Objects.requireNonNull(TamedBeast.this);
         super();
         this._tamedBeast = tamedBeast;
         this._numBuffs = numBuffs;
      }

      @Override
      public void run() {
         Player owner = this._tamedBeast.getOwner();
         if (owner != null && owner.isOnline()) {
            if (!TamedBeast.this.isInsideRadius3D(owner, 2000)) {
               TamedBeast.this.getAI().startFollow(owner);
            } else if (!owner.isDead()) {
               if (!TamedBeast.this.isCastingNow()) {
                  int totalBuffsOnOwner = 0;
                  int i = 0;
                  int rand = Rnd.get(this._numBuffs);
                  Skill buffToGive = null;

                  for (Skill skill : this._tamedBeast.getTemplate().getSkills().values()) {
                     if (skill.isContinuous() && !skill.isDebuff()) {
                        if (i++ == rand) {
                           buffToGive = skill;
                        }

                        if (owner.isAffectedBySkill(skill.getId())) {
                           totalBuffsOnOwner++;
                        }
                     }
                  }

                  if (this._numBuffs * 2 / 3 > totalBuffsOnOwner) {
                     this._tamedBeast.sitCastAndFollow(buffToGive, owner);
                  }

                  TamedBeast.this.getAI().setIntentionFollow(this._tamedBeast.getOwner());
               }
            }
         } else {
            TamedBeast.this.deleteMe();
         }
      }
   }

   private class buffCast implements Runnable {
      private final Skill _skill;

      public buffCast(Skill skill) {
         Objects.requireNonNull(TamedBeast.this);
         super();
         this._skill = skill;
      }

      @Override
      public void run() {
         if (this._skill == null) {
            TamedBeast.this.getAI().setIntentionFollow(TamedBeast.this._owner);
         } else {
            TamedBeast.this.sitCastAndFollow(this._skill, TamedBeast.this._owner);
         }
      }
   }
}
