package org.l2jmobius.gameserver.handler;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;

public interface IChatHandler {
   void onChat(ChatType var1, Player var2, String var3, String var4);

   ChatType[] getChatTypeList();
}
