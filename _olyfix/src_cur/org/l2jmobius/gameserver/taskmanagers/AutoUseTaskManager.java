package org.l2jmobius.gameserver.taskmanagers;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

public class AutoUseTaskManager {
   private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
   private static final int POOL_SIZE = 200;
   private static final int TASK_DELAY = 300;
   private static final int REUSE_MARGIN_TIME = 3;

   protected AutoUseTaskManager() {
   }

   public synchronized void startAutoUseTask(Player var1) {
      for (Set var3 : POOLS) {
         if (var3.contains(var1)) {
            return;
         }
      }

      for (Set var6 : POOLS) {
         if (var6.size() < 200) {
            var6.add(var1);
            return;
         }
      }

      KeySetView var5 = ConcurrentHashMap.newKeySet(200);
      var5.add(var1);
      ThreadPool.schedulePriorityTaskAtFixedRate(new AutoUseTaskManager.AutoUse(var5), 300L, 300L);
      POOLS.add(var5);
   }

   public void stopAutoUseTask(Player var1) {
      var1.getAutoUseSettings().resetSkillOrder();

      for (Set var3 : POOLS) {
         if (var3.remove(var1)) {
            return;
         }
      }
   }

   public static AutoUseTaskManager getInstance() {
      return AutoUseTaskManager.SingletonHolder.INSTANCE;
   }

   private class AutoUse implements Runnable {
      private final Set<Player> _players;

      public AutoUse(Set<Player> nullx) {
         Objects.requireNonNull(AutoUseTaskManager.this);
         Objects.requireNonNull(AutoUseTaskManager.this);
         super();
         this._players = nullx;
      }

