package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

class OlympiadGameTask implements Runnable {
   protected static final Logger _log = Logger.getLogger(OlympiadGameTask.class.getName());
   public OlympiadGame _game = null;
   protected static final long BATTLE_PERIOD = OlympiadConfig.OLYMPIAD_BATTLE;
   protected static final byte BATTLE_BEGIN_TIME = 15;
   private static final SkillHolder[] FIGHTER_BUFFS = new SkillHolder[]{new SkillHolder(1204, 2), new SkillHolder(1086, 1)};
   private static final SkillHolder[] MAGE_BUFFS = new SkillHolder[]{new SkillHolder(1204, 2), new SkillHolder(1086, 1)};
   private boolean _terminated = false;
   private boolean _started = false;

   public boolean isTerminated() {
      return this._terminated || this._game._aborted;
   }

   public boolean isStarted() {
      return this._started;
   }

   public OlympiadGameTask(OlympiadGame var1) {
      this._game = var1;
   }

   protected boolean checkBattleStatus() {
      boolean var1 = this._game._playerOne == null || this._game._playerOneDisconnected;
      boolean var2 = this._game._playerTwo == null || this._game._playerTwoDisconnected;
      return !var1 && !var2 && !this._game._aborted;
   }

   protected boolean checkDefaulted() {
      this._game._playerOne = World.getPlayer(this._game._playerOneID);
      this._game._players.set(0, this._game._playerOne);
      this._game._playerTwo = World.getPlayer(this._game._playerTwoID);
      this._game._players.set(1, this._game._playerTwo);

      for (int var1 = 0; var1 < 2; var1++) {
         boolean var2 = false;
         Player var3 = (Player)this._game._players.get(var1);
         if (var3 != null) {
            var3.setOlympiadGameId(this._game._stadiumID);
         }

         Player var4 = (Player)this._game._players.get(var1 ^ 1);
         SystemMessage var5 = null;
         if (var3 == null) {
            var2 = true;
         } else if (var3.isDead() && !this.reviveDeadParticipant(var3)) {
            var5 = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
            var5.addPcName(var3);
            var2 = true;
         } else if (var3.isSubClassActive()) {
            var5 = new SystemMessage(
               SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_TO_YOUR_SUB_CLASS
            );
            var5.addPcName(var3);
            var2 = true;
         } else if (var3.isCursedWeaponEquipped()) {
            var5 = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
            var5.addPcName(var3);
            var5.addItemName(var3.getCursedWeaponEquippedId());
            var2 = true;
         } else if (var3.getInventoryLimit() * 0.8 <= var3.getInventory().getSize()) {
            var5 = new SystemMessage(
               SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80
            );
            var5.addPcName(var3);
            var2 = true;
         } else if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0
            && !AntiFeedManager.getInstance().tryAddPlayer(1, var3, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)) {
            NpcHtmlMessage var6 = new NpcHtmlMessage(var3.getLastHtmlActionOriginId());
            var6.setFile(var3, "data/html/mods/OlympiadIPRestriction.htm");
            var6.replace(
               "%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(var3, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
            );
            var3.sendPacket(var6);
            var2 = true;
         }

         if (var2) {
            if (var3 != null && var5 != null) {
               var3.sendPacket(var5);
            }

            if (var4 != null) {
               var4.sendPacket(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
            }

            if (var1 == 0) {
               this._game._playerOneDefaulted = true;
            } else {
               this._game._playerTwoDefaulted = true;
            }
         }
      }

      return this._game._playerOneDefaulted || this._game._playerTwoDefaulted;
   }

   private boolean reviveDeadParticipant(Player var1) {
      for (Predicate var3 : Olympiad.DEAD_PARTICIPANT_REVIVE_HOOKS) {
         try {
            if (var3.test(var1)) {
               _log.info("Olympiad: dead participant " + var1.getName() + " was revived before the match; the match proceeds.");
               return true;
            }
         } catch (Exception var5) {
            _log.warning("Olympiad: dead-participant revive hook failed for " + var1.getName() + ": " + var5);
         }
      }

      return false;
   }

