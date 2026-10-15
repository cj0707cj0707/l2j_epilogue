package org.l2jmobius.gameserver.entity.actor.tasks.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class UsePotionTask implements Runnable {
   private final Creature _creature;
   private final Skill _skill;

   public UsePotionTask(Creature creature, Skill skill) {
      this._creature = creature;
      this._skill = skill;
   }

   @Override
   public void run() {
      if (this._creature != null) {
         this._creature.doSimultaneousCast(this._skill);
      }
   }
}
