package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoPlaySettingsHolder {
   private final AtomicInteger _options = new AtomicInteger();
   private final AtomicBoolean _pickup = new AtomicBoolean(true);
   private final AtomicInteger _nextTargetMode = new AtomicInteger(1);
   private final AtomicBoolean _shortRange = new AtomicBoolean();
   private final AtomicBoolean _respectfulHunting = new AtomicBoolean(true);
   private final AtomicInteger _autoPotionPercent = new AtomicInteger(50);

   public int getOptions() {
      return this._options.get();
   }

   public void setOptions(int options) {
      this._options.set(options);
   }

   public boolean doPickup() {
      return this._pickup.get();
   }

   public void setPickup(boolean value) {
      this._pickup.set(value);
   }

   public int getNextTargetMode() {
      return this._nextTargetMode.get();
   }

   public void setNextTargetMode(int nextTargetMode) {
      this._nextTargetMode.set(nextTargetMode);
   }

   public boolean isShortRange() {
      return this._shortRange.get();
   }

   public void setShortRange(boolean value) {
      this._shortRange.set(value);
   }

   public boolean isRespectfulHunting() {
      return this._respectfulHunting.get();
   }

   public void setRespectfulHunting(boolean value) {
      this._respectfulHunting.set(value);
   }

   public int getAutoPotionPercent() {
      return this._autoPotionPercent.get();
   }

   public void setAutoPotionPercent(int value) {
      this._autoPotionPercent.set(value);
   }
}
