package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRetire;

public class KrateisMatchManager extends Folk {
   private int _arenaId = 0;

   public KrateisMatchManager(NpcTemplate template) {
      super(template);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      KrateiArena arena = org.l2jmobius.gameserver.managers.games.KrateisCubeManager.getInstance().getArenaId(this.getArenaId());
      if (command.startsWith("TeleToArena")) {
         if (arena == null || !arena.isRegisterPlayer(player)) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
            player.teleToLocation(-70381, -70937, -1428, 0, true);
         } else if (arena.isActiveNow()) {
            player.teleToLocation(arena.getBattleLoc().get(Rnd.get(arena.getBattleLoc().size())), true);
            arena.addEffects(player);
         } else {
            this.showChatWindow(player, "data/html/krateisCube/" + this.getId() + "-01.htm");
         }
      } else if (command.startsWith("TeleFromArena")) {
         if (arena != null && arena.isRegisterPlayer(player)) {
            arena.removePlayer(player);
            player.stopAllEffects();
            player.sendPacket(ExPVPMatchCCRetire.STATIC);
            player.broadcastStatusUpdate();
            player.broadcastUserInfo();
            Summon pet = player.getSummon();
            if (pet != null) {
               pet.stopAllEffects();
            }
         }

         player.teleToLocation(-70381, -70937, -1428, 0, true);
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

   public void setArenaId(int id) {
      this._arenaId = id;
   }

   public int getArenaId() {
      return this._arenaId;
   }
}
