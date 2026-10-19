/*
 * Fake Players AI - admin command //bot with HTML management menu.
 */
package handlers.chat.commands.admin;

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;

import custom.fakeplayersai.BotClassPreset;
import custom.fakeplayersai.BotManager;
import custom.fakeplayersai.BotPresets;
import custom.fakeplayersai.BotSettings;
import custom.fakeplayersai.BotSpawner;

/**
 * @author FakePlayersAI
 */
public class AdminBot implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_bot"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // admin_bot
		
		String sub = "main";
		if (st.hasMoreTokens())
		{
			sub = st.nextToken();
		}
		
		final BotSettings settings = BotManager.getSettings();
		
		switch (sub)
		{
			case "spawnall":
			{
				BotManager.spawnAll();
				sendMain(activeChar, "Спавн выполнен.");
				break;
			}
			case "spawn":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final BotClassPreset preset = BotPresets.getPresetByClassId(classId);
				if (preset == null)
				{
					sendMain(activeChar, "Неизвестный класс: " + classId);
					break;
				}
				int count = st.hasMoreTokens() ? Math.max(1, Math.min(20, parse(st.nextToken(), 1))) : 1;
				int spawned = 0;
				while ((count-- > 0) && (BotManager.spawnBot(preset) != null))
				{
					spawned++;
				}
				sendMain(activeChar, "Заспавнено " + spawned + " ботов: " + preset.getClassName());
				break;
			}
			case "despawnall":
			{
				final int total = BotManager.getBots().size();
				BotManager.despawnAll();
				sendMain(activeChar, "Убрано ботов: " + total);
				break;
			}
			case "deleteall":
			{
				final int total = BotManager.deleteAll();
				sendMain(activeChar, "Удалено ботов (записи в БД остались): " + total);
				break;
			}
			case "count":
			{
				if (st.hasMoreTokens())
				{
					settings.setBotCount(parse(st.nextToken(), settings.getBotCount()));
					settings.save();
				}
				sendMain(activeChar, "BotCount = " + settings.getBotCount() + " (применится по //bot spawnall)");
				break;
			}
			case "encharmor":
			{
				if (st.hasMoreTokens())
				{
					settings.setEnchantArmor(parse(st.nextToken(), settings.getEnchantArmor()));
					settings.save();
				}
				final int changed = applyEnchantToBots(false, true, false);
				sendMain(activeChar, "Заточка брони: +" + settings.getEnchantArmor() + ". Перезаточено у живых ботов предметов: " + changed + " (оружие сейчас +" + settings.getEnchantWeapon() + ", бижутерия +" + settings.getEnchantJewel() + "). Новым ботам применяется автоматически.");
				break;
			}
			case "enchweapon":
			{
				if (st.hasMoreTokens())
				{
					settings.setEnchantWeapon(parse(st.nextToken(), settings.getEnchantWeapon()));
					settings.save();
				}
				final int changed = applyEnchantToBots(true, false, false);
				sendMain(activeChar, "Заточка оружия: +" + settings.getEnchantWeapon() + ". Перезаточено у живых ботов предметов: " + changed + " (броня сейчас +" + settings.getEnchantArmor() + ", бижутерия +" + settings.getEnchantJewel() + "). Новым ботам применяется автоматически.");
				break;
			}
			case "enchjewel":
			{
				if (st.hasMoreTokens())
				{
					settings.setEnchantJewel(parse(st.nextToken(), settings.getEnchantJewel()));
					settings.save();
				}
				final int changed = applyEnchantToBots(false, false, true);
				sendMain(activeChar, "Заточка бижутерии: +" + settings.getEnchantJewel() + ". Перезаточено у живых ботов предметов: " + changed + " (броня сейчас +" + settings.getEnchantArmor() + ", оружие +" + settings.getEnchantWeapon() + "). Новым ботам применяется автоматически.");
				break;
			}
			case "reenchant":
			{
				final int changed = applyEnchantToBots(true, true, true);
				sendMain(activeChar, "Перезаточка живых ботов по текущим настройкам: обновлено предметов " + changed + " (броня +" + settings.getEnchantArmor() + ", оружие +" + settings.getEnchantWeapon() + ", бижутерия +" + settings.getEnchantJewel() + ").");
				break;
			}
			case "set":
			{
				if (st.hasMoreTokens())
				{
					settings.setUseS84(st.nextToken().equalsIgnoreCase("s84"));
					settings.save();
				}
				sendMain(activeChar, "Град сетов: " + (settings.isUseS84() ? "S84 Vesper" : "S80 Dynasty"));
				break;
			}
			case "regear":
			{
				int count = 0;
				for (Player bot : BotManager.getBots().values())
				{
					final BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(bot));
					if (preset != null)
					{
						BotSpawner.equipGear(bot, preset);
						count++;
					}
				}
				sendMain(activeChar, "Переснаряжено ботов: " + count);
				break;
			}
			case "enchskills":
			{
				int count = 0;
				for (Player bot : BotManager.getBots().values())
				{
					count += BotSpawner.applySkillEnchants(bot);
				}
				sendMain(activeChar, "Заточено скиллов (дебафы: Chance, остальные: прежний маршрут; до native максимума): " + count + ". Новым ботам заточка выдаётся автоматически.");
				break;
			}
			case "olympiad":
			{
				if (st.hasMoreTokens())
				{
					settings.setAutoRegisterOlympiad(st.nextToken().equalsIgnoreCase("on"));
					settings.save();
				}
				sendMain(activeChar, "Авто-регистрация на олимпиаду: " + (settings.isAutoRegisterOlympiad() ? "ВКЛ" : "ВЫКЛ"));
				break;
			}
			case "register":
			{
				int count = 0;
				final Olympiad olympiad = Olympiad.getInstance();
				final boolean period = olympiad.inCompPeriod();
				if (period)
				{
					for (Player bot : BotManager.getBots().values())
					{
						if (bot.isNoble() && !bot.isInOlympiadMode() && !olympiad.isRegisteredInComp(bot) && olympiad.registerNoble(bot, false))
						{
							count++;
						}
					}
				}
				sendMain(activeChar, "Зарегистрировано на олимпиаду: " + count + (period ? "" : " (сейчас НЕ период олимпиады, регистрация откроется автоматически)"));
				break;
			}
			case "olyforce":
			{
				// Force-registration button: points are topped up first, then every eligible
				// noble bot is registered for the olympiad right now.
				final int result = BotManager.forceOlympiadRegister();
				sendMain(activeChar, result >= 0 ? ("Принудительно зарегистрировано на олимпиаду: " + result + " ботов.") : "Сейчас НЕ период олимпиады — принудительная регистрация невозможна.");
				break;
			}
			case "pause":
			{
				// Pause button: freezes the coliseum fights (world bots stop attacking and
				// casting) and revives all dead bots. Olympiad matches in progress continue.
				final int revived = BotManager.pauseCombat();
				sendMain(activeChar, "Бои в колизее поставлены на паузу. Воскрешено погибших ботов: " + revived + ".");
				break;
			}
			case "resume":
			{
				BotManager.resumeCombat();
				sendMain(activeChar, "Бои в колизее возобновлены.");
				break;
			}
			case "respawnall":
			{
				// Restart button: despawn every bot (not inside an active olympiad match) and
				// run the regular startup spawn 5 seconds later.
				final int despawned = BotManager.despawnAllForRestart();
				sendMain(activeChar, "Убрано ботов из мира: " + despawned + ". Спавн через 5 секунд (как при старте сервера).");
				break;
			}
			case "move":
			{
				final int radius = st.hasMoreTokens() ? parse(st.nextToken(), settings.getSpawnRadius()) : settings.getSpawnRadius();
				settings.setSpawnPoint(activeChar.getX(), activeChar.getY(), activeChar.getZ(), radius);
				settings.save();
				sendMain(activeChar, "Точка спавна: " + settings.getSpawnX() + ", " + settings.getSpawnY() + ", " + settings.getSpawnZ() + " (радиус " + settings.getSpawnRadius() + ")");
				break;
			}
			case "potion":
			{
				if (st.hasMoreTokens())
				{
					settings.setAutoPotionPercent(parse(st.nextToken(), settings.getAutoPotionPercent()));
					settings.save();
				}
				sendMain(activeChar, "AutoPotionPercent = " + settings.getAutoPotionPercent());
				break;
			}
			case "respawn":
			{
				if (st.hasMoreTokens())
				{
					settings.setRespawnSeconds(parse(st.nextToken(), settings.getRespawnSeconds()));
					settings.save();
				}
				sendMain(activeChar, "RespawnSeconds = " + settings.getRespawnSeconds());
				break;
			}
			case "rotation":
			case "skills":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				sendClassSkillsPage(activeChar, classId, "");
				break;
			}
			case "rotadd":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final int skillId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final BotClassPreset preset = BotPresets.getPresetByClassId(classId);
				if ((preset != null) && (skillId > 0) && BotPresets.rotationAdd(classId, skillId))
				{
					BotPresets.saveOverrides();
					BotManager.applyRotationToBots(preset);
					sendClassSkillsPage(activeChar, classId, "Скилл " + skillId + " разрешён (добавлен в ротацию) для " + preset.getClassName() + ".");
				}
				else
				{
					sendClassSkillsPage(activeChar, classId, "Не удалось разрешить: скилл не относится к классу или уже разрешён.");
				}
				break;
			}
			case "rotdel":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final int skillId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				if (BotPresets.rotationRemove(classId, skillId))
				{
					BotPresets.saveOverrides();
					BotManager.applyRotationToBots(BotPresets.getPresetByClassId(classId));
				}
				sendClassSkillsPage(activeChar, classId, "Скилл " + skillId + " запрещён (удалён из ротации).");
				break;
			}
			case "buffadd":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final int skillId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final BotClassPreset preset = BotPresets.getPresetByClassId(classId);
				if ((preset != null) && (skillId > 0) && BotPresets.buffAdd(classId, skillId))
				{
					BotPresets.saveOverrides();
					BotManager.applyRotationToBots(preset);
					sendClassSkillsPage(activeChar, classId, "Скилл " + skillId + " разрешён (селф-бафф) для " + preset.getClassName() + ".");
				}
				else
				{
					sendClassSkillsPage(activeChar, classId, "Не удалось разрешить: скилл должен быть селф-баффом этого класса.");
				}
				break;
			}
			case "buffdel":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final int skillId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				if (BotPresets.buffRemove(classId, skillId))
				{
					BotPresets.saveOverrides();
					BotManager.applyRotationToBots(BotPresets.getPresetByClassId(classId));
				}
				sendClassSkillsPage(activeChar, classId, "Скилл " + skillId + " запрещён (удалён из селф-баффов).");
				break;
			}
			case "rotreset":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				BotPresets.rotationReset(classId);
				BotPresets.buffReset(classId);
				BotPresets.saveOverrides();
				sendClassSkillsPage(activeChar, classId, "Набор скиллов класса сброшен к умолчанию (ротация и селф-баффы).");
				break;
			}
			case "weapon":
			{
				final int classId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				final int itemId = st.hasMoreTokens() ? parse(st.nextToken(), 0) : 0;
				BotPresets.setWeaponOverride(classId, itemId);
				BotPresets.saveOverrides();
				sendClassSkillsPage(activeChar, classId, "Оружие класса: " + (itemId > 0 ? ("item " + itemId) : "по умолчанию"));
				break;
			}
			case "reload":
			{
				settings.load();
				BotPresets.loadOverrides();
				sendMain(activeChar, "Конфигурация перечитана.");
				break;
			}
			case "list":
			{
				sendListPage(activeChar);
				break;
			}
			default:
			{
				sendMain(activeChar, "");
				break;
			}
		}
		return true;
	}
	
	private static int parse(String s, int def)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (Exception e)
		{
			return def;
		}
	}
	
	/**
	 * Applies the current enchant settings to the equipped gear of all live bots
	 * (in place, without re-creating items).
	 * @param enchantWeaponItems apply EnchantWeapon to equipped weapons
	 * @param enchantArmorItems apply EnchantArmor to equipped armor pieces and shields
	 * @param enchantJewelItems apply EnchantJewel to equipped jewelry (earrings, rings, necklace)
	 * @return number of items whose enchant level was changed
	 */
	private static int applyEnchantToBots(boolean enchantWeaponItems, boolean enchantArmorItems, boolean enchantJewelItems)
	{
		int changed = 0;
		for (Player bot : BotManager.getBots().values())
		{
			changed += BotSpawner.reenchantGear(bot, enchantWeaponItems, enchantArmorItems, enchantJewelItems);
		}
		return changed;
	}
	
	// ------------------------------------------------------------------
	// HTML pages
	// ------------------------------------------------------------------
	
	private static void sendMain(Player gm, String message)
	{
		final BotSettings s = BotManager.getSettings();
		final StringBuilder html = new StringBuilder(8192);
		html.append("<html><head><title>Fake Players AI</title></head><body><center><br>");
		html.append("<font color=\"LEVEL\">=== Fake Players AI: управление ботами ===</font><br>");
		if ((message != null) && !message.isEmpty())
		{
			html.append("<font color=\"00FF00\">").append(message).append("</font><br>");
		}
		html.append("<table width=750><tr><td width=375 valign=top><table width=355>");
		html.append(row("Ботов в мире", String.valueOf(BotManager.getBots().size())));
		html.append(row("Норма ботов", String.valueOf(s.getBotCount())));
		html.append(row("Сеты", s.isUseS84() ? "S84 Vesper" : "S80 Dynasty"));
		html.append(row("Заточка броня/оружие/бижут.", "+" + s.getEnchantArmor() + " / +" + s.getEnchantWeapon() + " / +" + s.getEnchantJewel()));
		html.append(row("Авто-регистрация олимп.", s.isAutoRegisterOlympiad() ? "ВКЛ" : "ВЫКЛ"));
		html.append(row("Период олимпиады", Olympiad.getInstance().inCompPeriod() ? "АКТИВЕН" : "закрыт"));
		html.append(row("Бои (колизей)", BotManager.isCombatPaused() ? "ПАУЗА" : "активны"));
		html.append("</table></td><td width=375 valign=top><table width=355>");
		html.append(row("Точка спавна", s.getSpawnX() + ", " + s.getSpawnY() + ", " + s.getSpawnZ()));
		html.append(row("Радиус", String.valueOf(s.getSpawnRadius())));
		html.append(row("Респавн", s.getRespawnSeconds() + " сек"));
		html.append(row("Авто-потки (HP %)", String.valueOf(s.getAutoPotionPercent())));
		html.append("</table></td></tr></table><br>");
		
		html.append("<table width=750><tr>");
		html.append(btn("spawnall", "Заспавнить всех", 240));
		html.append(btn("despawnall", "Убрать всех", 240));
		html.append(btn("list", "Список ботов", 240));
		html.append("</tr><tr>");
		html.append(btn("regear", "Переснарядить всех", 240));
		html.append(btn("enchskills", "Заточить скиллы", 240));
		html.append(btn("register", "Регистрация на олимп.", 240));
		html.append("</tr><tr>");
		html.append(btn("deleteall", "Удалить из мира и реестра", 240));
		html.append(btn("move 400", "Спавн здесь (радиус 400)", 240));
		html.append(btn("reload", "Перечитать конфиг", 240));
		html.append("</tr></table><br>");
		
		html.append("<font color=\"LEVEL\">Управление боями и олимпиадой</font><br1>");
		html.append("<table width=750><tr>");
		html.append(btn("pause", "1. Пауза боёв + воскрешение", 180));
		html.append(btn("resume", "2. Возобновить бои", 180));
		html.append(btn("olyforce", "3. Регистрация всех на олимп.", 180));
		html.append(btn("respawnall", "4. Перезапуск ботов (5 сек)", 180));
		html.append("</tr></table><br>");
		
		html.append("<font color=\"LEVEL\">Заточка экипировки (применяется к живым ботам и новым ботам)</font><br1>");
		html.append("<table width=750><tr>");
		html.append("<td width=250 align=center><font color=\"AAAAAA\">Броня (сейчас +").append(s.getEnchantArmor()).append("):</font> <edit var=\"ea\" width=40 height=15 length=2></td>");
		html.append("<td width=250 align=center><font color=\"AAAAAA\">Оружие (сейчас +").append(s.getEnchantWeapon()).append("):</font> <edit var=\"ew\" width=40 height=15 length=2></td>");
		html.append("<td width=250 align=center><font color=\"AAAAAA\">Бижутерия (сейчас +").append(s.getEnchantJewel()).append("):</font> <edit var=\"ej\" width=40 height=15 length=2></td>");
		html.append("</tr><tr>");
		html.append("<td align=center>").append(btn("encharmor $ea", "Применить заточку брони", 240)).append("</td>");
		html.append("<td align=center>").append(btn("enchweapon $ew", "Применить заточку оружия", 240)).append("</td>");
		html.append("<td align=center>").append(btn("enchjewel $ej", "Применить заточку бижутерии", 240)).append("</td>");
		html.append("</tr></table>");
		html.append(btn("reenchant", "Перезаточить всех по текущим значениям", 300));
		html.append("<br>");
		
		html.append("<font color=\"LEVEL\">Скиллы ботов по классам (полный список, разрешить/запретить)</font><br1>");
		html.append("<table width=750><tr>");
		final Collection<BotClassPreset> presets = BotPresets.getPresets();
		int i = 0;
		for (BotClassPreset preset : presets)
		{
			if ((i > 0) && ((i % 3) == 0))
			{
				html.append("</tr><tr>");
			}
			html.append("<td width=250><a action=\"bypass -h admin_bot rotation ").append(preset.getClassId()).append("\">").append(preset.getClassName()).append("</a></td>");
			i++;
		}
		while ((i % 3) != 0)
		{
			html.append("<td width=250></td>");
			i++;
		}
		html.append("</tr></table>");
		
		html.append("</center></body></html>");
		sendHtml(gm, html.toString());
	}
	
	private static void sendListPage(Player gm)
	{
		final StringBuilder html = new StringBuilder(8192);
		html.append("<html><head><title>Fake Players AI</title></head><body><center><br>");
		html.append("<font color=\"LEVEL\">=== Боты в мире (").append(BotManager.getBots().size()).append(") ===</font><br><br>");
		html.append("<table width=750><tr><td width=130><font color=\"AAAAAA\">Имя</font></td><td width=180><font color=\"AAAAAA\">Класс</font></td><td width=40><font color=\"AAAAAA\">Ур.</font></td><td width=300><font color=\"AAAAAA\">Оружие</font></td><td width=100><font color=\"AAAAAA\">Статус</font></td></tr>");
		for (Player bot : BotManager.getBots().values())
		{
			final BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(bot));
			html.append("<tr><td width=130>").append(bot.getName()).append("</td>");
			html.append("<td width=180>").append(preset != null ? preset.getClassName() : bot.getPlayerClass().name()).append("</td>");
			html.append("<td width=40>").append(bot.getLevel()).append("</td>");
			final Item weapon = bot.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			html.append("<td width=300>").append(weapon != null ? weapon.getItemName() : "-").append("</td>");
			html.append("<td width=100>").append(bot.isAlikeDead() ? "<font color=\"FF0000\">мёртв</font>" : (bot.isInOlympiadMode() ? "олимпиада" : "жив")).append("</td>");
			html.append("</tr>");
		}
		html.append("</table><br>");
		html.append(btn("main", "Назад", 200));
		html.append("</center></body></html>");
		sendHtml(gm, html.toString());
	}
	
	/**
	 * Class skills page: current rotation + self-buffs on top, then the FULL skill list of
	 * the class where every skill can be allowed/forbidden by the admin. The allowed set
	 * (rotation + buffs) is persisted in FakePlayersAIOverrides.ini and survives restarts.
	 */
	private static void sendClassSkillsPage(Player gm, int classId, String message)
	{
		final BotClassPreset preset = BotPresets.getPresetByClassId(classId);
		if (preset == null)
		{
			sendMain(gm, "Сначала выбери класс.");
			return;
		}
		
		final int[] rotation = preset.getRotation();
		final int[] buffs = preset.getBuffs();
		
		final StringBuilder html = new StringBuilder(16384);
		html.append("<html><head><title>Fake Players AI</title></head><body><center><br>");
		html.append("<font color=\"LEVEL\">Скиллы класса: ").append(preset.getClassName()).append(" (classId ").append(preset.getClassId()).append(")</font><br>");
		if ((message != null) && !message.isEmpty())
		{
			html.append("<font color=\"00FF00\">").append(message).append("</font><br>");
		}
		
		// Current usable set: rotation (ordered) + self-buffs.
		html.append("<table width=750><tr><td width=375 valign=top><font color=\"99FF99\">Ротация (по порядку):</font><table width=355>");
		if (rotation.length == 0)
		{
			html.append("<tr><td>—</td></tr>");
		}
		for (int i = 0; i < rotation.length; i++)
		{
			final int skillId = rotation[i];
			html.append("<tr><td width=280>").append(i + 1).append(". ").append(BotPresets.getSkillName(skillId)).append(" [").append(skillId).append("]</td>");
			html.append("<td width=75><a action=\"bypass -h admin_bot rotdel ").append(classId).append(" ").append(skillId).append("\">запретить</a></td></tr>");
		}
		html.append("</table></td><td width=375 valign=top><font color=\"99FF99\">Селф-баффы (кастуются автоматически):</font><table width=355>");
		if (buffs.length == 0)
		{
			html.append("<tr><td>—</td></tr>");
		}
		for (int skillId : buffs)
		{
			html.append("<tr><td width=280>").append(BotPresets.getSkillName(skillId)).append(" [").append(skillId).append("]</td>");
			html.append("<td width=75><a action=\"bypass -h admin_bot buffdel ").append(classId).append(" ").append(skillId).append("\">запретить</a></td></tr>");
		}
		html.append("</table></td></tr></table>");
		html.append("<a action=\"bypass -h admin_bot rotreset ").append(classId).append("\">[Сбросить набор скиллов к умолчанию]</a> ");
		html.append("<a action=\"bypass -h admin_bot main\">[Назад к меню]</a><br>");
		
		// Full skill list of the class: every skill can be allowed/forbidden with one click.
		// Green = the bot uses it (in rotation or buffs), red = forbidden (not used).
		html.append("<font color=\"99CCFF\">Полный список скиллов класса</font><br1>");
		html.append("<font color=\"AAAAAA\">зелёный = разрешён (бот использует), красный = запрещён. Изменения сохраняются при рестарте сервера.</font><br1>");
		html.append("<table width=750>");
		int i = 0;
		for (int skillId : BotPresets.getClassFullSkillPool(classId))
		{
			final boolean inRotation = contains(rotation, skillId);
			final boolean inBuffs = contains(buffs, skillId);
			if ((i % 2) == 0)
			{
				html.append("<tr>");
			}
			html.append("<td width=375>");
			if (inRotation || inBuffs)
			{
				html.append("<font color=\"00FF00\">").append(BotPresets.getSkillName(skillId)).append(" [").append(skillId).append("]</font>");
				html.append(inBuffs ? " <a action=\"bypass -h admin_bot buffdel " : " <a action=\"bypass -h admin_bot rotdel ");
				html.append(classId).append(" ").append(skillId).append("\">запретить</a>");
			}
			else
			{
				html.append("<font color=\"FF0000\">").append(BotPresets.getSkillName(skillId)).append(" [").append(skillId).append("]</font>");
				html.append(BotPresets.isSelfBuffSkill(skillId) ? " <a action=\"bypass -h admin_bot buffadd " : " <a action=\"bypass -h admin_bot rotadd ");
				html.append(classId).append(" ").append(skillId).append("\">разрешить</a>");
			}
			html.append("</td>");
			i++;
			if ((i % 2) == 0)
			{
				html.append("</tr>");
			}
		}
		if ((i % 2) != 0)
		{
			html.append("<td width=375></td></tr>");
		}
		html.append("</table>");
		
		html.append("</center></body></html>");
		sendHtml(gm, html.toString());
	}
	
	private static boolean contains(int[] arr, int v)
	{
		for (int a : arr)
		{
			if (a == v)
			{
				return true;
			}
		}
		return false;
	}
	
	private static String row(String label, String value)
	{
		return "<tr><td>" + label + ":</td><td>" + value + "</td></tr>";
	}
	
	private static String btn(String params, String text, int width)
	{
		return "<button value=\"" + text + "\" action=\"bypass -h admin_bot " + params + "\" width=" + width + " height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
	}
	
	/**
	 * Sends the page through the community board window (the big alt+b window):
	 * HtmlUtil.sendCBHtml builds the COMM_BOARD_HTML bypass cache, so the
	 * "bypass -h admin_bot ..." links of the page stay valid, and the window
	 * itself opens/updates on the ShowBoard packets (the same mechanism the
	 * stock //announcements and //geodata panels use).
	 */
	private static void sendHtml(Player gm, String content)
	{
		CommunityBoardHandler.separateAndSend(content, gm);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Script entry point (called by the ScriptEngine for each script file).
	 */
	public static void main(String[] args)
	{
		// Handler is normally registered via MasterHandler; this is a fallback for standalone loading.
		AdminCommandHandler.getInstance().registerHandler(new AdminBot());
	}
}
