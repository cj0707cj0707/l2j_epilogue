package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.util.StatSet;

public class CreatureTemplate extends ListenersContainer {
   private int _baseSTR;
   private int _baseCON;
   private int _baseDEX;
   private int _baseINT;
   private int _baseWIT;
   private int _baseMEN;
   protected float _baseHpMax;
   private float _baseCpMax;
   protected float _baseMpMax;
   private float _baseHpReg;
   private float _baseMpReg;
   protected int _basePAtk;
   protected int _baseMAtk;
   protected int _basePDef;
   protected int _baseMDef;
   private int _basePAtkSpd;
   private int _baseMAtkSpd;
   private int _baseAttackRange;
   private int _randomDamage;
   private WeaponType _baseAttackType;
   private int _baseShldDef;
   private int _baseShldRate;
   private int _baseCritRate;
   private int _baseMCritRate;
   private int _baseBreath;
   private int _baseFire;
   private int _baseWind;
   private int _baseWater;
   private int _baseEarth;
   private int _baseHoly;
   private int _baseDark;
   private double _baseFireRes;
   private double _baseWindRes;
   private double _baseWaterRes;
   private double _baseEarthRes;
   private double _baseHolyRes;
   private double _baseDarkRes;
   private double _baseElementRes;
   private int _collisionRadius;
   private int _collisionHeight;
   private float _fCollisionRadius;
   private float _fCollisionHeight;
   private final double[] _moveType = new double[MoveType.values().length];
   private Race _race;

   public CreatureTemplate(StatSet set) {
      this.set(set);
   }

   public void set(StatSet set) {
      this._baseSTR = set.getInt("baseSTR", 0);
      this._baseCON = set.getInt("baseCON", 0);
      this._baseDEX = set.getInt("baseDEX", 0);
      this._baseINT = set.getInt("baseINT", 0);
      this._baseWIT = set.getInt("baseWIT", 0);
      this._baseMEN = set.getInt("baseMEN", 0);
      this._baseHpMax = set.getFloat("baseHpMax", 0.0F);
      this._baseCpMax = set.getFloat("baseCpMax", 0.0F);
      this._baseMpMax = set.getFloat("baseMpMax", 0.0F);
      this._baseHpReg = set.getFloat("baseHpReg", 0.0F);
      this._baseMpReg = set.getFloat("baseMpReg", 0.0F);
      this._basePAtk = set.getInt("basePAtk", 0);
      this._baseMAtk = set.getInt("baseMAtk", 0);
      this._basePDef = set.getInt("basePDef", 0);
      this._baseMDef = set.getInt("baseMDef", 0);
      this._basePAtkSpd = set.getInt("basePAtkSpd", 300);
      this._baseMAtkSpd = set.getInt("baseMAtkSpd", 333);
      this._baseShldDef = set.getInt("baseShldDef", 0);
      this._baseAttackRange = set.getInt("baseAtkRange", 40);
      this._randomDamage = set.getInt("baseRndDam", 0);
      this._baseAttackType = set.getEnum("baseAtkType", WeaponType.class, WeaponType.FIST);
      this._baseShldRate = set.getInt("baseShldRate", 0);
      this._baseCritRate = set.getInt("baseCritRate", 4);
      this._baseMCritRate = set.getInt("baseMCritRate", 5);
      this._baseBreath = set.getInt("baseBreath", 100);
      this._baseFire = set.getInt("baseFire", 0);
      this._baseWind = set.getInt("baseWind", 0);
      this._baseWater = set.getInt("baseWater", 0);
      this._baseEarth = set.getInt("baseEarth", 0);
      this._baseHoly = set.getInt("baseHoly", 0);
      this._baseDark = set.getInt("baseDark", 0);
      this._baseFireRes = set.getInt("baseFireRes", 0);
      this._baseWindRes = set.getInt("baseWindRes", 0);
      this._baseWaterRes = set.getInt("baseWaterRes", 0);
      this._baseEarthRes = set.getInt("baseEarthRes", 0);
      this._baseHolyRes = set.getInt("baseHolyRes", 0);
      this._baseDarkRes = set.getInt("baseDarkRes", 0);
      this._baseElementRes = set.getInt("baseElementRes", 0);
      this._fCollisionHeight = set.getFloat("collisionHeight", 0.0F);
      this._fCollisionRadius = set.getFloat("collisionRadius", 0.0F);
      this._collisionRadius = (int)this._fCollisionRadius;
      this._collisionHeight = (int)this._fCollisionHeight;
      Arrays.fill(this._moveType, 1.0);
      this.setBaseMoveSpeed(MoveType.RUN, set.getDouble("baseRunSpd", 120.0));
      this.setBaseMoveSpeed(MoveType.WALK, set.getDouble("baseWalkSpd", 50.0));
      this.setBaseMoveSpeed(MoveType.FAST_SWIM, set.getDouble("baseSwimRunSpd", this.getBaseMoveSpeed(MoveType.RUN)));
      this.setBaseMoveSpeed(MoveType.SLOW_SWIM, set.getDouble("baseSwimWalkSpd", this.getBaseMoveSpeed(MoveType.WALK)));
      this.setBaseMoveSpeed(MoveType.FAST_FLY, set.getDouble("baseFlyRunSpd", this.getBaseMoveSpeed(MoveType.RUN)));
      this.setBaseMoveSpeed(MoveType.SLOW_FLY, set.getDouble("baseFlyWalkSpd", this.getBaseMoveSpeed(MoveType.WALK)));
   }

