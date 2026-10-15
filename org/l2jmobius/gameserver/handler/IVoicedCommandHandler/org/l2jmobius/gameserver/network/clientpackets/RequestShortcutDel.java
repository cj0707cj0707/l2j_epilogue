package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestShortcutDel extends ClientPacket {
   private int _slot;
   private int _page;

   @Override
   protected void readImpl() {
      int id = this.readInt();
      this._slot = id % 12;
      this._page = id / 12;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._page <= 10 && this._page >= 0) {
            player.deleteShortcut(this._slot, this._page);
         }
      }
   }
}
