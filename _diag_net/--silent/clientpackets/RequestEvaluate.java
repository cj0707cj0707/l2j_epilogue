package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestEvaluate extends ClientPacket {
   @Override
   protected void readImpl() {
      this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player target = player.getTarget().asPlayer();
         if (target == null) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.SELECT_TARGET);
         } else if (!(player.getTarget() instanceof Player)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.INVALID_TARGET);
         } else if (player.getLevel() < 10) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.ONLY_CHARACTERS_OF_LEVEL_10_OR_ABOVE_ARE_AUTHORIZED_TO_MAKE_RECOMMENDATIONS);
         } else if (player.getTarget() == player) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_RECOMMEND_YOURSELF);
         } else if (player.getRecomLeft() <= 0) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CURRENTLY_DO_NOT_HAVE_ANY_RECOMMENDATIONS);
         } else if (target.getRecomHave() >= 255) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_SELECTED_TARGET_CAN_NO_LONGER_RECEIVE_A_RECOMMENDATION);
         } else if (!PlayerConfig.ALT_RECOMMEND && !player.canRecom(target)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_CHARACTER_HAS_ALREADY_BEEN_RECOMMENDED);
         } else {
            player.giveRecom(target);
            SystemMessage sm = null;
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_RECOMMENDED_C1_YOU_HAVE_S2_RECOMMENDATIONS_LEFT);
            sm.addPcName(target);
            sm.addInt(player.getRecomLeft());
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_RECOMMENDED_BY_C1);
            sm.addPcName(player);
            target.sendPacket(sm);
            player.updateUserInfo();
            target.broadcastUserInfo();
         }
      }
   }
}
