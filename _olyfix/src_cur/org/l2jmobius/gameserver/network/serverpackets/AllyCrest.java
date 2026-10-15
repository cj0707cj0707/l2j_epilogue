package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class AllyCrest extends ServerPacket {
   private final int _crestId;
   private final byte[] _data;

   public AllyCrest(int crestId) {
      this._crestId = crestId;
      Crest crest = CrestTable.getInstance().getCrest(crestId);
      this._data = crest != null ? crest.getData() : null;
   }

   public AllyCrest(int crestId, byte[] data) {
      this._crestId = crestId;
      this._data = data;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.ALLIANCE_CREST.writeId(this, buffer);
      buffer.writeInt(this._crestId);
      if (this._data != null) {
         buffer.writeInt(this._data.length);
         buffer.writeBytes(this._data);
      } else {
         buffer.writeInt(0);
      }
   }
}