   public float getBaseHpMax() {
      return this._baseHpMax;
   }

   public int getBaseFire() {
      return this._baseFire;
   }

   public int getBaseWind() {
      return this._baseWind;
   }

   public int getBaseWater() {
      return this._baseWater;
   }

   public int getBaseEarth() {
      return this._baseEarth;
   }

   public int getBaseHoly() {
      return this._baseHoly;
   }

   public int getBaseDark() {
      return this._baseDark;
   }

   public double getBaseFireRes() {
      return this._baseFireRes;
   }

   public double getBaseWindRes() {
      return this._baseWindRes;
   }

   public double getBaseWaterRes() {
      return this._baseWaterRes;
   }

   public double getBaseEarthRes() {
      return this._baseEarthRes;
   }

   public double getBaseHolyRes() {
      return this._baseHolyRes;
   }

   public double getBaseDarkRes() {
      return this._baseDarkRes;
   }

   public double getBaseElementRes() {
      return this._baseElementRes;
   }

   public int getBaseSTR() {
      return this._baseSTR;
   }

   public int getBaseCON() {
      return this._baseCON;
   }

   public int getBaseDEX() {
      return this._baseDEX;
   }

   public int getBaseINT() {
      return this._baseINT;
   }

   public int getBaseWIT() {
      return this._baseWIT;
   }

   public int getBaseMEN() {
      return this._baseMEN;
   }

   public float getBaseCpMax() {
      return this._baseCpMax;
   }

   public float getBaseMpMax() {
      return this._baseMpMax;
   }

   public float getBaseHpReg() {
      return this._baseHpReg;
   }

   public float getBaseMpReg() {
      return this._baseMpReg;
   }

   public int getBasePAtk() {
      return this._basePAtk;
   }

   public int getBaseMAtk() {
      return this._baseMAtk;
   }

   public int getBasePDef() {
      return this._basePDef;
   }

   public int getBaseMDef() {
      return this._baseMDef;
   }

   public int getBasePAtkSpd() {
      return this._basePAtkSpd;
   }

   public int getBaseMAtkSpd() {
      return this._baseMAtkSpd;
   }

   public int getRandomDamage() {
      return this._randomDamage;
   }

   public int getBaseShldDef() {
      return this._baseShldDef;
   }

   public int getBaseShldRate() {
      return this._baseShldRate;
   }

   public int getBaseCritRate() {
      return this._baseCritRate;
   }

   public int getBaseMCritRate() {
      return this._baseMCritRate;
   }

   public void setBaseMoveSpeed(MoveType type, double value) {
      this._moveType[type.ordinal()] = value;
   }

   public double getBaseMoveSpeed(MoveType mt) {
      return this._moveType[mt.ordinal()];
   }

   public int getBaseBreath() {
      return this._baseBreath;
   }

   public int getCollisionRadius() {
      return this._collisionRadius;
   }

   public int getCollisionHeight() {
      return this._collisionHeight;
   }

   public float getFCollisionRadius() {
      return this._fCollisionRadius;
   }

   public float getFCollisionHeight() {
      return this._fCollisionHeight;
   }

   public void setBaseFire(int baseFire) {
      this._baseFire = baseFire;
   }

   public void setBaseWater(int baseWater) {
      this._baseWater = baseWater;
   }

   public void setBaseEarth(int baseEarth) {
      this._baseEarth = baseEarth;
   }

   public void setBaseWind(int baseWind) {
      this._baseWind = baseWind;
   }

   public void setBaseHoly(int baseHoly) {
      this._baseHoly = baseHoly;
   }

   public void setBaseDark(int baseDark) {
      this._baseDark = baseDark;
   }

   public void setBaseFireRes(double baseFireRes) {
      this._baseFireRes = baseFireRes;
   }

   public void setBaseWaterRes(double baseWaterRes) {
      this._baseWaterRes = baseWaterRes;
   }

   public void setBaseEarthRes(double baseEarthRes) {
      this._baseEarthRes = baseEarthRes;
   }

   public void setBaseWindRes(double baseWindRes) {
      this._baseWindRes = baseWindRes;
   }

   public void setBaseHolyRes(double baseHolyRes) {
      this._baseHolyRes = baseHolyRes;
   }

   public void setBaseDarkRes(double baseDarkRes) {
      this._baseDarkRes = baseDarkRes;
   }

   public void setBaseElementRes(double baseElementRes) {
      this._baseElementRes = baseElementRes;
   }

   public WeaponType getBaseAttackType() {
      return this._baseAttackType;
   }

   public void setBaseAttackType(WeaponType type) {
      this._baseAttackType = type;
   }

   public int getBaseAttackRange() {
      return this._baseAttackRange;
   }

   public void setBaseAttackRange(int value) {
      this._baseAttackRange = value;
   }

   public Map<Integer, Skill> getSkills() {
      return Collections.emptyMap();
   }

   public Race getRace() {
      return this._race;
   }

   public void setRace(Race race) {
      this._race = race;
   }
}
