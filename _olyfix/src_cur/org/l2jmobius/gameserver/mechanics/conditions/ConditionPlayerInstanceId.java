package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerInstanceId extends Condition {
   private final Set<Integer> _instanceIds;

   public ConditionPlayerInstanceId(Set<Integer> instanceIds) {
      this._instanceIds = instanceIds;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player == null) {
         return false;
      } else {
         int instanceId = effector.getInstanceId();
         if (instanceId <= 0) {
            return false;
         } else {
            InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
            return world != null && world.getInstanceId() == instanceId && this._instanceIds.contains(world.getTemplateId());
         }
      }
   }
}
