package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public class PlayerStatus extends PlayableStatus {
   private double _currentCp = 0.0;

   public PlayerStatus(Player player) {
      super(player);
   }

   @Override
   public void reduceCp(int value) {
      this.setCurrentCp(this._currentCp > value ? this._currentCp - value : 0.0);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false, false);
   }

   @Override
   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption) {
      this.reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
   }

   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP) {
      Player player = this.getActiveChar();
      if (!player.isDead()) {
         if (!OfflineTradeConfig.OFFLINE_MODE_NO_DAMAGE
            || player.getClient() == null
            || !player.getClient().isDetached()
            || (
                  !OfflineTradeConfig.OFFLINE_TRADE_ENABLE
                     || player.getPrivateStoreType() != PrivateStoreType.SELL && player.getPrivateStoreType() != PrivateStoreType.BUY
               )
               && (!OfflineTradeConfig.OFFLINE_CRAFT_ENABLE || !player.isCrafting() && player.getPrivateStoreType() != PrivateStoreType.MANUFACTURE)) {
            if (!player.isInvul() || isDOT || isHPConsumption) {
               if (!isHPConsumption) {
                  player.stopEffectsOnDamage(awake);
                  if (player.isCrafting() || player.isInStoreMode()) {
                     player.setPrivateStoreType(PrivateStoreType.NONE);
                     player.standUp();
                     player.broadcastUserInfo();
                  } else if (player.isSitting()) {
                     player.standUp();
                  }

                  if (!isDOT && player.isStunned() && Rnd.get(10) == 0) {
                     player.stopStunning(true);
                  }
               }

               double amount = value;
               int fullValue = (int)value;
               int tDmg = 0;
               int mpDam = 0;
               if (attacker != null && attacker != player) {
                  Player attackerPlayer = attacker.asPlayer();
                  if (attackerPlayer != null) {
                     if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage()) {
                        return;
                     }

                     if (player.isInDuel()) {
                        if (player.getDuelState() == 2) {
                           return;
                        }

                        if (player.getDuelState() == 3) {
                           return;
                        }

                        if (attackerPlayer.getDuelId() != player.getDuelId()) {
                           player.setDuelState(4);
                        }
                     }
                  }

                  PlayerStat stat = player.getStat();
                  Summon summon = player.getSummon();
                  if (player.hasServitor() && LocationUtil.checkIfInRange(1000, player, summon, true)) {
                     tDmg = (int)value * (int)stat.calcStat(Stat.TRANSFER_DAMAGE_PERCENT, 0.0, null, null) / 100;
                     tDmg = Math.min((int)summon.getCurrentHp() - 1, tDmg);
                     if (tDmg > 0) {
                        summon.reduceCurrentHp(tDmg, attacker, null);
                        amount = value - tDmg;
                        fullValue = (int)amount;
                     }
                  }

                  mpDam = (int)amount * (int)stat.calcStat(Stat.MANA_SHIELD_PERCENT, 0.0, null, null) / 100;
                  if (mpDam > 0) {
                     mpDam = (int)(amount - mpDam);
                     if (!(mpDam > player.getCurrentMp())) {
                        player.reduceCurrentMp(mpDam);
                        player.sendMessage("Arcane Shield decreased your MP by " + mpDam + " instead of HP.");
                        return;
                     }

                     player.sendMessage("MP became 0 and the Arcane Shield is disappearing.");
                     player.stopSkillEffects(SkillFinishType.REMOVED, 1556);
                     amount = mpDam - player.getCurrentMp();
                     player.setCurrentMp(0.0);
                  }

                  Player caster = player.getTransferingDamageTo();
                  if (caster != null
                     && player.getParty() != null
                     && LocationUtil.checkIfInRange(1000, player, caster, true)
                     && !caster.isDead()
                     && player != caster
                     && player.getParty().getMembers().contains(caster)) {
                     int transferDmg = Math.min(
                        (int)caster.getCurrentHp() - 1, (int)amount * (int)stat.calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0.0, null, null) / 100
                     );
                     if (transferDmg > 0) {
                        int membersInRange = 0;

                        for (Player member : caster.getParty().getMembers()) {
                           if (LocationUtil.checkIfInRange(1000, member, caster, false) && member != caster) {
                              membersInRange++;
                           }
                        }

                        if ((attacker.isPlayable() || attacker.isFakePlayer()) && caster.getCurrentCp() > 0.0) {
                           if (caster.getCurrentCp() > transferDmg) {
                              caster.getStatus().reduceCp(transferDmg);
                           } else {
                              transferDmg = (int)(transferDmg - caster.getCurrentCp());
                              caster.getStatus().reduceCp((int)caster.getCurrentCp());
                           }
                        }

                        if (membersInRange > 0) {
                           caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
                           amount -= transferDmg;
                           fullValue = (int)amount;
                        }
                     }
                  }

                  if (!ignoreCP && (attacker.isPlayable() || attacker.isFakePlayer())) {
                     if (this._currentCp >= amount) {
                        this.setCurrentCp(this._currentCp - amount);
                        amount = 0.0;
                     } else {
                        amount -= this._currentCp;
                        this.setCurrentCp(0.0, false);
                     }
                  }

                  if (fullValue > 0 && !isDOT) {
                     SystemMessage smsg = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
                     smsg.addString(player.getName());
                     String targetName = attacker.getName();
                     if (MultilingualSupportConfig.MULTILANG_ENABLE && attacker.isNpc()) {
                        String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(player.getLang(), attacker.getId());
                        if (localisation != null) {
                           targetName = localisation[0];
                        }
                     }

                     smsg.addString(targetName);
                     smsg.addInt(fullValue);
                     player.sendPacket(smsg);
                     if (tDmg > 0 && attackerPlayer != null) {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THE_SERVITOR);
                        smsg.addInt(fullValue);
                        smsg.addInt(tDmg);
                        attackerPlayer.sendPacket(smsg);
                     }
                  }
               }

               if (amount > 0.0) {
                  amount = this.getCurrentHp() - amount;
                  if (amount <= 0.0) {
                     if (player.isInDuel()) {
                        player.disableAllSkills();
                        this.stopHpMpRegeneration();
                        if (attacker != null) {
                           attacker.getAI().setIntentionActive();
                           attacker.sendPacket(ActionFailed.STATIC_PACKET);
                           attacker.setTarget(null);
                           attacker.abortAttack();
                        }

                        DuelManager.getInstance().onPlayerDefeat(player);
                        amount = 1.0;
                     } else {
                        amount = 0.0;
                     }
                  }

                  this.setCurrentHp(amount);
               }

               if (player.getCurrentHp() < 0.5 && !isHPConsumption) {
                  player.abortAttack();
                  player.abortCast();
                  if (player.isInOlympiadMode()) {
                     this.stopHpMpRegeneration();
                     player.setDead(true);
                     player.setIsPendingRevive(true);
                     if (player.hasSummon()) {
                        player.getSummon().getAI().setIntentionIdle();
                     }

                     return;
                  }

                  player.doDie(attacker);
                  if (!PlayerConfig.DISABLE_TUTORIAL) {
                     QuestState qs = player.getQuestState("Q00255_Tutorial");
                     if (qs != null) {
                        qs.getQuest().notifyEvent("CE30", null, player);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean setCurrentHp(double newHp, boolean broadcastPacket) {
      boolean result = super.setCurrentHp(newHp, broadcastPacket);
      Player player = this.getActiveChar();
      if (!PlayerConfig.DISABLE_TUTORIAL && this.getCurrentHp() <= player.getStat().getMaxHp() * 0.3) {
         QuestState qs = player.getQuestState("Q00255_Tutorial");
         if (qs != null) {
            qs.getQuest().notifyEvent("CE45", null, player);
         }
      }

      return result;
   }

   @Override
   public double getCurrentCp() {
      return this._currentCp;
   }

   @Override
   public void setCurrentCp(double newCp) {
      this.setCurrentCp(newCp, true);
   }

   public void setCurrentCp(double value, boolean broadcastPacket) {
      int currentCp = (int)this._currentCp;
      Player player = this.getActiveChar();
      int maxCp = player.getStat().getMaxCp();
      synchronized (this) {
         if (player.isDead()) {
            return;
         }

         double newCp = Math.max(0.0, value);
         if (newCp >= maxCp) {
            this._currentCp = maxCp;
            this._flagsRegenActive &= -5;
            if (this._flagsRegenActive == 0) {
               this.stopHpMpRegeneration();
            }
         } else {
            this._currentCp = newCp;
            this._flagsRegenActive = (byte)(this._flagsRegenActive | 4);
            this.startHpMpRegeneration();
         }
      }

      if (currentCp != this._currentCp && broadcastPacket) {
         player.broadcastStatusUpdate();
      }
   }

   @Override
   protected void doRegeneration() {
      Player player = this.getActiveChar();
      PlayerStat stat = player.getStat();
      if (this._currentCp < stat.getMaxRecoverableCp()) {
         this.setCurrentCp(this._currentCp + Formulas.calcCpRegen(player), false);
      }

      if (this.getCurrentHp() < stat.getMaxRecoverableHp()) {
         this.setCurrentHp(this.getCurrentHp() + Formulas.calcHpRegen(player), false);
      }

      if (this.getCurrentMp() < stat.getMaxRecoverableMp()) {
         this.setCurrentMp(this.getCurrentMp() + Formulas.calcMpRegen(player), false);
      }

      player.broadcastStatusUpdate();
   }

   public Player getActiveChar() {
      return super.getActiveChar().asPlayer();
   }
}
