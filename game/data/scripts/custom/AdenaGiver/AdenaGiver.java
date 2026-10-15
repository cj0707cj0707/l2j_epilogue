/*
 * Custom NPC: Adena Giver (Giran).
 * Each button click grants exactly 100,000,000 Adena for free.
 * Repeatable, with no cooldown or daily limit.
 */
package custom.AdenaGiver;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class AdenaGiver extends Script
{
	private static final int NPC_ID = 1007000;
	private static final long ADENA_AMOUNT = 100_000_000L;
	private static final String MENU = "<html><title>Adena Giver</title><body><center><br>"
		+ "<font color=\"LEVEL\">--- Adena Giver ---</font><br>"
		+ "Receive <font color=\"LEVEL\">100,000,000 Adena</font> for free.<br>"
		+ "One click per grant. You can receive Adena again.<br><br>"
		+ "<button value=\"Get 100,000,000 Adena\" action=\"bypass -h Script AdenaGiver give\" width=220 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">"
		+ "</center></body></html>";
	
	private AdenaGiver()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (canUse(npc, player))
		{
			HtmlUtil.sendCBHtml(player, MENU, npc.getObjectId());
		}
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!canUse(npc, player) || !"give".equals(event))
		{
			return null;
		}
		
		if (player.isDead() || player.isInCombat() || player.isCastingNow() || player.isAttackingNow() || player.isInStoreMode() || player.isProcessingRequest() || player.isFishing())
		{
			player.sendMessage("You cannot receive Adena right now.");
		}
		else if (player.getAdena() > (PlayerConfig.MAX_ADENA - ADENA_AMOUNT))
		{
			// Do not let addAdena truncate the grant at the configured balance cap.
			player.sendMessage("Not enough room below the Adena limit to receive 100,000,000 Adena.");
		}
		else
		{
			// Standard currency API: inventory update, persistence and system message.
			player.addAdena(ItemProcessType.QUEST, ADENA_AMOUNT, npc, true);
		}
		
		HtmlUtil.sendCBHtml(player, MENU, npc.getObjectId());
		return null;
	}
	
	private boolean canUse(Npc npc, Player player)
	{
		return (npc != null) && (player != null) && (npc.getId() == NPC_ID) && npc.canInteract(player);
	}
	
	public static void main(String[] args)
	{
		new AdenaGiver();
	}
}
