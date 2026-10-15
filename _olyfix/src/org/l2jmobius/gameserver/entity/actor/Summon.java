package org.l2jmobius.gameserver.entity.actor;

import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.stat.SummonStat;
import org.l2jmobius.gameserver.entity.actor.status.SummonStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.pathfinding.PathFinding;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonSpawn;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetDelete;
import org.l2jmobius.gameserver.network.serverpackets.PetInfo;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.network.serverpackets.PetStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.util.ArrayUtil;

public abstract class Summon extends Playable {
   private Player _owner;
   private int _attackRange = 36;
   private boolean _follow = true;
   private boolean _previousFollowStatus = true;
   protected boolean _restoreSummon = true;
   private int _shotsMask = 0;
   private ScheduledFuture<?> _abnormalEffectTask;
   private static final int[] PASSIVE_SUMMONS = new int[]{
      12564,
      12621,
      14702,
      14703,
      14704,
      14705,
      14706,
      14707,
      14708,
      14709,
      14710,
      14711,
      14712,
      14713,
      14714,
      14715,
      14716,
      14717,
      14718,
      14719,
      14720,
      14721,
      14722,
      14723,
      14724,
      14725,
      14726,
      14727,
      14728,
      14729,
      14730,
      14731,
      14732,
      14733,
      14734,
      14735,
      14736
   };

