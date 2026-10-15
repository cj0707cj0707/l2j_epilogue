package org.l2jmobius.gameserver.mechanics.clientstrings;

public class BuilderText extends Builder {
   private final String _text;

   BuilderText(String text) {
      this._text = text;
   }

   @Override
   public String toString(Object param) {
      return this.toString();
   }

   @Override
   public String toString(Object... params) {
      return this.toString();
   }

   @Override
   public int getIndex() {
      return -1;
   }

   @Override
   public String toString() {
      return this._text;
   }
}
