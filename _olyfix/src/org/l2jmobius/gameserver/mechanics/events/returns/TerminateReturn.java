package org.l2jmobius.gameserver.mechanics.events.returns;

public class TerminateReturn extends AbstractEventReturn {
   private final boolean _terminate;

   public TerminateReturn(boolean terminate, boolean override, boolean abort) {
      super(override, abort);
      this._terminate = terminate;
   }

   public boolean terminate() {
      return this._terminate;
   }
}
