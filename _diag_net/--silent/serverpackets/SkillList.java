package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SkillList extends ServerPacket {
   private final List<SkillList.Skill> _skills = new ArrayList<>();

   public void addSkill(int id, int level, boolean passive, boolean disabled, boolean enchanted) {
      this._skills.add(new SkillList.Skill(id, level, passive, disabled, enchanted));
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SKILL_LIST.writeId(this, buffer);
      buffer.writeInt(this._skills.size());

      for (SkillList.Skill temp : this._skills) {
         buffer.writeInt(temp.passive);
         buffer.writeInt(temp.level);
         buffer.writeInt(temp.id);
         buffer.writeByte(temp.disabled);
         buffer.writeByte(temp.enchanted);
      }
   }

   private static class Skill {
      public int id;
      public int level;
      public boolean passive;
      public boolean disabled;
      public boolean enchanted;

      Skill(int pId, int pLevel, boolean pPassive, boolean pDisabled, boolean pEnchanted) {
         this.id = pId;
         this.level = pLevel;
         this.passive = pPassive;
         this.disabled = pDisabled;
         this.enchanted = pEnchanted;
      }
   }
}
