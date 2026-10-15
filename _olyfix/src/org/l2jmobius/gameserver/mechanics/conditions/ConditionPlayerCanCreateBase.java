package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ConditionPlayerCanCreateBase extends Condition {
   private final boolean _value;

   public ConditionPlayerCanCreateBase(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effector != null && effector.isPlayer()) {
         Player player = effector.asPlayer();
         boolean canCreateBase = true;
         Clan clan = player.getClan();
         if (player.isAlikeDead() || player.isCursedWeaponEquipped() || clan == null) {
            canCreateBase = false;
         }

         Castle castle = CastleManager.getInstance().getCastle(player);
         Fort fort = FortManager.getInstance().getFort(player);
         if (castle == null && fort == null) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
            sm.addSkillName(skill);
            player.sendPacket(sm);
            canCreateBase = false;
         } else if ((castle == null || castle.getSiege().isInProgress()) && (fort == null || fort.getSiege().isInProgress())) {
            if ((castle == null || castle.getSiege().getAttackerClan(clan) != null) && (fort == null || fort.getSiege().getAttackerClan(clan) != null)) {
               if (!player.isClanLeader()) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
                  sm.addSkillName(skill);
                  player.sendPacket(sm);
                  canCreateBase = false;
               } else if ((castle == null || castle.getSiege().getAttackerClan(clan).getNumFlags() < SiegeManager.getInstance().getFlagMaxCount())
                  && (fort == null || fort.getSiege().getAttackerClan(clan).getNumFlags() < FortSiegeManager.getInstance().getFlagMaxCount())) {
                  if (!player.isInsideZone(ZoneId.HQ)) {
                     player.sendPacket(SystemMessageId.YOU_CANNOT_SET_UP_A_BASE_HERE);
                     canCreateBase = false;
                  }
               } else {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
                  sm.addSkillName(skill);
                  player.sendPacket(sm);
                  canCreateBase = false;
               }
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
               sm.addSkillName(skill);
               player.sendPacket(sm);
               canCreateBase = false;
            }
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
            sm.addSkillName(skill);
            player.sendPacket(sm);
            canCreateBase = false;
         }

         return this._value == canCreateBase;
      } else {
         return !this._value;
      }
   }
}
