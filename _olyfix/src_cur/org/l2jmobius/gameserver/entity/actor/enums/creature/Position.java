package org.l2jmobius.gameserver.entity.actor.enums.creature;

import org.l2jmobius.gameserver.interfaces.ILocational;

public enum Position {
   FRONT,
   SIDE,
   BACK;

   public static Position getPosition(ILocational from, ILocational to) {
      int heading = Math.abs(to.getHeading() - from.calculateHeadingTo(to));
      if ((heading < 8192 || heading > 24576) && Integer.toUnsignedLong(heading - 40960) > 16384L) {
         return Integer.toUnsignedLong(heading - 8192) <= 49152L ? FRONT : BACK;
      } else {
         return SIDE;
      }
   }
}
