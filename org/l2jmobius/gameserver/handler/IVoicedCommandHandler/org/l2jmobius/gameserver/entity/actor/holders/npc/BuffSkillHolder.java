package org.l2jmobius.gameserver.entity.actor.holders.npc;

public class BuffSkillHolder {
   private final int _id;
   private final int _level;
   private final int _price;
   private final String _type;
   private final String _description;

   public BuffSkillHolder(int id, int level, int price, String type, String description) {
      this._id = id;
      this._level = level;
      this._price = price;
      this._type = type;
      this._description = description;
   }

   public int getId() {
      return this._id;
   }

   public int getLevel() {
      return this._level;
   }

   public int getPrice() {
      return this._price;
   }

   public String getType() {
      return this._type;
   }

   public String getDescription() {
      return this._description;
   }
}
