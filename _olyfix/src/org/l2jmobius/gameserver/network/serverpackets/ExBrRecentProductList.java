package org.l2jmobius.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.PrimeShopProductHolder;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBrRecentProductList extends ServerPacket {
   private final List<PrimeShopProductHolder> _itemList = new ArrayList<>();

   public ExBrRecentProductList(Player player) {
      int playerObj = player.getObjectId();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT productId FROM prime_shop_transactions WHERE charId=? ORDER BY transactionTime DESC");
      ) {
         statement.setInt(1, playerObj);

         try (ResultSet rset = statement.executeQuery()) {
            while (rset.next()) {
               PrimeShopProductHolder product = PrimeShopData.getInstance().getProduct(rset.getInt("productId"));
               if (product != null && !this._itemList.contains(product)) {
                  this._itemList.add(product);
               }
            }
         }
      } catch (Exception var14) {
         PacketLogger.warning("Could not restore Item Mall transaction: " + var14.getMessage());
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      if (!this._itemList.isEmpty()) {
         ServerPackets.EX_BR_RECENT_PRODUCT_LIST.writeId(this, buffer);
         buffer.writeInt(this._itemList.size());

         for (PrimeShopProductHolder product : this._itemList) {
            buffer.writeInt(product.getProductId());
            buffer.writeShort(product.getCategory());
            buffer.writeInt(product.getPrice());
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
         }
      }
   }
}
