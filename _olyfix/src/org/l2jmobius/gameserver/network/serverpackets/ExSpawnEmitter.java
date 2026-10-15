package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExSpawnEmitter extends ServerPacket {
   private final int _playerObjectId;
   private final int _npcObjectId;

   public ExSpawnEmitter(int playerObjectId, int npcObjectId) {
      this._playerObjectId = playerObjectId;
      this._npcObjectId = npcObjectId;
   }

   public ExSpawnEmitter(Player player, Npc npc) {
      this(player.getObjectId(), npc.getObjectId());
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SPAWN_EMITTER.writeId(this, buffer);
      buffer.writeInt(this._npcObjectId);
      buffer.writeInt(this._playerObjectId);
      buffer.writeInt(0);
   }
}
