package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.custom.CustomMailManagerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MessageSenderType;
import org.l2jmobius.gameserver.network.holders.MailMessage;

public class CustomMailManager {
   private static final Logger LOGGER = Logger.getLogger(CustomMailManager.class.getName());
   private static final String READ_SQL = "SELECT * FROM custom_mail";
   private static final String DELETE_SQL = "DELETE FROM custom_mail WHERE date=? AND receiver=?";

   protected CustomMailManager() {
      ThreadPool.scheduleAtFixedRate(() -> {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement ps = con.createStatement();
            ResultSet rs = ps.executeQuery("SELECT * FROM custom_mail");
         ) {
            while (rs.next()) {
               int playerId = rs.getInt("receiver");
               Player player = World.getPlayer(playerId);
               if (player != null && player.isOnline()) {
                  String items = rs.getString("items");
                  MailMessage msg = new MailMessage(playerId, rs.getString("subject"), rs.getString("message"), MessageSenderType.PLAYER);
                  List<ItemEnchantHolder> itemHolders = new ArrayList<>();

                  for (String str : items.split(";")) {
                     if (str.contains(" ")) {
                        String[] split = str.split(" ");
                        String itemId = split[0];
                        String itemCount = split[1];
                        String enchant = split.length > 2 ? split[2] : "0";
                        if (StringUtil.isNumeric(itemId) && StringUtil.isNumeric(itemCount)) {
                           itemHolders.add(new ItemEnchantHolder(Integer.parseInt(itemId), Long.parseLong(itemCount), Integer.parseInt(enchant)));
                        }
                     } else if (StringUtil.isNumeric(str)) {
                        itemHolders.add(new ItemEnchantHolder(Integer.parseInt(str), 1L));
                     }
                  }

                  if (!itemHolders.isEmpty()) {
                     Mail attachments = msg.createAttachments();

                     for (ItemEnchantHolder itemHolder : itemHolders) {
                        Item item = attachments.addItem(ItemProcessType.TRANSFER, itemHolder.getId(), itemHolder.getCount(), null, null);
                        if (itemHolder.getEnchantLevel() > 0) {
                           item.setEnchantLevel(itemHolder.getEnchantLevel());
                        }
                     }
                  }

                  try (PreparedStatement stmt = con.prepareStatement("DELETE FROM custom_mail WHERE date=? AND receiver=?")) {
                     stmt.setString(1, rs.getString("date"));
                     stmt.setInt(2, playerId);
                     stmt.execute();
                  } catch (SQLException var22) {
                     LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error deleting entry from database: ", (Throwable)var22);
                  }

                  MailManager.getInstance().sendMessage(msg);
                  LOGGER.info(this.getClass().getSimpleName() + ": Message sent to " + player.getName() + ".");
               }
            }
         } catch (SQLException var26) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error reading from database: ", (Throwable)var26);
         }
      }, CustomMailManagerConfig.CUSTOM_MAIL_MANAGER_DELAY, CustomMailManagerConfig.CUSTOM_MAIL_MANAGER_DELAY);
      LOGGER.info(this.getClass().getSimpleName() + ": Enabled.");
   }

   public static CustomMailManager getInstance() {
      return CustomMailManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CustomMailManager INSTANCE = new CustomMailManager();
   }
}
