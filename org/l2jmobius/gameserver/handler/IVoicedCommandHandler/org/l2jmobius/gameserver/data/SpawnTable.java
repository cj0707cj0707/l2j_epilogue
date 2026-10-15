package org.l2jmobius.gameserver.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

public class SpawnTable {
   private final Map<Integer, Set<Spawn>> _npcSpawns = new ConcurrentHashMap<>();

   protected SpawnTable() {
   }

   public Map<Integer, Set<Spawn>> getSpawnTable() {
      return this._npcSpawns;
   }

   public Set<Spawn> getSpawns(int npcId) {
      return this._npcSpawns.getOrDefault(npcId, Collections.emptySet());
   }

   public int getSpawnCount(int npcId) {
      return this.getSpawns(npcId).size();
   }

   public Spawn getAnySpawn(int npcId) {
      return this.getSpawns(npcId).stream().findFirst().orElse(null);
   }

   public void addSpawn(Spawn spawn) {
      this._npcSpawns.computeIfAbsent(spawn.getId(), var0 -> ConcurrentHashMap.newKeySet(1)).add(spawn);
   }

   public void removeSpawn(Spawn spawn) {
      Set<Spawn> set = this._npcSpawns.get(spawn.getId());
      if (set != null) {
         set.remove(spawn);
         if (set.isEmpty()) {
            this._npcSpawns.remove(spawn.getId());
         }
      }
   }

   public static SpawnTable getInstance() {
      return SpawnTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SpawnTable INSTANCE = new SpawnTable();
   }
}
