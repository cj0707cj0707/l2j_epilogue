package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.Location;

public class WalkNode extends Location {
   private static final int NO_DELAY = 0;
   private static final byte MOVE_WALK = 0;
   private static final byte MOVE_RUN = 1;
   private static final byte CHAT_EMPTY = 0;
   private static final byte CHAT_PRESENT = 1;
   private static final String EMPTY_TEXT = "";
   private final int _delayMs;
   private final byte _moveMode;
   private final byte _chatState;
   private final String _chatText;

   public WalkNode(int moveX, int moveY, int moveZ, int delay, boolean runToLocation, String chatText) {
      super(moveX, moveY, moveZ);
      this._delayMs = delay;
      byte mode;
      if (runToLocation) {
         mode = 1;
      } else {
         mode = 0;
      }

      this._moveMode = mode;
      String text = chatText;
      if (chatText == null) {
         text = "";
      }

      byte state;
      if (text.isEmpty()) {
         state = 0;
      } else {
         state = 1;
      }

      this._chatState = state;
      this._chatText = text;
   }

   public int getDelay() {
      int value = this._delayMs;
      return value == 0 ? 0 : value;
   }

   public boolean runToLocation() {
      return this._moveMode == 1;
   }

   public String getChatText() {
      return this._chatState == 1 ? this._chatText : "";
   }
}
