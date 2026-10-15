package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;

public class AccountingFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd MMM H:mm:ss");

   @Override
   public String format(LogRecord record) {
      Object[] params = record.getParameters();
      StringBuilder output = new StringBuilder(30 + record.getMessage().length() + (params == null ? 0 : params.length * 10));
      StringUtil.append(output, "[", _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())), "] ", record.getMessage());
      if (params != null) {
         for (Object p : params) {
            if (p != null) {
               output.append(", ");
               if (p instanceof GameClient client) {
                  String address = null;

                  try {
                     if (!client.isDetached()) {
                        address = client.getIp();
                     }
                  } catch (Exception var11) {
                  }

                  switch (client.getConnectionState()) {
                     case ENTERING:
                     case IN_GAME:
                        if (client.getPlayer() != null) {
                           StringUtil.append(output, client.getPlayer().getName(), "(", String.valueOf(client.getPlayer().getObjectId()), ") ");
                        }
                        break;
                     case AUTHENTICATED:
                        if (client.getAccountName() != null) {
                           StringUtil.append(output, client.getAccountName(), " ");
                        }
                        break;
                     case CONNECTED:
                        if (address != null) {
                           output.append(address);
                        }
                        break;
                     default:
                        throw new IllegalStateException("Missing state on switch");
                  }
               } else if (p instanceof Player player) {
                  StringUtil.append(output, player.getName(), "(", String.valueOf(player.getObjectId()), ")");
               } else {
                  output.append(p.toString());
               }
            }
         }
      }

      output.append(System.lineSeparator());
      return output.toString();
   }
}
