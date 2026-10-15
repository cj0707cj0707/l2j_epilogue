package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class AbnormalStatusUpdate extends ServerPacket {
   private final List<BuffInfo> _effects = new ArrayList<>();

   public void addSkill(BuffInfo info) {
      if (!info.getSkill().isHealingPotionSkill()) {
         this._effects.add(info);
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.ABNORMAL_STATUS_UPDATE.writeId(this, buffer);
      buffer.writeShort(this._effects.size());

      for (BuffInfo info : this._effects) {
         if (info != null && info.isInUse()) {
            Skill skill = info.getSkill();
            buffer.writeInt(skill.getDisplayId());
            buffer.writeShort(skill.getDisplayLevel());
            buffer.writeInt(info.getTime());
         }
      }
   }
}
