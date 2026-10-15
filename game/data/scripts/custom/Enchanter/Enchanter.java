/*
 * Custom NPC: Enchanter (Giran)
 * Free enchant service: each equipment slot can be enchanted separately,
 * from +1 up to +16, at no cost.
 * Pages are shown in the big community board window (alt+b), like the
 * Skill Enchanter NPC, instead of the small NPC dialog.
 */
package custom.Enchanter;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author Custom
 */
public class Enchanter extends Script
{
	private static final int NPC_ID = 1004000;
	private static final int MAX_ENCHANT = 16;
	
	private static final String[] SLOT_KEYS =
	{
		"rhand",
		"lhand",
		"hairall",
		"rear",
		"lear",
		"neck",
		"rfinger",
		"lfinger",
		"underwear",
		"chest",
		"legs",
		"gloves",
		"feet",
		"back",
		"rbracelet",
		"belt"
	};
	
	private static final String[] SLOT_NAMES =
	{
		"Weapon",
		"Shield",
		"Hair Accessory",
		"Earring (Right)",
		"Earring (Left)",
		"Necklace",
		"Ring (Right)",
		"Ring (Left)",
		"Shirt",
		"Chest",
		"Legs",
		"Gloves",
		"Boots",
		"Cloak",
		"Bracelet",
		"Belt"
	};
	
	private static final BodyPart[] SLOT_BODY_PARTS =
	{
		BodyPart.R_HAND,
		BodyPart.L_HAND,
		BodyPart.HAIRALL,
		BodyPart.R_EAR,
		BodyPart.L_EAR,
		BodyPart.NECK,
		BodyPart.R_FINGER,
		BodyPart.L_FINGER,
		BodyPart.UNDERWEAR,
		BodyPart.CHEST,
		BodyPart.LEGS,
		BodyPart.GLOVES,
		BodyPart.FEET,
		BodyPart.BACK,
		BodyPart.R_BRACELET,
		BodyPart.BELT
	};
	
	private Enchanter()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		sendBoard(player, buildMainMenu(player), npc);
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String html = null;
		if (event.equals("menu"))
		{
			html = buildMainMenu(player);
		}
		
		if (html == null)
		{
			final String[] params = event.split(" ");
			if (params[0].equals("slot") && (params.length == 2))
			{
				final int slotIndex = getSlotIndex(params[1]);
				if (slotIndex >= 0)
				{
					html = buildSlotPage(player, slotIndex);
				}
			}
			else if (params[0].equals("ench") && (params.length == 3))
			{
				final int slotIndex = getSlotIndex(params[1]);
				if (slotIndex >= 0)
				{
					try
					{
						final int level = Integer.parseInt(params[2]);
						html = enchantItem(player, slotIndex, level);
					}
					catch (NumberFormatException e)
					{
						// Ignore malformed input.
					}
				}
			}
		}
		
		if (html == null)
		{
			html = buildMainMenu(player);
		}
		