   @Override
   public void run() {
      this._started = true;
      if (this._game != null) {
         if (this._game._playerOne == null || this._game._playerTwo == null) {
            return;
         }

         if (this.teleportCountdown()) {
            this.runGame();
         }

         this._game.validateWinner();
         this._game.PlayersStatusBack();
         this._game.cleanEffects();
         if (this._game._gamestarted) {
            this._game._gamestarted = false;

            try {
               this._game.portPlayersBack();
            } catch (Exception var3) {
               _log.log(Level.WARNING, "Exception on portPlayersBack(): " + var3.getMessage(), (Throwable)var3);
            }
         }

         if (OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators() != null) {
            for (Player var2 : OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators()) {
               if (var2 != null) {
                  var2.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
               }
            }
         }

         this._game.clearPlayers();
         OlympiadManager.getInstance().removeGame(this._game);
         this._game = null;
         this._terminated = true;
      }
   }

   protected void healplayer() {
      for (Player var2 : this._game._players) {
         if (var2 != null) {
            var2.setCurrentCp(var2.getMaxCp());
            var2.setCurrentHp(var2.getMaxHp());
            var2.setCurrentMp(var2.getMaxMp());
         }
      }
   }

   private void applyBuffs() {
      for (Player var2 : this._game._players) {
         if (var2 != null) {
            SkillHolder[] var3 = var2.isMageClass() ? MAGE_BUFFS : FIGHTER_BUFFS;

            for (SkillHolder var7 : var3) {
               Skill var8 = var7.getSkill();
               if (var8 != null) {
                  var2.broadcastPacket(new MagicSkillUse(var2, var2, var8.getId(), var8.getLevel(), var8.getHitTime(), var8.getReuseDelay()));
                  var8.applyEffects(var2, var2);
               }
            }
         }
      }
   }

   private static void applyClassStartState(Player var0) {
      int var1 = var0.getPlayerClass().getId();
      boolean var2 = var1 == 113;
      boolean var3 = var1 == 114;
      if (var2 || var3) {
         var0.setCurrentHp(var0.getMaxHp() * 0.9);
         var0.setCurrentCp(var0.getMaxCp() * 0.2);
         SkillData var4 = SkillData.getInstance();
         int var5 = var4.getMaxLevel(176);
         int var6 = var4.getMaxLevel(420);
         if (var2 && var5 > 0) {
            Skill var7 = var4.getSkill(176, var5);
            if (var7 != null) {
               var0.broadcastPacket(new MagicSkillUse(var0, var0, var7.getId(), var7.getLevel(), var7.getHitTime(), var7.getReuseDelay()));
               var7.applyEffects(var0, var0);
            }
         }

         if (var6 > 0) {
            Skill var8 = var4.getSkill(420, var6);
            if (var8 != null) {
               var0.broadcastPacket(new MagicSkillUse(var0, var0, var8.getId(), var8.getLevel(), var8.getHitTime(), var8.getReuseDelay()));
               var8.applyEffects(var0, var0);
            }
         }

         var0.broadcastUserInfo();
         _log.info(
            "Olympiad: class start state applied for " + var0.getName() + " (classId " + var1 + "): 90% HP, 20% CP, " + (var2 ? "Frenzy + Zealot." : "Zealot.")
         );
      }
   }

