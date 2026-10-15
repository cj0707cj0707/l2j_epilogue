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

   public Playable(int var1, CreatureTemplate var2) {
      super(var1, var2);
      this.setInstanceType(InstanceType.Playable);
      this.setInvul(false);
   }

   public Playable(CreatureTemplate var1) {
      super(var1);
      this.setInstanceType(InstanceType.Playable);
      this.setInvul(false);
   }

   public PlayableStat getStat() {
      return (PlayableStat)super.getStat();
   }

   public void initCharStat() {
      this.setStat(new PlayableStat(this));
   }

   public PlayableStatus getStatus() {
      return (PlayableStatus)super.getStatus();
   }

   public void initCharStatus() {
      this.setStatus(new PlayableStatus(this));
   }

   public boolean doDie(Creature var1) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this)) {
         TerminateReturn var2 = (TerminateReturn)EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(var1, this), this, TerminateReturn.class);
         if (var2 != null && var2.terminate()) {
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
      this.getEffectList().stopAllDebuffs(true);
      Player var6 = this.asPlayer();
      if (this.isPlayer() && var6.hasCharmOfCourage()) {
         if (var6.isInSiege()) {
            var6.reviveRequest(var6, false, 0);
         }

         var6.setCharmOfCourage(false);
         var6.sendPacket(new EtcStatusUpdate(var6));
      }

      this.broadcastStatusUpdate();
      ZoneManager.getInstance().getRegion(this).onDeath(this);
      if (!var6.isNotifyQuestOfDeathEmpty()) {
         for (QuestState var4 : var6.getNotifyQuestOfDeath()) {
            var4.getQuest().onDeath((Creature)(var1 == null ? this : var1), this, var4);
         }
      }

      if (this.isPlayer() && this.isInInstance()) {
         Instance var7 = InstanceManager.getInstance().getInstance(this.getInstanceId());
         if (var7 != null) {
            var7.notifyDeath(var6);
         }
      }

      if (var1 != null) {
         Player var8 = var1.asPlayer();
         if (var8 != null) {
            var8.onKillUpdatePvPKarma(this);
         }
      }

      this.getAI().notifyActionDeath();
      this.updateEffectIcons();
      return true;
   }

   public boolean checkIfPvP(Creature var1) {
      if (var1 == null) {
         return false;
      } else if (var1 == this) {
         return false;
      } else if (!var1.isPlayable()) {
         return false;
      } else {
         Player var2 = this.asPlayer();
         if (var2 == null) {
            return false;
         } else if (var2.getKarma() != 0) {
            return false;
         } else {
            Player var3 = var1.asPlayer();
            if (var3 == null) {
               return false;
            } else if (var3 == this) {
               return false;
            } else {
               return var3.getKarma() != 0 ? false : var3.getPvpFlag() != 0;
            }
         }
      }
   }

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

   public void updateEffectIcons(boolean var1) {
      this.getEffectList().updateEffectIcons(var1);
   }

   public boolean isLockedTarget() {
      return this._lockedTarget != null;
   }

   public Creature getLockedTarget() {
      return this._lockedTarget;
   }

   public void setLockedTarget(Creature var1) {
      this._lockedTarget = var1;
   }

   public void setTransferDamageTo(Player var1) {
      this.transferDmgTo = var1;
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

   public boolean isPlayable() {
      return true;
   }

   public Playable asPlayable() {
      return this;
   }
}
