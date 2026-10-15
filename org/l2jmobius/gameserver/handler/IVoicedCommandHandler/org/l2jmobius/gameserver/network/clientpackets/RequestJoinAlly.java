package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinAlly;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestJoinAlly extends ClientPacket {
   private int _id;

   @Override
   protected void readImpl() {
      this._id = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player ob = World.getPlayer(this._id);
         if (ob == null) {
            player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
         } else if (player.getClan() == null) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
         } else {
            Clan clan = player.getClan();
            if (clan.checkAllyJoinCondition(player, ob)) {
               if (player.getRequest().setRequest(ob, this)) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_LEADER_S2_HAS_REQUESTED_AN_ALLIANCE);
                  sm.addString(player.getClan().getAllyName());
                  sm.addString(player.getName());
                  ob.sendPacket(sm);
                  ob.sendPacket(new AskJoinAlly(player.getObjectId(), player.getClan().getAllyName()));
               }
            }
         }
      }
   }
}
