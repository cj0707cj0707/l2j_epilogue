package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class KrateisCubeManager extends Folk {
   public KrateisCubeManager(NpcTemplate template) {
      super(template);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("Register")) {
         if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize()) {
            player.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_RECEIVED_BECAUSE_THE_INVENTORY_WEIGHT_QUANTITY_LIMIT_HAS_BEEN_EXCEEDED);
            this.showChatWindow(player, "data/html/krateisCube/32503-08.htm");
            return;
         }

         if (player.getKrateiArena() == null
            && (Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode() || player.isOnEvent() || player.isRegisteredOnEvent())) {
            player.sendPacket(
               SystemMessageId.PVP_MATCHES_SUCH_AS_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEI_S_CUBE_AND_HANDY_S_BLOCK_CHECKERS_OVERLAP_AND_YOU_CANNOT_PARTICIPATE
            );
            return;
         }

         if (player.getParty() != null && player.getParty().getUCState() != null || player.getUCState() > 0) {
            player.sendPacket(
               SystemMessageId.PVP_MATCHES_SUCH_AS_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEI_S_CUBE_AND_HANDY_S_BLOCK_CHECKERS_OVERLAP_AND_YOU_CANNOT_PARTICIPATE
            );
            return;
         }

         if (player.isCursedWeaponEquipped()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_WHILE_POSSESSING_A_CURSED_WEAPON);
            return;
         }

         int id = Integer.parseInt(command.substring(9, 10).trim());
         KrateiArena arena = org.l2jmobius.gameserver.managers.games.KrateisCubeManager.getInstance().getArenaId(id);
         if (arena == null) {
            this.showChatWindow(player, "data/html/krateisCube/32503-09.htm");
            return;
         }

         if (player.getLevel() < arena.getMinLevel() || player.getLevel() > arena.getMaxLevel()) {
            this.showChatWindow(player, "data/html/krateisCube/32503-06.htm");
            return;
         }

         if (org.l2jmobius.gameserver.managers.games.KrateisCubeManager.getInstance().isRegisterTime()) {
            if (arena.addRegisterPlayer(player)) {
               this.showChatWindow(player, "data/html/krateisCube/32503-03.htm");
            } else {
               this.showChatWindow(player, "data/html/krateisCube/32503-04.htm");
            }
         } else {
            this.showChatWindow(player, "data/html/krateisCube/32503-07.htm");
         }
      } else if (command.startsWith("SeeList")) {
         if (player.getLevel() < 70) {
            this.showChatWindow(player, "data/html/krateisCube/32503-09.htm");
         } else {
            this.showChatWindow(player, "data/html/krateisCube/32503-02.htm");
         }
      } else if (command.startsWith("Cancel")) {
         for (KrateiArena arenax : org.l2jmobius.gameserver.managers.games.KrateisCubeManager.getInstance().getArenas().values()) {
            if (arenax != null && arenax.removePlayer(player)) {
               this.showChatWindow(player, "data/html/krateisCube/32503-05.htm");
               break;
            }
         }
      } else if (command.startsWith("TeleportToFI")) {
         player.teleToLocation(-59193, -56893, -2034, true);
         Summon pet = player.getSummon();
         if (pet != null) {
            pet.teleToLocation(-59193, -56893, -2034, true);
         }
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
         pom = npcId + "-0" + val;
      }

      return "data/html/krateisCube/" + pom + ".htm";
   }
}
