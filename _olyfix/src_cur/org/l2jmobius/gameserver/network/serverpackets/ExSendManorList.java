package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Comparator;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExSendManorList extends ServerPacket {
   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      List<Castle> castles = CastleManager.getInstance().getCastles();
      castles.sort(Comparator.comparingInt(AbstractResidence::getResidenceId));
      ServerPackets.EX_SEND_MANOR_LIST.writeId(this, buffer);
      buffer.writeInt(castles.size());

      for (Castle castle : castles) {
         buffer.writeInt(castle.getResidenceId());
         buffer.writeString(castle.getName().toLowerCase());
      }
   }
}
