package org.l2jmobius.gameserver.cache;

public class RelationCache {
   private final int _relation;
   private final boolean _isAutoAttackable;

   public RelationCache(int relation, boolean isAutoAttackable) {
      this._relation = relation;
      this._isAutoAttackable = isAutoAttackable;
   }

   public int getRelation() {
      return this._relation;
   }

   public boolean isAutoAttackable() {
      return this._isAutoAttackable;
   }
}
