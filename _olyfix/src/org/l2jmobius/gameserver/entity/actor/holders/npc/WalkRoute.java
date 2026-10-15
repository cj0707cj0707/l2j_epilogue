package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.List;
import java.util.Objects;

public class WalkRoute {
   private static final byte REPEAT_MIN = 0;
   private static final byte REPEAT_MID = 1;
   private static final byte REPEAT_MAX = 2;
   private final String _name;
   private final List<WalkNode> _nodeList;
   private final boolean _repeatWalk;
   private boolean _stopAfterCycle;
   private final byte _repeatType;

   public WalkRoute(String name, List<WalkNode> route, boolean repeat, byte repeatType) {
      this._name = Objects.requireNonNull(name, "name");
      this._nodeList = Objects.requireNonNull(route, "route");
      if (this._nodeList.isEmpty()) {
         throw new IllegalArgumentException("WalkRoute node list is empty: " + this._name);
      } else {
         for (int i = 0; i < this._nodeList.size(); i++) {
            if (this._nodeList.get(i) == null) {
               throw new IllegalArgumentException("WalkRoute has null node at index " + i + ": " + this._name);
            }
         }

         this._repeatType = repeatType;
         boolean supported = false;

         supported = switch (this._repeatType) {
            case 0, 1, 2 -> true;
            default -> false;
         };
         if (this._repeatType < 0 || this._repeatType > 2) {
            supported = false;
         }

         this._repeatWalk = repeat && supported;
      }
   }

   public boolean repeatWalk() {
      return this._repeatWalk;
   }

   public byte getRepeatType() {
      return this._repeatType;
   }

   public boolean doOnce() {
      return this._stopAfterCycle;
   }

   public WalkNode getLastNode() {
      int lastIndex = this._nodeList.size() - 1;
      return this._nodeList.get(lastIndex);
   }

   public int getNodesCount() {
      return this._nodeList.size();
   }

   public List<WalkNode> getNodeList() {
      return this._nodeList;
   }

   public String getName() {
      return this._name;
   }
}
