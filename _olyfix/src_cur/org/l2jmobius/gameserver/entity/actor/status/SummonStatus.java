package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.LocationUtil;

public class SummonStatus extends PlayableStatus {
   public SummonStatus(Summon activeChar) {
      super(activeChar);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   @Override
   public void reduceHp(double amount, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption) {
      Summon summon = this.getActiveChar();
      if (attacker != null && !summon.isDead()) {
         Player attackerPlayer = attacker.asPlayer();
         if (attackerPlayer != null && (summon.getOwner() == null || summon.getOwner().getDuelId() != attackerPlayer.getDuelId())) {
            attackerPlayer.setDuelState(4);
         }

         double value = amount;
         Player caster = summon.getTransferingDamageTo();
         if (summon.getOwner().getParty() != null) {
            if (caster != null
               && LocationUtil.checkIfInRange(1000, summon, caster, true)
               && !caster.isDead()
               && summon.getParty().getMembers().contains(caster)) {
               int transferDmg = Math.min(
                  (int)caster.getCurrentHp() - 1, (int)amount * (int)summon.getStat().calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0.0, null, null) / 100
               );
               if (transferDmg > 0) {
                  int membersInRange = 0;

                  for (Player member : caster.getParty().getMembers()) {
                     if (LocationUtil.checkIfInRange(1000, member, caster, false) && member != caster) {
                        membersInRange++;
                     }
                  }

                  if (attacker.isPlayable() && caster.getCurrentCp() > 0.0) {
                     if (caster.getCurrentCp() > transferDmg) {
                        caster.getStatus().reduceCp(transferDmg);
                     } else {
                        transferDmg = (int)(transferDmg - caster.getCurrentCp());
                        caster.getStatus().reduceCp((int)caster.getCurrentCp());
                     }
                  }

                  if (membersInRange > 0) {
                     caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
                     value = amount - transferDmg;
                  }
               }
            }
         } else if (caster != null && caster == summon.getOwner() && LocationUtil.checkIfInRange(1000, summon, caster, true) && !caster.isDead()) {
            int transferDmg = Math.min(
               (int)caster.getCurrentHp() - 1, (int)amount * (int)summon.getStat().calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0.0, null, null) / 100
            );
            if (transferDmg > 0) {
               if (attacker.isPlayable() && caster.getCurrentCp() > 0.0) {
                  if (caster.getCurrentCp() > transferDmg) {
                     caster.getStatus().reduceCp(transferDmg);
                  } else {
                     transferDmg = (int)(transferDmg - caster.getCurrentCp());
                     caster.getStatus().reduceCp((int)caster.getCurrentCp());
                  }
               }

               caster.reduceCurrentHp(transferDmg, attacker, null);
               value = amount - transferDmg;
            }
         }

         super.reduceHp(value, attacker, awake, isDOT, isHPConsumption);
      }
   }

   public Summon getActiveChar() {
      return super.getActiveChar().asSummon();
   }
}
