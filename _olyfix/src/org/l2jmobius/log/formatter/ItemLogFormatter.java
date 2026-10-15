package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class ItemLogFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd MMM H:mm:ss");

   @Override
   public String format(LogRecord record) {
      Object[] params = record.getParameters();
      StringBuilder output = new StringBuilder(30 + record.getMessage().length() + (params != null ? params.length * 50 : 0));
      StringUtil.append(output, "[", _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())), "] ", record.getMessage());
      if (params != null) {
         for (Object p : params) {
            if (p != null) {
               output.append(", ");
               if (p instanceof Item item) {
                  StringUtil.append(output, "item ", String.valueOf(item.getObjectId()), ":");
                  if (item.isEnchanted()) {
                     StringUtil.append(output, "+", String.valueOf(item.getEnchantLevel()), " ");
                  }

                  StringUtil.append(output, item.getTemplate().getName(), "(", String.valueOf(item.getCount()), ")");
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
