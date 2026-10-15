/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * @author UnAfraid
 */
public class AdminCommandHandler implements IHandler<IAdminCommandHandler, String>
{
	private static final Logger LOGGER = Logger.getLogger(AdminCommandHandler.class.getName());

	private final Map<String, IAdminCommandHandler> _datatable = new HashMap<>();

	protected AdminCommandHandler()
	{
	}

	public void registerHandler(IAdminCommandHandler handler)
	{
		for (String id : handler.getCommandList())
		{
			_datatable.put(id, handler);
		}
	}

	public synchronized void removeHandler(IAdminCommandHandler handler)
	{
		_datatable.remove(handler);
	}

	public IAdminCommandHandler getHandler(String adminCommand)
	{
		String command = adminCommand;
		if (adminCommand.contains(" "))
		{
			command = adminCommand.substring(0, adminCommand.indexOf(' '));
		}

		return _datatable.get(command);
	}

	public void onCommand(Player player, String fullCommand, boolean useConfirm)
	{
		final String command = fullCommand.split(" ")[0];
		// CUSTOM: admin commands are no longer GM-only. A non-GM player may execute a command
		// when it is explicitly available to his access level in AdminCommands.xml
		// (AdminData.hasAccess) - used by the player command //oly (accessLevel="0").
		// For GMs the behavior is unchanged (they enter the gate, then the same hasAccess
		// check below still applies); for non-GMs every regular command (accessLevel >= 10)
		// is still denied by AdminData.hasAccess, exactly as before.
		if (player.isGM() || AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
		{
			final String commandNoPrefix = command.substring(6);
			final IAdminCommandHandler handler = getHandler(command);
			if (handler == null)
			{
				player.sendMessage("The command '" + commandNoPrefix + "' does not exist!");
				LOGGER.warning("No handler registered for admin command '" + command + "'");
			}
			else if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command!");
				LOGGER.warning(player + " tried to use admin command '" + command + "', without proper access level!");
			}
			else
			{
				if (useConfirm && AdminData.getInstance().requireConfirm(command))
				{
					player.setAdminConfirmCmd(fullCommand);
					final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_3);
					dlg.getSystemMessage().addString("Are you sure you want execute command '" + commandNoPrefix + "' ?");
					player.addAction(PlayerAction.ADMIN_COMMAND);
					player.sendPacket(dlg);
				}
				else
				{
					ThreadPool.execute(() ->
					{
						final long begin = System.currentTimeMillis();

						try
						{
							if (GeneralConfig.GMAUDIT)
							{
								final WorldObject target = player.getTarget();
								GMAudit.logAction(player.getName() + " [" + player.getObjectId() + "]", fullCommand, target != null ? target.getName() : "no-target");
							}

							handler.onCommand(fullCommand, player);
						}
						catch (RuntimeException e)
						{
							player.sendMessage("Exception during execution of  '" + fullCommand + "': " + e.toString());
							LOGGER.log(Level.WARNING, "Exception during execution of " + fullCommand, e);
						}
						finally
						{
							final long runtime = System.currentTimeMillis() - begin;
							if (runtime > 5000)
							{
								player.sendMessage("The execution of '" + fullCommand + "' took " + TimeUtil.formatDuration(runtime) + ".");
							}
						}
					});
				}
			}
		}
	}

	public int size()
	{
		return _datatable.size();
	}

	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AdminCommandHandler INSTANCE = new AdminCommandHandler();
	}
}
