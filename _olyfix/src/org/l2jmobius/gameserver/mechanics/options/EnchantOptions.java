package org.l2jmobius.gameserver.mechanics.options;

public class EnchantOptions {
   private final int _level;
   private final int[] _options;

   public EnchantOptions(int level) {
      this._level = level;
      this._options = new int[3];
   }

   public int getLevel() {
      return this._level;
   }

   public int[] getOptions() {
      return this._options;
   }

   public void setOption(byte index, int option) {
      if (this._options.length > index) {
         this._options[index] = option;
      }
   }
}
