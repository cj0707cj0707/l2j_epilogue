package org.l2jmobius.gameserver.managers.games;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiCubePlayer;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiMsgType;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateisReward;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class KrateisCubeManager implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(KrateisCubeManager.class.getName());
   private final Map<Integer, KrateiArena> _arenas = new HashMap<>(3);
   private Npc _manager = null;
   private long _nextMatchTime;
   private boolean _registerActive = false;
   private boolean _isHalfAnHour = false;
   private KrateisCubeManager.EventState _state = KrateisCubeManager.EventState.REGISTRATION;
   private ScheduledFuture<?> _periodTask = null;
   private ScheduledFuture<?> _eventTask = null;
   private ScheduledFuture<?> _msgTask = null;

   public KrateisCubeManager() {
      this.load();
   }

   @Override
   public void load() {
      this.parseDatapackFile("data/KrateisCube.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._arenas.size() + " arena templates.");
      Spawn spawn = SpawnTable.getInstance().getAnySpawn(32503);
      if (spawn != null) {
         this._manager = spawn.getLastSpawn();
         this.recalcEventTime();
      } else {
         LOGGER.info(this.getClass().getSimpleName() + ": Event can't be started because npc not found!");
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("arena".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap map = d.getAttributes();
                  int id = Integer.parseInt(map.getNamedItem("id").getNodeValue());
                  int manager = Integer.parseInt(map.getNamedItem("manager").getNodeValue());
                  int minLevel = Integer.parseInt(map.getNamedItem("minLevel").getNodeValue());
                  int maxLevel = Integer.parseInt(map.getNamedItem("maxLevel").getNodeValue());
                  int minPlayers = Integer.parseInt(map.getNamedItem("minPlayers").getNodeValue());
                  int maxPlayers = Integer.parseInt(map.getNamedItem("maxPlayers").getNodeValue());
                  KrateiArena arena = new KrateiArena(id, manager, minLevel, maxLevel, minPlayers, maxPlayers);
                  StatSet params = new StatSet();

                  for (Node kc = d.getFirstChild(); kc != null; kc = kc.getNextSibling()) {
                     if ("doorA".equalsIgnoreCase(kc.getNodeName())) {
                        map = kc.getAttributes();
                        String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
                        if (!doorList.isEmpty()) {
                           String[] doorSplint = doorList.split(",");

                           for (String doorId : doorSplint) {
                              Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
                              if (door != null) {
                                 arena.addDoorA(door);
                              }
                           }
                        }
                     } else if ("doorB".equalsIgnoreCase(kc.getNodeName())) {
                        map = kc.getAttributes();
                        String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
                        if (!doorList.isEmpty()) {
                           String[] doorSplint = doorList.split(",");

                           for (String doorIdx : doorSplint) {
                              Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorIdx));
                              if (door != null) {
                                 arena.addDoorB(door);
                              }
                           }
                        }
                     } else if ("waitLocations".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling()) {
                           if ("point".equalsIgnoreCase(c.getNodeName())) {
                              map = c.getAttributes();
                              int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
                              int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
                              int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
                              arena.addWaitLoc(new Location(x, y, z));
                           }
                        }
                     } else if ("battleLocations".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node cx = kc.getFirstChild(); cx != null; cx = cx.getNextSibling()) {
                           if ("point".equalsIgnoreCase(cx.getNodeName())) {
                              map = cx.getAttributes();
                              int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
                              int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
                              int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
                              arena.addBattleLoc(new Location(x, y, z));
                           }
                        }
                     } else if ("watcherLocations".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node cxx = kc.getFirstChild(); cxx != null; cxx = cxx.getNextSibling()) {
                           if ("point".equalsIgnoreCase(cxx.getNodeName())) {
                              map = cxx.getAttributes();
                              int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
                              int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
                              int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
                              arena.addWatcherLoc(new Location(x, y, z));
                           }
                        }
                     } else if ("buffs".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node cxxx = kc.getFirstChild(); cxxx != null; cxxx = cxxx.getNextSibling()) {
                           if ("skill".equalsIgnoreCase(cxxx.getNodeName())) {
                              int skillId = Integer.parseInt(cxxx.getAttributes().getNamedItem("id").getNodeValue());
                              int level = Integer.parseInt(cxxx.getAttributes().getNamedItem("level").getNodeValue());
                              Skill skill = SkillData.getInstance().getSkill(skillId, level);
                              if (skill != null) {
                                 arena.addBuff(skill);
                              }
                           }
                        }
                     } else if ("rewards".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node cxxxx = kc.getFirstChild(); cxxxx != null; cxxxx = cxxxx.getNextSibling()) {
                           if ("item".equalsIgnoreCase(cxxxx.getNodeName())) {
                              int itemId = Integer.parseInt(cxxxx.getAttributes().getNamedItem("id").getNodeValue());
                              long amount = Long.parseLong(cxxxx.getAttributes().getNamedItem("amount").getNodeValue());
                              boolean useModifier = cxxxx.getAttributes().getNamedItem("useModifers") != null
                                 ? Boolean.parseBoolean(cxxxx.getAttributes().getNamedItem("useModifers").getNodeValue())
                                 : false;
                              arena.setReward(new KrateisReward(itemId, amount, useModifier));
                           }
                        }
                     } else if ("add_parameters".equalsIgnoreCase(kc.getNodeName())) {
                        for (Node cxxxxx = kc.getFirstChild(); cxxxxx != null; cxxxxx = cxxxxx.getNextSibling()) {
                           if ("set".equalsIgnoreCase(cxxxxx.getNodeName())) {
                              params.set(
                                 cxxxxx.getAttributes().getNamedItem("name").getNodeValue(), cxxxxx.getAttributes().getNamedItem("value").getNodeValue()
                              );
                           }
                        }

                        arena.addParam(params);
                     }
                  }

                  this._arenas.put(arena.getId(), arena);
               }
            }
         }
      }
   }

   public void prepareEvent() {
      if (this._registerActive) {
         this._registerActive = false;
      }

      if (this._periodTask != null) {
         this._periodTask.cancel(false);
      }

      if (this._eventTask != null) {
         this._eventTask.cancel(false);
      }

      if (this._msgTask != null) {
         this._msgTask.cancel(false);
      }

      boolean isActive = false;

      for (KrateiArena arena : this._arenas.values()) {
         if (arena != null && arena.getPlayers().size() >= arena.getMinPlayers()) {
            int count = 0;
            List<Player> invalidPlayers = new ArrayList<>();

            for (KrateiCubePlayer pl : arena.getPlayers().values()) {
               if (pl != null && pl.isRegister()) {
                  if (pl.getPlayer().isInsideRadius3D(this._manager, PlayerConfig.ALT_PARTY_RANGE)) {
                     pl.setIsInside(true);
                     pl.setIsRegister(false);
                     count++;
                  } else {
                     invalidPlayers.add(pl.getPlayer());
                  }
               }
            }

            if (!invalidPlayers.isEmpty()) {
               for (Player plx : invalidPlayers) {
                  arena.removePlayer(plx);
               }
            }

            invalidPlayers.clear();
            if (count > 1) {
               arena.setIsBattleNow(true);
               arena.teleportToBattle(null);
               arena.waitTimeInfo();
               isActive = true;
            } else {
               for (KrateiCubePlayer plx : arena.getPlayers().values()) {
                  if (plx != null && plx.isInside()) {
                     plx.setIsInside(false);
                     plx.setIsRegister(true);
                  }
               }
            }
         }
      }

      if (isActive) {
         this._eventTask = ThreadPool.schedule(this::startEvent, 11000L);
         this.getManagerMessage(KrateiMsgType.STARTED);
      } else {
         this.recalcEventTime();
      }
   }

   public void startEvent() {
      if (this._eventTask != null) {
         this._eventTask.cancel(false);
      }

      for (KrateiArena arena : this._arenas.values()) {
         if (arena != null && arena.isBattleNow()) {
            arena.startEvent();
            this._state = KrateisCubeManager.EventState.STARTED;
         }
      }

      this._periodTask = ThreadPool.schedule(this::recalcEventTime, 180000L);
   }

   public void abortEvent() {
      for (KrateiArena arena : this._arenas.values()) {
         if (arena != null && arena.isBattleNow()) {
            arena.endEvent();
         }
      }

      if (!this._registerActive) {
         this.recalcEventTime();
      }
   }

   public boolean isRegisterTime() {
      return this._registerActive;
   }

   public boolean isActive() {
      return this._state == KrateisCubeManager.EventState.STARTED;
   }

   public boolean isPreparing() {
      return this._state == KrateisCubeManager.EventState.PREPARING;
   }

   public void setIsActivate(boolean val) {
      if (!val) {
         this._state = KrateisCubeManager.EventState.REGISTRATION;
      }
   }

   public void recalcEventTime() {
      if (!this._registerActive) {
         this._registerActive = true;
      }

      if (this._periodTask != null) {
         this._periodTask.cancel(false);
      }

      if (this._eventTask != null) {
         this._eventTask.cancel(false);
      }

      Calendar calendar = Calendar.getInstance();
      if (calendar.get(12) >= 57) {
         calendar.add(10, 1);
         calendar.set(12, 27);
      } else if (calendar.get(12) >= 0 && calendar.get(12) <= 26) {
         calendar.set(12, 27);
         calendar.set(13, 0);
         this._isHalfAnHour = true;
      } else {
         calendar.set(12, 57);
         calendar.set(13, 0);
         this._isHalfAnHour = false;
      }

      long currentTime = System.currentTimeMillis();
      if (calendar.getTimeInMillis() < currentTime) {
         calendar.add(6, 1);
      }

      this._nextMatchTime = calendar.getTimeInMillis();
      long lastTime = this._nextMatchTime - currentTime;
      this._eventTask = ThreadPool.schedule(this::closeRegistration, lastTime);
      this.getManagerMessage(KrateiMsgType.INITIALIZED);
      int time = (int)(lastTime / 1000L / 60L);
      if (time >= 5) {
         this._msgTask = ThreadPool.schedule(() -> this.getManagerMessage(KrateiMsgType.REGISTRATION_5), lastTime - 300000L);
      } else if (time >= 3 && time < 5) {
         this._msgTask = ThreadPool.schedule(() -> this.getManagerMessage(KrateiMsgType.REGISTRATION_3), lastTime - 180000L);
      } else if (time >= 1 && time < 3) {
         this._msgTask = ThreadPool.schedule(() -> this.getManagerMessage(KrateiMsgType.REGISTRATION_1), lastTime - 60000L);
      }

      if (this._state != KrateisCubeManager.EventState.STARTED) {
         this._state = KrateisCubeManager.EventState.REGISTRATION;
      }
   }

   public void closeRegistration() {
      if (this._periodTask != null) {
         this._periodTask.cancel(false);
      }

      this._registerActive = false;
      this.getManagerMessage(KrateiMsgType.PREPATING);
      this._state = KrateisCubeManager.EventState.PREPARING;
      this._periodTask = ThreadPool.schedule(this::prepareEvent, 180000L);
   }

   private void getManagerMessage(KrateiMsgType state) {
      long lastTime = this._nextMatchTime - System.currentTimeMillis();
      switch (state) {
         case INITIALIZED:
            CreatureSay msg = new CreatureSay(
               this._manager, ChatType.NPC_SHOUT, "You have " + (!this._isHalfAnHour ? 57 : 27) + " minute(s) to register for the match."
            );
            this._manager.broadcastPacket(msg);
            break;
         case REGISTRATION_5:
            this._manager
               .broadcastPacket(new CreatureSay(this._manager, ChatType.NPC_SHOUT, "There are 5 minutes remaining to register for Kratei's cube match."));
            this._msgTask = ThreadPool.schedule(() -> this.getManagerMessage(KrateiMsgType.REGISTRATION_3), lastTime - 180000L);
            break;
         case REGISTRATION_3:
            this._manager
               .broadcastPacket(new CreatureSay(this._manager, ChatType.NPC_SHOUT, "There are 3 minutes remaining to register for Kratei's cube match."));
            this._msgTask = ThreadPool.schedule(() -> this.getManagerMessage(KrateiMsgType.REGISTRATION_1), lastTime - 60000L);
            break;
         case REGISTRATION_1:
            this._manager
               .broadcastPacket(new CreatureSay(this._manager, ChatType.NPC_SHOUT, "There are 1 minutes remaining to register for Kratei's cube match."));
            break;
         case PREPATING:
            CreatureSay msg3 = new CreatureSay(this._manager, ChatType.NPC_SHOUT, "The match will begin in 3 minute(s).");
            this._manager.broadcastPacket(msg3);
            break;
         case STARTED:
            this._manager.broadcastPacket(new CreatureSay(this._manager, ChatType.NPC_SHOUT, "The match will begin shortly."));
      }
   }

   public long getNextMatchTime() {
      return this._nextMatchTime;
   }

   public KrateiArena getArenaId(int id) {
      return this._arenas.get(id);
   }

   public Map<Integer, KrateiArena> getArenas() {
      return this._arenas;
   }

   public static KrateisCubeManager getInstance() {
      return KrateisCubeManager.SingletonHolder.INSTANCE;
   }

   private static enum EventState {
      REGISTRATION,
      PREPARING,
      STARTED;
   }

   private static class SingletonHolder {
      protected static final KrateisCubeManager INSTANCE = new KrateisCubeManager();
   }
}
