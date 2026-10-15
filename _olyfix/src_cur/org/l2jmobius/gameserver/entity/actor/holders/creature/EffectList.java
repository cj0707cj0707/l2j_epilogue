package org.l2jmobius.gameserver.entity.actor.holders.creature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import org.l2jmobius.gameserver.network.serverpackets.PartySpelled;
import org.l2jmobius.gameserver.network.serverpackets.ShortBuffStatusUpdate;

public class EffectList {
   private static final Logger LOGGER = Logger.getLogger(EffectList.class.getName());
   private final Queue<BuffInfo> _buffs = new ConcurrentLinkedQueue<>();
   private final Queue<BuffInfo> _dances = new ConcurrentLinkedQueue<>();
   private final Queue<BuffInfo> _toggles = new ConcurrentLinkedQueue<>();
   private final Queue<BuffInfo> _debuffs = new ConcurrentLinkedQueue<>();
   private final Queue<BuffInfo> _passives = new ConcurrentLinkedQueue<>();
   private final Map<AbnormalType, BuffInfo> _stackedEffects = new ConcurrentHashMap<>();
   private final Set<AbnormalType> _blockedAbnormalTypes = EnumSet.noneOf(AbnormalType.class);
   private BuffInfo _shortBuff = null;
   private volatile boolean _hasBuffsRemovedOnAnyAction = false;
   private volatile boolean _hasBuffsRemovedOnDamage = false;
   private volatile boolean _hasDebuffsRemovedOnDamage = false;
   private int _effectFlags;
   private boolean _partyOnly = false;
   private final Creature _owner;
   private final AtomicInteger _hiddenBuffs = new AtomicInteger();
   private ScheduledFuture<?> _updateEffectIconTask;

   public EffectList(Creature owner) {
      this._owner = owner;
   }

   public Queue<BuffInfo> getBuffs() {
      return this._buffs;
   }

   public Queue<BuffInfo> getDances() {
      return this._dances;
   }

   public Queue<BuffInfo> getToggles() {
      return this._toggles;
   }

   public Queue<BuffInfo> getDebuffs() {
      return this._debuffs;
   }

   public Queue<BuffInfo> getPassives() {
      return this._passives;
   }

   public List<BuffInfo> getEffects() {
      if (this.isEmpty()) {
         return Collections.emptyList();
      } else {
         List<BuffInfo> buffs = new ArrayList<>();
         if (this.hasBuffs()) {
            buffs.addAll(this._buffs);
         }

         if (this.hasDances()) {
            buffs.addAll(this._dances);
         }

         if (this.hasToggles()) {
            buffs.addAll(this._toggles);
         }

         if (this.hasDebuffs()) {
            buffs.addAll(this._debuffs);
         }

         return buffs;
      }
   }

   private Queue<BuffInfo> getEffectList(Skill skill) {
      if (skill == null) {
         return null;
      } else {
         Queue<BuffInfo> effects;
         if (skill.isPassive()) {
            effects = this._passives;
         } else if (skill.isDebuff()) {
            effects = this._debuffs;
         } else if (skill.isDance()) {
            effects = this._dances;
         } else if (skill.isToggle()) {
            effects = this._toggles;
         } else {
            effects = this._buffs;
         }

         return effects;
      }
   }

