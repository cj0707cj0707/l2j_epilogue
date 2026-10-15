package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;

public class AcquireSkillList extends ServerPacket {
   private final List<AcquireSkillList.Skill> _skills;
   private final AcquireSkillType _skillType;

   public AcquireSkillList(AcquireSkillType type) {
      this._skillType = type;
      this._skills = new ArrayList<>();
   }

   public void addSkill(int id, int nextLevel, int maxLevel, int spCost, int requirements) {
      this._skills.add(new AcquireSkillList.Skill(id, nextLevel, maxLevel, spCost, requirements));
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      if (!this._skills.isEmpty()) {
         org.l2jmobius.gameserver.network.ServerPackets.ACQUIRE_SKILL_LIST.writeId(this, buffer);
         buffer.writeInt(this._skillType.ordinal());
         buffer.writeInt(this._skills.size());

         for (AcquireSkillList.Skill temp : this._skills) {
            buffer.writeInt(temp.id);
            buffer.writeInt(temp.nextLevel);
            buffer.writeInt(temp.maxLevel);
            buffer.writeInt(temp.spCost);
            buffer.writeInt(temp.requirements);
            if (this._skillType == AcquireSkillType.SUBPLEDGE) {
               buffer.writeInt(0);
            }
         }
      }
   }

   private static class Skill {
      public int id;
      public int nextLevel;
      public int maxLevel;
      public int spCost;
      public int requirements;

      public Skill(int pId, int pNextLevel, int pMaxLevel, int pSpCost, int pRequirements) {
         this.id = pId;
         this.nextLevel = pNextLevel;
         this.maxLevel = pMaxLevel;
         this.spCost = pSpCost;
         this.requirements = pRequirements;
      }
   }
}
