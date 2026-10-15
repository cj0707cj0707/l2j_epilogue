package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortLogistics extends Merchant {
   private static final int[] SUPPLY_BOX_IDS = new int[]{
      35665, 35697, 35734, 35766, 35803, 35834, 35866, 35903, 35935, 35973, 36010, 36042, 36080, 36117, 36148, 36180, 36218, 36256, 36293, 36325, 36363
   };

   public FortLogistics(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FortLogistics);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (player.getLastFolkNPC().getObjectId() == this.getObjectId()) {
         StringTokenizer st = new StringTokenizer(command, " ");
         String actualCommand = st.nextToken();
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         if (actualCommand.equalsIgnoreCase("rewards")) {
            if (this.isMyLord(player)) {
               html.setFile(player, "data/html/fortress/logistics-rewards.htm");
               html.replace("%bloodoath%", String.valueOf(player.getClan().getBloodOathCount()));
            } else {
               html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
            }

            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
         } else if (actualCommand.equalsIgnoreCase("blood")) {
            if (this.isMyLord(player)) {
               Clan clan = player.getClan();
               int blood = clan.getBloodOathCount();
               if (blood > 0) {
                  player.addItem(ItemProcessType.QUEST, 9910, blood, this, true);
                  clan.resetBloodOathCount();
                  html.setFile(player, "data/html/fortress/logistics-blood.htm");
               } else {
                  html.setFile(player, "data/html/fortress/logistics-noblood.htm");
               }
            } else {
               html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
            }

            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
         } else if (actualCommand.equalsIgnoreCase("supplylvl")) {
            if (this.getFort().getFortState() == 2) {
               if (player.isClanLeader()) {
                  html.setFile(player, "data/html/fortress/logistics-supplylvl.htm");
                  html.replace("%supplylvl%", String.valueOf(this.getFort().getSupplyLeveL()));
               } else {
                  html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
               }
            } else {
               html.setFile(player, "data/html/fortress/logistics-1.htm");
            }

            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
         } else if (actualCommand.equalsIgnoreCase("supply")) {
            if (this.isMyLord(player)) {
               if (this.getFort().getSiege().isInProgress()) {
                  html.setFile(player, "data/html/fortress/logistics-siege.htm");
               } else {
                  int level = this.getFort().getSupplyLeveL();
                  if (level > 0) {
                     NpcTemplate boxTemplate = NpcData.getInstance().getTemplate(SUPPLY_BOX_IDS[level - 1]);
                     Monster box = new Monster(boxTemplate);
                     box.setCurrentHp(box.getMaxHp());
                     box.setCurrentMp(box.getMaxMp());
                     box.setHeading(0);
                     box.spawnMe(this.getX() - 23, this.getY() + 41, this.getZ());
                     this.getFort().setSupplyLeveL(0);
                     this.getFort().saveFortVariables();
                     html.setFile(player, "data/html/fortress/logistics-supply.htm");
                  } else {
                     html.setFile(player, "data/html/fortress/logistics-nosupply.htm");
                  }
               }
            } else {
               html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
            }

            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
         } else {
            super.onBypassFeedback(player, command);
         }
      }
   }

   @Override
   public void showChatWindow(Player player) {
      this.showMessageWindow(player, 0);
   }

   private void showMessageWindow(Player player, int value) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      String filename;
      if (value == 0) {
         filename = "data/html/fortress/logistics.htm";
      } else {
         filename = "data/html/fortress/logistics-" + value + ".htm";
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcId%", String.valueOf(this.getId()));
      if (this.getFort().getOwnerClan() != null) {
         html.replace("%clanname%", this.getFort().getOwnerClan().getName());
      } else {
         html.replace("%clanname%", "NPC");
      }

      player.sendPacket(html);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = "logistics";
      } else {
         pom = "logistics-" + value;
      }

      return "data/html/fortress/" + pom + ".htm";
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }
}
