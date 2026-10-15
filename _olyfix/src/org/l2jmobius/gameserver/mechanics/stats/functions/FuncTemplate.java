package org.l2jmobius.gameserver.mechanics.stats.functions;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncTemplate {
   private static final Logger LOG = Logger.getLogger(FuncTemplate.class.getName());
   private final Condition _attachCond;
   private final Condition _applyCond;
   private final Constructor<?> _constructor;
   private final Stat _stat;
   private final int _order;
   private final double _value;

   public FuncTemplate(Condition attachCond, Condition applyCond, String functionName, int order, Stat stat, double value) {
      StatFunction function = StatFunction.valueOf(functionName.toUpperCase());
      if (order >= 0) {
         this._order = order;
      } else {
         this._order = function.getOrder();
      }

      this._attachCond = attachCond;
      this._applyCond = applyCond;
      this._stat = stat;
      this._value = value;

      try {
         Class<?> functionClass = Class.forName("org.l2jmobius.gameserver.mechanics.stats.functions.Func" + function.getName());
         this._constructor = functionClass.getConstructor(Stat.class, int.class, Object.class, double.class, Condition.class);
      } catch (NoSuchMethodException | ClassNotFoundException var10) {
         throw new RuntimeException(var10);
      }
   }

   public Stat getStat() {
      return this._stat;
   }

   public int getOrder() {
      return this._order;
   }

   public double getValue() {
      return this._value;
   }

   public AbstractFunction getFunc(Creature caster, Creature target, Skill skill, Object owner) {
      return this.getFunc(caster, target, skill, null, owner);
   }

   public AbstractFunction getFunc(Creature caster, Creature target, Item item, Object owner) {
      return this.getFunc(caster, target, null, item, owner);
   }

   private AbstractFunction getFunc(Creature caster, Creature target, Skill skill, Item item, Object owner) {
      if (this._attachCond != null && !this._attachCond.test(caster, target, skill)) {
         return null;
      } else {
         try {
            return (AbstractFunction)this._constructor.newInstance(this._stat, this._order, owner, this._value, this._applyCond);
         } catch (Exception var7) {
            LOG.warning(FuncTemplate.class.getSimpleName() + ": " + var7.getMessage());
            return null;
         }
      }
   }
}
