package org.l2jmobius.commons.util;

public class HexUtil {
   public static byte[] generateHexBytes(int size) {
      byte[] array = new byte[size];
      Rnd.nextBytes(array);

      for (int i = 0; i < array.length; i++) {
         while (array[i] == 0) {
            array[i] = (byte)Rnd.get(127);
         }
      }

      return array;
   }
}
