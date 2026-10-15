package org.l2jmobius.commons.util;

import java.util.concurrent.ThreadLocalRandom;

public class Rnd {
   private static final int MINIMUM_POSITIVE_INT = 1;
   private static final long MINIMUM_POSITIVE_LONG = 1L;
   private static final float MINIMUM_POSITIVE_FLOAT = Float.intBitsToFloat(1);
   private static final double MINIMUM_POSITIVE_DOUBLE = Double.longBitsToDouble(1L);

   public static boolean nextBoolean() {
      return ThreadLocalRandom.current().nextBoolean();
   }

   public static void nextBytes(byte[] bytes) {
      ThreadLocalRandom.current().nextBytes(bytes);
   }

   public static int get(int bound) {
      return bound <= 0 ? 0 : ThreadLocalRandom.current().nextInt(bound);
   }

   public static int get(int origin, int bound) {
      return origin >= bound ? origin : ThreadLocalRandom.current().nextInt(origin, bound == Integer.MAX_VALUE ? bound : bound + 1);
   }

   public static int nextInt() {
      return ThreadLocalRandom.current().nextInt();
   }

   public static long get(long bound) {
      return bound <= 0L ? 0L : ThreadLocalRandom.current().nextLong(bound);
   }

   public static long get(long origin, long bound) {
      return origin >= bound ? origin : ThreadLocalRandom.current().nextLong(origin, bound == Long.MAX_VALUE ? bound : bound + 1L);
   }

   public static long nextLong() {
      return ThreadLocalRandom.current().nextLong();
   }

   public static float get(float bound) {
      return bound <= 0.0F ? 0.0F : ThreadLocalRandom.current().nextFloat(bound);
   }

   public static float get(float origin, float bound) {
      return origin >= bound ? origin : ThreadLocalRandom.current().nextFloat(origin, bound == Float.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_FLOAT);
   }

   public static float nextFloat() {
      return ThreadLocalRandom.current().nextFloat();
   }

   public static double get(double bound) {
      return bound <= 0.0 ? 0.0 : ThreadLocalRandom.current().nextDouble(bound);
   }

   public static double get(double origin, double bound) {
      return origin >= bound ? origin : ThreadLocalRandom.current().nextDouble(origin, bound == Double.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_DOUBLE);
   }

   public static double nextDouble() {
      return ThreadLocalRandom.current().nextDouble();
   }

   public static double nextGaussian() {
      return ThreadLocalRandom.current().nextGaussian();
   }
}
