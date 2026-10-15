package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.data.MerchantPriceConfigTable;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.BuyList;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;

public class Merchant extends Folk {
   private MerchantPriceConfigTable.MerchantPriceConfig _mpc;

   public Merchant(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Merchant);
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/merchant/" + pom + ".htm";
   }

   public MerchantPriceConfigTable.MerchantPriceConfig getMpc() {
      return this._mpc;
   }

   public void showBuyWindow(Player player, int value) {
      this.showBuyWindow(player, value, true);
   }

   public void showBuyWindow(Player player, int value, boolean applyTax) {
      BuyListHolder buyList = BuyListData.getInstance().getBuyList(value);
      if (buyList == null) {
         LOGGER.warning("BuyList not found! BuyListId:" + value);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (!buyList.isNpcAllowed(this.getId())) {
         LOGGER.warning("Npc not allowed in BuyList! BuyListId:" + value + " NpcId:" + this.getId());
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         double taxRate = applyTax ? this._mpc.getTotalTaxRate() : 0.0;
         player.setInventoryBlockingStatus(true);
         player.sendPacket(new BuyList(buyList, player.getAdena(), taxRate));
         player.sendPacket(new ExBuySellList(player, buyList, taxRate, false));
         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }
}