   public BuffInfo getFirstEffect(EffectType type) {
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (info != null) {
               for (AbstractEffect effect : info.getEffects()) {
                  if (effect != null && effect.getEffectType() == type) {
                     return info;
                  }
               }
            }
         }
      }

      if (this.hasDances()) {
         for (BuffInfo infox : this._dances) {
            if (infox != null) {
               for (AbstractEffect effectx : infox.getEffects()) {
                  if (effectx != null && effectx.getEffectType() == type) {
                     return infox;
                  }
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo infoxx : this._toggles) {
            if (infoxx != null) {
               for (AbstractEffect effectxx : infoxx.getEffects()) {
                  if (effectxx != null && effectxx.getEffectType() == type) {
                     return infoxx;
                  }
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infoxxx : this._debuffs) {
            if (infoxxx != null) {
               for (AbstractEffect effectxxx : infoxxx.getEffects()) {
                  if (effectxxx != null && effectxxx.getEffectType() == type) {
                     return infoxxx;
                  }
               }
            }
         }
      }

      return null;
   }

   public boolean isAffectedBySkill(int skillId) {
      return this.getBuffInfoBySkillId(skillId) != null;
   }

   public BuffInfo getBuffInfoBySkillId(int skillId) {
      BuffInfo info = null;
      if (this.hasBuffs()) {
         for (BuffInfo b : this._buffs) {
            if (b.getSkill().getId() == skillId) {
               info = b;
               break;
            }
         }
      }

      if (this.hasDances() && info == null) {
         for (BuffInfo bx : this._dances) {
            if (bx.getSkill().getId() == skillId) {
               info = bx;
               break;
            }
         }
      }

      if (this.hasToggles() && info == null) {
         for (BuffInfo bxx : this._toggles) {
            if (bxx.getSkill().getId() == skillId) {
               info = bxx;
               break;
            }
         }
      }

      if (this.hasDebuffs() && info == null) {
         for (BuffInfo bxxx : this._debuffs) {
            if (bxxx.getSkill().getId() == skillId) {
               info = bxxx;
               break;
            }
         }
      }

      if (this.hasPassives() && info == null) {
         for (BuffInfo bxxxx : this._passives) {
            if (bxxxx.getSkill().getId() == skillId) {
               info = bxxxx;
               break;
            }
         }
      }

      return info;
   }

   public BuffInfo getBuffInfoByAbnormalType(AbnormalType type) {
      return this._stackedEffects.get(type);
   }

   public void addBlockedAbnormalTypes(Set<AbnormalType> blockedAbnormalTypes) {
      this._blockedAbnormalTypes.addAll(blockedAbnormalTypes);
   }

   public boolean removeBlockedAbnormalTypes(Set<AbnormalType> blockedBuffSlots) {
      return this._blockedAbnormalTypes.removeAll(blockedBuffSlots);
   }

   public Set<AbnormalType> getBlockedAbnormalTypes() {
      return Collections.unmodifiableSet(this._blockedAbnormalTypes);
   }

   public BuffInfo getShortBuff() {
      return this._shortBuff;
   }

   public void shortBuffStatusUpdate(BuffInfo info) {
      if (this._owner.isPlayer()) {
         this._shortBuff = info;
         if (info != null) {
            this._owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getTime()));
         } else {
            this._owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
         }
      }
   }

   private boolean doesStack(Skill skill) {
      AbnormalType type = skill.getAbnormalType();
      if (!type.isNone() && !this.isEmpty()) {
         for (BuffInfo info : this.getEffectList(skill)) {
            if (info != null && info.getSkill().getAbnormalType() == type) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public int getBuffCount() {
      return this.hasBuffs() ? this._buffs.size() - this._hiddenBuffs.get() - (this._shortBuff != null ? 1 : 0) : 0;
   }

   public int getDanceCount() {
      return this.hasDances() ? this._dances.size() : 0;
   }

   public int getHiddenBuffsCount() {
      return this._hiddenBuffs.get();
   }

   protected void stopAndRemove(BuffInfo info) {
      this.stopAndRemove(true, SkillFinishType.REMOVED, info, this.getEffectList(info.getSkill()));
   }

   protected void stopAndRemove(boolean broadcast, BuffInfo info, Queue<BuffInfo> effects) {
      this.stopAndRemove(broadcast, SkillFinishType.REMOVED, info, effects);
   }

   private void stopAndRemove(boolean broadcast, SkillFinishType type, BuffInfo info, Queue<BuffInfo> buffs) {
      if (info != null) {
         buffs.remove(info);
         info.stopAllEffects(type, broadcast);
         if (!info.isInUse()) {
            this._hiddenBuffs.decrementAndGet();
         } else {
            this._stackedEffects.remove(info.getSkill().getAbnormalType());
         }

         if (info.getSkill().isAbnormalInstant() && this.hasBuffs()) {
            for (BuffInfo buff : this._buffs) {
               if (buff != null && buff.getSkill().getAbnormalType() == info.getSkill().getAbnormalType() && !buff.isInUse()) {
                  buff.setInUse(true);
                  buff.addStats();
                  this._stackedEffects.put(buff.getSkill().getAbnormalType(), buff);
                  this._hiddenBuffs.decrementAndGet();
                  break;
               }
            }
         }

         if (type != SkillFinishType.REMOVED) {
            info.getSkill().applyEffectScope(EffectScope.END, info, true, false);
         }
      }
   }

   public void stopAllEffects() {
      this.stopAllBuffs(false);
      this.stopAllDances(false);
      this.stopAllToggles(false);
      this.stopAllDebuffs(false);
      this._stackedEffects.clear();
      this.updateEffectList(true);
      this._owner.updateAbnormalEffect();
   }

   public void stopAllEffectsExceptThoseThatLastThroughDeath() {
      boolean update = false;
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (!info.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, info, this._buffs);
            }
         }

         update = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infox : this._debuffs) {
            if (!infox.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, infox, this._debuffs);
            }
         }

         update = true;
      }

      if (this.hasDances()) {
         for (BuffInfo infoxx : this._dances) {
            if (!infoxx.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, infoxx, this._dances);
            }
         }

         update = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo infoxxx : this._toggles) {
            if (!infoxxx.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, infoxxx, this._toggles);
            }
         }

         update = true;
      }

      this.updateEffectList(update);
   }

   public void stopAllEffectsWithoutExclusions(boolean update, boolean broadcast) {
      for (BuffInfo info : this._buffs) {
         this.stopAndRemove(broadcast, info, this._buffs);
      }

      for (BuffInfo info : this._dances) {
         this.stopAndRemove(broadcast, info, this._dances);
      }

      for (BuffInfo info : this._toggles) {
         this.stopAndRemove(broadcast, info, this._toggles);
      }

      for (BuffInfo info : this._debuffs) {
         this.stopAndRemove(broadcast, info, this._debuffs);
      }

      for (BuffInfo info : this._passives) {
         this.stopAndRemove(broadcast, info, this._passives);
      }

      if (update) {
         this.updateEffectList(broadcast);
      }
   }

   public void stopAllEffectsNotStayOnSubclassChange() {
      boolean update = false;
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (!info.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, info, this._buffs);
            }
         }

         update = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infox : this._debuffs) {
            if (!infox.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, infox, this._debuffs);
            }
         }

         update = true;
      }

      if (this.hasDances()) {
         for (BuffInfo infoxx : this._dances) {
            if (!infoxx.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, infoxx, this._dances);
            }
         }

         update = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo infoxxx : this._toggles) {
            if (!infoxxx.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, infoxxx, this._toggles);
            }
         }

         update = true;
      }

      this.updateEffectList(update);
   }

   public void stopAllBuffs(boolean update) {
      if (this.hasBuffs()) {
         this._buffs.forEach(b -> this.stopAndRemove(update, b, this._buffs));
      }

      this.updateEffectList(update);
   }

   public void stopAllToggles() {
      this.stopAllToggles(true);
   }

   public void stopAllToggles(boolean update) {
      if (this.hasToggles()) {
         this._toggles.forEach(b -> this.stopAndRemove(update, b, this._toggles));
         this.updateEffectList(update);
      }
   }

   public void stopAllDances(boolean update) {
      if (this.hasDances()) {
         this._dances.forEach(b -> this.stopAndRemove(update, b, this._dances));
         this.updateEffectList(update);
      }
   }

   public void stopAllDebuffs(boolean update) {
      if (this.hasDebuffs()) {
         this._debuffs.forEach(b -> this.stopAndRemove(update, b, this._debuffs));
         this.updateEffectList(update);
      }
   }

   public void stopEffects(EffectType type) {
      boolean update = false;
      Consumer<BuffInfo> action = infox -> {
         for (AbstractEffect effect : infox.getEffects()) {
            if (effect != null && effect.getEffectType() == type) {
               this.stopAndRemove(infox);
            }
         }
      };
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (info != null) {
               action.accept(info);
            }
         }

         update = true;
      }

      if (this.hasDances()) {
         for (BuffInfo infox : this._dances) {
            if (infox != null) {
               action.accept(infox);
            }
         }

         update = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo infoxx : this._toggles) {
            if (infoxx != null) {
               action.accept(infoxx);
            }
         }

         update = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infoxxx : this._debuffs) {
            if (infoxxx != null) {
               action.accept(infoxxx);
            }
         }

         update = true;
      }

      this.updateEffectList(update);
   }

   public void stopSkillEffects(SkillFinishType type, int skillId) {
      BuffInfo info = this.getBuffInfoBySkillId(skillId);
      if (info != null) {
         this.remove(type, info);
      }
   }

   public void stopSkillEffects(SkillFinishType type, Skill skill) {
      if (skill != null) {
         this.stopSkillEffects(type, skill.getId());
      }
   }

   public boolean stopSkillEffects(SkillFinishType removeType, AbnormalType abnormalType) {
      BuffInfo old = this._stackedEffects.remove(abnormalType);
      if (old != null) {
         this.stopSkillEffects(removeType, old.getSkill());
         return true;
      } else {
         return false;
      }
   }

   public void stopEffectsOnAction() {
      if (this._hasBuffsRemovedOnAnyAction) {
         boolean update = false;
         if (this.hasBuffs()) {
            for (BuffInfo info : this._buffs) {
               if (info.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, info, this._buffs);
               }
            }

            update = true;
         }

         if (this.hasDebuffs()) {
            for (BuffInfo infox : this._debuffs) {
               if (infox.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, infox, this._debuffs);
               }
            }

            update = true;
         }

         if (this.hasDances()) {
            for (BuffInfo infoxx : this._dances) {
               if (infoxx.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, infoxx, this._dances);
               }
            }

            update = true;
         }

         if (this.hasToggles()) {
            for (BuffInfo infoxxx : this._toggles) {
               if (infoxxx.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, infoxxx, this._toggles);
               }
            }

            update = true;
         }

         this.updateEffectList(update);
      }
   }

   public void stopEffectsOnDamage(boolean awake) {
      if (awake) {
         boolean update = false;
         if (this._hasBuffsRemovedOnDamage) {
            if (this.hasBuffs()) {
               for (BuffInfo info : this._buffs) {
                  if (info != null && info.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, info, this._buffs);
                  }
               }

               update = true;
            }

            if (this.hasDances()) {
               for (BuffInfo infox : this._dances) {
                  if (infox != null && infox.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, infox, this._dances);
                  }
               }

               update = true;
            }

            if (this.hasToggles()) {
               for (BuffInfo infoxx : this._toggles) {
                  if (infoxx != null && infoxx.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, infoxx, this._toggles);
                  }
               }

               update = true;
            }
         }

         if (this._hasDebuffsRemovedOnDamage && this.hasDebuffs()) {
            for (BuffInfo infoxxx : this._debuffs) {
               if (infoxxx != null && infoxxx.getSkill().isRemovedOnDamage()) {
                  this.stopAndRemove(true, infoxxx, this._debuffs);
               }
            }

            update = true;
         }

         this.updateEffectList(update);
      }
   }

   public void updateEffectIcons(boolean partyOnly) {
      if (partyOnly) {
         this._partyOnly = true;
      }

      this.updateEffectList(true);
   }

   public boolean isEmpty() {
      return !this.hasBuffs() && !this.hasDances() && !this.hasDebuffs() && !this.hasToggles();
   }

   public boolean hasBuffs() {
      return !this._buffs.isEmpty();
   }

   public boolean hasDances() {
      return !this._dances.isEmpty();
   }

   public boolean hasToggles() {
      return !this._toggles.isEmpty();
   }

   public boolean hasDebuffs() {
      return !this._debuffs.isEmpty();
   }

   public boolean hasPassives() {
      return !this._passives.isEmpty();
   }

   public void forEach(Function<BuffInfo, Boolean> function, boolean dances) {
      boolean update = false;
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            update |= function.apply(info);
         }
      }

      if (dances && this.hasDances()) {
         for (BuffInfo info : this._dances) {
            update |= function.apply(info);
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo info : this._toggles) {
            update |= function.apply(info);
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo info : this._debuffs) {
            update |= function.apply(info);
         }
      }

      this.updateEffectList(update);
   }

   public void remove(SkillFinishType type, BuffInfo info) {
      if (info != null) {
         this.stopAndRemove(true, type, info, this.getEffectList(info.getSkill()));
         this.updateEffectList(true);
      }
   }

   public void add(BuffInfo info) {
      if (info != null) {
         Skill skill = info.getSkill();
         if (!this._blockedAbnormalTypes.contains(skill.getAbnormalType())) {
            if (skill.isPassive()) {
               if (!skill.getAbnormalType().isNone()) {
                  LOGGER.warning("Passive " + skill + " with abnormal type: " + skill.getAbnormalType() + "!");
               }

               if (skill.checkCondition(info.getEffector(), info.getEffected(), false)) {
                  for (BuffInfo b : this._passives) {
                     if (b.getSkill().getId() == skill.getId()) {
                        b.setInUse(false);
                        b.removeStats();
                        this._passives.remove(b);
                     }
                  }

                  this._passives.add(info);
                  info.initializeEffects();
               }
            } else if (!info.getEffected().isDead()) {
               if (skill.getAbnormalType().isNone()) {
                  this.stopSkillEffects(SkillFinishType.NORMAL, skill);
               } else {
                  synchronized (this) {
                     if (this._stackedEffects.containsKey(skill.getAbnormalType())) {
                        BuffInfo stackedInfo = this._stackedEffects.get(skill.getAbnormalType());
                        if (stackedInfo == null || skill.getAbnormalLevel() < stackedInfo.getSkill().getAbnormalLevel()) {
                           return;
                        }

                        if (skill.isAbnormalInstant()) {
                           if (stackedInfo.getSkill().isAbnormalInstant()) {
                              this.stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
                              stackedInfo = this._stackedEffects.get(skill.getAbnormalType());
                           }

                           if (stackedInfo != null) {
                              stackedInfo.setInUse(false);
                              stackedInfo.removeStats();
                              this._hiddenBuffs.incrementAndGet();
                           }
                        } else {
                           if (stackedInfo.getSkill().isAbnormalInstant()) {
                              this.stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
                           }

                           this.stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
                        }
                     }

                     this._stackedEffects.put(skill.getAbnormalType(), info);
                  }
               }

               Queue<BuffInfo> effects = this.getEffectList(skill);
               if (!skill.isDebuff() && !skill.isToggle() && !skill.is7Signs() && !this.doesStack(skill)) {
                  int buffsToRemove = -1;
                  if (skill.isDance()) {
                     buffsToRemove = this.getDanceCount() - PlayerConfig.DANCES_MAX_AMOUNT;
                  } else if (!skill.isHealingPotionSkill()) {
                     buffsToRemove = this.getBuffCount() - this._owner.getStat().getMaxBuffCount();
                  }

                  for (BuffInfo bi : effects) {
                     if (buffsToRemove < 0) {
                        break;
                     }

                     if (bi.isInUse()) {
                        this.stopAndRemove(true, bi, effects);
                        buffsToRemove--;
                     }
                  }
               }

               effects.add(info);
               info.initializeEffects();
               this.updateEffectList(true);
            }
         }
      }
   }

   private void updateEffectIcons() {
      if (this._owner != null) {
         this.updateEffectFlags();
         if (this._owner.isPlayable()) {
            if (this._updateEffectIconTask == null) {
               this._updateEffectIconTask = ThreadPool.schedule(() -> {
                  AbnormalStatusUpdate asu = null;
                  PartySpelled ps = null;
                  PartySpelled psSummon = null;
                  ExOlympiadSpelledInfo os = null;
                  boolean isSummon = false;
                  if (this._owner.isPlayer()) {
                     if (this._partyOnly) {
                        this._partyOnly = false;
                     } else {
                        asu = new AbnormalStatusUpdate();
                     }

                     if (this._owner.isInParty()) {
                        ps = new PartySpelled(this._owner);
                     }

                     Player player = this._owner.asPlayer();
                     if (player.isInOlympiadMode() && player.isOlympiadStart()) {
                        os = new ExOlympiadSpelledInfo(player);
                     }
                  } else if (this._owner.isSummon()) {
                     isSummon = true;
                     ps = new PartySpelled(this._owner);
                     psSummon = new PartySpelled(this._owner);
                  }

                  if (this.hasBuffs()) {
                     for (BuffInfo info : this._buffs) {
                        if (info.getSkill().isHealingPotionSkill()) {
                           this.shortBuffStatusUpdate(info);
                        } else {
                           this.addIcon(info, asu, ps, psSummon, os, isSummon);
                        }
                     }
                  }

                  if (this.hasDances()) {
                     for (BuffInfo infox : this._dances) {
                        this.addIcon(infox, asu, ps, psSummon, os, isSummon);
                     }
                  }

                  if (this.hasToggles()) {
                     for (BuffInfo infox : this._toggles) {
                        this.addIcon(infox, asu, ps, psSummon, os, isSummon);
                     }
                  }

                  if (this.hasDebuffs()) {
                     for (BuffInfo infox : this._debuffs) {
                        this.addIcon(infox, asu, ps, psSummon, os, isSummon);
                     }
                  }

                  if (asu != null) {
                     this._owner.sendPacket(asu);
                  }

                  if (ps != null) {
                     if (this._owner.isSummon()) {
                        Player summonOwner = this._owner.asSummon().getOwner();
                        if (summonOwner != null) {
                           if (summonOwner.isInParty()) {
                              summonOwner.getParty().broadcastToPartyMembers(summonOwner, psSummon);
                              summonOwner.sendPacket(ps);
                           } else {
                              summonOwner.sendPacket(ps);
                           }
                        }
                     } else if (this._owner.isPlayer() && this._owner.isInParty()) {
                        this._owner.getParty().broadcastPacket(ps);
                     }
                  }

                  if (os != null) {
                     List<Player> specs = Olympiad.getInstance().getSpectators(this._owner.asPlayer().getOlympiadGameId());
                     if (specs != null && !specs.isEmpty()) {
                        for (Player spec : specs) {
                           if (spec != null) {
                              spec.sendPacket(os);
                           }
                        }
                     }

                     // Custom (olympiad opponent buffs): the opponent in the match
                     // must also see the participant's buffs and debuffs in the
                     // olympiad UI panel, not only the arena spectators.
                     final ExOlympiadSpelledInfo spelled = os;
                     final Player spelledOwner = this._owner.asPlayer();
                     World.forEachVisibleObject(spelledOwner, Player.class, player -> {
                        if (player != null && player != spelledOwner && player.isInOlympiadMode() && (player.getOlympiadGameId() == spelledOwner.getOlympiadGameId())) {
                           player.sendPacket(spelled);
                        }
                     });
                  }

                  this._updateEffectIconTask = null;
               }, 300L);
            }
         }
      }
   }

   private void addIcon(BuffInfo info, AbnormalStatusUpdate asu, PartySpelled ps, PartySpelled psSummon, ExOlympiadSpelledInfo os, boolean isSummon) {
      if (info != null && info.isInUse()) {
         Skill skill = info.getSkill();
         if (asu != null) {
            asu.addSkill(info);
         }

         if (ps != null && (isSummon || !skill.isToggle())) {
            ps.addSkill(info);
         }

         if (psSummon != null && !skill.isToggle()) {
            psSummon.addSkill(info);
         }

         if (os != null) {
            os.addSkill(info);
         }
      }
   }

   private void updateEffectList(boolean update) {
      if (update) {
         this.updateEffectIcons();
         this.computeEffectFlags();
      }
   }

   private void updateEffectFlags() {
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (info != null) {
               if (info.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this._hasBuffsRemovedOnAnyAction = true;
               }

               if (info.getSkill().isRemovedOnDamage()) {
                  this._hasBuffsRemovedOnDamage = true;
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo infox : this._toggles) {
            if (infox != null) {
               if (infox.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this._hasBuffsRemovedOnAnyAction = true;
               }

               if (infox.getSkill().isRemovedOnDamage()) {
                  this._hasBuffsRemovedOnDamage = true;
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infoxx : this._debuffs) {
            if (infoxx != null && infoxx.getSkill().isRemovedOnDamage()) {
               this._hasDebuffsRemovedOnDamage = true;
            }
         }
      }
   }

   private void computeEffectFlags() {
      int flags = 0;
      if (this.hasBuffs()) {
         for (BuffInfo info : this._buffs) {
            if (info != null) {
               for (AbstractEffect e : info.getEffects()) {
                  flags |= e.getEffectFlags();
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo infox : this._debuffs) {
            if (infox != null) {
               for (AbstractEffect e : infox.getEffects()) {
                  flags |= e.getEffectFlags();
               }
            }
         }
      }

      if (this.hasDances()) {
         for (BuffInfo infoxx : this._dances) {
            if (infoxx != null) {
               for (AbstractEffect e : infoxx.getEffects()) {
                  flags |= e.getEffectFlags();
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo infoxxx : this._toggles) {
            if (infoxxx != null) {
               for (AbstractEffect e : infoxxx.getEffects()) {
                  flags |= e.getEffectFlags();
               }
            }
         }
      }

      this._effectFlags = flags;
   }

   public boolean isAffected(EffectFlag flag) {
      return (this._effectFlags & flag.getMask()) != 0;
   }

   public boolean isAffectedByAbnormalType(AbnormalType type) {
      return this._stackedEffects.containsKey(type);
   }
}
