package org.l2jmobius.gameserver.mechanics.stats;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class Calculator {
   private static final AbstractFunction[] EMPTY_FUNCS = new AbstractFunction[0];
   private AbstractFunction[] _functions;

   public Calculator() {
      this._functions = EMPTY_FUNCS;
   }

   public Calculator(Calculator c) {
      this._functions = c._functions;
   }

   public static boolean equalsCals(Calculator c1, Calculator c2) {
      if (c1 == c2) {
         return true;
      } else if (c1 != null && c2 != null) {
         AbstractFunction[] funcs1 = c1._functions;
         AbstractFunction[] funcs2 = c2._functions;
         if (funcs1 == funcs2) {
            return true;
         } else if (funcs1.length != funcs2.length) {
            return false;
         } else if (funcs1.length == 0) {
            return true;
         } else {
            for (int i = 0; i < funcs1.length; i++) {
               if (funcs1[i] != funcs2[i]) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public int size() {
      return this._functions.length;
   }

   public synchronized void addFunc(AbstractFunction function) {
      AbstractFunction[] funcs = this._functions;
      AbstractFunction[] tmp = new AbstractFunction[funcs.length + 1];
      int order = function.getOrder();

      int i;
      for (i = 0; i < funcs.length && order >= funcs[i].getOrder(); i++) {
         tmp[i] = funcs[i];
      }

      for (tmp[i] = function; i < funcs.length; i++) {
         tmp[i + 1] = funcs[i];
      }

      this._functions = tmp;
   }

   public synchronized void removeFunc(AbstractFunction function) {
      AbstractFunction[] funcs = this._functions;
      AbstractFunction[] tmp = new AbstractFunction[funcs.length - 1];

      int i;
      for (i = 0; i < funcs.length - 1 && function != funcs[i]; i++) {
         tmp[i] = funcs[i];
      }

      if (i != funcs.length) {
         i++;

         while (i < funcs.length) {
            tmp[i - 1] = funcs[i];
            i++;
         }

         if (tmp.length == 0) {
            this._functions = EMPTY_FUNCS;
         } else {
            this._functions = tmp;
         }
      }
   }

   public synchronized List<Stat> removeOwner(Object owner) {
      List<Stat> modifiedStats = new ArrayList<>();

      for (AbstractFunction func : this._functions) {
         if (func.getFuncOwner() == owner) {
            modifiedStats.add(func.getStat());
            this.removeFunc(func);
         }
      }

      return modifiedStats;
   }

   public double calc(Creature caster, Creature target, Skill skill, double initVal) {
      double value = initVal;

      for (AbstractFunction func : this._functions) {
         value = func.calc(caster, target, skill, value);
      }

      return value;
   }

   public AbstractFunction[] getFunctions() {
      return this._functions;
   }
}
