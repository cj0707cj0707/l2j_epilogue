package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanCreateOutpost extends Condition {
   private final boolean _value;

   public ConditionPlayerCanCreateOutpost(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Player player = effector.asPlayer();
         boolean canCreateOutpost = !player.isAlikeDead() && !player.isCursedWeaponEquipped() && player.getClan() != null;
         Castle castle = CastleManager.getInstance().getCastle(player);
         Fort fort = FortManager.getInstance().getFort(player);
         if (castle == null && fort == null) {
            canCreateOutpost = false;
         }

         if ((fort == null || fort.getResidenceId() != 0) && (castle == null || castle.getResidenceId() != 0)) {
            if ((fort == null || fort.getZone().isActive()) && (castle == null || castle.getZone().isActive())) {
               if (!player.isClanLeader()) {
                  player.sendMessage("You must be a clan leader to construct an outpost or flag.");
                  canCreateOutpost = false;
               } else if (TerritoryWarManager.getInstance().getHQForClan(player.getClan()) != null) {
                  player.sendPacket(SystemMessageId.AN_OUTPOST_OR_HEADQUARTERS_CANNOT_BE_BUILT_BECAUSE_AT_LEAST_ONE_ALREADY_EXISTS);
                  canCreateOutpost = false;
               } else if (TerritoryWarManager.getInstance().getFlagForClan(player.getClan()) != null) {
                  player.sendPacket(SystemMessageId.A_FLAG_IS_ALREADY_BEING_DISPLAYED_ANOTHER_FLAG_CANNOT_BE_DISPLAYED);
                  canCreateOutpost = false;
               } else if (!player.isInsideZone(ZoneId.HQ)) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_SET_UP_A_BASE_HERE);
                  canCreateOutpost = false;
               }
            } else {
               player.sendMessage("You can only construct an outpost or flag on siege field.");
               canCreateOutpost = false;
            }
         } else {
            player.sendMessage("You must be on fort or castle ground to construct an outpost or flag.");
            canCreateOutpost = false;
         }

         return this._value == canCreateOutpost;
      } else {
         return !this._value;
      }
   }
}
