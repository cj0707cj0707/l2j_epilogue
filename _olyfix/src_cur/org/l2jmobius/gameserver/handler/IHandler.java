package org.l2jmobius.gameserver.handler;

public interface IHandler<K, V> {
   void registerHandler(K var1);

   void removeHandler(K var1);

   K getHandler(V var1);

   int size();
}
