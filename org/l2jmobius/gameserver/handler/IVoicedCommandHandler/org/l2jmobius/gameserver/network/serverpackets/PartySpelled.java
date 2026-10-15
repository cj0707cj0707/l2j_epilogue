package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartySpelled extends ServerPacket {
   private final List<BuffInfo> _effects = new ArrayList<>();
   private final Creature _creature;

   public PartySpelled(Creature creature) {
      this._creature = creature;
   }

   public void addSkill(BuffInfo info) {
      this._effects.add(info);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PARTY_SPELLED.writeId(this, buffer);
      buffer.writeInt(this._creature.isServitor() ? 2 : (this._creature.isPet() ? 1 : 0));
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeInt(this._effects.size());

      for (BuffInfo info : this._effects) {
         if (info != null && info.isInUse()) {
            buffer.writeInt(info.getSkill().getDisplayId());
            buffer.writeShort(info.getSkill().getDisplayLevel());
            buffer.writeInt(info.getTime());
         }
      }
   }
}
