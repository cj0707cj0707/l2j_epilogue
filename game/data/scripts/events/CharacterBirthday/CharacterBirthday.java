/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package events.CharacterBirthday;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.script.State;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.MessageSenderType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * @author Gnacik, Mobius
 */
public class CharacterBirthday extends Script
{
	// NPCs
	private static final int ALEGRIA = 32600;
	private static final int[] GATEKEEPERS =
	{
		30006,
		30059,
		30080,
		30134,
		30146,
		30177,
		30233,
		30256,
		30320,
		30540,
		30576,
		30836,
		30848,
		30878,
		30899,
		31275,
		31320,
		31964,
		32163
	};
	
	// Query: Get all players that have had a birthday the last 24 hours.
	private static final String SELECT_PENDING_BIRTHDAY_GIFTS = "SELECT charId, char_name, createDate, (YEAR(NOW()) - YEAR(createDate)) AS age FROM characters WHERE (YEAR(NOW()) - YEAR(createDate) > 0) AND ((DATE_ADD(createDate, INTERVAL (YEAR(NOW()) - YEAR(createDate)) YEAR)) BETWEEN FROM_UNIXTIME(?) AND NOW())";
	
	// Misc
	private static boolean HAS_SPAWNED = false;
	
	private CharacterBirthday()
	{
		addStartNpc(ALEGRIA);
		addFirstTalkId(ALEGRIA);
		addTalkId(ALEGRIA);
		for (int id : GATEKEEPERS)
		{
			addStartNpc(id);
			addTalkId(id);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = getQuestState(player, false);
		htmltext = event;
		if (event.equalsIgnoreCase("despawn_npc"))
		{
			npc.doDie(player);
			HAS_SPAWNED = false;
			htmltext = null;
		}
		
		if (event.equalsIgnoreCase("receive_reward"))
		{
			final Calendar now = Calendar.getInstance();
			now.setTimeInMillis(System.currentTimeMillis());
			
			// Check if already received reward
			final String nextBirthday = st.get("Birthday");
			if ((nextBirthday != null) && (Integer.parseInt(nextBirthday) > now.get(Calendar.YEAR)))
			{
				htmltext = "32600-already.htm";
			}
			else
			{
				// Give Adventurer Hat (Event)
				giveItems(player, 10250, 1);
				
				// Give Buff
				Skill skill;
				skill = SkillData.getInstance().getSkill(5950, 1);
				if (skill != null)
				{
					skill.applyEffects(npc, player);
				}
				
				npc.setTarget(player);
				npc.broadcastPacket(new MagicSkillUse(player, 5950, 1, 1000, 0));
				
				// Despawn npc
				npc.doDie(player);
				HAS_SPAWNED = false;
				
				// Update for next year
				st.set("Birthday", String.valueOf(now.get(Calendar.YEAR) + 1));
				htmltext = "32600-ok.htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (HAS_SPAWNED)
		{
			return null;
		}
		
		final QuestState st = getQuestState(player, true);
		if ((st != null) && (player.checkBirthDay() == 0))
		{
			player.sendPacket(new PlaySound(1, "HB01", 0, 0, 0, 0, 0));
			final Npc spawned = addSpawn(32600, player.getX() + 10, player.getY() + 10, player.getZ() + 20, 0, false, 0, true);
			st.setState(State.STARTED);
			startQuestTimer("despawn_npc", 60000, spawned, null);
			HAS_SPAWNED = true;
		}
		else
		{
			return "32600-no.htm";
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			final Quest q = ScriptManager.getInstance().getScript(getName());
			st = q.newQuestState(player);
		}
		
		if (player.checkBirthDay() == 0)
		{
			htmltext = "32600.htm";
		}
		else
		{
			htmltext = "32600-no.htm";
		}
		
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_DAILY_RESET)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDailyReset(OnDailyReset event)
	{
		int birthdayGiftCount = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PENDING_BIRTHDAY_GIFTS))
		{
			statement.setLong(1, System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // Last 24 hours.
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					final String text = GeneralConfig.ALT_BIRTHDAY_MAIL_TEXT.replaceAll("$c1", rs.getString("char_name")).replaceAll("$s1", Integer.toString(rs.getInt("age")));
					final MailMessage message = new MailMessage(rs.getInt("charId"), GeneralConfig.ALT_BIRTHDAY_MAIL_SUBJECT, text, MessageSenderType.ALEGRIA);
					message.createAttachments().addItem(ItemProcessType.REWARD, GeneralConfig.ALT_BIRTHDAY_GIFT, 1, null, null);
					MailManager.getInstance().sendMessage(message);
					birthdayGiftCount++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error checking birthdays. " + e.getMessage());
		}
		
		LOGGER.info(getClass().getSimpleName() + " " + birthdayGiftCount + " gifts sent.");
	}
	
	public static void main(String[] args)
	{
		new CharacterBirthday();
	}
}
