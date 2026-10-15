package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

public class VoicedCommandHandler implements IHandler<IVoicedCommandHandler, String> {
   private final Map<String, IVoicedCommandHandler> _datatable = new HashMap<>();

   protected VoicedCommandHandler() {
   }

   public void registerHandler(IVoicedCommandHandler handler) {
      for (String id : handler.getCommandList()) {
         this._datatable.put(id, handler);
      }
   }

   public synchronized void removeHandler(IVoicedCommandHandler handler) {
      for (String id : handler.getCommandList()) {
         this._datatable.remove(id);
      }
   }

   public IVoicedCommandHandler getHandler(String voicedCommand) {
      return this._datatable.get(voicedCommand.contains(" ") ? voicedCommand.substring(0, voicedCommand.indexOf(32)) : voicedCommand);
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public static VoicedCommandHandler getInstance() {
      return VoicedCommandHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
   }
}
