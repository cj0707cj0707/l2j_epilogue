package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerDelete;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.CharDeleteFail;
import org.l2jmobius.gameserver.network.serverpackets.CharDeleteSuccess;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

public class CharacterDelete extends ClientPacket {
   private int _charSlot;

   @Override
   protected void readImpl() {
      this._charSlot = this.readInt();
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.GameClient client = (org.l2jmobius.gameserver.network.GameClient)this.getClient();

      try {
         switch (client.markToDeleteChar(this._charSlot)) {
            case -1:
            default:
               break;
            case 0:
               client.sendPacket(new CharDeleteSuccess());
               if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_DELETE, Containers.Players())) {
                  CharacterInfoHolder charInfo = client.getCharSelection(this._charSlot);
                  EventDispatcher.getInstance().notifyEvent(new OnPlayerDelete(charInfo.getObjectId(), charInfo.getName(), client), Containers.Players());
               }
               break;
            case 1:
               client.sendPacket(new CharDeleteFail(2));
               break;
            case 2:
               client.sendPacket(new CharDeleteFail(3));
         }
      } catch (Exception var3) {
         org.l2jmobius.gameserver.network.PacketLogger.warning(this.getClass().getSimpleName() + ": " + var3.getMessage());
      }

      CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1, 0);
      client.sendPacket(cl);
      client.setCharSelection(cl.getCharInfo());
   }
}
