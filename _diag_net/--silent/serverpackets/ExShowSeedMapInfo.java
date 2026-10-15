package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.managers.SeedOfDestructionManager;
import org.l2jmobius.gameserver.managers.SeedOfInfinityManager;

public class ExShowSeedMapInfo extends ServerPacket {
   public static final ExShowSeedMapInfo STATIC_PACKET = new ExShowSeedMapInfo();
   private static final Location[] ENTRANCES = new Location[]{new Location(-246857, 251960, 4331, 1), new Location(-213770, 210760, 4400, 2)};

   private ExShowSeedMapInfo() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_SEED_MAP_INFO.writeId(this, buffer);
      buffer.writeInt(ENTRANCES.length);

      for (Location loc : ENTRANCES) {
         buffer.writeInt(loc.getX());
         buffer.writeInt(loc.getY());
         buffer.writeInt(loc.getZ());
         switch (loc.getHeading()) {
            case 1:
               buffer.writeInt(2770 + SeedOfDestructionManager.getInstance().getSoDState());
               break;
            case 2:
               buffer.writeInt(SeedOfInfinityManager.getCurrentStage() + 2765);
         }
      }
   }
}
