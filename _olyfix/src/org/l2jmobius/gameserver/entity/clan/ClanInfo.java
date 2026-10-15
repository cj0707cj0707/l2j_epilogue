package org.l2jmobius.gameserver.entity.clan;

public class ClanInfo {
   private final Clan _clan;
   private final int _total;
   private final int _online;

   public ClanInfo(Clan clan) {
      this._clan = clan;
      this._total = clan.getMembersCount();
      this._online = clan.getOnlineMembersCount();
   }

   public Clan getClan() {
      return this._clan;
   }

   public int getTotal() {
      return this._total;
   }

   public int getOnline() {
      return this._online;
   }
}
