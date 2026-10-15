/*
 * Custom feature: //oly — remote registration for the Grand Olympiad non-classed matches.
 * Available to players: declared in AdminCommands.xml with accessLevel="0", and the core
 * (AdminCommandHandler) executes admin commands for non-GM players when the command access
 * level is available to the player's access level (hasAccess).
 */
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

import handlers.chat.commands.voiced.Oly;

/**
 * //oly — удалённая регистрация на бесклассовую Гранд Олимпиаду (то же, что .oly и /oly).
 * Клиент отправляет текст //oly как bypass admin_oly, команда обработчиком попадает сюда.
 * @author custom
 */
public class AdminOly implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_oly"
	};
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		String params = "";
		final int index = command.indexOf(' ');
		if (index > 0)
		{
			params = command.substring(index + 1);
		}
		Oly.dispatch(player, params);
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
