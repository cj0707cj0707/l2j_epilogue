package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPartner {
   private static final int REQUEST_TIMEOUT = 15;
   protected Player _player;
   protected Player _partner;
   protected boolean _isRequestor;
   protected boolean _isAnswerer;
   protected ClientPacket _requestPacket;

   public RequestPartner(Player player) {
      this._player = player;
   }

   protected void clear() {
      this._partner = null;
      this._requestPacket = null;
      this._isRequestor = false;
      this._isAnswerer = false;
   }

   private synchronized void setPartner(Player partner) {
      this._partner = partner;
   }

   public Player getPartner() {
      return this._partner;
   }

   private synchronized void setRequestPacket(ClientPacket packet) {
      this._requestPacket = packet;
   }

   public ClientPacket getRequestPacket() {
      return this._requestPacket;
   }

   public synchronized boolean setRequest(Player partner, ClientPacket packet) {
      if (partner == null) {
         this._player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
         return false;
      } else if (partner.getRequest().isProcessingRequest()) {
         SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
         sm.addString(partner.getName());
         this._player.sendPacket(sm);
         return false;
      } else if (this.isProcessingRequest()) {
         this._player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
         return false;
      } else {
         this._partner = partner;
         this._requestPacket = packet;
         this.setOnRequestTimer(true);
         this._partner.getRequest().setPartner(this._player);
         this._partner.getRequest().setRequestPacket(packet);
         this._partner.getRequest().setOnRequestTimer(false);
         return true;
      }
   }

   private void setOnRequestTimer(boolean isRequestor) {
      this._isRequestor = isRequestor;
      this._isAnswerer = !isRequestor;
      ThreadPool.schedule(this::clear, 15000L);
   }

   public void onRequestResponse() {
      if (this._partner != null) {
         this._partner.getRequest().clear();
      }

      this.clear();
   }

   public boolean isProcessingRequest() {
      return this._partner != null;
   }
}
