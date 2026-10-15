package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.TeleportBookmark;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExGetBookMarkInfoPacket extends ServerPacket {
   private final Player _player;

   public ExGetBookMarkInfoPacket(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_GET_BOOKMARK_INFO.writeId(this, buffer);
      buffer.writeInt(0);
      buffer.writeInt(this._player.getBookmarkSlot());
      buffer.writeInt(this._player.getTeleportBookmarks().size());

      for (TeleportBookmark tpbm : this._player.getTeleportBookmarks()) {
         buffer.writeInt(tpbm.getId());
         buffer.writeInt(tpbm.getX());
         buffer.writeInt(tpbm.getY());
         buffer.writeInt(tpbm.getZ());
         buffer.writeString(tpbm.getName());
         buffer.writeInt(tpbm.getIcon());
         buffer.writeString(tpbm.getTag());
      }
   }
}
