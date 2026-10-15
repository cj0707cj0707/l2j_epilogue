package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestMagicSkillUse extends ClientPacket {
   private int _magicId;
   private boolean _ctrlPressed;
   private boolean _shiftPressed;

   @Override
   protected void readImpl() {
      this._magicId = this.readInt();
      this._ctrlPressed = this.readInt() != 0;
      this._shiftPressed = this.readByte() != 0;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isDead()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            if (player.isFakeDeath()) {
               if (this._magicId != CommonSkill.FAKE_DEATH.getId()) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               if (player.isSkillDisabled(CommonSkill.FAKE_DEATH.getSkill())) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }
            }

            Skill skill = player.getKnownSkill(this._magicId);
            if (skill == null) {
               skill = player.getCustomSkill(this._magicId);
               if (skill == null) {
                  skill = player.getTransformSkill(this._magicId);
                  if (skill == null) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }
            }

            if (player.isPlayable() && player.isInAirShip()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_ACTION_IS_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
               Transform transform = player.getTransformation();
               if (transform != null && !transform.canUseWeaponStats() && !player.hasTransformSkill(skill.getId())) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT
                  || player.getKarma() <= 0
                  || !skill.hasEffectType(EffectType.TELEPORT, new EffectType[0])) {
                  if (!skill.isToggle() || !player.isMounted()) {
                     player.onActionRequest();
                     if (skill.isContinuous() && !skill.isDebuff() && skill.getTargetType() == TargetType.SELF && (!player.isInAirShip() || !player.isInBoat())
                        )
                      {
                        player.getAI().setIntentionMoveTo(player.getLocation());
                     }

                     player.useMagic(skill, this._ctrlPressed, this._shiftPressed);
                  }
               }
            }
         }
      }
   }
}
