package org.l2jmobius.gameserver.entity.actor.stat;

import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityPointInfo;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PlayerStat extends PlayableStat {
   private int _oldMaxHp;
   private int _oldMaxMp;
   private int _oldMaxCp;
   private float _vitalityPoints = 1.0F;
   private byte _vitalityLevel = 0;
   private long _startingXp;
   private int _maxCubicCount = 1;
   private final AtomicInteger _talismanSlots = new AtomicInteger();
   private boolean _cloakSlot = false;
   public static final int[] VITALITY_LEVELS = new int[]{240, 2000, 13000, 17000, 20000};
   public static final int MAX_VITALITY_POINTS = VITALITY_LEVELS[4];
   public static final int MIN_VITALITY_POINTS = 1;

   public PlayerStat(Player player) {
      super(player);
   }

   @Override
   public boolean addExp(long value) {
      Player player = this.getActiveChar();
      if (!player.getAccessLevel().canGainExp()) {
         return false;
      } else if (!super.addExp(value)) {
         return false;
      } else {
         if (!player.isCursedWeaponEquipped() && player.getKarma() > 0 && !player.isInsideZone(ZoneId.PVP)) {
            int karmaLost = Formulas.calculateKarmaLost(player, value);
            if (karmaLost > 0) {
               player.setKarma(player.getKarma() - karmaLost);
               SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1);
               msg.addInt(player.getKarma());
               player.sendPacket(msg);
            }
         }

         player.updateUserInfo();
         return true;
      }
   }

   public boolean addExpAndSp(double addToExpValue, double addToSpValue, boolean useBonuses) {
      Player player = this.getActiveChar();
      if (!player.getAccessLevel().canGainExp()) {
         return false;
      } else {
         double bonusExp = 1.0;
         double bonusSp = 1.0;
         if (useBonuses) {
            bonusExp = this.getExpBonusMultiplier();
            bonusSp = this.getSpBonusMultiplier();
         }

         double addToExp = addToExpValue * bonusExp;
         double addToSp = addToSpValue * bonusSp;
         double ratioTakenByPlayer = 0.0;
         if (player.hasPet() && player.calculateDistance3D(player.getSummon()) < PlayerConfig.ALT_PARTY_RANGE) {
            Pet pet = player.getSummon().asPet();
            ratioTakenByPlayer = pet.getPetLevelData().getOwnerExpTaken() / 100.0F;
            if (ratioTakenByPlayer > 1.0) {
               ratioTakenByPlayer = 1.0;
            }

            if (!pet.isDead()) {
               pet.addExpAndSp(addToExp * (1.0 - ratioTakenByPlayer), addToSp * (1.0 - ratioTakenByPlayer));
            }

            addToExp *= ratioTakenByPlayer;
            addToSp *= ratioTakenByPlayer;
         }

         long finalExp = Math.round(addToExp);
         long finalSp = Math.round(addToSp);
         boolean expAdded = this.addExp(finalExp);
         boolean spAdded = this.addSp(finalSp);
         if (!expAdded && spAdded) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_SP);
            sm.addLong(finalSp);
            player.sendPacket(sm);
         } else if (expAdded && !spAdded) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_EXPERIENCE);
            sm.addLong(finalExp);
            player.sendPacket(sm);
         } else if (finalExp > 0L || finalSp > 0L) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_EXPERIENCE_AND_S2_SP);
            sm.addLong((long)addToExp);
            sm.addLong((long)addToSp);
            player.sendPacket(sm);
         }

         return true;
      }
   }

   @Override
   public boolean removeExpAndSp(long addToExp, long addToSp) {
      return this.removeExpAndSp(addToExp, addToSp, true);
   }

   public boolean removeExpAndSp(long addToExp, long addToSp, boolean sendMessage) {
      int level = this.getLevel();
      if (!super.removeExpAndSp(addToExp, addToSp)) {
         return false;
      } else {
         if (sendMessage) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_EXPERIENCE_HAS_DECREASED_BY_S1);
            sm.addLong(addToExp);
            Player player = this.getActiveChar();
            player.sendPacket(sm);
            sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
            sm.addLong(addToSp);
            player.sendPacket(sm);
            if (this.getLevel() < level) {
               player.broadcastStatusUpdate();
            }
         }

         return true;
      }
   }

   @Override
   public boolean addLevel(byte value) {
      if (this.getLevel() + value > ExperienceData.getInstance().getMaxLevel() - 1) {
         return false;
      } else {
         Player player = this.getActiveChar();
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LEVEL_CHANGED, player)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLevelChanged(player, this.getLevel(), this.getLevel() + value), player);
         }

         boolean levelIncreased = super.addLevel(value);
         if (levelIncreased) {
            if (!PlayerConfig.DISABLE_TUTORIAL) {
               QuestState qs = player.getQuestState("Q00255_Tutorial");
               if (qs != null) {
                  qs.getQuest().notifyEvent("CE40", null, player);
               }
            }

            player.setCurrentCp(this.getMaxCp());
            player.broadcastPacket(new SocialAction(player.getObjectId(), 2122));
            player.sendPacket(SystemMessageId.YOUR_LEVEL_HAS_INCREASED);
         }

         player.rewardSkills();
         Clan clan = player.getClan();
         if (clan != null) {
            clan.updateClanMember(player);
            clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
         }

         if (player.isInParty()) {
            player.getParty().recalculatePartyLevel();
         }

         if (player.isTransformed() || player.isInStance()) {
            player.getTransformation().onLevelUp(player);
         }

         if (player.hasPet()) {
            Pet pet = player.getSummon().asPet();
            if (pet.getPetData().isSynchLevel() && pet.getLevel() != this.getLevel()) {
               byte availableLevel = (byte)Math.min(pet.getPetData().getMaxLevel(), this.getLevel());
               pet.getStat().setLevel(availableLevel);
               pet.getStat().getExpForLevel(availableLevel);
               pet.setCurrentHp(pet.getMaxHp());
               pet.setCurrentMp(pet.getMaxMp());
               pet.broadcastPacket(new SocialAction(player.getObjectId(), 2122));
               pet.updateAndBroadcastStatus(1);
            }
         }

         StatusUpdate su = new StatusUpdate(player);
         su.addAttribute(1, this.getLevel());
         su.addAttribute(34, this.getMaxCp());
         su.addAttribute(10, this.getMaxHp());
         su.addAttribute(12, this.getMaxMp());
         player.sendPacket(su);
         player.refreshOverloaded();
         player.refreshExpertisePenalty();
         player.updateUserInfo();
         return levelIncreased;
      }
   }

   @Override
   public boolean addSp(long value) {
      if (!super.addSp(value)) {
         return false;
      } else {
         Player player = this.getActiveChar();
         StatusUpdate su = new StatusUpdate(player);
         su.addAttribute(13, (int)this.getSp());
         player.sendPacket(su);
         return true;
      }
   }

   @Override
   public long getExpForLevel(int level) {
      return ExperienceData.getInstance().getExpForLevel(level);
   }

   public Player getActiveChar() {
      return super.getActiveChar().asPlayer();
   }

   @Override
   public long getExp() {
      Player player = this.getActiveChar();
      return player.isSubClassActive() ? player.getSubClasses().get(player.getClassIndex()).getExp() : super.getExp();
   }

   public long getBaseExp() {
      return super.getExp();
   }

   @Override
   public void setExp(long value) {
      Player player = this.getActiveChar();
      if (player.isSubClassActive()) {
         player.getSubClasses().get(player.getClassIndex()).setExp(value);
      } else {
         super.setExp(value);
      }
   }

   public void setStartingExp(long value) {
      if (GeneralConfig.BOTREPORT_ENABLE) {
         this._startingXp = value;
      }
   }

   public long getStartingExp() {
      return this._startingXp;
   }

   public int getMaxCubicCount() {
      return this._maxCubicCount;
   }

   public void setMaxCubicCount(int cubicCount) {
      this._maxCubicCount = cubicCount;
   }

   public int getTalismanSlots() {
      return this._talismanSlots.get();
   }

   public void addTalismanSlots(int count) {
      this._talismanSlots.addAndGet(count);
   }

   public boolean canEquipCloak() {
      return !this.getActiveChar().hasEnteredWorld() ? true : this._cloakSlot;
   }

   public void setCloakSlotStatus(boolean cloakSlot) {
      this._cloakSlot = cloakSlot;
   }

   @Override
   public byte getLevel() {
      Player player = this.getActiveChar();
      if (player.isSubClassActive()) {
         SubClassHolder holder = player.getSubClasses().get(player.getClassIndex());
         if (holder != null) {
            return holder.getLevel();
         }
      }

      return super.getLevel();
   }

   public byte getBaseLevel() {
      return super.getLevel();
   }

   @Override
   public void setLevel(byte value) {
      byte level = value;
      if (value > ExperienceData.getInstance().getMaxLevel() - 1) {
         level = (byte)(ExperienceData.getInstance().getMaxLevel() - 1);
      }

      Player player = this.getActiveChar();
      if (player.isSubClassActive()) {
         player.getSubClasses().get(player.getClassIndex()).setLevel(value);
      } else {
         super.setLevel(level);
      }
   }

   @Override
   public int getMaxCp() {
      Player player = this.getActiveChar();
      int val = player == null ? 1 : (int)this.calcStat(Stat.MAX_CP, player.getTemplate().getBaseCpMax(player.getLevel()));
      if (val != this._oldMaxCp) {
         this._oldMaxCp = val;
         if (player != null) {
            PlayerStatus status = player.getStatus();
            if (status.getCurrentCp() != val) {
               status.setCurrentCp(status.getCurrentCp());
            }
         }
      }

      return val;
   }

   @Override
   public int getMaxHp() {
      Player player = this.getActiveChar();
      int val = player == null ? 1 : (int)this.calcStat(Stat.MAX_HP, player.getTemplate().getBaseHpMax(player.getLevel()));
      if (val != this._oldMaxHp) {
         this._oldMaxHp = val;
         if (player != null) {
            PlayerStatus status = player.getStatus();
            if (status.getCurrentHp() != val) {
               status.setCurrentHp(status.getCurrentHp());
            }
         }
      }

      return val;
   }

   @Override
   public int getMaxMp() {
      Player player = this.getActiveChar();
      int val = player == null ? 1 : (int)this.calcStat(Stat.MAX_MP, player.getTemplate().getBaseMpMax(player.getLevel()));
      if (val != this._oldMaxMp) {
         this._oldMaxMp = val;
         if (player != null) {
            PlayerStatus status = player.getStatus();
            if (status.getCurrentMp() != val) {
               status.setCurrentMp(status.getCurrentMp());
            }
         }
      }

      return val;
   }

   @Override
   public long getSp() {
      Player player = this.getActiveChar();
      return player.isSubClassActive() ? player.getSubClasses().get(player.getClassIndex()).getSp() : super.getSp();
   }

   public long getBaseSp() {
      return super.getSp();
   }

   @Override
   public void setSp(long value) {
      Player player = this.getActiveChar();
      if (player.isSubClassActive()) {
         player.getSubClasses().get(player.getClassIndex()).setSp(value);
      } else {
         super.setSp(value);
      }
   }

   @Override
   public double getBaseMoveSpeed(MoveType type) {
      Player player = this.getActiveChar();
      if (player.isTransformed()) {
         TransformTemplate template = player.getTransformation().getTemplate(player);
         if (template != null) {
            return template.getBaseMoveSpeed(type);
         }
      } else if (player.isMounted()) {
         PetLevelData data = PetDataTable.getInstance().getPetLevelData(player.getMountNpcId(), player.getMountLevel());
         if (data != null) {
            return data.getSpeedOnRide(type);
         }
      }

      return super.getBaseMoveSpeed(type);
   }

   @Override
   public double getRunSpeed() {
      double val = super.getRunSpeed() + PlayerConfig.RUN_SPD_BOOST;
      Player player = this.getActiveChar();
      if (val > PlayerConfig.MAX_RUN_SPEED && !player.isGM()) {
         return PlayerConfig.MAX_RUN_SPEED;
      } else {
         if (player.isMounted()) {
            if (player.getMountLevel() - player.getLevel() >= 10) {
               val /= 2.0;
            }

            if (player.isHungry()) {
               val /= 2.0;
            }
         }

         return val;
      }
   }

   @Override
   public double getWalkSpeed() {
      double val = super.getWalkSpeed() + PlayerConfig.RUN_SPD_BOOST;
      Player player = this.getActiveChar();
      if (val > PlayerConfig.MAX_RUN_SPEED && !player.isGM()) {
         return PlayerConfig.MAX_RUN_SPEED;
      } else {
         if (player.isMounted()) {
            if (player.getMountLevel() - player.getLevel() >= 10) {
               val /= 2.0;
            }

            if (player.isHungry()) {
               val /= 2.0;
            }
         }

         return val;
      }
   }

   @Override
   public double getPAtkSpd() {
      double val = super.getPAtkSpd();
      return val > PlayerConfig.MAX_PATK_SPEED && !this.getActiveChar().isGM() ? PlayerConfig.MAX_PATK_SPEED : val;
   }

   private void updateVitalityLevel(boolean quiet) {
      byte level;
      if (this._vitalityPoints <= VITALITY_LEVELS[0]) {
         level = 0;
      } else if (this._vitalityPoints <= VITALITY_LEVELS[1]) {
         level = 1;
      } else if (this._vitalityPoints <= VITALITY_LEVELS[2]) {
         level = 2;
      } else if (this._vitalityPoints <= VITALITY_LEVELS[3]) {
         level = 3;
      } else {
         level = 4;
      }

      if (!quiet && level != this._vitalityLevel) {
         Player player = this.getActiveChar();
         if (level < this._vitalityLevel) {
            player.sendPacket(SystemMessageId.YOUR_VITALITY_HAS_DECREASED);
         } else {
            player.sendPacket(SystemMessageId.YOUR_VITALITY_HAS_INCREASED);
         }

         if (level == 0) {
            player.sendPacket(SystemMessageId.YOUR_VITALITY_IS_FULLY_EXHAUSTED);
         } else if (level == 4) {
            player.sendPacket(SystemMessageId.YOUR_VITALITY_IS_AT_MAXIMUM);
         }
      }

      this._vitalityLevel = level;
   }

   public int getVitalityPoints() {
      return (int)this._vitalityPoints;
   }

   public void setVitalityPoints(int value, boolean quiet) {
      int points = Math.clamp((long)value, 1, MAX_VITALITY_POINTS);
      if (points != this._vitalityPoints) {
         this._vitalityPoints = points;
         this.updateVitalityLevel(quiet);
         this.getActiveChar().sendPacket(new ExVitalityPointInfo(this.getVitalityPoints()));
      }
   }

   public synchronized void updateVitalityPoints(float value, boolean useRates, boolean quiet) {
      if (value != 0.0F && PlayerConfig.ENABLE_VITALITY) {
         float points = value;
         if (useRates) {
            Player player = this.getActiveChar();
            if (player.isLucky()) {
               return;
            }

            if (value < 0.0F) {
               int stat = (int)this.calcStat(Stat.VITALITY_CONSUME_RATE, 1.0, player, null);
               if (stat == 0) {
                  return;
               }

               if (stat < 0) {
                  points = -value;
               }
            }

            if (points > 0.0F) {
               points *= RatesConfig.RATE_VITALITY_GAIN;
            } else {
               points *= RatesConfig.RATE_VITALITY_LOST;
            }
         }

         if (points > 0.0F) {
            points = Math.min(this._vitalityPoints + points, (float)MAX_VITALITY_POINTS);
         } else {
            points = Math.max(this._vitalityPoints + points, 1.0F);
         }

         if (!(Math.abs(points - this._vitalityPoints) <= 1.0E-6)) {
            this._vitalityPoints = points;
            this.updateVitalityLevel(quiet);
         }
      }
   }

   public double getVitalityMultiplier() {
      double vitality = 1.0;
      if (PlayerConfig.ENABLE_VITALITY) {
         switch (this.getVitalityLevel()) {
            case 1:
               vitality = RatesConfig.RATE_VITALITY_LEVEL_1;
               break;
            case 2:
               vitality = RatesConfig.RATE_VITALITY_LEVEL_2;
               break;
            case 3:
               vitality = RatesConfig.RATE_VITALITY_LEVEL_3;
               break;
            case 4:
               vitality = RatesConfig.RATE_VITALITY_LEVEL_4;
         }
      }

      return vitality;
   }

   public byte getVitalityLevel() {
      return this._vitalityLevel;
   }

   public double getExpBonusMultiplier() {
      double bonus = 1.0;
      double vitality = 1.0;
      double hunting = 1.0;
      double bonusExp = 1.0;
      vitality = this.getVitalityMultiplier();
      bonusExp = 1.0 + this.calcStat(Stat.BONUS_EXP, 0.0, null, null) / 100.0;
      if (vitality > 1.0) {
         bonus += vitality - 1.0;
      }

      if (hunting > 1.0) {
         bonus += hunting - 1.0;
      }

      if (bonusExp > 1.0) {
         bonus += bonusExp - 1.0;
      }

      bonus = Math.max(bonus, 1.0);
      if (PlayerConfig.MAX_BONUS_EXP > 0.0) {
         bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_EXP);
      }

      return bonus;
   }

   public double getSpBonusMultiplier() {
      double bonus = 1.0;
      double vitality = 1.0;
      double hunting = 1.0;
      double bonusSp = 1.0;
      vitality = this.getVitalityMultiplier();
      bonusSp = 1.0 + this.calcStat(Stat.BONUS_SP, 0.0, null, null) / 100.0;
      if (vitality > 1.0) {
         bonus += vitality - 1.0;
      }

      if (hunting > 1.0) {
         bonus += hunting - 1.0;
      }

      if (bonusSp > 1.0) {
         bonus += bonusSp - 1.0;
      }

      bonus = Math.max(bonus, 1.0);
      if (PlayerConfig.MAX_BONUS_SP > 0.0) {
         bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_SP);
      }

      return bonus;
   }

   public double getBonusDropAdenaMultiplier() {
      return 1.0 + this.calcStat(Stat.BONUS_DROP_ADENA, 0.0, null, null) / 100.0;
   }

   public double getBonusDropAmountMultiplier() {
      return 1.0 + this.calcStat(Stat.BONUS_DROP_AMOUNT, 0.0, null, null) / 100.0;
   }

   public double getBonusDropRateMultiplier() {
      return 1.0 + this.calcStat(Stat.BONUS_DROP_RATE, 0.0, null, null) / 100.0;
   }

   public double getBonusSpoilRateMultiplier() {
      return 1.0 + this.calcStat(Stat.BONUS_SPOIL_RATE, 0.0, null, null) / 100.0;
   }
}
