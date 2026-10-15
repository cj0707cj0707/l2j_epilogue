package org.l2jmobius.gameserver.util;

public class ArrayUtil {
   public static boolean contains(int[] array, int value) {
      for (int i = 0; i < array.length; i++) {
         if (array[i] == value) {
            return true;
         }
      }

      return false;
   }

   public static boolean contains(Object[] array, Object value) {
      if (value == null) {
         for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
               return true;
            }
         }
      } else {
         for (int ix = 0; ix < array.length; ix++) {
            if (value.equals(array[ix])) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean contains(String[] array, String value, boolean ignoreCase) {
      if (ignoreCase) {
         for (int i = 0; i < array.length; i++) {
            if (value.equalsIgnoreCase(array[i])) {
               return true;
            }
         }
      } else {
         for (int ix = 0; ix < array.length; ix++) {
            if (value.equals(array[ix])) {
               return true;
            }
         }
      }

      return false;
   }
}
