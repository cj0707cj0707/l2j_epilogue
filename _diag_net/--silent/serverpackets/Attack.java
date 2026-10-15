package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.holders.creature.Hit;

public class Attack extends ServerPacket {
   private final int _attackerObjId;
   private final boolean _soulshot;
   private final int _ssGrade;
   private final Location _attackerLoc;
   private final Location _targetLoc;
   private final List<Hit> _hits = new ArrayList<>();

   public Attack(Creature attacker, Creature target, boolean useShots, int ssGrade) {
      this._attackerObjId = attacker.getObjectId();
      this._soulshot = useShots;
      this._ssGrade = ssGrade;
      this._attackerLoc = new Location(attacker);
      this._targetLoc = new Location(target);
   }

   public void addHit(Creature target, int damage, boolean miss, boolean crit, byte shld) {
      this._hits.add(new Hit(target, damage, miss, crit, shld, this._soulshot, this._ssGrade));
   }

   public boolean hasHits() {
      return !this._hits.isEmpty();
   }

   public boolean hasSoulshot() {
      return this._soulshot;
   }

   private void writeHit(Hit hit, WriteBuffer buffer) {
      buffer.writeInt(hit.getTargetId());
      buffer.writeInt(hit.getDamage());
      buffer.writeByte(hit.getFlags());
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      Iterator<Hit> it = this._hits.iterator();
      org.l2jmobius.gameserver.network.ServerPackets.ATTACK.writeId(this, buffer);
      buffer.writeInt(this._attackerObjId);
      this.writeHit(it.next(), buffer);
      buffer.writeInt(this._attackerLoc.getX());
      buffer.writeInt(this._attackerLoc.getY());
      buffer.writeInt(this._attackerLoc.getZ());
      buffer.writeShort(this._hits.size() - 1);

      while (it.hasNext()) {
         this.writeHit(it.next(), buffer);
      }

      buffer.writeInt(this._targetLoc.getX());
      buffer.writeInt(this._targetLoc.getY());
      buffer.writeInt(this._targetLoc.getZ());
   }
}
