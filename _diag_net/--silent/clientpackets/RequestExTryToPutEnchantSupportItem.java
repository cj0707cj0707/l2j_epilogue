package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExPutEnchantSupportItemResult;

public class RequestExTryToPutEnchantSupportItem extends ClientPacket {
   private int _supportObjectId;
   private int _enchantObjectId;

   @Override
   protected void readImpl() {
      this._supportObjectId = this.readInt();
      this._enchantObjectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isEnchanting()) {
            Item item = player.getInventory().getItemByObjectId(this._enchantObjectId);
            Item scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
            Item support = player.getInventory().getItemByObjectId(this._supportObjectId);
            if (item == null || scroll == null || support == null) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
               player.setActiveEnchantSupportItemId(-1);
               return;
            }

            EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
            EnchantSupportItem supportTemplate = EnchantItemData.getInstance().getSupportItem(support);
            if (scrollTemplate == null || supportTemplate == null || !scrollTemplate.isValid(item, supportTemplate)) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
               player.setActiveEnchantSupportItemId(-1);
               player.sendPacket(new ExPutEnchantSupportItemResult(0));
               return;
            }

            player.setActiveEnchantSupportItemId(support.getObjectId());
            player.sendPacket(new ExPutEnchantSupportItemResult(this._supportObjectId));
         }
      }
   }
}
