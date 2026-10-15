package org.l2jmobius.gameserver.entity.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.FortSiegeGuardAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.SiegeGuardAI;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AbsorberInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DamageDoneInfo;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.status.AttackableStatus;
import org.l2jmobius.gameserver.entity.actor.tasks.attackable.CommandChannelTimer;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.EventDropManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

public class Attackable extends Npc {
   private boolean _isRaid = false;
   private boolean _isRaidMinion = false;
   private boolean _champion = false;
   private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
   private boolean _canReturnToSpawnPoint = true;
   private boolean _seeThroughSilentMove = false;
   private boolean _seeded = false;
   private Seed _seed = null;
   private int _seederObjId = 0;
   private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
   private int _spoilerObjectId;
   private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
   private boolean _overhit;
   private double _overhitDamage;
   private Creature _overhitAttacker;
   private CommandChannel _firstCommandChannelAttacked = null;
   private CommandChannelTimer _commandChannelTimer = null;
   private long _commandChannelLastAttack = 0L;
   private boolean _absorbed;
   private final Map<Integer, AbsorberInfo> _absorbersList = new ConcurrentHashMap<>();
   private boolean _mustGiveExpSp;
   private long _spawnTime = System.currentTimeMillis();
   protected int _onKillDelay = 2500;

