package org.l2jmobius.gameserver.handler;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;

public class ActionClickHandler implements IHandler<IActionClickHandler, InstanceType> {
   private final Map<InstanceType, IActionClickHandler> _actions = new EnumMap<>(InstanceType.class);

   protected ActionClickHandler() {
   }

   public void registerHandler(IActionClickHandler handler) {
      this._actions.put(handler.getInstanceType(), handler);
   }

   public synchronized void removeHandler(IActionClickHandler handler) {
      this._actions.remove(handler.getInstanceType());
   }

   public IActionClickHandler getHandler(InstanceType iType) {
      IActionClickHandler result = null;

      for (InstanceType t = iType; t != null; t = t.getParent()) {
         result = this._actions.get(t);
         if (result != null) {
            break;
         }
      }

      return result;
   }

   @Override
   public int size() {
      return this._actions.size();
   }

   public static ActionClickHandler getInstance() {
      return ActionClickHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ActionClickHandler INSTANCE = new ActionClickHandler();
   }
}
