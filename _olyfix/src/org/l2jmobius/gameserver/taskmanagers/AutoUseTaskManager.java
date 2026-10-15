package org.l2jmobius.gameserver.taskmanagers;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

   public synchronized void startAutoUseTask(Player player) {
      for (Set<Player> pool : POOLS) {
         if (pool.contains(player)) {
            return;
         }
      }

      for (Set<Player> poolx : POOLS) {
         if (poolx.size() < 200) {
            poolx.add(player);
            return;
         }
      }

      Set<Player> poolxx = ConcurrentHashMap.newKeySet(200);
      poolxx.add(player);
      ThreadPool.schedulePriorityTaskAtFixedRate(new AutoUseTaskManager.AutoUse(poolxx), 300L, 300L);
      POOLS.add(poolxx);
   }

   public void stopAutoUseTask(Player player) {
      player.getAutoUseSettings().resetSkillOrder();

      for (Set<Player> pool : POOLS) {
         if (pool.remove(player)) {
            return;
         }
      }
   }

   public static AutoUseTaskManager getInstance() {
      return AutoUseTaskManager.SingletonHolder.INSTANCE;
   }

   private class AutoUse implements Runnable {
      private final Set<Player> _players;

      public AutoUse(Set<Player> players) {
         Objects.requireNonNull(AutoUseTaskManager.this);
         super();
         this._players = players;
      }

      @Override
      public void run() {
         if (!this._players.isEmpty()) {
            for (Player player : this._players) {
               if (!player.isOnline() || player.isInOfflineMode() && !player.isOfflinePlay()) {
                  AutoUseTaskManager.this.stopAutoUseTask(player);
               } else if (!player.isSitting()
                  && !player.isStunned()
                  && !player.isSleeping()
                  && !player.isParalyzed()
                  && !player.isControlBlocked()
                  && !player.isAlikeDead()
                  && !player.isMounted()
                  && (!player.isTransformed() || !player.getTransformation().isRiding())) {
                  boolean isInPeaceZone = player.isInsideZone(ZoneId.PEACE);
                  if (AutoPlayConfig.ENABLE_AUTO_ITEM && !isInPeaceZone) {
                     label240:
                     for (Integer itemId : player.getAutoUseSettings().getAutoSupplyItems()) {
                        if (player.isTeleporting()) {
                           break;
                        }

                        Item item = player.getInventory().getItemByItemId(itemId);
                        if (item == null) {
                           player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
                        } else if (item.isOlyRestrictedItem() && player.isInOlympiadMode()) {
                           player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
                        } else {
                           ItemTemplate template = item.getTemplate();
                           if (template != null && template.checkCondition(player, player, false)) {
                              SkillHolder[] skills = item.getTemplate().getSkills();
                              if (skills != null) {
                                 for (SkillHolder itemSkillHolder : skills) {
                                    Skill skill = itemSkillHolder.getSkill();
                                    if (skill.isActive()
                                       && (
                                          player.isAffectedBySkill(skill.getId())
                                             || player.hasSkillReuse(skill.getReuseHashCode())
                                             || !skill.checkCondition(player, player, false)
                                       )) {
                                       continue label240;
                                    }
                                 }
                              }

                              int reuseDelay = item.getReuseDelay();
                              if (reuseDelay <= 0 || player.getItemRemainingReuseTime(item.getObjectId()) <= 0L) {
                                 IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                                 if (handler != null && handler.onItemUse(player, item, false) && reuseDelay > 0) {
                                    player.addTimeStampItem(item, reuseDelay);
                                 }
                              }
                           }
                        }
                     }
                  }

                  if (AutoPlayConfig.ENABLE_AUTO_POTION && !isInPeaceZone && player.getCurrentHpPercent() < player.getAutoPlaySettings().getAutoPotionPercent()
                     )
                   {
                     int itemId = player.getAutoUseSettings().getAutoPotionItem();
                     if (itemId > 0) {
                        Item item = player.getInventory().getItemByItemId(itemId);
                        if (item == null) {
                           player.getAutoUseSettings().setAutoPotionItem(0);
                        } else {
                           int reuseDelay = item.getReuseDelay();
                           if (reuseDelay <= 0 || player.getItemRemainingReuseTime(item.getObjectId()) <= 0L) {
                              EtcItem etcItem = item.getEtcItem();
                              IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
                              if (handler != null && handler.onItemUse(player, item, false) && reuseDelay > 0) {
                                 player.addTimeStampItem(item, reuseDelay);
                              }
                           }
                        }
                     }
                  }

                  if (AutoPlayConfig.ENABLE_AUTO_SKILL) {
                     for (Integer skillId : player.getAutoUseSettings().getAutoBuffs()) {
                        if (isInPeaceZone || player.isCastingNow() || player.isAttackingNow() || player.isTeleporting()) {
                           break;
                        }

                        Playable pet = null;
                        Skill skill = player.getKnownSkill(skillId);
                        if (skill == null) {
                           if (player.hasServitor() || player.hasPet()) {
                              Summon summon = player.getSummon();
                              skill = summon.getKnownSkill(skillId);
                              if (skill == null) {
                                 skill = PetSkillData.getInstance().getKnownSkill(summon, skillId);
                              }

                              if (skill != null) {
                                 pet = summon;
                              }
                           }

                           if (skill == null) {
                              player.getAutoUseSettings().getAutoBuffs().remove(skillId);
                              continue;
                           }
                        }

                        // Auto self-cast only (FakePlayersAI bots): evaluate the buff against the
                        // caster himself, so canCastBuff and the cast below always resolve to the
                        // bot - the engine must not buff/heal another player (e.g. the current target).
                        WorldObject target = player.getAutoUseSettings().isSelfCastOnly() ? player : player.getTarget();
                        if (this.canCastBuff(player, target, skill)) {
                           Playable caster = (Playable)(pet != null ? pet : player);
                           if (target != null && target.isPlayable()) {
                              Player targetPlayer = target.asPlayer();
                              if ((targetPlayer.getPvpFlag() != 0 || targetPlayer.getKarma() > 0) && targetPlayer.getParty() != caster.getParty()) {
                                 if (!caster.getEffectList().isAffectedBySkill(skill.getId())) {
                                    caster.setTarget(caster);
                                    caster.doCast(skill);
                                    caster.setTarget(target);
                                 }
                              } else {
                                 caster.doCast(skill);
                              }
                           } else {
                              caster.setTarget(caster);
                              caster.doCast(skill);
                              caster.setTarget(target);
                           }
                        }
                     }

                     if (player.isAutoPlaying()) {
                        int count = player.getAutoUseSettings().getAutoSkills().size();

                        for (int i = 0; i < count && !player.isCastingNow() && !player.isTeleporting(); i++) {
                           Playable petx = null;
                           WorldObject target = player.getTarget();
                           Integer skillId = player.getAutoUseSettings().getNextSkillId();
                           Skill skillx = player.getKnownSkill(skillId);
                           if (skillx == null) {
                              if (player.hasServitor() || player.hasPet()) {
                                 Summon summonx = player.getSummon();
                                 skillx = summonx.getKnownSkill(skillId);
                                 if (skillx == null) {
                                    skillx = PetSkillData.getInstance().getKnownSkill(summonx, skillId);
                                 }

                                 if (skillx != null) {
                                    petx = summonx;
                                    summonx.setTarget(target);
                                 }
                              }

                              if (skillx == null) {
                                 player.getAutoUseSettings().getAutoSkills().remove(skillId);
                                 player.getAutoUseSettings().resetSkillOrder();
                                 break;
                              }
                           }

                           if (target == player) {
                              break;
                           }

                           if (target == null || target.asCreature().isDead()) {
                              if (player.getQueuedSkill() != null) {
                                 player.setQueuedSkill(null, false, false);
                              }
                              break;
                           }

                           if (target.isInsideZone(ZoneId.PEACE) || !target.isAutoAttackable(player)) {
                              break;
                           }

                           if (target instanceof Guard) {
                              int targetMode = player.getAutoPlaySettings().getNextTargetMode();
                              if (targetMode != 3 && targetMode != 0) {
                                 break;
                              }
                           }

                           player.getAutoUseSettings().incrementSkillOrder();
                           Playable caster = (Playable)(petx != null ? petx : player);
                           if (player.getAutoUseSettings().isSelfCastOnly() && !skillx.isDamage()
                              && (skillx.getEffectPoint() > 0) && (skillx.getTargetType() != TargetType.SELF)) {
                              // Auto self-cast only (FakePlayersAI bots): a beneficial non-damage skill
                              // with a friendly target (heals, Touch of Life, ...) is cast on the bot
                              // itself - never on the targeted enemy. Enemy debuffs (effectPoint <= 0)
                              // and attacks are cast at the enemy as usual.
                              caster.setTarget(caster);
                              if (this.canUseMagic(caster, caster, skillx)) {
                                 caster.useMagic(skillx, true, false);
                                 caster.setTarget(target);
                                 break;
                              }

                              caster.setTarget(target);
                           } else if (this.canUseMagic(caster, target, skillx)) {
                              caster.useMagic(skillx, true, false);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private boolean canCastBuff(Player player, WorldObject target, Skill skill) {
         if (target != null
            && target.isCreature()
            && target.asCreature().isAlikeDead()
            && skill.getTargetType() != TargetType.SELF
            && skill.getTargetType() != TargetType.CORPSE
            && skill.getTargetType() != TargetType.PC_BODY) {
            return false;
         } else {
            Playable playableTarget = (Playable)(target != null && target.isPlayable() && skill.getTargetType() != TargetType.SELF
               ? target.asPlayable()
               : player);
            if (player != playableTarget && player.calculateDistance3D(playableTarget) > skill.getCastRange()) {
               return false;
            } else if (!this.canUseMagic(player, playableTarget, skill)) {
               return false;
            } else {
               BuffInfo buffInfo = playableTarget.getEffectList().getBuffInfoBySkillId(skill.getId());
               BuffInfo abnormalBuffInfo = playableTarget.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
               if (abnormalBuffInfo == null) {
                  return buffInfo == null;
               } else {
                  return buffInfo != null
                     ? abnormalBuffInfo.getSkill().getId() == buffInfo.getSkill().getId()
                        && (buffInfo.getTime() <= 3 || buffInfo.getSkill().getLevel() < skill.getLevel())
                     : abnormalBuffInfo.getSkill().getAbnormalLevel() < skill.getAbnormalLevel() || abnormalBuffInfo.isAbnormalType(AbnormalType.NONE);
               }
            }
         }
      }

      private boolean canUseMagic(Playable playable, WorldObject target, Skill skill) {
         if (skill.getItemConsumeCount() > 0 && playable.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()) {
            return false;
         } else if (playable.isPlayer() && playable.asPlayer().getCharges() < skill.getChargeConsumeCount()) {
            return false;
         } else {
            int mpConsume = skill.getMpInitialConsume() + skill.getMpConsume();
            if (mpConsume > 0 && playable.getCurrentMp() < mpConsume) {
               return false;
            } else {
               return skill.getId() == 254 && target != null && target.isMonster() && target.asMonster().isSpoiled()
                  ? false
                  : !playable.isSkillDisabled(skill) && skill.checkCondition(playable, target, false);
            }
         }
      }
   }

   private static class SingletonHolder {
      protected static final AutoUseTaskManager INSTANCE = new AutoUseTaskManager();
   }
}
