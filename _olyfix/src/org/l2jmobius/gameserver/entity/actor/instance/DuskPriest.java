package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class DuskPriest extends SignsPriest {
   public DuskPriest(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.DuskPriest);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("Chat")) {
         this.showChatWindow(player);
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   @Override
   public void showChatWindow(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      String filename = "data/html/seven_signs/";
      int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(2);
      int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
      boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
      boolean isCompResultsPeriod = SevenSigns.getInstance().isCompResultsPeriod();
      int recruitPeriod = SevenSigns.getInstance().getCurrentPeriod();
      int compWinner = SevenSigns.getInstance().getCabalHighestScore();

      filename = switch (playerCabal) {
         case 1 -> {
            if (isCompResultsPeriod) {
               yield filename + "dusk_priest_5.htm";
            } else if (recruitPeriod == 0) {
               yield filename + "dusk_priest_6.htm";
            } else if (isSealValidationPeriod) {
               if (compWinner == 1) {
                  if (compWinner != sealGnosisOwner) {
                     yield filename + "dusk_priest_2c.htm";
                  } else {
                     yield filename + "dusk_priest_2a.htm";
                  }
               } else if (compWinner == 0) {
                  yield filename + "dusk_priest_2d.htm";
               } else {
                  yield filename + "dusk_priest_2b.htm";
               }
            } else {
               yield filename + "dusk_priest_1b.htm";
            }
         }
         case 2 -> {
            if (isSealValidationPeriod) {
               yield filename + "dusk_priest_3a.htm";
            } else {
               yield filename + "dusk_priest_3b.htm";
            }
         }
         default -> {
            if (isCompResultsPeriod) {
               yield filename + "dusk_priest_5.htm";
            } else if (recruitPeriod == 0) {
               yield filename + "dusk_priest_6.htm";
            } else if (isSealValidationPeriod) {
               if (compWinner == 1) {
                  yield filename + "dusk_priest_4.htm";
               } else if (compWinner == 0) {
                  yield filename + "dusk_priest_2d.htm";
               } else {
                  yield filename + "dusk_priest_2b.htm";
               }
            } else {
               yield filename + "dusk_priest_1a.htm";
            }
         }
      };
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      player.sendPacket(html);
   }
}
