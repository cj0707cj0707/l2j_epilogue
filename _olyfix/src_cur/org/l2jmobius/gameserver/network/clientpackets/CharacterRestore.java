package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerRestore;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

public class CharacterRestore extends ClientPacket {
   private int _charSlot;

   @Override
   protected void readImpl() {
      this._charSlot = this.readInt();
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      if (client.getFloodProtectors().canSelectCharacter()) {
         client.restore(this._charSlot);
         CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1, 0);
         client.sendPacket(cl);
         client.setCharSelection(cl.getCharInfo());
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_RESTORE)) {
            CharacterInfoHolder charInfo = client.getCharSelection(this._charSlot);
            EventDispatcher.getInstance().notifyEvent(new OnPlayerRestore(charInfo.getObjectId(), charInfo.getName(), client));
         }
      }
   }
}