   public Summon(NpcTemplate template, Player owner) {
      super(template);
      this.setInstanceType(InstanceType.Summon);
      this.setInstanceId(owner.getInstanceId());
      this.setShowSummonAnimation(true);
      this._owner = owner;
      this.getAI();
      int x = owner.getX();
      int y = owner.getY();
      int z = owner.getZ();
      Location location = GeoEngine.getInstance().getValidLocation(x, y, z, x + Rnd.get(-100, 100), y + Rnd.get(-100, 100), z, owner.getInstanceId());
      this.setXYZInvisible(location.getX(), location.getY(), location.getZ());
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      if (PlayerConfig.SUMMON_STORE_SKILL_COOLTIME && !this.isTeleporting()) {
         this.restoreEffects();
      }

      this.setFollowStatus(true);
      this.updateAndBroadcastStatus(0);
      this.sendPacket(new RelationChanged(this, this._owner.getRelation(this._owner), false));
      World.forEachVisibleObjectInRange(
         this.getOwner(),
         Player.class,
         800,
         player -> player.sendPacket(new RelationChanged(this, this._owner.getRelation(player), this.isAutoAttackable(player)))
      );
      Party party = this._owner.getParty();
      if (party != null) {
         party.broadcastToPartyMembers(this._owner, new ExPartyPetWindowAdd(this));
      }

      this.setShowSummonAnimation(false);
      this._restoreSummon = false;
      this.rechargeShots(true, true);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_SPAWN, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonSpawn(this), this);
      }
   }

   public SummonStat getStat() {
      return (SummonStat)super.getStat();
   }

   @Override
   public void initCharStat() {
      this.setStat(new SummonStat(this));
   }

   public SummonStatus getStatus() {
      return (SummonStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new SummonStatus(this));
   }

   @Override
   protected CreatureAI initAI() {
      return new SummonAI(this);
   }

   public NpcTemplate getTemplate() {
      return (NpcTemplate)super.getTemplate();
   }

   public abstract int getSummonType();

   @Override
   public void stopAllEffects() {
      super.stopAllEffects();
      this.updateAndBroadcastStatus(1);
   }

   @Override
   public void stopAllEffectsExceptThoseThatLastThroughDeath() {
      super.stopAllEffectsExceptThoseThatLastThroughDeath();
      this.updateAndBroadcastStatus(1);
   }

   @Override
   public void updateAbnormalEffect() {
      if (this._abnormalEffectTask == null) {
         this._abnormalEffectTask = ThreadPool.schedule(() -> {
            if (this.isSpawned()) {
               World.forEachVisibleObject(this, Player.class, player -> player.sendPacket(new AbstractNpcInfo.SummonInfo(this, player, 1)));
            }

            this._abnormalEffectTask = null;
         }, 50L);
      }
   }

   public boolean isMountable() {
      return false;
   }

   public long getExpForThisLevel() {
      return this.getLevel() >= ExperienceData.getInstance().getMaxPetLevel() ? 0L : ExperienceData.getInstance().getExpForLevel(this.getLevel());
   }

   public long getExpForNextLevel() {
      return this.getLevel() >= ExperienceData.getInstance().getMaxPetLevel() - 1 ? 0L : ExperienceData.getInstance().getExpForLevel(this.getLevel() + 1);
   }

   @Override
   public int getKarma() {
      return this._owner != null ? this._owner.getKarma() : 0;
   }

   @Override
   public byte getPvpFlag() {
      return this._owner != null ? this._owner.getPvpFlag() : 0;
   }

   @Override
   public Team getTeam() {
      return this._owner != null ? this._owner.getTeam() : Team.NONE;
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   public int getId() {
      return this.getTemplate().getId();
   }

   public short getSoulShotsPerHit() {
      return this.getTemplate().getSoulShot() > 0 ? (short)this.getTemplate().getSoulShot() : 1;
   }

   public short getSpiritShotsPerHit() {
      return this.getTemplate().getSpiritShot() > 0 ? (short)this.getTemplate().getSpiritShot() : 1;
   }

   public void followOwner() {
      this.setFollowStatus(true);
   }

   @Override
   public boolean doDie(Creature killer) {
      if (this.isNoblesseBlessedAffected()) {
         this.stopEffects(EffectType.NOBLESSE_BLESSING);
         this.storeEffect(true);
      } else {
         this.storeEffect(false);
      }

      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this._owner != null) {
            World.forEachVisibleObject(this, Attackable.class, targetMob -> {
               if (!targetMob.isDead()) {
                  AggroInfo info = targetMob.getAggroList().get(this);
                  if (info != null) {
                     targetMob.addDamageHate(this._owner, info.getDamage(), info.getHate());
                  }
               }
            });
         }

         this.addKillerHate(killer);
         DecayTaskManager.getInstance().add(this);
         return true;
      }
   }

   public boolean doDie(Creature killer, boolean decayed) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         this.addKillerHate(killer);
         if (!decayed) {
            DecayTaskManager.getInstance().add(this);
         }

         return true;
      }
   }

   private void addKillerHate(Creature killer) {
      if (this._owner != null && killer != null && killer.isAttackable() && !killer.isDead()) {
         killer.asAttackable().addDamageHate(this._owner, 0L, 500L);
      }
   }

   public void stopDecay() {
      DecayTaskManager.getInstance().cancel(this);
   }

   @Override
   public void onDecay() {
      this.deleteMe(this._owner);
   }

   @Override
   public void broadcastStatusUpdate() {
      super.broadcastStatusUpdate();
      this.updateAndBroadcastStatus(1);
   }

   public void deleteMe(Player owner) {
      if (owner != null) {
         owner.sendPacket(new PetDelete(this.getSummonType(), this.getObjectId()));
         Party party = owner.getParty();
         if (party != null) {
            party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
         }
      }

      if (this.getInventory() != null) {
         this.getInventory().destroyAllItems(ItemProcessType.DESTROY, this._owner, this);
      }

      this.decayMe();
      if (owner != null) {
         owner.setPet(null);
      }

      super.deleteMe();
   }

   public void unSummon(Player owner) {
      if (this.isSpawned() && !this.isDead()) {
         this.stopHpMpRegeneration();
         this.getAI().stopFollow();
         if (owner != null) {
            owner.sendPacket(new PetDelete(this.getSummonType(), this.getObjectId()));
            Party party = owner.getParty();
            if (party != null) {
               party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
            }

            if (this.getInventory() != null && this.getInventory().getSize() > 0) {
               this._owner.setPetInvItems(true);
               this.sendPacket(
                  SystemMessageId.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEMS_PLEASE_EMPTY_YOUR_PET_INVENTORY
               );
            } else {
               this._owner.setPetInvItems(false);
            }
         }

         this.abortAttack();
         this.abortCast();
         this.storeMe();
         this.storeEffect(true);
         if (owner != null) {
            owner.setPet(null);
         }

         if (this.hasAI()) {
            this.getAI().stopAITask();
         }

         this.stopAllEffects();
         ZoneRegion oldRegion = ZoneManager.getInstance().getRegion(this);
         this.decayMe();
         oldRegion.removeFromZones(this);
         this.setTarget(null);
         if (owner != null) {
            for (int itemId : owner.getAutoSoulShot()) {
               String handler = ((EtcItem)ItemData.getInstance().getTemplate(itemId)).getHandlerName();
               if (handler != null && handler.contains("Beast")) {
                  owner.disableAutoShot(itemId);
               }
            }
         }
      }
   }

   public int getAttackRange() {
      return this._attackRange;
   }

   public void setAttackRange(int range) {
      this._attackRange = range < 36 ? 36 : range;
   }

   public void setFollowStatus(boolean value) {
      this._follow = value;
      if (this._follow) {
         this.getAI().setIntentionFollow(this._owner);
      } else {
         this.getAI().setIntentionIdle();
      }
   }

   public boolean getFollowStatus() {
      return this._follow;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return this._owner != null && this._owner.isAutoAttackable(attacker);
   }

   public int getControlObjectId() {
      return 0;
   }

   public Weapon getActiveWeapon() {
      return null;
   }

   public PetInventory getInventory() {
      return null;
   }

   public void setRestoreSummon(boolean value) {
   }

   @Override
   public Item getActiveWeaponInstance() {
      return null;
   }

   @Override
   public Weapon getActiveWeaponItem() {
      return null;
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return null;
   }

   public Weapon getSecondaryWeaponItem() {
      return null;
   }

   @Override
   public boolean isInvul() {
      return super.isInvul() || this._owner.isSpawnProtected();
   }

   @Override
   public Party getParty() {
      return this._owner == null ? null : this._owner.getParty();
   }

   @Override
   public boolean isInParty() {
      return this._owner != null && this._owner.isInParty();
   }

   @Override
   public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove) {
      if (skill == null || this.isDead() || this._owner == null) {
         return false;
      } else if (skill.isPassive()) {
         return false;
      } else if (this.isCastingNow()) {
         return false;
      } else {
         this._owner.setCurrentPetSkill(skill, forceUse, dontMove);
         WorldObject target = null;

         target = (WorldObject)(switch (skill.getTargetType()) {
            case OWNER_PET -> this._owner;
            case PARTY, AURA, FRONT_AURA, BEHIND_AURA, SELF, AURA_CORPSE_MOB, COMMAND_CHANNEL -> this;
            default -> skill.getFirstOfTargetList(this);
         });
         if (target == null) {
            this.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
            return false;
         } else if (this.isSkillDisabled(skill)) {
            this.sendPacket(SystemMessageId.THAT_PET_SERVITOR_SKILL_CANNOT_BE_USED_BECAUSE_IT_IS_RECHARGING);
            return false;
         } else if (this.getCurrentMp() < this.getStat().getMpConsume(skill) + this.getStat().getMpInitialConsume(skill)) {
            this.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
            return false;
         } else if (this.getCurrentHp() <= skill.getHpConsume()) {
            this.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
            return false;
         } else if (this != target
            && skill.isPhysical()
            && GeoEngineConfig.PATHFINDING > 0
            && PathFinding.getInstance()
                  .findPath(this.getX(), this.getY(), this.getZ(), target.getX(), target.getY(), target.getZ(), this.getInstanceId(), false)
               == null) {
            this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
            return false;
         } else {
            if (skill.hasNegativeEffect()) {
               if (this._owner == target) {
                  return false;
               }

               if (this.isInsidePeaceZone(this, target) && !this._owner.getAccessLevel().allowPeaceAttack()) {
                  this.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
                  return false;
               }

               if (this._owner.isInOlympiadMode() && !this._owner.isOlympiadStart()) {
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return false;
               }

               if (this._owner.isSiegeFriend(target)) {
                  if (TerritoryWarManager.getInstance().isTWInProgress()) {
                     this.sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
                  } else {
                     this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
                  }

                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return false;
               }

               if (target.isDoor()) {
                  if (!target.isAutoAttackable(this._owner)) {
                     return false;
                  }
               } else {
                  if (!target.canBeAttacked() && !this._owner.getAccessLevel().allowPeaceAttack()) {
                     return false;
                  }

                  if (!target.isAutoAttackable(this)
                     && !forceUse
                     && !target.isNpc()
                     && skill.getTargetType() != TargetType.AURA
                     && skill.getTargetType() != TargetType.FRONT_AURA
                     && skill.getTargetType() != TargetType.BEHIND_AURA
                     && skill.getTargetType() != TargetType.CLAN
                     && skill.getTargetType() != TargetType.PARTY
                     && skill.getTargetType() != TargetType.SELF) {
                     return false;
                  }
               }
            }

            this.getAI().setIntentionCast(skill, target);
            return true;
         }
      }
   }

   @Override
   public void setImmobilized(boolean value) {
      super.setImmobilized(value);
      if (value) {
         this._previousFollowStatus = this._follow;
         if (this._previousFollowStatus) {
            this.setFollowStatus(false);
         }
      } else {
         this.setFollowStatus(this._previousFollowStatus);
      }
   }

   public void setOwner(Player newOwner) {
      this._owner = newOwner;
   }

   @Override
   public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
      if (!miss && this._owner != null) {
         if (target.getObjectId() != this._owner.getObjectId()) {
            if (pcrit || mcrit) {
               if (this.isServitor()) {
                  this.sendPacket(SystemMessageId.SUMMONED_MONSTER_S_CRITICAL_HIT);
               } else {
                  this.sendPacket(SystemMessageId.PET_S_CRITICAL_HIT);
               }
            }

            if (this._owner.isInOlympiadMode()
               && target.isPlayer()
               && target.asPlayer().isInOlympiadMode()
               && target.asPlayer().getOlympiadGameId() == this._owner.getOlympiadGameId()) {
               Olympiad.getInstance().notifyCompetitorDamage(this.getOwner(), damage, this.getOwner().getOlympiadGameId());
            }

            SystemMessage sm;
            if (target.isInvul() && !target.isNpc()) {
               sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
            } else {
               sm = new SystemMessage(SystemMessageId.C1_HAS_GIVEN_C2_DAMAGE_OF_S3);
               sm.addNpcName(this);
               sm.addString(target.getName());
               sm.addInt(damage);
            }

            this.sendPacket(sm);
         }
      }
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, Skill skill) {
      super.reduceCurrentHp(damage, attacker, skill);
      if (this._owner != null && attacker != null) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
         sm.addNpcName(this);
         sm.addString(attacker.getName());
         sm.addInt((int)damage);
         this.sendPacket(sm);
      }
   }

   @Override
   public void doCast(Skill skill) {
      Player actingPlayer = this.getOwner();
      if (!actingPlayer.checkPvpSkill(this.getTarget(), skill) && !actingPlayer.getAccessLevel().allowPeaceAttack()) {
         actingPlayer.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
         actingPlayer.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         super.doCast(skill);
      }
   }

   @Override
   public boolean isInCombat() {
      return this._owner != null && this._owner.isInCombat();
   }

   @Override
   public Player asPlayer() {
      return this._owner;
   }

   public void updateAndBroadcastStatus(int value) {
      if (this._owner != null) {
         if (this.isSpawned()) {
            this.sendPacket(new PetInfo(this, value));
            this.sendPacket(new PetStatusUpdate(this));
            this.broadcastNpcInfo(value);
            Party party = this._owner.getParty();
            if (party != null) {
               party.broadcastToPartyMembers(this._owner, new ExPartyPetWindowUpdate(this));
            }

            this.updateEffectIcons(true);
         }
      }
   }

   public void broadcastNpcInfo(int value) {
      World.forEachVisibleObject(this, Player.class, player -> {
         if (player != this._owner) {
            player.sendPacket(new AbstractNpcInfo.SummonInfo(this, player, value));
         }
      });
   }

   public boolean isHungry() {
      return false;
   }

   public int getWeapon() {
      return 0;
   }

   public int getArmor() {
      return 0;
   }

   @Override
   public void sendInfo(Player player) {
      if (player == this._owner) {
         player.sendPacket(new PetInfo(this, 0));
         this.updateEffectIcons(true);
         if (this.isPet()) {
            player.sendPacket(new PetItemList(this.getInventory().getItems()));
         }
      } else {
         player.sendPacket(new AbstractNpcInfo.SummonInfo(this, player, 0));
      }
   }

   @Override
   public synchronized void onTeleported() {
      super.onTeleported();
      this.sendPacket(new TeleportToLocation(this, this.getX(), this.getY(), this.getZ(), this.getHeading()));
   }

   @Override
   public boolean isUndead() {
      return this.getTemplate().getRace() == Race.UNDEAD;
   }

   public void switchMode() {
   }

   public void cancelAction() {
      if (!this.isMovementDisabled()) {
         this.getAI().setIntentionActive();
      }
   }

   public void doSummonAttack(WorldObject target) {
      if (this._owner != null && target != null) {
         this.setTarget(target);
         this.getAI().setIntentionAttack(target);
         if (target.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE) {
            this._owner.updatePvPStatus();
         }
      }
   }

   public boolean canAttack(boolean ctrlPressed) {
      if (this._owner == null) {
         return false;
      } else {
         WorldObject target = this._owner.getTarget();
         if (target != null && this != target && this._owner != target) {
            int npcId = this.getId();
            if (ArrayUtil.contains(PASSIVE_SUMMONS, npcId)) {
               this._owner.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else if (this.isBetrayed()) {
               this.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else if (this.isPet() && this.getLevel() - this._owner.getLevel() > 20) {
               this.sendPacket(SystemMessageId.YOUR_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else if (this._owner.isInOlympiadMode() && !this._owner.isOlympiadStart()) {
               this._owner.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else if (this._owner.isSiegeFriend(target)) {
               if (TerritoryWarManager.getInstance().isTWInProgress()) {
                  this.sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
               } else {
                  this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
               }

               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else if (!this._owner.getAccessLevel().allowPeaceAttack() && this._owner.isInsidePeaceZone(this, target)) {
               this.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
               return false;
            } else if (this.isLockedTarget()) {
               this.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
               return false;
            } else if (!target.isAutoAttackable(this._owner) && !ctrlPressed && !target.isNpc()) {
               this.setFollowStatus(false);
               this.getAI().setIntentionFollow(target);
               this.sendPacket(SystemMessageId.INVALID_TARGET);
               return false;
            } else if (target.isDoor() && this.getTemplate().getRace() != Race.SIEGE_WEAPON) {
               return false;
            } else if (this._owner.calculateDistance3D(target) > 3000.0) {
               this.getAI().setIntentionFollow(this._owner);
               this.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
               return false;
            } else if (!GeoEngine.getInstance().canSeeTarget(this, target)) {
               this.getAI().setIntentionFollow(this._owner);
               this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
               return false;
            } else {
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public void sendPacket(ServerPacket packet) {
      if (this._owner != null) {
         this._owner.sendPacket(packet);
      }
   }

   @Override
   public void sendPacket(SystemMessageId id) {
      if (this._owner != null) {
         this._owner.sendPacket(id);
      }
   }

   @Override
   public boolean isSummon() {
      return true;
   }

   @Override
   public Summon asSummon() {
      return this;
   }

   @Override
   public boolean isChargedShot(ShotType type) {
      return (this._shotsMask & type.getMask()) == type.getMask();
   }

   @Override
   public void setChargedShot(ShotType type, boolean charged) {
      if (charged) {
         this._shotsMask = this._shotsMask | type.getMask();
      } else {
         this._shotsMask = this._shotsMask & ~type.getMask();
      }
   }

   @Override
   public void rechargeShots(boolean physical, boolean magic) {
      if (this._owner.getAutoSoulShot() != null && !this._owner.getAutoSoulShot().isEmpty()) {
         for (int itemId : this._owner.getAutoSoulShot()) {
            Item item = this._owner.getInventory().getItemByItemId(itemId);
            if (item != null) {
               if (magic && item.getTemplate().getDefaultAction() == ActionType.SUMMON_SPIRITSHOT) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                  if (handler != null) {
                     handler.onItemUse(this._owner, item, false);
                  }
               }

               if (physical && item.getTemplate().getDefaultAction() == ActionType.SUMMON_SOULSHOT) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                  if (handler != null) {
                     handler.onItemUse(this._owner, item, false);
                  }
               }
            } else {
               this._owner.removeAutoSoulShot(itemId);
            }
         }
      }
   }

   @Override
   public int getClanId() {
      return this._owner != null ? this._owner.getClanId() : 0;
   }

   @Override
   public int getAllyId() {
      return this._owner != null ? this._owner.getAllyId() : 0;
   }

   public int getFormId() {
      int formId = 0;
      int npcId = this.getId();
      if (npcId == 16041 || npcId == 16042) {
         if (this.getLevel() > 69) {
            formId = 3;
         } else if (this.getLevel() > 64) {
            formId = 2;
         } else if (this.getLevel() > 59) {
            formId = 1;
         }
      } else if (npcId == 16025 || npcId == 16037) {
         if (this.getLevel() > 69) {
            formId = 3;
         } else if (this.getLevel() > 64) {
            formId = 2;
         } else if (this.getLevel() > 59) {
            formId = 1;
         }
      }

      return formId;
   }

   @Override
   public boolean isOnEvent() {
      return this._owner != null && this._owner.isOnEvent();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append('(');
      sb.append(this.getId());
      sb.append(") Owner: ");
      sb.append(this._owner);
      return sb.toString();
   }
}
