package org.l2jmobius.gameserver.mechanics.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.entity.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.Ids;
import org.l2jmobius.gameserver.mechanics.events.annotations.NpcLevelRange;
import org.l2jmobius.gameserver.mechanics.events.annotations.NpcLevelRanges;
import org.l2jmobius.gameserver.mechanics.events.annotations.Priority;
import org.l2jmobius.gameserver.mechanics.events.annotations.Range;
import org.l2jmobius.gameserver.mechanics.events.annotations.Ranges;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneExit;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveRouteFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableHate;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.trap.OnTrapAction;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemBypassEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.AnnotationEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.DummyEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.RunnableEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.script.timers.IEventTimerCancel;
import org.l2jmobius.gameserver.mechanics.script.timers.IEventTimerEvent;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerExecutor;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

public class Quest implements IEventTimerEvent<String>, IEventTimerCancel<String> {
   public static final Logger LOGGER = Logger.getLogger(Quest.class.getName());
   private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
   private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";
   private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
   private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
   private final Map<ListenerRegisterType, Set<Integer>> _registeredIds = new ConcurrentHashMap<>();
   private final Queue<AbstractEventListener> _listeners = new PriorityBlockingQueue<>();
   private final Map<Predicate<Player>, String> _startCondition = new LinkedHashMap<>(1);
   private final Map<String, List<QuestTimer>> _questTimers = new HashMap<>();
   private TimerExecutor<String> _timerExecutor;
   private final int _questId;
   private final String _description;
   private final Path _scriptFile;
   private boolean _isCustom = false;
   public int[] _questItemIds = null;

   public Quest(int questId, String description) {
      this._scriptFile = Path.of(ScriptEngine.getInstance().getCurrentLoadingScript().toUri());
      this.initializeAnnotationListeners();
      this._questId = questId;
      this._description = description;
      if (questId > 0) {
         ScriptManager.getInstance().addQuest(this);
      } else {
         ScriptManager.getInstance().addScript(this);
      }

      this.onLoad();
   }

   @Override
   public void onTimerEvent(TimerHolder<String> holder) {
      this.onTimerEvent(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
   }

   @Override
   public void onTimerCancel(TimerHolder<String> holder) {
      this.onTimerCancel(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
   }

   public void onTimerEvent(String event, StatSet params, Npc npc, Player player) {
      LOGGER.warning(
         "["
            + this.getClass().getSimpleName()
            + "]: Timer event arrived at non overriden onTimerEvent method event: "
            + event
            + " npc: "
            + npc
            + " player: "
            + player
      );
   }

   public void onTimerCancel(String event, StatSet params, Npc npc, Player player) {
   }

   public TimerExecutor<String> getTimers() {
      if (this._timerExecutor == null) {
         synchronized (this) {
            if (this._timerExecutor == null) {
               this._timerExecutor = new TimerExecutor<>(this, this);
            }
         }
      }

      return this._timerExecutor;
   }

   public boolean hasTimers() {
      return this._timerExecutor != null;
   }

   public int getId() {
      return this._questId;
   }

   public String getName() {
      return this.getClass().getSimpleName();
   }

   public String getDescription() {
      return this._description;
   }

   public Path getScriptFile() {
      return this._scriptFile;
   }

   public String getPath() {
      String path = this.getClass().getName().replace('.', '/');
      return path.substring(0, path.lastIndexOf("/" + this.getClass().getSimpleName()));
   }

   public boolean isCustomQuest() {
      return this._isCustom;
   }

   public void setCustom(boolean value) {
      this._isCustom = value;
   }

   public int getNpcStringId() {
      return this._questId > 10000 ? this._questId - 5000 : this._questId;
   }

   public void reload() {
      this.unload();

      try {
         ScriptEngine.getInstance().executeScript(this._scriptFile);
      } catch (Exception var2) {
         LOGGER.log(Level.WARNING, "Failed to reload script!", (Throwable)var2);
      }
   }

   public void unload() {
      this.unload(true);
   }

   public void unload(boolean removeFromList) {
      this.onSave();

      for (List<QuestTimer> timers : this._questTimers.values()) {
         for (QuestTimer timer : timers) {
            timer.cancel();
         }

         timers.clear();
      }

      this._questTimers.clear();
      if (removeFromList) {
         ScriptManager.getInstance().removeScript(this);
      }

      this._listeners.forEach(AbstractEventListener::unregisterMe);
      this._listeners.clear();
      if (this._timerExecutor != null) {
         this._timerExecutor.cancelAllTimers();
      }
   }

   protected void onLoad() {
   }

   public void onSave() {
   }

   public QuestState newQuestState(Player player) {
      return new QuestState(this, player, (byte)0);
   }

   public QuestState getQuestState(Player player, boolean initIfNone) {
      QuestState qs = player.getQuestState(this.getName());
      return qs == null && initIfNone ? this.newQuestState(player) : qs;
   }

   public void startQuestTimer(String name, long time, Npc npc, Player player) {
      this.startQuestTimer(name, time, npc, player, false);
   }

   public Map<String, List<QuestTimer>> getQuestTimers() {
      return this._questTimers;
   }

   public void startQuestTimer(String name, long time, Npc npc, Player player, boolean repeating) {
      if (name != null) {
         synchronized (this._questTimers) {
            this._questTimers.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>());
            if (this.getQuestTimer(name, npc, player) == null) {
               this._questTimers.get(name).add(new QuestTimer(this, name, time, npc, player, repeating));
            }
         }
      }
   }

   public QuestTimer getQuestTimer(String name, Npc npc, Player player) {
      if (name == null) {
         return null;
      } else {
         List<QuestTimer> timers = this._questTimers.get(name);
         if (timers != null && !timers.isEmpty()) {
            for (QuestTimer timer : timers) {
               if (timer != null && timer.equals(this, name, npc, player)) {
                  return timer;
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   public void cancelQuestTimers(String name) {
      if (name != null) {
         List<QuestTimer> timers = this._questTimers.get(name);
         if (timers != null && !timers.isEmpty()) {
            for (QuestTimer timer : timers) {
               if (timer != null) {
                  timer.cancel();
               }
            }

            timers.clear();
         }
      }
   }

   public void cancelQuestTimer(String name, Npc npc, Player player) {
      if (name != null) {
         List<QuestTimer> timers = this._questTimers.get(name);
         if (timers != null && !timers.isEmpty()) {
            for (QuestTimer timer : timers) {
               if (timer != null && timer.equals(this, name, npc, player)) {
                  timer.cancel();
               }
            }
         }
      }
   }

   public void removeQuestTimer(QuestTimer timer) {
      if (timer != null) {
         List<QuestTimer> timers = this._questTimers.get(timer.toString());
         if (timers != null) {
            timers.remove(timer);
         }
      }
   }

   public void notifyTeleport(Npc npc) {
      try {
         this.onTeleport(npc);
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on onTeleport() in notifyTeleport(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void notifyEvent(String event, Npc npc, Player player) {
      String res = null;

      try {
         res = this.onEvent(event, npc, player);
      } catch (Exception var6) {
         this.showError(player, var6);
         return;
      }

      this.showResult(player, res, npc);
   }

   public void notifyTalk(Npc npc, Player player) {
      String res = null;

      try {
         String startConditionHtml = this.getStartConditionHtml(player);
         if (!player.hasQuestState(this.getName()) && startConditionHtml != null) {
            res = startConditionHtml;
         } else {
            res = this.onTalk(npc, player);
         }
      } catch (Exception var5) {
         this.showError(player, var5);
         return;
      }

      player.setLastQuestNpcObject(npc.getObjectId());
      this.showResult(player, res, npc);
   }

   public void notifyFirstTalk(Npc npc, Player player) {
      String res = null;

      try {
         res = this.onFirstTalk(npc, player);
      } catch (Exception var5) {
         this.showError(player, var5);
         return;
      }

      this.showResult(player, res, npc);
   }

   public void notifyItemTalk(Item item, Player player) {
      String res = null;

      try {
         res = this.onItemTalk(item, player);
      } catch (Exception var5) {
         this.showError(player, var5);
         return;
      }

      this.showResult(player, res);
   }

   public String onItemTalk(Item item, Player player) {
      return null;
   }

   public void notifyEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference) {
      try {
         this.onEventReceived(eventName, sender, receiver, reference);
      } catch (Exception var6) {
         LOGGER.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + var6.getMessage(), (Throwable)var6);
      }
   }

   public void notifyMoveFinished(Npc npc) {
      try {
         this.onMoveFinished(npc);
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void notifyNodeArrived(Npc npc) {
      try {
         this.onNodeArrived(npc);
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void notifyRouteFinished(Npc npc) {
      try {
         this.onRouteFinished(npc);
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on onRouteFinished() in notifyRouteFinished(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public boolean notifyOnCanSeeMe(Npc npc, Player player) {
      try {
         return this.onCanSeeMe(npc, player);
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, "Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + var4.getMessage(), (Throwable)var4);
         return false;
      }
   }

   public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon) {
   }

   public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill) {
      this.onAttack(npc, attacker, damage, isSummon);
   }

   public void onDeath(Creature killer, Creature victim, QuestState qs) {
      this.onEvent("", killer instanceof Npc ? killer.asNpc() : null, qs.getPlayer());
   }

   public String onEvent(String event, Npc npc, Player player) {
      return null;
   }

   public void onKill(Npc npc, Player killer, boolean isSummon) {
   }

   public String onTalk(Npc npc, Player talker) {
      return null;
   }

   public String onFirstTalk(Npc npc, Player player) {
      return null;
   }

   public void onItemEvent(Item item, Player player, String event) {
   }

   public void onAcquireSkillList(Npc npc, Player player) {
   }

   public void onAcquireSkillInfo(Npc npc, Player player, Skill skill) {
   }

   public void onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type) {
   }

   public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon) {
   }

   public void onSpellFinished(Npc npc, Player player, Skill skill) {
   }

   public void onTrapAction(Trap trap, Creature trigger, TrapAction action) {
   }

   public void onSpawn(Npc npc) {
   }

   protected void onTeleport(Npc npc) {
   }

   public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon) {
   }

   public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon) {
   }

   public void onCreatureSee(Npc npc, Creature creature) {
   }

   public void onEnterWorld(Player player) {
   }

   public void onEnterZone(Creature creature, ZoneType zone) {
   }

   public void onExitZone(Creature creature, ZoneType zone) {
   }

   public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference) {
      return null;
   }

   public void onMoveFinished(Npc npc) {
   }

   public void onNodeArrived(Npc npc) {
   }

   public void onRouteFinished(Npc npc) {
   }

   public boolean onNpcHate(Attackable mob, Player player, boolean isSummon) {
      return true;
   }

   public void onSummonSpawn(Summon summon) {
   }

   public void onSummonTalk(Summon summon) {
   }

   public boolean onCanSeeMe(Npc npc, Player player) {
      return false;
   }

   public boolean showError(Player player, Throwable t) {
      LOGGER.log(Level.WARNING, this.getScriptFile().toAbsolutePath().toString(), t);
      if (t.getMessage() == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": " + t.getMessage());
      }

      if (player != null && player.getAccessLevel().isGm()) {
         String res = "<html><body><title>Script error</title>" + TraceUtil.getStackTrace(t) + "</body></html>";
         return this.showResult(player, res);
      } else {
         return false;
      }
   }

   public boolean showResult(Player player, String res) {
      return this.showResult(player, res, null);
   }

   public boolean showResult(Player player, String res, Npc npc) {
      if (res != null && !res.isEmpty() && player != null) {
         if (res.endsWith(".htm") || res.endsWith(".html")) {
            this.showHtmlFile(player, res, npc);
         } else if (res.startsWith("<html")) {
            NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, res);
            npcReply.replace("%playername%", player.getName());
            player.sendPacket(npcReply);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            player.sendMessage(res);
         }

         return false;
      } else {
         return true;
      }
   }

   public static void playerEnter(Player player) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
         PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
         PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?");
      ) {
         ps1.setInt(1, player.getObjectId());
         ps1.setString(2, "<state>");

         try (ResultSet rs = ps1.executeQuery()) {
            while (rs.next()) {
               String questId = rs.getString("name");
               String statename = rs.getString("value");
               Quest q = ScriptManager.getInstance().getScript(questId);
               if (q == null) {
                  LOGGER.finer("Unknown quest " + questId + " for " + player);
                  if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA) {
                     invalidQuestData.setInt(1, player.getObjectId());
                     invalidQuestData.setString(2, questId);
                     invalidQuestData.executeUpdate();
                  }
               } else {
                  new QuestState(q, player, State.getStateId(statename));
               }
            }
         }

         try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?")) {
            ps2.setInt(1, player.getObjectId());
            ps2.setString(2, "<state>");

            try (ResultSet rs = ps2.executeQuery()) {
               while (rs.next()) {
                  String questId = rs.getString("name");
                  String var = rs.getString("var");
                  String value = rs.getString("value");
                  Quest quest = ScriptManager.getInstance().getScript(questId);
                  if (quest != null) {
                     QuestState qs = quest.getQuestState(player, true);
                     if (qs == null) {
                        LOGGER.finer("Lost variable " + var + " in quest " + questId + " for " + player);
                        if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA) {
                           invalidQuestDataVar.setInt(1, player.getObjectId());
                           invalidQuestDataVar.setString(2, questId);
                           invalidQuestDataVar.setString(3, var);
                           invalidQuestDataVar.executeUpdate();
                        }
                     } else {
                        qs.setInternal(var, value);
                     }
                  }
               }
            }
         }
      } catch (Exception var26) {
         LOGGER.log(Level.WARNING, "could not insert char quest:", (Throwable)var26);
      }

      for (String name : ScriptManager.getInstance().getScripts().keySet()) {
         player.processScriptEvent(name, "enter");
      }
   }

