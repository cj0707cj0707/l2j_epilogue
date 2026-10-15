package org.l2jmobius.gameserver.network;

import org.l2jmobius.commons.util.Rnd;

public class BlowFishKeygen {
   private static final int KEY_LENGTH_BYTES = 16;
   private static final int RANDOM_PREFIX_LENGTH = 8;
   private static final byte[] KEY_TAIL_BYTES = new byte[]{-56, 39, -109, 1, -95, 108, 49, -105};

   private BlowFishKeygen() {
   }

   public static byte[] getRandomKey() {
      byte[] key = new byte[16];
      Rnd.nextBytes(key);
      System.arraycopy(KEY_TAIL_BYTES, 0, key, 8, KEY_TAIL_BYTES.length);
      return key;
   }
}
