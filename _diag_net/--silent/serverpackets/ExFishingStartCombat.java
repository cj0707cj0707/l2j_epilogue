package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class ExFishingStartCombat extends ServerPacket {
   private final Creature _creature;
   private final int _time;
   private final int _hp;
   private final int _lureType;
   private final int _deceptiveMode;
   private final int _mode;

   public ExFishingStartCombat(Creature creature, int time, int hp, int mode, int lureType, int deceptiveMode) {
      this._creature = creature;
      this._time = time;
      this._hp = hp;
      this._mode = mode;
      this._lureType = lureType;
      this._deceptiveMode = deceptiveMode;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_FISHING_START_COMBAT.writeId(this, buffer);
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeInt(this._time);
      buffer.writeInt(this._hp);
      buffer.writeByte(this._mode);
      buffer.writeByte(this._lureType);
      buffer.writeByte(this._deceptiveMode);
   }
}
