/*
 * Custom NPC: Blacksmith Aug (Giran)
 * Free "skill-on-choice" weapon augmentation: the player picks a weapon from
 * the inventory and a skill from the offer list; the skill is augmented into
 * the weapon (available while the weapon is equipped).
 * Offers all retail augmentation skills - every skill whose name starts
 * with "Item Skill:" (castable actives, passives and chance triggers).
 */
package custom.BlacksmithAug;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author Custom
 */
public class BlacksmithAug extends Script
{
	private static final int NPC_ID = 1006000;
	private static final int SKILLS_PER_PAGE = 12;
	
	/**
	 * First augmentation option id (must stay in sync with
	 * data\stats\augmentation\options\25000-25099.xml and
	 * data\stats\augmentation\options\25100-25199.xml:
	 * option id = FIRST_OPTION_ID + index in OFFERED_SKILLS).
	 */
	private static final int FIRST_OPTION_ID = 25000;

	/**
	 * Skills offered for augmentation: {skillId, level, typeLabel} - all skills
	 * whose name starts with "Item Skill:", i.e. the retail augmentation skills.
	 * Levels are the maximum base levels of the skills (SkillData max level).
	 * Type label: element name or "Active" (plus " AoE") for castable actives,
	 * "Passive" for passives, "Chance N% (attack|magic|crit)" for chance
	 * triggers (chances match the retail top-grade values).
	 */
	private static final Object[][] OFFERED_SKILLS =
	{
		{ 3080, 10, "Chance 1% (attack)" }, // Item Skill: Aggression
		{ 3081, 10, "Chance 1% (attack)" }, // Item Skill: Charm
		{ 3082, 10, "Chance 1% (attack)" }, // Item Skill: Mana Burn
		{ 3083, 10, "Chance 1% (attack)" }, // Item Skill: Slow
		{ 3084, 10, "Chance 1% (attack)" }, // Item Skill: Winter
		{ 3085, 10, "Chance 1% (attack)" }, // Item Skill: Stun
		{ 3086, 10, "Chance 1% (attack)" }, // Item Skill: Hold
		{ 3087, 10, "Chance 1% (attack)" }, // Item Skill: Sleep
		{ 3088, 10, "Chance 1% (attack)" }, // Item Skill: Paralyze
		{ 3089, 10, "Chance 1% (attack)" }, // Item Skill: Medusa
		{ 3090, 10, "Chance 1% (attack)" }, // Item Skill: Fear
		{ 3091, 10, "Chance 1% (attack)" }, // Item Skill: Poison
		{ 3092, 10, "Chance 1% (attack)" }, // Item Skill: Bleed
		{ 3093, 10, "Chance 1% (attack)" }, // Item Skill: Silence
		{ 3094, 10, "Chance 1% (attack)" }, // Item Skill: Doom
		{ 3095, 10, "Chance 2% (magic)" }, // Item Skill: Mana Burn
		{ 3096, 10, "Chance 2% (magic)" }, // Item Skill: Slow
		{ 3097, 10, "Chance 2% (magic)" }, // Item Skill: Winter
		{ 3098, 10, "Chance 2% (magic)" }, // Item Skill: Stun
		{ 3099, 10, "Chance 2% (magic)" }, // Item Skill: Hold
		{ 3100, 10, "Chance 2% (magic)" }, // Item Skill: Sleep
		{ 3101, 10, "Chance 2% (magic)" }, // Item Skill: Paralyze
		{ 3102, 10, "Chance 2% (magic)" }, // Item Skill: Medusa
		{ 3103, 10, "Chance 2% (magic)" }, // Item Skill: Fear
		{ 3104, 10, "Chance 2% (magic)" }, // Item Skill: Poison
		{ 3105, 10, "Chance 2% (magic)" }, // Item Skill: Bleed
		{ 3106, 10, "Chance 2% (magic)" }, // Item Skill: Silence
		{ 3107, 10, "Chance 2% (magic)" }, // Item Skill: Doom
		{ 3108, 10, "Chance 5% (crit)" }, // Item Skill: Aggression Up
		{ 3109, 10, "Chance 5% (crit)" }, // Item Skill: Aggression Down
		{ 3110, 10, "Chance 5% (crit)" }, // Item Skill: Mana Burn
		{ 3111, 10, "Chance 5% (crit)" }, // Item Skill: Slow
		{ 3112, 10, "Chance 5% (crit)" }, // Item Skill: Winter
		{ 3113, 10, "Chance 5% (crit)" }, // Item Skill: Stun
		{ 3114, 10, "Chance 5% (crit)" }, // Item Skill: Hold
		{ 3115, 10, "Chance 5% (crit)" }, // Item Skill: Sleep
		{ 3116, 10, "Chance 5% (crit)" }, // Item Skill: Paralyze
		{ 3117, 10, "Chance 5% (crit)" }, // Item Skill: Medusa
		{ 3118, 10, "Chance 5% (crit)" }, // Item Skill: Fear
		{ 3119, 10, "Chance 5% (crit)" }, // Item Skill: Poison
		{ 3120, 10, "Chance 5% (crit)" }, // Item Skill: Bleed
		{ 3121, 10, "Chance 5% (crit)" }, // Item Skill: Silence
		{ 3122, 10, "Chance 5% (crit)" }, // Item Skill: Doom
		{ 3123, 10, "Active" }, // Item Skill: Heal
		{ 3124, 10, "Active" }, // Item Skill: Blessed Body
		{ 3125, 10, "Active" }, // Item Skill: Battle Roar
		{ 3126, 10, "Active" }, // Item Skill: Prayer
		{ 3127, 10, "Active" }, // Item Skill: Recharge
		{ 3128, 10, "Active" }, // Item Skill: Blessed Soul
		{ 3129, 10, "Active" }, // Item Skill: Mana Gain
		{ 3130, 10, "Active" }, // Item Skill: Ritual
		{ 3131, 10, "Active" }, // Item Skill: Cheer
		{ 3132, 10, "Active" }, // Item Skill: Might
		{ 3133, 10, "Active" }, // Item Skill: Empower
		{ 3134, 10, "Active" }, // Item Skill: Duel Might
		{ 3135, 10, "Active" }, // Item Skill: Shield
		{ 3136, 10, "Active" }, // Item Skill: Magic Barrier
		{ 3137, 10, "Active" }, // Item Skill: Duel Weakness
		{ 3138, 10, "Active" }, // Item Skill: Heal Empower
		{ 3139, 10, "Active" }, // Item Skill: Agility
		{ 3140, 10, "Active" }, // Item Skill: Guidance
		{ 3141, 10, "Active" }, // Item Skill: Focus
		{ 3142, 10, "Active" }, // Item Skill: Wild Magic
		{ 3143, 10, "Active" }, // Item Skill: Kiss of Eva
		{ 3144, 10, "Active" }, // Item Skill: Acrobatics
		{ 3145, 10, "Active" }, // Item Skill: Iron Body
		{ 3146, 1, "Active" }, // Item Skill: Recall
		{ 3147, 1, "Active" }, // Item Skill: Recall
		{ 3148, 10, "Active" }, // Item Skill: Vampiric Rage
		{ 3149, 10, "Active" }, // Item Skill: Aggression
		{ 3150, 10, "Active" }, // Item Skill: Charm
		{ 3151, 10, "Active" }, // Item Skill: Peace
		{ 3152, 10, "Active" }, // Item Skill: Trick
		{ 3153, 10, "Dark" }, // Item Skill: Vampiric Touch
		{ 3154, 10, "Active" }, // Item Skill: Mana Burn
		{ 3155, 10, "Active" }, // Item Skill: Unlock
		{ 3156, 1, "Active" }, // Item Skill: Firework
		{ 3157, 1, "Active" }, // Item Skill: Large Firework
		{ 3158, 1, "Active" }, // Item Skill: Lesser Celestial Shield
		{ 3159, 3, "Active" }, // Item Skill: Stealth
		{ 3160, 9, "Active" }, // Item Skill: Resurrection
		{ 3161, 3, "Active" }, // Item Skill: Skill Clarity
		{ 3162, 3, "Active" }, // Item Skill: Spell Clarity
		{ 3163, 3, "Active" }, // Item Skill: Music Clarity
		{ 3164, 3, "Active" }, // Item Skill: Clarity
		{ 3165, 10, "Fire" }, // Item Skill: Prominence
		{ 3167, 10, "Water" }, // Item Skill: Hydro Blast
		{ 3168, 10, "Wind" }, // Item Skill: Tempest
		{ 3169, 10, "Earth" }, // Item Skill: Stone
		{ 3170, 10, "Holy" }, // Item Skill: Solar Flare
		{ 3171, 10, "Dark" }, // Item Skill: Shadow Flare
		{ 3172, 10, "Active" }, // Item Skill: Aura Flare
		{ 3173, 10, "Fire AoE" }, // Item Skill: Prominence
		{ 3174, 10, "Water AoE" }, // Item Skill: Hydro Blast
		{ 3175, 10, "Wind AoE" }, // Item Skill: Hurricane
		{ 3176, 10, "Earth AoE" }, // Item Skill: Stone
		{ 3177, 10, "Holy AoE" }, // Item Skill: Solar Flare
		{ 3178, 10, "Dark AoE" }, // Item Skill: Shadow Flare
		{ 3179, 10, "Active AoE" }, // Item Skill: Aura Flare
		{ 3180, 10, "Fire AoE" }, // Item Skill: Prominence
		{ 3181, 10, "Water AoE" }, // Item Skill: Hydro Blast
		{ 3182, 10, "Wind AoE" }, // Item Skill: Hurricane
		{ 3183, 10, "Earth AoE" }, // Item Skill: Stone
		{ 3184, 10, "Holy AoE" }, // Item Skill: Solar Flare
		{ 3185, 10, "Dark AoE" }, // Item Skill: Shadow Flare
		{ 3186, 10, "Active AoE" }, // Item Skill: Aura Flare
		{ 3187, 10, "Active" }, // Item Skill: Slow
		{ 3188, 10, "Active" }, // Item Skill: Winter
		{ 3189, 10, "Active" }, // Item Skill: Stun
		{ 3190, 10, "Active" }, // Item Skill: Hold
		{ 3191, 10, "Active" }, // Item Skill: Sleep
		{ 3192, 10, "Active" }, // Item Skill: Paralyze
		{ 3193, 10, "Earth" }, // Item Skill: Medusa
		{ 3194, 10, "Active" }, // Item Skill: Fear
		{ 3195, 10, "Active" }, // Item Skill: Poison
		{ 3196, 10, "Active" }, // Item Skill: Bleed
		{ 3197, 10, "Active" }, // Item Skill: Silence
		{ 3198, 10, "Active" }, // Item Skill: Doom
		{ 3199, 3, "Active" }, // Item Skill: Skill Refresh
		{ 3200, 3, "Active" }, // Item Skill: Spell Refresh
		{ 3201, 3, "Active" }, // Item Skill: Music Refresh
		{ 3202, 3, "Active" }, // Item Skill: Refresh
		{ 3203, 1, "Active" }, // Item Skill: Mystery Skill
		{ 3204, 3, "Active" }, // Item Skill: Reflect Damage
		{ 3205, 2, "Active" }, // Item Skill: Party Recall
		{ 3206, 10, "Active" }, // Item Skill: Music
		{ 3207, 10, "Passive" }, // Item Skill: Heal
		{ 3208, 10, "Passive" }, // Item Skill: Blessed Body
		{ 3209, 10, "Passive" }, // Item Skill: Prayer
		{ 3210, 10, "Passive" }, // Item Skill: Recharge
		{ 3211, 10, "Passive" }, // Item Skill: Blessed Soul
		{ 3212, 10, "Passive" }, // Item Skill: Mana Gain
		{ 3213, 10, "Passive" }, // Item Skill: Ritual
		{ 3214, 10, "Passive" }, // Item Skill: Cheer
		{ 3215, 10, "Passive" }, // Item Skill: Might
		{ 3216, 10, "Passive" }, // Item Skill: Empower
		{ 3217, 10, "Passive" }, // Item Skill: Duel Might
		{ 3218, 10, "Passive" }, // Item Skill: Shield
		{ 3219, 10, "Passive" }, // Item Skill: Magic Barrier
		{ 3220, 10, "Passive" }, // Item Skill: Duel Weakness
		{ 3221, 10, "Passive" }, // Item Skill: Agility
		{ 3222, 10, "Passive" }, // Item Skill: Guidance
		{ 3223, 10, "Passive" }, // Item Skill: Focus
		{ 3224, 10, "Passive" }, // Item Skill: Wild Magic
		{ 3225, 10, "Passive" }, // Item Skill: Charm
		{ 3226, 10, "Passive" }, // Item Skill: Slow
		{ 3227, 10, "Passive" }, // Item Skill: Winter
		{ 3228, 10, "Passive" }, // Item Skill: Stun
		{ 3229, 10, "Passive" }, // Item Skill: Hold
		{ 3230, 10, "Passive" }, // Item Skill: Sleep
		{ 3231, 10, "Passive" }, // Item Skill: Paralyze
		{ 3232, 10, "Passive" }, // Item Skill: Medusa
		{ 3233, 10, "Passive" }, // Item Skill: Fear
		{ 3234, 10, "Passive" }, // Item Skill: Poison
		{ 3235, 10, "Passive" }, // Item Skill: Bleed
		{ 3236, 10, "Passive" }, // Item Skill: Silence
		{ 3237, 10, "Passive" }, // Item Skill: Doom
		{ 3238, 10, "Passive" }, // Item Skill: Prayer
		{ 3239, 10, "Passive" }, // Item Skill: Mana Gain
		{ 3240, 10, "Passive" }, // Item Skill: Might
		{ 3241, 10, "Passive" }, // Item Skill: Empower
		{ 3243, 10, "Passive" }, // Item Skill: Duel Might
		{ 3244, 10, "Passive" }, // Item Skill: Shield
		{ 3245, 10, "Passive" }, // Item Skill: Magic Barrier
		{ 3246, 10, "Passive" }, // Item Skill: Heal Empower
		{ 3247, 10, "Passive" }, // Item Skill: Agility
		{ 3248, 10, "Passive" }, // Item Skill: Guidance
		{ 3249, 10, "Passive" }, // Item Skill: Focus
		{ 3250, 10, "Passive" }, // Item Skill: Wild Magic
		{ 3251, 10, "Passive" }, // Item Skill: Weight Limit
		{ 3252, 10, "Passive" }, // Item Skill: Kiss of Eva
		{ 3253, 10, "Passive" }, // Item Skill: Acrobatics
		{ 3254, 10, "Passive" }, // Item Skill: Iron Body
		{ 3255, 3, "Passive" }, // Item Skill: Skill Clarity
		{ 3256, 3, "Passive" }, // Item Skill: Spell Clarity
		{ 3257, 3, "Passive" }, // Item Skill: Music Clarity
		{ 3258, 3, "Passive" }, // Item Skill: Clarity
		{ 3259, 3, "Passive" }, // Item Skill: Reflect Damage
	};
	
