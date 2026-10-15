package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Npc;

public class MonRaceInfo extends ServerPacket {
   private final int _unknown1;
   private final int _unknown2;
   private final Npc[] _monsters;
   private final int[][] _speeds;

   public MonRaceInfo(int unknown1, int unknown2, Npc[] monsters, int[][] speeds) {
      this._unknown1 = unknown1;
      this._unknown2 = unknown2;
      this._monsters = monsters;
      this._speeds = speeds;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.MON_RACE_INFO.writeId(this, buffer);
      buffer.writeInt(this._unknown1);
      buffer.writeInt(this._unknown2);
      buffer.writeInt(8);

      for (int i = 0; i < 8; i++) {
         buffer.writeInt(this._monsters[i].getObjectId());
         buffer.writeInt(this._monsters[i].getTemplate().getDisplayId() + 1000000);
         buffer.writeInt(14107);
         buffer.writeInt(181875 + 58 * (7 - i));
         buffer.writeInt(-3566);
         buffer.writeInt(12080);
         buffer.writeInt(181875 + 58 * (7 - i));
         buffer.writeInt(-3566);
         buffer.writeDouble(this._monsters[i].getTemplate().getCollisionHeight());
         buffer.writeDouble(this._monsters[i].getTemplate().getCollisionRadius());
         buffer.writeInt(120);

         for (int j = 0; j < 20; j++) {
            if (this._unknown1 == 0) {
               buffer.writeByte(this._speeds[i][j]);
            } else {
               buffer.writeByte(0);
            }
         }

         buffer.writeInt(0);
         buffer.writeInt(0);
      }
   }
}
