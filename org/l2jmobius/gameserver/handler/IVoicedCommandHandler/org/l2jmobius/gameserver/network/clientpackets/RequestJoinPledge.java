package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinPledge;

public class RequestJoinPledge extends ClientPacket {
   private int _target;
   private int _pledgeType;

   @Override
   protected void readImpl() {
      this._target = this.readInt();
      this._pledgeType = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            Player target = World.getPlayer(this._target);
            if (target == null) {
               player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            } else if (clan.checkClanJoinCondition(player, target, this._pledgeType)) {
               if (player.getRequest().setRequest(target, this)) {
                  String pledgeName = player.getClan().getName();
                  String subPledgeName = player.getClan().getSubPledge(this._pledgeType) != null
                     ? player.getClan().getSubPledge(this._pledgeType).getName()
                     : null;
                  target.sendPacket(new AskJoinPledge(player.getObjectId(), subPledgeName, this._pledgeType, pledgeName));
               }
            }
         }
      }
   }

   public int getPledgeType() {
      return this._pledgeType;
   }
}
