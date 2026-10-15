package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

public class AutoPlayTaskManager {
   private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
   private static final Map<Player, Integer> IDLE_COUNT = new ConcurrentHashMap<>();
   private static final int POOL_SIZE = 200;
   private static final int TASK_DELAY = 700;
   private static final Integer AUTO_ATTACK_ACTION = 2;

   protected AutoPlayTaskManager() {
   }

   public synchronized void startAutoPlay(Player player) {
      for (Set<Player> pool : POOLS) {
         if (pool.contains(player)) {
            return;
         }
      }

      player.setAutoPlaying(true);

      for (Set<Player> poolx : POOLS) {
         if (poolx.size() < 200) {
            player.onActionRequest();
            poolx.add(player);
            return;
         }
      }

      Set<Player> poolxx = ConcurrentHashMap.newKeySet(200);
      player.onActionRequest();
      poolxx.add(player);
      ThreadPool.schedulePriorityTaskAtFixedRate(new org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.AutoPlay(poolxx), 700L, 700L);
      POOLS.add(poolxx);
   }

   public void stopAutoPlay(Player player) {
      for (Set<Player> pool : POOLS) {
         if (pool.remove(player)) {
            player.setAutoPlaying(false);
            if (player.hasServitor() || player.hasPet()) {
               player.getSummon().followOwner();
            }

            IDLE_COUNT.remove(player);
            return;
         }
      }
   }

   public static org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager getInstance() {
      return org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.SingletonHolder.INSTANCE;
   }

   private class AutoPlay implements Runnable {
      private final Set<Player> _players;

      public AutoPlay(Set<Player> players) {
         Objects.requireNonNull(AutoPlayTaskManager.this);
         super();
         this._players = players;
      }

