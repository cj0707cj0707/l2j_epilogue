package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class RelationChanged extends ServerPacket {
   public static final int RELATION_PARTY1 = 1;
   public static final int RELATION_PARTY2 = 2;
   public static final int RELATION_PARTY3 = 4;
   public static final int RELATION_PARTY4 = 8;
   public static final int RELATION_PARTYLEADER = 16;
   public static final int RELATION_HAS_PARTY = 32;
   public static final int RELATION_CLAN_MEMBER = 64;
   public static final int RELATION_LEADER = 128;
   public static final int RELATION_CLAN_MATE = 256;
   public static final int RELATION_INSIEGE = 512;
   public static final int RELATION_ATTACKER = 1024;
   public static final int RELATION_ALLY = 2048;
   public static final int RELATION_ENEMY = 4096;
   public static final int RELATION_MUTUAL_WAR = 16384;
   public static final int RELATION_1SIDED_WAR = 32768;
   public static final int RELATION_ALLY_MEMBER = 65536;
   public static final int RELATION_TERRITORY_WAR = 524288;
   private RelationChanged.Relation _singled;
   private List<RelationChanged.Relation> _multi;

   public RelationChanged(Playable activeChar, int relation, boolean autoattackable) {
      this._singled = new RelationChanged.Relation();
      this._singled._objId = activeChar.getObjectId();
      this._singled._relation = relation;
      this._singled._autoAttackable = autoattackable;
      this._singled._karma = activeChar.getKarma();
      this._singled._pvpFlag = activeChar.getPvpFlag();
   }

   public RelationChanged() {
      this._multi = new ArrayList<>();
   }

   public void addRelation(Playable activeChar, int relation, boolean autoattackable) {
      if (!activeChar.isInvisible()) {
         RelationChanged.Relation r = new RelationChanged.Relation();
         r._objId = activeChar.getObjectId();
         r._relation = relation;
         r._autoAttackable = autoattackable;
         r._karma = activeChar.getKarma();
         r._pvpFlag = activeChar.getPvpFlag();
         this._multi.add(r);
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.RELATION_CHANGED.writeId(this, buffer);
      if (this._multi == null) {
         buffer.writeInt(1);
         this.writeRelation(this._singled, buffer);
      } else {
         buffer.writeInt(this._multi.size());

         for (RelationChanged.Relation relation : this._multi) {
            this.writeRelation(relation, buffer);
         }
      }
   }

   private void writeRelation(RelationChanged.Relation relation, WriteBuffer buffer) {
      buffer.writeInt(relation._objId);
      buffer.writeInt(relation._relation);
      buffer.writeInt(relation._autoAttackable);
      buffer.writeInt(relation._karma);
      buffer.writeInt(relation._pvpFlag);
   }

   protected static class Relation {
      int _objId;
      int _relation;
      boolean _autoAttackable;
      int _karma;
      int _pvpFlag;
   }
}
