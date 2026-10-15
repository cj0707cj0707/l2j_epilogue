package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.util.LocationUtil;

public class SkillChannelizer implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(SkillChannelizer.class.getName());
   private final Creature _channelizer;
   private List<Creature> _channelized;
   private Skill _skill;
   private ScheduledFuture<?> _task = null;

   public SkillChannelizer(Creature channelizer) {
      this._channelizer = channelizer;
   }

   public Creature getChannelizer() {
      return this._channelizer;
   }

   public List<Creature> getChannelized() {
      return this._channelized;
   }

   public boolean hasChannelized() {
      return this._channelized != null;
   }

   public void startChanneling(Skill skill) {
      if (this.isChanneling()) {
         LOGGER.warning("Character: " + this._channelizer + " is attempting to channel skill but he already does!");
      } else {
         this._skill = skill;
         this._task = ThreadPool.scheduleAtFixedRate(this, skill.getChannelingTickInitialDelay(), skill.getChannelingTickInterval());
      }
   }

   public void stopChanneling() {
      if (!this.isChanneling()) {
         LOGGER.warning("Character: " + this._channelizer + " is attempting to stop channel skill but he does not!");
      } else {
         this._task.cancel(true);
         this._task = null;
         if (this._channelized != null) {
            for (Creature creature : this._channelized) {
               creature.getSkillChannelized().removeChannelizer(this._skill.getChannelingSkillId(), this._channelizer);
            }

            this._channelized = null;
         }

         this._skill = null;
      }
   }

   public Skill getSkill() {
      return this._skill;
   }

   public boolean isChanneling() {
      return this._task != null;
   }

   @Override
   public void run() {
      if (this.isChanneling()) {
         Skill skill = this._skill;
         if (skill != null) {
            try {
               if (skill.getMpPerChanneling() > 0) {
                  if (this._channelizer.getCurrentMp() < skill.getMpPerChanneling()) {
                     if (this._channelizer.isPlayer()) {
                        this._channelizer.sendPacket(SystemMessageId.YOUR_SKILL_WAS_REMOVED_DUE_TO_A_LACK_OF_MP);
                     }

                     this._channelizer.abortCast();
                     return;
                  }

                  this._channelizer.reduceCurrentMp(skill.getMpPerChanneling());
               }

               if (skill.getChannelingSkillId() > 0) {
                  Skill baseSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), 1);
                  if (baseSkill == null) {
                     LOGGER.warning(
                        this.getClass().getSimpleName() + ": skill " + skill + " couldn't find effect id skill: " + skill.getChannelingSkillId() + " !"
                     );
                     this._channelizer.abortCast();
                     return;
                  }

                  List<Creature> targetList = new ArrayList<>();

                  for (WorldObject chars : skill.getTargetList(this._channelizer)) {
                     if (chars.isCreature()) {
                        targetList.add(chars.asCreature());
                        chars.asCreature().getSkillChannelized().addChannelizer(skill.getChannelingSkillId(), this._channelizer);
                     }
                  }

                  if (targetList.isEmpty()) {
                     return;
                  }

                  this._channelized = targetList;

                  for (Creature creature : this._channelized) {
                     if (LocationUtil.checkIfInRange(skill.getEffectRange(), this._channelizer, creature, true)
                        && GeoEngine.getInstance().canSeeTarget(this._channelizer, (WorldObject)creature)) {
                        int maxSkillLevel = SkillData.getInstance().getMaxLevel(skill.getChannelingSkillId());
                        int skillLevel = Math.min(creature.getSkillChannelized().getChannerlizersSize(skill.getChannelingSkillId()), maxSkillLevel);
                        if (skillLevel != 0) {
                           BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(skill.getChannelingSkillId());
                           if (info == null || info.getSkill().getLevel() < skillLevel) {
                              Skill channelingSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), skillLevel);
                              if (channelingSkill == null) {
                                 LOGGER.warning(this.getClass().getSimpleName() + ": Non existent channeling skill requested: " + skill);
                                 this._channelizer.abortCast();
                                 return;
                              }

                              if (creature.isPlayable() && this._channelizer.isPlayer() && channelingSkill.hasNegativeEffect()) {
                                 this._channelizer.asPlayer().updatePvPStatus(creature);
                              }

                              channelingSkill.applyEffects(this._channelizer, creature);
                              if (skill.useSpiritShot()) {
                                 this._channelizer
                                    .setChargedShot(
                                       this._channelizer.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS,
                                       false
                                    );
                              } else {
                                 this._channelizer.setChargedShot(ShotType.SOULSHOTS, false);
                              }

                              this._channelizer.rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
                           }

                           this._channelizer
                              .broadcastSkillPacket(new MagicSkillLaunched(this._channelizer, skill.getId(), skill.getLevel(), creature), creature);
                        }
                     }
                  }
               }
            } catch (Exception var10) {
               LOGGER.log(
                  Level.WARNING,
                  "Error while channelizing skill: " + skill + " channelizer: " + this._channelizer + " channelized: " + this._channelized + "; ",
                  (Throwable)var10
               );
            }
         }
      }
   }
}
