package org.l2jmobius.gameserver.mechanics.html;

import java.util.Collection;

@FunctionalInterface
public interface IBodyHandler<T> {
   void apply(int var1, T var2, StringBuilder var3);

   default void create(Collection<T> elements, int pages, int start, int elementsPerPage, StringBuilder sb) {
      int i = 0;

      for (T element : elements) {
         if (i++ >= start) {
            this.apply(pages, element, sb);
            if (i >= elementsPerPage + start) {
               break;
            }
         }
      }
   }
}
