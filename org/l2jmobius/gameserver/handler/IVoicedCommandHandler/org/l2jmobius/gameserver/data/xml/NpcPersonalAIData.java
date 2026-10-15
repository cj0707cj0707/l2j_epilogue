package org.l2jmobius.gameserver.data.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

public class NpcPersonalAIData {
   private final Map<String, Map<String, Integer>> _AIData = new HashMap<>();

   protected NpcPersonalAIData() {
   }

   public void storeData(Spawn spawnDat, Map<String, Integer> data) {
      if (data != null && !data.isEmpty()) {
         if (spawnDat.getName() == null) {
            spawnDat.setName(Long.toString(Rnd.nextLong()));
         }

         this._AIData.put(spawnDat.getName(), data);
      }
   }

   public int getAIValue(String spawnName, String paramName) {
      return this.hasAIValue(spawnName, paramName) ? this._AIData.get(spawnName).get(paramName) : -1;
   }

   public boolean hasAIValue(String spawnName, String paramName) {
      if (spawnName == null) {
         return false;
      } else {
         Map<String, Integer> params = this._AIData.get(spawnName);
         return params != null && params.containsKey(paramName);
      }
   }

   public void initializeNpcParameters(Npc npc, Spawn spawn, String spawnName) {
      Map<String, Integer> map = this._AIData.get(spawnName);
      if (map != null) {
         try {
            for (Entry<String, Integer> entry : map.entrySet()) {
               String key = entry.getKey();
               switch (key) {
                  case "disableRandomAnimation":
                     npc.setRandomAnimationEnabled(entry.getValue() == 0);
                     break;
                  case "disableRandomWalk":
                     boolean enable = entry.getValue() == 0;
                     npc.setRandomWalking(enable);
                     spawn.setRandomWalking(enable);
               }
            }
         } catch (Exception var11) {
         }
      }
   }

   public static NpcPersonalAIData getInstance() {
      return NpcPersonalAIData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final NpcPersonalAIData INSTANCE = new NpcPersonalAIData();
   }
}
