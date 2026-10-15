package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MyTargetSelected extends ServerPacket {
   private final int _objectId;
   private final int _color;

   public MyTargetSelected(Player player, Creature target) {
      this._objectId = target instanceof ControllableAirShip ? ((ControllableAirShip)target).getHelmObjectId() : target.getObjectId();
      this._color = target.isAutoAttackable(player) ? player.getLevel() - target.getLevel() : 0;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MY_TARGET_SELECTED.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeShort(this._color);
      buffer.writeInt(0);
   }
}
