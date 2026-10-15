package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;

public class ExShowManorDefaultInfo extends ServerPacket {
   private final List<Seed> _crops = CastleManorManager.getInstance().getCrops();
   private final boolean _hideButtons;

   public ExShowManorDefaultInfo(boolean hideButtons) {
      this._hideButtons = hideButtons;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_MANOR_DEFAULT_INFO.writeId(this, buffer);
      buffer.writeByte(this._hideButtons);
      buffer.writeInt(this._crops.size());

      for (Seed crop : this._crops) {
         buffer.writeInt(crop.getCropId());
         buffer.writeInt(crop.getLevel());
         buffer.writeInt(crop.getSeedReferencePrice());
         buffer.writeInt(crop.getCropReferencePrice());
         buffer.writeByte(1);
         buffer.writeInt(crop.getReward(1));
         buffer.writeByte(1);
         buffer.writeInt(crop.getReward(2));
      }
   }
}
