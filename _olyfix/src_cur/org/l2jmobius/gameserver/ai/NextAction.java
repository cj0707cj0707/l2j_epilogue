package org.l2jmobius.gameserver.ai;

public class NextAction {
   private final Action _action;
   private final Intention _intention;
   private final NextAction.Callback _callback;

   public NextAction(Action action, Intention intention, NextAction.Callback callback) {
      this._action = action;
      this._intention = intention;
      this._callback = callback;
   }

   public boolean isTriggeredBy(Action action) {
      return this._action == action;
   }

   public boolean isRemovedBy(Intention intention) {
      return this._intention == intention;
   }

   public void doAction() {
      this._callback.doAction();
   }

   public interface Callback {
      void doAction();
   }
}
