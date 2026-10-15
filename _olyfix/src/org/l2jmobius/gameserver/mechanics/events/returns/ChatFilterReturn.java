package org.l2jmobius.gameserver.mechanics.events.returns;

public class ChatFilterReturn extends AbstractEventReturn {
   private final String _filteredText;

   public ChatFilterReturn(String filteredText, boolean override, boolean abort) {
      super(override, abort);
      this._filteredText = filteredText;
   }

   public String getFilteredText() {
      return this._filteredText;
   }
}
