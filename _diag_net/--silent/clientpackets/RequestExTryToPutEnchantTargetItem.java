package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;

public class RequestExTryToPutEnchantTargetItem extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (this._objectId != 0 && player != null) {
         if (!player.isEnchanting()) {
            Item scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
            if (scroll != null) {
               Item item = player.getInventory().getItemByObjectId(this._objectId);
               if (item == null) {
                  PunishmentManager.handleIllegalPlayerAction(
                     player,
                     "RequestExTryToPutEnchantTargetItem: " + player + " tried to cheat using a packet manipulation tool! Ban this player!",
                     GeneralConfig.DEFAULT_PUNISH
                  );
               } else {
                  EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
                  if (item.isEnchantable() && scrollTemplate != null && scrollTemplate.isValid(item, null)) {
                     player.setEnchanting(true);
                     player.setActiveEnchantTimestamp(System.currentTimeMillis());
                     player.sendPacket(new ExPutEnchantTargetItemResult(this._objectId));
                  } else {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
                     player.setActiveEnchantItemId(-1);
                     player.sendPacket(new ExPutEnchantTargetItemResult(0));
                     if (scrollTemplate == null) {
                        org.l2jmobius.gameserver.network.PacketLogger.warning(
                           "RequestExTryToPutEnchantTargetItem: " + player + " has used undefined scroll with id " + scroll.getId()
                        );
                     }
                  }
               }
            }
         }
      }
   }
}
