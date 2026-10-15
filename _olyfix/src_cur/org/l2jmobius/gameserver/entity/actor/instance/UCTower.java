package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;

public class UCTower extends Folk {
   private UCTeam _team;

   public UCTower(UCTeam team, NpcTemplate template) {
      super(template);
      this._team = team;
   }

   @Override
   public boolean canBeAttacked() {
      return true;
   }

   @Override
   public boolean isAutoAttackable(Creature creature) {
      return true;
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (attacker.getTeam() != this.getTeam()) {
         if (damage < this.getStatus().getCurrentHp()) {
            this.getStatus().setCurrentHp(this.getStatus().getCurrentHp() - damage);
         } else {
            this.doDie(attacker);
         }
      }
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this._team != null) {
            this._team.deleteTower();
            this._team = null;
         }

         return true;
      }
   }

   @Override
   public Team getTeam() {
      return this._team.getIndex() == 1 ? Team.BLUE : Team.RED;
   }
}
