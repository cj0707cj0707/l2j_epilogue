package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public class InvulSkillHolder extends SkillHolder {
   private final AtomicInteger _instances = new AtomicInteger(1);

   public InvulSkillHolder(int skillId, int skillLevel) {
      super(skillId, skillLevel);
   }

   public InvulSkillHolder(SkillHolder holder) {
      super(holder.getSkill());
   }

   public int getInstances() {
      return this._instances.get();
   }

   public int increaseInstances() {
      return this._instances.incrementAndGet();
   }

   public int decreaseInstances() {
      return this._instances.decrementAndGet();
   }
}
