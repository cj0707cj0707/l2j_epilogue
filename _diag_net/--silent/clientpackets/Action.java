package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class Action extends ClientPacket {
   private int _objectId;
   private int _originX;
   private int _originY;
   private int _originZ;
   private int _actionId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._originX = this.readInt();
      this._originY = this.readInt();
      this._originZ = this.readInt();
      this._actionId = this.readByte();
   }

   @Override
   protected void runImpl() {
      if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformPlayerAction()) {
         Player player = this.getPlayer();
         if (player != null) {
            if (player.inObserverMode()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
               if (info != null) {
                  for (AbstractEffect effect : info.getEffects()) {
                     if (!effect.checkCondition(-4)) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_YOUR_ACTIVITIES_ARE_ONLY_ALLOWED_WITHIN_LIMITATION
                        );
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }
                  }
               }

               WorldObject obj;
               if (player.getTargetId() == this._objectId) {
                  obj = player.getTarget();
               } else if (player.isInAirShip() && player.getAirShip().getHelmObjectId() == this._objectId) {
                  obj = player.getAirShip();
               } else {
                  obj = World.findObject(this._objectId);
               }

               if (obj == null) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (!obj.isTargetable() && !player.isGM()) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (obj.getInstanceId() != player.getInstanceId() && player.getInstanceId() != -1) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (!obj.isVisibleFor(player)) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (player.getActiveRequester() != null) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  player.onActionRequest();
                  switch (this._actionId) {
                     case 0:
                        obj.onAction(player);
                        break;
                     case 1:
                        if (player.isGM() || obj.isNpc() && NpcConfig.ALT_GAME_VIEWNPC && !obj.isFakePlayer()) {
                           obj.onActionShift(player);
                        } else {
                           obj.onAction(player, false);
                        }
                        break;
                     default:
                        org.l2jmobius.gameserver.network.PacketLogger.warning(
                           "[C] Action: Character: " + player.getName() + " requested invalid action: " + this._actionId
                        );
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }
}
