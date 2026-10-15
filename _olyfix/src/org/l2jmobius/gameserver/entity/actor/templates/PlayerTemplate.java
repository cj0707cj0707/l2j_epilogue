package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.util.StatSet;

public class PlayerTemplate extends CreatureTemplate {
   private final PlayerClass _playerClass;
   private final float[] _baseHp;
   private final float[] _baseMp;
   private final float[] _baseCp;
   private final double[] _baseHpReg;
   private final double[] _baseMpReg;
   private final double[] _baseCpReg;
   private final float _fCollisionHeightFemale;
   private final float _fCollisionRadiusFemale;
   private final int _baseSafeFallHeight;
   private final List<Location> _creationPoints;
   private final Map<Integer, Integer> _baseSlotDef;

   public PlayerTemplate(StatSet set, List<Location> creationPoints) {
      super(set);
      this._playerClass = PlayerClass.getPlayerClass(set.getInt("classId"));
      this.setRace(this._playerClass.getRace());
      this._baseHp = new float[ExperienceData.getInstance().getMaxLevel()];
      this._baseMp = new float[ExperienceData.getInstance().getMaxLevel()];
      this._baseCp = new float[ExperienceData.getInstance().getMaxLevel()];
      this._baseHpReg = new double[ExperienceData.getInstance().getMaxLevel()];
      this._baseMpReg = new double[ExperienceData.getInstance().getMaxLevel()];
      this._baseCpReg = new double[ExperienceData.getInstance().getMaxLevel()];
      this._baseSlotDef = new HashMap<>(12);
      this._baseSlotDef.put(6, set.getInt("basePDefchest", 0));
      this._baseSlotDef.put(11, set.getInt("basePDeflegs", 0));
      this._baseSlotDef.put(1, set.getInt("basePDefhead", 0));
      this._baseSlotDef.put(12, set.getInt("basePDeffeet", 0));
      this._baseSlotDef.put(10, set.getInt("basePDefgloves", 0));
      this._baseSlotDef.put(0, set.getInt("basePDefunderwear", 0));
      this._baseSlotDef.put(23, set.getInt("basePDefcloak", 0));
      this._baseSlotDef.put(8, set.getInt("baseMDefrear", 0));
      this._baseSlotDef.put(9, set.getInt("baseMDeflear", 0));
      this._baseSlotDef.put(13, set.getInt("baseMDefrfinger", 0));
      this._baseSlotDef.put(14, set.getInt("baseMDefrfinger", 0));
      this._baseSlotDef.put(4, set.getInt("baseMDefneck", 0));
      this._fCollisionRadiusFemale = set.getFloat("collisionFemaleradius");
      this._fCollisionHeightFemale = set.getFloat("collisionFemaleheight");
      this._baseSafeFallHeight = set.getInt("baseSafeFall", 333);
      this._creationPoints = creationPoints;
   }

   public PlayerClass getPlayerClass() {
      return this._playerClass;
   }

   public Location getCreationPoint() {
      return this._creationPoints.get(Rnd.get(this._creationPoints.size()));
   }

   public void setUpgainValue(String paramName, int level, double value) {
      switch (paramName) {
         case "hp":
            this._baseHp[level] = (float)value;
            break;
         case "mp":
            this._baseMp[level] = (float)value;
            break;
         case "cp":
            this._baseCp[level] = (float)value;
            break;
         case "hpRegen":
            this._baseHpReg[level] = value;
            break;
         case "mpRegen":
            this._baseMpReg[level] = value;
            break;
         case "cpRegen":
            this._baseCpReg[level] = value;
      }
   }

   public float getBaseHpMax(int level) {
      return this._baseHp[level];
   }

   public float getBaseMpMax(int level) {
      return this._baseMp[level];
   }

   public float getBaseCpMax(int level) {
      return this._baseCp[level];
   }

   public double getBaseHpRegen(int level) {
      return this._baseHpReg[level];
   }

   public double getBaseMpRegen(int level) {
      return this._baseMpReg[level];
   }

   public double getBaseCpRegen(int level) {
      return this._baseCpReg[level];
   }

   public int getBaseDefBySlot(int slotId) {
      return this._baseSlotDef.getOrDefault(slotId, 0);
   }

   public float getFCollisionHeightFemale() {
      return this._fCollisionHeightFemale;
   }

   public float getFCollisionRadiusFemale() {
      return this._fCollisionRadiusFemale;
   }

   public int getSafeFallHeight() {
      return this._baseSafeFallHeight;
   }
}
