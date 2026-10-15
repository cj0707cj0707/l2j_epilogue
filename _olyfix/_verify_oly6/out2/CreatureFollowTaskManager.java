package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class CreatureFollowTaskManager {
   protected static final Map<Creature, Integer> NORMAL_FOLLOW_CREATURES = new ConcurrentHashMap<>();
   protected static final Map<Creature, Integer> ATTACK_FOLLOW_CREATURES = new ConcurrentHashMap<>();
   protected static boolean _workingNormal = false;
   protected static boolean _workingAttack = false;

   protected CreatureFollowTaskManager() {
      ThreadPool.schedulePriorityTaskAtFixedRate(new org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.CreatureFollowNormalTask(), 1000L, 1000L);
      ThreadPool.schedulePriorityTaskAtFixedRate(new org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.CreatureFollowAttackTask(), 500L, 500L);
   }

   protected void follow(Creature creature, int range) {
      try {
         if (creature.hasAI()) {
            CreatureAI ai = creature.getAI();
            if (ai != null) {
               WorldObject followTarget = ai.getFollowTarget();
               if (followTarget == null) {
                  if (creature.isSummon()) {
                     creature.asSummon().setFollowStatus(false);
                  }

                  ai.setIntentionIdle();
                  return;
               }

               int followRange = range == -1 ? Rnd.get(50, 100) : range;
               if (!creature.isInsideRadius3D(followTarget, followRange)) {
                  if (!creature.isInsideRadius3D(followTarget, 3000)) {
                     if (creature.isSummon()) {
                        creature.asSummon().setFollowStatus(false);
                     }

                     ai.setIntentionIdle();
                     return;
                  }

                  ai.moveToPawn(followTarget, followRange);
                  if (!creature.isMoving() && (ai.getIntention() == Intention.ATTACK || ai.getIntention() == Intention.CAST)) {
                     ai.notifyActionArrived();
                  }
               } else if (!creature.isMoving() && (ai.getIntention() == Intention.ATTACK || ai.getIntention() == Intention.CAST)) {
                  ai.notifyActionArrived();
               }
            } else {
               this.remove(creature);
            }
         } else {
            this.remove(creature);
         }
      } catch (Exception var6) {
      }
   }

   public boolean isFollowing(Creature creature) {
      return NORMAL_FOLLOW_CREATURES.containsKey(creature) || ATTACK_FOLLOW_CREATURES.containsKey(creature);
   }

   public void addNormalFollow(Creature creature, int range) {
      NORMAL_FOLLOW_CREATURES.putIfAbsent(creature, range);
      this.follow(creature, range);
   }

   public void addAttackFollow(Creature creature, int range) {
      ATTACK_FOLLOW_CREATURES.putIfAbsent(creature, range);
      this.follow(creature, range);
   }

   public void remove(Creature creature) {
      NORMAL_FOLLOW_CREATURES.remove(creature);
      ATTACK_FOLLOW_CREATURES.remove(creature);
   }

   public static org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.SingletonHolder.INSTANCE;
   }

   protected class CreatureFollowAttackTask implements Runnable {
      protected CreatureFollowAttackTask() {
         Objects.requireNonNull(CreatureFollowTaskManager.this);
         super();
      }

      @Override
      public void run() {
         if (!org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingAttack) {
            org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingAttack = true;
            if (!org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.ATTACK_FOLLOW_CREATURES.isEmpty()) {
               for (Entry<Creature, Integer> entry : org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.ATTACK_FOLLOW_CREATURES.entrySet()) {
                  CreatureFollowTaskManager.this.follow(entry.getKey(), entry.getValue());
               }
            }

            org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingAttack = false;
         }
      }
   }

   protected class CreatureFollowNormalTask implements Runnable {
      protected CreatureFollowNormalTask() {
         Objects.requireNonNull(CreatureFollowTaskManager.this);
         super();
      }

      @Override
      public void run() {
         if (!org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingNormal) {
            org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingNormal = true;
            if (!org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.NORMAL_FOLLOW_CREATURES.isEmpty()) {
               for (Entry<Creature, Integer> entry : org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager.NORMAL_FOLLOW_CREATURES.entrySet()) {
                  CreatureFollowTaskManager.this.follow(entry.getKey(), entry.getValue());
               }
            }

            org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager._workingNormal = false;
         }
      }
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager();
   }
}
