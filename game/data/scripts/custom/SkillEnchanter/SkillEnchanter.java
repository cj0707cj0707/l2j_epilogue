/*
 * Custom NPC: Skill Enchanter (Giran)
 * Free skill enchant service: a skill learned at its maximum base level can be
 * enchanted straight to the maximum enchant level of the chosen route (parameter).
 * The "Cost" route is excluded from the offer by design.
 */
package custom.SkillEnchanter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author Custom
 */
public class SkillEnchanter extends Script
{
	private static final int NPC_ID = 1005000;
	private static final int MAX_ENCHANT_LEVEL = 30; // largest enchant route length in EnchantSkillGroups.xml
	private static final int SKILLS_PER_PAGE = 12;
	
	private SkillEnchanter()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}
	
	private static class EnchantableSkill
	{
		int skillId;
		String name;
		int currentLevel;
		final List<RouteInfo> routes = new ArrayList<>();
	}
	
	private static class RouteInfo
	{
		int route;
		int maxLevel;
		String label;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		sendBoard(player, buildSkillList(player, 0), npc);
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String html = null;
		if (event.equals("menu"))
		{
			html = buildSkillList(player, 0);
		}
		
		if (html == null)
		{
			final String[] params = event.split(" ");
			if (params[0].equals("list") && (params.length == 2))
			{
				try
				{
					html = buildSkillList(player, Integer.parseInt(params[1]));
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
			else if (params[0].equals("ench") && ((params.length == 3) || (params.length == 4)))
			{
				try
				{
					final int page = params.length == 4 ? Integer.parseInt(params[3]) : 0;
					html = enchantSkill(player, Integer.parseInt(params[1]), Integer.parseInt(params[2]), page);
				}
				catch (NumberFormatException e)
				{
					// Ignore malformed input.
				}
			}
		}
		
		if (html == null)
		{
			html = buildSkillList(player, 0);
		}
		
		sendBoard(player, html, npc);
		return null;
	}
	
	/**
	 * Sends the page through the community board window (the big alt+b window
	 * instead of the small NPC dialog). The NPC objectId is attached to the
	 * page, so the "bypass -h Script SkillEnchanter ..." links validate with
	 * the NPC as their origin (range-checked, like the retail dialog flow).
	 */
	private void sendBoard(Player player, String html, Npc npc)
	{
		HtmlUtil.sendCBHtml(player, html, npc != null ? npc.getObjectId() : 0);
	}
	
	/**
	 * Finds the maximum full skill level of an enchant route, e.g. route 2 of a
	 * 2nd class skill -> 230 (+30), route 1 of a 3rd class skill -> 115 (+15).
	 */
	private int getRouteMaxLevel(EnchantSkillLearn learn, int route)
	{
		for (int i = 1; i <= MAX_ENCHANT_LEVEL; i++)
		{
			if (learn.isMaxEnchant((route * 100) + i))
			{
				return (route * 100) + i;
			}
		}
		
		return 0;
	}
	
	/**
	 * Derives a human readable route name by comparing the base skill with the
	 * first enchanted level of the route (the route names are client-side data,
	 * so the server approximates them from the changed stats).
	 */
	private String getRouteLabel(Skill baseSkill, Skill enchantedSkill, int route)
	{
		if ((enchantedSkill.getMpConsume() < baseSkill.getMpConsume()) || (enchantedSkill.getMpInitialConsume() < baseSkill.getMpInitialConsume()))
		{
			return "Cost";
		}
		
		if ((enchantedSkill.getElementPower() != baseSkill.getElementPower()) || (enchantedSkill.getElement() != baseSkill.getElement()))
		{
			return "Element";
		}
		
		if (enchantedSkill.getAbnormalType() != baseSkill.getAbnormalType())
		{
			return "Debuff";
		}
		
		if (enchantedSkill.getActivateRate() != baseSkill.getActivateRate())
		{
			return "Chance";
		}
		
		if (enchantedSkill.getAbnormalTime() != baseSkill.getAbnormalTime())
		{
			return "Time";
		}
		
		if (enchantedSkill.getPower() != baseSkill.getPower())
		{
			return "Power";
		}
		
		if (enchantedSkill.getPower(true, false) != baseSkill.getPower(true, false))
		{
			return "Duel";
		}
		
		return "Enchant " + route;
	}
	
	private List<EnchantableSkill> getEnchantableSkills(Player player)
	{
		// Sorted by skill name (then id) for a stable, readable list.
		final TreeMap<String, EnchantableSkill> skills = new TreeMap<>();
		for (Skill skill : player.getAllSkills())
		{
			if (skill == null)
			{
				continue;
			}
			
			final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skill.getId());
			if (learn == null)
			{
				continue;
			}
			
			// The skill must be learned up to its maximum base level first.
			final int currentLevel = skill.getLevel();
			if (currentLevel < learn.getBaseLevel())
			{
				continue;
			}
			
			final Skill baseSkill = SkillData.getInstance().getSkill(skill.getId(), learn.getBaseLevel());
			if (baseSkill == null)
			{
				continue;
			}
			
			final EnchantableSkill holder = new EnchantableSkill();
			holder.skillId = skill.getId();
			holder.name = skill.getName();
			holder.currentLevel = currentLevel;
			
			for (int route : learn.getAllRoutes())
			{
				final int maxLevel = getRouteMaxLevel(learn, route);
				if (maxLevel == 0)
				{
					continue;
				}
				
				final Skill enchantedSkill = SkillData.getInstance().getSkill(skill.getId(), maxLevel);
				if (enchantedSkill == null)
				{
					continue;
				}
				
				final String label = getRouteLabel(baseSkill, enchantedSkill, route);
				if (label.equals("Cost"))
				{
					// Cost enchant is not offered.
					continue;
				}
				
				final RouteInfo info = new RouteInfo();
				info.route = route;
				info.maxLevel = maxLevel;
				info.label = label;
				holder.routes.add(info);
			}
			
			skills.put(holder.name + " " + holder.skillId, holder);
		}
		
		return new ArrayList<>(skills.values());
	}
	
	private String enchantSkill(Player player, int skillId, int route, int page)
	{
		final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
		if ((learn == null) || !learn.getAllRoutes().contains(route))
		{
			player.sendMessage("Invalid skill enchant request.");
			return buildSkillList(player, page);
		}
		
		final int currentLevel = player.getSkillLevel(skillId);
		if (currentLevel < learn.getBaseLevel())
		{
			player.sendMessage("You must first learn the maximum base level of this skill.");
			return buildSkillList(player, page);
		}
		
		final int targetLevel = getRouteMaxLevel(learn, route);
		final Skill enchantedSkill = targetLevel > 0 ? SkillData.getInstance().getSkill(skillId, targetLevel) : null;
		if (enchantedSkill == null)
		{
			player.sendMessage("This enchant route is not available for this skill.");
			return buildSkillList(player, page);
		}
		
		// Already at the maximum of this very route?
		if (((currentLevel / 100) == route) && (currentLevel >= targetLevel))
		{
			player.sendMessage("Your " + enchantedSkill.getName() + " is already enchanted to the maximum +" + (targetLevel % 100) + " of this parameter.");
			return buildSkillList(player, page);
		}
		
		if ((player.getLevel() < 76) || (player.getPlayerClass().level() < 3) || !player.isAllowedToEnchantSkills())
		{
			player.sendMessage("You cannot enchant your skills right now (third profession and level 76+ are required).");
			return buildSkillList(player, page);
		}
		
		player.addSkill(enchantedSkill, true);
		player.sendSkillList();
		player.updateUserInfo();
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		player.sendMessage(enchantedSkill.getName() + " has been enchanted to +" + (targetLevel % 100) + " for free.");
		
		// Stay on the page the player was on when enchanting (the page is carried in the bypass link).
		return buildSkillList(player, page);
	}
	
	private String buildSkillList(Player player, int page)
	{
		final List<EnchantableSkill> skills = getEnchantableSkills(player);
		if (skills.isEmpty())
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><title>Skill Enchanter</title><body><center><br>");
			sb.append("<font color=\"LEVEL\">--- Skill Enchanter ---</font><br>");
			sb.append("You have no skills that can be enchanted.<br1>");
			sb.append("Learn a skill up to its maximum base level first.");
			sb.append("</center></body></html>");
			return sb.toString();
		}
		
		final int maxPage = (skills.size() - 1) / SKILLS_PER_PAGE;
		if (page < 0)
		{
			page = 0;
		}
		else if (page > maxPage)
		{
			page = maxPage;
		}
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Skill Enchanter</title><body><center><br>");
		sb.append("<font color=\"LEVEL\">--- Skill Enchanter ---</font><br>");
		sb.append("I enchant skills straight to the maximum enchant level, for free.<br1>");
		sb.append("Choose the parameter to enchant a skill with ");
		sb.append("<font color=\"00FFFF\">(current enchant)</font>.<br1>");
		sb.append("<font color=\"FF0000\">Cost</font> enchant is not offered.<br>");
		sb.append("<table width=750>");
		
		final int from = page * SKILLS_PER_PAGE;
		final int to = Math.min(from + SKILLS_PER_PAGE, skills.size());
		for (int i = from; i < to; i++)
		{
			final EnchantableSkill holder = skills.get(i);
			final String current = holder.currentLevel > 100 ? "+" + (holder.currentLevel % 100) : "Lv. " + holder.currentLevel;
			sb.append("<tr>");
			sb.append("<td width=290>" + holder.name + " <font color=\"00FFFF\">(" + current + ")</font></td>");
			
			int buttons = 0;
			for (RouteInfo info : holder.routes)
			{
				// The page is carried in the link, so the list stays on the same page after enchanting.
				sb.append("<td width=110 align=center><a action=\"bypass -h Script SkillEnchanter ench " + holder.skillId + " " + info.route + " " + page + "\">" + info.label + " +" + (info.maxLevel % 100) + "</a></td>");
				buttons++;
			}
			
			// Pad rows with fewer route buttons to keep the table aligned.
			for (int j = buttons; j < 4; j++)
			{
				sb.append("<td width=110 align=center>" + (holder.routes.isEmpty() && (j == 0) ? "<font color=\"FF0000\">Cost only</font>" : " ") + "</td>");
			}
			
			sb.append("</tr>");
		}
		sb.append("</table><br>");
		
		sb.append("<table width=300><tr>");
		sb.append("<td width=80 align=center>");
		if (page > 0)
		{
			sb.append("<a action=\"bypass -h Script SkillEnchanter list " + (page - 1) + "\">Prev</a>");
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
			sb.append("<a action=\"bypass -h Script SkillEnchanter list " + (page + 1) + "\">Next</a>");
		}
		else
		{
			sb.append(" ");
		}
		sb.append("</td>");
		sb.append("</tr></table>");
		
		sb.append("</center></body></html>");
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		new SkillEnchanter();
	}
}
