package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.HeroSkillTable;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame.1;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

class OlympiadGame {
   protected static final Logger LOGGER = Logger.getLogger(OlympiadGame.class.getName());
   protected static final Logger _logResults = Logger.getLogger("olympiad");
   protected final CompetitionType _type;
   protected boolean _aborted;
   protected boolean _gamestarted;
   protected boolean _playerOneDisconnected;
   protected boolean _playerTwoDisconnected;
   protected boolean _playerOneDefaulted;
   protected boolean _playerTwoDefaulted;
   protected String _playerOneName;
   protected String _playerTwoName;
   protected int _playerOneID = 0;
   protected int _playerTwoID = 0;
   protected int _playerOneClass = 0;
   protected int _playerTwoClass = 0;
   protected static final int OLY_BUFFER = 36402;
   protected static final int OLY_MANAGER = 31688;
   protected static final byte TOWN_RETURN_TIME = 15;
   private static final String POINTS = "olympiad_points";
   private static final String COMP_DONE = "competitions_done";
   private static final String COMP_WON = "competitions_won";
   private static final String COMP_LOST = "competitions_lost";
   private static final String COMP_DRAWN = "competitions_drawn";
   protected static boolean _battleStarted;
   protected static boolean _gameIsStarted;
   protected long _startTime = 0L;
   public int _damageP1 = 0;
   public int _damageP2 = 0;
   public Player _playerOne;
   public Player _playerTwo;
   public Spawn _spawnOne;
   public Spawn _spawnTwo;
   protected List<Player> _players;
   private final int[] _stadiumPort;
   private int x1;
   private int y1;
   private int z1;
   private int x2;
   private int y2;
   private int z2;
   public final int _stadiumID;
   protected SystemMessage _sm;
   private SystemMessage _sm2;
   private SystemMessage _sm3;

   protected OlympiadGame(int var1, CompetitionType var2, List<Player> var3) {
      this._aborted = false;
      this._gamestarted = false;
      this._stadiumID = var1;
      this._playerOneDisconnected = false;
      this._playerTwoDisconnected = false;
      this._type = var2;
      this._stadiumPort = OlympiadManager.STADIUMS[var1].getCoordinates();
      if (var3 != null) {
         this._players = var3;
         this._playerOne = (Player)var3.get(0);
         this._playerTwo = (Player)var3.get(1);

         try {
            this._playerOneName = this._playerOne.getName();
            this._playerTwoName = this._playerTwo.getName();
            this._playerOne.setOlympiadGameId(var1);
            this._playerTwo.setOlympiadGameId(var1);
            this._playerOneID = this._playerOne.getObjectId();
            this._playerTwoID = this._playerTwo.getObjectId();
            this._playerOneClass = this._playerOne.getBaseClass();
            this._playerTwoClass = this._playerTwo.getBaseClass();
         } catch (Exception var5) {
            this._aborted = true;
            this.clearPlayers();
         }
      } else {
         this._aborted = true;
         this.clearPlayers();
      }
   }

   public boolean isAborted() {
      return this._aborted;
   }

   protected void clearPlayers() {
      this._playerOne = null;
      this._playerTwo = null;
      this._players = null;
      this._playerOneName = "";
      this._playerTwoName = "";
      this._playerOneID = 0;
      this._playerTwoID = 0;
   }

   protected void handleDisconnect(Player var1) {
      if (this._gamestarted) {
         if (var1 == this._playerOne) {
            this._playerOneDisconnected = true;
         } else if (var1 == this._playerTwo) {
            this._playerTwoDisconnected = true;
         }
      }
   }

   public Spawn SpawnBuffer(int var1, int var2, int var3, int var4) {
      NpcTemplate var5 = NpcData.getInstance().getTemplate(var4);

      try {
         Spawn var6 = new Spawn(var5);
         var6.setXYZ(var1, var2, var3);
         var6.setAmount(1);
         var6.setHeading(0);
         var6.setRespawnDelay(1);
         SpawnTable.getInstance().addSpawn(var6);
         var6.init();
         Npc var7 = var6.getLastSpawn();
         if (var7 != null) {
            var7.setInvul(true);
         }

         var6.stopRespawn();
         return var6;
      } catch (Exception var8) {
         LOGGER.log(Level.WARNING, "SpawnBuffer: ", (Throwable)var8);
         return null;
      }
   }

