package org.l2jmobius.gameserver.mechanics.stats.functions;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public abstract class AbstractFunction {
   protected static final Logger LOG = Logger.getLogger(AbstractFunction.class.getName());
   private final Stat _stat;
   private final int _order;
   private final Object _funcOwner;
   private final Condition _applyCond;
   private final double _value;

   public AbstractFunction(Stat stat, int order, Object owner, double value, Condition applyCond) {
      this._stat = stat;
      this._order = order;
      this._funcOwner = owner;
      this._value = value;
      this._applyCond = applyCond;
   }

   public Condition getApplyCond() {
      return this._applyCond;
   }

   public Object getFuncOwner() {
      return this._funcOwner;
   }

   public int getOrder() {
      return this._order;
   }

   public Stat getStat() {
      return this._stat;
   }

   public double getValue() {
      return this._value;
   }

   public abstract double calc(Creature var1, Creature var2, Skill var3, double var4);
}
