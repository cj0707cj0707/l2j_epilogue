package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

class OlympiadGameTask implements Runnable {
   protected static final Logger _log = Logger.getLogger(OlympiadGameTask.class.getName());
   public OlympiadGame _game = null;
   protected static final long BATTLE_PERIOD = OlympiadConfig.OLYMPIAD_BATTLE;
   /** Battle preparation countdown, seconds (custom: 15 instead of retail 60). */
   protected static final byte BATTLE_BEGIN_TIME = 15;
   private static final SkillHolder[] FIGHTER_BUFFS = new SkillHolder[]{new SkillHolder(1204, 2), new SkillHolder(1086, 1)};
   private static final SkillHolder[] MAGE_BUFFS = new SkillHolder[]{new SkillHolder(1204, 2), new SkillHolder(1085, 1)};
   private boolean _terminated = false;
   private boolean _started = false;

   public boolean isTerminated() {
      return this._terminated || this._game._aborted;
   }

   public boolean isStarted() {
      return this._started;
   }

   public OlympiadGameTask(OlympiadGame game) {
      this._game = game;
   }

   protected boolean checkBattleStatus() {
      boolean pOneCrash = this._game._playerOne == null || this._game._playerOneDisconnected;
      boolean pTwoCrash = this._game._playerTwo == null || this._game._playerTwoDisconnected;
      return !pOneCrash && !pTwoCrash && !this._game._aborted;
   }

   protected boolean checkDefaulted() {
      this._game._playerOne = World.getPlayer(this._game._playerOneID);
      this._game._players.set(0, this._game._playerOne);
      this._game._playerTwo = World.getPlayer(this._game._playerTwoID);
      this._game._players.set(1, this._game._playerTwo);

      for (int i = 0; i < 2; i++) {
         boolean defaulted = false;
         Player player = this._game._players.get(i);
         if (player != null) {
            player.setOlympiadGameId(this._game._stadiumID);
         }

         Player otherPlayer = this._game._players.get(i ^ 1);
         SystemMessage sm = null;
         if (player == null) {
            defaulted = true;
         } else if (player.isDead()) {
            sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
            sm.addPcName(player);
            defaulted = true;
         } else if (player.isSubClassActive()) {
            sm = new SystemMessage(
               SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_TO_YOUR_SUB_CLASS
            );
            sm.addPcName(player);
            defaulted = true;
         } else if (player.isCursedWeaponEquipped()) {
            sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
            sm.addPcName(player);
            sm.addItemName(player.getCursedWeaponEquippedId());
            defaulted = true;
         } else if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize()) {
            sm = new SystemMessage(
               SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80
            );
            sm.addPcName(player);
            defaulted = true;
         } else if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0
            && !AntiFeedManager.getInstance().tryAddPlayer(1, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)) {
            NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
            message.setFile(player, "data/html/mods/OlympiadIPRestriction.htm");
            message.replace(
               "%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
            );
            player.sendPacket(message);
            defaulted = true;
         }

         if (defaulted) {
            if (player != null && sm != null) {
               player.sendPacket(sm);
            }

            if (otherPlayer != null) {
               otherPlayer.sendPacket(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
            }

            if (i == 0) {
               this._game._playerOneDefaulted = true;
            } else {
               this._game._playerTwoDefaulted = true;
            }
         }
      }