   protected void removals() {
      if (!this._aborted && this._playerOne != null && this._playerTwo != null && !this._playerOneDisconnected && !this._playerTwoDisconnected) {
         for (Player var2 : this._players) {
            try {
               var2.stopAllEffectsExceptThoseThatLastThroughDeath();
               Clan var3 = var2.getClan();
               if (var3 != null) {
                  for (Skill var5 : var3.getAllSkills()) {
                     var2.removeSkill(var5, false, true);
                  }

                  if (var3.getCastleId() > 0) {
                     Castle var9 = CastleManager.getInstance().getCastleByOwner(var3);
                     if (var9 != null) {
                        var9.removeResidentialSkills(var2);
                     }
                  }

                  if (var3.getFortId() > 0) {
                     Fort var10 = FortManager.getInstance().getFortByOwner(var3);
                     if (var10 != null) {
                        var10.removeResidentialSkills(var2);
                     }
                  }
               }

               if (var2.isCastingNow()) {
                  var2.abortCast();
               }

               var2.setInvisible(false);
               if (var2.isHero()) {
                  for (Skill var7 : HeroSkillTable.getHeroSkills()) {
                     var2.removeSkill(var7, false);
                  }
               }

               var2.setCurrentCp(var2.getMaxCp());
               var2.setCurrentHp(var2.getMaxHp());
               var2.setCurrentMp(var2.getMaxMp());
               if (var2.hasSummon()) {
                  Summon var12 = var2.getSummon();
                  var12.stopAllEffects();
                  if (var12.isPet()) {
                     var12.unSummon(var2);
                  }
               }

               var2.stopCubicsByOthers();
               if (var2.getParty() != null) {
                  Party var13 = var2.getParty();
                  var13.removePartyMember(var2, null);
               }

               if (var2.getAgathionId() > 0) {
                  var2.setAgathionId(0);
                  var2.broadcastUserInfo();
               }

               var2.checkItemRestriction();
               var2.disableAutoShotsAll();
               Item var14 = var2.getActiveWeaponInstance();
               if (var14 != null) {
                  var14.unChargeAllShots();
               }

               for (Skill var17 : var2.getAllSkills()) {
                  if (var17.getReuseDelay() <= 900000) {
                     var2.enableSkill(var17);
                  }
               }

               var2.sendSkillList();
               var2.sendPacket(new SkillCoolTime(var2));
            } catch (Exception var8) {
               LOGGER.log(Level.WARNING, "removals: ", (Throwable)var8);
            }
         }
      }
   }

