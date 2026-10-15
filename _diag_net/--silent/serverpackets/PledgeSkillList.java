package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class PledgeSkillList extends ServerPacket {
   private final Collection<Skill> _skills;
   private final Collection<PledgeSkillList.SubPledgeSkill> _subSkills;

   public PledgeSkillList(Clan clan) {
      this._skills = clan.getAllSkills();
      this._subSkills = clan.getAllSubSkills();
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_SKILL_LIST.writeId(this, buffer);
      buffer.writeInt(this._skills.size());
      buffer.writeInt(this._subSkills.size());

      for (Skill sk : this._skills) {
         buffer.writeInt(sk.getDisplayId());
         buffer.writeInt(sk.getDisplayLevel());
      }

      for (PledgeSkillList.SubPledgeSkill sk : this._subSkills) {
         buffer.writeInt(sk._subType);
         buffer.writeInt(sk._skillId);
         buffer.writeInt(sk._skillLevel);
      }
   }

   public static class SubPledgeSkill {
      int _subType;
      int _skillId;
      int _skillLevel;

      public SubPledgeSkill(int subType, int skillId, int skillLevel) {
         this._subType = subType;
         this._skillId = skillId;
         this._skillLevel = skillLevel;
      }
   }
}
