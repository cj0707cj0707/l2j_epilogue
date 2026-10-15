package org.l2jmobius.gameserver.entity.actor.tasks.creature;

import java.util.List;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class MagicUseTask implements Runnable {
   private final Creature _creature;
   private List<WorldObject> _targets;
   private final Skill _skill;
   private int _count;
   private int _skillTime;
   private int _phase;
   private final boolean _simultaneously;

   public MagicUseTask(Creature creature, List<WorldObject> targets, Skill s, int hit, boolean simultaneous) {
      this._creature = creature;
      this._targets = targets;
      this._skill = s;
      this._count = 0;
      this._phase = 1;
      this._skillTime = hit;
      this._simultaneously = simultaneous;
   }

   @Override
   public void run() {
      if (this._creature != null) {
         switch (this._phase) {
            case 1:
               this._creature.onMagicLaunchedTimer(this);
               break;
            case 2:
               this._creature.onMagicHitTimer(this);
               break;
            case 3:
               this._creature.onMagicFinalizer(this);
         }
      }
   }

   public int getCount() {
      return this._count;
   }

   public int getPhase() {
      return this._phase;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public int getSkillTime() {
      return this._skillTime;
   }

   public List<WorldObject> getTargets() {
      return this._targets;
   }

   public boolean isSimultaneous() {
      return this._simultaneously;
   }

   public void setCount(int count) {
      this._count = count;
   }

   public void setPhase(int phase) {
      this._phase = phase;
   }

   public void setSkillTime(int skillTime) {
      this._skillTime = skillTime;
   }

   public void setTargets(List<WorldObject> targets) {
      this._targets = targets;
   }
}
