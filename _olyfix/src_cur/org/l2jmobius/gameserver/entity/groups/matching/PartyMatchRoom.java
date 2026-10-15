package org.l2jmobius.gameserver.entity.groups.matching;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PartyMatchRoom {
   private final int _id;
   private String _title;
   private int _loot;
   private int _minLevel;
   private int _maxLevel;
   private int _maxmem;
   private final List<Player> _members = new ArrayList<>();

   public PartyMatchRoom(int id, String title, int loot, int minLevel, int maxLevel, int maxmem, Player owner) {
      this._id = id;
      this._title = title;
      this._loot = loot;
      this._minLevel = minLevel;
      this._maxLevel = maxLevel;
      this._maxmem = maxmem;
      this._members.add(owner);
   }

   public List<Player> getPartyMembers() {
      return this._members;
   }

   public void addMember(Player player) {
      this._members.add(player);
   }

   public void deleteMember(Player player) {
      if (player != this.getOwner()) {
         this._members.remove(player);
         this.notifyMembersAboutExit(player);
      } else if (this._members.size() == 1) {
         PartyMatchRoomList.getInstance().deleteRoom(this._id);
      } else {
         this.changeLeader(this._members.get(1));
         this.deleteMember(player);
      }
   }

   public void notifyMembersAboutExit(Player player) {
      for (Player _member : this._members) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LEFT_THE_PARTY_ROOM);
         sm.addString(player.getName());
         _member.sendPacket(sm);
         _member.sendPacket(new ExManagePartyRoomMember(player, this, 2));
      }
   }

   public void changeLeader(Player newLeader) {
      Player oldLeader = this._members.get(0);
      this._members.remove(newLeader);
      this._members.set(0, newLeader);
      this._members.add(oldLeader);

      for (Player member : this._members) {
         member.sendPacket(new ExManagePartyRoomMember(newLeader, this, 1));
         member.sendPacket(new ExManagePartyRoomMember(oldLeader, this, 1));
         member.sendPacket(SystemMessageId.THE_LEADER_OF_THE_PARTY_ROOM_HAS_CHANGED);
      }
   }

   public int getId() {
      return this._id;
   }

   public int getLootType() {
      return this._loot;
   }

   public int getMinLevel() {
      return this._minLevel;
   }

   public int getMaxLevel() {
      return this._maxLevel;
   }

   public int getLocation() {
      return MapRegionData.getInstance().getBBs(this._members.get(0));
   }

   public int getMembers() {
      return this._members.size();
   }

   public int getMaxMembers() {
      return this._maxmem;
   }

   public String getTitle() {
      return this._title;
   }

   public Player getOwner() {
      return this._members.get(0);
   }

   public void setMinLevel(int minLevel) {
      this._minLevel = minLevel;
   }

   public void setMaxLevel(int maxLevel) {
      this._maxLevel = maxLevel;
   }

   public void setLootType(int loot) {
      this._loot = loot;
   }

   public void setMaxMembers(int maxmem) {
      this._maxmem = maxmem;
   }

   public void setTitle(String title) {
      this._title = title;
   }
}
