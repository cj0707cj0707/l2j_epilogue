package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;

public class ShortcutInit extends ServerPacket {
   private Collection<Shortcut> _shortcuts;

   public ShortcutInit(Player player) {
      if (player != null) {
         this._shortcuts = player.getAllShortcuts();
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHORT_CUT_INIT.writeId(this, buffer);
      buffer.writeInt(this._shortcuts.size());

      for (Shortcut sc : this._shortcuts) {
         buffer.writeInt(sc.getType().ordinal());
         buffer.writeInt(sc.getSlot() + sc.getPage() * 12);
         switch (sc.getType()) {
            case ITEM:
               buffer.writeInt(sc.getId());
               buffer.writeInt(1);
               buffer.writeInt(sc.getSharedReuseGroup());
               buffer.writeInt(0);
               buffer.writeInt(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               break;
            case SKILL:
               buffer.writeInt(sc.getId());
               buffer.writeInt(sc.getLevel());
               buffer.writeByte(0);
               buffer.writeInt(1);
               break;
            case ACTION:
            case MACRO:
            case RECIPE:
            case BOOKMARK:
               buffer.writeInt(sc.getId());
               buffer.writeInt(1);
         }
      }
   }
}
