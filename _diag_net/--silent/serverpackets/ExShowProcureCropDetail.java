package org.l2jmobius.gameserver.network.serverpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.manor.CropProcure;

public class ExShowProcureCropDetail extends ServerPacket {
   private final int _cropId;
   private final Map<Integer, CropProcure> _castleCrops = new HashMap<>();

   public ExShowProcureCropDetail(int cropId) {
      this._cropId = cropId;

      for (Castle c : CastleManager.getInstance().getCastles()) {
         CropProcure cropItem = CastleManorManager.getInstance().getCropProcure(c.getResidenceId(), cropId, false);
         if (cropItem != null && cropItem.getAmount() > 0L) {
            this._castleCrops.put(c.getResidenceId(), cropItem);
         }
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_PROCURE_CROP_DETAIL.writeId(this, buffer);
      buffer.writeInt(this._cropId);
      buffer.writeInt(this._castleCrops.size());

      for (Entry<Integer, CropProcure> entry : this._castleCrops.entrySet()) {
         CropProcure crop = entry.getValue();
         buffer.writeInt(entry.getKey());
         buffer.writeLong(crop.getAmount());
         buffer.writeLong(crop.getPrice());
         buffer.writeByte(crop.getReward());
      }
   }
}