      return this._game._playerOneDefaulted || this._game._playerTwoDefaulted;
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
            for (Player spec : OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators()) {
               if (spec != null) {
                  spec.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
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
      for (Player player : this._game._players) {
         if (player != null) {
            player.setCurrentCp(player.getMaxCp());
            player.setCurrentHp(player.getMaxHp());
            player.setCurrentMp(player.getMaxMp());
         }
      }
   }

   private void applyBuffs() {
      for (Player player : this._game._players) {
         if (player != null) {
            SkillHolder[] buffs = player.isMageClass() ? MAGE_BUFFS : FIGHTER_BUFFS;

            for (SkillHolder buff : buffs) {
               Skill skill = buff.getSkill();
               if (skill != null) {
                  player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
                  skill.applyEffects(player, player);
               }
            }
         }
      }
   }

   /**
    * Class-specific starting state, applied exactly 2 seconds before the battle starts (after the
    * core standard buffs and the pre-battle full heal): Titan (113) enters the fight with 90% HP,
    * 20% CP and the Frenzy + Zealot buffs; Grand Khavatari (114) with 90% HP, 20% CP and Zealot.
    * Buffs are applied instantly (no cast time, no condition checks), the same way as applyBuffs().
    */
   private static void applyClassStartState(Player player) {
      final int classId = player.getPlayerClass().getId();
      final boolean titan = classId == 113;
      final boolean khavatari = classId == 114;
      if (!titan && !khavatari) {
         return;
      }

      player.setCurrentHp(player.getMaxHp() * 0.9);
      player.setCurrentCp(player.getMaxCp() * 0.2);

      final SkillData data = SkillData.getInstance();
      final int frenzyLevel = data.getMaxLevel(176); // Frenzy
      final int zealotLevel = data.getMaxLevel(420); // Zealot
      if (titan && (frenzyLevel > 0)) {
         final Skill frenzy = data.getSkill(176, frenzyLevel);
         if (frenzy != null) {
            player.broadcastPacket(new MagicSkillUse(player, player, frenzy.getId(), frenzy.getLevel(), frenzy.getHitTime(), frenzy.getReuseDelay()));
            frenzy.applyEffects(player, player);
         }
      }

      if (zealotLevel > 0) {
         final Skill zealot = data.getSkill(420, zealotLevel);
         if (zealot != null) {
            player.broadcastPacket(new MagicSkillUse(player, player, zealot.getId(), zealot.getLevel(), zealot.getHitTime(), zealot.getReuseDelay()));
            zealot.applyEffects(player, player);
         }
      }

      player.broadcastUserInfo();
      _log.info("Olympiad: class start state applied for " + player.getName() + " (classId " + classId + "): 90% HP, 20% CP, " + (titan ? "Frenzy + Zealot." : "Zealot."));
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
         } catch (Exception var6) {
         }

         synchronized (this) {
            if (!OlympiadGame._battleStarted) {
               OlympiadGame._battleStarted = true;
            }
         }

         // Battle preparation countdown (custom: 15 seconds instead of retail 60).
         final byte battleBeginTime = BATTLE_BEGIN_TIME;
         this._game._damageP1 = 0;
         this._game._damageP2 = 0;
         this.applyBuffs();
         this.healplayer();

         byte step = 5;

         for (byte i = battleBeginTime; i > 0; i -= step) {
            SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
            sm.addInt(i);
            this._game.broadcastMessage(sm, true);

            if (i == 2) {
               // Exactly two seconds before the battle: apply the class-specific starting state
               // (Titan / Grand Khavatari) and fire the script pre-start hooks (FakePlayersAI).
               for (Player participant : this._game._players) {
                  if (participant != null) {
                     OlympiadGameTask.applyClassStartState(participant);

                     for (Consumer<Player> hook : Olympiad.PRE_START_HOOKS) {
                        try {
                           hook.accept(participant);
                        } catch (Exception e) {
                           _log.warning("Olympiad pre-start hook failed for " + participant.getName() + ": " + e);
                        }
                     }
                  }
               }
            }

            switch (i) {
               case 5:
                  step = 1;
                  break;
               case 10:
                  step = 5;
            }

            try {
               Thread.sleep(step * 1000);
            } catch (Exception var5) {
            }
         }

         if (!this.checkBattleStatus()) {
            return false;
         } else if (!this._game.makeCompetitionStart()) {
            return false;
         } else {
            this._game._playerOne.broadcastInfo();
            this._game._playerTwo.broadcastInfo();
            this._game._playerOne.sendPacket(new ExOlympiadUserInfo(this._game._playerTwo, 1));
            this._game._playerTwo.sendPacket(new ExOlympiadUserInfo(this._game._playerOne, 1));
            if (OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators() != null) {
               for (Player spec : OlympiadManager.STADIUMS[this._game._stadiumID].getSpectators()) {
                  if (spec != null) {
                     spec.sendPacket(new ExOlympiadUserInfo(this._game._playerOne, 1));
                     spec.sendPacket(new ExOlympiadUserInfo(this._game._playerTwo, 2));
                  }
               }
            }

            for (int i = 0; i < BATTLE_PERIOD; i += 10000) {
               try {
                  Thread.sleep(10000L);
                  if (this._game.haveWinner()) {
                     break;
                  }
               } catch (Exception var7) {
               }
            }

            this._game._playerOne.broadcastInfo();
            this._game._playerTwo.broadcastInfo();
            return this.checkBattleStatus();
         }
      }
   }

   private boolean teleportCountdown() {
      // Custom: generic countdown that is exact for any allowed wait time
      // (120, 60, 30, 15, 10, 5): coarse steps first, 1-second ticks for the
      // last 10 seconds. The retail switch below only worked for 120/60/30/15/5.
      int remaining = OlympiadConfig.OLYMPIAD_WAIT_TIME;
      int sleep = remaining > 60 ? 60 : remaining > 30 ? 30 : remaining > 10 ? 5 : 1;

      while (remaining > 0) {
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
         sm.addInt(remaining);
         this._game.broadcastMessage(sm, false);

         try {
            Thread.sleep(sleep * 1000L);
         } catch (InterruptedException var5) {
            return false;
         }

         remaining -= sleep;
         sleep = remaining > 60 ? 60 : remaining > 30 ? 30 : remaining > 10 ? 5 : 1;
      }

      return true;
   }
}
