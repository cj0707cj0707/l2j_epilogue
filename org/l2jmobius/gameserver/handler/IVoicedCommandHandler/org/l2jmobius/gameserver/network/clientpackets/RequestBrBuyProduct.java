package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.PrimeShopProductHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.ExBrBuyProduct;
import org.l2jmobius.gameserver.network.serverpackets.ExBrGamePoint;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestBrBuyProduct extends ClientPacket {
   private int _productId;
   private int _count;

   @Override
   protected void readImpl() {
      this._productId = this.readInt();
      this._count = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._count <= 99 && this._count >= 0) {
            PrimeShopProductHolder product = PrimeShopData.getInstance().getProduct(this._productId);
            if (product == null) {
               player.sendPacket(new ExBrBuyProduct(-2));
            } else {
               long totalPoints = product.getPrice() * this._count;
               if (totalPoints < 0L) {
                  player.sendPacket(new ExBrBuyProduct(-2));
               } else {
                  long gamePointSize = GeneralConfig.PRIME_SHOP_ITEM_ID == -1
                     ? player.getGamePoints()
                     : player.getInventory().getInventoryItemCount(GeneralConfig.PRIME_SHOP_ITEM_ID, -1);
                  if (totalPoints > gamePointSize) {
                     player.sendPacket(new ExBrBuyProduct(-1));
                  } else {
                     ItemTemplate item = ItemData.getInstance().getTemplate(product.getItemId());
                     if (item == null) {
                        player.sendPacket(new ExBrBuyProduct(-2));
                     } else {
                        int totalWeight = product.getItemWeight() * product.getItemCount() * this._count;
                        int totalCount = 0;
                        totalCount += item.isStackable() ? 1 : product.getItemCount() * this._count;
                        if (player.getInventory().validateCapacity(totalCount) && player.getInventory().validateWeight(totalWeight)) {
                           if (GeneralConfig.PRIME_SHOP_ITEM_ID == -1) {
                              player.setGamePoints(player.getGamePoints() - totalPoints);
                           } else {
                              player.getInventory().destroyItemByItemId(ItemProcessType.FEE, GeneralConfig.PRIME_SHOP_ITEM_ID, totalPoints, player, null);
                           }

                           player.addItem(ItemProcessType.BUY, product.getItemId(), product.getItemCount() * this._count, player, true);
                           StatusUpdate su = new StatusUpdate(player.getObjectId());
                           su.addAttribute(14, player.getCurrentLoad());
                           player.sendPacket(su);
                           player.sendPacket(new ExBrGamePoint(player));
                           player.sendPacket(new ExBrBuyProduct(1));
                           player.broadcastUserInfo();

                           try (
                              Connection con = DatabaseFactory.getConnection();
                              PreparedStatement statement = con.prepareStatement(
                                 "INSERT INTO prime_shop_transactions (charId, productId, quantity) values (?,?,?)"
                              );
                           ) {
                              statement.setLong(1, player.getObjectId());
                              statement.setInt(2, product.getProductId());
                              statement.setLong(3, this._count);
                              statement.executeUpdate();
                           } catch (Exception var19) {
                              PacketLogger.warning("Could not save Item Mall transaction: " + var19.getMessage());
                           }
                        } else {
                           player.sendPacket(new ExBrBuyProduct(-4));
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
