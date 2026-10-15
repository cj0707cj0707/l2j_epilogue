package org.l2jmobius.gameserver.handler;

import org.l2jmobius.gameserver.entity.actor.Player;

public interface IWriteBoardHandler extends IParseBoardHandler {
   boolean writeCommunityBoardCommand(Player var1, String var2, String var3, String var4, String var5, String var6);
}
