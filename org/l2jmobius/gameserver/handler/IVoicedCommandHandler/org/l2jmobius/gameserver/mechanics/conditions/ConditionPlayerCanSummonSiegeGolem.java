package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSummonSiegeGolem extends Condition {
   private final boolean _value;

   public ConditionPlayerCanSummonSiegeGolem(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Player player = effector.asPlayer();
         boolean canSummonSiegeGolem = true;
         if (player.isAlikeDead() || player.isCursedWeaponEquipped() || player.getClan() == null) {
            canSummonSiegeGolem = false;
         }

         Castle castle = CastleManager.getInstance().getCastle(player);
         Fort fort = FortManager.getInstance().getFort(player);
         if (castle == null && fort == null) {
            canSummonSiegeGolem = false;
         }

         if ((fort == null || fort.getResidenceId() != 0) && (castle == null || castle.getResidenceId() != 0)) {
            if ((castle == null || castle.getSiege().isInProgress()) && (fort == null || fort.getSiege().isInProgress())) {
               if (player.getClanId() == 0
                  || (castle == null || castle.getSiege().getAttackerClan(player.getClanId()) != null)
                     && (fort == null || fort.getSiege().getAttackerClan(player.getClanId()) != null)) {
                  if (SevenSigns.getInstance().checkSummonConditions(player)) {
                     canSummonSiegeGolem = false;
                  }
               } else {
                  player.sendPacket(SystemMessageId.INVALID_TARGET);
                  canSummonSiegeGolem = false;
               }
            } else {
               player.sendPacket(SystemMessageId.INVALID_TARGET);
               canSummonSiegeGolem = false;
            }
         } else {
            player.sendPacket(SystemMessageId.INVALID_TARGET);
            canSummonSiegeGolem = false;
         }

         return this._value == canSummonSiegeGolem;
      } else {
         return !this._value;
      }
   }
}
