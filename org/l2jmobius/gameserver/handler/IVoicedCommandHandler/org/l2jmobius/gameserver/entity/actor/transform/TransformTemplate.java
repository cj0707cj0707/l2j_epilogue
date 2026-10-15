package org.l2jmobius.gameserver.entity.actor.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.util.StatSet;

public class TransformTemplate {
   private final float _collisionRadius;
   private final float _collisionHeight;
   private final WeaponType _baseAttackType;
   private final int _baseAttackRange;
   private final double _baseRandomDamage;
   private List<SkillHolder> _skills;
   private List<AdditionalSkillHolder> _additionalSkills;
   private List<AdditionalItemHolder> _additionalItems;
   private Map<Integer, Integer> _baseDefense;
   private Map<Integer, Double> _baseStats;
   private Map<Integer, Double> _baseSpeed;
   private ExBasicActionList _list;
   private final Map<Integer, TransformLevelData> _data = new LinkedHashMap<>(100);

   public TransformTemplate(StatSet set) {
      this._collisionRadius = set.getFloat("radius", 0.0F);
      this._collisionHeight = set.getFloat("height", 0.0F);
      this._baseAttackType = set.getEnum("attackType", WeaponType.class, WeaponType.FIST);
      this._baseAttackRange = set.getInt("range", 40);
      this._baseRandomDamage = set.getDouble("randomDamage", 0.0);
      this.addSpeed(MoveType.WALK, set.getDouble("walk", 0.0));
      this.addSpeed(MoveType.RUN, set.getDouble("run", 0.0));
      this.addSpeed(MoveType.SLOW_SWIM, set.getDouble("waterWalk", 0.0));
      this.addSpeed(MoveType.FAST_SWIM, set.getDouble("waterRun", 0.0));
      this.addSpeed(MoveType.SLOW_FLY, set.getDouble("flyWalk", 0.0));
      this.addSpeed(MoveType.FAST_FLY, set.getDouble("flyRun", 0.0));
      this.addStats(Stat.POWER_ATTACK, set.getDouble("pAtk", 0.0));
      this.addStats(Stat.MAGIC_ATTACK, set.getDouble("mAtk", 0.0));
      this.addStats(Stat.POWER_ATTACK_RANGE, set.getInt("range", 0));
      this.addStats(Stat.POWER_ATTACK_SPEED, set.getInt("attackSpeed", 0));
      this.addStats(Stat.CRITICAL_RATE, set.getInt("critRate", 0));
      this.addStats(Stat.STAT_STR, set.getInt("str", 0));
      this.addStats(Stat.STAT_INT, set.getInt("int", 0));
      this.addStats(Stat.STAT_CON, set.getInt("con", 0));
      this.addStats(Stat.STAT_DEX, set.getInt("dex", 0));
      this.addStats(Stat.STAT_WIT, set.getInt("wit", 0));
      this.addStats(Stat.STAT_MEN, set.getInt("men", 0));
      this.addDefense(6, set.getInt("chest", 0));
      this.addDefense(11, set.getInt("legs", 0));
      this.addDefense(1, set.getInt("head", 0));
      this.addDefense(12, set.getInt("feet", 0));
      this.addDefense(10, set.getInt("gloves", 0));
      this.addDefense(0, set.getInt("underwear", 0));
      this.addDefense(23, set.getInt("cloak", 0));
      this.addDefense(8, set.getInt("rear", 0));
      this.addDefense(9, set.getInt("lear", 0));
      this.addDefense(13, set.getInt("rfinger", 0));
      this.addDefense(14, set.getInt("lfinger", 0));
      this.addDefense(4, set.getInt("neck", 0));
   }

   private void addSpeed(MoveType type, double value) {
      if (this._baseSpeed == null) {
         this._baseSpeed = new HashMap<>();
      }

      this._baseSpeed.put(type.ordinal(), value);
   }

   public double getBaseMoveSpeed(MoveType type) {
      return this._baseSpeed != null && this._baseSpeed.containsKey(type.ordinal()) ? this._baseSpeed.get(type.ordinal()) : 0.0;
   }

   private void addDefense(int type, int value) {
      if (this._baseDefense == null) {
         this._baseDefense = new HashMap<>();
      }

      this._baseDefense.put(type, value);
   }

   public int getDefense(int type) {
      return this._baseDefense != null && this._baseDefense.containsKey(type) ? this._baseDefense.get(type) : 0;
   }

   private void addStats(Stat stat, double value) {
      if (this._baseStats == null) {
         this._baseStats = new HashMap<>();
      }

      this._baseStats.put(stat.ordinal(), value);
   }

   public double getStats(Stat stat) {
      return this._baseStats != null && this._baseStats.containsKey(stat.ordinal()) ? this._baseStats.get(stat.ordinal()) : 0.0;
   }

   public float getCollisionRadius() {
      return this._collisionRadius;
   }

   public float getCollisionHeight() {
      return this._collisionHeight;
   }

   public WeaponType getBaseAttackType() {
      return this._baseAttackType;
   }

   public int getBaseAttackRange() {
      return this._baseAttackRange;
   }

   public double getBaseRandomDamage() {
      return this._baseRandomDamage;
   }

   public void addSkill(SkillHolder holder) {
      if (this._skills == null) {
         this._skills = new ArrayList<>();
      }

      this._skills.add(holder);
   }

   public List<SkillHolder> getSkills() {
      return this._skills != null ? this._skills : Collections.emptyList();
   }

   public void addAdditionalSkill(AdditionalSkillHolder holder) {
      if (this._additionalSkills == null) {
         this._additionalSkills = new ArrayList<>();
      }

      this._additionalSkills.add(holder);
   }

   public List<AdditionalSkillHolder> getAdditionalSkills() {
      return this._additionalSkills != null ? this._additionalSkills : Collections.emptyList();
   }

   public void addAdditionalItem(AdditionalItemHolder holder) {
      if (this._additionalItems == null) {
         this._additionalItems = new ArrayList<>();
      }

      this._additionalItems.add(holder);
   }

   public List<AdditionalItemHolder> getAdditionalItems() {
      return this._additionalItems != null ? this._additionalItems : Collections.emptyList();
   }

   public void setBasicActionList(ExBasicActionList list) {
      this._list = list;
   }

   public ExBasicActionList getBasicActionList() {
      return this._list;
   }

   public boolean hasBasicActionList() {
      return this._list != null;
   }

   public void addLevelData(TransformLevelData data) {
      this._data.put(data.getLevel(), data);
   }

   public TransformLevelData getData(int level) {
      return this._data.get(level);
   }
}
