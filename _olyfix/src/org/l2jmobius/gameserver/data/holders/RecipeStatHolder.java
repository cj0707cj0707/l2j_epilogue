package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.enums.StatType;

public class RecipeStatHolder {
   private final StatType _type;
   private final int _value;

   public RecipeStatHolder(String type, int value) {
      this._type = Enum.valueOf(StatType.class, type);
      this._value = value;
   }

   public StatType getType() {
      return this._type;
   }

   public int getValue() {
      return this._value;
   }
}