   protected boolean portPlayersToArena() {
      boolean var1 = this._playerOne == null || this._playerOneDisconnected;
      boolean var2 = this._playerTwo == null || this._playerTwoDisconnected;
      if (!var1 && !var2 && !this._aborted) {
         try {
            this.x1 = this._playerOne.getX();
            this.y1 = this._playerOne.getY();
            this.z1 = this._playerOne.getZ();
            this.x2 = this._playerTwo.getX();
            this.y2 = this._playerTwo.getY();
            this.z2 = this._playerTwo.getZ();
            if (this._playerOne.isSitting()) {
               this._playerOne.standUp();
            }

            if (this._playerTwo.isSitting()) {
               this._playerTwo.standUp();
            }

            this._playerOne.setTarget(null);
            this._playerTwo.setTarget(null);
            this._gamestarted = true;
            this._playerOne.setInstanceId(0);
            this._playerOne.teleToLocation(this._stadiumPort[0] + 1200, this._stadiumPort[1], this._stadiumPort[2], false);
            this._playerTwo.setInstanceId(0);
            this._playerTwo.teleToLocation(this._stadiumPort[0] - 1200, this._stadiumPort[1], this._stadiumPort[2], false);
            this._playerOne.sendPacket(new ExOlympiadMode(2));
            this._playerTwo.sendPacket(new ExOlympiadMode(2));
            this._playerOne.setInOlympiadMode(true);
            this._playerOne.setOlympiadStart(false);
            this._playerOne.setOlympiadSide(1);
            this._playerOne.setOlympiadBuffCount(OlympiadConfig.OLYMPIAD_MAX_BUFFS);
            this._playerTwo.setInOlympiadMode(true);
            this._playerTwo.setOlympiadStart(false);
            this._playerTwo.setOlympiadSide(2);
            this._playerTwo.setOlympiadBuffCount(OlympiadConfig.OLYMPIAD_MAX_BUFFS);
            this._spawnOne = this.SpawnBuffer(this._stadiumPort[0] + 1100, this._stadiumPort[1], this._stadiumPort[2], 36402);
            this._spawnTwo = this.SpawnBuffer(this._stadiumPort[0] - 1100, this._stadiumPort[1], this._stadiumPort[2], 36402);
            _gameIsStarted = false;
            return true;
         } catch (NullPointerException var4) {
            LOGGER.log(Level.WARNING, "portPlayersToArena: ", (Throwable)var4);
            return false;
         }
      } else {
         this._playerOne = null;
         this._playerTwo = null;
         this._aborted = true;
         return false;
      }
   }

   protected void cleanEffects() {
      if (this._playerOne != null && this._playerTwo != null && !this._playerOneDisconnected && !this._playerTwoDisconnected) {
         for (Player var2 : this._players) {
            try {
               var2.stopAllEffectsExceptThoseThatLastThroughDeath();
               var2.clearSouls();
               var2.clearCharges();
               if (var2.getAgathionId() > 0) {
                  var2.setAgathionId(0);
               }

               if (var2.hasSummon()) {
                  Summon var3 = var2.getSummon();
                  var3.stopAllEffects();
               }
            } catch (Exception var4) {
               LOGGER.log(Level.WARNING, "cleanEffects()", (Throwable)var4);
            }
         }
      }
   }

   protected void portPlayersBack() {
      if (this._playerOne != null) {
         this._playerOne.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
         this._playerOne.teleToLocation(this.x1, this.y1, this.z1, true);
      }

      if (this._playerTwo != null) {
         this._playerTwo.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
         this._playerTwo.teleToLocation(this.x2, this.y2, this.z2, true);
      }

      try {
         OlympiadManager.STADIUMS[this._stadiumID].closeDoors();
      } catch (Exception var2) {
         LOGGER.log(Level.WARNING, "closeDoors: ", (Throwable)var2);
      }

      this.stopSpawns();
   }

   private void stopSpawns() {
      if (this._spawnOne != null) {
         this._spawnOne.stopRespawn();
         if (this._spawnOne.getLastSpawn() != null) {
            this._spawnOne.getLastSpawn().deleteMe();
         }

         this._spawnOne = null;
      }

      if (this._spawnTwo != null) {
         this._spawnTwo.stopRespawn();
         if (this._spawnTwo.getLastSpawn() != null) {
            this._spawnTwo.getLastSpawn().deleteMe();
         }

         this._spawnTwo = null;
      }
   }

