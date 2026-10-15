package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.LoginServerThread.SessionKey;

public class AuthLogin extends ClientPacket {
   private String _loginName;
   private int _playKey1;
   private int _playKey2;
   private int _loginKey1;
   private int _loginKey2;

   @Override
   protected void readImpl() {
      this._loginName = this.readString().toLowerCase();
      this._playKey2 = this.readInt();
      this._playKey1 = this.readInt();
      this._loginKey1 = this.readInt();
      this._loginKey2 = this.readInt();
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.GameClient client = (org.l2jmobius.gameserver.network.GameClient)this.getClient();
      if (!this._loginName.isEmpty() && client.isProtocolOk()) {
         if (client.getAccountName() == null) {
            if (LoginServerThread.getInstance().addGameServerLogin(this._loginName, client)) {
               client.setAccountName(this._loginName);
               SessionKey key = new SessionKey(this._loginKey1, this._loginKey2, this._playKey1, this._playKey2);
               LoginServerThread.getInstance().addWaitingClientAndSendRequest(this._loginName, client, key);
            } else {
               client.close(null);
            }
         }
      } else {
         client.closeNow();
      }
   }
}
