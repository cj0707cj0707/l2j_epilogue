package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.List;
import java.util.Locale;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.games.MonsterRaceManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RaceManager extends Npc {
   protected static final int[] TICKET_PRICES = new int[]{100, 500, 1000, 5000, 10000, 20000, 50000, 100000};

   public RaceManager(NpcTemplate template) {
      super(template);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("BuyTicket")) {
         if (!GeneralConfig.ALLOW_RACE || MonsterRaceManager.getInstance().getCurrentRaceState() != MonsterRaceManager.RaceState.ACCEPTING_BETS) {
            player.sendPacket(SystemMessageId.MONSTER_RACE_TICKETS_ARE_NO_LONGER_AVAILABLE);
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         int val = Integer.parseInt(command.substring(10));
         if (val == 0) {
            player.setRaceTicket(0, 0);
            player.setRaceTicket(1, 0);
         }

         if (val == 10 && player.getRaceTicket(0) == 0 || val == 20 && player.getRaceTicket(0) == 0 && player.getRaceTicket(1) == 0) {
            val = 0;
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         if (val < 10) {
            html.setFile(player, this.getHtmlPath(this.getId(), 2));

            for (int i = 0; i < 8; i++) {
               int n = i + 1;
               String search = "Mob" + n;
               html.replace(search, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
            }

            String search = "No1";
            if (val == 0) {
               html.replace(search, "");
            } else {
               html.replace(search, val);
               player.setRaceTicket(0, val);
            }
         } else if (val < 20) {
            if (player.getRaceTicket(0) == 0) {
               return;
            }

            html.setFile(player, this.getHtmlPath(this.getId(), 3));
            html.replace("0place", player.getRaceTicket(0));
            String search = "Mob1";
            String replace = MonsterRaceManager.getInstance().getMonsters()[player.getRaceTicket(0) - 1].getTemplate().getName();
            html.replace(search, replace);
            search = "0adena";
            if (val == 10) {
               html.replace(search, "");
            } else {
               html.replace(search, TICKET_PRICES[val - 11]);
               player.setRaceTicket(1, val - 10);
            }
         } else {
            if (val != 20) {
               if (player.getRaceTicket(0) != 0 && player.getRaceTicket(1) != 0) {
                  int priceId = player.getRaceTicket(1);
                  if (!player.reduceAdena(ItemProcessType.FEE, TICKET_PRICES[priceId - 1], this, true)) {
                     return;
                  }

                  int ticket = player.getRaceTicket(0);
                  player.setRaceTicket(0, 0);
                  player.setRaceTicket(1, 0);
                  Item item = new Item(IdManager.getInstance().getNextId(), 4443);
                  item.setCount(1L);
                  item.setEnchantLevel(MonsterRaceManager.getInstance().getRaceNumber());
                  item.setCustomType1(ticket);
                  item.setCustomType2(TICKET_PRICES[priceId - 1] / 100);
                  player.addItem(ItemProcessType.QUEST, item, player, false);
                  SystemMessage msg = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
                  msg.addInt(MonsterRaceManager.getInstance().getRaceNumber());
                  msg.addItemName(4443);
                  player.sendPacket(msg);
                  MonsterRaceManager.getInstance().setBetOnLane(ticket, TICKET_PRICES[priceId - 1], true);
                  super.onBypassFeedback(player, "Chat 0");
                  return;
               }

               return;
            }

            if (player.getRaceTicket(0) == 0 || player.getRaceTicket(1) == 0) {
               return;
            }

            html.setFile(player, this.getHtmlPath(this.getId(), 4));
            html.replace("0place", player.getRaceTicket(0));
            String search = "Mob1";
            String replace = MonsterRaceManager.getInstance().getMonsters()[player.getRaceTicket(0) - 1].getTemplate().getName();
            html.replace(search, replace);
            search = "0adena";
            int price = TICKET_PRICES[player.getRaceTicket(1) - 1];
            html.replace(search, price);
            search = "0tax";
            int tax = 0;
            html.replace(search, tax);
            search = "0total";
            int total = price + tax;
            html.replace(search, total);
         }

         html.replace("1race", MonsterRaceManager.getInstance().getRaceNumber());
         html.replace("%objectId%", this.getObjectId());
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (command.equals("ShowOdds")) {
         if (!GeneralConfig.ALLOW_RACE || MonsterRaceManager.getInstance().getCurrentRaceState() == MonsterRaceManager.RaceState.ACCEPTING_BETS) {
            player.sendPacket(SystemMessageId.MONSTER_RACE_PAYOUT_INFORMATION_IS_NOT_AVAILABLE_WHILE_TICKETS_ARE_BEING_SOLD);
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, this.getHtmlPath(this.getId(), 5));

         for (int i = 0; i < 8; i++) {
            int n = i + 1;
            html.replace("Mob" + n, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
            double odd = MonsterRaceManager.getInstance().getOdds().get(i);
            html.replace("Odd" + n, odd > 0.0 ? String.format(Locale.ENGLISH, "%.1f", odd) : "&$804;");
         }

         html.replace("1race", MonsterRaceManager.getInstance().getRaceNumber());
         html.replace("%objectId%", this.getObjectId());
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (command.equals("ShowInfo")) {
         if (!GeneralConfig.ALLOW_RACE) {
            return;
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, this.getHtmlPath(this.getId(), 6));

         for (int i = 0; i < 8; i++) {
            int n = i + 1;
            String search = "Mob" + n;
            html.replace(search, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
         }

         html.replace("%objectId%", this.getObjectId());
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (command.equals("ShowTickets")) {
         if (!GeneralConfig.ALLOW_RACE) {
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         StringBuilder sb = new StringBuilder();

         for (Item ticket : player.getInventory().getAllItemsByItemId(4443)) {
            if (ticket.getEnchantLevel() != MonsterRaceManager.getInstance().getRaceNumber()) {
               StringUtil.append(
                  sb,
                  "<tr><td><a action=\"bypass -h npc_%objectId%_ShowTicket ",
                  String.valueOf(ticket.getObjectId()),
                  "\">",
                  String.valueOf(ticket.getEnchantLevel()),
                  " Race Number</a></td><td align=right><font color=\"LEVEL\">",
                  String.valueOf(ticket.getCustomType1()),
                  "</font> Number</td><td align=right><font color=\"LEVEL\">",
                  String.valueOf(ticket.getCustomType2() * 100),
                  "</font> Adena</td></tr>"
               );
            }
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, this.getHtmlPath(this.getId(), 7));
         html.replace("%tickets%", sb.toString());
         html.replace("%objectId%", this.getObjectId());
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (command.startsWith("ShowTicket")) {
         int valx = Integer.parseInt(command.substring(11));
         if (!GeneralConfig.ALLOW_RACE || valx == 0) {
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         Item ticketx = player.getInventory().getItemByObjectId(valx);
         if (ticketx == null) {
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         int raceId = ticketx.getEnchantLevel();
         MonsterRaceManager.HistoryInfo info = MonsterRaceManager.getInstance().getHistory().get(raceId - 1);
         if (info == null) {
            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         int lane = ticketx.getCustomType1();
         int bet = ticketx.getCustomType2() * 100;
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, this.getHtmlPath(this.getId(), 8));
         html.replace("%raceId%", raceId);
         html.replace("%lane%", lane);
         html.replace("%bet%", bet);
         html.replace("%firstLane%", info.getFirst() + 1);
         html.replace("%odd%", lane == info.getFirst() + 1 ? String.format(Locale.ENGLISH, "%.2f", info.getOddRate()) : "0.01");
         html.replace("%objectId%", this.getObjectId());
         html.replace("%ticketObjectId%", valx);
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         if (command.startsWith("CalculateWin")) {
            int valxx = Integer.parseInt(command.substring(13));
            if (GeneralConfig.ALLOW_RACE && valxx != 0) {
               Item ticketxx = player.getInventory().getItemByObjectId(valxx);
               if (ticketxx == null) {
                  super.onBypassFeedback(player, "Chat 0");
                  return;
               }

               int raceId = ticketxx.getEnchantLevel();
               MonsterRaceManager.HistoryInfo info = MonsterRaceManager.getInstance().getHistory().get(raceId - 1);
               if (info == null) {
                  super.onBypassFeedback(player, "Chat 0");
                  return;
               }

               if (player.destroyItem(ItemProcessType.FEE, ticketxx, this, true)) {
                  int lane = ticketxx.getCustomType1();
                  int bet = ticketxx.getCustomType2() * 100;
                  player.addAdena(ItemProcessType.REWARD, (int)(bet * (lane == info.getFirst() + 1 ? info.getOddRate() : 0.01)), this, true);
               }

               super.onBypassFeedback(player, "Chat 0");
               return;
            }

            super.onBypassFeedback(player, "Chat 0");
            return;
         }

         if (command.equals("ViewHistory")) {
            if (!GeneralConfig.ALLOW_RACE) {
               super.onBypassFeedback(player, "Chat 0");
               return;
            }

            StringBuilder sb = new StringBuilder();
            List<MonsterRaceManager.HistoryInfo> history = MonsterRaceManager.getInstance().getHistory();

            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 7); i--) {
               MonsterRaceManager.HistoryInfo infox = history.get(i);
               StringUtil.append(
                  sb,
                  "<tr><td><font color=\"LEVEL\">",
                  String.valueOf(infox.getRaceId()),
                  "</font> th</td><td><font color=\"LEVEL\">",
                  String.valueOf(infox.getFirst() + 1),
                  "</font> Lane </td><td><font color=\"LEVEL\">",
                  String.valueOf(infox.getSecond() + 1),
                  "</font> Lane</td><td align=right><font color=00ffff>",
                  String.format(Locale.ENGLISH, "%.2f", infox.getOddRate()),
                  "</font> Times</td></tr>"
               );
            }

            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, this.getHtmlPath(this.getId(), 9));
            html.replace("%infos%", sb.toString());
            html.replace("%objectId%", this.getObjectId());
            player.sendPacket(html);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            super.onBypassFeedback(player, command);
         }
      }
   }
}
