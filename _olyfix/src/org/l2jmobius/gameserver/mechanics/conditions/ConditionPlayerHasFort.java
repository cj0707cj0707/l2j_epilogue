package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerHasFort extends Condition {
   private final int _fort;

   public ConditionPlayerHasFort(int fort) {
      this._fort = fort;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (!effector.isPlayer()) {
         return false;
      } else {
         Clan clan = effector.asPlayer().getClan();
         if (clan == null) {
            return this._fort == 0;
         } else {
            return this._fort == -1 ? clan.getFortId() > 0 : clan.getFortId() == this._fort;
         }
      }
   }
}
