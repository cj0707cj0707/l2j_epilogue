package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.network.serverpackets.ExBrLoadEventTopRankers;

public class BrEventRankerList extends ClientPacket {
   private int _eventId;
   private int _day;
   private int _ranking;

   @Override
   protected void readImpl() {
      this._eventId = this.readInt();
      this._day = this.readInt();
      this._ranking = this.readInt();
   }

   @Override
   protected void runImpl() {
      int count = 0;
      int bestScore = 0;
      int myScore = 0;
      this.getClient().sendPacket(new ExBrLoadEventTopRankers(this._eventId, this._day, 0, 0, 0));
   }
}
