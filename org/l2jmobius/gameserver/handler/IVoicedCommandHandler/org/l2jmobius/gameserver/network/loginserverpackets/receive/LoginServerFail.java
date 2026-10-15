package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

public class LoginServerFail extends SimpleReadablePacket {
   private static final String[] REASONS = new String[]{
      "None",
      "Reason: ip banned",
      "Reason: ip reserved",
      "Reason: wrong hexid",
      "Reason: id reserved",
      "Reason: no free ID",
      "Not authed",
      "Reason: already logged in"
   };
   private final int _reason;

   public LoginServerFail(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      this._reason = this.readByte();
   }

   public String getReasonString() {
      return REASONS[this._reason];
   }

   public int getReason() {
      return this._reason;
   }
}
