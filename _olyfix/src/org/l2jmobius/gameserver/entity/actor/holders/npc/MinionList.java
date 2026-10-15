package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class MinionList {
   private static final int SPAWN_OFFSET_RADIUS = 200;
   private static final int MINIMUM_SPAWN_RADIUS = 30;
   private static final int BASE_AGGRO_AMOUNT = 1;
   private static final int MASTER_AGGRO_AMOUNT = 10;
   private static final int RAID_AGGRO_MULTIPLIER = 10;
   private static final int INVALID_RESPAWN_TIME = -1;
   private static final int NO_RESPAWN = 0;
   protected final Monster _master;
   private final Set<Monster> _spawnedMinions = ConcurrentHashMap.newKeySet();
   private final Set<ScheduledFuture<?>> _respawnTasks = ConcurrentHashMap.newKeySet();

   public MinionList(Monster master) {
      if (master == null) {
         throw new NullPointerException("MinionList: Master monster cannot be null!");
      } else {
         this._master = master;
      }
   }

   public Collection<Monster> getSpawnedMinions() {
      return this._spawnedMinions;
   }

   public void spawnMinions(Collection<MinionHolder> minions) {
      if (!this._master.isAlikeDead() && minions != null) {
         for (MinionHolder minion : minions) {
            int minionCount = minion.getCount();
            int minionId = minion.getId();
            int minionsToSpawn = minionCount - this.countSpawnedMinionsById(minionId);
            if (minionsToSpawn > 0) {
               for (int i = 0; i < minionsToSpawn; i++) {
                  this.spawnMinion(minionId);
               }
            }
         }
      }
   }

   public void onMinionSpawn(Monster minion) {
      this._spawnedMinions.add(minion);
   }

   public void onMasterDie(boolean force) {
      if (this._master.isRaid() || force || NpcConfig.FORCE_DELETE_MINIONS) {
         if (!this._spawnedMinions.isEmpty()) {
            for (Monster minion : this._spawnedMinions) {
               if (minion != null) {
                  minion.setLeader(null);
                  minion.deleteMe();
               }
            }

            this._spawnedMinions.clear();
         }

         if (!this._respawnTasks.isEmpty()) {
            for (ScheduledFuture<?> task : this._respawnTasks) {
               if (task != null && !task.isCancelled() && !task.isDone()) {
                  task.cancel(true);
               }
            }

            this._respawnTasks.clear();
         }
      }
   }

   public void onMinionDie(Monster minion, int respawnTime) {
      if (respawnTime == 0) {
         minion.setLeader(null);
      }

      this._spawnedMinions.remove(minion);
      int actualRespawnTime = respawnTime <= -1 ? (this._master.isRaid() ? (int)NpcConfig.RAID_MINION_RESPAWN_TIMER : 0) : respawnTime;
      if (actualRespawnTime > 0 && !this._master.isAlikeDead()) {
         this._respawnTasks.add(ThreadPool.schedule(new MinionList.MinionRespawnTask(minion), actualRespawnTime));
      }
   }

   public void onAssist(Creature caller, Creature attacker) {
      if (attacker != null) {
         if (!this._master.isAlikeDead() && !this._master.isInCombat()) {
            this._master.addDamageHate(attacker, 0L, 1L);
         }

         boolean callerIsMaster = caller == this._master;
         int aggroAmount = callerIsMaster ? 10 : 1;
         if (this._master.isRaid()) {
            aggroAmount *= 10;
         }

         for (Monster minion : this._spawnedMinions) {
            if (minion != null && !minion.isDead() && (callerIsMaster || !minion.isInCombat())) {
               minion.addDamageHate(attacker, 0L, aggroAmount);
            }
         }
      }
   }

   public void onMasterTeleported() {
      int spawnRadius = 200;
      int minimumRadius = (int)this._master.getCollisionRadius() + 30;

      for (Monster minion : this._spawnedMinions) {
         if (minion != null && !minion.isDead() && !minion.isMovementDisabled()) {
            int newX = Rnd.get(minimumRadius * 2, 400);
            int newY = Rnd.get(newX, 400);
            newY = (int)Math.sqrt(newY * newY - newX * newX);
            if (newX > 200 + minimumRadius) {
               newX = this._master.getX() + newX - 200;
            } else {
               newX = this._master.getX() - newX + minimumRadius;
            }

            if (newY > 200 + minimumRadius) {
               newY = this._master.getY() + newY - 200;
            } else {
               newY = this._master.getY() - newY + minimumRadius;
            }

            minion.teleToLocation(new Location(newX, newY, this._master.getZ()));
         }
      }
   }

   private void spawnMinion(int minionTemplateId) {
      if (minionTemplateId != 0) {
         spawnMinion(this._master, minionTemplateId);
      }
   }

   public static Monster spawnMinion(Monster master, int minionTemplateId) {
      NpcTemplate minionTemplate = NpcData.getInstance().getTemplate(minionTemplateId);
      return minionTemplate == null ? null : initializeNpc(master, new Monster(minionTemplate));
   }

   protected static Monster initializeNpc(Monster master, Monster minion) {
      minion.stopAllEffects();
      minion.setDead(false);
      minion.setDecayed(false);
      minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
      minion.setHeading(master.getHeading());
      minion.setLeader(master);
      minion.setInstanceId(master.getInstanceId());
      if (minion.getTemplate().isUsingServerSideName()) {
         minion.setName(minion.getTemplate().getName());
      }

      if (minion.getTemplate().isUsingServerSideTitle()) {
         minion.setTitle(minion.getTemplate().getTitle());
      }

      int spawnRadius = 200;
      int minimumRadius = (int)master.getCollisionRadius() + 30;
      int newX = Rnd.get(minimumRadius * 2, 400);
      int newY = Rnd.get(newX, 400);
      newY = (int)Math.sqrt(newY * newY - newX * newX);
      if (newX > 200 + minimumRadius) {
         newX = master.getX() + newX - 200;
      } else {
         newX = master.getX() - newX + minimumRadius;
      }

      if (newY > 200 + minimumRadius) {
         newY = master.getY() + newY - 200;
      } else {
         newY = master.getY() - newY + minimumRadius;
      }

      minion.spawnMe(newX, newY, master.getZ());
      if (minion.isInInstance()) {
         minion.broadcastInfo();
      }

      return minion;
   }

   private int countSpawnedMinionsById(int minionTemplateId) {
      int count = 0;

      for (Monster minion : this._spawnedMinions) {
         if (minion != null && minion.getId() == minionTemplateId) {
            count++;
         }
      }

      return count;
   }

   public int getSpawnedMinionCount() {
      return this._spawnedMinions.size();
   }

   private class MinionRespawnTask implements Runnable {
      private final Monster _minion;

      public MinionRespawnTask(Monster minion) {
         Objects.requireNonNull(MinionList.this);
         super();
         this._minion = minion;
      }

      @Override
      public void run() {
         if (!MinionList.this._master.isAlikeDead() && MinionList.this._master.isSpawned() && !this._minion.isSpawned()) {
            MinionList.initializeNpc(MinionList.this._master, this._minion);
            if (!MinionList.this._master.getAggroList().isEmpty()) {
               this._minion.getAggroList().putAll(MinionList.this._master.getAggroList());
               this._minion.getAI().setIntentionAttack(this._minion.getAggroList().keySet().iterator().next());
            }
         }
      }
   }
}