      @Override
      public void run() {
         if (!this._players.isEmpty()) {
            for (Player var2 : this._players) {
               if (!var2.isOnline() || var2.isInOfflineMode() && !var2.isOfflinePlay()) {
                  AutoUseTaskManager.this.stopAutoUseTask(var2);
               } else if (!var2.isSitting()
                  && !var2.isStunned()
                  && !var2.isSleeping()
                  && !var2.isParalyzed()
                  && !var2.isControlBlocked()
                  && !var2.isAlikeDead()
                  && !var2.isMounted()
                  && (!var2.isTransformed() || !var2.getTransformation().isRiding())) {
                  boolean var3 = var2.isInsideZone(ZoneId.PEACE);
                  if (AutoPlayConfig.ENABLE_AUTO_ITEM && !var3) {
                     label238:
                     for (Integer var5 : var2.getAutoUseSettings().getAutoSupplyItems()) {
                        if (var2.isTeleporting()) {
                           break;
                        }

                        Item var6 = var2.getInventory().getItemByItemId(var5);
                        if (var6 == null) {
                           var2.getAutoUseSettings().getAutoSupplyItems().remove(var5);
                        } else if (var6.isOlyRestrictedItem() && var2.isInOlympiadMode()) {
                           var2.getAutoUseSettings().getAutoSupplyItems().remove(var5);
                        } else {
                           ItemTemplate var7 = var6.getTemplate();
                           if (var7 != null && var7.checkCondition(var2, var2, false)) {
                              SkillHolder[] var8 = var6.getTemplate().getSkills();
                              if (var8 != null) {
                                 for (SkillHolder var12 : var8) {
                                    Skill var13 = var12.getSkill();
                                    if (var13.isActive()
                                       && (
                                          var2.isAffectedBySkill(var13.getId())
                                             || var2.hasSkillReuse(var13.getReuseHashCode())
                                             || !var13.checkCondition(var2, var2, false)
                                       )) {
                                       continue label238;
                                    }
                                 }
                              }

                              int var30 = var6.getReuseDelay();
                              if (var30 <= 0 || var2.getItemRemainingReuseTime(var6.getObjectId()) <= 0L) {
                                 IItemHandler var33 = ItemHandler.getInstance().getHandler(var6.getEtcItem());
                                 if (var33 != null && var33.onItemUse(var2, var6, false) && var30 > 0) {
                                    var2.addTimeStampItem(var6, var30);
                                 }
                              }
                           }
                        }
                     }
                  }

                  if (AutoPlayConfig.ENABLE_AUTO_POTION && !var3 && var2.getCurrentHpPercent() < var2.getAutoPlaySettings().getAutoPotionPercent()) {
                     int var14 = var2.getAutoUseSettings().getAutoPotionItem();
                     if (var14 > 0) {
                        Item var17 = var2.getInventory().getItemByItemId(var14);
                        if (var17 == null) {
                           var2.getAutoUseSettings().setAutoPotionItem(0);
                        } else {
                           int var20 = var17.getReuseDelay();
                           if (var20 <= 0 || var2.getItemRemainingReuseTime(var17.getObjectId()) <= 0L) {
                              EtcItem var23 = var17.getEtcItem();
                              IItemHandler var26 = ItemHandler.getInstance().getHandler(var23);
                              if (var26 != null && var26.onItemUse(var2, var17, false) && var20 > 0) {
                                 var2.addTimeStampItem(var17, var20);
                              }
                           }
                        }
                     }
                  }

                  if (AutoPlayConfig.ENABLE_AUTO_SKILL) {
                     for (Integer var18 : var2.getAutoUseSettings().getAutoBuffs()) {
                        if (var3 || var2.isCastingNow() || var2.isAttackingNow() || var2.isTeleporting()) {
                           break;
                        }

                        Summon var21 = null;
                        Skill var24 = var2.getKnownSkill(var18);
                        if (var24 == null) {
                           if (var2.hasServitor() || var2.hasPet()) {
                              Summon var27 = var2.getSummon();
                              var24 = var27.getKnownSkill(var18);
                              if (var24 == null) {
                                 var24 = PetSkillData.getInstance().getKnownSkill(var27, var18);
                              }

                              if (var24 != null) {
                                 var21 = var27;
                              }
                           }

                           if (var24 == null) {
                              var2.getAutoUseSettings().getAutoBuffs().remove(var18);
                              continue;
                           }
                        }

                        Object var28 = var2.getAutoUseSettings().isSelfCastOnly() ? var2 : var2.getTarget();
                        if (this.canCastBuff(var2, (WorldObject)var28, var24)) {
                           Object var31 = var21 != null ? var21 : var2;
                           if (var28 != null && ((WorldObject)var28).isPlayable()) {
                              Player var34 = ((WorldObject)var28).asPlayer();
                              if ((var34.getPvpFlag() != 0 || var34.getKarma() > 0) && var34.getParty() != ((Playable)var31).getParty()) {
                                 if (!((Playable)var31).getEffectList().isAffectedBySkill(var24.getId())) {
                                    ((Playable)var31).setTarget((WorldObject)var31);
                                    ((Playable)var31).doCast(var24);
                                    ((Playable)var31).setTarget((WorldObject)var28);
                                 }
                              } else if (var28 == var31) {
                                 // Auto self-cast only (FakePlayersAI bots): the evaluated target IS the
                                 // caster himself. beginCast() resolves a beneficial continuous cast from
                                 // the target list, which in combat points at the enemy - aim the cast at
                                 // the bot itself and restore the combat target afterwards, so a self-buff
                                 // or heal can never land on the targeted opponent (and the buff then
                                 // registers on the bot, ending the endless "not applied -> recast" loop).
                                 WorldObject var39 = ((Playable)var31).getTarget();
                                 ((Playable)var31).setTarget((WorldObject)var31);
                                 ((Playable)var31).doCast(var24);
                                 ((Playable)var31).setTarget(var39);
                              } else {
                                 ((Playable)var31).doCast(var24);
                              }
                           } else {
                              ((Playable)var31).setTarget((WorldObject)var31);
                              ((Playable)var31).doCast(var24);
                              ((Playable)var31).setTarget((WorldObject)var28);
                           }
                        }
                     }

                     if (var2.isAutoPlaying()) {
                        int var16 = var2.getAutoUseSettings().getAutoSkills().size();

                        for (int var19 = 0; var19 < var16 && !var2.isCastingNow() && !var2.isTeleporting(); var19++) {
                           Summon var22 = null;
                           WorldObject var25 = var2.getTarget();
                           Integer var29 = var2.getAutoUseSettings().getNextSkillId();
                           Skill var32 = var2.getKnownSkill(var29);
                           if (var32 == null) {
                              if (var2.hasServitor() || var2.hasPet()) {
                                 Summon var35 = var2.getSummon();
                                 var32 = var35.getKnownSkill(var29);
                                 if (var32 == null) {
                                    var32 = PetSkillData.getInstance().getKnownSkill(var35, var29);
                                 }

                                 if (var32 != null) {
                                    var22 = var35;
                                    var35.setTarget(var25);
                                 }
                              }

                              if (var32 == null) {
                                 var2.getAutoUseSettings().getAutoSkills().remove(var29);
                                 var2.getAutoUseSettings().resetSkillOrder();
                                 break;
                              }
                           }

                           if (var25 == var2) {
                              break;
                           }

                           if (var25 == null || var25.asCreature().isDead()) {
                              if (var2.getQueuedSkill() != null) {
                                 var2.setQueuedSkill(null, false, false);
                              }
                              break;
                           }

                           if (var25.isInsideZone(ZoneId.PEACE) || !var25.isAutoAttackable(var2)) {
                              break;
                           }

                           if (var25 instanceof Guard) {
                              int var36 = var2.getAutoPlaySettings().getNextTargetMode();
                              if (var36 != 3 && var36 != 0) {
                                 break;
                              }
                           }

                           var2.getAutoUseSettings().incrementSkillOrder();
                           Object var37 = var22 != null ? var22 : var2;
                           if (var2.getAutoUseSettings().isSelfCastOnly()
                              && !var32.isDamage()
                              && var32.getEffectPoint() > 0
                              && var32.getTargetType() != TargetType.SELF) {
                              // Custom (FakePlayersAI bots, self-cast rotation skills): a beneficial
                              // non-damage skill that is ALREADY active on the bot must be skipped.
                              // The rotation engine has no "already applied" check of its own, so a
                              // buff placed into the rotation (e.g. Blazing Skin added by //bot rotadd)
                              // was re-cast on every rotation cycle - the visible endless self-buffing
                              // at the olympiad. Same remaining-time margin as canCastBuff below.
                              BuffInfo activeBuff = ((Playable)var37).getEffectList().getBuffInfoBySkillId(var32.getId());
                              if (activeBuff != null && activeBuff.getTime() > REUSE_MARGIN_TIME) {
                                 continue;
                              }

                              ((Playable)var37).setTarget((WorldObject)var37);
                              if (this.canUseMagic((Playable)var37, (WorldObject)var37, var32)) {
                                 ((Playable)var37).useMagic(var32, true, false);
                                 ((Playable)var37).setTarget(var25);
                                 break;
                              }

                              ((Playable)var37).setTarget(var25);
                           } else if (this.canUseMagic((Playable)var37, var25, var32)) {
                              ((Playable)var37).useMagic(var32, true, false);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private boolean canCastBuff(Player var1, WorldObject var2, Skill var3) {
         if (var2 != null
            && var2.isCreature()
            && var2.asCreature().isAlikeDead()
            && var3.getTargetType() != TargetType.SELF
            && var3.getTargetType() != TargetType.CORPSE
            && var3.getTargetType() != TargetType.PC_BODY) {
            return false;
         } else {
            Object var4 = var2 != null && var2.isPlayable() && var3.getTargetType() != TargetType.SELF ? var2.asPlayable() : var1;
            if (var1 != var4 && var1.calculateDistance3D((ILocational)var4) > var3.getCastRange()) {
               return false;
            } else if (!this.canUseMagic(var1, (WorldObject)var4, var3)) {
               return false;
            } else {
               BuffInfo var5 = ((Playable)var4).getEffectList().getBuffInfoBySkillId(var3.getId());
               BuffInfo var6 = ((Playable)var4).getEffectList().getBuffInfoByAbnormalType(var3.getAbnormalType());
               if (var6 == null) {
                  return var5 == null;
               } else {
                  return var5 != null
                     ? var6.getSkill().getId() == var5.getSkill().getId() && (var5.getTime() <= 3 || var5.getSkill().getLevel() < var3.getLevel())
                     : var6.getSkill().getAbnormalLevel() < var3.getAbnormalLevel() || var6.isAbnormalType(AbnormalType.NONE);
               }
            }
         }
      }

      private boolean canUseMagic(Playable var1, WorldObject var2, Skill var3) {
         if (var3.getItemConsumeCount() > 0 && var1.getInventory().getInventoryItemCount(var3.getItemConsumeId(), -1) < var3.getItemConsumeCount()) {
            return false;
         } else if (var1.isPlayer() && var1.asPlayer().getCharges() < var3.getChargeConsumeCount()) {
            return false;
         } else {
            int var4 = var3.getMpInitialConsume() + var3.getMpConsume();
            if (var4 > 0 && var1.getCurrentMp() < var4) {
               return false;
            } else {
               return var3.getId() == 254 && var2 != null && var2.isMonster() && var2.asMonster().isSpoiled()
                  ? false
                  : !var1.isSkillDisabled(var3) && var3.checkCondition(var1, var2, false);
            }
         }
      }
   }

   private static class SingletonHolder {
      protected static final AutoUseTaskManager INSTANCE = new AutoUseTaskManager();
   }
}
