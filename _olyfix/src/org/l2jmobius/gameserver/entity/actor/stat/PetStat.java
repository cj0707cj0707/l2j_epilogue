package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PetStat extends SummonStat {
   public PetStat(Pet activeChar) {
      super(activeChar);
   }

   public boolean addExp(int value) {
      Pet pet = this.getActiveChar();
      if (!pet.isUncontrollable() && super.addExp(value)) {
         pet.updateAndBroadcastStatus(1);
         pet.updateEffectIcons(true);
         return true;
      } else {
         return false;
      }
   }

   public boolean addExpAndSp(double addToExp, double addToSp) {
      long finalExp = Math.round(addToExp);
      Pet pet = this.getActiveChar();
      if (!pet.isUncontrollable() && this.addExp(finalExp)) {
         SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_GAINED_S1_EXPERIENCE_POINTS);
         sm.addLong(finalExp);
         pet.updateAndBroadcastStatus(1);
         pet.sendPacket(sm);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addLevel(byte value) {
      if (this.getLevel() + value > this.getMaxLevel() - 1) {
         return false;
      } else {
         boolean levelIncreased = super.addLevel(value);
         Pet pet = this.getActiveChar();
         StatusUpdate su = new StatusUpdate(pet);
         su.addAttribute(1, this.getLevel());
         su.addAttribute(10, this.getMaxHp());
         su.addAttribute(12, this.getMaxMp());
         pet.broadcastPacket(su);
         if (levelIncreased) {
            pet.broadcastPacket(new SocialAction(pet.getObjectId(), 2122));
         }

         pet.updateAndBroadcastStatus(1);
         if (pet.getControlItem() != null) {
            pet.getControlItem().setEnchantLevel(this.getLevel());
         }

         return levelIncreased;
      }
   }

   @Override
   public long getExpForLevel(int level) {
      Pet pet = this.getActiveChar();

      try {
         return PetDataTable.getInstance().getPetLevelData(pet.getId(), level).getPetMaxExp();
      } catch (NullPointerException var4) {
         if (pet != null) {
            LOGGER.warning("Pet objectId:" + pet.getObjectId() + ", NpcId:" + pet.getId() + ", level:" + level + " is missing data from pets_stats table!");
         }

         throw var4;
      }
   }

   public Pet getActiveChar() {
      return super.getActiveChar().asPet();
   }

   public int getFeedBattle() {
      return this.getActiveChar().getPetLevelData().getPetFeedBattle();
   }

   public int getFeedNormal() {
      return this.getActiveChar().getPetLevelData().getPetFeedNormal();
   }

   @Override
   public void setLevel(byte value) {
      Pet pet = this.getActiveChar();
      pet.setPetData(PetDataTable.getInstance().getPetLevelData(pet.getTemplate().getId(), value));
      if (pet.getPetLevelData() == null) {
         throw new IllegalArgumentException("No pet data for npc: " + pet.getTemplate().getId() + " level: " + value);
      } else {
         pet.stopFeed();
         super.setLevel(value);
         pet.startFeed();
         if (pet.getControlItem() != null) {
            pet.getControlItem().setEnchantLevel(this.getLevel());
         }
      }
   }

   public int getMaxFeed() {
      return this.getActiveChar().getPetLevelData().getPetMaxFeed();
   }

   @Override
   public int getMaxHp() {
      return (int)this.calcStat(Stat.MAX_HP, this.getActiveChar().getPetLevelData().getPetMaxHP(), null, null);
   }

   @Override
   public int getMaxMp() {
      return (int)this.calcStat(Stat.MAX_MP, this.getActiveChar().getPetLevelData().getPetMaxMP(), null, null);
   }

   @Override
   public double getMAtk(Creature target, Skill skill) {
      return this.calcStat(Stat.MAGIC_ATTACK, this.getActiveChar().getPetLevelData().getPetMAtk(), target, skill);
   }

   @Override
   public double getMDef(Creature target, Skill skill) {
      return this.calcStat(Stat.MAGIC_DEFENCE, this.getActiveChar().getPetLevelData().getPetMDef(), target, skill);
   }

   @Override
   public double getPAtk(Creature target) {
      return this.calcStat(Stat.POWER_ATTACK, this.getActiveChar().getPetLevelData().getPetPAtk(), target, null);
   }

   @Override
   public double getPDef(Creature target) {
      if (target != null) {
         return target.isPlayable()
            ? ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[this.getActiveChar().asPlayer().getPlayerClass().getId()]
               * this.calcStat(Stat.POWER_DEFENCE, this.getActiveChar().getPetLevelData().getPetPDef(), target, null)
            : ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[this.getActiveChar().asPlayer().getPlayerClass().getId()]
               * this.calcStat(Stat.POWER_DEFENCE, this.getActiveChar().getPetLevelData().getPetPDef(), target, null);
      } else {
         return this.calcStat(Stat.POWER_DEFENCE, this.getActiveChar().getPetLevelData().getPetPDef(), target, null);
      }
   }

   @Override
   public double getPAtkSpd() {
      return this.getActiveChar().isHungry() ? super.getPAtkSpd() / 2.0 : super.getPAtkSpd();
   }

   @Override
   public int getMAtkSpd() {
      return this.getActiveChar().isHungry() ? super.getMAtkSpd() / 2 : super.getMAtkSpd();
   }

   @Override
   public int getMaxLevel() {
      return ExperienceData.getInstance().getMaxPetLevel();
   }
}
