package org.l2jmobius.gameserver.data.holders;

public class StringStringHolder {
   private final String _string1;
   private final String _string2;

   public StringStringHolder(String string1, String string2) {
      this._string1 = string1;
      this._string2 = string2;
   }

   public String getString1() {
      return this._string1;
   }

   public String getString2() {
      return this._string2;
   }
}
