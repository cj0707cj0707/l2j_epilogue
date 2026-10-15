package org.l2jmobius.gameserver.entity.actor.tasks.player;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PetFeedTask implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(PetFeedTask.class.getName());
   private final Player _player;

   public PetFeedTask(Player player) {
      this._player = player;
   }

   @Override
   public void run() {
      try {
         if (!this._player.isMounted() || this._player.getMountNpcId() == 0 || PetDataTable.getInstance().getPetData(this._player.getMountNpcId()) == null) {
            this._player.stopFeed();
            return;
         }

         if (this._player.getCurrentFeed() <= this._player.getFeedConsume()) {
            this._player.setCurrentFeed(0);
            this._player.stopFeed();
            if (this._player.isFlying() && this._player.getMountType() == MountType.WYVERN) {
               this._player.dismount();
               this._player.teleToLocation(TeleportWhereType.TOWN);
            } else {
               this._player.dismount();
            }

            this._player.sendPacket(SystemMessageId.YOU_ARE_OUT_OF_FEED_MOUNT_STATUS_CANCELED);
            return;
         }

         this._player.setCurrentFeed(this._player.getCurrentFeed() - this._player.getFeedConsume());
         Set<Integer> foodIds = PetDataTable.getInstance().getPetData(this._player.getMountNpcId()).getFood();
         if (foodIds.isEmpty()) {
            return;
         }

         Item food = null;

         for (int id : foodIds) {
            food = this._player.getInventory().getItemByItemId(id);
            if (food != null) {
               break;
            }
         }

         if (food != null && this._player.isHungry()) {
            IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
            if (handler != null) {
               handler.onItemUse(this._player, food, false);
               SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_WAS_HUNGRY_SO_IT_ATE_S1);
               sm.addItemName(food.getId());
               this._player.sendPacket(sm);
            }
         }
      } catch (Exception var5) {
         LOGGER.log(Level.SEVERE, "Mounted Pet [NpcId: " + this._player.getMountNpcId() + "] a feed task error has occurred", (Throwable)var5);
      }
   }
}
