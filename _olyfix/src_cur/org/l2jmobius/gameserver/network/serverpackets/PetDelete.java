package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PetDelete extends ServerPacket {
   private final int _petType;
   private final int _petObjId;

   public PetDelete(int petType, int petObjId) {
      this._petType = petType;
      this._petObjId = petObjId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PET_DELETE.writeId(this, buffer);
      buffer.writeInt(this._petType);
      buffer.writeInt(this._petObjId);
   }
}
