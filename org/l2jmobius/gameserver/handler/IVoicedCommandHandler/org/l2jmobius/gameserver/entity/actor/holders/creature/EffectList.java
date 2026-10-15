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

   public EffectList(Creature var1) {
      this._owner = var1;
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
         ArrayList var1 = new ArrayList();
         if (this.hasBuffs()) {
            var1.addAll(this._buffs);
         }

         if (this.hasDances()) {
            var1.addAll(this._dances);
         }

         if (this.hasToggles()) {
            var1.addAll(this._toggles);
         }

         if (this.hasDebuffs()) {
            var1.addAll(this._debuffs);
         }

         return var1;
      }
   }

   private Queue<BuffInfo> getEffectList(Skill var1) {
      if (var1 == null) {
         return null;
      } else {
         Queue var2;
         if (var1.isPassive()) {
            var2 = this._passives;
         } else if (var1.isDebuff()) {
            var2 = this._debuffs;
         } else if (var1.isDance()) {
            var2 = this._dances;
         } else if (var1.isToggle()) {
            var2 = this._toggles;
         } else {
            var2 = this._buffs;
         }

         return var2;
      }
   }

   public BuffInfo getFirstEffect(EffectType var1) {
      if (this.hasBuffs()) {
         for (BuffInfo var3 : this._buffs) {
            if (var3 != null) {
               for (AbstractEffect var5 : var3.getEffects()) {
                  if (var5 != null && var5.getEffectType() == var1) {
                     return var3;
                  }
               }
            }
         }
      }

      if (this.hasDances()) {
         for (BuffInfo var9 : this._dances) {
            if (var9 != null) {
               for (AbstractEffect var15 : var9.getEffects()) {
                  if (var15 != null && var15.getEffectType() == var1) {
                     return var9;
                  }
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo var10 : this._toggles) {
            if (var10 != null) {
               for (AbstractEffect var16 : var10.getEffects()) {
                  if (var16 != null && var16.getEffectType() == var1) {
                     return var10;
                  }
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var11 : this._debuffs) {
            if (var11 != null) {
               for (AbstractEffect var17 : var11.getEffects()) {
                  if (var17 != null && var17.getEffectType() == var1) {
                     return var11;
                  }
               }
            }
         }
      }

      return null;
   }

   public boolean isAffectedBySkill(int var1) {
      return this.getBuffInfoBySkillId(var1) != null;
   }

   public BuffInfo getBuffInfoBySkillId(int var1) {
      BuffInfo var2 = null;
      if (this.hasBuffs()) {
         for (BuffInfo var4 : this._buffs) {
            if (var4.getSkill().getId() == var1) {
               var2 = var4;
               break;
            }
         }
      }

      if (this.hasDances() && var2 == null) {
         for (BuffInfo var9 : this._dances) {
            if (var9.getSkill().getId() == var1) {
               var2 = var9;
               break;
            }
         }
      }

      if (this.hasToggles() && var2 == null) {
         for (BuffInfo var10 : this._toggles) {
            if (var10.getSkill().getId() == var1) {
               var2 = var10;
               break;
            }
         }
      }

      if (this.hasDebuffs() && var2 == null) {
         for (BuffInfo var11 : this._debuffs) {
            if (var11.getSkill().getId() == var1) {
               var2 = var11;
               break;
            }
         }
      }

      if (this.hasPassives() && var2 == null) {
         for (BuffInfo var12 : this._passives) {
            if (var12.getSkill().getId() == var1) {
               var2 = var12;
               break;
            }
         }
      }

      return var2;
   }

   public BuffInfo getBuffInfoByAbnormalType(AbnormalType var1) {
      return this._stackedEffects.get(var1);
   }

   public void addBlockedAbnormalTypes(Set<AbnormalType> var1) {
      this._blockedAbnormalTypes.addAll(var1);
   }

   public boolean removeBlockedAbnormalTypes(Set<AbnormalType> var1) {
      return this._blockedAbnormalTypes.removeAll(var1);
   }

   public Set<AbnormalType> getBlockedAbnormalTypes() {
      return Collections.unmodifiableSet(this._blockedAbnormalTypes);
   }

   public BuffInfo getShortBuff() {
      return this._shortBuff;
   }

   public void shortBuffStatusUpdate(BuffInfo var1) {
      if (this._owner.isPlayer()) {
         this._shortBuff = var1;
         if (var1 != null) {
            this._owner.sendPacket(new ShortBuffStatusUpdate(var1.getSkill().getId(), var1.getSkill().getLevel(), var1.getTime()));
         } else {
            this._owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
         }
      }
   }

   private boolean doesStack(Skill var1) {
      AbnormalType var2 = var1.getAbnormalType();
      if (!var2.isNone() && !this.isEmpty()) {
         for (BuffInfo var4 : this.getEffectList(var1)) {
            if (var4 != null && var4.getSkill().getAbnormalType() == var2) {
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

   protected void stopAndRemove(BuffInfo var1) {
      this.stopAndRemove(true, SkillFinishType.REMOVED, var1, this.getEffectList(var1.getSkill()));
   }

   protected void stopAndRemove(boolean var1, BuffInfo var2, Queue<BuffInfo> var3) {
      this.stopAndRemove(var1, SkillFinishType.REMOVED, var2, var3);
   }

   private void stopAndRemove(boolean var1, SkillFinishType var2, BuffInfo var3, Queue<BuffInfo> var4) {
      if (var3 != null) {
         var4.remove(var3);
         var3.stopAllEffects(var2, var1);
         if (!var3.isInUse()) {
            this._hiddenBuffs.decrementAndGet();
         } else {
            this._stackedEffects.remove(var3.getSkill().getAbnormalType());
         }

         if (var3.getSkill().isAbnormalInstant() && this.hasBuffs()) {
            for (BuffInfo var6 : this._buffs) {
               if (var6 != null && var6.getSkill().getAbnormalType() == var3.getSkill().getAbnormalType() && !var6.isInUse()) {
                  var6.setInUse(true);
                  var6.addStats();
                  this._stackedEffects.put(var6.getSkill().getAbnormalType(), var6);
                  this._hiddenBuffs.decrementAndGet();
                  break;
               }
            }
         }

         if (var2 != SkillFinishType.REMOVED) {
            var3.getSkill().applyEffectScope(EffectScope.END, var3, true, false);
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
      boolean var1 = false;
      if (this.hasBuffs()) {
         for (BuffInfo var3 : this._buffs) {
            if (!var3.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, var3, this._buffs);
            }
         }

         var1 = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var7 : this._debuffs) {
            if (!var7.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, var7, this._debuffs);
            }
         }

         var1 = true;
      }

      if (this.hasDances()) {
         for (BuffInfo var8 : this._dances) {
            if (!var8.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, var8, this._dances);
            }
         }

         var1 = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo var9 : this._toggles) {
            if (!var9.getSkill().isStayAfterDeath()) {
               this.stopAndRemove(true, var9, this._toggles);
            }
         }

         var1 = true;
      }

      this.updateEffectList(var1);
   }

   public void stopAllEffectsWithoutExclusions(boolean var1, boolean var2) {
      for (BuffInfo var4 : this._buffs) {
         this.stopAndRemove(var2, var4, this._buffs);
      }

      for (BuffInfo var9 : this._dances) {
         this.stopAndRemove(var2, var9, this._dances);
      }

      for (BuffInfo var10 : this._toggles) {
         this.stopAndRemove(var2, var10, this._toggles);
      }

      for (BuffInfo var11 : this._debuffs) {
         this.stopAndRemove(var2, var11, this._debuffs);
      }

      for (BuffInfo var12 : this._passives) {
         this.stopAndRemove(var2, var12, this._passives);
      }

      if (var1) {
         this.updateEffectList(var2);
      }
   }

   public void stopAllEffectsNotStayOnSubclassChange() {
      boolean var1 = false;
      if (this.hasBuffs()) {
         for (BuffInfo var3 : this._buffs) {
            if (!var3.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, var3, this._buffs);
            }
         }

         var1 = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var7 : this._debuffs) {
            if (!var7.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, var7, this._debuffs);
            }
         }

         var1 = true;
      }

      if (this.hasDances()) {
         for (BuffInfo var8 : this._dances) {
            if (!var8.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, var8, this._dances);
            }
         }

         var1 = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo var9 : this._toggles) {
            if (!var9.getSkill().isStayOnSubclassChange()) {
               this.stopAndRemove(true, var9, this._toggles);
            }
         }

         var1 = true;
      }

      this.updateEffectList(var1);
   }

   public void stopAllBuffs(boolean var1) {
      if (this.hasBuffs()) {
         this._buffs.forEach(var2 -> this.stopAndRemove(var1, var2, this._buffs));
      }

      this.updateEffectList(var1);
   }

   public void stopAllToggles() {
      this.stopAllToggles(true);
   }

   public void stopAllToggles(boolean var1) {
      if (this.hasToggles()) {
         this._toggles.forEach(var2 -> this.stopAndRemove(var1, var2, this._toggles));
         this.updateEffectList(var1);
      }
   }

   public void stopAllDances(boolean var1) {
      if (this.hasDances()) {
         this._dances.forEach(var2 -> this.stopAndRemove(var1, var2, this._dances));
         this.updateEffectList(var1);
      }
   }

   public void stopAllDebuffs(boolean var1) {
      if (this.hasDebuffs()) {
         this._debuffs.forEach(var2 -> this.stopAndRemove(var1, var2, this._debuffs));
         this.updateEffectList(var1);
      }
   }

   public void stopEffects(EffectType var1) {
      boolean var2 = false;
      Consumer var3 = var2x -> {
         for (AbstractEffect var4 : var2x.getEffects()) {
            if (var4 != null && var4.getEffectType() == var1) {
               this.stopAndRemove(var2x);
            }
         }
      };
      if (this.hasBuffs()) {
         for (BuffInfo var5 : this._buffs) {
            if (var5 != null) {
               var3.accept(var5);
            }
         }

         var2 = true;
      }

      if (this.hasDances()) {
         for (BuffInfo var9 : this._dances) {
            if (var9 != null) {
               var3.accept(var9);
            }
         }

         var2 = true;
      }

      if (this.hasToggles()) {
         for (BuffInfo var10 : this._toggles) {
            if (var10 != null) {
               var3.accept(var10);
            }
         }

         var2 = true;
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var11 : this._debuffs) {
            if (var11 != null) {
               var3.accept(var11);
            }
         }

         var2 = true;
      }

      this.updateEffectList(var2);
   }

   public void stopSkillEffects(SkillFinishType var1, int var2) {
      BuffInfo var3 = this.getBuffInfoBySkillId(var2);
      if (var3 != null) {
         this.remove(var1, var3);
      }
   }

   public void stopSkillEffects(SkillFinishType var1, Skill var2) {
      if (var2 != null) {
         this.stopSkillEffects(var1, var2.getId());
      }
   }

   public boolean stopSkillEffects(SkillFinishType var1, AbnormalType var2) {
      BuffInfo var3 = this._stackedEffects.remove(var2);
      if (var3 != null) {
         this.stopSkillEffects(var1, var3.getSkill());
         return true;
      } else {
         return false;
      }
   }

   public void stopEffectsOnAction() {
      if (this._hasBuffsRemovedOnAnyAction) {
         boolean var1 = false;
         if (this.hasBuffs()) {
            for (BuffInfo var3 : this._buffs) {
               if (var3.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, var3, this._buffs);
               }
            }

            var1 = true;
         }

         if (this.hasDebuffs()) {
            for (BuffInfo var7 : this._debuffs) {
               if (var7.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, var7, this._debuffs);
               }
            }

            var1 = true;
         }

         if (this.hasDances()) {
            for (BuffInfo var8 : this._dances) {
               if (var8.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, var8, this._dances);
               }
            }

            var1 = true;
         }

         if (this.hasToggles()) {
            for (BuffInfo var9 : this._toggles) {
               if (var9.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this.stopAndRemove(true, var9, this._toggles);
               }
            }

            var1 = true;
         }

         this.updateEffectList(var1);
      }
   }

   public void stopEffectsOnDamage(boolean var1) {
      if (var1) {
         boolean var2 = false;
         if (this._hasBuffsRemovedOnDamage) {
            if (this.hasBuffs()) {
               for (BuffInfo var4 : this._buffs) {
                  if (var4 != null && var4.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, var4, this._buffs);
                  }
               }

               var2 = true;
            }

            if (this.hasDances()) {
               for (BuffInfo var8 : this._dances) {
                  if (var8 != null && var8.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, var8, this._dances);
                  }
               }

               var2 = true;
            }

            if (this.hasToggles()) {
               for (BuffInfo var9 : this._toggles) {
                  if (var9 != null && var9.getSkill().isRemovedOnDamage()) {
                     this.stopAndRemove(true, var9, this._toggles);
                  }
               }

               var2 = true;
            }
         }

         if (this._hasDebuffsRemovedOnDamage && this.hasDebuffs()) {
            for (BuffInfo var10 : this._debuffs) {
               if (var10 != null && var10.getSkill().isRemovedOnDamage()) {
                  this.stopAndRemove(true, var10, this._debuffs);
               }
            }

            var2 = true;
         }

         this.updateEffectList(var2);
      }
   }

   public void updateEffectIcons(boolean var1) {
      if (var1) {
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

   public void forEach(Function<BuffInfo, Boolean> var1, boolean var2) {
      boolean var3 = false;
      if (this.hasBuffs()) {
         for (BuffInfo var5 : this._buffs) {
            var3 |= var1.apply(var5);
         }
      }

      if (var2 && this.hasDances()) {
         for (BuffInfo var9 : this._dances) {
            var3 |= var1.apply(var9);
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo var10 : this._toggles) {
            var3 |= var1.apply(var10);
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var11 : this._debuffs) {
            var3 |= var1.apply(var11);
         }
      }

      this.updateEffectList(var3);
   }

   public void remove(SkillFinishType var1, BuffInfo var2) {
      if (var2 != null) {
         this.stopAndRemove(true, var1, var2, this.getEffectList(var2.getSkill()));
         this.updateEffectList(true);
      }
   }

   public void add(BuffInfo var1) {
      if (var1 != null) {
         Skill var2 = var1.getSkill();
         if (!this._blockedAbnormalTypes.contains(var2.getAbnormalType())) {
            if (var2.isPassive()) {
               if (!var2.getAbnormalType().isNone()) {
                  LOGGER.warning("Passive " + var2 + " with abnormal type: " + var2.getAbnormalType() + "!");
               }

               if (var2.checkCondition(var1.getEffector(), var1.getEffected(), false)) {
                  for (BuffInfo var10 : this._passives) {
                     if (var10.getSkill().getId() == var2.getId()) {
                        var10.setInUse(false);
                        var10.removeStats();
                        this._passives.remove(var10);
                     }
                  }

                  this._passives.add(var1);
                  var1.initializeEffects();
               }
            } else if (!var1.getEffected().isDead()) {
               if (var2.getAbnormalType().isNone()) {
                  this.stopSkillEffects(SkillFinishType.NORMAL, var2);
               } else {
                  synchronized (this) {
                     if (this._stackedEffects.containsKey(var2.getAbnormalType())) {
                        BuffInfo var4 = this._stackedEffects.get(var2.getAbnormalType());
                        if (var4 == null || var2.getAbnormalLevel() < var4.getSkill().getAbnormalLevel()) {
                           return;
                        }

                        if (var2.isAbnormalInstant()) {
                           if (var4.getSkill().isAbnormalInstant()) {
                              this.stopSkillEffects(SkillFinishType.NORMAL, var2.getAbnormalType());
                              var4 = this._stackedEffects.get(var2.getAbnormalType());
                           }

                           if (var4 != null) {
                              var4.setInUse(false);
                              var4.removeStats();
                              this._hiddenBuffs.incrementAndGet();
                           }
                        } else {
                           if (var4.getSkill().isAbnormalInstant()) {
                              this.stopSkillEffects(SkillFinishType.NORMAL, var2.getAbnormalType());
                           }

                           this.stopSkillEffects(SkillFinishType.NORMAL, var2.getAbnormalType());
                        }
                     }

                     this._stackedEffects.put(var2.getAbnormalType(), var1);
                  }
               }

               Queue var3 = this.getEffectList(var2);
               if (!var2.isDebuff() && !var2.isToggle() && !var2.is7Signs() && !this.doesStack(var2)) {
                  int var9 = -1;
                  if (var2.isDance()) {
                     var9 = this.getDanceCount() - PlayerConfig.DANCES_MAX_AMOUNT;
                  } else if (!var2.isHealingPotionSkill()) {
                     var9 = this.getBuffCount() - this._owner.getStat().getMaxBuffCount();
                  }

                  for (BuffInfo var6 : var3) {
                     if (var9 < 0) {
                        break;
                     }

                     if (var6.isInUse()) {
                        this.stopAndRemove(true, var6, var3);
                        var9--;
                     }
                  }
               }

               var3.add(var1);
               var1.initializeEffects();
               this.updateEffectList(true);
            }
         }
      }
   }

   private void updateEffectIcons() {
      if (this._owner != null) {
         this.updateEffectFlags();
         if (this._owner.isPlayable() && this._updateEffectIconTask == null) {
            this._updateEffectIconTask = ThreadPool.schedule(() -> {
               AbnormalStatusUpdate var1 = null;
               PartySpelled var2 = null;
               PartySpelled var3 = null;
               ExOlympiadSpelledInfo var4 = null;
               boolean var5 = false;
               if (this._owner.isPlayer()) {
                  if (this._partyOnly) {
                     this._partyOnly = false;
                  } else {
                     var1 = new AbnormalStatusUpdate();
                  }

                  if (this._owner.isInParty()) {
                     var2 = new PartySpelled(this._owner);
                  }

                  Player var6 = this._owner.asPlayer();
                  if (var6.isInOlympiadMode() && var6.isOlympiadStart()) {
                     var4 = new ExOlympiadSpelledInfo(var6);
                  }
               } else if (this._owner.isSummon()) {
                  var5 = true;
                  var2 = new PartySpelled(this._owner);
                  var3 = new PartySpelled(this._owner);
               }

               if (this.hasBuffs()) {
                  for (BuffInfo var7 : this._buffs) {
                     if (var7.getSkill().isHealingPotionSkill()) {
                        this.shortBuffStatusUpdate(var7);
                     } else {
                        this.addIcon(var7, var1, var2, var3, var4, var5);
                     }
                  }
               }

               if (this.hasDances()) {
                  for (BuffInfo var15 : this._dances) {
                     this.addIcon(var15, var1, var2, var3, var4, var5);
                  }
               }

               if (this.hasToggles()) {
                  for (BuffInfo var16 : this._toggles) {
                     this.addIcon(var16, var1, var2, var3, var4, var5);
                  }
               }

               if (this.hasDebuffs()) {
                  for (BuffInfo var17 : this._debuffs) {
                     this.addIcon(var17, var1, var2, var3, var4, var5);
                  }
               }

               if (var1 != null) {
                  this._owner.sendPacket(var1);
               }

               if (var2 != null) {
                  if (this._owner.isSummon()) {
                     Player var13 = this._owner.asSummon().getOwner();
                     if (var13 != null) {
                        if (var13.isInParty()) {
                           var13.getParty().broadcastToPartyMembers(var13, var3);
                           var13.sendPacket(var2);
                        } else {
                           var13.sendPacket(var2);
                        }
                     }
                  } else if (this._owner.isPlayer() && this._owner.isInParty()) {
                     this._owner.getParty().broadcastPacket(var2);
                  }
               }

               if (var4 != null) {
                  List var14 = Olympiad.getInstance().getSpectators(this._owner.asPlayer().getOlympiadGameId());
                  if (var14 != null && !var14.isEmpty()) {
                     for (Player var8 : var14) {
                        if (var8 != null) {
                           var8.sendPacket(var4);
                        }
                     }
                  }

                  ExOlympiadSpelledInfo var19 = var4;
                  Player var20 = this._owner.asPlayer();
                  World.forEachVisibleObject(var20, Player.class, var2x -> {
                     if (var2x != null && var2x != var20 && var2x.isInOlympiadMode() && var2x.getOlympiadGameId() == var20.getOlympiadGameId()) {
                        var2x.sendPacket(var19);
                     }
                  });
               }

               this._updateEffectIconTask = null;
            }, 300L);
         }
      }
   }

   private void addIcon(BuffInfo var1, AbnormalStatusUpdate var2, PartySpelled var3, PartySpelled var4, ExOlympiadSpelledInfo var5, boolean var6) {
      if (var1 != null && var1.isInUse()) {
         Skill var7 = var1.getSkill();
         if (var2 != null) {
            var2.addSkill(var1);
         }

         if (var3 != null && (var6 || !var7.isToggle())) {
            var3.addSkill(var1);
         }

         if (var4 != null && !var7.isToggle()) {
            var4.addSkill(var1);
         }

         if (var5 != null) {
            var5.addSkill(var1);
         }
      }
   }

   private void updateEffectList(boolean var1) {
      if (var1) {
         this.updateEffectIcons();
         this.computeEffectFlags();
      }
   }

   private void updateEffectFlags() {
      if (this.hasBuffs()) {
         for (BuffInfo var2 : this._buffs) {
            if (var2 != null) {
               if (var2.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this._hasBuffsRemovedOnAnyAction = true;
               }

               if (var2.getSkill().isRemovedOnDamage()) {
                  this._hasBuffsRemovedOnDamage = true;
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo var5 : this._toggles) {
            if (var5 != null) {
               if (var5.getSkill().isRemovedOnAnyActionExceptMove()) {
                  this._hasBuffsRemovedOnAnyAction = true;
               }

               if (var5.getSkill().isRemovedOnDamage()) {
                  this._hasBuffsRemovedOnDamage = true;
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var6 : this._debuffs) {
            if (var6 != null && var6.getSkill().isRemovedOnDamage()) {
               this._hasDebuffsRemovedOnDamage = true;
            }
         }
      }
   }

   private void computeEffectFlags() {
      int var1 = 0;
      if (this.hasBuffs()) {
         for (BuffInfo var3 : this._buffs) {
            if (var3 != null) {
               for (AbstractEffect var5 : var3.getEffects()) {
                  var1 |= var5.getEffectFlags();
               }
            }
         }
      }

      if (this.hasDebuffs()) {
         for (BuffInfo var9 : this._debuffs) {
            if (var9 != null) {
               for (AbstractEffect var15 : var9.getEffects()) {
                  var1 |= var15.getEffectFlags();
               }
            }
         }
      }

      if (this.hasDances()) {
         for (BuffInfo var10 : this._dances) {
            if (var10 != null) {
               for (AbstractEffect var16 : var10.getEffects()) {
                  var1 |= var16.getEffectFlags();
               }
            }
         }
      }

      if (this.hasToggles()) {
         for (BuffInfo var11 : this._toggles) {
            if (var11 != null) {
               for (AbstractEffect var17 : var11.getEffects()) {
                  var1 |= var17.getEffectFlags();
               }
            }
         }
      }

      this._effectFlags = var1;
   }

   public boolean isAffected(EffectFlag var1) {
      return (this._effectFlags & var1.getMask()) != 0;
   }

   public boolean isAffectedByAbnormalType(AbnormalType var1) {
      return this._stackedEffects.containsKey(var1);
   }
}
