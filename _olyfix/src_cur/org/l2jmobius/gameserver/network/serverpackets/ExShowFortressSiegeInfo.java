package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowFortressSiegeInfo extends ServerPacket {
   private final int _fortId;
   private final int _size;
   private final int _csize;
   private final int _csize2;

   public ExShowFortressSiegeInfo(Fort fort) {
      this._fortId = fort.getResidenceId();
      this._size = fort.getFortSize();
      List<FortSpawnHolder> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(this._fortId);
      this._csize = commanders == null ? 0 : commanders.size();
      this._csize2 = fort.getSiege().getCommanders().size();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_FORTRESS_SIEGE_INFO.writeId(this, buffer);
      buffer.writeInt(this._fortId);
      buffer.writeInt(this._size);
      if (this._csize > 0) {
         switch (this._csize) {
            case 3:
               switch (this._csize2) {
                  case 0:
                     buffer.writeInt(3);
                     return;
                  case 1:
                     buffer.writeInt(2);
                     return;
                  case 2:
                     buffer.writeInt(1);
                     return;
                  case 3:
                     buffer.writeInt(0);
                     return;
                  default:
                     return;
               }
            case 4:
               switch (this._csize2) {
                  case 0:
                     buffer.writeInt(5);
                     break;
                  case 1:
                     buffer.writeInt(4);
                     break;
                  case 2:
                     buffer.writeInt(3);
                     break;
                  case 3:
                     buffer.writeInt(2);
                     break;
                  case 4:
                     buffer.writeInt(1);
               }
         }
      } else {
         for (int i = 0; i < this._size; i++) {
            buffer.writeInt(0);
         }
      }
   }
}
