package org.l2jmobius.gameserver.entity.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExMPCCPartyInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class CommandChannel extends AbstractPlayerGroup {
   private final Collection<Party> _parties = ConcurrentHashMap.newKeySet();
   private Player _commandLeader;
   private int _channelLvl;

   public CommandChannel(Player leader) {
      this._commandLeader = leader;
      Party party = leader.getParty();
      this._parties.add(party);
      this._channelLvl = party.getLevel();
      party.setCommandChannel(this);
      party.broadcastMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
      party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
   }

   public void addParty(Party party) {
      if (party != null) {
         this.broadcastPacket(new ExMPCCPartyInfoUpdate(party, 1));
         this._parties.add(party);
         if (party.getLevel() > this._channelLvl) {
            this._channelLvl = party.getLevel();
         }

         party.setCommandChannel(this);
         party.broadcastPacket(new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL));
         party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
      }
   }

   public void removeParty(Party party) {
      if (party != null) {
         this._parties.remove(party);
         this._channelLvl = 0;

         for (Party pty : this._parties) {
            if (pty.getLevel() > this._channelLvl) {
               this._channelLvl = pty.getLevel();
            }
         }

         party.setCommandChannel(null);
         party.broadcastPacket(new ExCloseMPCC());
         if (this._parties.size() < 2) {
            this.broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_DISBANDED));
            this.disbandChannel();
         } else {
            this.broadcastPacket(new ExMPCCPartyInfoUpdate(party, 0));
         }
      }
   }

   public void disbandChannel() {
      if (this._parties != null) {
         for (Party party : this._parties) {
            if (party != null) {
               this.removeParty(party);
            }
         }

         this._parties.clear();
      }
   }

   @Override
   public int getMemberCount() {
      int count = 0;

      for (Party party : this._parties) {
         if (party != null) {
            count += party.getMemberCount();
         }
      }

      return count;
   }

   public Collection<Party> getParties() {
      return this._parties;
   }

   @Override
   public List<Player> getMembers() {
      List<Player> members = new ArrayList<>();

      for (Party party : this._parties) {
         members.addAll(party.getMembers());
      }

      return members;
   }

   @Override
   public int getLevel() {
      return this._channelLvl;
   }

   @Override
   public void setLeader(Player leader) {
      this._commandLeader = leader;
      if (leader.getLevel() > this._channelLvl) {
         this._channelLvl = leader.getLevel();
      }
   }

   public boolean meetRaidWarCondition(WorldObject obj) {
      return obj.isCreature() && obj.asCreature().isRaid() ? this.getMemberCount() >= PlayerConfig.LOOT_RAIDS_PRIVILEGE_CC_SIZE : false;
   }

   @Override
   public Player getLeader() {
      return this._commandLeader;
   }

   @Override
   public boolean containsPlayer(Player player) {
      if (this._parties != null && !this._parties.isEmpty()) {
         for (Party party : this._parties) {
            if (party.containsPlayer(player)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean forEachMember(Function<Player, Boolean> function) {
      if (this._parties != null && !this._parties.isEmpty()) {
         for (Party party : this._parties) {
            if (!party.forEachMember(function)) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean equals(CommandChannel cc) {
      return cc != null && this.getLeaderObjectId() == cc.getLeaderObjectId();
   }
}
