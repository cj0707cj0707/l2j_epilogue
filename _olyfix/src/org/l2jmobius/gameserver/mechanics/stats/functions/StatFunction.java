package org.l2jmobius.gameserver.mechanics.stats.functions;

public enum StatFunction {
   ADD("Add", 30),
   DIV("Div", 20),
   ENCHANT("Enchant", 0),
   ENCHANTHP("EnchantHp", 40),
   MUL("Mul", 20),
   SET("Set", 0),
   SHARE("Share", 30),
   SUB("Sub", 30);

   private final String _name;
   private final int _order;

   private StatFunction(String name, int order) {
      this._name = name;
      this._order = order;
   }

   public String getName() {
      return this._name;
   }

   public int getOrder() {
      return this._order;
   }
}
