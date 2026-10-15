package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.CubicData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.tasks.cubics.CubicAction;
import org.l2jmobius.gameserver.entity.actor.tasks.cubics.CubicDisappear;
import org.l2jmobius.gameserver.entity.actor.tasks.cubics.CubicHeal;
import org.l2jmobius.gameserver.entity.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.entity.cubic.CubicSelectionMode;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.CubicTargetType;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class Cubic {
   private static final Logger LOGGER = Logger.getLogger(Cubic.class.getName());
   public static final int STORM_CUBIC = 1;
   public static final int VAMPIRIC_CUBIC = 2;
   public static final int LIFE_CUBIC = 3;
   public static final int VIPER_CUBIC = 4;
   public static final int POLTERGEIST_CUBIC = 5;
   public static final int BINDING_CUBIC = 6;
   public static final int AQUA_CUBIC = 7;
   public static final int SPARK_CUBIC = 8;
   public static final int ATTRACT_CUBIC = 9;
   public static final int MAX_MAGIC_RANGE = 900;
   public static final int SKILL_CUBIC_HEAL = 4051;
   private final Player _owner;
   private Creature _target;
   private final int _cubicId;
   private final CubicTemplate _template;
   private final int _cubicPower;
   private final int _cubicDelay;
   private final int _cubicSkillChance;
   private final int _cubicMaxCount;
   private final boolean _givenByOther;
   private final List<CubicSkill> _cubicSkills;
   private Future<?> _disappearTask;
   private Future<?> _actionTask;

   public Cubic(
      Player owner, int cubicId, int level, int cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther
   ) {
      this._owner = owner;
      this._cubicId = cubicId;
      this._template = CubicData.getInstance().getCubicTemplate(cubicId, level);
      if (this._template == null) {
         this._cubicPower = cubicPower;
         this._cubicDelay = cubicDelay * 1000;
         this._cubicSkillChance = cubicSkillChance;
         this._cubicMaxCount = cubicMaxCount;
         this._cubicSkills = Collections.emptyList();
         LOGGER.warning("Missing cubic template. cubicId: " + cubicId + " level: " + level + " ownerId: " + owner.getObjectId());
      } else {
         this._cubicPower = cubicPower > 0 ? cubicPower : this._template.getBaseMAtk();
         this._cubicDelay = (cubicDelay > 0 ? cubicDelay : this._template.getDelay()) * 1000;
         this._cubicSkillChance = cubicSkillChance;
         this._cubicMaxCount = cubicMaxCount > -1 ? cubicMaxCount : this._template.getMaxCount();
         this._cubicSkills = this._template.getCubicSkills();
      }

      this._givenByOther = givenByOther;
      int duration = cubicDuration > 0 ? cubicDuration : (this._template != null ? this._template.getDuration() : cubicDuration);
      this._disappearTask = ThreadPool.schedule(new CubicDisappear(this), duration * 1000);
      if (this._template != null && this._template.getTargetType() == CubicTargetType.HEAL) {
         this.doAction();
      }
   }

   public void doAction() {
      if (this._actionTask == null && this._template != null && !this._cubicSkills.isEmpty()) {
         synchronized (this) {
            if (this._actionTask == null) {
               if (this._template.getTargetType() == CubicTargetType.HEAL) {
                  this._actionTask = ThreadPool.scheduleAtFixedRate(new CubicHeal(this), 0L, this._cubicDelay);
               } else {
                  this._actionTask = ThreadPool.scheduleAtFixedRate(new CubicAction(this, this._cubicSkillChance), 0L, this._cubicDelay);
               }
            }
         }
      }
   }

   public int getId() {
      return this._cubicId;
   }

   public Player getOwner() {
      return this._owner;
   }

   public int getCubicPower() {
      return this._cubicPower;
   }

   public Creature getTarget() {
      return this._target;
   }

   public void setTarget(Creature target) {
      this._target = target;
   }

   public CubicTemplate getTemplate() {
      return this._template;
   }

   public List<CubicSkill> getCubicSkills() {
      return this._cubicSkills;
   }

   public CubicSkill getCubicSkill() {
      if (this.getNormalCubicSkillCount() == 0) {
         return null;
      } else {
         CubicSelectionMode selectionMode = this._template != null ? this._template.getSelectionMode() : CubicSelectionMode.RANDOM;
         switch (selectionMode) {
            case ORDERED:
               return this.getOrderedCubicSkill();
            case WEIGHTED:
               return this.getWeightedCubicSkill();
            case RANDOM:
            default:
               return this.getRandomCubicSkill();
         }
      }
   }

   private int getNormalCubicSkillCount() {
      int count = 0;

      for (CubicSkill cubicSkill : this._cubicSkills) {
         if (!cubicSkill.isPriority()) {
            count++;
         }
      }

      return count;
   }

   private CubicSkill getRandomCubicSkill() {
      int index = Rnd.get(this.getNormalCubicSkillCount());

      for (CubicSkill cubicSkill : this._cubicSkills) {
         if (!cubicSkill.isPriority() && index-- == 0) {
            return cubicSkill;
         }
      }

      return null;
   }

   private CubicSkill getOrderedCubicSkill() {
      for (CubicSkill cubicSkill : this._cubicSkills) {
         if (!cubicSkill.isPriority() && (cubicSkill.getTriggerRate() >= 100 || Rnd.get(100) < cubicSkill.getTriggerRate())) {
            return cubicSkill;
         }
      }

      return null;
   }

   private CubicSkill getWeightedCubicSkill() {
      int totalRate = 0;

      for (CubicSkill cubicSkill : this._cubicSkills) {
         if (!cubicSkill.isPriority() && cubicSkill.getTriggerRate() > 0) {
            totalRate += cubicSkill.getTriggerRate();
         }
      }

      if (totalRate <= 0) {
         return this.getRandomCubicSkill();
      } else {
         int chance = Rnd.get(totalRate);

         for (CubicSkill cubicSkillx : this._cubicSkills) {
            if (!cubicSkillx.isPriority() && cubicSkillx.getTriggerRate() > 0) {
               chance -= cubicSkillx.getTriggerRate();
               if (chance < 0) {
                  return cubicSkillx;
               }
            }
         }

         return this.getRandomCubicSkill();
      }
   }

   public int getCubicMaxCount() {
      return this._cubicMaxCount;
   }

   public void stopAction() {
      this._target = null;
      if (this._actionTask != null) {
         this._actionTask.cancel(true);
         this._actionTask = null;
      }
   }

   public void cancelDisappear() {
      if (this._disappearTask != null) {
         this._disappearTask.cancel(true);
         this._disappearTask = null;
      }
   }

   public void getCubicTarget() {
      try {
         this._target = null;
         WorldObject ownerTarget = this._owner.getTarget();
         if (ownerTarget == null) {
            return;
         }

         if (this._owner.isOnEvent()) {
            Player target = ownerTarget.asPlayer();
            if (target != null && (this._owner.getTeam() != target.getTeam() || this._owner.isOnSoloEvent()) && !target.isDead()) {
               this._target = ownerTarget.asCreature();
            }

            return;
         }

         if (this._owner.isInDuel()) {
            Player playerA = DuelManager.getInstance().getDuel(this._owner.getDuelId()).getPlayerA();
            Player playerB = DuelManager.getInstance().getDuel(this._owner.getDuelId()).getPlayerB();
            if (DuelManager.getInstance().getDuel(this._owner.getDuelId()).isPartyDuel()) {
               Party partyA = playerA.getParty();
               Party partyB = playerB.getParty();
               Party partyEnemy = null;
               if (partyA != null) {
                  if (partyA.getMembers().contains(this._owner)) {
                     if (partyB != null) {
                        partyEnemy = partyB;
                     } else {
                        this._target = playerB;
                     }
                  } else {
                     partyEnemy = partyA;
                  }
               } else if (playerA == this._owner) {
                  if (partyB != null) {
                     partyEnemy = partyB;
                  } else {
                     this._target = playerB;
                  }
               } else {
                  this._target = playerA;
               }

               if ((this._target == playerA || this._target == playerB) && this._target == ownerTarget) {
                  return;
               }

               if (partyEnemy != null) {
                  if (partyEnemy.getMembers().contains(ownerTarget)) {
                     this._target = ownerTarget.asCreature();
                  }

                  return;
               }
            }

            if (playerA != this._owner && ownerTarget == playerA) {
               this._target = playerA;
               return;
            }

            if (playerB != this._owner && ownerTarget == playerB) {
               this._target = playerB;
               return;
            }

            this._target = null;
            return;
         }

         if (this._owner.isInOlympiadMode()) {
            if (this._owner.isOlympiadStart() && ownerTarget.isPlayable()) {
               Player targetPlayer = ownerTarget.asPlayer();
               if (targetPlayer != null
                  && targetPlayer.getOlympiadGameId() == this._owner.getOlympiadGameId()
                  && targetPlayer.getOlympiadSide() != this._owner.getOlympiadSide()) {
                  this._target = ownerTarget.asCreature();
               }
            }

            return;
         }

         if (ownerTarget.isCreature() && ownerTarget != this._owner.getSummon() && ownerTarget != this._owner) {
            if (ownerTarget.isAttackable()) {
               Attackable attackable = ownerTarget.asAttackable();
               if (attackable.isInAggroList(this._owner) && !attackable.isDead()) {
                  this._target = ownerTarget.asCreature();
                  return;
               }

               if (this._owner.hasSummon() && attackable.isInAggroList(this._owner.getSummon()) && !attackable.isDead()) {
                  this._target = ownerTarget.asCreature();
                  return;
               }
            }

            Player enemy = null;
            if (this._owner.getPvpFlag() > 0 && !this._owner.isInsideZone(ZoneId.PEACE) || this._owner.isInsideZone(ZoneId.PVP)) {
               if (!ownerTarget.asCreature().isDead()) {
                  enemy = ownerTarget.asPlayer();
               }

               if (enemy != null) {
                  boolean targetIt = true;
                  if (this._owner.getParty() != null) {
                     if (this._owner.getParty().getMembers().contains(enemy)) {
                        targetIt = false;
                     } else if (this._owner.getParty().getCommandChannel() != null && this._owner.getParty().getCommandChannel().getMembers().contains(enemy)) {
                        targetIt = false;
                     }
                  }

                  if (this._owner.getClan() != null && !this._owner.isInsideZone(ZoneId.PVP)) {
                     if (this._owner.getClan().isMember(enemy.getObjectId())) {
                        targetIt = false;
                     }

                     if (this._owner.getAllyId() > 0 && enemy.getAllyId() > 0 && this._owner.getAllyId() == enemy.getAllyId()) {
                        targetIt = false;
                     }
                  }

                  if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(ZoneId.PVP)) {
                     targetIt = false;
                  }

                  if (enemy.isInsideZone(ZoneId.PEACE)) {
                     targetIt = false;
                  }

                  if (this._owner.getSiegeState() > 0 && this._owner.getSiegeState() == enemy.getSiegeState()) {
                     targetIt = false;
                  }

                  if (!enemy.isSpawned()) {
                     targetIt = false;
                  }

                  if (targetIt) {
                     this._target = enemy;
                  }
               }
            }
         }
      } catch (Exception var7) {
         LOGGER.log(Level.WARNING, "Cubic target selection failed. cubicId: " + this._cubicId + " ownerId: " + this._owner.getObjectId(), (Throwable)var7);
      }
   }

   private void applyCubicEffects(Skill skill, Creature target) {
      if (target != null
         && (target == this._owner || !skill.hasNegativeEffect() || !target.isInvul() && (!this._owner.isGM() || this._owner.getAccessLevel().canGiveDamage()))
         )
       {
         if (!target.isInvulAgainst(skill.getId(), skill.getLevel())) {
            boolean addContinuousEffects = skill.isToggle() || skill.isContinuous();
            BuffInfo info = new BuffInfo(this._owner, target, skill);
            skill.applyEffectScope(EffectScope.GENERAL, info, true, addContinuousEffects);
            EffectScope pvpOrPveEffectScope = this._owner.isPlayable() && target.isAttackable()
               ? EffectScope.PVE
               : (this._owner.isPlayable() && target.isPlayable() ? EffectScope.PVP : null);
            skill.applyEffectScope(pvpOrPveEffectScope, info, true, addContinuousEffects);
            skill.applyEffectScope(EffectScope.CHANNELING, info, true, addContinuousEffects);
            if (addContinuousEffects) {
               target.getEffectList().add(info);
            }
         }
      }
   }

   public void useCubicContinuous(Skill skill, List<Creature> targets) {
      for (Creature target : targets) {
         if (target != null && !target.isDead()) {
            if (skill.hasNegativeEffect()) {
               boolean acted = Formulas.calcCubicSkillSuccess(this, target, skill, (byte)0);
               if (!acted) {
                  this._owner.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
                  continue;
               }
            }

            this.applyCubicEffects(skill, target);
            if (target.isPlayer()) {
               Player player = target.asPlayer();
               if (player.isInDuel() && skill.hasNegativeEffect() && this._owner.getDuelId() == player.getDuelId()) {
                  DuelManager.getInstance().onBuff(player, skill);
               }
            }
         }
      }
   }

   public void useCubicMdam(Skill skill, List<Creature> targets) {
      for (Creature target : targets) {
         if (target != null) {
            if (target.isAlikeDead()) {
               if (!target.isPlayer() || !PlayerConfig.FAKE_DEATH_DAMAGE_STAND) {
                  continue;
               }

               target.stopFakeDeath(true);
            }

            boolean mcrit = Formulas.calcMCrit(this._owner.getMCriticalHit(target, skill));
            byte shld = Formulas.calcShldUse(this._owner, target, skill);
            int damage = (int)Formulas.calcMagicDam(this, target, skill, mcrit, shld);
            if (damage > 0) {
               if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                  target.breakAttack();
                  target.breakCast();
               }

               if (target.getStat().calcStat(Stat.VENGEANCE_SKILL_MAGIC_DAMAGE, 0.0, target, skill) > Rnd.get(100)) {
                  int var8 = false;
               } else {
                  this._owner.sendDamageMessage(target, damage, mcrit, false, false);
                  target.reduceCurrentHp(damage, this._owner, skill);
               }
            }
         }
      }
   }

   public void useCubicDrain(Skill skill, List<Creature> targets) {
      for (Creature target : targets) {
         if (!target.isAlikeDead()) {
            boolean mcrit = Formulas.calcMCrit(this._owner.getMCriticalHit(target, skill));
            byte shld = Formulas.calcShldUse(this._owner, target, skill);
            int damage = (int)Formulas.calcMagicDam(this, target, skill, mcrit, shld);
            double hpAdd = 0.4 * damage;
            Player owner = this._owner;
            double hp = owner.getCurrentHp() + hpAdd > owner.getMaxHp() ? owner.getMaxHp() : owner.getCurrentHp() + hpAdd;
            owner.setCurrentHp(hp);
            if (damage > 0 && !target.isDead()) {
               target.reduceCurrentHp(damage, this._owner, skill);
               if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                  target.breakAttack();
                  target.breakCast();
               }

               owner.sendDamageMessage(target, damage, mcrit, false, false);
            }
         }
      }
   }

   public void useCubicDisabler(Skill skill, List<Creature> targets) {
      for (Creature target : targets) {
         if (target != null && !target.isDead()) {
            if (skill.hasEffectType(EffectType.STUN, EffectType.PARALYZE, EffectType.ROOT) && Formulas.calcCubicSkillSuccess(this, target, skill, (byte)0)) {
               this.applyCubicEffects(skill, target);
               if (target.isPlayer()) {
                  Player player = target.asPlayer();
                  if (player.isInDuel() && skill.hasNegativeEffect() && this._owner.getDuelId() == player.getDuelId()) {
                     DuelManager.getInstance().onBuff(player, skill);
                  }
               }
            }

            if (skill.hasEffectType(EffectType.AGGRESSION) && Formulas.calcCubicSkillSuccess(this, target, skill, (byte)0)) {
               if (target.isAttackable()) {
                  target.getAI().notifyActionAggression(this._owner, (int)(150.0 * skill.getPower() / (target.getLevel() + 7)));
               }

               this.applyCubicEffects(skill, target);
            }
         }
      }
   }

   public static boolean isInCubicRange(Creature owner, Creature target) {
      if (owner != null && target != null) {
         long range = 900L;
         long x = owner.getX() - target.getX();
         long y = owner.getY() - target.getY();
         long z = owner.getZ() - target.getZ();
         return x * x + y * y + z * z <= 810000L;
      } else {
         return false;
      }
   }

   public void cubicTargetForHeal() {
      Creature target = null;
      double percentleft = 100.0;
      Party party = this._owner.getParty();
      if (this._owner.isInDuel() && !DuelManager.getInstance().getDuel(this._owner.getDuelId()).isPartyDuel()) {
         party = null;
      }

      if (party != null && !this._owner.isInOlympiadMode()) {
         for (Creature partyMember : party.getMembers()) {
            if (!partyMember.isDead()
               && isInCubicRange(this._owner, partyMember)
               && partyMember.getCurrentHp() < partyMember.getMaxHp()
               && percentleft > partyMember.getCurrentHp() / partyMember.getMaxHp()) {
               percentleft = partyMember.getCurrentHp() / partyMember.getMaxHp();
               target = partyMember;
            }

            Player player = partyMember.asPlayer();
            if (player != null) {
               Summon summon = player.getSummon();
               if (summon != null
                  && !summon.isDead()
                  && isInCubicRange(this._owner, summon)
                  && summon.getCurrentHp() < summon.getMaxHp()
                  && percentleft > summon.getCurrentHp() / summon.getMaxHp()) {
                  percentleft = summon.getCurrentHp() / summon.getMaxHp();
                  target = summon;
               }
            }
         }
      } else {
         if (this._owner.getCurrentHp() < this._owner.getMaxHp()) {
            percentleft = this._owner.getCurrentHp() / this._owner.getMaxHp();
            target = this._owner;
         }

         if (this._owner.hasSummon()
            && !this._owner.getSummon().isDead()
            && this._owner.getSummon().getCurrentHp() < this._owner.getSummon().getMaxHp()
            && percentleft > this._owner.getSummon().getCurrentHp() / this._owner.getSummon().getMaxHp()
            && isInCubicRange(this._owner, this._owner.getSummon())) {
            target = this._owner.getSummon();
         }
      }

      this._target = target;
   }

   public boolean givenByOther() {
      return this._givenByOther;
   }
}