   private boolean runGame() {
      if (this.checkDefaulted()) {
         return false;
      } else {
         this._game.portPlayersToArena();
         this._game.removals();
         if (OlympiadConfig.OLYMPIAD_ANNOUNCE_GAMES) {
            this._game.announceGame();
         }

         try {
            Thread.sleep(5000L);
         } catch (Exception var11) {
         }

         synchronized (this) {
            if (!OlympiadGame._battleStarted) {
               OlympiadGame._battleStarted = true;
            }
         }

         this._game._damageP1 = 0;
         this._game._damageP2 = 0;
         this.applyBuffs();
         this.healplayer();
         byte var1 = 5;

         for (byte var2 = 15; var2 > 0; var2 -= var1) {
            SystemMessage var3 = new SystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
            var3.addInt(var2);
            this._game.broadcastMessage(var3, true);
            if (var2 == 2) {
               for (Player var5 : this._game._players) {
                  if (var5 != null) {
                     applyClassStartState(var5);

                     for (Consumer var7 : Olympiad.PRE_START_HOOKS) {
                        try {
                           var7.accept(var5);
                        } catch (Exception var10) {
                           _log.warning("Olympiad pre-start hook failed for " + var5.getName() + ": " + var10);
                        }
                     }
                  }
               }
            }

            if (var2 == 1) {
               for (Player var17 : this._game._players) {
                  if (var17 != null) {
                     int var19 = var17.getPlayerClass().getId();
                     if (var19 != 113 && var19 != 114) {
                        var17.setCurrentCp(var17.getMaxCp());
                        var17.setCurrentHp(var17.getMaxHp());
                        var17.setCurrentMp(var17.getMaxMp());
                        var17.broadcastStatusUpdate();
                        _log.info("Olympiad: full heal applied 1 second before the match for " + var17.getName() + " (classId " + var19 + ").");
                     }
                  }
               }

               for (Player var18 : this._game._players) {
                  if (var18 != null && var18.isGM() && var18.isInvul()) {
                     var18.setInvul(false);
                     var18.sendSysMessage("Your invulnerability has been forcibly cancelled: the Olympiad battle is about to begin.");
                     _log.info("Olympiad: admin invul forcibly cancelled 1 second before the match for " + var18.getName() + ".");
                  }
               }
            }

            switch (var2) {
               case 5:
                  var1 = 1;
                  break;
               case 10:
                  var1 = 5;
            }

            try {
               Thread.sleep(var1 * 1000);
            } catch (Exception var9) {
            }
         }

         if (!this.checkBattleStatus()) {
            return false;
         } else if (!this._game.makeCompetitionStart()) {
            return false;
         } else {
            this._game._playerOne.broadcastInfo();
            this._game._playerTwo.broadcastInfo();
            this.broadcastBattleState();

            for (short var14 = 0; var14 < BATTLE_PERIOD; var14 = (short)(var14 + 10000)) {
               try {
                  Thread.sleep(10000L);
                  if (this._game.haveWinner()) {
                     break;
                  }
               } catch (Exception var12) {
               }

               this.broadcastBattleState();
            }

            this._game._playerOne.broadcastInfo();
            this._game._playerTwo.broadcastInfo();
            return this.checkBattleStatus();
         }
      }
   }

   private void broadcastBattleState() {
      this.broadcastPlayerState(this._game._playerOne);
      this.broadcastPlayerState(this._game._playerTwo);
   }

   private void broadcastPlayerState(Player var1) {
      if (var1 != null) {
         Player var2 = null;

         for (Player var4 : this._game._players) {
            if (var4 != null && var4 != var1) {
               var2 = var4;
               break;
            }
         }

         if (var2 != null) {
            var2.sendPacket(new ExOlympiadUserInfo(var1, 1));
         }

         ExOlympiadSpelledInfo var9 = null;
         List var5 = var1.getEffectList().getEffects();
         if (!var5.isEmpty()) {
            var9 = new ExOlympiadSpelledInfo(var1);

            for (BuffInfo var7 : var5) {
               if (var7 != null && var7.isInUse()) {
                  var9.addSkill(var7);
               }
            }
         }

         ExOlympiadSpelledInfo var10 = var9;
         if (OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators() != null) {
            for (Player var8 : OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators()) {
               if (var8 != null) {
                  var8.sendPacket(new ExOlympiadUserInfo(var1, var1 == this._game._playerOne ? 1 : 2));
                  if (var10 != null) {
                     var8.sendPacket(var10);
                  }
               }
            }
         }

         if (var2 != null && var10 != null) {
            var2.sendPacket(var10);
         }
      }
   }

   private boolean teleportCountdown() {
      int var1 = OlympiadConfig.OLYMPIAD_WAIT_TIME;

      for (int var2 = var1 > 60 ? 60 : (var1 > 30 ? 30 : (var1 > 10 ? 5 : 1)); var1 > 0; var2 = var1 > 60 ? 60 : (var1 > 30 ? 30 : (var1 > 10 ? 5 : 1))) {
         SystemMessage var3 = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
         var3.addInt(var1);
         this._game.broadcastMessage(var3, false);

         try {
            Thread.sleep(var2 * 1000L);
         } catch (InterruptedException var5) {
            return false;
         }

         var1 -= var2;
      }

      return true;
   }
}
