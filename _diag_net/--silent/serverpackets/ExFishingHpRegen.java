package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class ExFishingHpRegen extends ServerPacket {
   private final Creature _creature;
   private final int _time;
   private final int _fishHP;
   private final int _hpMode;
   private final int _anim;
   private final int _goodUse;
   private final int _penalty;
   private final int _hpBarColor;

   public ExFishingHpRegen(Creature creature, int time, int fishHP, int hpMode, int goodUse, int anim, int penalty, int hpBarColor) {
      this._creature = creature;
      this._time = time;
      this._fishHP = fishHP;
      this._hpMode = hpMode;
      this._goodUse = goodUse;
      this._anim = anim;
      this._penalty = penalty;
      this._hpBarColor = hpBarColor;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_FISHING_HP_REGEN.writeId(this, buffer);
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeInt(this._time);
      buffer.writeInt(this._fishHP);
      buffer.writeByte(this._hpMode);
      buffer.writeByte(this._goodUse);
      buffer.writeByte(this._anim);
      buffer.writeInt(this._penalty);
      buffer.writeByte(this._hpBarColor);
   }
}
