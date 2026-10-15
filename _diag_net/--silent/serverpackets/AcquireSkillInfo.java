package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;

public class AcquireSkillInfo extends ServerPacket {
   private final AcquireSkillType _type;
   private final int _id;
   private final int _level;
   private final int _spCost;
   private final List<AcquireSkillInfo.Req> _reqs;

   public AcquireSkillInfo(AcquireSkillType skillType, SkillLearn skillLearn) {
      this._id = skillLearn.getSkillId();
      this._level = skillLearn.getSkillLevel();
      this._spCost = skillLearn.getLevelUpSp();
      this._type = skillType;
      this._reqs = new ArrayList<>();
      if (skillType != AcquireSkillType.PLEDGE || PlayerConfig.LIFE_CRYSTAL_NEEDED) {
         for (ItemHolder item : skillLearn.getRequiredItems()) {
            if (PlayerConfig.DIVINE_SP_BOOK_NEEDED || this._id != CommonSkill.DIVINE_INSPIRATION.getId()) {
               this._reqs.add(new AcquireSkillInfo.Req(99, item.getId(), item.getCount(), 50));
            }
         }
      }
   }

   public AcquireSkillInfo(AcquireSkillType skillType, SkillLearn skillLearn, int sp) {
      this._id = skillLearn.getSkillId();
      this._level = skillLearn.getSkillLevel();
      this._spCost = sp;
      this._type = skillType;
      this._reqs = new ArrayList<>();

      for (ItemHolder item : skillLearn.getRequiredItems()) {
         this._reqs.add(new AcquireSkillInfo.Req(99, item.getId(), item.getCount(), 50));
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ACQUIRE_SKILL_INFO.writeId(this, buffer);
      buffer.writeInt(this._id);
      buffer.writeInt(this._level);
      buffer.writeInt(this._spCost);
      buffer.writeInt(this._type.ordinal());
      buffer.writeInt(this._reqs.size());

      for (AcquireSkillInfo.Req temp : this._reqs) {
         buffer.writeInt(temp.type);
         buffer.writeInt(temp.itemId);
         buffer.writeLong(temp.count);
         buffer.writeInt(temp.unk);
      }
   }

   private static class Req {
      public int itemId;
      public long count;
      public int type;
      public int unk;

      public Req(int pType, int pItemId, long itemCount, int pUnk) {
         this.itemId = pItemId;
         this.type = pType;
         this.count = itemCount;
         this.unk = pUnk;
      }
   }
}
