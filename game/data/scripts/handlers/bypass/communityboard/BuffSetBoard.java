/*
 * Community Board: full buff sets (warrior / mage) applied to the player
 * with a single button click (alt+b). Free of charge, instant application
 * (no cast time), the same way the core and the FakePlayersAI system apply
 * buff sets. Every buff applied here lasts 60 minutes.
 */
package handlers.bypass.communityboard;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author FakePlayersAI
 */
public class BuffSetBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsbuffset"
	};

	/** Duration of every buff applied through this board: 60 minutes. */
	private static final int BUFF_DURATION_SECONDS = 60 * 60;

	/**
	 * Full "warrior" buff profile (21 buffs) - the same set the FakePlayersAI
	 * system keeps on its melee bots (fits into the 34 buff slots of this server).
	 */
	private static final int[][] FIGHTER_SET =
	{
		{ 1204, 2 }, // Wind Walk 2
		{ 1086, 2 }, // Haste 2
		{ 1068, 3 }, // Might 3
		{ 1077, 3 }, // Focus 3
		{ 1242, 3 }, // Death Whisper 3
		{ 1240, 3 }, // Guidance 3
		{ 1062, 2 }, // Berserker Spirit 2
		{ 1045, 6 }, // Blessed Body 6
		{ 1048, 6 }, // Blessed Soul 6
		{ 1040, 3 }, // Shield 3
		{ 1036, 2 }, // Magic Barrier 2
		{ 1268, 4 }, // Vampiric Rage 4
		{ 1044, 3 }, // Regeneration 3
		{ 1363, 1 }, // Chant of Victory
		{ 271, 1 }, // Dance of the Warrior
		{ 274, 1 }, // Dance of Fire
		{ 264, 1 }, // Song of Earth
		{ 267, 1 }, // Song of Warding
		{ 268, 1 }, // Song of Wind
		{ 269, 1 }, // Song of Hunter
		{ 304, 1 }, // Song of Vitality
	};

	/** Full "mage" buff profile (16 buffs) - the FakePlayersAI caster set. */
	private static final int[][] MAGE_SET =
	{
		{ 1204, 2 }, // Wind Walk 2
		{ 1085, 3 }, // Acumen 3
		{ 1059, 3 }, // Empower 3
		{ 1078, 6 }, // Concentration 6
		{ 1303, 2 }, // Wild Magic 2
		{ 1062, 2 }, // Berserker Spirit 2
		{ 1045, 6 }, // Blessed Body 6
		{ 1048, 6 }, // Blessed Soul 6
		{ 1040, 3 }, // Shield 3
		{ 1036, 2 }, // Magic Barrier 2
		{ 1355, 1 }, // Prophecy of Water
		{ 1363, 1 }, // Chant of Victory
		{ 276, 1 }, // Dance of Concentration
		{ 363, 1 }, // Song of Meditation
		{ 304, 1 }, // Song of Vitality
		{ 267, 1 }, // Song of Warding
	};

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		if (player.isAlikeDead())
		{
			player.sendMessage("You cannot receive buffs while dead.");
			return false;
		}
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Buff sets cannot be used inside the Olympiad.");
			return false;
		}

		final String kind = command.substring(COMMANDS[0].length()).trim().toLowerCase();
		final int[][] set;
		final String title;
		switch (kind)
		{
			case "fighter":
			{
				set = FIGHTER_SET;
				title = "Warrior";
				break;
			}
			case "mage":
			{
				set = MAGE_SET;
				title = "Mystic";
				break;
			}
			default:
			{
				return false;
			}
		}

		int applied = 0;
		for (int[] buff : set)
		{
			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null)
			{
				continue;
			}
			skill.applyEffects(player, player);

			// Every buff from the board lasts 60 minutes: extend the freshly
			// applied BuffInfo before the delayed status update (300 ms) reaches
			// the client, so the icons show 1:00:00 and the buff finish task
			// (BuffFinishTask compares the elapsed seconds against abnormal time)
			// expires it exactly one hour later.
			final BuffInfo info = player.getEffectList().getBuffInfoBySkillId(skill.getId());
			if (info != null)
			{
				info.setAbnormalTime(BUFF_DURATION_SECONDS);
			}

			player.sendPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
			applied++;
		}
		player.sendMessage("Full " + title + " buff set applied: " + applied + " buffs.");

		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/buffset.html");
		if ((html == null) || html.isEmpty())
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body><center><br>");
			sb.append("Full ").append(title).append(" buff set applied: ").append(applied).append(" buffs.<br><br>");
			sb.append("<button value=\"Back\" action=\"bypass _bbshome\" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</center></body></html>");
			html = sb.toString();
		}
		else
		{
			html = html.replace("%setname%", title).replace("%applied%", String.valueOf(applied));
		}
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}
}
