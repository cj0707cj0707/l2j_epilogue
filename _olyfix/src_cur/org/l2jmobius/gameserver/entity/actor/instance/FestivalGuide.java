package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class FestivalGuide extends Npc {
   private final int _festivalType;
   private final int _festivalOracle;
   private final int _blueStonesNeeded;
   private final int _greenStonesNeeded;
   private final int _redStonesNeeded;

   public FestivalGuide(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FestivalGuide);
      switch (this.getId()) {
         case 31127:
         case 31132:
            this._festivalType = 0;
            this._festivalOracle = 2;
            this._blueStonesNeeded = 900;
            this._greenStonesNeeded = 540;
            this._redStonesNeeded = 270;
            break;
         case 31128:
         case 31133:
            this._festivalType = 1;
            this._festivalOracle = 2;
            this._blueStonesNeeded = 1500;
            this._greenStonesNeeded = 900;
            this._redStonesNeeded = 450;
            break;
         case 31129:
         case 31134:
            this._festivalType = 2;
            this._festivalOracle = 2;
            this._blueStonesNeeded = 3000;
            this._greenStonesNeeded = 1800;
            this._redStonesNeeded = 900;
            break;
         case 31130:
         case 31135:
            this._festivalType = 3;
            this._festivalOracle = 2;
            this._blueStonesNeeded = 4500;
            this._greenStonesNeeded = 2700;
            this._redStonesNeeded = 1350;
            break;
         case 31131:
         case 31136:
            this._festivalType = 4;
            this._festivalOracle = 2;
            this._blueStonesNeeded = 6000;
            this._greenStonesNeeded = 3600;
            this._redStonesNeeded = 1800;
            break;
         case 31137:
         case 31142:
            this._festivalType = 0;
            this._festivalOracle = 1;
            this._blueStonesNeeded = 900;
            this._greenStonesNeeded = 540;
            this._redStonesNeeded = 270;
            break;
         case 31138:
         case 31143:
            this._festivalType = 1;
            this._festivalOracle = 1;
            this._blueStonesNeeded = 1500;
            this._greenStonesNeeded = 900;
            this._redStonesNeeded = 450;
            break;
         case 31139:
         case 31144:
            this._festivalType = 2;
            this._festivalOracle = 1;
            this._blueStonesNeeded = 3000;
            this._greenStonesNeeded = 1800;
            this._redStonesNeeded = 900;
            break;
         case 31140:
         case 31145:
            this._festivalType = 3;
            this._festivalOracle = 1;
            this._blueStonesNeeded = 4500;
            this._greenStonesNeeded = 2700;
            this._redStonesNeeded = 1350;
            break;
         case 31141:
         case 31146:
            this._festivalType = 4;
            this._festivalOracle = 1;
            this._blueStonesNeeded = 6000;
            this._greenStonesNeeded = 3600;
            this._redStonesNeeded = 1800;
            break;
         default:
            this._festivalType = 4;
            this._festivalOracle = 0;
            this._blueStonesNeeded = 0;
            this._greenStonesNeeded = 0;
            this._redStonesNeeded = 0;
      }
   }

   public int getFestivalType() {
      return this._festivalType;
   }

   public int getFestivalOracle() {
      return this._festivalOracle;
   }

   public int getStoneCount(int stoneType) {
      switch (stoneType) {
         case 6360:
            return this._blueStonesNeeded;
         case 6361:
            return this._greenStonesNeeded;
         case 6362:
            return this._redStonesNeeded;
         default:
            return -1;
      }
   }

   public void showChatWindow(Player player, int value, String suffix, boolean isDescription) {
      String filename = "data/html/seven_signs/festival/";
      filename = filename + (isDescription ? "desc_" : "festival_");
      filename = filename + (suffix != null ? value + suffix + ".htm" : value + ".htm");
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%festivalType%", SevenSignsFestival.getFestivalName(this._festivalType));
      html.replace("%cycleMins%", String.valueOf(SevenSignsFestival.getInstance().getMinsToNextCycle()));
      if (!isDescription && "2b".equals(value + suffix)) {
         html.replace("%minFestivalPartyMembers%", String.valueOf(FeatureConfig.ALT_FESTIVAL_MIN_PLAYER));
      }

      if (value == 5) {
         html.replace("%statsTable%", getStatsTable());
      }

      if (value == 6) {
         html.replace("%bonusTable%", getBonusTable());
      }

      if (value == 1) {
         html.replace("%blueStoneNeeded%", String.valueOf(this._blueStonesNeeded));
         html.replace("%greenStoneNeeded%", String.valueOf(this._greenStonesNeeded));
         html.replace("%redStoneNeeded%", String.valueOf(this._redStonesNeeded));
      }

      player.sendPacket(html);
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   private static String getStatsTable() {
      StringBuilder tableHtml = new StringBuilder(1000);

      for (int i = 0; i < 5; i++) {
         int dawnScore = SevenSignsFestival.getInstance().getHighestScore(2, i);
         int duskScore = SevenSignsFestival.getInstance().getHighestScore(1, i);
         String festivalName = SevenSignsFestival.getFestivalName(i);
         String winningCabal = "Children of Dusk";
         if (dawnScore > duskScore) {
            winningCabal = "Children of Dawn";
         } else if (dawnScore == duskScore) {
            winningCabal = "None";
         }

         tableHtml.append(
            "<tr><td width=\"100\" align=\"center\">"
               + festivalName
               + "</td><td align=\"center\" width=\"35\">"
               + duskScore
               + "</td><td align=\"center\" width=\"35\">"
               + dawnScore
               + "</td><td align=\"center\" width=\"130\">"
               + winningCabal
               + "</td></tr>"
         );
      }

      return tableHtml.toString();
   }

   private static String getBonusTable() {
      StringBuilder tableHtml = new StringBuilder(500);

      for (int i = 0; i < 5; i++) {
         int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
         String festivalName = SevenSignsFestival.getFestivalName(i);
         tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
      }

      return tableHtml.toString();
   }
}
