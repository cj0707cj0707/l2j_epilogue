package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class EnchantFormatter extends Formatter {
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
               if (p instanceof Player player) {
                  StringUtil.append(output, "Character:", player.getName(), " [", String.valueOf(player.getObjectId()), "] Account:", player.getAccountName());
                  if (player.getClient() != null && !player.getClient().isDetached()) {
                     StringUtil.append(output, " IP:", player.getClient().getIp());
                  }
               } else if (p instanceof Item item) {
                  if (item.isEnchanted()) {
                     StringUtil.append(output, "+", String.valueOf(item.getEnchantLevel()), " ");
                  }

                  StringUtil.append(
                     output, item.getTemplate().getName(), "(", String.valueOf(item.getCount()), ")", " [", String.valueOf(item.getObjectId()), "]"
                  );
               } else if (p instanceof Skill skill) {
                  if (skill.getLevel() > 100) {
                     StringUtil.append(output, "+", String.valueOf(skill.getLevel() % 100), " ");
                  }

                  StringUtil.append(output, skill.getName(), "(", String.valueOf(skill.getId()), " ", String.valueOf(skill.getLevel()), ")");
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
