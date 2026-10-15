package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPVPMatchUserDie extends ServerPacket {
   private final int _blueKills;
   private final int _redKills;

   public ExPVPMatchUserDie(UCArena arena) {
      this._redKills = arena.getTeams()[0].getKillCount();
      this._blueKills = arena.getTeams()[1].getKillCount();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PVP_MATCH_USER_DIE.writeId(this, buffer);
      buffer.writeInt(this._redKills);
      buffer.writeInt(this._blueKills);
   }
}
