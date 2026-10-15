package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class RequestRecipeShopListSet extends ClientPacket {
   private static final int BATCH_LENGTH = 12;
   private ManufactureItem[] _items = null;

   @Override
   protected void readImpl() {
      int count = this.readInt();
      if (count > 0 && count <= PlayerConfig.MAX_ITEM_IN_PACKET && count * 12 == this.remaining()) {
         this._items = new ManufactureItem[count];

         for (int i = 0; i < count; i++) {
            int id = this.readInt();
            long cost = this.readLong();
            if (cost < 0L) {
               this._items = null;
               return;
            }

            this._items[i] = new ManufactureItem(id, cost);
         }
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._items == null) {
            player.setPrivateStoreType(PrivateStoreType.NONE);
            player.broadcastUserInfo();
         } else if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !player.isInDuel()) {
            if (player.isInsideZone(ZoneId.NO_STORE)) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_WORKSHOP_HERE);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!player.canOpenPrivateStore()) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               player.getManufactureItems().clear();

               for (ManufactureItem i : this._items) {
                  RecipeList list = RecipeData.getInstance().getRecipeList(i.getRecipeId());
                  if (!player.getDwarvenRecipeBook().contains(list) && !player.getCommonRecipeBook().contains(list)) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        "Warning!! " + player + " of account " + player.getAccountName() + " tried to set recipe which he does not have.",
                        GeneralConfig.DEFAULT_PUNISH
                     );
                     return;
                  }

                  if (i.getCost() > Inventory.MAX_ADENA) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        "Warning!! "
                           + player
                           + " of account "
                           + player.getAccountName()
                           + " tried to set price more than "
                           + Inventory.MAX_ADENA
                           + " adena in Private Manufacture.",
                        GeneralConfig.DEFAULT_PUNISH
                     );
                     return;
                  }

                  player.getManufactureItems().put(i.getRecipeId(), i);
               }

               player.setStoreName(!player.hasManufactureShop() ? "" : player.getStoreName());
               player.setPrivateStoreType(PrivateStoreType.MANUFACTURE);
               player.sitDown();
               player.broadcastUserInfo();
               World.broadcastToSelfAndVisiblePlayers(player, new RecipeShopMsg(player));
            }
         } else {
            player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }
}
