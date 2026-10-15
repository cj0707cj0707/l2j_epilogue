package org.l2jmobius.gameserver.entity.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;

public class CreatureStatus {
   protected static final Logger LOGGER = Logger.getLogger(CreatureStatus.class.getName());
   private final Creature _creature;
   private double _currentHp = 0.0;
   private double _currentMp = 0.0;
   private Set<Creature> _statusListener;
   private Future<?> _regTask;
   protected byte _flagsRegenActive = 0;
   protected static final byte REGEN_FLAG_CP = 4;
   private static final byte REGEN_FLAG_HP = 1;
   private static final byte REGEN_FLAG_MP = 2;

   public CreatureStatus(Creature creature) {
      this._creature = creature;
   }

   public void addStatusListener(Creature object) {
      if (object != this._creature) {
         this.getStatusListener().add(object);
      }
   }

   public void removeStatusListener(Creature object) {
      this.getStatusListener().remove(object);
   }

   public Set<Creature> getStatusListener() {
      if (this._statusListener == null) {
         this._statusListener = ConcurrentHashMap.newKeySet();
      }

      return this._statusListener;
   }

   public void reduceCp(int value) {
   }

   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   public void reduceHp(double value, Creature attacker, boolean isHpConsumption) {
      this.reduceHp(value, attacker, true, false, isHpConsumption);
   }

   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption) {
      if (!this._creature.isDead()) {
         if (!this._creature.isInvul() || isDOT || isHPConsumption) {
            if (attacker != null) {
               Player attackerPlayer = attacker.asPlayer();
               if (attackerPlayer != null && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage()) {
                  return;
               }
            }

            if (!isDOT && !isHPConsumption) {
               this._creature.stopEffectsOnDamage(awake);
               if (this._creature.isStunned() && Rnd.get(10) == 0) {
                  this._creature.stopStunning(true);
               }
            }

            if (value > 0.0) {
               this.setCurrentHp(Math.max(this._currentHp - value, 0.0));
            }

            if (this._creature.getCurrentHp() < 0.5 && this._creature.isMortal()) {
               this._creature.abortAttack();
               this._creature.abortCast();
               this._creature.doDie(attacker);
            }
         }
      }
   }

   public void reduceMp(double value) {
      this.setCurrentMp(Math.max(this._currentMp - value, 0.0));
   }

   public synchronized void startHpMpRegeneration() {
      if (this._regTask == null && !this._creature.isDead()) {
         int period = Formulas.getRegeneratePeriod(this._creature);
         this._regTask = ThreadPool.scheduleAtFixedRate(this::doRegeneration, period, period);
      }
   }

   public synchronized void stopHpMpRegeneration() {
      if (this._regTask != null) {
         this._regTask.cancel(false);
         this._regTask = null;
         this._flagsRegenActive = 0;
      }
   }

   public double getCurrentCp() {
      return 0.0;
   }

   public void setCurrentCp(double newCp) {
   }

   public double getCurrentHp() {
      return this._currentHp;
   }

   public void setCurrentHp(double newHp) {
      this.setCurrentHp(newHp, true);
   }

   public boolean setCurrentHp(double newHp, boolean broadcastPacket) {
      int currentHp = (int)this._currentHp;
      double maxHp = this._creature.getMaxHp();
      double thirtyPercent = maxHp * 0.3;
      boolean updateUserInfo = this._creature.isPlayer()
         && (currentHp < thirtyPercent && newHp > thirtyPercent || currentHp > thirtyPercent && newHp < thirtyPercent);
      synchronized (this) {
         if (this._creature.isDead()) {
            return false;
         }

         if (newHp >= maxHp) {
            this._currentHp = maxHp;
            this._flagsRegenActive &= -2;
            if (this._flagsRegenActive == 0) {
               this.stopHpMpRegeneration();
            }
         } else {
            this._currentHp = newHp;
            this._flagsRegenActive = (byte)(this._flagsRegenActive | 1);
            this.startHpMpRegeneration();
         }
      }

      boolean hpWasChanged = currentHp != this._currentHp;
      if (hpWasChanged && broadcastPacket) {
         this._creature.broadcastStatusUpdate();
      }

      if (updateUserInfo) {
         this._creature.asPlayer().updateUserInfo();
      }

      return hpWasChanged;
   }

   public void setCurrentHpMp(double newHp, double newMp) {
      boolean hpChanged = this.setCurrentHp(newHp, false);
      boolean mpChanged = this.setCurrentMp(newMp, false);
      if (hpChanged || mpChanged) {
         this._creature.broadcastStatusUpdate();
      }
   }

   public double getCurrentMp() {
      return this._currentMp;
   }

   public void setCurrentMp(double newMp) {
      this.setCurrentMp(newMp, true);
   }

   public boolean setCurrentMp(double newMp, boolean broadcastPacket) {
      int currentMp = (int)this._currentMp;
      int maxMp = this._creature.getMaxMp();
      synchronized (this) {
         if (this._creature.isDead()) {
            return false;
         }

         if (newMp >= maxMp) {
            this._currentMp = maxMp;
            this._flagsRegenActive &= -3;
            if (this._flagsRegenActive == 0) {
               this.stopHpMpRegeneration();
            }
         } else {
            this._currentMp = newMp;
            this._flagsRegenActive = (byte)(this._flagsRegenActive | 2);
            this.startHpMpRegeneration();
         }
      }

      boolean mpWasChanged = currentMp != this._currentMp;
      if (mpWasChanged && broadcastPacket) {
         this._creature.broadcastStatusUpdate();
      }

      return mpWasChanged;
   }

   protected void doRegeneration() {
      if (this._creature.isDead() || !(this._currentHp < this._creature.getMaxRecoverableHp()) && !(this._currentMp < this._creature.getMaxRecoverableMp())) {
         this.stopHpMpRegeneration();
      } else {
         double newHp = this._currentHp + Formulas.calcHpRegen(this._creature);
         double newMp = this._currentMp + Formulas.calcMpRegen(this._creature);
         this.setCurrentHpMp(newHp, newMp);
      }
   }

   public Creature getActiveChar() {
      return this._creature;
   }
}
