package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExUISetting extends ServerPacket {
   public static final String SPLIT_VAR = "\t";
   private final byte[] _uiKeyMapping;

   public ExUISetting(Player player) {
      if (player.getVariables().hasVariable("UI_KEY_MAPPING")) {
         this._uiKeyMapping = player.getVariables().getByteArray("UI_KEY_MAPPING", "\t");
      } else {
         this._uiKeyMapping = null;
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_UI_SETTING.writeId(this, buffer);
      if (this._uiKeyMapping != null) {
         buffer.writeInt(this._uiKeyMapping.length);
         buffer.writeBytes(this._uiKeyMapping);
      } else {
         buffer.writeInt(0);
      }
   }
}
