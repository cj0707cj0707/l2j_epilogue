package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class DungeonGatekeeper extends Npc {
   public DungeonGatekeeper(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.DungeonGatekeeper);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      StringTokenizer st = new StringTokenizer(command, " ");
      String actualCommand = st.nextToken();
      String filename = "data/html/seven_signs/";
      int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(1);
      int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(2);
      int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
      boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
      int compWinner = SevenSigns.getInstance().getCabalHighestScore();
      if (actualCommand.startsWith("necro")) {
         boolean canPort = true;
         if (!FeatureConfig.ALT_SEVENSIGNS_OPEN_NECROPOLIS) {
            if (!isSealValidationPeriod) {
               if (playerCabal == 0) {
                  canPort = false;
               }
            } else if (compWinner != 2 || playerCabal == 2 && sealAvariceOwner == 2) {
               if (compWinner != 1 || playerCabal == 1 && sealAvariceOwner == 1) {
                  if (compWinner == 0 && playerCabal != 0) {
                     canPort = true;
                  } else if (playerCabal == 0) {
                     canPort = false;
                  }
               } else {
                  player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
                  canPort = false;
               }
            } else {
               player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
               canPort = false;
            }
         }

         if (!canPort) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            filename = filename + "necro_no.htm";
            html.setFile(player, filename);
            player.sendPacket(html);
         } else {
            this.doTeleport(player, Integer.parseInt(st.nextToken()));
            player.setIn7sDungeon(true);
         }
      } else if (actualCommand.startsWith("cata")) {
         boolean canPortx = true;
         if (!FeatureConfig.ALT_SEVENSIGNS_OPEN_CATACUMBS) {
            if (!isSealValidationPeriod) {
               if (playerCabal == 0) {
                  canPortx = false;
               }
            } else if (compWinner != 2 || playerCabal == 2 && sealGnosisOwner == 2) {
               if (compWinner != 1 || playerCabal == 1 && sealGnosisOwner == 1) {
                  if (compWinner == 0 && playerCabal != 0) {
                     canPortx = true;
                  } else if (playerCabal == 0) {
                     canPortx = false;
                  }
               } else {
                  player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
                  canPortx = false;
               }
            } else {
               player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
               canPortx = false;
            }
         }

         if (!canPortx) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            filename = filename + "cata_no.htm";
            html.setFile(player, filename);
            player.sendPacket(html);
         } else {
            this.doTeleport(player, Integer.parseInt(st.nextToken()));
            player.setIn7sDungeon(true);
         }
      } else if (actualCommand.startsWith("exit")) {
         this.doTeleport(player, Integer.parseInt(st.nextToken()));
         player.setIn7sDungeon(false);
      } else if (actualCommand.startsWith("goto")) {
         this.doTeleport(player, Integer.parseInt(st.nextToken()));
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   private void doTeleport(Player player, int value) {
      TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), TeleportType.OTHER.name());
      if (holder != null) {
         holder.doTeleport(player, this, value);
      } else {
         LOGGER.warning("No teleport destination with id:" + value);
      }

      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/teleporter/" + pom + ".htm";
   }
}
