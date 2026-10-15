package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class StatusUpdate extends ServerPacket {
   public static final int LEVEL = 1;
   public static final int EXP = 2;
   public static final int STR = 3;
   public static final int DEX = 4;
   public static final int CON = 5;
   public static final int INT = 6;
   public static final int WIT = 7;
   public static final int MEN = 8;
   public static final int CUR_HP = 9;
   public static final int MAX_HP = 10;
   public static final int CUR_MP = 11;
   public static final int MAX_MP = 12;
   public static final int SP = 13;
   public static final int CUR_LOAD = 14;
   public static final int MAX_LOAD = 15;
   public static final int P_ATK = 17;
   public static final int ATK_SPD = 18;
   public static final int P_DEF = 19;
   public static final int EVASION = 20;
   public static final int ACCURACY = 21;
   public static final int CRITICAL = 22;
   public static final int M_ATK = 23;
   public static final int CAST_SPD = 24;
   public static final int M_DEF = 25;
   public static final int PVP_FLAG = 26;
   public static final int KARMA = 27;
   public static final int CUR_CP = 33;
   public static final int MAX_CP = 34;
   private final int _objectId;
   private final List<StatusUpdate.Attribute> _attributes = new ArrayList<>();

   public StatusUpdate(int objectId) {
      this._objectId = objectId;
   }

   public StatusUpdate(WorldObject object) {
      this._objectId = object.getObjectId();
   }

   public void addAttribute(int id, int level) {
      this._attributes.add(new StatusUpdate.Attribute(id, level));
   }

   public boolean hasAttributes() {
      return !this._attributes.isEmpty();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.STATUS_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._attributes.size());

      for (StatusUpdate.Attribute temp : this._attributes) {
         buffer.writeInt(temp.id);
         buffer.writeInt(temp.value);
      }
   }

   public boolean canBeDropped(GameClient client) {
      return true;
   }

   private static class Attribute {
      public int id;
      public int value;

      Attribute(int pId, int pValue) {
         this.id = pId;
         this.value = pValue;
      }
   }
}