	private BlacksmithAug()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}
	
	private static class Offer
	{
		int skillId;
		int level;
		String label;
		int optionId;
		String name;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		sendBoard(player, buildWeaponList(player), npc);
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String html = null;
		if (event.equals("menu"))
		{
			html = buildWeaponList(player);
		}
		
		if (html == null)
		{
			final String[] params = event.split(" ");
			if (params[0].equals("weapon") && (params.length == 2))
			{
				try
				{
					html = buildSkillList(player, Integer.parseInt(params[1]), 0);
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
			else if (params[0].equals("list") && (params.length == 3))
			{
				try
				{
					html = buildSkillList(player, Integer.parseInt(params[1]), Integer.parseInt(params[2]));
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
			else if (params[0].equals("aug") && (params.length == 4))
			{
				try
				{
					html = augmentItem(player, Integer.parseInt(params[1]), Integer.parseInt(params[2]), Integer.parseInt(params[3]));
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
			else if (params[0].equals("remove") && (params.length == 2))
			{
				try
				{
					html = removeAugmentation(player, Integer.parseInt(params[1]));
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
		}
		
		if (html == null)
		{
			html = buildWeaponList(player);
		}
		
		sendBoard(player, html, npc);
		return null;
	}
	
	/**
	 * Sends the page through the community board window (the big alt+b window),
	 * the same way the Enchanter and Skill Enchanter NPCs do it. The NPC
	 * objectId is attached to the page, so the "bypass -h Script BlacksmithAug
	 * ..." links validate with the NPC as their origin (range-checked).
	 */
	private void sendBoard(Player player, String html, Npc npc)
	{
		HtmlUtil.sendCBHtml(player, html, npc != null ? npc.getObjectId() : 0);
	}
	
	private static List<Offer> getOffers()
	{
		final List<Offer> offers = new ArrayList<>();
		for (int i = 0; i < OFFERED_SKILLS.length; i++)
		{
			final int skillId = (int) OFFERED_SKILLS[i][0];
			final int level = (int) OFFERED_SKILLS[i][1];
			final String label = (String) OFFERED_SKILLS[i][2];
			final Skill skill = SkillData.getInstance().getSkill(skillId, level);
			if (skill == null)
			{
				continue;
			}

			final Offer offer = new Offer();
			offer.skillId = skillId;
			offer.level = level;
			offer.label = label;
			offer.optionId = FIRST_OPTION_ID + i;
			offer.name = skill.getName();
			offers.add(offer);
		}
		
		return offers;
	}
	
	private List<Item> getWeapons(Player player)
	{
		// Sorted by name (then objectId) for a stable, readable list; equipped weapons first.
		final TreeMap<String, Item> weapons = new TreeMap<>();
		for (Item item : player.getInventory().getItems())
		{
			if ((item == null) || !item.isWeapon())
			{
				continue;
			}
			
			final String key = (item.isEquipped() ? "0" : "1") + item.getTemplate().getName() + " " + item.getObjectId();
			weapons.put(key, item);
		}
		
		return new ArrayList<>(weapons.values());
	}
	
	private String augmentItem(Player player, int objectId, int skillId, int page)
	{
		final Item item = player.getInventory().getItemByObjectId(objectId);
		if ((item == null) || !item.isWeapon())
		{
			player.sendMessage("This weapon is no longer available.");
			return buildWeaponList(player);
		}
		
		// Resolve the offer (the skill id must be one of the offered ones).
		Offer offer = null;
		for (Offer o : getOffers())
		{
			if (o.skillId == skillId)
			{
				offer = o;
				break;
			}
		}
		
		if (offer == null)
		{
			player.sendMessage("This skill is not offered for augmentation.");
			return buildSkillList(player, objectId, page);
		}
		
		if (player.getKnownSkill(skillId) != null)
		{
			player.sendMessage("You already know " + offer.name + " - augmenting it would overwrite your own copy.");
			return buildSkillList(player, objectId, page);
		}
		
		if (player.isDead() || player.isInCombat() || player.isCastingNow() || player.isAttackingNow() || player.isInStoreMode() || player.isProcessingRequest() || player.isFishing())
		{
			player.sendMessage("You cannot augment your items right now.");
			return buildSkillList(player, objectId, page);
		}
		
		// The option must be loaded from the augmentation data.
		if (OptionData.getInstance().getOptions(offer.optionId) == null)
		{
			player.sendMessage("This augmentation is not available right now.");
			return buildSkillList(player, objectId, page);
		}
		
		final String previous = item.isAugmented() ? " (previous augmentation removed)" : "";
		final Augmentation augmentation = new Augmentation(offer.optionId);
		if (item.isAugmented())
		{
			// Remove the bonuses of the old augmentation before replacing it.
			if (item.isEquipped())
			{
				item.getAugmentation().removeBonus(player);
			}
			item.removeAugmentation();
		}
		
		item.setAugmentation(augmentation);
		
		// An equipped weapon applies the new augmentation immediately; an
		// unequipped one applies it on the next equip (standard augment flow).
		if (item.isEquipped())
		{
			augmentation.applyBonus(player);
			player.updateUserInfo();
		}
		
		player.sendItemList(false);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		player.sendMessage("Your " + item.getTemplate().getName() + " has been augmented with " + offer.name + " (works while equipped)" + previous + ".");
		
		// Stay on the skill list page the player was on (page carried in the link).
		return buildSkillList(player, objectId, page);
	}
	
	private String removeAugmentation(Player player, int objectId)
	{
		final Item item = player.getInventory().getItemByObjectId(objectId);
		if ((item == null) || !item.isWeapon())
		{
			player.sendMessage("This weapon is no longer available.");
			return buildWeaponList(player);
		}
		
		if (!item.isAugmented())
		{
			player.sendMessage("Your " + item.getTemplate().getName() + " has no augmentation.");
			return buildWeaponList(player);
		}
		
		if (player.isDead() || player.isInCombat() || player.isCastingNow() || player.isAttackingNow() || player.isInStoreMode() || player.isProcessingRequest() || player.isFishing())
		{
			player.sendMessage("You cannot remove an augmentation right now.");
			return buildWeaponList(player);
		}
		
		// Remove the bonuses first (the augmentation is equipped or not).
		if (item.isEquipped())
		{
			item.getAugmentation().removeBonus(player);
		}
		item.removeAugmentation();
		player.sendItemList(false);
		player.sendMessage("The augmentation of your " + item.getTemplate().getName() + " has been removed for free.");
		
		return buildWeaponList(player);
	}
	
	private String buildWeaponList(Player player)
	{
		final List<Item> weapons = getWeapons(player);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Blacksmith Aug</title><body><center><br>");
		sb.append("<font color=\"LEVEL\">--- Blacksmith Aug ---</font><br>");
		sb.append("I augment a skill of your choice into a weapon, for free.<br1>");
		sb.append("The augmented skill works <font color=\"00FFFF\">while the weapon is equipped</font><br1>");
		sb.append("and is removed when the weapon is unequipped. Select a weapon:<br>");
		
		if (weapons.isEmpty())
		{
			sb.append("<br>You have no weapons in your inventory.");
			sb.append("</center></body></html>");
			return sb.toString();
		}
		
		sb.append("<table width=750>");
		sb.append("<tr><td width=440>Weapon</td><td width=160 align=center>Augmentation</td><td width=150 align=center>Action</td></tr>");
		for (Item item : weapons)
		{
			final String name = item.getTemplate().getName();
			final String equippedMark = item.isEquipped() ? " <font color=\"00FFFF\">[equipped]</font>" : "";
			final String augmentedName = getAugmentedSkillName(item);
			
			sb.append("<tr>");
			sb.append("<td>" + name + " <font color=\"LEVEL\">+" + item.getEnchantLevel() + "</font>" + equippedMark + "</td>");
			sb.append("<td align=center>" + (augmentedName != null ? augmentedName : (item.isAugmented() ? "augmented" : "<font color=\"FF0000\">none</font>")) + "</td>");
			sb.append("<td align=center>");
			if (item.isAugmented())
			{
				sb.append("<a action=\"bypass -h Script BlacksmithAug weapon " + item.getObjectId() + "\">Change</a>");
				sb.append(" | <a action=\"bypass -h Script BlacksmithAug remove " + item.getObjectId() + "\">Remove</a>");
			}
			else
			{
				sb.append("<a action=\"bypass -h Script BlacksmithAug weapon " + item.getObjectId() + "\">Augment</a>");
			}
			sb.append("</td></tr>");
		}
		sb.append("</table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	private String getAugmentedSkillName(Item item)
	{
		if ((item == null) || !item.isAugmented() || (item.getAugmentation() == null))
		{
			return null;
		}
		
		// The custom augmentations pack a single option id in the low word.
		final int optionId = item.getAugmentation().getAugmentationId() & 0xFFFF;
		for (Offer offer : getOffers())
		{
			if (offer.optionId == optionId)
			{
				return offer.name;
			}
		}
		
		return null;
	}
	
	private String buildSkillList(Player player, int objectId, int page)
	{
		final Item item = player.getInventory().getItemByObjectId(objectId);
		if ((item == null) || !item.isWeapon())
		{
			return buildWeaponList(player);
		}
		
		final List<Offer> offers = getOffers();
		final int maxPage = (offers.size() - 1) / SKILLS_PER_PAGE;
		if (page < 0)
		{
			page = 0;
		}
		else if (page > maxPage)
		{
			page = maxPage;
		}
		
		final String current = getAugmentedSkillName(item);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Blacksmith Aug</title><body><center><br>");
		sb.append("<font color=\"LEVEL\">--- Blacksmith Aug ---</font><br>");
		sb.append("Weapon: <font color=\"LEVEL\">" + item.getTemplate().getName() + " +" + item.getEnchantLevel() + "</font>");
		if (item.isEquipped())
		{
			sb.append(" <font color=\"00FFFF\">[equipped]</font>");
		}
		sb.append("<br1>");
		sb.append("Current augmentation: <font color=\"LEVEL\">" + (current != null ? current : (item.isAugmented() ? "augmented" : "none")) + "</font><br>");
		sb.append("Select the skill to augment (works while the weapon is equipped):<br>");
		sb.append("<table width=750>");
		
		final int from = page * SKILLS_PER_PAGE;
		final int to = Math.min(from + SKILLS_PER_PAGE, offers.size());
		for (int i = from; i < to; i++)
		{
			final Offer offer = offers.get(i);
			final boolean isCurrent = item.isAugmented() && ((item.getAugmentation().getAugmentationId() & 0xFFFF) == offer.optionId);
			final boolean isKnown = player.getKnownSkill(offer.skillId) != null;
			
			sb.append("<tr>");
			sb.append("<td width=430>" + offer.name + " <font color=\"808080\">(" + offer.skillId + ")</font>" + (isCurrent ? " <font color=\"00FFFF\">(current)</font>" : (isKnown ? " <font color=\"FF0000\">(known)</font>" : "")) + "</td>");
			sb.append("<td width=150 align=center><font color=\"LEVEL\">" + offer.label + "</font></td>");
			sb.append("<td width=170 align=center>");
			if (isCurrent || isKnown)
			{
				sb.append(" ");
			}
			else
			{
				sb.append("<a action=\"bypass -h Script BlacksmithAug aug " + item.getObjectId() + " " + offer.skillId + " " + page + "\">Augment</a>");
			}
			sb.append("</td></tr>");
		}
		sb.append("</table><br>");
		
		sb.append("<table width=300><tr>");
		sb.append("<td width=80 align=center>");
		if (page > 0)
		{
			sb.append("<a action=\"bypass -h Script BlacksmithAug list " + objectId + " " + (page - 1) + "\">Prev</a>");
		}
		else
		{
			sb.append(" ");
		}
		sb.append("</td>");
		sb.append("<td width=80 align=center>Page " + (page + 1) + "/" + (maxPage + 1) + "</td>");
		sb.append("<td width=80 align=center>");
		if (page < maxPage)
		{
			sb.append("<a action=\"bypass -h Script BlacksmithAug list " + objectId + " " + (page + 1) + "\">Next</a>");
		}
		else
		{
			sb.append(" ");
		}
		sb.append("</td>");
		sb.append("</tr></table>");
		
		sb.append("<a action=\"bypass -h Script BlacksmithAug menu\">Back to weapon list</a>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		new BlacksmithAug();
	}
}