package org.l2jmobius.gameserver.entity.actor.tasks.cubics;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

public class CubicHeal implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(CubicHeal.class.getName());
   private final Cubic _cubic;
   private final AtomicInteger _currentCount = new AtomicInteger();

   public CubicHeal(Cubic cubic) {
      this._cubic = cubic;
   }

   @Override
   public void run() {
      if (this._cubic != null) {
         if (!this._cubic.getOwner().isDead() && this._cubic.getOwner().isOnline()) {
            try {
               if (this._cubic.getCubicMaxCount() > -1 && this._currentCount.get() >= this._cubic.getCubicMaxCount()) {
                  this._cubic.stopAction();
                  if (this._cubic.getTemplate() != null && this._cubic.getTemplate().getUseUp() > 0) {
                     this._cubic.getOwner().removeCubic(this._cubic.getId());
                     this._cubic.getOwner().broadcastUserInfo();
                  }

                  return;
               }

               this._cubic.cubicTargetForHeal();
               Creature target = this._cubic.getTarget();
               if (target == null || target.isDead()) {
                  return;
               }

               CubicSkill cubicSkill = this.getHealSkill(target);
               if (cubicSkill == null) {
                  return;
               }

               Skill skill = cubicSkill.getSkill();
               if (skill == null || target.getMaxHp() - target.getCurrentHp() <= skill.getPower()) {
                  return;
               }

               skill.activateSkill(this._cubic, Collections.singletonList(target));
               this._cubic.getOwner().broadcastPacket(new MagicSkillUse(this._cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
               this._currentCount.incrementAndGet();
            } catch (Exception var4) {
               LOGGER.log(
                  Level.SEVERE, "Cubic heal failed. cubicId: " + this._cubic.getId() + " ownerId: " + this._cubic.getOwner().getObjectId(), (Throwable)var4
               );
            }
         } else {
            this._cubic.getOwner().removeCubic(this._cubic.getId());
            this._cubic.getOwner().broadcastUserInfo();
         }
      }
   }

   private CubicSkill getHealSkill(Creature target) {
      if (this._cubic.getTemplate() != null && !this._cubic.getTemplate().validateConditions(this._cubic, this._cubic.getOwner(), target)) {
         return null;
      } else {
         for (CubicSkill cubicSkill : this._cubic.getCubicSkills()) {
            Skill skill = cubicSkill.getSkill();
            if (skill != null && skill.getId() == 4051 && cubicSkill.validateConditions(this._cubic, this._cubic.getOwner(), target)) {
               return cubicSkill;
            }
         }

         return null;
      }
   }
}
