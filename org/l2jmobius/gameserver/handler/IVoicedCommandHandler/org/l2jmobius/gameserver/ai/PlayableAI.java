package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public abstract class PlayableAI extends CreatureAI {
   protected PlayableAI(Playable playable) {
      super(playable);
   }

   @Override
   public void setIntentionAttack(WorldObject target) {
      if (target != null && target.isPlayable()) {
         Player player = this._actor.asPlayer();
         Player targetPlayer = target.asPlayer();
         if (player != null && targetPlayer != null) {
            if (targetPlayer.isProtectionBlessingAffected()
               && player.getLevel() - targetPlayer.getLevel() >= 10
               && player.getKarma() > 0
               && !target.isInsideZone(ZoneId.PVP)) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }

            if (player.isProtectionBlessingAffected()
               && targetPlayer.getLevel() - player.getLevel() >= 10
               && targetPlayer.getKarma() > 0
               && !target.isInsideZone(ZoneId.PVP)) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }

            if (targetPlayer.isCursedWeaponEquipped() && player.getLevel() <= 20) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }

            if (player.isCursedWeaponEquipped() && targetPlayer.getLevel() <= 20) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }
         }
      }

      super.setIntentionAttack(target);
   }

   @Override
   public void setIntentionCast(Skill skill, WorldObject target) {
      if (target != null && target.isPlayable() && skill != null && skill.hasNegativeEffect()) {
         Player player = this._actor.asPlayer();
         Player targetPlayer = target.asPlayer();
         if (player != null && targetPlayer != null) {
            if (targetPlayer.isProtectionBlessingAffected()
               && player.getLevel() - targetPlayer.getLevel() >= 10
               && player.getKarma() > 0
               && !target.isInsideZone(ZoneId.PVP)) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }

            if (player.isProtectionBlessingAffected()
               && targetPlayer.getLevel() - player.getLevel() >= 10
               && targetPlayer.getKarma() > 0
               && !target.isInsideZone(ZoneId.PVP)) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }

            if (targetPlayer.isCursedWeaponEquipped() && (player.getLevel() <= 20 || targetPlayer.getLevel() <= 20)) {
               player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               this.clientActionFailed();
               return;
            }
         }
      }

      super.setIntentionCast(skill, target);
   }
}
