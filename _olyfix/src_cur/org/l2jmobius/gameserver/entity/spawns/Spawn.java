package org.l2jmobius.gameserver.entity.spawns;

import java.lang.reflect.Constructor;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcPersonalAIData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.zone.type.NpcSpawnTerritory;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Spawn extends Location {
   protected static final Logger LOGGER = Logger.getLogger(Spawn.class.getName());
   private String _name;
   private NpcTemplate _template;
   private int _maximumCount;
   private int _currentCount;
   public int _scheduledCount;
   private int _locationId;
   private NpcSpawnTerritory _spawnTerritory = null;
   private int _respawnMinDelay;
   private int _respawnMaxDelay;
   private int _chaseRange = 0;
   private Constructor<? extends Npc> _constructor;
   private boolean _doRespawn = true;
   private static Set<SevenSignsFestival> _spawnListeners = ConcurrentHashMap.newKeySet(1);
   private final Deque<Npc> _spawnedNpcs = new ConcurrentLinkedDeque<>();
   private boolean _randomWalk = false;
   private int _spawnTemplateId = 0;
   private Location _spawnLocation = null;
   private SpawnGroup _group = null;

   public Spawn(NpcTemplate template) throws ClassNotFoundException, NoSuchMethodException, ClassCastException {
      super(0, 0, -10000);
      this._template = template;
      if (this._template != null) {
         this._constructor = Class.forName("org.l2jmobius.gameserver.entity.actor.instance." + this._template.getType())
            .asSubclass(Npc.class)
            .getConstructor(NpcTemplate.class);
      }
   }

   public Spawn(int npcId) throws ClassNotFoundException, NoSuchMethodException, ClassCastException {
      this(NpcData.getInstance().getTemplate(npcId));
   }

   public int getAmount() {
      return this._maximumCount;
   }

   public String getName() {
      return this._name;
   }

   public void setName(String name) {
      this._name = name;
   }

   public int getLocationId() {
      return this._locationId;
   }

   public int getId() {
      return this._template.getId();
   }

   public int getRespawnMinDelay() {
      return this._respawnMinDelay;
   }

   public int getRespawnMaxDelay() {
      return this._respawnMaxDelay;
   }

   public void setAmount(int amount) {
      this._maximumCount = amount;
   }

   public void setLocationId(int id) {
      this._locationId = id;
   }

   public void setRespawnMinDelay(int date) {
      this._respawnMinDelay = date;
   }

   public void setRespawnMaxDelay(int date) {
      this._respawnMaxDelay = date;
   }

   public void decreaseCount(Npc oldNpc) {
      if (this._currentCount > 0) {
         this._currentCount--;
         this._spawnedNpcs.remove(oldNpc);
         if (this._doRespawn && this._scheduledCount + this._currentCount < this._maximumCount) {
            this._scheduledCount++;
            RespawnTaskManager.getInstance()
               .add(
                  oldNpc,
                  System.currentTimeMillis() + (this.hasRespawnRandom() ? Rnd.get(this._respawnMinDelay, this._respawnMaxDelay) : this._respawnMinDelay)
               );
         }

         if (this._group != null) {
            this._group.onUnitDeath(this);
         }
      }
   }

   public SpawnGroup getGroup() {
      return this._group;
   }

   public void setGroup(SpawnGroup group) {
      this._group = group;
   }

   public int init() {
      while (this._currentCount < this._maximumCount) {
         int previousCount = this._currentCount;
         if (this.doSpawn() != null || this._currentCount != previousCount) {
            continue;
         }
         break;
      }

      this._doRespawn = this._respawnMinDelay > 0;
      return this._currentCount;
   }

   public boolean isRespawnEnabled() {
      return this._doRespawn;
   }

   public void stopRespawn() {
      this._doRespawn = false;
   }

   public void startRespawn() {
      this._doRespawn = true;
   }

   public Npc doSpawn() {
      return this._doRespawn ? this.doSpawn(false) : null;
   }

   public Npc doSpawn(boolean isSummonSpawn) {
      try {
         if (!this._template.isType("Pet") && !this._template.isType("Decoy") && !this._template.isType("Trap")) {
            Npc npc = this._constructor.newInstance(this._template);
            npc.setInstanceId(this.getInstanceId());
            if (isSummonSpawn) {
               npc.setShowSummonAnimation(isSummonSpawn);
            }

            return this.initializeNpc(npc);
         } else {
            this._currentCount++;
            return null;
         }
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "NPC " + this._template.getId() + " class not found", (Throwable)var3);
         return null;
      }
   }

   private Npc initializeNpc(Npc npc) {
      npc.setDead(false);
      npc.setDecayed(false);
      int newlocx = 0;
      int newlocy = 0;
      int newlocz = -10000;
      if (this._spawnTerritory != null) {
         Location loc = this._spawnTerritory.getRandomPoint();
         newlocx = loc.getX();
         newlocy = loc.getY();
         newlocz = loc.getZ();
         this.setLocation(loc);
         this.setHeading(-1);
      } else {
         if (this.getX() == 0 && this.getY() == 0) {
            LOGGER.warning("NPC " + npc + " doesn't have spawn location!");
            return null;
         }

         newlocx = this._spawnLocation != null ? this._spawnLocation.getX() : this.getX();
         newlocy = this._spawnLocation != null ? this._spawnLocation.getY() : this.getY();
         newlocz = this._spawnLocation != null ? this._spawnLocation.getZ() : this.getZ();
         if (this._spawnLocation == null) {
            this._spawnLocation = new Location(newlocx, newlocy, newlocz);
         }
      }

      WaterZone water = ZoneManager.getInstance().getZone(newlocx, newlocy, newlocz, WaterZone.class);
      if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS
         && this.getHeading() != -1
         && npc.isMonster()
         && !npc.isQuestMonster()
         && !WalkingManager.getInstance().isTargeted(npc)
         && this.getInstanceId() == 0
         && !this.getTemplate().isUndying()
         && !npc.isRaid()
         && !npc.isRaidMinion()
         && !npc.isFlying()
         && water == null
         && !RandomSpawnsConfig.MOBS_LIST_NOT_RANDOM.contains(npc.getId())) {
         int randX = newlocx + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
         int randY = newlocy + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
         if (GeoEngine.getInstance().canMoveToTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, this.getInstanceId())
            && GeoEngine.getInstance().canSeeTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, this.getInstanceId())) {
            newlocx = randX;
            newlocy = randY;
            this.setXYZ(randX, randY, newlocz);
            this.setHeading(-1);
         }
      }

      if (npc.isMonster() && !npc.isFlying() && water == null) {
         int geoZ = GeoEngine.getInstance().getHeight(newlocx, newlocy, newlocz);
         if (LocationUtil.calculateDistance(newlocx, newlocy, newlocz, newlocx, newlocy, geoZ, true, true) < 300.0) {
            newlocz = geoZ;
         }

         if (this._spawnTerritory != null) {
            int highZ = this._spawnTerritory.getHighZ();
            if (highZ < newlocz) {
               newlocz = highZ;
            }
         }
      }

      npc.stopAllEffects();
      npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
      if (npc.hasVariables()) {
         npc.getVariables().getSet().clear();
      }

      npc.setRandomWalking(this._randomWalk);
      if (this.getHeading() == -1) {
         npc.setHeading(Rnd.get(61794));
      } else {
         npc.setHeading(this.getHeading());
      }

      npc.setSummoner(null);
      npc.resetSummonedNpcs();
      npc.setSpawn(this);
      npc.spawnMe(newlocx, newlocy, newlocz);
      if (npc.isInInstance()) {
         npc.broadcastInfo();
      }

      notifyNpcSpawned(npc);
      if (this._name != null) {
         NpcPersonalAIData.getInstance().initializeNpcParameters(npc, this, this._name);
      }

      this._spawnedNpcs.add(npc);
      this._currentCount++;
      if (npc.isMonster() && NpcData.getMasterMonsterIDs().contains(npc.getId())) {
         npc.asMonster().getMinionList().spawnMinions(npc.getTemplate().getParameters().getMinionList("Privates"));
      }

      if (npc.isAttackable()) {
         npc.asAttackable().setChampion(false);
      }

      if (ChampionMonstersConfig.CHAMPION_ENABLE
         && npc.isMonster()
         && !npc.isQuestMonster()
         && !this._template.isUndying()
         && !npc.isRaid()
         && !npc.isRaidMinion()
         && ChampionMonstersConfig.CHAMPION_FREQUENCY > 0
         && npc.getLevel() >= ChampionMonstersConfig.CHAMP_MIN_LEVEL
         && npc.getLevel() <= ChampionMonstersConfig.CHAMP_MAX_LEVEL
         && (ChampionMonstersConfig.CHAMPION_ENABLE_IN_INSTANCES || this.getInstanceId() == 0)
         && Rnd.get(100) < ChampionMonstersConfig.CHAMPION_FREQUENCY) {
         npc.asAttackable().setChampion(true);
      }

      return npc;
   }

   public static void addSpawnListener(SevenSignsFestival listener) {
      _spawnListeners.add(listener);
   }

   public static void removeSpawnListener(SevenSignsFestival listener) {
      _spawnListeners.remove(listener);
   }

   public static void notifyNpcSpawned(Npc npc) {
      for (SevenSignsFestival listener : _spawnListeners) {
         listener.npcSpawned(npc);
      }
   }

   public void setRespawnDelay(int delay, int randomInterval) {
      if (delay != 0) {
         if (delay < 0) {
            LOGGER.warning("respawn delay is negative for spawn:" + this);
         }

         int minDelay = delay - randomInterval;
         int maxDelay = delay + randomInterval;
         this._respawnMinDelay = Math.max(1, minDelay) * 1000;
         this._respawnMaxDelay = Math.max(1, maxDelay) * 1000;
      } else {
         this._respawnMinDelay = 0;
         this._respawnMaxDelay = 0;
      }
   }

   public void setRespawnDelay(int delay) {
      this.setRespawnDelay(delay, 0);
   }

   public int getRespawnDelay() {
      return (this._respawnMinDelay + this._respawnMaxDelay) / 2;
   }

   public boolean hasRespawnRandom() {
      return this._respawnMinDelay != this._respawnMaxDelay;
   }

   public void setChaseRange(int chaseRange) {
      this._chaseRange = chaseRange;
   }

   public int getChaseRange() {
      return this._chaseRange;
   }

   public void setSpawnTerritory(NpcSpawnTerritory territory) {
      this._spawnTerritory = territory;
   }

   public NpcSpawnTerritory getSpawnTerritory() {
      return this._spawnTerritory;
   }

   public boolean isTerritoryBased() {
      return this._spawnTerritory != null && this.getX() == 0 && this.getY() == 0;
   }

   public Npc getLastSpawn() {
      return this._spawnedNpcs.peekLast();
   }

   public Deque<Npc> getSpawnedNpcs() {
      return this._spawnedNpcs;
   }

   public void respawnNpc(Npc oldNpc) {
      if (this._doRespawn) {
         this.initializeNpc(oldNpc);
      }
   }

   public NpcTemplate getTemplate() {
      return this._template;
   }

   public boolean getRandomWalking() {
      return this._randomWalk;
   }

   public void setRandomWalking(boolean value) {
      this._randomWalk = value;
   }

   public void setSpawnTemplateId(int npcSpawnTemplateId) {
      this._spawnTemplateId = npcSpawnTemplateId;
   }

   public int getNpcSpawnTemplateId() {
      return this._spawnTemplateId;
   }

   public Location getSpawnLocation() {
      return this._spawnLocation;
   }

   @Override
   public String toString() {
      return "Spawn ID: " + this._template.getId() + " X: " + this.getX() + " Y: " + this.getY() + " Z: " + this.getZ() + " Heading: " + this.getHeading();
   }
}
