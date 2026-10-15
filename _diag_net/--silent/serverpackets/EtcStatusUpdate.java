package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;

public class EtcStatusUpdate extends ServerPacket {
   private final Player _player;

   public EtcStatusUpdate(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ETC_STATUS_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._player.getCharges());
      buffer.writeInt(this._player.getWeightPenalty());
      buffer.writeInt(this._player.getMessageRefusal() || this._player.isChatBanned() || this._player.isSilenceMode());
      buffer.writeInt(this._player.isInsideZone(ZoneId.DANGER_AREA));
      buffer.writeInt(this._player.getExpertiseWeaponPenalty());
      buffer.writeInt(this._player.getExpertiseArmorPenalty());
      buffer.writeInt(this._player.hasCharmOfCourage());
      buffer.writeInt(this._player.getDeathPenaltyBuffLevel());
      buffer.writeInt(this._player.getChargedSouls());
   }
}
