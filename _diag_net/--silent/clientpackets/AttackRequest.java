package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class AttackRequest extends ClientPacket {
   private int _objectId;
   private int _originX;
   private int _originY;
   private int _originZ;
   private int _attackId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
      this._originX = this.readInt();
      this._originY = this.readInt();
      this._originZ = this.readInt();
      this._attackId = this.readByte();
   }

   @Override
   protected void runImpl() {
      if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canPerformPlayerAction()) {
         Player player = this.getPlayer();
         if (player != null) {
            if (player.isPlayable() && player.isInBoat()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_NOT_ALLOWED_WHILE_RIDING_A_FERRY_OR_BOAT);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
               if (info != null) {
                  for (AbstractEffect effect : info.getEffects()) {
                     if (!effect.checkCondition(-1)) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_YOUR_ACTIVITIES_ARE_ONLY_ALLOWED_WITHIN_LIMITATION
                        );
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }
                  }
               }

               WorldObject target;
               if (player.getTargetId() == this._objectId) {
                  target = player.getTarget();
               } else {
                  target = World.findObject(this._objectId);
               }

               if (target == null) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (!target.isTargetable() && !player.isGM()) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (target.getInstanceId() != player.getInstanceId() && player.getInstanceId() != -1) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (!target.isVisibleFor(player)) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  player.onActionRequest();
                  if (player.getTarget() != target) {
                     target.onAction(player);
                  } else if (target.getObjectId() != player.getObjectId() && !player.isInStoreMode() && player.getActiveRequester() == null) {
                     target.onForcedAttack(player);
                  } else {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  }
               }
            }
         }
      }
   }
}
