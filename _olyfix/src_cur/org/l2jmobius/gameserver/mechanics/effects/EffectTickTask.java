package org.l2jmobius.gameserver.mechanics.effects;

import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

public class EffectTickTask implements Runnable {
   private final BuffInfo _info;
   private final AbstractEffect _effect;

   public EffectTickTask(BuffInfo info, AbstractEffect effect) {
      this._info = info;
      this._effect = effect;
   }

   public BuffInfo getBuffInfo() {
      return this._info;
   }

   public AbstractEffect getEffect() {
      return this._effect;
   }

   @Override
   public void run() {
      this._info.onTick(this._effect);
   }
}
