package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Collection;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.PackageSendableList;

public class RequestPackageSendableItemList extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Collection<Item> items = player.getInventory().getAvailableItems(true, true, true);
         player.sendPacket(new PackageSendableList(items, this._objectId, player.getAdena()));
      }
   }
}
