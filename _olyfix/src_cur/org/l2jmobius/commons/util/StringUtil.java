package org.l2jmobius.commons.util;

import java.util.StringJoiner;
import java.util.StringTokenizer;

public class StringUtil {
   public static void append(StringBuilder sb, String arg) {
      sb.append(arg);
   }

   public static void append(StringBuilder sb, String... args) {
      int totalLength = sb.length();

      for (String arg : args) {
         totalLength += arg != null ? arg.length() : 4;
      }

      sb.ensureCapacity(totalLength);

      for (String arg : args) {
         sb.append(arg);
      }
   }

   public static void append(StringBuilder sb, Object... args) {
      int totalLength = sb.length();
      String[] strings = new String[args.length];

      for (int i = 0; i < args.length; i++) {
         strings[i] = String.valueOf(args[i]);
         totalLength += strings[i].length();
      }

      sb.ensureCapacity(totalLength);

      for (String string : strings) {
         sb.append(string);
      }
   }

   public static String concat(String... args) {
      int totalLength = 0;

      for (String arg : args) {
         totalLength += arg != null ? arg.length() : 4;
      }

      StringBuilder sb = new StringBuilder(totalLength);

      for (String arg : args) {
         sb.append(arg);
      }

      return sb.toString();
   }

   public static String concat(Object... args) {
      int totalLength = 0;
      String[] strings = new String[args.length];

      for (int i = 0; i < args.length; i++) {
         strings[i] = String.valueOf(args[i]);
         totalLength += strings[i].length();
      }

      StringBuilder sb = new StringBuilder(totalLength);

      for (String string : strings) {
         sb.append(string);
      }

      return sb.toString();
   }

   public static <T> String implode(Iterable<T> items, String delimiter) {
      StringJoiner joiner = new StringJoiner(delimiter);

      for (T item : items) {
         joiner.add(item.toString());
      }

      return joiner.toString();
   }

   public static <T> String implode(T[] array, String delimiter) {
      StringJoiner joiner = new StringJoiner(delimiter);

      for (T element : array) {
         joiner.add(element.toString());
      }

      return joiner.toString();
   }

   public static String capitalizeFirst(String text) {
      return text != null && !text.isEmpty() ? Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase() : text;
   }

   public static String separateWords(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder result = new StringBuilder();
         char[] chars = text.toCharArray();

         for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            if (Character.isUpperCase(current) && i > 0 && Character.isLowerCase(chars[i - 1])) {
               result.append(' ');
            }

            result.append(current);
         }

         return result.toString();
      } else {
         return text;
      }
   }

   public static String enumToString(Enum<?> enumeration) {
      String name = enumeration.name().toLowerCase();
      StringBuilder sb = new StringBuilder(name.length());
      boolean capitalizeNext = true;

      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         if (c == '_') {
            sb.append(' ');
            capitalizeNext = true;
         } else if (capitalizeNext) {
            sb.append(Character.toUpperCase(c));
            capitalizeNext = false;
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   public static Object parseValue(String value) {
      if (value == null) {
         return null;
      } else {
         String val = value.trim();
         if (val.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
         } else if (val.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
         } else {
            try {
               return Long.valueOf(val);
            } catch (NumberFormatException var9) {
               try {
                  return Integer.valueOf(val);
               } catch (NumberFormatException var8) {
                  try {
                     return Float.valueOf(val);
                  } catch (NumberFormatException var7) {
                     try {
                        return Double.valueOf(val);
                     } catch (NumberFormatException var6) {
                        return val;
                     }
                  }
               }
            }
         }
      }
   }

   public static int parseInt(String text, int defaultValue) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException var3) {
         return defaultValue;
      }
   }

   public static int parseNextInt(StringTokenizer tokenizer, int defaultValue) {
      if (tokenizer.hasMoreTokens()) {
         try {
            String value = tokenizer.nextToken().trim();
            return Integer.parseInt(value);
         } catch (NumberFormatException var3) {
         }
      }

      return defaultValue;
   }

   public static boolean isAlphaNumeric(String text) {
      if (text != null && !text.isEmpty()) {
         for (int i = 0; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isNumeric(String text) {
      if (text != null && !text.isEmpty()) {
         for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isInteger(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            Integer.parseInt(text);
            return true;
         } catch (NumberFormatException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isFloat(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            Float.parseFloat(text);
            return true;
         } catch (NumberFormatException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isDouble(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            Double.parseDouble(text);
            return true;
         } catch (NumberFormatException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static <T extends Enum<T>> boolean isEnum(String name, Class<T> enumType) {
      if (name != null && !name.isEmpty()) {
         try {
            Enum.valueOf(enumType, name);
            return true;
         } catch (IllegalArgumentException var3) {
            return false;
         }
      } else {
         return false;
      }
   }
}
