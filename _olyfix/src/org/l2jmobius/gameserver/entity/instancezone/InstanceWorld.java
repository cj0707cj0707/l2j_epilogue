package org.l2jmobius.gameserver.entity.instancezone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

public class InstanceWorld {
   private Instance _instance;
   private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet();
   private final StatSet _parameters = new StatSet();

   public void setInstance(Instance instance) {
      this._instance = instance;
   }

   public int getInstanceId() {
      return this._instance.getId();
   }

   public int getTemplateId() {
      return this._instance.getTemplateId();
   }

   public List<Player> getAllowed() {
      List<Player> allowed = new ArrayList<>(this._allowed.size());

      for (int playerId : this._allowed) {
         Player player = World.getPlayer(playerId);
         if (player != null) {
            allowed.add(player);
         }
      }

      return allowed;
   }

   public void removeAllowed(Player player) {
      this._allowed.remove(player.getObjectId());
   }

   public void addAllowed(Player player) {
      if (!this._allowed.contains(player.getObjectId())) {
         this._allowed.add(player.getObjectId());
      }
   }

   public boolean isAllowed(Player player) {
      return this._allowed.contains(player.getObjectId());
   }

   public void ejectPlayer(Player player) {
      if (player != null && player.getInstanceId() == this._instance.getId()) {
         player.setInstanceId(0);
         if (this._instance.getExitLoc() != null) {
            player.teleToLocation(this._instance.getExitLoc(), true);
         } else {
            player.teleToLocation(TeleportWhereType.TOWN);
         }
      }
   }

   public void setParameter(String key, Object value) {
      if (value == null) {
         this._parameters.remove(key);
      } else {
         this._parameters.set(key, value);
      }
   }

   public StatSet getParameters() {
      return this._parameters;
   }

   public int getStatus() {
      return this._parameters.getInt("INSTANCE_STATUS", 0);
   }

   public boolean isStatus(int status) {
      return this.getStatus() == status;
   }

   public void setStatus(int value) {
      this._parameters.set("INSTANCE_STATUS", value);
   }

   public int incStatus() {
      int status = this.getStatus() + 1;
      this.setStatus(status);
      return status;
   }

   public Collection<Npc> getNpcs() {
      return this._instance.getNpcs();
   }

   public List<Npc> getNpcs(int... id) {
      List<Npc> result = new ArrayList<>();

      for (Npc npc : this._instance.getNpcs()) {
         if (ArrayUtil.contains(id, npc.getId())) {
            result.add(npc);
         }
      }

      return result;
   }

   @SafeVarargs
   public final <T extends Creature> List<T> getNpcs(Class<T> clazz, int... ids) {
      List<T> result = new ArrayList<>();

      for (Npc npc : this._instance.getNpcs()) {
         if ((ids.length == 0 || ArrayUtil.contains(ids, npc.getId())) && clazz.isInstance(npc)) {
            result.add((T)npc);
         }
      }

      return result;
   }

   public List<Npc> getAliveNpcs() {
      List<Npc> result = new ArrayList<>();

      for (Npc npc : this._instance.getNpcs()) {
         if (npc.getCurrentHp() > 0.0) {
            result.add(npc);
         }
      }

      return result;
   }

   public List<Npc> getAliveNpcs(int... id) {
      List<Npc> result = new ArrayList<>();

      for (Npc npc : this._instance.getNpcs()) {
         if (npc.getCurrentHp() > 0.0 && ArrayUtil.contains(id, npc.getId())) {
            result.add(npc);
         }
      }

      return result;
   }

   @SafeVarargs
   public final <T extends Creature> List<T> getAliveNpcs(Class<T> clazz, int... ids) {
      List<T> result = new ArrayList<>();

      for (Npc npc : this._instance.getNpcs()) {
         if ((ids.length == 0 || ArrayUtil.contains(ids, npc.getId())) && npc.getCurrentHp() > 0.0 && clazz.isInstance(npc)) {
            result.add((T)npc);
         }
      }

      return result;
   }

   public Npc getNpc(int id) {
      for (Npc npc : this._instance.getNpcs()) {
         if (npc.getId() == id) {
            return npc;
         }
      }

      return null;
   }

   public List<Npc> spawnGroup(String groupName) {
      return this._instance.spawnGroup(groupName);
   }

   public void openDoor(int doorId) {
      Door door = this._instance.getDoor(doorId);
      if (door != null && !door.isOpen()) {
         door.openMe();
      }
   }

   public void closeDoor(int doorId) {
      Door door = this._instance.getDoor(doorId);
      if (door != null && door.isOpen()) {
         door.closeMe();
      }
   }

   public Collection<Door> getDoors() {
      return this._instance.getDoors();
   }

   public Door getDoor(int doorId) {
      return this._instance.getDoor(doorId);
   }

   public int getPlayersCount() {
      return this._instance.getPlayers().size();
   }

   public void destroy() {
      this._instance.setEmptyDestroyTime(0L);
      this._instance.setDuration(1000);
   }

   public void broadcastPacket(ServerPacket... packets) {
      for (int objectId : this._instance.getPlayers()) {
         Player player = World.getPlayer(objectId);
         if (player != null && player.getInstanceId() == this._instance.getId()) {
            for (ServerPacket packet : packets) {
               player.sendPacket(packet);
            }
         }
      }
   }
}
