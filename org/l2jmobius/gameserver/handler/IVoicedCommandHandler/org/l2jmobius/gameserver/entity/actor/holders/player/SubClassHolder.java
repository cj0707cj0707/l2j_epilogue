package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

public class SubClassHolder {
   private static final byte MAX_LEVEL = PlayerConfig.MAX_SUBCLASS_LEVEL < ExperienceData.getInstance().getMaxLevel()
      ? PlayerConfig.MAX_SUBCLASS_LEVEL
      : (byte)(ExperienceData.getInstance().getMaxLevel() - 1);
   private PlayerClass _playerClass;
   private long _exp = ExperienceData.getInstance().getExpForLevel(PlayerConfig.BASE_SUBCLASS_LEVEL);
   private long _sp = 0L;
   private byte _level = PlayerConfig.BASE_SUBCLASS_LEVEL;
   private int _classIndex = 1;

   public SubClassHolder(int classId, long exp, int sp, byte level, int classIndex) {
      this._playerClass = PlayerClass.getPlayerClass(classId);
      this._exp = exp;
      this._sp = sp;
      this._level = level;
      this._classIndex = classIndex;
   }

   public SubClassHolder(int classId, int classIndex) {
      this._playerClass = PlayerClass.getPlayerClass(classId);
      this._classIndex = classIndex;
   }

   public SubClassHolder() {
   }

   public PlayerClass getPlayerClass() {
      return this._playerClass;
   }

   public int getId() {
      return this._playerClass.getId();
   }

   public long getExp() {
      return this._exp;
   }

   public long getSp() {
      return this._sp;
   }

   public byte getLevel() {
      return this._level;
   }

   public int getClassIndex() {
      return this._classIndex;
   }

   public void setPlayerClass(int id) {
      this._playerClass = PlayerClass.getPlayerClass(id);
   }

   public void setExp(long expValue) {
      if (expValue > ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1L) {
         this._exp = ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1L;
      } else {
         this._exp = expValue;
      }
   }

   public void setSp(long spValue) {
      this._sp = spValue;
   }

   public void setClassIndex(int classIndex) {
      this._classIndex = classIndex;
   }

   public void setLevel(byte levelValue) {
      if (levelValue > MAX_LEVEL) {
         this._level = MAX_LEVEL;
      } else if (levelValue < PlayerConfig.BASE_SUBCLASS_LEVEL) {
         this._level = PlayerConfig.BASE_SUBCLASS_LEVEL;
      } else {
         this._level = levelValue;
      }
   }

   public void incLevel() {
      if (this._level != MAX_LEVEL) {
         this._level++;
         this.setExp(ExperienceData.getInstance().getExpForLevel(this.getLevel()));
      }
   }

   public void decLevel() {
      if (this._level != PlayerConfig.BASE_SUBCLASS_LEVEL) {
         this._level--;
         this.setExp(ExperienceData.getInstance().getExpForLevel(this.getLevel()));
      }
   }
}
