package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.ZoneBuildManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMoveRequest;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class MoveToLocation extends ClientPacket {
   private int _targetX;
   private int _targetY;
   private int _targetZ;
   private int _originX;
   private int _originY;
   private int _originZ;
   private int _movementMode;

   @Override
   protected void readImpl() {
      this._targetX = this.readInt();
      this._targetY = this.readInt();
      this._targetZ = this.readInt();
      this._originX = this.readInt();
      this._originY = this.readInt();
      this._originZ = this.readInt();
      this._movementMode = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isOverloaded()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_MOVE_YOU_ARE_TOO_ENCUMBERED);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME > 0 && !player.isGM() && player.getNotMoveUntil() > System.currentTimeMillis()) {
            player.sendMessage("You cannot move while speaking to an NPC. One moment please.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (this._targetX == this._originX && this._targetY == this._originY && this._targetZ == this._originZ) {
            player.stopMove(player.getLocation());
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (GeoEngine.getInstance().isCompletelyBlocked(GeoEngine.getGeoX(this._targetX), GeoEngine.getGeoY(this._targetY), this._targetZ)) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (DoorData.getInstance().checkIfDoorsBetween(player.getLastServerPosition(), player.getLocation(), player.getInstanceId())) {
            player.stopMove(player.getLastServerPosition());
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            if (this._movementMode == 1) {
               player.setCursorKeyMovement(false);
               if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MOVE_REQUEST, player)) {
                  TerminateReturn terminate = (TerminateReturn)EventDispatcher.getInstance()
                     .notifyEvent(new OnPlayerMoveRequest(player, new Location(this._targetX, this._targetY, this._targetZ)), player, TerminateReturn.class);
                  if (terminate != null && terminate.terminate()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }
            } else {
               if (!PlayerConfig.ENABLE_KEYBOARD_MOVEMENT && !player.inObserverMode()) {
                  return;
               }

               int dx = this._originX - player.getX();
               int dy = this._originY - player.getY();
               if (dx * dx + dy * dy < 1000000) {
                  player.setSyncedXYZ(this._originX, this._originY, this._originZ);
               }

               player.setCursorKeyMovement(true);
               player.setLastServerPosition(player.getX(), player.getY(), player.getZ());
            }

            int teleMode = player.getTeleMode();
            if (teleMode > 0) {
               if (teleMode == 3) {
                  ZoneBuildManager.getInstance().addPoint(player, new Location(this._targetX, this._targetY, this._targetZ));
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  if (teleMode == 1) {
                     player.setTeleMode(0);
                  }

                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  player.teleToLocation(new Location(this._targetX, this._targetY, this._targetZ));
               }
            } else if (player.isOutOfControl()) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               double dx = this._targetX - player.getX();
               double dy = this._targetY - player.getY();
               if (dx * dx + dy * dy > 9.801E7) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  player.getAI().setIntentionMoveTo(new Location(this._targetX, this._targetY, this._targetZ));
                  player.onActionRequest();
               }
            }
         }
      }
   }
}
