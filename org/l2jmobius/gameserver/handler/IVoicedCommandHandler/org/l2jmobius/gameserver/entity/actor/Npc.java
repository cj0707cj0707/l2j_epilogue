package org.l2jmobius.gameserver.entity.actor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.data.xml.DynamicExpRateData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcPersonalAIData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.actor.instance.ClanHallManager;
import org.l2jmobius.gameserver.entity.actor.instance.Doorman;
import org.l2jmobius.gameserver.entity.actor.instance.Fisherman;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.actor.instance.Teleporter;
import org.l2jmobius.gameserver.entity.actor.instance.Trainer;
import org.l2jmobius.gameserver.entity.actor.instance.Warehouse;
import org.l2jmobius.gameserver.entity.actor.stat.NpcStat;
import org.l2jmobius.gameserver.entity.actor.status.NpcStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.TownZone;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.TownManager;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;

public class Npc extends Creature {
   public static final int INTERACTION_DISTANCE = 250;
   public static final int RANDOM_ITEM_DROP_LIMIT = 70;
   private static final Set<Integer> CREATURE_SEE_IDS = ConcurrentHashMap.newKeySet();
   private Spawn _spawn;
   private boolean _isBusy = false;
   private String _busyMessage = "";
   private volatile boolean _isDecayed = false;
   private int _castleIndex = -2;
   private int _fortIndex = -2;
   private boolean _isInTown = false;
   private boolean _isAutoAttackable = false;
   private long _lastSocialBroadcast = 0L;
   private static final int MINIMUM_SOCIAL_INTERVAL = 6000;
   private boolean _isRandomAnimationEnabled = true;
   private boolean _isRandomWalkingEnabled = true;
   private boolean _isWalker = false;
   private boolean _isTalkable = this.getTemplate().isTalkable();
   private final boolean _isQuestMonster = this.getTemplate().isQuestMonster();
   private final boolean _isFakePlayer = this.getTemplate().isFakePlayer();
   private int _currentLHandId;
   private int _currentRHandId;
   private int _currentEnchant;
   private double _currentCollisionHeight;
   private double _currentCollisionRadius;
   private int _soulshotamount = 0;
   private int _spiritshotamount = 0;
   private int _displayEffect = 0;
   private int _shotsMask = 0;
   private int _killingBlowWeaponId;
   private volatile int _scriptValue = 0;
   private Map<Integer, Npc> _summonedNpcs = null;
   private final List<QuestTimer> _questTimers = new ArrayList<>();
   private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();

