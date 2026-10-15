package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.List;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSweep extends Condition {
   private final boolean _value;

   public ConditionPlayerCanSweep(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean canSweep = false;
      if (effector.isPlayer() && skill != null) {
         Player sweeper = effector.asPlayer();
         List<WorldObject> targets = skill.getTargetList(sweeper);
         if (targets != null) {
            for (WorldObject objTarget : targets) {
               if (objTarget != null && objTarget.isAttackable()) {
                  Attackable target = objTarget.asAttackable();
                  if (target.isDead()) {
                     if (target.isSpoiled()) {
                        canSweep = target.checkSpoilOwner(sweeper, true);
                        canSweep &= !target.isOldCorpse(sweeper, NpcConfig.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true);
                        canSweep &= sweeper.getInventory().checkInventorySlotsAndWeight(target.getSpoilLootItems(), true, true);
                     } else {
                        sweeper.sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
                     }
                  }
               }
            }
         }
      }

      return this._value == canSweep;
   }
}
