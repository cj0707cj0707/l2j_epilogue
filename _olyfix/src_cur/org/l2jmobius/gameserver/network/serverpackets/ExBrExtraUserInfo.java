package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBrExtraUserInfo extends ServerPacket {
   private final int _objectId;
   private final int _abnormalVisualEffectsEvent;
   private final int _lectureMark;

   public ExBrExtraUserInfo(Player player) {
      this._objectId = player.getObjectId();
      this._abnormalVisualEffectsEvent = player.getAbnormalVisualEffectEvent();
      this._lectureMark = 1;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BR_EXTRA_USER_INFO.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._abnormalVisualEffectsEvent);
      buffer.writeByte(this._lectureMark);
   }
}
