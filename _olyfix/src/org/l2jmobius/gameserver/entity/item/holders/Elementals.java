package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.data.xml.ElementalAttributeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd;

public class Elementals {
   public static final byte NONE = -1;
   public static final byte FIRE = 0;
   public static final byte WATER = 1;
   public static final byte WIND = 2;
   public static final byte EARTH = 3;
   public static final byte HOLY = 4;
   public static final byte DARK = 5;
   public static final int FIRST_WEAPON_BONUS = 20;
   public static final int NEXT_WEAPON_BONUS = 5;
   public static final int ARMOR_BONUS = 6;
   public static final int[] WEAPON_VALUES = new int[]{0, 25, 75, 150, 175, 225, 300, 325, 375, 450, 475, 525, 600, Integer.MAX_VALUE};
   public static final int[] ARMOR_VALUES = new int[]{0, 12, 30, 60, 72, 90, 120, 132, 150, 180, 192, 210, 240, Integer.MAX_VALUE};
   private Elementals.ElementalStatBonus _bonus = null;
   private byte _element = -1;
   private int _value = 0;

   public static byte getItemElement(int itemId) {
      ElementalItemHolder item = ElementalAttributeData.getInstance().getElementalItem(itemId);
      return item != null ? item.getElementId() : -1;
   }

   public static ElementalItemHolder getItemElemental(int itemId) {
      return ElementalAttributeData.getInstance().getElementalItem(itemId);
   }

   public static int getMaxElementLevel(int itemId) {
      ElementalItemHolder item = ElementalAttributeData.getInstance().getElementalItem(itemId);
      return item != null ? item.getType().getMaxLevel() : -1;
   }

   public static String getElementName(byte element) {
      switch (element) {
         case 0:
            return "Fire";
         case 1:
            return "Water";
         case 2:
            return "Wind";
         case 3:
            return "Earth";
         case 4:
            return "Holy";
         case 5:
            return "Dark";
         default:
            return "None";
      }
   }

   public static byte getElementId(String name) {
      String tmp = name.toLowerCase();
      if (tmp.equals("fire")) {
         return 0;
      } else if (tmp.equals("water")) {
         return 1;
      } else if (tmp.equals("wind")) {
         return 2;
      } else if (tmp.equals("earth")) {
         return 3;
      } else if (tmp.equals("dark")) {
         return 5;
      } else {
         return (byte)(tmp.equals("holy") ? 4 : -1);
      }
   }

   public static byte getOppositeElement(byte element) {
      return (byte)(element % 2 == 0 ? element + 1 : element - 1);
   }

   public byte getElement() {
      return this._element;
   }

   public void setElement(byte type) {
      this._element = type;
      this._bonus.setElement(type);
   }

   public int getValue() {
      return this._value;
   }

   public void setValue(int value) {
      this._value = value;
      this._bonus.setValue(value);
   }

   @Override
   public String toString() {
      return getElementName(this._element) + " +" + this._value;
   }

   public Elementals(byte type, int value) {
      this._element = type;
      this._value = value;
      this._bonus = new Elementals.ElementalStatBonus(this._element, this._value);
   }

   public void applyBonus(Player player, boolean isArmor) {
      this._bonus.applyBonus(player, isArmor);
   }

   public void removeBonus(Player player) {
      this._bonus.removeBonus(player);
   }

   public void updateBonus(Player player, boolean isArmor) {
      this._bonus.removeBonus(player);
      this._bonus.applyBonus(player, isArmor);
   }

   public static class ElementalStatBonus {
      private byte _elementalType;
      private int _elementalValue;
      private boolean _active;

      public ElementalStatBonus(byte type, int value) {
         this._elementalType = type;
         this._elementalValue = value;
         this._active = false;
      }

      public void applyBonus(Player player, boolean isArmor) {
         if (!this._active) {
            switch (this._elementalType) {
               case 0:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.FIRE_RES : Stat.FIRE_POWER, 64, this, this._elementalValue, null));
                  break;
               case 1:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.WATER_RES : Stat.WATER_POWER, 64, this, this._elementalValue, null));
                  break;
               case 2:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.WIND_RES : Stat.WIND_POWER, 64, this, this._elementalValue, null));
                  break;
               case 3:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.EARTH_RES : Stat.EARTH_POWER, 64, this, this._elementalValue, null));
                  break;
               case 4:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.HOLY_RES : Stat.HOLY_POWER, 64, this, this._elementalValue, null));
                  break;
               case 5:
                  player.addStatFunc(new FuncAdd(isArmor ? Stat.DARK_RES : Stat.DARK_POWER, 64, this, this._elementalValue, null));
            }

            this._active = true;
         }
      }

      public void removeBonus(Player player) {
         if (this._active) {
            player.removeStatsOwner(this);
            this._active = false;
         }
      }

      public void setValue(int value) {
         this._elementalValue = value;
      }

      public void setElement(byte type) {
         this._elementalType = type;
      }
   }
}
