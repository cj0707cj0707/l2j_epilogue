package org.l2jmobius.gameserver.mechanics.effects;

import java.util.concurrent.ScheduledFuture;

public class EffectTaskInfo {
   private final EffectTickTask _effectTask;
   private final ScheduledFuture<?> _scheduledFuture;

   public EffectTaskInfo(EffectTickTask effectTask, ScheduledFuture<?> scheduledFuture) {
      this._effectTask = effectTask;
      this._scheduledFuture = scheduledFuture;
   }

   public EffectTickTask getEffectTask() {
      return this._effectTask;
   }

   public ScheduledFuture<?> getScheduledFuture() {
      return this._scheduledFuture;
   }
}
