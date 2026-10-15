package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestModifyBookMarkSlot extends ClientPacket {
   private int _id;
   private int _icon;
   private String _name;
   private String _tag;

   @Override
   protected void readImpl() {
      this._id = this.readInt();
      this._name = this.readString();
      this._icon = this.readInt();
      this._tag = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.teleportBookmarkModify(this._id, this._icon, this._tag, this._name);
      }
   }
}
