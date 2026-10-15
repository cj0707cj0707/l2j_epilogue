package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

public class BypassHandler implements IHandler<IBypassHandler, String> {
   private final Map<String, IBypassHandler> _datatable = new HashMap<>();

   protected BypassHandler() {
   }

   public void registerHandler(IBypassHandler handler) {
      for (String element : handler.getCommandList()) {
         this._datatable.put(element.toLowerCase(), handler);
      }
   }

   public synchronized void removeHandler(IBypassHandler handler) {
      for (String element : handler.getCommandList()) {
         this._datatable.remove(element.toLowerCase());
      }
   }

   public IBypassHandler getHandler(String commandValue) {
      String command = commandValue;
      if (commandValue.contains(" ")) {
         command = commandValue.substring(0, commandValue.indexOf(32));
      }

      return this._datatable.get(command.toLowerCase());
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public static BypassHandler getInstance() {
      return BypassHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final BypassHandler INSTANCE = new BypassHandler();
   }
}
