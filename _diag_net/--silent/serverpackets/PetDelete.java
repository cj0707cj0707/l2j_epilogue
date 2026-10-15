package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PetDelete extends ServerPacket {
   private final int _petType;
   private final int _petObjId;

   public PetDelete(int petType, int petObjId) {
      this._petType = petType;
      this._petObjId = petObjId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PET_DELETE.writeId(this, buffer);
      buffer.writeInt(this._petType);
      buffer.writeInt(this._petObjId);
   }
}
