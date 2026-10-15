package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExOlympiadSpelledInfo extends ServerPacket {
   private final int _playerId;
   private final List<BuffInfo> _effects = new ArrayList<>();

   public ExOlympiadSpelledInfo(Player player) {
      this._playerId = player.getObjectId();
   }

   public void addSkill(BuffInfo info) {
      this._effects.add(info);
   }

   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_OLYMPIAD_SPELLED_INFO.writeId(this, buffer);
      buffer.writeInt(this._playerId);
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