      @Override
      public void run() {
         if (!this._players.isEmpty()) {
            for (Player player : this._players) {
               if (player.isOnline() && (!player.isInOfflineMode() || player.isOfflinePlay()) && AutoPlayConfig.ENABLE_AUTO_PLAY) {
                  if (!player.isSitting() && !player.isCastingNow() && player.getQueuedSkill() == null) {
                     int targetMode = player.getAutoPlaySettings().getNextTargetMode();
                     WorldObject target = player.getTarget();
                     if (target != null && target.isCreature()) {
                        Creature creature = target.asCreature();
                        if (!creature.isAlikeDead() && this.isTargetModeValid(targetMode, player, creature)) {
                           if (creature.getTarget() == player || creature.getTarget() == null) {
                              if (!GeoEngine.getInstance().canSeeTarget(player, creature)) {
                                 player.setTarget(null);
                              } else {
                                 Summon summon = player.getSummon();
                                 if (summon != null
                                    && summon.hasAI()
                                    && !summon.isMoving()
                                    && !summon.isDisabled()
                                    && summon.getAI().getIntention() != Intention.ATTACK
                                    && summon.getAI().getIntention() != Intention.CAST
                                    && creature.isAutoAttackable(player)
                                    && GeoEngine.getInstance().canSeeTarget(player, creature)) {
                                    summon.getAI().setIntentionAttack(creature);
                                 }

                                 if (this.isMageCaster(player)) {
                                    if (player.calculateDistance2D(creature) > 900.0 && !player.isMoving() && player.hasAI() && !player.isDisabled()) {
                                       player.getAI().setIntentionMoveTo(creature);
                                    }
                                 } else if (player.hasAI() && !player.isAttackingNow() && !player.isCastingNow() && !player.isMoving() && !player.isDisabled()) {
                                    if (player.getAI().getIntention() == Intention.ATTACK) {
                                       player.getAI().setIntentionAttack(creature);
                                       if (!player.isAttackingNow() && !player.isCastingNow() && !player.isMoving()) {
                                          Weapon weapon = player.getActiveWeaponItem();
                                          if (weapon != null) {
                                             int idleCount = org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.IDLE_COUNT.getOrDefault(player, 0);
                                             if (idleCount > 10) {
                                                boolean ranged = weapon.getItemType().isRanged();
                                                double angle = LocationUtil.calculateHeadingFrom(player, creature);
                                                double radian = Math.toRadians(angle);
                                                double course = Math.toRadians(180.0);
                                                double distance = (
                                                      ranged
                                                         ? player.getCollisionRadius()
                                                         : player.getCollisionRadius() + creature.getTemplate().getCollisionRadius()
                                                   )
                                                   * 2.0F;
                                                int x1 = (int)(Math.cos(Math.PI + radian + course) * distance);
                                                int y1 = (int)(Math.sin(Math.PI + radian + course) * distance);
                                                Location location;
                                                if (ranged) {
                                                   location = new Location(player.getX() + x1, player.getY() + y1, player.getZ());
                                                } else {
                                                   location = new Location(creature.getX() + x1, creature.getY() + y1, player.getZ());
                                                }

                                                player.getAI().setIntentionMoveTo(location);
                                                org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.IDLE_COUNT.remove(player);
                                             } else {
                                                org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.IDLE_COUNT.put(player, idleCount + 1);
                                             }
                                          }
                                       } else {
                                          org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.IDLE_COUNT.remove(player);
                                       }
                                    } else if (creature.isAutoAttackable(player)) {
                                       if (!GeoEngine.getInstance().canSeeTarget(player, creature)) {
                                          player.setTarget(null);
                                       } else {
                                          player.getAI().setIntentionAttack(creature);
                                       }
                                    }
                                 }
                              }
                              continue;
                           }
                        } else {
                           if (creature.isMonster() && creature.isDead() && player.getAutoUseSettings().getAutoSkills().contains(254)) {
                              Skill sweeper = player.getKnownSkill(42);
                              if (sweeper != null) {
                                 Monster monster = target.asMonster();
                                 if (monster.checkSpoilOwner(player, false)) {
                                    if (player.calculateDistance2D(target) > 40.0) {
                                       if (!player.isMoving()) {
                                          player.getAI().setIntentionMoveTo(target);
                                       }
                                    } else {
                                       player.doCast(sweeper);
                                    }
                                    continue;
                                 }
                              }
                           }

                           player.setTarget(null);
                        }
                     }

                     org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.IDLE_COUNT.remove(player);
                     if (player.getAutoPlaySettings().doPickup() && player.isInventoryUnder90(false)) {
                        Item droppedItem = (Item)World.getFirstVisibleObjectInRange(
                           player,
                           Item.class,
                           200,
                           item -> {
                              if (!item.isSpawned()
                                 || AutoPlayConfig.IGNORED_AUTO_PICK_ITEMS.contains(item.getId())
                                 || !GeoEngine.getInstance()
                                    .canMoveToTarget(player.getX(), player.getY(), player.getZ(), item.getX(), item.getY(), item.getZ(), player.getInstanceId())
                                 )
                               {
                                 return false;
                              } else {
                                 return player.calculateDistance2D(item) > 70.0 ? true : !item.isProtected() || item.getOwnerId() == player.getObjectId();
                              }
                           }
                        );
                        if (droppedItem != null) {
                           if (player.calculateDistance2D(droppedItem) > 70.0) {
                              if (!player.isMoving()) {
                                 player.getAI().setIntentionMoveTo(droppedItem);
                              }
                           } else {
                              player.doPickupItem(droppedItem);
                           }
                           continue;
                        }
                     }

                     Creature creature = null;
                     Party party = player.getParty();
                     Player leader = party == null ? null : party.getLeader();
                     if (AutoPlayConfig.ENABLE_AUTO_ASSIST && party != null && leader != null && leader != player && !leader.isDead()) {
                        if (leader.calculateDistance3D(player) < PlayerConfig.ALT_PARTY_RANGE * 2) {
                           WorldObject leaderTarget = leader.getTarget();
                           if (leaderTarget == null
                              || !leaderTarget.isAttackable() && (!leaderTarget.isPlayable() || party.containsPlayer(leaderTarget.asPlayer()))) {
                              if (player.getAI().getIntention() != Intention.FOLLOW && !player.isDisabled()) {
                                 player.getAI().setIntentionFollow(leader);
                              }
                           } else {
                              creature = leaderTarget.asCreature();
                           }
                        }
                     } else {
                        creature = (Creature)World.getNearestVisibleObjectInRange(
                           player,
                           Creature.class,
                           player.getAutoPlaySettings().isShortRange() && targetMode != 2
                              ? AutoPlayConfig.AUTO_PLAY_SHORT_RANGE
                              : AutoPlayConfig.AUTO_PLAY_LONG_RANGE,
                           nearby -> {
                              if (nearby.isAlikeDead()) {
                                 return false;
                              } else if (!player.getAutoPlaySettings().isRespectfulHunting()
                                 || nearby.isPlayable()
                                 || nearby.getTarget() == null
                                 || nearby.getTarget() == player
                                 || player.hasSummon() && player.getSummon().getObjectId() == nearby.getTarget().getObjectId()) {
                                 return !this.isTargetModeValid(targetMode, player, nearby)
                                    ? false
                                    : Math.abs(player.getZ() - nearby.getZ()) < 800
                                       && GeoEngine.getInstance().canSeeTarget(player, nearby)
                                       && GeoEngine.getInstance()
                                          .canMoveToTarget(
                                             player.getX(), player.getY(), player.getZ(), nearby.getX(), nearby.getY(), nearby.getZ(), player.getInstanceId()
                                          );
                              } else {
                                 return false;
                              }
                           }
                        );
                     }

                     if (creature != null) {
                        player.setTarget(creature);
                        if (this.isMageCaster(player)) {
                           if (player.calculateDistance2D(creature) > 900.0 && !player.isMoving() && player.hasAI() && !player.isDisabled()) {
                              player.getAI().setIntentionMoveTo(creature);
                           }
                        } else {
                           player.getAI().setIntentionAttack(creature);
                        }
                     }
                  }
               } else {
                  AutoPlayTaskManager.this.stopAutoPlay(player);
               }
            }
         }
      }

      private boolean isMageCaster(Player player) {
         return !player.getAutoUseSettings().getAutoActions().contains(org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.AUTO_ATTACK_ACTION);
      }

      private boolean isTargetModeValid(int mode, Player player, Creature creature) {
         if (creature.isTargetable() && (!creature.isNpc() || !creature.isInvul() && creature.asNpc().isShowName())) {
            switch (mode) {
               case 1:
                  return creature.isMonster() && !creature.isRaid() && creature.isAutoAttackable(player);
               case 2:
                  return creature.isPlayable() && creature.isAutoAttackable(player);
               case 3:
                  return creature.isNpc() && !creature.isMonster() && !creature.isInsideZone(ZoneId.PEACE);
               default:
                  return creature.isNpc() && !creature.isInsideZone(ZoneId.PEACE) || creature.isPlayable() && creature.isAutoAttackable(player);
            }
         } else {
            return false;
         }
      }
   }

   private static class SingletonHolder {
      protected static final org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager INSTANCE = new org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager();
   }
}
