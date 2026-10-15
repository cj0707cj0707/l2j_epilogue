package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;

public class RequestShortcutReg extends ClientPacket {
   private ShortcutType _type;
   private int _id;
   private int _slot;
   private int _page;
   private int _level;
   private int _characterType;

   @Override
   protected void readImpl() {
      int typeId = this.readInt();
      this._type = ShortcutType.values()[typeId >= 1 && typeId <= 6 ? typeId : 0];
      int slot = this.readInt();
      this._slot = slot % 12;
      this._page = slot / 12;
      this._id = this.readInt();
      this._level = this.readInt();
      this._characterType = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._page <= 10 && this._page >= 0) {
            Shortcut sc = new Shortcut(this._slot, this._page, this._type, this._id, this._level, this._characterType);
            player.registerShortcut(sc);
            player.sendPacket(new ShortcutRegister(sc));
         }
      }
   }
}
