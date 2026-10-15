/*
 * Custom feature: remote registration for the Grand Olympiad non-classed matches.
 * Duplicates the Grand Olympiad Manager registration (bypass "OlympiadNoble 4" / "1")
 * without the need to approach the manager NPC.
 *
 * Invoked as .oly (voiced), /oly (alias in ChatGeneral) and //oly (admin_oly, accessLevel 0).
 */
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Удалённая регистрация на Гранд Олимпиаду (бесклассовые бои 1x1, без привязки к классу).
 * Вся валидация (ноблес, базовый класс, очки >= 5, инвентарь <= 80%, период олимпиады)
 * выполняется ядром в Olympiad.registerNoble(player, false) — тот же вход, что и у
 * кнопки регистрации у Grand Olympiad Manager, включая все штатные системные сообщения.
 */
public class Oly implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"oly"
	};
	
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		dispatch(player, params);
		return true;
	}
	
	/**
	 * Общая точка входа для .oly (voiced), /oly (алиас в ChatGeneral) и //oly (admin_oly).
	 * @param player игрок
	 * @param params аргументы команды (пусто = регистрация)
	 */
	public static void dispatch(Player player, String params)
	{
		final String param = (params == null) ? "" : params.trim().toLowerCase();
		switch (param)
		{
			case "":
			case "reg":
			case "register":
			{
				register(player);
				break;
			}
			case "cancel":
			case "unreg":
			case "unregister":
			{
				unRegister(player);
				break;
			}
			case "status":
			case "stat":
			{
				status(player);
				break;
			}
			default:
			{
				sendUsage(player);
			}
		}
	}
	
	/**
	 * Регистрация на бесклассовую олимпиаду — эквивалент кнопки менеджера
	 * "регистрация без привязки к классу" (Olympiad.registerNoble(player, false)).
	 * @param player игрок
	 */
	public static void register(Player player)
	{
		if (!OlympiadConfig.OLYMPIAD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}
		if (player.isInOlympiadMode() || (player.getOlympiadGameId() != -1))
		{
			player.sendMessage("Ты уже участвуешь в олимпиаде.");
			return;
		}
		if (player.inObserverMode())
		{
			player.sendMessage("Нельзя регистрироваться, пока ты наблюдаешь за боем.");
			return;
		}
		
		// Все отказы (не ноблес, сабкласс, меньше 5 очков, инвентарь > 80%, уже в списке,
		// период олимпиады закрыт) ядро сообщает игроку само штатными системными сообщениями.
		if (Olympiad.getInstance().registerNoble(player, false))
		{
			final int[] waitingList = Olympiad.getInstance().getWaitingList();
			final int nonClassed = (waitingList != null) ? waitingList[1] : 0;
			player.sendMessage("Регистрация на бесклассовую олимпиаду принята — подходить к менеджеру не нужно, соперник будет найден автоматически. В листе ожидания: " + nonClassed + ".");
		}
	}
	
	/**
	 * Отмена регистрации — эквивалент кнопки менеджера "отмена участия"
	 * (Olympiad.unRegisterNoble(player)); все отказы (не зарегистрирован, уже выбран в матч)
	 * ядро сообщает само.
	 * @param player игрок
	 */
	public static void unRegister(Player player)
	{
		if (!OlympiadConfig.OLYMPIAD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}
		Olympiad.getInstance().unRegisterNoble(player);
	}
	
	/**
	 * Статистика: период, очки, бои, лист ожидания.
	 * @param player игрок
	 */
	public static void status(Player player)
	{
		if (!player.isNoble())
		{
			player.sendPacket(SystemMessageId.THIS_COMMAND_CAN_ONLY_BE_USED_BY_A_NOBLESSE);
			return;
		}
		
		final Olympiad olympiad = Olympiad.getInstance();
		final int objectId = player.getObjectId();
		player.sendMessage("Гранд Олимпиада: период " + (olympiad.inCompPeriod() ? "активен" : "закрыт")
			+ " | очки: " + olympiad.getNoblePoints(objectId)
			+ " | бои: " + olympiad.getCompetitionDone(objectId)
			+ " (побед: " + olympiad.getCompetitionWon(objectId)
			+ ", поражений: " + olympiad.getCompetitionLost(objectId) + ")");
		if (olympiad.isRegisteredInComp(player))
		{
			player.sendMessage("Ты сейчас в листе ожидания олимпиады.");
		}
		final int[] waitingList = olympiad.getWaitingList();
		if (waitingList != null)
		{
			player.sendMessage("Всего в листе ожидания: классовые " + waitingList[0] + ", бесклассовые " + waitingList[1] + ".");
		}
	}
	
	private static void sendUsage(Player player)
	{
		player.sendMessage("Использование: .oly (регистрация на бесклассовую олимпиаду) | .oly cancel (отмена) | .oly status (статистика).");
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
