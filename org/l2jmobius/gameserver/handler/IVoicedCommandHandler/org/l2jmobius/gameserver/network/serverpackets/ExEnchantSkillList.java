package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.EnchantSkillType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExEnchantSkillList extends ServerPacket {
   private final EnchantSkillType _type;
   private final List<Skill> _skills = new ArrayList<>();

   public ExEnchantSkillList(EnchantSkillType type) {
      this._type = type;
   }

   public void addSkill(Skill skill) {
      this._skills.add(skill);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_ENCHANT_SKILL_LIST.writeId(this, buffer);
      buffer.writeInt(this._type.ordinal());
      buffer.writeInt(this._skills.size());

      for (Skill skill : this._skills) {
         buffer.writeInt(skill.getId());
         buffer.writeInt(skill.getLevel());
      }
   }
}
