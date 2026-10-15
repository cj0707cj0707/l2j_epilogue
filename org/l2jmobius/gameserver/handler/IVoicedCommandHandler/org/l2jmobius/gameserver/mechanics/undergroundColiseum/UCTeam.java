package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.List;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.UCTower;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.games.UndergroundColiseumManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchUserDie;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class UCTeam {
   public static final byte NOT_DECIDED = 0;
   public static final byte WIN = 1;
   public static final byte FAIL = 2;
   private final int _index;
   private final UCArena _baseArena;
   protected final int _x;
   protected final int _y;
   protected final int _z;
   private final int _npcId;
   private UCTower _tower = null;
   private Party _party;
   private int _killCount;
   private byte _status;
   private Party _lastParty;
   private int _consecutiveWins;
   private long _registerTime;

   public UCTeam(int index, UCArena baseArena, int x, int y, int z, int npcId) {
      this._index = index;
      this._baseArena = baseArena;
      this._x = x;
      this._y = y;
      this._z = z;
      this._npcId = npcId;
      this.setStatus((byte)0);
   }

   public long getRegisterTime() {
      return this._registerTime;
   }

   public void setLastParty(Party party) {
      this._lastParty = party;
   }

   public void setRegisterTime(long time) {
      this._registerTime = time;
   }

   public void increaseConsecutiveWins() {
      this._consecutiveWins++;
      if (this._consecutiveWins > 1 && this._party != null && this._party.getLeader() != null) {
         UndergroundColiseumManager.getInstance().updateBestTeam(this._baseArena.getId(), this._party.getLeader().getName(), this._consecutiveWins);
      }
   }

   public int getConsecutiveWins() {
      return this._consecutiveWins;
   }

   public void spawnTower() {
      if (this._tower == null) {
         NpcTemplate template = NpcData.getInstance().getTemplate(this._npcId);
         if (template != null) {
            this._tower = new UCTower(this, template);
            this._tower.setInvul(false);
            this._tower.setCurrentHpMp(this._tower.getMaxHp(), this._tower.getMaxMp());
            this._tower.spawnMe(this._x, this._y, this._z);
         }
      }
   }

   public void deleteTower() {
      if (this._tower != null) {
         this._tower.deleteMe();
         this._tower = null;
      }
   }

   public void onKill(Player player, Player killer) {
      if (player != null && killer != null && this.getParty() != null) {
         if (player.getParty() == null || player.getParty() != killer.getParty()) {
            UCTeam otherTeam = this.getOtherTeam();
            otherTeam.increaseKillCount();
            player.addDeathCountUC();
            killer.addKillCountUC();
            this._baseArena.broadcastToAll(new ExPVPMatchUserDie(this._baseArena));
            if (player.getUCState() == 1) {
               for (UCPoint point : this._baseArena.getPoints()) {
                  if (point.checkPlayer(player)) {
                     break;
                  }
               }
            }

            if (this._tower == null) {
               boolean flag = true;

               for (Player member : this.getParty().getMembers()) {
                  if (member != null && !member.isDead()) {
                     flag = false;
                  }
               }

               if (flag) {
                  this.setStatus((byte)2);
                  otherTeam.setStatus((byte)1);
                  this._baseArena.runTaskNow();
               }
            } else {
               ThreadPool.schedule(() -> {
                  if (this._tower != null) {
                     if (player.isDead()) {
                        resPlayer(player);
                        player.teleToLocation(this._x + Rnd.get(2, 50), this._y + Rnd.get(10, 100), this._z, true);
                        if (player.hasSummon()) {
                           Summon summon = player.getSummon();
                           summon.abortAttack();
                           summon.abortCast();
                           if (!summon.isDead()) {
                              summon.setCurrentHp(summon.getMaxHp());
                              summon.setCurrentMp(summon.getMaxMp());
                              summon.teleToLocation(this._x + Rnd.get(2, 50), this._y + Rnd.get(10, 100), this._z, true);
                           }
                        }
                     }
                  }
               }, UndergroundColiseumConfig.UC_RESS_TIME * 1000);
            }
         }
      }
   }

   public void increaseKillCount() {
      this._killCount++;
   }

   public static void resPlayer(Player player) {
      if (player != null) {
         player.restoreExp(100.0);
         player.doRevive();
         player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
         player.setCurrentCp(player.getMaxCp());
      }
   }

   public void cleanUp() {
      if (this.getParty() != null) {
         this.getParty().setUCState(null);
         this._party = null;
      }

      this._party = null;
      this._lastParty = null;
      this._consecutiveWins = 0;
      this.setStatus((byte)0);
      this._killCount = 0;
   }

   public byte getStatus() {
      return this._status;
   }

   public UCArena getBaseArena() {
      return this._baseArena;
   }

   public void computeReward() {
      if (this._lastParty == null || this._lastParty != this.getOtherTeam().getParty()) {
         List<UCReward> rewards = this._baseArena.getRewards();
         double modifier = 1.0;

         modifier = switch (this._consecutiveWins) {
            case 1 -> 1.0;
            case 2 -> 1.06;
            case 3 -> 1.12;
            case 4 -> 1.18;
            case 5 -> 1.25;
            case 6 -> 1.27;
            case 7 -> 1.3;
            case 8 -> 1.32;
            case 9 -> 1.35;
            case 10 -> 1.37;
            default -> {
               if (this._consecutiveWins > 10) {
                  yield 1.4;
               }
            }
         };

         if (rewards == null || rewards.isEmpty()) {
            return;
         }

         for (Player member : this.getParty().getMembers()) {
            if (member != null) {
               for (UCReward reward : rewards) {
                  if (reward.getId() == -100) {
                     long amount = reward.isAllowMidifier() ? (long)(reward.getAmount() * modifier) : reward.getAmount();
                     if (member.getPcCafePoints() + amount > PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
                        amount = PremiumSystemConfig.PC_CAFE_MAX_POINTS - member.getPcCafePoints();
                     }

                     SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
                     sm.addInt((int)amount);
                     member.sendPacket(sm);
                     member.setPcCafePoints((int)(member.getPcCafePoints() + amount));
                     member.sendPacket(new ExPCCafePointInfo(member.getPcCafePoints(), (int)amount, 1));
                  } else if (reward.getId() == -200) {
                     if (member.getClan() != null) {
                        long amount = reward.isAllowMidifier() ? (long)(reward.getAmount() * modifier) : reward.getAmount();
                        member.getClan().addReputationScore((int)amount);
                     }
                  } else if (reward.getId() == -300) {
                     long amount = reward.isAllowMidifier() ? (long)(reward.getAmount() * modifier) : reward.getAmount();
                     member.setFame((int)(member.getFame() + amount));
                     SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_REPUTATION_SCORE);
                     sm.addInt((int)amount);
                     member.sendPacket(sm);
                     member.updateUserInfo();
                  } else if (reward.getId() > 0) {
                     long amount = reward.isAllowMidifier() ? (long)(reward.getAmount() * modifier) : reward.getAmount();
                     member.addItem(ItemProcessType.REWARD, reward.getId(), amount, null, true);
                  }
               }
            }
         }
      }
   }

   public void setStatus(byte status) {
      this._status = status;
      if (this._status == 1) {
         if (this.getIndex() == 0) {
            this._baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_BLUE_TEAM_IS_VICTORIOUS));
         } else {
            this._baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_RED_TEAM_IS_VICTORIOUS));
         }
      }

      switch (this._status) {
         case 0:
         default:
            break;
         case 1:
            this.increaseConsecutiveWins();
            this.computeReward();
            this.deleteTower();
            break;
         case 2:
            this.deleteTower();
      }
   }

   public void broadcastToTeam(ServerPacket packet) {
      Party party = this._party;
      if (party != null) {
         for (Player member : party.getMembers()) {
            if (member != null) {
               member.sendPacket(packet);
            }
         }
      }
   }

   public UCTeam getOtherTeam() {
      return this._baseArena.getTeams()[this.getOtherTeamIndex()];
   }

   public int getOtherTeamIndex() {
      return this._index == 0 ? 1 : 0;
   }

   public int getKillCount() {
      return this._killCount;
   }

   public void setParty(Party party) {
      Party oldParty = this._party;
      this._party = party;
      if (oldParty != null) {
         oldParty.setUCState(null);
      }

      if (this._party != null) {
         this._party.setUCState(this);
      }
   }

   public Party getParty() {
      return this._party;
   }

   public int getIndex() {
      return this._index;
   }
}
