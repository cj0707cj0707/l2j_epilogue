package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class UCHelper extends Folk {
   private static final Location[][] LOCATIONS = new Location[][]{
      {new Location(-84451, -45452, -10728), new Location(-84580, -45587, -10728)},
      {new Location(-86154, -50429, -10728), new Location(-86118, -50624, -10728)},
      {new Location(-82009, -53652, -10728), new Location(-81802, -53665, -10728)},
      {new Location(-77603, -50673, -10728), new Location(-77586, -50503, -10728)},
      {new Location(-79186, -45644, -10728), new Location(-79309, -45561, -10728)}
   };

   public UCHelper(NpcTemplate template) {
      super(template);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      StringTokenizer token = new StringTokenizer(command, " ");
      String actualCommand = token.nextToken();
      if (actualCommand.startsWith("coliseum")) {
         int index = Integer.parseInt(token.nextToken());
         Location[] locs = LOCATIONS[index];
         player.teleToLocation(locs[Rnd.get(locs.length)], true);
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   @Override
   public String getHtmlPath(int npcId, int val) {
      String pom = "";
      if (val == 0) {
         pom = String.valueOf(npcId);
      } else {
         pom = npcId + "-" + val;
      }

      return "data/html/undergroundColiseum/" + pom + ".htm";
   }
}
