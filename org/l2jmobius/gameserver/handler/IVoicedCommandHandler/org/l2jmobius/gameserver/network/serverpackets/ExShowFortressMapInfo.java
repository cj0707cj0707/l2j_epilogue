package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowFortressMapInfo extends ServerPacket {
   private final Fort _fortress;
   private final FortSiege _siege;
   private final List<FortSpawnHolder> _commanders;

   public ExShowFortressMapInfo(Fort fortress) {
      this._fortress = fortress;
      this._siege = fortress.getSiege();
      this._commanders = FortSiegeManager.getInstance().getCommanderSpawnList(fortress.getResidenceId());
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_FORTRESS_MAP_INFO.writeId(this, buffer);
      buffer.writeInt(this._fortress.getResidenceId());
      buffer.writeInt(this._siege.isInProgress());
      buffer.writeInt(this._fortress.getFortSize());
      if (this._commanders != null && !this._commanders.isEmpty() && this._siege.isInProgress()) {
         switch (this._commanders.size()) {
            case 3:
               for (FortSpawnHolder spawn : this._commanders) {
                  if (this.isSpawned(spawn.getId())) {
                     buffer.writeInt(0);
                  } else {
                     buffer.writeInt(1);
                  }
               }
               break;
            case 4:
               int count = 0;

               for (FortSpawnHolder spawn : this._commanders) {
                  if (++count == 4) {
                     buffer.writeInt(1);
                  }

                  if (this.isSpawned(spawn.getId())) {
                     buffer.writeInt(0);
                  } else {
                     buffer.writeInt(1);
                  }
               }
         }
      } else {
         for (int i = 0; i < this._fortress.getFortSize(); i++) {
            buffer.writeInt(0);
         }
      }
   }

   private boolean isSpawned(int npcId) {
      boolean ret = false;

      for (Spawn spawn : this._siege.getCommanders()) {
         if (spawn.getId() == npcId) {
            ret = true;
            break;
         }
      }

      return ret;
   }
}
