package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerHasClanHall extends Condition {
   private final List<Integer> _clanHall;

   public ConditionPlayerHasClanHall(List<Integer> clanHall) {
      this._clanHall = clanHall;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (!effector.isPlayer()) {
         return false;
      } else {
         Clan clan = effector.asPlayer().getClan();
         if (clan != null) {
            return this._clanHall.size() == 1 && this._clanHall.get(0) == -1 ? clan.getHideoutId() > 0 : this._clanHall.contains(clan.getHideoutId());
         } else {
            return this._clanHall.size() == 1 && this._clanHall.get(0) == 0;
         }
      }
   }
}
