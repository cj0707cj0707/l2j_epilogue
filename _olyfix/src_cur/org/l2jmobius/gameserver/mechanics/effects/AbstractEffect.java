package org.l2jmobius.gameserver.mechanics.effects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.util.StatSet;

public abstract class AbstractEffect {
   protected static final Logger LOGGER = Logger.getLogger(AbstractEffect.class.getName());
   private final Condition _attachCond;
   private List<FuncTemplate> _funcTemplates;
   private final String _name;
   private final int _ticks;

   protected AbstractEffect(Condition attachCond, Condition applyCond, StatSet set, StatSet params) {
      this._attachCond = attachCond;
      this._name = set.getString("name");
      this._ticks = set.getInt("ticks", 0);
   }

   public static AbstractEffect createEffect(Condition attachCond, Condition applyCond, StatSet set, StatSet params) {
      String name = set.getString("name");
      Class<? extends AbstractEffect> handler = EffectHandler.getInstance().getHandler(name);
      if (handler == null) {
         LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent effect handler: " + name);
         return null;
      } else {
         Constructor<?> constructor;
         try {
            constructor = handler.getConstructor(Condition.class, Condition.class, StatSet.class, StatSet.class);
         } catch (SecurityException | NoSuchMethodException var9) {
            LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent constructor for effect handler: " + name + ": " + var9.getMessage());
            return null;
         }

         try {
            return (AbstractEffect)constructor.newInstance(attachCond, applyCond, set, params);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException var8) {
            LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Unable to initialize effect handler: " + name + ": " + var8.getMessage());
            return null;
         }
      }
   }

   public boolean testConditions(Creature caster, Creature target, Skill skill) {
      return this._attachCond == null || this._attachCond.test(caster, target, skill);
   }

   public void attach(FuncTemplate f) {
      if (this._funcTemplates == null) {
         this._funcTemplates = new ArrayList<>(1);
      }

      this._funcTemplates.add(f);
   }

   public String getName() {
      return this._name;
   }

   public int getTicks() {
      return this._ticks;
   }

   public double getTicksMultiplier() {
      return (float)(this.getTicks() * PlayerConfig.EFFECT_TICK_RATIO) / 1000.0F;
   }

   public List<FuncTemplate> getFuncTemplates() {
      return this._funcTemplates;
   }

   public boolean calcSuccess(Creature effector, Creature effected, Skill skill) {
      return true;
   }

   public EffectType getEffectType() {
      return EffectType.NONE;
   }

   public boolean canStart(Creature effector, Creature effected, Skill skill) {
      return true;
   }

   public void onStart(Creature effector, Creature effected, Skill skill) {
   }

   public boolean onActionTime(Creature effector, Creature effected, Skill skill) {
      return false;
   }

   public void onExit(Creature effector, Creature effected, Skill skill) {
   }

   public List<AbstractFunction> getStatFuncs(Creature caster, Creature target, Skill skill) {
      if (this._funcTemplates == null) {
         return Collections.emptyList();
      } else {
         List<AbstractFunction> functions = new ArrayList<>(this._funcTemplates.size());

         for (FuncTemplate functionTemplate : this._funcTemplates) {
            AbstractFunction function = functionTemplate.getFunc(caster, target, skill, this);
            if (function != null) {
               functions.add(function);
            }
         }

         return functions;
      }
   }

   public int getEffectFlags() {
      return EffectFlag.NONE.getMask();
   }

   @Override
   public String toString() {
      return "Effect " + this._name;
   }

   public void decreaseForce() {
   }

   public void increaseEffect() {
   }

   public boolean checkCondition(Object obj) {
      return true;
   }

   public boolean isInstant() {
      return false;
   }
}
