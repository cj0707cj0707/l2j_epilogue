package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.games.BlockCheckerManager;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameChangePoints;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameExtendedChangePoints;

public class Block extends Monster {
   private int _colorEffect;

   public Block(NpcTemplate template) {
      super(template);
   }

   public void changeColor(Player attacker, ArenaParticipantsHolder holder, int team) {
      synchronized (this) {
         BlockCheckerManager event = holder.getEvent();
         if (this._colorEffect == 83) {
            this._colorEffect = 0;
            this.broadcastPacket(new AbstractNpcInfo.NpcInfo(this, attacker));
            this.increaseTeamPointsAndSend(attacker, team, event);
         } else {
            this._colorEffect = 83;
            this.broadcastPacket(new AbstractNpcInfo.NpcInfo(this, attacker));
            this.increaseTeamPointsAndSend(attacker, team, event);
         }

         int random = Rnd.get(100);
         if (random > 69 && random <= 84) {
            this.dropItem(13787, event, attacker);
         } else if (random > 84) {
            this.dropItem(13788, event, attacker);
         }
      }
   }

   public void setRed(boolean isRed) {
      this._colorEffect = isRed ? 83 : 0;
   }

   @Override
   public int getColorEffect() {
      return this._colorEffect;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return !attacker.isPlayer() ? true : attacker.isPlayer() && attacker.asPlayer().getBlockCheckerArena() > -1;
   }

   @Override
   public boolean doDie(Creature killer) {
      return false;
   }

   @Override
   public void onAction(Player player, boolean interact) {
      if (this.canTarget(player)) {
         player.setLastFolkNPC(this);
         if (player.getTarget() != this) {
            player.setTarget(this);
            this.getAI();
         } else if (interact) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }

   private void increaseTeamPointsAndSend(Player player, int team, BlockCheckerManager eng) {
      eng.increasePlayerPoints(player, team);
      int timeLeft = (int)((eng.getStarterTime() - System.currentTimeMillis()) / 1000L);
      boolean isRed = eng.getHolder().getRedPlayers().contains(player);
      ExCubeGameChangePoints changePoints = new ExCubeGameChangePoints(timeLeft, eng.getBluePoints(), eng.getRedPoints());
      ExCubeGameExtendedChangePoints secretPoints = new ExCubeGameExtendedChangePoints(
         timeLeft, eng.getBluePoints(), eng.getRedPoints(), isRed, player, eng.getPlayerPoints(player, isRed)
      );
      eng.getHolder().broadCastPacketToTeam(changePoints);
      eng.getHolder().broadCastPacketToTeam(secretPoints);
   }

   private void dropItem(int id, BlockCheckerManager eng, Player player) {
      Item drop = ItemManager.createItem(ItemProcessType.LOOT, id, 1L, player, this);
      int x = this.getX() + Rnd.get(50);
      int y = this.getY() + Rnd.get(50);
      int z = this.getZ();
      drop.dropMe(this, x, y, z);
      eng.addNewDrop(drop);
   }
}