   public Attackable(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Attackable);
      this.setInvul(false);
      this._mustGiveExpSp = true;
   }

   public AttackableStatus getStatus() {
      return (AttackableStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new AttackableStatus(this));
   }

   @Override
   protected CreatureAI initAI() {
      return new AttackableAI(this);
   }

   public Map<Creature, AggroInfo> getAggroList() {
      return this._aggroList;
   }

   public boolean canReturnToSpawnPoint() {
      return this._canReturnToSpawnPoint;
   }

   public void setCanReturnToSpawnPoint(boolean value) {
      this._canReturnToSpawnPoint = value;
   }

   public boolean canSeeThroughSilentMove() {
      return this._seeThroughSilentMove;
   }

   public void setSeeThroughSilentMove(boolean value) {
      this._seeThroughSilentMove = value;
   }

   public void useMagic(Skill skill) {
      if (skill != null && !this.isAlikeDead() && !skill.isPassive() && !this.isCastingNow() && !this.isSkillDisabled(skill)) {
         if (!(this.getCurrentMp() < this.getStat().getMpConsume(skill) + this.getStat().getMpInitialConsume(skill))
            && !(this.getCurrentHp() <= skill.getHpConsume())) {
            if (!skill.isStatic()) {
               if (skill.isMagic()) {
                  if (this.isMuted()) {
                     return;
                  }
               } else if (this.isPhysicalMuted()) {
                  return;
               }
            }

            WorldObject target = skill.getFirstOfTargetList(this);
            if (target != null) {
               this.getAI().setIntentionCast(skill, target);
            }
         }
      }
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, Skill skill) {
      this.reduceCurrentHp(damage, attacker, true, false, skill);
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (this._isRaid
         && !this.isMinion()
         && attacker != null
         && attacker.getParty() != null
         && attacker.getParty().isInCommandChannel()
         && attacker.getParty().getCommandChannel().meetRaidWarCondition(this)) {
         if (this._firstCommandChannelAttacked == null) {
            synchronized (this) {
               if (this._firstCommandChannelAttacked == null) {
                  this._firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
                  if (this._firstCommandChannelAttacked != null) {
                     this._commandChannelTimer = new CommandChannelTimer(this);
                     this._commandChannelLastAttack = System.currentTimeMillis();
                     ThreadPool.schedule(this._commandChannelTimer, 10000L);
                     this._firstCommandChannelAttacked.broadcastPacket(new CreatureSay(null, ChatType.PARTYROOM_ALL, "", "You have looting rights!"));
                  }
               }
            }
         } else if (attacker.getParty().getCommandChannel().equals(this._firstCommandChannelAttacked)) {
            this._commandChannelLastAttack = System.currentTimeMillis();
         }
      }

      if (attacker != null) {
         this.addDamage(attacker, (int)damage, skill);
      }

      if (this.isMonster()) {
         Monster master = this.asMonster();
         if (master.hasMinions()) {
            master.getMinionList().onAssist(this, attacker);
         }

         master = master.getLeader();
         if (master != null && master.hasMinions()) {
            master.getMinionList().onAssist(this, attacker);
         }
      }

      super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
   }

   public synchronized void setMustRewardExpSp(boolean value) {
      this._mustGiveExpSp = value;
   }

   public synchronized boolean getMustRewardExpSP() {
      return this._mustGiveExpSp && !this.isFakePlayer();
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (killer != null) {
            Player player = killer.asPlayer();
            if (player != null && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this)) {
               EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(player, this, killer.isSummon()), this, this._onKillDelay);
            }
         }

         if (this.isMonster()) {
            Monster mob = this.asMonster();
            Monster leader = mob.getLeader();
            if (leader != null && leader.hasMinions()) {
               int respawnTime = NpcConfig.MINIONS_RESPAWN_TIME.containsKey(this.getId()) ? NpcConfig.MINIONS_RESPAWN_TIME.get(this.getId()) * 1000 : -1;
               leader.getMinionList().onMinionDie(mob, respawnTime);
            }

            if (mob.hasMinions()) {
               mob.getMinionList().onMasterDie(false);
            }
         }

         return true;
      }
   }

   @Override
   protected void calculateRewards(Creature lastAttacker) {
      try {
         if (this._aggroList.isEmpty()) {
            return;
         }

         Map<Player, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
         Player maxDealer = null;
         long maxDamage = 0L;
         long totalDamage = 0L;

         for (AggroInfo info : this._aggroList.values()) {
            if (info != null) {
               Player attacker = info.getAttacker().asPlayer();
               if (attacker != null) {
                  long damage = info.getDamage();
                  if (damage > 1L && !(this.calculateDistance3D(attacker) > PlayerConfig.ALT_PARTY_RANGE)) {
                     totalDamage += damage;
                     DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
                     reward.addDamage(damage);
                     if (reward.getDamage() > maxDamage) {
                        maxDealer = attacker;
                        maxDamage = reward.getDamage();
                     }
                  }
               }
            }
         }

         List<Attackable.PartyContainer> damagingParties = new ArrayList<>();

         for (AggroInfo infox : this._aggroList.values()) {
            Creature attacker = infox.getAttacker();
            if (attacker != null) {
               long totalMemberDamage = 0L;
               Party party = attacker.getParty();
               if (party != null) {
                  Attackable.PartyContainer found = null;
                  int i = 0;

                  for (int damagingPartiesSize = damagingParties.size(); i < damagingPartiesSize; i++) {
                     Attackable.PartyContainer p = damagingParties.get(i);
                     if (p.party == party) {
                        found = p;
                        break;
                     }
                  }

                  Attackable.PartyContainer container = found != null ? found : new Attackable.PartyContainer(party, 0L);

                  for (Player e : party.getMembers()) {
                     AggroInfo memberAggro = this._aggroList.get(e);
                     if (memberAggro != null && memberAggro.getDamage() > 1L) {
                        totalMemberDamage += memberAggro.getDamage();
                     }
                  }

                  container.damage = totalMemberDamage;
                  if (found == null) {
                     damagingParties.add(container);
                  }
               }
            }
         }

         damagingParties.sort(Comparator.comparingLong(c -> -c.damage));
         Attackable.PartyContainer mostDamageParty = !damagingParties.isEmpty() ? damagingParties.get(0) : null;
         if (mostDamageParty != null && mostDamageParty.damage > maxDamage) {
            Player leader = mostDamageParty.party.getLeader();
            this.doItemDrop(leader);
            EventDropManager.getInstance().doEventDrop(leader, this);
         } else {
            this.doItemDrop((Creature)(maxDealer != null && maxDealer.isOnline() ? maxDealer : lastAttacker));
            EventDropManager.getInstance().doEventDrop(lastAttacker, this);
         }

         if (!this.getMustRewardExpSP()) {
            return;
         }

         if (!rewards.isEmpty()) {
            for (DamageDoneInfo reward : rewards.values()) {
               if (reward != null) {
                  Player attacker = reward.getAttacker();
                  long damage = reward.getDamage();
                  Party attackerParty = attacker.getParty();
                  float penalty = attacker.hasServitor() ? attacker.getSummon().asServitor().getExpMultiplier() : 1.0F;
                  if (attackerParty != null) {
                     long partyDmg = 0L;
                     double partyMul = 1.0;
                     int partyLvl = 0;
                     List<Player> rewardedMembers = new ArrayList<>();

                     for (Player partyPlayer : attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers()) {
                        if (partyPlayer != null && !partyPlayer.isDead()) {
                           DamageDoneInfo reward2 = rewards.get(partyPlayer);
                           if (reward2 != null) {
                              if (this.calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE) {
                                 partyDmg += reward2.getDamage();
                                 rewardedMembers.add(partyPlayer);
                                 if (partyPlayer.getLevel() > partyLvl) {
                                    if (attackerParty.isInCommandChannel()) {
                                       partyLvl = attackerParty.getCommandChannel().getLevel();
                                    } else {
                                       partyLvl = partyPlayer.getLevel();
                                    }
                                 }
                              }

                              rewards.remove(partyPlayer);
                           } else if (this.calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE) {
                              rewardedMembers.add(partyPlayer);
                              if (partyPlayer.getLevel() > partyLvl) {
                                 if (attackerParty.isInCommandChannel()) {
                                    partyLvl = attackerParty.getCommandChannel().getLevel();
                                 } else {
                                    partyLvl = partyPlayer.getLevel();
                                 }
                              }
                           }
                        }
                     }

                     if (partyDmg < totalDamage) {
                        partyMul = (double)partyDmg / totalDamage;
                     }

                     double[] expSp = this.calculateExpAndSp(partyLvl, partyDmg, totalDamage);
                     double exp = expSp[0];
                     double sp = expSp[1];
                     if (ChampionMonstersConfig.CHAMPION_ENABLE && this._champion) {
                        exp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
                        sp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
                     }

                     exp *= partyMul;
                     sp *= partyMul;
                     Creature overhitAttacker = this._overhitAttacker;
                     if (this._overhit && overhitAttacker != null) {
                        Player player = overhitAttacker.asPlayer();
                        if (player != null && attacker == player) {
                           attacker.sendPacket(SystemMessageId.OVER_HIT);
                           exp += this.calculateOverhitExp(exp);
                        }
                     }

                     if (partyDmg > 0L) {
                        attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, partyDmg, this);
                     }
                  } else if (this.isInSurroundingRegion(attacker)) {
                     double[] expSpx = this.calculateExpAndSp(attacker.getLevel(), damage, totalDamage);
                     double expx = expSpx[0];
                     double spx = expSpx[1];
                     if (ChampionMonstersConfig.CHAMPION_ENABLE && this._champion) {
                        expx *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
                        spx *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
                     }

                     expx *= penalty;
                     Creature overhitAttackerx = this._overhitAttacker;
                     if (this._overhit && overhitAttackerx != null) {
                        Player player = overhitAttackerx.asPlayer();
                        if (player != null && attacker == player) {
                           attacker.sendPacket(SystemMessageId.OVER_HIT);
                           expx += this.calculateOverhitExp(expx);
                        }
                     }

                     if (!attacker.isDead()) {
                        long addExp = Math.round(attacker.calcStat(Stat.EXPSP_RATE, expx, null, null));
                        int addSp = (int)attacker.calcStat(Stat.EXPSP_RATE, spx, null, null);
                        if (attacker.hasPremiumStatus()) {
                           addExp = (long)((float)addExp * PremiumSystemConfig.PREMIUM_RATE_XP);
                           addSp = (int)(addSp * PremiumSystemConfig.PREMIUM_RATE_SP);
                        }

                        attacker.addExpAndSp(addExp, addSp, this.useVitalityRate());
                        if (addExp > 0L && this.useVitalityRate()) {
                           attacker.updateVitalityPoints(this.getVitalityPoints(attacker.getLevel(), damage), true, false);
                           PcCafePointsManager.getInstance().givePcCafePoint(attacker, expx);
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var31) {
         LOGGER.log(Level.SEVERE, "", (Throwable)var31);
      }
   }

   @Override
   public void addAttackerToAttackByList(Creature creature) {
      if (creature != null && creature != this) {
         Set<Creature> attackByList = this.getAttackByList();
         if (!attackByList.contains(creature)) {
            attackByList.add(creature);
         }
      }
   }

   public Creature getMainDamageDealer() {
      if (this._aggroList.isEmpty()) {
         return null;
      } else {
         long damage = 0L;
         Creature damageDealer = null;

         for (AggroInfo info : this._aggroList.values()) {
            if (info != null && info.getDamage() > damage && this.calculateDistance3D(info.getAttacker()) < PlayerConfig.ALT_PARTY_RANGE) {
               damage = info.getDamage();
               damageDealer = info.getAttacker();
            }
         }

         return damageDealer;
      }
   }

   public void addDamage(Creature attacker, int damage, Skill skill) {
      if (attacker != null) {
         if (!this.isDead()) {
            try {
               if (this.isWalker() && !this.isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this)) {
                  WalkingManager.getInstance().stopMoving(this, false, true);
               }

               this.getAI().notifyActionAttacked(attacker);
               long hateValue = (long)(damage * 100L / (this.getLevel() + 7) * this.getHateRatio(attacker));
               this.addDamageHate(attacker, damage, (int)hateValue);
               Player player = attacker.asPlayer();
               if (player != null && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this)) {
                  EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
               }
            } catch (Exception var7) {
               LOGGER.log(Level.SEVERE, "", (Throwable)var7);
            }
         }
      }
   }

   public double getHateRatio(Creature attacker) {
      if (!attacker.isPlayer()) {
         return 1.0;
      } else {
         StatSet params = this.getTemplate().getParameters();
         if (params.isEmpty()) {
            return 1.0;
         } else {
            Player player = attacker.asPlayer();
            int ratio = 0;
            CategoryType group = params.getEnum("SetHateGroup", CategoryType.class, null);
            if (group != null && player.isInCategory(group)) {
               ratio += params.getInt("SetHateGroupRatio", 0);
            }

            if (player.getPlayerClass().getId() == params.getInt("SetHateOccupation", -1)) {
               ratio += params.getInt("SetHateOccupationRatio", 0);
            }

            if (player.getRace() == params.getEnum("SetHateRace", Race.class, null)) {
               ratio += params.getInt("SetHateRaceRatio", 0);
            }

            return 1.0 + ratio / 100.0;
         }
      }
   }

   public void addDamageHate(Creature attacker, long damage, long aggroValue) {
      if (attacker != null && attacker != this) {
         if (!this.isFakePlayer() || FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC || !attacker.isFakePlayer()) {
            AggroInfo ai = this._aggroList.computeIfAbsent(attacker, AggroInfo::new);
            ai.addDamage(damage);
            long aggro = aggroValue;
            Player targetPlayer = attacker.asPlayer();
            if (targetPlayer == null || targetPlayer.getTrap() == null || !targetPlayer.getTrap().isTriggered()) {
               ai.addHate(aggroValue);
            }

            if (targetPlayer != null && aggroValue == 0L) {
               this.addDamageHate(attacker, 0L, 1L);
               if (this.getAI().getIntention() == Intention.IDLE) {
                  this.getAI().setIntentionActive();
               }

               if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, this)) {
                  EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
               }
            } else if (targetPlayer == null && aggroValue == 0L) {
               aggro = 1L;
               ai.addHate(1L);
            }

            if (aggro != 0L && this.getAI().getIntention() == Intention.IDLE) {
               this.getAI().setIntentionActive();
            }
         }
      }
   }

   public void reduceHate(Creature target, long amount) {
      if (this.getAI() instanceof SiegeGuardAI || this.getAI() instanceof FortSiegeGuardAI) {
         this.stopHating(target);
         this.setTarget(null);
         this.getAI().setIntentionIdle();
      } else if (target != null) {
         AggroInfo ai = this._aggroList.get(target);
         if (ai != null) {
            ai.addHate(amount);
            if (ai.getHate() >= 0L && this.getMostHated() == null) {
               ((AttackableAI)this.getAI()).setGlobalAggro(-25);
               this.clearAggroList();
               this.getAI().setIntentionActive();
               if (!this.isFakePlayer()) {
                  this.setWalking();
               }
            }
         }
      } else {
         Creature mostHated = this.getMostHated();
         if (mostHated == null) {
            ((AttackableAI)this.getAI()).setGlobalAggro(-25);
         } else {
            for (AggroInfo ai : this._aggroList.values()) {
               ai.addHate(amount);
            }

            if (this.getHating(mostHated) >= 0L) {
               ((AttackableAI)this.getAI()).setGlobalAggro(-25);
               this.clearAggroList();
               this.getAI().setIntentionActive();
               if (!this.isFakePlayer()) {
                  this.setWalking();
               }
            }
         }
      }
   }

   public void stopHating(Creature target) {
      if (target != null) {
         AggroInfo ai = this._aggroList.get(target);
         if (ai != null) {
            ai.stopHate();
         }
      }
   }

   public Creature getMostHated() {
      if (!this._aggroList.isEmpty() && !this.isAlikeDead()) {
         Creature mostHated = null;
         long maxHate = 0L;

         for (AggroInfo ai : this._aggroList.values()) {
            if (ai != null && ai.checkHate(this) > maxHate) {
               mostHated = ai.getAttacker();
               maxHate = ai.getHate();
            }
         }

         return mostHated;
      } else {
         return null;
      }
   }

   public List<Creature> get2MostHated() {
      if (!this._aggroList.isEmpty() && !this.isAlikeDead()) {
         Creature mostHated = null;
         Creature secondMostHated = null;
         long maxHate = 0L;
         List<Creature> result = new ArrayList<>();

         for (AggroInfo ai : this._aggroList.values()) {
            if (ai != null && ai.checkHate(this) > maxHate) {
               secondMostHated = mostHated;
               mostHated = ai.getAttacker();
               maxHate = ai.getHate();
            }
         }

         result.add(mostHated);
         if (this.getAttackByList().contains(secondMostHated)) {
            result.add(secondMostHated);
         } else {
            result.add(null);
         }

         return result;
      } else {
         return null;
      }
   }

   public List<Creature> getHateList() {
      if (!this._aggroList.isEmpty() && !this.isAlikeDead()) {
         List<Creature> result = new ArrayList<>();

         for (AggroInfo ai : this._aggroList.values()) {
            if (ai != null) {
               ai.checkHate(this);
               result.add(ai.getAttacker());
            }
         }

         return result;
      } else {
         return null;
      }
   }

   public long getHating(Creature target) {
      if (!this._aggroList.isEmpty() && target != null) {
         AggroInfo ai = this._aggroList.get(target);
         if (ai == null) {
            return 0L;
         } else {
            if (ai.getAttacker().isPlayer()) {
               Player act = ai.getAttacker().asPlayer();
               if (act.isInvisible() || act.isInvul() || act.isSpawnProtected()) {
                  this._aggroList.remove(target);
                  return 0L;
               }
            }

            if (!ai.getAttacker().isSpawned() || ai.getAttacker().isInvisible()) {
               this._aggroList.remove(target);
               return 0L;
            } else if (ai.getAttacker().isAlikeDead()) {
               ai.stopHate();
               return 0L;
            } else {
               return ai.getHate();
            }
         }
      } else {
         return 0L;
      }
   }

   public void doItemDrop(Creature mainDamageDealer) {
      this.doItemDrop(this.getTemplate(), mainDamageDealer);
   }

   public void doItemDrop(NpcTemplate npcTemplate, Creature mainDamageDealer) {
      if (mainDamageDealer != null) {
         Player player = mainDamageDealer.asPlayer();
         if (player == null) {
            if (mainDamageDealer.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_CAN_DROP_ITEMS) {
               Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, mainDamageDealer);
               if (deathItems != null) {
                  for (ItemHolder drop : deathItems) {
                     ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
                     if (!PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(item.getId())
                        && !this.isFlying()
                        && (item.hasExImmediateEffect() || (this._isRaid || !PlayerConfig.AUTO_LOOT) && (!this._isRaid || !PlayerConfig.AUTO_LOOT_RAIDS))) {
                        if (PlayerConfig.AUTO_LOOT_HERBS && item.hasExImmediateEffect()) {
                           for (SkillHolder skillHolder : item.getSkills()) {
                              this.doSimultaneousCast(skillHolder.getSkill());
                           }

                           mainDamageDealer.broadcastInfo();
                        } else {
                           Item droppedItem = this.dropItem(mainDamageDealer, drop);
                           if (FakePlayersConfig.FAKE_PLAYER_CAN_PICKUP) {
                              mainDamageDealer.getFakePlayerDrops().add(droppedItem);
                           }
                        }
                     }
                  }

                  deathItems.clear();
               }
            }
         } else {
            CursedWeaponsManager.getInstance().checkDrop(this, player);
            if (this.isSpoiled()) {
               this._sweepItems.set(npcTemplate.calculateDrops(DropType.SPOIL, this, player));
            }

            Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, player);
            if (deathItems != null) {
               for (ItemHolder dropx : deathItems) {
                  ItemTemplate item = ItemData.getInstance().getTemplate(dropx.getId());
                  if (!PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(item.getId())
                     && !this.isFlying()
                     && (item.hasExImmediateEffect() || (this._isRaid || !PlayerConfig.AUTO_LOOT) && (!this._isRaid || !PlayerConfig.AUTO_LOOT_RAIDS))
                     && (!item.hasExImmediateEffect() || !PlayerConfig.AUTO_LOOT_HERBS)) {
                     this.dropItem(player, dropx);
                  } else {
                     player.doAutoLoot(this, dropx);
                  }

                  if (this._isRaid && !this._isRaidMinion && dropx.getCount() > 0L) {
                     SystemMessage sm = new SystemMessage(SystemMessageId.C1_DIED_AND_DROPPED_S3_S2);
                     sm.addString(this.getName());
                     sm.addItemName(item);
                     sm.addLong(dropx.getCount());
                     this.broadcastPacket(sm);
                  }
               }

               deathItems.clear();
            }
         }
      }
   }

   public Item getActiveWeapon() {
      return null;
   }

   public boolean isInAggroList(Creature creature) {
      return creature != null && this._aggroList.containsKey(creature);
   }

   public void clearAggroList() {
      this._aggroList.clear();
      this._overhit = false;
      this._overhitDamage = 0.0;
      this._overhitAttacker = null;
   }

   @Override
   public boolean isSweepActive() {
      return this._sweepItems.get() != null;
   }

   public List<ItemTemplate> getSpoilLootItems() {
      Collection<ItemHolder> sweepItems = this._sweepItems.get();
      List<ItemTemplate> lootItems = new ArrayList<>();
      if (sweepItems != null) {
         for (ItemHolder item : sweepItems) {
            lootItems.add(ItemData.getInstance().getTemplate(item.getId()));
         }
      }

      return lootItems;
   }

   public Collection<ItemHolder> takeSweep() {
      return this._sweepItems.getAndSet(null);
   }

   public ItemHolder takeHarvest() {
      return this._harvestItem.getAndSet(null);
   }

   public boolean isOldCorpse(Player attacker, int remainingTime, boolean sendMessage) {
      if (this.isDead() && DecayTaskManager.getInstance().getRemainingTime(this) < remainingTime) {
         if (sendMessage && attacker != null) {
            attacker.sendPacket(SystemMessageId.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean checkSpoilOwner(Player sweeper, boolean sendMessage) {
      if (sweeper.getObjectId() != this._spoilerObjectId && !sweeper.isInLooterParty(this._spoilerObjectId)) {
         if (sendMessage) {
            sweeper.sendPacket(SystemMessageId.THERE_ARE_NO_PRIORITY_RIGHTS_ON_A_SWEEPER);
         }

         return false;
      } else {
         return true;
      }
   }

   public void overhitEnabled(boolean status) {
      this._overhit = status;
   }

   public void setOverhitValues(Creature attacker, double damage) {
      double overhitDmg = -(this.getCurrentHp() - damage);
      if (overhitDmg < 0.0) {
         this.overhitEnabled(false);
         this._overhitDamage = 0.0;
         this._overhitAttacker = null;
      } else {
         this.overhitEnabled(true);
         this._overhitDamage = overhitDmg;
         this._overhitAttacker = attacker;
      }
   }

   public Creature getOverhitAttacker() {
      return this._overhitAttacker;
   }

   public double getOverhitDamage() {
      return this._overhitDamage;
   }

   public boolean isOverhit() {
      return this._overhit;
   }

   public void absorbSoul() {
      this._absorbed = true;
   }

   public boolean isAbsorbed() {
      return this._absorbed;
   }

   public void addAbsorber(Player attacker) {
      AbsorberInfo ai = this._absorbersList.get(attacker.getObjectId());
      if (ai == null) {
         this._absorbersList.put(attacker.getObjectId(), new AbsorberInfo(attacker.getObjectId(), this.getCurrentHp()));
      } else {
         ai.setAbsorbedHp(this.getCurrentHp());
      }

      this.absorbSoul();
   }

   public void resetAbsorbList() {
      this._absorbed = false;
      this._absorbersList.clear();
   }

   public Map<Integer, AbsorberInfo> getAbsorbersList() {
      return this._absorbersList;
   }

   private double[] calculateExpAndSp(int charLevel, long damage, long totalDamage) {
      int levelDiff = charLevel - this.getLevel();
      double xp = 0.0;
      double sp = 0.0;
      if (levelDiff < RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE && levelDiff > -RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE) {
         xp = Math.max(0L, this.getExpReward(charLevel) * damage / totalDamage);
         sp = Math.max(0L, this.getSpReward(charLevel) * damage / totalDamage);
         if (charLevel > 84 && levelDiff <= -3) {
            double mul = switch (levelDiff) {
               case -10 -> 0.03;
               case -9 -> 0.05;
               case -8 -> 0.09;
               case -7 -> 0.15;
               case -6 -> 0.25;
               case -5 -> 0.42;
               case -4 -> 0.67;
               case -3 -> 0.97;
               default -> 1.0;
            };
            xp *= mul;
            sp *= mul;
         }
      }

      return new double[]{xp, sp};
   }

   public double calculateOverhitExp(double exp) {
      double overhitPercentage = this._overhitDamage * 100.0 / this.getMaxHp();
      if (overhitPercentage > 25.0) {
         overhitPercentage = 25.0;
      }

      return overhitPercentage / 100.0 * exp;
   }

   @Override
   public boolean canBeAttacked() {
      return true;
   }

   @Override
   public void onSpawn() {
      this._spawnTime = System.currentTimeMillis();
      super.onSpawn();
      this.setSpoilerObjectId(0);
      this.clearAggroList();
      this._harvestItem.set(null);
      if (this.isFakePlayer()) {
         this.getFakePlayerDrops().clear();
         this.setKarma(0);
         this.setScriptValue(0);
         this.setRunning();
      } else {
         this.setWalking();
      }

      this._seeded = false;
      this._seed = null;
      this._seederObjId = 0;
      this.overhitEnabled(false);
      this._sweepItems.set(null);
      this.resetAbsorbList();
      this.setWalking();
      if (this.hasAI()) {
         if (this.getAI() instanceof AttackableAI) {
            ((AttackableAI)this.getAI()).rollGlobalAggro();
         }

         this.getAI().setIntentionActive();
         if (!this.isInActiveRegion()) {
            this.getAI().stopAITask();
         }
      }
   }

   public long getSpawnTime() {
      return this._spawnTime;
   }

   public boolean isSpoiled() {
      return this._spoilerObjectId != 0;
   }

   public int getSpoilerObjectId() {
      return this._spoilerObjectId;
   }

   public void setSpoilerObjectId(int spoilerObjectId) {
      this._spoilerObjectId = spoilerObjectId;
   }

   public void setSeeded(Player seeder) {
      if (this._seed != null && this._seederObjId == seeder.getObjectId()) {
         this._seeded = true;
         int count = 1;

         for (int skillId : this.getTemplate().getSkills().keySet()) {
            switch (skillId) {
               case 4303:
                  count *= 2;
                  break;
               case 4304:
                  count *= 3;
                  break;
               case 4305:
                  count *= 4;
                  break;
               case 4306:
                  count *= 5;
                  break;
               case 4307:
                  count *= 6;
                  break;
               case 4308:
                  count *= 7;
                  break;
               case 4309:
                  count *= 8;
                  break;
               case 4310:
                  count *= 9;
            }
         }

         int diff = this.getLevel() - this._seed.getLevel() - 5;
         if (diff > 0) {
            count += diff;
         }

         this._harvestItem.set(new ItemHolder(this._seed.getCropId(), count * RatesConfig.RATE_DROP_MANOR));
      }
   }

   public void setSeeded(Seed seed, Player seeder) {
      if (!this._seeded) {
         this._seed = seed;
         this._seederObjId = seeder.getObjectId();
      }
   }

   public int getSeederId() {
      return this._seederObjId;
   }

   public Seed getSeed() {
      return this._seed;
   }

   public boolean isSeeded() {
      return this._seeded;
   }

   public void setOnKillDelay(int delay) {
      this._onKillDelay = delay;
   }

   public int getOnKillDelay() {
      return this._onKillDelay;
   }

   @Override
   public boolean hasRandomAnimation() {
      return GeneralConfig.MAX_MONSTER_ANIMATION > 0 && this.isRandomAnimationEnabled() && !(this instanceof GrandBoss);
   }

   public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer) {
      this._commandChannelTimer = commandChannelTimer;
   }

   public CommandChannelTimer getCommandChannelTimer() {
      return this._commandChannelTimer;
   }

   public CommandChannel getFirstCommandChannelAttacked() {
      return this._firstCommandChannelAttacked;
   }

   public void setFirstCommandChannelAttacked(CommandChannel firstCommandChannelAttacked) {
      this._firstCommandChannelAttacked = firstCommandChannelAttacked;
   }

   public long getCommandChannelLastAttack() {
      return this._commandChannelLastAttack;
   }

   public void setCommandChannelLastAttack(long channelLastAttack) {
      this._commandChannelLastAttack = channelLastAttack;
   }

   public void returnHome() {
      this.clearAggroList();
      if (this.hasAI() && this.getSpawn() != null) {
         this.getAI().setIntentionMoveTo(this.getSpawn().getLocation());
      }
   }

   public float getVitalityPoints(int level, long damage) {
      if (damage <= 0L) {
         return 0.0F;
      } else {
         long expReward = this.getExpReward(level);
         float divider = this.getLevel() > 0 && expReward > 0L
            ? this.getTemplate().getBaseHpMax() * 9.0F * this.getLevel() * this.getLevel() / (float)(100L * expReward)
            : 0.0F;
         return divider == 0.0F ? 0.0F : (float)(-Math.min(damage, (long)this.getMaxHp())) / divider;
      }
   }

   public boolean useVitalityRate() {
      return !this._champion || ChampionMonstersConfig.CHAMPION_ENABLE_VITALITY;
   }

   @Override
   public boolean isRaid() {
      return this._isRaid;
   }

   public void setIsRaid(boolean isRaid) {
      this._isRaid = isRaid;
   }

   public void setIsRaidMinion(boolean value) {
      this._isRaid = value;
      this._isRaidMinion = value;
   }

   @Override
   public boolean isRaidMinion() {
      return this._isRaidMinion;
   }

   @Override
   public boolean isMinion() {
      return this.getLeader() != null;
   }

   public Attackable getLeader() {
      return null;
   }

   public void setChampion(boolean champ) {
      this._champion = champ;
      if (ChampionMonstersConfig.SHOW_CHAMPION_AURA) {
         this.setTeam(champ ? Team.RED : Team.NONE);
      }
   }

   @Override
   public boolean isChampion() {
      return this._champion;
   }

   @Override
   public boolean isAttackable() {
      return true;
   }

   @Override
   public Attackable asAttackable() {
      return this;
   }

   private static class PartyContainer {
      public Party party;
      public long damage;

      public PartyContainer(Party party, long damage) {
         this.party = party;
         this.damage = damage;
      }
   }
}
