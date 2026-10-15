package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class Die extends ServerPacket {
   private final int _objectId;
   private final boolean _canTeleport;
   private final boolean _sweepable;
   private AccessLevel _access = AdminData.getInstance().getAccessLevel(0);
   private Clan _clan;
   private final Creature _creature;
   private boolean _isJailed;
   private boolean _staticRes = false;

   public Die(Creature creature) {
      this._objectId = creature.getObjectId();
      this._creature = creature;
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         this._access = player.getAccessLevel();
         this._clan = player.getClan();
         this._isJailed = player.isJailed();
      }

      this._canTeleport = creature.canRevive() && !creature.isPendingRevive();
      this._sweepable = creature.isSweepActive();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.DIE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._canTeleport);
      if (this._creature.isPlayer()) {
         Player player = this._creature.asPlayer();
         if (!Olympiad.getInstance().isRegistered(player) && !player.isOnEvent()) {
            this._staticRes = this._creature.getInventory().haveItemForSelfResurrection();
         }

         if (this._access.allowFixedRes()) {
            this._staticRes = true;
         }
      }

      if (this._canTeleport && this._clan != null && !this._isJailed) {
         boolean isInCastleDefense = false;
         boolean isInFortDefense = false;
         SiegeClan siegeClan = null;
         Castle castle = CastleManager.getInstance().getCastle(this._creature);
         Fort fort = FortManager.getInstance().getFort(this._creature);
         SiegableHall hall = CHSiegeManager.getInstance().getNearbyClanHall(this._creature);
         if (castle != null && castle.getSiege().isInProgress()) {
            siegeClan = castle.getSiege().getAttackerClan(this._clan);
            if (siegeClan == null && castle.getSiege().checkIsDefender(this._clan)) {
               isInCastleDefense = true;
            }
         } else if (fort != null && fort.getSiege().isInProgress()) {
            siegeClan = fort.getSiege().getAttackerClan(this._clan);
            if (siegeClan == null && fort.getSiege().checkIsDefender(this._clan)) {
               isInFortDefense = true;
            }
         }

         buffer.writeInt(this._clan.getHideoutId() > 0);
         buffer.writeInt(this._clan.getCastleId() > 0 || isInCastleDefense);
         buffer.writeInt(
            TerritoryWarManager.getInstance().getHQForClan(this._clan) != null
               || siegeClan != null && !isInCastleDefense && !isInFortDefense && !siegeClan.getFlag().isEmpty()
               || hall != null && hall.getSiege() != null && hall.getSiege().checkIsAttacker(this._clan)
         );
         buffer.writeInt(this._sweepable);
         buffer.writeInt(this._staticRes);
         buffer.writeInt(this._clan.getFortId() > 0 || isInFortDefense);
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(this._sweepable);
         buffer.writeInt(this._staticRes);
         buffer.writeInt(0);
      }
   }
}
