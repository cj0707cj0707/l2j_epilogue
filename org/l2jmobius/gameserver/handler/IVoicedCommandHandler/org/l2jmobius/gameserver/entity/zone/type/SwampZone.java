package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

public class SwampZone extends ZoneType {
   private double _move_bonus = 0.5;
   private int _castleId = 0;
   private Castle _castle = null;
   private int _eventId = 0;

   public SwampZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("move_bonus")) {
         this._move_bonus = Double.parseDouble(value);
      } else if (name.equals("castleId")) {
         this._castleId = Integer.parseInt(value);
      } else if (name.equals("eventId")) {
         this._eventId = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   private Castle getCastle() {
      if (this._castleId > 0 && this._castle == null) {
         this._castle = CastleManager.getInstance().getCastleById(this._castleId);
      }

      return this._castle;
   }

   @Override
   protected void onEnter(Creature creature) {
      if (this.getCastle() != null) {
         if (!this.getCastle().getSiege().isInProgress()) {
            return;
         }

         Player player = creature.asPlayer();
         if (player != null && player.isInSiege() && player.getSiegeState() == 2) {
            return;
         }
      }

      creature.setInsideZone(ZoneId.SWAMP, true);
      if (creature.isPlayer()) {
         if (this._eventId > 0) {
            creature.sendPacket(new OnEventTrigger(this._eventId, true));
         }

         creature.asPlayer().broadcastUserInfo();
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isInsideZone(ZoneId.SWAMP)) {
         creature.setInsideZone(ZoneId.SWAMP, false);
         if (creature.isPlayer()) {
            if (this._eventId > 0) {
               creature.sendPacket(new OnEventTrigger(this._eventId, false));
            }

            if (!creature.isTeleporting()) {
               creature.asPlayer().broadcastUserInfo();
            }
         }
      }
   }

   public double getMoveBonus() {
      return this._move_bonus;
   }
}
