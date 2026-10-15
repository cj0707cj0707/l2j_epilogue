package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.tasks.npc.trap.TrapTask;
import org.l2jmobius.gameserver.entity.actor.tasks.npc.trap.TrapTriggerTask;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.trap.OnTrapAction;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

public class Trap extends Npc {
   private static final int TICK = 1000;
   private boolean _hasLifeTime;
   private boolean _isInArena = false;
   private boolean _isTriggered;
   private final int _lifeTime;
   private Player _owner;
   private final Set<Integer> _playersWhoDetectedMe = new HashSet<>();
   private final SkillHolder _skill;
   private int _remainingTime;
   private ScheduledFuture<?> _trapTask = null;

   public Trap(NpcTemplate template, int instanceId, int lifeTime) {
      super(template);
      this.setInstanceType(InstanceType.Trap);
      this.setInstanceId(instanceId);
      this.setName(template.getName());
      this.setInvul(false);
      this._owner = null;
      this._isTriggered = false;
      this._skill = this.getTemplate().getParameters().getObject("trap_skill", SkillHolder.class);
      this._hasLifeTime = lifeTime >= 0;
      this._lifeTime = lifeTime != 0 ? lifeTime : 30000;
      this._remainingTime = this._lifeTime;
      if (this._skill != null) {
         this._trapTask = ThreadPool.scheduleAtFixedRate(new TrapTask(this), 1000L, 1000L);
      }
   }

   public Trap(NpcTemplate template, Player owner, int lifeTime) {
      this(template, owner.getInstanceId(), lifeTime);
      this._owner = owner;
   }

   @Override
   public void broadcastPacket(ServerPacket packet) {
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this._isTriggered || this.canBeSeen(player)) {
            player.sendPacket(packet);
         }
      });
   }

   public boolean canBeSeen(Creature creature) {
      if (creature != null && this._playersWhoDetectedMe.contains(creature.getObjectId())) {
         return true;
      } else if (this._owner == null || creature == null) {
         return false;
      } else if (creature == this._owner) {
         return true;
      } else {
         if (creature.isPlayer()) {
            if (creature.asPlayer().inObserverMode()) {
               return false;
            }

            if (this._owner.isInOlympiadMode()
               && creature.asPlayer().isInOlympiadMode()
               && creature.asPlayer().getOlympiadSide() != this._owner.getOlympiadSide()) {
               return false;
            }
         }

         return this._isInArena
            ? true
            : this._owner.isInParty() && creature.isInParty() && this._owner.getParty().getLeaderObjectId() == creature.getParty().getLeaderObjectId();
      }
   }

   public boolean checkTarget(Creature target) {
      if (!target.isInsideRadius2D(this, 150)) {
         return false;
      } else if (!Skill.checkForAreaOffensiveSkills(this, target, this._skill.getSkill(), this._isInArena)) {
         return false;
      } else {
         Player player = target.asPlayer();
         if (target.isPlayer() && player.inObserverMode()) {
            return false;
         } else if (this._owner != null
            && this._owner.isInOlympiadMode()
            && player != null
            && player.isInOlympiadMode()
            && player.getOlympiadSide() == this._owner.getOlympiadSide()) {
            return false;
         } else if (this._isInArena) {
            return true;
         } else {
            if (this._owner != null) {
               if (target.isAttackable()) {
                  return true;
               }

               if (player == null || player.getPvpFlag() == 0 && player.getKarma() == 0) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public boolean deleteMe() {
      if (this._owner != null) {
         this._owner.setTrap(null);
         this._owner = null;
      }

      return super.deleteMe();
   }

   @Override
   public Player asPlayer() {
      return this._owner;
   }

   @Override
   public Weapon getActiveWeaponItem() {
      return null;
   }

   @Override
   public int getKarma() {
      return this._owner != null ? this._owner.getKarma() : 0;
   }

   public Player getOwner() {
      return this._owner;
   }

   public byte getPvpFlag() {
      return this._owner != null ? this._owner.getPvpFlag() : 0;
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return null;
   }

   @Override
   public Weapon getSecondaryWeaponItem() {
      return null;
   }

   public Skill getSkill() {
      return this._skill.getSkill();
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return !this.canBeSeen(attacker);
   }

   @Override
   public boolean isTrap() {
      return true;
   }

   public boolean isTriggered() {
      return this._isTriggered;
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._isInArena = this.isInsideZone(ZoneId.PVP) && !this.isInsideZone(ZoneId.SIEGE);
      this._playersWhoDetectedMe.clear();
   }

   @Override
   public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
      if (!miss && this._owner != null) {
         if (this._owner.isInOlympiadMode()
            && target.isPlayer()
            && target.asPlayer().isInOlympiadMode()
            && target.asPlayer().getOlympiadGameId() == this._owner.getOlympiadGameId()) {
            Olympiad.getInstance().notifyCompetitorDamage(this.getOwner(), damage, this._owner.getOlympiadGameId());
         }

         if (target.isInvul() && !target.isNpc()) {
            this._owner.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_GIVEN_C2_DAMAGE_OF_S3);
            sm.addString(this.getName());
            sm.addString(target.getName());
            sm.addInt(damage);
            this._owner.sendPacket(sm);
         }
      }
   }

   @Override
   public void sendInfo(Player player) {
      if (this._isTriggered || this.canBeSeen(player)) {
         player.sendPacket(new AbstractNpcInfo.TrapInfo(this, player));
      }
   }

   public void setDetected(Creature detector) {
      if (this._isInArena) {
         if (detector.isPlayable()) {
            this.sendInfo(detector.asPlayer());
         }
      } else if (this._owner == null || this._owner.getPvpFlag() != 0 || this._owner.getKarma() != 0) {
         this._playersWhoDetectedMe.add(detector.getObjectId());
         if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, detector, TrapAction.TRAP_DETECTED), this);
         }

         if (detector.isPlayable()) {
            this.sendInfo(detector.asPlayer());
         }
      }
   }

   public void stopDecay() {
      DecayTaskManager.getInstance().cancel(this);
   }

   public void triggerTrap(Creature target) {
      if (this._trapTask != null) {
         this._trapTask.cancel(true);
         this._trapTask = null;
      }

      this._isTriggered = true;
      this.broadcastPacket(new AbstractNpcInfo.TrapInfo(this, null));
      this.setTarget(target);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, target, TrapAction.TRAP_TRIGGERED), this);
      }

      ThreadPool.schedule(new TrapTriggerTask(this), 500L);
   }

   public void unSummon() {
      if (this._trapTask != null) {
         this._trapTask.cancel(true);
         this._trapTask = null;
      }

      if (this._owner != null) {
         this._owner.setTrap(null);
         this._owner = null;
      }

      if (this.isSpawned() && !this.isDead()) {
         ZoneManager.getInstance().getRegion(this).removeFromZones(this);
         this.deleteMe();
      }
   }

   @Override
   public void updateAbnormalEffect() {
   }

   public boolean hasLifeTime() {
      return this._hasLifeTime;
   }

   public void setHasLifeTime(boolean value) {
      this._hasLifeTime = value;
   }

   public int getRemainingTime() {
      return this._remainingTime;
   }

   public void setRemainingTime(int time) {
      this._remainingTime = time;
   }

   public int getLifeTime() {
      return this._lifeTime;
   }
}
