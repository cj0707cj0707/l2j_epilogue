package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMAtkMod extends AbstractFunction {
   private static final FuncMAtkMod _fma_instance = new FuncMAtkMod();

   public static AbstractFunction getInstance() {
      return _fma_instance;
   }

   private FuncMAtkMod() {
      super(Stat.MAGIC_ATTACK, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double lvlMod = effector.isPlayer() ? BaseStat.INT.calcBonus(effector.asPlayer()) : BaseStat.INT.calcBonus(effector);
      double intMod = effector.isPlayer() ? effector.asPlayer().getLevelMod() : effector.getLevelMod();
      return initVal * (lvlMod * lvlMod) * (intMod * intMod);
   }
}
