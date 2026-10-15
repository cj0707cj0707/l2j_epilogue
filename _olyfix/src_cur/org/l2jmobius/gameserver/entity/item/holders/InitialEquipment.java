package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.util.StatSet;

public class InitialEquipment extends ItemHolder {
   private final boolean _equipped;

   public InitialEquipment(StatSet set) {
      super(set.getInt("id"), set.getInt("count"));
      this._equipped = set.getBoolean("equipped", false);
   }

   public boolean isEquipped() {
      return this._equipped;
   }
}
