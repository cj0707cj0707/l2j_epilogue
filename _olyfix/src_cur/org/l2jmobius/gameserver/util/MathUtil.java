package org.l2jmobius.gameserver.util;

public class MathUtil {
   public static byte add(byte a, byte b) {
      return (byte)(a + b);
   }

   public static short add(short a, short b) {
      return (short)(a + b);
   }

   public static int add(int a, int b) {
      return a + b;
   }

   public static double add(double a, double b) {
      return a + b;
   }

   public static byte mul(byte a, byte b) {
      return (byte)(a * b);
   }

   public static short mul(short a, short b) {
      return (short)(a * b);
   }

   public static int mul(int a, int b) {
      return a * b;
   }

   public static double mul(double a, double b) {
      return a * b;
   }

   public static byte div(byte a, byte b) {
      if (b == 0) {
         throw new ArithmeticException("Division by zero is not allowed for byte values.");
      } else {
         return (byte)(a / b);
      }
   }

   public static short div(short a, short b) {
      if (b == 0) {
         throw new ArithmeticException("Division by zero is not allowed for short values.");
      } else {
         return (short)(a / b);
      }
   }

   public static int div(int a, int b) {
      if (b == 0) {
         throw new ArithmeticException("Division by zero is not allowed for int values.");
      } else {
         return a / b;
      }
   }

   public static double div(double a, double b) {
      if (b == 0.0) {
         throw new ArithmeticException("Division by zero is not allowed for double values.");
      } else {
         return a / b;
      }
   }

   public static int clamp(int value, int min, int max) {
      return max(min, min(max, value));
   }

   public static long clamp(long value, long min, long max) {
      return max(min, min(max, value));
   }

   public static float clamp(float value, float min, float max) {
      return max(min, min(max, value));
   }

   public static double clamp(double value, double min, double max) {
      return max(min, min(max, value));
   }

   public static int scaleToRange(int value, int sourceMin, int sourceMax, int targetMin, int targetMax) {
      int clampedValue = clamp(value, sourceMin, sourceMax);
      return (clampedValue - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin) + targetMin;
   }

   public static long scaleToRange(long value, long sourceMin, long sourceMax, long targetMin, long targetMax) {
      long clampedValue = clamp(value, sourceMin, sourceMax);
      return (clampedValue - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin) + targetMin;
   }

   public static double scaleToRange(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
      double clampedValue = clamp(value, sourceMin, sourceMax);
      return (clampedValue - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin) + targetMin;
   }

   public static int getIndexOfMinValue(int... array) {
      int minIndex = 0;

      for (int i = 1; i < array.length; i++) {
         if (array[i] < array[minIndex]) {
            minIndex = i;
         }
      }

      return minIndex;
   }

   public static int getIndexOfMaxValue(int... array) {
      int maxIndex = 0;

      for (int i = 1; i < array.length; i++) {
         if (array[i] > array[maxIndex]) {
            maxIndex = i;
         }
      }

      return maxIndex;
   }

   public static int min(int... values) {
      int minValue = values[0];

      for (int value : values) {
         if (value < minValue) {
            minValue = value;
         }
      }

      return minValue;
   }

   public static int max(int... values) {
      int maxValue = values[0];

      for (int value : values) {
         if (value > maxValue) {
            maxValue = value;
         }
      }

      return maxValue;
   }

   public static long min(long... values) {
      long minValue = values[0];

      for (long value : values) {
         if (value < minValue) {
            minValue = value;
         }
      }

      return minValue;
   }

   public static long max(long... values) {
      long maxValue = values[0];

      for (long value : values) {
         if (value > maxValue) {
            maxValue = value;
         }
      }

      return maxValue;
   }

   public static float min(float... values) {
      float minValue = values[0];

      for (float value : values) {
         if (Float.isNaN(value) || value < minValue) {
            minValue = value;
         }
      }

      return minValue;
   }

   public static float max(float... values) {
      float maxValue = values[0];

      for (float value : values) {
         if (Float.isNaN(value) || value > maxValue) {
            maxValue = value;
         }
      }

      return maxValue;
   }

   public static double min(double... values) {
      double minValue = values[0];

      for (double value : values) {
         if (Double.isNaN(value) || value < minValue) {
            minValue = value;
         }
      }

      return minValue;
   }

   public static double max(double... values) {
      double maxValue = values[0];

      for (double value : values) {
         if (Double.isNaN(value) || value > maxValue) {
            maxValue = value;
         }
      }

      return maxValue;
   }

   public static double pow(double base, int exp) {
      if (exp == 0) {
         return 1.0;
      } else if (exp < 0) {
         return 1.0 / pow(base, -exp);
      } else {
         double result;
         for (result = 1.0; exp != 0; exp >>= 1) {
            if ((exp & 1) != 0) {
               result *= base;
            }

            base *= base;
         }

         return result;
      }
   }

   public static double hypot(double dx, double dy) {
      return Math.sqrt(dx * dx + dy * dy);
   }

   public static double hypot(double dx, double dy, double dz) {
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static double hypot(int dx, int dy) {
      double dxd = dx;
      double dyd = dy;
      return Math.sqrt(dxd * dxd + dyd * dyd);
   }

   public static double hypot(int dx, int dy, int dz) {
      double dxd = dx;
      double dyd = dy;
      double dzd = dz;
      return Math.sqrt(dxd * dxd + dyd * dyd + dzd * dzd);
   }
}
