package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerInsideZoneId extends Condition {
   private final Set<Integer> _zones;

   public ConditionPlayerInsideZoneId(Set<Integer> zones) {
      this._zones = zones;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector.asPlayer() == null) {
         return false;
      } else {
         for (ZoneType zone : ZoneManager.getInstance().getZones(effector)) {
            if (this._zones.contains(zone.getId())) {
               return true;
            }
         }

         return false;
      }
   }
}
