package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Objects;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;

public class RequestRestartPoint extends ClientPacket {
   protected int _requestedPointType;
   protected boolean _continuation;

   @Override
   protected void readImpl() {
      this._requestedPointType = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.canRevive()) {
            if (player.isFakeDeath()) {
               player.stopFakeDeath(true);
            } else if (player.isDead()) {
               if (player.isOnEvent()) {
                  for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH)) {
                     if (listener.getOwner() instanceof Event) {
                        ((Event)listener.getOwner()).notifyEvent("ResurrectPlayer", null, player);
                        return;
                     }
                  }
               }

               Castle castle = CastleManager.getInstance().getCastle(player.getX(), player.getY(), player.getZ());
               if (castle != null && castle.getSiege().isInProgress() && player.getClan() != null && castle.getSiege().checkIsAttacker(player.getClan())) {
                  ThreadPool.schedule(new RequestRestartPoint.DeathTask(player), castle.getSiege().getAttackerRespawnDelay());
                  if (castle.getSiege().getAttackerRespawnDelay() > 0) {
                     player.sendMessage("You will be re-spawned in " + castle.getSiege().getAttackerRespawnDelay() / 1000 + " seconds");
                  }
               } else {
                  this.portPlayer(player);
               }
            }
         }
      }
   }

   protected void portPlayer(Player player) {
      Location loc = null;
      Castle castle = null;
      Fort fort = null;
      SiegableHall hall = null;
      boolean isInDefense = false;
      int instanceId = 0;
      if (player.isJailed()) {
         this._requestedPointType = 27;
      } else if (player.isFestivalParticipant()) {
         this._requestedPointType = 5;
      }

      label171: {
         Clan clan = player.getClan();
         switch (this._requestedPointType) {
            case 1:
               if (clan == null || clan.getHideoutId() == 0) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     "Player [" + player.getName() + "] called RestartPointPacket - To Clanhall and he doesn't have Clanhall!"
                  );
                  return;
               }

               loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.CLANHALL);
               if (ClanHallTable.getInstance().getClanHallByOwner(clan) != null && ClanHallTable.getInstance().getClanHallByOwner(clan).getFunction(5) != null) {
                  player.restoreExp(ClanHallTable.getInstance().getClanHallByOwner(clan).getFunction(5).getLevel());
               }
               break;
            case 2:
               castle = CastleManager.getInstance().getCastle(player);
               if (castle != null && castle.getSiege().isInProgress()) {
                  if (castle.getSiege().checkIsDefender(clan)) {
                     loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
                  } else {
                     if (!castle.getSiege().checkIsAttacker(clan)) {
                        org.l2jmobius.gameserver.network.PacketLogger.warning(
                           "Player [" + player.getName() + "] called RestartPointPacket - To Castle and he doesn't have Castle!"
                        );
                        return;
                     }

                     loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
                  }
               } else {
                  if (clan == null || clan.getCastleId() == 0) {
                     return;
                  }

                  loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
               }

               if (CastleManager.getInstance().getCastleByOwner(clan) != null && CastleManager.getInstance().getCastleByOwner(clan).getFunction(4) != null) {
                  player.restoreExp(CastleManager.getInstance().getCastleByOwner(clan).getFunction(4).getLvl());
               }
               break;
            case 3:
               if (clan == null || clan.getFortId() == 0) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     "Player [" + player.getName() + "] called RestartPointPacket - To Fortress and he doesn't have Fortress!"
                  );
                  return;
               }

               loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.FORTRESS);
               if (FortManager.getInstance().getFortByOwner(clan) != null && FortManager.getInstance().getFortByOwner(clan).getFunction(4) != null) {
                  player.restoreExp(FortManager.getInstance().getFortByOwner(clan).getFunction(4).getLevel());
               }
               break;
            case 4:
               SiegeClan siegeClan = null;
               castle = CastleManager.getInstance().getCastle(player);
               fort = FortManager.getInstance().getFort(player);
               hall = CHSiegeManager.getInstance().getNearbyClanHall(player);
               SiegeFlag flag = TerritoryWarManager.getInstance().getHQForClan(clan);
               if (flag == null) {
                  flag = TerritoryWarManager.getInstance().getFlagForClan(clan);
               }

               if (castle != null && castle.getSiege().isInProgress()) {
                  siegeClan = castle.getSiege().getAttackerClan(clan);
               } else if (fort != null && fort.getSiege().isInProgress()) {
                  siegeClan = fort.getSiege().getAttackerClan(clan);
               } else if (hall != null && hall.isInSiege()) {
                  siegeClan = hall.getSiege().getAttackerClan(clan);
               }

               if ((siegeClan == null || siegeClan.getFlag().isEmpty()) && flag == null) {
                  if (hall == null) {
                     break label171;
                  }

                  loc = hall.getSiege().getInnerSpawnLoc(player);
                  if (loc == null) {
                     break label171;
                  }
                  break;
               }

               loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.SIEGEFLAG);
               break;
            case 5:
               if (!player.isGM() && !player.isFestivalParticipant() && !player.getInventory().haveItemForSelfResurrection()) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     "Player [" + player.getName() + "] called RestartPointPacket - Fixed and he isn't festival participant!"
                  );
                  return;
               }

               if (!player.isGM()
                  && !player.destroyItemByItemId(ItemProcessType.FEE, 10649, 1L, player, false)
                  && !player.destroyItemByItemId(ItemProcessType.FEE, 13300, 1L, player, false)
                  && !player.destroyItemByItemId(ItemProcessType.FEE, 13128, 1L, player, false)) {
                  instanceId = player.getInstanceId();
                  loc = new Location(player);
               } else {
                  player.doRevive(100.0);
               }
            case 6:
               break;
            case 27:
               if (!player.isJailed()) {
                  return;
               }

               loc = new Location(-114356, -249645, -2984);
               break;
            default:
               loc = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
         }

         if (loc != null) {
            player.setInstanceId(instanceId);
            player.setIn7sDungeon(false);
            player.setIsPendingRevive(true);
            player.teleToLocation(loc, true);
         }

         return;
      }

      org.l2jmobius.gameserver.network.PacketLogger.warning(
         "Player [" + player.getName() + "] called RestartPointPacket - To Siege HQ and he doesn't have Siege HQ!"
      );
   }

   class DeathTask implements Runnable {
      final Player _player;

      DeathTask(Player player) {
         Objects.requireNonNull(RequestRestartPoint.this);
         super();
         this._player = player;
      }

      @Override
      public void run() {
         RequestRestartPoint.this.portPlayer(this._player);
      }
   }
}
