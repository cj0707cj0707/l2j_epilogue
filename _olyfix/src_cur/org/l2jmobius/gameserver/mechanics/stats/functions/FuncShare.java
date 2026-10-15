package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncShare extends AbstractFunction {
   public FuncShare(Stat stat, int order, Object owner, double value, Condition applyCond) {
      super(stat, order, owner, value, applyCond);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      if ((this.getApplyCond() == null || this.getApplyCond().test(effector, effected, skill)) && effector != null && effector.isServitor()) {
         Summon summon = effector.asSummon();
         Player player = summon.getOwner();
         if (player != null) {
            return initVal + getBaseValue(this.getStat(), player) * this.getValue();
         }
      }

      return initVal;
   }

   public static double getBaseValue(Stat stat, Player player) {
      switch (stat) {
         case MAX_HP:
            return player.getMaxHp();
         case MAX_MP:
            return player.getMaxMp();
         case POWER_ATTACK:
            return player.getPAtk(null);
         case MAGIC_ATTACK:
            return player.getMAtk(null, null);
         case POWER_DEFENCE:
            return player.getPDef(null);
         case MAGIC_DEFENCE:
            return player.getMDef(null, null);
         case CRITICAL_RATE:
            return player.getCriticalHit(null, null);
         case POWER_ATTACK_SPEED:
            return player.getPAtkSpd();
         case MAGIC_ATTACK_SPEED:
            return player.getMAtkSpd();
         default:
            return player.calcStat(stat, 0.0, null, null);
      }
   }
}
