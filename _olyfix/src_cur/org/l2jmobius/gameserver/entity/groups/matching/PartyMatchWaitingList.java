package org.l2jmobius.gameserver.entity.groups.matching;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Player;

public class PartyMatchWaitingList {
   private final Collection<Player> _members = ConcurrentHashMap.newKeySet();

   protected PartyMatchWaitingList() {
   }

   public void addPlayer(Player player) {
      if (!this._members.contains(player)) {
         this._members.add(player);
      }
   }

   public void removePlayer(Player player) {
      this._members.remove(player);
   }

   public Collection<Player> getPlayers() {
      return this._members;
   }

   public static PartyMatchWaitingList getInstance() {
      return PartyMatchWaitingList.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PartyMatchWaitingList INSTANCE = new PartyMatchWaitingList();
   }
}
