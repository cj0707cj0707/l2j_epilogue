package org.l2jmobius.gameserver.entity.actor.tasks.cubics;

import org.l2jmobius.gameserver.entity.actor.instance.Cubic;

public class CubicDisappear implements Runnable {
   private final Cubic _cubic;

   public CubicDisappear(Cubic cubic) {
      this._cubic = cubic;
   }

   @Override
   public void run() {
      if (this._cubic != null) {
         this._cubic.getOwner().removeCubic(this._cubic.getId());
         this._cubic.getOwner().broadcastUserInfo();
      }
   }
}
