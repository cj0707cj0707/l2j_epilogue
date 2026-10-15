package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;

public class RequestExEnchantSkillInfo extends ClientPacket {
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
            if (player.getLevel() >= 76) {
               Skill skill = SkillData.getInstance().getSkill(this._skillId, this._skillLevel);
               if (skill != null && skill.getId() == this._skillId) {
                  if (EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._skillId) != null) {
                     int playerskillLevel = player.getSkillLevel(this._skillId);
                     if (playerskillLevel != -1 && playerskillLevel == this._skillLevel) {
                        player.sendPacket(new ExEnchantSkillInfo(this._skillId, this._skillLevel));
                     }
                  }
               }
            }
         }
      }
   }
}
