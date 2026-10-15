package org.l2jmobius.gameserver.entity.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.custom.BossAnnouncementsConfig;
import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature.MoveData;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.holders.creature.TimeStamp;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.QuestGuard;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.entity.actor.tasks.creature.HitTask;
import org.l2jmobius.gameserver.entity.actor.tasks.creature.MagicUseTask;
import org.l2jmobius.gameserver.entity.actor.tasks.creature.QueuedMagicUseTask;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.pathfinding.GeoLocation;
import org.l2jmobius.gameserver.geoengine.pathfinding.PathFinding;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.CaptchaManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.InvulSkillHolder;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttackAvoid;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillHolder;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffFinishTask;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillChannelized;
import org.l2jmobius.gameserver.mechanics.skill.SkillChannelizer;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Calculator;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.Attack;
import org.l2jmobius.gameserver.network.serverpackets.ChangeMoveType;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceled;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.Revive;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureSeeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.MovementTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public abstract class Creature extends WorldObject {
   public static final Logger LOGGER = Logger.getLogger(Creature.class.getName());
   private final Set<Creature> _attackByList = ConcurrentHashMap.newKeySet(1);
   private volatile boolean _isCastingNow = false;
   private volatile boolean _isCastingSimultaneouslyNow = false;
   private Skill _lastSkillCast;
   private Skill _lastSimultaneousSkillCast;
   private boolean _isDead = false;
   private boolean _isImmobilized = false;
   private boolean _isOverloaded = false;
   private boolean _isParalyzed = false;
   private boolean _isPendingRevive = false;
   private boolean _isRunning = false;
   protected boolean _showSummonAnimation = false;
   protected boolean _isTeleporting = false;
   private boolean _isInvul = false;
   private boolean _isMortal = true;
   private boolean _isFlying = false;
   private CreatureStat _stat;
   private CreatureStatus _status;
   private CreatureTemplate _template;
   private String _title;
   public static final double MAX_HP_BAR_PX = 352.0;
   private double _hpUpdateIncCheck = 0.0;
   private double _hpUpdateDecCheck = 0.0;
   private double _hpUpdateInterval = 0.0;
   private int _karma = 0;
   private Calculator[] _calculators;
   private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
   private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
   private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
   private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
   private boolean _allSkillsDisabled;
   private final byte[] _zones = new byte[ZoneId.getZoneCount()];
   protected final Location _lastZoneValidateLocation = new Location(this.getX(), this.getY(), this.getZ());
   private final StampedLock _attackLock = new StampedLock();
   private Team _team = Team.NONE;
   private boolean _lethalable = true;
   private final Map<Integer, OptionSkillHolder> _triggerSkills = new ConcurrentHashMap<>(1);
   private final Map<Integer, InvulSkillHolder> _invulAgainst = new ConcurrentHashMap<>(1);
   private final EffectList _effectList = new EffectList(this);
   private Creature _summoner = null;
   private SkillChannelizer _channelizer = null;
   private SkillChannelized _channelized = null;
   private final BuffFinishTask _buffFinishTask = new BuffFinishTask();
   private int _abnormalVisualEffects;
   private int _abnormalVisualEffectsSpecial;
   private int _abnormalVisualEffectsEvent;
   protected MoveData _move;
   private boolean _cursorKeyMovement = false;
   private WorldObject _target;
   private volatile long _attackEndTime;
   private int _disableBowAttackEndTime;
   private int _castInterruptTime;
   private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
   private volatile CreatureAI _ai = null;
   protected Future<?> _skillCast;
   protected Future<?> _skillCast2;
   private final Map<Integer, RelationCache> _knownRelations = new ConcurrentHashMap<>();
   private Set<Creature> _seenCreatures = null;
   private int _seenCreatureRange = PlayerConfig.ALT_PARTY_RANGE;
   private final List<Item> _fakePlayerDrops = new CopyOnWriteArrayList<>();
   private OnCreatureAttack _onCreatureAttack = null;
   private OnCreatureAttacked _onCreatureAttacked = null;
   private OnCreatureDamageDealt _onCreatureDamageDealt = null;
   private OnCreatureDamageReceived _onCreatureDamageReceived = null;
   private OnCreatureAttackAvoid _onCreatureAttackAvoid = null;
   private OnCreatureSkillUse _onCreatureSkillUse = null;
   private boolean _disabledAI = false;

   public Creature(CreatureTemplate template) {
      this(IdManager.getInstance().getNextId(), template);
   }

   public Creature(int objectId, CreatureTemplate template) {
      super(objectId);
      if (template == null) {
         throw new NullPointerException("Template is null!");
      } else {
         this.setInstanceType(InstanceType.Creature);
         this.initCharStat();
         this.initCharStatus();
         this._template = template;
         if (this.isDoor()) {
            this._calculators = Formulas.getStdDoorCalculators();
         } else if (this.isNpc()) {
            this._calculators = NPC_STD_CALCULATOR;

            for (Skill skill : template.getSkills().values()) {
               this.addSkill(skill);
            }
         } else {
            this._calculators = new Calculator[Stat.NUM_STATS];
            if (this.isSummon()) {
               for (Skill skill : template.getSkills().values()) {
                  this.addSkill(skill);
               }
            }

            Formulas.addFuncsToNewCharacter(this);
         }

         this.setInvul(true);
      }
   }

   public EffectList getEffectList() {
      return this._effectList;
   }

   public Inventory getInventory() {
      return null;
   }

   public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage) {
      return true;
   }

   public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage) {
      return true;
   }

   public boolean isInsideZone(ZoneId zone) {
      Instance instance = InstanceManager.getInstance().getInstance(this.getInstanceId());
      if (zone == ZoneId.PVP) {
         if (instance != null && instance.isPvP()) {
            return true;
         }

         return this._zones[ZoneId.PVP.ordinal()] > 0 && this._zones[ZoneId.PEACE.ordinal()] == 0 && this._zones[ZoneId.NO_PVP.ordinal()] == 0;
      }

      if (zone == ZoneId.PEACE) {
         if (instance != null && instance.isPvP()) {
            return false;
         }
      }

      return this._zones[zone.ordinal()] > 0;
   }

   public void setInsideZone(ZoneId zone, boolean state) {
      synchronized (this._zones) {
         if (state) {
            this._zones[zone.ordinal()]++;
         } else if (this._zones[zone.ordinal()] > 0) {
            this._zones[zone.ordinal()]--;
         }
      }
   }

   public boolean isTransformed() {
      return false;
   }

   public Transform getTransformation() {
      return null;
   }

   public void untransform() {
   }

   public boolean isGM() {
      return false;
   }

   public AccessLevel getAccessLevel() {
      return null;
   }

   protected void initCharStatusUpdateValues() {
      this._hpUpdateIncCheck = this._stat.getMaxHp();
      this._hpUpdateInterval = this._hpUpdateIncCheck / 352.0;
      this._hpUpdateDecCheck = this._hpUpdateIncCheck - this._hpUpdateInterval;
   }

   public void onDecay() {
      if (PlayerConfig.DISCONNECT_AFTER_DEATH && this.isPlayer()) {
         Player player = this.asPlayer();
         if (player.isOnline()) {
            player.sendPacket(
               new SystemMessage(
                  SendMessageLocalisationData.getLocalisation(
                     player, "60 min. have passed after the death of your character, so you were disconnected from the game."
                  )
               )
            );
            Disconnection.of(player).storeAndDeleteWith(ServerClose.STATIC_PACKET);
         }
      } else {
         this.decayMe();
         ZoneRegion region = ZoneManager.getInstance().getRegion(this);
         if (region != null) {
            region.removeFromZones(this);
         }
      }
   }

   public void onSpawn() {
      super.onSpawn();
      this._buffFinishTask.start();
      this.revalidateZone(true);
      if (this instanceof GrandBoss) {
         if (BossAnnouncementsConfig.GRANDBOSS_SPAWN_ANNOUNCEMENTS
            && (!this.isInInstance() || BossAnnouncementsConfig.GRANDBOSS_INSTANCE_ANNOUNCEMENTS)
            && !this.isMinion()
            && !this.isRaidMinion()) {
            String name = NpcData.getInstance().getTemplate(this.getId()).getName();
            if (name != null && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(this.getId())) {
               World.broadcastToAllOnlinePlayers(name + " has spawned!");
               World.broadcastToAllOnlinePlayersOnScreen(name + " has spawned!");
            }
         }
      } else if (this.isRaid()
         && BossAnnouncementsConfig.RAIDBOSS_SPAWN_ANNOUNCEMENTS
         && (!this.isInInstance() || BossAnnouncementsConfig.RAIDBOSS_INSTANCE_ANNOUNCEMENTS)
         && !this.isMinion()
         && !this.isRaidMinion()) {
         String name = NpcData.getInstance().getTemplate(this.getId()).getName();
         if (name != null && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(this.getId())) {
            World.broadcastToAllOnlinePlayers(name + " has spawned!");
            World.broadcastToAllOnlinePlayersOnScreen(name + " has spawned!");
         }
      }
   }

   public synchronized void onTeleported() {
      if (this._isTeleporting) {
         this.spawnMe(this.getX(), this.getY(), this.getZ());
         this.setTeleporting(false);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_TELEPORTED, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
         }
      }
   }

   public void addAttackerToAttackByList(Creature creature) {
   }

   public void broadcastPacket(ServerPacket packet) {
      packet.sendInBroadcast();
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            player.sendPacket(packet);
         }
      });
   }

   public void broadcastSkillPacket(ServerPacket packet, WorldObject target) {
      this.broadcastPacket(packet);
   }

   public void broadcastSkillPacket(ServerPacket packet, Collection<WorldObject> targets) {
      this.broadcastPacket(packet);
   }

   public void broadcastMoveToLocation() {
      this.broadcastMoveToLocation(false);
   }

   public void broadcastMoveToLocation(boolean force) {
      MoveData move = this._move;
      if (move != null) {
         int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
         if (force || this._cursorKeyMovement || move.moveTimestamp <= 0 || gameTicks - move.lastBroadcastTime >= 10) {
            move.lastBroadcastTime = gameTicks;
            if (this.isPlayable()) {
               this.broadcastPacket(new MoveToLocation(this));
            } else {
               CreatureAI ai = this.hasAI() ? this.getAI() : null;
               Intention intention = ai != null ? ai.getIntention() : null;
               WorldObject target = intention != Intention.ATTACK && intention != Intention.FOLLOW ? null : this._target;
               if (target != null) {
                  if (target != this && !this.isOnGeodataPath(move)) {
                     this.broadcastPacket(new MoveToPawn(this, target, this.getAI().getClientMovingToPawnOffset()));
                  } else {
                     this.broadcastPacket(new MoveToLocation(this));
                  }
               } else {
                  WorldRegion region = this.getWorldRegion();
                  if (region != null && region.areNeighborsActive()) {
                     this.broadcastPacket(new MoveToLocation(this));
                  }
               }
            }
         }
      }
   }

   public void broadcastSocialAction(int id) {
      if (this.isPlayable()) {
         this.broadcastPacket(new SocialAction(this.getObjectId(), id));
      } else {
         WorldRegion region = this.getWorldRegion();
         if (region != null && region.areNeighborsActive()) {
            this.broadcastPacket(new SocialAction(this.getObjectId(), id));
         }
      }
   }

   protected boolean needHpUpdate() {
      double currentHp = this._status.getCurrentHp();
      double maxHp = this._stat.getMaxHp();
      if (currentHp <= 1.0 || maxHp < 352.0) {
         return true;
      } else if (!(currentHp <= this._hpUpdateDecCheck) && !(currentHp >= this._hpUpdateIncCheck)) {
         return false;
      } else {
         if (currentHp == maxHp) {
            this._hpUpdateIncCheck = currentHp + 1.0;
            this._hpUpdateDecCheck = currentHp - this._hpUpdateInterval;
         } else {
            double doubleMulti = currentHp / this._hpUpdateInterval;
            int intMulti = (int)doubleMulti;
            this._hpUpdateDecCheck = this._hpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
            this._hpUpdateIncCheck = this._hpUpdateDecCheck + this._hpUpdateInterval;
         }

         return true;
      }
   }

   public void broadcastStatusUpdate() {
      if (!this._status.getStatusListener().isEmpty() && this.needHpUpdate()) {
         StatusUpdate su = new StatusUpdate(this);
         su.addAttribute(10, this._stat.getMaxHp());
         su.addAttribute(9, (int)this._status.getCurrentHp());

         for (Creature temp : this._status.getStatusListener()) {
            if (temp != null) {
               temp.sendPacket(su);
            }
         }
      }
   }

   public void sendMessage(String text) {
   }

   public void teleToLocation(int xValue, int yValue, int zValue, int headingValue, int instanceId, int randomOffset) {
      if (!this.isPlayer() || this.asPlayer().isOnline()) {
         int x = xValue;
         int y = yValue;
         int z = this._isFlying ? zValue : GeoEngine.getInstance().getHeight(xValue, yValue, zValue);
         if (this._isPendingRevive) {
            this.doRevive();
         }

         this.stopMove(null);
         this.abortAttack();
         this.abortCast();
         this.setTarget(null);
         this.setTeleporting(true);
         this.getAI().setIntentionActive();
         this.decayMe();
         if (PlayerConfig.OFFSET_ON_TELEPORT_ENABLED || randomOffset > 0) {
            x = xValue + Rnd.get(-randomOffset, randomOffset);
            y = yValue + Rnd.get(-randomOffset, randomOffset);
            int count = 0;

            for (float collision = Math.min(this.getTemplate().getCollisionRadius() * 2, randomOffset);
               count++ < 100
                  && (
                     LocationUtil.calculateDistance(xValue, yValue, zValue, x, y, z, true, false) < collision
                        || !GeoEngine.getInstance().canSeeTarget(xValue, yValue, zValue, x, y, z, instanceId)
                        || !GeoEngine.getInstance().canMoveToTarget(xValue, yValue, zValue, x, y, z, instanceId)
                  );
               y = yValue + Rnd.get(-randomOffset, randomOffset)
            ) {
               x = xValue + Rnd.get(-randomOffset, randomOffset);
            }

            if (!this._isFlying) {
               Location validLocation = GeoEngine.getInstance().getValidLocation(xValue, yValue, z, x, y, z, instanceId);
               x = validLocation.getX();
               y = validLocation.getY();
               z = validLocation.getZ();
            }
         }

         z += 5;
         this.broadcastPacket(new TeleportToLocation(this, x, y, z, headingValue));
         this.setInstanceId(instanceId);
         this.setXYZ(x, y, z);
         if (headingValue != 0) {
            this.setHeading(headingValue);
         }

         if (this.isPlayer()) {
            Player player = this.asPlayer();
            GameClient client = player.getClient();
            if (client != null && client.isDetached()) {
               this.onTeleported();
            }
         } else {
            this.onTeleported();
         }

         this.revalidateZone(true);
      }
   }

   public void teleToLocation(int x, int y, int z, int heading, int instanceId, boolean randomOffset) {
      this.teleToLocation(x, y, z, heading, instanceId, randomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
   }

   public void teleToLocation(int x, int y, int z, int heading, int instanceId) {
      this.teleToLocation(x, y, z, heading, instanceId, 0);
   }

   public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset) {
      this.teleToLocation(x, y, z, heading, -1, randomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
   }

   public void teleToLocation(int x, int y, int z, int heading) {
      this.teleToLocation(x, y, z, heading, -1, 0);
   }

   public void teleToLocation(int x, int y, int z, boolean randomOffset) {
      this.teleToLocation(x, y, z, 0, -1, randomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
   }

   public void teleToLocation(int x, int y, int z) {
      this.teleToLocation(x, y, z, 0, -1, 0);
   }

   public void teleToLocation(ILocational loc, int randomOffset) {
      this.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), randomOffset);
   }

   public void teleToLocation(ILocational loc, int instanceId, int randomOffset) {
      this.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instanceId, randomOffset);
   }

   public void teleToLocation(ILocational loc, boolean randomOffset) {
      this.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), randomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
   }

   public void teleToLocation(ILocational loc) {
      this.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), 0);
   }

   public void teleToLocation(TeleportWhereType teleportWhere) {
      this.teleToLocation(MapRegionData.getInstance().getTeleToLocation(this, teleportWhere), true);
   }

   private boolean canUseRangeWeapon() {
      if (this.isTransformed()) {
         return true;
      } else {
         if (this.isPlayer()) {
            Weapon weaponItem = this.getActiveWeaponItem();
            if (weaponItem == null || !weaponItem.isRange()) {
               return false;
            }

            if (!this.checkAndEquipArrows()) {
               this.getAI().setIntentionIdle();
               this.sendPacket(ActionFailed.STATIC_PACKET);
               this.sendPacket(weaponItem.isBow() ? SystemMessageId.YOU_HAVE_RUN_OUT_OF_ARROWS : SystemMessageId.NOT_ENOUGH_BOLTS);
               return false;
            }

            if (this._disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks()) {
               ThreadPool.schedule(() -> this.getAI().notifyActionReadyToAct(), 1000L);
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            }

            int mpConsume = weaponItem.getMpConsume();
            if (weaponItem.getReducedMpConsume() > 0 && Rnd.get(100) < weaponItem.getReducedMpConsumeChance()) {
               mpConsume = weaponItem.getReducedMpConsume();
            }

            mpConsume = (int)this.calcStat(Stat.BOW_MP_CONSUME_RATE, mpConsume, null, null);
            if (this._status.getCurrentMp() < mpConsume) {
               ThreadPool.schedule(() -> this.getAI().notifyActionReadyToAct(), 1000L);
               this.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            }

            if (mpConsume > 0) {
               this._status.reduceMp(mpConsume);
            }

            this._disableBowAttackEndTime = 50 + GameTimeTaskManager.getInstance().getGameTicks();
         } else if (this.isNpc() && this._disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks()) {
            return false;
         }

         return true;
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void doAttack(Creature target) {
      long stamp = this._attackLock.tryWriteLock();
      if (stamp != 0L) {
         try {
            if (target != null && !this.isAttackDisabled() && target.isTargetable()) {
               if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK, this)) {
                  if (this._onCreatureAttack == null) {
                     this._onCreatureAttack = new OnCreatureAttack();
                  }

                  this._onCreatureAttack.setAttacker(this);
                  this._onCreatureAttack.setTarget(target);
                  TerminateReturn attackReturn = (TerminateReturn)EventDispatcher.getInstance()
                     .notifyEvent(this._onCreatureAttack, this, TerminateReturn.class);
                  if (attackReturn != null && attackReturn.terminate()) {
                     this.getAI().setIntentionActive();
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }

               if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACKED, target)) {
                  if (this._onCreatureAttacked == null) {
                     this._onCreatureAttacked = new OnCreatureAttacked();
                  }

                  this._onCreatureAttacked.setAttacker(this);
                  this._onCreatureAttacked.setTarget(target);
                  TerminateReturn attackedReturn = (TerminateReturn)EventDispatcher.getInstance()
                     .notifyEvent(this._onCreatureAttacked, target, TerminateReturn.class);
                  if (attackedReturn != null && attackedReturn.terminate()) {
                     this.getAI().setIntentionActive();
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }

               if (!this.isAlikeDead()) {
                  if (this.isNpc() && target.isAlikeDead() || !this.isInSurroundingRegion(target)) {
                     this.getAI().setIntentionActive();
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (this.isPlayer()) {
                     if (target.isDead()) {
                        this.getAI().setIntentionActive();
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }

                     if (this.isTransformed() && !this.getTransformation().canAttack()) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                     }
                  }
               }

               if (this.getActiveWeaponItem() != null) {
                  Weapon wpn = this.getActiveWeaponItem();
                  if (!wpn.isAttackWeapon() && !this.isGM()) {
                     if (wpn.getItemType() == WeaponType.FISHINGROD) {
                        this.sendPacket(SystemMessageId.YOU_LOOK_ODDLY_AT_THE_FISHING_POLE_IN_DISBELIEF_AND_REALIZE_THAT_YOU_CAN_T_ATTACK_ANYTHING_WITH_THIS);
                     } else {
                        this.sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
                     }

                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }

               Player player = this.asPlayer();
               if (player != null) {
                  if (player.inObserverMode()) {
                     this.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (player.isSiegeFriend(target)) {
                     if (TerritoryWarManager.getInstance().isTWInProgress()) {
                        this.sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
                     } else {
                        this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
                     }

                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (target.isInsidePeaceZone(player)) {
                     this.getAI().setIntentionActive();
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               } else if (this.isInsidePeaceZone(this, target)) {
                  this.getAI().setIntentionActive();
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               this.stopEffectsOnAction();
               if (!GeoEngine.getInstance().canSeeTarget(this, target)) {
                  this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                  this.getAI().setIntentionActive();
                  this.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  if (this.isMoving()) {
                     this.stopMove(this.getLocation());
                  }

                  Weapon weaponItem = this.getActiveWeaponItem();
                  int timeAtk = this.calculateTimeBetweenAttacks();
                  int timeToHit = timeAtk / 2;
                  Attack attack = new Attack(
                     this, target, this.isChargedShot(ShotType.SOULSHOTS), weaponItem != null ? weaponItem.getCrystalTypePlus().getLevel() : 0
                  );
                  this.setHeading(LocationUtil.calculateHeadingFrom(this, target));
                  int reuse = this.calculateReuseTime(weaponItem);
                  long currentTime = System.nanoTime();
                  boolean hitted = false;
                  final WeaponType attackType = this.getAttackType();
                  if (attackType == WeaponType.BOW) {
                     if (!this.canUseRangeWeapon()) {
                        return;
                     }

                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeToHit + reuse / 2);
                     hitted = this.doAttackHitByBow(attack, target, timeAtk, reuse);
                  } else if (attackType == WeaponType.CROSSBOW) {
                     if (!this.canUseRangeWeapon()) {
                        return;
                     }

                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeToHit + reuse / 2);
                     hitted = this.doAttackHitByCrossBow(attack, target, timeAtk, reuse);
                  } else if (attackType == WeaponType.POLE) {
                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
                     hitted = this.doAttackHitByPole(attack, target, timeToHit);
                  } else if (attackType == WeaponType.FIST && !this.isPlayer()) {
                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
                     hitted = this.doAttackHitSimple(attack, target, timeToHit);
                  } else if (attackType == WeaponType.DUAL || attackType == WeaponType.DUALFIST || attackType == WeaponType.DUALDAGGER || attackType == WeaponType.FIST) {
                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
                     hitted = this.doAttackHitByDual(attack, target, timeToHit);
                  } else {
                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
                     hitted = this.doAttackHitSimple(attack, target, timeToHit);
                  }

                  if (this._attackEndTime < currentTime) {
                     this._attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(2147483647L);
                  }

                  if (this.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE && (target.isPlayable() || target.isFakePlayer())) {
                     Npc npc = this.asNpc();
                     if (!npc.isScriptValue(1)) {
                        npc.setScriptValue(1);
                        this.broadcastInfo();
                        ScriptManager.getInstance().getScript("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
                     }
                  }

                  if (player != null && !player.isInsideZone(ZoneId.PVP) && player != target) {
                     AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
                     if (player.getSummon() != target) {
                        if ((!target.isRaid() || !PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK) && (!target.isChampion() || !PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)) {
                           player.updatePvPStatus(target);
                        } else {
                           player.updatePvPStatus();
                        }
                     }
                  } else if (PvpConfig.FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK && (this.isSummon() || this.isPet())) {
                     Player owner = this.isSummon() ? this.asSummon().getOwner() : this.asPet().getOwner();
                     if (owner != null
                        && !owner.isInsideZone(ZoneId.PVP)
                        && owner != target
                        && owner.getSummon() != target
                        && (target.isRaid() && PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK || target.isChampion() && PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)) {
                        AttackStanceTaskManager.getInstance().addAttackStanceTask(owner);
                        owner.updatePvPStatus();
                     }
                  }

                  if (!hitted) {
                     this.abortAttack();
                  } else {
                     this.setChargedShot(ShotType.SOULSHOTS, false);
                     if (player != null) {
                        if (player.isCursedWeaponEquipped()) {
                           if (!target.isInvul()) {
                              target.setCurrentCp(0.0);
                           }
                        } else if (player.isHero() && target.isPlayer() && target.asPlayer().isCursedWeaponEquipped()) {
                           target.setCurrentCp(0.0);
                        }
                     }
                  }

                  if (attack.hasHits()) {
                     this.broadcastPacket(attack);
                  }

                  ThreadPool.schedule(() -> this.getAI().notifyActionReadyToAct(), timeAtk + reuse);
               }
            }
         } finally {
            this._attackLock.unlockWrite(stamp);
         }
      }
   }

   private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse) {
      int damage1 = 0;
      byte shld1 = 0;
      boolean crit1 = false;
      boolean miss1 = Formulas.calcHitMiss(this, target);
      this.reduceArrowCount(false);
      this._move = null;
      if (!miss1) {
         shld1 = Formulas.calcShldUse(this, target);
         crit1 = Formulas.calcCrit(this, target);
         damage1 = (int)Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
         if (PlayerConfig.CALCULATE_DISTANCE_BOW_DAMAGE) {
            damage1 = (int)(damage1 * (this.calculateDistance3D(target) / 4000.0 + 0.8));
         }
      }

      if (this.isPlayer()) {
         this.sendPacket(new SetupGauge(this.getObjectId(), 1, sAtk + reuse));
      }

      ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk);
      int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
      this._disableBowAttackEndTime = gameTime + (sAtk + reuse) / 100;
      if (this._disableBowAttackEndTime < gameTime) {
         this._disableBowAttackEndTime = Integer.MAX_VALUE;
      }

      attack.addHit(target, damage1, miss1, crit1, shld1);
      return !miss1;
   }

   private boolean doAttackHitByCrossBow(Attack attack, Creature target, int sAtk, int reuse) {
      int damage1 = 0;
      byte shld1 = 0;
      boolean crit1 = false;
      boolean miss1 = Formulas.calcHitMiss(this, target);
      this.reduceArrowCount(true);
      this._move = null;
      if (!miss1) {
         shld1 = Formulas.calcShldUse(this, target);
         crit1 = Formulas.calcCrit(this, target);
         damage1 = (int)Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
      }

      if (this.isPlayer()) {
         this.sendPacket(SystemMessageId.CROSSBOW_IS_PREPARING_TO_FIRE);
         this.sendPacket(new SetupGauge(this.getObjectId(), 1, sAtk + reuse));
      }

      ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk);
      int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
      this._disableBowAttackEndTime = gameTime + (sAtk + reuse) / 100;
      if (this._disableBowAttackEndTime < gameTime) {
         this._disableBowAttackEndTime = Integer.MAX_VALUE;
      }

      attack.addHit(target, damage1, miss1, crit1, shld1);
      return !miss1;
   }

   private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk) {
      int damage1 = 0;
      int damage2 = 0;
      byte shld1 = 0;
      byte shld2 = 0;
      boolean crit1 = false;
      boolean crit2 = false;
      boolean miss1 = Formulas.calcHitMiss(this, target);
      boolean miss2 = Formulas.calcHitMiss(this, target);
      if (!miss1) {
         shld1 = Formulas.calcShldUse(this, target);
         crit1 = Formulas.calcCrit(this, target);
         damage1 = (int)Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
         damage1 /= 2;
      }

      if (!miss2) {
         shld2 = Formulas.calcShldUse(this, target);
         crit2 = Formulas.calcCrit(this, target);
         damage2 = (int)Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.hasSoulshot());
         damage2 /= 2;
      }

      ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk / 2);
      ThreadPool.schedule(new HitTask(this, target, damage2, crit2, miss2, shld2, attack.hasSoulshot(), false), sAtk);
      attack.addHit(target, damage1, miss1, crit1, shld1);
      attack.addHit(target, damage2, miss2, crit2, shld2);
      return !miss1 || !miss2;
   }

   private boolean doAttackHitByPole(Attack attack, Creature target, int sAtk) {
      boolean hitted = this.doAttackHitSimple(attack, target, 100.0, sAtk, true);
      if (!this.isAffected(EffectFlag.POLEARM_SINGLE_TARGET)) {
         int attackCountMax = (int)this._stat.calcStat(Stat.ATTACK_COUNT_MAX, 1.0, null, null);
         if (attackCountMax > 1) {
            double headingAngle = LocationUtil.convertHeadingToDegree(this.getHeading());
            int maxRadius = this._stat.getPhysicalAttackRange();
            int physicalAttackAngle = this._stat.getPhysicalAttackAngle();
            double attackpercent = 85.0;

            for (Creature obj : World.getVisibleObjectsInRange(this, Creature.class, maxRadius)) {
               if (obj != target
                  && !obj.isAlikeDead()
                  && obj.isAutoAttackable(this)
                  && !(Math.abs(this.calculateDirectionTo(obj) - headingAngle) > physicalAttackAngle)
                  && (!obj.isPet() || !this.isPlayer() || obj.asPet().getOwner() != this.asPlayer())
                  && (!this.isAttackable() || !obj.isPlayer() || !this._target.isAttackable())
                  && (!this.isAttackable() || !obj.isAttackable() || this.asAttackable().isChaos())) {
                  hitted |= this.doAttackHitSimple(attack, obj, attackpercent, sAtk, false);
                  attackpercent /= 1.15;
                  if (--attackCountMax <= 0) {
                     break;
                  }
               }
            }
         }
      }

      return hitted;
   }

   private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk) {
      return this.doAttackHitSimple(attack, target, 100.0, sAtk, true);
   }

   private boolean doAttackHitSimple(Attack attack, Creature target, double attackpercent, int sAtk, boolean rechargeShots) {
      int damage1 = 0;
      byte shld1 = 0;
      boolean crit1 = false;
      boolean miss1 = Formulas.calcHitMiss(this, target);
      if (!miss1) {
         shld1 = Formulas.calcShldUse(this, target);
         crit1 = Formulas.calcCrit(this, target);
         damage1 = (int)Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
         if (attackpercent != 100.0) {
            damage1 = (int)(damage1 * attackpercent / 100.0);
         }
      }

      ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), rechargeShots), sAtk);
      attack.addHit(target, damage1, miss1, crit1, shld1);
      return !miss1;
   }

   public void doCast(Skill skill) {
      this.beginCast(skill, false);
   }

   public void doSimultaneousCast(Skill skill) {
      this.beginCast(skill, true);
   }

   public void doCast(Skill skill, Creature target, List<WorldObject> targets) {
      if (!this.checkDoCastConditions(skill)) {
         this.setCastingNow(false);
      } else if (skill.isSimultaneousCast()) {
         this.doSimultaneousCast(skill, target, targets);
      } else {
         this.stopEffectsOnAction();
         this.beginCast(skill, false, target, targets);
      }
   }

   public void doSimultaneousCast(Skill skill, Creature target, List<WorldObject> targets) {
      if (!this.checkDoCastConditions(skill)) {
         this.setCastingSimultaneouslyNow(false);
      } else {
         this.stopEffectsOnAction();
         this.beginCast(skill, true, target, targets);
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void beginCast(Skill skill, boolean isSimultaneous) {
      if (!this.isAttackable() || !this.isMoving()) {
         if (!this.checkDoCastConditions(skill)) {
            if (isSimultaneous) {
               this.setCastingSimultaneouslyNow(false);
            } else {
               this.setCastingNow(false);
            }

            if (this.isPlayer()) {
               this.getAI().setIntentionActive();
            }
         } else {
            boolean simultaneously = isSimultaneous;
            if (skill.isSimultaneousCast() && !isSimultaneous) {
               simultaneously = true;
            }

            this.stopEffectsOnAction();
            Creature target = null;
            List<WorldObject> targets = skill.getTargetList(this);
            boolean doit = false;
            final TargetType targetType = skill.getTargetType();
            if (targetType == TargetType.AREA_SUMMON) {
               target = this.isPlayer() ? this.asPlayer().getSummon() : null;
            } else if (targetType == TargetType.AURA
               || targetType == TargetType.AURA_CORPSE_MOB
               || targetType == TargetType.FRONT_AURA
               || targetType == TargetType.BEHIND_AURA
               || targetType == TargetType.GROUND
               || targetType == TargetType.AURA_FRIENDLY) {
               target = this;
            } else {
               if (targetType == TargetType.SELF
                  || targetType == TargetType.PET
                  || targetType == TargetType.SERVITOR
                  || targetType == TargetType.SUMMON
                  || targetType == TargetType.OWNER_PET
                  || targetType == TargetType.PARTY
                  || targetType == TargetType.CLAN
                  || targetType == TargetType.PARTY_CLAN
                  || targetType == TargetType.COMMAND_CHANNEL) {
                  doit = true;
               }

               if (targets.isEmpty()) {
                  if (simultaneously) {
                     this.setCastingSimultaneouslyNow(false);
                  } else {
                     this.setCastingNow(false);
                  }

                  if (this.isPlayer()) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.getAI().setIntentionActive();
                  }

                  return;
               }

               if (skill.isContinuous() && !skill.isDebuff() || skill.hasEffectType(EffectType.CPHEAL, new EffectType[]{EffectType.HEAL})) {
                  doit = true;
               }

               if (doit) {
                  target = targets.get(0).asCreature();
               } else {
                  target = this._target != null ? this._target.asCreature() : null;
               }
            }

            this.beginCast(skill, simultaneously, target, targets);
         }
      }
   }

   private void beginCast(Skill skill, boolean simultaneously, Creature target, List<WorldObject> targets) {
      if (target == null) {
         if (simultaneously) {
            this.setCastingSimultaneouslyNow(false);
         } else {
            this.setCastingNow(false);
         }

         if (this.isPlayer()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.getAI().setIntentionActive();
         }
      } else {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SKILL_USE, this)) {
            if (this._onCreatureSkillUse == null) {
               this._onCreatureSkillUse = new OnCreatureSkillUse();
            }

            this._onCreatureSkillUse.setCaster(this);
            this._onCreatureSkillUse.setSkill(skill);
            this._onCreatureSkillUse.setSimultaneously(simultaneously);
            this._onCreatureSkillUse.setTarget(target);
            this._onCreatureSkillUse.setTargets(targets);
            TerminateReturn term = (TerminateReturn)EventDispatcher.getInstance().notifyEvent(this._onCreatureSkillUse, this, TerminateReturn.class);
            if (term != null && term.terminate()) {
               if (simultaneously) {
                  this.setCastingSimultaneouslyNow(false);
               } else {
                  this.setCastingNow(false);
               }

               if (this.isPlayer()) {
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  this.getAI().setIntentionActive();
               }

               return;
            }
         }

         if (!skill.hasEffectType(EffectType.RESURRECTION, new EffectType[0]) || !this.isResurrectionBlocked() && !target.isResurrectionBlocked()) {
            int skillTime = skill.getHitTime() + skill.getCoolTime();
            if (!skill.isChanneling() || skill.getChannelingSkillId() == 0) {
               if (!skill.isStatic()) {
                  skillTime = Formulas.calcAtkSpd(this, skill, skillTime);
               }

               if (skill.isMagic() && (this.isChargedShot(ShotType.SPIRITSHOTS) || this.isChargedShot(ShotType.BLESSED_SPIRITSHOTS))) {
                  skillTime = (int)(0.6 * skillTime);
               }
            }

            if (skill.isMagic() && skill.getHitTime() + skill.getCoolTime() > 550 && skillTime < 550) {
               skillTime = 550;
            } else if (!skill.isStatic() && skill.getHitTime() + skill.getCoolTime() >= 500 && skillTime < 500) {
               skillTime = 500;
            }

            if (this._isCastingSimultaneouslyNow && simultaneously) {
               ThreadPool.schedule(() -> this.beginCast(skill, simultaneously, target, targets), 100L);
            } else {
               if (simultaneously) {
                  this.setCastingSimultaneouslyNow(true);
               } else {
                  this.setCastingNow(true);
               }

               if (!simultaneously) {
                  this._castInterruptTime = -2 + GameTimeTaskManager.getInstance().getGameTicks() + skillTime / 100;
                  this.setLastSkillCast(skill);
               } else {
                  this.setLastSimultaneousSkillCast(skill);
               }

               int reuseDelay;
               if (skill.isStaticReuse() || skill.isStatic()) {
                  reuseDelay = skill.getReuseDelay();
               } else if (skill.isMagic()) {
                  reuseDelay = (int)(skill.getReuseDelay() * this.calcStat(Stat.MAGIC_REUSE_RATE, 1.0, null, null));
               } else if (skill.isPhysical()) {
                  reuseDelay = (int)(skill.getReuseDelay() * this.calcStat(Stat.P_REUSE, 1.0, null, null));
               } else {
                  reuseDelay = (int)(skill.getReuseDelay() * this.calcStat(Stat.DANCE_REUSE, 1.0, null, null));
               }

               if (this.isPlayable()) {
                  reuseDelay = (int)(reuseDelay * ClassBalanceConfig.SKILL_REUSE_MULTIPLIERS[this.asPlayer().getPlayerClass().getId()]);
               }

               int initmpcons = this._stat.getMpInitialConsume(skill);
               if (initmpcons > 0) {
                  this._status.reduceMp(initmpcons);
                  StatusUpdate su = new StatusUpdate(this);
                  su.addAttribute(11, (int)this._status.getCurrentMp());
                  this.sendPacket(su);
               }

               if (reuseDelay > 10) {
                  if (Formulas.calcSkillMastery(this, skill)) {
                     reuseDelay = 100;
                     if (this.isPlayable()) {
                        this.sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
                     }
                  }

                  if (reuseDelay > 1000) {
                     this.addTimeStamp(skill, reuseDelay);
                  } else {
                     this.disableSkill(skill, reuseDelay);
                  }
               }

               if (target != this) {
                  this.setHeading(LocationUtil.calculateHeadingFrom(this, target));
               }

               if (this.isPlayable()) {
                  if (skill.getItemConsumeId() > 0 && !this.destroyItemByItemId(null, skill.getItemConsumeId(), skill.getItemConsumeCount(), null, true)) {
                     this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
                     this.abortCast();
                     return;
                  }

                  if (skill.getReferenceItemId() > 0 && ItemData.getInstance().getTemplate(skill.getReferenceItemId()).getBodyPart() == BodyPart.DECO) {
                     for (Item item : this.getInventory().getAllItemsByItemId(skill.getReferenceItemId())) {
                        if (item.isEquipped()) {
                           if (item.getMana() < item.useSkillDisTime()) {
                              this.abortCast();
                              return;
                           }

                           item.decreaseMana(false, item.useSkillDisTime());
                           break;
                        }
                     }
                  }
               }

               if (skill.getFlyType() != null) {
                  this.broadcastPacket(new FlyToLocation(this, target, skill.getFlyType()));
                  this.setXYZ(target.getX(), target.getY(), target.getZ());
               }

               if (!skill.isToggle()) {
                  this.broadcastSkillPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getDisplayLevel(), skillTime, reuseDelay), target);
                  this.broadcastSkillPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getDisplayLevel(), targets), targets);
               }

               if (this.isPlayer() && !skill.isAbnormalInstant()) {
                  SystemMessage sm = null;
                  switch (skill.getId()) {
                     case 1312:
                        break;
                     case 2046:
                        sm = new SystemMessage(SystemMessageId.SUMMONING_YOUR_PET);
                        break;
                     default:
                        sm = new SystemMessage(SystemMessageId.USE_S1);
                        sm.addSkillName(skill);
                  }

                  if (sm != null) {
                     this.sendPacket(sm);
                  }
               }

               if (skill.hasEffects(EffectScope.START)) {
                  skill.applyEffectScope(EffectScope.START, new BuffInfo(this, target, skill), true, false);
               }

               MagicUseTask mut = new MagicUseTask(this, targets, skill, skillTime, simultaneously);
               if (skillTime > 0) {
                  if (this.isPlayer() && !simultaneously) {
                     this.sendPacket(new SetupGauge(this.getObjectId(), 0, skillTime));
                  }

                  if (skill.isChanneling() && skill.getChannelingSkillId() > 0) {
                     this.getSkillChannelizer().startChanneling(skill);
                  }

                  if (simultaneously) {
                     Future<?> future = this._skillCast2;
                     if (future != null) {
                        future.cancel(true);
                        this._skillCast2 = null;
                     }

                     this._skillCast2 = ThreadPool.schedule(mut, Math.max(0, skillTime - 400));
                  } else {
                     Future<?> future = this._skillCast;
                     if (future != null) {
                        future.cancel(true);
                        this._skillCast = null;
                     }

                     this._skillCast = ThreadPool.schedule(mut, Math.max(0, skillTime - 400));
                  }
               } else {
                  mut.setSkillTime(0);
                  this.onMagicLaunchedTimer(mut);
               }
            }
         } else {
            this.sendPacket(SystemMessageId.REJECT_RESURRECTION);
            target.sendPacket(SystemMessageId.REJECT_RESURRECTION);
            if (simultaneously) {
               this.setCastingSimultaneouslyNow(false);
            } else {
               this.setCastingNow(false);
            }

            if (this.isPlayer()) {
               this.getAI().setIntentionActive();
               this.sendPacket(ActionFailed.STATIC_PACKET);
            }
         }
      }
   }

   public boolean checkDoCastConditions(Skill skill) {
      if (skill != null && !this.isSkillDisabled(skill) && (skill.getFlyType() != FlyType.CHARGE || !this.isMovementDisabled())) {
         if (this._status.getCurrentMp() < this._stat.getMpConsume(skill) + this._stat.getMpInitialConsume(skill)) {
            this.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else if (this._status.getCurrentHp() <= skill.getHpConsume()) {
            this.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else {
            if (!skill.isStatic()) {
               if (skill.isMagic()) {
                  if (this.isMuted()) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return false;
                  }
               } else if (this.isPhysicalMuted()) {
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return false;
               }
            }

            if (skill.isChanneling() && skill.getChannelingSkillId() > 0 && !this.isInInstance()) {
               ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(this);
               boolean canCast = true;
               if (skill.getTargetType() == TargetType.GROUND && this.isPlayer()) {
                  Location wp = this.asPlayer().getCurrentSkillWorldPosition();
                  if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ())) {
                     canCast = false;
                  }
               } else if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, this.getX(), this.getY(), this.getZ())) {
                  canCast = false;
               }

               if (!canCast) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
                  sm.addSkillName(skill);
                  this.sendPacket(sm);
                  return false;
               }
            }

            if (this.getActiveWeaponItem() != null) {
               Weapon wep = this.getActiveWeaponItem();
               if (wep.useWeaponSkillsOnly() && !this.isGM() && wep.hasSkills()) {
                  boolean found = false;

                  for (SkillHolder sh : wep.getSkills()) {
                     if (sh.getSkillId() == skill.getId()) {
                        found = true;
                     }
                  }

                  if (!found) {
                     if (this.asPlayer() != null) {
                        this.sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
                     }

                     return false;
                  }
               }
            }

            if (skill.getItemConsumeId() > 0 && this.getInventory() != null) {
               Item requiredItems = this.getInventory().getItemByItemId(skill.getItemConsumeId());
               if (requiredItems == null || requiredItems.getCount() < skill.getItemConsumeCount()) {
                  if (skill.hasEffectType(EffectType.SUMMON, new EffectType[0])) {
                     SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
                     sm.addItemName(skill.getItemConsumeId());
                     sm.addInt(skill.getItemConsumeCount());
                     this.sendPacket(sm);
                  } else {
                     this.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
                  }

                  return false;
               }
            }

            return true;
         }
      } else {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      }
   }

   public Map<Integer, TimeStamp> getItemReuseTimeStamps() {
      return this._reuseTimeStampsItems;
   }

   public void addTimeStampItem(Item item, long reuse) {
      this.addTimeStampItem(item, reuse, -1L);
   }

   public void addTimeStampItem(Item item, long reuse, long systime) {
      this._reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
   }

   public long getItemRemainingReuseTime(int itemObjId) {
      TimeStamp reuseStamp = this._reuseTimeStampsItems.get(itemObjId);
      return reuseStamp != null ? reuseStamp.getRemaining() : -1L;
   }

   public long getReuseDelayOnGroup(int group) {
      if (group > 0 && !this._reuseTimeStampsItems.isEmpty()) {
         long currentTime = System.currentTimeMillis();

         for (TimeStamp ts : this._reuseTimeStampsItems.values()) {
            if (ts.getSharedReuseGroup() == group) {
               long stamp = ts.getStamp();
               if (currentTime < stamp) {
                  return Math.max(stamp - currentTime, 0L);
               }
            }
         }
      }

      return -1L;
   }

   public Map<Integer, TimeStamp> getSkillReuseTimeStamps() {
      return this._reuseTimeStampsSkills;
   }

   public void addTimeStamp(Skill skill, long reuse) {
      this.addTimeStamp(skill, reuse, -1L);
   }

   public void addTimeStamp(Skill skill, long reuse, long systime) {
      this._reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
   }

   public void removeTimeStamp(Skill skill) {
      this._reuseTimeStampsSkills.remove(skill.getReuseHashCode());
   }

   public void resetTimeStamps() {
      this._reuseTimeStampsSkills.clear();
   }

   public long getSkillRemainingReuseTime(int hashCode) {
      TimeStamp reuseStamp = this._reuseTimeStampsSkills.get(hashCode);
      return reuseStamp != null ? reuseStamp.getRemaining() : -1L;
   }

   public boolean hasSkillReuse(int hashCode) {
      TimeStamp reuseStamp = this._reuseTimeStampsSkills.get(hashCode);
      return reuseStamp != null && reuseStamp.hasNotPassed();
   }

   public TimeStamp getSkillReuseTimeStamp(int hashCode) {
      return this._reuseTimeStampsSkills.get(hashCode);
   }

   public Map<Integer, Long> getDisabledSkills() {
      return this._disabledSkills;
   }

   public void enableSkill(Skill skill) {
      if (skill != null) {
         this._disabledSkills.remove(skill.getReuseHashCode());
      }
   }

   public void disableSkill(Skill skill, long delay) {
      if (skill != null) {
         this._disabledSkills.put(skill.getReuseHashCode(), delay > 0L ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
      }
   }

   public void resetDisabledSkills() {
      this._disabledSkills.clear();
   }

   public boolean isSkillDisabled(Skill skill) {
      if (skill == null) {
         return false;
      } else if (this.isAllSkillsDisabled()) {
         return true;
      } else {
         int hashCode = skill.getReuseHashCode();
         if (this.hasSkillReuse(hashCode)) {
            return true;
         } else if (this._disabledSkills.isEmpty()) {
            return false;
         } else {
            Long stamp = this._disabledSkills.get(hashCode);
            if (stamp == null) {
               return false;
            } else if (stamp < System.currentTimeMillis()) {
               this._disabledSkills.remove(hashCode);
               return false;
            } else {
               return true;
            }
         }
      }
   }

   public void disableAllSkills() {
      this._allSkillsDisabled = true;
   }

   public void enableAllSkills() {
      this._allSkillsDisabled = false;
   }

   public boolean doDie(Creature killer) {
      if (CaptchaConfig.ENABLE_CAPTCHA) {
         CaptchaManager.getInstance().updateCounter(killer, this);
      }

      synchronized (this) {
         if (this._isDead) {
            return false;
         }

         this.setCurrentHp(0.0);
         this.setDead(true);
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this)) {
         EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this);
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_KILLED, this)) {
         TerminateReturn returnBack = (TerminateReturn)EventDispatcher.getInstance()
            .notifyEvent(new OnCreatureKilled(killer, this), this, TerminateReturn.class);
         if (returnBack != null && returnBack.terminate()) {
            return false;
         }
      }

      Creature mainDamageDealer = this.isMonster() ? this.asMonster().getMainDamageDealer() : null;
      this.calculateRewards(mainDamageDealer != null ? mainDamageDealer : killer);
      this.setTarget(null);
      this.stopMove(null);
      this._status.stopHpMpRegeneration();
      if (this.isAttackable()) {
         Spawn spawn = this.asNpc().getSpawn();
         if (spawn != null && spawn.isRespawnEnabled()) {
            this.stopAllEffects();
         } else {
            this._effectList.stopAllEffectsWithoutExclusions(true, true);
         }

         if (killer != null && killer.isPlayable() && !killer.asPlayer().isGM()) {
            NpcTemplate template = this.asAttackable().getTemplate();
            Set<Integer> clans = template.getClans();
            if (clans != null && !clans.isEmpty()) {
               World.forEachVisibleObjectInRange(
                  this,
                  Attackable.class,
                  template.getClanHelpRange(),
                  called -> {
                     if (!called.isDead() && called.hasAI() && Math.abs(killer.getZ() - called.getZ()) <= 600) {
                        if (System.currentTimeMillis() - called.getSpawnTime() >= 7000L) {
                           if (called.getAI().getIntention() == Intention.IDLE || called.getAI().getIntention() == Intention.ACTIVE) {
                              if (template.isClan(called.getTemplate().getClans())) {
                                 called.getAI().notifyActionAggression(killer, 1);
                                 if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, called)) {
                                    EventDispatcher.getInstance()
                                       .notifyEventAsync(new OnAttackableFactionCall(called, this.asAttackable(), killer.asPlayer(), killer.isSummon()), called);
                                 }
                              }
                           }
                        }
                     }
                  }
               );
            }
         }
      } else {
         this.stopAllEffectsExceptThoseThatLastThroughDeath();
      }

      this.broadcastStatusUpdate();
      if (this.hasAI()) {
         this.getAI().notifyActionDeath();
      }

      ZoneManager.getInstance().getRegion(this).onDeath(this);
      this.getAttackByList().clear();
      if (this.isChannelized()) {
         this.getSkillChannelized().abortChannelization();
      }

      if (this instanceof GrandBoss) {
         if (BossAnnouncementsConfig.GRANDBOSS_DEFEAT_ANNOUNCEMENTS
            && (!this.isInInstance() || BossAnnouncementsConfig.GRANDBOSS_INSTANCE_ANNOUNCEMENTS)
            && !this.isMinion()
            && !this.isRaidMinion()) {
            String name = NpcData.getInstance().getTemplate(this.getId()).getName();
            if (name != null && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(this.getId())) {
               World.broadcastToAllOnlinePlayers(name + " has been defeated!");
               World.broadcastToAllOnlinePlayersOnScreen(name + " has been defeated!");
            }
         }
      } else if (this.isRaid()
         && BossAnnouncementsConfig.RAIDBOSS_DEFEAT_ANNOUNCEMENTS
         && (!this.isInInstance() || BossAnnouncementsConfig.RAIDBOSS_INSTANCE_ANNOUNCEMENTS)
         && !this.isMinion()
         && !this.isRaidMinion()) {
         String name = NpcData.getInstance().getTemplate(this.getId()).getName();
         if (name != null && !BossAnnouncementsConfig.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(this.getId())) {
            World.broadcastToAllOnlinePlayers(name + " has been defeated!");
            World.broadcastToAllOnlinePlayersOnScreen(name + " has been defeated!");
         }
      }

      return true;
   }

   public boolean decayMe() {
      if (this.hasAI()) {
         if (this.isAttackable()) {
            this.getAttackByList().clear();
            this.asAttackable().clearAggroList();
            this.getAI().setIntentionIdle();
         }

         this.getAI().stopAITask();
      }

      this._disabledAI = false;
      this._onCreatureAttack = null;
      this._onCreatureAttacked = null;
      this._onCreatureDamageDealt = null;
      this._onCreatureDamageReceived = null;
      this._onCreatureAttackAvoid = null;
      this._onCreatureSkillUse = null;
      return super.decayMe();
   }

   public boolean deleteMe() {
      if (this.hasAI()) {
         this.getAI().stopAITask();
      }

      this._effectList.stopAllEffectsWithoutExclusions(false, false);
      if (this._seenCreatures != null) {
         CreatureSeeTaskManager.getInstance().remove(this);
         this._seenCreatures.clear();
      }

      this._buffFinishTask.stop();
      this.setWorldRegion(null);
      return true;
   }

   public void detachAI() {
      if (!this.isWalker()) {
         this.setAI(null);
      }
   }

   protected void calculateRewards(Creature killer) {
   }

   public void doRevive() {
      if (this._isDead) {
         if (!this._isTeleporting) {
            this.setIsPendingRevive(false);
            this.setDead(false);
            if (PlayerConfig.RESPAWN_RESTORE_CP > 0.0 && this._status.getCurrentCp() < this._stat.getMaxCp() * PlayerConfig.RESPAWN_RESTORE_CP) {
               this._status.setCurrentCp(this._stat.getMaxCp() * PlayerConfig.RESPAWN_RESTORE_CP);
            }

            if (PlayerConfig.RESPAWN_RESTORE_HP > 0.0 && this._status.getCurrentHp() < this._stat.getMaxHp() * PlayerConfig.RESPAWN_RESTORE_HP) {
               this._status.setCurrentHp(this._stat.getMaxHp() * PlayerConfig.RESPAWN_RESTORE_HP);
            }

            if (PlayerConfig.RESPAWN_RESTORE_MP > 0.0 && this._status.getCurrentMp() < this._stat.getMaxMp() * PlayerConfig.RESPAWN_RESTORE_MP) {
               this._status.setCurrentMp(this._stat.getMaxMp() * PlayerConfig.RESPAWN_RESTORE_MP);
            }

            this.broadcastPacket(new Revive(this));
            ZoneManager.getInstance().getRegion(this).onRevive(this);
         } else {
            this.setIsPendingRevive(true);
         }
      }
   }

   public void doRevive(double revivePower) {
      this.doRevive();
   }

   public CreatureAI getAI() {
      CreatureAI ai = this._ai;
      if (ai == null) {
         synchronized (this) {
            ai = this._ai;
            if (ai == null) {
               this._ai = ai = this.initAI();
            }
         }
      }

      return ai;
   }

   protected CreatureAI initAI() {
      return new CreatureAI(this);
   }

   public void setAI(CreatureAI newAI) {
      CreatureAI oldAI = this._ai;
      if (oldAI != null && oldAI != newAI && oldAI instanceof AttackableAI) {
         oldAI.stopAITask();
      }

      this._ai = newAI;
   }

   public boolean hasAI() {
      return this._ai != null;
   }

   public boolean isRaid() {
      return false;
   }

   public boolean isMinion() {
      return false;
   }

   public boolean isRaidMinion() {
      return false;
   }

   public Set<Creature> getAttackByList() {
      return this._attackByList;
   }

   public Skill getLastSimultaneousSkillCast() {
      return this._lastSimultaneousSkillCast;
   }

   public void setLastSimultaneousSkillCast(Skill skill) {
      this._lastSimultaneousSkillCast = skill;
   }

   public Skill getLastSkillCast() {
      return this._lastSkillCast;
   }

   public void setLastSkillCast(Skill skill) {
      this._lastSkillCast = skill;
   }

   public boolean isControlBlocked() {
      return this.isAffected(EffectFlag.FEAR);
   }

   public boolean isAllSkillsDisabled() {
      return this._allSkillsDisabled || this.isStunned() || this.isSleeping() || this.isParalyzed();
   }

   public boolean isAttackDisabled() {
      return this.isAttackingNow() || this.isDisabled();
   }

   public boolean isDisabled() {
      return this._disabledAI || this.isAlikeDead() || this.isPhysicalAttackMuted() || this.isStunned() || this.isSleeping() || this.isParalyzed();
   }

   public Calculator[] getCalculators() {
      return this._calculators;
   }

   public boolean isConfused() {
      return this.isAffected(EffectFlag.CONFUSED);
   }

   public boolean isAlikeDead() {
      return this._isDead;
   }

   public boolean isDead() {
      return this._isDead;
   }

   public void setDead(boolean value) {
      this._isDead = value;
   }

   public boolean isImmobilized() {
      return this._isImmobilized;
   }

   public void setImmobilized(boolean value) {
      this._isImmobilized = value;
   }

   public boolean isMuted() {
      return this.isAffected(EffectFlag.MUTED);
   }

   public boolean isPhysicalMuted() {
      return this.isAffected(EffectFlag.PSYCHICAL_MUTED);
   }

   public boolean isPhysicalAttackMuted() {
      return this.isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
   }

   public boolean isMovementDisabled() {
      return this.isStunned()
         || this.isRooted()
         || this.isSleeping()
         || this._isOverloaded
         || this.isParalyzed()
         || this._isImmobilized
         || this.isAlikeDead()
         || this._isTeleporting;
   }

   public boolean isOutOfControl() {
      return this.isConfused() || this.isControlBlocked();
   }

   public boolean isOverloaded() {
      return this._isOverloaded;
   }

   public void setOverloaded(boolean value) {
      this._isOverloaded = value;
   }

   public boolean isParalyzed() {
      return this._isParalyzed || this.isAffected(EffectFlag.PARALYZED);
   }

   public void setParalyzed(boolean value) {
      this._isParalyzed = value;
   }

   public boolean isPendingRevive() {
      return this._isDead && this._isPendingRevive;
   }

   public void setIsPendingRevive(boolean value) {
      this._isPendingRevive = value;
   }

   public boolean isDisarmed() {
      return this.isAffected(EffectFlag.DISARMED);
   }

   public boolean isRooted() {
      return this.isAffected(EffectFlag.ROOTED);
   }

   public boolean isRunning() {
      return this._isRunning;
   }

   private void setRunning(boolean value) {
      if (this._isRunning != value) {
         this._isRunning = value;
         if (this._stat.getRunSpeed() != 0.0) {
            this.broadcastPacket(new ChangeMoveType(this));
         }

         if (this.isPlayer()) {
            this.asPlayer().broadcastUserInfo();
         } else if (this.isSummon()) {
            this.broadcastStatusUpdate();
         } else if (this.isNpc()) {
            World.forEachVisibleObject(this, Player.class, player -> {
               if (this.isVisibleFor(player)) {
                  if (this.isFakePlayer()) {
                     player.sendPacket(new FakePlayerInfo(this.asNpc()));
                  } else if (this._stat.getRunSpeed() == 0.0) {
                     player.sendPacket(new ServerObjectInfo(this.asNpc(), player));
                  } else {
                     player.sendPacket(new NpcInfo(this.asNpc(), player));
                  }
               }
            });
         }
      }
   }

   public void setRunning() {
      this.setRunning(true);
   }

   public boolean isSleeping() {
      return this.isAffected(EffectFlag.SLEEP);
   }

   public boolean isStunned() {
      return this.isAffected(EffectFlag.STUNNED);
   }

   public boolean isBetrayed() {
      return this.isAffected(EffectFlag.BETRAYED);
   }

   public boolean isTeleporting() {
      return this._isTeleporting;
   }

   public void setTeleporting(boolean value) {
      this._isTeleporting = value;
   }

   public void setInvul(boolean value) {
      this._isInvul = value;
   }

   public boolean isInvul() {
      return this._isInvul || this._isTeleporting || this.isAffected(EffectFlag.INVUL);
   }

   public void setMortal(boolean value) {
      this._isMortal = value;
   }

   public boolean isMortal() {
      return this._isMortal;
   }

   public boolean isUndead() {
      return false;
   }

   public boolean isResurrectionBlocked() {
      return this.isAffected(EffectFlag.BLOCK_RESURRECTION);
   }

   public boolean isFlying() {
      return this._isFlying;
   }

   public void setFlying(boolean mode) {
      this._isFlying = mode;
   }

   public CreatureStat getStat() {
      return this._stat;
   }

   public void initCharStat() {
      this._stat = new CreatureStat(this);
   }

   public void setStat(CreatureStat value) {
      this._stat = value;
   }

   public CreatureStatus getStatus() {
      return this._status;
   }

   public void initCharStatus() {
      this._status = new CreatureStatus(this);
   }

   public void setStatus(CreatureStatus value) {
      this._status = value;
   }

   public CreatureTemplate getTemplate() {
      return this._template;
   }

   protected void setTemplate(CreatureTemplate template) {
      this._template = template;
   }

   public String getTitle() {
      return this._title;
   }

   public void setTitle(String value) {
      if (value == null) {
         this._title = "";
      } else {
         this._title = this.isPlayer() && value.length() > 21 ? value.substring(0, 20) : value;
      }
   }

   public void setWalking() {
      this.setRunning(false);
   }

   public int getAbnormalVisualEffects() {
      return this._abnormalVisualEffects;
   }

   public int getAbnormalVisualEffectSpecial() {
      return this._abnormalVisualEffectsSpecial;
   }

   public int getAbnormalVisualEffectEvent() {
      return this._abnormalVisualEffectsEvent;
   }

   public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave) {
      if (ave.isEvent()) {
         return (this._abnormalVisualEffectsEvent & ave.getMask()) == ave.getMask();
      } else {
         return ave.isSpecial()
            ? (this._abnormalVisualEffectsSpecial & ave.getMask()) == ave.getMask()
            : (this._abnormalVisualEffects & ave.getMask()) == ave.getMask();
      }
   }

   public void startAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves) {
      for (AbnormalVisualEffect ave : aves) {
         if (ave.isEvent()) {
            this._abnormalVisualEffectsEvent = this._abnormalVisualEffectsEvent | ave.getMask();
         } else if (ave.isSpecial()) {
            this._abnormalVisualEffectsSpecial = this._abnormalVisualEffectsSpecial | ave.getMask();
         } else {
            this._abnormalVisualEffects = this._abnormalVisualEffects | ave.getMask();
         }
      }

      if (update) {
         this.updateAbnormalEffect();
      }
   }

   public void stopAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves) {
      for (AbnormalVisualEffect ave : aves) {
         if (ave.isEvent()) {
            this._abnormalVisualEffectsEvent = this._abnormalVisualEffectsEvent & ~ave.getMask();
         } else if (ave.isSpecial()) {
            this._abnormalVisualEffectsSpecial = this._abnormalVisualEffectsSpecial & ~ave.getMask();
         } else {
            this._abnormalVisualEffects = this._abnormalVisualEffects & ~ave.getMask();
         }
      }

      if (update) {
         this.updateAbnormalEffect();
      }
   }

   public void startFakeDeath() {
      if (this.isPlayer()) {
         this.asPlayer().setFakeDeath(true);
         this.abortAttack();
         this.abortCast();
         this.stopMove(null);
         this.getAI().notifyActionFakeDeath();
         this.broadcastPacket(new ChangeWaitType(this, 2));
         if (PlayerConfig.FAKE_DEATH_UNTARGET) {
            World.forEachVisibleObject(this, Creature.class, c -> {
               if (c.getTarget() == this) {
                  c.setTarget(null);
               }
            });
         }
      }
   }

   public void startStunning() {
      this.abortAttack();
      this.abortCast();
      this.stopMove(null);
      this.getAI().notifyActionStunned();
      if (!this.isSummon()) {
         this.getAI().setIntentionIdle();
      }

      this.updateAbnormalEffect();
   }

   public void startParalyze() {
      this.abortAttack();
      this.abortCast();
      this.stopMove(null);
      this.getAI().notifyActionParalyzed();
   }

   public void stopAllEffects() {
      this._effectList.stopAllEffects();
   }

   public void stopAllEffectsExceptThoseThatLastThroughDeath() {
      this._effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
   }

   public void stopSkillEffects(SkillFinishType type, int skillId) {
      this._effectList.stopSkillEffects(type, skillId);
   }

   public void stopEffects(EffectType type) {
      this._effectList.stopEffects(type);
   }

   public void stopEffectsOnAction() {
      this._effectList.stopEffectsOnAction();
   }

   public void stopEffectsOnDamage(boolean awake) {
      this._effectList.stopEffectsOnDamage(awake);
   }

   public void stopFakeDeath(boolean removeEffects) {
      if (removeEffects) {
         this.stopEffects(EffectType.FAKE_DEATH);
      }

      if (this.isPlayer()) {
         Player player = this.asPlayer();
         player.setFakeDeath(false);
         player.setRecentFakeDeath(true);
      }

      this.broadcastPacket(new ChangeWaitType(this, 3));
      this.broadcastPacket(new Revive(this));
   }

   public void stopStunning(boolean removeEffects) {
      if (removeEffects) {
         this.stopEffects(EffectType.STUN);
      }

      if (!this.isPlayer()) {
         this.getAI().notifyActionThink();
      }

      this.updateAbnormalEffect();
   }

   public void stopTransformation(boolean removeEffects) {
      if (removeEffects) {
         this._effectList.stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.TRANSFORM);
      }

      if (this.isPlayer() && this.getTransformation() != null) {
         this.untransform();
      }

      if (!this.isPlayer()) {
         this.getAI().notifyActionThink();
      }

      this.updateAbnormalEffect();
   }

   public abstract void updateAbnormalEffect();

   public void updateEffectIcons() {
      this.updateEffectIcons(false);
   }

   public void updateEffectIcons(boolean partyOnly) {
   }

   public boolean isAffectedBySkill(int skillId) {
      return this._effectList.isAffectedBySkill(skillId);
   }

   public void addStatFunc(AbstractFunction function) {
      if (function != null) {
         synchronized (this) {
            if (this._calculators == NPC_STD_CALCULATOR) {
               this._calculators = new Calculator[Stat.NUM_STATS];

               for (int i = 0; i < Stat.NUM_STATS; i++) {
                  if (NPC_STD_CALCULATOR[i] != null) {
                     this._calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
                  }
               }
            }

            int stat = function.getStat().ordinal();
            if (this._calculators[stat] == null) {
               this._calculators[stat] = new Calculator();
            }

            this._calculators[stat].addFunc(function);
         }
      }
   }

   public void addStatFuncs(List<AbstractFunction> functions) {
      List<Stat> modifiedStats = new ArrayList<>();

      for (AbstractFunction f : functions) {
         modifiedStats.add(f.getStat());
         this.addStatFunc(f);
      }

      this.broadcastModifiedStats(modifiedStats);
   }

   public void removeStatFunc(AbstractFunction function) {
      if (function != null) {
         int stat = function.getStat().ordinal();
         synchronized (this) {
            if (this._calculators[stat] != null) {
               this._calculators[stat].removeFunc(function);
               if (this._calculators[stat].size() == 0) {
                  this._calculators[stat] = null;
               }

               if (this.isNpc()) {
                  int i = 0;

                  while (i < Stat.NUM_STATS && Calculator.equalsCals(this._calculators[i], NPC_STD_CALCULATOR[i])) {
                     i++;
                  }

                  if (i >= Stat.NUM_STATS) {
                     this._calculators = NPC_STD_CALCULATOR;
                  }
               }
            }
         }
      }
   }

   public void removeStatFuncs(AbstractFunction[] functions) {
      List<Stat> modifiedStats = new ArrayList<>();

      for (AbstractFunction f : functions) {
         modifiedStats.add(f.getStat());
         this.removeStatFunc(f);
      }

      this.broadcastModifiedStats(modifiedStats);
   }

   public void removeStatsOwner(Object owner) {
      List<Stat> modifiedStats = null;
      int i = 0;
      synchronized (this) {
         for (Calculator calc : this._calculators) {
            if (calc != null) {
               if (modifiedStats != null) {
                  modifiedStats.addAll(calc.removeOwner(owner));
               } else {
                  modifiedStats = calc.removeOwner(owner);
               }

               if (calc.size() == 0) {
                  this._calculators[i] = null;
               }
            }

            i++;
         }

         if (this.isNpc()) {
            i = 0;

            while (i < Stat.NUM_STATS && Calculator.equalsCals(this._calculators[i], NPC_STD_CALCULATOR[i])) {
               i++;
            }

            if (i >= Stat.NUM_STATS) {
               this._calculators = NPC_STD_CALCULATOR;
            }
         }

         if (this.isSummon()) {
            if (this.getCurrentHp() > this.getMaxHp()) {
               this.setCurrentHp(this.getMaxHp());
            }

            if (this.getCurrentMp() > this.getMaxMp()) {
               this.setCurrentMp(this.getMaxMp());
            }
         }

         this.broadcastModifiedStats(modifiedStats);
      }
   }

   protected void broadcastModifiedStats(List<Stat> stats) {
      if (this.isSpawned()) {
         if (stats != null && !stats.isEmpty()) {
            if (this.isSummon()) {
               Summon summon = this.asSummon();
               if (summon.getOwner() != null) {
                  summon.updateAndBroadcastStatus(1);
               }
            } else {
               boolean broadcastFull = false;
               StatusUpdate su = new StatusUpdate(this);

               for (Stat stat : stats) {
                  if (stat == Stat.POWER_ATTACK_SPEED) {
                     su.addAttribute(18, (int)this._stat.getPAtkSpd());
                  } else if (stat == Stat.MAGIC_ATTACK_SPEED) {
                     su.addAttribute(24, this._stat.getMAtkSpd());
                  } else if (stat == Stat.MOVE_SPEED) {
                     broadcastFull = true;
                  }
               }

               if (this.isPlayer()) {
                  Player player = this.asPlayer();
                  if (broadcastFull) {
                     player.broadcastUserInfo();
                  } else {
                     player.updateUserInfo();
                     if (su.hasAttributes()) {
                        this.broadcastPacket(su);
                     }
                  }

                  Summon summon = player.getSummon();
                  if (summon != null && this.isAffected(EffectFlag.SERVITOR_SHARE)) {
                     summon.broadcastStatusUpdate();
                  }
               } else if (this.isNpc()) {
                  if (broadcastFull) {
                     World.forEachVisibleObject(this, Player.class, playerx -> {
                        if (this.isVisibleFor(playerx)) {
                           if (this.isFakePlayer()) {
                              playerx.sendPacket(new FakePlayerInfo(this.asNpc()));
                           } else if (this._stat.getRunSpeed() == 0.0) {
                              playerx.sendPacket(new ServerObjectInfo(this.asNpc(), playerx));
                           } else {
                              playerx.sendPacket(new NpcInfo(this.asNpc(), playerx));
                           }
                        }
                     });
                  } else if (su.hasAttributes()) {
                     this.broadcastPacket(su);
                  }
               } else if (su.hasAttributes()) {
                  this.broadcastPacket(su);
               }
            }
         }
      }
   }

   public int getXdestination() {
      MoveData move = this._move;
      return move != null ? move.xDestination : this.getX();
   }

   public int getYdestination() {
      MoveData move = this._move;
      return move != null ? move.yDestination : this.getY();
   }

   public int getZdestination() {
      MoveData move = this._move;
      return move != null ? move.zDestination : this.getZ();
   }

   public boolean isInCombat() {
      return this.hasAI() && (this.getAI().getAttackTarget() != null || this.getAI().isAutoAttacking());
   }

   public boolean isMoving() {
      return this._move != null;
   }

   public boolean isOnGeodataPath() {
      MoveData move = this._move;
      return move == null ? false : this.isOnGeodataPath(move);
   }

   public boolean isOnGeodataPath(MoveData move) {
      return move.onGeodataPathIndex == -1 ? false : move.onGeodataPathIndex != move.geoPath.size() - 1;
   }

   public List<GeoLocation> getGeoPath() {
      MoveData move = this._move;
      return move != null ? move.geoPath : null;
   }

   public boolean isCastingNow() {
      return this._isCastingNow;
   }

   public void setCastingNow(boolean value) {
      this._isCastingNow = value;
   }

   public boolean isCastingSimultaneouslyNow() {
      return this._isCastingSimultaneouslyNow;
   }

   public void setCastingSimultaneouslyNow(boolean value) {
      this._isCastingSimultaneouslyNow = value;
   }

   public boolean canAbortCast() {
      return this._castInterruptTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public int getCastInterruptTime() {
      return this._castInterruptTime;
   }

   public boolean isAttackingOrCastingNow() {
      return this.isAttackingNow() || this.isRangeAttackingNow() || this.isCastingNow() || this.isCastingSimultaneouslyNow();
   }

   public boolean isAttackingNow() {
      return this._attackEndTime > System.nanoTime();
   }

   public boolean isRangeAttackingNow() {
      return this._disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public void abortAttack() {
      if (this.isAttackingNow()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public void abortCast() {
      if (this._isCastingNow || this._isCastingSimultaneouslyNow) {
         Future<?> future = this._skillCast;
         if (future != null) {
            future.cancel(true);
            this._skillCast = null;
         }

         future = this._skillCast2;
         if (future != null) {
            future.cancel(true);
            this._skillCast2 = null;
         }

         if (this.isChanneling()) {
            this.getSkillChannelizer().stopChanneling();
         }

         if (this._allSkillsDisabled) {
            this.enableAllSkills();
         }

         this.setCastingNow(false);
         this.setCastingSimultaneouslyNow(false);
         this._castInterruptTime = 0;
         if (this.isPlayer()) {
            this.getAI().notifyActionFinishCasting();
         }

         this.broadcastPacket(new MagicSkillCanceled(this.getObjectId()));
         this.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public boolean updatePosition() {
      if (!this.isSpawned()) {
         this._move = null;
         return true;
      } else {
         MoveData move = this._move;
         if (move == null) {
            return true;
         } else {
            if (move.moveTimestamp == 0) {
               move.moveTimestamp = move.moveStartTime;
               move.xAccurate = this.getX();
               move.yAccurate = this.getY();
            }

            int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
            if (move.moveTimestamp == gameTicks) {
               return false;
            } else {
               int xPrev = this.getX();
               int yPrev = this.getY();
               int zPrev = this.getZ();
               double dx = move.xDestination - move.xAccurate;
               double dy = move.yDestination - move.yAccurate;
               double dz = move.zDestination - zPrev;
               if (this.isPlayer() && !this._isFlying) {
                  if (this._cursorKeyMovement) {
                     double angle = LocationUtil.convertHeadingToDegree(this.getHeading());
                     double radian = Math.toRadians(angle);
                     double course = Math.toRadians(180.0);
                     double frontDistance = 10.0 * (this._stat.getMoveSpeed() / 100.0);
                     int x1 = (int)(Math.cos(Math.PI + radian + course) * frontDistance);
                     int y1 = (int)(Math.sin(Math.PI + radian + course) * frontDistance);
                     int x = xPrev + x1;
                     int y = yPrev + y1;
                     if (!GeoEngine.getInstance().canMoveToTarget(xPrev, yPrev, zPrev, x, y, zPrev, this.getInstanceId())) {
                        move.onGeodataPathIndex = -1;
                        this.stopMove(this.getLocation());
                        return true;
                     }
                  } else {
                     double distance = Math.sqrt(dx * dx + dy * dy);
                     if (distance > 3000.0) {
                        double angle = LocationUtil.convertHeadingToDegree(this.getHeading());
                        double radian = Math.toRadians(angle);
                        double course = Math.toRadians(180.0);
                        double frontDistance = 10.0 * (this._stat.getMoveSpeed() / 100.0);
                        int x1 = (int)(Math.cos(Math.PI + radian + course) * frontDistance);
                        int y1 = (int)(Math.sin(Math.PI + radian + course) * frontDistance);
                        int x = xPrev + x1;
                        int y = yPrev + y1;
                        if (!GeoEngine.getInstance().canMoveToTarget(xPrev, yPrev, zPrev, x, y, zPrev, this.getInstanceId())) {
                           move.onGeodataPathIndex = -1;
                           if (this.hasAI()) {
                              if (this.getAI().isFollowing()) {
                                 this.getAI().stopFollow();
                              }

                              this.getAI().setIntentionIdle();
                           }

                           return true;
                        }
                     } else if (this.hasAI() && this.getAI().getIntention() == Intention.ATTACK) {
                        double angle = LocationUtil.convertHeadingToDegree(this.getHeading());
                        double radian = Math.toRadians(angle);
                        double course = Math.toRadians(180.0);
                        double frontDistance = 10.0 * (this._stat.getMoveSpeed() / 100.0);
                        int x1 = (int)(Math.cos(Math.PI + radian + course) * frontDistance);
                        int y1 = (int)(Math.sin(Math.PI + radian + course) * frontDistance);
                        int x = xPrev + x1;
                        int y = yPrev + y1;
                        if (!GeoEngine.getInstance().canMoveToTarget(xPrev, yPrev, zPrev, x, y, zPrev, this.getInstanceId())) {
                           move.onGeodataPathIndex = -1;
                           this.broadcastPacket(new StopMove(this));
                           return true;
                        }
                     } else {
                        WorldRegion region = this.getWorldRegion();
                        if (region != null) {
                           boolean hasDoors = !region.getDoors().isEmpty();
                           boolean hasFences = !region.getFences().isEmpty();
                           if (hasDoors || hasFences) {
                              double angle = LocationUtil.convertHeadingToDegree(this.getHeading());
                              double radian = Math.toRadians(angle);
                              double course = Math.toRadians(180.0);
                              double frontDistance = 10.0 * (this._stat.getMoveSpeed() / 100.0);
                              int x1 = (int)(Math.cos(Math.PI + radian + course) * frontDistance);
                              int y1 = (int)(Math.sin(Math.PI + radian + course) * frontDistance);
                              int x = xPrev + x1;
                              int y = yPrev + y1;
                              if (hasDoors && DoorData.getInstance().checkIfDoorsBetween(xPrev, yPrev, zPrev, x, y, zPrev, this.getInstanceId(), false)
                                 || hasFences && FenceData.getInstance().checkIfFenceBetween(xPrev, yPrev, zPrev, x, y, zPrev, this.getInstanceId())) {
                                 move.onGeodataPathIndex = -1;
                                 if (this.hasAI()) {
                                    if (this.getAI().isFollowing()) {
                                       this.getAI().stopFollow();
                                    }

                                    this.getAI().setIntentionIdle();
                                 }

                                 this.stopMove(null);
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }

               double delta = dx * dx + dy * dy;
               boolean isFloating = this._isFlying || this.isInsideZone(ZoneId.WATER) && !this.isInsideZone(ZoneId.CASTLE);
               if (!isFloating && delta < 10000.0 && dz * dz > 2500.0) {
                  delta = Math.sqrt(delta);
               } else {
                  delta = Math.sqrt(delta + dz * dz);
               }

               WorldObject target = this._target;
               double collision;
               if (target != null && target.isCreature() && this.hasAI() && this.getAI().getIntention() == Intention.ATTACK) {
                  collision = target.asCreature().getTemplate().getCollisionRadius();
               } else {
                  collision = this.getTemplate().getCollisionRadius();
               }

               delta = Math.max(1.0E-5, delta - collision);
               double distFraction = Double.MAX_VALUE;
               if (delta > 1.0) {
                  double distPassed = this._stat.getMoveSpeed() * (gameTicks - move.moveTimestamp) / 10.0;
                  distFraction = distPassed / delta;
               }

               boolean arrived = distFraction > 1.0;
               if (arrived) {
                  super.setXYZ(move.xDestination, move.yDestination, move.zDestination);
               } else {
                  int newZ = zPrev + (int)(dz * distFraction + 0.5);
                  move.xAccurate += dx * distFraction;
                  move.yAccurate += dy * distFraction;
                  if (this.isAttackable() && !isFloating && Math.abs(newZ - zPrev) > 300) {
                     Spawn spawn = this.asAttackable().getSpawn();
                     if (spawn != null) {
                        this.teleToLocation(spawn, this.getInstanceId(), 0);
                        this.getAttackByList().clear();
                        this.asAttackable().clearAggroList();
                        this.getAI().setIntentionIdle();
                     }

                     return true;
                  }

                  super.setXYZ((int)move.xAccurate, (int)move.yAccurate, newZ);
               }

               this.revalidateZone(false);
               move.moveTimestamp = gameTicks;
               if (arrived && !this.isOnGeodataPath()) {
                  this.broadcastMoveToLocation(true);
               } else if (this.isAttackable() && target != null) {
                  this.broadcastMoveToLocation();
               }

               return arrived;
            }
         }
      }
   }

   public void revalidateZone(boolean force) {
      if (force || !(this.calculateDistance3D(this._lastZoneValidateLocation) < (this.isNpc() && !this.isInCombat() ? NpcConfig.MAX_DRIFT_RANGE : 100))) {
         this._lastZoneValidateLocation.setXYZ(this);
         ZoneRegion region = ZoneManager.getInstance().getRegion(this);
         if (region != null) {
            region.revalidateZones(this);
         } else {
            World.disposeOutOfBoundsObject(this);
         }
      }
   }

   public void stopMove(Location loc) {
      this._move = null;
      if (loc != null) {
         this.setXYZ(loc.getX(), loc.getY(), loc.getZ());
         this.setHeading(loc.getHeading());
         this.revalidateZone(true);
      }

      this.broadcastPacket(new StopMove(this));
   }

   public boolean isShowSummonAnimation() {
      return this._showSummonAnimation;
   }

   public void setShowSummonAnimation(boolean showSummonAnimation) {
      this._showSummonAnimation = showSummonAnimation;
   }

   public void setTarget(WorldObject object) {
      if (object != null && !object.isSpawned()) {
         this._target = null;
      } else {
         this._target = object;
      }
   }

   public int getTargetId() {
      return this._target != null ? this._target.getObjectId() : 0;
   }

   public WorldObject getTarget() {
      return this._target;
   }

   public void moveToLocation(int xValue, int yValue, int zValue, int offsetValue) {
      double speed = this._stat.getMoveSpeed();
      if (!(speed <= 0.0) && !this.isMovementDisabled()) {
         int x = xValue;
         int y = yValue;
         int z = zValue;
         int curX = this.getX();
         int curY = this.getY();
         int curZ = this.getZ();
         double dx = xValue - curX;
         double dy = yValue - curY;
         double dz = zValue - curZ;
         double distance = Math.sqrt(dx * dx + dy * dy);
         boolean verticalMovementOnly = this._isFlying && distance == 0.0 && dz != 0.0;
         if (verticalMovementOnly) {
            distance = Math.abs(dz);
         }

         boolean isInWater = this.isInsideZone(ZoneId.WATER) && !this.isInsideZone(ZoneId.CASTLE);
         if (isInWater && distance > 700.0) {
            double divider = 700.0 / distance;
            x = curX + (int)(divider * dx);
            y = curY + (int)(divider * dy);
            z = curZ + (int)(divider * dz);
            dx = x - curX;
            dy = y - curY;
            dz = z - curZ;
            distance = Math.sqrt(dx * dx + dy * dy);
         }

         double sin;
         double cos;
         if (offsetValue <= 0 && !(distance < 1.0)) {
            sin = dy / distance;
            cos = dx / distance;
         } else {
            int offset = (int)(offsetValue - Math.abs(dz));
            if (offset < 5) {
               offset = 5;
            }

            if (distance < 1.0 || distance - offset <= 0.0) {
               this.getAI().notifyActionArrived();
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return;
            }

            sin = dy / distance;
            cos = dx / distance;
            distance -= offset - 5;
            x = curX + (int)(distance * cos);
            y = curY + (int)(distance * sin);
         }

         MoveData move = new MoveData();
         WorldRegion region = this.getWorldRegion();
         move.disregardingGeodata = region == null || !region.areNeighborsActive();
         move.onGeodataPathIndex = -1;
         if (!move.disregardingGeodata && !this._isFlying && !isInWater && !this.isVehicle() && !this._cursorKeyMovement) {
            boolean isInVehicle = this.isPlayer() && this.asPlayer().getVehicle() != null;
            if (isInVehicle) {
               move.disregardingGeodata = true;
            }

            if (GeoEngineConfig.PATHFINDING > 0 && !(this instanceof QuestGuard)) {
               int originalX = x;
               int originalY = y;
               int originalZ = z;
               double originalDistance = distance;
               int gtx = x - -294912 >> 4;
               int gty = y - -262144 >> 4;
               MoveData currentMove = this._move;
               if (currentMove != null && this.isOnGeodataPath(currentMove)) {
                  if (gtx == currentMove.geoPathGtx && gty == currentMove.geoPathGty) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  currentMove.onGeodataPathIndex = -1;
               }

               boolean directMove = this.isPlayer() && this.hasAI() && this.getAI().getIntention() == Intention.ATTACK;
               boolean isDestinationInWater = this.isPlayer() && ZoneManager.getInstance().getZone(x, y, z, WaterZone.class) != null;
               if (directMove
                  || !isInVehicle
                     && !isDestinationInWater
                     && (!this.isPlayer() || !(distance > 3000.0))
                     && (!this.isMonster() || !(Math.abs(dz) > 100.0) || !GeoEngine.getInstance().canSeeTarget(this, this._target))
                     && (curZ - z <= 300 || !(distance < 300.0))) {
                  Location destination = GeoEngine.getInstance().getValidLocation(curX, curY, curZ, x, y, z, this.getInstanceId());
                  x = destination.getX();
                  y = destination.getY();
                  if (!this.isPlayer()) {
                     z = destination.getZ();
                  }

                  dx = x - curX;
                  dy = y - curY;
                  dz = z - curZ;
                  distance = verticalMovementOnly ? dz * dz : Math.sqrt(dx * dx + dy * dy);
               }

               if (!directMove && originalDistance - distance > 30.0 && !this.isControlBlocked() && !isInVehicle) {
                  move.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, this.getInstanceId(), this.isPlayer());
                  boolean found = move.geoPath != null && move.geoPath.size() > 1;
                  if (!found && this.isAttackable()) {
                     int xMin = Math.min(curX, originalX);
                     int xMax = Math.max(curX, originalX);
                     int yMin = Math.min(curY, originalY);
                     int yMax = Math.max(curY, originalY);
                     int maxDiff = Math.min(Math.max(xMax - xMin, yMax - yMin), 500);
                     xMin -= maxDiff;
                     xMax += maxDiff;
                     yMin -= maxDiff;
                     yMax += maxDiff;
                     int destinationX = 0;
                     int destinationY = 0;
                     double shortDistance = Double.MAX_VALUE;

                     for (int sX = xMin; sX < xMax; sX += 500) {
                        for (int sY = yMin; sY < yMax; sY += 500) {
                           double tdx = sX - originalX;
                           double tdy = sY - originalY;
                           double tempDistance = Math.sqrt(tdx * tdx + tdy * tdy);
                           if (tempDistance < shortDistance) {
                              List<GeoLocation> tempPath = PathFinding.getInstance().findPath(curX, curY, curZ, sX, sY, originalZ, this.getInstanceId(), false);
                              found = tempPath != null && tempPath.size() > 1;
                              if (found) {
                                 shortDistance = tempDistance;
                                 move.geoPath = tempPath;
                                 destinationX = sX;
                                 destinationY = sY;
                              }
                           }
                        }
                     }

                     found = move.geoPath != null && move.geoPath.size() > 1;
                     if (found) {
                        originalX = destinationX;
                        originalY = destinationY;
                     }
                  }

                  if (found) {
                     move.onGeodataPathIndex = 0;
                     move.geoPathGtx = gtx;
                     move.geoPathGty = gty;
                     move.geoPathAccurateTx = originalX;
                     move.geoPathAccurateTy = originalY;
                     x = ((GeoLocation)move.geoPath.get(move.onGeodataPathIndex)).getX();
                     y = ((GeoLocation)move.geoPath.get(move.onGeodataPathIndex)).getY();
                     z = ((GeoLocation)move.geoPath.get(move.onGeodataPathIndex)).getZ();
                     dx = x - curX;
                     dy = y - curY;
                     dz = z - curZ;
                     distance = verticalMovementOnly ? dz * dz : Math.sqrt(dx * dx + dy * dy);
                     sin = dy / distance;
                     cos = dx / distance;
                  } else {
                     move.disregardingGeodata = true;
                     x = originalX;
                     y = originalY;
                     z = originalZ;
                     distance = originalDistance;
                  }
               }

               if (this.isPlayable()
                  && !this._cursorKeyMovement
                  && move.geoPath == null
                  && !isInVehicle
                  && distance < 3000.0
                  && (curZ - z <= 300 || !(distance < 300.0))) {
                  Location destination = GeoEngine.getInstance().getValidLocation(curX, curY, curZ, x, y, z, this.getInstanceId());
                  x = destination.getX();
                  y = destination.getY();
                  z = destination.getZ();
                  dx = x - curX;
                  dy = y - curY;
                  dz = z - curZ;
                  distance = verticalMovementOnly ? dz * dz : Math.sqrt(dx * dx + dy * dy);
               }
            }

            if (distance < 1.0 && (GeoEngineConfig.PATHFINDING > 0 || this.isPlayable())) {
               Intention intention = this.hasAI() ? this.getAI().getIntention() : null;
               boolean pendingTargetAction = intention == Intention.ATTACK || intention == Intention.CAST;
               if (this.isSummon() && !pendingTargetAction) {
                  if (this.getAI().getFollowTarget() != this.asPlayer()) {
                     this.asSummon().setFollowStatus(false);
                     this.getAI().setIntentionIdle();
                  }
               } else if (!pendingTargetAction) {
                  this.getAI().setIntentionIdle();
               }

               this.sendPacket(ActionFailed.STATIC_PACKET);
               return;
            }
         }

         if ((this._isFlying || isInWater) && !verticalMovementOnly) {
            distance = Math.sqrt(distance * distance + dz * dz);
         }

         int ticksToMove = (int)(10.0 * distance / speed);
         move.xDestination = x;
         move.yDestination = y;
         move.zDestination = z;
         move.heading = 0;
         if (!verticalMovementOnly) {
            this.setHeading(LocationUtil.calculateHeadingFrom(cos, sin));
         }

         move.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
         this._move = move;
         MovementTaskManager.getInstance().registerMovingObject(this);
         if (ticksToMove * 100 > 3000) {
            ThreadPool.schedule(() -> this.getAI().notifyActionArrivedRevalidate(), 2000L);
         }
      } else {
         this.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public boolean moveToNextRoutePoint() {
      MoveData move = this._move;
      if (move == null) {
         return false;
      } else if (!this.isOnGeodataPath(move)) {
         this._move = null;
         return false;
      } else {
         double speed = this._stat.getMoveSpeed();
         if (!(speed <= 0.0) && !this.isMovementDisabled()) {
            int curX = this.getX();
            int curY = this.getY();
            MoveData newMove = new MoveData();
            newMove.onGeodataPathIndex = move.onGeodataPathIndex + 1;
            newMove.geoPath = move.geoPath;
            newMove.geoPathGtx = move.geoPathGtx;
            newMove.geoPathGty = move.geoPathGty;
            newMove.geoPathAccurateTx = move.geoPathAccurateTx;
            newMove.geoPathAccurateTy = move.geoPathAccurateTy;
            if (move.onGeodataPathIndex == move.geoPath.size() - 2) {
               newMove.xDestination = move.geoPathAccurateTx;
               newMove.yDestination = move.geoPathAccurateTy;
               newMove.zDestination = ((GeoLocation)move.geoPath.get(newMove.onGeodataPathIndex)).getZ();
            } else {
               newMove.xDestination = ((GeoLocation)move.geoPath.get(newMove.onGeodataPathIndex)).getX();
               newMove.yDestination = ((GeoLocation)move.geoPath.get(newMove.onGeodataPathIndex)).getY();
               newMove.zDestination = ((GeoLocation)move.geoPath.get(newMove.onGeodataPathIndex)).getZ();
            }

            double ndx = newMove.xDestination - curX;
            double ndy = newMove.yDestination - curY;
            double distance = Math.sqrt(ndx * ndx + ndy * ndy);
            if (distance != 0.0) {
               this.setHeading(LocationUtil.calculateHeadingFrom(curX, curY, newMove.xDestination, newMove.yDestination));
            }

            int ticksToMove = (int)(10.0 * distance / speed);
            newMove.heading = 0;
            newMove.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
            this._move = newMove;
            MovementTaskManager.getInstance().registerMovingObject(this);
            if (ticksToMove * 100 > 3000) {
               ThreadPool.schedule(() -> this.getAI().notifyActionArrivedRevalidate(), 2000L);
            }

            this.broadcastMoveToLocation(true);
            return true;
         } else {
            this._move = null;
            return false;
         }
      }
   }

   public boolean validateMovementHeading(int heading) {
      MoveData move = this._move;
      if (move == null) {
         return true;
      } else {
         boolean result = true;
         if (move.heading != heading) {
            result = move.heading == 0;
            move.heading = heading;
         }

         return result;
      }
   }

   public boolean isInsideRadius2D(ILocational loc, int radius) {
      return this.isInsideRadius2D(loc.getX(), loc.getY(), loc.getZ(), radius);
   }

   public boolean isInsideRadius2D(int x, int y, int z, int radius) {
      return this.calculateDistance2D(x, y, z) < radius;
   }

   public boolean isInsideRadius3D(ILocational loc, int radius) {
      return this.isInsideRadius3D(loc.getX(), loc.getY(), loc.getZ(), radius);
   }

   public boolean isInsideRadius3D(int x, int y, int z, int radius) {
      return this.calculateDistance3D(x, y, z) < radius;
   }

   protected boolean checkAndEquipArrows() {
      return true;
   }

   protected boolean checkAndEquipBolts() {
      return true;
   }

   public synchronized void addExpAndSp(double addToExp, double addToSp) {
   }

   public abstract Item getActiveWeaponInstance();

   public abstract Weapon getActiveWeaponItem();

   public abstract Item getSecondaryWeaponInstance();

   public abstract ItemTemplate getSecondaryWeaponItem();

   public void onHitTimer(Creature target, int damageValue, boolean crit, boolean miss, byte shld, boolean soulshot, boolean rechargeShots) {
      if (target == null || this.isAlikeDead()) {
         this.getAI().notifyActionCancel();
      } else if (!this.isFakePlayer() || FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC || !target.isFakePlayer()) {
         if ((!this.isNpc() || !target.isAlikeDead()) && !target.isDead() && (this.isInSurroundingRegion(target) || this.isDoor())) {
            if (miss) {
               if (target.hasAI()) {
                  target.getAI().notifyActionEvaded(this);
               }

               this.notifyAttackAvoid(target, false);
            }

            int damage = damageValue;
            this.sendDamageMessage(target, damageValue, false, crit, miss);
            if (target.isRaid() && target.giveRaidCurse() && !NpcConfig.RAID_DISABLE_CURSE && this.getLevel() > target.getLevel() + 8) {
               Skill skill = CommonSkill.RAID_CURSE2.getSkill();
               if (skill != null) {
                  this.abortAttack();
                  this.abortCast();
                  this.getAI().setIntentionIdle();
                  skill.applyEffects(target, this);
               } else {
                  LOGGER.warning("Skill 4515 at level 1 is missing in DP.");
               }

               damage = 0;
            }

            if (target.isPlayer()) {
               Player enemy = target.asPlayer();
               enemy.getAI().clientStartAutoAttack();
            }

            if (!miss && damage > 0) {
               Weapon weapon = this.getActiveWeaponItem();
               boolean isBow = weapon != null && (weapon.getItemType() == WeaponType.BOW || weapon.getItemType() == WeaponType.CROSSBOW);
               int reflectedDamage = 0;
               if (!isBow && !target.isInvul() && (!target.isRaid() || this.asPlayer() == null || this.asPlayer().getLevel() <= target.getLevel() + 8)) {
                  double reflectPercent = target.getStat().calcStat(Stat.REFLECT_DAMAGE_PERCENT, 0.0, null, null);
                  if (reflectPercent > 0.0) {
                     reflectedDamage = (int)(reflectPercent / 100.0 * damage);
                     if (reflectedDamage > target.getMaxHp()) {
                        reflectedDamage = target.getMaxHp();
                     }
                  }
               }

               target.reduceCurrentHp(damage, this, null);
               target.notifyDamageReceived(damage, this, null, crit, false);
               if (reflectedDamage > 0) {
                  this.reduceCurrentHp(reflectedDamage, target, true, false, null);
                  this.notifyDamageReceived(reflectedDamage, target, null, crit, false);
               }

               if (!isBow) {
                  double absorbPercent = this._stat.calcStat(Stat.ABSORB_DAMAGE_PERCENT, 0.0, null, null);
                  if (absorbPercent > 0.0) {
                     int maxCanAbsorb = (int)(this._stat.getMaxRecoverableHp() - this._status.getCurrentHp());
                     int absorbDamage = (int)(absorbPercent / 100.0 * damage);
                     if (absorbDamage > maxCanAbsorb) {
                        absorbDamage = maxCanAbsorb;
                     }

                     if (absorbDamage > 0) {
                        this.setCurrentHp(this._status.getCurrentHp() + absorbDamage);
                     }
                  }

                  absorbPercent = this._stat.calcStat(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0.0, null, null);
                  if (absorbPercent > 0.0) {
                     int maxCanAbsorbx = (int)(this._stat.getMaxRecoverableMp() - this._status.getCurrentMp());
                     int absorbDamagex = (int)(absorbPercent / 100.0 * damage);
                     if (absorbDamagex > maxCanAbsorbx) {
                        absorbDamagex = maxCanAbsorbx;
                     }

                     if (absorbDamagex > 0) {
                        this.setCurrentMp(this._status.getCurrentMp() + absorbDamagex);
                     }
                  }
               }

               if (target.hasAI()) {
                  target.getAI().notifyActionAttacked(this);
               }

               this.getAI().clientStartAutoAttack();
               if (this.isSummon()) {
                  Player owner = this.asSummon().getOwner();
                  if (owner != null) {
                     owner.getAI().clientStartAutoAttack();
                  }
               }

               if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                  target.breakAttack();
                  target.breakCast();
               }

               if (!this._triggerSkills.isEmpty()) {
                  for (OptionSkillHolder holder : this._triggerSkills.values()) {
                     if ((!crit && holder.getSkillType() == OptionSkillType.ATTACK || holder.getSkillType() == OptionSkillType.CRITICAL && crit)
                        && Rnd.get(100) < holder.getChance()) {
                        this.makeTriggerCast(holder.getSkill(), target);
                     }
                  }
               }

               if (crit && weapon != null) {
                  weapon.castOnCriticalSkill(this, target);
               }
            }

            if (rechargeShots) {
               this.rechargeShots(true, false);
            }
         } else {
            if (rechargeShots) {
               this.rechargeShots(true, false);
            }

            this.getAI().notifyActionCancel();
            this.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }

   public void breakAttack() {
      if (this.isAttackingNow()) {
         this.abortAttack();
         if (this.isPlayer()) {
            this.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
         }
      }
   }

   public void breakCast() {
      if (this._isCastingNow && this.canAbortCast() && this._lastSkillCast != null && (this._lastSkillCast.isMagic() || this._lastSkillCast.isStatic())) {
         this.abortCast();
         if (this.isPlayer()) {
            this.sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
         }
      }
   }

   protected void reduceArrowCount(boolean bolts) {
   }

   public void onForcedAttack(Player player) {
      if (this.isInsidePeaceZone(player)) {
         player.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         if (player.isInOlympiadMode() && player.getTarget() != null && player.getTarget().isPlayable()) {
            Player target = null;
            WorldObject object = player.getTarget();
            if (object != null && object.isPlayable()) {
               target = object.asPlayer();
            }

            if (target == null || target.isInOlympiadMode() && (!player.isOlympiadStart() || player.getOlympiadGameId() != target.getOlympiadGameId())) {
               player.sendPacket(ActionFailed.STATIC_PACKET);
               return;
            }
         }

         if (player.getTarget() != null && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (player.isConfused()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (player.getBlockCheckerArena() != -1) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            player.getAI().setIntentionAttack(this);
         }
      }
   }

   public boolean isInsidePeaceZone(Player attacker) {
      return this.isInsidePeaceZone(attacker, this);
   }

   public boolean isInsidePeaceZone(Player attacker, WorldObject target) {
      return !attacker.getAccessLevel().allowPeaceAttack() && this.isInsidePeaceZone((WorldObject)attacker, target);
   }

   public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target) {
      if (target == null || !target.isPlayable() && !target.isFakePlayer() || !attacker.isPlayable()) {
         return false;
      } else if (InstanceManager.getInstance().getInstance(this.getInstanceId()).isPvP()) {
         return false;
      } else if (TerritoryWarManager.PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE
         && TerritoryWarManager.getInstance().isTWInProgress()
         && target.isPlayer()
         && target.asPlayer().isCombatFlagEquipped()) {
         return false;
      } else {
         if (PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE) {
            Player player = this.asPlayer();
            if (player != null && player.getKarma() > 0) {
               return false;
            }

            Player attackerPlayer = attacker.asPlayer();
            if (attackerPlayer != null && attackerPlayer.getKarma() > 0 && player != null && player.getPvpFlag() > 0) {
               return false;
            }
         }

         return target.isInsideZone(ZoneId.PEACE)
            || attacker.isInsideZone(ZoneId.PEACE)
            || target.isInsideZone(ZoneId.NO_PVP)
            || attacker.isInsideZone(ZoneId.NO_PVP);
      }
   }

   public boolean isInActiveRegion() {
      WorldRegion region = this.getWorldRegion();
      return region != null && region.isActive();
   }

   public boolean isInParty() {
      return false;
   }

   public Party getParty() {
      return null;
   }

   public int calculateTimeBetweenAttacks() {
      return (int)(500000.0 / this._stat.getPAtkSpd());
   }

   public int calculateReuseTime(Weapon weapon) {
      if (this.isTransformed()) {
         final WeaponType reuseType = this.getAttackType();
         if (reuseType == WeaponType.BOW) {
            return (int)(499500.0 * this._stat.getWeaponReuseModifier(null) / this._stat.getPAtkSpd());
         }

         if (reuseType == WeaponType.CROSSBOW) {
            return (int)(399600.0 * this._stat.getWeaponReuseModifier(null) / this._stat.getPAtkSpd());
         }
      }

      return weapon != null && weapon.getReuseDelay() != 0 ? (int)(weapon.getReuseDelay() * 333 / this._stat.getPAtkSpd()) : 0;
   }

   public Skill addSkill(Skill newSkill) {
      Skill oldSkill = null;
      if (newSkill != null) {
         oldSkill = this._skills.put(newSkill.getId(), newSkill);
         if (oldSkill != null) {
            this.removeStatsOwner(oldSkill);
            if (oldSkill.isPassive()) {
               this.stopSkillEffects(SkillFinishType.NORMAL, oldSkill.getId());
            }
         }

         this.addStatFuncs(newSkill.getStatFuncs(null, this));
         if (newSkill.isPassive()) {
            newSkill.applyEffects(this, this, false, true, false, 0);
         }
      }

      return oldSkill;
   }

   public Skill removeSkill(Skill skill, boolean cancelEffect) {
      return skill != null ? this.removeSkill(skill.getId(), cancelEffect) : null;
   }

   public Skill removeSkill(int skillId) {
      return this.removeSkill(skillId, true);
   }

   public Skill removeSkill(int skillId, boolean cancelEffect) {
      Skill oldSkill = this._skills.remove(skillId);
      if (oldSkill != null) {
         if (this._lastSkillCast != null && this._isCastingNow && oldSkill.getId() == this._lastSkillCast.getId()) {
            this.abortCast();
         }

         if (this._lastSimultaneousSkillCast != null && this._isCastingSimultaneouslyNow && oldSkill.getId() == this._lastSimultaneousSkillCast.getId()) {
            this.abortCast();
         }

         if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive()) {
            this.removeStatsOwner(oldSkill);
            this.stopSkillEffects(SkillFinishType.REMOVED, oldSkill.getId());
         }
      }

      return oldSkill;
   }

   public Collection<Skill> getAllSkills() {
      return this._skills.values();
   }

   public Map<Integer, Skill> getSkills() {
      return this._skills;
   }

   public int getSkillLevel(int skillId) {
      Skill skill = this.getKnownSkill(skillId);
      return skill == null ? 0 : skill.getLevel();
   }

   public Skill getKnownSkill(int skillId) {
      return this._skills.get(skillId);
   }

   public int getBuffCount() {
      return this._effectList.getBuffCount();
   }

   public int getDanceCount() {
      return this._effectList.getDanceCount();
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void onMagicLaunchedTimer(MagicUseTask mut) {
      Skill skill = mut.getSkill();
      List<WorldObject> targets = mut.getTargets();
      if (skill == null || targets == null) {
         this.abortCast();
      } else {
         if (targets.isEmpty()) {
            final TargetType launchedTargetType = skill.getTargetType();
            if (launchedTargetType != TargetType.AURA
               && launchedTargetType != TargetType.AURA_CORPSE_MOB
               && launchedTargetType != TargetType.FRONT_AURA
               && launchedTargetType != TargetType.BEHIND_AURA
               && launchedTargetType != TargetType.AURA_FRIENDLY) {
               this.abortCast();
               return;
            }
         }

         int escapeRange = 0;
         if (skill.getEffectRange() > escapeRange) {
            escapeRange = skill.getEffectRange();
         } else if (skill.getCastRange() < 0 && skill.getAffectRange() > 80) {
            escapeRange = skill.getAffectRange();
         }

         if (!targets.isEmpty() && escapeRange > 0) {
            int skipRange = 0;
            int skipLOS = 0;
            int skipPeaceZone = 0;
            List<WorldObject> targetList = new ArrayList<>();

            for (WorldObject target : targets) {
               if (target.isCreature()) {
                  if (!this.isInsideRadius3D(target.getX(), target.getY(), target.getZ(), escapeRange + this._template.getCollisionRadius())) {
                     skipRange++;
                  } else if ((skill.getTargetType() != TargetType.PARTY || !skill.hasEffectType(EffectType.HEAL, new EffectType[0]))
                     && mut.getSkillTime() > 550
                     && !GeoEngine.getInstance().canSeeTarget(this, target)) {
                     skipLOS++;
                  } else {
                     if (skill.hasNegativeEffect()) {
                        if (this.isPlayer()) {
                           if (target.asCreature().isInsidePeaceZone(this.asPlayer())) {
                              skipPeaceZone++;
                              continue;
                           }
                        } else if (target.asCreature().isInsidePeaceZone(this, target)) {
                           skipPeaceZone++;
                           continue;
                        }
                     }

                     targetList.add(target);
                  }
               }
            }

            if (targetList.isEmpty()) {
               if (this.isPlayer()) {
                  if (skipRange > 0) {
                     this.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
                  } else if (skipLOS > 0) {
                     this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                  } else if (skipPeaceZone > 0) {
                     this.sendPacket(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
                  }
               }

               this.abortCast();
               return;
            }

            mut.setTargets(targetList);
         }

         if ((!mut.isSimultaneous() || this._isCastingSimultaneouslyNow)
            && (mut.isSimultaneous() || this._isCastingNow)
            && (!this.isAlikeDead() || skill.isStatic())) {
            mut.setPhase(2);
            if (mut.getSkillTime() == 0) {
               this.onMagicHitTimer(mut);
            } else {
               this._skillCast = ThreadPool.schedule(mut, 400L);
            }
         } else {
            this.getAI().notifyActionCancel();
         }
      }
   }

   public void onMagicHitTimer(MagicUseTask mut) {
      Skill skill = mut.getSkill();
      List<WorldObject> targets = mut.getTargets();
      if (skill != null && targets != null) {
         try {
            for (WorldObject tgt : targets) {
               if (tgt.isPlayable()) {
                  if (this.isPlayer() && tgt.isSummon()) {
                     tgt.asSummon().updateAndBroadcastStatus(1);
                  }
               } else if (this.isPlayable() && tgt.isAttackable()) {
                  Creature target = tgt.asCreature();
                  if (skill.getEffectPoint() > 0) {
                     target.asAttackable().reduceHate(this, skill.getEffectPoint());
                  } else if (skill.getEffectPoint() < 0) {
                     target.asAttackable().addDamageHate(this, 0L, -skill.getEffectPoint());
                  }
               }
            }

            this.rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
            StatusUpdate su = new StatusUpdate(this);
            boolean isSendStatus = false;
            double mpConsume = this._stat.getMpConsume(skill);
            if (mpConsume > 0.0) {
               if (mpConsume > this._status.getCurrentMp()) {
                  this.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
                  this.abortCast();
                  return;
               }

               this._status.reduceMp(mpConsume);
               su.addAttribute(11, (int)this._status.getCurrentMp());
               isSendStatus = true;
            }

            if (skill.getHpConsume() > 0) {
               double consumeHp = skill.getHpConsume();
               if (consumeHp >= this._status.getCurrentHp()) {
                  this.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
                  this.abortCast();
                  return;
               }

               this._status.reduceHp(consumeHp, this, true);
               su.addAttribute(9, (int)this._status.getCurrentHp());
               isSendStatus = true;
            }

            if (isSendStatus) {
               this.sendPacket(su);
            }

            if (this.isPlayer()) {
               Player player = this.asPlayer();
               if (skill.getChargeConsumeCount() > 0) {
                  player.decreaseCharges(skill.getChargeConsumeCount());
               }

               if (skill.getMaxSoulConsumeCount() > 0 && !player.decreaseSouls(skill.getMaxSoulConsumeCount())) {
                  this.abortCast();
                  return;
               }
            }

            this.callSkill(mut.getSkill(), mut.getTargets());
         } catch (NullPointerException var10) {
            LOGGER.log(Level.WARNING, "onMagicHitTimer: ", (Throwable)var10);
         }

         if (mut.getSkillTime() > 0) {
            mut.setCount(mut.getCount() + 1);
         }

         mut.setPhase(3);
         if (mut.getSkillTime() == 0) {
            this.onMagicFinalizer(mut);
         } else if (mut.isSimultaneous()) {
            this._skillCast2 = ThreadPool.schedule(mut, 0L);
         } else {
            this._skillCast = ThreadPool.schedule(mut, 0L);
         }
      } else {
         this.abortCast();
      }
   }

   public void onMagicFinalizer(MagicUseTask mut) {
      if (mut.isSimultaneous()) {
         this._skillCast2 = null;
         this.setCastingSimultaneouslyNow(false);
      } else {
         this._skillCast = null;
         this._castInterruptTime = 0;
         if (mut.getCount() > 0) {
            this.rechargeShots(mut.getSkill().useSoulShot(), mut.getSkill().useSpiritShot());
         }

         this.setCastingNow(false);
         this.setCastingSimultaneouslyNow(false);
         Skill skill = mut.getSkill();
         WorldObject target = !mut.getTargets().isEmpty() ? (WorldObject)mut.getTargets().get(0) : null;
         if (skill.nextActionIsAttack()
            && this._target != this
            && target != null
            && this._target == target
            && this._target.isCreature()
            && target.canBeAttacked()
            && (!this.isPlayer() || !this.asPlayer().isAutoPlaying())) {
            Intention nextIntention = this.getAI().getNextIntention();
            if (nextIntention != Intention.MOVE_TO) {
               if (this.isPlayer()) {
                  SkillUseHolder currSkill = this.asPlayer().getCurrentSkill();
                  if (currSkill == null || !currSkill.isShiftPressed()) {
                     ThreadPool.schedule(() -> {
                        if (!this.isDisabled() && !this.isAttackingOrCastingNow()) {
                           this.getAI().setIntentionAttack(target);
                        }
                     }, 333L);
                  }
               } else {
                  this.getAI().setIntentionAttack(target);
               }
            } else if (this.isPlayer()) {
               ThreadPool.schedule(() -> this.completeMagicFinalizer(skill, target), 333L);
               return;
            }
         }

         this.completeMagicFinalizer(skill, target);
      }
   }

   private void completeMagicFinalizer(Skill skill, WorldObject target) {
      if (skill.hasNegativeEffect() && skill.getTargetType() != TargetType.UNLOCKABLE) {
         this.getAI().clientStartAutoAttack();
      }

      this.getAI().notifyActionFinishCasting();
      this.notifyQuestEventSkillFinished(skill, target);
      if (this.isPlayer()) {
         Player player = this.asPlayer();
         SkillUseHolder queuedSkill = player.getQueuedSkill();
         player.setCurrentSkill(null, false, false);
         if (queuedSkill != null) {
            player.setQueuedSkill(null, false, false);
            ThreadPool.execute(new QueuedMagicUseTask(player, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
         }
      }

      if (this.isChanneling()) {
         this.getSkillChannelizer().stopChanneling();
      }
   }

   protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target) {
   }

   public void callSkill(Skill skill, List<WorldObject> targets) {
      try {
         if (skill.isToggle() && this.isAffectedBySkill(skill.getId())) {
            return;
         }

         if (targets == null || targets.isEmpty()) {
            // [custom 13.09.2026] Исправление Blink (скилл 1448, Soul Hound): self-эффекты скилла
            // должны применяться даже когда рядом нет ни одной валидной цели. В ритейле Blink
            // телепортирует кастера назад (flyRadius 400, flyCourse 0) независимо от целей.
            // Раньше пустой список целей приводил к выходу из callSkill до применения
            // self-эффектов: анимация проигрывалась, телепортация не происходила.
            // Логика повторяет self-блок Skill.activateSkill() (стоп существующего эффекта,
            // затем applyEffects SELF с instant=true).
            if (targets != null && skill.hasEffects(EffectScope.SELF)) {
               if (this.isAffectedBySkill(skill.getId())) {
                  this.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
               }

               skill.applyEffects(this, this, true, false, true, 0);
            }

            return;
         }

         if (GrandBossConfig.DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS || GrandBossConfig.DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS) {
            Player actingPlayer = this.isPlayer()
               ? this.asPlayer()
               : (this.isSummon() ? this.asSummon().getOwner() : (this.isPet() ? this.asPet().getOwner() : null));
            if (actingPlayer != null) {
               boolean isHealSkill = skill.hasEffectType(
                  EffectType.HEAL, new EffectType[]{EffectType.CPHEAL, EffectType.MANAHEAL_BY_LEVEL, EffectType.MANAHEAL_PERCENT, EffectType.REBALANCE_HP}
               );
               boolean isBuffSkill = !isHealSkill
                  && (
                     skill.hasEffectType(EffectType.BUFF, new EffectType[]{EffectType.NOBLESSE_BLESSING, EffectType.CHARM_OF_LUCK, EffectType.RELAXING})
                        || skill.isContinuous() && !skill.isDebuff()
                  );
               boolean blockRaidBoss = isHealSkill && GrandBossConfig.DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS
                  || isBuffSkill && GrandBossConfig.DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS;
               if (blockRaidBoss) {
                  boolean hasRaidBossTarget = false;
                  Iterator activeWeapon = targets.iterator();

                  while (true) {
                     if (activeWeapon.hasNext()) {
                        WorldObject obj = (WorldObject)activeWeapon.next();
                        if (!(obj instanceof RaidBoss)) {
                           continue;
                        }

                        hasRaidBossTarget = true;
                     }

                     if (hasRaidBossTarget) {
                        if (targets.size() == 1) {
                           return;
                        }

                        targets.removeIf(objx -> objx instanceof RaidBoss);
                        if (targets.isEmpty()) {
                           return;
                        }
                     }
                     break;
                  }
               }
            }
         }

         for (WorldObject obj : targets) {
            if (obj != null && obj.isCreature()) {
               Creature target = obj.asCreature();
               Creature targetsAttackTarget = null;
               Creature targetsCastTarget = null;
               if (target.hasAI()) {
                  targetsAttackTarget = target.getAI().getAttackTarget();
                  targetsCastTarget = target.getAI().getCastTarget();
               }

               if (!NpcConfig.RAID_DISABLE_CURSE
                  && (
                     target.isRaid() && target.giveRaidCurse() && this.getLevel() > target.getLevel() + 8
                        || !skill.hasNegativeEffect()
                           && targetsAttackTarget != null
                           && targetsAttackTarget.isRaid()
                           && targetsAttackTarget.giveRaidCurse()
                           && targetsAttackTarget.getAttackByList().contains(target)
                           && this.getLevel() > targetsAttackTarget.getLevel() + 8
                        || !skill.hasNegativeEffect()
                           && targetsCastTarget != null
                           && targetsCastTarget.isRaid()
                           && targetsCastTarget.giveRaidCurse()
                           && targetsCastTarget.getAttackByList().contains(target)
                           && this.getLevel() > targetsCastTarget.getLevel() + 8
                  )) {
                  CommonSkill curse = skill.isMagic() ? CommonSkill.RAID_CURSE : CommonSkill.RAID_CURSE2;
                  Skill curseSkill = curse.getSkill();
                  if (curseSkill != null) {
                     this.abortAttack();
                     this.abortCast();
                     this.getAI().setIntentionIdle();
                     curseSkill.applyEffects(target, this);
                  } else {
                     LOGGER.warning("Skill ID " + curse.getId() + " level " + curse.getLevel() + " is missing in DP!");
                  }

                  return;
               }

               if (skill.isOverhit() && target.isAttackable()) {
                  target.asAttackable().overhitEnabled(true);
               }

               if (!skill.isStatic()) {
                  Weapon activeWeapon = this.getActiveWeaponItem();
                  if (activeWeapon != null && !target.isDead()) {
                     activeWeapon.castOnMagicSkill(this, target, skill);
                  }

                  if (!this._triggerSkills.isEmpty()) {
                     for (OptionSkillHolder holder : this._triggerSkills.values()) {
                        if ((
                              skill.isMagic() && holder.getSkillType() == OptionSkillType.MAGIC
                                 || skill.isPhysical() && holder.getSkillType() == OptionSkillType.ATTACK
                           )
                           && Rnd.get(100) < holder.getChance()) {
                           this.makeTriggerCast(holder.getSkill(), target);
                        }
                     }
                  }
               }
            }
         }

         skill.activateSkill(this, targets);
         if (skill.getEffectPoint() <= 0 && skill.hasNegativeEffect() && (PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK || PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK)) {
            Player actingPlayer;
            if (this.isPlayer()) {
               actingPlayer = this.asPlayer();
            } else if (PvpConfig.FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK && (this.isSummon() || this.isPet())) {
               actingPlayer = this.isSummon() ? this.asSummon().getOwner() : this.asPet().getOwner();
            } else {
               actingPlayer = null;
            }

            if (actingPlayer != null) {
               for (WorldObject targetx : targets) {
                  if (targetx.isCreature()) {
                     Creature targetCreature = targetx.asCreature();
                     if (targetCreature.isRaid() && PvpConfig.FLAG_PLAYER_ON_RAID_ATTACK
                        || targetCreature.isChampion() && PvpConfig.FLAG_PLAYER_ON_CHAMPION_ATTACK) {
                        actingPlayer.updatePvPStatus();
                        break;
                     }
                  }
               }
            }
         }

         Player player = this.asPlayer();
         if (player != null) {
            for (WorldObject targetxx : targets) {
               if (targetxx.isCreature()) {
                  if (skill.getEffectPoint() <= 0) {
                     if ((targetxx.isPlayable() || targetxx.isTrap()) && skill.hasNegativeEffect()) {
                        if (!targetxx.equals(this)) {
                           if (targetxx.isPlayer()) {
                              targetxx.asPlayer().getAI().clientStartAutoAttack();
                           } else if (targetxx.isSummon() && targetxx.asCreature().hasAI()) {
                              Player owner = targetxx.asSummon().getOwner();
                              if (owner != null) {
                                 owner.getAI().clientStartAutoAttack();
                              }
                           }

                           if (player.getSummon() != targetxx && !this.isTrap() && (skill.getEffectPoint() != 0 || skill.getAffectRange() <= 0)) {
                              player.updatePvPStatus(targetxx.asCreature());
                           }
                        }
                     } else if (targetxx.isAttackable()) {
                        switch (skill.getId()) {
                           case 51:
                           case 511:
                              break;
                           default:
                              targetxx.asCreature().addAttackerToAttackByList(this);
                        }
                     }

                     if (targetxx.asCreature().hasAI()
                        && skill.hasNegativeEffect()
                        && !skill.hasEffectType(EffectType.HATE, new EffectType[0])
                        && skill.getAbnormalType() != AbnormalType.TURN_PASSIVE) {
                        targetxx.asCreature().getAI().notifyActionAttacked(this);
                     }
                  } else if (targetxx.isPlayer()) {
                     if (!targetxx.equals(this) && !targetxx.equals(player) && (targetxx.asPlayer().getPvpFlag() > 0 || targetxx.asPlayer().getKarma() > 0)) {
                        player.updatePvPStatus();
                     }
                  } else if (targetxx.isAttackable()) {
                     player.updatePvPStatus();
                  }
               }

               if (targetxx.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE) {
                  player.updatePvPStatus();
               }
            }

            World.forEachVisibleObjectInRange(
               player,
               Npc.class,
               1000,
               npcx -> {
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npcx)) {
                     EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npcx, player, skill, targets, this.isSummon()), npcx);
                  }

                  if (npcx.isAttackable()) {
                     Attackable attackable = npcx.asAttackable();
                     int skillEffectPoint = skill.getEffectPoint();
                     if (player.hasSummon() && targets.size() == 1 && targets.contains(player.getSummon())) {
                        skillEffectPoint = 0;
                     }

                     if (skillEffectPoint > 0 && attackable.hasAI() && attackable.getAI().getIntention() == Intention.ATTACK) {
                        WorldObject npcTarget = attackable.getTarget();

                        for (WorldObject skillTarget : targets) {
                           if (npcTarget == skillTarget || npcx == skillTarget) {
                              Creature originalCaster = (Creature)(this.isSummon() ? this : player);
                              attackable.addDamageHate(
                                 originalCaster, 0L, (long)(skillEffectPoint * 150 / (attackable.getLevel() + 7) * attackable.getHateRatio(originalCaster))
                              );
                           }
                        }
                     }

                     if (!NpcConfig.RAID_DISABLE_CURSE
                        && attackable.isRaid()
                        && attackable.giveRaidCurse()
                        && attackable.isInCombat()
                        && player.getLevel() - attackable.getLevel() > 8) {
                        player.abortAttack();
                        player.abortCast();
                        player.getAI().setIntentionIdle();
                        CommonSkill curse = skill.hasNegativeEffect() ? CommonSkill.RAID_CURSE2 : CommonSkill.RAID_CURSE;
                        curse.getSkill().applyEffects(attackable, player);
                     }
                  }
               }
            );
         }

         if (skill.hasNegativeEffect() && !skill.hasEffectType(EffectType.HATE, new EffectType[0])) {
            for (WorldObject targetxx : targets) {
               if (targetxx.isCreature()) {
                  Creature creature = targetxx.asCreature();
                  if (creature.hasAI()) {
                     creature.getAI().notifyActionAttacked(this);
                  }
               }

               if (this.isFakePlayer() && (targetxx.isPlayable() || targetxx.isFakePlayer())) {
                  Npc npc = this.asNpc();
                  if (!npc.isScriptValue(1)) {
                     npc.setScriptValue(1);
                     npc.broadcastInfo();
                     ScriptManager.getInstance().getScript("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK", npc, null);
                  }
               }
            }
         }
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": callSkill() failed.", (Throwable)var11);
      }
   }

   public double getLevelMod() {
      return (this.getLevel() + 89) / 100.0;
   }

   public void setSkillCast(Future<?> newSkillCast) {
      this._skillCast = newSkillCast;
   }

   public void forceIsCasting(int newSkillCastEndTick) {
      this.setCastingNow(true);
      this._castInterruptTime = newSkillCastEndTick - 4;
   }

   public void updatePvPFlag(int value) {
   }

   public double getRandomDamageMultiplier() {
      int random;
      if (this.getActiveWeaponItem() != null) {
         random = (int)this._stat.calcStat(Stat.RANDOM_DAMAGE, 0.0);
      } else {
         random = 5 + (int)Math.sqrt(this.getLevel());
      }

      return 1.0 + Rnd.get(0 - random, random) / 100.0;
   }

   public long getAttackEndTime() {
      return this._attackEndTime;
   }

   public int getBowAttackEndTime() {
      return this._disableBowAttackEndTime;
   }

   public abstract int getLevel();

   public double calcStat(Stat stat, double init) {
      return this._stat.calcStat(stat, init, null, null);
   }

   public double calcStat(Stat stat, double init, Creature target, Skill skill) {
      return this._stat.calcStat(stat, init, target, skill);
   }

   public int getAccuracy() {
      return this._stat.getAccuracy();
   }

   public float getAttackSpeedMultiplier() {
      return this._stat.getAttackSpeedMultiplier();
   }

   public double getCriticalDmg(Creature target, double init) {
      return this._stat.getCriticalDmg(target, init);
   }

   public int getCriticalHit(Creature target, Skill skill) {
      return this._stat.getCriticalHit(target, skill);
   }

   public int getEvasionRate(Creature target) {
      return this._stat.getEvasionRate(target);
   }

   public int getMagicalAttackRange(Skill skill) {
      return this._stat.getMagicalAttackRange(skill);
   }

   public int getMaxCp() {
      return this._stat.getMaxCp();
   }

   public int getMaxRecoverableCp() {
      return this._stat.getMaxRecoverableCp();
   }

   public double getMAtk(Creature target, Skill skill) {
      return this._stat.getMAtk(target, skill);
   }

   public int getMAtkSpd() {
      return this._stat.getMAtkSpd();
   }

   public int getMaxMp() {
      return this._stat.getMaxMp();
   }

   public int getMaxRecoverableMp() {
      return this._stat.getMaxRecoverableMp();
   }

   public int getMaxHp() {
      return this._stat.getMaxHp();
   }

   public int getMaxRecoverableHp() {
      return this._stat.getMaxRecoverableHp();
   }

   public int getMCriticalHit(Creature target, Skill skill) {
      return this._stat.getMCriticalHit(target, skill);
   }

   public double getMDef(Creature target, Skill skill) {
      return this._stat.getMDef(target, skill);
   }

   public double getMReuseRate(Skill skill) {
      return this._stat.getMReuseRate(skill);
   }

   public double getPAtk(Creature target) {
      return this._stat.getPAtk(target);
   }

   public double getPAtkSpd() {
      return this._stat.getPAtkSpd();
   }

   public double getPDef(Creature target) {
      return this._stat.getPDef(target);
   }

   public int getPhysicalAttackRange() {
      return this._stat.getPhysicalAttackRange();
   }

   public double getMovementSpeedMultiplier() {
      return this._stat.getMovementSpeedMultiplier();
   }

   public double getRunSpeed() {
      return this._stat.getRunSpeed();
   }

   public double getWalkSpeed() {
      return this._stat.getWalkSpeed();
   }

   public double getSwimRunSpeed() {
      return this._stat.getSwimRunSpeed();
   }

   public double getSwimWalkSpeed() {
      return this._stat.getSwimWalkSpeed();
   }

   public double getMoveSpeed() {
      return this._stat.getMoveSpeed();
   }

   public int getShldDef() {
      return this._stat.getShldDef();
   }

   public int getSTR() {
      return this._stat.getSTR();
   }

   public int getDEX() {
      return this._stat.getDEX();
   }

   public int getCON() {
      return this._stat.getCON();
   }

   public int getINT() {
      return this._stat.getINT();
   }

   public int getWIT() {
      return this._stat.getWIT();
   }

   public int getMEN() {
      return this._stat.getMEN();
   }

   public void addStatusListener(Creature object) {
      this._status.addStatusListener(object);
   }

   public void reduceCurrentHp(double amount, Creature attacker, Skill skill) {
      this.reduceCurrentHp(amount, attacker, true, false, skill);
   }

   public void reduceCurrentHpByDOT(double amount, Creature attacker, Skill skill) {
      this.reduceCurrentHp(amount, attacker, !skill.isToggle(), true, skill);
   }

   public void reduceCurrentHp(double amount, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (ChampionMonstersConfig.CHAMPION_ENABLE && this.isChampion() && ChampionMonstersConfig.CHAMPION_HP != 0) {
         this._status.reduceHp(amount / ChampionMonstersConfig.CHAMPION_HP, attacker, awake, isDOT, false);
      } else {
         if (this.isPlayer()
            && !isDOT
            && skill != null
            && skill.getCastRange() > 0
            && attacker != null
            && !GeoEngine.getInstance().canSeeTarget(attacker, this)) {
            amount = 0.0;
         }

         this._status.reduceHp(amount, attacker, awake, isDOT, false);
      }
   }

   public void reduceCurrentMp(double amount) {
      this._status.reduceMp(amount);
   }

   public void removeStatusListener(Creature object) {
      this._status.removeStatusListener(object);
   }

   protected void stopHpMpRegeneration() {
      this._status.stopHpMpRegeneration();
   }

   public double getCurrentCp() {
      return this._status.getCurrentCp();
   }

   public int getCurrentCpPercent() {
      return (int)(this._status.getCurrentCp() * 100.0 / this._stat.getMaxCp());
   }

   public void setCurrentCp(double newCp) {
      this._status.setCurrentCp(newCp);
   }

   public double getCurrentHp() {
      return this._status.getCurrentHp();
   }

   public int getCurrentHpPercent() {
      return (int)(this._status.getCurrentHp() * 100.0 / this._stat.getMaxHp());
   }

   public void setCurrentHp(double newHp) {
      this._status.setCurrentHp(newHp);
   }

   public void setCurrentHpMp(double newHp, double newMp) {
      this._status.setCurrentHpMp(newHp, newMp);
   }

   public double getCurrentMp() {
      return this._status.getCurrentMp();
   }

   public int getCurrentMpPercent() {
      return (int)(this._status.getCurrentMp() * 100.0 / this._stat.getMaxMp());
   }

   public void setCurrentMp(double newMp) {
      this._status.setCurrentMp(newMp);
   }

   public void fullRestore() {
      this._status.setCurrentHp(this.getMaxHp());
      this._status.setCurrentMp(this.getMaxMp(), this.isPlayable());
   }

   public int getMaxLoad() {
      if (!this.isPlayer() && !this.isPet()) {
         return 0;
      } else {
         double baseLoad = Math.floor(BaseStat.CON.calcBonus(this) * 69000.0 * PlayerConfig.ALT_WEIGHT_LIMIT);
         return (int)this.calcStat(Stat.WEIGHT_LIMIT, baseLoad, this, null);
      }
   }

   public int getBonusWeightPenalty() {
      return !this.isPlayer() && !this.isPet() ? 0 : (int)this.calcStat(Stat.WEIGHT_PENALTY, 1.0, this, null);
   }

   public int getCurrentLoad() {
      if (!this.isPlayer() && !this.isPet()) {
         return 0;
      } else {
         Inventory inventory = this.getInventory();
         return inventory != null ? inventory.getTotalWeight() : 0;
      }
   }

   public boolean isChampion() {
      return false;
   }

   public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
      if (miss && target.isPlayer()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
         sm.addPcName(target.asPlayer());
         sm.addString(this.getName());
         target.sendPacket(sm);
      }
   }

   public byte getAttackElement() {
      return this._stat.getAttackElement();
   }

   public int getAttackElementValue(byte attackAttribute) {
      return this._stat.getAttackElementValue(attackAttribute);
   }

   public int getDefenseElementValue(byte defenseAttribute) {
      return this._stat.getDefenseElementValue(defenseAttribute);
   }

   public void disableCoreAI(boolean value) {
      this._disabledAI = value;
   }

   public boolean isCoreAIDisabled() {
      return this._disabledAI;
   }

   public boolean giveRaidCurse() {
      return true;
   }

   public boolean isAffected(EffectFlag flag) {
      return this._effectList.isAffected(flag);
   }

   public boolean isAffectedByAbnormalType(AbnormalType type) {
      return this._effectList.isAffectedByAbnormalType(type);
   }

   public Team getTeam() {
      return this._team;
   }

   public void setTeam(Team team) {
      this._team = team;
   }

   public void setLethalable(boolean value) {
      this._lethalable = value;
   }

   public boolean isLethalable() {
      return this._lethalable;
   }

   public Map<Integer, OptionSkillHolder> getTriggerSkills() {
      return this._triggerSkills;
   }

   public void addTriggerSkill(OptionSkillHolder holder) {
      this.getTriggerSkills().put(holder.getSkill().getId(), holder);
   }

   public void removeTriggerSkill(OptionSkillHolder holder) {
      this.getTriggerSkills().remove(holder.getSkill().getId());
   }

   public void makeTriggerCast(Skill skill, Creature target, boolean ignoreTargetType) {
      try {
         if (skill == null) {
            return;
         }

         if (skill.checkCondition(this, target, false)) {
            if (this.isSkillDisabled(skill)) {
               return;
            }

            if (skill.getReuseDelay() > 0) {
               this.disableSkill(skill, skill.getReuseDelay());
            }

            List<WorldObject> targets = !ignoreTargetType ? skill.getTargetList(this, false, target) : Collections.singletonList(target);
            if (targets.isEmpty()) {
               return;
            }

            Creature skillTarget = target;

            for (WorldObject obj : targets) {
               if (obj != null && obj.isCreature()) {
                  skillTarget = obj.asCreature();
                  break;
               }
            }

            if (PlayerConfig.ALT_VALIDATE_TRIGGER_SKILLS && this.isPlayable() && skillTarget != null && skillTarget.isPlayable()) {
               Player player = this.asPlayer();
               if (!player.checkPvpSkill(skillTarget, skill)) {
                  return;
               }
            }

            this.broadcastSkillPacket(new MagicSkillUse(this, skillTarget, skill.getDisplayId(), skill.getLevel(), 0, 0), skillTarget);
            this.broadcastSkillPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets), targets);
            skill.activateSkill(this, targets);
         }
      } catch (Exception var8) {
         LOGGER.log(Level.WARNING, "makeTriggerCast: ", (Throwable)var8);
      }
   }

   public void makeTriggerCast(Skill skill, Creature target) {
      this.makeTriggerCast(skill, target, false);
   }

   public boolean canRevive() {
      return true;
   }

   public void setCanRevive(boolean value) {
   }

   public boolean isSweepActive() {
      return false;
   }

   public int getClanId() {
      return 0;
   }

   public Clan getClan() {
      return null;
   }

   public boolean isAcademyMember() {
      return false;
   }

   public int getPledgeType() {
      return 0;
   }

   public int getAllyId() {
      return 0;
   }

   public void notifyDamageReceived(double damage, Creature attacker, Skill skill, boolean critical, boolean damageOverTime) {
      if (this.isPlayer() && this.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND && damage > 0.0) {
         this.stopFakeDeath(true);
      }

      if (attacker != null && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_DEALT, attacker)) {
         if (this._onCreatureDamageDealt == null) {
            this._onCreatureDamageDealt = new OnCreatureDamageDealt();
         }

         this._onCreatureDamageDealt.setAttacker(attacker);
         this._onCreatureDamageDealt.setTarget(this);
         this._onCreatureDamageDealt.setDamage(damage);
         this._onCreatureDamageDealt.setSkill(skill);
         this._onCreatureDamageDealt.setCritical(critical);
         this._onCreatureDamageDealt.setDamageOverTime(damageOverTime);
         EventDispatcher.getInstance().notifyEvent(this._onCreatureDamageDealt, attacker);
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_RECEIVED, this)) {
         if (this._onCreatureDamageReceived == null) {
            this._onCreatureDamageReceived = new OnCreatureDamageReceived();
         }

         this._onCreatureDamageReceived.setAttacker(attacker);
         this._onCreatureDamageReceived.setTarget(this);
         this._onCreatureDamageReceived.setDamage(damage);
         this._onCreatureDamageReceived.setSkill(skill);
         this._onCreatureDamageReceived.setCritical(critical);
         this._onCreatureDamageReceived.setDamageOverTime(damageOverTime);
         EventDispatcher.getInstance().notifyEventAsync(this._onCreatureDamageReceived, this);
      }
   }

   public void notifyAttackAvoid(Creature target, boolean isDot) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK_AVOID, target)) {
         if (this._onCreatureAttackAvoid == null) {
            this._onCreatureAttackAvoid = new OnCreatureAttackAvoid();
         }

         this._onCreatureAttackAvoid.setAttacker(this);
         this._onCreatureAttackAvoid.setTarget(target);
         this._onCreatureAttackAvoid.setDamageOverTime(isDot);
         EventDispatcher.getInstance().notifyEvent(this._onCreatureAttackAvoid, target);
      }
   }

   public WeaponType getAttackType() {
      if (this.isTransformed()) {
         TransformTemplate template = this.getTransformation().getTemplate(this.asPlayer());
         if (template != null) {
            return template.getBaseAttackType();
         }
      }

      Weapon weapon = this.getActiveWeaponItem();
      return weapon != null ? weapon.getItemType() : this._template.getBaseAttackType();
   }

   public boolean isInCategory(CategoryType type) {
      return CategoryData.getInstance().isInCategory(type, this.getId());
   }

   public Creature getSummoner() {
      return this._summoner;
   }

   public void setSummoner(Creature summoner) {
      this._summoner = summoner;
   }

   public boolean isCreature() {
      return true;
   }

   public Creature asCreature() {
      return this;
   }

   public boolean isChanneling() {
      return this._channelizer != null && this._channelizer.isChanneling();
   }

   public SkillChannelizer getSkillChannelizer() {
      if (this._channelizer == null) {
         this._channelizer = new SkillChannelizer(this);
      }

      return this._channelizer;
   }

   public boolean isChannelized() {
      return this._channelized != null && !this._channelized.isChannelized();
   }

   public SkillChannelized getSkillChannelized() {
      if (this._channelized == null) {
         this._channelized = new SkillChannelized();
      }

      return this._channelized;
   }

   public void addInvulAgainst(SkillHolder holder) {
      InvulSkillHolder invulHolder = this.getInvulAgainstSkills().get(holder.getSkillId());
      if (invulHolder != null) {
         invulHolder.increaseInstances();
      } else {
         this.getInvulAgainstSkills().put(holder.getSkillId(), new InvulSkillHolder(holder));
      }
   }

   public void removeInvulAgainst(SkillHolder holder) {
      InvulSkillHolder invulHolder = this.getInvulAgainstSkills().get(holder.getSkillId());
      if (invulHolder != null && invulHolder.decreaseInstances() < 1) {
         this.getInvulAgainstSkills().remove(holder.getSkillId());
      }
   }

   public boolean isInvulAgainst(int skillId, int skillLevel) {
      if (this._invulAgainst.isEmpty()) {
         return false;
      } else {
         SkillHolder holder = (SkillHolder)this.getInvulAgainstSkills().get(skillId);
         return holder != null && (holder.getSkillLevel() < 1 || holder.getSkillLevel() == skillLevel);
      }
   }

   private Map<Integer, InvulSkillHolder> getInvulAgainstSkills() {
      return this._invulAgainst;
   }

   public Collection<AbstractEventListener> getListeners(EventType type) {
      Collection<AbstractEventListener> objectListeners = super.getListeners(type);
      Collection<AbstractEventListener> templateListeners = this._template.getListeners(type);
      Collection<AbstractEventListener> globalListeners = (Collection<AbstractEventListener>)(this.isMonster()
         ? Containers.Monsters().getListeners(type)
         : (this.isNpc() ? Containers.Npcs().getListeners(type) : (this.isPlayer() ? Containers.Players().getListeners(type) : Collections.emptyList())));
      if (objectListeners.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty()) {
         return Collections.emptyList();
      } else if (!objectListeners.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty()) {
         return objectListeners;
      } else if (!templateListeners.isEmpty() && objectListeners.isEmpty() && globalListeners.isEmpty()) {
         return templateListeners;
      } else if (!globalListeners.isEmpty() && objectListeners.isEmpty() && templateListeners.isEmpty()) {
         return globalListeners;
      } else {
         Collection<AbstractEventListener> allListeners = new ArrayList<>(objectListeners.size() + templateListeners.size() + globalListeners.size());
         allListeners.addAll(objectListeners);
         allListeners.addAll(templateListeners);
         allListeners.addAll(globalListeners);
         return allListeners;
      }
   }

   public Race getRace() {
      return this._template.getRace();
   }

   public void setXYZ(int newX, int newY, int newZ) {
      if (newX != 0 || newY != 0) {
         ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
         if (newZoneRegion != null) {
            ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
            if (oldZoneRegion != newZoneRegion) {
               oldZoneRegion.removeFromZones(this);
               newZoneRegion.revalidateZones(this);
            }

            super.setXYZ(newX, newY, newZ);
         }
      }
   }

   public boolean isInDuel() {
      return false;
   }

   public int getDuelId() {
      return 0;
   }

   public byte getSiegeState() {
      return 0;
   }

   public int getSiegeSide() {
      return 0;
   }

   public Map<Integer, RelationCache> getKnownRelations() {
      return this._knownRelations;
   }

   protected void initSeenCreatures() {
      if (this._seenCreatures == null) {
         synchronized (this) {
            if (this._seenCreatures == null) {
               if (this.isNpc()) {
                  NpcTemplate template = this.asNpc().getTemplate();
                  if (template != null && template.getAggroRange() > 0) {
                     this._seenCreatureRange = template.getAggroRange();
                  }
               }

               this._seenCreatures = ConcurrentHashMap.newKeySet(1);
            }
         }
      }

      CreatureSeeTaskManager.getInstance().add(this);
   }

   public void updateSeenCreatures() {
      if (this._seenCreatures != null && !this._isDead && this.isSpawned()) {
         WorldRegion region = this.getWorldRegion();
         if (region != null && region.areNeighborsActive()) {
            World.forEachVisibleObjectInRange(this, Creature.class, this._seenCreatureRange, creature -> {
               if (!creature.isInvisible() && this._seenCreatures.add(creature) && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SEE, this)) {
                  EventDispatcher.getInstance().notifyEventAsync(new OnCreatureSee(this, creature), this);
               }
            });
         }
      }
   }

   public void removeSeenCreature(WorldObject worldObject) {
      if (this._seenCreatures != null) {
         this._seenCreatures.remove(worldObject);
      }
   }

   public int getKarma() {
      return this._karma;
   }

   public void setKarma(int karma) {
      this._karma = karma;
   }

   public int getMinShopDistance() {
      return 0;
   }

   public void setCursorKeyMovement(boolean value) {
      this._cursorKeyMovement = value;
   }

   public boolean isCursorKeyMovement() {
      return this._cursorKeyMovement;
   }

   public void setSyncedXYZ(int x, int y, int z) {
      this.setXYZ(x, y, z);
      MoveData move = this._move;
      if (move != null) {
         move.xAccurate = x;
         move.yAccurate = y;
      }
   }

   public List<Item> getFakePlayerDrops() {
      return this._fakePlayerDrops;
   }

   public void addBuffInfoTime(BuffInfo info) {
      this._buffFinishTask.addBuffInfo(info);
   }

   public void removeBuffInfoTime(BuffInfo info) {
      this._buffFinishTask.removeBuffInfo(info);
   }

   // [13.09.2026] Вложенный класс, восстановленный из Creature$MoveData.class
   // (при декомпиляции отдельно от Creature он не попадал в этот файл).
   public static class MoveData
   {
      public int moveStartTime;
      public int moveTimestamp;
      public int xDestination;
      public int yDestination;
      public int zDestination;
      public double xAccurate;
      public double yAccurate;
      public double zAccurate;
      public int heading;
      public boolean disregardingGeodata;
      public int onGeodataPathIndex;
      public List<GeoLocation> geoPath;
      public int geoPathAccurateTx;
      public int geoPathAccurateTy;
      public int geoPathGtx;
      public int geoPathGty;
      public int lastBroadcastTime;
   }
}
