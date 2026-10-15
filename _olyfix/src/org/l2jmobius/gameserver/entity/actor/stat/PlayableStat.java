package org.l2jmobius.gameserver.entity.actor.stat;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.SwampZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.playable.OnPlayableExpChanged;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;

public class PlayableStat extends CreatureStat {
   protected static final Logger LOGGER = Logger.getLogger(PlayableStat.class.getName());

   public PlayableStat(Playable activeChar) {
      super(activeChar);
   }

   public boolean addExp(long amount) {
      Playable playable = this.getActiveChar();
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYABLE_EXP_CHANGED, playable)) {
         TerminateReturn term = EventDispatcher.getInstance()
            .notifyEvent(new OnPlayableExpChanged(playable, this.getExp(), this.getExp() + amount), playable, TerminateReturn.class);
         if (term != null && term.terminate()) {
            return false;
         }
      }

      if (this.getExp() + amount >= 0L && (amount <= 0L || this.getExp() != this.getExpForLevel(this.getMaxLevel()) - 1L)) {
         long value = amount;
         if (this.getExp() + amount >= this.getExpForLevel(this.getMaxLevel())) {
            value = this.getExpForLevel(this.getMaxLevel()) - 1L - this.getExp();
         }

         this.setExp(this.getExp() + value);
         byte minimumLevel = 1;
         if (playable.isPet()) {
            minimumLevel = (byte)PetDataTable.getInstance().getPetMinLevel(playable.asPet().getTemplate().getId());
         }

         byte level = minimumLevel;

         for (byte tmp = minimumLevel; tmp <= this.getMaxLevel(); tmp++) {
            if (this.getExp() < this.getExpForLevel(tmp)) {
               level = --tmp;
               break;
            }
         }

         if (level != this.getLevel() && level >= minimumLevel) {
            this.addLevel((byte)(level - this.getLevel()));
         }

         return true;
      } else {
         return true;
      }
   }

   public boolean removeExp(long amount) {
      long value = amount;
      if (this.getExp() - amount < 0L) {
         value = this.getExp() - 1L;
      }

      this.setExp(this.getExp() - value);
      byte minimumLevel = 1;
      Playable playable = this.getActiveChar();
      if (playable.isPet()) {
         minimumLevel = (byte)PetDataTable.getInstance().getPetMinLevel(playable.asPet().getTemplate().getId());
      }

      byte level = minimumLevel;

      for (byte tmp = minimumLevel; tmp <= this.getMaxLevel(); tmp++) {
         if (this.getExp() < this.getExpForLevel(tmp)) {
            level = --tmp;
            break;
         }
      }

      if (level != this.getLevel() && level >= minimumLevel) {
         this.addLevel((byte)(level - this.getLevel()));
      }

      return true;
   }

   public boolean removeExpAndSp(long removeExp, long removeSp) {
      boolean expRemoved = false;
      boolean spRemoved = false;
      if (removeExp > 0L) {
         expRemoved = this.removeExp(removeExp);
      }

      if (removeSp > 0L) {
         spRemoved = this.removeSp(removeSp);
      }

      return expRemoved || spRemoved;
   }

   public boolean addLevel(byte amount) {
      byte value = amount;
      if (this.getLevel() + amount > this.getMaxLevel() - 1) {
         if (this.getLevel() >= this.getMaxLevel() - 1) {
            return false;
         }

         value = (byte)(this.getMaxLevel() - 1 - this.getLevel());
      }

      boolean levelIncreased = this.getLevel() + value > this.getLevel();
      value = (byte)(value + this.getLevel());
      this.setLevel(value);
      if (this.getExp() >= this.getExpForLevel(this.getLevel() + 1) || this.getExpForLevel(this.getLevel()) > this.getExp()) {
         this.setExp(this.getExpForLevel(this.getLevel()));
      }

      Playable playable = this.getActiveChar();
      if (!levelIncreased && playable.isPlayer() && !playable.isGM() && PlayerConfig.DECREASE_SKILL_LEVEL) {
         playable.asPlayer().checkPlayerSkills();
      }

      if (!levelIncreased) {
         return false;
      } else {
         playable.getStatus().setCurrentHp(playable.getStat().getMaxHp());
         playable.getStatus().setCurrentMp(playable.getStat().getMaxMp());
         return true;
      }
   }

   public boolean addSp(long amount) {
      if (amount < 0L) {
         LOGGER.warning("wrong usage");
         return false;
      } else {
         long currentSp = this.getSp();
         if (currentSp >= PlayerConfig.MAX_SP) {
            return false;
         } else {
            long value = amount;
            if (currentSp > PlayerConfig.MAX_SP - amount) {
               value = PlayerConfig.MAX_SP - currentSp;
            }

            this.setSp(currentSp + value);
            return true;
         }
      }
   }

   public boolean removeSp(long amount) {
      long currentSp = this.getSp();
      if (currentSp < amount) {
         this.setSp(this.getSp() - currentSp);
         return true;
      } else {
         this.setSp(this.getSp() - amount);
         return true;
      }
   }

   public long getExpForLevel(int level) {
      return ExperienceData.getInstance().getExpForLevel(level);
   }

   @Override
   public double getRunSpeed() {
      Playable playable = this.getActiveChar();
      if (playable.isInsideZone(ZoneId.SWAMP)) {
         SwampZone zone = ZoneManager.getInstance().getZone(playable, SwampZone.class);
         if (zone != null) {
            return super.getRunSpeed() * zone.getMoveBonus();
         }
      }

      return super.getRunSpeed();
   }

   @Override
   public double getWalkSpeed() {
      Playable playable = this.getActiveChar();
      if (playable.isInsideZone(ZoneId.SWAMP)) {
         SwampZone zone = ZoneManager.getInstance().getZone(playable, SwampZone.class);
         if (zone != null) {
            return super.getWalkSpeed() * zone.getMoveBonus();
         }
      }

      return super.getWalkSpeed();
   }

   public Playable getActiveChar() {
      return super.getActiveChar().asPlayable();
   }

   public int getMaxLevel() {
      return ExperienceData.getInstance().getMaxLevel();
   }

   @Override
   public int getPhysicalAttackAngle() {
      Weapon weapon = this.getActiveChar().getActiveWeaponItem();
      return weapon != null ? weapon.getBaseAttackAngle() : super.getPhysicalAttackAngle();
   }
}
