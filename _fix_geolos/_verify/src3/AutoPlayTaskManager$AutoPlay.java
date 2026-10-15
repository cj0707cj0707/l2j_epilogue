package org.l2jmobius.gameserver.taskmanagers;

import java.util.Objects;
import java.util.Set;
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

class AutoPlayTaskManager$AutoPlay implements Runnable {
   private final Set<Player> _players;

   public AutoPlayTaskManager$AutoPlay(final Set<Player> param1, Set players) {
      Objects.requireNonNull(var1);
      this.this$0 = var1;
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
                                          int idleCount = AutoPlayTaskManager.IDLE_COUNT.getOrDefault(player, 0);
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
                                             AutoPlayTaskManager.IDLE_COUNT.remove(player);
                                          } else {
                                             AutoPlayTaskManager.IDLE_COUNT.put(player, idleCount + 1);
                                          }
                                       }
                                    } else {
                                       AutoPlayTaskManager.IDLE_COUNT.remove(player);
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

                  AutoPlayTaskManager.IDLE_COUNT.remove(player);
                  if (player.getAutoPlaySettings().doPickup() && player.isInventoryUnder90(false)) {
                     Item droppedItem = (Item)World.getFirstVisibleObjectInRange(
                        player,
                        Item.class,
                        200,
                        item -> {
                           if (!item.isSpawned()
                              || AutoPlayConfig.IGNORED_AUTO_PICK_ITEMS.contains(item.getId())
                              || !GeoEngine.getInstance()
                                 .canMoveToTarget(player.getX(), player.getY(), player.getZ(), item.getX(), item.getY(), item.getZ(), player.getInstanceId())) {
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
               this.this$0.stopAutoPlay(player);
            }
         }
      }
   }

   private boolean isMageCaster(Player player) {
      return !player.getAutoUseSettings().getAutoActions().contains(AutoPlayTaskManager.AUTO_ATTACK_ACTION);
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
