package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

public class UserCommandHandler implements IHandler<IUserCommandHandler, Integer> {
   private final Map<Integer, IUserCommandHandler> _datatable = new HashMap<>();

   protected UserCommandHandler() {
   }

   public void registerHandler(IUserCommandHandler handler) {
      for (int id : handler.getCommandList()) {
         this._datatable.put(id, handler);
      }
   }

   public synchronized void removeHandler(IUserCommandHandler handler) {
      for (int id : handler.getCommandList()) {
         this._datatable.remove(id);
      }
   }

   public IUserCommandHandler getHandler(Integer userCommand) {
      return this._datatable.get(userCommand);
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public static UserCommandHandler getInstance() {
      return UserCommandHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final UserCommandHandler INSTANCE = new UserCommandHandler();
   }
}
