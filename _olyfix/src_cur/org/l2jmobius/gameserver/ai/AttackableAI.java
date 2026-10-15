package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.instance.FestivalMonster;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyMob;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.instance.RiftInvader;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.BossZone;
import org.l2jmobius.gameserver.entity.zone.type.NpcSpawnTerritory;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableHate;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

public class AttackableAI extends CreatureAI {
   protected static final int FEAR_TICKS = 5;
   private static final int RANDOM_WALK_RATE = 30;
   private static final int MAX_ATTACK_TIMEOUT = 1200;
   private static final int WANDER_ATTEMPTS = 3;
   private static final int SKILL_PROBABILITY_SCALE = 10000;
   private static final int MAX_PARAMETERIZED_SKILL_SLOTS = 6;
   private static final int MINION_LEASH_INTERVAL = 120000;
   private static final int MINION_LEASH_OFFSET = 100;
   private int _attackTimeout;
   private int _globalAggro;
   private boolean _thinking;
   private int _chaosTime = 0;
   private long _minionLeashTime;
   private int _fearTime;
   private Future<?> _fearTask = null;

   public AttackableAI(Attackable attackable) {
      super(attackable);
      this._attackTimeout = Integer.MAX_VALUE;
      this.rollGlobalAggro();
   }

   private boolean isAggressiveTowards(Creature target) {
      if (target != null && this.getActiveChar() != null) {
         if (target.isInvul()) {
            if (target.isPlayer() && target.isGM()) {
               return false;
            }

            if (target.isSummon() && target.asSummon().getOwner().isGM()) {
               return false;
            }
         }

         if (target.isDoor()) {
            return false;
         } else {
            Attackable me = this.getActiveChar();
            if (!target.isAlikeDead() && (!target.isPlayable() || me.isInsideRadius3D(target, me.getAggroRange()))) {
               if (target.isPlayable() && !me.isRaid() && !me.canSeeThroughSilentMove() && target.asPlayable().isSilentMovingAffected()) {
                  return false;
               } else {
                  Player player = target.asPlayer();
                  if (player != null) {
                     if (!player.getAccessLevel().canTakeAggro()) {
                        return false;
                     }

                     if (player.isRecentFakeDeath()) {
                        return false;
                     }

                     if (FactionSystemConfig.FACTION_SYSTEM_ENABLED
                        && FactionSystemConfig.FACTION_GUARDS_ENABLED
                        && (
                           player.isGood() && this._actor.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)
                              || player.isEvil() && this._actor.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME)
                        )) {
                        return true;
                     }

                     if (me instanceof RiftInvader && player.isInParty()) {
                        Party party = player.getParty();
                        if (party.isInDimensionalRift()) {
                           byte riftType = party.getDimensionalRift().getType();
                           byte riftRoom = party.getDimensionalRift().getCurrentRoom();
                           if (!DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ())) {
                              return false;
                           }
                        }
                     }
                  }

                  if (me instanceof Guard) {
                     if (player != null && player.getKarma() > 0) {
                        return GeoEngine.getInstance().canSeeTarget(me, (WorldObject)player);
                     } else {
                        return target.isMonster() && NpcConfig.GUARD_ATTACK_AGGRO_MOB
                           ? target.asMonster().isAggressive() && GeoEngine.getInstance().canSeeTarget(me, (WorldObject)target)
                           : false;
                     }
                  } else if (me instanceof FriendlyMob) {
                     if (target instanceof Npc) {
                        return false;
                     } else {
                        return target.isPlayer() && target.asPlayer().getKarma() > 0 ? GeoEngine.getInstance().canSeeTarget(me, (WorldObject)target) : false;
                     }
                  } else {
                     if (target.isAttackable()) {
                        if (!target.isAutoAttackable(me)) {
                           return false;
                        }

                        if (me.isChaos() && me.isInsideRadius2D(target, me.getAggroRange())) {
                           if (target.asAttackable().isInMyClan(me)) {
                              return false;
                           }

                           return GeoEngine.getInstance().canSeeTarget(me, (WorldObject)target);
                        }
                     }

                     if (target.isAttackable() || target instanceof Npc) {
                        return false;
                     } else if (!NpcConfig.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE) && target.isInsideZone(ZoneId.NO_PVP)) {
                        return false;
                     } else {
                        return me.isChampion() && ChampionMonstersConfig.CHAMPION_PASSIVE
                           ? false
                           : me.isAggressive() && GeoEngine.getInstance().canSeeTarget(me, (WorldObject)target);
                     }
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void startAITask() {
      AttackableThinkTaskManager.getInstance().add(this.getActiveChar());
   }

   @Override
   public void stopAITask() {
      AttackableThinkTaskManager.getInstance().remove(this.getActiveChar());
      super.stopAITask();
   }

   private boolean shouldPromoteIdleToActive() {
      Attackable npc = this.getActiveChar();
      if (npc.isAlikeDead()) {
         return false;
      } else {
         return World.getFirstVisibleObject(npc, Player.class) != null
            ? true
            : npc.getSpawn() != null
               && (
                  npc.getSpawn().getSpawnTerritory() != null
                     ? !npc.getSpawn().getSpawnTerritory().isInsideZone(npc.getX(), npc.getY())
                     : !npc.isInsideRadius3D(npc.getSpawn().getLocation(), NpcConfig.MAX_DRIFT_RANGE + NpcConfig.MAX_DRIFT_RANGE)
               );
      }
   }

   @Override
   public synchronized void setIntentionIdle() {
      if (this.shouldPromoteIdleToActive()) {
         this.setIntentionActive();
      } else {
         super.setIntentionIdle();
         this.stopAITask();
         this._actor.detachAI();
      }
   }

   @Override
   public synchronized void setIntentionActive() {
      this._attackTimeout = Integer.MAX_VALUE;
      super.setIntentionActive();
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionRest() {
      super.setIntentionRest();
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionAttack(WorldObject target) {
      this._attackTimeout = 1200 + GameTimeTaskManager.getInstance().getGameTicks();
      super.setIntentionAttack(target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionCast(Skill skill, WorldObject target) {
      if (target != null) {
         this.setTarget(target);
      }

      super.setIntentionCast(skill, target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionMoveTo(ILocational destination) {
      super.setIntentionMoveTo(destination);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionFollow(WorldObject target) {
      super.setIntentionFollow(target);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionPickUp(WorldObject item) {
      super.setIntentionPickUp(item);
      this.startAITask();
   }

   @Override
   public synchronized void setIntentionInteract(WorldObject object) {
      super.setIntentionInteract(object);
      this.startAITask();
   }

   @Override
   public void notifyActionAfraid(WorldObject effector, boolean start) {
      if (this._fearTime > 0 && this._fearTask == null) {
         this._fearTask = ThreadPool.scheduleAtFixedRate(() -> {
            if (effector != null) {
               int fearTimeLeft = this.getFearTime() - 5;
               this.setFearTime(fearTimeLeft);
               this.notifyActionAfraid(effector, start);
            }
         }, 0L, 5000L);
         this._actor.startAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
      } else {
         super.notifyActionAfraid(effector, start);
         if ((this._actor.isDead() || this._fearTime <= 0) && this._fearTask != null) {
            this._fearTask.cancel(true);
            this._fearTask = null;
            this._actor.stopAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
            this.setIntentionIdle();
         }
      }
   }

   protected void thinkCast() {
      if (this.checkTargetLost(this.getCastTarget())) {
         this.setCastTarget(null);
      } else if (!this.maybeMoveToPawn(this.getCastTarget(), this._actor.getMagicalAttackRange(this._skill))) {
         this.clientStopMoving(null);
         this.setIntentionActive();
         this._actor.doCast(this._skill);
      }
   }

   protected void thinkActive() {
      WorldRegion region = this._actor.getWorldRegion();
      if (region != null && region.areNeighborsActive()) {
         Attackable npc = this.getActiveChar();
         if (this._globalAggro != 0) {
            if (this._globalAggro < 0) {
               this._globalAggro++;
            } else {
               this._globalAggro--;
            }
         }

         if (this._globalAggro >= 0) {
            World.forEachVisibleObject(
               npc,
               Creature.class,
               target -> {
                  if (!(target instanceof StaticObject)) {
                     if (npc.isFakePlayer() && npc.isAggressive()) {
                        List<Item> droppedItems = npc.getFakePlayerDrops();
                        if (droppedItems.isEmpty()) {
                           Creature nearestTarget = World.getNearestVisibleObjectInRange(
                              npc,
                              Creature.class,
                              npc.getAggroRange(),
                              t -> t != this._actor
                                 && !t.isDead()
                                 && (
                                    FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()
                                       || FakePlayersConfig.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()
                                       || FakePlayersConfig.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer()
                                 )
                                 && npc.getHating(t) == 0L
                           );
                           if (nearestTarget != null) {
                              npc.addDamageHate(nearestTarget, 0L, 1L);
                           }
                        } else if (!npc.isInCombat()) {
                           int itemIndex = npc.getFakePlayerDrops().size() - 1;
                           Item droppedItem = npc.getFakePlayerDrops().get(itemIndex);
                           if (droppedItem != null && droppedItem.isSpawned()) {
                              if (npc.calculateDistance2D(droppedItem) > 50.0) {
                                 this.moveTo(droppedItem);
                              } else {
                                 npc.getFakePlayerDrops().remove(itemIndex);
                                 droppedItem.pickupMe(npc);
                                 if (GeneralConfig.SAVE_DROPPED_ITEM) {
                                    ItemsOnGroundManager.getInstance().removeObject(droppedItem);
                                 }

                                 if (droppedItem.getTemplate().hasExImmediateEffect()) {
                                    for (SkillHolder skillHolder : droppedItem.getTemplate().getSkills()) {
                                       npc.doSimultaneousCast(skillHolder.getSkill());
                                    }

                                    npc.broadcastInfo();
                                 }
                              }
                           } else {
                              npc.getFakePlayerDrops().remove(itemIndex);
                           }

                           npc.setRunning();
                        }
                     } else {
                        if (npc instanceof FestivalMonster && target.isPlayer()) {
                           Player targetPlayer = target.asPlayer();
                           if (!targetPlayer.isFestivalParticipant()) {
                              return;
                           }
                        }

                        if (this.isAggressiveTowards(target)) {
                           if (target.isFakePlayer()) {
                              if (!npc.isFakePlayer() || npc.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC) {
                                 long hating = npc.getHating(target);
                                 if (hating == 0L) {
                                    npc.addDamageHate(target, 0L, 0L);
                                 }
                              }

                              return;
                           }

                           if (target.isPlayable() && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_HATE, this.getActiveChar())) {
                              TerminateReturn term = EventDispatcher.getInstance()
                                 .notifyEvent(
                                    new OnAttackableHate(this.getActiveChar(), target.asPlayer(), target.isSummon()),
                                    this.getActiveChar(),
                                    TerminateReturn.class
                                 );
                              if (term != null && term.terminate()) {
                                 return;
                              }
                           }

                           if (npc.getHating(target) == 0L) {
                              npc.addDamageHate(target, 0L, 0L);
                           }
                        }
                     }
                  }
               }
            );
            Creature hated = npc.isConfused() ? this.getAttackTarget() : npc.getMostHated();
            if (hated != null && !npc.isCoreAIDisabled()) {
               long aggro = npc.getHating(hated);
               if (aggro + this._globalAggro > 0L) {
                  if (!npc.isRunning()) {
                     npc.setRunning();
                  }

                  this.setIntentionAttack(hated);
               }

               return;
            }
         }

         if (npc.getCurrentHp() == npc.getMaxHp() && npc.getCurrentMp() == npc.getMaxMp() && !npc.getAttackByList().isEmpty() && Rnd.get(500) == 0) {
            npc.clearAggroList();
            npc.getAttackByList().clear();
         }

         if (npc.canReturnToSpawnPoint()) {
            if (npc.isWalker()
               || npc.getSpawn() == null
               || (
                  npc.getSpawn().getSpawnTerritory() != null
                     ? npc.getSpawn().getSpawnTerritory().isInsideZone(npc.getX(), npc.getY())
                     : !(npc.calculateDistance2D(npc.getSpawn()) > NpcConfig.MAX_DRIFT_RANGE)
               )
               || this.getTarget() != null
                  && !this.getTarget().isInvisible()
                  && (!this.getTarget().isPlayer() || NpcConfig.ATTACKABLES_CAMP_PLAYER_CORPSES || !this.getTarget().asPlayer().isAlikeDead())) {
               if (this.getTarget() == null || !this.getTarget().isPlayer() || !this.getTarget().asPlayer().isAlikeDead()) {
                  Creature leader = npc.getLeader();
                  if (leader != null && !leader.isAlikeDead()) {
                     int minRadius = 30;
                     int offset;
                     if (npc.isRaidMinion()) {
                        offset = 500;
                     } else {
                        offset = 200;
                     }

                     if (leader.isRunning()) {
                        npc.setRunning();
                     } else {
                        npc.setWalking();
                     }

                     if (npc.calculateDistance2D(leader) > offset) {
                        int x1 = Rnd.get(60, offset * 2);
                        int y1 = Rnd.get(x1, offset * 2);
                        y1 = (int)Math.sqrt(y1 * y1 - x1 * x1);
                        x1 = x1 > offset + 30 ? leader.getX() + x1 - offset : leader.getX() - x1 + 30;
                        y1 = y1 > offset + 30 ? leader.getY() + y1 - offset : leader.getY() - y1 + 30;
                        this.moveTo(x1, y1, leader.getZ());
                        return;
                     }

                     if (Rnd.get(30) == 0) {
                        for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF)) {
                           if (!npc.isAffectedBySkill(sk.getId())
                              && !(npc.getCurrentMp() <= sk.getMpConsume())
                              && (sk.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS || !(npc.getCurrentHp() >= npc.getMaxHp()))) {
                              npc.setTarget(npc);
                              npc.doCast(sk);
                              return;
                           }
                        }
                     }
                  } else if (npc.getSpawn() != null && Rnd.get(30) == 0 && npc.isRandomWalkingEnabled()) {
                     for (Skill skx : npc.getTemplate().getAISkills(AISkillScope.BUFF)) {
                        if (!npc.isAffectedBySkill(skx.getId())
                           && !(npc.getCurrentMp() <= skx.getMpConsume())
                           && (skx.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS || !(npc.getCurrentHp() >= npc.getMaxHp()))) {
                           npc.setTarget(npc);
                           npc.doCast(skx);
                           return;
                        }
                     }

                     NpcSpawnTerritory spawnTerritory = npc.getSpawn().getSpawnTerritory();
                     if (spawnTerritory != null) {
                        for (int i = 0; i < 3; i++) {
                           int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2);
                           int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2);
                           deltaY = (int)Math.sqrt(deltaY * deltaY - deltaX * deltaX);
                           int x2 = deltaX + npc.getX() - NpcConfig.MAX_DRIFT_RANGE;
                           int y2 = deltaY + npc.getY() - NpcConfig.MAX_DRIFT_RANGE;
                           if (spawnTerritory.isInsideZone(x2, y2)) {
                              Location moveLoc = this._actor.isFlying()
                                 ? new Location(x2, y2, npc.getZ())
                                 : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x2, y2, npc.getZ(), npc.getInstanceId());
                              if (spawnTerritory.isInsideZone(moveLoc.getX(), moveLoc.getY())) {
                                 this.moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
                                 break;
                              }
                           }
                        }

                        return;
                     }

                     int x1 = npc.getSpawn().getX();
                     int y1 = npc.getSpawn().getY();
                     int z1 = npc.getSpawn().getZ();
                     if (npc.isInsideRadius2D(x1, y1, 0, NpcConfig.MAX_DRIFT_RANGE)) {
                        int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2);
                        int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2);
                        deltaY = (int)Math.sqrt(deltaY * deltaY - deltaX * deltaX);
                        x1 = deltaX + x1 - NpcConfig.MAX_DRIFT_RANGE;
                        y1 = deltaY + y1 - NpcConfig.MAX_DRIFT_RANGE;
                        z1 = npc.getZ();
                     }

                     Location moveLoc = this._actor.isFlying()
                        ? new Location(x1, y1, z1)
                        : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId());
                     if (LocationUtil.calculateDistance(npc.getSpawn(), moveLoc, false, false) <= NpcConfig.MAX_DRIFT_RANGE) {
                        this.moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
                     }
                  }
               }
            } else {
               npc.setWalking();
               npc.returnHome();
            }
         }
      }
   }

   protected void thinkAttack() {
      Attackable npc = this.getActiveChar();
      if (npc != null && !npc.isCastingNow()) {
         if (npc.isMonster()
            && !npc.isWalker()
            && !(npc instanceof GrandBoss)
            && (npc.isRaid() ? NpcConfig.AGGRO_DISTANCE_CHECK_RAIDS : NpcConfig.AGGRO_DISTANCE_CHECK_ENABLED)) {
            Spawn spawn = npc.getSpawn();
            if (spawn != null
               && npc.calculateDistance2D(spawn.getLocation())
                  > (
                     spawn.getChaseRange() > 0
                        ? Math.max(NpcConfig.MAX_DRIFT_RANGE, spawn.getChaseRange())
                        : (npc.isRaid() ? NpcConfig.AGGRO_DISTANCE_CHECK_RAID_RANGE : NpcConfig.AGGRO_DISTANCE_CHECK_RANGE)
                  )
               && (NpcConfig.AGGRO_DISTANCE_CHECK_INSTANCES || !npc.isInInstance())) {
               if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE) {
                  npc.setCurrentHp(npc.getMaxHp());
                  npc.setCurrentMp(npc.getMaxMp());
               }

               npc.abortAttack();
               npc.clearAggroList();
               npc.getAttackByList().clear();
               if (npc.hasAI()) {
                  npc.getAI().setIntentionMoveTo(spawn.getLocation());
               } else {
                  npc.teleToLocation(spawn.getLocation(), true);
               }

               if (this._actor.asMonster().hasMinions()) {
                  for (Monster minion : this._actor.asMonster().getMinionList().getSpawnedMinions()) {
                     if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE) {
                        minion.setCurrentHp(minion.getMaxHp());
                        minion.setCurrentMp(minion.getMaxMp());
                     }

                     minion.abortAttack();
                     minion.clearAggroList();
                     minion.getAttackByList().clear();
                     if (minion.hasAI()) {
                        minion.getAI().setIntentionMoveTo(spawn.getLocation());
                     } else {
                        minion.teleToLocation(spawn.getLocation(), true);
                     }
                  }
               }

               return;
            }
         }

         if (!npc.isCoreAIDisabled()) {
            Creature mostHate = npc.getMostHated();
            if (mostHate == null) {
               this.setIntentionActive();
            } else {
               if (this.getAttackTarget() != mostHate) {
                  this.setAttackTarget(mostHate);
               }

               if (this.getTarget() != mostHate) {
                  this.setTarget(mostHate);
               }

               if (npc.isMovementDisabled()) {
                  this.movementDisable();
               } else if (mostHate.isAlikeDead()) {
                  npc.stopHating(mostHate);
               } else if (this._attackTimeout < GameTimeTaskManager.getInstance().getGameTicks()) {
                  boolean wasInCombat = npc.isInCombat();
                  this.setIntentionActive();
                  this.setTarget(null);
                  npc.clearAggroList();
                  npc.getAttackByList().clear();
                  if (!this._actor.isFakePlayer()) {
                     npc.setWalking();
                  }

                  if (npc.isMonster()
                     && npc.getSpawn() != null
                     && !npc.isInInstance()
                     && (wasInCombat || World.getFirstVisibleObject(npc, Player.class) == null)) {
                     npc.teleToLocation(npc.getSpawn(), false);
                  }
               } else if (!GeoEngine.getInstance().canSeeTarget(this._actor, (WorldObject)mostHate)) {
                  if (this._actor.calculateDistance3D(mostHate) < 6000.0) {
                     this.moveTo(mostHate);
                  }
               } else {
                  NpcTemplate template = npc.getTemplate();
                  List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
                  if (!aiSuicideSkills.isEmpty() && (int)(npc.getCurrentHp() / npc.getMaxHp() * 100.0) < 30) {
                     Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
                     if (LocationUtil.checkIfInRange(skill.getAffectRange(), npc, mostHate, false) && npc.hasSkillChance() && this.cast(skill)) {
                        return;
                     }
                  }

                  int collision = template.getCollisionRadius();
                  int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
                  if (!npc.isMovementDisabled()
                     && Rnd.get(100) <= 3
                     && World.getFirstVisibleObject(npc, Attackable.class, nearby -> npc.isInsideRadius2D(nearby, collision) && nearby != mostHate) != null) {
                     int newX = combinedCollision + Rnd.get(40);
                     newX = Rnd.nextBoolean() ? mostHate.getX() + newX : mostHate.getX() - newX;
                     int newY = combinedCollision + Rnd.get(40);
                     newY = Rnd.nextBoolean() ? mostHate.getY() + newY : mostHate.getY() - newY;
                     if (!npc.isInsideRadius2D(newX, newY, 0, collision)) {
                        int newZ = npc.getZ() + 30;
                        this.moveTo(GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()));
                     }
                  } else {
                     if (!npc.isMovementDisabled() && npc.getAiType() == AIType.ARCHER && Rnd.get(100) < 15) {
                        double distance = npc.calculateDistance2D(mostHate);
                        if (distance <= 60 + combinedCollision) {
                           int posX = npc.getX();
                           int posY = npc.getY();
                           int posZ = npc.getZ() + 30;
                           if (mostHate.getX() < posX) {
                              posX += 300;
                           } else {
                              posX -= 300;
                           }

                           if (mostHate.getY() < posY) {
                              posY += 300;
                           } else {
                              posY -= 300;
                           }

                           if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId())) {
                              this.setIntentionMoveTo(new Location(posX, posY, posZ));
                           }

                           return;
                        }
                     }

                     if (npc.isRaid() || npc.isRaidMinion()) {
                        this._chaosTime++;
                        boolean changeTarget = false;
                        if (npc instanceof RaidBoss && this._chaosTime > NpcConfig.RAID_CHAOS_TIME) {
                           double multiplier = npc.asMonster().hasMinions() ? 200.0 : 100.0;
                           changeTarget = Rnd.get(100) <= 100.0 - npc.getCurrentHp() * multiplier / npc.getMaxHp();
                        } else if (npc instanceof GrandBoss && this._chaosTime > NpcConfig.GRAND_CHAOS_TIME) {
                           double chaosRate = 100.0 - npc.getCurrentHp() * 300.0 / npc.getMaxHp();
                           changeTarget = chaosRate <= 10.0 && Rnd.get(100) <= 10 || chaosRate > 10.0 && Rnd.get(100) <= chaosRate;
                        } else if (this._chaosTime > NpcConfig.MINION_CHAOS_TIME) {
                           changeTarget = Rnd.get(100) <= 100.0 - npc.getCurrentHp() * 200.0 / npc.getMaxHp();
                        }

                        if (changeTarget) {
                           mostHate = this.targetReconsider(true);
                           if (mostHate != null) {
                              this.setAttackTarget(mostHate);
                              this.setTarget(mostHate);
                              this._chaosTime = 0;
                              return;
                           }
                        }
                     }

                     if (mostHate == null) {
                        mostHate = this.targetReconsider(false);
                        if (mostHate == null) {
                           return;
                        }

                        this.setAttackTarget(mostHate);
                        this.setTarget(mostHate);
                     }

                     if (!npc.isMoving() || npc.getAiType() == AIType.MAGE) {
                        List<Skill> generalSkills = template.getAISkills(AISkillScope.GENERAL);
                        if (!generalSkills.isEmpty()) {
                           List<Skill> aiHealSkills = template.getAISkills(AISkillScope.HEAL);
                           if (!aiHealSkills.isEmpty()) {
                              if (npc.isMinion()) {
                                 Creature leader = npc.getLeader();
                                 if (leader != null && !leader.isDead() && Rnd.get(100) > leader.getCurrentHp() / leader.getMaxHp() * 100.0) {
                                    for (Skill healSkill : aiHealSkills) {
                                       if (healSkill.getTargetType() != TargetType.SELF && checkSkillCastConditions(npc, healSkill)) {
                                          if (!LocationUtil.checkIfInRange(
                                                healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius(), npc, leader, false
                                             )
                                             && !this.isParty(healSkill)
                                             && !npc.isMovementDisabled()) {
                                             this.moveToPawn(leader, healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
                                             return;
                                          }

                                          if (GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)leader)) {
                                             this.clientStopMoving(null);
                                             WorldObject target = npc.getTarget();
                                             npc.setTarget(leader);
                                             npc.doCast(healSkill);
                                             npc.setTarget(target);
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }

                              double percentage = npc.getCurrentHp() / npc.getMaxHp() * 100.0;
                              if (Rnd.get(100) < (100.0 - percentage) / 3.0) {
                                 for (Skill sk : aiHealSkills) {
                                    if (checkSkillCastConditions(npc, sk)) {
                                       this.clientStopMoving(null);
                                       WorldObject target = npc.getTarget();
                                       npc.setTarget(npc);
                                       npc.doCast(sk);
                                       npc.setTarget(target);
                                       return;
                                    }
                                 }
                              }

                              for (Skill skx : aiHealSkills) {
                                 if (checkSkillCastConditions(npc, skx)) {
                                    if (skx.getTargetType() == TargetType.ONE) {
                                       Attackable healTarget = World.getFirstVisibleObjectInRange(
                                          npc,
                                          Attackable.class,
                                          skx.getCastRange() + collision,
                                          obj -> obj.isDead()
                                             && obj.isInMyClan(npc)
                                             && Rnd.get(100) < (100.0 - obj.getCurrentHp() / obj.getMaxHp() * 100.0) / 10.0
                                             && GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)obj)
                                       );
                                       if (healTarget != null) {
                                          this.clientStopMoving(null);
                                          WorldObject target = npc.getTarget();
                                          npc.setTarget(healTarget);
                                          npc.doCast(skx);
                                          npc.setTarget(target);
                                          return;
                                       }
                                    }

                                    if (this.isParty(skx)) {
                                       this.clientStopMoving(null);
                                       npc.doCast(skx);
                                       return;
                                    }
                                 }
                              }
                           }

                           List<Skill> aiResSkills = template.getAISkills(AISkillScope.RES);
                           if (!aiResSkills.isEmpty()) {
                              if (npc.isMinion()) {
                                 Creature leader = npc.getLeader();
                                 if (leader != null && leader.isDead()) {
                                    for (Skill skxx : aiResSkills) {
                                       if (skxx.getTargetType() != TargetType.SELF && checkSkillCastConditions(npc, skxx)) {
                                          if (!LocationUtil.checkIfInRange(
                                                skxx.getCastRange() + collision + leader.getTemplate().getCollisionRadius(), npc, leader, false
                                             )
                                             && !this.isParty(skxx)
                                             && !npc.isMovementDisabled()) {
                                             this.moveToPawn(leader, skxx.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
                                             return;
                                          }

                                          if (GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)leader)) {
                                             this.clientStopMoving(null);
                                             WorldObject target = npc.getTarget();
                                             npc.setTarget(leader);
                                             npc.doCast(skxx);
                                             npc.setTarget(target);
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }

                              for (Skill skxxx : aiResSkills) {
                                 if (checkSkillCastConditions(npc, skxxx)) {
                                    if (skxxx.getTargetType() == TargetType.ONE) {
                                       Attackable resTarget = World.getFirstVisibleObjectInRange(
                                          npc,
                                          Attackable.class,
                                          skxxx.getCastRange() + collision,
                                          obj -> obj.isDead()
                                             && npc.isInMyClan(obj)
                                             && Rnd.get(100) < 10
                                             && GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)obj)
                                       );
                                       if (resTarget != null) {
                                          this.clientStopMoving(null);
                                          WorldObject target = npc.getTarget();
                                          npc.setTarget(resTarget);
                                          npc.doCast(skxxx);
                                          npc.setTarget(target);
                                          return;
                                       }
                                    }

                                    if (this.isParty(skxxx)) {
                                       this.clientStopMoving(null);
                                       WorldObject target = npc.getTarget();
                                       npc.setTarget(npc);
                                       npc.doCast(skxxx);
                                       npc.setTarget(target);
                                       return;
                                    }
                                 }
                              }
                           }
                        }

                        List<Skill> shortRangeSkills = npc.getShortRangeSkills();
                        if (!shortRangeSkills.isEmpty() && npc.hasSkillChance() && npc.calculateDistance2D(mostHate) <= 150.0) {
                           Skill shortRangeSkill = shortRangeSkills.get(Rnd.get(shortRangeSkills.size()));
                           int castRange = shortRangeSkill.getCastRange();
                           if ((castRange < 1 || npc.calculateDistance3D(mostHate) < castRange)
                              && checkSkillCastConditions(npc, shortRangeSkill)
                              && this.allowsParameterizedCast(npc, shortRangeSkill, mostHate)) {
                              this.clientStopMoving(null);
                              npc.setTarget(mostHate);
                              npc.doCast(shortRangeSkill);
                              return;
                           }
                        }

                        List<Skill> longRangeSkills = npc.getLongRangeSkills();
                        if (!longRangeSkills.isEmpty() && npc.hasSkillChance()) {
                           Skill longRangeSkill = longRangeSkills.get(Rnd.get(longRangeSkills.size()));
                           int castRange = longRangeSkill.getCastRange();
                           if ((castRange < 1 || npc.calculateDistance3D(mostHate) < castRange)
                              && checkSkillCastConditions(npc, longRangeSkill)
                              && this.allowsParameterizedCast(npc, longRangeSkill, mostHate)) {
                              this.clientStopMoving(null);
                              npc.setTarget(mostHate);
                              npc.doCast(longRangeSkill);
                              return;
                           }
                        }
                     }

                     int range = npc.getPhysicalAttackRange() + combinedCollision;
                     if (npc.isMoving()) {
                        range *= 2;
                     }

                     if (npc.getAiType() == AIType.ARCHER) {
                        range = 850 + combinedCollision;
                     }

                     if (npc.calculateDistance2D(mostHate) > range) {
                        if (this.checkTarget(mostHate)) {
                           this.moveToPawn(mostHate, range);
                           return;
                        }

                        mostHate = this.targetReconsider(false);
                        if (mostHate == null) {
                           return;
                        }

                        this.setAttackTarget(mostHate);
                        this.setTarget(mostHate);
                     }

                     this._actor.doAttack(this.getAttackTarget());
                  }
               }
            }
         }
      }
   }

   private boolean checkTarget(WorldObject target) {
      if (target == null) {
         return false;
      } else {
         Attackable npc = this.getActiveChar();
         if (target.isCreature()) {
            if (target.asCreature().isDead()) {
               return false;
            }

            if (npc.isMovementDisabled()) {
               if (!npc.isInsideRadius2D(
                  target, npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.asCreature().getTemplate().getCollisionRadius()
               )) {
                  return false;
               }

               if (!GeoEngine.getInstance().canSeeTarget(npc, target)) {
                  return false;
               }
            }

            if (!target.isAutoAttackable(npc)) {
               return false;
            }
         }

         return true;
      }
   }

   private Creature targetReconsider(boolean randomTarget) {
      Attackable npc = this.getActiveChar();
      if (randomTarget) {
         List<Creature> result = new ArrayList<>();

         for (AggroInfo aggro : npc.getAggroList().values()) {
            if (this.checkTarget(aggro.getAttacker())) {
               result.add(aggro.getAttacker());
            }
         }

         if (npc.isAggressive()) {
            World.forEachVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), creaturex -> {
               if (this.checkTarget(creaturex)) {
                  result.add(creaturex);
               }
            });
         }

         if (!result.isEmpty()) {
            return result.get(Rnd.get(result.size()));
         }
      }

      long searchValue = Long.MIN_VALUE;
      Creature creature = null;

      for (AggroInfo aggrox : npc.getAggroList().values()) {
         if (this.checkTarget(aggrox.getAttacker()) && aggrox.getHate() > searchValue) {
            searchValue = aggrox.getHate();
            creature = aggrox.getAttacker();
         }
      }

      if (creature == null && npc.isAggressive()) {
         Creature _match = World.getFirstVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), nearby -> this.checkTarget(nearby));
         if (_match != null) {
            return _match;
         }
      }

      return null;
   }

   private boolean cast(Skill sk) {
      if (sk == null) {
         return false;
      } else {
         Attackable caster = this.getActiveChar();
         if (!checkSkillCastConditions(caster, sk)) {
            return false;
         } else if (!this.allowsParameterizedCast(caster, sk, this.getAttackTarget())) {
            return false;
         } else {
            if (this.getAttackTarget() == null && caster.getMostHated() != null) {
               this.setAttackTarget(caster.getMostHated());
            }

            Creature attackTarget = this.getAttackTarget();
            if (attackTarget == null) {
               return false;
            } else {
               double dist = caster.calculateDistance2D(attackTarget);
               double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
               double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
               if (attackTarget.isMoving()) {
                  dist2 -= 30.0;
               }

               if (sk.isContinuous()) {
                  if (!sk.isDebuff()) {
                     if (!caster.isAffectedBySkill(sk.getId())) {
                        this.clientStopMoving(null);
                        caster.setTarget(caster);
                        caster.doCast(sk);
                        this._actor.setTarget(attackTarget);
                        return true;
                     }

                     if (sk.getTargetType() == TargetType.SELF) {
                        return false;
                     }

                     if (sk.getTargetType() == TargetType.ONE) {
                        Creature target = this.effectTargetReconsider(sk, true);
                        if (target != null) {
                           this.clientStopMoving(null);
                           caster.setTarget(target);
                           caster.doCast(sk);
                           caster.setTarget(attackTarget);
                           return true;
                        }
                     }

                     if (this.canParty(sk)) {
                        this.clientStopMoving(null);
                        caster.setTarget(caster);
                        caster.doCast(sk);
                        caster.setTarget(attackTarget);
                        return true;
                     }
                  } else if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                     && !this.canAOE(sk)
                     && !attackTarget.isDead()
                     && dist2 <= srange) {
                     if (!attackTarget.isAffectedBySkill(sk.getId())) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (this.canAOE(sk)) {
                     if (sk.getTargetType() == TargetType.AURA
                        || sk.getTargetType() == TargetType.BEHIND_AURA
                        || sk.getTargetType() == TargetType.FRONT_AURA
                        || sk.getTargetType() == TargetType.AURA_CORPSE_MOB) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     if ((sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (sk.getTargetType() == TargetType.ONE) {
                     Creature target = this.effectTargetReconsider(sk, false);
                     if (target != null) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  }
               }

               if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)) {
                  if (sk.getTargetType() == TargetType.ONE) {
                     if (attackTarget.getEffectList().getFirstEffect(EffectType.BUFF) != null
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     Creature target = this.effectTargetReconsider(sk, false);
                     if (target != null) {
                        this.clientStopMoving(null);
                        caster.setTarget(target);
                        caster.doCast(sk);
                        caster.setTarget(attackTarget);
                        return true;
                     }
                  } else if (this.canAOE(sk)) {
                     if ((sk.getTargetType() == TargetType.AURA || sk.getTargetType() == TargetType.BEHIND_AURA || sk.getTargetType() == TargetType.FRONT_AURA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     if ((sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  }
               }

               if (sk.hasEffectType(EffectType.HEAL)) {
                  if (caster.isMinion() && sk.getTargetType() != TargetType.SELF) {
                     Creature leader = caster.getLeader();
                     if (leader != null && !leader.isDead() && Rnd.get(100) > leader.getCurrentHp() / leader.getMaxHp() * 100.0) {
                        if (!LocationUtil.checkIfInRange(
                              sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius(), caster, leader, false
                           )
                           && !this.isParty(sk)
                           && !caster.isMovementDisabled()) {
                           this.moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
                        }

                        if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)leader)) {
                           this.clientStopMoving(null);
                           caster.setTarget(leader);
                           caster.doCast(sk);
                           caster.setTarget(attackTarget);
                           return true;
                        }
                     }
                  }

                  double percentage = caster.getCurrentHp() / caster.getMaxHp() * 100.0;
                  if (Rnd.get(100) < (100.0 - percentage) / 3.0) {
                     this.clientStopMoving(null);
                     caster.setTarget(caster);
                     caster.doCast(sk);
                     caster.setTarget(attackTarget);
                     return true;
                  }

                  if (sk.getTargetType() == TargetType.ONE) {
                     Attackable healTarget = World.getFirstVisibleObjectInRange(
                        caster,
                        Attackable.class,
                        sk.getCastRange() + caster.getTemplate().getCollisionRadius(),
                        obj -> !obj.isDead()
                           && caster.isInMyClan(obj)
                           && Rnd.get(100) < (100.0 - obj.getCurrentHp() / obj.getMaxHp() * 100.0) / 10.0
                           && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)obj)
                     );
                     if (healTarget != null) {
                        this.clientStopMoving(null);
                        caster.setTarget(healTarget);
                        caster.doCast(sk);
                        caster.setTarget(attackTarget);
                        return true;
                     }
                  }

                  if (this.isParty(sk)
                     && World.getFirstVisibleObjectInRange(
                           caster,
                           Attackable.class,
                           sk.getAffectRange() + caster.getTemplate().getCollisionRadius(),
                           obj -> obj.isInMyClan(caster) && obj.getCurrentHp() < obj.getMaxHp() && Rnd.get(100) <= 20
                        )
                        != null) {
                     this.clientStopMoving(null);
                     caster.setTarget(caster);
                     caster.doCast(sk);
                     caster.setTarget(attackTarget);
                     return true;
                  }
               }

               if (sk.hasEffectType(
                  EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK, EffectType.MAGICAL_ATTACK, EffectType.DEATH_LINK, EffectType.HP_DRAIN
               )) {
                  if (this.canAura(sk)) {
                     this.clientStopMoving(null);
                     caster.doCast(sk);
                     return true;
                  }

                  if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget) && !attackTarget.isDead() && dist2 <= srange) {
                     this.clientStopMoving(null);
                     caster.doCast(sk);
                     return true;
                  }

                  Creature target = this.skillTargetReconsider(sk);
                  if (target != null) {
                     this.clientStopMoving(null);
                     caster.setTarget(target);
                     caster.doCast(sk);
                     caster.setTarget(attackTarget);
                     return true;
                  }
               }

               if (sk.hasEffectType(EffectType.SLEEP)) {
                  if (sk.getTargetType() == TargetType.ONE) {
                     double range = caster.getPhysicalAttackRange()
                        + caster.getTemplate().getCollisionRadius()
                        + attackTarget.getTemplate().getCollisionRadius();
                     if (!attackTarget.isDead() && dist2 <= srange && (dist2 > range || attackTarget.isMoving()) && !attackTarget.isAffectedBySkill(sk.getId())
                        )
                      {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     Creature target = this.effectTargetReconsider(sk, false);
                     if (target != null) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (this.canAOE(sk)) {
                     if (sk.getTargetType() == TargetType.AURA || sk.getTargetType() == TargetType.BEHIND_AURA || sk.getTargetType() == TargetType.FRONT_AURA) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     if ((sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  }
               }

               if (sk.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE, EffectType.MUTE, EffectType.FEAR)) {
                  if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget) && !this.canAOE(sk) && dist2 <= srange) {
                     if (!attackTarget.isAffectedBySkill(sk.getId())) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (this.canAOE(sk)) {
                     if (sk.getTargetType() == TargetType.AURA || sk.getTargetType() == TargetType.BEHIND_AURA || sk.getTargetType() == TargetType.FRONT_AURA) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     if ((sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (sk.getTargetType() == TargetType.ONE) {
                     Creature target = this.effectTargetReconsider(sk, false);
                     if (target != null) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  }
               }

               if (sk.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT)) {
                  if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget) && !this.canAOE(sk) && !attackTarget.isDead() && dist2 <= srange) {
                     if (!attackTarget.isAffectedBySkill(sk.getId())) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (this.canAOE(sk)) {
                     if (sk.getTargetType() == TargetType.AURA
                        || sk.getTargetType() == TargetType.BEHIND_AURA
                        || sk.getTargetType() == TargetType.FRONT_AURA
                        || sk.getTargetType() == TargetType.AURA_CORPSE_MOB) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }

                     if ((sk.getTargetType() == TargetType.AREA || sk.getTargetType() == TargetType.BEHIND_AREA || sk.getTargetType() == TargetType.FRONT_AREA)
                        && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget)
                        && !attackTarget.isDead()
                        && dist2 <= srange) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  } else if (sk.getTargetType() == TargetType.ONE) {
                     Creature target = this.effectTargetReconsider(sk, false);
                     if (target != null) {
                        this.clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                     }
                  }
               }

               if (sk.hasEffectType(EffectType.RESURRECTION)) {
                  if (!this.isParty(sk)) {
                     if (caster.isMinion() && sk.getTargetType() != TargetType.SELF) {
                        Creature leader = caster.getLeader();
                        if (leader != null) {
                           if (leader.isDead()
                              && !LocationUtil.checkIfInRange(
                                 sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius(),
                                 caster,
                                 leader,
                                 false
                              )
                              && !this.isParty(sk)
                              && !caster.isMovementDisabled()) {
                              this.moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
                           }

                           if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)leader)) {
                              this.clientStopMoving(null);
                              caster.setTarget(leader);
                              caster.doCast(sk);
                              caster.setTarget(attackTarget);
                              return true;
                           }
                        }
                     }

                     Attackable resTarget = World.getFirstVisibleObjectInRange(
                        caster,
                        Attackable.class,
                        sk.getCastRange() + caster.getTemplate().getCollisionRadius(),
                        obj -> obj.isDead() && caster.isInMyClan(obj) && Rnd.get(100) < 10 && GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)obj)
                     );
                     if (resTarget != null) {
                        this.clientStopMoving(null);
                        caster.setTarget(resTarget);
                        caster.doCast(sk);
                        caster.setTarget(attackTarget);
                        return true;
                     }
                  } else if (this.isParty(sk)
                     && World.getFirstVisibleObjectInRange(
                           caster,
                           Npc.class,
                           sk.getAffectRange() + caster.getTemplate().getCollisionRadius(),
                           obj -> caster.isInMyClan(obj) && obj.getCurrentHp() < obj.getMaxHp() && Rnd.get(100) <= 20
                        )
                        != null) {
                     this.clientStopMoving(null);
                     caster.setTarget(caster);
                     caster.doCast(sk);
                     caster.setTarget(attackTarget);
                     return true;
                  }
               }

               if (this.canAura(sk)) {
                  this.clientStopMoving(null);
                  caster.doCast(sk);
                  return true;
               } else if (GeoEngine.getInstance().canSeeTarget(caster, (WorldObject)attackTarget) && !attackTarget.isDead() && dist2 <= srange) {
                  this.clientStopMoving(null);
                  caster.doCast(sk);
                  return true;
               } else {
                  Creature target = this.skillTargetReconsider(sk);
                  if (target != null) {
                     this.clientStopMoving(null);
                     caster.setTarget(target);
                     caster.doCast(sk);
                     caster.setTarget(attackTarget);
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         }
      }
   }

   private void movementDisable() {
      Creature target = this.getAttackTarget();
      if (target != null) {
         Attackable npc = this.getActiveChar();
         if (npc.getTarget() == null) {
            npc.setTarget(target);
         }

         double dist = npc.calculateDistance2D(target);
         int random = Rnd.get(100);
         if (target.isImmobilized() || random >= 15 || !this.tryCast(npc, target, AISkillScope.IMMOBILIZE, dist)) {
            if (random >= 20 || !this.tryCast(npc, target, AISkillScope.COT, dist)) {
               if (random >= 30 || !this.tryCast(npc, target, AISkillScope.DEBUFF, dist)) {
                  if (random >= 40 || !this.tryCast(npc, target, AISkillScope.NEGATIVE, dist)) {
                     if (!npc.isMovementDisabled() && npc.getAiType() != AIType.MAGE && npc.getAiType() != AIType.HEALER
                        || !this.tryCast(npc, target, AISkillScope.ATTACK, dist)) {
                        if (!this.tryCast(npc, target, AISkillScope.UNIVERSAL, dist)) {
                           int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
                           if (dist <= range && GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)target)) {
                              this._actor.doAttack(target);
                           } else {
                              this.targetReconsider();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryCast(Attackable npc, Creature target, AISkillScope aiSkillScope, double dist) {
      for (Skill sk : npc.getTemplate().getAISkills(aiSkillScope)) {
         if (checkSkillCastConditions(npc, sk)
            && (!(sk.getCastRange() + target.getTemplate().getCollisionRadius() <= dist) || this.canAura(sk))
            && GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)target)
            && this.allowsParameterizedCast(npc, sk, target)) {
            this.clientStopMoving(null);
            npc.doCast(sk);
            return true;
         }
      }

      return false;
   }

   private static boolean checkSkillCastConditions(Attackable caster, Skill skill) {
      if (caster.isCastingNow() && !skill.isSimultaneousCast()) {
         return false;
      } else if (skill.getMpConsume() >= caster.getCurrentMp()) {
         return false;
      } else {
         return caster.isSkillDisabled(skill) ? false : skill.isStatic() || (!skill.isMagic() || !caster.isMuted()) && !caster.isPhysicalMuted();
      }
   }

   private Creature effectTargetReconsider(Skill sk, boolean positive) {
      if (sk == null) {
         return null;
      } else {
         Attackable actor = this.getActiveChar();
         Creature attackTarget = this.getAttackTarget();
         if (actor != null && attackTarget != null) {
            if (!sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)) {
               if (!positive) {
                  double dist = 0.0;
                  double dist2 = 0.0;
                  int range = 0;
                  Iterator found = actor.getAttackByList().iterator();

                  while (true) {
                     Creature obj;
                     while (true) {
                        if (!found.hasNext()) {
                           Creature foundx = World.getFirstVisibleObjectInRange(actor, Creature.class, range, objx -> {
                              if (!objx.isDead() && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)objx)) {
                                 try {
                                    actor.setTarget(attackTarget);
                                    double localDist = actor.calculateDistance2D(objx);
                                    int localRange = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + objx.getTemplate().getCollisionRadius();
                                    double localDist2 = objx.isMoving() ? localDist - 70.0 : localDist;
                                    return (objx.isPlayer() || objx.isSummon()) && localDist2 <= localRange && !attackTarget.isAffectedBySkill(sk.getId());
                                 } catch (NullPointerException var9x) {
                                    return false;
                                 }
                              } else {
                                 return false;
                              }
                           });
                           if (foundx != null) {
                              return foundx;
                           }

                           return null;
                        }

                        obj = (Creature)found.next();
                        if (obj != null && !obj.isDead() && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)obj) && obj != attackTarget) {
                           try {
                              actor.setTarget(attackTarget);
                              dist = actor.calculateDistance2D(obj);
                              dist2 = dist - actor.getTemplate().getCollisionRadius();
                              range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
                              if (obj.isMoving()) {
                                 dist2 -= 70.0;
                              }
                              break;
                           } catch (NullPointerException var13) {
                           }
                        }
                     }

                     if (dist2 <= range && !attackTarget.isAffectedBySkill(sk.getId())) {
                        return obj;
                     }
                  }
               } else {
                  int range = sk.getCastRange() + actor.getTemplate().getCollisionRadius();
                  Attackable foundClan = World.getFirstVisibleObjectInRange(actor, Attackable.class, range, targets -> {
                     if (!targets.isDead() && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)targets) && targets.isInMyClan(actor)) {
                        try {
                           actor.setTarget(attackTarget);
                           double localDist = actor.calculateDistance2D(targets);
                           int localRange = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + targets.getTemplate().getCollisionRadius();
                           double localDist2Base = localDist - actor.getTemplate().getCollisionRadius();
                           double localDist2 = targets.isMoving() ? localDist2Base - 70.0 : localDist2Base;
                           return localDist2 <= localRange && !targets.isAffectedBySkill(sk.getId());
                        } catch (NullPointerException var11x) {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  });
                  if (foundClan != null) {
                     return foundClan;
                  }
               }
            } else {
               int range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
               Creature foundBuff = World.getFirstVisibleObjectInRange(
                  actor,
                  Creature.class,
                  range,
                  objx -> {
                     if (!objx.isDead() && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)objx)) {
                        try {
                           actor.setTarget(attackTarget);
                           double localDist = actor.calculateDistance2D(objx);
                           int localRange = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + objx.getTemplate().getCollisionRadius();
                           double localDist2Base = localDist - actor.getTemplate().getCollisionRadius();
                           double localDist2 = objx.isMoving() ? localDist2Base - 70.0 : localDist2Base;
                           return (objx.isPlayer() || objx.isSummon())
                              && localDist2 <= localRange
                              && attackTarget.getEffectList().getFirstEffect(EffectType.BUFF) != null;
                        } catch (NullPointerException var11x) {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  }
               );
               if (foundBuff != null) {
                  return foundBuff;
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   private Creature skillTargetReconsider(Skill sk) {
      double dist = 0.0;
      double dist2 = 0.0;
      int range = 0;
      Attackable actor = this.getActiveChar();
      if (actor.getHateList() != null) {
         for (Creature obj : actor.getHateList()) {
            if (obj != null && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)obj) && !obj.isDead()) {
               try {
                  actor.setTarget(this.getAttackTarget());
                  dist = actor.calculateDistance2D(obj);
                  dist2 = dist - actor.getTemplate().getCollisionRadius();
                  range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + this.getAttackTarget().getTemplate().getCollisionRadius();
               } catch (NullPointerException var11) {
                  continue;
               }

               if (dist2 <= range) {
                  return obj;
               }
            }
         }
      }

      if (!(actor instanceof Guard)) {
         Creature foundWorld = World.getFirstVisibleObject(actor, Creature.class, objx -> {
            try {
               actor.setTarget(this.getAttackTarget());
               double localDist = actor.calculateDistance2D(objx);
               int localRange = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + this.getAttackTarget().getTemplate().getCollisionRadius();
               if (localDist > localRange) {
                  return false;
               }
            } catch (NullPointerException var7x) {
               return false;
            }

            if (!GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)objx)) {
               return false;
            } else if (objx.isPlayer()) {
               return true;
            } else {
               return objx.isAttackable() && actor.isChaos() ? !objx.asAttackable().isInMyClan(actor) : objx.isSummon();
            }
         });
         if (foundWorld != null) {
            return foundWorld;
         }
      }

      return null;
   }

   private void targetReconsider() {
      double dist = 0.0;
      double dist2 = 0.0;
      int range = 0;
      Attackable actor = this.getActiveChar();
      Creature mostHate = actor.getMostHated();
      if (actor.getHateList() != null) {
         Iterator var8 = actor.getHateList().iterator();

         label62:
         while (true) {
            Creature obj;
            while (true) {
               if (!var8.hasNext()) {
                  break label62;
               }

               obj = (Creature)var8.next();
               if (obj != null && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)obj) && !obj.isDead() && obj == mostHate && obj != actor) {
                  try {
                     dist = actor.calculateDistance2D(obj);
                     dist2 = dist - actor.getTemplate().getCollisionRadius();
                     range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
                     if (obj.isMoving()) {
                        dist2 -= 70.0;
                     }
                     break;
                  } catch (NullPointerException var11) {
                  }
               }
            }

            if (dist2 <= range) {
               actor.addDamageHate(obj, 0L, mostHate != null ? actor.getHating(mostHate) : 2000L);
               actor.setTarget(obj);
               this.setAttackTarget(obj);
               return;
            }
         }
      }

      if (!(actor instanceof Guard)) {
         World.forEachVisibleObject(
            actor,
            Creature.class,
            objx -> {
               if (objx != null
                  && GeoEngine.getInstance().canSeeTarget(actor, (WorldObject)objx)
                  && !objx.isDead()
                  && objx == mostHate
                  && objx != actor
                  && objx != this.getAttackTarget()) {
                  if (objx.isPlayer()) {
                     actor.addDamageHate(objx, 0L, mostHate != null ? actor.getHating(mostHate) : 2000L);
                     actor.setTarget(objx);
                     this.setAttackTarget(objx);
                  } else if (objx.isAttackable()) {
                     if (actor.isChaos()) {
                        if (objx.asAttackable().isInMyClan(actor)) {
                           return;
                        }

                        actor.addDamageHate(objx, 0L, mostHate != null ? actor.getHating(mostHate) : 2000L);
                        actor.setTarget(objx);
                        this.setAttackTarget(objx);
                     }
                  } else if (objx.isSummon()) {
                     actor.addDamageHate(objx, 0L, mostHate != null ? actor.getHating(mostHate) : 2000L);
                     actor.setTarget(objx);
                     this.setAttackTarget(objx);
                  }
               }
            }
         );
      }
   }

   @Override
   public void notifyActionThink() {
      if (!this._thinking) {
         WorldRegion region = this._actor.getWorldRegion();
         if (region != null && region.areNeighborsActive()) {
            if (!this.getActiveChar().isAllSkillsDisabled()) {
               this._thinking = true;

               try {
                  this.checkMinionLeash();
                  switch (this.getIntention()) {
                     case ACTIVE:
                        this.thinkActive();
                        break;
                     case ATTACK:
                        this.thinkAttack();
                        break;
                     case CAST:
                        this.thinkCast();
                  }
               } catch (Exception var6) {
               } finally {
                  this._thinking = false;
               }
            }
         }
      }
   }

   private void checkMinionLeash() {
      Attackable npc = this.getActiveChar();
      if (npc.isRaidMinion() && !npc.isDead() && !npc.isMoving()) {
         long currentTime = System.currentTimeMillis();
         if (this._minionLeashTime == 0L) {
            this._minionLeashTime = currentTime + 120000L;
         } else if (this._minionLeashTime <= currentTime) {
            this._minionLeashTime = currentTime + 120000L;
            Attackable master = npc.getLeader();
            if (master != null && !master.isDead() && !this.isInsideMasterTerritory(npc, master)) {
               npc.clearAggroList();
               npc.getAttackByList().clear();
               npc.teleToLocation(master, 100);
            }
         }
      }
   }

   private boolean isInsideMasterTerritory(Attackable npc, Attackable master) {
      Spawn masterSpawn = master.getSpawn();
      if (masterSpawn != null) {
         NpcSpawnTerritory territory = masterSpawn.getSpawnTerritory();
         if (territory != null) {
            return territory.isInsideZone(npc.getX(), npc.getY(), npc.getZ());
         }
      }

      BossZone bossZone = ZoneManager.getInstance().getZone(master, BossZone.class);
      return bossZone != null ? bossZone.isInsideZone(npc.getX(), npc.getY(), npc.getZ()) : true;
   }

   @Override
   public void notifyActionAttacked(WorldObject attackerObj) {
      if (attackerObj != null) {
         Creature attacker = attackerObj.asCreature();
         if (attacker != null) {
            Attackable me = this.getActiveChar();
            this._attackTimeout = 1200 + GameTimeTaskManager.getInstance().getGameTicks();
            if (this._globalAggro < 0) {
               this._globalAggro = 0;
            }

            if (!me.isInAggroList(attacker)) {
               me.addDamageHate(attacker, 0L, 1L);
            }

            if (!me.isRunning()) {
               me.setRunning();
            }

            if (!me.isCoreAIDisabled()) {
               if (this.getIntention() != Intention.ATTACK) {
                  this.setIntentionAttack(attacker);
               } else if (me.getMostHated() != this.getAttackTarget()) {
                  this.setIntentionAttack(attacker);
               }
            }

            if (me.isMonster()) {
               Monster master = me.asMonster();
               if (master.hasMinions()) {
                  master.getMinionList().onAssist(me, attacker);
               }

               master = master.getLeader();
               if (master != null && master.hasMinions()) {
                  master.getMinionList().onAssist(me, attacker);
               }
            }

            NpcTemplate template = me.getTemplate();
            Set<Integer> clans = template.getClans();
            if (clans != null && !clans.isEmpty()) {
               int collision = template.getCollisionRadius();
               int factionRange = template.getClanHelpRange() + collision;

               try {
                  Creature finalTarget = attacker;
                  boolean targetExistsInAttackByList = false;

                  for (Creature reference : me.getAttackByList()) {
                     if (reference == finalTarget) {
                        targetExistsInAttackByList = true;
                        break;
                     }
                  }

                  if (targetExistsInAttackByList) {
                     World.forEachVisibleObjectInRange(
                        me,
                        Attackable.class,
                        factionRange,
                        nearby -> {
                           if (!nearby.isDead() && nearby.hasAI() && Math.abs(finalTarget.getZ() - nearby.getZ()) <= 600) {
                              if (System.currentTimeMillis() - nearby.getSpawnTime() >= 7000L) {
                                 if (nearby.getAI()._intention == Intention.IDLE || nearby.getAI()._intention == Intention.ACTIVE) {
                                    NpcTemplate nearbytemplate = nearby.getTemplate();
                                    if (template.isClan(nearbytemplate.getClans())
                                       && (!nearbytemplate.hasIgnoreClanNpcIds() || !nearbytemplate.getIgnoreClanNpcIds().contains(me.getId()))) {
                                       if (finalTarget.isPlayable()) {
                                          if (me instanceof RiftInvader && finalTarget.isInParty()) {
                                             Party party = finalTarget.getParty();
                                             if (party.isInDimensionalRift()) {
                                                byte riftType = party.getDimensionalRift().getType();
                                                byte riftRoom = party.getDimensionalRift().getCurrentRoom();
                                                if (!DimensionalRiftManager.getInstance()
                                                   .getRoom(riftType, riftRoom)
                                                   .checkIfInZone(me.getX(), me.getY(), me.getZ())) {
                                                   return;
                                                }
                                             }
                                          }

                                          if (GeoEngine.getInstance().canSeeTarget(nearby, (WorldObject)finalTarget)) {
                                             nearby.getAI().notifyActionAggression(finalTarget, 1);
                                          }

                                          if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby)) {
                                             EventDispatcher.getInstance()
                                                .notifyEventAsync(
                                                   new OnAttackableFactionCall(nearby, me, finalTarget.asPlayer(), finalTarget.isSummon()), nearby
                                                );
                                          }
                                       } else if (nearby.getAI()._intention != Intention.ATTACK
                                          && GeoEngine.getInstance().canSeeTarget(nearby, (WorldObject)finalTarget)) {
                                          nearby.addDamageHate(finalTarget, 0L, me.getHating(finalTarget));
                                          nearby.getAI().setIntentionAttack(finalTarget);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     );
                  }
               } catch (NullPointerException var12) {
               }
            }

            super.notifyActionAttacked(attacker);
         }
      }
   }

   @Override
   public void notifyActionAggression(WorldObject targetObj, int aggro) {
      Attackable me = this.getActiveChar();
      if (targetObj != null && !me.isDead()) {
         Creature target = targetObj.asCreature();
         if (target != null) {
            me.addDamageHate(target, 0L, aggro);
            if (this.getIntention() != Intention.ATTACK) {
               if (!me.isRunning()) {
                  me.setRunning();
               }

               this.setIntentionAttack(target);
            }

            if (me.isMonster()) {
               Monster master = me.asMonster();
               if (master.hasMinions()) {
                  master.getMinionList().onAssist(me, target);
               }

               master = master.getLeader();
               if (master != null && master.hasMinions()) {
                  master.getMinionList().onAssist(me, target);
               }
            }
         }
      }
   }

   public void rollGlobalAggro() {
      int aggressiveTime = this.getActiveChar().getTemplate().getParameters().getInt("SetAggressiveTime", -1);
      if (aggressiveTime == 0) {
         this._globalAggro = 0;
      } else if (aggressiveTime > 0) {
         this._globalAggro = -(aggressiveTime + 1);
      } else {
         this._globalAggro = -(Rnd.get(5) + 4);
      }
   }

   public void setGlobalAggro(int value) {
      this._globalAggro = value;
   }

   public Attackable getActiveChar() {
      return this._actor.asAttackable();
   }

   public int getFearTime() {
      return this._fearTime;
   }

   public void setFearTime(int fearTime) {
      this._fearTime = fearTime;
   }

   private boolean allowsParameterizedCast(Attackable caster, Skill skill, Creature target) {
      if (caster != null && skill != null) {
         StatSet params = caster.getTemplate() != null ? caster.getTemplate().getParameters() : null;
         if (params != null && !params.isEmpty()) {
            int slot = this.findParameterizedSkillSlot(params, skill.getId());
            return slot == 0 ? true : this.passesParameterizedGates(slot, params, caster, target);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private int findParameterizedSkillSlot(StatSet params, int skillId) {
      for (int slot = 1; slot <= 6; slot++) {
         SkillHolder declared = params.getObject("Skill0" + slot + "_ID", SkillHolder.class);
         if (declared != null && declared.getSkillId() == skillId) {
            return slot;
         }
      }

      return 0;
   }

   private boolean passesParameterizedGates(int slot, StatSet params, Attackable caster, Creature target) {
      String prefix = "Skill0" + slot + "_";
      int probability = params.getInt(prefix + "Probablity", params.getInt(prefix + "Probability", params.getInt(prefix + "Prob", 0)));
      boolean checksDistance = params.getInt(prefix + "Check_Dist", 0) == 1;
      int highHp = params.getInt(prefix + "HighHP", 0);
      if (probability == 0) {
         boolean hasAttackHint = params.getInt(prefix + "AttackSplash", 0) == 1
            || params.getInt(prefix + "MainAttack", 0) == 1
            || params.getInt(prefix + "Target", 0) > 0;
         if (!checksDistance && highHp == 0 && !hasAttackHint) {
            return false;
         }
      } else if (Rnd.get(10000) >= probability) {
         return false;
      }

      if (checksDistance && target != null) {
         int distMin = params.getInt(prefix + "Dist_Min", 0);
         int distMax = params.getInt(prefix + "Dist_Max", Integer.MAX_VALUE);
         int distance = (int)caster.calculateDistance2D(target);
         if (distance < distMin || distance > distMax) {
            return false;
         }
      }

      if (highHp > 0) {
         int hpTarget = params.getInt(prefix + "HPTarget", 0);
         Creature hpSource = (Creature)(hpTarget == 1 ? caster : target);
         if (hpSource == null) {
            return false;
         }

         double hpPercent = hpSource.getCurrentHp() * 100.0 / hpSource.getMaxHp();
         if (hpPercent <= highHp) {
            return false;
         }
      }

      return true;
   }
}
