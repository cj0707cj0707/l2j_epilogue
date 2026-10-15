package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class TerritoryWard extends Attackable {
   public TerritoryWard(NpcTemplate template) {
      super(template);
      this.disableCoreAI(true);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (this.isInvul()) {
         return false;
      } else if (this.getCastle() != null && this.getCastle().getZone().isActive()) {
         Player actingPlayer = attacker.asPlayer();
         if (actingPlayer == null) {
            return false;
         } else {
            return actingPlayer.getSiegeSide() == 0 ? false : !TerritoryWarManager.getInstance().isAllyField(actingPlayer, this.getCastle().getResidenceId());
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      if (this.getCastle() == null) {
         LOGGER.warning("TerritoryWard(" + this.getName() + ") spawned outside Castle Zone!");
      }
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (skill == null && TerritoryWarManager.getInstance().isTWInProgress()) {
         Player actingPlayer = attacker.asPlayer();
         if (actingPlayer != null) {
            if (!actingPlayer.isCombatFlagEquipped()) {
               if (actingPlayer.getSiegeSide() != 0) {
                  if (this.getCastle() != null) {
                     if (!TerritoryWarManager.getInstance().isAllyField(actingPlayer, this.getCastle().getResidenceId())) {
                        super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill) {
   }

   @Override
   public boolean doDie(Creature killer) {
      if (super.doDie(killer) && this.getCastle() != null && TerritoryWarManager.getInstance().isTWInProgress()) {
         if (killer.isPlayer()) {
            Player player = killer.asPlayer();
            if (player.getSiegeSide() > 0 && !player.isCombatFlagEquipped()) {
               player.addItem(ItemProcessType.PICKUP, this.getId() - 23012, 1L, null, false);
            } else {
               TerritoryWarManager.getInstance().getTerritoryWard(this.getId() - 36491).spawnMe();
            }

            SystemMessage sm = new SystemMessage(SystemMessageId.THE_S1_WARD_HAS_BEEN_DESTROYED_C2_NOW_HAS_THE_TERRITORY_WARD);
            sm.addString(this.getName().replace(" Ward", ""));
            sm.addPcName(player);
            TerritoryWarManager.getInstance().announceToParticipants(sm, 0, 0);
         } else {
            TerritoryWarManager.getInstance().getTerritoryWard(this.getId() - 36491).spawnMe();
         }

         this.decayMe();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onForcedAttack(Player player) {
      this.onAction(player);
   }

   @Override
   public void onAction(Player player, boolean interact) {
      if (player != null && this.canTarget(player)) {
         if (this != player.getTarget()) {
            player.setTarget(this);
         } else if (interact) {
            if (this.isAutoAttackable(player) && Math.abs(player.getZ() - this.getZ()) < 100) {
               player.getAI().setIntentionAttack(this);
            } else {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            }
         }
      }
   }
}
