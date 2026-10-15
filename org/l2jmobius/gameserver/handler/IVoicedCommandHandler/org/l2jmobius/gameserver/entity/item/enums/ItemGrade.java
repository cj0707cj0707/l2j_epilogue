package org.l2jmobius.gameserver.entity.item.enums;

import org.l2jmobius.gameserver.entity.item.type.CrystalType;

public enum ItemGrade {
   NONE,
   D,
   C,
   B,
   A,
   S;

   public static ItemGrade valueOf(CrystalType type) {
      switch (type) {
         case NONE:
            return NONE;
         case D:
            return D;
         case C:
            return C;
         case B:
            return B;
         case A:
            return A;
         case S:
         case S80:
         case S84:
            return S;
         default:
            return null;
      }
   }
}
