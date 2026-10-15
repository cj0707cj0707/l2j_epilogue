package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestBuySeed extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private int _manorId;
   private List<ItemHolder> _items = null;

   @Override
   protected void readImpl() {
      this._manorId = this.readInt();
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 12 == this.remaining()) {
         this._items = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            int itemId = this.readInt();
            long cnt = this.readLong();
            if (cnt < 1L || itemId < 1) {
               this._items = null;
               return;
            }

            this._items.add(new ItemHolder(itemId, cnt));
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformTransaction()) {
            player.sendMessage("You are buying seeds too fast!");
         } else if (this._items == null) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            CastleManorManager manor = CastleManorManager.getInstance();
            if (manor.isUnderMaintenance()) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               Castle castle = CastleManager.getInstance().getCastleById(this._manorId);
               if (castle == null) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  Npc manager = player.getLastFolkNPC();
                  if (manager instanceof Merchant
                     && manager.canInteract(player)
                     && manager.getTemplate().getParameters().getInt("manor_id", -1) == this._manorId) {
                     long totalPrice = 0L;
                     int slots = 0;
                     int totalWeight = 0;
                     Map<Integer, SeedProduction> productInfo = new HashMap<>();

                     for (ItemHolder ih : this._items) {
                        SeedProduction sp = manor.getSeedProduct(this._manorId, ih.getId(), false);
                        if (sp == null || sp.getPrice() <= 0L || sp.getAmount() < ih.getCount() || Inventory.MAX_ADENA / ih.getCount() < sp.getPrice()) {
                           player.sendPacket(ActionFailed.STATIC_PACKET);
                           return;
                        }

                        totalPrice += sp.getPrice() * ih.getCount();
                        if (totalPrice > Inventory.MAX_ADENA) {
                           PunishmentManager.handleIllegalPlayerAction(
                              player,
                              "Warning!! Character "
                                 + player.getName()
                                 + " of account "
                                 + player.getAccountName()
                                 + " tried to purchase over "
                                 + Inventory.MAX_ADENA
                                 + " adena worth of goods.",
                              GeneralConfig.DEFAULT_PUNISH
                           );
                           player.sendPacket(ActionFailed.STATIC_PACKET);
                           return;
                        }

                        ItemTemplate template = ItemData.getInstance().getTemplate(ih.getId());
                        totalWeight = (int)(totalWeight + ih.getCount() * template.getWeight());
                        if (!template.isStackable()) {
                           slots = (int)(slots + ih.getCount());
                        } else if (player.getInventory().getItemByItemId(ih.getId()) == null) {
                           slots++;
                        }

                        productInfo.put(ih.getId(), sp);
                     }

                     if (!player.getInventory().validateWeight(totalWeight)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
                     } else if (!player.getInventory().validateCapacity(slots)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_INVENTORY_IS_FULL);
                     } else if (totalPrice >= 0L && player.getAdena() >= totalPrice) {
                        for (ItemHolder i : this._items) {
                           SeedProduction spx = productInfo.get(i.getId());
                           long price = spx.getPrice() * i.getCount();
                           if (spx.decreaseAmount(i.getCount()) && player.reduceAdena(ItemProcessType.BUY, price, player, false)) {
                              player.addItem(ItemProcessType.BUY, i.getId(), i.getCount(), manager, true);
                           } else {
                              totalPrice -= price;
                           }
                        }

                        if (totalPrice > 0L) {
                           castle.addToTreasuryNoTax(totalPrice);
                           SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_ADENA_DISAPPEARED);
                           sm.addLong(totalPrice);
                           player.sendPacket(sm);
                           if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
                              manor.updateCurrentProduction(this._manorId, productInfo.values());
                           }
                        }
                     } else {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                     }
                  } else {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }
}
