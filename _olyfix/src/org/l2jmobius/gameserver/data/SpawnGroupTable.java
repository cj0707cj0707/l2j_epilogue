package org.l2jmobius.gameserver.data;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;

public class SpawnGroupTable {
   private final Collection<SpawnGroup> _groups = new CopyOnWriteArrayList<>();

   protected SpawnGroupTable() {
   }

   public Collection<SpawnGroup> getGroups() {
      return this._groups;
   }

   public void addGroup(SpawnGroup group) {
      this._groups.add(group);
   }

   public void clear() {
      this._groups.clear();
   }

   public void deactivateAll() {
      for (SpawnGroup group : this._groups) {
         group.setActive(false);
      }
   }

   public static SpawnGroupTable getInstance() {
      return SpawnGroupTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SpawnGroupTable INSTANCE = new SpawnGroupTable();
   }
}
