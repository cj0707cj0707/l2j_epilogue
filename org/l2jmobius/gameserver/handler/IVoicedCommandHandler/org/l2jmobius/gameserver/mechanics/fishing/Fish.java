package org.l2jmobius.gameserver.mechanics.fishing;

import org.l2jmobius.gameserver.util.StatSet;

public class Fish implements Cloneable {
   private final int _fishId;
   private final int _itemId;
   private final String _itemName;
   private int _fishGroup;
   private final int _fishLevel;
   private final double _fishBiteRate;
   private final double _fishGuts;
   private final int _fishHp;
   private final int _fishMaxLength;
   private final double _fishLengthRate;
   private final double _hpRegen;
   private final int _startCombatTime;
   private final int _combatDuration;
   private final int _gutsCheckTime;
   private final double _gutsCheckProbability;
   private final double _cheatingProb;
   private final int _fishGrade;

   public Fish(StatSet set) {
      this._fishId = set.getInt("fishId");
      this._itemId = set.getInt("itemId");
      this._itemName = set.getString("itemName");
      this._fishGroup = this.getGroupId(set.getString("fishGroup"));
      this._fishLevel = set.getInt("fishLevel");
      this._fishBiteRate = set.getDouble("fishBiteRate");
      this._fishGuts = set.getDouble("fishGuts");
      this._fishHp = set.getInt("fishHp");
      this._fishMaxLength = set.getInt("fishMaxLength");
      this._fishLengthRate = set.getDouble("fishLengthRate");
      this._hpRegen = set.getDouble("hpRegen");
      this._startCombatTime = set.getInt("startCombatTime");
      this._combatDuration = set.getInt("combatDuration");
      this._gutsCheckTime = set.getInt("gutsCheckTime");
      this._gutsCheckProbability = set.getDouble("gutsCheckProbability");
      this._cheatingProb = set.getDouble("cheatingProb");
      this._fishGrade = this.getGradeId(set.getString("fishGrade"));
   }

   public Fish clone() {
      try {
         return (Fish)super.clone();
      } catch (CloneNotSupportedException var2) {
         return null;
      }
   }

   public int getFishId() {
      return this._fishId;
   }

   public int getItemId() {
      return this._itemId;
   }

   public String getItemName() {
      return this._itemName;
   }

   public int getFishGroup() {
      return this._fishGroup;
   }

   public int getFishLevel() {
      return this._fishLevel;
   }

   public double getFishBiteRate() {
      return this._fishBiteRate;
   }

   public double getFishGuts() {
      return this._fishGuts;
   }

   public int getFishHp() {
      return this._fishHp;
   }

   public int getFishMaxLength() {
      return this._fishMaxLength;
   }

   public double getFishLengthRate() {
      return this._fishLengthRate;
   }

   public double getHpRegen() {
      return this._hpRegen;
   }

   public int getStartCombatTime() {
      return this._startCombatTime;
   }

   public int getCombatDuration() {
      return this._combatDuration;
   }

   public int getGutsCheckTime() {
      return this._gutsCheckTime;
   }

   public double getGutsCheckProbability() {
      return this._gutsCheckProbability;
   }

   public double getCheatingProb() {
      return this._cheatingProb;
   }

   public int getFishGrade() {
      return this._fishGrade;
   }

   public void setFishGroup(int fg) {
      this._fishGroup = fg;
   }

   private int getGroupId(String name) {
      switch (name) {
         case "swift":
            return 1;
         case "ugly":
            return 2;
         case "fish_box":
            return 3;
         case "easy_wide":
            return 4;
         case "easy_swift":
            return 5;
         case "easy_ugly":
            return 6;
         case "hard_wide":
            return 7;
         case "hard_swift":
            return 8;
         case "hard_ugly":
            return 9;
         case "hs_fish":
            return 10;
         case "wide":
         default:
            return 0;
      }
   }

   private int getGradeId(String name) {
      switch (name) {
         case "fish_easy":
            return 0;
         case "fish_hard":
            return 2;
         case "fish_normal":
         default:
            return 1;
      }
   }
}
