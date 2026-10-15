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
   /** Return-to-town countdown after a finished battle, seconds (custom: 15 instead of retail 40). */
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

   protected OlympiadGame(int id, CompetitionType type, List<Player> list) {
      this._aborted = false;
      this._gamestarted = false;
      this._stadiumID = id;
      this._playerOneDisconnected = false;
      this._playerTwoDisconnected = false;
      this._type = type;
      this._stadiumPort = OlympiadManager.STADIUMS[id].getCoordinates();
      if (list != null) {
         this._players = list;
         this._playerOne = list.get(0);
         this._playerTwo = list.get(1);

         try {
            this._playerOneName = this._playerOne.getName();
            this._playerTwoName = this._playerTwo.getName();
            this._playerOne.setOlympiadGameId(id);
            this._playerTwo.setOlympiadGameId(id);
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

   protected void handleDisconnect(Player player) {
      if (this._gamestarted) {
         if (player == this._playerOne) {
            this._playerOneDisconnected = true;
         } else if (player == this._playerTwo) {
            this._playerTwoDisconnected = true;
         }
      }
   }

   public Spawn SpawnBuffer(int xPos, int yPos, int zPos, int npcId) {
      NpcTemplate template = NpcData.getInstance().getTemplate(npcId);

      try {
         Spawn spawn = new Spawn(template);
         spawn.setXYZ(xPos, yPos, zPos);
         spawn.setAmount(1);
         spawn.setHeading(0);
         spawn.setRespawnDelay(1);
         SpawnTable.getInstance().addSpawn(spawn);
         spawn.init();
         // The olympiad buffer must not be attackable (retail behavior). This also prevents
         // AutoPlay-driven players (FakePlayersAI bots) from locking onto it as a target.
         Npc spawnedBuffer = spawn.getLastSpawn();
         if (spawnedBuffer != null) {
            spawnedBuffer.setInvul(true);
         }

         spawn.stopRespawn();
         return spawn;
      } catch (Exception var7) {
         LOGGER.log(Level.WARNING, "SpawnBuffer: ", (Throwable)var7);
         return null;
      }
   }

   protected void removals() {
      if (!this._aborted) {
         if (this._playerOne != null && this._playerTwo != null) {
            if (!this._playerOneDisconnected && !this._playerTwoDisconnected) {
               for (Player player : this._players) {
                  try {
                     player.stopAllEffectsExceptThoseThatLastThroughDeath();
                     Clan clan = player.getClan();
                     if (clan != null) {
                        for (Skill skill : clan.getAllSkills()) {
                           player.removeSkill(skill, false, true);
                        }

                        if (clan.getCastleId() > 0) {
                           Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
                           if (castle != null) {
                              castle.removeResidentialSkills(player);
                           }
                        }

                        if (clan.getFortId() > 0) {
                           Fort fort = FortManager.getInstance().getFortByOwner(clan);
                           if (fort != null) {
                              fort.removeResidentialSkills(player);
                           }
                        }
                     }

                     if (player.isCastingNow()) {
                        player.abortCast();
                     }

                     player.setInvisible(false);
                     if (player.isHero()) {
                        for (Skill skill : HeroSkillTable.getHeroSkills()) {
                           player.removeSkill(skill, false);
                        }
                     }

                     player.setCurrentCp(player.getMaxCp());
                     player.setCurrentHp(player.getMaxHp());
                     player.setCurrentMp(player.getMaxMp());
                     if (player.hasSummon()) {
                        Summon summon = player.getSummon();
                        summon.stopAllEffects();
                        if (summon.isPet()) {
                           summon.unSummon(player);
                        }
                     }

                     player.stopCubicsByOthers();
                     if (player.getParty() != null) {
                        Party party = player.getParty();
                        party.removePartyMember(player, null);
                     }

                     if (player.getAgathionId() > 0) {
                        player.setAgathionId(0);
                        player.broadcastUserInfo();
                     }

                     player.checkItemRestriction();
                     player.disableAutoShotsAll();
                     Item item = player.getActiveWeaponInstance();
                     if (item != null) {
                        item.unChargeAllShots();
                     }

                     for (Skill skill : player.getAllSkills()) {
                        if (skill.getReuseDelay() <= 900000) {
                           player.enableSkill(skill);
                        }
                     }

                     player.sendSkillList();
                     player.sendPacket(new SkillCoolTime(player));
                  } catch (Exception var8) {
                     LOGGER.log(Level.WARNING, "removals: ", (Throwable)var8);
                  }
               }
            }
         }
      }
   }

   protected boolean portPlayersToArena() {
      boolean _playerOneCrash = this._playerOne == null || this._playerOneDisconnected;
      boolean _playerTwoCrash = this._playerTwo == null || this._playerTwoDisconnected;
      if (!_playerOneCrash && !_playerTwoCrash && !this._aborted) {
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
      if (this._playerOne != null && this._playerTwo != null) {
         if (!this._playerOneDisconnected && !this._playerTwoDisconnected) {
            for (Player player : this._players) {
               try {
                  player.stopAllEffectsExceptThoseThatLastThroughDeath();
                  player.clearSouls();
                  player.clearCharges();
                  if (player.getAgathionId() > 0) {
                     player.setAgathionId(0);
                  }

                  if (player.hasSummon()) {
                     Summon summon = player.getSummon();
                     summon.stopAllEffects();
                  }
               } catch (Exception var4) {
                  LOGGER.log(Level.WARNING, "cleanEffects()", (Throwable)var4);
               }
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

      // Close the stadium gates again (they were opened at match start) ...
      try {
         OlympiadManager.STADIUMS[this._stadiumID].closeDoors();
      } catch (Exception var7) {
         LOGGER.log(Level.WARNING, "closeDoors: ", (Throwable)var7);
      }

      // ... and remove the buffer NPCs spawned for this game (they accumulated before).
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
      for (Player player : this._players) {
         if (player != null) {
            try {
               if (Olympiad.getInstance().playerInStadia(player)) {
                  if (player.isDead()) {
                     player.setDead(false);
                  }

                  player.getStatus().startHpMpRegeneration();
                  player.setCurrentCp(player.getMaxCp());
                  player.setCurrentHp(player.getMaxHp());
                  player.setCurrentMp(player.getMaxMp());
               }

               if (player.isTransformed()) {
                  player.untransform();
               }

               player.setInOlympiadMode(false);
               player.setOlympiadStart(false);
               player.setOlympiadSide(-1);
               player.setOlympiadGameId(-1);
               player.sendPacket(new ExOlympiadMode(0));
               Clan clan = player.getClan();
               if (clan != null) {
                  for (Skill skill : clan.getAllSkills()) {
                     if (skill.getMinPledgeClass() <= player.getPledgeClass()) {
                        player.addSkill(skill, false);
                     }
                  }

                  if (clan.getCastleId() > 0) {
                     Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
                     if (castle != null) {
                        castle.giveResidentialSkills(player);
                     }
                  }

                  if (clan.getFortId() > 0) {
                     Fort fort = FortManager.getInstance().getFortByOwner(clan);
                     if (fort != null) {
                        fort.giveResidentialSkills(player);
                     }
                  }
               }

               if (player.isHero()) {
                  for (Skill skillx : HeroSkillTable.getHeroSkills()) {
                     player.addSkill(skillx, false);
                  }
               }

               player.sendSkillList();
               if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) {
                  AntiFeedManager.getInstance().removePlayer(1, player);
               }
            } catch (Exception var8) {
               LOGGER.log(Level.WARNING, "portPlayersToArena()", (Throwable)var8);
            }
         }
      }
   }

   protected boolean haveWinner() {
      if (!this._aborted && this._playerOne != null && this._playerTwo != null && !this._playerOneDisconnected && !this._playerTwoDisconnected) {
         double playerOneHp = 0.0;

         try {
            if (this._playerOne.getOlympiadGameId() != -1) {
               playerOneHp = this._playerOne.getCurrentHp();
            }
         } catch (Exception var7) {
            playerOneHp = 0.0;
         }

         double playerTwoHp = 0.0;

         try {
            if (this._playerTwo.getOlympiadGameId() != -1) {
               playerTwoHp = this._playerTwo.getCurrentHp();
            }
         } catch (Exception var6) {
            playerTwoHp = 0.0;
         }

         return playerTwoHp <= 0.0 || playerOneHp <= 0.0;
      } else {
         return true;
      }
   }

   protected void validateWinner() {
      if (!this._aborted) {
         boolean _pOneCrash = this._playerOne == null || this._playerOneDisconnected;
         boolean _pTwoCrash = this._playerTwo == null || this._playerTwoDisconnected;
         int _div;
         int _gpreward;
         String classed;
         switch (this._type) {
            case NON_CLASSED:
               _div = 5;
               _gpreward = OlympiadConfig.OLYMPIAD_NONCLASSED_RITEM_C;
               classed = "no";
               break;
            default:
               _div = 3;
               _gpreward = OlympiadConfig.OLYMPIAD_CLASSED_RITEM_C;
               classed = "yes";
         }

         StatSet playerOneStat = Olympiad.getNobleStats(this._playerOneID);
         StatSet playerTwoStat = Olympiad.getNobleStats(this._playerTwoID);
         int playerOnePlayed = playerOneStat.getInt("competitions_done");
         int playerTwoPlayed = playerTwoStat.getInt("competitions_done");
         int playerOneWon = playerOneStat.getInt("competitions_won");
         int playerTwoWon = playerTwoStat.getInt("competitions_won");
         int playerOneLost = playerOneStat.getInt("competitions_lost");
         int playerTwoLost = playerTwoStat.getInt("competitions_lost");
         int playerOneDrawn = playerOneStat.getInt("competitions_drawn");
         int playerTwoDrawn = playerTwoStat.getInt("competitions_drawn");
         int playerOnePoints = playerOneStat.getInt("olympiad_points");
         int playerTwoPoints = playerTwoStat.getInt("olympiad_points");
         int pointDiff = Math.min(Math.min(playerOnePoints, playerTwoPoints) / _div, OlympiadConfig.OLYMPIAD_MAX_POINTS);
         if (!this._playerOneDefaulted && !this._playerTwoDefaulted) {
            if (!_pOneCrash && !_pTwoCrash) {
               double playerOneHp = 0.0;
               if (!this._playerOne.isDead()) {
                  playerOneHp = this._playerOne.getCurrentHp() + this._playerOne.getCurrentCp();
               }

               double playerTwoHp = 0.0;
               if (!this._playerTwo.isDead()) {
                  playerTwoHp = this._playerTwo.getCurrentHp() + this._playerTwo.getCurrentCp();
               }

               this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
               this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               this._sm3 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               this._playerOne = World.getPlayer(this._playerOneID);
               this._players.set(0, this._playerOne);
               this._playerTwo = World.getPlayer(this._playerTwoID);
               this._players.set(1, this._playerTwo);
               String winner = "draw";
               long fightTime = System.currentTimeMillis() - this._startTime;
               if (this._playerOne == null && this._playerTwo == null) {
                  playerOneStat.set("competitions_drawn", playerOneDrawn + 1);
                  playerTwoStat.set("competitions_drawn", playerTwoDrawn + 1);
                  this._sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
                  this.broadcastMessage(this._sm, true);
               } else if (this._playerTwo != null
                  && this._playerTwo.isOnline()
                  && (playerTwoHp != 0.0 || playerOneHp == 0.0)
                  && (this._damageP1 <= this._damageP2 || playerTwoHp == 0.0 || playerOneHp == 0.0)) {
                  if (this._playerOne == null
                     || !this._playerOne.isOnline()
                     || playerOneHp == 0.0 && playerTwoHp != 0.0
                     || this._damageP2 > this._damageP1 && playerOneHp != 0.0 && playerTwoHp != 0.0) {
                     playerTwoStat.set("olympiad_points", playerTwoPoints + pointDiff);
                     playerOneStat.set("olympiad_points", playerOnePoints - pointDiff);
                     playerTwoStat.set("competitions_won", playerTwoWon + 1);
                     playerOneStat.set("competitions_lost", playerOneLost + 1);
                     this._sm.addString(this._playerTwoName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerTwoName);
                     this._sm2.addInt(pointDiff);
                     this.broadcastMessage(this._sm2, false);
                     this._sm3.addString(this._playerOneName);
                     this._sm3.addInt(pointDiff);
                     this.broadcastMessage(this._sm3, false);
                     winner = this._playerTwoName + " won";

                     try {
                        this.saveResults(
                           this._playerOneID,
                           this._playerTwoID,
                           this._playerOneClass,
                           this._playerTwoClass,
                           2,
                           this._startTime,
                           fightTime,
                           this._type == CompetitionType.CLASSED ? 1 : 0
                        );
                        Item item = this._playerTwo
                           .getInventory()
                           .addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, _gpreward, this._playerTwo, null);
                        InventoryUpdate iu = new InventoryUpdate();
                        iu.addModifiedItem(item);
                        this._playerTwo.sendInventoryUpdate(iu);
                        SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                        sm.addItemName(item);
                        sm.addInt(_gpreward);
                        this._playerTwo.sendPacket(sm);
                        if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                           EventDispatcher.getInstance()
                              .notifyEventAsync(new OnOlympiadMatchResult(this._playerTwo, this._playerOne, this._type), Olympiad.getInstance());
                        }
                     } catch (Exception var30) {
                     }
                  } else {
                     this.saveResults(
                        this._playerOneID,
                        this._playerTwoID,
                        this._playerOneClass,
                        this._playerTwoClass,
                        0,
                        this._startTime,
                        fightTime,
                        this._type == CompetitionType.CLASSED ? 1 : 0
                     );
                     this._sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
                     this.broadcastMessage(this._sm, true);
                     int pointOneDiff = Math.min(playerOnePoints / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
                     int pointTwoDiff = Math.min(playerTwoPoints / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
                     playerOneStat.set("olympiad_points", playerOnePoints - pointOneDiff);
                     playerTwoStat.set("olympiad_points", playerTwoPoints - pointTwoDiff);
                     playerOneStat.set("competitions_drawn", playerOneDrawn + 1);
                     playerTwoStat.set("competitions_drawn", playerTwoDrawn + 1);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm2.addString(this._playerOneName);
                     this._sm2.addInt(pointOneDiff);
                     this.broadcastMessage(this._sm2, false);
                     this._sm3 = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm3.addString(this._playerTwoName);
                     this._sm3.addInt(pointTwoDiff);
                     this.broadcastMessage(this._sm3, false);
                  }
               } else {
                  playerOneStat.set("olympiad_points", playerOnePoints + pointDiff);
                  playerTwoStat.set("olympiad_points", playerTwoPoints - pointDiff);
                  playerOneStat.set("competitions_won", playerOneWon + 1);
                  playerTwoStat.set("competitions_lost", playerTwoLost + 1);
                  this._sm.addString(this._playerOneName);
                  this.broadcastMessage(this._sm, true);
                  this._sm2.addString(this._playerOneName);
                  this._sm2.addInt(pointDiff);
                  this.broadcastMessage(this._sm2, false);
                  this._sm3.addString(this._playerTwoName);
                  this._sm3.addInt(pointDiff);
                  this.broadcastMessage(this._sm3, false);
                  winner = this._playerOneName + " won";

                  try {
                     this.saveResults(
                        this._playerOneID,
                        this._playerTwoID,
                        this._playerOneClass,
                        this._playerTwoClass,
                        1,
                        this._startTime,
                        fightTime,
                        this._type == CompetitionType.CLASSED ? 1 : 0
                     );
                     Item item = this._playerOne
                        .getInventory()
                        .addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, _gpreward, this._playerOne, null);
                     InventoryUpdate iu = new InventoryUpdate();
                     iu.addModifiedItem(item);
                     this._playerOne.sendInventoryUpdate(iu);
                     SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                     sm.addItemName(item);
                     sm.addInt(_gpreward);
                     this._playerOne.sendPacket(sm);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerOne, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var31) {
                  }
               }

               playerOneStat.set("competitions_done", playerOnePlayed + 1);
               playerTwoStat.set("competitions_done", playerTwoPlayed + 1);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord record = new LogRecord(Level.INFO, winner);
                  record.setParameters(
                     new Object[]{this._playerOneName, this._playerTwoName, playerOneHp, playerTwoHp, this._damageP1, this._damageP2, pointDiff, classed}
                  );
                  _logResults.log(record);
               }

               byte step = 10;

               // Return-to-town countdown: 15 then 5, 4, 3, 2, 1 (exact TOWN_RETURN_TIME seconds).
               for (byte i = TOWN_RETURN_TIME; i > 0; i -= step) {
                  this._sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
                  this._sm.addInt(i);
                  this.broadcastMessage(this._sm, false);
                  switch (i) {
                     case 5:
                        step = 1;
                        break;
                     case 10:
                        step = 5;
                  }

                  try {
                     Thread.sleep(step * 1000);
                  } catch (Exception var29) {
                  }
               }
            } else {
               if (_pOneCrash && !_pTwoCrash) {
                  try {
                     playerOneStat.set("olympiad_points", playerOnePoints - pointDiff);
                     playerOneStat.set("competitions_lost", playerOneLost + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord record = new LogRecord(Level.INFO, this._playerOneName + " crash");
                        record.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, pointDiff, classed});
                        _logResults.log(record);
                     }

                     playerTwoStat.set("olympiad_points", playerTwoPoints + pointDiff);
                     playerTwoStat.set("competitions_won", playerTwoWon + 1);
                     this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm.addString(this._playerTwoName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerTwoName);
                     this._sm2.addInt(pointDiff);
                     this.broadcastMessage(this._sm2, false);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerTwo, this._playerOne, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var34) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var34.getMessage(), (Throwable)var34);
                  }
               } else if (_pTwoCrash && !_pOneCrash) {
                  try {
                     playerTwoStat.set("olympiad_points", playerTwoPoints - pointDiff);
                     playerTwoStat.set("competitions_lost", playerTwoLost + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord record = new LogRecord(Level.INFO, this._playerTwoName + " crash");
                        record.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, pointDiff, classed});
                        _logResults.log(record);
                     }

                     playerOneStat.set("olympiad_points", playerOnePoints + pointDiff);
                     playerOneStat.set("competitions_won", playerOneWon + 1);
                     this._sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
                     this._sm2 = new SystemMessage(SystemMessageId.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
                     this._sm.addString(this._playerOneName);
                     this.broadcastMessage(this._sm, true);
                     this._sm2.addString(this._playerOneName);
                     this._sm2.addInt(pointDiff);
                     this.broadcastMessage(this._sm2, false);
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance()
                           .notifyEventAsync(new OnOlympiadMatchResult(this._playerOne, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var33) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var33.getMessage(), (Throwable)var33);
                  }
               } else if (_pOneCrash && _pTwoCrash) {
                  try {
                     playerOneStat.set("olympiad_points", playerOnePoints - pointDiff);
                     playerOneStat.set("competitions_lost", playerOneLost + 1);
                     playerTwoStat.set("olympiad_points", playerTwoPoints - pointDiff);
                     playerTwoStat.set("competitions_lost", playerTwoLost + 1);
                     if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                        LogRecord record = new LogRecord(Level.INFO, "both crash");
                        record.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, pointDiff, classed});
                        _logResults.log(record);
                     }

                     if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance())) {
                        EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, this._playerOne, this._type), Olympiad.getInstance());
                        EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, this._playerTwo, this._type), Olympiad.getInstance());
                     }
                  } catch (Exception var32) {
                     LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + var32.getMessage(), (Throwable)var32);
                  }
               }

               playerOneStat.set("competitions_done", playerOnePlayed + 1);
               playerTwoStat.set("competitions_done", playerTwoPlayed + 1);
            }
         } else {
            if (this._playerOneDefaulted) {
               int lostPoints = Math.min(playerOnePoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
               playerOneStat.set("olympiad_points", playerOnePoints - lostPoints);
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               sm.addString(this._playerOneName);
               sm.addInt(lostPoints);
               this.broadcastMessage(sm, false);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord record = new LogRecord(Level.INFO, this._playerOneName + " default");
                  record.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, lostPoints, classed});
                  _logResults.log(record);
               }
            }

            if (this._playerTwoDefaulted) {
               int lostPoints = Math.min(playerTwoPoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
               playerTwoStat.set("olympiad_points", playerTwoPoints - lostPoints);
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
               sm.addString(this._playerTwoName);
               sm.addInt(lostPoints);
               this.broadcastMessage(sm, false);
               if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS) {
                  LogRecord record = new LogRecord(Level.INFO, this._playerTwoName + " default");
                  record.setParameters(new Object[]{this._playerOneName, this._playerTwoName, 0, 0, 0, 0, lostPoints, classed});
                  _logResults.log(record);
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

         // Open the stadium gates: participants wait locked in the side alcoves during the
         // preparation countdown; when the match starts the gates open and they can fight.
         // Without this the gates stayed closed forever (nothing else called openDoors()).
         try {
            OlympiadManager.STADIUMS[this._stadiumID].openDoors();
         } catch (Exception var5) {
            LOGGER.log(Level.WARNING, "openDoors: ", (Throwable)var5);
         }

         try {
            for (Player player : this._players) {
               player.setOlympiadStart(true);
               player.updateEffectIcons();
            }

            return true;
         } catch (Exception var3) {
            LOGGER.log(Level.WARNING, "makeCompetitionStart: ", (Throwable)var3);
            this._aborted = true;
            return false;
         }
      }
   }

   protected void addDamage(Player player, int damage) {
      if (this._playerOne != null && this._playerTwo != null) {
         if (player == this._playerOne) {
            if (!this._playerTwo.isInvul()) {
               this._damageP1 += damage;
            }
         } else if (player == this._playerTwo && !this._playerOne.isInvul()) {
            this._damageP2 += damage;
         }
      }
   }

   protected String getTitle() {
      return this._playerOneName + " / " + this._playerTwoName;
   }

   protected Player[] getPlayers() {
      if (this._players != null && !this._players.isEmpty()) {
         Player[] players = new Player[this._players.size()];
         this._players.toArray(players);
         return players;
      } else {
         return null;
      }
   }

   protected void broadcastMessage(SystemMessage sm, boolean toAll) {
      for (Player player : this._players) {
         if (player != null) {
            player.sendPacket(sm);
         }
      }

      if (toAll && OlympiadManager.STADIUMS[this._stadiumID].getSpectators() != null) {
         for (Player spec : OlympiadManager.STADIUMS[this._stadiumID].getSpectators()) {
            if (spec != null) {
               spec.sendPacket(sm);
            }
         }
      }
   }

   protected void announceGame() {
      String gameType = null;

      gameType = switch (this._type) {
         case NON_CLASSED -> "class-free individual match";
         default -> "class-specific individual match";
      };

      for (Spawn spawn : SpawnTable.getInstance().getSpawns(31688)) {
         Npc manager = spawn.getLastSpawn();
         if (manager != null) {
            manager.broadcastPacket(
               new CreatureSay(
                  manager,
                  ChatType.NPC_SHOUT,
                  manager.getName(),
                  "Olympiad " + gameType + " is going to begin in Arena " + (this._stadiumID + 1) + " in a moment."
               )
            );
         }
      }
   }

   protected void saveResults(int playerOne, int playerTwo, int playerOneClass, int playerTwoClass, int winner, long startTime, long fightTime, int classed) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)"
         );
      ) {
         statement.setInt(1, playerOne);
         statement.setInt(2, playerTwo);
         statement.setInt(3, playerOneClass);
         statement.setInt(4, playerTwoClass);
         statement.setInt(5, winner);
         statement.setLong(6, startTime);
         statement.setLong(7, fightTime);
         statement.setInt(8, classed);
         statement.execute();
      } catch (SQLException var19) {
         if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.log(Level.SEVERE, "SQL exception while saving olympiad fight.", (Throwable)var19);
         }
      }
   }
}
