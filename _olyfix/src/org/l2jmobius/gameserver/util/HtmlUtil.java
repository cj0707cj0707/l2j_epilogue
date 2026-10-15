package org.l2jmobius.gameserver.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

public class HtmlUtil {
   private static final Logger LOGGER = Logger.getLogger(HtmlUtil.class.getName());

   public static String getCpGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_CP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_CP_Center", 17L, -13L
      );
   }

   public static String getHpGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HP_Center", 17L, -13L
      );
   }

   public static String getHpWarnGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HPWarn_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HPWarn_Center", 17L, -13L
      );
   }

   public static String getHpFillGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HPFill_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HPFill_Center", 17L, -13L
      );
   }

   public static String getMpGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_MP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_MP_Center", 17L, -13L
      );
   }

   public static String getExpGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_EXP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_EXP_Center", 17L, -13L
      );
   }

   public static String getFoodGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getGauge(
         width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_Food_Bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_Food_Center", 17L, -13L
      );
   }

   public static String getWeightGauge(int width, long current, long max, boolean displayAsPercentage) {
      return getWeightGauge(width, current, max, displayAsPercentage, MathUtil.scaleToRange(current, 0L, max, 1L, 5L));
   }

   public static String getWeightGauge(int width, long current, long max, boolean displayAsPercentage, long level) {
      return getGauge(
         width,
         current,
         max,
         displayAsPercentage,
         "L2UI_CT1.Gauges.Gauge_DF_Large_Weight_bg_Center" + level,
         "L2UI_CT1.Gauges.Gauge_DF_Large_Weight_Center" + level,
         17L,
         -13L
      );
   }

   private static String getGauge(
      int width, long currentValue, long max, boolean displayAsPercentage, String backgroundImage, String image, long imageHeight, long top
   ) {
      long current = Math.min(currentValue, max);
      StringBuilder sb = new StringBuilder();
      sb.append("<table width=");
      sb.append(width);
      sb.append(" cellpadding=0 cellspacing=0>");
      sb.append("<tr>");
      sb.append("<td background=\"");
      sb.append(backgroundImage);
      sb.append("\">");
      sb.append("<img src=\"");
      sb.append(image);
      sb.append("\" width=");
      sb.append((long)((double)current / max * width));
      sb.append(" height=");
      sb.append(imageHeight);
      sb.append('>');
      sb.append("</td>");
      sb.append("</tr>");
      sb.append("<tr>");
      sb.append("<td align=center>");
      sb.append("<table cellpadding=0 cellspacing=");
      sb.append(top);
      sb.append('>');
      sb.append("<tr>");
      sb.append("<td>");
      if (displayAsPercentage) {
         sb.append("<table cellpadding=0 cellspacing=2>");
         sb.append("<tr><td>");
         sb.append(String.format("%.2f%%", (double)current / max * 100.0));
         sb.append("</td></tr>");
         sb.append("</table>");
      } else {
         int tdWidth = (width - 10) / 2;
         sb.append("<table cellpadding=0 cellspacing=0>");
         sb.append("<tr>");
         sb.append("<td width=");
         sb.append(tdWidth);
         sb.append(" align=right>");
         sb.append(current);
         sb.append("</td>");
         sb.append("<td width=10 align=center>/</td>");
         sb.append("<td width=");
         sb.append(tdWidth);
         sb.append('>');
         sb.append(max);
         sb.append("</td>");
         sb.append("</tr>");
         sb.append("</table>");
      }

      sb.append("</td>");
      sb.append("</tr>");
      sb.append("</table>");
      sb.append("</td>");
      sb.append("</tr>");
      sb.append("</table>");
      return sb.toString();
   }

   private static void buildHtmlBypassCache(Player player, HtmlActionScope scope, String html) {
      String htmlLower = html.toLowerCase(Locale.ENGLISH);
      int bypassEnd = 0;
      int bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);

      while (bypassStart != -1) {
         int bypassStartEnd = bypassStart + 9;
         bypassEnd = htmlLower.indexOf("\"", bypassStartEnd);
         if (bypassEnd == -1) {
            break;
         }

         int hParamPos = htmlLower.indexOf("-h ", bypassStartEnd);
         String bypass;
         if (hParamPos != -1 && hParamPos < bypassEnd) {
            bypass = html.substring(hParamPos + 3, bypassEnd).trim();
         } else {
            bypass = html.substring(bypassStartEnd, bypassEnd).trim();
         }

         int firstParameterStart = bypass.indexOf(36);
         if (firstParameterStart != -1) {
            bypass = bypass.substring(0, firstParameterStart + 1);
         }

         if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG) {
            LOGGER.info("Cached html bypass(" + scope + "): '" + bypass + "'");
         }

         player.addHtmlAction(scope, bypass);
         bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
      }
   }

   private static void buildHtmlLinkCache(Player player, HtmlActionScope scope, String html) {
      String htmlLower = html.toLowerCase(Locale.ENGLISH);
      int linkEnd = 0;
      int linkStart = htmlLower.indexOf("=\"link ", linkEnd);

      while (linkStart != -1) {
         int linkStartEnd = linkStart + 7;
         linkEnd = htmlLower.indexOf("\"", linkStartEnd);
         if (linkEnd == -1) {
            break;
         }

         String htmlLink = html.substring(linkStartEnd, linkEnd).trim();
         if (htmlLink.isEmpty()) {
            LOGGER.warning("Html link path is empty!");
         } else if (htmlLink.contains("..")) {
            LOGGER.warning("Html link path is invalid: " + htmlLink);
         } else {
            if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG) {
               LOGGER.info("Cached html link(" + scope + "): '" + htmlLink + "'");
            }

            player.addHtmlAction(scope, "link " + htmlLink);
            linkStart = htmlLower.indexOf("=\"link ", linkEnd);
         }
      }
   }

   public static void buildHtmlActionCache(Player player, HtmlActionScope scope, int npcObjId, String html) {
      if (player != null && scope != null && npcObjId >= 0 && html != null) {
         if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG) {
            LOGGER.info("Set html action npc(" + scope + "): " + npcObjId);
         }

         player.setHtmlActionOriginObjectId(scope, npcObjId);
         buildHtmlBypassCache(player, scope, html);
         buildHtmlLinkCache(player, scope, html);
      } else {
         throw new IllegalArgumentException();
      }
   }

   public static void sendHtml(Player player, String html) {
      NpcHtmlMessage message = new NpcHtmlMessage();
      message.setHtml(html);
      player.sendPacket(message);
   }

   public static void sendCBHtml(Player player, String html) {
      sendCBHtml(player, html, 0);
   }

   public static void sendCBHtml(Player player, String html, int npcObjId) {
      sendCBHtml(player, html, null, npcObjId);
   }

   public static void sendCBHtml(Player player, String html, String fillMultiEdit) {
      sendCBHtml(player, html, fillMultiEdit, 0);
   }

   public static void sendCBHtml(Player player, String html, String fillMultiEdit, int npcObjId) {
      if (player != null && html != null) {
         player.clearHtmlActions(HtmlActionScope.COMM_BOARD_HTML);
         if (npcObjId > -1) {
            buildHtmlActionCache(player, HtmlActionScope.COMM_BOARD_HTML, npcObjId, html);
         }

         if (fillMultiEdit != null) {
            player.sendPacket(new ShowBoard(html, "1001"));
            fillMultiEditContent(player, fillMultiEdit);
         } else if (html.length() < 16250) {
            player.sendPacket(new ShowBoard(html, "101"));
            player.sendPacket(new ShowBoard(null, "102"));
            player.sendPacket(new ShowBoard(null, "103"));
         } else if (html.length() < 32500) {
            player.sendPacket(new ShowBoard(html.substring(0, 16250), "101"));
            player.sendPacket(new ShowBoard(html.substring(16250), "102"));
            player.sendPacket(new ShowBoard(null, "103"));
         } else if (html.length() < 48750) {
            player.sendPacket(new ShowBoard(html.substring(0, 16250), "101"));
            player.sendPacket(new ShowBoard(html.substring(16250, 32500), "102"));
            player.sendPacket(new ShowBoard(html.substring(32500), "103"));
         } else {
            player.sendPacket(new ShowBoard("<html><body><br><center>Error: HTML was too long!</center></body></html>", "101"));
            player.sendPacket(new ShowBoard(null, "102"));
            player.sendPacket(new ShowBoard(null, "103"));
         }
      }
   }

   public static void fillMultiEditContent(Player player, String text) {
      player.sendPacket(
         new ShowBoard(
            Arrays.asList(
               "0",
               "0",
               "0",
               "0",
               "0",
               "0",
               player.getName(),
               Integer.toString(player.getObjectId()),
               player.getAccountName(),
               "9",
               " ",
               " ",
               text.replace("<br>", System.lineSeparator()),
               "0",
               "0",
               "0",
               "0"
            )
         )
      );
   }

   public static int countPageNumber(int totalItems, int itemsPerPage) {
      return itemsPerPage <= 0 ? 0 : (totalItems + itemsPerPage - 1) / itemsPerPage;
   }
}
