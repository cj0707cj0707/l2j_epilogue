package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;

public class SSQInfo extends ServerPacket {
   private int _state = 0;

   public SSQInfo() {
      int compWinner = SevenSigns.getInstance().getCabalHighestScore();
      if (SevenSigns.getInstance().isSealValidationPeriod()) {
         if (compWinner == 2) {
            this._state = 2;
         } else if (compWinner == 1) {
            this._state = 1;
         }
      }
   }

   public SSQInfo(int state) {
      this._state = state;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SSQ_INFO.writeId(this, buffer);
      buffer.writeShort(256 + this._state);
   }
}
