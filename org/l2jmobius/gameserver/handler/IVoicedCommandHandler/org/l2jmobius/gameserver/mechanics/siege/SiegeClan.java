package org.l2jmobius.gameserver.mechanics.siege;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;

public class SiegeClan {
   private int _clanId = 0;
   private final Set<Npc> _flag = ConcurrentHashMap.newKeySet();
   private SiegeClanType _type;

   public SiegeClan(int clanId, SiegeClanType type) {
      this._clanId = clanId;
      this._type = type;
   }

   public int getNumFlags() {
      return this._flag.size();
   }

   public void addFlag(Npc flag) {
      this._flag.add(flag);
   }

   public boolean removeFlag(Npc flag) {
      boolean ret = this._flag.remove(flag);
      flag.deleteMe();
      return ret;
   }

   public void removeFlags() {
      this._flag.forEach(Creature::decayMe);
      this._flag.clear();
   }

   public int getClanId() {
      return this._clanId;
   }

   public Set<Npc> getFlag() {
      return this._flag;
   }

   public SiegeClanType getType() {
      return this._type;
   }

   public void setType(SiegeClanType setType) {
      this._type = setType;
   }
}
