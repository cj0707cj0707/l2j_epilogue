package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectTaskInfo;
import org.l2jmobius.gameserver.mechanics.effects.EffectTickTask;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class BuffInfo {
   private final Creature _effector;
   private final Creature _effected;
   private final Skill _skill;
   private final List<AbstractEffect> _effects = new ArrayList<>(1);
   private final Map<AbstractEffect, EffectTaskInfo> _tasks = new ConcurrentHashMap<>();
   private int _abnormalTime;
   private final int _periodStartTicks;
   private volatile SkillFinishType _finishType = SkillFinishType.NORMAL;
   private boolean _isInUse = true;

   public BuffInfo(Creature effector, Creature effected, Skill skill) {
      this._effector = effector;
      this._effected = effected;
      this._skill = skill;
      this._abnormalTime = Formulas.calcEffectAbnormalTime(effector, effected, skill);
      this._periodStartTicks = GameTimeTaskManager.getInstance().getGameTicks();
   }

   public List<AbstractEffect> getEffects() {
      return this._effects;
   }

   public void addEffect(AbstractEffect effect) {
      this._effects.add(effect);
   }

   private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo) {
      this._tasks.put(effect, effectTaskInfo);
   }

   private EffectTaskInfo getEffectTask(AbstractEffect effect) {
      return this._tasks.get(effect);
   }

   public Skill getSkill() {
      return this._skill;
   }

   public int getAbnormalTime() {
      return this._abnormalTime;
   }

   public void setAbnormalTime(int abnormalTime) {
      this._abnormalTime = abnormalTime;
   }

   public int getPeriodStartTicks() {
      return this._periodStartTicks;
   }

   public int getTime() {
      return this._abnormalTime - (GameTimeTaskManager.getInstance().getGameTicks() - this._periodStartTicks) / 10;
   }

   public boolean isRemoved() {
      return this._finishType == SkillFinishType.REMOVED;
   }

   public void setFinishType(SkillFinishType type) {
      this._finishType = type;
   }

   public boolean isInUse() {
      return this._isInUse;
   }

   public void setInUse(boolean value) {
      this._isInUse = value;
   }

   public Creature getEffector() {
      return this._effector;
   }

   public Creature getEffected() {
      return this._effected;
   }

   public void stopAllEffects(SkillFinishType type, boolean broadcast) {
      this.setFinishType(type);
      this._effected.removeBuffInfoTime(this);
      this.finishEffects(broadcast);
   }

   public void initializeEffects() {
      if (this._effected != null && this._skill != null) {
         if (this._effected.isPlayer()
            && (this._effected.asPlayer().hasEnteredWorld() || PlayerConfig.SHOW_EFFECT_MESSAGES_ON_LOGIN)
            && !this._skill.isPassive()) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_EFFECT_CAN_BE_FELT);
            sm.addSkillName(this._skill);
            this._effected.sendPacket(sm);
         }

         if (this._abnormalTime > 0) {
            this._effected.addBuffInfoTime(this);
         }

         boolean update = false;

         for (AbstractEffect effect : this._effects) {
            if (!effect.isInstant() && (!this._effected.isDead() || this._skill.isPassive())) {
               effect.onStart(this._effector, this._effected, this._skill);
               if (!this._effected.isDead() || this._skill.isPassive()) {
                  if (effect.getTicks() > 0) {
                     EffectTickTask effectTask = new EffectTickTask(this, effect);
                     this.addTask(
                        effect,
                        new EffectTaskInfo(
                           effectTask,
                           ThreadPool.scheduleAtFixedRate(
                              effectTask, effect.getTicks() * PlayerConfig.EFFECT_TICK_RATIO, effect.getTicks() * PlayerConfig.EFFECT_TICK_RATIO
                           )
                        )
                     );
                  }

                  this._effected.addStatFuncs(effect.getStatFuncs(this._effector, this._effected, this._skill));
                  update = true;
               }
            }
         }

         if (update) {
            if (this._effected.isSummon()) {
               this._effected.asSummon().updateAndBroadcastStatus(1);
            }

            this.addAbnormalVisualEffects();
         }
      }
   }

   public void onTick(AbstractEffect effect) {
      boolean continueForever = false;
      if (this._isInUse) {
         continueForever = effect.onActionTime(this._effector, this._effected, this._skill);
      }

      if (!continueForever && this._skill.isToggle()) {
         EffectTaskInfo task = this.getEffectTask(effect);
         if (task != null) {
            ScheduledFuture<?> schedule = task.getScheduledFuture();
            if (schedule != null && !schedule.isCancelled() && !schedule.isDone()) {
               schedule.cancel(true);
            }

            this._effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, this._skill);
         }
      }
   }

   public void finishEffects(boolean broadcast) {
      for (EffectTaskInfo effectTask : this._tasks.values()) {
         ScheduledFuture<?> schedule = effectTask.getScheduledFuture();
         if (schedule != null && !schedule.isCancelled() && !schedule.isDone()) {
            schedule.cancel(true);
         }
      }

      this.removeStats();

      for (AbstractEffect effect : this._effects) {
         if (effect != null && !effect.isInstant()) {
            effect.onExit(this._effector, this._effected, this._skill);
         }
      }

      this.removeAbnormalVisualEffects(broadcast);
      if (!this._effected.isSummon() || this._effected.asSummon().getOwner().hasSummon()) {
         SystemMessageId smId = null;
         if (this._finishType != SkillFinishType.SILENT) {
            if (this._skill.isToggle()) {
               smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
            } else if (this._finishType == SkillFinishType.REMOVED) {
               smId = SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED;
            } else if (!this._skill.isPassive()) {
               smId = SystemMessageId.S1_HAS_WORN_OFF;
            }
         }

         if (smId != null) {
            SystemMessage sm = new SystemMessage(smId);
            sm.addSkillName(this._skill);
            this._effected.sendPacket(sm);
         }
      }

      if (this == this._effected.getEffectList().getShortBuff()) {
         this._effected.getEffectList().shortBuffStatusUpdate(null);
      }
   }

   public boolean isAbnormalType(AbnormalType type) {
      return this._skill.getAbnormalType() == type;
   }

   private void addAbnormalVisualEffects() {
      if (this._skill.hasAbnormalVisualEffects()) {
         this._effected.startAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffects());
      }

      if (this._effected.isPlayer() && this._skill.hasAbnormalVisualEffectsEvent()) {
         this._effected.startAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffectsEvent());
      }

      if (this._skill.hasAbnormalVisualEffectsSpecial()) {
         this._effected.startAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffectsSpecial());
      }

      this._effected.updateAbnormalEffect();
   }

   private void removeAbnormalVisualEffects(boolean broadcast) {
      if (this._effected != null && this._skill != null) {
         if (this._skill.hasAbnormalVisualEffects()) {
            this._effected.stopAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffects());
         }

         if (this._effected.isPlayer() && this._skill.hasAbnormalVisualEffectsEvent()) {
            this._effected.stopAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffectsEvent());
         }

         if (this._skill.hasAbnormalVisualEffectsSpecial()) {
            this._effected.stopAbnormalVisualEffect(false, this._skill.getAbnormalVisualEffectsSpecial());
         }

         if (broadcast) {
            this._effected.updateAbnormalEffect();
         }
      }
   }

   public void addStats() {
      this._effects.forEach(effect -> this._effected.addStatFuncs(effect.getStatFuncs(this._effector, this._effected, this._skill)));
   }

   public void removeStats() {
      this._effects.forEach(this._effected::removeStatsOwner);
      this._effected.removeStatsOwner(this._skill);
   }

   @Override
   public String toString() {
      return "BuffInfo [effector="
         + this._effector
         + ", effected="
         + this._effected
         + ", skill="
         + this._skill
         + ", effects="
         + this._effects
         + ", tasks="
         + this._tasks
         + ", abnormalTime="
         + this._abnormalTime
         + ", periodStartTicks="
         + this._periodStartTicks
         + ", isRemoved="
         + this.isRemoved()
         + ", isInUse="
         + this._isInUse
         + "]";
   }
}
