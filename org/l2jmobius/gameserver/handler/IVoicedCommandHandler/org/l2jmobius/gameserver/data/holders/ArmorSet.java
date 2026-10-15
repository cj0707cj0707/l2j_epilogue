package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public class ArmorSet {
   private int _chestId;
   private final List<Integer> _legs = new ArrayList<>();
   private final List<Integer> _head = new ArrayList<>();
   private final List<Integer> _gloves = new ArrayList<>();
   private final List<Integer> _feet = new ArrayList<>();
   private final List<Integer> _shield = new ArrayList<>();
   private final List<SkillHolder> _skills = new ArrayList<>();
   private final List<SkillHolder> _shieldSkills = new ArrayList<>();
   private final List<SkillHolder> _enchant6Skill = new ArrayList<>();
   private int _con;
   private int _dex;
   private int _str;
   private int _men;
   private int _wit;
   private int _int;

   public void addChest(int id) {
      this._chestId = id;
   }

   public void addLegs(int id) {
      this._legs.add(id);
   }

   public void addHead(int id) {
      this._head.add(id);
   }

   public void addGloves(int id) {
      this._gloves.add(id);
   }

   public void addFeet(int id) {
      this._feet.add(id);
   }

   public void addShield(int id) {
      this._shield.add(id);
   }

   public void addSkill(SkillHolder holder) {
      this._skills.add(holder);
   }

   public void addShieldSkill(SkillHolder holder) {
      this._shieldSkills.add(holder);
   }

   public void addEnchant6Skill(SkillHolder holder) {
      this._enchant6Skill.add(holder);
   }

   public void addCon(int value) {
      this._con = value;
   }

   public void addDex(int value) {
      this._dex = value;
   }

   public void addStr(int value) {
      this._str = value;
   }

   public void addMen(int value) {
      this._men = value;
   }

   public void addWit(int value) {
      this._wit = value;
   }

   public void addInt(int value) {
      this._int = value;
   }

   public boolean containAll(Player player) {
      Inventory inv = player.getInventory();
      Item legsItem = inv.getPaperdollItem(11);
      Item headItem = inv.getPaperdollItem(1);
      Item glovesItem = inv.getPaperdollItem(10);
      Item feetItem = inv.getPaperdollItem(12);
      int legs = 0;
      int head = 0;
      int gloves = 0;
      int feet = 0;
      if (legsItem != null) {
         legs = legsItem.getId();
      }

      if (headItem != null) {
         head = headItem.getId();
      }

      if (glovesItem != null) {
         gloves = glovesItem.getId();
      }

      if (feetItem != null) {
         feet = feetItem.getId();
      }

      return this.containAll(this._chestId, legs, head, gloves, feet);
   }

   public boolean containAll(int chest, int legs, int head, int gloves, int feet) {
      if (this._chestId != 0 && this._chestId != chest) {
         return false;
      } else if (!this._legs.isEmpty() && !this._legs.contains(legs)) {
         return false;
      } else if (!this._head.isEmpty() && !this._head.contains(head)) {
         return false;
      } else {
         return !this._gloves.isEmpty() && !this._gloves.contains(gloves) ? false : this._feet.isEmpty() || this._feet.contains(feet);
      }
   }

   public boolean containItem(int slot, int itemId) {
      switch (slot) {
         case 1:
            return this._head.contains(itemId);
         case 2:
         case 3:
         case 4:
         case 5:
         case 7:
         case 8:
         case 9:
         default:
            return false;
         case 6:
            return this._chestId == itemId;
         case 10:
            return this._gloves.contains(itemId);
         case 11:
            return this._legs.contains(itemId);
         case 12:
            return this._feet.contains(itemId);
      }
   }

   public int getChestId() {
      return this._chestId;
   }

   public List<SkillHolder> getSkills() {
      return this._skills;
   }

   public boolean containShield(Player player) {
      Inventory inv = player.getInventory();
      Item shieldItem = inv.getPaperdollItem(7);
      return shieldItem != null && this._shield.contains(shieldItem.getId());
   }

   public boolean containShield(int shieldId) {
      return this._shield.isEmpty() ? false : this._shield.contains(shieldId);
   }

   public List<SkillHolder> getShieldSkillId() {
      return this._shieldSkills;
   }

   public List<SkillHolder> getEnchant6skillId() {
      return this._enchant6Skill;
   }

   public boolean isEnchanted6(Player player) {
      if (!this.containAll(player)) {
         return false;
      } else {
         Inventory inv = player.getInventory();
         Item chestItem = inv.getPaperdollItem(6);
         if (chestItem != null && chestItem.getEnchantLevel() >= 6) {
            Item legsItem = inv.getPaperdollItem(11);
            if (this._legs.isEmpty() || legsItem != null && legsItem.getEnchantLevel() >= 6) {
               Item glovesItem = inv.getPaperdollItem(10);
               if (this._gloves.isEmpty() || glovesItem != null && glovesItem.getEnchantLevel() >= 6) {
                  Item headItem = inv.getPaperdollItem(1);
                  if (this._head.isEmpty() || headItem != null && headItem.getEnchantLevel() >= 6) {
                     Item feetItem = inv.getPaperdollItem(12);
                     return this._feet.isEmpty() || feetItem != null && feetItem.getEnchantLevel() >= 6;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public int getCON() {
      return this._con;
   }

   public int getDEX() {
      return this._dex;
   }

   public int getSTR() {
      return this._str;
   }

   public int getMEN() {
      return this._men;
   }

   public int getWIT() {
      return this._wit;
   }

   public int getINT() {
      return this._int;
   }
}
