package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;
import org.l2jmobius.gameserver.entity.residences.ClanHall.ClanHallFunction;

public class AgitDecoInfo extends ServerPacket {
   private final AuctionableHall _clanHall;

   public AgitDecoInfo(AuctionableHall clanHall) {
      this._clanHall = clanHall;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.AGIT_DECO_INFO.writeId(this, buffer);
      buffer.writeInt(this._clanHall.getId());
      ClanHallFunction function = this._clanHall.getFunction(3);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 || function.getLevel() >= 220)
            && (this._clanHall.getGrade() != 1 || function.getLevel() >= 160)
            && (this._clanHall.getGrade() != 2 || function.getLevel() >= 260)
            && (this._clanHall.getGrade() != 3 || function.getLevel() >= 300)) {
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(4);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 && this._clanHall.getGrade() != 1 || function.getLevel() >= 25)
            && (this._clanHall.getGrade() != 2 || function.getLevel() >= 30)
            && (this._clanHall.getGrade() != 3 || function.getLevel() >= 40)) {
            buffer.writeByte(2);
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(5);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 || function.getLevel() >= 25)
            && (this._clanHall.getGrade() != 1 || function.getLevel() >= 30)
            && (this._clanHall.getGrade() != 2 || function.getLevel() >= 40)
            && (this._clanHall.getGrade() != 3 || function.getLevel() >= 50)) {
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(1);
      if (function != null && function.getLevel() != 0) {
         if (function.getLevel() < 2) {
            buffer.writeByte(1);
         } else {
            buffer.writeByte(2);
         }
      } else {
         buffer.writeByte(0);
      }

      buffer.writeByte(0);
      function = this._clanHall.getFunction(8);
      if (function != null && function.getLevel() != 0) {
         if (function.getLevel() <= 1) {
            buffer.writeByte(1);
         } else {
            buffer.writeByte(2);
         }
      } else {
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(2);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 || function.getLevel() >= 2) && function.getLevel() >= 3) {
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(6);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 || function.getLevel() >= 2)
            && (this._clanHall.getGrade() != 1 || function.getLevel() >= 4)
            && (this._clanHall.getGrade() != 2 || function.getLevel() >= 5)
            && (this._clanHall.getGrade() != 3 || function.getLevel() >= 8)) {
            buffer.writeByte(2);
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(7);
      if (function != null && function.getLevel() != 0) {
         if (function.getLevel() <= 1) {
            buffer.writeByte(1);
         } else {
            buffer.writeByte(2);
         }
      } else {
         buffer.writeByte(0);
      }

      function = this._clanHall.getFunction(2);
      if (function != null && function.getLevel() != 0) {
         if ((this._clanHall.getGrade() != 0 || function.getLevel() >= 2) && function.getLevel() >= 3) {
            buffer.writeByte(2);
         } else {
            buffer.writeByte(1);
         }
      } else {
         buffer.writeByte(0);
      }
   }
}
