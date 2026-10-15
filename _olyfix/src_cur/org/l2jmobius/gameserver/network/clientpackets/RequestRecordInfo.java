package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestRecordInfo extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.updateUserInfo();
         World.forEachVisibleObject(player, WorldObject.class, object -> {
            if (object.isVisibleFor(player)) {
               object.sendInfo(player);
               if (object.isCreature()) {
                  Creature creature = object.asCreature();
                  if (creature.hasAI()) {
                     creature.getAI().describeStateToPlayer(player);
                  }
               }
            }
         });
      }
   }
}
