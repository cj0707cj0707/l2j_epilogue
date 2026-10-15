package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExListPartyMatchingWaitingRoom;

public class RequestListPartyMatchingWaitingRoom extends ClientPacket {
   private int _page;
   private int _minLevel;
   private int _maxLevel;
   private int _mode;

   @Override
   protected void readImpl() {
      this._page = this.readInt();
      this._minLevel = this.readInt();
      this._maxLevel = this.readInt();
      this._mode = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new ExListPartyMatchingWaitingRoom(player, this._page, this._minLevel, this._maxLevel, this._mode));
      }
   }
}
