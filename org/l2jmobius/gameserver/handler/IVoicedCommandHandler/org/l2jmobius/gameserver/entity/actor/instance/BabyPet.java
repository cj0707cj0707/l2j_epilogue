package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class BabyPet extends Pet {
   private static final int BUFF_CONTROL = 5771;
   private static final int AWAKENING = 5753;
   protected List<SkillHolder> _buffs = null;
   protected SkillHolder _majorHeal = null;
   protected SkillHolder _minorHeal = null;
   protected SkillHolder _recharge = null;
   private Future<?> _castTask;
   protected boolean _bufferMode = true;

   public BabyPet(NpcTemplate template, Player owner, Item control) {
      super(template, owner, control);
      this.setInstanceType(InstanceType.BabyPet);
   }

   public BabyPet(NpcTemplate template, Player owner, Item control, byte level) {
      super(template, owner, control, level);
      this.setInstanceType(InstanceType.BabyPet);
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      double healPower = 0.0;

      for (PetData.PetSkillLearn psl : PetDataTable.getInstance().getPetData(this.getId()).getAvailableSkills()) {
         int id = psl.getSkillId();
         int level = PetDataTable.getInstance().getPetData(this.getId()).getAvailableLevel(id, this.getLevel());
         if (level != 0) {
            Skill skill = SkillData.getInstance().getSkill(id, level);
            if (skill != null && skill.getId() != 5771 && skill.getId() != 5753) {
               if (skill.hasEffectType(EffectType.MANAHEAL_BY_LEVEL)) {
                  this._recharge = new SkillHolder(skill);
               } else if (skill.hasEffectType(EffectType.HEAL)) {
                  if (healPower == 0.0) {
                     this._majorHeal = new SkillHolder(skill);
                     this._minorHeal = this._majorHeal;
                     healPower = skill.getPower();
                  } else if (skill.getPower() > healPower) {
                     this._majorHeal = new SkillHolder(skill);
                  } else {
                     this._minorHeal = new SkillHolder(skill);
                  }
               } else if (skill.isContinuous() && !skill.isDebuff()) {
                  if (this._buffs == null) {
                     this._buffs = new ArrayList<>();
                  }

                  this._buffs.add(new SkillHolder(skill));
               }
            }
         }
      }

      this.startCastTask();
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         this.stopCastTask();
         this.abortCast();
         return true;
      }
   }

   @Override
   public synchronized void unSummon(Player owner) {
      this.stopCastTask();
      this.abortCast();
      super.unSummon(owner);
   }

   @Override
   public void doRevive() {
      super.doRevive();
      this.startCastTask();
   }

   @Override
   public void onDecay() {
      super.onDecay();
      if (this._buffs != null) {
         this._buffs.clear();
      }
   }

   private void startCastTask() {
      if (this._majorHeal != null || this._buffs != null || this._recharge != null && this._castTask == null && !this.isDead()) {
         this._castTask = ThreadPool.scheduleAtFixedRate(new BabyPet.CastTask(this), 3000L, 2000L);
      }
   }

   @Override
   public void switchMode() {
      this._bufferMode = !this._bufferMode;
   }

   public boolean isInSupportMode() {
      return this._bufferMode;
   }

   private void stopCastTask() {
      if (this._castTask != null) {
         this._castTask.cancel(false);
         this._castTask = null;
      }
   }

   protected void castSkill(Skill skill) {
      boolean previousFollowStatus = this.getFollowStatus();
      if (previousFollowStatus || this.isInsideRadius3D(this.getOwner(), skill.getCastRange())) {
         this.setTarget(this.getOwner());
         this.useMagic(skill, false, false);
         SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_PET_USES_S1);
         msg.addSkillName(skill);
         this.sendPacket(msg);
         if (previousFollowStatus != this.getFollowStatus()) {
            this.setFollowStatus(previousFollowStatus);
         }
      }
   }

   private class CastTask implements Runnable {
      private final BabyPet _baby;
      private final List<Skill> _currentBuffs;

      public CastTask(BabyPet baby) {
         Objects.requireNonNull(BabyPet.this);
         super();
         this._currentBuffs = new ArrayList<>();
         this._baby = baby;
      }

      @Override
      public void run() {
         Player owner = this._baby.getOwner();
         if (owner != null && !owner.isDead() && !owner.isInvul()) {
            if (!this._baby.isDead()
               && !this._baby.isCastingNow()
               && !this._baby.isBetrayed()
               && !this._baby.isMuted()
               && !this._baby.isOutOfControl()
               && BabyPet.this._bufferMode
               && this._baby.getAI().getIntention() != Intention.CAST) {
               Skill skill = null;
               if (BabyPet.this._majorHeal != null) {
                  double hpPercent = owner.getCurrentHp() / owner.getMaxHp();
                  boolean isImprovedBaby = BabyPet.this.isInCategory(CategoryType.BABY_PET_GROUP);
                  if ((!isImprovedBaby || !(hpPercent < 0.3)) && (isImprovedBaby || !(hpPercent < 0.15))) {
                     if (BabyPet.this._majorHeal.getSkill() != BabyPet.this._minorHeal.getSkill()
                        && (isImprovedBaby && hpPercent < 0.7 || !isImprovedBaby && hpPercent < 0.8)) {
                        skill = BabyPet.this._minorHeal.getSkill();
                        if (!this._baby.isSkillDisabled(skill) && Rnd.get(100) <= 25 && this._baby.getCurrentMp() >= skill.getMpConsume()) {
                           BabyPet.this.castSkill(skill);
                           return;
                        }
                     }
                  } else {
                     skill = BabyPet.this._majorHeal.getSkill();
                     if (!this._baby.isSkillDisabled(skill) && Rnd.get(100) <= 75 && this._baby.getCurrentMp() >= skill.getMpConsume()) {
                        BabyPet.this.castSkill(skill);
                        return;
                     }
                  }
               }

               if (!this._baby.isAffectedBySkill(5771)) {
                  if (BabyPet.this._buffs != null && !BabyPet.this._buffs.isEmpty()) {
                     for (SkillHolder buff : BabyPet.this._buffs) {
                        skill = buff.getSkill();
                        if (!this._baby.isSkillDisabled(skill) && !(this._baby.getCurrentMp() < skill.getMpConsume())) {
                           BuffInfo buffInfo = owner.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
                           if ((buffInfo == null || skill.getAbnormalLevel() > buffInfo.getSkill().getAbnormalLevel())
                              && (
                                 owner.getEffectList().getBlockedAbnormalTypes() == null
                                    || !owner.getEffectList().getBlockedAbnormalTypes().contains(skill.getAbnormalType())
                              )) {
                              this._currentBuffs.add(skill);
                           }
                        }
                     }
                  }

                  if (!this._currentBuffs.isEmpty()) {
                     skill = this._currentBuffs.get(Rnd.get(this._currentBuffs.size()));
                     BabyPet.this.castSkill(skill);
                     this._currentBuffs.clear();
                     return;
                  }
               }

               if (BabyPet.this._recharge != null && owner.isInCombat() && owner.getCurrentMp() / owner.getMaxMp() < 0.6 && Rnd.get(100) <= 60) {
                  skill = BabyPet.this._recharge.getSkill();
                  if (!this._baby.isSkillDisabled(skill) && this._baby.getCurrentMp() >= skill.getMpConsume()) {
                     BabyPet.this.castSkill(skill);
                  }
               }
            }
         }
      }
   }
}
