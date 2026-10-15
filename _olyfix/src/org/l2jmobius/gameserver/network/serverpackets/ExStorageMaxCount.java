package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExStorageMaxCount extends ServerPacket {
   private final int _inventory;
   private final int _warehouse;
   private final int _clan;
   private final int _privateSell;
   private final int _privateBuy;
   private final int _receipeD;
   private final int _recipe;
   private final int _inventoryExtraSlots;

   public ExStorageMaxCount(Player player) {
      this._inventory = player.getInventoryLimit();
      this._warehouse = player.getWareHouseLimit();
      this._privateSell = player.getPrivateSellStoreLimit();
      this._privateBuy = player.getPrivateBuyStoreLimit();
      this._clan = PlayerConfig.WAREHOUSE_SLOTS_CLAN;
      this._receipeD = player.getDwarfRecipeLimit();
      this._recipe = player.getCommonRecipeLimit();
      this._inventoryExtraSlots = (int)player.getStat().calcStat(Stat.INV_LIM, 0.0, null, null);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_STORAGE_MAX_COUNT.writeId(this, buffer);
      buffer.writeInt(this._inventory);
      buffer.writeInt(this._warehouse);
      buffer.writeInt(this._clan);
      buffer.writeInt(this._privateSell);
      buffer.writeInt(this._privateBuy);
      buffer.writeInt(this._receipeD);
      buffer.writeInt(this._recipe);
      buffer.writeInt(this._inventoryExtraSlots);
   }
}
