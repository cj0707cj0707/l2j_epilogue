package org.l2jmobius.gameserver.mechanics.skill.holders;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

public class EnchantSkillGroup {
   private final int _id;
   private final List<EnchantSkillGroup.EnchantSkillHolder> _enchantDetails = new ArrayList<>();

   public EnchantSkillGroup(int id) {
      this._id = id;
   }

   public void addEnchantDetail(EnchantSkillGroup.EnchantSkillHolder detail) {
      this._enchantDetails.add(detail);
   }

   public int getId() {
      return this._id;
   }

   public List<EnchantSkillGroup.EnchantSkillHolder> getEnchantGroupDetails() {
      return this._enchantDetails;
   }

   public static class EnchantSkillHolder {
      private final int _level;
      private final int _adenaCost;
      private final int _expCost;
      private final int _spCost;
      private final byte[] _rate;

      public EnchantSkillHolder(StatSet set) {
         this._level = set.getInt("level");
         this._adenaCost = set.getInt("adena", 0);
         this._expCost = set.getInt("exp", 0);
         this._spCost = set.getInt("sp", 0);
         this._rate = new byte[24];

         for (int i = 0; i < 24; i++) {
            this._rate[i] = set.getByte("chance" + (76 + i), (byte)0);
         }
      }

      public int getLevel() {
         return this._level;
      }

      public int getSpCost() {
         return this._spCost;
      }

      public int getExpCost() {
         return this._expCost;
      }

      public int getAdenaCost() {
         return this._adenaCost;
      }

      public byte getRate(Player ply) {
         return ply.getLevel() < 76 ? 0 : this._rate[ply.getLevel() - 76];
      }
   }
}
