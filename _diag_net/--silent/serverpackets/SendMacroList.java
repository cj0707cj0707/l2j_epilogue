package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.holders.player.Macro;
import org.l2jmobius.gameserver.entity.actor.holders.player.MacroCmd;

public class SendMacroList extends ServerPacket {
   private final int _rev;
   private final int _count;
   private final Macro _macro;

   public SendMacroList(int rev, int count, Macro macro) {
      this._rev = rev;
      this._count = count;
      this._macro = macro;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.MACRO_LIST.writeId(this, buffer);
      buffer.writeInt(this._rev);
      buffer.writeByte(0);
      buffer.writeByte(this._count);
      buffer.writeByte(this._macro != null);
      if (this._macro != null) {
         buffer.writeInt(this._macro.getId());
         buffer.writeString(this._macro.getName());
         buffer.writeString(this._macro.getDescr());
         buffer.writeString(this._macro.getAcronym());
         buffer.writeByte(this._macro.getIcon());
         buffer.writeByte(this._macro.getCommands().size());
         int i = 1;

         for (MacroCmd cmd : this._macro.getCommands()) {
            buffer.writeByte(i++);
            buffer.writeByte(cmd.getType().ordinal());
            buffer.writeInt(cmd.getD1());
            buffer.writeByte(cmd.getD2());
            buffer.writeString(cmd.getCmd());
         }
      }
   }
}
