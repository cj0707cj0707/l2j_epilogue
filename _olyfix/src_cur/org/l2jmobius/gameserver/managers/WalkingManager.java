package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.holders.NpcRoutesHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkNode;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkRoute;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.tasks.npc.walker.ArrivedTask;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class WalkingManager implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(WalkingManager.class.getName());
   public static final byte NO_REPEAT = -1;
   public static final byte REPEAT_GO_BACK = 0;
   public static final byte REPEAT_GO_FIRST = 1;
   public static final byte REPEAT_TELE_FIRST = 2;
   public static final byte REPEAT_RANDOM = 3;
   private final Set<Integer> _targetedNpcIds = new HashSet<>();
   private final Map<String, WalkRoute> _routes = new HashMap<>();
   private final Map<Integer, WalkInfo> _activeRoutes = new HashMap<>();
   private final Map<Integer, NpcRoutesHolder> _routesToAttach = new HashMap<>();
   private final Map<Npc, ScheduledFuture<?>> _startMoveTasks = new ConcurrentHashMap<>();
   private final Map<Npc, ScheduledFuture<?>> _repeatMoveTasks = new ConcurrentHashMap<>();
   private final Map<Npc, ScheduledFuture<?>> _arriveTasks = new ConcurrentHashMap<>();

   protected WalkingManager() {
      this.load();
   }

   @Override
   public void load() {
      this.parseDatapackFile("data/Routes.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._routes.size() + " walking routes.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling()) {
         if (d.getNodeName().equals("route")) {
            String routeName = this.parseString(d.getAttributes(), "name");
            boolean repeat = this.parseBoolean(d.getAttributes(), "repeat");
            String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue().toLowerCase();

            byte repeatType = switch (repeatStyle) {
               case "back" -> 0;
               case "cycle" -> 1;
               case "conveyor" -> 2;
               case "random" -> 3;
               default -> -1;
            };
            List<WalkNode> list = new ArrayList<>();

            for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling()) {
               if (r.getNodeName().equals("point")) {
                  NamedNodeMap attrs = r.getAttributes();
                  int x = this.parseInteger(attrs, "X");
                  int y = this.parseInteger(attrs, "Y");
                  int z = this.parseInteger(attrs, "Z");
                  int delay = this.parseInteger(attrs, "delay");
                  boolean run = this.parseBoolean(attrs, "run");
                  String chatString = null;
                  Node node = attrs.getNamedItem("string");
                  if (node != null) {
                     chatString = node.getNodeValue();
                  }

                  list.add(new WalkNode(x, y, z, delay, run, chatString));
               } else if (r.getNodeName().equals("target")) {
                  NamedNodeMap attrs = r.getAttributes();

                  try {
                     int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                     int x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
                     int y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
                     int z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());
                     if (NpcData.getInstance().getTemplate(npcId) != null) {
                        NpcRoutesHolder holder = this._routesToAttach.containsKey(npcId) ? this._routesToAttach.get(npcId) : new NpcRoutesHolder();
                        holder.addRoute(routeName, new Location(x, y, z));
                        this._routesToAttach.put(npcId, holder);
                        if (!this._targetedNpcIds.contains(npcId)) {
                           this._targetedNpcIds.add(npcId);
                        }
                     }
                  } catch (Exception var18) {
                     LOGGER.warning(this.getClass().getSimpleName() + ": Error in target definition for route '" + routeName + "'");
                  }
               }
            }

            this._routes.put(routeName, new WalkRoute(routeName, list, repeat, repeatType));
         }
      }
   }

   public boolean isOnWalk(Npc npc) {
      Monster monster = npc.isMonster() ? (npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader()) : null;
      if ((monster == null || this.isRegistered(monster)) && this.isRegistered(npc)) {
         WalkInfo walk = monster != null ? this._activeRoutes.get(monster.getObjectId()) : this._activeRoutes.get(npc.getObjectId());
         return !walk.isStoppedByAttack() && !walk.isSuspended();
      } else {
         return false;
      }
   }

   public WalkRoute getRoute(String route) {
      return this._routes.get(route);
   }

   public boolean isTargeted(Npc npc) {
      return this._targetedNpcIds.contains(npc.getId());
   }

   private boolean isRegistered(Npc npc) {
      return this._activeRoutes.containsKey(npc.getObjectId());
   }

   public String getRouteName(Npc npc) {
      return this._activeRoutes.containsKey(npc.getObjectId()) ? this._activeRoutes.get(npc.getObjectId()).getRoute().getName() : "";
   }

   public void startMoving(Npc npc, String routeName) {
      if (this._routes.containsKey(routeName) && npc != null && !npc.isDead()) {
         if (!this._activeRoutes.containsKey(npc.getObjectId())) {
            if (npc.getAI().getIntention() != Intention.ACTIVE && npc.getAI().getIntention() != Intention.IDLE) {
               ScheduledFuture<?> task = this._startMoveTasks.get(npc);
               if (task == null || task.isCancelled() || task.isDone()) {
                  this._startMoveTasks.put(npc, ThreadPool.schedule(() -> this.startMoving(npc, routeName), 10000L));
               }
            } else {
               WalkInfo walk = new WalkInfo(routeName);
               WalkNode node = walk.getCurrentNode();
               if (npc.getX() == node.getX() && npc.getY() == node.getY()) {
                  walk.calculateNextNode(npc);
                  node = walk.getCurrentNode();
               }

               if (!npc.isInsideRadius3D(node, 3000)) {
                  LOGGER.warning(
                     this.getClass().getSimpleName()
                        + ": Route '"
                        + routeName
                        + "': NPC (id="
                        + npc.getId()
                        + ", x="
                        + npc.getX()
                        + ", y="
                        + npc.getY()
                        + ", z="
                        + npc.getZ()
                        + ") is too far from starting point (node x="
                        + node.getX()
                        + ", y="
                        + node.getY()
                        + ", z="
                        + node.getZ()
                        + ", range="
                        + npc.calculateDistance3D(node)
                        + "). Teleporting to proper location."
                  );
                  npc.teleToLocation(node);
               }

               if (node.runToLocation()) {
                  npc.setRunning();
               } else {
                  npc.setWalking();
               }

               npc.getAI().setIntentionMoveTo(node);
               ScheduledFuture<?> task = this._repeatMoveTasks.get(npc);
               if (task == null || task.isCancelled() || task.isDone()) {
                  ScheduledFuture<?> newTask = ThreadPool.scheduleAtFixedRate(() -> this.startMoving(npc, routeName), 10000L, 10000L);
                  this._repeatMoveTasks.put(npc, newTask);
                  walk.setWalkCheckTask(newTask);
               }

               npc.setWalker();
               this._activeRoutes.put(npc.getObjectId(), walk);
            }
         } else if (this._activeRoutes.containsKey(npc.getObjectId())
            && (npc.getAI().getIntention() == Intention.ACTIVE || npc.getAI().getIntention() == Intention.IDLE)) {
            WalkInfo walkx = this._activeRoutes.get(npc.getObjectId());
            if (walkx == null) {
               return;
            }

            if (walkx.isBlocked() || walkx.isSuspended()) {
               return;
            }

            walkx.setBlocked(true);
            WalkNode nodex = walkx.getCurrentNode();
            if (nodex.runToLocation()) {
               npc.setRunning();
            } else {
               npc.setWalking();
            }

            npc.getAI().setIntentionMoveTo(nodex);
            walkx.setBlocked(false);
            walkx.setStoppedByAttack(false);
         }
      }
   }

   public synchronized void cancelMoving(Npc npc) {
      WalkInfo walk = this._activeRoutes.remove(npc.getObjectId());
      if (walk != null) {
         ScheduledFuture<?> task = walk.getWalkCheckTask();
         if (task != null) {
            task.cancel(true);
         }
      }
   }

   public void resumeMoving(Npc npc) {
      WalkInfo walk = this._activeRoutes.get(npc.getObjectId());
      if (walk != null) {
         walk.setSuspended(false);
         walk.setStoppedByAttack(false);
         this.startMoving(npc, walk.getRoute().getName());
      }
   }

   public void stopMoving(Npc npc, boolean suspend, boolean stoppedByAttack) {
      Monster monster = npc.isMonster() ? (npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader()) : null;
      if ((monster == null || this.isRegistered(monster)) && this.isRegistered(npc)) {
         WalkInfo walk = monster != null ? this._activeRoutes.get(monster.getObjectId()) : this._activeRoutes.get(npc.getObjectId());
         walk.setSuspended(suspend);
         walk.setStoppedByAttack(stoppedByAttack);
         if (monster != null) {
            monster.stopMove(null);
            monster.getAI().setIntentionActive();
         } else {
            npc.stopMove(null);
            npc.getAI().setIntentionActive();
         }
      }
   }

   public void onArrived(Npc npc) {
      if (this._activeRoutes.containsKey(npc.getObjectId())) {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_NODE_ARRIVED, npc)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveNodeArrived(npc), npc);
         }

         WalkInfo walk = this._activeRoutes.get(npc.getObjectId());
         if (walk.getCurrentNodeId() >= 0 && walk.getCurrentNodeId() < walk.getRoute().getNodesCount()) {
            List<WalkNode> nodelist = walk.getRoute().getNodeList();
            WalkNode node = nodelist.get(Math.min(walk.getCurrentNodeId(), nodelist.size() - 1));
            if (npc.isInsideRadius2D(node, 10)) {
               walk.calculateNextNode(npc);
               walk.setBlocked(true);
               if (!node.getChatText().isEmpty()) {
                  npc.broadcastSay(ChatType.NPC_GENERAL, node.getChatText());
               }

               ScheduledFuture<?> task = this._arriveTasks.get(npc);
               if (task == null || task.isCancelled() || task.isDone()) {
                  this._arriveTasks.put(npc, ThreadPool.schedule(new ArrivedTask(npc, walk), 100 + node.getDelay() * 1000));
               }
            }
         }
      }
   }

   public void onDeath(Npc npc) {
      this.cancelMoving(npc);
   }

   public void onSpawn(Npc npc) {
      if (this._routesToAttach.containsKey(npc.getId())) {
         String routeName = this._routesToAttach.get(npc.getId()).getRouteName(npc);
         if (!routeName.isEmpty()) {
            this.startMoving(npc, routeName);
         }
      }
   }

   public static WalkingManager getInstance() {
      return WalkingManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final WalkingManager INSTANCE = new WalkingManager();
   }
}
