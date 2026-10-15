package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;

public class ShortcutRegister extends ServerPacket {
   private final Shortcut _shortcut;

   public ShortcutRegister(Shortcut shortcut) {
      this._shortcut = shortcut;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHORT_CUT_REGISTER.writeId(this, buffer);
      buffer.writeInt(this._shortcut.getType().ordinal());
      buffer.writeInt(this._shortcut.getSlot() + this._shortcut.getPage() * 12);
      switch (this._shortcut.getType()) {
         case ITEM:
            buffer.writeInt(this._shortcut.getId());
            buffer.writeInt(this._shortcut.getCharacterType());
            buffer.writeInt(this._shortcut.getSharedReuseGroup());
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
            break;
         case SKILL:
            buffer.writeInt(this._shortcut.getId());
            buffer.writeInt(this._shortcut.getLevel());
            buffer.writeByte(0);
            buffer.writeInt(this._shortcut.getCharacterType());
            break;
         case ACTION:
         case MACRO:
         case RECIPE:
         case BOOKMARK:
            buffer.writeInt(this._shortcut.getId());
            buffer.writeInt(this._shortcut.getCharacterType());
      }
   }
}
