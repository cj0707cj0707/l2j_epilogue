package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPVPMatchRecord extends ServerPacket {
   public static final int START = 0;
   public static final int UPDATE = 1;
   public static final int FINISH = 2;
   private final int _type;
   private final int _winnerTeam;
   private final int _blueKills;
   private final int _redKills;
   private final List<ExPVPMatchRecord.Member> _blueList;
   private final List<ExPVPMatchRecord.Member> _redList;

   public ExPVPMatchRecord(int type, int winnerTeam, UCArena arena) {
      this._type = type;
      this._winnerTeam = winnerTeam;
      UCTeam blueTeam = arena.getTeams()[0];
      this._blueKills = blueTeam.getKillCount();
      UCTeam redTeam = arena.getTeams()[1];
      this._redKills = redTeam.getKillCount();
      this._blueList = new ArrayList<>(9);
      if (blueTeam.getParty() != null) {
         for (Player memberObject : blueTeam.getParty().getMembers()) {
            if (memberObject != null) {
               this._blueList.add(new ExPVPMatchRecord.Member(memberObject.getName(), memberObject.getUCKills(), memberObject.getUCDeaths()));
            }
         }
      }

      this._redList = new ArrayList<>(9);
      if (redTeam.getParty() != null) {
         for (Player memberObjectx : redTeam.getParty().getMembers()) {
            if (memberObjectx != null) {
               this._redList.add(new ExPVPMatchRecord.Member(memberObjectx.getName(), memberObjectx.getUCKills(), memberObjectx.getUCDeaths()));
            }
         }
      }
   }

   public ExPVPMatchRecord(int type, int winnerTeam) {
      this._type = type;
      this._winnerTeam = winnerTeam;
      this._blueKills = 0;
      this._redKills = 0;
      this._blueList = new ArrayList<>(9);
      this._redList = new ArrayList<>(9);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PVP_MATCH_RECORD.writeId(this, buffer);
      buffer.writeInt(this._type);
      buffer.writeInt(this._winnerTeam);
      buffer.writeInt(this._winnerTeam == 0 ? 0 : (this._winnerTeam == 1 ? 2 : 1));
      buffer.writeInt(this._blueKills);
      buffer.writeInt(this._redKills);
      buffer.writeInt(this._blueList.size());

      for (ExPVPMatchRecord.Member member : this._blueList) {
         buffer.writeString(member._name);
         buffer.writeInt(member._kills);
         buffer.writeInt(member._deaths);
      }

      buffer.writeInt(this._redList.size());

      for (ExPVPMatchRecord.Member member : this._redList) {
         buffer.writeString(member._name);
         buffer.writeInt(member._kills);
         buffer.writeInt(member._deaths);
      }
   }

   public static class Member {
      public String _name;
      public int _kills;
      public int _deaths;

      public Member(String name, int kills, int deaths) {
         this._name = name;
         this._kills = kills;
         this._deaths = deaths;
      }
   }
}
