package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationInAirShip;
import org.l2jmobius.gameserver.network.serverpackets.StopMoveInVehicle;

public class MoveToLocationInAirShip extends ClientPacket {
   private int _shipId;
   private int _targetX;
   private int _targetY;
   private int _targetZ;
   private int _originX;
   private int _originY;
   private int _originZ;

   @Override
   protected void readImpl() {
      this._shipId = this.readInt();
      this._targetX = this.readInt();
      this._targetY = this.readInt();
      this._targetZ = this.readInt();
      this._originX = this.readInt();
      this._originY = this.readInt();
      this._originZ = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._targetX == this._originX && this._targetY == this._originY && this._targetZ == this._originZ) {
            player.sendPacket(new StopMoveInVehicle(player, this._shipId));
         } else if (player.isAttackingNow() && player.getActiveWeaponItem() != null && player.getActiveWeaponItem().getItemType() == WeaponType.BOW) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (player.isSitting() || player.isMovementDisabled()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (!player.isInAirShip()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            AirShip airShip = player.getAirShip();
            if (airShip.getObjectId() != this._shipId) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               player.setInVehiclePosition(new Location(this._targetX, this._targetY, this._targetZ));
               player.broadcastPacket(new ExMoveToLocationInAirShip(player));
            }
         }
      }
   }
}
