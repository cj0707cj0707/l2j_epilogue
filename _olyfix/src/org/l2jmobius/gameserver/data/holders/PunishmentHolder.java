package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

public class PunishmentHolder {
   private final Map<String, Map<PunishmentType, PunishmentTask>> _holder = new ConcurrentHashMap<>();

   public void addPunishment(PunishmentTask task) {
      if (!task.isExpired()) {
         this._holder.computeIfAbsent(String.valueOf(task.getKey()), var0 -> new ConcurrentHashMap<>()).put(task.getType(), task);
      }
   }

   public void stopPunishment(PunishmentTask task) {
      String key = String.valueOf(task.getKey());
      Map<PunishmentType, PunishmentTask> punishments = this._holder.get(key);
      if (punishments != null) {
         task.stopPunishment();
         punishments.remove(task.getType());
         if (punishments.isEmpty()) {
            this._holder.remove(key);
         }
      }
   }

   public boolean hasPunishment(String key, PunishmentType type) {
      return this.getPunishment(key, type) != null;
   }

   public PunishmentTask getPunishment(String key, PunishmentType type) {
      return this._holder.getOrDefault(key, Collections.emptyMap()).get(type);
   }
}