   protected void PlayersStatusBack() {
      for (Player var2 : this._players) {
         if (var2 != null) {
            try {
               if (Olympiad.getInstance().playerInStadia(var2)) {
                  if (var2.isDead()) {
                     var2.setDead(false);
                  }

                  var2.getStatus().startHpMpRegeneration();
                  var2.setCurrentCp(var2.getMaxCp());
                  var2.setCurrentHp(var2.getMaxHp());
                  var2.setCurrentMp(var2.getMaxMp());
               }

               if (var2.isTransformed()) {
                  var2.untransform();
               }

               var2.setInOlympiadMode(false);
               var2.setOlympiadStart(false);
               var2.setOlympiadSide(-1);
               var2.setOlympiadGameId(-1);
               var2.sendPacket(new ExOlympiadMode(0));
               Clan var3 = var2.getClan();
               if (var3 != null) {
                  for (Skill var5 : var3.getAllSkills()) {
                     if (var5.getMinPledgeClass() <= var2.getPledgeClass()) {
                        var2.addSkill(var5, false);
                     }
                  }

                  if (var3.getCastleId() > 0) {
                     Castle var9 = CastleManager.getInstance().getCastleByOwner(var3);
                     if (var9 != null) {
                        var9.giveResidentialSkills(var2);
                     }
                  }

                  if (var3.getFortId() > 0) {
                     Fort var10 = FortManager.getInstance().getFortByOwner(var3);
                     if (var10 != null) {
                        var10.giveResidentialSkills(var2);
                     }
                  }
               }

               if (var2.isHero()) {
                  for (Skill var7 : HeroSkillTable.getHeroSkills()) {
                     var2.addSkill(var7, false);
                  }
               }

               var2.sendSkillList();
               if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) {
                  AntiFeedManager.getInstance().removePlayer(1, var2);
               }
            } catch (Exception var8) {
               LOGGER.log(Level.WARNING, "portPlayersToArena()", (Throwable)var8);
            }
         }
      }
   }

   protected boolean haveWinner() {
      if (!this._aborted && this._playerOne != null && this._playerTwo != null && !this._playerOneDisconnected && !this._playerTwoDisconnected) {
         double var1 = 0.0;

         try {
            if (this._playerOne.getOlympiadGameId() != -1) {
               var1 = this._playerOne.getCurrentHp();
            }
         } catch (Exception var7) {
            var1 = 0.0;
         }

         double var3 = 0.0;

         try {
            if (this._playerTwo.getOlympiadGameId() != -1) {
               var3 = this._playerTwo.getCurrentHp();
            }
         } catch (Exception var6) {
            var3 = 0.0;
         }

         return var3 <= 0.0 || var1 <= 0.0;
      } else {
         return true;
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   protected void validateWinner() {
      if (!this._aborted) {
         boolean var1 = this._playerOne == null || this._playerOneDisconnected;
         boolean var2 = this._playerTwo == null || this._playerTwoDisconnected;
         byte var3;
         int var4;
         String var5;
         switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$olympiad$CompetitionType[this._type.ordinal()]) {
            case 1:
               var3 = 5;
               var4 = OlympiadConfig.OLYMPIAD_NONCLASSED_RITEM_C;
               var5 = "no";
               break;
            default:
               var3 = 3;
               var4 = OlympiadConfig.OLYMPIAD_CLASSED_RITEM_C;
               var5 = "yes";
         }

         StatSet var6 = Olympiad.getNobleStats(this._playerOneID);
         StatSet var7 = Olympiad.getNobleStats(this._playerTwoID);
         int var8 = var6.getInt("competitions_done");
         int var9 = var7.getInt("competitions_done");
         int var10 = var6.getInt("competitions_won");
         int var11 = var7.getInt("competitions_won");
         int var12 = var6.getInt("competitions_lost");
         int var13 = var7.getInt("competitions_lost");
         int var14 = var6.getInt("competitions_drawn");
         int var15 = var7.getInt("competitions_drawn");
         int var16 = var6.getInt("olympiad_points");
         int var17 = var7.getInt("olympiad_points");
         int var18 = Math.min(Math.min(var16, var17) / var3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
         if (!this._playerOneDefaulted && !this._playerTwoDefaulted) {
            if (!var1 && !var2) {
               double var39 = 0.0;
               if (!this._playerOne.isDead()) {
                  var39 = this._playerOne.getCurrentHp() + this._playerOne.getCurrentCp();
               }

               double var42 = 0.0;
               if (!this._playerTwo.isDead()) {
                  var42 = this._playerTwo.getCurrentHp() + this._playerTwo.getCurrentCp();
               }

               this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
               this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               this._sm3 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               this._playerOne = World.getPlayer(this._playerOneID);
               this._players.set(0, this._playerOne);
               this._playerTwo = World.getPlayer(this._playerTwoID);
               this._players.set(1, this._playerTwo);
               String var23 = "draw";
               long var24 = System.currentTimeMillis() - this._startTime;
               if (this._playerOne == null && this._playerTwo == null) {
                  var6.set("competitions_drawn", var14 + 1);
                  var7.set("competitions_drawn", var15 + 1);
                  this._sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
                  this.broadcastMessage(this._sm, true);
               } else if (this._playerTwo != null
                  && this._playerTwo.isOnline()
                  && (var42 != 0.0 || var39 == 0.0)
                  && (this._damageP1 <= this._damageP2 || var42 == 0.0 || var39 == 0.0)) {
                  if (this._playerOne == null
                     || !this._playerOne.isOnline()
                     || var39 == 0.0 && var42 != 0.0
                     || this._damageP2 > this._damageP1 && var39 != 0.0 && var42 != 0.0) {
                     var7.set("olympiad_points", var17 + var18);
                     var6.set("olympiad_points", var16 - var18);
                     var7.set("competitions_won", var11 + 1);
                     var6.set("competitions_lost", var12 + 1);
                     this._sm.addString(this._playerTwoName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerTwoName);
                     this._sm2.addInt(var18);
                     this.broadcastMessage(this._sm2, false);
                     this._sm3.addString(this._playerOneName);
                     this._sm3.addInt(var18);
                     this.broadcastMessage(this._sm3, false);
                     var23 = this._playerTwoName + " won";

                     try {
                        this.saveResults(
                           this._playerOneID,
                           this._playerTwoID,
                           this._playerOneClass,
                           this._playerTwoClass,
                           2,
                           this._startTime,
                           var24,
                           this._type == CompetitionType.CLASSED ? 1 : 0
                        );
                        Item var44 = this._playerTwo
                           .getInventory()
                           .addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, var4, this._playerTwo, null);
                        InventoryUpdate var48 = new InventoryUpdate();
                        var48.addModifiedItem(var44);
                        this._playerTwo.sendInventoryUpdate(var48);
                        SystemMessage var50 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                        var50.addItemName(var44);
                        var50.addInt(var4);
                        this._playerTwo.sendPacket(var50);
                        if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                           EventDispatcher.getInstance()
                              .notifyEventAsync(new OnOlympiadMatchResult(this._playerTwo, this._playerOne, this._type), Olympiad.getInstance());
                        }
                     } catch (Exception var34) {
                     }
                  } else {
                     this.saveResults(
                        this._playerOneID,
                        this._playerTwoID,
                        this._playerOneClass,
                        this._playerTwoClass,
                        0,
                        this._startTime,
                        var24,
                        this._type == CompetitionType.CLASSED ? 1 : 0
                     );
                     this._sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
                     this.broadcastMessage(this._sm, true);
                     int var43 = Math.min(var16 / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
                     int var47 = Math.min(var17 / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
                     var6.set("olympiad_points", var16 - var43);
                     var7.set("olympiad_points", var17 - var47);
                     var6.set("competitions_drawn", var14 + 1);
                     var7.set("competitions_drawn", var15 + 1);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm2.addString(this._playerOneName);
                     this._sm2.addInt(var43);
                     this.broadcastMessage(this._sm2, false);
                     this._sm3 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm3.addString(this._playerTwoName);
                     this._sm3.addInt(var47);
                     this.broadcastMessage(this._sm3, false);
                  }
               } else {
                  var6.set("olympiad_points", var16 + var18);
                  var7.set("olympiad_points", var17 - var18);
                  var6.set("competitions_won", var10 + 1);
                  var7.set("competitions_lost", var13 + 1);
                  this._sm.addString(this._playerOneName);
                  this.broadcastMessage(this._sm, true);
                  this._sm2.addString(this._playerOneName);
                  this._sm2.addInt(var18);
                  this.broadcastMessage(this._sm2, false);
                  this._sm3.addString(this._playerTwoName);
                  this._sm3.addInt(var18);
                  this.broadcastMessage(this._sm3, false);
                  var23 = this._playerOneName + " won";

                  try {
                     this.saveResults(
                        this._playerOneID,
                        this._playerTwoID,
                        this._playerOneClass,
                        this._playerTwoClass,
                        1,
                        this._startTime,
                        var24,
                        this._type == CompetitionType.CLASSED ? 1 : 0
                     );
                     Item var26 = this._playerOne
                        .getInventory()
                        .addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, var4, this._playerOne, null);
                     InventoryUpdate var27 = new InventoryUpdate();
                     var27.addModifiedItem(var26);
                     this._playerOne.sendInventoryUpdate(var27);
                     SystemMessage var28 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                     var28.addItemName(var26);
                     var28.addInt(var4);
                     this._playerOne.sendPacket(var28);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerOne, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var33) {
                  }
               }

               var6.set("competitions_done", var8 + 1);
               var7.set("competitions_done", var9 + 1);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord var45 = new LogRecord(Level.INFO, var23);
                  var45.setParameters(new Object[]{this._playerOneName, this._playerTwoName, var39, var42, this._damageP1, this._damageP2, var18, var5});
                  _logResults.log(var45);
               }

               byte var46 = 10;

               for (byte var49 = 15; var49 > 0; var49 -= var46) {
                  this._sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
                  this._sm.addInt(var49);
                  this.broadcastMessage(this._sm, false);
                  switch (var49) {
                     case 5:
                        var46 = 1;
                        break;
                     case 10:
                        var46 = 5;
                  }

                  try {
                     Thread.sleep(var46 * 1000);
                  } catch (Exception var32) {
                  }
               }
            } else {
               if (var1 && !var2) {
                  try {
                     var6.set("olympiad_points", var16 - var18);
                     var6.set("competitions_lost", var12 + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord var38 = new LogRecord(Level.INFO, this._playerOneName + " crash");
                        var38.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, var18, var5});
                        _logResults.log(var38);
                     }

                     var7.set("olympiad_points", var17 + var18);
                     var7.set("competitions_won", var11 + 1);
                     this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm.addString(this._playerTwoName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerTwoName);
                     this._sm2.addInt(var18);
                     this.broadcastMessage(this._sm2, false);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerTwo, this._playerOne, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var31) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var31.getMessage(), (Throwable)var31);
                  }
               } else if (var2 && !var1) {
                  try {
                     var7.set("olympiad_points", var17 - var18);
                     var7.set("competitions_lost", var13 + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord var37 = new LogRecord(Level.INFO, this._playerTwoName + " crash");
                        var37.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, var18, var5});
                        _logResults.log(var37);
                     }

                     var6.set("olympiad_points", var16 + var18);
                     var6.set("competitions_won", var10 + 1);
                     this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm.addString(this._playerOneName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerOneName);
                     this._sm2.addInt(var18);
                     this.broadcastMessage(this._sm2, false);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerOne, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var30) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var30.getMessage(), (Throwable)var30);
                  }
               } else if (var1 && var2) {
                  try {
                     var6.set("olympiad_points", var16 - var18);
                     var6.set("competitions_lost", var12 + 1);
                     var7.set("olympiad_points", var17 - var18);
                     var7.set("competitions_lost", var13 + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord var36 = new LogRecord(Level.INFO, "both crash");
                        var36.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, var18, var5});
                        _logResults.log(var36);
                     }

                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, this._playerOne, this._type), Olympiad.getInstance());
                        EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var29) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var29.getMessage(), (Throwable)var29);
                  }
               }

               var6.set("competitions_done", var8 + 1);
               var7.set("competitions_done", var9 + 1);
            }
         } else {
            if (this._playerOneDefaulted) {
               int var19 = Math.min(var16 / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
               var6.set("olympiad_points", var16 - var19);
               SystemMessage var20 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               var20.addString(this._playerOneName);
               var20.addInt(var19);
               this.broadcastMessage(var20, false);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord var21 = new LogRecord(Level.INFO, this._playerOneName + " default");
                  var21.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, var19, var5});
                  _logResults.log(var21);
               }
            }

            if (this._playerTwoDefaulted) {
               int var35 = Math.min(var17 / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
               var7.set("olympiad_points", var17 - var35);
               SystemMessage var40 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               var40.addString(this._playerTwoName);
               var40.addInt(var35);
               this.broadcastMessage(var40, false);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord var41 = new LogRecord(Level.INFO, this._playerTwoName + " default");
                  var41.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, var35, var5});
                  _logResults.log(var41);
               }
            }
         }
      }
   }

   protected boolean makeCompetitionStart() {
      this._startTime = System.currentTimeMillis();
      if (this._aborted) {
         return false;
      } else {
         this._sm = new SystemMessage(SystemMessageId.THE_MATCH_HAS_STARTED_FIGHT);
         this.broadcastMessage(this._sm, true);
         _gameIsStarted = true;

         try {
            OlympiadManager.STADIUMS[this._stadiumID].openDoors();
         } catch (Exception var3) {
            LOGGER.log(Level.WARNING, "openDoors: ", (Throwable)var3);
         }

         try {
            for (Player var2 : this._players) {
               var2.setOlympiadStart(true);
               var2.updateEffectIcons();
            }

            return true;
         } catch (Exception var4) {
            LOGGER.log(Level.WARNING, "makeCompetitionStart: ", (Throwable)var4);
            this._aborted = true;
            return false;
         }
      }
   }

   protected void addDamage(Player var1, int var2) {
      if (this._playerOne != null && this._playerTwo != null) {
         if (var1 == this._playerOne) {
            if (!this._playerTwo.isInvul()) {
               this._damageP1 += var2;
            }
         } else if (var1 == this._playerTwo && !this._playerOne.isInvul()) {
            this._damageP2 += var2;
         }
      }
   }

   protected String getTitle() {
      return this._playerOneName + " / " + this._playerTwoName;
   }

   protected Player[] getPlayers() {
      if (this._players != null && !this._players.isEmpty()) {
         Player[] var1 = new Player[this._players.size()];
         this._players.toArray(var1);
         return var1;
      } else {
         return null;
      }
   }

   protected void broadcastMessage(SystemMessage var1, boolean var2) {
      for (Player var4 : this._players) {
         if (var4 != null) {
            var4.sendPacket(var1);
         }
      }

      if (var2 && OlympiadManager.STADIUMS[this._stadiumID].getSpectators() != null) {
         for (Player var6 : OlympiadManager.STADIUMS[this._stadiumID].getSpectators()) {
            if (var6 != null) {
               var6.sendPacket(var1);
            }
         }
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   protected void announceGame() {
      Object var1 = null;

      var1 = switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$olympiad$CompetitionType[this._type.ordinal()]) {
         case 1 -> "class-free individual match";
         default -> "class-specific individual match";
      };

      for (Spawn var3 : SpawnTable.getInstance().getSpawns(31688)) {
         Npc var4 = var3.getLastSpawn();
         if (var4 != null) {
            var4.broadcastPacket(
               new CreatureSay(
                  var4, ChatType.NPC_SHOUT, var4.getName(), "Olympiad " + var1 + " is going to begin in Arena " + (this._stadiumID + 1) + " in a moment."
               )
            );
         }
      }
   }

   protected void saveResults(int var1, int var2, int var3, int var4, int var5, long var6, long var8, int var10) {
      try (
         Connection var11 = DatabaseFactory.getConnection();
         PreparedStatement var12 = var11.prepareStatement(
            "INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)"
         );
      ) {
         var12.setInt(1, var1);
         var12.setInt(2, var2);
         var12.setInt(3, var3);
         var12.setInt(4, var4);
         var12.setInt(5, var5);
         var12.setLong(6, var6);
         var12.setLong(7, var8);
         var12.setInt(8, var10);
         var12.execute();
      } catch (SQLException var19) {
         if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.log(Level.SEVERE, "SQL exception while saving olympiad fight.", (Throwable)var19);
         }
      }
   }
}
