package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;

public class DropHolder {
   private final DropType _dropType;
   private final int _itemId;
   private final long _min;
   private final long _max;
   private final double _chance;

   public DropHolder(DropType dropType, int itemId, long min, long max, double chance) {
      this._dropType = dropType;
      this._itemId = itemId;
      this._min = min;
      this._max = max;
      this._chance = chance;
   }

   public DropType getDropType() {
      return this._dropType;
   }

   public int getItemId() {
      return this._itemId;
   }

   public long getMin() {
      return this._min;
   }

   public long getMax() {
      return this._max;
   }

   public double getChance() {
      return this._chance;
   }
}
