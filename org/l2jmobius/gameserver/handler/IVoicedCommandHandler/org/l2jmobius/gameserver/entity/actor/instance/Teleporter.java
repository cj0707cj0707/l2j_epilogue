package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Teleporter extends Merchant {
   private static final Logger LOGGER = Logger.getLogger(Teleporter.class.getName());
   private static final CommonSkill[] FORBIDDEN_TRANSFORM = new CommonSkill[]{
      CommonSkill.FROG_TRANSFORM, CommonSkill.CHILD_TRANSFORM, CommonSkill.NATIVE_TRANSFORM
   };

   public Teleporter(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Teleporter);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return attacker.isMonster() || super.isAutoAttackable(attacker);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      for (CommonSkill skill : FORBIDDEN_TRANSFORM) {
         if (player.isAffectedBySkill(skill.getId())) {
            this.sendHtmlMessage(player, "data/html/teleporter/epictransformed.htm");
            return;
         }
      }

      StringTokenizer st = new StringTokenizer(command, " ");
      String var10 = st.nextToken();
      switch (var10) {
         case "showNoblesSelect":
            this.sendHtmlMessage(player, "data/html/teleporter/" + (player.isNoble() ? "nobles_select" : "not_nobles") + ".htm");
            break;
         case "showTeleports":
            String listName = st.hasMoreTokens() ? st.nextToken() : TeleportType.NORMAL.name();
            TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), listName);
            if (holder == null) {
               LOGGER.warning(player + " requested show teleports for list with name " + listName + " at NPC " + this.getId() + "!");
               return;
            }

            holder.showTeleportList(player, this);
            break;
         case "teleport":
            if (st.countTokens() != 2) {
               LOGGER.warning(player + " send unhandled teleport command: " + command);
               return;
            }

            String listName = st.nextToken();
            TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), listName);
            if (holder == null) {
               LOGGER.warning(player + " requested unknown teleport list: " + listName + " for npc: " + this.getId() + "!");
               return;
            }

            holder.doTeleport(player, this, this.parseNextInt(st, -1));
            break;
         case "chat":
            int val = 0;

            try {
               val = Integer.parseInt(command.substring(5));
            } catch (NumberFormatException | IndexOutOfBoundsException var8) {
            }

            this.showChatWindow(player, val);
            break;
         default:
            super.onBypassFeedback(player, command);
      }
   }

   private int parseNextInt(StringTokenizer st, int defaultVal) {
      if (st.hasMoreTokens()) {
         String token = st.nextToken();
         if (StringUtil.isNumeric(token)) {
            return Integer.parseInt(token);
         }
      }

      return defaultVal;
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom;
      if (value == 0) {
         pom = String.valueOf(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/teleporter/" + pom + ".htm";
   }

   @Override
   public void showChatWindow(Player player) {
      if (CastleManager.getInstance().getCastle(this) == null) {
         super.showChatWindow(player);
      } else {
         String filename = "data/html/teleporter/castleteleporter-no.htm";
         if (player.getClan() != null && this.getCastle().getOwnerId() == player.getClanId()) {
            filename = this.getHtmlPath(this.getId(), 0);
         } else if (this.getCastle().getSiege().isInProgress()) {
            filename = "data/html/teleporter/castleteleporter-busy.htm";
         }

         this.sendHtmlMessage(player, filename);
      }
   }

   private void sendHtmlMessage(Player player, String filename) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcname%", this.getName());
      player.sendPacket(html);
   }
}
