package org.l2jmobius.gameserver.entity.actor.instance;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.residences.ClanHallAuction;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Auctioneer extends Npc {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
   private static final DateTimeFormatter DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("yy/MM/dd");
   private static final int COND_ALL_FALSE = 0;
   private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
   private static final int COND_REGULAR = 3;
   private final Map<Integer, ClanHallAuction> _pendingAuctions = new ConcurrentHashMap<>();

   public Auctioneer(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Auctioneer);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      int condition = this.validateCondition();
      if (condition <= 0) {
         player.sendMessage("Wrong conditions.");
      } else if (condition == 1) {
         String filename = "data/html/auction/auction-busy.htm";
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, "data/html/auction/auction-busy.htm");
         html.replace("%objectId%", String.valueOf(this.getObjectId()));
         player.sendPacket(html);
      } else {
         if (condition == 3) {
            StringTokenizer st = new StringTokenizer(command, " ");
            String actualCommand = st.nextToken();
            String val = "";
            if (st.countTokens() >= 1) {
               val = st.nextToken();
            }

            Clan clan = player.getClan();
            if (actualCommand.equalsIgnoreCase("auction")) {
               if (val.isEmpty()) {
                  return;
               }

               try {
                  int days = Integer.parseInt(val);

                  try {
                     long bid = 0L;
                     if (st.countTokens() >= 1) {
                        bid = Math.min(Long.parseLong(st.nextToken()), Inventory.MAX_ADENA);
                     }

                     ClanHallAuction a = new ClanHallAuction(
                        clan.getHideoutId(), clan, days * 86400000, bid, ClanHallTable.getInstance().getClanHallByOwner(clan).getName()
                     );
                     if (this._pendingAuctions.get(a.getId()) != null) {
                        this._pendingAuctions.remove(a.getId());
                     }

                     this._pendingAuctions.put(a.getId(), a);
                     String filename = "data/html/auction/AgitSale3.htm";
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/auction/AgitSale3.htm");
                     html.replace("%x%", val);
                     html.replace("%AGIT_AUCTION_END%", Instant.ofEpochMilli(a.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                     html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                     html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
                     html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getClanHallByOwner(clan).getDesc());
                     html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_sale2");
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                     player.sendPacket(html);
                  } catch (Exception var16) {
                     player.sendMessage("Invalid bid!");
                  }
               } catch (Exception var17) {
                  player.sendMessage("Invalid auction duration!");
               }

               return;
            }

            if (actualCommand.equalsIgnoreCase("confirmAuction")) {
               try {
                  ClanHallAuction a = this._pendingAuctions.get(clan.getHideoutId());
                  a.confirmAuction();
                  this._pendingAuctions.remove(clan.getHideoutId());
               } catch (Exception var18) {
                  player.sendMessage("Invalid auction");
               }

               return;
            }

            if (actualCommand.equalsIgnoreCase("bidding")) {
               if (val.isEmpty()) {
                  return;
               }

               try {
                  int auctionId = Integer.parseInt(val);
                  String filename = "data/html/auction/AgitAuctionInfo.htm";
                  ClanHallAuction a = ClanHallAuctionManager.getInstance().getAuction(auctionId);
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitAuctionInfo.htm");
                  if (a != null) {
                     html.replace("%AGIT_NAME%", a.getItemName());
                     html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
                     html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
                     html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
                     html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
                     html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
                     html.replace("%AGIT_AUCTION_END%", Instant.ofEpochMilli(a.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                     html.replace(
                        "%AGIT_AUCTION_REMAIN%",
                        (a.getEndDate() - System.currentTimeMillis()) / 3600000L
                           + " hours "
                           + (a.getEndDate() - System.currentTimeMillis()) / 60000L % 60L
                           + " minutes"
                     );
                     html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                     html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
                     html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
                     html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_list");
                     html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + this.getObjectId() + "_bidlist " + a.getId());
                     html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + this.getObjectId() + "_bid1 " + a.getId());
                  } else {
                     LOGGER.warning("Auctioneer Auction null for AuctionId : " + auctionId);
                  }

                  player.sendPacket(html);
               } catch (Exception var19) {
                  player.sendMessage("Invalid auction!");
               }

               return;
            }

            if (actualCommand.equalsIgnoreCase("bid")) {
               if (val.isEmpty()) {
                  return;
               }

               try {
                  int auctionId = Integer.parseInt(val);

                  try {
                     long bidx = 0L;
                     if (st.countTokens() >= 1) {
                        bidx = Math.min(Long.parseLong(st.nextToken()), Inventory.MAX_ADENA);
                     }

                     ClanHallAuctionManager.getInstance().getAuction(auctionId).setBid(player, bidx);
                  } catch (Exception var20) {
                     player.sendMessage("Invalid bid!");
                  }
               } catch (Exception var21) {
                  player.sendMessage("Invalid auction!");
               }

               return;
            }

            if (actualCommand.equalsIgnoreCase("bid1")) {
               if (clan != null && clan.getLevel() >= 2) {
                  if (val.isEmpty()) {
                     return;
                  }

                  if ((clan.getAuctionBiddedAt() <= 0 || clan.getAuctionBiddedAt() == Integer.parseInt(val)) && clan.getHideoutId() <= 0) {
                     try {
                        String filename = "data/html/auction/AgitBid1.htm";
                        long minimumBid = ClanHallAuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
                        if (minimumBid == 0L) {
                           minimumBid = ClanHallAuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();
                        }

                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/auction/AgitBid1.htm");
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_bidding " + val);
                        html.replace("%PLEDGE_ADENA%", String.valueOf(clan.getWarehouse().getAdena()));
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
                        html.replace("npc_%objectId%_bid", "npc_" + this.getObjectId() + "_bid " + val);
                        player.sendPacket(html);
                     } catch (Exception var22) {
                        player.sendMessage("Invalid auction!");
                     }

                     return;
                  }

                  player.sendPacket(SystemMessageId.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME);
                  return;
               }

               player.sendPacket(SystemMessageId.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION);
               return;
            }

            if (actualCommand.equalsIgnoreCase("list")) {
               List<ClanHallAuction> auctions = ClanHallAuctionManager.getInstance().getAuctions();
               int limit = 15;
               int i = 1;
               int npage = Math.ceilDiv(auctions.size(), limit);
               int start;
               if (val.isEmpty()) {
                  start = 1;
               } else {
                  start = limit * (Integer.parseInt(val) - 1) + 1;
                  limit *= Integer.parseInt(val);
               }

               StringBuilder items = new StringBuilder();
               items.append("<table width=280 border=0><tr>");

               for (int j = 1; j <= npage; j++) {
                  items.append("<td><center><a action=\"bypass -h npc_");
                  items.append(this.getObjectId());
                  items.append("_list ");
                  items.append(j);
                  items.append("\"> Page ");
                  items.append(j);
                  items.append(" </a></center></td>");
               }

               items.append("</tr></table>");
               items.append("<table width=280 border=0>");

               for (ClanHallAuction a : auctions) {
                  if (a != null) {
                     if (i > limit) {
                        break;
                     }

                     if (i < start) {
                        i++;
                     } else {
                        i++;
                        items.append("<tr>");
                        items.append("<td>");
                        items.append(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
                        items.append("</td>");
                        items.append("<td><a action=\"bypass -h npc_");
                        items.append(this.getObjectId());
                        items.append("_bidding ");
                        items.append(a.getId());
                        items.append("\">");
                        items.append(a.getItemName());
                        items.append("</a></td>");
                        items.append("<td>" + Instant.ofEpochMilli(a.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT_SHORT));
                        items.append("</td>");
                        items.append("<td>");
                        items.append(a.getStartingBid());
                        items.append("</td>");
                        items.append("</tr>");
                     }
                  }
               }

               items.append("</table>");
               String filename = "data/html/auction/AgitAuctionList.htm";
               NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
               html.setFile(player, "data/html/auction/AgitAuctionList.htm");
               html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_start");
               html.replace("%itemsField%", items.toString());
               player.sendPacket(html);
               return;
            }

            if (actualCommand.equalsIgnoreCase("bidlist")) {
               int auctionId = 0;
               if (val.isEmpty()) {
                  if (clan.getAuctionBiddedAt() <= 0) {
                     return;
                  }

                  auctionId = clan.getAuctionBiddedAt();
               } else {
                  auctionId = Integer.parseInt(val);
               }

               String biders = "";
               Map<Integer, ClanHallAuction.Bidder> bidders = ClanHallAuctionManager.getInstance().getAuction(auctionId).getBidders();

               for (ClanHallAuction.Bidder b : bidders.values()) {
                  biders = biders
                     + "<tr><td>"
                     + b.getClanName()
                     + "</td><td>"
                     + b.getName()
                     + "</td><td>"
                     + b.getTimeBid().get(1)
                     + "/"
                     + (b.getTimeBid().get(2) + 1)
                     + "/"
                     + b.getTimeBid().get(5)
                     + "</td><td>"
                     + b.getBid()
                     + "</td></tr>";
               }

               String filename = "data/html/auction/AgitBidderList.htm";
               NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
               html.setFile(player, "data/html/auction/AgitBidderList.htm");
               html.replace("%AGIT_LIST%", biders);
               html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_selectedItems");
               html.replace("%x%", val);
               html.replace("%objectId%", String.valueOf(this.getObjectId()));
               player.sendPacket(html);
               return;
            }

            if (actualCommand.equalsIgnoreCase("selectedItems")) {
               if (clan != null && clan.getHideoutId() == 0 && clan.getAuctionBiddedAt() > 0) {
                  String filename = "data/html/auction/AgitBidInfo.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitBidInfo.htm");
                  ClanHallAuction ax = ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());
                  if (ax != null) {
                     html.replace("%AGIT_NAME%", ax.getItemName());
                     html.replace("%OWNER_PLEDGE_NAME%", ax.getSellerClanName());
                     html.replace("%OWNER_PLEDGE_MASTER%", ax.getSellerName());
                     html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getGrade() * 10));
                     html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getLease()));
                     html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getLocation());
                     html.replace("%AGIT_AUCTION_END%", Instant.ofEpochMilli(ax.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                     html.replace(
                        "%AGIT_AUCTION_REMAIN%",
                        (ax.getEndDate() - System.currentTimeMillis()) / 3600000L
                           + " hours "
                           + (ax.getEndDate() - System.currentTimeMillis()) / 60000L % 60L
                           + " minutes"
                     );
                     html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(ax.getStartingBid()));
                     html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(ax.getBidders().get(player.getClanId()).getBid()));
                     html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getDesc());
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                     html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_start");
                  } else {
                     LOGGER.warning("Auctioneer Auction null for AuctionBiddedAt : " + clan.getAuctionBiddedAt());
                  }

                  player.sendPacket(html);
                  return;
               }

               if (clan != null && ClanHallAuctionManager.getInstance().getAuction(clan.getHideoutId()) != null) {
                  String filename = "data/html/auction/AgitSaleInfo.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitSaleInfo.htm");
                  ClanHallAuction ax = ClanHallAuctionManager.getInstance().getAuction(clan.getHideoutId());
                  if (ax != null) {
                     html.replace("%AGIT_NAME%", ax.getItemName());
                     html.replace("%AGIT_OWNER_PLEDGE_NAME%", ax.getSellerClanName());
                     html.replace("%OWNER_PLEDGE_MASTER%", ax.getSellerName());
                     html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getGrade() * 10));
                     html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getLease()));
                     html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getLocation());
                     html.replace("%AGIT_AUCTION_END%", Instant.ofEpochMilli(ax.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                     html.replace(
                        "%AGIT_AUCTION_REMAIN%",
                        (ax.getEndDate() - System.currentTimeMillis()) / 3600000L
                           + " hours "
                           + (ax.getEndDate() - System.currentTimeMillis()) / 60000L % 60L
                           + " minutes"
                     );
                     html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(ax.getStartingBid()));
                     html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(ax.getBidders().size()));
                     html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(ax.getItemId()).getDesc());
                     html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_start");
                     html.replace("%id%", String.valueOf(ax.getId()));
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  } else {
                     LOGGER.warning("Auctioneer Auction null for getHideoutId : " + clan.getHideoutId());
                  }

                  player.sendPacket(html);
                  return;
               }

               if (clan != null && clan.getHideoutId() != 0) {
                  int ItemId = clan.getHideoutId();
                  String filename = "data/html/auction/AgitInfo.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitInfo.htm");
                  if (ClanHallTable.getInstance().getAuctionableHallById(ItemId) != null) {
                     html.replace("%AGIT_NAME%", ClanHallTable.getInstance().getAuctionableHallById(ItemId).getName());
                     html.replace("%AGIT_OWNER_PLEDGE_NAME%", clan.getName());
                     html.replace("%OWNER_PLEDGE_MASTER%", clan.getLeaderName());
                     html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ItemId).getGrade() * 10));
                     html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ItemId).getLease()));
                     html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(ItemId).getLocation());
                     html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_start");
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  } else {
                     LOGGER.warning("Clan Hall ID NULL : " + ItemId + " Can be caused by concurent write in ClanHallManager");
                  }

                  player.sendPacket(html);
                  return;
               }

               if (clan != null && clan.getHideoutId() == 0) {
                  player.sendPacket(SystemMessageId.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR);
                  return;
               }

               if (clan == null) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION);
                  return;
               }
            } else {
               if (actualCommand.equalsIgnoreCase("cancelBid")) {
                  long bidx = ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
                  String filename = "data/html/auction/AgitBidCancel.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitBidCancel.htm");
                  html.replace("%AGIT_BID%", String.valueOf(bidx));
                  html.replace("%AGIT_BID_REMAIN%", String.valueOf((long)(bidx * 0.9)));
                  html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_selectedItems");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("doCancelBid")) {
                  if (ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt()) != null) {
                     ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt()).cancelBid(player.getClanId());
                     player.sendPacket(SystemMessageId.YOU_HAVE_CANCELED_YOUR_BID);
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("cancelAuction")) {
                  if (!player.hasAccess(ClanAccess.HALL_AUCTION)) {
                     String filename = "data/html/auction/not_authorized.htm";
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/auction/not_authorized.htm");
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                     player.sendPacket(html);
                     return;
                  }

                  String filename = "data/html/auction/AgitSaleCancel.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitSaleCancel.htm");
                  html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(clan).getLease()));
                  html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_selectedItems");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("doCancelAuction")) {
                  if (ClanHallAuctionManager.getInstance().getAuction(clan.getHideoutId()) != null) {
                     ClanHallAuctionManager.getInstance().getAuction(clan.getHideoutId()).cancelAuction();
                     player.sendMessage("Your auction has been canceled");
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("sale2")) {
                  String filename = "data/html/auction/AgitSale2.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitSale2.htm");
                  html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(clan).getLease()));
                  html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_sale");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("sale")) {
                  if (!player.hasAccess(ClanAccess.HALL_AUCTION)) {
                     String filename = "data/html/auction/not_authorized.htm";
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/auction/not_authorized.htm");
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                     player.sendPacket(html);
                     return;
                  }

                  String filename = "data/html/auction/AgitSale1.htm";
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/AgitSale1.htm");
                  html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(clan).getLease()));
                  html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(clan.getWarehouse().getAdena()));
                  html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_selectedItems");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("rebid")) {
                  if (!player.hasAccess(ClanAccess.HALL_AUCTION)) {
                     String filename = "data/html/auction/not_authorized.htm";
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/auction/not_authorized.htm");
                     html.replace("%objectId%", String.valueOf(this.getObjectId()));
                     player.sendPacket(html);
                     return;
                  }

                  try {
                     String filename = "data/html/auction/AgitBid2.htm";
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/auction/AgitBid2.htm");
                     ClanHallAuction ax = ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());
                     if (ax != null) {
                        html.replace("%AGIT_AUCTION_BID%", String.valueOf(ax.getBidders().get(player.getClanId()).getBid()));
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(ax.getStartingBid()));
                        html.replace("%AGIT_AUCTION_END%", Instant.ofEpochMilli(ax.getEndDate()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_selectedItems");
                        html.replace("npc_%objectId%_bid1", "npc_" + this.getObjectId() + "_bid1 " + ax.getId());
                     } else {
                        LOGGER.warning("Auctioneer Auction null for AuctionBiddedAt : " + clan.getAuctionBiddedAt());
                     }

                     player.sendPacket(html);
                  } catch (Exception var23) {
                     player.sendMessage("Invalid auction!");
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("location")) {
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setFile(player, "data/html/auction/location.htm");
                  html.replace("%location%", MapRegionData.getInstance().getClosestTownName(player));
                  html.replace("%LOCATION%", this.getPictureName(player));
                  html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + this.getObjectId() + "_start");
                  player.sendPacket(html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("start")) {
                  this.showChatWindow(player);
                  return;
               }
            }
         }

         super.onBypassFeedback(player, command);
      }
   }

   @Override
   public void showChatWindow(Player player) {
      int condition = this.validateCondition();
      String filename;
      if (condition == 1) {
         filename = "data/html/auction/auction-busy.htm";
      } else {
         filename = "data/html/auction/auction.htm";
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcId%", String.valueOf(this.getId()));
      html.replace("%npcname%", this.getName());
      player.sendPacket(html);
   }

   private int validateCondition() {
      if (this.getCastle() == null || this.getCastle().getResidenceId() <= 0) {
         return 0;
      } else {
         return this.getCastle().getSiege().isInProgress() ? 1 : 3;
      }
   }

   private String getPictureName(Player plyr) {
      int nearestTownId = MapRegionData.getInstance().getMapRegionLocId(plyr);

      return switch (nearestTownId) {
         case 911 -> "GLUDIN";
         case 912 -> "GLUDIO";
         case 916 -> "DION";
         case 918 -> "GIRAN";
         case 1537 -> "RUNE";
         case 1538 -> "GODARD";
         case 1714 -> "SCHUTTGART";
         default -> "ADEN";
      };
   }
}
