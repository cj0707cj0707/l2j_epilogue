package org.l2jmobius.gameserver.entity.groups.matching;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;

public class PartyMatchRoomList {
   private int _maxid = 1;
   private final Map<Integer, PartyMatchRoom> _rooms = new ConcurrentHashMap<>();

   protected PartyMatchRoomList() {
   }

   public synchronized void addPartyMatchRoom(int id, PartyMatchRoom room) {
      this._rooms.put(id, room);
      this._maxid++;
   }

   public void deleteRoom(int id) {
      for (Player _member : this.getRoom(id).getPartyMembers()) {
         if (_member != null) {
            _member.sendPacket(new ExClosePartyRoom());
            _member.sendPacket(SystemMessageId.THE_PARTY_ROOM_HAS_BEEN_DISBANDED);
            _member.setPartyRoom(0);
            _member.broadcastUserInfo();
         }
      }

      this._rooms.remove(id);
   }

   public PartyMatchRoom getRoom(int id) {
      return this._rooms.get(id);
   }

   public Collection<PartyMatchRoom> getRooms() {
      return this._rooms.values();
   }

   public int getPartyMatchRoomCount() {
      return this._rooms.size();
   }

   public int getMaxId() {
      return this._maxid;
   }

   public PartyMatchRoom getPlayerRoom(Player player) {
      for (PartyMatchRoom _room : this._rooms.values()) {
         for (Player member : _room.getPartyMembers()) {
            if (member.equals(player)) {
               return _room;
            }
         }
      }

      return null;
   }

   public int getPlayerRoomId(Player player) {
      for (PartyMatchRoom _room : this._rooms.values()) {
         for (Player member : _room.getPartyMembers()) {
            if (member.equals(player)) {
               return _room.getId();
            }
         }
      }

      return -1;
   }

   public static PartyMatchRoomList getInstance() {
      return PartyMatchRoomList.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PartyMatchRoomList INSTANCE = new PartyMatchRoomList();
   }
}
