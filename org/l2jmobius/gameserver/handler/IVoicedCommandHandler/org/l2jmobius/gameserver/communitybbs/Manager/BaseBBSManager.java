package org.l2jmobius.gameserver.communitybbs.Manager;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

public abstract class BaseBBSManager {
   public abstract void parsecmd(String var1, Player var2);

   public abstract void parsewrite(String var1, String var2, String var3, String var4, String var5, Player var6);

   protected void send1001(String html, Player acha) {
      if (html.length() < 8192) {
         acha.sendPacket(new ShowBoard(html, "1001"));
      }
   }

   protected void send1002(Player acha) {
      this.send1002(acha, " ", " ", "0");
   }

   protected void send1002(Player player, String string, String string2, String string3) {
      List<String> arg = new ArrayList<>(20);
      arg.add("0");
      arg.add("0");
      arg.add("0");
      arg.add("0");
      arg.add("0");
      arg.add("0");
      arg.add(player.getName());
      arg.add(Integer.toString(player.getObjectId()));
      arg.add(player.getAccountName());
      arg.add("9");
      arg.add(string2);
      arg.add(string2);
      arg.add(string);
      arg.add(string3);
      arg.add(string3);
      arg.add("0");
      arg.add("0");
      player.sendPacket(new ShowBoard(arg));
   }
}
