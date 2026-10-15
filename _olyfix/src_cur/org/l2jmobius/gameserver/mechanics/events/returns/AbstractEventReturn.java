package org.l2jmobius.gameserver.mechanics.events.returns;

public abstract class AbstractEventReturn {
   private final boolean _override;
   private final boolean _abort;

   public AbstractEventReturn(boolean override, boolean abort) {
      this._override = override;
      this._abort = abort;
   }

   public boolean override() {
      return this._override;
   }

   public boolean abort() {
      return this._abort;
   }
}
