package org.l2jmobius.gameserver.mechanics.fishing;

import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.FishingMonstersData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.FishingChampionshipManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingHpRegen;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingStartCombat;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Fishing implements Runnable {
   private Player _fisher;
   private int _time;
   private int _stop = 0;
   private int _goodUse = 0;
   private int _anim = 0;
   private int _mode = 0;
   private int _deceptiveMode = 0;
   private Future<?> _fishAiTask;
   private boolean _thinking;
   private final int _fishId;
   private final int _fishMaxHp;
   private int _fishCurHp;
   private final double _regenHp;
   private final boolean _isUpperGrade;
   private final int _lureId;

   @Override
   public void run() {
      if (this._fisher != null) {
         if (this._fishCurHp >= this._fishMaxHp * 2) {
            this._fisher.sendPacket(SystemMessageId.YOUR_BAIT_WAS_STOLEN_BY_THAT_FISH);
            this.doDie(false);
         } else if (this._time <= 0) {
            this._fisher.sendPacket(SystemMessageId.THAT_FISH_IS_MORE_DETERMINED_THAN_YOU_ARE_IT_SPIT_THE_HOOK);
            this.doDie(false);
         } else {
            this.aiTask();
         }
      }
   }

   public Fishing(Player fisher, Fish fish, boolean isNoob, boolean isUpperGrade, int lureId) {
      this._fisher = fisher;
      this._fishMaxHp = fish.getFishHp();
      this._fishCurHp = this._fishMaxHp;
      this._regenHp = fish.getHpRegen();
      this._fishId = fish.getItemId();
      this._time = fish.getCombatDuration();
      this._isUpperGrade = isUpperGrade;
      this._lureId = lureId;
      int lureType;
      if (isUpperGrade) {
         this._deceptiveMode = Rnd.get(100) >= 90 ? 1 : 0;
         lureType = 2;
      } else {
         this._deceptiveMode = 0;
         lureType = isNoob ? 0 : 1;
      }

      this._mode = Rnd.get(100) >= 80 ? 1 : 0;
      this._fisher.broadcastPacket(new ExFishingStartCombat(this._fisher, this._time, this._fishMaxHp, this._mode, lureType, this._deceptiveMode));
      this._fisher.sendPacket(new PlaySound(1, "SF_S_01", 0, 0, 0, 0, 0));
      this._fisher.sendPacket(SystemMessageId.YOU_VE_GOT_A_BITE);
      if (this._fishAiTask == null) {
         this._fishAiTask = ThreadPool.scheduleAtFixedRate(this, 1000L, 1000L);
      }
   }

   public void changeHp(int hp, int pen) {
      this._fishCurHp -= hp;
      if (this._fishCurHp < 0) {
         this._fishCurHp = 0;
      }

      this._fisher
         .broadcastPacket(new ExFishingHpRegen(this._fisher, this._time, this._fishCurHp, this._mode, this._goodUse, this._anim, pen, this._deceptiveMode));
      this._anim = 0;
      if (this._fishCurHp > this._fishMaxHp * 2) {
         this._fishCurHp = this._fishMaxHp * 2;
         this.doDie(false);
      } else if (this._fishCurHp == 0) {
         this.doDie(true);
      }
   }

   public synchronized void doDie(boolean win) {
      if (this._fishAiTask != null) {
         this._fishAiTask.cancel(false);
         this._fishAiTask = null;
      }

      if (this._fisher != null) {
         if (win) {
            FishingMonster fishingMonster = FishingMonstersData.getInstance().getFishingMonster(this._fisher.getLevel());
            if (fishingMonster != null) {
               if (Rnd.get(100) <= fishingMonster.getProbability()) {
                  this._fisher.sendPacket(SystemMessageId.YOU_CAUGHT_SOMETHING_SMELLY_AND_SCARY_MAYBE_YOU_SHOULD_THROW_IT_BACK);
                  Npc monster = Quest.addSpawn(fishingMonster.getFishingMonsterId(), this._fisher);
                  monster.setTarget(this._fisher);
               } else {
                  this._fisher.sendPacket(SystemMessageId.YOU_CAUGHT_SOMETHING);
                  this._fisher.addItem(ItemProcessType.PICKUP, this._fishId, 1L, null, true);
                  FishingChampionshipManager.getInstance().newFish(this._fisher, this._lureId);
               }
            }
         }

         this._fisher.endFishing(win);
         this._fisher = null;
      }
   }

   protected void aiTask() {
      if (!this._thinking) {
         this._thinking = true;
         this._time--;

         try {
            if (this._mode == 1) {
               if (this._deceptiveMode == 0) {
                  this._fishCurHp = this._fishCurHp + (int)this._regenHp;
               }
            } else if (this._deceptiveMode == 1) {
               this._fishCurHp = this._fishCurHp + (int)this._regenHp;
            }

            if (this._stop == 0) {
               this._stop = 1;
               int check = Rnd.get(100);
               if (check >= 70) {
                  this._mode = this._mode == 0 ? 1 : 0;
               }

               if (this._isUpperGrade) {
                  check = Rnd.get(100);
                  if (check >= 90) {
                     this._deceptiveMode = this._deceptiveMode == 0 ? 1 : 0;
                  }
               }
            } else {
               this._stop--;
            }
         } finally {
            this._thinking = false;
            ExFishingHpRegen efhr = new ExFishingHpRegen(this._fisher, this._time, this._fishCurHp, this._mode, 0, this._anim, 0, this._deceptiveMode);
            if (this._anim != 0) {
               this._fisher.broadcastPacket(efhr);
            } else {
               this._fisher.sendPacket(efhr);
            }
         }
      }
   }

   public void useReeling(int dmg, int pen) {
      this._anim = 2;
      if (Rnd.get(100) > 90) {
         this._fisher.sendPacket(SystemMessageId.THE_FISH_HAS_RESISTED_YOUR_ATTEMPT_TO_BRING_IT_IN);
         this._goodUse = 0;
         this.changeHp(0, pen);
      } else if (this._fisher != null) {
         if (this._mode == 1) {
            if (this._deceptiveMode == 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_REEL_THAT_FISH_IN_CLOSER_AND_CAUSE_S1_DAMAGE);
               sm.addInt(dmg);
               this._fisher.sendPacket(sm);
               if (pen > 0) {
                  sm = new SystemMessage(SystemMessageId.YOUR_REELING_WAS_SUCCESSFUL_MASTERY_PENALTY_S1);
                  sm.addInt(pen);
                  this._fisher.sendPacket(sm);
               }

               this._goodUse = 1;
               this.changeHp(dmg, pen);
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FAILED_TO_REEL_THAT_FISH_IN_FURTHER_AND_IT_REGAINS_S1_HP);
               sm.addInt(dmg);
               this._fisher.sendPacket(sm);
               this._goodUse = 2;
               this.changeHp(-dmg, pen);
            }
         } else if (this._deceptiveMode == 0) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FAILED_TO_REEL_THAT_FISH_IN_FURTHER_AND_IT_REGAINS_S1_HP);
            sm.addInt(dmg);
            this._fisher.sendPacket(sm);
            this._goodUse = 2;
            this.changeHp(-dmg, pen);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_REEL_THAT_FISH_IN_CLOSER_AND_CAUSE_S1_DAMAGE);
            sm.addInt(dmg);
            this._fisher.sendPacket(sm);
            if (pen > 0) {
               sm = new SystemMessage(SystemMessageId.YOUR_REELING_WAS_SUCCESSFUL_MASTERY_PENALTY_S1);
               sm.addInt(pen);
               this._fisher.sendPacket(sm);
            }

            this._goodUse = 1;
            this.changeHp(dmg, pen);
         }
      }
   }

   public void usePumping(int dmg, int pen) {
      this._anim = 1;
      if (Rnd.get(100) > 90) {
         this._fisher.sendPacket(SystemMessageId.THE_FISH_HAS_RESISTED_YOUR_ATTEMPT_TO_BRING_IT_IN);
         this._goodUse = 0;
         this.changeHp(0, pen);
      } else if (this._fisher != null) {
         if (this._mode == 0) {
            if (this._deceptiveMode == 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PUMPING_IS_SUCCESSFUL_CAUSING_S1_DAMAGE);
               sm.addInt(dmg);
               this._fisher.sendPacket(sm);
               if (pen > 0) {
                  sm = new SystemMessage(SystemMessageId.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTY_S1);
                  sm.addInt(pen);
                  this._fisher.sendPacket(sm);
               }

               this._goodUse = 1;
               this.changeHp(dmg, pen);
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FAILED_TO_DO_ANYTHING_WITH_THE_FISH_AND_IT_REGAINS_S1_HP);
               sm.addInt(dmg);
               this._fisher.sendPacket(sm);
               this._goodUse = 2;
               this.changeHp(-dmg, pen);
            }
         } else if (this._deceptiveMode == 0) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FAILED_TO_DO_ANYTHING_WITH_THE_FISH_AND_IT_REGAINS_S1_HP);
            sm.addInt(dmg);
            this._fisher.sendPacket(sm);
            this._goodUse = 2;
            this.changeHp(-dmg, pen);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PUMPING_IS_SUCCESSFUL_CAUSING_S1_DAMAGE);
            sm.addInt(dmg);
            this._fisher.sendPacket(sm);
            if (pen > 0) {
               sm = new SystemMessage(SystemMessageId.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTY_S1);
               sm.addInt(pen);
               this._fisher.sendPacket(sm);
            }

            this._goodUse = 1;
            this.changeHp(dmg, pen);
         }
      }
   }
}
