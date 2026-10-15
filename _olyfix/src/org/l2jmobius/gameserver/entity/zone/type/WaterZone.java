package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;

public class WaterZone extends ZoneType {
   public WaterZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      creature.setInsideZone(ZoneId.WATER, true);
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.isTransformed() && !player.getTransformation().canSwim()) {
            creature.stopTransformation(true);
         } else {
            player.broadcastUserInfo();
         }
      } else if (creature.isNpc()) {
         World.forEachVisibleObject(creature, Player.class, playerx -> {
            if (creature.isFakePlayer()) {
               playerx.sendPacket(new FakePlayerInfo(creature.asNpc()));
            } else if (creature.getRunSpeed() == 0.0) {
               playerx.sendPacket(new ServerObjectInfo(creature.asNpc(), playerx));
            } else {
               playerx.sendPacket(new AbstractNpcInfo.NpcInfo(creature.asNpc(), playerx));
            }
         });
      }
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.WATER, false);
      if (creature.isPlayer()) {
         if (!creature.isInsideZone(ZoneId.WATER)) {
            creature.asPlayer().stopWaterTask();
         }

         if (!creature.isTeleporting()) {
            creature.asPlayer().broadcastUserInfo();
         }
      } else if (creature.isNpc()) {
         World.forEachVisibleObject(creature, Player.class, player -> {
            if (creature.isFakePlayer()) {
               player.sendPacket(new FakePlayerInfo(creature.asNpc()));
            } else if (creature.getRunSpeed() == 0.0) {
               player.sendPacket(new ServerObjectInfo(creature.asNpc(), player));
            } else {
               player.sendPacket(new AbstractNpcInfo.NpcInfo(creature.asNpc(), player));
            }
         });
      }
   }

   public int getWaterZ() {
      return this.getZone().getHighZ();
   }
}
