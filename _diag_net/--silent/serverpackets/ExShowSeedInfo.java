package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;

public class ExShowSeedInfo extends ServerPacket {
   private final List<SeedProduction> _seeds;
   private final int _manorId;
   private final boolean _hideButtons;

   public ExShowSeedInfo(int manorId, boolean nextPeriod, boolean hideButtons) {
      this._manorId = manorId;
      this._hideButtons = hideButtons;
      CastleManorManager manor = CastleManorManager.getInstance();
      this._seeds = nextPeriod && !manor.isManorApproved() ? null : manor.getSeedProduction(manorId, nextPeriod);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_SEED_INFO.writeId(this, buffer);
      buffer.writeByte(this._hideButtons);
      buffer.writeInt(this._manorId);
      buffer.writeInt(0);
      if (this._seeds == null) {
         buffer.writeInt(0);
      } else {
         buffer.writeInt(this._seeds.size());

         for (SeedProduction seed : this._seeds) {
            buffer.writeInt(seed.getId());
            buffer.writeLong(seed.getAmount());
            buffer.writeLong(seed.getStartAmount());
            buffer.writeLong(seed.getPrice());
            Seed s = CastleManorManager.getInstance().getSeed(seed.getId());
            if (s == null) {
               buffer.writeInt(0);
               buffer.writeByte(1);
               buffer.writeInt(0);
               buffer.writeByte(1);
               buffer.writeInt(0);
            } else {
               buffer.writeInt(s.getLevel());
               buffer.writeByte(1);
               buffer.writeInt(s.getReward(1));
               buffer.writeByte(1);
               buffer.writeInt(s.getReward(2));
            }
         }
      }
   }
}