		sendBoard(player, html, npc);
		return null;
	}
	
	/**
	 * Sends the page through the community board window (the big alt+b window
	 * instead of the small NPC dialog). The NPC objectId is attached to the
	 * page, so the "bypass -h Script Enchanter ..." links validate with the
	 * NPC as their origin (range-checked, like the retail dialog flow).
	 */
	private void sendBoard(Player player, String html, Npc npc)
	{
		HtmlUtil.sendCBHtml(player, html, npc != null ? npc.getObjectId() : 0);
	}
	
	private int getSlotIndex(String key)
	{
		for (int i = 0; i < SLOT_KEYS.length; i++)
		{
			if (SLOT_KEYS[i].equals(key))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	private String enchantItem(Player player, int slotIndex, int targetLevel)
	{
		if (player.isDead() || player.isInCombat() || player.isCastingNow() || player.isAttackingNow() || player.isInStoreMode() || player.isProcessingRequest() || player.isFishing())
		{
			player.sendMessage("You cannot enchant your items right now.");
			return buildSlotPage(player, slotIndex);
		}
		
		final Item item = player.getInventory().getPaperdollItemByBodyPart(SLOT_BODY_PARTS[slotIndex]);
		if (item == null)
		{
			player.sendMessage("There is no item equipped in the " + SLOT_NAMES[slotIndex] + " slot.");
			return buildMainMenu(player);
		}
		
		final int currentEnchant = item.getEnchantLevel();
		if (currentEnchant >= MAX_ENCHANT)
		{
			player.sendMessage("Your " + item.getTemplate().getName() + " is already enchanted to the maximum +" + MAX_ENCHANT + ".");
			return buildSlotPage(player, slotIndex);
		}
		
		if ((targetLevel <= currentEnchant) || (targetLevel < 1))
		{
			player.sendMessage("Choose an enchant level higher than the current +" + currentEnchant + ".");
			return buildSlotPage(player, slotIndex);
		}
		
		if (targetLevel > MAX_ENCHANT)
		{
			targetLevel = MAX_ENCHANT;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		item.setEnchantLevel(targetLevel);
		iu.addModifiedItem(item);
		item.updateDatabase();
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		player.sendMessage("Your " + item.getTemplate().getName() + " has been enchanted to +" + targetLevel + " for free.");
		
		return buildSlotPage(player, slotIndex);
	}
	
	private String buildMainMenu(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Enchanter</title><body><center><br>");
		sb.append("<font color=\"LEVEL\">--- Enchanter ---</font><br>");
		sb.append("I enchant equipment for free.<br1>");
		sb.append("Allowed range: <font color=\"LEVEL\">+1 to +16</font> per slot.<br>");
		sb.append("<table width=750>");
		sb.append("<tr><td width=110>Slot</td><td width=480>Item</td><td width=60 align=center>Ench</td><td width=100 align=center> </td></tr>");
		for (int i = 0; i < SLOT_KEYS.length; i++)
		{
			final Item item = player.getInventory().getPaperdollItemByBodyPart(SLOT_BODY_PARTS[i]);
			String itemName = "-";
			String enchant = "-";
			if (item != null)
			{
				itemName = item.getTemplate().getName();
				enchant = "+" + item.getEnchantLevel();
			}
			
			sb.append("<tr><td>" + SLOT_NAMES[i] + "</td><td>" + itemName + "</td><td align=center>" + enchant + "</td>");
			if (item == null)
			{
				sb.append("<td align=center> </td>");
			}
			else
			{
				sb.append("<td align=center><a action=\"bypass -h Script Enchanter slot " + SLOT_KEYS[i] + "\">Set</a></td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	private String buildSlotPage(Player player, int slotIndex)
	{
		final Item item = player.getInventory().getPaperdollItemByBodyPart(SLOT_BODY_PARTS[slotIndex]);
		if (item == null)
		{
			return buildMainMenu(player);
		}
		
		final int currentEnchant = item.getEnchantLevel();
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Enchanter</title><body><center><br>");
		sb.append("<font color=\"LEVEL\">--- Enchanter ---</font><br>");
		sb.append("<font color=\"LEVEL\">" + SLOT_NAMES[slotIndex] + ":</font> " + item.getTemplate().getName() + "<br1>");
		sb.append("Current enchant: <font color=\"LEVEL\">+" + currentEnchant + "</font><br>");
		sb.append("Select the new enchant level (free):<br>");
		sb.append("<table width=750>");
		for (int level = 1; level <= MAX_ENCHANT; level++)
		{
			// 8 levels per row: the wide alt+b window fits the full range in two rows.
			if (((level - 1) % 8) == 0)
			{
				sb.append("<tr>");
			}
			
			if (level <= currentEnchant)
			{
				sb.append("<td width=91 align=center>+" + level + "</td>");
			}
			else
			{
				sb.append("<td width=91 align=center><a action=\"bypass -h Script Enchanter ench " + SLOT_KEYS[slotIndex] + " " + level + "\">+" + level + "</a></td>");
			}
			
			if ((level % 8) == 0)
			{
				sb.append("</tr>");
			}
		}
		sb.append("</table><br>");
		sb.append("<a action=\"bypass -h Script Enchanter menu\">Back</a>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		new Enchanter();
	}
}