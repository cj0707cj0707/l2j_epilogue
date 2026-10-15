package org.l2jmobius.gameserver.entity.actor.transform;

import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

public class TransformLevelData {
   private final int _level;
   private final double _levelMod;
   private Map<Integer, Double> _stats;

   public TransformLevelData(StatSet set) {
      this._level = set.getInt("val");
      this._levelMod = set.getDouble("levelMod");
      this.addStats(Stat.MAX_HP, set.getDouble("hp"));
      this.addStats(Stat.MAX_MP, set.getDouble("mp"));
      this.addStats(Stat.MAX_CP, set.getDouble("cp"));
      this.addStats(Stat.REGENERATE_HP_RATE, set.getDouble("hpRegen"));
      this.addStats(Stat.REGENERATE_MP_RATE, set.getDouble("mpRegen"));
      this.addStats(Stat.REGENERATE_CP_RATE, set.getDouble("cpRegen"));
   }

   private void addStats(Stat stat, double value) {
      if (this._stats == null) {
         this._stats = new HashMap<>();
      }

      this._stats.put(stat.ordinal(), value);
   }

   public double getStats(Stat stat) {
      return this._stats != null && this._stats.containsKey(stat.ordinal()) ? this._stats.get(stat.ordinal()) : 0.0;
   }

   public int getLevel() {
      return this._level;
   }

   public double getLevelMod() {
      return this._levelMod;
   }
}
