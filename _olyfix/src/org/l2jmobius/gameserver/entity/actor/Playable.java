package org.l2jmobius.gameserver.entity.actor;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.PlayableStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayableStatus;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

public abstract class Playable extends Creature {
   private Creature _lockedTarget = null;
   private Player transferDmgTo = null;

   public Playable(int objectId, CreatureTemplate template) {
      super(objectId, template);
      this.setInstanceType(InstanceType.Playable);
      this.setInvul(false);
   }

   public Playable(CreatureTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Playable);
      this.setInvul(false);
   }

   public PlayableStat getStat() {
      return (PlayableStat)super.getStat();
   }

   @Override
   public void initCharStat() {
      this.setStat(new PlayableStat(this));
   }

   public PlayableStatus getStatus() {
      return (PlayableStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new PlayableStatus(this));
   }

   @Override
   public boolean doDie(Creature killer) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this)) {
         TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this, TerminateReturn.class);
         if (returnBack != null && returnBack.terminate()) {
            return false;
         }
      }

      synchronized (this) {
         if (this.isDead()) {
            return false;
         }

         this.setCurrentHp(0.0);
         this.setDead(true);
      }

      this.setTarget(null);
      this.stopMove(null);
      this.getStatus().stopHpMpRegeneration();
      boolean deleteBuffs = true;
      if (this.isNoblesseBlessedAffected()) {
         this.stopEffects(EffectType.NOBLESSE_BLESSING);
         deleteBuffs = false;
      }

      if (this.isResurrectSpecialAffected()) {
         this.stopEffects(EffectType.RESURRECTION_SPECIAL);
         deleteBuffs = false;
      }

      Player player = this.asPlayer();
      if (this.isPlayer() && player.hasCharmOfCourage()) {
         if (player.isInSiege()) {
            player.reviveRequest(player, false, 0);
         }

         player.setCharmOfCourage(false);
         player.sendPacket(new EtcStatusUpdate(player));
      }

      if (deleteBuffs) {
         this.stopAllEffectsExceptThoseThatLastThroughDeath();
      }

      this.broadcastStatusUpdate();
      ZoneManager.getInstance().getRegion(this).onDeath(this);
      if (!player.isNotifyQuestOfDeathEmpty()) {
         for (QuestState qs : player.getNotifyQuestOfDeath()) {
            qs.getQuest().onDeath((Creature)(killer == null ? this : killer), this, qs);
         }
      }

      if (this.isPlayer() && this.isInInstance()) {
         Instance instance = InstanceManager.getInstance().getInstance(this.getInstanceId());
         if (instance != null) {
            instance.notifyDeath(player);
         }
      }

      if (killer != null) {
         Player killerPlayer = killer.asPlayer();
         if (killerPlayer != null) {
            killerPlayer.onKillUpdatePvPKarma(this);
         }
      }

      this.getAI().notifyActionDeath();
      this.updateEffectIcons();
      return true;
   }

   public boolean checkIfPvP(Creature target) {
      if (target == null) {
         return false;
      } else if (target == this) {
         return false;
      } else if (!target.isPlayable()) {
         return false;
      } else {
         Player player = this.asPlayer();
         if (player == null) {
            return false;
         } else if (player.getKarma() != 0) {
            return false;
         } else {
            Player targetPlayer = target.asPlayer();
            if (targetPlayer == null) {
               return false;
            } else if (targetPlayer == this) {
               return false;
            } else {
               return targetPlayer.getKarma() != 0 ? false : targetPlayer.getPvpFlag() != 0;
            }
         }
      }
   }

   @Override
   public boolean canBeAttacked() {
      return true;
   }

   public boolean isNoblesseBlessedAffected() {
      return this.isAffected(EffectFlag.NOBLESS_BLESSING);
   }

   public boolean isResurrectSpecialAffected() {
      return this.isAffected(EffectFlag.RESURRECTION_SPECIAL);
   }

   public boolean isSilentMovingAffected() {
      return this.isAffected(EffectFlag.SILENT_MOVE);
   }

   public boolean isProtectionBlessingAffected() {
      return this.isAffected(EffectFlag.PROTECTION_BLESSING);
   }

   @Override
   public void updateEffectIcons(boolean partyOnly) {
      this.getEffectList().updateEffectIcons(partyOnly);
   }

   public boolean isLockedTarget() {
      return this._lockedTarget != null;
   }

   public Creature getLockedTarget() {
      return this._lockedTarget;
   }

   public void setLockedTarget(Creature creature) {
      this._lockedTarget = creature;
   }

   public void setTransferDamageTo(Player val) {
      this.transferDmgTo = val;
   }

   public Player getTransferingDamageTo() {
      return this.transferDmgTo;
   }

   public abstract void doPickupItem(WorldObject var1);

   public abstract byte getPvpFlag();

   public abstract boolean useMagic(Skill var1, boolean var2, boolean var3);

   public abstract void storeMe();

   public abstract void storeEffect(boolean var1);

   public abstract void restoreEffects();

   public boolean isOnEvent() {
      return false;
   }

   @Override
   public boolean isPlayable() {
      return true;
   }

   @Override
   public Playable asPlayable() {
      return this;
   }
}
