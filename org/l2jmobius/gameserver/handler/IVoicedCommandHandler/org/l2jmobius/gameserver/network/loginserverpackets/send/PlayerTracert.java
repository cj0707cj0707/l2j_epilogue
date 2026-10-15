package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class PlayerTracert extends SimpleWritablePacket {
   private static final int OPCODE = 7;
   private final String _account;
   private final String _pcIp;
   private final String _hop1;
   private final String _hop2;
   private final String _hop3;
   private final String _hop4;

   public PlayerTracert(String account, String pcIp, String hop1, String hop2, String hop3, String hop4) {
      this._account = account != null ? account : "";
      this._pcIp = pcIp != null ? pcIp : "";
      this._hop1 = hop1 != null ? hop1 : "";
      this._hop2 = hop2 != null ? hop2 : "";
      this._hop3 = hop3 != null ? hop3 : "";
      this._hop4 = hop4 != null ? hop4 : "";
   }

   @Override
   public void write() {
      this.writeByte(7);
      this.writeString(this._account);
      this.writeString(this._pcIp);
      this.writeString(this._hop1);
      this.writeString(this._hop2);
      this.writeString(this._hop3);
      this.writeString(this._hop4);
   }
}
