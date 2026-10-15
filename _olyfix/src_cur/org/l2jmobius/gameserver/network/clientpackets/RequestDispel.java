package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

public class RequestDispel extends ClientPacket {
   private int _skillId;
   private int _skillLevel;

   @Override
   protected void readImpl() {
      this._skillId = this.readInt();
      this._skillLevel = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (this._skillId > 0 && this._skillLevel > 0) {
         Player player = this.getPlayer();
         if (player != null) {
            Skill skill = SkillData.getInstance().getSkill(this._skillId, this._skillLevel);
            if (skill != null) {
               if (skill.canBeDispeled() && !skill.isStayAfterDeath() && !skill.isDebuff()) {
                  if (skill.getAbnormalType() != AbnormalType.TRANSFORM) {
                     if (!skill.isDance() || PlayerConfig.DANCE_CANCEL_BUFF) {
                        player.stopSkillEffects(SkillFinishType.REMOVED, this._skillId);
                     }
                  }
               }
            }
         }
      }
   }
}
