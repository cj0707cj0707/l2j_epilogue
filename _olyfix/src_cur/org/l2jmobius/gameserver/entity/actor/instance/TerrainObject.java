package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class TerrainObject extends Npc {
   public TerrainObject(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.TerrainObject);
   }

   @Override
   public void onAction(Player player, boolean interact) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   @Override
   public void onActionShift(Player player) {
      if (player.isGM()) {
         super.onActionShift(player);
      } else {
         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }
}
