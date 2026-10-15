package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.Set;

public class EventDropHolder extends DropHolder {
   private final int _minLevel;
   private final int _maxLevel;
   private final Set<Integer> _monsterIds;

   public EventDropHolder(int itemId, long min, long max, double chance, int minLevel, int maxLevel, Set<Integer> monsterIds) {
      super(null, itemId, min, max, chance);
      this._minLevel = minLevel;
      this._maxLevel = maxLevel;
      this._monsterIds = monsterIds;
   }

   public int getMinLevel() {
      return this._minLevel;
   }

   public int getMaxLevel() {
      return this._maxLevel;
   }

   public Set<Integer> getMonsterIds() {
      return this._monsterIds;
   }
}
