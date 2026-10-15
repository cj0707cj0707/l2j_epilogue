package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

public class RequestHennaEquip extends ClientPacket {
   private int _symbolId;

   @Override
   protected void readImpl() {
      this._symbolId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this.getClient().getFloodProtectors().canPerformTransaction()) {
            if (player.getHennaEmptySlots() == 0) {
               player.sendPacket(SystemMessageId.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               Henna henna = HennaData.getInstance().getHenna(this._symbolId);
               if (henna == null) {
                  PacketLogger.warning(this.getClass().getName() + ": Invalid Henna Id: " + this._symbolId + " from " + player);
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  long count = player.getInventory().getInventoryItemCount(henna.getDyeItemId(), -1);
                  if (henna.isAllowedClass(player.getPlayerClass())
                     && count >= henna.getWearCount()
                     && player.getAdena() >= henna.getWearFee()
                     && player.addHenna(henna)) {
                     player.destroyItemByItemId(ItemProcessType.FEE, henna.getDyeItemId(), henna.getWearCount(), player, true);
                     player.getInventory().reduceAdena(ItemProcessType.FEE, henna.getWearFee(), player, player.getLastFolkNPC());
                     InventoryUpdate iu = new InventoryUpdate();
                     iu.addModifiedItem(player.getInventory().getAdenaInstance());
                     player.sendInventoryUpdate(iu);
                     player.sendPacket(SystemMessageId.THE_SYMBOL_HAS_BEEN_ADDED);
                  } else {
                     player.sendPacket(SystemMessageId.THE_SYMBOL_CANNOT_BE_DRAWN);
                     if (!player.isGM() && !henna.isAllowedClass(player.getPlayerClass())) {
                        PunishmentManager.handleIllegalPlayerAction(
                           player, "Exploit attempt: " + player + " tryed to add a forbidden henna.", GeneralConfig.DEFAULT_PUNISH
                        );
                     }

                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }
}
