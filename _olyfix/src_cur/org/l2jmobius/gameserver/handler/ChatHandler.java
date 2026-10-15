package org.l2jmobius.gameserver.handler;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class ChatHandler implements IHandler<IChatHandler, ChatType> {
   private final Map<ChatType, IChatHandler> _datatable = new EnumMap<>(ChatType.class);

   protected ChatHandler() {
   }

   public void registerHandler(IChatHandler handler) {
      for (ChatType type : handler.getChatTypeList()) {
         this._datatable.put(type, handler);
      }
   }

   public synchronized void removeHandler(IChatHandler handler) {
      for (ChatType type : handler.getChatTypeList()) {
         this._datatable.remove(type);
      }
   }

   public IChatHandler getHandler(ChatType chatType) {
      return this._datatable.get(chatType);
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public static ChatHandler getInstance() {
      return ChatHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ChatHandler INSTANCE = new ChatHandler();
   }
}
