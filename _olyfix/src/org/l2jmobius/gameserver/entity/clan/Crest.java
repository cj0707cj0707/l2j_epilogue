package org.l2jmobius.gameserver.entity.clan;

import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

public class Crest {
   private final int _id;
   private final byte[] _data;
   private final CrestType _type;

   public Crest(int id, byte[] data, CrestType type) {
      this._id = id;
      this._data = data;
      this._type = type;
   }

   public int getId() {
      return this._id;
   }

   public byte[] getData() {
      return this._data;
   }

   public CrestType getType() {
      return this._type;
   }
}
