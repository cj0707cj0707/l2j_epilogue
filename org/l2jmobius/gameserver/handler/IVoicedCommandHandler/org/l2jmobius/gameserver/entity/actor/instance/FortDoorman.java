package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class FortDoorman extends Doorman {
   public FortDoorman(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FortDoorman);
   }

   @Override
   public void showChatWindow(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      if (!this.isOwnerClan(player)) {
         html.setFile(player, "data/html/doorman/" + this.getTemplate().getId() + "-no.htm");
      } else if (this.isUnderSiege()) {
         html.setFile(player, "data/html/doorman/" + this.getTemplate().getId() + "-busy.htm");
      } else {
         html.setFile(player, "data/html/doorman/" + this.getTemplate().getId() + ".htm");
      }

      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      player.sendPacket(html);
   }

   @Override
   protected void openDoors(Player player, String command) {
      StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
      st.nextToken();

      while (st.hasMoreTokens()) {
         this.getFort().openDoor(player, Integer.parseInt(st.nextToken()));
      }
   }

   @Override
   protected void closeDoors(Player player, String command) {
      StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
      st.nextToken();

      while (st.hasMoreTokens()) {
         this.getFort().closeDoor(player, Integer.parseInt(st.nextToken()));
      }
   }

   @Override
   protected boolean isOwnerClan(Player player) {
      return player.getClan() != null
         && this.getFort() != null
         && this.getFort().getOwnerClan() != null
         && player.getClanId() == this.getFort().getOwnerClan().getId();
   }

   @Override
   protected boolean isUnderSiege() {
      return this.getFort().getZone().isActive();
   }
}
