package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.ConnectionState;

public class RequestSaveKeyMapping extends ClientPacket {
   public static final String SPLIT_VAR = "\t";
   private byte[] _uiKeyMapping;

   @Override
   protected void readImpl() {
      int dataSize = this.readInt();
      if (dataSize > 0) {
         this._uiKeyMapping = this.readBytes(dataSize);
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (PlayerConfig.STORE_UI_SETTINGS && player != null && this._uiKeyMapping != null && this.getClient().getConnectionState() == ConnectionState.IN_GAME) {
         StringBuilder sb = new StringBuilder(this._uiKeyMapping.length * 4);
         byte[] var3 = this._uiKeyMapping;
         int var4 = var3.length;

         for (int var5 = 0; var5 < var4; var5++) {
            Byte b = var3[var5];
            sb.append(b).append("\t");
         }

         player.getVariables().set("UI_KEY_MAPPING", sb.toString());
      }
   }
}