   public static void createQuestVarInDb(QuestState qs, String var, String value) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"
         );
      ) {
         statement.setInt(1, qs.getPlayer().getObjectId());
         statement.setString(2, qs.getQuestName());
         statement.setString(3, var);
         statement.setString(4, value);
         statement.setString(5, value);
         statement.executeUpdate();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "could not insert char quest:", (Throwable)var11);
      }
   }

   public static void updateQuestVarInDb(QuestState qs, String var, String value) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?");
      ) {
         statement.setString(1, value);
         statement.setInt(2, qs.getPlayer().getObjectId());
         statement.setString(3, qs.getQuestName());
         statement.setString(4, var);
         statement.executeUpdate();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "could not update char quest:", (Throwable)var11);
      }
   }

   public static void deleteQuestVarInDb(QuestState qs, String var) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?");
      ) {
         statement.setInt(1, qs.getPlayer().getObjectId());
         statement.setString(2, qs.getQuestName());
         statement.setString(3, var);
         statement.executeUpdate();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Unable to delete char quest!", (Throwable)var10);
      }
   }

   public static void deleteQuestInDb(QuestState qs, boolean repeatable) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            repeatable ? "DELETE FROM character_quests WHERE charId=? AND name=?" : "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?"
         );
      ) {
         ps.setInt(1, qs.getPlayer().getObjectId());
         ps.setString(2, qs.getQuestName());
         if (!repeatable) {
            ps.setString(3, "<state>");
         }

         ps.executeUpdate();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "could not delete char quest:", (Throwable)var10);
      }
   }

   public static void createQuestInDb(QuestState qs) {
      createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
   }

   public static void updateQuestInDb(QuestState qs) {
      updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
   }

   public static String getNoQuestMsg(Player player) {
      String result = HtmCache.getInstance().getHtm(player, "data/html/noquest.htm");
      return result != null && !result.isEmpty()
         ? result
         : "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
   }

   public static String getAlreadyCompletedMsg(Player player) {
      String result = HtmCache.getInstance().getHtm(player, "data/html/alreadycompleted.htm");
      return result != null && !result.isEmpty() ? result : "<html><body>This quest has already been completed.</body></html>";
   }

   public void addStartNpc(int... npcIds) {
      this.setNpcQuestStartId(npcIds);
   }

   public void addStartNpc(Collection<Integer> npcIds) {
      this.setNpcQuestStartId(npcIds);
   }

   public void addFirstTalkId(int... npcIds) {
      this.setNpcFirstTalkId(event -> this.notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
   }

   public void addFirstTalkId(Collection<Integer> npcIds) {
      this.setNpcFirstTalkId(event -> this.notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
   }

   public void addAcquireSkillId(int... npcIds) {
      this.setPlayerSkillLearnId(event -> this.onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
   }

   public void addAcquireSkillId(Collection<Integer> npcIds) {
      this.setPlayerSkillLearnId(event -> this.onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
   }

   public void addItemBypassEventId(int... itemIds) {
      this.setItemBypassEvenId(event -> this.onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
   }

   public void addItemBypassEventId(Collection<Integer> itemIds) {
      this.setItemBypassEvenId(event -> this.onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
   }

   public void addItemTalkId(int... itemIds) {
      this.setItemTalkId(event -> this.notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
   }

   public void addItemTalkId(Collection<Integer> itemIds) {
      this.setItemTalkId(event -> this.notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
   }

   public void addAttackId(int... npcIds) {
      this.setAttackableAttackId(
         attack -> this.onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds
      );
   }

   public void addAttackId(Collection<Integer> npcIds) {
      this.setAttackableAttackId(
         attack -> this.onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds
      );
   }

   public void addKillId(int... npcIds) {
      this.setAttackableKillId(kill -> this.onKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
   }

   public void addKillId(Collection<Integer> npcIds) {
      this.setAttackableKillId(kill -> this.onKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
   }

   public void addTalkId(int... npcIds) {
      this.setNpcTalkId(npcIds);
   }

   public void addTalkId(Collection<Integer> npcIds) {
      this.setNpcTalkId(npcIds);
   }

   public void addTeleportId(int... npcIds) {
      this.setNpcTeleportId(event -> this.notifyTeleport(event.getNpc()), npcIds);
   }

   public void addTeleportId(Collection<Integer> npcIds) {
      this.setNpcTeleportId(event -> this.notifyTeleport(event.getNpc()), npcIds);
   }

   public void addSpawnId(int... npcIds) {
      this.setNpcSpawnId(event -> this.onSpawn(event.getNpc()), npcIds);
   }

   public void addSpawnId(Collection<Integer> npcIds) {
      this.setNpcSpawnId(event -> this.onSpawn(event.getNpc()), npcIds);
   }

   public void addSkillSeeId(int... npcIds) {
      this.setNpcSkillSeeId(event -> this.onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
   }

   public void addSkillSeeId(Collection<Integer> npcIds) {
      this.setNpcSkillSeeId(event -> this.onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
   }

   public void addSpellFinishedId(int... npcIds) {
      this.setNpcSkillFinishedId(event -> this.onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
   }

   public void addSpellFinishedId(Collection<Integer> npcIds) {
      this.setNpcSkillFinishedId(event -> this.onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
   }

   public void addTrapActionId(int... npcIds) {
      this.setTrapActionId(event -> this.onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
   }

   public void addTrapActionId(Collection<Integer> npcIds) {
      this.setTrapActionId(event -> this.onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
   }

   public void addFactionCallId(int... npcIds) {
      this.setAttackableFactionIdId(event -> this.onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
   }

   public void addFactionCallId(Collection<Integer> npcIds) {
      this.setAttackableFactionIdId(event -> this.onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
   }

   public void addAggroRangeEnterId(int... npcIds) {
      this.setAttackableAggroRangeEnterId(event -> this.onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
   }

   public void addAggroRangeEnterId(Collection<Integer> npcIds) {
      this.setAttackableAggroRangeEnterId(event -> this.onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
   }

   public void addCreatureSeeId(int... npcIds) {
      this.setCreatureSeeId(event -> this.onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
   }

   public void addCreatureSeeId(Collection<Integer> npcIds) {
      this.setCreatureSeeId(event -> this.onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
   }

   public void addEnterZoneId(int zoneId) {
      this.setCreatureZoneEnterId(event -> this.onEnterZone(event.getCreature(), event.getZone()), zoneId);
   }

   public void addEnterZoneId(int... zoneIds) {
      this.setCreatureZoneEnterId(event -> this.onEnterZone(event.getCreature(), event.getZone()), zoneIds);
   }

   public void addEnterZoneId(Collection<Integer> zoneIds) {
      this.setCreatureZoneEnterId(event -> this.onEnterZone(event.getCreature(), event.getZone()), zoneIds);
   }

   public void addExitZoneId(int zoneId) {
      this.setCreatureZoneExitId(event -> this.onExitZone(event.getCreature(), event.getZone()), zoneId);
   }

   public void addExitZoneId(int... zoneIds) {
      this.setCreatureZoneExitId(event -> this.onExitZone(event.getCreature(), event.getZone()), zoneIds);
   }

   public void addExitZoneId(Collection<Integer> zoneIds) {
      this.setCreatureZoneExitId(event -> this.onExitZone(event.getCreature(), event.getZone()), zoneIds);
   }

   public void addEventReceivedId(int... npcIds) {
      this.setNpcEventReceivedId(event -> this.notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
   }

   public void addEventReceivedId(Collection<Integer> npcIds) {
      this.setNpcEventReceivedId(event -> this.notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
   }

   public void addMoveFinishedId(int... npcIds) {
      this.setNpcMoveFinishedId(event -> this.notifyMoveFinished(event.getNpc()), npcIds);
   }

   public void addMoveFinishedId(Collection<Integer> npcIds) {
      this.setNpcMoveFinishedId(event -> this.notifyMoveFinished(event.getNpc()), npcIds);
   }

   public void addNodeArrivedId(int... npcIds) {
      this.setNpcMoveNodeArrivedId(event -> this.notifyNodeArrived(event.getNpc()), npcIds);
   }

   public void addNodeArrivedId(Collection<Integer> npcIds) {
      this.setNpcMoveNodeArrivedId(event -> this.notifyNodeArrived(event.getNpc()), npcIds);
   }

   public void addRouteFinishedId(int... npcIds) {
      this.setNpcMoveRouteFinishedId(event -> this.notifyRouteFinished(event.getNpc()), npcIds);
   }

   public void addRouteFinishedId(Collection<Integer> npcIds) {
      this.setNpcMoveRouteFinishedId(event -> this.notifyRouteFinished(event.getNpc()), npcIds);
   }

   public void addNpcHateId(int... npcIds) {
      this.addNpcHateId(event -> new TerminateReturn(!this.onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
   }

   public void addNpcHateId(Collection<Integer> npcIds) {
      this.addNpcHateId(event -> new TerminateReturn(!this.onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
   }

   public void addSummonSpawnId(int... npcIds) {
      this.setPlayerSummonSpawnId(event -> this.onSummonSpawn(event.getSummon()), npcIds);
   }

   public void addSummonSpawnId(Collection<Integer> npcIds) {
      this.setPlayerSummonSpawnId(event -> this.onSummonSpawn(event.getSummon()), npcIds);
   }

   public void addSummonTalkId(int... npcIds) {
      this.setPlayerSummonTalkId(event -> this.onSummonTalk(event.getSummon()), npcIds);
   }

   public void addSummonTalkId(Collection<Integer> npcIds) {
      this.setPlayerSummonTalkId(event -> this.onSummonTalk(event.getSummon()), npcIds);
   }

   public void addCanSeeMeId(int... npcIds) {
      this.addNpcHateId(event -> new TerminateReturn(!this.notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
   }

   public void addCanSeeMeId(Collection<Integer> npcIds) {
      this.addNpcHateId(event -> new TerminateReturn(!this.notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
   }

   public static Player getRandomPartyMember(Player player) {
      if (player == null) {
         return null;
      } else {
         Party party = player.getParty();
         if (party == null) {
            return player;
         } else {
            List<Player> members = party.getMembers();
            if (members.isEmpty()) {
               return player;
            } else {
               Player member = members.get(Rnd.get(members.size()));

               while (player.getInstanceId() != member.getInstanceId()) {
                  member = members.get(Rnd.get(members.size()));
               }

               return member;
            }
         }
      }
   }

   public Player getRandomPartyMember(Player player, int cond) {
      return this.getRandomPartyMember(player, "cond", String.valueOf(cond));
   }

   public Player getRandomPartyMember(Player player, String var, String value) {
      if (player == null) {
         return null;
      } else if (var == null) {
         return getRandomPartyMember(player);
      } else {
         QuestState temp = null;
         Party party = player.getParty();
         if (party != null && !party.getMembers().isEmpty()) {
            List<Player> candidates = new ArrayList<>();
            WorldObject target = player.getTarget();
            if (target == null) {
               target = player;
            }

            for (Player partyMember : party.getMembers()) {
               if (partyMember != null) {
                  temp = partyMember.getQuestState(this.getName());
                  if (temp != null
                     && temp.get(var) != null
                     && temp.get(var).equalsIgnoreCase(value)
                     && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE)
                     && player.getInstanceId() == partyMember.getInstanceId()) {
                     candidates.add(partyMember);
                  }
               }
            }

            return candidates.isEmpty() ? null : candidates.get(Rnd.get(candidates.size()));
         } else {
            temp = player.getQuestState(this.getName());
            return temp != null && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value) ? player : null;
         }
      }
   }

   public Player getRandomPartyMemberState(Player player, byte state) {
      if (player == null) {
         return null;
      } else {
         QuestState temp = null;
         Party party = player.getParty();
         if (party != null && !party.getMembers().isEmpty()) {
            List<Player> candidates = new ArrayList<>();
            WorldObject target = player.getTarget();
            if (target == null) {
               target = player;
            }

            for (Player partyMember : party.getMembers()) {
               if (partyMember != null) {
                  temp = partyMember.getQuestState(this.getName());
                  if (temp != null
                     && temp.getState() == state
                     && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE)
                     && player.getInstanceId() == partyMember.getInstanceId()) {
                     candidates.add(partyMember);
                  }
               }
            }

            return candidates.isEmpty() ? null : candidates.get(Rnd.get(candidates.size()));
         } else {
            temp = player.getQuestState(this.getName());
            return temp != null && temp.getState() == state ? player : null;
         }
      }
   }

   public Player getRandomPartyMember(Player player, Npc npc) {
      if (player != null && checkDistanceToTarget(player, npc)) {
         Party party = player.getParty();
         Player luckyPlayer = null;
         if (party == null) {
            if (this.checkPartyMember(player, npc)) {
               luckyPlayer = player;
            }
         } else {
            int highestRoll = 0;

            for (Player member : party.getMembers()) {
               int rnd = getRandom(1000);
               if (rnd > highestRoll && this.checkPartyMember(member, npc)) {
                  highestRoll = rnd;
                  luckyPlayer = member;
               }
            }
         }

         return luckyPlayer != null && checkDistanceToTarget(luckyPlayer, npc) ? luckyPlayer : null;
      } else {
         return null;
      }
   }

   public boolean checkPartyMember(Player player, Npc npc) {
      return true;
   }

   public QuestState getRandomPartyMemberState(Player player, int condition, int playerChance, Npc target) {
      if (player != null && playerChance >= 1) {
         QuestState qs = player.getQuestState(this.getName());
         if (!player.isInParty()) {
            return this.checkPartyMemberConditions(qs, condition, target) && checkDistanceToTarget(player, target) ? qs : null;
         } else {
            List<QuestState> candidates = new ArrayList<>();
            if (this.checkPartyMemberConditions(qs, condition, target) && playerChance > 0) {
               for (int i = 0; i < playerChance; i++) {
                  candidates.add(qs);
               }
            }

            for (Player member : player.getParty().getMembers()) {
               if (member != player) {
                  qs = member.getQuestState(this.getName());
                  if (this.checkPartyMemberConditions(qs, condition, target)) {
                     candidates.add(qs);
                  }
               }
            }

            if (candidates.isEmpty()) {
               return null;
            } else {
               qs = candidates.get(getRandom(candidates.size()));
               return !checkDistanceToTarget(qs.getPlayer(), target) ? null : qs;
            }
         }
      } else {
         return null;
      }
   }

   private boolean checkPartyMemberConditions(QuestState qs, int condition, Npc npc) {
      return qs != null && (condition == -1 ? qs.isStarted() : qs.isCond(condition)) && this.checkPartyMember(qs, npc);
   }

   private static boolean checkDistanceToTarget(Player player, Npc target) {
      return target == null || LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, player, target, true);
   }

   public boolean checkPartyMember(QuestState qs, Npc npc) {
      return true;
   }

   public String showHtmlFile(Player player, String filename) {
      return this.showHtmlFile(player, filename, null);
   }

   public String showHtmlFile(Player player, String filename, Npc npc) {
      boolean questwindow = !filename.endsWith(".html");
      String content = this.getHtm(player, filename);
      if (content != null) {
         if (npc != null) {
            content = content.replace("%objectId%", String.valueOf(npc.getObjectId()));
         }

         if (questwindow && this._questId > 0 && this._questId < 20000 && this._questId != 999) {
            NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, this._questId);
            npcReply.setHtml(content);
            npcReply.replace("%playername%", player.getName());
            player.sendPacket(npcReply);
         } else {
            NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
            npcReply.replace("%playername%", player.getName());
            player.sendPacket(npcReply);
         }

         player.sendPacket(ActionFailed.STATIC_PACKET);
      }

      return content;
   }

   public String getHtm(Player player, String fileName) {
      HtmCache hc = HtmCache.getInstance();
      String content = hc.getHtm(player, fileName.startsWith("data/") ? fileName : "data/scripts/" + this.getPath() + "/" + fileName);
      if (content == null) {
         content = hc.getHtm(player, "data/scripts/" + this.getPath() + "/" + fileName);
         if (content == null) {
            content = hc.getHtm(player, "data/scripts/quests/" + this.getName() + "/" + fileName);
         }
      }

      return content;
   }

   public int[] getRegisteredItemIds() {
      return this._questItemIds;
   }

   public void registerQuestItems(int... items) {
      for (int id : items) {
         if (id != 0 && ItemData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found registerQuestItems for non existing item: " + id + "!");
         }
      }

      this._questItemIds = items;
   }

   public void removeRegisteredQuestItems(Player player) {
      takeItems(player, -1, this._questItemIds);
   }

   public void setOnEnterWorld(boolean value) {
      if (value) {
         this.setPlayerLoginId(event -> this.onEnterWorld(event.getPlayer()));
      } else {
         for (AbstractEventListener listener : this.getListeners()) {
            if (listener.getType() == EventType.ON_PLAYER_LOGIN) {
               listener.unregisterMe();
            }
         }
      }
   }

   private Map<Predicate<Player>, String> getStartConditions() {
      return this._startCondition;
   }

   public boolean canStartQuest(Player player) {
      for (Predicate<Player> cond : this._startCondition.keySet()) {
         if (!cond.test(player)) {
            return false;
         }
      }

      return true;
   }

   public String getStartConditionHtml(Player player) {
      for (Entry<Predicate<Player>, String> startRequirement : this._startCondition.entrySet()) {
         if (!startRequirement.getKey().test(player)) {
            return startRequirement.getValue();
         }
      }

      return null;
   }

   public void addCondStart(Predicate<Player> questStartRequirement, String html) {
      this.getStartConditions().put(questStartRequirement, html);
   }

   public void addCondLevel(int minLevel, int maxLevel, String html) {
      this.getStartConditions().put(p -> p.getLevel() >= minLevel && p.getLevel() <= maxLevel, html);
   }

   public void addCondMinLevel(int minLevel, String html) {
      this.getStartConditions().put(p -> p.getLevel() >= minLevel, html);
   }

   public void addCondMaxLevel(int maxLevel, String html) {
      this.getStartConditions().put(p -> p.getLevel() <= maxLevel, html);
   }

   public void addCondRace(Race race, String html) {
      this.getStartConditions().put(p -> p.getRace() == race, html);
   }

   public void addCondNotRace(Race race, String html) {
      this.getStartConditions().put(p -> p.getRace() != race, html);
   }

   public void addCondCompletedQuest(String name, String html) {
      this.getStartConditions().put(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
   }

   public void addCondClassId(PlayerClass playerClass, String html) {
      this.getStartConditions().put(p -> p.getPlayerClass() == playerClass, html);
   }

   public void addCondNotClassId(PlayerClass playerClass, String html) {
      this.getStartConditions().put(p -> p.getPlayerClass() != playerClass, html);
   }

   public void addCondIsSubClassActive(String html) {
      this.getStartConditions().put(p -> p.isSubClassActive(), html);
   }

   public void addCondIsNotSubClassActive(String html) {
      this.getStartConditions().put(p -> !p.isSubClassActive(), html);
   }

   public void addCondInCategory(CategoryType categoryType, String html) {
      this.getStartConditions().put(p -> p.isInCategory(categoryType), html);
   }

   private void initializeAnnotationListeners() {
      List<Integer> ids = new ArrayList<>();

      for (Method method : this.getClass().getMethods()) {
         if (method.isAnnotationPresent(RegisterEvent.class) && method.isAnnotationPresent(RegisterType.class)) {
            RegisterEvent listener = method.getAnnotation(RegisterEvent.class);
            EventType eventType = listener.value();
            if (method.getParameterCount() != 1) {
               LOGGER.warning(
                  this.getClass().getSimpleName()
                     + ": Non properly defined annotation listener on method: "
                     + method.getName()
                     + " expected parameter count is 1 but found: "
                     + method.getParameterCount()
               );
            } else if (!eventType.isEventClass(method.getParameterTypes()[0])) {
               LOGGER.warning(
                  this.getClass().getSimpleName()
                     + ": Non properly defined annotation listener on method: "
                     + method.getName()
                     + " expected parameter to be type of: "
                     + eventType.getEventClass().getSimpleName()
                     + " but found: "
                     + method.getParameterTypes()[0].getSimpleName()
               );
            } else if (!eventType.isReturnClass(method.getReturnType())) {
               LOGGER.warning(
                  this.getClass().getSimpleName()
                     + ": Non properly defined annotation listener on method: "
                     + method.getName()
                     + " expected return type to be one of: "
                     + Arrays.toString((Object[])eventType.getReturnClasses())
                     + " but found: "
                     + method.getReturnType().getSimpleName()
               );
            } else {
               RegisterType regType = method.getAnnotation(RegisterType.class);
               ListenerRegisterType type = regType.value();
               int priority = 0;
               ids.clear();

               for (Annotation annotation : method.getAnnotations()) {
                  if (annotation instanceof Id npc) {
                     for (int id : npc.value()) {
                        ids.add(id);
                     }
                  } else if (annotation instanceof Ids npcs) {
                     for (Id npc : npcs.value()) {
                        for (int id : npc.value()) {
                           ids.add(id);
                        }
                     }
                  } else if (annotation instanceof Range range) {
                     if (range.from() > range.to()) {
                        LOGGER.warning(this.getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
                     } else {
                        for (int id = range.from(); id <= range.to(); id++) {
                           ids.add(id);
                        }
                     }
                  } else if (annotation instanceof Ranges ranges) {
                     for (Range rangex : ranges.value()) {
                        if (rangex.from() > rangex.to()) {
                           LOGGER.warning(this.getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
                        } else {
                           for (int id = rangex.from(); id <= rangex.to(); id++) {
                              ids.add(id);
                           }
                        }
                     }
                  } else if (annotation instanceof NpcLevelRange rangexx) {
                     if (rangexx.from() > rangexx.to()) {
                        LOGGER.warning(this.getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
                     } else if (type != ListenerRegisterType.NPC) {
                        LOGGER.warning(
                           this.getClass().getSimpleName()
                              + ": ListenerRegisterType "
                              + type
                              + " for "
                              + annotation.getClass().getSimpleName()
                              + " NPC is expected!"
                        );
                     } else {
                        for (int level = rangexx.from(); level <= rangexx.to(); level++) {
                           List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
                           templates.forEach(template -> ids.add(template.getId()));
                        }
                     }
                  } else if (!(annotation instanceof NpcLevelRanges ranges)) {
                     if (annotation instanceof Priority p) {
                        priority = p.value();
                     }
                  } else {
                     for (NpcLevelRange rangexxx : ranges.value()) {
                        if (rangexxx.from() > rangexxx.to()) {
                           LOGGER.warning(this.getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
                        } else if (type != ListenerRegisterType.NPC) {
                           LOGGER.warning(
                              this.getClass().getSimpleName()
                                 + ": ListenerRegisterType "
                                 + type
                                 + " for "
                                 + annotation.getClass().getSimpleName()
                                 + " NPC is expected!"
                           );
                        } else {
                           for (int level = rangexxx.from(); level <= rangexxx.to(); level++) {
                              List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
                              templates.forEach(template -> ids.add(template.getId()));
                           }
                        }
                     }
                  }
               }

               if (!ids.isEmpty()) {
                  this._registeredIds.computeIfAbsent(type, var0 -> ConcurrentHashMap.newKeySet()).addAll(ids);
               }

               this.registerAnnotation(method, eventType, type, priority, ids);
            }
         }
      }
   }

   protected List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> addCreatureKillId(Function<OnCreatureDeath, ? extends AbstractEventReturn> callback, int... npcIds) {
      return this.registerFunction(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcTalkId(Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
         }
      }

      return this.registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcTalkId(int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
         }
      }

      return this.registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcQuestStartId(int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
         }
      }

      return this.registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcQuestStartId(Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
         }
      }

      return this.registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, int... npcIds) {
      return this.registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, Collection<Integer> npcIds) {
      return this.registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, int... npcIds) {
      return this.registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, Collection<Integer> npcIds) {
      return this.registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, int... npcIds) {
      for (int id : npcIds) {
         Npc.addCreatureSeeId(id);
      }

      return this.registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         Npc.addCreatureSeeId(id);
      }

      return this.registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableFactionIdId(Consumer<OnAttackableFactionCall> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableFactionIdId(Consumer<OnAttackableFactionCall> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, int... npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, Collection<Integer> npcIds) {
      for (int id : npcIds) {
         if (NpcData.getInstance().getTemplate(id) == null) {
            LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
         }
      }

      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonTalk> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setPlayerLoginId(Consumer<OnPlayerLogin> callback) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_LOGIN, ListenerRegisterType.GLOBAL);
   }

   protected List<AbstractEventListener> setPlayerLogoutId(Consumer<OnPlayerLogout> callback) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_LOGOUT, ListenerRegisterType.GLOBAL);
   }

   protected List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
   }

   protected List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
   }

   protected List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
   }

   protected List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
   }

   protected List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
   }

   protected List<AbstractEventListener> setItemBypassEvenId(Consumer<OnItemBypassEvent> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
   }

   protected List<AbstractEventListener> setItemBypassEvenId(Consumer<OnItemBypassEvent> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
   }

   protected List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, int... npcIds) {
      return this.registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
   }

   protected List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, Collection<Integer> npcIds) {
      return this.registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
   }

   protected List<AbstractEventListener> setOlympiadMatchResult(Consumer<OnOlympiadMatchResult> callback) {
      return this.registerConsumer(callback, EventType.ON_OLYMPIAD_MATCH_RESULT, ListenerRegisterType.OLYMPIAD);
   }

   protected List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, int... castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, Collection<Integer> castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, int... castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, Collection<Integer> castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, int... castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, Collection<Integer> castleIds) {
      return this.registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
   }

   protected List<AbstractEventListener> setPlayerProfessionChangeId(Consumer<OnPlayerProfessionChange> callback) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CHANGE, ListenerRegisterType.GLOBAL);
   }

   protected List<AbstractEventListener> setPlayerProfessionCancelId(Consumer<OnPlayerProfessionCancel> callback) {
      return this.registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CANCEL, ListenerRegisterType.GLOBAL);
   }

   protected List<AbstractEventListener> registerConsumer(
      Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, int... npcIds
   ) {
      return this.registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerConsumer(
      Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds
   ) {
      return this.registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerFunction(
      Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, int... npcIds
   ) {
      return this.registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerFunction(
      Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds
   ) {
      return this.registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, int... npcIds) {
      return this.registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds) {
      return this.registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerAnnotation(Method callback, EventType type, ListenerRegisterType registerType, int priority, int... npcIds) {
      return this.registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerAnnotation(
      Method callback, EventType type, ListenerRegisterType registerType, int priority, Collection<Integer> npcIds
   ) {
      return this.registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, int... npcIds) {
      return this.registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds) {
      return this.registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
   }

   protected List<AbstractEventListener> registerListener(
      Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, int... ids
   ) {
      List<AbstractEventListener> listeners = new ArrayList<>(ids.length > 0 ? ids.length : 1);
      if (ids.length > 0) {
         for (int id : ids) {
            switch (registerType) {
               case NPC:
                  NpcTemplate templatexxxx = NpcData.getInstance().getTemplate(id);
                  if (templatexxxx != null) {
                     listeners.add(templatexxxx.addListener(action.apply(templatexxxx)));
                  }
                  break;
               case ZONE:
                  ZoneType templatexxx = ZoneManager.getInstance().getZoneById(id);
                  if (templatexxx != null) {
                     listeners.add(templatexxx.addListener(action.apply(templatexxx)));
                  }
                  break;
               case ITEM:
                  ItemTemplate templatexx = ItemData.getInstance().getTemplate(id);
                  if (templatexx != null) {
                     listeners.add(templatexx.addListener(action.apply(templatexx)));
                  }
                  break;
               case CASTLE:
                  Castle templatex = CastleManager.getInstance().getCastleById(id);
                  if (templatex != null) {
                     listeners.add(templatex.addListener(action.apply(templatex)));
                  }
                  break;
               case FORTRESS:
                  Fort template = FortManager.getInstance().getFortById(id);
                  if (template != null) {
                     listeners.add(template.addListener(action.apply(template)));
                  }
                  break;
               default:
                  LOGGER.warning(this.getClass().getSimpleName() + ": Unhandled register type: " + registerType);
            }

            this._registeredIds.computeIfAbsent(registerType, var0 -> ConcurrentHashMap.newKeySet()).add(id);
         }
      } else {
         switch (registerType) {
            case OLYMPIAD:
               Olympiad templatexxxx = Olympiad.getInstance();
               listeners.add(templatexxxx.addListener(action.apply(templatexxxx)));
               break;
            case GLOBAL:
               ListenersContainer templatexxxxx = Containers.Global();
               listeners.add(templatexxxxx.addListener(action.apply(templatexxxxx)));
               break;
            case GLOBAL_NPCS:
               ListenersContainer templatexxxxxx = Containers.Npcs();
               listeners.add(templatexxxxxx.addListener(action.apply(templatexxxxxx)));
               break;
            case GLOBAL_MONSTERS:
               ListenersContainer templatexxxxxxx = Containers.Monsters();
               listeners.add(templatexxxxxxx.addListener(action.apply(templatexxxxxxx)));
               break;
            case GLOBAL_PLAYERS:
               ListenersContainer templatexxxxxxxx = Containers.Players();
               listeners.add(templatexxxxxxxx.addListener(action.apply(templatexxxxxxxx)));
         }
      }

      this._listeners.addAll(listeners);
      return listeners;
   }

   protected List<AbstractEventListener> registerListener(
      Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, Collection<Integer> ids
   ) {
      List<AbstractEventListener> listeners = new ArrayList<>(!ids.isEmpty() ? ids.size() : 1);
      if (!ids.isEmpty()) {
         for (int id : ids) {
            switch (registerType) {
               case NPC:
                  NpcTemplate templatexxxx = NpcData.getInstance().getTemplate(id);
                  if (templatexxxx != null) {
                     listeners.add(templatexxxx.addListener(action.apply(templatexxxx)));
                  }
                  break;
               case ZONE:
                  ZoneType templatexxx = ZoneManager.getInstance().getZoneById(id);
                  if (templatexxx != null) {
                     listeners.add(templatexxx.addListener(action.apply(templatexxx)));
                  }
                  break;
               case ITEM:
                  ItemTemplate templatexx = ItemData.getInstance().getTemplate(id);
                  if (templatexx != null) {
                     listeners.add(templatexx.addListener(action.apply(templatexx)));
                  }
                  break;
               case CASTLE:
                  Castle templatex = CastleManager.getInstance().getCastleById(id);
                  if (templatex != null) {
                     listeners.add(templatex.addListener(action.apply(templatex)));
                  }
                  break;
               case FORTRESS:
                  Fort template = FortManager.getInstance().getFortById(id);
                  if (template != null) {
                     listeners.add(template.addListener(action.apply(template)));
                  }
                  break;
               default:
                  LOGGER.warning(this.getClass().getSimpleName() + ": Unhandled register type: " + registerType);
            }
         }

         this._registeredIds.computeIfAbsent(registerType, var0 -> ConcurrentHashMap.newKeySet()).addAll(ids);
      } else {
         switch (registerType) {
            case OLYMPIAD:
               Olympiad templatexxxx = Olympiad.getInstance();
               listeners.add(templatexxxx.addListener(action.apply(templatexxxx)));
               break;
            case GLOBAL:
               ListenersContainer templatexxxxx = Containers.Global();
               listeners.add(templatexxxxx.addListener(action.apply(templatexxxxx)));
               break;
            case GLOBAL_NPCS:
               ListenersContainer templatexxxxxx = Containers.Npcs();
               listeners.add(templatexxxxxx.addListener(action.apply(templatexxxxxx)));
               break;
            case GLOBAL_MONSTERS:
               ListenersContainer templatexxxxxxx = Containers.Monsters();
               listeners.add(templatexxxxxxx.addListener(action.apply(templatexxxxxxx)));
               break;
            case GLOBAL_PLAYERS:
               ListenersContainer templatexxxxxxxx = Containers.Players();
               listeners.add(templatexxxxxxxx.addListener(action.apply(templatexxxxxxxx)));
         }
      }

      this._listeners.addAll(listeners);
      return listeners;
   }

   public Set<Integer> getRegisteredIds(ListenerRegisterType type) {
      return this._registeredIds.getOrDefault(type, Collections.emptySet());
   }

   public Queue<AbstractEventListener> getListeners() {
      return this._listeners;
   }

   public static void showOnScreenMsg(Player player, String text, int time) {
      player.sendPacket(new ExShowScreenMessage(text, time));
   }

   public static void showOnScreenMsg(Player player, String text, int position, int time) {
      player.sendPacket(new ExShowScreenMessage(text, position, time));
   }

   public static void showOnScreenMsg(Player player, SystemMessageId systemMsg, int position, int time, String... params) {
      player.sendPacket(new ExShowScreenMessage(systemMsg, position, time, params));
   }

   public static Npc addSpawn(int npcId, IPositionable pos) {
      return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0L, false, 0);
   }

   public Npc addSpawn(int npcId, Location pos, int instanceId) {
      return addSpawn(npcId, pos, false, 0L, false, instanceId);
   }

   public static Npc addSpawn(Npc summoner, int npcId, IPositionable pos, boolean randomOffset, long despawnDelay) {
      return addSpawn(summoner, npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
   }

   public static Npc addSpawn(int npcId, IPositionable pos, boolean isSummonSpawn) {
      return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0L, isSummonSpawn, 0);
   }

   public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay) {
      return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
   }

   public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn) {
      return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, 0);
   }

   public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId) {
      return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, instanceId);
   }

   public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay) {
      return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, false, 0);
   }

   public static Npc addSpawn(int npcId, int x, int y, int z, int heading) {
      return addSpawn(npcId, x, y, z, heading, false, 0L, false, 0);
   }

   public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn) {
      return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
   }

   public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId) {
      return addSpawn(null, npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, instanceId);
   }

   public static Npc addSpawn(
      Npc summoner, int npcId, int xValue, int yValue, int zValue, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId
   ) {
      try {
         if (xValue == 0 && yValue == 0) {
            LOGGER.severe("addSpawn(): invalid spawn coordinates for NPC #" + npcId + "!");
            return null;
         } else {
            int x = xValue;
            int y = yValue;
            if (randomOffset) {
               int offset = Rnd.get(50, 100);
               if (Rnd.nextBoolean()) {
                  offset *= -1;
               }

               x = xValue + offset;
               offset = Rnd.get(50, 100);
               if (Rnd.nextBoolean()) {
                  offset *= -1;
               }

               y = yValue + offset;
            }

            Spawn spawn = new Spawn(npcId);
            spawn.setInstanceId(instanceId);
            spawn.setHeading(heading);
            spawn.setXYZ(x, y, zValue);
            spawn.stopRespawn();
            Npc npc = spawn.doSpawn(isSummonSpawn);
            if (despawnDelay > 0L) {
               npc.scheduleDespawn(despawnDelay);
            }

            if (summoner != null) {
               summoner.addSummonedNpc(npc);
            }

            if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS && !randomOffset && npc.isMonster()) {
               spawn.setXYZ(x, y, zValue);
               npc.setXYZ(x, y, zValue);
               if (heading > -1) {
                  npc.setHeading(heading);
               }
            }

            npc.broadcastInfo();
            return npc;
         }
      } catch (Exception var15) {
         LOGGER.warning("Could not spawn NPC #" + npcId + "; error: " + var15.getMessage());
         return null;
      }
   }

   public Trap addTrap(int trapId, int x, int y, int z, int heading, int instanceId) {
      NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(trapId);
      Trap trap = new Trap(npcTemplate, instanceId, -1);
      trap.setCurrentHp(trap.getMaxHp());
      trap.setCurrentMp(trap.getMaxMp());
      trap.setInvul(true);
      trap.setHeading(heading);
      trap.spawnMe(x, y, z);
      return trap;
   }

   public Npc addMinion(Monster master, int minionId) {
      return MinionList.spawnMinion(master, minionId);
   }

   public static long getQuestItemsCount(Player player, int itemId) {
      return player.getInventory().getInventoryItemCount(itemId, -1);
   }

   public long getQuestItemsCount(Player player, int... itemIds) {
      long count = 0L;

      for (Item item : player.getInventory().getItems()) {
         if (item != null) {
            for (int itemId : itemIds) {
               if (item.getId() == itemId) {
                  if (count + item.getCount() > Long.MAX_VALUE) {
                     return Long.MAX_VALUE;
                  }

                  count += item.getCount();
               }
            }
         }
      }

      return count;
   }

   protected static boolean hasItem(Player player, ItemHolder item) {
      return hasItem(player, item, true);
   }

   protected static boolean hasItem(Player player, ItemHolder item, boolean checkCount) {
      if (item == null) {
         return false;
      } else {
         return checkCount ? getQuestItemsCount(player, item.getId()) >= item.getCount() : hasQuestItems(player, item.getId());
      }
   }

   protected static boolean hasAllItems(Player player, boolean checkCount, ItemHolder... itemList) {
      if (itemList != null && itemList.length != 0) {
         for (ItemHolder item : itemList) {
            if (!hasItem(player, item, checkCount)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean hasQuestItems(Player player, int itemId) {
      return player.getInventory().getItemByItemId(itemId) != null;
   }

   public static boolean hasQuestItems(Player player, int... itemIds) {
      if (itemIds != null && itemIds.length != 0) {
         PlayerInventory inv = player.getInventory();

         for (int itemId : itemIds) {
            if (inv.getItemByItemId(itemId) == null) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean hasAtLeastOneQuestItem(Player player, int... itemIds) {
      PlayerInventory inv = player.getInventory();

      for (int itemId : itemIds) {
         if (inv.getItemByItemId(itemId) != null) {
            return true;
         }
      }

      return false;
   }

   public boolean ownsAtLeastOneItem(Player player, int... itemIds) {
      PlayerInventory inventory = player.getInventory();

      for (int itemId : itemIds) {
         if (inventory.getItemByItemId(itemId) != null) {
            return true;
         }
      }

      PlayerWarehouse warehouse = player.getWarehouse();

      for (int itemIdx : itemIds) {
         if (warehouse.getItemByItemId(itemIdx) != null) {
            return true;
         }
      }

      if (player.hasSummon()) {
         PetInventory petInventory = player.getSummon().getInventory();
         if (petInventory != null) {
            for (int itemIdxx : itemIds) {
               if (petInventory.getItemByItemId(itemIdxx) != null) {
                  return true;
               }
            }
         }
      }

      if (GeneralConfig.ALLOW_MAIL) {
         List<MailMessage> inbox = MailManager.getInstance().getInbox(player.getObjectId());

         for (int itemIdxxx : itemIds) {
            for (MailMessage message : inbox) {
               Mail mail = message.getAttachments();
               if (mail != null && mail.getItemByItemId(itemIdxxx) != null) {
                  return true;
               }
            }
         }

         List<MailMessage> outbox = MailManager.getInstance().getOutbox(player.getObjectId());

         for (int itemIdxxx : itemIds) {
            for (MailMessage messagex : outbox) {
               Mail mail = messagex.getAttachments();
               if (mail != null && mail.getItemByItemId(itemIdxxx) != null) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static int getEnchantLevel(Player player, int itemId) {
      Item enchantedItem = player.getInventory().getItemByItemId(itemId);
      return enchantedItem == null ? 0 : enchantedItem.getEnchantLevel();
   }

   public static void giveAdena(Player player, long count, boolean applyRates) {
      if (applyRates) {
         rewardItems(player, 57, count);
      } else {
         giveItems(player, 57, count);
      }
   }

   public static void rewardItems(Player player, ItemHolder holder) {
      rewardItems(player, holder.getId(), holder.getCount());
   }

   public static void rewardItems(Player player, int itemId, long countValue) {
      if (countValue > 0L) {
         ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
         if (item != null) {
            long count = countValue;

            try {
               if (itemId == 57) {
                  count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD_ADENA);
               } else if (RatesConfig.RATE_QUEST_REWARD_USE_MULTIPLIERS) {
                  if (item instanceof EtcItem) {
                     switch (((EtcItem)item).getItemType()) {
                        case POTION:
                           count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD_POTION);
                           break;
                        case SCRL_ENCHANT_WP:
                        case SCRL_ENCHANT_AM:
                        case SCROLL:
                           count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD_SCROLL);
                           break;
                        case RECIPE:
                           count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD_RECIPE);
                           break;
                        case MATERIAL:
                           count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD_MATERIAL);
                           break;
                        default:
                           count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD);
                     }
                  }
               } else {
                  count = (long)((float)count * RatesConfig.RATE_QUEST_REWARD);
               }
            } catch (Exception var8) {
               count = Long.MAX_VALUE;
            }

            Item itemInstance = player.getInventory().addItem(ItemProcessType.QUEST, itemId, count, player, player.getTarget());
            if (itemInstance != null) {
               sendItemGetMessage(player, itemInstance, count);
            }
         }
      }
   }

   private static void sendItemGetMessage(Player player, Item item, long count) {
      if (item.getId() == 57) {
         SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
         smsg.addLong(count);
         player.sendPacket(smsg);
      } else if (count > 1L) {
         SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
         smsg.addItemName(item);
         smsg.addLong(count);
         player.sendPacket(smsg);
      } else {
         SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
         smsg.addItemName(item);
         player.sendPacket(smsg);
      }

      StatusUpdate su = new StatusUpdate(player);
      su.addAttribute(14, player.getCurrentLoad());
      player.sendPacket(su);
   }

   public static void giveItems(Player player, int itemId, long count) {
      giveItems(player, itemId, count, 0);
   }

   protected static void giveItems(Player player, ItemHolder holder) {
      giveItems(player, holder.getId(), holder.getCount());
   }

   public static void giveItems(Player player, int itemId, long count, int enchantlevel) {
      if (count > 0L) {
         ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
         if (template != null) {
            long finalCount = count;
            if (template.isQuestItem()) {
               finalCount = (long)((float)count * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
            }

            Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, finalCount, player, player.getTarget());
            if (item != null) {
               if (enchantlevel > 0 && itemId != 57) {
                  item.setEnchantLevel(enchantlevel);
               }

               sendItemGetMessage(player, item, finalCount);
            }
         }
      }
   }

   public static void giveItems(Player player, int itemId, long count, byte attributeId, int attributeLevel) {
      if (count > 0L) {
         ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
         if (template != null) {
            long finalCount = count;
            if (template.isQuestItem()) {
               finalCount = (long)((float)count * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
            }

            Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, finalCount, player, player.getTarget());
            if (item != null) {
               if (attributeId >= 0 && attributeLevel > 0) {
                  item.setElementAttr(attributeId, attributeLevel);
                  if (item.isEquipped()) {
                     item.updateElementAttrBonus(player);
                  }

                  InventoryUpdate iu = new InventoryUpdate();
                  iu.addModifiedItem(item);
                  player.sendInventoryUpdate(iu);
               }

               sendItemGetMessage(player, item, finalCount);
            }
         }
      }
   }

   public static boolean giveItemRandomly(Player player, int itemId, long amountToGive, long limit, double dropChance, boolean playSound) {
      return giveItemRandomly(player, null, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
   }

   public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long amountToGive, long limit, double dropChance, boolean playSound) {
      return giveItemRandomly(player, npc, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
   }

   public static boolean giveItemRandomly(Player player, Npc npc, int itemId, long minAmount, long maxAmount, long limit, double dropChance, boolean playSound) {
      long currentCount = getQuestItemsCount(player, itemId);
      if (limit > 0L && currentCount >= limit) {
         return true;
      } else {
         long minAmountWithBonus = (long)((float)minAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
         long maxAmountWithBonus = (long)((float)maxAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
         double dropChanceWithBonus = dropChance;
         if (npc != null && ChampionMonstersConfig.CHAMPION_ENABLE && npc.isChampion()) {
            if (itemId != 57 && itemId != 5575) {
               dropChanceWithBonus = dropChance * ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE;
               minAmountWithBonus = (long)((float)minAmountWithBonus * ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT);
               maxAmountWithBonus = (long)((float)maxAmountWithBonus * ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT);
            } else {
               dropChanceWithBonus = dropChance * ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
               minAmountWithBonus = (long)((float)minAmountWithBonus * ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT);
               maxAmountWithBonus = (long)((float)maxAmountWithBonus * ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT);
            }
         }

         long amountToGive = minAmountWithBonus == maxAmountWithBonus ? minAmountWithBonus : Rnd.get(minAmountWithBonus, maxAmountWithBonus);
         double random = Rnd.nextDouble();
         if (dropChanceWithBonus >= random && amountToGive > 0L && player.getInventory().validateCapacityByItemId(itemId)) {
            if (limit > 0L && currentCount + amountToGive > limit) {
               amountToGive = limit - currentCount;
            }

            if (player.addItem(ItemProcessType.QUEST, itemId, amountToGive, npc, true) != null) {
               if (currentCount + amountToGive == limit) {
                  if (playSound) {
                     playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
                  }

                  return true;
               }

               if (playSound) {
                  playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
               }

               if (limit <= 0L) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static boolean takeItems(Player player, int itemId, long amount) {
      Collection<Item> items = player.getInventory().getAllItemsByItemId(itemId);
      if (amount < 0L) {
         items.forEach(ix -> takeItem(player, ix, ix.getCount()));
      } else {
         long currentCount = 0L;

         for (Item i : items) {
            long toDelete = i.getCount();
            if (currentCount + toDelete > amount) {
               toDelete = amount - currentCount;
            }

            if (toDelete > 0L) {
               takeItem(player, i, toDelete);
            }

            currentCount += toDelete;
         }
      }

      return true;
   }

   private static boolean takeItem(Player player, Item item, long toDelete) {
      if (item.isEquipped()) {
         InventoryUpdate iu = new InventoryUpdate();

         for (Item itm : player.getInventory().unEquipItemInBodySlotAndRecord(item.getTemplate().getBodyPart())) {
            iu.addModifiedItem(itm);
         }

         player.sendInventoryUpdate(iu);
         player.broadcastUserInfo();
      }

      return player.destroyItemByItemId(ItemProcessType.QUEST, item.getId(), toDelete, player, true);
   }

   protected static boolean takeItem(Player player, ItemHolder holder) {
      return holder != null && takeItems(player, holder.getId(), holder.getCount());
   }

   protected static boolean takeAllItems(Player player, ItemHolder... itemList) {
      if (itemList != null && itemList.length != 0) {
         if (!hasAllItems(player, true, itemList)) {
            return false;
         } else {
            for (ItemHolder item : itemList) {
               if (!takeItem(player, item)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean takeItems(Player player, int amount, int... itemIds) {
      boolean check = true;
      if (itemIds != null) {
         for (int item : itemIds) {
            check &= takeItems(player, item, amount);
         }
      }

      return check;
   }

   public static void playSound(Player player, String sound) {
      player.sendPacket(QuestSound.getSound(sound));
   }

   public static void playSound(Player player, QuestSound sound) {
      player.sendPacket(sound.getPacket());
   }

   public static void addExpAndSp(Player player, long exp, int sp) {
      long addExp = exp;
      int addSp = sp;
      if (player.hasPremiumStatus()) {
         addExp = (long)((float)exp * PremiumSystemConfig.PREMIUM_RATE_QUEST_XP);
         addSp = (int)(sp * PremiumSystemConfig.PREMIUM_RATE_QUEST_SP);
      }

      player.addExpAndSp(
         (long)player.calcStat(Stat.EXPSP_RATE, (float)addExp * RatesConfig.RATE_QUEST_REWARD_XP, null, null),
         (int)player.calcStat(Stat.EXPSP_RATE, addSp * RatesConfig.RATE_QUEST_REWARD_SP, null, null)
      );
      PcCafePointsManager.getInstance().givePcCafePoint(player, (long)((float)addExp * RatesConfig.RATE_QUEST_REWARD_XP));
   }

   public static int getRandom(int max) {
      return Rnd.get(max);
   }

   public static int getRandom(int min, int max) {
      return Rnd.get(min, max);
   }

   public static long getRandom(long max) {
      return Rnd.get(max);
   }

   public static long getRandom(long min, long max) {
      return Rnd.get(min, max);
   }

   public static boolean getRandomBoolean() {
      return Rnd.nextBoolean();
   }

   public static <T> T getRandomEntry(T... array) {
      return array.length == 0 ? null : array[Rnd.get(array.length)];
   }

   public static <T> T getRandomEntry(List<T> list) {
      return list.isEmpty() ? null : list.get(Rnd.get(list.size()));
   }

   public static int getRandomEntry(int... array) {
      return array[Rnd.get(array.length)];
   }

   public static int getItemEquipped(Player player, int slot) {
      return player.getInventory().getPaperdollItemId(slot);
   }

   public static int getGameTicks() {
      return GameTimeTaskManager.getInstance().getGameTicks();
   }

   public void executeForEachPlayer(Player player, Npc npc, boolean isSummon, boolean includeParty, boolean includeCommandChannel) {
      if ((includeParty || includeCommandChannel) && player.isInParty()) {
         Party party = player.getParty();
         if (includeCommandChannel && party.isInCommandChannel()) {
            party.getCommandChannel().forEachMember(member -> {
               this.actionForEachPlayer(member, npc, isSummon);
               return true;
            });
         } else if (includeParty) {
            party.forEachMember(member -> {
               this.actionForEachPlayer(member, npc, isSummon);
               return true;
            });
         }
      } else {
         this.actionForEachPlayer(player, npc, isSummon);
      }
   }

   public void actionForEachPlayer(Player player, Npc npc, boolean isSummon) {
   }

   public void openDoor(int doorId, int instanceId) {
      Door door = this.getDoor(doorId, instanceId);
      if (door == null) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": called openDoor(" + doorId + ", " + instanceId + "); but door was not found!",
            (Throwable)(new NullPointerException())
         );
      } else if (!door.isOpen()) {
         door.openMe();
      }
   }

   public void closeDoor(int doorId, int instanceId) {
      Door door = this.getDoor(doorId, instanceId);
      if (door == null) {
         LOGGER.log(
            Level.WARNING,
            this.getClass().getSimpleName() + ": called closeDoor(" + doorId + ", " + instanceId + "); but door was not found!",
            (Throwable)(new NullPointerException())
         );
      } else if (door.isOpen()) {
         door.closeMe();
      }
   }

   public Door getDoor(int doorId, int instanceId) {
      Door door;
      if (instanceId == 0) {
         door = DoorData.getInstance().getDoor(doorId);
      } else {
         Instance instance = InstanceManager.getInstance().getInstance(instanceId);
         if (instance != null) {
            door = instance.getDoor(doorId);
         } else {
            door = DoorData.getInstance().getDoor(doorId);
         }
      }

      return door;
   }

   public void teleportPlayer(Player player, Location loc, int instanceId) {
      this.teleportPlayer(player, loc, instanceId, true);
   }

   public void teleportPlayer(Player player, Location loc, int instanceId, boolean allowRandomOffset) {
      player.teleToLocation(loc, instanceId, allowRandomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
   }

   protected void addAttackDesire(Npc npc, Creature target) {
      this.addAttackDesire(npc, target, 999);
   }

   protected void addAttackDesire(Npc npc, Creature target, int desire) {
      if (npc.isAttackable()) {
         npc.asAttackable().addDamageHate(target, 0L, desire);
      }

      npc.setRunning();
      npc.getAI().setIntentionAttack(target);
   }

   protected void addMoveToDesire(Npc npc, Location loc, int desire) {
      npc.getAI().setIntentionMoveTo(loc);
   }

   protected void castSkill(Npc npc, Playable target, SkillHolder skill) {
      npc.setTarget(target);
      npc.doCast(skill.getSkill());
   }

   protected void castSkill(Npc npc, Playable target, Skill skill) {
      npc.setTarget(target);
      npc.doCast(skill);
   }

   protected void addSkillCastDesire(Npc npc, Creature target, SkillHolder skill, int desire) {
      this.addSkillCastDesire(npc, target, skill.getSkill(), desire);
   }

   protected void addSkillCastDesire(Npc npc, Creature target, Skill skill, int desire) {
      if (npc.isAttackable()) {
         npc.asAttackable().addDamageHate(target, 0L, desire);
      }

      npc.setTarget(target);
      npc.getAI().setIntentionCast(skill, target);
   }

   public static void specialCamera(
      Player player,
      Creature creature,
      int force,
      int angle1,
      int angle2,
      int time,
      int range,
      int duration,
      int relYaw,
      int relPitch,
      int isWide,
      int relAngle
   ) {
      player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle));
   }

   public static void specialCameraEx(
      Player player, Creature creature, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle
   ) {
      player.sendPacket(new SpecialCamera(creature, player, force, angle1, angle2, time, duration, relYaw, relPitch, isWide, relAngle));
   }

   public static void specialCamera3(
      Player player,
      Creature creature,
      int force,
      int angle1,
      int angle2,
      int time,
      int range,
      int duration,
      int relYaw,
      int relPitch,
      int isWide,
      int relAngle,
      int unk
   ) {
      player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle, unk));
   }

   public static void addRadar(Player player, int x, int y, int z) {
      player.getRadar().addMarker(x, y, z);
   }

   public void removeRadar(Player player, int x, int y, int z) {
      player.getRadar().removeMarker(x, y, z);
   }

   public void clearRadar(Player player) {
      player.getRadar().removeAllMarkers();
   }

   public void playMovie(Player player, Movie movie) {
      new MovieHolder(Arrays.asList(player), movie);
   }

   public void playMovie(Collection<Player> players, Movie movie) {
      new MovieHolder(players, movie);
   }

   public void playMovie(Set<Player> players, Movie movie) {
      new MovieHolder(new ArrayList<>(players), movie);
   }

   public void playMovie(InstanceWorld world, Movie movie) {
      if (world != null) {
         for (Player player : world.getAllowed()) {
            if (player != null && player.getInstanceId() == world.getInstanceId()) {
               this.playMovie(player, movie);
            }
         }
      }
   }

   public void setOneTimeQuestFlag(Player talker, int questId, int flag) {
      Quest quest = ScriptManager.getInstance().getQuest(questId);
      if (quest != null) {
         quest.getQuestState(talker, true).setState((byte)(flag == 1 ? 2 : 1));
      }
   }

   public int getOneTimeQuestFlag(Player talker, int questId) {
      Quest quest = ScriptManager.getInstance().getQuest(questId);
      return quest != null && quest.getQuestState(talker, true).isCompleted() ? 1 : 0;
   }

   public boolean haveMemo(Player talker, int questId) {
      Quest quest = ScriptManager.getInstance().getQuest(questId);
      return quest != null && talker.hasQuestState(quest.getName());
   }

   public void removeNRMemo(QuestState qs, int value) {
      qs.unset("NRmemo");
   }

   public void setNRMemoStateEx(QuestState qs, int slot, int unknown, int value) {
      qs.set("NRmemoStateEx" + slot, String.valueOf(value));
   }

   public int getNRMemoStateEx(QuestState qs, int slot, int unknown) {
      return qs.getInt("NRmemoStateEx" + slot);
   }

   public void setNRFlagJournal(QuestState qs, int questId, int flagId) {
      qs.set("NRFlagJournal", String.valueOf(flagId));
   }

   public void setFlagJournal(QuestState qs, int flagId) {
      qs.set("FlagJournal", String.valueOf(flagId));
   }

   public void setNRMemo(QuestState qs, int value) {
      qs.set("NRmemo", String.valueOf(value));
   }

   public void setNRMemoState(QuestState qs, int slot, int value) {
      qs.set("NRmemoState" + slot, String.valueOf(value));
   }

   public int getNRMemoState(QuestState qs, int slot) {
      return qs.getInt("NRmemoState" + slot);
   }

   public boolean haveNRMemo(QuestState qs, int slot) {
      return qs.getInt("NRmemo") == slot;
   }
}