   public Npc(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Npc);
      this.initCharStatusUpdateValues();
      this._currentLHandId = this.getTemplate().getLHandId();
      this._currentRHandId = this.getTemplate().getRHandId();
      this._currentEnchant = NpcConfig.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : this.getTemplate().getWeaponEnchant();
      this._currentCollisionHeight = this.getTemplate().getFCollisionHeight();
      this._currentCollisionRadius = this.getTemplate().getFCollisionRadius();
      this.setFlying(template.isFlying());
   }

   public Npc(int npcId) {
      this(NpcData.getInstance().getTemplate(npcId));
   }

   public int getSoulShotChance() {
      return this.getTemplate().getSoulShotChance();
   }

   public int getSpiritShotChance() {
      return this.getTemplate().getSpiritShotChance();
   }

   public int getMinSkillChance() {
      return this.getTemplate().getMinSkillChance();
   }

   public int getMaxSkillChance() {
      return this.getTemplate().getMaxSkillChance();
   }

   public boolean hasSkillChance() {
      return Rnd.get(100) < Rnd.get(this.getMinSkillChance(), this.getMaxSkillChance());
   }

   public boolean canMove() {
      return this.getTemplate().canMove();
   }

   public boolean isChaos() {
      return this.getTemplate().isChaos();
   }

   public List<Skill> getLongRangeSkills() {
      return this.getTemplate().getAISkills(AISkillScope.LONG_RANGE);
   }

   public List<Skill> getShortRangeSkills() {
      return this.getTemplate().getAISkills(AISkillScope.SHORT_RANGE);
   }

   public void onRandomAnimation(int animationId) {
      long now = System.currentTimeMillis();
      if (now - this._lastSocialBroadcast > 6000L) {
         this._lastSocialBroadcast = now;
         this.broadcastSocialAction(animationId);
      }
   }

   public boolean hasRandomAnimation() {
      return GeneralConfig.MAX_NPC_ANIMATION > 0 && this.isRandomAnimationEnabled() && this.getAiType() != AIType.CORPSE;
   }

   public void setRandomAnimationEnabled(boolean value) {
      this._isRandomAnimationEnabled = value;
   }

   public boolean isRandomAnimationEnabled() {
      return !this._isFakePlayer && this._isRandomAnimationEnabled;
   }

   public void setRandomWalking(boolean enabled) {
      this._isRandomWalkingEnabled = enabled;
   }

   public boolean isRandomWalkingEnabled() {
      return this._isRandomWalkingEnabled;
   }

   public NpcStat getStat() {
      return (NpcStat)super.getStat();
   }

   @Override
   public void initCharStat() {
      this.setStat(new NpcStat(this));
   }

   public NpcStatus getStatus() {
      return (NpcStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new NpcStatus(this));
   }

   public NpcTemplate getTemplate() {
      return (NpcTemplate)super.getTemplate();
   }

   @Override
   public int getId() {
      return this.getTemplate().getId();
   }

   @Override
   public boolean canBeAttacked() {
      return NpcConfig.ALT_ATTACKABLE_NPCS;
   }

   @Override
   public int getLevel() {
      return this.getTemplate().getLevel();
   }

   public boolean isAggressive() {
      return false;
   }

   public int getAggroRange() {
      return this.hasAIValue("aggroRange") ? this.getAIValue("aggroRange") : this.getTemplate().getAggroRange();
   }

   public boolean isInMyClan(Npc npc) {
      return this.getTemplate().isClan(npc.getTemplate().getClans());
   }

   @Override
   public boolean isUndead() {
      return this.getTemplate().getRace() == Race.UNDEAD;
   }

   @Override
   public void updateAbnormalEffect() {
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            if (this._isFakePlayer) {
               player.sendPacket(new FakePlayerInfo(this));
            } else if (this.getRunSpeed() == 0.0) {
               player.sendPacket(new ServerObjectInfo(this, player));
            } else if (this.isSpawned()) {
               player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
            }
         }
      });
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return this._isAutoAttackable;
   }

   public void setAutoAttackable(boolean flag) {
      this._isAutoAttackable = flag;
   }

   public int getLeftHandItem() {
      return this._currentLHandId;
   }

   public int getRightHandItem() {
      return this._currentRHandId;
   }

   public int getEnchantEffect() {
      return this._currentEnchant;
   }

   public boolean isBusy() {
      return this._isBusy;
   }

   public void setBusy(boolean isBusy) {
      this._isBusy = isBusy;
   }

   public String getBusyMessage() {
      return this._busyMessage;
   }

   public void setBusyMessage(String message) {
      this._busyMessage = message;
   }

   public boolean isWarehouse() {
      return false;
   }

   public boolean canTarget(Player player) {
      if (player.isOutOfControl()) {
         player.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (player.isLockedTarget() && player.getLockedTarget() != this) {
         player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
         player.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else {
         return true;
      }
   }

   public boolean canInteract(Player player) {
      if (player.isCastingNow() || player.isCastingSimultaneouslyNow()) {
         return false;
      } else if (player.isDead() || player.isFakeDeath()) {
         return false;
      } else if (player.isSitting()) {
         return false;
      } else if (player.isInStoreMode()) {
         return false;
      } else {
         return !this.isInsideRadius3D(player, 250) ? false : player.getInstanceId() == this.getInstanceId();
      }
   }

   public Castle getCastle() {
      if (this._castleIndex < 0) {
         TownZone town = TownManager.getTown(this.getX(), this.getY(), this.getZ());
         if (town != null) {
            this._castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
         }

         if (this._castleIndex < 0) {
            this._castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
         } else {
            this._isInTown = true;
         }
      }

      return this._castleIndex < 0 ? null : CastleManager.getInstance().getCastles().get(this._castleIndex);
   }

   public boolean isMyLord(Player player) {
      if (player.isClanLeader()) {
         int castleId = this.getCastle() != null ? this.getCastle().getResidenceId() : -1;
         int fortId = this.getFort() != null ? this.getFort().getResidenceId() : -1;
         return player.getClan().getCastleId() == castleId || player.getClan().getFortId() == fortId;
      } else {
         return false;
      }
   }

   public SiegableHall getConquerableHall() {
      return CHSiegeManager.getInstance().getNearbyClanHall(this.getX(), this.getY(), 10000);
   }

   public Castle getCastle(long maxDistance) {
      int index = CastleManager.getInstance().findNearestCastleIndex(this, maxDistance);
      return index < 0 ? null : CastleManager.getInstance().getCastles().get(index);
   }

   public Fort getFort() {
      if (this._fortIndex < 0) {
         Fort fort = FortManager.getInstance().getFort(this.getX(), this.getY(), this.getZ());
         if (fort != null) {
            this._fortIndex = FortManager.getInstance().getFortIndex(fort.getResidenceId());
         }

         if (this._fortIndex < 0) {
            this._fortIndex = FortManager.getInstance().findNearestFortIndex(this);
         }
      }

      return this._fortIndex < 0 ? null : FortManager.getInstance().getForts().get(this._fortIndex);
   }

   public Fort getFort(long maxDistance) {
      int index = FortManager.getInstance().findNearestFortIndex(this, maxDistance);
      return index < 0 ? null : FortManager.getInstance().getForts().get(index);
   }

   public boolean isInTown() {
      if (this._castleIndex < 0) {
         this.getCastle();
      }

      return this._isInTown;
   }

   public void onBypassFeedback(Player player, String command) {
      if (this.canInteract(player)) {
         if (this._isBusy && !this._busyMessage.isEmpty()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, "data/html/npcbusy.htm");
            html.replace("%busymessage%", this._busyMessage);
            html.replace("%npcname%", this.getName());
            html.replace("%playername%", player.getName());
            player.sendPacket(html);
         } else {
            IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
            if (handler != null) {
               handler.onCommand(command, player, this);
            } else {
               LOGGER.info(this.getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + this.getId());
            }
         }
      }
   }

   @Override
   public Item getActiveWeaponInstance() {
      return null;
   }

   @Override
   public Weapon getActiveWeaponItem() {
      int weaponId = this.getTemplate().getRHandId();
      if (weaponId < 1) {
         return null;
      } else {
         ItemTemplate item = ItemData.getInstance().getTemplate(this.getTemplate().getRHandId());
         return !(item instanceof Weapon) ? null : (Weapon)item;
      }
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return null;
   }

   public Weapon getSecondaryWeaponItem() {
      int weaponId = this.getTemplate().getLHandId();
      if (weaponId < 1) {
         return null;
      } else {
         ItemTemplate item = ItemData.getInstance().getTemplate(this.getTemplate().getLHandId());
         return !(item instanceof Weapon) ? null : (Weapon)item;
      }
   }

   public void insertObjectIdAndShowChatWindow(Player player, String content) {
      player.sendPacket(new NpcHtmlMessage(this.getObjectId(), content.replaceAll("%objectId%", String.valueOf(this.getObjectId()))));
   }

   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      String temp = "data/html/default/" + pom + ".htm";
      if (GeneralConfig.HTM_CACHE) {
         if (HtmCache.getInstance().contains(temp)) {
            return temp;
         }
      } else {
         File file = new File(ServerConfig.DATAPACK_ROOT, temp);
         if (file.isFile()) {
            String lowerCaseName = file.getName().toLowerCase();
            if (lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html")) {
               return temp;
            }
         }
      }

      return "data/html/npcdefault.htm";
   }

   public void showChatWindow(Player player) {
      this.showChatWindow(player, 0);
   }

   private boolean showPkDenyChatWindow(Player player, String type) {
      String html = HtmCache.getInstance().getHtm(player, "data/html/" + type + "/" + this.getId() + "-pk.htm");
      if (html != null) {
         this.insertObjectIdAndShowChatWindow(player, html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
         return true;
      } else {
         return false;
      }
   }

   public void showChatWindow(Player player, int value) {
      if (!this._isTalkable) {
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else if (!player.isCursedWeaponEquipped() || player.getTarget() instanceof ClanHallManager && player.getTarget() instanceof Doorman) {
         if (player.getKarma() > 0) {
            if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof Merchant) {
               if (this.showPkDenyChatWindow(player, "merchant")) {
                  return;
               }
            } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof Teleporter) {
               if (this.showPkDenyChatWindow(player, "teleporter")) {
                  return;
               }
            } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof Warehouse) {
               if (this.showPkDenyChatWindow(player, "warehouse")) {
                  return;
               }
            } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof Fisherman && this.showPkDenyChatWindow(player, "fisherman")) {
               return;
            }
         }

         if (!this.getTemplate().isType("Auctioneer") || value != 0) {
            int npcId = this.getTemplate().getId();
            String filename = "data/html/seven_signs/";
            int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(1);
            int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(2);
            int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
            int compWinner = SevenSigns.getInstance().getCabalHighestScore();
            switch (npcId) {
               case 30298:
                  if (player.isAcademyMember()) {
                     filename = this.getHtmlPath(npcId, 1);
                  } else {
                     filename = this.getHtmlPath(npcId, value);
                  }
                  break;
               case 31092:
                  filename = filename + "blkmrkt_1.htm";
                  break;
               case 31113:
                  if (FeatureConfig.ALT_STRICT_SEVENSIGNS) {
                     switch (compWinner) {
                        case 1:
                           if (playerCabal != compWinner || playerCabal != sealAvariceOwner) {
                              player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
                              player.sendPacket(ActionFailed.STATIC_PACKET);
                              return;
                           }
                           break;
                        case 2:
                           if (playerCabal != compWinner || playerCabal != sealAvariceOwner) {
                              player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
                              player.sendPacket(ActionFailed.STATIC_PACKET);
                              return;
                           }
                           break;
                        default:
                           player.sendPacket(SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD);
                           return;
                     }
                  }

                  filename = filename + "mammmerch_1.htm";
                  break;
               case 31126:
                  if (FeatureConfig.ALT_STRICT_SEVENSIGNS) {
                     switch (compWinner) {
                        case 1:
                           if (playerCabal != compWinner || playerCabal != sealGnosisOwner) {
                              player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
                              player.sendPacket(ActionFailed.STATIC_PACKET);
                              return;
                           }
                           break;
                        case 2:
                           if (playerCabal != compWinner || playerCabal != sealGnosisOwner) {
                              player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
                              player.sendPacket(ActionFailed.STATIC_PACKET);
                              return;
                           }
                           break;
                        default:
                           player.sendPacket(SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD);
                           return;
                     }
                  }

                  filename = filename + "mammblack_1.htm";
                  break;
               case 31127:
               case 31128:
               case 31129:
               case 31130:
               case 31131:
                  filename = filename + "festival/dawn_guide.htm";
                  break;
               case 31132:
               case 31133:
               case 31134:
               case 31135:
               case 31136:
               case 31142:
               case 31143:
               case 31144:
               case 31145:
               case 31146:
                  filename = filename + "festival/festival_witch.htm";
                  break;
               case 31137:
               case 31138:
               case 31139:
               case 31140:
               case 31141:
                  filename = filename + "festival/dusk_guide.htm";
                  break;
               case 31688:
                  if (player.isNoble()) {
                     filename = "data/html/olympiad/noble_main.htm";
                  } else {
                     filename = this.getHtmlPath(npcId, value);
                  }
                  break;
               case 31690:
               case 31769:
               case 31770:
               case 31771:
               case 31772:
                  if (!player.isHero() && !player.isNoble()) {
                     filename = this.getHtmlPath(npcId, value);
                  } else {
                     filename = "data/html/olympiad/hero_main.htm";
                  }
                  break;
               case 36402:
                  if (player.getOlympiadBuffCount() > 0) {
                     filename = player.getOlympiadBuffCount() == OlympiadConfig.OLYMPIAD_MAX_BUFFS
                        ? "data/html/olympiad/olympiad_buffs.htm"
                        : "data/html/olympiad/olympiad_5buffs.htm";
                  } else {
                     filename = "data/html/olympiad/olympiad_nobuffs.htm";
                  }
                  break;
               default:
                  if (npcId >= 31865 && npcId <= 31918) {
                     if (value == 0) {
                        filename = filename + "rift/GuardianOfBorder.htm";
                     } else {
                        filename = filename + "rift/GuardianOfBorder-" + value + ".htm";
                     }
                  } else {
                     if (npcId >= 31093 && npcId <= 31094 || npcId >= 31172 && npcId <= 31201 || npcId >= 31239 && npcId <= 31254) {
                        return;
                     }

                     filename = this.getHtmlPath(npcId, value);
                  }
            }

            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, filename);
            if (this instanceof Merchant && NpcConfig.LIST_PET_RENT_NPC.contains(npcId)) {
               html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass Script");
            }

            html.replace("%npcname%", String.valueOf(this.getName()));
            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
            player.sendPacket(html);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      } else {
         player.setTarget(player);
      }
   }

   public void showChatWindow(Player player, String filename) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      player.sendPacket(html);
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   public long getExpReward(int level) {
      return DynamicExpRateData.getInstance().isEnabled()
         ? (long)(this.getTemplate().getExp() * DynamicExpRateData.getInstance().getDynamicExpRate(level))
         : (long)(this.getTemplate().getExp() * RatesConfig.RATE_XP);
   }

   public int getSpReward(int level) {
      return DynamicExpRateData.getInstance().isEnabled()
         ? (int)(this.getTemplate().getSP() * DynamicExpRateData.getInstance().getDynamicSpRate(level))
         : (int)(this.getTemplate().getSP() * RatesConfig.RATE_SP);
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         this._currentLHandId = this.getTemplate().getLHandId();
         this._currentRHandId = this.getTemplate().getRHandId();
         this._currentCollisionHeight = this.getTemplate().getFCollisionHeight();
         this._currentCollisionRadius = this.getTemplate().getFCollisionRadius();
         Weapon weapon = killer != null ? killer.getActiveWeaponItem() : null;
         this._killingBlowWeaponId = weapon != null ? weapon.getId() : 0;
         if (this._isFakePlayer && killer != null && killer.isPlayable()) {
            Player player = killer.asPlayer();
            if (this.isScriptValue(0) && this.getKarma() < 0) {
               if (FakePlayersConfig.FAKE_PLAYER_KILL_KARMA) {
                  player.setKarma(player.getKarma() + Formulas.calculateKarmaGain(player.getPkKills(), killer.isSummon()));
                  player.setPkKills(player.getPkKills() + 1);
                  player.broadcastUserInfo();
                  player.checkItemRestriction();
                  if (PvpRewardItemConfig.REWARD_PK_ITEM
                     && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES || this.getInstanceId() == 0)
                     && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES || !this.isInsideZone(ZoneId.PVP))) {
                     player.addItem(
                        ItemProcessType.REWARD,
                        PvpRewardItemConfig.REWARD_PK_ITEM_ID,
                        PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT,
                        this,
                        PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE
                     );
                  }

                  if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM()) {
                     String msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG.replace("$killer", player.getName()).replace("$target", this.getName());
                     if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
                        sm.addString(msg);
                        World.broadcastToAllOnlinePlayers(sm);
                     } else {
                        World.broadcastToAllOnlinePlayers(msg, false);
                     }
                  }
               }
            } else if (FakePlayersConfig.FAKE_PLAYER_KILL_PVP) {
               player.setPvpKills(player.getPvpKills() + 1);
               player.broadcastUserInfo();
               if (PvpRewardItemConfig.REWARD_PVP_ITEM
                  && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES || this.getInstanceId() == 0)
                  && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES || !this.isInsideZone(ZoneId.PVP))) {
                  player.addItem(
                     ItemProcessType.REWARD,
                     PvpRewardItemConfig.REWARD_PVP_ITEM_ID,
                     PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT,
                     this,
                     PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE
                  );
               }

               if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM()) {
                  String msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", player.getName()).replace("$target", this.getName());
                  if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                     SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
                     sm.addString(msg);
                     World.broadcastToAllOnlinePlayers(sm);
                  } else {
                     World.broadcastToAllOnlinePlayers(msg, false);
                  }
               }
            }
         }

         DecayTaskManager.getInstance().add(this);
         return true;
      }
   }

   public void setSpawn(Spawn spawn) {
      this._spawn = spawn;
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._soulshotamount = this.getTemplate().getSoulShot();
      this._spiritshotamount = this.getTemplate().getSpiritShot();
      this._killingBlowWeaponId = 0;
      this._isRandomAnimationEnabled = this.getTemplate().isRandomAnimationEnabled();
      this._isRandomWalkingEnabled = !WalkingManager.getInstance().isTargeted(this) && this.getTemplate().isRandomWalkEnabled();
      if (this.isTeleporting()) {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_TELEPORT, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnNpcTeleport(this), this);
         }
      } else if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SPAWN, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnNpcSpawn(this), this);
      }

      if (!this.isTeleporting()) {
         WalkingManager.getInstance().onSpawn(this);
      }

      if (CREATURE_SEE_IDS.contains(this.getId())) {
         this.initSeenCreatures();
      }
   }

   public static void addCreatureSeeId(int id) {
      CREATURE_SEE_IDS.add(id);
   }

   @Override
   public void onDecay() {
      if (!this._isDecayed) {
         this.setDecayed(true);
         super.onDecay();
         if (this._spawn != null && !RaidBossSpawnManager.getInstance().isDefined(this.getId())) {
            this._spawn.decreaseCount(this);
         }

         WalkingManager.getInstance().onDeath(this);
         Creature summoner = this.getSummoner();
         if (summoner != null && summoner.isNpc()) {
            summoner.asNpc().removeSummonedNpc(this.getObjectId());
         }

         this.stopQuestTimers();
         this.stopTimerHolders();
         this._scriptValue = 0;
      }
   }

   @Override
   public boolean deleteMe() {
      try {
         this.onDecay();
      } catch (Exception var2) {
         LOGGER.log(Level.SEVERE, "Failed decayMe().", (Throwable)var2);
      }

      if (this.isChannelized()) {
         this.getSkillChannelized().abortChannelization();
      }

      ZoneManager.getInstance().getRegion(this).removeFromZones(this);
      return super.deleteMe();
   }

   public Spawn getSpawn() {
      return this._spawn;
   }

   public boolean isDecayed() {
      return this._isDecayed;
   }

   public void setDecayed(boolean decayed) {
      this._isDecayed = decayed;
   }

   public void endDecayTask() {
      if (!this._isDecayed) {
         DecayTaskManager.getInstance().cancel(this);
         this.onDecay();
      }
   }

   public void setLHandId(int newWeaponId) {
      this._currentLHandId = newWeaponId;
      this.updateAbnormalEffect();
   }

   public void setRHandId(int newWeaponId) {
      this._currentRHandId = newWeaponId;
      this.updateAbnormalEffect();
   }

   public void setLRHandId(int newLWeaponId, int newRWeaponId) {
      this._currentRHandId = newRWeaponId;
      this._currentLHandId = newLWeaponId;
      this.updateAbnormalEffect();
   }

   public void setEnchant(int newEnchantValue) {
      this._currentEnchant = newEnchantValue;
      this.updateAbnormalEffect();
   }

   public boolean isShowName() {
      return this.getTemplate().isShowName();
   }

   @Override
   public boolean isTargetable() {
      return this.getTemplate().isTargetable();
   }

   public void setCollisionHeight(double height) {
      this._currentCollisionHeight = height;
   }

   public void setCollisionRadius(double radius) {
      this._currentCollisionRadius = radius;
   }

   public double getCollisionHeight() {
      return this._currentCollisionHeight;
   }

   public double getCollisionRadius() {
      return this._currentCollisionRadius;
   }

   @Override
   public void sendInfo(Player player) {
      if (this.isVisibleFor(player)) {
         if (this._isFakePlayer) {
            player.sendPacket(new FakePlayerInfo(this));
            FakePlayerHolder fakePlayerInfo = this.getTemplate().getFakePlayerInfo();
            int storeType = fakePlayerInfo.getPrivateStoreType();
            if (storeType > 0) {
               String message = fakePlayerInfo.getPrivateStoreMessage();
               if (!message.isEmpty()) {
                  switch (storeType) {
                     case 1:
                        player.sendPacket(new PrivateStoreMsgSell(this.getObjectId(), message));
                     case 2:
                     case 4:
                     case 6:
                     case 7:
                     default:
                        break;
                     case 3:
                        player.sendPacket(new PrivateStoreMsgBuy(this.getObjectId(), message));
                        break;
                     case 5:
                        player.sendPacket(new RecipeShopMsg(this.getObjectId(), message));
                        break;
                     case 8:
                        player.sendPacket(new ExPrivateStoreSetWholeMsg(this.getObjectId(), message));
                  }
               }
            }
         } else if (this.getRunSpeed() == 0.0) {
            player.sendPacket(new ServerObjectInfo(this, player));
         } else {
            player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
         }
      }
   }

   public void showNoTeachHtml(Player player) {
      int npcId = this.getId();
      String html = "";
      if (this instanceof Warehouse) {
         html = HtmCache.getInstance().getHtm(player, "data/html/warehouse/" + npcId + "-noteach.htm");
      } else if (this instanceof Trainer) {
         html = HtmCache.getInstance().getHtm(player, "data/html/trainer/" + npcId + "-noteach.htm");
         if (html == null) {
            html = HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/HealerTrainer/" + npcId + "-noteach.html");
         }
      }

      NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(this.getObjectId());
      if (html == null) {
         LOGGER.warning("Npc " + npcId + " missing noTeach html!");
         noTeachMsg.setHtml("<html><body>I cannot teach you any skills.<br>You must find your current class teachers.</body></html>");
      } else {
         noTeachMsg.setHtml(html);
         noTeachMsg.replace("%objectId%", String.valueOf(this.getObjectId()));
      }

      player.sendPacket(noTeachMsg);
   }

   public Npc scheduleDespawn(long delay) {
      ThreadPool.schedule(() -> {
         if (!this._isDecayed) {
            this.deleteMe();
         }
      }, delay);
      return this;
   }

   @Override
   protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target) {
      if (target != null && target.isPlayable() && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_FINISHED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillFinished(this, target.asPlayer(), skill), this);
      }
   }

   @Override
   public boolean isMovementDisabled() {
      return super.isMovementDisabled() || !this.canMove() || this.getAiType() == AIType.CORPSE;
   }

   public AIType getAiType() {
      return this.getTemplate().getAIType();
   }

   public void setDisplayEffect(int value) {
      if (value != this._displayEffect) {
         this._displayEffect = value;
         this.broadcastPacket(new ExChangeNpcState(this.getObjectId(), value));
      }
   }

   public int getDisplayEffect() {
      return this._displayEffect;
   }

   public int getColorEffect() {
      return 0;
   }

   @Override
   public boolean isNpc() {
      return true;
   }

   @Override
   public Npc asNpc() {
      return this;
   }

   public void setTeam(Team team, boolean broadcast) {
      super.setTeam(team);
      if (broadcast) {
         this.broadcastInfo();
      }
   }

   @Override
   public void setTeam(Team team) {
      super.setTeam(team);
      this.broadcastInfo();
   }

   @Override
   public boolean isWalker() {
      return this._isWalker;
   }

   public void setWalker() {
      this._isWalker = true;
   }

   @Override
   public boolean isChargedShot(ShotType type) {
      return (this._shotsMask & type.getMask()) == type.getMask();
   }

   @Override
   public void setChargedShot(ShotType type, boolean charged) {
      if (charged) {
         this._shotsMask = this._shotsMask | type.getMask();
      } else {
         this._shotsMask = this._shotsMask & ~type.getMask();
      }
   }

   @Override
   public void rechargeShots(boolean physical, boolean magic) {
      if (this._isFakePlayer && FakePlayersConfig.FAKE_PLAYER_USE_SHOTS) {
         if (physical) {
            this.broadcastPacket(new MagicSkillUse(this, this, 2154, 1, 0, 0));
            this.setChargedShot(ShotType.SOULSHOTS, true);
         }

         if (magic) {
            this.broadcastPacket(new MagicSkillUse(this, this, 2159, 1, 0, 0));
            this.setChargedShot(ShotType.SPIRITSHOTS, true);
         }
      } else if (this._soulshotamount > 0 || this._spiritshotamount > 0) {
         if (physical) {
            if (this._soulshotamount == 0 || Rnd.get(100) > this.getSoulShotChance()) {
               return;
            }

            this._soulshotamount--;
            this.broadcastPacket(new MagicSkillUse(this, this, 2154, 1, 0, 0));
            this.setChargedShot(ShotType.SOULSHOTS, true);
         }

         if (magic) {
            if (this._spiritshotamount == 0 || Rnd.get(100) > this.getSpiritShotChance()) {
               return;
            }

            this._spiritshotamount--;
            this.broadcastPacket(new MagicSkillUse(this, this, 2159, 1, 0, 0));
            this.setChargedShot(ShotType.SPIRITSHOTS, true);
         }
      }
   }

   public int getScriptValue() {
      return this._scriptValue;
   }

   public void setScriptValue(int value) {
      this._scriptValue = value;
   }

   public boolean isScriptValue(int value) {
      return this._scriptValue == value;
   }

   public int getAIValue(String paramName) {
      return this.hasAIValue(paramName) ? NpcPersonalAIData.getInstance().getAIValue(this.getSpawn().getName(), paramName) : -1;
   }

   public boolean hasAIValue(String paramName) {
      return this.getSpawn() != null && this.getSpawn().getName() != null && NpcPersonalAIData.getInstance().hasAIValue(this.getSpawn().getName(), paramName);
   }

   public boolean isInMySpawnGroup(Npc npc) {
      return this.getSpawn() != null
         && npc.getSpawn() != null
         && this.getSpawn().getName() != null
         && this.getSpawn().getName().equals(npc.getSpawn().getName());
   }

   public boolean staysInSpawnLoc() {
      return this._spawn != null && this._spawn.getX() == this.getX() && this._spawn.getY() == this.getY();
   }

   public boolean hasVariables() {
      return this.getScript(NpcVariables.class) != null;
   }

   public NpcVariables getVariables() {
      NpcVariables vars = this.getScript(NpcVariables.class);
      return vars != null ? vars : this.addScript(new NpcVariables());
   }

   public void broadcastEvent(String eventName, int radius, WorldObject reference) {
      World.forEachVisibleObjectInRange(this, Npc.class, radius, obj -> {
         if (obj.hasListener(EventType.ON_NPC_EVENT_RECEIVED)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, obj, reference), obj);
         }
      });
   }

   public void sendScriptEvent(String eventName, WorldObject receiver, WorldObject reference) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_EVENT_RECEIVED, receiver)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, receiver.asNpc(), reference), receiver);
      }
   }

   public Location getPointInRange(int radiusMin, int radiusMax) {
      if (radiusMax != 0 && radiusMax >= radiusMin) {
         int radius = Rnd.get(radiusMin, radiusMax);
         double angle = Rnd.nextDouble() * 2.0 * Math.PI;
         return new Location((int)(this.getX() + radius * Math.cos(angle)), (int)(this.getY() + radius * Math.sin(angle)), this.getZ());
      } else {
         return new Location(this.getX(), this.getY(), this.getZ());
      }
   }

   public Item dropItem(Creature creature, int itemId, long itemCount) {
      Item item = null;

      for (int i = 0; i < itemCount; i++) {
         if (ItemData.getInstance().getTemplate(itemId) == null) {
            LOGGER.log(Level.SEVERE, "Item doesn't exist so cannot be dropped. Item ID: " + itemId + " Quest: " + this.getName());
            return null;
         }

         item = ItemManager.createItem(ItemProcessType.LOOT, itemId, itemCount, creature, this);
         if (item == null) {
            return null;
         }

         if (creature != null) {
            item.getDropProtection().protect(creature);
         }

         int newX = this.getX() + Rnd.get(141) - 70;
         int newY = this.getY() + Rnd.get(141) - 70;
         int newZ = this.getZ() + 20;
         item.dropMe(this, newX, newY, newZ);
         if (!GeneralConfig.LIST_PROTECTED_ITEMS.contains(itemId)
            && (
               GeneralConfig.AUTODESTROY_ITEM_AFTER > 0 && !item.getTemplate().hasExImmediateEffect()
                  || GeneralConfig.HERB_AUTO_DESTROY_TIME > 0 && item.getTemplate().hasExImmediateEffect()
            )) {
            ItemsAutoDestroyTaskManager.getInstance().addItem(item);
         }

         item.setProtected(false);
         if (item.isStackable() || !GeneralConfig.MULTIPLE_ITEM_DROP) {
            break;
         }
      }

      return item;
   }

   public Item dropItem(Creature creature, ItemHolder item) {
      return this.dropItem(creature, item.getId(), item.getCount());
   }

   @Override
   public String getName() {
      return this.getTemplate().getName();
   }

   @Override
   public boolean isVisibleFor(Player player) {
      if (this.hasListener(EventType.ON_NPC_CAN_BE_SEEN)) {
         TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcCanBeSeen(this, player), this, TerminateReturn.class);
         if (term != null) {
            return term.terminate();
         }
      }

      return super.isVisibleFor(player);
   }

   public void setTalkable(boolean value) {
      this._isTalkable = value;
   }

   public boolean isTalkable() {
      return this._isTalkable;
   }

   public boolean isQuestMonster() {
      return this._isQuestMonster;
   }

   public void setKillingBlowWeapon(int weaponId) {
      this._killingBlowWeaponId = weaponId;
   }

   public int getKillingBlowWeapon() {
      return this._killingBlowWeaponId;
   }

   @Override
   public boolean isFakePlayer() {
      return this._isFakePlayer;
   }

   public void addSummonedNpc(Npc npc) {
      if (this._summonedNpcs == null) {
         synchronized (this) {
            if (this._summonedNpcs == null) {
               this._summonedNpcs = new ConcurrentHashMap<>();
            }
         }
      }

      this._summonedNpcs.put(npc.getObjectId(), npc);
      npc.setSummoner(this);
   }

   public void removeSummonedNpc(int objectId) {
      if (this._summonedNpcs != null) {
         this._summonedNpcs.remove(objectId);
      }
   }

   public Collection<Npc> getSummonedNpcs() {
      return (Collection<Npc>)(this._summonedNpcs != null ? this._summonedNpcs.values() : Collections.emptyList());
   }

   public Npc getSummonedNpc(int objectId) {
      return this._summonedNpcs != null ? this._summonedNpcs.get(objectId) : null;
   }

   public int getSummonedNpcCount() {
      return this._summonedNpcs != null ? this._summonedNpcs.size() : 0;
   }

   public void resetSummonedNpcs() {
      if (this._summonedNpcs != null) {
         this._summonedNpcs.clear();
      }
   }

   public void broadcastSay(ChatType chatType, String text) {
      World.broadcastToVisiblePlayers(this, new NpcSay(this, chatType, text));
   }

   public void broadcastSay(ChatType chatType, String text, int radius) {
      World.broadcastToVisiblePlayersInRange(this, new NpcSay(this, chatType, text), radius);
   }

   @Override
   public int getMinShopDistance() {
      return PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC;
   }

   public void addQuestTimer(QuestTimer questTimer) {
      synchronized (this._questTimers) {
         this._questTimers.add(questTimer);
      }
   }

   public void removeQuestTimer(QuestTimer questTimer) {
      synchronized (this._questTimers) {
         this._questTimers.remove(questTimer);
      }
   }

   public void stopQuestTimers() {
      synchronized (this._questTimers) {
         for (QuestTimer timer : this._questTimers) {
            timer.cancelTask();
         }

         this._questTimers.clear();
      }
   }

   public void addTimerHolder(TimerHolder<?> timer) {
      synchronized (this._timerHolders) {
         this._timerHolders.add(timer);
      }
   }

   public void removeTimerHolder(TimerHolder<?> timer) {
      synchronized (this._timerHolders) {
         this._timerHolders.remove(timer);
      }
   }

   public void stopTimerHolders() {
      synchronized (this._timerHolders) {
         for (TimerHolder<?> timer : this._timerHolders) {
            timer.cancelTask();
         }

         this._timerHolders.clear();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getClass().getSimpleName());
      sb.append(':');
      sb.append(this.getName());
      sb.append('(');
      sb.append(this.getId());
      sb.append(")[");
      sb.append(this.getObjectId());
      sb.append(']');
      return sb.toString();
   }
}
