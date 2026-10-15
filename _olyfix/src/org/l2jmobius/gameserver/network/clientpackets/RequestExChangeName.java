package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.network.PacketLogger;

public class RequestExChangeName extends ClientPacket {
   private String _newName;
   private int _type;
   private int _charSlot;

   @Override
   protected void readImpl() {
      this._type = this.readInt();
      this._newName = this.readString();
      this._charSlot = this.readInt();
   }

   @Override
   protected void runImpl() {
      PacketLogger.info("Received " + this.getClass().getSimpleName() + " name: " + this._newName + " type: " + this._type + " CharSlot: " + this._charSlot);
   }
}
