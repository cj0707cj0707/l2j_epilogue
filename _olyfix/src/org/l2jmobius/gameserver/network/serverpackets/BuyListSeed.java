package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class BuyListSeed extends ServerPacket {
   private final int _manorId;
   private final long _money;
   private final List<SeedProduction> _list = new ArrayList<>();

   public BuyListSeed(long currentMoney, int castleId) {
      this._money = currentMoney;
      this._manorId = castleId;

      for (SeedProduction s : CastleManorManager.getInstance().getSeedProduction(castleId, false)) {
         if (s.getAmount() > 0L && s.getPrice() > 0L) {
            this._list.add(s);
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.BUY_LIST_SEED.writeId(this, buffer);
      buffer.writeLong(this._money);
      buffer.writeInt(this._manorId);
      if (!this._list.isEmpty()) {
         buffer.writeShort(this._list.size());

         for (SeedProduction s : this._list) {
            buffer.writeShort(4);
            buffer.writeInt(0);
            buffer.writeInt(s.getId());
            buffer.writeLong(s.getAmount());
            buffer.writeShort(4);
            buffer.writeShort(0);
            buffer.writeLong(s.getPrice());
         }

         this._list.clear();
      } else {
         buffer.writeShort(0);
      }
   }
}
