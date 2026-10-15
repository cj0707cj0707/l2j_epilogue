package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;

public class RequestExEnchantSkillInfoDetail extends ClientPacket {
   private int _type;
   private int _skillId;
   private int _skillLevel;

   @Override
   protected void readImpl() {
      this._type = this.readInt();
      this._skillId = this.readInt();
      this._skillLevel = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (this._skillId > 0 && this._skillLevel > 0) {
         Player player = this.getPlayer();
         if (player != null) {
            int reqskillLevel = -2;
            if (this._type == 0 || this._type == 1) {
               reqskillLevel = this._skillLevel - 1;
            } else if (this._type == 2) {
               reqskillLevel = this._skillLevel + 1;
            } else if (this._type == 3) {
               reqskillLevel = this._skillLevel;
            }

            int playerskillLevel = player.getSkillLevel(this._skillId);
            if (playerskillLevel != 0) {
               if (reqskillLevel % 100 == 0) {
                  EnchantSkillLearn esl = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._skillId);
                  if (esl == null) {
                     return;
                  }

                  if (playerskillLevel != esl.getBaseLevel()) {
                     return;
                  }
               } else if (playerskillLevel != reqskillLevel && this._type == 3 && playerskillLevel % 100 != this._skillLevel % 100) {
                  return;
               }

               player.sendPacket(new ExEnchantSkillInfoDetail(this._type, this._skillId, this._skillLevel, player));
            }
         }
      }
   }
}
