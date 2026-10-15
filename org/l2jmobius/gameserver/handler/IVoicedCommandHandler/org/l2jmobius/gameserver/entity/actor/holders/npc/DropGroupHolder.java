package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.ArrayList;
import java.util.List;

public class DropGroupHolder {
   private final List<DropHolder> _dropList = new ArrayList<>();
   private final double _chance;

   public DropGroupHolder(double chance) {
      this._chance = chance;
   }

   public List<DropHolder> getDropList() {
      return this._dropList;
   }

   public void addDrop(DropHolder holder) {
      this._dropList.add(holder);
   }

   public double getChance() {
      return this._chance;
   }

   public void sortByChance() {
      this._dropList.sort((d1, d2) -> Double.compare(d1.getChance(), d2.getChance()));
   }
}
