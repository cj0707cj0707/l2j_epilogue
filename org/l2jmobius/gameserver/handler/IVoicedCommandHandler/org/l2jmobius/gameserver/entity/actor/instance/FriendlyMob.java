package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class FriendlyMob extends Attackable {
   public FriendlyMob(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FriendlyMob);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return attacker.isPlayer() ? attacker.asPlayer().getKarma() > 0 : false;
   }

   @Override
   public boolean isAggressive() {
      return true;
   }
}
