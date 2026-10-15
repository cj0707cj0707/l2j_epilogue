package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GMViewSkillInfo extends ServerPacket {
   private final Player _player;
   private final Collection<Skill> _skills;

   public GMViewSkillInfo(Player player) {
      this._player = player;
      this._skills = this._player.getAllSkills();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.GM_VIEW_SKILL_INFO.writeId(this, buffer);
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._skills.size());
      boolean isDisabled = this._player.getClan() != null ? this._player.getClan().getReputationScore() < 0 : false;

      for (Skill skill : this._skills) {
         buffer.writeInt(skill.isPassive());
         buffer.writeInt(skill.getDisplayLevel());
         buffer.writeInt(skill.getDisplayId());
         buffer.writeByte(isDisabled && skill.isClanSkill());
         buffer.writeByte(SkillData.getInstance().isEnchantable(skill.getDisplayId()));
      }
   }
}
