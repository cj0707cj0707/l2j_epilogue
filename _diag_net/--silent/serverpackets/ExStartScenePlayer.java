package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.enums.Movie;

public class ExStartScenePlayer extends ServerPacket {
   private final Movie _movie;

   public ExStartScenePlayer(Movie movie) {
      this._movie = movie;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_START_SCENE_PLAYER.writeId(this, buffer);
      buffer.writeInt(this._movie.getClientId());
   }
}
