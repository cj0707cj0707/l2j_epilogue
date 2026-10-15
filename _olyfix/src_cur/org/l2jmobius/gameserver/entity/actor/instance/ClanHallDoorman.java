package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Arrays;
import java.util.StringTokenizer;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ClanHallDoorman extends Doorman {
   private volatile boolean _init = false;
   private ClanHall _clanHall = null;
   private boolean _hasEvolve = false;
   private static final int[] CH_WITH_EVOLVE = new int[]{36, 37, 38, 39, 40, 41, 51, 52, 53, 54, 55, 56, 57};

   public ClanHallDoorman(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.ClanHallDoorman);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (this._hasEvolve && command.startsWith("evolve") && this.isOwnerClan(player)) {
         StringTokenizer st = new StringTokenizer(command, " ");
         if (st.countTokens() >= 2) {
            st.nextToken();
            boolean ok = false;
            switch (Integer.parseInt(st.nextToken())) {
               case 1:
                  ok = PetManager.doEvolve(player, this, 9882, 10307, 55);
                  break;
               case 2:
                  ok = PetManager.doEvolve(player, this, 4422, 10308, 55);
                  break;
               case 3:
                  ok = PetManager.doEvolve(player, this, 4423, 10309, 55);
                  break;
               case 4:
                  ok = PetManager.doEvolve(player, this, 4424, 10310, 55);
                  break;
               case 5:
                  ok = PetManager.doEvolve(player, this, 10426, 10611, 70);
            }

            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            if (ok) {
               html.setFile(player, "data/html/clanHallDoorman/evolve-ok.htm");
            } else {
               html.setFile(player, "data/html/clanHallDoorman/evolve-no.htm");
            }

            player.sendPacket(html);
         }
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   @Override
   public void showChatWindow(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      if (this.getClanHall() != null) {
         Clan owner = ClanTable.getInstance().getClan(this.getClanHall().getOwnerId());
         if (this.isOwnerClan(player)) {
            if (this._hasEvolve) {
               html.setFile(player, "data/html/clanHallDoorman/doorman2.htm");
               html.replace("%clanname%", owner.getName());
            } else {
               html.setFile(player, "data/html/clanHallDoorman/doorman1.htm");
               html.replace("%clanname%", owner.getName());
            }
         } else if (owner != null && owner.getLeader() != null) {
            html.setFile(player, "data/html/clanHallDoorman/doorman-no.htm");
            html.replace("%leadername%", owner.getLeaderName());
            html.replace("%clanname%", owner.getName());
         } else {
            html.setFile(player, "data/html/clanHallDoorman/emptyowner.htm");
            html.replace("%hallname%", this.getClanHall().getName());
         }

         html.replace("%objectId%", String.valueOf(this.getObjectId()));
         player.sendPacket(html);
      }
   }

   @Override
   protected void openDoors(Player player, String command) {
      this.getClanHall().openCloseDoors(true);
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, "data/html/clanHallDoorman/doorman-opened.htm");
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      player.sendPacket(html);
   }

   @Override
   protected void closeDoors(Player player, String command) {
      this.getClanHall().openCloseDoors(false);
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, "data/html/clanHallDoorman/doorman-closed.htm");
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      player.sendPacket(html);
   }

   private ClanHall getClanHall() {
      if (!this._init) {
         synchronized (this) {
            if (!this._init) {
               this._clanHall = ClanHallTable.getInstance().getNearbyClanHall(this.getX(), this.getY(), 500);
               if (this._clanHall != null) {
                  this._hasEvolve = Arrays.binarySearch(CH_WITH_EVOLVE, this._clanHall.getId()) >= 0;
               }

               this._init = true;
            }
         }
      }

      return this._clanHall;
   }

   @Override
   protected boolean isOwnerClan(Player player) {
      return player.getClan() != null && this.getClanHall() != null && player.getClanId() == this.getClanHall().getOwnerId();
   }
}
