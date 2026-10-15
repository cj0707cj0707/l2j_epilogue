package org.l2jmobius.gameserver.mechanics.clientstrings;

import java.util.ArrayList;
import java.util.List;

public abstract class Builder {
   public abstract String toString(Object var1);

   public abstract String toString(Object... var1);

   public abstract int getIndex();

   public static Builder newBuilder(String text) {
      List<Builder> builders = new ArrayList<>();
      int index1 = 0;
      int index2 = 0;
      char[] array = text.toCharArray();

      int arrayLength;
      for (arrayLength = array.length; index1 < arrayLength; index1++) {
         char c = array[index1];
         if (c == '$' && index1 < arrayLength - 2) {
            char c2 = array[index1 + 1];
            if (c2 == 'c' || c2 == 's' || c2 == 'p' || c2 == 'C' || c2 == 'S' || c2 == 'P') {
               char c3 = array[index1 + 2];
               if (Character.isDigit(c3)) {
                  int paramId = Character.getNumericValue(c3);
                  int subTextLen = index1 - index2;
                  if (subTextLen != 0) {
                     builders.add(new BuilderText(new String(array, index2, subTextLen)));
                  }

                  builders.add(new BuilderObject(paramId));
                  index1 += 2;
                  index2 = index1 + 1;
               }
            }
         }
      }

      if (arrayLength >= index1) {
         int subTextLen = index1 - index2;
         if (subTextLen != 0) {
            builders.add(new BuilderText(new String(array, index2, subTextLen)));
         }
      }

      return (Builder)(builders.size() == 1 ? builders.get(0) : new BuilderContainer(builders.toArray(new Builder[0])));
   }
}
