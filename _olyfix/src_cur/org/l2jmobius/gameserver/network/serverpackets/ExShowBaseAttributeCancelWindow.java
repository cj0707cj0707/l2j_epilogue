package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowBaseAttributeCancelWindow extends ServerPacket {
   private final Collection<Item> _items;
   private long _price;

   public ExShowBaseAttributeCancelWindow(Player player) {
      this._items = player.getInventory().getElementItems();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_BASE_ATTRIBUTE_CANCEL_WINDOW.writeId(this, buffer);
      buffer.writeInt(this._items.size());

      for (Item item : this._items) {
         buffer.writeInt(item.getObjectId());
         buffer.writeLong(this.getPrice(item));
      }
   }

   private long getPrice(Item item) {
      switch (item.getTemplate().getCrystalType()) {
         case S:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 50000L;
            } else {
               this._price = 40000L;
            }
            break;
         case S80:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 100000L;
            } else {
               this._price = 80000L;
            }
            break;
         case S84:
            if (item.getTemplate() instanceof Weapon) {
               this._price = 200000L;
            } else {
               this._price = 160000L;
            }
      }

      return this._price;
   }
}
