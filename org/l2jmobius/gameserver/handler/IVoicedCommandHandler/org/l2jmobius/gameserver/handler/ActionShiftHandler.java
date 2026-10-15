package org.l2jmobius.gameserver.handler;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;

public class ActionShiftHandler implements IHandler<IActionShiftHandler, InstanceType> {
   private final Map<InstanceType, IActionShiftHandler> _actionsShift = new EnumMap<>(InstanceType.class);

   protected ActionShiftHandler() {
   }

   public void registerHandler(IActionShiftHandler handler) {
      this._actionsShift.put(handler.getInstanceType(), handler);
   }

   public synchronized void removeHandler(IActionShiftHandler handler) {
      this._actionsShift.remove(handler.getInstanceType());
   }

   public IActionShiftHandler getHandler(InstanceType iType) {
      IActionShiftHandler result = null;

      for (InstanceType t = iType; t != null; t = t.getParent()) {
         result = this._actionsShift.get(t);
         if (result != null) {
            break;
         }
      }

      return result;
   }

   @Override
   public int size() {
      return this._actionsShift.size();
   }

   public static ActionShiftHandler getInstance() {
      return ActionShiftHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ActionShiftHandler INSTANCE = new ActionShiftHandler();
   }
}
