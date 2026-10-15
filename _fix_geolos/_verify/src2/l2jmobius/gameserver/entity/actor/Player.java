package org.l2jmobius.gameserver.entity.actor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.PlayerAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.FreeMountsConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.config.custom.PvpTitleColorConfig;
import org.l2jmobius.gameserver.config.custom.RebirthConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ExperienceLossData;
import org.l2jmobius.gameserver.data.xml.FishData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player.1;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.TimeStamp;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ContactList;
import org.l2jmobius.gameserver.entity.actor.holders.player.Macro;
import org.l2jmobius.gameserver.entity.actor.holders.player.MacroList;
import org.l2jmobius.gameserver.entity.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.Radar;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.TeleportBookmark;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.actor.instance.ControlTower;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.actor.instance.Decoy;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.EventMonster;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyMob;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.TamedBeast;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.entity.actor.tasks.player.DismountTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.FameTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.InventoryEnableTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.LookingForFishTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.PetFeedTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.RentPetTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.ResetChargesTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.ResetSoulsTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.SitDownTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.StandUpTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.TeleportWatchdogTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.VitalityTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.WarnUserTakeBreakTask;
import org.l2jmobius.gameserver.entity.actor.tasks.player.WaterTask;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.actor.transform.AdditionalSkillHolder;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.clan.ClanPrivileges;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.PremiumItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerRefund;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.BossZone;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.RebirthManager;
import org.l2jmobius.gameserver.managers.RecipeManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerFameChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerHennaRemove;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerKarmaChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPKChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPvPChanged;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSit;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerTransform;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.fishing.Fish;
import org.l2jmobius.gameserver.mechanics.fishing.Fishing;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerHolder;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.TerritoryWard;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.holders.RequestPartner;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ExBrExtraUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingStart;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.ExSetCompassZoneCode;
import org.l2jmobius.gameserver.network.serverpackets.ExStartScenePlayer;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.FriendStatusPacket;
import org.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.NicknameChanged;
import org.l2jmobius.gameserver.network.serverpackets.ObservationMode;
import org.l2jmobius.gameserver.network.serverpackets.ObservationReturn;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.Ride;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.network.serverpackets.Snoop;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.TargetUnselected;
import org.l2jmobius.gameserver.network.serverpackets.TradeDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeStart;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PlayerAutoSaveTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Player extends Playable {
   private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
   private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
   private static final String ADD_NEW_SKILLS = "REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
   private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
   private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";
   private static final String ADD_SKILL_SAVE = "REPLACE INTO character_skills_save (charId,skill_id,skill_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
   private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
   private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
   private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
   private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
   private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
   private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate,lastAccess,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
   private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=?,faction=?,pccafe_points=?,last_recom_date=?,rec_have=?,rec_left=? WHERE charId=?";
   private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE charId=?";
   private static final String INSERT_TP_BOOKMARK = "INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)";
   private static final String UPDATE_TP_BOOKMARK = "UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?";
   private static final String RESTORE_TP_BOOKMARK = "SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?";
   private static final String DELETE_TP_BOOKMARK = "DELETE FROM character_tpbookmark WHERE charId=? AND Id=?";
   private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
   private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
   private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
   private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
   private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
   private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
   private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
   private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
   private static final String RESTORE_CHAR_RECOMS = "SELECT charId,target_id FROM character_recommends WHERE charId=?";
   private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (charId,target_id) VALUES (?,?)";
   private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE charId=?";
   private static final String DELETE_CHAR_RECIPE_SHOP = "DELETE FROM character_recipeshoplist WHERE charId=?";
   private static final String INSERT_CHAR_RECIPE_SHOP = "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)";
   private static final String RESTORE_CHAR_RECIPE_SHOP = "SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`";
   public static final String NEWBIE_KEY = "NEWBIE";
   public static final int ID_NONE = -1;
   public static final int REQUEST_TIMEOUT = 15;
   private int _pcCafePoints = 0;
   private GameClient _client;
   private String _ip = "N/A";
   private final String _accountName;
   private long _deleteTimer;
   private Calendar _createDate = Calendar.getInstance();
   private String _lang = null;
   private String _htmlPrefix = "";
   private volatile boolean _isOnline = false;
   private boolean _offlinePlay = false;
   private boolean _enteredWorld = false;
   private long _onlineTime;
   private long _onlineBeginTime;
   private long _lastAccess;
   private long _uptime;
   private final InventoryUpdate _inventoryUpdate = new InventoryUpdate();
   private ScheduledFuture<?> _inventoryUpdateTask;
   private ScheduledFuture<?> _itemListTask;
   private ScheduledFuture<?> _skillListTask;
   private ScheduledFuture<?> _updateAndBroadcastStatusTask;
   private ScheduledFuture<?> _broadcastCharInfoTask;
   private ScheduledFuture<?> _broadcastStatusUpdateTask;
   private boolean _subclassLock = false;
   protected int _baseClass;
   protected int _activeClass;
   protected int _classIndex = 0;
   private int _controlItemId;
   private PetLevelData _leveldata;
   private int _curFeed;
   protected Future<?> _mountFeedTask;
   private ScheduledFuture<?> _dismountTask;
   private boolean _petItems = false;
   private final Map<Integer, SubClassHolder> _subClasses = new ConcurrentHashMap<>();
   private final PlayerAppearance _appearance;
   private long _expBeforeDeath;
   private int _pvpKills;
   private int _pkKills;
   private byte _pvpFlag;
   private int _fame;
   private ScheduledFuture<?> _fameTask;
   private ScheduledFuture<?> _vitalityTask;
   private ScheduledFuture<?> _teleportWatchdog;
   private byte _siegeState = 0;
   private int _siegeSide = 0;
   private int _curWeightPenalty = 0;
   private int _lastCompassZone;
   private boolean _isIn7sDungeon = false;
   private final ContactList _contactList = new ContactList(this);
   private int _bookmarkSlot = 0;
   private final Map<Integer, TeleportBookmark> _tpbookmarks = new ConcurrentHashMap<>();
   private boolean _canFeed;
   private boolean _isInSiege;
   private boolean _isInHideoutSiege = false;
   private boolean _inOlympiadMode = false;
   private boolean _olympiadStart = false;
   private int _olympiadGameId = -1;
   private int _olympiadSide = -1;
   private int _olyBuffsCount = 0;
   private boolean _isInDuel = false;
   private boolean _startingDuel = false;
   private int _duelState = 0;
   private int _duelId = 0;
   private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
   private Vehicle _vehicle = null;
   private Location _inVehiclePosition;
   public ScheduledFuture<?> _taskForFish;
   private MountType _mountType = MountType.NONE;
   private int _mountNpcId;
   private int _mountLevel;
   private int _mountObjectID = 0;
   public int _telemode = 0;
   private boolean _inCrystallize;
   private boolean _isCrafting;
   private long _offlineShopStart = 0L;
   private Transform _transformation;
   private final Map<Integer, Skill> _transformSkills = new ConcurrentHashMap<>();
   private final Map<Integer, RecipeList> _dwarvenRecipeBook = new ConcurrentHashMap<>();
   private final Map<Integer, RecipeList> _commonRecipeBook = new ConcurrentHashMap<>();
   private final Map<Integer, PremiumItem> _premiumItems = new ConcurrentHashMap<>();
   private boolean _waitTypeSitting;
   private boolean _sittingInProgress;
   private final Location _lastLoc = new Location(0, 0, 0);
   private boolean _observerMode = false;
   private final Location _lastServerPosition = new Location(0, 0, 0);
   private final AtomicBoolean _blinkActive = new AtomicBoolean();
   private int _recomHave;
   private int _recomLeft;
   private long _lastRecomUpdate;
   private final Collection<Integer> _recomChars = ConcurrentHashMap.newKeySet();
   private final PlayerInventory _inventory = new PlayerInventory(this);
   private final PlayerFreight _freight = new PlayerFreight(this);
   private final PlayerWarehouse _warehouse = new PlayerWarehouse(this);
   private PlayerRefund _refund;
   private PrivateStoreType _privateStoreType = PrivateStoreType.NONE;
   private TradeList _activeTradeList;
   private ItemContainer _activeWarehouse;
   private Map<Integer, ManufactureItem> _manufactureItems;
   private String _storeName = "";
   private TradeList _sellList;
   private TradeList _buyList;
   private PreparedListContainer _currentMultiSell = null;
   private int _newbie;
   private boolean _noble = false;
   private boolean _hero = false;
   private boolean _premiumStatus = false;
   private boolean _isGood = false;
   private boolean _isEvil = false;
   private Npc _lastFolkNpc = null;
   private int _questNpcObject = 0;
   private final Map<String, QuestState> _quests = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
   private final Shortcuts _shortcuts = new Shortcuts(this);
   private final MacroList _macros = new MacroList(this);
   private final Set<Player> _snoopListener = ConcurrentHashMap.newKeySet(1);
   private final Set<Player> _snoopedPlayer = ConcurrentHashMap.newKeySet(1);
   private final Henna[] _henna = new Henna[3];
   private int _hennaSTR;
   private int _hennaINT;
   private int _hennaDEX;
   private int _hennaMEN;
   private int _hennaWIT;
   private int _hennaCON;
   private Summon _summon = null;
   private Decoy _decoy = null;
   private Trap _trap = null;
   private int _agathionId = 0;
   private Collection<TamedBeast> _tamedBeast = null;
   private boolean _minimapAllowed = false;
   private final Radar _radar;
   private ScheduledFuture<?> _taskWarnUserTakeBreak;
   private int _partyroom = 0;
   private int _clanId;
   private Clan _clan;
   private int _apprentice = 0;
   private int _sponsor = 0;
   private long _clanJoinExpiryTime;
   private long _clanCreateExpiryTime;
   private int _powerGrade = 0;
   private ClanPrivileges _clanPrivileges = new ClanPrivileges();
   private int _pledgeClass = 0;
   private int _pledgeType = 0;
   private int _lvlJoinedAcademy = 0;
   private int _wantsPeace = 0;
   private int _deathPenaltyBuffLevel = 0;
   private final AtomicInteger _charges = new AtomicInteger();
   private ScheduledFuture<?> _chargeTask = null;
   private int _souls = 0;
   private ScheduledFuture<?> _soulTask = null;
   private Location _currentSkillWorldPosition;
   private AccessLevel _accessLevel;
   private boolean _messageRefusal = false;
   private boolean _silenceMode = false;
   private List<Integer> _silenceModeExcluded;
   private boolean _dietMode = false;
   private boolean _tradeRefusal = false;
   private boolean _exchangeRefusal = false;
   private Party _party;
   PartyDistributionType _partyDistributionType;
   private Player _activeRequester;
   private long _requestExpireTime = 0L;
   private final RequestPartner _request = new RequestPartner(this);
   private Item _arrowItem;
   private Item _boltItem;
   private long _spawnProtectEndTime = 0L;
   private long _teleportProtectEndTime = 0L;
   private long _recentFakeDeathEndTime = 0L;
   private boolean _isFakeDeath;
   private Weapon _fistsWeaponItem;
   private final Map<Integer, String> _chars = new LinkedHashMap<>();
   private int _expertiseArmorPenalty = 0;
   private int _expertiseWeaponPenalty = 0;
   private int _expertisePenaltyBonus = 0;
   private boolean _isEnchanting = false;
   private int _activeEnchantItemId = -1;
   private int _activeEnchantSupportItemId = -1;
   private int _activeEnchantAttrItemId = -1;
   private long _activeEnchantTimestamp = 0L;
   protected boolean _inventoryDisable = false;
   private final Map<Integer, Cubic> _cubics = new ConcurrentSkipListMap<>();
   private final ConcurrentLinkedDeque<Integer> _cubicOrder = new ConcurrentLinkedDeque<>();
   protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);
   public ReentrantLock soulShotLock = new ReentrantLock();
   private boolean _isRegisteredOnEvent = false;
   private boolean _isOnSoloEvent = false;
   private boolean _isOnEvent = false;
   private byte _handysBlockCheckerEventArena = -1;
   private KrateiArena _krateiArena = null;
   private int _ucKills = 0;
   private int _ucDeaths = 0;
   private int _ucState = 0;
   public static final int UC_STATE_NONE = 0;
   public static final int UC_STATE_POINT = 1;
   public static final int UC_STATE_ARENA = 2;
   private final int[] _loto = new int[5];
   private final int[] _raceTickets = new int[2];
   private final BlockList _blockList = new BlockList(this);
   private Fish _fish;
   private Item _lure;
   private Fishing _fishCombat;
   private boolean _fishing = false;
   private int _fishX = 0;
   private int _fishY = 0;
   private int _fishZ = 0;
   private ScheduledFuture<?> _taskRentPet;
   private ScheduledFuture<?> _taskWater;
   private final int[] _htmlActionOriginObjectIds = new int[HtmlActionScope.values().length];
   private int _lastHtmlActionOriginObjId;
   private final List<String>[] _htmlActionCaches = new List[HtmlActionScope.values().length];
   private Forum _forumMail;
   private Forum _forumMemo;
   private SkillUseHolder _currentSkill;
   private SkillUseHolder _currentPetSkill;
   private SkillUseHolder _queuedSkill;
   private int _cursedWeaponEquippedId = 0;
   private boolean _combatFlagEquippedId = false;
   private boolean _canRevive = true;
   private int _reviveRequested = 0;
   private double _revivePower = 0.0;
   private boolean _revivePet = false;
   private double _cpUpdateIncCheck = 0.0;
   private double _cpUpdateDecCheck = 0.0;
   private double _cpUpdateInterval = 0.0;
   private double _mpUpdateIncCheck = 0.0;
   private double _mpUpdateDecCheck = 0.0;
   private double _mpUpdateInterval = 0.0;
   private double _originalCp = 0.0;
   private double _originalHp = 0.0;
   private double _originalMp = 0.0;
   private int _clientX;
   private int _clientY;
   private int _clientZ;
   private int _clientHeading;
   private static final int FALLING_VALIDATION_DELAY = 1000;
   private volatile long _fallingTimestamp = 0L;
   private volatile int _fallingDamage = 0;
   private Future<?> _fallingDamageTask = null;
   private int _multiSocialTarget = 0;
   private int _multiSociaAction = 0;
   private MovieHolder _movieHolder = null;
   private String _adminConfirmCmd = null;
   private volatile long _lastItemAuctionInfoRequest = 0L;
   private long _pvpFlagLasts;
   private long _notMoveUntil = 0L;
   private Map<Integer, Skill> _customSkills = null;
   private volatile int _actionMask;
   private Map<Stat, Double> _servitorShare;
   private boolean _married = false;
   private int _partnerId = 0;
   private int _coupleId = 0;
   private boolean _engagerequest = false;
   private int _engageid = 0;
   private boolean _marryrequest = false;
   private boolean _marryaccepted = false;
   private static final String GAME_POINTS_VAR = "PRIME_POINTS";
   private String _lastPetitionGmName = null;
   private boolean _hasCharmOfCourage = false;
   private final AutoPlaySettingsHolder _autoPlaySettings = new AutoPlaySettingsHolder();
   private final AutoUseSettingsHolder _autoUseSettings = new AutoUseSettingsHolder();
   private final AtomicBoolean _autoPlaying = new AtomicBoolean();
   private final List<QuestTimer> _questTimers = new ArrayList<>();
   private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
   private boolean _isSellingBuffs = false;
   private List<SellBuffHolder> _sellingBuffs = null;
   private final Collection<QuestState> _notifyQuestOfDeathList = ConcurrentHashMap.newKeySet();
   private PlayerClass _learningClass = this.getPlayerClass();
   private final Collection<Integer> _friendList = ConcurrentHashMap.newKeySet();

   private Player(int var1, PlayerTemplate var2, String var3, PlayerAppearance var4) {
      super(var1, var2);
      this.setInstanceType(InstanceType.Player);
      this.initCharStatusUpdateValues();
      this.initPcStatusUpdateValues();

      for (int var5 = 0; var5 < this._htmlActionCaches.length; var5++) {
         this._htmlActionCaches[var5] = new ArrayList<>();
      }

      this._accountName = var3;
      var4.setOwner(this);
      this._appearance = var4;
      this.getAI();
      this._radar = new Radar(this);
      this.startVitalityTask();
   }

   private Player(PlayerTemplate var1, String var2, PlayerAppearance var3) {
      this(IdManager.getInstance().getNextId(), var1, var2, var3);
   }

   public void setPvpFlagLasts(long var1) {
      this._pvpFlagLasts = var1;
   }

   public long getPvpFlagLasts() {
      return this._pvpFlagLasts;
   }

   public void startPvPFlag() {
      this.updatePvPFlag(1);
      PvpFlagTaskManager.getInstance().add(this);
   }

   public void stopPvpRegTask() {
      PvpFlagTaskManager.getInstance().remove(this);
   }

   public void stopPvPFlag() {
      this.stopPvpRegTask();
      this.updatePvPFlag(0);
   }

   public boolean isSellingBuffs() {
      return this._isSellingBuffs;
   }

   public void setSellingBuffs(boolean var1) {
      this._isSellingBuffs = var1;
   }

   public List<SellBuffHolder> getSellingBuffs() {
      if (this._sellingBuffs == null) {
         this._sellingBuffs = new ArrayList<>();
      }

      return this._sellingBuffs;
   }

   public static Player create(PlayerTemplate var0, String var1, String var2, PlayerAppearance var3) {
      Player var4 = new Player(var0, var1, var3);
      var4.setName(var2);
      var4.setCreateDate(Calendar.getInstance());
      var4.setBaseClass(var4.getPlayerClass());
      var4.setNewbie(1);
      return var4.createDb() ? var4 : null;
   }

   public String getAccountName() {
      return this._client == null ? this._accountName : this._client.getAccountName();
   }

   public String getAccountNamePlayer() {
      return this._accountName;
   }

   public Map<Integer, String> getAccountChars() {
      return this._chars;
   }

   public int getRelation(Player var1) {
      int var2 = 0;
      if (this._clan != null) {
         var2 |= 64;
         if (this.getClan() == var1.getClan()) {
            var2 |= 256;
         }

         if (this.getAllyId() != 0) {
            var2 |= 65536;
         }
      }

      if (this.isClanLeader()) {
         var2 |= 128;
      }

      if (this.getParty() != null && this.getParty() == var1.getParty()) {
         var2 |= 32;

         for (int var3 = 0; var3 < this._party.getMembers().size(); var3++) {
            if (this._party.getMembers().get(var3) == this) {
               switch (var3) {
                  case 0:
                     var2 |= 16;
                     break;
                  case 1:
                     var2 |= 8;
                     break;
                  case 2:
                     var2 |= 7;
                     break;
                  case 3:
                     var2 |= 6;
                     break;
                  case 4:
                     var2 |= 5;
                     break;
                  case 5:
                     var2 |= 4;
                     break;
                  case 6:
                     var2 |= 3;
                     break;
                  case 7:
                     var2 |= 2;
                     break;
                  case 8:
                     var2 |= 1;
               }
            }
         }
      }

      if (this._siegeState != 0) {
         if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(this) != 0) {
            var2 |= 524288;
         } else {
            var2 |= 512;
            if (this.getSiegeState() != var1.getSiegeState()) {
               var2 |= 4096;
            } else {
               var2 |= 2048;
            }

            if (this._siegeState == 1) {
               var2 |= 1024;
            }
         }
      }

      if (this.getClan() != null
         && var1.getClan() != null
         && var1.getPledgeType() != -1
         && this.getPledgeType() != -1
         && var1.getClan().isAtWarWith(this.getClan().getId())) {
         var2 |= 32768;
         if (this.getClan().isAtWarWith(var1.getClan().getId())) {
            var2 |= 16384;
         }
      }

      if (this._handysBlockCheckerEventArena != -1) {
         var2 |= 512;
         ArenaParticipantsHolder var7 = HandysBlockCheckerManager.getInstance().getHolder(this.getBlockCheckerArena());
         if (var7.getPlayerTeam(this) == 0) {
            var2 |= 4096;
         } else {
            var2 |= 2048;
         }

         var2 |= 1024;
      }

      return var2;
   }

   public static Player load(int var0) {
      return restore(var0);
   }

   private void initPcStatusUpdateValues() {
      this._cpUpdateInterval = this.getMaxCp() / 352.0;
      this._cpUpdateIncCheck = this.getMaxCp();
      this._cpUpdateDecCheck = this.getMaxCp() - this._cpUpdateInterval;
      this._mpUpdateInterval = this.getMaxMp() / 352.0;
      this._mpUpdateIncCheck = this.getMaxMp();
      this._mpUpdateDecCheck = this.getMaxMp() - this._mpUpdateInterval;
   }

   public PlayerStat getStat() {
      return (PlayerStat)super.getStat();
   }

   public void initCharStat() {
      this.setStat(new PlayerStat(this));
   }

   public PlayerStatus getStatus() {
      return (PlayerStatus)super.getStatus();
   }

   public void initCharStatus() {
      this.setStatus(new PlayerStatus(this));
   }

   public PlayerAppearance getAppearance() {
      return this._appearance;
   }

   public PlayerTemplate getBaseTemplate() {
      return PlayerTemplateData.getInstance().getTemplate(this._baseClass);
   }

   public PlayerTemplate getTemplate() {
      return (PlayerTemplate)super.getTemplate();
   }

   public void setTemplate(PlayerClass var1) {
      super.setTemplate(PlayerTemplateData.getInstance().getTemplate(var1));
   }

   protected CreatureAI initAI() {
      return new PlayerAI(this);
   }

   public int getLevel() {
      return this.getStat().getLevel();
   }

   public double getLevelMod() {
      if (this.isTransformed()) {
         double var1 = this._transformation.getLevelMod(this);
         if (var1 > -1.0) {
            return var1;
         }
      }

      return super.getLevelMod();
   }

   public int getNewbie() {
      return this._newbie;
   }

   public void setNewbie(int var1) {
      this._newbie = var1;
   }

   public void setBaseClass(int var1) {
      this._baseClass = var1;
   }

   public void setBaseClass(PlayerClass var1) {
      this._baseClass = var1.getId();
   }

   public boolean isInStoreMode() {
      return this._privateStoreType != PrivateStoreType.NONE;
   }

   public boolean isCrafting() {
      return this._isCrafting;
   }

   public void setCrafting(boolean var1) {
      this._isCrafting = var1;
   }

   public Collection<RecipeList> getCommonRecipeBook() {
      return this._commonRecipeBook.values();
   }

   public Collection<RecipeList> getDwarvenRecipeBook() {
      return this._dwarvenRecipeBook.values();
   }

   public void registerCommonRecipeList(RecipeList var1, boolean var2) {
      this._commonRecipeBook.put(var1.getId(), var1);
      if (var2) {
         this.insertNewRecipeData(var1.getId(), false);
      }
   }

   public void registerDwarvenRecipeList(RecipeList var1, boolean var2) {
      this._dwarvenRecipeBook.put(var1.getId(), var1);
      if (var2) {
         this.insertNewRecipeData(var1.getId(), true);
      }
   }

   public boolean hasRecipeList(int var1) {
      return this._dwarvenRecipeBook.containsKey(var1) || this._commonRecipeBook.containsKey(var1);
   }

   public void unregisterRecipeList(int var1) {
      if (this._dwarvenRecipeBook.remove(var1) != null) {
         this.deleteRecipeData(var1, true);
      } else if (this._commonRecipeBook.remove(var1) != null) {
         this.deleteRecipeData(var1, false);
      } else {
         LOGGER.warning("Attempted to remove unknown RecipeList: " + var1);
      }

      for (Shortcut var3 : this._shortcuts.getAllShortcuts()) {
         if (var3 != null && var3.getId() == var1 && var3.getType() == ShortcutType.RECIPE) {
            this.deleteShortcut(var3.getSlot(), var3.getPage());
         }
      }
   }

   private void insertNewRecipeData(int var1, boolean var2) {
      try (
         Connection var3 = DatabaseFactory.getConnection();
         PreparedStatement var4 = var3.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)");
      ) {
         var4.setInt(1, this.getObjectId());
         var4.setInt(2, var1);
         var4.setInt(3, var2 ? this._classIndex : 0);
         var4.setInt(4, var2 ? 1 : 0);
         var4.execute();
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "SQL exception while inserting recipe: " + var1 + " from character " + this.getObjectId(), (Throwable)var11);
      }
   }

   private void deleteRecipeData(int var1, boolean var2) {
      try (
         Connection var3 = DatabaseFactory.getConnection();
         PreparedStatement var4 = var3.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?");
      ) {
         var4.setInt(1, this.getObjectId());
         var4.setInt(2, var1);
         var4.setInt(3, var2 ? this._classIndex : 0);
         var4.execute();
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "SQL exception while deleting recipe: " + var1 + " from character " + this.getObjectId(), (Throwable)var11);
      }
   }

   public int getLastQuestNpcObject() {
      return this._questNpcObject;
   }

   public void setLastQuestNpcObject(int var1) {
      this._questNpcObject = var1;
   }

   public QuestState getQuestState(String var1) {
      return this._quests.get(var1);
   }

   public void setQuestState(QuestState var1) {
      this._quests.put(var1.getQuestName(), var1);
   }

   public boolean hasQuestState(String var1) {
      return this._quests.containsKey(var1);
   }

   public void delQuestState(String var1) {
      this._quests.remove(var1);
   }

   public List<Quest> getAllActiveQuests() {
      ArrayList var1 = new ArrayList();

      for (QuestState var3 : this._quests.values()) {
         if (var3 != null && var3.getQuest() != null && var3.isStarted()) {
            int var4 = var3.getQuest().getId();
            if (var4 <= 19999 && var4 >= 1) {
               var1.add(var3.getQuest());
            }
         }
      }

      return var1;
   }

   public void processScriptEvent(String var1, String var2) {
      Quest var3 = ScriptManager.getInstance().getScript(var1);
      if (var3 != null && var2 != null && !var2.isEmpty()) {
         Npc var4 = this._lastFolkNpc;
         if (var4 != null && this.isInsideRadius2D(var4, 250)) {
            var3.notifyEvent(var2, var4, this);
         } else if (this._questNpcObject > 0) {
            WorldObject var5 = World.findObject(this.getLastQuestNpcObject());
            if (var5 != null && var5.isNpc() && this.isInsideRadius2D(var5, 250)) {
               Npc var6 = var5.asNpc();
               var3.notifyEvent(var2, var6, this);
            }
         }
      }
   }

   public void addNotifyQuestOfDeath(QuestState var1) {
      if (var1 != null && !this._notifyQuestOfDeathList.contains(var1)) {
         this._notifyQuestOfDeathList.add(var1);
      }
   }

   public void removeNotifyQuestOfDeath(QuestState var1) {
      if (var1 != null) {
         this._notifyQuestOfDeathList.remove(var1);
      }
   }

   public Collection<QuestState> getNotifyQuestOfDeath() {
      return this._notifyQuestOfDeathList;
   }

   public boolean isNotifyQuestOfDeathEmpty() {
      return this._notifyQuestOfDeathList.isEmpty();
   }

   private void restoreShortcuts() {
      this._shortcuts.restoreMe();
   }

   public Collection<Shortcut> getAllShortcuts() {
      return this._shortcuts.getAllShortcuts();
   }

   public Shortcut getShortcut(int var1, int var2) {
      return this._shortcuts.getShortcut(var1, var2);
   }

   public void registerShortcut(Shortcut var1) {
      this._shortcuts.registerShortcut(var1);
   }

   public void updateShortcuts(int var1, int var2) {
      this._shortcuts.updateShortcuts(var1, var2);
   }

   public void deleteShortcut(int var1, int var2) {
      this._shortcuts.deleteShortcut(var1, var2);
   }

   public void registerMacro(Macro var1) {
      this._macros.registerMacro(var1);
   }

   public void deleteMacro(int var1) {
      this._macros.deleteMacro(var1);
   }

   public MacroList getMacros() {
      return this._macros;
   }

   public void setSiegeState(byte var1) {
      this._siegeState = var1;
   }

   public byte getSiegeState() {
      return this._siegeState;
   }

   public void setSiegeSide(int var1) {
      this._siegeSide = var1;
   }

   public boolean isRegisteredOnThisSiegeField(int var1) {
      return this._siegeSide == var1 || this._siegeSide >= 81 && this._siegeSide <= 89;
   }

   public int getSiegeSide() {
      return this._siegeSide;
   }

   public boolean isSiegeFriend(WorldObject var1) {
      if (this._siegeState != 0 && this.isInsideZone(ZoneId.SIEGE)) {
         Castle var2 = CastleManager.getInstance().getCastleById(this._siegeSide);
         if (var2 == null) {
            return false;
         } else {
            Player var3 = var1.asPlayer();
            if (var3 != null && var3 != this) {
               if (var3.getSiegeSide() != this._siegeSide || this._siegeState != var3.getSiegeState()) {
                  return false;
               } else if (this._siegeState != 1) {
                  return true;
               } else {
                  return !var2.isFirstMidVictory() && this._siegeState == var3.getSiegeState() ? true : var2.getOwner() == null;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void setPvpFlag(int var1) {
      this._pvpFlag = (byte)var1;
   }

   public byte getPvpFlag() {
      return this._pvpFlag;
   }

   public void updatePvPFlag(int var1) {
      if (this._pvpFlag != var1) {
         this.setPvpFlag(var1);
         this.updateUserInfo();
         if (this.hasSummon()) {
            this.sendPacket(new RelationChanged(this._summon, this.getRelation(this), false));
         }

         World.forEachVisibleObject(this, Player.class, var1x -> {
            var1x.sendPacket(new RelationChanged(this, this.getRelation(var1x), this.isAutoAttackable(var1x)));
            if (this.hasSummon()) {
               var1x.sendPacket(new RelationChanged(this._summon, this.getRelation(var1x), this.isAutoAttackable(var1x)));
            }
         });
      }
   }

   public void revalidateZone(boolean var1) {
      if (this.getWorldRegion() != null && (var1 || !(this.calculateDistance3D(this._lastZoneValidateLocation) < 100.0))) {
         this._lastZoneValidateLocation.setXYZ(this);
         ZoneManager.getInstance().getRegion(this).revalidateZones(this);
         if (GeneralConfig.ALLOW_WATER) {
            this.checkWaterState();
         }

         if (this.isInsideZone(ZoneId.ALTERED)) {
            if (this._lastCompassZone == 8) {
               return;
            }

            this._lastCompassZone = 8;
            this.sendPacket(new ExSetCompassZoneCode(8));
         } else if (this.isInsideZone(ZoneId.SIEGE)) {
            if (this._lastCompassZone == 11) {
               return;
            }

            this._lastCompassZone = 11;
            this.sendPacket(new ExSetCompassZoneCode(11));
         } else if (this.isInsideZone(ZoneId.PVP)) {
            if (this._lastCompassZone == 14) {
               return;
            }

            this._lastCompassZone = 14;
            this.sendPacket(new ExSetCompassZoneCode(14));
         } else if (this._isIn7sDungeon) {
            if (this._lastCompassZone == 13) {
               return;
            }

            this._lastCompassZone = 13;
            this.sendPacket(new ExSetCompassZoneCode(13));
         } else if (this.isInsideZone(ZoneId.PEACE)) {
            if (this._lastCompassZone == 12) {
               return;
            }

            this._lastCompassZone = 12;
            this.sendPacket(new ExSetCompassZoneCode(12));
         } else if (this.isInsideZone(ZoneId.NO_PVP)) {
            if (this._lastCompassZone == 12) {
               return;
            }

            this._lastCompassZone = 12;
            this.sendPacket(new ExSetCompassZoneCode(12));
         } else {
            if (this._lastCompassZone == 15) {
               return;
            }

            if (this._lastCompassZone == 11) {
               this.updatePvPStatus();
            }

            this._lastCompassZone = 15;
            this.sendPacket(new ExSetCompassZoneCode(15));
         }
      }
   }

   public boolean hasDwarvenCraft() {
      return this.getSkillLevel(CommonSkill.CREATE_DWARVEN.getId()) >= 1;
   }

   public int getDwarvenCraft() {
      return this.getSkillLevel(CommonSkill.CREATE_DWARVEN.getId());
   }

   public boolean hasCommonCraft() {
      return this.getSkillLevel(CommonSkill.CREATE_COMMON.getId()) >= 1;
   }

   public int getCommonCraft() {
      return this.getSkillLevel(CommonSkill.CREATE_COMMON.getId());
   }

   public int getPkKills() {
      return this._pkKills;
   }

   public void setPkKills(int var1) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PK_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPKChanged(this, this._pkKills, var1), this);
      }

      this._pkKills = var1;
   }

   public long getDeleteTimer() {
      return this._deleteTimer;
   }

   public void setDeleteTimer(long var1) {
      this._deleteTimer = var1;
   }

   public long getLastRecomUpdate() {
      return this._lastRecomUpdate;
   }

   public void setLastRecomUpdate(long var1) {
      this._lastRecomUpdate = var1;
   }

   public int getRecomHave() {
      return this._recomHave;
   }

   protected void incRecomHave() {
      if (this._recomHave < 255) {
         this._recomHave++;
      }
   }

   public void setRecomHave(int var1) {
      this._recomHave = Math.clamp((long)var1, 0, 255);
   }

   public int getRecomLeft() {
      return this._recomLeft;
   }

   protected void decRecomLeft() {
      if (this._recomLeft > 0) {
         this._recomLeft--;
      }
   }

   public void giveRecom(Player var1) {
      var1.incRecomHave();
      this.decRecomLeft();
      this._recomChars.add(var1.getObjectId());
      if (!PlayerConfig.ALT_RECOMMEND) {
         try (
            Connection var2 = DatabaseFactory.getConnection();
            PreparedStatement var3 = var2.prepareStatement("INSERT INTO character_recommends (charId,target_id) VALUES (?,?)");
         ) {
            var3.setInt(1, this.getObjectId());
            var3.setInt(2, var1.getObjectId());
            var3.execute();
         } catch (Exception var10) {
            LOGGER.log(Level.SEVERE, "Failed updating character recommendations for player: " + this.getObjectId(), (Throwable)var10);
         }
      }
   }

   public boolean canRecom(Player var1) {
      return !this._recomChars.contains(var1.getObjectId());
   }

   private void restoreRecom() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT charId,target_id FROM character_recommends WHERE charId=?");
      ) {
         var2.setInt(1, this.getObjectId());
         ResultSet var3 = var2.executeQuery();

         while (var3.next()) {
            this._recomChars.add(var3.getInt("target_id"));
         }
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Could not restore Recommendations for player: " + this.getObjectId(), (Throwable)var9);
      }
   }

   private void checkRecom(int var1, int var2) {
      Calendar var3 = Calendar.getInstance();
      var3.setTimeInMillis(this._lastRecomUpdate);
      var3.add(5, 1);
      Calendar var4 = Calendar.getInstance();
      this._recomHave = var1;
      this._recomLeft = var2;
      if (this.getStat().getLevel() >= 10 && !var3.after(var4)) {
         this.restartRecom();
      }
   }

   public void restartRecom() {
      this._recomChars.clear();
      if (!PlayerConfig.ALT_RECOMMEND) {
         try (
            Connection var1 = DatabaseFactory.getConnection();
            PreparedStatement var2 = var1.prepareStatement("DELETE FROM character_recommends WHERE charId=?");
         ) {
            var2.setInt(1, this.getObjectId());
            var2.execute();
         } catch (Exception var9) {
            LOGGER.log(Level.SEVERE, "Failed cleaning character recommendations for player: " + this.getObjectId(), (Throwable)var9);
         }
      }

      if (this.getStat().getLevel() < 20) {
         this._recomLeft = 3;
         this._recomHave--;
      } else if (this.getStat().getLevel() < 40) {
         this._recomLeft = 6;
         this._recomHave -= 2;
      } else {
         this._recomLeft = 9;
         this._recomHave -= 3;
      }

      if (this._recomHave < 0) {
         this._recomHave = 0;
      }

      Calendar var10 = Calendar.getInstance();
      if (var10.get(11) < 13) {
         var10.add(5, -1);
      }

      var10.set(11, 13);
      this._lastRecomUpdate = var10.getTimeInMillis();
   }

   public void setExpBeforeDeath(long var1) {
      this._expBeforeDeath = var1;
   }

   public long getExpBeforeDeath() {
      return this._expBeforeDeath;
   }

   public void setKarma(int var1) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_KARMA_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerKarmaChanged(this, this.getKarma(), var1), this);
      }

      int var2 = var1;
      if (var1 < 0) {
         var2 = 0;
      }

      if (this.getKarma() == 0 && var2 > 0) {
         World.forEachVisibleObject(this, Guard.class, var0 -> {
            if (var0.getAI().getIntention() == Intention.IDLE) {
               var0.getAI().setIntentionActive();
            }
         });
      } else if (this.getKarma() > 0 && var2 == 0) {
         this.setKarmaFlag();
      }

      super.setKarma(var2);
      this.broadcastKarma();
   }

   public int getExpertiseArmorPenalty() {
      return this._expertiseArmorPenalty;
   }

   public int getExpertiseWeaponPenalty() {
      return this._expertiseWeaponPenalty;
   }

   public int getExpertisePenaltyBonus() {
      return this._expertisePenaltyBonus;
   }

   public void setExpertisePenaltyBonus(int var1) {
      this._expertisePenaltyBonus = var1;
   }

   public int getWeightPenalty() {
      return this._dietMode ? 0 : this._curWeightPenalty;
   }

   public void refreshOverloaded() {
      int var1 = this.getMaxLoad();
      if (var1 > 0) {
         long var2 = (this.getCurrentLoad() - this.getBonusWeightPenalty()) * 1000 / this.getMaxLoad();
         byte var4;
         if (var2 < 500L || this._dietMode) {
            var4 = 0;
         } else if (var2 < 666L) {
            var4 = 1;
         } else if (var2 < 800L) {
            var4 = 2;
         } else if (var2 < 1000L) {
            var4 = 3;
         } else {
            var4 = 4;
         }

         if (this._curWeightPenalty != var4) {
            this._curWeightPenalty = var4;
            if (var4 > 0 && !this._dietMode) {
               this.addSkill(SkillData.getInstance().getSkill(4270, var4));
               this.setOverloaded(this.getCurrentLoad() > var1);
            } else {
               this.removeSkill(this.getKnownSkill(4270), false, true);
               this.setOverloaded(false);
            }

            this.broadcastUserInfo();
            this.sendPacket(new EtcStatusUpdate(this));
         }
      }
   }

   public void refreshExpertisePenalty() {
      if (PlayerConfig.EXPERTISE_PENALTY) {
         int var1 = this.getExpertiseLevel();
         int var2 = 0;
         int var3 = 0;

         for (Item var5 : this._inventory.getItems()) {
            if (var5 != null && var5.isEquipped() && var5.getItemType() != EtcItemType.ARROW && var5.getItemType() != EtcItemType.BOLT) {
               int var6 = var5.getTemplate().getCrystalType().getLevel();
               if (var6 > var1) {
                  if (var5.isWeapon() && var6 > var3) {
                     var3 = var6;
                  } else if (var6 > var2) {
                     var2 = var6;
                  }
               }
            }
         }

         boolean var11 = false;
         var3 = var3 - var1 - this._expertisePenaltyBonus;
         var3 = Math.clamp((long)var3, 0, 4);
         if (this._expertiseWeaponPenalty != var3 || this.getSkillLevel(CommonSkill.WEAPON_GRADE_PENALTY.getId()) != var3) {
            this._expertiseWeaponPenalty = var3;
            if (this._expertiseWeaponPenalty > 0) {
               this.addSkill(SkillData.getInstance().getSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId(), this._expertiseWeaponPenalty));
            } else {
               this.removeSkill(this.getKnownSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId()), false, true);
            }

            var11 = true;
         }

         var2 = var2 - var1 - this._expertisePenaltyBonus;
         var2 = Math.clamp((long)var2, 0, 4);
         if (this._expertiseArmorPenalty != var2 || this.getSkillLevel(CommonSkill.ARMOR_GRADE_PENALTY.getId()) != var2) {
            this._expertiseArmorPenalty = var2;
            if (this._expertiseArmorPenalty > 0) {
               this.addSkill(SkillData.getInstance().getSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId(), this._expertiseArmorPenalty));
            } else {
               this.removeSkill(this.getKnownSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId()), false, true);
            }

            var11 = true;
         }

         if (var11) {
            this.sendPacket(new EtcStatusUpdate(this));
         }
      }
   }

   public void useEquippableItem(Item var1, boolean var2) {
      if (var1 != null && var1.getOwnerId() == this.getObjectId()) {
         ItemLocation var3 = var1.getItemLocation();
         if (var3 == ItemLocation.INVENTORY || var3 == ItemLocation.PAPERDOLL) {
            Object var4 = null;
            boolean var5 = var1.isEquipped();
            int var6 = this.getInventoryLimit();
            SystemMessage var7 = null;
            if (var5) {
               BodyPart var12 = BodyPart.fromItem(var1);
               if (var1.isEnchanted()) {
                  var7 = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                  var7.addInt(var1.getEnchantLevel());
               } else {
                  var7 = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
               }

               var7.addItemName(var1);
               this.sendPacket(var7);
               if (var12 == BodyPart.DECO) {
                  var4 = this._inventory.unEquipItemInSlotAndRecord(var1.getLocationSlot());
               } else {
                  var4 = this._inventory.unEquipItemInBodySlotAndRecord(var12);
               }
            } else {
               var4 = this._inventory.equipItemAndRecord(var1);
               if (var1.isEquipped()) {
                  BodyPart var8 = var1.getTemplate().getBodyPart();
                  if (var1.isEnchanted()) {
                     var7 = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
                     var7.addInt(var1.getEnchantLevel());
                  } else {
                     var7 = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
                  }

                  var7.addItemName(var1);
                  this.sendPacket(var7);
                  var1.decreaseMana(false);
                  if (var8 == BodyPart.R_HAND || var8 == BodyPart.LR_HAND) {
                     this.rechargeShots(true, true);
                  }

                  if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_EQUIP, var1.getTemplate())) {
                     EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemEquip(this, var1), var1.getTemplate());
                  }
               } else {
                  this.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
               }
            }

            this.refreshExpertisePenalty();
            this.broadcastUserInfo();
            InventoryUpdate var13 = new InventoryUpdate();
            var13.addItems((Collection)var4);
            this.sendInventoryUpdate(var13);
            if (var2) {
               this.abortAttack();
            }

            if (this.getInventoryLimit() != var6) {
               this.sendPacket(new ExStorageMaxCount(this));
            }
         }
      }
   }

   public int getPvpKills() {
      return this._pvpKills;
   }

   public void setPvpKills(int var1) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPChanged(this, this._pvpKills, var1), this);
      }

      this._pvpKills = var1;
   }

   public int getFame() {
      return this._fame;
   }

   public void setFame(int var1) {
      int var2 = var1;
      if (var1 > PlayerConfig.MAX_PERSONAL_FAME_POINTS) {
         var2 = PlayerConfig.MAX_PERSONAL_FAME_POINTS;
      } else if (var1 < 0) {
         var2 = 0;
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_FAME_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFameChanged(this, this._fame, var2), this);
      }

      this._fame = var2;
   }

   public PlayerClass getPlayerClass() {
      return this.getTemplate().getPlayerClass();
   }

   public void setPlayerClass(int var1) {
      if (!this._subclassLock) {
         this._subclassLock = true;

         try {
            if (this._lvlJoinedAcademy != 0 && this._clan != null && CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, var1)) {
               if (this._lvlJoinedAcademy <= 16) {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE);
               } else if (this._lvlJoinedAcademy >= 39) {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MIN_REP_SCORE);
               } else {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE - (this._lvlJoinedAcademy - 16) * 20);
               }

               this.setLvlJoinedAcademy(0);
               SystemMessage var2 = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED);
               var2.addPcName(this);
               this._clan.broadcastToOnlineMembers(var2);
               this._clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(this.getName()));
               this._clan.removeClanMember(this.getObjectId(), 0L);
               this.sendPacket(
                  SystemMessageId.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES
               );
               this._inventory.addItem(ItemProcessType.REWARD, 8181, 1L, this, null);
            }

            if (this.isSubClassActive()) {
               this.getSubClasses().get(this._classIndex).setPlayerClass(var1);
            }

            this.setTarget(this);
            this.broadcastSkillPacket(new MagicSkillUse(this, 5103, 1, 0, 0), this);
            this.setClassTemplate(var1);
            if (this.getPlayerClass().level() == 3) {
               this.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_THE_THIRD_CLASS_TRANSFER_QUEST);
            } else {
               this.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
            }

            for (int var7 = 1; var7 < 4; var7++) {
               Henna var3 = this.getHenna(var7);
               if (var3 != null && !var3.isAllowedClass(this.getPlayerClass())) {
                  this.removeHenna(var7);
               }
            }

            if (this.isInParty()) {
               this._party.broadcastPacket(new PartySmallWindowUpdate(this));
            }

            if (this._clan != null) {
               this._clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
            }

            this.rewardSkills();
            if (!this.isGM() && PlayerConfig.DECREASE_SKILL_LEVEL) {
               this.checkPlayerSkills();
            }
         } finally {
            this._subclassLock = false;
            ThreadPool.schedule(() -> {
               this.getInventory().applyItemSkills();
               this.sendSkillList();
            }, 100L);
         }
      }
   }

   public boolean isChangingClass() {
      return this._subclassLock;
   }

   public PlayerClass getLearningClass() {
      return this._learningClass;
   }

   public void setLearningClass(PlayerClass var1) {
      this._learningClass = var1;
   }

   public long getExp() {
      return this.getStat().getExp();
   }

   public void setActiveEnchantAttrItemId(int var1) {
      this._activeEnchantAttrItemId = var1;
   }

   public int getActiveEnchantAttrItemId() {
      return this._activeEnchantAttrItemId;
   }

   public void setActiveEnchantItemId(int var1) {
      if (var1 == -1) {
         this.setActiveEnchantSupportItemId(-1);
         this.setActiveEnchantTimestamp(0L);
         this.setEnchanting(false);
      }

      this._activeEnchantItemId = var1;
   }

   public int getActiveEnchantItemId() {
      return this._activeEnchantItemId;
   }

   public void setActiveEnchantSupportItemId(int var1) {
      this._activeEnchantSupportItemId = var1;
   }

   public int getActiveEnchantSupportItemId() {
      return this._activeEnchantSupportItemId;
   }

   public long getActiveEnchantTimestamp() {
      return this._activeEnchantTimestamp;
   }

   public void setActiveEnchantTimestamp(long var1) {
      this._activeEnchantTimestamp = var1;
   }

   public void setEnchanting(boolean var1) {
      this._isEnchanting = var1;
   }

   public boolean isEnchanting() {
      return this._isEnchanting;
   }

   public void setFistsWeaponItem(Weapon var1) {
      this._fistsWeaponItem = var1;
   }

   public Weapon getFistsWeaponItem() {
      return this._fistsWeaponItem;
   }

   public Weapon findFistsWeaponItem(int var1) {
      Weapon var2;
      if (var1 >= 0 && var1 <= 9) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(246);
      } else if (var1 >= 10 && var1 <= 17) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(251);
      } else if (var1 >= 18 && var1 <= 24) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(244);
      } else if (var1 >= 25 && var1 <= 30) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(249);
      } else if (var1 >= 31 && var1 <= 37) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(245);
      } else if (var1 >= 38 && var1 <= 43) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(250);
      } else if (var1 >= 44 && var1 <= 48) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(248);
      } else if (var1 >= 49 && var1 <= 52) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(252);
      } else if (var1 >= 53 && var1 <= 57) {
         var2 = (Weapon)ItemData.getInstance().getTemplate(247);
      } else {
         var2 = (Weapon)ItemData.getInstance().getTemplate(246);
      }

      return var2;
   }

   public void rewardSkills() {
      if (PlayerConfig.AUTO_LEARN_SKILLS) {
         this.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
      } else {
         this.giveAvailableAutoGetSkills();
      }

      if (PlayerConfig.DECREASE_SKILL_LEVEL && !this.isGM()) {
         this.checkPlayerSkills();
      }

      if (FreeMountsConfig.ENABLE_FREE_STRIDER) {
         Skill var1 = SkillData.getInstance().getSkill(49991, 1);
         if (var1 != null) {
            this.addSkill(var1, false);
         }
      }

      if (FreeMountsConfig.ENABLE_FREE_WYVERN) {
         Skill var2 = SkillData.getInstance().getSkill(49992, 1);
         if (var2 != null) {
            this.addSkill(var2, false);
         }
      }

      if (RebirthConfig.REBIRTH_ALLOW_REBIRTH) {
         RebirthManager.getInstance().grantRebirthSkills(this);
      }

      this.checkItemRestriction();
      this.sendSkillList();
   }

   public void regiveTemporarySkills() {
      if (this._noble) {
         this.setNoble(true);
      }

      if (this._hero) {
         this.setHero(true);
      }

      if (this._clan != null) {
         this._clan.addSkillEffects(this);
         if (this._clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel() && this.isClanLeader()) {
            SiegeManager.getInstance().addSiegeSkills(this);
         }

         if (this._clan.getCastleId() > 0) {
            Castle var1 = CastleManager.getInstance().getCastleByOwner(this._clan);
            if (var1 != null) {
               var1.giveResidentialSkills(this);
            }
         }

         if (this._clan.getFortId() > 0) {
            Fort var2 = FortManager.getInstance().getFortByOwner(this._clan);
            if (var2 != null) {
               var2.giveResidentialSkills(this);
            }
         }
      }

      if (RebirthConfig.REBIRTH_ALLOW_REBIRTH) {
         RebirthManager.getInstance().grantRebirthSkills(this);
      }

      this.getInventory().reloadEquippedItems();
      this.restoreDeathPenaltyBuffLevel();
   }

   public int giveAvailableSkills(boolean var1, boolean var2, boolean var3) {
      int var4 = 0;
      Collection var5 = SkillTreeData.getInstance().getAllAvailableSkills(this, this.getPlayerClass(), var1, var2, var3);
      ArrayList var6 = new ArrayList();

      for (Skill var8 : var5) {
         if (this.getKnownSkill(var8.getId()) != var8) {
            if (this.getSkillLevel(var8.getId()) == 0) {
               var4++;
            }

            if (var8.isToggle() && this.isAffectedBySkill(var8.getId())) {
               this.stopSkillEffects(SkillFinishType.REMOVED, var8.getId());
            }

            this.addSkill(var8, false);
            var6.add(var8);
            if (PlayerConfig.AUTO_LEARN_SKILLS) {
               this.updateShortcuts(var8.getId(), var8.getLevel());
            }
         }
      }

      this.storeSkills(var6, -1);
      if (PlayerConfig.AUTO_LEARN_SKILLS && var4 > 0) {
         this.sendPacket(new ShortcutInit(this));
         this.sendMessage("You have learned " + var4 + " new skills.");
      }

      return var4;
   }

   public void giveAvailableAutoGetSkills() {
      List var1 = SkillTreeData.getInstance().getAvailableAutoGetSkills(this);
      SkillData var2 = SkillData.getInstance();

      for (SkillLearn var4 : var1) {
         Skill var5 = var2.getSkill(var4.getSkillId(), var4.getSkillLevel());
         if (var5 != null) {
            this.addSkill(var5, true);
         } else {
            LOGGER.warning("Skipping null auto-get skill for " + this);
         }
      }
   }

   public void setExp(long var1) {
      this.getStat().setExp(Math.max(0L, var1));
   }

   public Race getRace() {
      return !this.isSubClassActive() ? this.getTemplate().getRace() : PlayerTemplateData.getInstance().getTemplate(this._baseClass).getRace();
   }

   public Radar getRadar() {
      return this._radar;
   }

   public boolean isMinimapAllowed() {
      return this._minimapAllowed;
   }

   public void setMinimapAllowed(boolean var1) {
      this._minimapAllowed = var1;
   }

   public long getSp() {
      return this.getStat().getSp();
   }

   public void setSp(long var1) {
      super.getStat().setSp(Math.max(0L, var1));
   }

   public boolean isCastleLord(int var1) {
      if (this._clan != null && this._clan.getLeader().getPlayer() == this) {
         Castle var2 = CastleManager.getInstance().getCastleByOwner(this._clan);
         if (var2 != null && var2 == CastleManager.getInstance().getCastleById(var1)) {
            return true;
         }
      }

      return false;
   }

   public int getClanId() {
      return this._clanId;
   }

   public int getClanCrestId() {
      return this._clan != null ? this._clan.getCrestId() : 0;
   }

   public int getClanCrestLargeId() {
      return this._clan != null && (this._clan.getCastleId() != 0 || this._clan.getHideoutId() != 0) ? this._clan.getCrestLargeId() : 0;
   }

   public long getClanJoinExpiryTime() {
      return this._clanJoinExpiryTime;
   }

   public void setClanJoinExpiryTime(long var1) {
      this._clanJoinExpiryTime = var1;
   }

   public long getClanCreateExpiryTime() {
      return this._clanCreateExpiryTime;
   }

   public void setClanCreateExpiryTime(long var1) {
      this._clanCreateExpiryTime = var1;
   }

   public void setOnlineTime(long var1) {
      this._onlineTime = var1;
      this._onlineBeginTime = System.currentTimeMillis();
   }

   public int getOnlineTimeMillis() {
      return (int)(System.currentTimeMillis() - this._onlineBeginTime);
   }

   public PlayerInventory getInventory() {
      return this._inventory;
   }

   public void removeItemFromShortcut(int var1) {
      this._shortcuts.deleteShortcutByObjectId(var1);
   }

   public boolean isSitting() {
      return this._waitTypeSitting;
   }

   public void setSitting(boolean var1) {
      if (var1 && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SIT, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSit(this), this);
      }

      this._waitTypeSitting = var1;
   }

   public void setSittingProgress(boolean var1) {
      this._sittingInProgress = var1;
   }

   public void sitDown() {
      this.sitDown(true);
   }

   public void sitDown(boolean var1) {
      if (!this._sittingInProgress) {
         if (var1 && this.isCastingNow()) {
            this.sendMessage("Cannot sit while casting");
         } else if (!this._waitTypeSitting && !this.isAttackDisabled() && !this.isOutOfControl() && !this.isImmobilized()) {
            this.breakAttack();
            this.setSitting(true);
            this.setSittingProgress(true);
            this.getAI().setIntentionRest();
            this.broadcastPacket(new ChangeWaitType(this, 0));
            ThreadPool.schedule(new SitDownTask(this), 2500L);
         }
      }
   }

   public void standUp() {
      if (!this._sittingInProgress && this._waitTypeSitting && !this.isInStoreMode() && !this.isAlikeDead()) {
         this.setSittingProgress(true);
         if (this.getEffectList().isAffected(EffectFlag.RELAXING)) {
            this.stopEffects(EffectType.RELAXING);
         }

         this.broadcastPacket(new ChangeWaitType(this, 1));
         ThreadPool.schedule(new StandUpTask(this), 2500L);
      }
   }

   public PlayerWarehouse getWarehouse() {
      return this._warehouse;
   }

   public PlayerFreight getFreight() {
      return this._freight;
   }

   public boolean hasRefund() {
      return this._refund != null && this._refund.getSize() > 0 && GeneralConfig.ALLOW_REFUND;
   }

   public PlayerRefund getRefund() {
      if (this._refund == null) {
         this._refund = new PlayerRefund(this);
      }

      return this._refund;
   }

   public void clearRefund() {
      if (this._refund != null) {
         this._refund.deleteMe();
      }

      this._refund = null;
   }

   public long getAdena() {
      return this._inventory.getAdena();
   }

   public long getAncientAdena() {
      return this._inventory.getAncientAdena();
   }

   public void addAdena(ItemProcessType var1, long var2, WorldObject var4, boolean var5) {
      long var6 = this._inventory.getAdena();
      long var8 = PlayerConfig.MAX_ADENA - var6;
      long var10 = var2;
      if (var2 > var8) {
         var2 = var8;
      }

      if (var5) {
         if (var2 == 0L && var10 > 0L) {
            SystemMessage var13 = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_EARN_S1_ADENA);
            var13.addLong(var10);
            this.sendPacket(var13);
         } else {
            SystemMessage var12 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
            var12.addLong(var2);
            this.sendPacket(var12);
         }
      }

      if (var2 > 0L) {
         this._inventory.addAdena(var1, var2, this, var4);
         if (var2 == this.getAdena()) {
            this.sendItemList(false);
         } else {
            InventoryUpdate var14 = new InventoryUpdate();
            var14.addModifiedItem(this._inventory.getAdenaInstance());
            this.sendInventoryUpdate(var14);
         }
      }

      if (this._inventory.getAdena() == PlayerConfig.MAX_ADENA && var5) {
         this.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
      }
   }

   public boolean reduceAdena(ItemProcessType var1, long var2, WorldObject var4, boolean var5) {
      if (var2 > this._inventory.getAdena()) {
         if (var5) {
            this.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
         }

         return false;
      } else {
         if (var2 > 0L) {
            if (!this._inventory.reduceAdena(var1, var2, this, var4)) {
               return false;
            }

            Item var6 = this._inventory.getAdenaInstance();
            InventoryUpdate var7 = new InventoryUpdate();
            var7.addItem(var6);
            this.sendInventoryUpdate(var7);
            if (var5) {
               SystemMessage var8 = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
               var8.addLong(var2);
               this.sendPacket(var8);
            }
         }

         return true;
      }
   }

   public void addAncientAdena(ItemProcessType var1, long var2, WorldObject var4, boolean var5) {
      if (var5) {
         SystemMessage var6 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
         var6.addItemName(5575);
         var6.addLong(var2);
         this.sendPacket(var6);
      }

      if (var2 > 0L) {
         this._inventory.addAncientAdena(var1, var2, this, var4);
         InventoryUpdate var7 = new InventoryUpdate();
         var7.addItem(this._inventory.getAncientAdenaInstance());
         this.sendInventoryUpdate(var7);
      }
   }

   public boolean reduceAncientAdena(ItemProcessType var1, long var2, WorldObject var4, boolean var5) {
      if (var2 > this._inventory.getAncientAdena()) {
         if (var5) {
            this.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
         }

         return false;
      } else {
         if (var2 > 0L) {
            if (!this._inventory.reduceAncientAdena(var1, var2, this, var4)) {
               return false;
            }

            Item var6 = this._inventory.getAncientAdenaInstance();
            InventoryUpdate var7 = new InventoryUpdate();
            var7.addItem(var6);
            this.sendInventoryUpdate(var7);
            if (var5) {
               SystemMessage var8;
               if (var2 > 1L) {
                  var8 = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                  var8.addItemName(5575);
                  var8.addLong(var2);
               } else {
                  var8 = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  var8.addItemName(5575);
               }

               this.sendPacket(var8);
            }
         }

         return true;
      }
   }

   public void addItem(ItemProcessType var1, Item var2, WorldObject var3, boolean var4) {
      if (var2.getCount() > 0L) {
         if (var4) {
            SystemMessage var5;
            if (var2.getCount() > 1L) {
               var5 = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
               var5.addItemName(var2);
               var5.addLong(var2.getCount());
            } else if (var2.isEnchanted()) {
               var5 = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_A_S1_S2);
               var5.addInt(var2.getEnchantLevel());
               var5.addItemName(var2);
            } else {
               var5 = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
               var5.addItemName(var2);
            }

            this.sendPacket(var5);
         }

         Item var9 = this._inventory.addItem(var1, var2, this, var3);
         InventoryUpdate var6 = new InventoryUpdate();
         var6.addItem(var9);
         this.sendInventoryUpdate(var6);
         StatusUpdate var7 = new StatusUpdate(this);
         var7.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(var7);
         if (this.isGM() || this._inventory.validateCapacity(0L, var2.isQuestItem()) || !var9.isDropable() || var9.isStackable() && var9.getLastChange() == 2) {
            if (CursedWeaponsManager.getInstance().isCursed(var9.getId())) {
               CursedWeaponsManager.getInstance().activate(this, var9);
            } else if (FortSiegeManager.getInstance().isCombat(var2.getId())) {
               if (FortSiegeManager.getInstance().activateCombatFlag(this, var2)) {
                  Fort var8 = FortManager.getInstance().getFort(this);
                  var8.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_HAS_ACQUIRED_THE_FLAG), this.getName());
               }
            } else if (var2.getId() >= 13560 && var2.getId() <= 13568) {
               TerritoryWard var10 = TerritoryWarManager.getInstance().getTerritoryWard(var2.getId() - 13479);
               if (var10 != null) {
                  var10.activate(this, var2);
               }
            }
         } else {
            this.dropItem(ItemProcessType.DROP, var9, null, true, true);
         }
      }
   }

   public Item addItem(ItemProcessType var1, int var2, long var3, WorldObject var5, boolean var6) {
      return this.addItem(var1, var2, var3, -1, var5, var6);
   }

   public Item addItem(ItemProcessType var1, int var2, long var3, int var5, WorldObject var6, boolean var7) {
      if (var3 > 0L) {
         ItemTemplate var8 = ItemData.getInstance().getTemplate(var2);
         if (var8 == null) {
            LOGGER.severe("Item doesn't exist so cannot be added. Item ID: " + var2);
            return null;
         }

         if (var7 && (!this.isCastingNow() && var8.hasExImmediateEffect() || !var8.hasExImmediateEffect())) {
            if (var3 > 1L) {
               if (var1 != ItemProcessType.SWEEP && var1 != ItemProcessType.QUEST) {
                  SystemMessage var13 = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
                  var13.addItemName(var2);
                  var13.addLong(var3);
                  this.sendPacket(var13);
               } else {
                  SystemMessage var9 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                  var9.addItemName(var2);
                  var9.addLong(var3);
                  this.sendPacket(var9);
               }
            } else if (var1 != ItemProcessType.SWEEP && var1 != ItemProcessType.QUEST) {
               SystemMessage var15 = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
               var15.addItemName(var2);
               this.sendPacket(var15);
            } else {
               SystemMessage var14 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
               var14.addItemName(var2);
               this.sendPacket(var14);
            }
         }

         if (!var8.hasExImmediateEffect() || !var8.isEtcItem()) {
            Item var17 = this._inventory.addItem(var1, var2, var3, this, var6);
            if (var5 > -1) {
               var17.setEnchantLevel(var5);
               InventoryUpdate var18 = new InventoryUpdate();
               if (var17.isStackable() && var17.getCount() > var3) {
                  var18.addModifiedItem(var17);
               } else {
                  var18.addNewItem(var17);
               }

               this.sendInventoryUpdate(var18);
            }

            if (this.isGM()
               || this._inventory.validateCapacity(0L, var8.isQuestItem())
               || !var17.isDropable()
               || var17.isStackable() && var17.getLastChange() == 2) {
               if (CursedWeaponsManager.getInstance().isCursed(var17.getId())) {
                  CursedWeaponsManager.getInstance().activate(this, var17);
               } else if (var17.getId() >= 13560 && var17.getId() <= 13568) {
                  TerritoryWard var19 = TerritoryWarManager.getInstance().getTerritoryWard(var17.getId() - 13479);
                  if (var19 != null) {
                     var19.activate(this, var17);
                  }
               }
            } else {
               this.dropItem(ItemProcessType.DROP, var17, null, true);
            }

            return var17;
         }

         for (SkillHolder var12 : var8.getSkills()) {
            this.doSimultaneousCast(var12.getSkill());
         }

         this.broadcastInfo();
      }

      return null;
   }

   public void addItem(ItemProcessType var1, ItemHolder var2, WorldObject var3, boolean var4) {
      this.addItem(var1, var2.getId(), var2.getCount(), var3, var4);
   }

   public boolean destroyItem(ItemProcessType var1, Item var2, WorldObject var3, boolean var4) {
      return this.destroyItem(var1, var2, var2.getCount(), var3, var4);
   }

   public boolean destroyItem(ItemProcessType var1, Item var2, long var3, WorldObject var5, boolean var6) {
      Item var7 = this._inventory.destroyItem(var1, var2, var3, this, var5);
      if (var7 == null) {
         if (var6) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         InventoryUpdate var8 = new InventoryUpdate();
         var8.addItem(var7);
         this.sendInventoryUpdate(var8);
         StatusUpdate var9 = new StatusUpdate(this);
         var9.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(var9);
         if (var6) {
            SystemMessage var10;
            if (var3 > 1L) {
               var10 = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
               var10.addItemName(var7);
               var10.addLong(var3);
            } else {
               var10 = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
               var10.addItemName(var7);
            }

            this.sendPacket(var10);
         }

         return true;
      }
   }

   public boolean destroyItem(ItemProcessType var1, int var2, long var3, WorldObject var5, boolean var6) {
      Item var7 = this._inventory.getItemByObjectId(var2);
      if (var7 != null && var7.getCount() >= var3) {
         return this.destroyItem(var1, var7, var3, var5, var6);
      } else {
         if (var6) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      }
   }

   public boolean destroyItemByItemId(ItemProcessType var1, int var2, long var3, WorldObject var5, boolean var6) {
      if (var2 == 57) {
         return this.reduceAdena(var1, var3, var5, var6);
      } else {
         Item var7 = this._inventory.getItemByItemId(var2);
         if (var7 == null) {
            if (var6) {
               this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
            }

            return false;
         } else {
            long var8 = var7.isStackable() ? var7.getCount() : this._inventory.getInventoryItemCount(var2, -1);
            long var10 = var3 < 0L ? var8 : var3;
            if (var10 > 0L && var8 >= var10 && this._inventory.destroyItemByItemId(var1, var2, var10, this, var5) != null) {
               InventoryUpdate var12 = new InventoryUpdate();
               var12.addItem(var7);
               this.sendInventoryUpdate(var12);
               StatusUpdate var13 = new StatusUpdate(this);
               var13.addAttribute(14, this.getCurrentLoad());
               this.sendPacket(var13);
               if (var6) {
                  SystemMessage var14;
                  if (var10 > 1L) {
                     var14 = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                     var14.addItemName(var2);
                     var14.addLong(var10);
                  } else {
                     var14 = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                     var14.addItemName(var2);
                  }

                  this.sendPacket(var14);
               }

               return true;
            } else {
               if (var6) {
                  this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
               }

               return false;
            }
         }
      }
   }

   public Item transferItem(ItemProcessType var1, int var2, long var3, Inventory var5, WorldObject var6) {
      Item var7 = this.checkItemManipulation(var2, var3, "transfer");
      if (var7 == null) {
         return null;
      } else {
         Item var8 = this._inventory.transferItem(var1, var2, var3, var5, this, var6);
         if (var8 == null) {
            return null;
         } else {
            InventoryUpdate var9 = new InventoryUpdate();
            if (var7.getCount() > 0L && var7 != var8) {
               var9.addModifiedItem(var7);
            } else {
               var9.addRemovedItem(var7);
            }

            this.sendInventoryUpdate(var9);
            StatusUpdate var10 = new StatusUpdate(this);
            var10.addAttribute(14, this.getCurrentLoad());
            this.sendPacket(var10);
            if (var5 instanceof PlayerInventory) {
               Player var11 = ((PlayerInventory)var5).getOwner();
               InventoryUpdate var12 = new InventoryUpdate();
               if (var8.getCount() > var3) {
                  var12.addModifiedItem(var8);
               } else {
                  var12.addNewItem(var8);
               }

               var11.sendInventoryUpdate(var12);
               var10 = new StatusUpdate(var11);
               var10.addAttribute(14, var11.getCurrentLoad());
               var11.sendPacket(var10);
            } else if (var5 instanceof PetInventory) {
               PetInventoryUpdate var14 = new PetInventoryUpdate();
               if (var8.getCount() > var3) {
                  var14.addModifiedItem(var8);
               } else {
                  var14.addNewItem(var8);
               }

               ((PetInventory)var5).getOwner().sendPacket(var14);
            }

            return var8;
         }
      }
   }

   public boolean exchangeItemsById(ItemProcessType var1, WorldObject var2, int var3, long var4, int var6, long var7, boolean var9) {
      if (!this._inventory.validateCapacityByItemId(var6, var7)) {
         if (var9) {
            this.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
         }

         return false;
      } else if (!this._inventory.validateWeightByItemId(var6, var7)) {
         if (var9) {
            this.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
         }

         return false;
      } else if (this.destroyItemByItemId(var1, var3, var4, var2, var9)) {
         this.addItem(var1, var6, var7, var2, var9);
         return true;
      } else {
         return false;
      }
   }

   public boolean dropItem(ItemProcessType var1, Item var2, WorldObject var3, boolean var4, boolean var5) {
      Item var6 = this._inventory.dropItem(var1, var2, this, var3);
      if (var6 == null) {
         if (var4) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         var6.dropMe(this, this.getX() + Rnd.get(50) - 25, this.getY() + Rnd.get(50) - 25, this.getZ() + 20);
         if (GeneralConfig.AUTODESTROY_ITEM_AFTER > 0
            && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM
            && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(var6.getId())
            && (var6.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM || !var6.isEquipable())) {
            ItemsAutoDestroyTaskManager.getInstance().addItem(var6);
         }

         if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM) {
            var6.setProtected(var6.isEquipable() && (!var6.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
         } else {
            var6.setProtected(true);
         }

         if (var5) {
            var6.getDropProtection().protect(this);
         }

         InventoryUpdate var7 = new InventoryUpdate();
         var7.addItem(var6);
         this.sendInventoryUpdate(var7);
         StatusUpdate var8 = new StatusUpdate(this);
         var8.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(var8);
         if (var4) {
            SystemMessage var9 = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
            var9.addItemName(var6);
            this.sendPacket(var9);
         }

         return true;
      }
   }

   public boolean dropItem(ItemProcessType var1, Item var2, WorldObject var3, boolean var4) {
      return this.dropItem(var1, var2, var3, var4, false);
   }

   public Item dropItem(ItemProcessType var1, int var2, long var3, int var5, int var6, int var7, WorldObject var8, boolean var9, boolean var10) {
      Item var11 = this._inventory.getItemByObjectId(var2);
      Item var12 = this._inventory.dropItem(var1, var2, var3, this, var8);
      if (var12 == null) {
         if (var9) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return null;
      } else {
         var12.dropMe(this, var5, var6, var7);
         if (GeneralConfig.AUTODESTROY_ITEM_AFTER > 0
            && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM
            && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(var12.getId())
            && (var12.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM || !var12.isEquipable())) {
            ItemsAutoDestroyTaskManager.getInstance().addItem(var12);
         }

         if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM) {
            var12.setProtected(var12.isEquipable() && (!var12.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
         } else {
            var12.setProtected(true);
         }

         if (var10) {
            var12.getDropProtection().protect(this);
         }

         InventoryUpdate var13 = new InventoryUpdate();
         var13.addItem(var11);
         this.sendInventoryUpdate(var13);
         StatusUpdate var14 = new StatusUpdate(this);
         var14.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(var14);
         if (var9) {
            SystemMessage var15 = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
            var15.addItemName(var12);
            this.sendPacket(var15);
         }

         return var12;
      }
   }

   public Item checkItemManipulation(int var1, long var2, String var4) {
      if (World.findObject(var1) == null) {
         LOGGER.finest(this.getObjectId() + ": player tried to " + var4 + " item not available in World");
         return null;
      } else {
         Item var5 = this._inventory.getItemByObjectId(var1);
         if (var5 == null || var5.getOwnerId() != this.getObjectId()) {
            LOGGER.finest(this.getObjectId() + ": player tried to " + var4 + " item he is not owner of");
            return null;
         } else if (var2 >= 0L && (var2 <= 1L || var5.isStackable())) {
            if (var2 > var5.getCount()) {
               LOGGER.finest(this.getObjectId() + ": player tried to " + var4 + " more items than he owns");
               return null;
            } else if ((!this.hasSummon() || this._summon.getControlObjectId() != var1) && this._mountObjectID != var1) {
               if (this._activeEnchantItemId == var1) {
                  return null;
               } else {
                  return var5.isAugmented() && (this.isCastingNow() || this.isCastingSimultaneouslyNow()) ? null : var5;
               }
            } else {
               return null;
            }
         } else {
            LOGGER.finest(this.getObjectId() + ": player tried to " + var4 + " item with invalid count: " + var2);
            return null;
         }
      }
   }

   public boolean isSpawnProtected() {
      return this._spawnProtectEndTime != 0L && this._spawnProtectEndTime > System.currentTimeMillis();
   }

   public boolean isTeleportProtected() {
      return this._teleportProtectEndTime != 0L && this._teleportProtectEndTime > System.currentTimeMillis();
   }

   public void setSpawnProtection(boolean var1) {
      this._spawnProtectEndTime = var1 ? System.currentTimeMillis() + PlayerConfig.PLAYER_SPAWN_PROTECTION * 1000 : 0L;
   }

   public void setTeleportProtection(boolean var1) {
      this._teleportProtectEndTime = var1 ? System.currentTimeMillis() + PlayerConfig.PLAYER_TELEPORT_PROTECTION * 1000 : 0L;
   }

   public void setRecentFakeDeath(boolean var1) {
      this._recentFakeDeathEndTime = var1 ? GameTimeTaskManager.getInstance().getGameTicks() + PlayerConfig.PLAYER_FAKEDEATH_UP_PROTECTION * 10 : 0L;
   }

   public boolean isRecentFakeDeath() {
      return this._recentFakeDeathEndTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public boolean isFakeDeath() {
      return this._isFakeDeath;
   }

   public void setFakeDeath(boolean var1) {
      this._isFakeDeath = var1;
   }

   public boolean isAlikeDead() {
      return super.isAlikeDead() || this._isFakeDeath;
   }

   public GameClient getClient() {
      return this._client;
   }

   public void setClient(GameClient var1) {
      this._client = var1;
      if (this._client != null && this._client.getIp() != null) {
         this._ip = this._client.getIp();
      }
   }

   public String getIPAddress() {
      return this._ip;
   }

   public Location getCurrentSkillWorldPosition() {
      return this._currentSkillWorldPosition;
   }

   public void setCurrentSkillWorldPosition(Location var1) {
      this._currentSkillWorldPosition = var1;
   }

   public void enableSkill(Skill var1) {
      super.enableSkill(var1);
      this.removeTimeStamp(var1);
   }

   public boolean checkDoCastConditions(Skill var1) {
      if (!super.checkDoCastConditions(var1) || this._observerMode) {
         return false;
      } else if (this._inOlympiadMode && var1.isBlockedInOlympiad()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
         return false;
      } else if (this._charges.get() < var1.getChargeConsumeCount() || this.isInAirShip() && !var1.hasEffectType(EffectType.REFUEL_AIRSHIP)) {
         SystemMessage var2 = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
         var2.addSkillName(var1);
         this.sendPacket(var2);
         return false;
      } else {
         return true;
      }
   }

   private boolean needCpUpdate() {
      double var1 = this.getCurrentCp();
      if (var1 <= 1.0 || this.getMaxCp() < 352.0) {
         return true;
      } else if (!(var1 <= this._cpUpdateDecCheck) && !(var1 >= this._cpUpdateIncCheck)) {
         return false;
      } else {
         if (var1 == this.getMaxCp()) {
            this._cpUpdateIncCheck = var1 + 1.0;
            this._cpUpdateDecCheck = var1 - this._cpUpdateInterval;
         } else {
            double var3 = var1 / this._cpUpdateInterval;
            int var5 = (int)var3;
            this._cpUpdateDecCheck = this._cpUpdateInterval * (var3 < var5 ? var5 - 1 : var5);
            this._cpUpdateIncCheck = this._cpUpdateDecCheck + this._cpUpdateInterval;
         }

         return true;
      }
   }

   private boolean needMpUpdate() {
      double var1 = this.getCurrentMp();
      if (var1 <= 1.0 || this.getMaxMp() < 352.0) {
         return true;
      } else if (!(var1 <= this._mpUpdateDecCheck) && !(var1 >= this._mpUpdateIncCheck)) {
         return false;
      } else {
         if (var1 == this.getMaxMp()) {
            this._mpUpdateIncCheck = var1 + 1.0;
            this._mpUpdateDecCheck = var1 - this._mpUpdateInterval;
         } else {
            double var3 = var1 / this._mpUpdateInterval;
            int var5 = (int)var3;
            this._mpUpdateDecCheck = this._mpUpdateInterval * (var3 < var5 ? var5 - 1 : var5);
            this._mpUpdateIncCheck = this._mpUpdateDecCheck + this._mpUpdateInterval;
         }

         return true;
      }
   }

   public void broadcastStatusUpdate() {
      if (this._broadcastStatusUpdateTask == null) {
         this._broadcastStatusUpdateTask = ThreadPool.schedule(() -> {
            StatusUpdate var1 = new StatusUpdate(this);
            var1.addAttribute(10, this.getMaxHp());
            var1.addAttribute(9, (int)this.getCurrentHp());
            var1.addAttribute(12, this.getMaxMp());
            var1.addAttribute(11, (int)this.getCurrentMp());
            var1.addAttribute(34, this.getMaxCp());
            var1.addAttribute(33, (int)this.getCurrentCp());
            this.sendPacket(var1);
            boolean var2 = this.needCpUpdate();
            boolean var3 = this.needHpUpdate();
            Party var4 = this.getParty();
            if (var4 != null && (var2 || var3 || this.needMpUpdate())) {
               var4.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
            }

            if (this._inOlympiadMode && this._olympiadStart && (var2 || var3)) {
               ExOlympiadUserInfo var5 = new ExOlympiadUserInfo(this, this.getOlympiadSide());
               World.forEachVisibleObject(this, Player.class, var2x -> {
                  if (var2x != null && var2x.isInOlympiadMode() && var2x.getOlympiadGameId() == this._olympiadGameId) {
                     var2x.sendPacket(var5);
                  }
               });
               List var6 = Olympiad.getInstance().getSpectators(this._olympiadGameId);
               if (var6 != null && !var6.isEmpty()) {
                  ExOlympiadUserInfo var7 = new ExOlympiadUserInfo(this, this.getOlympiadSide());

                  for (Player var9 : var6) {
                     if (var9 != null) {
                        var9.sendPacket(var7);
                     }
                  }
               }
            }

            if (this._isInDuel && (var2 || var3)) {
               DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
            }

            this._broadcastStatusUpdateTask = null;
         }, 50L);
      }
   }

   public void broadcastUserInfo() {
      this.updateUserInfo();
      this.broadcastCharInfo();
   }

   public void updateUserInfo() {
      this.sendPacket(new UserInfo(this));
      this.sendPacket(new ExBrExtraUserInfo(this));
   }

   public void broadcastCharInfo() {
      if (this.isOnline() && this._broadcastCharInfoTask == null) {
         this._broadcastCharInfoTask = ThreadPool.schedule(() -> {
            CharInfo var1 = new CharInfo(this, false);
            var1.sendInBroadcast();
            World.forEachVisibleObject(this, Player.class, var2 -> {
               if (this.isVisibleFor(var2)) {
                  if (this.isInvisible() && this.isGM()) {
                     var2.sendPacket(new CharInfo(this, true));
                  } else {
                     var2.sendPacket(var1);
                  }

                  var2.sendPacket(new ExBrExtraUserInfo(this));
                  int var3 = this.getRelation(var2);
                  boolean var4 = this.isAutoAttackable(var2);
                  RelationCache var5 = (RelationCache)this.getKnownRelations().get(var2.getObjectId());
                  if (var5 == null || var5.getRelation() != var3 || var5.isAutoAttackable() != var4) {
                     var2.sendPacket(new RelationChanged(this, var3, var4));
                     if (this.hasSummon()) {
                        var2.sendPacket(new RelationChanged(this._summon, var3, var4));
                     }

                     this.getKnownRelations().put(var2.getObjectId(), new RelationCache(var3, var4));
                  }
               }
            });
            this._broadcastCharInfoTask = null;
         }, 100L);
      }
   }

   public void broadcastTitleInfo() {
      this.updateUserInfo();
      this.broadcastPacket(new NicknameChanged(this));
   }

   public void broadcastPacket(ServerPacket var1) {
      var1.sendInBroadcast();
      boolean var2 = var1 instanceof CharInfo;
      if (!var2) {
         this.sendPacket(var1);
      }

      World.forEachVisibleObject(this, Player.class, var3 -> {
         if (this.isVisibleFor(var3)) {
            var3.sendPacket(var1);
            if (var2) {
               int var4 = this.getRelation(var3);
               boolean var5 = this.isAutoAttackable(var3);
               RelationCache var6 = (RelationCache)this.getKnownRelations().get(var3.getObjectId());
               if (var6 == null || var6.getRelation() != var4 || var6.isAutoAttackable() != var5) {
                  var3.sendPacket(new RelationChanged(this, var4, var5));
                  if (this.hasSummon()) {
                     var3.sendPacket(new RelationChanged(this._summon, var4, var5));
                  }

                  this.getKnownRelations().put(var3.getObjectId(), new RelationCache(var4, var5));
               }
            }
         }
      });
   }

   public int getAllyId() {
      return this._clan == null ? 0 : this._clan.getAllyId();
   }

   public int getAllyCrestId() {
      return this.getAllyId() == 0 ? 0 : this._clan.getAllyCrestId();
   }

   public void sendPacket(ServerPacket var1) {
      if (this._client != null) {
         this._client.sendPacket(var1);
      }
   }

   public void sendPacket(SystemMessageId var1) {
      this.sendPacket(new SystemMessage(var1));
   }

   public void doInteract(Creature var1) {
      if (var1 != null) {
         if (var1.isPlayer()) {
            Player var2 = var1.asPlayer();
            this.sendPacket(ActionFailed.STATIC_PACKET);
            if (var2.getPrivateStoreType() == PrivateStoreType.SELL || var2.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL) {
               this.sendPacket(new PrivateStoreListSell(this, var2));
            } else if (var2.getPrivateStoreType() == PrivateStoreType.BUY) {
               this.sendPacket(new PrivateStoreListBuy(this, var2));
            } else if (var2.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
               this.sendPacket(new RecipeShopSellList(this, var2));
            }
         } else {
            var1.onAction(this);
         }
      }
   }

   public void doAutoLoot(Attackable var1, int var2, long var3) {
      if (this.isInParty() && !ItemData.getInstance().getTemplate(var2).hasExImmediateEffect()) {
         this._party.distributeItem(this, var2, var3, false, var1);
      } else if (var2 == 57) {
         this.addAdena(ItemProcessType.LOOT, var3, var1, true);
      } else {
         this.addItem(ItemProcessType.LOOT, var2, var3, var1, true);
      }
   }

   public void doAutoLoot(Attackable var1, ItemHolder var2) {
      this.doAutoLoot(var1, var2.getId(), var2.getCount());
   }

   public void doPickupItem(WorldObject var1) {
      if (!this.isAlikeDead() && !this._isFakeDeath) {
         if (this.getActiveTradeList() != null) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING);
            this.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            this.getAI().setIntentionIdle();
            if (!var1.isItem()) {
               LOGGER.warning(this + " trying to pickup wrong target." + this.getTarget());
            } else {
               Item var2 = (Item)var1;
               this.sendPacket(new StopMove(this));
               SystemMessage var3 = null;
               synchronized (var2) {
                  if (!var2.isSpawned()) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (!var2.getDropProtection().tryPickUp(this)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     var3 = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                     var3.addItemName(var2);
                     this.sendPacket(var3);
                     return;
                  }

                  if ((this.isInParty() && this._party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS || !this.isInParty())
                     && !this._inventory.validateCapacity(var2)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
                     return;
                  }

                  if (this.isInvisible() && !this.isGM()) {
                     return;
                  }

                  if (var2.getOwnerId() != 0 && var2.getOwnerId() != this.getObjectId() && !this.isInLooterParty(var2.getOwnerId())) {
                     if (var2.getId() == 57) {
                        var3 = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
                        var3.addLong(var2.getCount());
                     } else if (var2.getCount() > 1L) {
                        var3 = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
                        var3.addItemName(var2);
                        var3.addLong(var2.getCount());
                     } else {
                        var3 = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                        var3.addItemName(var2);
                     }

                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(var3);
                     return;
                  }

                  if (FortSiegeManager.getInstance().isCombat(var2.getId()) && !FortSiegeManager.getInstance().checkIfCanPickup(this)) {
                     return;
                  }

                  if (var2.getItemLootShedule() != null && (var2.getOwnerId() == this.getObjectId() || this.isInLooterParty(var2.getOwnerId()))) {
                     var2.resetOwnerTimer();
                  }

                  var2.pickupMe(this);
                  if (GeneralConfig.SAVE_DROPPED_ITEM) {
                     ItemsOnGroundManager.getInstance().removeObject(var2);
                  }
               }

               if (var2.getTemplate().hasExImmediateEffect()) {
                  IItemHandler var4 = ItemHandler.getInstance().getHandler(var2.getEtcItem());
                  if (var4 != null) {
                     var4.onItemUse(this, var2, false);
                  } else {
                     LOGGER.warning("No item handler registered for item ID: " + var2.getId() + ".");
                  }

                  ItemManager.destroyItem(ItemProcessType.NONE, var2, this, null);
               } else if (CursedWeaponsManager.getInstance().isCursed(var2.getId())) {
                  this.addItem(ItemProcessType.PICKUP, var2, null, true);
               } else if (FortSiegeManager.getInstance().isCombat(var2.getId())) {
                  this.addItem(ItemProcessType.PICKUP, var2, null, true);
               } else {
                  if (var2.getItemType() instanceof ArmorType || var2.getItemType() instanceof WeaponType) {
                     if (var2.isEnchanted()) {
                        var3 = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_HAS_PICKED_UP_S2_S3);
                        var3.addPcName(this);
                        var3.addInt(var2.getEnchantLevel());
                     } else {
                        var3 = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_HAS_PICKED_UP_S2);
                        var3.addPcName(this);
                     }

                     var3.addItemName(var2.getId());
                     this.broadcastPacket(var3);
                  }

                  if (this.isInParty()) {
                     this._party.distributeItem(this, var2);
                  } else if (var2.getId() == 57 && this._inventory.getAdenaInstance() != null) {
                     this.addAdena(ItemProcessType.PICKUP, var2.getCount(), null, true);
                     ItemManager.destroyItem(ItemProcessType.PICKUP, var2, this, null);
                  } else {
                     this.addItem(ItemProcessType.PICKUP, var2, null, true);
                     Item var11 = this._inventory.getPaperdollItem(5);
                     if (var11 != null) {
                        EtcItem var5 = var2.getEtcItem();
                        if (var5 != null) {
                           EtcItemType var6 = var5.getItemType();
                           if (var11.getItemType() == WeaponType.BOW && var6 == EtcItemType.ARROW) {
                              this.checkAndEquipArrows();
                           } else if (var11.getItemType() == WeaponType.CROSSBOW && var6 == EtcItemType.BOLT) {
                              this.checkAndEquipBolts();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void doAttack(Creature var1) {
      super.doAttack(var1);
      this.setRecentFakeDeath(false);
      if (var1.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE) {
         this.updatePvPStatus();
      }
   }

   public void doCast(Skill var1) {
      if (this._currentSkill != null && !this.checkUseMagicConditions(var1, this._currentSkill.isCtrlPressed(), this._currentSkill.isShiftPressed())) {
         this.setCastingNow(false);
         this.setCastingSimultaneouslyNow(false);
      } else {
         super.doCast(var1);
         this.setRecentFakeDeath(false);
      }
   }

   public boolean canOpenPrivateStore() {
      if ((PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC > 0 || PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER > 0)
         && World.getFirstVisibleObject(
               this, Creature.class, var1 -> var1.getMinShopDistance() > 0 && LocationUtil.checkIfInRange(var1.getMinShopDistance(), this, var1, true)
            )
            != null) {
         this.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE));
         return false;
      } else {
         return !this.isAlikeDead() && !this._inOlympiadMode && !this.isMounted() && !this.isInsideZone(ZoneId.NO_STORE) && !this.isCastingNow();
      }
   }

   public int getMinShopDistance() {
      return this._waitTypeSitting ? PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER : 0;
   }

   public void tryOpenPrivateBuyStore() {
      if (this.canOpenPrivateStore()) {
         if (this._privateStoreType == PrivateStoreType.BUY || this._privateStoreType == PrivateStoreType.BUY_MANAGE) {
            this.setPrivateStoreType(PrivateStoreType.NONE);
         }

         if (this._privateStoreType == PrivateStoreType.NONE) {
            if (this._waitTypeSitting) {
               this.standUp();
            }

            this.setPrivateStoreType(PrivateStoreType.BUY_MANAGE);
            this.sendPacket(new PrivateStoreManageListBuy(this));
         }
      } else {
         if (this.isInsideZone(ZoneId.NO_STORE)) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
         }

         this.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public void tryOpenPrivateSellStore(boolean var1) {
      if (this.canOpenPrivateStore()) {
         if (this._privateStoreType == PrivateStoreType.SELL
            || this._privateStoreType == PrivateStoreType.SELL_MANAGE
            || this._privateStoreType == PrivateStoreType.PACKAGE_SELL) {
            this.setPrivateStoreType(PrivateStoreType.NONE);
         }

         if (this._privateStoreType == PrivateStoreType.NONE) {
            if (this._waitTypeSitting) {
               this.standUp();
            }

            this.setPrivateStoreType(PrivateStoreType.SELL_MANAGE);
            this.sendPacket(new PrivateStoreManageListSell(this, var1));
         }
      } else {
         if (this._privateStoreType != PrivateStoreType.NONE && !this.isAlikeDead()) {
            this.setPrivateStoreType(PrivateStoreType.NONE);
            if (this._waitTypeSitting) {
               this.standUp();
            }

            return;
         }

         if (this.isInsideZone(ZoneId.NO_STORE)) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
         }

         this.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   public PreparedListContainer getMultiSell() {
      return this._currentMultiSell;
   }

   public void setMultiSell(PreparedListContainer var1) {
      this._currentMultiSell = var1;
   }

   public boolean isTransformed() {
      return this._transformation != null && !this._transformation.isStance();
   }

   public boolean isInStance() {
      return this._transformation != null && this._transformation.isStance();
   }

   public void transform(Transform var1) {
      if ((!var1.isFlying() || this.getX() <= -166168) && (FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE || !var1.isRiding() || !this.isInsideZone(ZoneId.SIEGE))) {
         if (this._transformation != null) {
            this.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN));
         } else {
            this.setQueuedSkill(null, false, false);
            if (this.isMounted()) {
               this.dismount();
            }

            this._transformation = var1;
            this.getEffectList().stopAllToggles();
            var1.onTransform(this);
            this.sendSkillList();
            this.sendPacket(new SkillCoolTime(this));
            this.broadcastUserInfo();
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TRANSFORM, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(this, var1.getId()), this);
            }
         }
      }
   }

   public void untransform() {
      if (this._transformation != null) {
         this.setQueuedSkill(null, false, false);
         this._transformation.onUntransform(this);
         this._transformation = null;
         this.getEffectList().stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.TRANSFORM);
         this.sendSkillList();
         this.sendPacket(new SkillCoolTime(this));
         this.broadcastUserInfo();
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TRANSFORM, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(this, 0), this);
         }
      }
   }

   public Transform getTransformation() {
      return this._transformation;
   }

   public int getTransformationId() {
      return this.isTransformed() ? this._transformation.getId() : 0;
   }

   public int getTransformationDisplayId() {
      return this.isTransformed() ? this._transformation.getDisplayId() : 0;
   }

   public void setTarget(WorldObject var1) {
      WorldObject var2 = var1;
      if (var1 != null) {
         boolean var3 = var1.isPlayer() && this.isInParty() && this._party.containsPlayer(var1.asPlayer());
         if (!var3 && Math.abs(var1.getZ() - this.getZ()) > 3000) {
            var2 = null;
         }

         if (var2 != null && !var3 && !var2.isSpawned()) {
            var2 = null;
         }

         if (!this.isGM() && var2 instanceof Vehicle) {
            var2 = null;
         }
      }

      WorldObject var6 = this.getTarget();
      if (var6 != null) {
         if (var6.equals(var2)) {
            if (var2 != null && var2.getObjectId() != this.getObjectId()) {
               this.sendPacket(new ValidateLocation(var2));
            }

            return;
         }

         var6.removeStatusListener(this);
      }

      if (var2 != null && var2.isCreature()) {
         Creature var4 = var2.asCreature();
         if (var2.getObjectId() != this.getObjectId()) {
            this.sendPacket(new ValidateLocation(var4));
         }

         this.sendPacket(new MyTargetSelected(this, var4));
         var4.addStatusListener(this);
         StatusUpdate var5 = new StatusUpdate(var4);
         var5.addAttribute(10, var4.getMaxHp());
         var5.addAttribute(9, (int)var4.getCurrentHp());
         this.sendPacket(var5);
         World.broadcastToVisiblePlayers(this, new TargetSelected(this.getObjectId(), var2.getObjectId(), this.getX(), this.getY(), this.getZ()));
      }

      if (var2 == null && this.getTarget() != null) {
         this.broadcastPacket(new TargetUnselected(this));
      }

      super.setTarget(var2);
   }

   public Item getActiveWeaponInstance() {
      return this._inventory.getPaperdollItem(5);
   }

   public Weapon getActiveWeaponItem() {
      Item var1 = this.getActiveWeaponInstance();
      return var1 == null ? this._fistsWeaponItem : (Weapon)var1.getTemplate();
   }

   public Item getChestArmorInstance() {
      return this._inventory.getPaperdollItem(6);
   }

   public Item getLegsArmorInstance() {
      return this._inventory.getPaperdollItem(11);
   }

   public Armor getActiveChestArmorItem() {
      Item var1 = this.getChestArmorInstance();
      return var1 == null ? null : (Armor)var1.getTemplate();
   }

   public Armor getActiveLegsArmorItem() {
      Item var1 = this.getLegsArmorInstance();
      return var1 == null ? null : (Armor)var1.getTemplate();
   }

   public boolean isWearingHeavyArmor() {
      Item var1 = this.getLegsArmorInstance();
      Item var2 = this.getChestArmorInstance();
      return var2 != null && var1 != null && var1.getItemType() == ArmorType.HEAVY && var2.getItemType() == ArmorType.HEAVY
         ? true
         : var2 != null && this.getInventory().getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR && var2.getItemType() == ArmorType.HEAVY;
   }

   public boolean isWearingLightArmor() {
      Item var1 = this.getLegsArmorInstance();
      Item var2 = this.getChestArmorInstance();
      return var2 != null && var1 != null && var1.getItemType() == ArmorType.LIGHT && var2.getItemType() == ArmorType.LIGHT
         ? true
         : var2 != null && this._inventory.getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR && var2.getItemType() == ArmorType.LIGHT;
   }

   public boolean isWearingMagicArmor() {
      Item var1 = this.getLegsArmorInstance();
      Item var2 = this.getChestArmorInstance();
      return var2 != null && var1 != null && var1.getItemType() == ArmorType.MAGIC && var2.getItemType() == ArmorType.MAGIC
         ? true
         : var2 != null && this._inventory.getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR && var2.getItemType() == ArmorType.MAGIC;
   }

   public boolean isMarried() {
      return this._married;
   }

   public void setMarried(boolean var1) {
      this._married = var1;
   }

   public boolean isEngageRequest() {
      return this._engagerequest;
   }

   public void setEngageRequest(boolean var1, int var2) {
      this._engagerequest = var1;
      this._engageid = var2;
   }

   public void setMarryRequest(boolean var1) {
      this._marryrequest = var1;
   }

   public boolean isMarryRequest() {
      return this._marryrequest;
   }

   public void setMarryAccepted(boolean var1) {
      this._marryaccepted = var1;
   }

   public boolean isMarryAccepted() {
      return this._marryaccepted;
   }

   public int getEngageId() {
      return this._engageid;
   }

   public int getPartnerId() {
      return this._partnerId;
   }

   public void setPartnerId(int var1) {
      this._partnerId = var1;
   }

   public int getCoupleId() {
      return this._coupleId;
   }

   public void setCoupleId(int var1) {
      this._coupleId = var1;
   }

   public void engageAnswer(int var1) {
      if (this._engagerequest && this._engageid != 0) {
         Player var2 = World.getPlayer(this._engageid);
         this.setEngageRequest(false, 0);
         if (var2 != null) {
            if (var1 == 1) {
               CoupleManager.getInstance().createCouple(var2, this);
               var2.sendMessage("Request to Engage has been >ACCEPTED<");
            } else {
               var2.sendMessage("Request to Engage has been >DENIED<!");
            }
         }
      }
   }

   public Item getSecondaryWeaponInstance() {
      return this._inventory.getPaperdollItem(7);
   }

   public ItemTemplate getSecondaryWeaponItem() {
      Item var1 = this._inventory.getPaperdollItem(7);
      return var1 != null ? var1.getTemplate() : null;
   }

   public boolean doDie(Creature var1) {
      if (!super.doDie(var1)) {
         return false;
      } else {
         if (this.isMounted()) {
            this.stopFeed();
         }

         if (this._isFakeDeath) {
            this.stopFakeDeath(true);
         }

         KrateiArena var2 = this.getKrateiArena();
         if (var2 != null) {
            var2.setRespawnTask(this);
            if (var1 != null && var1.isPlayable()) {
               Player var3 = var1.asPlayer();
               if (var3 != null) {
                  KrateiArena var4 = var3.getKrateiArena();
                  if (var4 != null) {
                     var4.addPoints(var3, true);
                  }
               }
            }
         }

         if (var1 != null) {
            Player var7 = var1.asPlayer();
            boolean var9 = var1.isFakePlayer();
            if (var7 != null || var9) {
               if (this.getParty() != null && this.getParty().getUCState() instanceof UCTeam) {
                  ((UCTeam)this.getParty().getUCState()).onKill(this, var7);
               }

               if (var7 != null) {
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_KILL, this)) {
                     EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPKill(var7, this), this);
                  }

                  if ((!PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES || this.getInstanceId() == 0)
                     && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES || !this.isInsideZone(ZoneId.PVP))) {
                     if (PvpRewardItemConfig.REWARD_PVP_ITEM && this._pvpFlag != 0) {
                        var7.addItem(
                           ItemProcessType.REWARD,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_ID,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT,
                           this,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE
                        );
                     }

                     if (PvpRewardItemConfig.REWARD_PK_ITEM && this._pvpFlag == 0) {
                        var7.addItem(
                           ItemProcessType.REWARD,
                           PvpRewardItemConfig.REWARD_PK_ITEM_ID,
                           PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT,
                           this,
                           PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE
                        );
                     }
                  }
               }

               if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && (var7 != null && !var7.isGM() || var9)) {
                  String var5 = "";
                  if (this._pvpFlag == 0) {
                     var5 = PvpAnnounceConfig.ANNOUNCE_PK_MSG
                        .replace("$killer", var7 != null ? var7.getName() : var1.getName())
                        .replace("$target", this.getName());
                     if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                        SystemMessage var6 = new SystemMessage(SystemMessageId.S1);
                        var6.addString(var5);
                        World.broadcastToAllOnlinePlayers(var6);
                     } else {
                        World.broadcastToAllOnlinePlayers(var5, false);
                     }
                  } else if (this._pvpFlag != 0) {
                     var5 = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", var1.getName()).replace("$target", this.getName());
                     if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                        SystemMessage var15 = new SystemMessage(SystemMessageId.S1);
                        var15.addString(var5);
                        World.broadcastToAllOnlinePlayers(var15);
                     } else {
                        World.broadcastToAllOnlinePlayers(var5, false);
                     }
                  }
               }

               if (var9 && FakePlayersConfig.FAKE_PLAYER_KILL_KARMA && this._pvpFlag == 0 && this.getKarma() <= 0) {
                  var1.setKarma(var1.getKarma() + 150);
                  var1.broadcastInfo();
               }
            }

            this.broadcastStatusUpdate();
            this.setExpBeforeDeath(0L);
            if (this.isCursedWeaponEquipped()) {
               CursedWeaponsManager.getInstance().drop(this._cursedWeaponEquippedId, var1);
            } else if (this._combatFlagEquippedId) {
               if (TerritoryWarManager.getInstance().isTWInProgress()) {
                  TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
               } else {
                  Fort var13 = FortManager.getInstance().getFort(this);
                  if (var13 != null) {
                     FortSiegeManager.getInstance().dropCombatFlag(this, var13.getResidenceId());
                  } else {
                     BodyPart var16 = BodyPart.fromItem(this._inventory.getItemByItemId(9819));
                     this._inventory.unEquipItemInBodySlot(var16);
                     this.destroyItem(ItemProcessType.DESTROY, this._inventory.getItemByItemId(9819), null, true);
                  }
               }
            } else {
               boolean var14 = this.isInsideZone(ZoneId.PVP);
               boolean var17 = this.isInsideZone(ZoneId.SIEGE);
               if (var7 == null || !var7.isCursedWeaponEquipped()) {
                  this.onDieDropItem(var1);
                  if (!var14
                     && !var17
                     && var7 != null
                     && var7.getClan() != null
                     && this.getClan() != null
                     && !this.isAcademyMember()
                     && !var7.isAcademyMember()
                     && (this._clan.isAtWarWith(var7.getClanId()) && var7.getClan().isAtWarWith(this._clan.getId()) || this.isInSiege() && var7.isInSiege())
                     && AntiFeedManager.getInstance().check(var1, this)) {
                     if (this._clan.getReputationScore() > 0) {
                        var7.getClan().addReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
                     }

                     if (var7.getClan().getReputationScore() > 0) {
                        this._clan.takeReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
                     }
                  }

                  if (PlayerConfig.PLAYER_DELEVEL && !this.isLucky() && (var17 || !var14) && !this.isOnEvent()) {
                     this.calculateDeathExpPenalty(var1, this.isAtWarWith(var7));
                  }
               }
            }
         }

         if (!this._cubics.isEmpty()) {
            for (Cubic var10 : this._cubics.values()) {
               var10.stopAction();
               var10.cancelDisappear();
            }

            this._cubics.clear();
            this._cubicOrder.clear();
         }

         if (this.isChannelized()) {
            this.getSkillChannelized().abortChannelization();
         }

         if (this.isInParty() && this._party.isInDimensionalRift()) {
            this._party.getDimensionalRift().getDeadMemberList().add(this);
         }

         if (this._agathionId != 0) {
            this.setAgathionId(0);
         }

         this.calculateDeathPenaltyBuffLevel(var1);
         if (this.hasSummon()) {
            if (this._summon.isBetrayed()) {
               this.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
            }

            this._summon.cancelAction();
         }

         this.stopRentPet();
         this.stopWaterTask();
         AntiFeedManager.getInstance().setLastDeathTime(this.getObjectId());
         if (this.getKarma() > 0) {
            this.setKarma(this.getKarma() < 200 ? 0 : this.getKarma() - this.getKarma() / 4);
         }

         if (PlayerConfig.DISCONNECT_AFTER_DEATH) {
            DecayTaskManager.getInstance().add(this);
         }

         return true;
      }
   }

   private void onDieDropItem(Creature var1) {
      if (!this.isOnEvent() && var1 != null) {
         Player var2 = var1.asPlayer();
         if ((this.getKarma() > 0 || var2 == null || var2.getClan() == null || this.getClan() == null || !var2.getClan().isAtWarWith(this._clanId))
            && (!this.isInsideZone(ZoneId.PVP) || var2 == null)
            && (!this.isGM() || PvpConfig.KARMA_DROP_GM)) {
            boolean var3 = false;
            boolean var4 = var1 instanceof Npc;
            int var5 = PvpConfig.KARMA_PK_LIMIT;
            int var6 = 0;
            int var7 = 0;
            int var8 = 0;
            int var9 = 0;
            int var10 = 0;
            if (this.getKarma() > 0 && this._pkKills >= var5) {
               var3 = true;
               var10 = RatesConfig.KARMA_RATE_DROP;
               var6 = RatesConfig.KARMA_RATE_DROP_EQUIP;
               var7 = RatesConfig.KARMA_RATE_DROP_EQUIP_WEAPON;
               var8 = RatesConfig.KARMA_RATE_DROP_ITEM;
               var9 = RatesConfig.KARMA_DROP_LIMIT;
            } else if (var4 && this.getLevel() > 4 && !this.isFestivalParticipant()) {
               var10 = RatesConfig.PLAYER_RATE_DROP;
               var6 = RatesConfig.PLAYER_RATE_DROP_EQUIP;
               var7 = RatesConfig.PLAYER_RATE_DROP_EQUIP_WEAPON;
               var8 = RatesConfig.PLAYER_RATE_DROP_ITEM;
               var9 = RatesConfig.PLAYER_DROP_LIMIT;
            }

            if (var10 > 0 && Rnd.get(100) < var10) {
               int var11 = 0;
               int var12 = 0;

               for (Item var14 : this._inventory.getItems()) {
                  if (!var14.isShadowItem()
                     && !var14.isTimeLimitedItem()
                     && var14.isDropable()
                     && var14.getId() != 57
                     && var14.getTemplate().getType2() != 3
                     && (!this.hasSummon() || this._summon.getControlObjectId() != var14.getId())
                     && Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_ITEMS, var14.getId()) < 0
                     && Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_PET_ITEMS, var14.getId()) < 0) {
                     if (var14.isEquipped()) {
                        var12 = var14.getTemplate().getType2() == 0 ? var7 : var6;
                     } else {
                        var12 = var8;
                     }

                     if (Rnd.get(100) < var12) {
                        if (var14.isEquipped()) {
                           this._inventory.unEquipItemInSlot(var14.getLocationSlot());
                        }

                        this.dropItem(ItemProcessType.DEATH, var14, var1, true);
                        this.sendItemList(false);
                        if (var3) {
                           LOGGER.warning(this.getName() + " has karma and dropped id = " + var14.getId() + ", count = " + var14.getCount());
                        } else {
                           LOGGER.warning(this.getName() + " dropped id = " + var14.getId() + ", count = " + var14.getCount());
                        }

                        if (++var11 >= var9) {
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onKillUpdatePvPKarma(Creature var1) {
      if (var1 != null && var1.isPlayable()) {
         Player var2 = var1.asPlayer();
         if (var2 != null && var2 != this) {
            if (this.isCursedWeaponEquipped() && var1.isPlayer()) {
               CursedWeaponsManager.getInstance().increaseKills(this._cursedWeaponEquippedId);
            } else if (!this.isInOlympiadMode() && !var2.isInOlympiadMode() && (!this.isInDuel() || !var2.isInDuel())) {
               if (var1.isPlayer() && this.isInsideZone(ZoneId.SIEGE) && var2.isInsideZone(ZoneId.SIEGE)) {
                  if (!this.isSiegeFriend(var2)) {
                     Clan var3 = var2.getClan();
                     if (this._clan != null && var3 != null) {
                        this._clan.addSiegeKill();
                        var3.addSiegeDeath();
                     }
                  }
               } else if (!this.isInsideZone(ZoneId.PVP) && !var1.isInsideZone(ZoneId.PVP)) {
                  if (this.checkIfPvP(var1) && var2.getPvpFlag() != 0) {
                     this.increasePvpKills(var1);
                  } else {
                     if (var2.getClan() != null
                        && this.getClan() != null
                        && this.getClan().isAtWarWith(var2.getClanId())
                        && var2.getClan().isAtWarWith(this.getClanId())
                        && var2.getPledgeType() != -1
                        && this.getPledgeType() != -1) {
                        this.increasePvpKills(var1);
                        return;
                     }

                     if (var2.getKarma() > 0) {
                        if (PvpConfig.KARMA_AWARD_PK_KILL) {
                           this.increasePvpKills(var1);
                        }
                     } else if (var2.getPvpFlag() == 0) {
                        if (!FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
                           this.increasePkKillsAndKarma(var1);
                        } else if (this._isGood && var2.isGood() || this._isEvil && var2.isEvil()) {
                           this.increasePkKillsAndKarma(var1);
                        }

                        this.checkItemRestriction();
                     }
                  }
               }
            }
         }
      }
   }

   public void increasePvpKills(Creature var1) {
      if (var1.isPlayer() && AntiFeedManager.getInstance().check(this, var1)) {
         this.setPvpKills(this._pvpKills + 1);
         this.updatePvpTitleAndColor(true);
         this.updateUserInfo();
      }
   }

   public void increasePkKillsAndKarma(Creature var1) {
      if (var1 != null && var1.isPlayable()) {
         this.setKarma(this.getKarma() + Formulas.calculateKarmaGain(this._pkKills, var1.isSummon()));
         if (var1.isPlayer()) {
            this.setPkKills(this._pkKills + 1);
         }

         this.updateUserInfo();
      }
   }

   public void updatePvpTitleAndColor(boolean var1) {
      if (PvpTitleColorConfig.PVP_COLOR_SYSTEM_ENABLED && !FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
         if (this._pvpKills >= PvpTitleColorConfig.PVP_AMOUNT1 && this._pvpKills < PvpTitleColorConfig.PVP_AMOUNT2) {
            this.setTitle("® " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT1 + " ®");
            this._appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT1);
         } else if (this._pvpKills >= PvpTitleColorConfig.PVP_AMOUNT2 && this._pvpKills < PvpTitleColorConfig.PVP_AMOUNT3) {
            this.setTitle("® " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT2 + " ®");
            this._appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT2);
         } else if (this._pvpKills >= PvpTitleColorConfig.PVP_AMOUNT3 && this._pvpKills < PvpTitleColorConfig.PVP_AMOUNT4) {
            this.setTitle("® " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT3 + " ®");
            this._appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT3);
         } else if (this._pvpKills >= PvpTitleColorConfig.PVP_AMOUNT4 && this._pvpKills < PvpTitleColorConfig.PVP_AMOUNT5) {
            this.setTitle("® " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT4 + " ®");
            this._appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT4);
         } else if (this._pvpKills >= PvpTitleColorConfig.PVP_AMOUNT5) {
            this.setTitle("® " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT5 + " ®");
            this._appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT5);
         }

         if (var1) {
            this.broadcastTitleInfo();
         }
      }
   }

   public void updatePvPStatus() {
      if (!this.isInsideZone(ZoneId.PVP)) {
         this.setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
         if (this._pvpFlag == 0) {
            this.startPvPFlag();
         }
      }
   }

   public void updatePvPStatus(Creature var1) {
      Player var2 = var1.asPlayer();
      if (var2 != null
         && this != var2
         && (!FactionSystemConfig.FACTION_SYSTEM_ENABLED || !var1.isPlayer() || (!this.isGood() || !var2.isEvil()) && (!this.isEvil() || !var2.isGood()))
         && (!this._isInDuel || var2.getDuelId() != this.getDuelId())
         && (!this.isInsideZone(ZoneId.PVP) || !var1.isInsideZone(ZoneId.PVP))
         && var2.getKarma() == 0) {
         if (this.checkIfPvP(var2)) {
            this.setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_PVP_TIME);
         } else {
            this.setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
         }

         if (this._pvpFlag == 0) {
            this.startPvPFlag();
         }
      }
   }

   public boolean isLucky() {
      return this.getLevel() <= 9 && this.isAffectedBySkill(CommonSkill.LUCKY.getId());
   }

   public void restoreExp(double var1) {
      if (this._expBeforeDeath > 0L) {
         this.getStat().addExp(Math.round((this._expBeforeDeath - this.getExp()) * var1 / 100.0));
         this.setExpBeforeDeath(0L);
      }
   }

   public void calculateDeathExpPenalty(Creature var1, boolean var2) {
      int var3 = this.getLevel();
      double var4 = ExperienceLossData.getInstance().getPercentLost(this.getLevel());
      if (var1 != null) {
         if (var1.isRaid()) {
            var4 *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_RAID, 1.0);
         } else if (var1.isMonster()) {
            var4 *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_MOB, 1.0);
         } else if (var1.isPlayable()) {
            var4 *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_PVP, 1.0);
         }
      }

      if (this.getKarma() > 0) {
         var4 *= RatesConfig.RATE_KARMA_EXP_LOST;
      }

      long var6 = 0L;
      if (!this.isOnEvent()) {
         byte var8 = ExperienceData.getInstance().getMaxLevel();
         if (var3 < var8) {
            var6 = this.getStat().getExpForLevel(var3 + 1) - this.getStat().getExpForLevel(var3);
         } else {
            var6 = this.getStat().getExpForLevel(var8) - this.getStat().getExpForLevel(var8 - 1);
         }
      }

      long var10 = 0L;
      if (var6 > 0L) {
         var10 = Math.round(var6 * var4 / 100.0);
         var10 = Math.min(var10, var6 / 10L);
         if (this.isFestivalParticipant() || var2) {
            var10 /= 4L;
         }
      }

      this.setExpBeforeDeath(this.getExp());
      this.getStat().removeExp(var10);
   }

   public boolean isPartyWaiting() {
      return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
   }

   public void setPartyRoom(int var1) {
      this._partyroom = var1;
   }

   public int getPartyRoom() {
      return this._partyroom;
   }

   public boolean isInPartyMatchRoom() {
      return this._partyroom > 0;
   }

   public void stopAllTimers() {
      this.stopHpMpRegeneration();
      this.stopWarnUserTakeBreak();
      this.stopWaterTask();
      this.stopFeed();
      this.storePetFood(this._mountNpcId);
      this.stopRentPet();
      this.stopPvpRegTask();
      this.stopSoulTask();
      this.stopChargeTask();
      this.stopFameTask();
      this.stopVitalityTask();
   }

   public boolean hasSummon() {
      return this._summon != null;
   }

   public boolean hasPet() {
      return this._summon != null && this._summon.isPet();
   }

   public boolean hasServitor() {
      return this._summon != null && this._summon.isServitor();
   }

   public Summon getSummon() {
      return this._summon;
   }

   public Decoy getDecoy() {
      return this._decoy;
   }

   public Trap getTrap() {
      return this._trap;
   }

   public void setPet(Summon var1) {
      this._summon = var1;
   }

   public void setDecoy(Decoy var1) {
      this._decoy = var1;
   }

   public void setTrap(Trap var1) {
      this._trap = var1;
   }

   public Collection<TamedBeast> getTrainedBeasts() {
      return this._tamedBeast;
   }

   public void addTrainedBeast(TamedBeast var1) {
      if (this._tamedBeast == null) {
         this._tamedBeast = ConcurrentHashMap.newKeySet();
      }

      this._tamedBeast.add(var1);
   }

   public RequestPartner getRequest() {
      return this._request;
   }

   public void setActiveRequester(Player var1) {
      this._activeRequester = var1;
   }

   public Player getActiveRequester() {
      Player var1 = this._activeRequester;
      if (var1 != null && var1.isRequestExpired() && this._activeTradeList == null) {
         this._activeRequester = null;
      }

      return this._activeRequester;
   }

   public boolean isProcessingRequest() {
      return this.getActiveRequester() != null || this._requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public boolean isProcessingTransaction() {
      return this.getActiveRequester() != null || this._activeTradeList != null || this._requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public void onTransactionRequest(Player var1) {
      this._requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + 150;
      var1.setActiveRequester(this);
   }

   public boolean isRequestExpired() {
      return this._requestExpireTime <= GameTimeTaskManager.getInstance().getGameTicks();
   }

   public void onTransactionResponse() {
      this._requestExpireTime = 0L;
   }

   public void setActiveWarehouse(ItemContainer var1) {
      this._activeWarehouse = var1;
   }

   public ItemContainer getActiveWarehouse() {
      return this._activeWarehouse;
   }

   public void setActiveTradeList(TradeList var1) {
      this._activeTradeList = var1;
   }

   public TradeList getActiveTradeList() {
      return this._activeTradeList;
   }

   public void onTradeStart(Player var1) {
      this._activeTradeList = new TradeList(this);
      this._activeTradeList.setPartner(var1);
      SystemMessage var2 = new SystemMessage(SystemMessageId.YOU_BEGIN_TRADING_WITH_C1);
      var2.addPcName(var1);
      this.sendPacket(var2);
      this.sendPacket(new TradeStart(this));
   }

   public void onTradeConfirm(Player var1) {
      SystemMessage var2 = new SystemMessage(SystemMessageId.C1_HAS_CONFIRMED_THE_TRADE);
      var2.addPcName(var1);
      this.sendPacket(var2);
      this.sendPacket(TradeOtherDone.STATIC_PACKET);
   }

   public void onTradeCancel(Player var1) {
      if (this._activeTradeList != null) {
         this._activeTradeList.lock();
         this._activeTradeList = null;
         this.sendPacket(new TradeDone(0));
         SystemMessage var2 = new SystemMessage(SystemMessageId.C1_HAS_CANCELLED_THE_TRADE);
         var2.addPcName(var1);
         this.sendPacket(var2);
      }
   }

   public void onTradeFinish(boolean var1) {
      this._activeTradeList = null;
      this.sendPacket(new TradeDone(1));
      if (var1) {
         this.sendPacket(SystemMessageId.YOUR_TRADE_IS_SUCCESSFUL);
      }
   }

   public void startTrade(Player var1) {
      this.onTradeStart(var1);
      var1.onTradeStart(this);
   }

   public void cancelActiveTrade() {
      if (this._activeTradeList != null) {
         Player var1 = this._activeTradeList.getPartner();
         if (var1 != null) {
            var1.onTradeCancel(this);
         }

         this.onTradeCancel(this);
      }
   }

   public boolean hasManufactureShop() {
      return this._manufactureItems != null && !this._manufactureItems.isEmpty();
   }

   public Map<Integer, ManufactureItem> getManufactureItems() {
      if (this._manufactureItems == null) {
         synchronized (this) {
            this._manufactureItems = Collections.synchronizedMap(new LinkedHashMap<>());
         }
      }

      return this._manufactureItems;
   }

   public String getStoreName() {
      return this._storeName;
   }

   public void setStoreName(String var1) {
      this._storeName = var1 == null ? "" : var1;
   }

   public TradeList getSellList() {
      if (this._sellList == null) {
         this._sellList = new TradeList(this);
      }

      return this._sellList;
   }

   public TradeList getBuyList() {
      if (this._buyList == null) {
         this._buyList = new TradeList(this);
      }

      return this._buyList;
   }

   public void setPrivateStoreType(PrivateStoreType var1) {
      this._privateStoreType = var1;
      if (OfflineTradeConfig.OFFLINE_DISCONNECT_FINISHED && var1 == PrivateStoreType.NONE && (this._client == null || this._client.isDetached())) {
         OfflineTraderTable.getInstance().removeTrader(this.getObjectId());
         Disconnection.of(this).storeAndDelete();
      }
   }

   public PrivateStoreType getPrivateStoreType() {
      return this._privateStoreType;
   }

   public void setClan(Clan var1) {
      this._clan = var1;
      if (var1 == null) {
         this.setTitle("");
         this._clanId = 0;
         this._clanPrivileges = new ClanPrivileges();
         this._pledgeType = 0;
         this._powerGrade = 0;
         this._lvlJoinedAcademy = 0;
         this._apprentice = 0;
         this._sponsor = 0;
         this._activeWarehouse = null;
      } else if (!var1.isMember(this.getObjectId())) {
         this.setClan(null);
      } else {
         this._clanId = var1.getId();
      }
   }

   public Clan getClan() {
      return this._clan;
   }

   public boolean isClanLeader() {
      return this._clan != null && this.getObjectId() == this._clan.getLeaderId();
   }

   protected void reduceArrowCount(boolean var1) {
      Item var2 = this._inventory.getPaperdollItem(7);
      if (var2 == null) {
         this._inventory.unEquipItemInSlot(7);
         if (var1) {
            this._boltItem = null;
         } else {
            this._arrowItem = null;
         }

         this.sendItemList(false);
      } else if (var2.getCount() > 1L) {
         synchronized (var2) {
            var2.changeCount(null, -1L, this, null);
            var2.setLastChange(2);
            this._inventory.refreshWeight();
         }

         InventoryUpdate var3 = new InventoryUpdate();
         var3.addModifiedItem(var2);
         this.sendInventoryUpdate(var3);
      } else {
         this._inventory.destroyItem(ItemProcessType.NONE, var2, this, null);
         this._inventory.unEquipItemInSlot(7);
         if (var1) {
            this._boltItem = null;
         } else {
            this._arrowItem = null;
         }

         this.sendItemList(false);
      }
   }

   protected boolean checkAndEquipArrows() {
      if (this._inventory.getPaperdollItem(7) == null) {
         this._arrowItem = this._inventory.findArrowForBow(this.getActiveWeaponItem());
         if (this._arrowItem != null) {
            this._inventory.setPaperdollItem(7, this._arrowItem);
            this.sendItemList(false);
         }
      } else {
         this._arrowItem = this._inventory.getPaperdollItem(7);
      }

      return this._arrowItem != null;
   }

   protected boolean checkAndEquipBolts() {
      if (this._inventory.getPaperdollItem(7) == null) {
         this._boltItem = this._inventory.findBoltForCrossBow(this.getActiveWeaponItem());
         if (this._boltItem != null) {
            this._inventory.setPaperdollItem(7, this._boltItem);
            this.sendItemList(false);
         }
      } else {
         this._boltItem = this._inventory.getPaperdollItem(7);
      }

      return this._boltItem != null;
   }

   public boolean disarmWeapons() {
      Item var1 = this._inventory.getPaperdollItem(5);
      if (var1 == null) {
         return true;
      } else if (this.isCursedWeaponEquipped()) {
         return false;
      } else if (this._combatFlagEquippedId) {
         return false;
      } else if (var1.getWeaponItem().isForceEquip()) {
         return false;
      } else {
         Collection var2 = this._inventory.unEquipItemInBodySlotAndRecord(var1.getTemplate().getBodyPart());
         InventoryUpdate var3 = new InventoryUpdate();

         for (Item var5 : var2) {
            var3.addModifiedItem(var5);
         }

         this.sendInventoryUpdate(var3);
         this.abortAttack();
         this.broadcastUserInfo();
         if (!var2.isEmpty()) {
            Item var6 = (Item)var2.iterator().next();
            SystemMessage var7;
            if (var6.isEnchanted()) {
               var7 = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               var7.addInt(var6.getEnchantLevel());
            } else {
               var7 = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            var7.addItemName(var6);
            this.sendPacket(var7);
         }

         return true;
      }
   }

   public boolean disarmShield() {
      Item var1 = this._inventory.getPaperdollItem(7);
      if (var1 != null) {
         Collection var2 = this._inventory.unEquipItemInBodySlotAndRecord(var1.getTemplate().getBodyPart());
         InventoryUpdate var3 = new InventoryUpdate();

         for (Item var5 : var2) {
            var3.addModifiedItem(var5);
         }

         this.sendInventoryUpdate(var3);
         this.abortAttack();
         this.broadcastUserInfo();
         if (!var2.isEmpty()) {
            SystemMessage var6 = null;
            Item var8 = (Item)var2.iterator().next();
            if (var8.isEnchanted()) {
               var6 = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               var6.addInt(var8.getEnchantLevel());
            } else {
               var6 = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            var6.addItemName(var8);
            this.sendPacket(var6);
         }
      }

      return true;
   }

   public boolean mount(Summon var1) {
      if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE && this.isInsideZone(ZoneId.SIEGE)) {
         MountType var2 = MountType.findByNpcId(var1.getId());
         boolean var3 = var2 == MountType.STRIDER
            && CastleManager.getInstance().getCastle(this) != null
            && this.isClanLeader()
            && this.getClanId() == CastleManager.getInstance().getCastle(this).getOwnerId();
         if (!var3) {
            this.sendMessage("You cannot mount during a siege.");
            return false;
         }
      }

      if (this.disarmWeapons() && this.disarmShield() && !this.isTransformed()) {
         this.getEffectList().stopAllToggles();
         this.setMount(var1.getId(), var1.getLevel());
         this.setMountObjectID(var1.getControlObjectId());
         this.startFeed(var1.getId());
         this.broadcastPacket(new Ride(this));
         this.broadcastUserInfo();
         var1.unSummon(this);
         return true;
      } else {
         return false;
      }
   }

   public boolean mount(int var1, int var2, boolean var3) {
      if (this.disarmWeapons() && this.disarmShield() && !this.isTransformed()) {
         this.getEffectList().stopAllToggles();
         this.setMount(var1, this.getLevel());
         this.setMountObjectID(var2);
         this.broadcastPacket(new Ride(this));
         this.broadcastUserInfo();
         if (var3) {
            this.startFeed(var1);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mountPlayer(Summon var1) {
      if (var1 != null && var1.isMountable() && !this.isMounted() && !this.isBetrayed()) {
         if (this.isDead()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHEN_DEAD);
            return false;
         }

         if (var1.isDead()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_DEAD_STRIDER_CANNOT_BE_RIDDEN);
            return false;
         }

         if (var1.isInCombat() || var1.isRooted()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_STRIDER_IN_BATTLE_CANNOT_BE_RIDDEN);
            return false;
         }

         if (this.isInCombat()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
            return false;
         }

         if (this._waitTypeSitting) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_STRIDER_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
            return false;
         }

         if (this._fishing) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
            return false;
         }

         if (this.isTransformed() || this.isCursedWeaponEquipped()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         }

         if (this._inventory.getItemByItemId(9819) != null) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendMessage("You cannot mount a steed while holding a flag.");
            return false;
         }

         if (var1.isHungry()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
            return false;
         }

         if (!LocationUtil.checkIfInRange(200, this, var1, true)) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_AWAY_FROM_YOUR_MOUNT_TO_RIDE);
            return false;
         }

         if (!var1.isDead() && !this.isMounted()) {
            this.mount(var1);
         }
      } else if (this.isRentedPet()) {
         this.stopRentPet();
      } else if (this.isMounted()) {
         if (this._mountType == MountType.WYVERN && this.isInsideZone(ZoneId.NO_LANDING) && !this.isGM()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
            return false;
         }

         if (this.isHungry()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
            return false;
         }

         this.dismount();
      }

      return true;
   }

   public boolean dismount() {
      if (ZoneManager.getInstance().getZone(this.getX(), this.getY(), this.getZ() - 300, WaterZone.class) == null) {
         if (!this.isGM() && !this.isInWater() && this.getZ() > 10000) {
            this.sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         }

         if (!this.isInsideZone(ZoneId.NO_LANDING) && GeoEngine.getInstance().getHeight(this.getX(), this.getY(), this.getZ()) + 300 < this.getZ()) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_DISMOUNT_FROM_THIS_ELEVATION);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         }
      } else {
         ThreadPool.schedule(() -> {
            if (this.isInWater()) {
               this.broadcastUserInfo();
            }
         }, 1500L);
      }

      boolean var1 = this.isFlying();
      this.sendPacket(new SetupGauge(this.getObjectId(), 3, 0, 0));
      int var2 = this._mountNpcId;
      this.setMount(0, 0);
      this.stopFeed();
      if (var1) {
         this.removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
      }

      this.broadcastPacket(new Ride(this));
      this.setMountObjectID(0);
      this.storePetFood(var2);
      this.broadcastUserInfo();
      return true;
   }

   public void setUptime(long var1) {
      this._uptime = var1;
   }

   public long getUptime() {
      return System.currentTimeMillis() - this._uptime;
   }

   public boolean isInvul() {
      return super.isInvul() || this.isTeleportProtected();
   }

   public boolean isInParty() {
      return this._party != null;
   }

   public void setParty(Party var1) {
      this._party = var1;
   }

   public void joinParty(Party var1) {
      if (var1 != null) {
         this._party = var1;
         var1.addPartyMember(this);
      }
   }

   public void leaveParty() {
      if (this.isInParty()) {
         this._party.removePartyMember(this, PartyMessageType.DISCONNECTED);
         this._party = null;
      }
   }

   public Party getParty() {
      return this._party;
   }

   public void setPartyDistributionType(PartyDistributionType var1) {
      this._partyDistributionType = var1;
   }

   public PartyDistributionType getPartyDistributionType() {
      return this._partyDistributionType;
   }

   public boolean isGM() {
      return this.getAccessLevel().isGm();
   }

   public void setAccessLevel(int var1) {
      this._accessLevel = AdminData.getInstance().getAccessLevel(var1);
      this._appearance.setNameColor(this._accessLevel.getNameColor());
      this._appearance.setTitleColor(this._accessLevel.getTitleColor());
      this.broadcastUserInfo();
      CharInfoTable.getInstance().addName(this);
      if (!AdminData.getInstance().hasAccessLevel(var1)) {
         LOGGER.warning("Tried to set unregistered access level " + var1 + " for " + this + ". Setting access level without privileges!");
      } else if (var1 > 0) {
         LOGGER.warning(this._accessLevel.getName() + " access level set for character " + this.getName() + "! Just a warning to be careful ;)");
      }
   }

   public void setAccountAccesslevel(int var1) {
      LoginServerThread.getInstance().sendAccessLevel(this.getAccountName(), var1);
   }

   public AccessLevel getAccessLevel() {
      if (GeneralConfig.EVERYBODY_HAS_ADMIN_RIGHTS) {
         return AdminData.getInstance().getMasterAccessLevel();
      } else {
         if (this._accessLevel == null) {
            this.setAccessLevel(0);
         }

         return this._accessLevel;
      }
   }

   public void updateAndBroadcastStatus() {
      if (this._updateAndBroadcastStatusTask == null) {
         this._updateAndBroadcastStatusTask = ThreadPool.schedule(() -> {
            this.refreshOverloaded();
            this.refreshExpertisePenalty();
            this.broadcastUserInfo();
            this._updateAndBroadcastStatusTask = null;
         }, 50L);
      }
   }

   public void setKarmaFlag() {
      this.updateUserInfo();
      World.forEachVisibleObject(this, Player.class, var1 -> {
         var1.sendPacket(new RelationChanged(this, this.getRelation(var1), this.isAutoAttackable(var1)));
         if (this.hasSummon()) {
            var1.sendPacket(new RelationChanged(this._summon, this.getRelation(var1), this.isAutoAttackable(var1)));
         }
      });
   }

   public void broadcastKarma() {
      StatusUpdate var1 = new StatusUpdate(this);
      var1.addAttribute(27, this.getKarma());
      this.sendPacket(var1);
      World.forEachVisibleObject(this, Player.class, var1x -> {
         var1x.sendPacket(new RelationChanged(this, this.getRelation(var1x), this.isAutoAttackable(var1x)));
         if (this.hasSummon()) {
            var1x.sendPacket(new RelationChanged(this._summon, this.getRelation(var1x), this.isAutoAttackable(var1x)));
         }
      });
   }

   public void setOnlineStatus(boolean var1, boolean var2) {
      if (this._isOnline != var1) {
         this._isOnline = var1;
      }

      if (var2) {
         this.updateOnlineStatus();
      }
   }

   public void setIn7sDungeon(boolean var1) {
      this._isIn7sDungeon = var1;
   }

   public void updateOnlineStatus() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
      ) {
         var2.setInt(1, this.isOnlineInt());
         var2.setLong(2, System.currentTimeMillis());
         var2.setInt(3, this.getObjectId());
         var2.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Failed updating character online status.", (Throwable)var9);
      }
   }

   private boolean createDb() {
      try {
         try (
            Connection var1 = DatabaseFactory.getConnection();
            PreparedStatement var2 = var1.prepareStatement(
               "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate,lastAccess,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            );
         ) {
            var2.setString(1, this._accountName);
            var2.setInt(2, this.getObjectId());
            var2.setString(3, this.getName());
            var2.setInt(4, this.getLevel());
            var2.setInt(5, this.getMaxHp());
            var2.setDouble(6, this.getCurrentHp());
            var2.setInt(7, this.getMaxCp());
            var2.setDouble(8, this.getCurrentCp());
            var2.setInt(9, this.getMaxMp());
            var2.setDouble(10, this.getCurrentMp());
            var2.setInt(11, this._appearance.getFace());
            var2.setInt(12, this._appearance.getHairStyle());
            var2.setInt(13, this._appearance.getHairColor());
            var2.setInt(14, this._appearance.isFemale() ? 1 : 0);
            var2.setLong(15, this.getExp());
            var2.setLong(16, this.getSp());
            var2.setInt(17, this.getKarma());
            var2.setInt(18, this._fame);
            var2.setInt(19, this._pvpKills);
            var2.setInt(20, this._pkKills);
            var2.setInt(21, this._clanId);
            var2.setInt(22, this.getRace().ordinal());
            var2.setInt(23, this.getPlayerClass().getId());
            var2.setLong(24, this._deleteTimer);
            var2.setInt(25, this.hasDwarvenCraft() ? 1 : 0);
            var2.setString(26, this.getTitle());
            var2.setInt(27, this._appearance.getTitleColor());
            var2.setInt(28, this.getAccessLevel().getLevel());
            var2.setInt(29, this.isOnlineInt());
            var2.setInt(30, this._isIn7sDungeon ? 1 : 0);
            var2.setInt(31, this._clanPrivileges.getMask());
            var2.setInt(32, this._wantsPeace);
            var2.setInt(33, this._baseClass);
            var2.setInt(34, this._newbie);
            var2.setInt(35, this._noble ? 1 : 0);
            var2.setLong(36, 0L);
            var2.setTimestamp(37, new Timestamp(this._createDate.getTimeInMillis()));
            var2.setLong(38, System.currentTimeMillis());
            var2.setLong(39, System.currentTimeMillis());
            var2.executeUpdate();
         }

         return true;
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Could not insert char data: " + var9.getMessage(), (Throwable)var9);
         return false;
      }
   }

   private static Player restore(int var0) {
      Player var1 = null;
      double var2 = 0.0;
      double var4 = 0.0;
      double var6 = 0.0;

      try (
         Connection var8 = DatabaseFactory.getConnection();
         PreparedStatement var9 = var8.prepareStatement("SELECT * FROM characters WHERE charId=?");
      ) {
         var9.setInt(1, var0);

         try (ResultSet var10 = var9.executeQuery()) {
            if (var10.next()) {
               int var11 = var10.getInt("classid");
               boolean var12 = var10.getInt("sex") != Sex.MALE.ordinal();
               PlayerTemplate var13 = PlayerTemplateData.getInstance().getTemplate(var11);
               PlayerAppearance var14 = new PlayerAppearance(var10.getByte("face"), var10.getByte("hairColor"), var10.getByte("hairStyle"), var12);
               var1 = new Player(var0, var13, var10.getString("account_name"), var14);
               var1.setName(var10.getString("char_name"));
               var1._lastAccess = var10.getLong("lastAccess");
               PlayerStat var15 = var1.getStat();
               var15.setExp(var10.getLong("exp"));
               var1.setExpBeforeDeath(var10.getLong("expBeforeDeath"));
               var15.setLevel(var10.getByte("level"));
               var15.setSp(var10.getLong("sp"));
               var1.setWantsPeace(var10.getInt("wantspeace"));
               var1.setHeading(var10.getInt("heading"));
               var1.setKarma(var10.getInt("karma"));
               var1.setFame(var10.getInt("fame"));
               var1.setPvpKills(var10.getInt("pvpkills"));
               var1.setPkKills(var10.getInt("pkkills"));
               var1.setOnlineTime(var10.getLong("onlinetime"));
               var1.setNewbie(var10.getInt("newbie"));
               var1.setNoble(var10.getInt("nobless") == 1);
               int var16 = var10.getInt("faction");
               if (var16 == 1) {
                  var1.setGood();
               }

               if (var16 == 2) {
                  var1.setEvil();
               }

               var1.setClanJoinExpiryTime(var10.getLong("clan_join_expiry_time"));
               if (var1.getClanJoinExpiryTime() < System.currentTimeMillis()) {
                  var1.setClanJoinExpiryTime(0L);
               }

               var1.setClanCreateExpiryTime(var10.getLong("clan_create_expiry_time"));
               if (var1.getClanCreateExpiryTime() < System.currentTimeMillis()) {
                  var1.setClanCreateExpiryTime(0L);
               }

               var1.setPcCafePoints(var10.getInt("pccafe_points"));
               var1.setPowerGrade(var10.getInt("power_grade"));
               var1.setPledgeType(var10.getInt("subpledge"));
               var1.setDeleteTimer(var10.getLong("deletetime"));
               var1.setTitle(var10.getString("title"));
               var1.setAccessLevel(var10.getInt("accesslevel"));
               int var17 = var10.getInt("title_color");
               if (var17 != 15530402) {
                  var1.getAppearance().setTitleColor(var17);
               }

               var1.setFistsWeaponItem(var1.findFistsWeaponItem(var11));
               var1.setUptime(System.currentTimeMillis());
               var4 = var10.getDouble("curHp");
               var2 = var10.getDouble("curCp");
               var6 = var10.getDouble("curMp");
               var1.setLastRecomUpdate(var10.getLong("last_recom_date"));
               var1.checkRecom(var10.getInt("rec_have"), var10.getInt("rec_left"));
               var1._classIndex = 0;

               try {
                  var1.setBaseClass(var10.getInt("base_class"));
               } catch (Exception var32) {
                  var1.setBaseClass(var11);
               }

               if (restoreSubClassData(var1) && var11 != var1.getBaseClass()) {
                  for (SubClassHolder var19 : var1.getSubClasses().values()) {
                     if (var19.getId() == var11) {
                        var1._classIndex = var19.getClassIndex();
                     }
                  }
               }

               if (var1.getClassIndex() == 0 && var11 != var1.getBaseClass()) {
                  var1.setPlayerClass(var1.getBaseClass());
                  LOGGER.warning(var1 + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
               } else {
                  var1._activeClass = var11;
               }

               var1.setApprentice(var10.getInt("apprentice"));
               var1.setSponsor(var10.getInt("sponsor"));
               var1.setLvlJoinedAcademy(var10.getInt("lvl_joined_academy"));
               var1.setIn7sDungeon(var10.getInt("isin7sdungeon") == 1);
               CursedWeaponsManager.getInstance().checkPlayer(var1);
               var1.setDeathPenaltyBuffLevel(var10.getInt("death_penalty_level"));
               var1.setVitalityPoints(var10.getInt("vitality_points"), true);
               int var39 = var10.getInt("x");
               int var40 = var10.getInt("y");
               int var20 = GeoEngine.getInstance().getHeight(var39, var40, var10.getInt("z"));
               var1.setXYZInvisible(var39, var40, var20);
               var1.setLastServerPosition(var39, var40, var20);
               var1.setBookmarkSlot(var10.getInt("BookmarkSlot"));
               var1.getCreateDate().setTimeInMillis(var10.getTimestamp("createDate").getTime());
               var1.setLang(var10.getString("language"));
               var1.setHero(Hero.getInstance().isHero(var0));
               int var21 = var10.getInt("clanid");
               Clan var22 = null;
               if (var21 > 0) {
                  var22 = ClanTable.getInstance().getClan(var21);
                  var1.setClan(var22);
                  if (var22 != null && var22.isMember(var0)) {
                     if (var22.getLeaderId() != var1.getObjectId()) {
                        if (var1.getPowerGrade() == 0) {
                           var1.setPowerGrade(5);
                        }

                        var1.setClanPrivileges(var22.getRankPrivs(var1.getPowerGrade()));
                     } else {
                        var1.getClanPrivileges().enableAll();
                        var1.setPowerGrade(1);
                     }

                     var1.setPledgeClass(ClanMember.calculatePledgeClass(var1));
                  }
               }

               if (var22 == null) {
                  if (var1.isNoble()) {
                     var1.setPledgeClass(5);
                  }

                  if (var1.isHero()) {
                     var1.setPledgeClass(8);
                  }

                  var1.getClanPrivileges().disableAll();
               }

               try (PreparedStatement var23 = var8.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?")) {
                  var23.setString(1, var1._accountName);
                  var23.setInt(2, var0);

                  try (ResultSet var24 = var23.executeQuery()) {
                     while (var24.next()) {
                        var1._chars.put(var24.getInt("charId"), var24.getString("char_name"));
                     }
                  }
               }
            }
         }
      } catch (Exception var38) {
         LOGGER.log(Level.SEVERE, "Failed loading character.", (Throwable)var38);
      }

      if (var1 == null) {
         return null;
      } else {
         var1.getInventory().restore();
         var1.getWarehouse().restore();
         var1.getFreight().restore();
         var1.restoreSkills();
         var1.getMacros().restoreMe();
         var1.restoreShortcuts();
         var1.restoreHenna();
         var1.restoreTeleportBookmark();
         if (!PlayerConfig.ALT_RECOMMEND) {
            var1.restoreRecom();
         }

         var1.restoreRecipeBook(true);
         if (PlayerConfig.STORE_RECIPE_SHOPLIST) {
            var1.restoreRecipeShopList();
         }

         var1.loadPremiumItemList();
         var1.restorePetInventoryItems();
         var1.rewardSkills();
         var1.restoreItemReuse();
         var1.setCurrentCp(var2);
         var1.setCurrentHp(var4);
         var1.setCurrentMp(var6);
         var1.setOriginalCpHpMp(var2, var4, var6);
         if (var4 < 0.5) {
            var1.setDead(true);
            var1.stopHpMpRegeneration();
         }

         var1.setPet(World.getPet(var1.getObjectId()));
         if (var1.hasSummon()) {
            var1.getSummon().setOwner(var1);
         }

         var1.refreshOverloaded();
         var1.refreshExpertisePenalty();
         var1.restoreFriendList();
         var1.setOnlineStatus(true, false);
         PlayerAutoSaveTaskManager.getInstance().add(var1);
         return var1;
      }
   }

   public Forum getMail() {
      if (this._forumMail == null) {
         this.setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(this.getName()));
         if (this._forumMail == null) {
            ForumsBBSManager.getInstance().createNewForum(this.getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), 4, 3, this.getObjectId());
            this.setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(this.getName()));
         }
      }

      return this._forumMail;
   }

   public void setMail(Forum var1) {
      this._forumMail = var1;
   }

   public Forum getMemo() {
      if (this._forumMemo == null) {
         this.setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(this._accountName));
         if (this._forumMemo == null) {
            ForumsBBSManager.getInstance()
               .createNewForum(this._accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), 3, 3, this.getObjectId());
            this.setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(this._accountName));
         }
      }

      return this._forumMemo;
   }

   public void setMemo(Forum var1) {
      this._forumMemo = var1;
   }

   private static boolean restoreSubClassData(Player var0) {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement(
            "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC"
         );
      ) {
         var2.setInt(1, var0.getObjectId());

         try (ResultSet var3 = var2.executeQuery()) {
            while (var3.next()) {
               SubClassHolder var4 = new SubClassHolder();
               var4.setPlayerClass(var3.getInt("class_id"));
               var4.setLevel(var3.getByte("level"));
               var4.setExp(var3.getLong("exp"));
               var4.setSp(var3.getLong("sp"));
               var4.setClassIndex(var3.getInt("class_index"));
               var0.getSubClasses().put(var4.getClassIndex(), var4);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore classes for " + var0.getName() + ": " + var12.getMessage(), (Throwable)var12);
      }

      return true;
   }

   private void restoreRecipeBook(boolean var1) {
      String var2 = var1
         ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?"
         : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";

      try (
         Connection var3 = DatabaseFactory.getConnection();
         PreparedStatement var4 = var3.prepareStatement(var2);
      ) {
         var4.setInt(1, this.getObjectId());
         if (!var1) {
            var4.setInt(2, this._classIndex);
         }

         try (ResultSet var5 = var4.executeQuery()) {
            this._dwarvenRecipeBook.clear();
            RecipeData var6 = RecipeData.getInstance();

            while (var5.next()) {
               RecipeList var7 = var6.getRecipeList(var5.getInt("id"));
               if (var1) {
                  if (var5.getInt(2) == 1) {
                     if (var5.getInt(3) == this._classIndex) {
                        this.registerDwarvenRecipeList(var7, false);
                     }
                  } else {
                     this.registerCommonRecipeList(var7, false);
                  }
               } else {
                  this.registerDwarvenRecipeList(var7, false);
               }
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, "Could not restore recipe book data:" + var14.getMessage(), (Throwable)var14);
      }
   }

   public Map<Integer, PremiumItem> getPremiumItemList() {
      return this._premiumItems;
   }

   private void loadPremiumItemList() {
      String var1 = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";

      try (
         Connection var2 = DatabaseFactory.getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?");
      ) {
         var3.setInt(1, this.getObjectId());

         try (ResultSet var4 = var3.executeQuery()) {
            while (var4.next()) {
               this._premiumItems.put(var4.getInt("itemNum"), new PremiumItem(var4.getInt("itemId"), var4.getLong("itemCount"), var4.getString("itemSender")));
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "Could not restore premium items: " + var13.getMessage(), (Throwable)var13);
      }
   }

   public void updatePremiumItem(int var1, long var2) {
      try (
         Connection var4 = DatabaseFactory.getConnection();
         PreparedStatement var5 = var4.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? ");
      ) {
         var5.setLong(1, var2);
         var5.setInt(2, this.getObjectId());
         var5.setInt(3, var1);
         var5.execute();
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Could not update premium items: " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void deletePremiumItem(int var1) {
      try (
         Connection var2 = DatabaseFactory.getConnection();
         PreparedStatement var3 = var2.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? ");
      ) {
         var3.setInt(1, this.getObjectId());
         var3.setInt(2, var1);
         var3.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Could not delete premium item: " + var10);
      }
   }

   public synchronized void store(boolean var1) {
      this.storeCharBase();
      this.storeCharSub();
      this.storeEffect(var1);
      this.storeItemReuseDelay();
      if (PlayerConfig.STORE_RECIPE_SHOPLIST) {
         this.storeRecipeShopList();
      }

      SevenSigns.getInstance().saveSevenSignsData(this.getObjectId());
      PlayerVariables var2 = (PlayerVariables)this.getScript(PlayerVariables.class);
      if (var2 != null) {
         var2.storeMe();
      }

      AccountVariables var3 = (AccountVariables)this.getScript(AccountVariables.class);
      if (var3 != null) {
         var3.storeMe();
      }

      this.getInventory().updateDatabase();
      this.getWarehouse().updateDatabase();
      this.getFreight().updateDatabase();
   }

   public void storeMe() {
      this.store(true);
   }

   private void storeCharBase() {
      long var1 = this.getStat().getBaseExp();
      byte var3 = this.getStat().getBaseLevel();
      long var4 = this.getStat().getBaseSp();

      try (
         Connection var6 = DatabaseFactory.getConnection();
         PreparedStatement var7 = var6.prepareStatement(
            "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=?,faction=?,pccafe_points=?,last_recom_date=?,rec_have=?,rec_left=? WHERE charId=?"
         );
      ) {
         var7.setInt(1, var3);
         var7.setInt(2, this.getMaxHp());
         var7.setDouble(3, this.getCurrentHp());
         var7.setInt(4, this.getMaxCp());
         var7.setDouble(5, this.getCurrentCp());
         var7.setInt(6, this.getMaxMp());
         var7.setDouble(7, this.getCurrentMp());
         var7.setInt(8, this._appearance.getFace());
         var7.setInt(9, this._appearance.getHairStyle());
         var7.setInt(10, this._appearance.getHairColor());
         var7.setInt(11, this._appearance.isFemale() ? 1 : 0);
         var7.setInt(12, this.getHeading());
         var7.setInt(13, this._observerMode ? this._lastLoc.getX() : this.getX());
         var7.setInt(14, this._observerMode ? this._lastLoc.getY() : this.getY());
         var7.setInt(15, this._observerMode ? this._lastLoc.getZ() : this.getZ());
         var7.setLong(16, var1);
         var7.setLong(17, this._expBeforeDeath);
         var7.setLong(18, var4);
         var7.setInt(19, this.getKarma());
         var7.setInt(20, this._fame);
         var7.setInt(21, this._pvpKills);
         var7.setInt(22, this._pkKills);
         var7.setInt(23, this._clanId);
         var7.setInt(24, this.getRace().ordinal());
         var7.setInt(25, this.getPlayerClass().getId());
         var7.setLong(26, this._deleteTimer);
         var7.setString(27, this.getTitle());
         var7.setInt(28, this._appearance.getTitleColor());
         var7.setInt(29, this.getAccessLevel().getLevel());
         var7.setInt(30, this.isOnlineInt());
         var7.setInt(31, this._isIn7sDungeon ? 1 : 0);
         var7.setInt(32, this._clanPrivileges.getMask());
         var7.setInt(33, this._wantsPeace);
         var7.setInt(34, this._baseClass);
         long var8 = this._onlineTime;
         if (this._onlineBeginTime > 0L) {
            var8 += (System.currentTimeMillis() - this._onlineBeginTime) / 1000L;
         }

         var7.setLong(35, this._offlineShopStart > 0L ? this._onlineTime : var8);
         var7.setInt(36, this._newbie);
         var7.setInt(37, this._noble ? 1 : 0);
         var7.setInt(38, this._powerGrade);
         var7.setInt(39, this._pledgeType);
         var7.setInt(40, this._lvlJoinedAcademy);
         var7.setLong(41, this._apprentice);
         var7.setLong(42, this._sponsor);
         var7.setLong(43, this._clanJoinExpiryTime);
         var7.setLong(44, this._clanCreateExpiryTime);
         var7.setString(45, this.getName());
         var7.setLong(46, this._deathPenaltyBuffLevel);
         var7.setInt(47, this._bookmarkSlot);
         var7.setInt(48, this.getVitalityPoints());
         var7.setString(49, this._lang);
         byte var10 = 0;
         if (this._isGood) {
            var10 = 1;
         }

         if (this._isEvil) {
            var10 = 2;
         }

         var7.setInt(50, var10);
         var7.setInt(51, this._pcCafePoints);
         var7.setLong(52, this.getLastRecomUpdate());
         var7.setInt(53, this.getRecomHave());
         var7.setInt(54, this.getRecomLeft());
         var7.setInt(55, this.getObjectId());
         var7.execute();
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Could not store char base data: " + this + " - " + var15.getMessage(), (Throwable)var15);
      }
   }

   private void storeCharSub() {
      if (this.getTotalSubClasses() > 0) {
         try (
            Connection var1 = DatabaseFactory.getConnection();
            PreparedStatement var2 = var1.prepareStatement("UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?");
         ) {
            for (SubClassHolder var4 : this.getSubClasses().values()) {
               var2.setLong(1, var4.getExp());
               var2.setLong(2, var4.getSp());
               var2.setInt(3, var4.getLevel());
               var2.setInt(4, var4.getId());
               var2.setInt(5, this.getObjectId());
               var2.setInt(6, var4.getClassIndex());
               var2.addBatch();
            }

            var2.executeBatch();
         } catch (Exception var9) {
            LOGGER.log(Level.WARNING, "Could not store sub class data for " + this.getName() + ": " + var9.getMessage(), (Throwable)var9);
         }
      }
   }

   public void storeEffect(boolean var1) {
      if (PlayerConfig.STORE_SKILL_COOLTIME) {
         try (Connection var2 = DatabaseFactory.getConnection()) {
            try (PreparedStatement var3 = var2.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?")) {
               var3.setInt(1, this.getObjectId());
               var3.setInt(2, this._classIndex);
               var3.execute();
            }

            int var19 = 0;
            ArrayList var4 = new ArrayList();
            long var5 = System.currentTimeMillis();

            try (PreparedStatement var7 = var2.prepareStatement(
                  "REPLACE INTO character_skills_save (charId,skill_id,skill_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)"
               )) {
               if (var1) {
                  for (BuffInfo var9 : this.getEffectList().getEffects()) {
                     if (var9 != null) {
                        Skill var10 = var9.getSkill();
                        if (var10.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS
                           && (!var10.isToggle() || PlayerConfig.ALT_STORE_TOGGLES)
                           && (!var10.isDance() || PlayerConfig.ALT_STORE_DANCES)
                           && !var4.contains(var10.getReuseHashCode())) {
                           var4.add(var10.getReuseHashCode());
                           var7.setInt(1, this.getObjectId());
                           var7.setInt(2, var10.getId());
                           var7.setInt(3, var10.getLevel());
                           var7.setInt(4, var9.getTime());
                           TimeStamp var11 = this.getSkillReuseTimeStamp(var10.getReuseHashCode());
                           var7.setLong(5, var11 != null && var5 < var11.getStamp() ? var11.getReuse() : 0L);
                           var7.setLong(6, var11 != null && var5 < var11.getStamp() ? var11.getStamp() : 0L);
                           var7.setInt(7, 0);
                           var7.setInt(8, this._classIndex);
                           var7.setInt(9, ++var19);
                           var7.addBatch();
                        }
                     }
                  }
               }

               for (Entry var21 : this.getSkillReuseTimeStamps().entrySet()) {
                  int var22 = (Integer)var21.getKey();
                  if (!var4.contains(var22)) {
                     TimeStamp var23 = (TimeStamp)var21.getValue();
                     if (var23 != null && var5 < var23.getStamp()) {
                        var4.add(var22);
                        var7.setInt(1, this.getObjectId());
                        var7.setInt(2, var23.getSkillId());
                        var7.setInt(3, var23.getSkillLevel());
                        var7.setInt(4, -1);
                        var7.setLong(5, var23.getReuse());
                        var7.setLong(6, var23.getStamp());
                        var7.setInt(7, 1);
                        var7.setInt(8, this._classIndex);
                        var7.setInt(9, ++var19);
                        var7.addBatch();
                     }
                  }
               }

               var7.executeBatch();
            }
         } catch (Exception var18) {
            LOGGER.log(Level.WARNING, "Could not store char effect data: ", (Throwable)var18);
         }
      }
   }

   private void storeItemReuseDelay() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("DELETE FROM character_item_reuse_save WHERE charId=?");
         PreparedStatement var3 = var1.prepareStatement("INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)");
      ) {
         var2.setInt(1, this.getObjectId());
         var2.execute();
         long var4 = System.currentTimeMillis();

         for (TimeStamp var7 : this.getItemReuseTimeStamps().values()) {
            if (var7 != null && var4 < var7.getStamp()) {
               var3.setInt(1, this.getObjectId());
               var3.setInt(2, var7.getItemId());
               var3.setInt(3, var7.getItemObjectId());
               var3.setLong(4, var7.getReuse());
               var3.setLong(5, var7.getStamp());
               var3.addBatch();
            }
         }

         var3.executeBatch();
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Could not store char item reuse data: ", (Throwable)var14);
      }
   }

   public boolean isOnline() {
      return this._isOnline;
   }

   public int isOnlineInt() {
      if (this._isOnline && this._client != null) {
         return this._client.isDetached() ? 2 : 1;
      } else {
         return 0;
      }
   }

   public void startOfflinePlay() {
      if (this.hasPremiumStatus()
         && DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP > 0
         && !AntiFeedManager.getInstance().tryAddPlayer(4, this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP)) {
         String var2 = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
         this.sendMessage("Only " + var2 + " offline players allowed per IP.");
      } else if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP > 0
         && !AntiFeedManager.getInstance().tryAddPlayer(4, this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP)) {
         String var1 = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
         this.sendMessage("Only " + var1 + " offline players allowed per IP.");
      } else {
         AntiFeedManager.getInstance().removePlayer(0, this);
         this.sendPacket(LeaveWorld.STATIC_PACKET);
         if (OfflinePlayConfig.OFFLINE_PLAY_SET_NAME_COLOR) {
            this.getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
         }

         if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty()) {
            this.startAbnormalVisualEffect(
               true,
               new AbnormalVisualEffect[]{
                  (AbnormalVisualEffect)OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size()))
               }
            );
         }

         this.broadcastUserInfo();
         this._offlinePlay = true;
         this._client.setDetached(true);
         if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS) {
            OfflinePlayTable.getInstance().storeOfflinePlay(this);
         }
      }
   }

   public boolean isOfflinePlay() {
      return this._offlinePlay;
   }

   public void setOfflinePlay(boolean var1) {
      this._offlinePlay = var1;
   }

   public void setEnteredWorld() {
      this._enteredWorld = true;
   }

   public boolean hasEnteredWorld() {
      return this._enteredWorld;
   }

   public boolean isInOfflineMode() {
      return this._client == null || this._client.isDetached();
   }

   public boolean isIn7sDungeon() {
      return this._isIn7sDungeon;
   }

   public Skill addSkill(Skill var1) {
      this.addCustomSkill(var1);
      return super.addSkill(var1);
   }

   public Skill addSkill(Skill var1, boolean var2) {
      Skill var3 = this.addSkill(var1);
      if (var2) {
         this.storeSkill(var1, var3, -1);
      }

      return var3;
   }

   public Skill removeSkill(Skill var1, boolean var2) {
      this.removeCustomSkill(var1);
      return var2 ? this.removeSkill(var1) : super.removeSkill(var1, true);
   }

   public Skill removeSkill(Skill var1, boolean var2, boolean var3) {
      this.removeCustomSkill(var1);
      return var2 ? this.removeSkill(var1) : super.removeSkill(var1, var3);
   }

   public Skill removeSkill(Skill var1) {
      this.removeCustomSkill(var1);
      Skill var2 = super.removeSkill(var1, true);
      if (var2 != null) {
         try (
            Connection var3 = DatabaseFactory.getConnection();
            PreparedStatement var4 = var3.prepareStatement("DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?");
         ) {
            var4.setInt(1, var2.getId());
            var4.setInt(2, this.getObjectId());
            var4.setInt(3, this._classIndex);
            var4.execute();
         } catch (Exception var11) {
            LOGGER.log(Level.WARNING, "Error could not delete skill: " + var11.getMessage(), (Throwable)var11);
         }
      }

      if (this.getTransformationId() <= 0 && !this.isCursedWeaponEquipped()) {
         if (var1 != null) {
            for (Shortcut var13 : this._shortcuts.getAllShortcuts()) {
               if (var13 != null && var13.getId() == var1.getId() && var13.getType() == ShortcutType.SKILL && (var1.getId() < 3080 || var1.getId() > 3259)) {
                  this.deleteShortcut(var13.getSlot(), var13.getPage());
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private void storeSkill(Skill var1, Skill var2, int var3) {
      int var4 = var3 > -1 ? var3 : this._classIndex;

      try (Connection var5 = DatabaseFactory.getConnection()) {
         if (var2 != null && var1 != null) {
            try (PreparedStatement var16 = var5.prepareStatement("UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?")) {
               var16.setInt(1, var1.getLevel());
               var16.setInt(2, var2.getId());
               var16.setInt(3, this.getObjectId());
               var16.setInt(4, var4);
               var16.execute();
            }
         } else if (var1 != null) {
            try (PreparedStatement var6 = var5.prepareStatement("REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)")) {
               var6.setInt(1, this.getObjectId());
               var6.setInt(2, var1.getId());
               var6.setInt(3, var1.getLevel());
               var6.setInt(4, var4);
               var6.execute();
            }
         }
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Error could not store char skills: " + var15.getMessage(), (Throwable)var15);
      }
   }

   private void storeSkills(List<Skill> var1, int var2) {
      if (!var1.isEmpty()) {
         int var3 = var2 > -1 ? var2 : this._classIndex;

         try (
            Connection var4 = DatabaseFactory.getConnection();
            PreparedStatement var5 = var4.prepareStatement("REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)");
         ) {
            for (Skill var7 : var1) {
               var5.setInt(1, this.getObjectId());
               var5.setInt(2, var7.getId());
               var5.setInt(3, var7.getLevel());
               var5.setInt(4, var3);
               var5.addBatch();
            }

            var5.executeBatch();
         } catch (SQLException var12) {
            LOGGER.log(Level.WARNING, "Error could not store char skills: " + var12.getMessage(), (Throwable)var12);
         }
      }
   }

   private void restoreSkills() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?");
      ) {
         var2.setInt(1, this.getObjectId());
         var2.setInt(2, this._classIndex);

         try (ResultSet var3 = var2.executeQuery()) {
            while (var3.next()) {
               int var4 = var3.getInt("skill_id");
               int var5 = var3.getInt("skill_level");
               Skill var6 = SkillData.getInstance().getSkill(var4, var5);
               if (var6 == null) {
                  LOGGER.warning("Skipped null skill Id: " + var4 + " Level: " + var5 + " while restoring player skills for playerObjId: " + this.getObjectId());
               } else {
                  this.addSkill(var6);
                  if (GeneralConfig.SKILL_CHECK_ENABLE
                     && (!this.isGM() || GeneralConfig.SKILL_CHECK_GM)
                     && !SkillTreeData.getInstance().isSkillAllowed(this, var6)) {
                     PunishmentManager.handleIllegalPlayerAction(
                        this,
                        "Player "
                           + this.getName()
                           + " has invalid skill "
                           + var6.getName()
                           + " ("
                           + var6.getId()
                           + "/"
                           + var6.getLevel()
                           + "), class:"
                           + ClassListData.getInstance().getClass(this.getPlayerClass()).getClassName(),
                        IllegalActionPunishmentType.BROADCAST
                     );
                     if (GeneralConfig.SKILL_CHECK_REMOVE) {
                        this.removeSkill(var6);
                     }
                  }
               }
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Could not restore character " + this + " skills: " + var13.getMessage(), (Throwable)var13);
      }

      Skill var14 = SkillData.getInstance().getSkill(655, 1);
      if (var14 != null && this.getKnownSkill(655) == null) {
         this.addSkill(var14);
      }
   }

   public void restoreEffects() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement(
            "SELECT skill_id,skill_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC"
         );
      ) {
         var2.setInt(1, this.getObjectId());
         var2.setInt(2, this._classIndex);

         try (ResultSet var3 = var2.executeQuery()) {
            long var4 = System.currentTimeMillis();

            while (var3.next()) {
               Skill var6 = SkillData.getInstance().getSkill(var3.getInt("skill_id"), var3.getInt("skill_level"));
               if (var6 != null) {
                  long var7 = var3.getLong("systime");
                  long var9 = var7 - var4;
                  if (var9 > 10L) {
                     this.disableSkill(var6, var9);
                     this.addTimeStamp(var6, var3.getLong("reuse_delay"), var7);
                  }

                  if (var3.getInt("restore_type") <= 0) {
                     var6.applyEffects(this, this, false, var3.getInt("remaining_time"));
                  }
               }
            }
         }

         try (PreparedStatement var20 = var1.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?")) {
            var20.setInt(1, this.getObjectId());
            var20.setInt(2, this._classIndex);
            var20.executeUpdate();
         }
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + var19.getMessage(), (Throwable)var19);
      }
   }

   private void restoreItemReuse() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?");
         PreparedStatement var3 = var1.prepareStatement("DELETE FROM character_item_reuse_save WHERE charId=?");
      ) {
         var2.setInt(1, this.getObjectId());

         try (ResultSet var4 = var2.executeQuery()) {
            long var5 = System.currentTimeMillis();

            while (var4.next()) {
               int var7 = var4.getInt("itemId");
               long var8 = var4.getLong("reuseDelay");
               long var10 = var4.getLong("systime");
               boolean var12 = true;
               Item var13 = this._inventory.getItemByItemId(var7);
               if (var13 == null) {
                  var13 = this.getWarehouse().getItemByItemId(var7);
                  var12 = false;
               }

               if (var13 != null && var13.getId() == var7 && var13.getReuseDelay() > 0) {
                  long var14 = var10 - var5;
                  if (var14 > 10L) {
                     this.addTimeStampItem(var13, var8, var10);
                     if (var12 && var13.isEtcItem()) {
                        int var16 = var13.getSharedReuseGroup();
                        if (var16 > 0) {
                           this.sendPacket(new ExUseSharedGroupItem(var7, var16, (int)var14, (int)var8));
                        }
                     }
                  }
               }
            }
         }

         var3.setInt(1, this.getObjectId());
         var3.executeUpdate();
      } catch (Exception var25) {
         LOGGER.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + var25.getMessage(), (Throwable)var25);
      }
   }

   private void restoreHenna() {
      for (int var1 = 0; var1 < 3; var1++) {
         this._henna[var1] = null;
      }

      try (
         Connection var13 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var13.prepareStatement("SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?");
      ) {
         var2.setInt(1, this.getObjectId());
         var2.setInt(2, this._classIndex);

         try (ResultSet var3 = var2.executeQuery()) {
            while (var3.next()) {
               int var4 = var3.getInt("slot");
               if (var4 >= 1 && var4 <= 3) {
                  int var5 = var3.getInt("symbol_id");
                  if (var5 != 0) {
                     this._henna[var4 - 1] = HennaData.getInstance().getHenna(var5);
                  }
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Failed restoing character " + this + " hennas.", (Throwable)var12);
      }

      this.recalcHennaStats();
   }

   public int getHennaEmptySlots() {
      int var1 = 0;
      if (this.getPlayerClass().level() == 1) {
         var1 = 2;
      } else if (this.getPlayerClass().level() > 1) {
         var1 = 3;
      }

      for (int var2 = 0; var2 < 3; var2++) {
         if (this._henna[var2] != null) {
            var1--;
         }
      }

      return var1 <= 0 ? 0 : var1;
   }

   public boolean removeHenna(int var1) {
      if (var1 >= 1 && var1 <= 3) {
         Henna var2 = this._henna[var1 - 1];
         if (var2 == null) {
            return false;
         } else {
            this._henna[var1 - 1] = null;

            try (
               Connection var3 = DatabaseFactory.getConnection();
               PreparedStatement var4 = var3.prepareStatement("DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?");
            ) {
               var4.setInt(1, this.getObjectId());
               var4.setInt(2, var1);
               var4.setInt(3, this._classIndex);
               var4.execute();
            } catch (Exception var11) {
               LOGGER.log(Level.SEVERE, "Failed removing character henna.", (Throwable)var11);
            }

            this.recalcHennaStats();
            this.sendPacket(new HennaInfo(this));
            this.updateUserInfo();
            this._inventory.addItem(ItemProcessType.RESTORE, var2.getDyeItemId(), var2.getCancelCount(), this, null);
            this.reduceAdena(ItemProcessType.FEE, var2.getCancelFee(), this, false);
            SystemMessage var12 = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
            var12.addItemName(var2.getDyeItemId());
            var12.addLong(var2.getCancelCount());
            this.sendPacket(var12);
            this.sendPacket(SystemMessageId.THE_SYMBOL_HAS_BEEN_DELETED);
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, var2), this);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean addHenna(Henna var1) {
      for (int var2 = 0; var2 < 3; var2++) {
         if (this._henna[var2] == null) {
            this._henna[var2] = var1;
            this.recalcHennaStats();

            try (
               Connection var3 = DatabaseFactory.getConnection();
               PreparedStatement var4 = var3.prepareStatement("INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)");
            ) {
               var4.setInt(1, this.getObjectId());
               var4.setInt(2, var1.getDyeId());
               var4.setInt(3, var2 + 1);
               var4.setInt(4, this._classIndex);
               var4.execute();
            } catch (Exception var11) {
               LOGGER.log(Level.SEVERE, "Failed saving character henna.", (Throwable)var11);
            }

            this.sendPacket(new HennaInfo(this));
            this.updateUserInfo();
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, var1), this);
            }

            return true;
         }
      }

      return false;
   }

   private void recalcHennaStats() {
      this._hennaINT = 0;
      this._hennaSTR = 0;
      this._hennaCON = 0;
      this._hennaMEN = 0;
      this._hennaWIT = 0;
      this._hennaDEX = 0;

      for (Henna var4 : this._henna) {
         if (var4 != null) {
            this._hennaINT = this._hennaINT + (this._hennaINT + var4.getStatINT() > 5 ? 5 - this._hennaINT : var4.getStatINT());
            this._hennaSTR = this._hennaSTR + (this._hennaSTR + var4.getStatSTR() > 5 ? 5 - this._hennaSTR : var4.getStatSTR());
            this._hennaMEN = this._hennaMEN + (this._hennaMEN + var4.getStatMEN() > 5 ? 5 - this._hennaMEN : var4.getStatMEN());
            this._hennaCON = this._hennaCON + (this._hennaCON + var4.getStatCON() > 5 ? 5 - this._hennaCON : var4.getStatCON());
            this._hennaWIT = this._hennaWIT + (this._hennaWIT + var4.getStatWIT() > 5 ? 5 - this._hennaWIT : var4.getStatWIT());
            this._hennaDEX = this._hennaDEX + (this._hennaDEX + var4.getStatDEX() > 5 ? 5 - this._hennaDEX : var4.getStatDEX());
         }
      }
   }

   public Henna getHenna(int var1) {
      return var1 >= 1 && var1 <= 3 ? this._henna[var1 - 1] : null;
   }

   public boolean hasHennas() {
      for (Henna var4 : this._henna) {
         if (var4 != null) {
            return true;
         }
      }

      return false;
   }

   public Henna[] getHennaList() {
      return this._henna;
   }

   public int getHennaStatINT() {
      return this._hennaINT;
   }

   public int getHennaStatSTR() {
      return this._hennaSTR;
   }

   public int getHennaStatCON() {
      return this._hennaCON;
   }

   public int getHennaStatMEN() {
      return this._hennaMEN;
   }

   public int getHennaStatWIT() {
      return this._hennaWIT;
   }

   public int getHennaStatDEX() {
      return this._hennaDEX;
   }

   public void autoSave() {
      this.storeMe();
      if (GeneralConfig.UPDATE_ITEMS_ON_CHAR_STORE) {
         this.getInventory().updateDatabase();
         this.getWarehouse().updateDatabase();
         this.getFreight().updateDatabase();
      }
   }

   public boolean canLogout() {
      if (this._subclassLock) {
         LOGGER.warning("Player " + this.getName() + " tried to restart/logout during class change.");
         return false;
      } else if (this._activeEnchantItemId == -1 && this._activeEnchantAttrItemId == -1) {
         if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) || this.isGM() && GeneralConfig.GM_RESTART_FIGHTING) {
            if (this.isRegisteredOnEvent()) {
               this.sendMessage("A superior power doesn't allow you to leave.");
               return false;
            } else {
               if (this.isFestivalParticipant()) {
                  if (SevenSignsFestival.getInstance().isFestivalInitialized()) {
                     this.sendMessage("You cannot log out while you are a participant in a Festival.");
                     return false;
                  }

                  if (this.isInParty()) {
                     this._party.broadcastPacket(new SystemMessage(this.getName() + " has been removed from the upcoming Festival."));
                  }
               }

               return true;
            }
         } else {
            this.sendPacket(SystemMessageId.YOU_CANNOT_EXIT_WHILE_IN_COMBAT);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isAutoAttackable(Creature var1) {
      if (var1 == null) {
         return false;
      } else if (this.isInvisible() && this.isGM()) {
         return false;
      } else if (var1 == this || var1 == this._summon) {
         return false;
      } else if (var1 instanceof FriendlyMob) {
         return false;
      } else if (var1.isMonster()) {
         return true;
      } else {
         Player var2 = var1.asPlayer();
         if (var1.isPlayable() && this._duelState == 1 && this.getDuelId() == var2.getDuelId()) {
            return true;
         } else if (this.isInParty() && this._party.getMembers().contains(var1)) {
            return false;
         } else if (var1.isPlayer() && var2.isInOlympiadMode()) {
            return this._inOlympiadMode && this._olympiadStart && var1.asPlayer().getOlympiadGameId() == this.getOlympiadGameId();
         } else if (this.isOnEvent()) {
            return this.isOnSoloEvent() || this.getTeam() != var1.getTeam();
         } else {
            if (var1.isPlayable()) {
               if (this.isInsideZone(ZoneId.PEACE) || this.isInsideZone(ZoneId.NO_PVP)) {
                  return false;
               }

               Clan var3 = this.getClan();
               Clan var4 = var2.getClan();
               if (var3 != null && var4 != null) {
                  if (var3 != var4) {
                     Siege var5 = SiegeManager.getInstance().getSiege(this.getX(), this.getY(), this.getZ());
                     if (var5 != null) {
                        if (var5.checkIsDefender(var4) && var5.checkIsDefender(var3)) {
                           return false;
                        }

                        if (var5.checkIsAttacker(var4) && var5.checkIsAttacker(var3)) {
                           Castle var6 = CastleManager.getInstance().getCastleById(this._siegeSide);
                           return var6 != null && var6.isFirstMidVictory();
                        }
                     }
                  }

                  if (this.getWantsPeace() == 0
                     && var2.getWantsPeace() == 0
                     && !this.isAcademyMember()
                     && var3.isAtWarWith(var2.getClanId())
                     && var4.isAtWarWith(this.getClanId())) {
                     return true;
                  }
               }

               if (this.isInsideZone(ZoneId.PVP) && var2.isInsideZone(ZoneId.PVP) && (!this.isInsideZone(ZoneId.SIEGE) || !var2.isInsideZone(ZoneId.SIEGE))) {
                  return true;
               }

               if (var3 != null && var3.isMember(var1.getObjectId())) {
                  return false;
               }

               if (var1.isPlayer() && this.getAllyId() != 0 && this.getAllyId() == var2.getAllyId()) {
                  return false;
               }

               if (this.isInsideZone(ZoneId.PVP) && var2.isInsideZone(ZoneId.PVP) && this.isInsideZone(ZoneId.SIEGE) && var2.isInsideZone(ZoneId.SIEGE)) {
                  return true;
               }

               if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && (this.isGood() && var2.isEvil() || this.isEvil() && var2.isGood())) {
                  return true;
               }
            }

            if (var1 instanceof Defender && this._clan != null) {
               Siege var7 = SiegeManager.getInstance().getSiege(this);
               return var7 != null && var7.checkIsAttacker(this._clan);
            } else if (!(var1 instanceof Guard)) {
               return this.getKarma() > 0 || this._pvpFlag > 0;
            } else {
               return !FactionSystemConfig.FACTION_SYSTEM_ENABLED
                     || !FactionSystemConfig.FACTION_GUARDS_ENABLED
                     || (!this._isGood || !var1.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME, new String[0]))
                        && (!this._isEvil || !var1.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME, new String[0]))
                  ? this.getKarma() > 0
                  : true;
            }
         }
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public boolean useMagic(Skill var1, boolean var2, boolean var3) {
      if (var1.isPassive()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this.isCastingNow()) {
         if (this._currentSkill != null && var1.getId() == this._currentSkill.getSkillId()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else if (this.isSkillDisabled(var1)) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else {
            this.setQueuedSkill(var1, var2, var3);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         }
      } else {
         this.setCastingNow(true);
         this.setCurrentSkill(var1, var2, var3);
         if (this._queuedSkill != null) {
            this.setQueuedSkill(null, false, false);
         }

         if (!this.checkUseMagicConditions(var1, var2, var3)) {
            this.setCastingNow(false);
            return false;
         } else {
            Object var4 = null;

            this.getAI()
               .setIntentionCast(
                  var1, (WorldObject)(switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$skill$targets$TargetType[var1.getTargetType().ordinal()]) {
                     case 1, 2, 3, 4, 5, 6, 7, 8 -> this;
                     default -> var1.getFirstOfTargetList(this);
                  })
               );
            return true;
         }
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private boolean checkUseMagicConditions(Skill var1, boolean var2, boolean var3) {
      if (this.isOutOfControl() || this.isParalyzed() || this.isStunned() || this.isSleeping()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this.isDead()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this._fishing && !var1.hasEffectType(EffectType.FISHING, EffectType.FISHING_START)) {
         this.sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_MAY_BE_USED_AT_THIS_TIME);
         return false;
      } else if (this._observerMode) {
         this.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
         this.abortCast();
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this._waitTypeSitting) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (var1.isToggle() && this.isAffectedBySkill(var1.getId())) {
         this.stopSkillEffects(SkillFinishType.REMOVED, var1.getId());
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this._isFakeDeath) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else {
         Object var4 = null;
         TargetType var5 = var1.getTargetType();
         if (var5 == TargetType.GROUND && this._currentSkillWorldPosition == null) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else {
            var4 = switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$skill$targets$TargetType[var5.ordinal()]) {
               case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 -> this;
               case 13, 14, 15 -> this._summon;
               default -> this.getTarget();
            };
            if (var4 == null) {
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else {
               if (var4.isDoor()) {
                  Door var6 = var4.asDoor();
                  if (var6.isInsideZone(ZoneId.SIEGE)) {
                     Castle var7 = var6.getCastle();
                     if (var7 != null && var7.getResidenceId() > 0 && !var7.getSiege().isInProgress()) {
                        this.sendPacket(SystemMessageId.INVALID_TARGET);
                        return false;
                     }

                     Fort var8 = var6.getFort();
                     if (var8 != null && var8.getResidenceId() > 0 && (!var8.getSiege().isInProgress() || !var6.isShowHp())) {
                        this.sendPacket(SystemMessageId.INVALID_TARGET);
                        return false;
                     }
                  } else if (var1.getTargetType() != TargetType.UNLOCKABLE) {
                     this.sendPacket(SystemMessageId.INVALID_TARGET);
                     return false;
                  }
               }

               if (this._isInDuel && var4.isPlayable()) {
                  Player var12 = var4.asPlayer();
                  if (var12.getDuelId() != this.getDuelId()) {
                     this.sendMessage("You cannot do this while duelling.");
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return false;
                  }
               }

               if (this.isSkillDisabled(var1)) {
                  SystemMessage var13;
                  if (this.hasSkillReuse(var1.getReuseHashCode())) {
                     int var14 = (int)(this.getSkillRemainingReuseTime(var1.getReuseHashCode()) / 1000L);
                     int var15 = var14 / 3600;
                     int var9 = var14 % 3600 / 60;
                     int var10 = var14 % 60;
                     if (var15 > 0) {
                        var13 = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        var13.addSkillName(var1);
                        var13.addInt(var15);
                        var13.addInt(var9);
                     } else if (var9 > 0) {
                        var13 = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        var13.addSkillName(var1);
                        var13.addInt(var9);
                     } else {
                        var13 = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        var13.addSkillName(var1);
                     }

                     var13.addInt(var10);
                  } else {
                     var13 = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
                     var13.addSkillName(var1);
                  }

                  this.sendPacket(var13);
                  return false;
               } else if (!var1.checkCondition(this, (WorldObject)var4, false)) {
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return false;
               } else {
                  if (var1.hasNegativeEffect()) {
                     if (this.isInsidePeaceZone(this, (WorldObject)var4) && !this.getAccessLevel().allowPeaceAttack()) {
                        this.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (this._inOlympiadMode && !this._olympiadStart) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (this.isSiegeFriend((WorldObject)var4)) {
                        if (TerritoryWarManager.getInstance().isTWInProgress()) {
                           this.sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
                        } else {
                           this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
                        }

                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (!var4.canBeAttacked() && !this.getAccessLevel().allowPeaceAttack() && !var4.isDoor()) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (var4 instanceof EventMonster && ((EventMonster)var4).eventSkillAttackBlocked()) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (!var4.isAutoAttackable(this) && !var2) {
                        switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$skill$targets$TargetType[var5.ordinal()]) {
                           case 1:
                           case 2:
                           case 3:
                           case 4:
                           case 5:
                           case 6:
                           case 8:
                           case 9:
                           case 10:
                           case 12:
                           case 16:
                              break;
                           case 7:
                           case 11:
                           case 13:
                           case 14:
                           case 15:
                           default:
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                        }
                     }

                     if (var3) {
                        if (var5 == TargetType.GROUND) {
                           if (!this.isInsideRadius2D(
                              this._currentSkillWorldPosition.getX(),
                              this._currentSkillWorldPosition.getY(),
                              this._currentSkillWorldPosition.getZ(),
                              var1.getCastRange() + this.getTemplate().getCollisionRadius()
                           )) {
                              this.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                           }
                        } else if (var1.getCastRange() > 0
                           && !this.isInsideRadius2D((ILocational)var4, var1.getCastRange() + this.getTemplate().getCollisionRadius())) {
                           this.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
                           this.sendPacket(ActionFailed.STATIC_PACKET);
                           return false;
                        }
                     }
                  }

                  if (var1.getEffectPoint() > 0 && var4.isMonster() && !var2) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return false;
                  } else {
                     switch (1.$SwitchMap$org$l2jmobius$gameserver$mechanics$skill$targets$TargetType[var5.ordinal()]) {
                        case 6:
                        case 7:
                        case 8:
                        default:
                           if (var4.isPlayable() && !this.getAccessLevel().allowPeaceAttack() && !this.checkPvpSkill((WorldObject)var4, var1)) {
                              this.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                           }
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                           if (var1.getCastRange() > 0) {
                              if (var5 == TargetType.GROUND) {
                                 if (!GeoEngine.getInstance().canSeeTarget(this, this._currentSkillWorldPosition)) {
                                    this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                                    this.sendPacket(ActionFailed.STATIC_PACKET);
                                    return false;
                                 }
                              } else if (!GeoEngine.getInstance().canSeeTarget(this, (WorldObject)var4)) {
                                 this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                                 this.sendPacket(ActionFailed.STATIC_PACKET);
                                 return false;
                              }
                           }

                           if (var1.getFlyType() == FlyType.CHARGE
                              && !GeoEngine.getInstance()
                                 .canMoveToTarget(this.getX(), this.getY(), this.getZ(), var4.getX(), var4.getY(), var4.getZ(), this.getInstanceId())) {
                              this.sendPacket(SystemMessageId.THE_TARGET_IS_LOCATED_WHERE_YOU_CANNOT_CHARGE);
                              return false;
                           } else {
                              return true;
                           }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isInLooterParty(int var1) {
      Player var2 = World.getPlayer(var1);
      return this.isInParty() && this._party.isInCommandChannel() && var2 != null
         ? this._party.getCommandChannel().getMembers().contains(var2)
         : this.isInParty() && var2 != null && this._party.getMembers().contains(var2);
   }

   public boolean checkPvpSkill(WorldObject var1, Skill var2) {
      if (var2 == null || var1 == null) {
         return false;
      } else if (!var1.isPlayable()) {
         return true;
      } else if (!var2.isDebuff() && !var2.hasEffectType(EffectType.STEAL_ABNORMAL) && !var2.hasNegativeEffect()) {
         return true;
      } else {
         Player var3 = var1.asPlayer();
         if (var3 != null && this != var1) {
            if (this.isInDuel() && var3.isInDuel() && this.getDuelId() == var3.getDuelId()) {
               return true;
            } else if (this.isInOlympiadMode() && var3.isInOlympiadMode() && this.getOlympiadGameId() == var3.getOlympiadGameId()) {
               return true;
            } else if (var1.isInsideZone(ZoneId.PEACE)) {
               return false;
            } else if (var2.isPvPOnly() && var3.getPvpFlag() == 0 && var3.getKarma() == 0 && !this.isInsideZone(ZoneId.PVP) && !this.isInsideZone(ZoneId.SIEGE)
               )
             {
               return false;
            } else if (this.isSiegeFriend(var3)) {
               this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
               return false;
            } else {
               boolean var4 = this._currentSkill != null && this._currentSkill.isCtrlPressed();
               if (!this.isInParty()
                  || !var3.isInParty()
                  || this.getParty().getLeader() != var3.getParty().getLeader()
                     && (this._party.getCommandChannel() == null || !this._party.getCommandChannel().containsPlayer(var3))) {
                  if (this.isInsideZone(ZoneId.PVP) && var3.isInsideZone(ZoneId.PVP)) {
                     return true;
                  } else {
                     Clan var5 = var3.getClan();
                     if (this._clan != null && var5 != null) {
                        if (this._clan.isAtWarWith(var5.getId()) && var5.isAtWarWith(this._clan.getId())) {
                           return true;
                        }

                        if (this.getClanId() == var3.getClanId() || this.getAllyId() > 0 && this.getAllyId() == var3.getAllyId()) {
                           return var2.getEffectRange() > 0 && var4 && this.getTarget() == var1 && var2.isDamage();
                        }
                     }

                     return var3.getPvpFlag() == 0 && var3.getKarma() == 0
                        ? var2.getEffectRange() > 0 && var4 && this.getTarget() == var1 && var2.isDamage()
                        : var3.getPvpFlag() > 0 || var3.getKarma() > 0;
                  }
               } else {
                  return var2.getEffectRange() > 0 && var4 && this.getTarget() == var1 && var2.isDamage();
               }
            }
         } else {
            return false;
         }
      }
   }

   public boolean isMageClass() {
      return this.getPlayerClass().isMage();
   }

   public boolean isMounted() {
      return this._mountType != MountType.NONE;
   }

   public boolean checkLandingState() {
      return this.isInsideZone(ZoneId.NO_LANDING)
         ? true
         : this.isInsideZone(ZoneId.SIEGE)
            && (
               this.getClan() == null
                  || CastleManager.getInstance().getCastle(this) != CastleManager.getInstance().getCastleByOwner(this.getClan())
                  || this != this.getClan().getLeader().getPlayer()
            );
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void setMount(int var1, int var2) {
      MountType var3 = MountType.findByNpcId(var1);
      switch (1.$SwitchMap$org$l2jmobius$gameserver$entity$actor$enums$player$MountType[var3.ordinal()]) {
         case 1:
            if (this.isFlying()) {
               this.removeSkill(CommonSkill.WYVERN_BREATH.getSkill().getId(), false);
               this.setFlying(false);
               this.sendSkillList();
            }
            break;
         case 2:
            this.setFlying(true);
            this.addSkill(CommonSkill.WYVERN_BREATH.getSkill(), false);
            this.sendSkillList();
      }

      this._mountType = var3;
      this._mountNpcId = var1;
      this._mountLevel = var2;
   }

   public MountType getMountType() {
      return this._mountType;
   }

   public void stopAllEffects() {
      super.stopAllEffects();
      this.updateAndBroadcastStatus();
   }

   public void stopAllEffectsExceptThoseThatLastThroughDeath() {
      super.stopAllEffectsExceptThoseThatLastThroughDeath();
      this.updateAndBroadcastStatus();
   }

   public void stopAllEffectsNotStayOnSubclassChange() {
      this.getEffectList().stopAllEffectsNotStayOnSubclassChange();
      this.updateAndBroadcastStatus();
   }

   public void stopCubics() {
      if (!this._cubics.isEmpty()) {
         for (Cubic var2 : this._cubics.values()) {
            var2.stopAction();
            var2.cancelDisappear();
         }

         this._cubics.clear();
         this._cubicOrder.clear();
         this.broadcastUserInfo();
      }
   }

   public void stopCubicsByOthers() {
      if (!this._cubics.isEmpty()) {
         boolean var1 = false;

         for (Cubic var3 : this._cubics.values()) {
            if (var3.givenByOther()) {
               this.removeCubic(var3.getId());
               var1 = true;
            }
         }

         if (var1) {
            this.broadcastUserInfo();
         }
      }
   }

   public void updateAbnormalEffect() {
      this.broadcastUserInfo();
   }

   public void setInventoryBlockingStatus(boolean var1) {
      this._inventoryDisable = var1;
      if (var1) {
         ThreadPool.schedule(new InventoryEnableTask(this), 1500L);
      }
   }

   public boolean isInventoryDisabled() {
      return this._inventoryDisable;
   }

   public Cubic addCubic(int var1, int var2, double var3, int var5, int var6, int var7, int var8, boolean var9) {
      Cubic var10 = new Cubic(this, var1, var2, (int)var3, var5, var6, var7, var8, var9);
      Cubic var11 = this._cubics.put(var1, var10);
      this._cubicOrder.remove(var1);
      this._cubicOrder.add(var1);
      return var11;
   }

   public Cubic removeCubic(int var1) {
      Cubic var2 = this._cubics.remove(var1);
      this._cubicOrder.remove(var1);
      if (var2 != null) {
         var2.stopAction();
         var2.cancelDisappear();
      }

      return var2;
   }

   public Cubic removeFirstCubic() {
      while (!this._cubicOrder.isEmpty()) {
         Integer var1 = this._cubicOrder.poll();
         if (var1 == null) {
            return null;
         }

         Cubic var2 = this._cubics.remove(var1);
         if (var2 != null) {
            var2.stopAction();
            var2.cancelDisappear();
            return var2;
         }
      }

      return null;
   }

   public Map<Integer, Cubic> getCubics() {
      return Collections.unmodifiableMap(this._cubics);
   }

   public Cubic getCubicById(int var1) {
      return this._cubics.get(var1);
   }

   public int getEnchantEffect() {
      Item var1 = this.getActiveWeaponInstance();
      return var1 == null ? 0 : Math.min(127, var1.getEnchantLevel());
   }

   public void setLastFolkNPC(Npc var1) {
      this._lastFolkNpc = var1;
   }

   public Npc getLastFolkNPC() {
      return this._lastFolkNpc;
   }

   public boolean isFestivalParticipant() {
      return SevenSignsFestival.getInstance().isParticipant(this);
   }

   public void addAutoSoulShot(int var1) {
      this._activeSoulShots.add(var1);
   }

   public boolean removeAutoSoulShot(int var1) {
      return this._activeSoulShots.remove(var1);
   }

   public Set<Integer> getAutoSoulShot() {
      return this._activeSoulShots;
   }

   public void rechargeShots(boolean var1, boolean var2) {
      if (this._activeSoulShots != null && !this._activeSoulShots.isEmpty()) {
         for (int var4 : this._activeSoulShots) {
            Item var5 = this._inventory.getItemByItemId(var4);
            if (var5 != null) {
               if (var2 && var5.getTemplate().getDefaultAction() == ActionType.SPIRITSHOT) {
                  IItemHandler var6 = ItemHandler.getInstance().getHandler(var5.getEtcItem());
                  if (var6 != null) {
                     var6.onItemUse(this, var5, false);
                  }
               }

               if (var1 && var5.getTemplate().getDefaultAction() == ActionType.SOULSHOT) {
                  IItemHandler var7 = ItemHandler.getInstance().getHandler(var5.getEtcItem());
                  if (var7 != null) {
                     var7.onItemUse(this, var5, false);
                  }
               }
            } else {
               this.removeAutoSoulShot(var4);
            }
         }
      }
   }

   public void disableAutoShotByCrystalType(int var1) {
      for (int var3 : this._activeSoulShots) {
         if (ItemData.getInstance().getTemplate(var3).getCrystalType().getLevel() == var1) {
            this.disableAutoShot(var3);
         }
      }
   }

   public boolean disableAutoShot(int var1) {
      if (this._activeSoulShots.contains(var1)) {
         this.removeAutoSoulShot(var1);
         this.sendPacket(new ExAutoSoulShot(var1, 0));
         SystemMessage var2 = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
         var2.addItemName(var1);
         this.sendPacket(var2);
         return true;
      } else {
         return false;
      }
   }

   public void disableAutoShotsAll() {
      for (int var2 : this._activeSoulShots) {
         this.sendPacket(new ExAutoSoulShot(var2, 0));
         SystemMessage var3 = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
         var3.addItemName(var2);
         this.sendPacket(var3);
      }

      this._activeSoulShots.clear();
   }

   public ClanPrivileges getClanPrivileges() {
      return this._clanPrivileges;
   }

   public void setClanPrivileges(ClanPrivileges var1) {
      this._clanPrivileges = var1.clone();
   }

   public boolean hasAccess(ClanAccess var1) {
      return this._clanPrivileges.hasMinimumPrivileges(var1, new ClanAccess[0]);
   }

   public void setPledgeClass(int var1) {
      this._pledgeClass = var1;
      this.checkItemRestriction();
   }

   public int getPledgeClass() {
      return this._pledgeClass;
   }

   public void setPledgeType(int var1) {
      this._pledgeType = var1;
   }

   public int getPledgeType() {
      return this._pledgeType;
   }

   public int getApprentice() {
      return this._apprentice;
   }

   public void setApprentice(int var1) {
      this._apprentice = var1;
   }

   public int getSponsor() {
      return this._sponsor;
   }

   public void setSponsor(int var1) {
      this._sponsor = var1;
   }

   public void sendMessage(String var1) {
      this.sendPacket(new SystemMessage(SendMessageLocalisationData.getLocalisation(this, var1)));
   }

   public void sendSysMessage(String var1) {
      if (GeneralConfig.GM_STARTUP_BUILDER_HIDE) {
         this.sendPacket(new CreatureSay(null, ChatType.GENERAL, "SYS", SendMessageLocalisationData.getLocalisation(this, var1)));
      } else {
         this.sendMessage(var1);
      }
   }

   public boolean setHiding(boolean var1) {
      if (!this.isGM()) {
         return false;
      } else {
         if (this.hasEnteredWorld()) {
            if (this.isInvisible() && var1) {
               return false;
            }

            if (!this.isInvisible() && !var1) {
               return false;
            }
         }

         this.setSilenceMode(var1);
         this.setInvul(var1);
         this.setInvisible(var1);
         if (var1 && this.hasEnteredWorld()) {
            World.forEachVisibleObject(this, Player.class, var1x -> {
               if (var1x.getTarget() == this) {
                  var1x.setTarget(null);
               }
            });
         }

         this.broadcastUserInfo();
         return true;
      }
   }

   public void enterObserverMode(Location var1) {
      this.setLastLocation();
      this.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
      this._observerMode = true;
      this.setTarget(null);
      this.setParalyzed(true);
      this.startParalyze();
      this.setInvul(true);
      this.setInvisible(true);
      this.sendPacket(new ObservationMode(var1));
      this.teleToLocation(var1, false);
      this.broadcastUserInfo();
   }

   public void setLastLocation() {
      this._lastLoc.setXYZ(this.getX(), this.getY(), this.getZ());
   }

   public void unsetLastLocation() {
      this._lastLoc.setXYZ(0, 0, 0);
   }

   public void enterOlympiadObserverMode(Location var1, int var2, boolean var3) {
      if (this.hasSummon()) {
         this._summon.unSummon(this);
      }

      this.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
      if (!this._cubics.isEmpty()) {
         for (Cubic var5 : this._cubics.values()) {
            var5.stopAction();
            var5.cancelDisappear();
         }

         this._cubics.clear();
         this._cubicOrder.clear();
      }

      if (this._party != null) {
         this._party.removePartyMember(this, PartyMessageType.EXPELLED);
      }

      this._olympiadGameId = var2;
      if (this._waitTypeSitting) {
         this.standUp();
      }

      if (var3) {
         this.setLastLocation();
      }

      this._observerMode = true;
      this.setTarget(null);
      this.setInvul(true);
      this.setInvisible(true);
      this.teleToLocation(var1, 0);
      this.sendPacket(new ExOlympiadMode(3));
      this.broadcastUserInfo();
   }

   public void leaveObserverMode() {
      this.setTarget(null);
      this.teleToLocation(this._lastLoc, false);
      this.unsetLastLocation();
      this.sendPacket(new ObservationReturn(this.getLocation()));
      this.setParalyzed(false);
      if (!this.isGM()) {
         this.setInvisible(false);
         this.setInvul(false);
      }

      if (this.hasAI()) {
         this.getAI().setIntentionIdle();
      }

      this.setFalling();
      this._observerMode = false;
      this.broadcastUserInfo();
   }

   public void leaveOlympiadObserverMode() {
      if (this._olympiadGameId != -1) {
         Olympiad.removeSpectator(this._olympiadGameId, this);
         this._olympiadGameId = -1;
         this._observerMode = false;
         this.setTarget(null);
         this.sendPacket(new ExOlympiadMode(0));
         this.setInstanceId(0);
         this.teleToLocation(this._lastLoc, true);
         if (!this.isGM()) {
            this.setInvisible(false);
            this.setInvul(false);
         }

         if (this.hasAI()) {
            this.getAI().setIntentionIdle();
         }

         this.unsetLastLocation();
         this.broadcastUserInfo();
      }
   }

   public void setOlympiadSide(int var1) {
      this._olympiadSide = var1;
   }

   public int getOlympiadSide() {
      return this._olympiadSide;
   }

   public void setOlympiadGameId(int var1) {
      this._olympiadGameId = var1;
   }

   public int getOlympiadGameId() {
      return this._olympiadGameId;
   }

   public int getOlympiadBuffCount() {
      return this._olyBuffsCount;
   }

   public void setOlympiadBuffCount(int var1) {
      this._olyBuffsCount = var1;
   }

   public Location getLastLocation() {
      return this._lastLoc;
   }

   public boolean inObserverMode() {
      return this._observerMode;
   }

   public int getTeleMode() {
      return this._telemode;
   }

   public void setTeleMode(int var1) {
      this._telemode = var1;
   }

   public void setLoto(int var1, int var2) {
      this._loto[var1] = var2;
   }

   public int getLoto(int var1) {
      return this._loto[var1];
   }

   public void setRaceTicket(int var1, int var2) {
      this._raceTickets[var1] = var2;
   }

   public int getRaceTicket(int var1) {
      return this._raceTickets[var1];
   }

   public boolean getMessageRefusal() {
      return this._messageRefusal;
   }

   public void setMessageRefusal(boolean var1) {
      this._messageRefusal = var1;
      this.sendPacket(new EtcStatusUpdate(this));
   }

   public void setDietMode(boolean var1) {
      this._dietMode = var1;
   }

   public boolean getDietMode() {
      return this._dietMode;
   }

   public void setTradeRefusal(boolean var1) {
      this._tradeRefusal = var1;
   }

   public boolean getTradeRefusal() {
      return this._tradeRefusal;
   }

   public void setExchangeRefusal(boolean var1) {
      this._exchangeRefusal = var1;
   }

   public boolean getExchangeRefusal() {
      return this._exchangeRefusal;
   }

   public BlockList getBlockList() {
      return this._blockList;
   }

   public void setHero(boolean var1) {
      if (var1 && this._baseClass == this._activeClass) {
         for (Skill var5 : SkillTreeData.getInstance().getHeroSkillTree().values()) {
            this.addSkill(var5, false);
         }
      } else {
         for (Skill var3 : SkillTreeData.getInstance().getHeroSkillTree().values()) {
            this.removeSkill(var3, false, true);
         }
      }

      this._hero = var1;
      this.sendSkillList();
   }

   public void setInOlympiadMode(boolean var1) {
      this._inOlympiadMode = var1;
   }

   public void setOlympiadStart(boolean var1) {
      this._olympiadStart = var1;
   }

   public boolean isOlympiadStart() {
      return this._olympiadStart;
   }

   public boolean isHero() {
      return this._hero;
   }

   public boolean isInOlympiadMode() {
      return this._inOlympiadMode;
   }

   public boolean isInDuel() {
      return this._isInDuel;
   }

   public void setStartingDuel() {
      this._startingDuel = true;
   }

   public int getDuelId() {
      return this._duelId;
   }

   public void setDuelState(int var1) {
      this._duelState = var1;
   }

   public int getDuelState() {
      return this._duelState;
   }

   public void setInDuel(int var1) {
      if (var1 > 0) {
         this._isInDuel = true;
         this._duelState = 1;
         this._duelId = var1;
      } else {
         if (this._duelState == 2) {
            this.enableAllSkills();
            this.getStatus().startHpMpRegeneration();
         }

         this._isInDuel = false;
         this._duelState = 0;
         this._duelId = 0;
      }

      this._startingDuel = false;
   }

   public SystemMessage getNoDuelReason() {
      SystemMessage var1 = new SystemMessage(this._noDuelReason);
      var1.addPcName(this);
      this._noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
      return var1;
   }

   public boolean canDuel() {
      if (!this.isInCombat() && !this.isJailed()) {
         if (this.isDead() || this.isAlikeDead() || this.getCurrentHp() < this.getMaxHp() / 2 || this.getCurrentMp() < this.getMaxMp() / 2) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_S_HP_OR_MP_IS_BELOW_50;
            return false;
         } else if (this._isInDuel || this._startingDuel) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL;
            return false;
         } else if (this._inOlympiadMode) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
            return false;
         } else if (this.isCursedWeaponEquipped()) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE;
            return false;
         } else if (this._privateStoreType != PrivateStoreType.NONE) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
            return false;
         } else if (this.isMounted() || this.isInBoat()) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_STEED_OR_STRIDER;
            return false;
         } else if (this._fishing) {
            this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING;
            return false;
         } else if (!this.isInsideZone(ZoneId.PVP) && !this.isInsideZone(ZoneId.PEACE) && !this.isInsideZone(ZoneId.SIEGE) && !this.isInsideZone(ZoneId.NO_PVP)
            )
          {
            return true;
         } else {
            this._noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLENGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA_PEACEFUL_ZONE_SEVEN_SIGNS_ZONE_NEAR_WATER_RESTART_PROHIBITED_AREA;
            return false;
         }
      } else {
         this._noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
         return false;
      }
   }

   public boolean isNoble() {
      return this._noble;
   }

   public void setNoble(boolean var1) {
      Collection var2 = SkillTreeData.getInstance().getNobleSkillTree().values();
      if (var1) {
         for (Skill var4 : var2) {
            this.addSkill(var4, false);
         }
      } else {
         for (Skill var6 : var2) {
            this.removeSkill(var6, false, true);
         }
      }

      this._noble = var1;
      this.sendSkillList();
   }

   public void setLvlJoinedAcademy(int var1) {
      this._lvlJoinedAcademy = var1;
   }

   public int getLvlJoinedAcademy() {
      return this._lvlJoinedAcademy;
   }

   public boolean isAcademyMember() {
      return this._lvlJoinedAcademy > 0;
   }

   public void setTeam(Team var1) {
      super.setTeam(var1);
      this.broadcastUserInfo();
      if (this.hasSummon()) {
         this._summon.broadcastStatusUpdate();
      }
   }

   public void setWantsPeace(int var1) {
      this._wantsPeace = var1;
   }

   public int getWantsPeace() {
      return this._wantsPeace;
   }

   public boolean isFishing() {
      return this._fishing;
   }

   public void setFishing(boolean var1) {
      this._fishing = var1;
   }

   public synchronized void sendSkillList() {
      if (this._skillListTask == null) {
         this._skillListTask = ThreadPool.schedule(
            () -> {
               boolean var1 = false;
               SkillList var2 = new SkillList();

               for (Skill var4 : this.getAllSkills()) {
                  if (var4 != null && (this._transformation == null || var4.isPassive()) && (!this.hasTransformSkill(var4.getId()) || !var4.isPassive())) {
                     if (this._clan != null) {
                        var1 = var4.isClanSkill() && this._clan.getReputationScore() < 0;
                     }

                     boolean var5 = SkillData.getInstance().isEnchantable(var4.getId());
                     if (var5) {
                        EnchantSkillLearn var6 = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(var4.getId());
                        if (var6 == null || var4.getLevel() < var6.getBaseLevel()) {
                           var5 = false;
                        }
                     }

                     var2.addSkill(var4.getDisplayId(), var4.getDisplayLevel(), var4.isPassive(), var1, var5);
                  }
               }

               if (this._transformation != null) {
                  TreeMap var7 = new TreeMap();

                  for (SkillHolder var12 : this._transformation.getTemplate(this).getSkills()) {
                     var7.putIfAbsent(var12.getSkillId(), var12.getSkillLevel());
                     if ((Integer)var7.get(var12.getSkillId()) < var12.getSkillLevel()) {
                        var7.put(var12.getSkillId(), var12.getSkillLevel());
                     }
                  }

                  for (AdditionalSkillHolder var13 : this._transformation.getTemplate(this).getAdditionalSkills()) {
                     if (this.getLevel() >= var13.getMinLevel()) {
                        var7.putIfAbsent(var13.getSkillId(), var13.getSkillLevel());
                        if ((Integer)var7.get(var13.getSkillId()) < var13.getSkillLevel()) {
                           var7.put(var13.getSkillId(), var13.getSkillLevel());
                        }
                     }
                  }

                  if (this._transformation.isFlying()) {
                     for (SkillLearn var14 : SkillTreeData.getInstance().getCollectSkillTree().values()) {
                        if (this.getKnownSkill(var14.getSkillId()) != null
                           && (!var7.containsKey(var14.getSkillId()) || (Integer)var7.get(var14.getSkillId()) < var14.getSkillLevel())) {
                           var7.put(var14.getSkillId(), var14.getSkillLevel());
                        }
                     }
                  }

                  for (Entry var15 : var7.entrySet()) {
                     Skill var16 = SkillData.getInstance().getSkill((Integer)var15.getKey(), (Integer)var15.getValue());
                     this.addTransformSkill(var16);
                     var2.addSkill((Integer)var15.getKey(), (Integer)var15.getValue(), false, false, false);
                  }
               }

               this.sendPacket(var2);
               this._skillListTask = null;
            },
            300L
         );
      }
   }

   public boolean addSubClass(int var1, int var2) {
      if (this._subclassLock) {
         return false;
      } else {
         this._subclassLock = true;

         try {
            if (this.getTotalSubClasses() == PlayerConfig.MAX_SUBCLASS || var2 == 0) {
               return false;
            } else if (this.getSubClasses().containsKey(var2)) {
               return false;
            } else {
               SubClassHolder var3 = new SubClassHolder();
               var3.setPlayerClass(var1);
               var3.setClassIndex(var2);

               try (
                  Connection var4 = DatabaseFactory.getConnection();
                  PreparedStatement var27 = var4.prepareStatement(
                     "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)"
                  );
               ) {
                  var27.setInt(1, this.getObjectId());
                  var27.setInt(2, var3.getId());
                  var27.setLong(3, var3.getExp());
                  var27.setLong(4, var3.getSp());
                  var27.setInt(5, var3.getLevel());
                  var27.setInt(6, var3.getClassIndex());
                  var27.execute();
               } catch (Exception var22) {
                  LOGGER.log(Level.WARNING, "WARNING: Could not add character sub class for " + this.getName() + ": " + var22.getMessage(), (Throwable)var22);
                  return false;
               }

               this.getSubClasses().put(var3.getClassIndex(), var3);
               PlayerClass var26 = PlayerClass.getPlayerClass(var1);
               Map var28 = SkillTreeData.getInstance().getCompleteClassSkillTree(var26);
               HashMap var6 = new HashMap();

               for (SkillLearn var8 : var28.values()) {
                  if (var8.getGetLevel() <= 40) {
                     Skill var9 = (Skill)var6.get(var8.getSkillId());
                     Skill var10 = SkillData.getInstance().getSkill(var8.getSkillId(), var8.getSkillLevel());
                     if (var9 == null || var9.getLevel() <= var10.getLevel()) {
                        var6.put(var10.getId(), var10);
                        this.storeSkill(var10, var9, var2);
                     }
                  }
               }

               return true;
            }
         } finally {
            this._subclassLock = false;
         }
      }
   }

   public boolean modifySubClass(int var1, int var2) {
      SubClassHolder var3 = this.getSubClasses().get(var1);
      if (var3 == null) {
         return false;
      } else {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(this, var3.getId()), this);
         }

         this.getSubClasses().remove(var1);

         try (
            Connection var4 = DatabaseFactory.getConnection();
            PreparedStatement var5 = var4.prepareStatement("DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?");
            PreparedStatement var6 = var4.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND class_index=?");
            PreparedStatement var7 = var4.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?");
            PreparedStatement var8 = var4.prepareStatement("DELETE FROM character_skills WHERE charId=? AND class_index=?");
            PreparedStatement var9 = var4.prepareStatement("DELETE FROM character_subclasses WHERE charId=? AND class_index=?");
         ) {
            for (int var10 = 1; var10 < 4; var10++) {
               Henna var11 = this.getHenna(var10);
               if (var11 != null) {
                  var5.setInt(1, this.getObjectId());
                  var5.setInt(2, var10);
                  var5.setInt(3, var1);
                  var5.execute();
               }
            }

            var6.setInt(1, this.getObjectId());
            var6.setInt(2, var1);
            var6.execute();
            var7.setInt(1, this.getObjectId());
            var7.setInt(2, var1);
            var7.execute();
            var8.setInt(1, this.getObjectId());
            var8.setInt(2, var1);
            var8.execute();
            var9.setInt(1, this.getObjectId());
            var9.setInt(2, var1);
            var9.execute();
         } catch (Exception var24) {
            LOGGER.log(
               Level.WARNING, "Could not modify sub class for " + this.getName() + " to class index " + var1 + ": " + var24.getMessage(), (Throwable)var24
            );
            return false;
         }

         return this.addSubClass(var2, var1);
      }
   }

   public boolean isSubClassActive() {
      return this._classIndex > 0;
   }

   public Map<Integer, SubClassHolder> getSubClasses() {
      return this._subClasses;
   }

   public int getTotalSubClasses() {
      return this.getSubClasses().size();
   }

   public int getBaseClass() {
      return this._baseClass;
   }

   public int getActiveClass() {
      return this._activeClass;
   }

   public int getClassIndex() {
      return this._classIndex;
   }

   private void setClassTemplate(int var1) {
      this._activeClass = var1;
      PlayerTemplate var2 = PlayerTemplateData.getInstance().getTemplate(var1);
      if (var2 == null) {
         LOGGER.severe("Missing template for classId: " + var1);
         throw new Error();
      } else {
         this.setTemplate(var2);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CHANGE, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionChange(this, var2, this.isSubClassActive()), this);
         }
      }
   }

   public void setActiveClass(int var1) {
      if (!this._subclassLock) {
         this._subclassLock = true;

         try {
            if (this._transformation == null) {
               for (Item var3 : this._inventory.getAugmentedItems()) {
                  if (var3 != null && var3.isEquipped()) {
                     var3.getAugmentation().removeBonus(this);
                  }
               }

               this.abortCast();
               if (this.isChannelized()) {
                  this.getSkillChannelized().abortChannelization();
               }

               this.store(PlayerConfig.SUBCLASS_STORE_SKILL_COOLTIME);
               this.resetTimeStamps();
               this._charges.set(0);
               this.stopChargeTask();
               if (this.hasServitor()) {
                  this._summon.unSummon(this);
               }

               if (var1 == 0) {
                  this.setClassTemplate(this._baseClass);
               } else {
                  try {
                     this.setClassTemplate(this.getSubClasses().get(var1).getId());
                  } catch (Exception var7) {
                     LOGGER.log(
                        Level.WARNING, "Could not switch " + this.getName() + "'s sub class to class index " + var1 + ": " + var7.getMessage(), (Throwable)var7
                     );
                     return;
                  }
               }

               this._classIndex = var1;
               this.setLearningClass(this.getPlayerClass());
               if (this.isInParty()) {
                  this._party.recalculatePartyLevel();
               }

               this._autoUseSettings.getAutoSkills().clear();
               this._autoUseSettings.getAutoBuffs().clear();

               for (Skill var11 : this.getAllSkills()) {
                  this.removeSkill(var11, false, true);
               }

               this.stopAllEffectsExceptThoseThatLastThroughDeath();
               this.stopAllEffectsNotStayOnSubclassChange();
               this.getEffectList().stopAllToggles();
               this.stopCubics();
               this.restoreRecipeBook(false);
               this.restoreDeathPenaltyBuffLevel();
               this.restoreSkills();
               this.rewardSkills();
               this.regiveTemporarySkills();
               this.getInventory().applyItemSkills();
               this.resetDisabledSkills();
               this.restoreEffects();
               this.sendPacket(new EtcStatusUpdate(this));
               QuestState var10 = this.getQuestState("Q00422_RepentYourSins");
               if (var10 != null) {
                  var10.exitQuest(true);
               }

               for (int var12 = 0; var12 < 3; var12++) {
                  this._henna[var12] = null;
               }

               this.restoreHenna();
               this.sendPacket(new HennaInfo(this));
               if (this.getCurrentHp() > this.getMaxHp()) {
                  this.setCurrentHp(this.getMaxHp());
               }

               if (this.getCurrentMp() > this.getMaxMp()) {
                  this.setCurrentMp(this.getMaxMp());
               }

               if (this.getCurrentCp() > this.getMaxCp()) {
                  this.setCurrentCp(this.getMaxCp());
               }

               this.refreshOverloaded();
               this.refreshExpertisePenalty();
               this.broadcastUserInfo();
               this.setExpBeforeDeath(0L);
               this._shortcuts.restoreMe();
               this.sendPacket(new ShortcutInit(this));
               this.broadcastPacket(new SocialAction(this.getObjectId(), 2122));
               this.sendPacket(new SkillCoolTime(this));
               this.sendPacket(new ExStorageMaxCount(this));
            }
         } finally {
            this._subclassLock = false;
         }
      }
   }

   public void stopWarnUserTakeBreak() {
      if (this._taskWarnUserTakeBreak != null) {
         this._taskWarnUserTakeBreak.cancel(true);
         this._taskWarnUserTakeBreak = null;
      }
   }

   public void startWarnUserTakeBreak() {
      if (this._taskWarnUserTakeBreak == null) {
         this._taskWarnUserTakeBreak = ThreadPool.scheduleAtFixedRate(new WarnUserTakeBreakTask(this), 7200000L, 7200000L);
      }
   }

   public void stopRentPet() {
      if (this._taskRentPet != null) {
         if (this.checkLandingState() && this._mountType == MountType.WYVERN) {
            this.teleToLocation(TeleportWhereType.TOWN);
         }

         if (this.dismount()) {
            this._taskRentPet.cancel(true);
            this._taskRentPet = null;
         }
      }
   }

   public void startRentPet(int var1) {
      if (this._taskRentPet == null) {
         this._taskRentPet = ThreadPool.scheduleAtFixedRate(new RentPetTask(this), var1 * 1000, var1 * 1000);
      }
   }

   public boolean isRentedPet() {
      return this._taskRentPet != null;
   }

   public void stopWaterTask() {
      if (this._taskWater != null) {
         this._taskWater.cancel(false);
         this._taskWater = null;
         this.sendPacket(new SetupGauge(this.getObjectId(), 2, 0));
      }
   }

   public void startWaterTask() {
      if (!this.isDead() && this._taskWater == null) {
         char var1 = '\uea60';
         double var2 = this.calcStat(Stat.BREATH, this.getBaseTemplate().getBaseBreath()) / 100.0;
         int var4 = (int)(60000.0 * var2);
         this.sendPacket(new SetupGauge(this.getObjectId(), 2, var4));
         this._taskWater = ThreadPool.scheduleAtFixedRate(new WaterTask(this), var4, 1000L);
      }
   }

   public boolean isInWater() {
      return this._taskWater != null;
   }

   public void checkWaterState() {
      if (this.isInsideZone(ZoneId.WATER)) {
         this.startWaterTask();
      } else {
         this.stopWaterTask();
      }
   }

   public void onPlayerEnter() {
      this.startWarnUserTakeBreak();
      if (!SevenSigns.getInstance().isSealValidationPeriod() && !SevenSigns.getInstance().isCompResultsPeriod()) {
         if (!this.isGM() && this.isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this.getObjectId()) == 0) {
            this.teleToLocation(TeleportWhereType.TOWN);
            this.setIn7sDungeon(false);
            this.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
         }
      } else if (!this.isGM()
         && this.isIn7sDungeon()
         && SevenSigns.getInstance().getPlayerCabal(this.getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()) {
         this.teleToLocation(TeleportWhereType.TOWN);
         this.setIn7sDungeon(false);
         this.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
      }

      if (this.isGM() && !GeneralConfig.GM_STARTUP_BUILDER_HIDE) {
         if (this.isInvul()) {
            this.sendMessage("Entering world in Invulnerable mode.");
         }

         if (this.isInvisible()) {
            this.sendMessage("Entering world in Invisible mode.");
         }

         if (this._silenceMode) {
            this.sendMessage("Entering world in Silence mode.");
         }
      }

      if (PlayerConfig.STORE_SKILL_COOLTIME) {
         this.restoreEffects();
      }

      this.revalidateZone(true);
      this.notifyFriends();
      if (!this.isGM() && PlayerConfig.DECREASE_SKILL_LEVEL) {
         this.checkPlayerSkills();
      }

      try {
         for (ZoneType var2 : ZoneManager.getInstance().getZones(this)) {
            var2.onPlayerLoginInside(this);
         }
      } catch (Exception var3) {
         LOGGER.log(Level.SEVERE, "", (Throwable)var3);
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGIN, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogin(this), this);
      }

      if (!this.isDead()) {
         ThreadPool.schedule(() -> {
            this.setCurrentHp(this._originalHp);
            this.setCurrentMp(this._originalMp);
            this.setCurrentCp(this._originalCp);
         }, 300L);
      }
   }

   public void fullRestore() {
      this.setCurrentHp(this.getMaxHp());
      this.setCurrentMp(this.getMaxMp());
      this.setCurrentCp(this.getMaxCp());
   }

   public long getLastAccess() {
      return this._lastAccess;
   }

   public void doRevive() {
      super.doRevive();
      DecayTaskManager.getInstance().cancel(this);
      this.updateEffectIcons();
      this.sendPacket(new EtcStatusUpdate(this));
      this._revivePet = false;
      this._reviveRequested = 0;
      this._revivePower = 0.0;
      if (this.isInsideZone(ZoneId.PEACE) && this._summon != null && !this._summon.isInsideZone(ZoneId.SIEGE)) {
         this._summon.teleToLocation(this.getLocation(), true);
      }

      if (this.isMounted()) {
         this.startFeed(this._mountNpcId);
      }

      if (this.isInParty()
         && this._party.isInDimensionalRift()
         && !DimensionalRiftManager.getInstance().checkIfInPeaceZone(this.getX(), this.getY(), this.getZ())) {
         this._party.getDimensionalRift().memberRessurected(this);
      }

      if (this.isInInstance()) {
         Instance var1 = InstanceManager.getInstance().getInstance(this.getInstanceId());
         if (var1 != null) {
            var1.cancelEjectDeadPlayer(this);
         }
      }
   }

   public void setName(String var1) {
      super.setName(var1);
      CharInfoTable.getInstance().addName(this);
   }

   public void doRevive(double var1) {
      this.doRevive();
      this.restoreExp(var1);
   }

   public void reviveRequest(Player var1, boolean var2, int var3) {
      if (!this.isResurrectionBlocked()) {
         if (this._reviveRequested == 1) {
            if (this._revivePet == var2) {
               var1.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED);
            } else if (var2) {
               var1.sendPacket(SystemMessageId.A_PET_CANNOT_BE_RESURRECTED_WHILE_IT_S_OWNER_IS_IN_THE_PROCESS_OF_RESURRECTING);
            } else {
               var1.sendPacket(SystemMessageId.WHILE_A_PET_IS_ATTEMPTING_TO_RESURRECT_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER);
            }
         } else if (var2 && this.hasPet() && this._summon.isDead() || !var2 && this.isDead()) {
            this._reviveRequested = 1;
            int var4 = 0;
            this._revivePower = Formulas.calculateSkillResurrectRestorePercent(var3, var1);
            var4 = (int)Math.round((this._expBeforeDeath - this.getExp()) * this._revivePower / 100.0);
            this._revivePet = var2;
            if (this._hasCharmOfCourage) {
               ConfirmDlg var7 = new ConfirmDlg(
                  SystemMessageId.RESURRECTION_IS_POSSIBLE_BECAUSE_OF_THE_COURAGE_CHARM_S_EFFECT_WOULD_YOU_LIKE_TO_RESURRECT_NOW.getId()
               );
               var7.addTime(60000);
               this.sendPacket(var7);
               return;
            }

            ConfirmDlg var5 = new ConfirmDlg(
               SystemMessageId.C1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_WITH_S2_EXPERIENCE_POINTS_DO_YOU_WANT_TO_BE_RESURRECTED.getId()
            );
            var5.getSystemMessage().addPcName(var1);
            var5.getSystemMessage().addString(Integer.toString(var4));
            this.sendPacket(var5);
         }
      }
   }

   public void reviveAnswer(int var1) {
      if (this._reviveRequested == 1 && (this.isDead() || this._revivePet) && (!this._revivePet || !this.hasPet() || this._summon.isDead())) {
         if (var1 == 1) {
            if (!this._revivePet) {
               if (this._revivePower != 0.0) {
                  this.doRevive(this._revivePower);
               } else {
                  this.doRevive();
               }
            } else if (this.hasPet()) {
               if (this._revivePower != 0.0) {
                  this._summon.doRevive(this._revivePower);
               } else {
                  this._summon.doRevive();
               }
            }
         }

         this._revivePet = false;
         this._reviveRequested = 0;
         this._revivePower = 0.0;
      }
   }

   public boolean isReviveRequested() {
      return this._reviveRequested == 1;
   }

   public boolean isRevivingPet() {
      return this._revivePet;
   }

   public void removeReviving() {
      this._reviveRequested = 0;
      this._revivePower = 0.0;
   }

   public void onActionRequest() {
      if (this.isSpawnProtected()) {
         this.setSpawnProtection(false);
         if (!this.isInsideZone(ZoneId.PEACE)) {
            this.sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
         }

         if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT && !this.hasSummon() && CharSummonTable.getInstance().getServitors().containsKey(this.getObjectId())) {
            CharSummonTable.getInstance().restoreServitor(this);
         }

         if (PlayerConfig.RESTORE_PET_ON_RECONNECT && !this.hasSummon() && CharSummonTable.getInstance().getPets().containsKey(this.getObjectId())) {
            CharSummonTable.getInstance().restorePet(this);
         }
      }

      if (this.isTeleportProtected()) {
         this.setTeleportProtection(false);
         if (!this.isInsideZone(ZoneId.PEACE)) {
            this.sendMessage("Teleport spawn protection ended.");
         }
      }
   }

   public int getExpertiseLevel() {
      return this.getSkillLevel(239);
   }

   public void teleToLocation(ILocational var1, boolean var2) {
      if (this._vehicle != null && !this._vehicle.isTeleporting()) {
         this.setVehicle(null);
      }

      if (this.isFlyingMounted() && var1.getZ() < -1005) {
         super.teleToLocation(var1.getX(), var1.getY(), -1005, var1.getHeading(), var1.getInstanceId());
      }

      super.teleToLocation(var1, var2);
   }

   public synchronized void onTeleported() {
      super.onTeleported();
      if (this.isInAirShip()) {
         this.getAirShip().sendInfo(this);
      } else {
         this.setLastServerPosition(this.getX(), this.getY(), this.getZ());
      }

      this.revalidateZone(true);
      this.checkItemRestriction();
      if (PlayerConfig.PLAYER_TELEPORT_PROTECTION > 0 && !this._inOlympiadMode) {
         this.setTeleportProtection(true);
      }

      if (this._tamedBeast != null) {
         for (TamedBeast var2 : this._tamedBeast) {
            var2.deleteMe();
         }

         this._tamedBeast.clear();
      }

      if (this._summon != null) {
         this._summon.setFollowStatus(false);
         this._summon.teleToLocation(this.getLocation(), false);
         ((SummonAI)this._summon.getAI()).setStartFollowController(true);
         this._summon.setFollowStatus(true);
         this._summon.updateAndBroadcastStatus(0);
      }

      if (this._movieHolder != null) {
         this.sendPacket(new ExStartScenePlayer(this._movieHolder.getMovie()));
      }

      if (AutoPlayConfig.ENABLE_AUTO_PLAY) {
         AutoPlayTaskManager.getInstance().stopAutoPlay(this);
         AutoUseTaskManager.getInstance().stopAutoUseTask(this);
      }

      this.broadcastInfo();
   }

   public void setTeleporting(boolean var1) {
      this.setTeleporting(var1, true);
   }

   public void setTeleporting(boolean var1, boolean var2) {
      super.setTeleporting(var1);
      if (var2) {
         if (var1) {
            if (this._teleportWatchdog == null && PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT > 0) {
               synchronized (this) {
                  this._teleportWatchdog = ThreadPool.schedule(new TeleportWatchdogTask(this), PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT * 1000);
               }
            }
         } else if (this._teleportWatchdog != null) {
            this._teleportWatchdog.cancel(false);
            this._teleportWatchdog = null;
         }
      }
   }

   public void setLastServerPosition(int var1, int var2, int var3) {
      this._lastServerPosition.setXYZ(var1, var2, var3);
   }

   public Location getLastServerPosition() {
      return this._lastServerPosition;
   }

   public void setBlinkActive(boolean var1) {
      this._blinkActive.set(var1);
   }

   public boolean isBlinkActive() {
      return this._blinkActive.get();
   }

   public synchronized void addExpAndSp(double var1, double var3) {
      this.getStat().addExpAndSp(var1, var3, false);
   }

   public synchronized void addExpAndSp(double var1, double var3, boolean var5) {
      this.getStat().addExpAndSp(var1, var3, var5);
   }

   public void removeExpAndSp(long var1, long var3) {
      this.getStat().removeExpAndSp(var1, var3, true);
   }

   public void removeExpAndSp(long var1, long var3, boolean var5) {
      this.getStat().removeExpAndSp(var1, var3, var5);
   }

   public void reduceCurrentHp(double var1, Creature var3, boolean var4, boolean var5, Skill var6) {
      if (var6 != null) {
         this.getStatus().reduceHp(var1, var3, var4, var5, var6.isToggle(), var6.getDmgDirectlyToHP());
      } else {
         this.getStatus().reduceHp(var1, var3, var4, var5, false, false);
      }

      if (this._tamedBeast != null) {
         for (TamedBeast var8 : this._tamedBeast) {
            var8.onOwnerGotAttacked(var3);
         }
      }
   }

   public void broadcastSnoop(ChatType var1, String var2, String var3, CreatureSay var4) {
      if (!this._snoopListener.isEmpty()) {
         Snoop var5 = new Snoop(this.getObjectId(), this.getName(), var1, var2, var3);

         for (Player var7 : this._snoopListener) {
            if (var7 != null) {
               var7.sendPacket(var4);
               var7.sendPacket(var5);
            }
         }
      }
   }

   public void addSnooper(Player var1) {
      this._snoopListener.add(var1);
   }

   public void removeSnooper(Player var1) {
      this._snoopListener.remove(var1);
   }

   public void addSnooped(Player var1) {
      this._snoopedPlayer.add(var1);
   }

   public void removeSnooped(Player var1) {
      this._snoopedPlayer.remove(var1);
   }

   public void addHtmlAction(HtmlActionScope var1, String var2) {
      this._htmlActionCaches[var1.ordinal()].add(var2);
   }

   public void clearHtmlActions(HtmlActionScope var1) {
      this._htmlActionCaches[var1.ordinal()].clear();
   }

   public void setHtmlActionOriginObjectId(HtmlActionScope var1, int var2) {
      if (var2 < 0) {
         throw new IllegalArgumentException();
      } else {
         this._htmlActionOriginObjectIds[var1.ordinal()] = var2;
      }
   }

   public int getLastHtmlActionOriginId() {
      return this._lastHtmlActionOriginObjId;
   }

   private boolean validateHtmlAction(Iterable<String> var1, String var2) {
      for (String var4 : var1) {
         if (var4.charAt(var4.length() - 1) == '$') {
            if (var2.startsWith(var4.substring(0, var4.length() - 1).trim())) {
               return true;
            }
         } else if (var4.equals(var2)) {
            return true;
         }
      }

      return false;
   }

   public int validateHtmlAction(String var1) {
      for (int var2 = 0; var2 < this._htmlActionCaches.length; var2++) {
         if (this.validateHtmlAction(this._htmlActionCaches[var2], var1)) {
            this._lastHtmlActionOriginObjId = this._htmlActionOriginObjectIds[var2];
            return this._lastHtmlActionOriginObjId;
         }
      }

      return -1;
   }

   public boolean validateItemManipulation(int var1, ItemProcessType var2) {
      Item var3 = this._inventory.getItemByObjectId(var1);
      if (var3 != null && var3.getOwnerId() == this.getObjectId()) {
         if ((!this.hasSummon() || this._summon.getControlObjectId() != var1) && this._mountObjectID != var1) {
            return this._activeEnchantItemId == var1 ? false : !CursedWeaponsManager.getInstance().isCursed(var3.getId());
         } else {
            return false;
         }
      } else {
         LOGGER.finest(this.getObjectId() + ": player tried to " + var2 + " item he is not owner of.");
         return false;
      }
   }

   public boolean isInBoat() {
      return this._vehicle != null && this._vehicle.isBoat();
   }

   public Boat getBoat() {
      return (Boat)this._vehicle;
   }

   public boolean isInAirShip() {
      return this._vehicle != null && this._vehicle.isAirShip();
   }

   public AirShip getAirShip() {
      return (AirShip)this._vehicle;
   }

   public Vehicle getVehicle() {
      return this._vehicle;
   }

   public void setVehicle(Vehicle var1) {
      if (var1 == null && this._vehicle != null) {
         this._vehicle.removePassenger(this);
      }

      this._vehicle = var1;
   }

   public boolean isInVehicle() {
      return this._vehicle != null;
   }

   public void setInCrystallize(boolean var1) {
      this._inCrystallize = var1;
   }

   public boolean isInCrystallize() {
      return this._inCrystallize;
   }

   public Location getInVehiclePosition() {
      return this._inVehiclePosition;
   }

   public void setInVehiclePosition(Location var1) {
      this._inVehiclePosition = var1;
   }

   public boolean deleteMe() {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGOUT, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogout(this), this);
      }

      try {
         for (ZoneType var2 : ZoneManager.getInstance().getZones(this)) {
            var2.onPlayerLogoutInside(this);
         }
      } catch (Exception var31) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var31);
      }

      try {
         if (!this._isOnline) {
            LOGGER.log(Level.SEVERE, "deleteMe() called on offline character " + this, (Throwable)(new RuntimeException()));
         }

         this.setOnlineStatus(false, true);
      } catch (Exception var30) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var30);
      }

      try {
         if (GeneralConfig.ENABLE_BLOCK_CHECKER_EVENT && this._handysBlockCheckerEventArena != -1) {
            HandysBlockCheckerManager.getInstance().onDisconnect(this);
         }
      } catch (Exception var29) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var29);
      }

      try {
         if (this._krateiArena != null) {
            this._krateiArena.removePlayer(this);
         }
      } catch (Exception var28) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var28);
      }

      try {
         this._isOnline = false;
         this._offlinePlay = false;
         this.abortAttack();
         this.abortCast();
         this.stopMove(null);
      } catch (Exception var27) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var27);
      }

      try {
         if (this._inventory.getItemByItemId(9819) != null) {
            Fort var32 = FortManager.getInstance().getFort(this);
            if (var32 != null) {
               FortSiegeManager.getInstance().dropCombatFlag(this, var32.getResidenceId());
            } else {
               BodyPart var35 = BodyPart.fromItem(this._inventory.getItemByItemId(9819));
               this._inventory.unEquipItemInBodySlot(var35);
               this.destroyItem(ItemProcessType.DESTROY, this._inventory.getItemByItemId(9819), null, true);
            }
         } else if (this._combatFlagEquippedId) {
            TerritoryWarManager.getInstance().dropCombatFlag(this, false, false);
         }
      } catch (Exception var26) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var26);
      }

      try {
         PartyMatchWaitingList.getInstance().removePlayer(this);
         if (this._partyroom != 0) {
            PartyMatchRoom var33 = PartyMatchRoomList.getInstance().getRoom(this._partyroom);
            if (var33 != null) {
               var33.deleteMember(this);
            }
         }
      } catch (Exception var25) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var25);
      }

      try {
         if (this.isFlying()) {
            this.removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
         }
      } catch (Exception var24) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var24);
      }

      this.getVariables().saveNow();
      this.getAccountVariables().saveNow();

      try {
         this.stopAllTimers();
      } catch (Exception var23) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var23);
      }

      try {
         this.setTeleporting(false);
      } catch (Exception var22) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var22);
      }

      try {
         RecipeManager.getInstance().requestMakeItemAbort(this);
      } catch (Exception var21) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var21);
      }

      try {
         this.setTarget(null);
      } catch (Exception var20) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var20);
      }

      if (this.isChannelized()) {
         this.getSkillChannelized().abortChannelization();
      }

      this.getEffectList().stopAllToggles();
      ZoneRegion var34 = ZoneManager.getInstance().getRegion(this);
      if (var34 != null) {
         var34.removeFromZones(this);
      }

      if (this.isInParty()) {
         try {
            this.leaveParty();
         } catch (Exception var19) {
            LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var19);
         }
      }

      this.stopCubics();

      try {
         this.decayMe();
      } catch (Exception var18) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var18);
      }

      if (Olympiad.getInstance().isRegistered(this) || this.getOlympiadGameId() != -1) {
         Olympiad.getInstance().removeDisconnectedCompetitor(this);
      }

      if (this.hasSummon()) {
         try {
            this._summon.setRestoreSummon(true);
            this._summon.unSummon(this);
            if (this.hasSummon()) {
               this._summon.broadcastNpcInfo(0);
            }
         } catch (Exception var17) {
            LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var17);
         }
      }

      if (this._clan != null) {
         try {
            ClanMember var36 = this._clan.getClanMember(this.getObjectId());
            if (var36 != null) {
               var36.setPlayer(null);
            }
         } catch (Exception var16) {
            LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var16);
         }
      }

      if (this.getActiveRequester() != null) {
         this.setActiveRequester(null);
         this.cancelActiveTrade();
      }

      if (this.isGM()) {
         try {
            AdminData.getInstance().deleteGm(this);
         } catch (Exception var15) {
            LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var15);
         }
      }

      try {
         if (this._observerMode) {
            this.setLocationInvisible(this._lastLoc);
         }

         if (this._vehicle != null) {
            this._vehicle.oustPlayer(this);
         }
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var14);
      }

      try {
         int var37 = this.getInstanceId();
         if (var37 != 0 && !GeneralConfig.RESTORE_PLAYER_INSTANCE) {
            Instance var3 = InstanceManager.getInstance().getInstance(var37);
            if (var3 != null) {
               var3.removePlayer(this.getObjectId());
               Location var4 = var3.getExitLoc();
               if (var4 != null) {
                  int var5 = var4.getX() + Rnd.get(-30, 30);
                  int var6 = var4.getY() + Rnd.get(-30, 30);
                  this.setXYZInvisible(var5, var6, var4.getZ());
                  if (this.hasSummon()) {
                     this._summon.teleToLocation(var4, true);
                     this._summon.setInstanceId(0);
                  }
               }
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var13);
      }

      try {
         this._inventory.deleteMe();
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var12);
      }

      try {
         this.getWarehouse().deleteMe();
      } catch (Exception var11) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var11);
      }

      try {
         this._freight.deleteMe();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var10);
      }

      try {
         this.clearRefund();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var9);
      }

      if (this.isCursedWeaponEquipped()) {
         try {
            CursedWeaponsManager.getInstance().getCursedWeapon(this._cursedWeaponEquippedId).setPlayer(null);
         } catch (Exception var8) {
            LOGGER.log(Level.SEVERE, "deleteMe()", (Throwable)var8);
         }
      }

      if (this._clanId > 0) {
         this._clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
      }

      for (Player var40 : this._snoopedPlayer) {
         var40.removeSnooper(this);
      }

      for (Player var41 : this._snoopListener) {
         var41.removeSnooped(this);
      }

      try {
         this.notifyFriends();
         this._blockList.playerLogout();
      } catch (Exception var7) {
         LOGGER.log(Level.WARNING, "Exception on deleteMe() notifyFriends: " + var7.getMessage(), (Throwable)var7);
      }

      PlayerAutoSaveTaskManager.getInstance().remove(this);
      return super.deleteMe();
   }

   public void startFishing(int var1, int var2, int var3) {
      this.stopMove(null);
      this.setImmobilized(true);
      this._fishing = true;
      this._fishX = var1;
      this._fishY = var2;
      this._fishZ = var3;
      int var4 = this.getRandomFishLvl();
      int var5 = this.getRandomFishGrade();
      int var6 = this.getRandomFishGroup(var5);
      List var7 = FishData.getInstance().getFish(var4, var6, var5);
      if (var7 != null && !var7.isEmpty()) {
         this._fish = ((Fish)var7.get(Rnd.get(var7.size()))).clone();
         var7.clear();
         this.sendPacket(SystemMessageId.YOU_CAST_YOUR_LINE_AND_START_TO_FISH);
         if (!GameTimeTaskManager.getInstance().isNight() && this._lure.isNightLure()) {
            this._fish.setFishGroup(-1);
         }

         this.broadcastPacket(new ExFishingStart(this, this._fish.getFishGroup(), var1, var2, var3, this._lure.isNightLure()));
         this.sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
         this.startLookingForFishTask();
      } else {
         this.sendMessage("Error - Fish are not defined");
         this.endFishing(false);
      }
   }

   public void stopLookingForFishTask() {
      if (this._taskForFish != null) {
         this._taskForFish.cancel(false);
         this._taskForFish = null;
      }
   }

   public void startLookingForFishTask() {
      if (!this.isDead() && this._taskForFish == null) {
         int var1 = 0;
         boolean var2 = false;
         boolean var3 = false;
         if (this._lure != null) {
            int var4 = this._lure.getId();
            var2 = this._fish.getFishGrade() == 0;
            var3 = this._fish.getFishGrade() == 2;
            if (var4 != 6519 && var4 != 6522 && var4 != 6525 && var4 != 8505 && var4 != 8508 && var4 != 8511) {
               if (var4 != 6520
                  && var4 != 6523
                  && var4 != 6526
                  && (var4 < 8505 || var4 > 8513)
                  && (var4 < 7610 || var4 > 7613)
                  && (var4 < 7807 || var4 > 7809)
                  && (var4 < 8484 || var4 > 8486)) {
                  if (var4 == 6521 || var4 == 6524 || var4 == 6527 || var4 == 8507 || var4 == 8510 || var4 == 8513) {
                     var1 = this._fish.getGutsCheckTime() * 66;
                  }
               } else {
                  var1 = this._fish.getGutsCheckTime() * 100;
               }
            } else {
               var1 = this._fish.getGutsCheckTime() * 133;
            }
         }

         this._taskForFish = ThreadPool.scheduleAtFixedRate(
            new LookingForFishTask(this, this._fish.getStartCombatTime(), this._fish.getFishGuts(), this._fish.getFishGroup(), var2, var3), 10000L, var1
         );
      }
   }

   private int getRandomFishGrade() {
      switch (this._lure.getId()) {
         case 7807:
         case 7808:
         case 7809:
         case 8486:
            return 0;
         case 8485:
         case 8506:
         case 8509:
         case 8512:
            return 2;
         default:
            return 1;
      }
   }

   private int getRandomFishGroup(int var1) {
      int var2 = Rnd.get(100);
      byte var3 = 1;
      switch (var1) {
         case 0:
            switch (this._lure.getId()) {
               case 7807:
                  if (var2 <= 54) {
                     return 5;
                  }

                  byte var11;
                  if (var2 <= 77) {
                     var11 = 4;
                  } else {
                     var11 = 6;
                  }

                  return var11;
               case 7808:
                  if (var2 <= 54) {
                     return 4;
                  }

                  byte var10;
                  if (var2 <= 77) {
                     var10 = 6;
                  } else {
                     var10 = 5;
                  }

                  return var10;
               case 7809:
                  if (var2 <= 54) {
                     return 6;
                  }

                  byte var9;
                  if (var2 <= 77) {
                     var9 = 5;
                  } else {
                     var9 = 4;
                  }

                  return var9;
               case 8486:
                  if (var2 <= 33) {
                     return 4;
                  }

                  byte var8;
                  if (var2 <= 66) {
                     var8 = 5;
                  } else {
                     var8 = 6;
                  }

                  return var8;
               default:
                  return var3;
            }
         case 1:
            switch (this._lure.getId()) {
               case 6519:
               case 6520:
               case 6521:
               case 8505:
               case 8507:
                  if (var2 <= 54) {
                     return 1;
                  } else {
                     if (var2 <= 74) {
                        return 0;
                     }

                     byte var7;
                     if (var2 <= 94) {
                        var7 = 2;
                     } else {
                        var7 = 3;
                     }

                     return var7;
                  }
               case 6522:
               case 6523:
               case 6524:
               case 8508:
               case 8510:
                  if (var2 <= 54) {
                     return 0;
                  } else {
                     if (var2 <= 74) {
                        return 1;
                     }

                     byte var6;
                     if (var2 <= 94) {
                        var6 = 2;
                     } else {
                        var6 = 3;
                     }

                     return var6;
                  }
               case 6525:
               case 6526:
               case 6527:
               case 8511:
               case 8513:
                  if (var2 <= 55) {
                     return 2;
                  } else {
                     if (var2 <= 74) {
                        return 1;
                     }

                     byte var5;
                     if (var2 <= 94) {
                        var5 = 0;
                     } else {
                        var5 = 3;
                     }

                     return var5;
                  }
               case 7610:
               case 7611:
               case 7612:
               case 7613:
                  return 3;
               case 8484:
                  if (var2 <= 33) {
                     return 0;
                  }

                  byte var4;
                  if (var2 <= 66) {
                     var4 = 1;
                  } else {
                     var4 = 2;
                  }

                  return var4;
               default:
                  return var3;
            }
         case 2:
            switch (this._lure.getId()) {
               case 8485:
                  if (var2 <= 33) {
                     var3 = 7;
                  } else if (var2 <= 66) {
                     var3 = 8;
                  } else {
                     var3 = 9;
                  }
                  break;
               case 8506:
                  if (var2 <= 54) {
                     var3 = 8;
                  } else if (var2 <= 77) {
                     var3 = 7;
                  } else {
                     var3 = 9;
                  }
                  break;
               case 8509:
                  if (var2 <= 54) {
                     var3 = 7;
                  } else if (var2 <= 77) {
                     var3 = 9;
                  } else {
                     var3 = 8;
                  }
                  break;
               case 8512:
                  if (var2 <= 54) {
                     var3 = 9;
                  } else if (var2 <= 77) {
                     var3 = 8;
                  } else {
                     var3 = 7;
                  }
            }
         default:
            return var3;
      }
   }

   private int getRandomFishLvl() {
      int var1 = this.getSkillLevel(1315);
      BuffInfo var2 = this.getEffectList().getBuffInfoBySkillId(2274);
      if (var2 != null) {
         var1 = (int)var2.getSkill().getPower();
      }

      if (var1 <= 0) {
         return 1;
      } else {
         int var3 = Rnd.get(100);
         int var4;
         if (var3 <= 50) {
            var4 = var1;
         } else if (var3 <= 85) {
            var4 = var1 - 1;
            if (var4 <= 0) {
               var4 = 1;
            }
         } else {
            var4 = var1 + 1;
            if (var4 > 27) {
               var4 = 27;
            }
         }

         return var4;
      }
   }

   public void startFishCombat(boolean var1, boolean var2) {
      this._fishCombat = new Fishing(this, this._fish, var1, var2, this._lure.getId());
   }

   public void endFishing(boolean var1) {
      this._fishing = false;
      this._fishX = 0;
      this._fishY = 0;
      this._fishZ = 0;
      if (this._fishCombat == null) {
         this.sendPacket(SystemMessageId.BAITS_HAVE_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
      }

      this._fishCombat = null;
      this._lure = null;
      this.broadcastPacket(new ExFishingEnd(var1, this));
      this.sendPacket(SystemMessageId.YOU_REEL_YOUR_LINE_IN_AND_STOP_FISHING);
      this.setImmobilized(false);
      this.stopLookingForFishTask();
   }

   public Fishing getFishCombat() {
      return this._fishCombat;
   }

   public int getFishX() {
      return this._fishX;
   }

   public int getFishY() {
      return this._fishY;
   }

   public int getFishZ() {
      return this._fishZ;
   }

   public void setLure(Item var1) {
      this._lure = var1;
   }

   public Item getLure() {
      return this._lure;
   }

   public int getInventoryLimit() {
      int var1;
      if (this.isGM()) {
         var1 = PlayerConfig.INVENTORY_MAXIMUM_GM;
      } else {
         var1 = this.getRace() == Race.DWARF ? PlayerConfig.INVENTORY_MAXIMUM_DWARF : PlayerConfig.INVENTORY_MAXIMUM_NO_DWARF;
      }

      return var1 + (int)this.getStat().calcStat(Stat.INV_LIM, 0.0, null, null);
   }

   public int getWareHouseLimit() {
      return (this.getRace() == Race.DWARF ? PlayerConfig.WAREHOUSE_SLOTS_DWARF : PlayerConfig.WAREHOUSE_SLOTS_NO_DWARF)
         + (int)this.getStat().calcStat(Stat.WH_LIM, 0.0, null, null);
   }

   public int getPrivateSellStoreLimit() {
      return (this.getRace() == Race.DWARF ? PlayerConfig.MAX_PVTSTORESELL_SLOTS_DWARF : PlayerConfig.MAX_PVTSTORESELL_SLOTS_OTHER)
         + (int)this.getStat().calcStat(Stat.P_SELL_LIM, 0.0, null, null);
   }

   public int getPrivateBuyStoreLimit() {
      return (this.getRace() == Race.DWARF ? PlayerConfig.MAX_PVTSTOREBUY_SLOTS_DWARF : PlayerConfig.MAX_PVTSTOREBUY_SLOTS_OTHER)
         + (int)this.getStat().calcStat(Stat.P_BUY_LIM, 0.0, null, null);
   }

   public int getDwarfRecipeLimit() {
      return PlayerConfig.DWARF_RECIPE_LIMIT + (int)this.getStat().calcStat(Stat.REC_D_LIM, 0.0, null, null);
   }

   public int getCommonRecipeLimit() {
      return PlayerConfig.COMMON_RECIPE_LIMIT + (int)this.getStat().calcStat(Stat.REC_C_LIM, 0.0, null, null);
   }

   public int getMountNpcId() {
      return this._mountNpcId;
   }

   public int getMountLevel() {
      return this._mountLevel;
   }

   public void setMountObjectID(int var1) {
      this._mountObjectID = var1;
   }

   public int getMountObjectID() {
      return this._mountObjectID;
   }

   public SkillUseHolder getCurrentSkill() {
      return this._currentSkill;
   }

   public void setCurrentSkill(Skill var1, boolean var2, boolean var3) {
      if (var1 == null) {
         this._currentSkill = null;
      } else {
         this._currentSkill = new SkillUseHolder(var1, var2, var3);
      }
   }

   public SkillUseHolder getCurrentPetSkill() {
      return this._currentPetSkill;
   }

   public void setCurrentPetSkill(Skill var1, boolean var2, boolean var3) {
      if (var1 == null) {
         this._currentPetSkill = null;
      } else {
         this._currentPetSkill = new SkillUseHolder(var1, var2, var3);
      }
   }

   public SkillUseHolder getQueuedSkill() {
      return this._queuedSkill;
   }

   public void setQueuedSkill(Skill var1, boolean var2, boolean var3) {
      if (var1 == null) {
         this._queuedSkill = null;
      } else {
         this._queuedSkill = new SkillUseHolder(var1, var2, var3);
      }
   }

   public boolean isJailed() {
      return PunishmentManager.getInstance().hasPunishment(this.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL)
         || PunishmentManager.getInstance().hasPunishment(this.getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.JAIL)
         || PunishmentManager.getInstance().hasPunishment(this.getIPAddress(), PunishmentAffect.IP, PunishmentType.JAIL)
         || this._client != null
            && this._client.getHardwareInfo() != null
            && PunishmentManager.getInstance().hasPunishment(this._client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.JAIL);
   }

   public boolean isChatBanned() {
      return PunishmentManager.getInstance().hasPunishment(this.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN)
         || PunishmentManager.getInstance().hasPunishment(this.getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.CHAT_BAN)
         || PunishmentManager.getInstance().hasPunishment(this.getIPAddress(), PunishmentAffect.IP, PunishmentType.CHAT_BAN)
         || this._client != null
            && this._client.getHardwareInfo() != null
            && PunishmentManager.getInstance().hasPunishment(this._client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.CHAT_BAN);
   }

   public void startFameTask(long var1, int var3) {
      if (PlayerConfig.FAME_SYSTEM_ENABLED && this.getLevel() >= 40 && this.getPlayerClass().level() >= 2 && this._fameTask == null) {
         this._fameTask = ThreadPool.scheduleAtFixedRate(new FameTask(this, var3), var1, var1);
      }
   }

   public void stopFameTask() {
      if (this._fameTask != null) {
         this._fameTask.cancel(false);
         this._fameTask = null;
      }
   }

   public void startVitalityTask() {
      if (PlayerConfig.ENABLE_VITALITY && this._vitalityTask == null) {
         this._vitalityTask = ThreadPool.scheduleAtFixedRate(new VitalityTask(this), 1000L, 60000L);
      }
   }

   public void stopVitalityTask() {
      if (this._vitalityTask != null) {
         this._vitalityTask.cancel(false);
         this._vitalityTask = null;
      }
   }

   public int getPowerGrade() {
      return this._powerGrade;
   }

   public void setPowerGrade(int var1) {
      this._powerGrade = var1;
   }

   public boolean isCursedWeaponEquipped() {
      return this._cursedWeaponEquippedId != 0;
   }

   public void setCursedWeaponEquippedId(int var1) {
      this._cursedWeaponEquippedId = var1;
   }

   public int getCursedWeaponEquippedId() {
      return this._cursedWeaponEquippedId;
   }

   public boolean isCombatFlagEquipped() {
      return this._combatFlagEquippedId;
   }

   public void setCombatFlagEquipped(boolean var1) {
      this._combatFlagEquippedId = var1;
   }

   public int getChargedSouls() {
      return this._souls;
   }

   public void increaseSouls(int var1) {
      this._souls += var1;
      SystemMessage var2 = new SystemMessage(SystemMessageId.YOUR_SOUL_COUNT_HAS_INCREASED_BY_S1_IT_IS_NOW_AT_S2);
      var2.addInt(var1);
      var2.addInt(this._souls);
      this.sendPacket(var2);
      this.restartSoulTask();
      this.sendPacket(new EtcStatusUpdate(this));
   }

   public boolean decreaseSouls(int var1) {
      this._souls -= var1;
      if (this._souls < 0) {
         this._souls = 0;
      }

      if (this._souls == 0) {
         this.stopSoulTask();
      } else {
         this.restartSoulTask();
      }

      this.sendPacket(new EtcStatusUpdate(this));
      return true;
   }

   public void clearSouls() {
      this._souls = 0;
      this.stopSoulTask();
      this.sendPacket(new EtcStatusUpdate(this));
   }

   private void restartSoulTask() {
      if (this._soulTask != null) {
         this._soulTask.cancel(false);
         this._soulTask = null;
      }

      this._soulTask = ThreadPool.schedule(new ResetSoulsTask(this), 600000L);
   }

   public void stopSoulTask() {
      if (this._soulTask != null) {
         this._soulTask.cancel(false);
         this._soulTask = null;
      }
   }

   public int getDeathPenaltyBuffLevel() {
      return this._deathPenaltyBuffLevel;
   }

   public void setDeathPenaltyBuffLevel(int var1) {
      this._deathPenaltyBuffLevel = var1;
   }

   public void calculateDeathPenaltyBuffLevel(Creature var1) {
      if (var1 == null) {
         LOGGER.warning(this + " called calculateDeathPenaltyBuffLevel with killer null!");
      } else if (!this.isResurrectSpecialAffected()
         && !this.isLucky()
         && !this.isOnEvent()
         && !this.isInsideZone(ZoneId.PVP)
         && !this.isInsideZone(ZoneId.SIEGE)
         && !this.isGM()) {
         double var2 = 1.0;
         if (var1.isRaid()) {
            var2 *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_RAID, 1.0);
         } else if (var1.isMonster()) {
            var2 *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_MOB, 1.0);
         } else if (var1.isPlayable()) {
            var2 *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_PVP, 1.0);
         }

         if (Rnd.get(1, 100) <= PlayerConfig.DEATH_PENALTY_CHANCE * var2 && (!var1.isPlayable() || this.getKarma() > 0)) {
            this.increaseDeathPenaltyBuffLevel();
         }
      }
   }

   public void increaseDeathPenaltyBuffLevel() {
      if (this._deathPenaltyBuffLevel < 15) {
         if (this._deathPenaltyBuffLevel != 0) {
            Skill var1 = SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel());
            if (var1 != null) {
               this.removeSkill(var1, true);
            }
         }

         this._deathPenaltyBuffLevel++;
         this.addSkill(SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel()), false);
         this.sendPacket(new EtcStatusUpdate(this));
         SystemMessage var2 = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
         var2.addInt(this._deathPenaltyBuffLevel);
         this.sendPacket(var2);
      }
   }

   public void reduceDeathPenaltyBuffLevel() {
      if (this._deathPenaltyBuffLevel > 0) {
         Skill var1 = SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel());
         if (var1 != null) {
            this.removeSkill(var1, true);
         }

         this._deathPenaltyBuffLevel--;
         if (this._deathPenaltyBuffLevel > 0) {
            this.addSkill(SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel()), false);
            this.sendPacket(new EtcStatusUpdate(this));
            SystemMessage var2 = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
            var2.addInt(this._deathPenaltyBuffLevel);
            this.sendPacket(var2);
         } else {
            this.sendPacket(new EtcStatusUpdate(this));
            this.sendPacket(SystemMessageId.YOUR_DEATH_PENALTY_HAS_BEEN_LIFTED);
         }
      }
   }

   public void restoreDeathPenaltyBuffLevel() {
      if (this._deathPenaltyBuffLevel > 0) {
         this.addSkill(SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel()), false);
      }
   }

   public void sendDamageMessage(Creature var1, int var2, boolean var3, boolean var4, boolean var5) {
      if (var5) {
         if (var1.isPlayer()) {
            SystemMessage var6 = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
            var6.addPcName(var1.asPlayer());
            var6.addString(this.getName());
            var1.sendPacket(var6);
         }

         SystemMessage var9 = new SystemMessage(SystemMessageId.C1_S_ATTACK_WENT_ASTRAY);
         var9.addPcName(this);
         this.sendPacket(var9);
      } else {
         if (var4) {
            SystemMessage var10 = new SystemMessage(SystemMessageId.C1_HAD_A_CRITICAL_HIT);
            var10.addPcName(this);
            this.sendPacket(var10);
         }

         if (var3) {
            this.sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
         }

         if (this.isInOlympiadMode()
            && var1.isPlayer()
            && var1.asPlayer().isInOlympiadMode()
            && var1.asPlayer().getOlympiadGameId() == this.getOlympiadGameId()) {
            Olympiad.getInstance().notifyCompetitorDamage(this, var2, this.getOlympiadGameId());
         }

         if (var1.isInvul() && !var1.isNpc()) {
            this.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
         } else if (var1.isDoor() || var1 instanceof ControlTower) {
            SystemMessage var12 = new SystemMessage(SystemMessageId.YOU_HIT_FOR_S1_DAMAGE);
            var12.addInt(var2);
            this.sendPacket(var12);
         } else if (this != var1) {
            SystemMessage var11 = new SystemMessage(SystemMessageId.C1_HAS_GIVEN_C2_DAMAGE_OF_S3);
            var11.addPcName(this);
            String var7 = var1.getName();
            if (MultilingualSupportConfig.MULTILANG_ENABLE && var1.isNpc()) {
               String[] var8 = NpcNameLocalisationData.getInstance().getLocalisation(this._lang, var1.getId());
               if (var8 != null) {
                  var7 = var8[0];
               }
            }

            var11.addString(var7);
            var11.addInt(var2);
            this.sendPacket(var11);
         }
      }
   }

   public void setAgathionId(int var1) {
      this._agathionId = var1;
   }

   public int getAgathionId() {
      return this._agathionId;
   }

   public int getVitalityPoints() {
      return this.getStat().getVitalityPoints();
   }

   public int getVitalityLevel() {
      return this.getStat().getVitalityLevel();
   }

   public void setVitalityPoints(int var1, boolean var2) {
      this.getStat().setVitalityPoints(var1, var2);
   }

   public void updateVitalityPoints(float var1, boolean var2, boolean var3) {
      this.getStat().updateVitalityPoints(var1, var2, var3);
   }

   public void checkItemRestriction() {
      for (int var1 = 0; var1 < 25; var1++) {
         Item var2 = this._inventory.getPaperdollItem(var1);
         if (var2 != null && !var2.getTemplate().checkCondition(this, this, false)) {
            this._inventory.unEquipItemInSlot(var1);
            InventoryUpdate var3 = new InventoryUpdate();
            var3.addModifiedItem(var2);
            this.sendInventoryUpdate(var3);
            SystemMessage var4 = null;
            if (var2.getTemplate().getBodyPart() == BodyPart.BACK) {
               this.sendPacket(SystemMessageId.THE_CLOAK_EQUIP_HAS_BEEN_REMOVED_BECAUSE_THE_ARMOR_SET_EQUIP_HAS_BEEN_REMOVED);
               return;
            }

            if (var2.isEnchanted()) {
               var4 = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               var4.addInt(var2.getEnchantLevel());
            } else {
               var4 = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            var4.addItemName(var2);
            this.sendPacket(var4);
         }
      }
   }

   public void addTransformSkill(Skill var1) {
      this._transformSkills.put(var1.getId(), var1);
      if (var1.isPassive()) {
         this.addSkill(var1, false);
      }
   }

   public Skill getTransformSkill(int var1) {
      return this._transformSkills.get(var1);
   }

   public boolean hasTransformSkill(int var1) {
      return this._transformSkills.containsKey(var1);
   }

   public void removeAllTransformSkills() {
      this._transformSkills.clear();
   }

   protected void startFeed(int var1) {
      this._canFeed = var1 > 0;
      if (this.isMounted()) {
         if (this.hasSummon()) {
            this.setCurrentFeed(this._summon.asPet().getCurrentFed());
            this._controlItemId = this._summon.getControlObjectId();
            this.sendPacket(
               new SetupGauge(this.getObjectId(), 3, this._curFeed * 10000 / this.getFeedConsume(), this.getMaxFeed() * 10000 / this.getFeedConsume())
            );
            if (!this.isDead()) {
               this._mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000L, 10000L);
            }
         } else if (this._canFeed) {
            this.setCurrentFeed(this.getMaxFeed());
            this.sendPacket(
               new SetupGauge(this.getObjectId(), 3, this._curFeed * 10000 / this.getFeedConsume(), this.getMaxFeed() * 10000 / this.getFeedConsume())
            );
            if (!this.isDead()) {
               this._mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000L, 10000L);
            }
         }
      }
   }

   public void stopFeed() {
      if (this._mountFeedTask != null) {
         this._mountFeedTask.cancel(false);
         this._mountFeedTask = null;
      }
   }

   private PetLevelData getPetLevelData(int var1) {
      if (this._leveldata == null) {
         this._leveldata = PetDataTable.getInstance().getPetData(var1).getPetLevelData(this.getMountLevel());
      }

      return this._leveldata;
   }

   public int getCurrentFeed() {
      return this._curFeed;
   }

   public int getFeedConsume() {
      return this.isAttackingNow() ? this.getPetLevelData(this._mountNpcId).getPetFeedBattle() : this.getPetLevelData(this._mountNpcId).getPetFeedNormal();
   }

   public void setCurrentFeed(int var1) {
      boolean var2 = this.isHungry();
      this._curFeed = var1 > this.getMaxFeed() ? this.getMaxFeed() : var1;
      this.sendPacket(new SetupGauge(this.getObjectId(), 3, this._curFeed * 10000 / this.getFeedConsume(), this.getMaxFeed() * 10000 / this.getFeedConsume()));
      if (var2 != this.isHungry()) {
         this.broadcastUserInfo();
      }
   }

   private int getMaxFeed() {
      return this.getPetLevelData(this._mountNpcId).getPetMaxFeed();
   }

   public boolean isHungry() {
      return this._canFeed
         && this.getCurrentFeed()
            < PetDataTable.getInstance().getPetData(this.getMountNpcId()).getHungryLimit()
               / 100.0F
               * this.getPetLevelData(this.getMountNpcId()).getPetMaxFeed();
   }

   public void enteredNoLanding(int var1) {
      this._dismountTask = ThreadPool.schedule(new DismountTask(this), var1 * 1000);
   }

   public void exitedNoLanding() {
      if (this._dismountTask != null) {
         this._dismountTask.cancel(true);
         this._dismountTask = null;
      }
   }

   public void storePetFood(int var1) {
      if (this._controlItemId != 0 && var1 != 0) {
         String var2 = "UPDATE pets SET fed=? WHERE item_obj_id = ?";

         try (
            Connection var3 = DatabaseFactory.getConnection();
            PreparedStatement var4 = var3.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
         ) {
            var4.setInt(1, this._curFeed);
            var4.setInt(2, this._controlItemId);
            var4.executeUpdate();
            this._controlItemId = 0;
         } catch (Exception var11) {
            LOGGER.log(Level.SEVERE, "Failed to store Pet [NpcId: " + var1 + "] data", (Throwable)var11);
         }
      }
   }

   public void setInSiege(boolean var1) {
      this._isInSiege = var1;
   }

   public boolean isInSiege() {
      return this._isInSiege;
   }

   public void setInHideoutSiege(boolean var1) {
      this._isInHideoutSiege = var1;
   }

   public boolean isInHideoutSiege() {
      return this._isInHideoutSiege;
   }

   public boolean isFlyingMounted() {
      return this.isTransformed() && this._transformation.isFlying();
   }

   public int getCharges() {
      return this._charges.get();
   }

   public void increaseCharges(int var1, int var2) {
      if (this._charges.get() >= var2) {
         this.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
      } else {
         this.restartChargeTask();
         if (this._charges.addAndGet(var1) >= var2) {
            this._charges.set(var2);
            this.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
         } else {
            SystemMessage var3 = new SystemMessage(SystemMessageId.YOUR_FORCE_HAS_INCREASED_TO_S1_LEVEL);
            var3.addInt(this._charges.get());
            this.sendPacket(var3);
         }

         this.sendPacket(new EtcStatusUpdate(this));
      }
   }

   public boolean decreaseCharges(int var1) {
      if (this._charges.get() < var1) {
         return false;
      } else {
         if (this._charges.addAndGet(-var1) == 0) {
            this.stopChargeTask();
         } else {
            this.restartChargeTask();
         }

         this.sendPacket(new EtcStatusUpdate(this));
         return true;
      }
   }

   public void clearCharges() {
      this._charges.set(0);
      this.sendPacket(new EtcStatusUpdate(this));
   }

   private void restartChargeTask() {
      if (this._chargeTask != null) {
         synchronized (this) {
            this._chargeTask.cancel(false);
         }
      }

      this._chargeTask = ThreadPool.schedule(new ResetChargesTask(this), 600000L);
   }

   public void stopChargeTask() {
      if (this._chargeTask != null) {
         this._chargeTask.cancel(false);
         this._chargeTask = null;
      }
   }

   public void teleportBookmarkModify(int var1, int var2, String var3, String var4) {
      TeleportBookmark var5 = this._tpbookmarks.get(var1);
      if (var5 != null) {
         var5.setIcon(var2);
         var5.setTag(var3);
         var5.setName(var4);

         try (
            Connection var6 = DatabaseFactory.getConnection();
            PreparedStatement var7 = var6.prepareStatement("UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?");
         ) {
            var7.setInt(1, var2);
            var7.setString(2, var3);
            var7.setString(3, var4);
            var7.setInt(4, this.getObjectId());
            var7.setInt(5, var1);
            var7.execute();
         } catch (Exception var14) {
            LOGGER.log(Level.WARNING, "Could not update character teleport bookmark data: " + var14.getMessage(), (Throwable)var14);
         }
      }

      this.sendPacket(new ExGetBookMarkInfoPacket(this));
   }

   public void teleportBookmarkDelete(int var1) {
      if (this._tpbookmarks.remove(var1) != null) {
         try (
            Connection var2 = DatabaseFactory.getConnection();
            PreparedStatement var3 = var2.prepareStatement("DELETE FROM character_tpbookmark WHERE charId=? AND Id=?");
         ) {
            var3.setInt(1, this.getObjectId());
            var3.setInt(2, var1);
            var3.execute();
         } catch (Exception var10) {
            LOGGER.log(Level.WARNING, "Could not delete character teleport bookmark data: " + var10.getMessage(), (Throwable)var10);
         }

         this.sendPacket(new ExGetBookMarkInfoPacket(this));
      }
   }

   public void teleportBookmarkGo(int var1) {
      if (this.teleportBookmarkCondition(0)) {
         if (this._inventory.getInventoryItemCount(13016, 0) == 0L) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
         } else {
            SystemMessage var2 = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
            var2.addItemName(13016);
            this.sendPacket(var2);
            TeleportBookmark var3 = this._tpbookmarks.get(var1);
            if (var3 != null) {
               this.destroyItem(ItemProcessType.NONE, this._inventory.getItemByItemId(13016).getObjectId(), 1L, null, false);
               this.teleToLocation(var3, false);
            }

            this.sendPacket(new ExGetBookMarkInfoPacket(this));
         }
      }
   }

   public boolean teleportBookmarkCondition(int var1) {
      if (this.isInCombat()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
         return false;
      } else if (this._isInSiege || this._siegeState != 0) {
         this.sendPacket(
            SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_A_LARGE_SCALE_BATTLE_SUCH_AS_A_CASTLE_SIEGE_FORTRESS_SIEGE_OR_HIDEOUT_SIEGE
         );
         return false;
      } else if (this._isInDuel || this._startingDuel) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
         return false;
      } else if (this.isFlying()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
         return false;
      } else if (this._inOlympiadMode) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
         return false;
      } else if (this.isParalyzed()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_IN_A_FLINT_OR_PARALYZED_STATE);
         return false;
      } else if (this.isDead()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD);
         return false;
      } else if (var1 != 1 || !this._isIn7sDungeon && (!this.isInParty() || !this._party.isInDimensionalRift())) {
         if (this.isInWater()) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
            return false;
         } else if (var1 != 1
            || !this.isInsideZone(ZoneId.SIEGE)
               && !this.isInsideZone(ZoneId.CLAN_HALL)
               && !this.isInsideZone(ZoneId.JAIL)
               && !this.isInsideZone(ZoneId.CASTLE)
               && !this.isInsideZone(ZoneId.NO_SUMMON_FRIEND)
               && !this.isInsideZone(ZoneId.FORT)) {
            if (!this.isInsideZone(ZoneId.NO_BOOKMARK) && !this.isInBoat() && !this.isInAirShip()) {
               return true;
            } else {
               if (var1 == 0) {
                  this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
               } else if (var1 == 1) {
                  this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
               }

               return false;
            }
         } else {
            this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
            return false;
         }
      } else {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
         return false;
      }
   }

   public void teleportBookmarkAdd(int var1, int var2, int var3, int var4, String var5, String var6) {
      if (this.teleportBookmarkCondition(1)) {
         if (this._tpbookmarks.size() >= this._bookmarkSlot) {
            this.sendPacket(SystemMessageId.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
         } else {
            if (GeneralConfig.BOOKMARK_CONSUME_ITEM_ID > 0) {
               if (this._inventory.getInventoryItemCount(GeneralConfig.BOOKMARK_CONSUME_ITEM_ID, -1) == 0L) {
                  if (GeneralConfig.BOOKMARK_CONSUME_ITEM_ID == 20033) {
                     this.sendPacket(SystemMessageId.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
                  } else {
                     this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
                  }

                  return;
               }

               this.destroyItem(ItemProcessType.NONE, this._inventory.getItemByItemId(GeneralConfig.BOOKMARK_CONSUME_ITEM_ID).getObjectId(), 1L, null, true);
            }

            int var7 = 1;

            while (var7 <= this._bookmarkSlot && this._tpbookmarks.containsKey(var7)) {
               var7++;
            }

            this._tpbookmarks.put(var7, new TeleportBookmark(var7, var1, var2, var3, var4, var5, var6));

            try (
               Connection var8 = DatabaseFactory.getConnection();
               PreparedStatement var9 = var8.prepareStatement("INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)");
            ) {
               var9.setInt(1, this.getObjectId());
               var9.setInt(2, var7);
               var9.setInt(3, var1);
               var9.setInt(4, var2);
               var9.setInt(5, var3);
               var9.setInt(6, var4);
               var9.setString(7, var5);
               var9.setString(8, var6);
               var9.execute();
            } catch (Exception var16) {
               LOGGER.log(Level.WARNING, "Could not insert character teleport bookmark data: " + var16.getMessage(), (Throwable)var16);
            }

            this.sendPacket(new ExGetBookMarkInfoPacket(this));
         }
      }
   }

   public void restoreTeleportBookmark() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?");
      ) {
         var2.setInt(1, this.getObjectId());

         try (ResultSet var3 = var2.executeQuery()) {
            while (var3.next()) {
               this._tpbookmarks
                  .put(
                     var3.getInt("Id"),
                     new TeleportBookmark(
                        var3.getInt("Id"),
                        var3.getInt("x"),
                        var3.getInt("y"),
                        var3.getInt("z"),
                        var3.getInt("icon"),
                        var3.getString("tag"),
                        var3.getString("name")
                     )
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Failed restoing character teleport bookmark.", (Throwable)var12);
      }
   }

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void sendInfo(Player var1) {
      if (this.isInBoat()) {
         this.setXYZ(this.getBoat().getLocation());
         var1.sendPacket(new CharInfo(this, this.isInvisible() && var1.isGM()));
         var1.sendPacket(new ExBrExtraUserInfo(this));
         var1.sendPacket(new GetOnVehicle(this.getObjectId(), this.getBoat().getObjectId(), this._inVehiclePosition));
      } else if (this.isInAirShip()) {
         this.setXYZ(this.getAirShip().getLocation());
         var1.sendPacket(new CharInfo(this, this.isInvisible() && var1.isGM()));
         var1.sendPacket(new ExBrExtraUserInfo(this));
         var1.sendPacket(new ExGetOnAirShip(this, this.getAirShip()));
      } else {
         var1.sendPacket(new CharInfo(this, this.isInvisible() && var1.isGM()));
         var1.sendPacket(new ExBrExtraUserInfo(this));
      }

      int var2 = this.getRelation(var1);
      boolean var3 = this.isAutoAttackable(var1);
      RelationCache var4 = (RelationCache)this.getKnownRelations().get(var1.getObjectId());
      if (var4 == null || var4.getRelation() != var2 || var4.isAutoAttackable() != var3) {
         var1.sendPacket(new RelationChanged(this, var2, var3));
         if (this.hasSummon()) {
            var1.sendPacket(new RelationChanged(this._summon, var2, var3));
         }

         this.getKnownRelations().put(var1.getObjectId(), new RelationCache(var2, var3));
      }

      switch (1.$SwitchMap$org$l2jmobius$gameserver$entity$actor$enums$player$PrivateStoreType[this._privateStoreType.ordinal()]) {
         case 1:
            var1.sendPacket(new PrivateStoreMsgSell(this));
            break;
         case 2:
            var1.sendPacket(new ExPrivateStoreSetWholeMsg(this));
            break;
         case 3:
            var1.sendPacket(new PrivateStoreMsgBuy(this));
            break;
         case 4:
            var1.sendPacket(new RecipeShopMsg(this));
      }

      if (this.isTransformed()) {
         var1.sendPacket(new CharInfo(this, this.isInvisible() && var1.isGM()));
      }
   }

   public void playMovie(MovieHolder var1) {
      if (this._movieHolder == null) {
         this.abortAttack();
         this.stopMove(null);
         this.setMovieHolder(var1);
         if (!this.isTeleporting()) {
            this.sendPacket(new ExStartScenePlayer(var1.getMovie()));
         }
      }
   }

   public void stopMovie() {
      this.setMovieHolder(null);
   }

   public boolean isAllowedToEnchantSkills() {
      if (this._subclassLock) {
         return false;
      } else if (!this.isTransformed() && !this.isInStance()) {
         if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this)) {
            return false;
         } else {
            return !this.isCastingNow() && !this.isCastingSimultaneouslyNow() ? !this.isInBoat() && !this.isInAirShip() : false;
         }
      } else {
         return false;
      }
   }

   public void setCreateDate(Calendar var1) {
      this._createDate = var1;
   }

   public Calendar getCreateDate() {
      return this._createDate;
   }

   public int checkBirthDay() {
      Calendar var1 = Calendar.getInstance();
      if (this._createDate.get(5) == 29 && this._createDate.get(2) == 1) {
         this._createDate.add(11, -24);
      }

      if (var1.get(2) == this._createDate.get(2) && var1.get(5) == this._createDate.get(5) && var1.get(1) != this._createDate.get(1)) {
         return 0;
      } else {
         for (int var2 = 1; var2 < 6; var2++) {
            var1.add(11, 24);
            if (var1.get(2) == this._createDate.get(2) && var1.get(5) == this._createDate.get(5) && var1.get(1) != this._createDate.get(1)) {
               return var2;
            }
         }

         return -1;
      }
   }

   public Collection<Integer> getFriendList() {
      return this._friendList;
   }

   public void restoreFriendList() {
      this._friendList.clear();
      String var1 = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";

      try (
         Connection var2 = DatabaseFactory.getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=0");
      ) {
         var3.setInt(1, this.getObjectId());

         try (ResultSet var4 = var3.executeQuery()) {
            while (var4.next()) {
               int var5 = var4.getInt("friendId");
               if (var5 != this.getObjectId()) {
                  this._friendList.add(var5);
               }
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Error found in " + this.getName() + "'s FriendList: " + var13.getMessage(), (Throwable)var13);
      }
   }

   private void notifyFriends() {
      FriendStatusPacket var1 = new FriendStatusPacket(this.getObjectId());

      for (int var3 : this._friendList) {
         Player var4 = World.getPlayer(var3);
         if (var4 != null) {
            var4.sendPacket(var1);
         }
      }
   }

   public boolean isSilenceMode() {
      return this._silenceMode;
   }

   public boolean isSilenceMode(int var1) {
      return PlayerConfig.SILENCE_MODE_EXCLUDE && this._silenceMode && this._silenceModeExcluded != null
         ? !this._silenceModeExcluded.contains(var1)
         : this._silenceMode;
   }

   public void setSilenceMode(boolean var1) {
      this._silenceMode = var1;
      if (this._silenceModeExcluded != null) {
         this._silenceModeExcluded.clear();
      }

      this.sendPacket(new EtcStatusUpdate(this));
   }

   public void addSilenceModeExcluded(int var1) {
      if (this._silenceModeExcluded == null) {
         this._silenceModeExcluded = new ArrayList<>(1);
      }

      this._silenceModeExcluded.add(var1);
   }

   private void storeRecipeShopList() {
      if (this.hasManufactureShop()) {
         try (Connection var1 = DatabaseFactory.getConnection()) {
            try (PreparedStatement var2 = var1.prepareStatement("DELETE FROM character_recipeshoplist WHERE charId=?")) {
               var2.setInt(1, this.getObjectId());
               var2.execute();
            }

            try (PreparedStatement var13 = var1.prepareStatement(
                  "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)"
               )) {
               AtomicInteger var3 = new AtomicInteger(1);

               for (ManufactureItem var5 : this._manufactureItems.values()) {
                  var13.setInt(1, this.getObjectId());
                  var13.setInt(2, var5.getRecipeId());
                  var13.setLong(3, var5.getCost());
                  var13.setInt(4, var3.getAndIncrement());
                  var13.addBatch();
               }

               var13.executeBatch();
            }
         } catch (Exception var12) {
            LOGGER.log(Level.SEVERE, "Could not store recipe shop for playerId " + this.getObjectId() + ": ", (Throwable)var12);
         }
      }
   }

   private void restoreRecipeShopList() {
      if (this._manufactureItems != null) {
         this._manufactureItems.clear();
      }

      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`");
      ) {
         var2.setInt(1, this.getObjectId());

         try (ResultSet var3 = var2.executeQuery()) {
            while (var3.next()) {
               this.getManufactureItems().put(var3.getInt("recipeId"), new ManufactureItem(var3.getInt("recipeId"), var3.getLong("price")));
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: " + this.getObjectId(), (Throwable)var12);
      }
   }

   public float getCollisionRadius() {
      if (this.isMounted() && this._mountNpcId > 0) {
         return NpcData.getInstance().getTemplate(this.getMountNpcId()).getFCollisionRadius();
      } else if (this.isTransformed()) {
         return this._transformation.getCollisionRadius(this);
      } else {
         return this._appearance.isFemale() ? this.getBaseTemplate().getFCollisionRadiusFemale() : this.getBaseTemplate().getFCollisionRadius();
      }
   }

   public float getCollisionHeight() {
      if (this.isMounted() && this._mountNpcId > 0) {
         return NpcData.getInstance().getTemplate(this.getMountNpcId()).getFCollisionHeight();
      } else if (this.isTransformed()) {
         return this._transformation.getCollisionHeight(this);
      } else {
         return this._appearance.isFemale() ? this.getBaseTemplate().getFCollisionHeightFemale() : this.getBaseTemplate().getFCollisionHeight();
      }
   }

   public int getClientX() {
      return this._clientX;
   }

   public int getClientY() {
      return this._clientY;
   }

   public int getClientZ() {
      return this._clientZ;
   }

   public int getClientHeading() {
      return this._clientHeading;
   }

   public void setClientX(int var1) {
      this._clientX = var1;
   }

   public void setClientY(int var1) {
      this._clientY = var1;
   }

   public void setClientZ(int var1) {
      this._clientZ = var1;
   }

   public void setClientHeading(int var1) {
      this._clientHeading = var1;
   }

   public boolean isFalling(int var1) {
      if (!this.isDead() && !this.isFlying() && !this.isFlyingMounted() && !this.isInsideZone(ZoneId.WATER)) {
         if (this._fallingTimestamp != 0L && System.currentTimeMillis() < this._fallingTimestamp) {
            return true;
         } else {
            int var2 = this.getZ() - var1;
            if (var2 <= this.getBaseTemplate().getSafeFallHeight()) {
               this._fallingTimestamp = 0L;
               return false;
            } else if (!GeoEngine.getInstance().hasGeo(this.getX(), this.getY())) {
               this._fallingTimestamp = 0L;
               return false;
            } else {
               if (this._fallingDamage == 0) {
                  this._fallingDamage = (int)Formulas.calcFallDam(this, var2);
               }

               if (this._fallingDamageTask != null) {
                  this._fallingDamageTask.cancel(true);
               }

               this._fallingDamageTask = ThreadPool.schedule(() -> {
                  if (this._fallingDamage > 0 && !this.isInvul()) {
                     this.reduceCurrentHp(Math.min((double)this._fallingDamage, this.getCurrentHp() - 1.0), null, false, true, null);
                     SystemMessage var1x = new SystemMessage(SystemMessageId.YOU_RECEIVED_S1_DAMAGE_FROM_TAKING_A_HIGH_FALL);
                     var1x.addInt(this._fallingDamage);
                     this.sendPacket(var1x);
                  }

                  this._fallingDamage = 0;
                  this._fallingDamageTask = null;
               }, 1500L);
               this.sendPacket(new ValidateLocation(this));
               this.setFalling();
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void setFalling() {
      this._fallingTimestamp = System.currentTimeMillis() + 1000L;
   }

   public MovieHolder getMovieHolder() {
      return this._movieHolder;
   }

   public void setMovieHolder(MovieHolder var1) {
      this._movieHolder = var1;
   }

   public void updateLastItemAuctionRequest() {
      this._lastItemAuctionInfoRequest = System.currentTimeMillis();
   }

   public boolean isItemAuctionPolling() {
      return System.currentTimeMillis() - this._lastItemAuctionInfoRequest < 2000L;
   }

   public boolean isMovementDisabled() {
      return super.isMovementDisabled() || this._movieHolder != null;
   }

   public String getHtmlPrefix() {
      return MultilingualSupportConfig.MULTILANG_ENABLE ? this._htmlPrefix : "";
   }

   public String getLang() {
      return this._lang;
   }

   public boolean setLang(String var1) {
      boolean var2 = false;
      if (MultilingualSupportConfig.MULTILANG_ENABLE) {
         if (MultilingualSupportConfig.MULTILANG_ALLOWED.contains(var1)) {
            this._lang = var1;
            var2 = true;
         } else {
            this._lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
         }

         this._htmlPrefix = this._lang.equals("en") ? "" : "data/lang/" + this._lang + "/";
      } else {
         this._lang = null;
         this._htmlPrefix = "";
      }

      return var2;
   }

   public long getOfflineStartTime() {
      return this._offlineShopStart;
   }

   public void setOfflineStartTime(long var1) {
      this._offlineShopStart = var1;
   }

   public int getPcCafePoints() {
      return this._pcCafePoints;
   }

   public void setPcCafePoints(int var1) {
      this._pcCafePoints = var1 < PremiumSystemConfig.PC_CAFE_MAX_POINTS ? var1 : PremiumSystemConfig.PC_CAFE_MAX_POINTS;
   }

   public void removeFromBossZone() {
      try {
         for (BossZone var2 : GrandBossManager.getInstance().getZones().values()) {
            var2.removePlayer(this);
         }
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on removeFromBossZone(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void checkPlayerSkills() {
      for (Entry var2 : this.getSkills().entrySet()) {
         SkillLearn var3 = SkillTreeData.getInstance().getClassSkill((Integer)var2.getKey(), ((Skill)var2.getValue()).getLevel() % 100, this.getPlayerClass());
         if (var3 != null) {
            int var4 = var2.getKey() == CommonSkill.EXPERTISE.getId() ? 0 : 9;
            if (this.getLevel() < var3.getGetLevel() - var4) {
               this.deacreaseSkillLevel((Skill)var2.getValue(), var4);
            }
         }
      }
   }

   private void deacreaseSkillLevel(Skill var1, int var2) {
      int var3 = -1;
      Map var4 = SkillTreeData.getInstance().getCompleteClassSkillTree(this.getPlayerClass());

      for (SkillLearn var6 : var4.values()) {
         if (var6.getSkillId() == var1.getId() && var3 < var6.getSkillLevel() && this.getLevel() >= var6.getGetLevel() - var2) {
            var3 = var6.getSkillLevel();
         }
      }

      if (var3 == -1) {
         LOGGER.info("Removing skill " + var1 + " from " + this);
         this.removeSkill(var1, true);
      } else {
         LOGGER.info("Decreasing skill " + var1 + " to " + var3 + " for " + this);
         this.addSkill(SkillData.getInstance().getSkill(var1.getId(), var3), true);
      }
   }

   public boolean canMakeSocialAction() {
      return this._privateStoreType == PrivateStoreType.NONE
         && this.getActiveRequester() == null
         && !this.isAlikeDead()
         && !this.isAllSkillsDisabled()
         && !this.isCastingNow()
         && !this.isCastingSimultaneouslyNow()
         && this.getAI().getIntention() == Intention.IDLE;
   }

   public void setMultiSocialAction(int var1, int var2) {
      this._multiSociaAction = var1;
      this._multiSocialTarget = var2;
   }

   public int getMultiSociaAction() {
      return this._multiSociaAction;
   }

   public int getMultiSocialTarget() {
      return this._multiSocialTarget;
   }

   public Collection<TeleportBookmark> getTeleportBookmarks() {
      return this._tpbookmarks.values();
   }

   public int getBookmarkSlot() {
      return this._bookmarkSlot;
   }

   public void setBookmarkSlot(int var1) {
      this._bookmarkSlot = var1;
      this.sendPacket(new ExGetBookMarkInfoPacket(this));
   }

   public boolean canAttackCreature(Creature var1) {
      if (var1 == null) {
         return false;
      } else if (var1.isAttackable()) {
         return true;
      } else {
         if (var1.isPlayable()) {
            if (var1.isInsideZone(ZoneId.PVP) && !var1.isInsideZone(ZoneId.SIEGE)) {
               return true;
            }

            Player var2;
            if (var1.isSummon()) {
               var2 = var1.asSummon().getOwner();
            } else {
               var2 = var1.asPlayer();
            }

            if (this.isInDuel() && var2.isInDuel() && var2.getDuelId() == this.getDuelId()) {
               return true;
            }

            if (this.isInParty() && var2.isInParty()) {
               if (this.getParty() == var2.getParty()) {
                  return false;
               }

               if ((this.getParty().getCommandChannel() != null || var2.getParty().getCommandChannel() != null)
                  && this.getParty().getCommandChannel() == var2.getParty().getCommandChannel()) {
                  return false;
               }
            } else if (this.getClan() != null && var2.getClan() != null) {
               if (this.getClanId() == var2.getClanId()) {
                  return false;
               }

               if ((this.getAllyId() > 0 || var2.getAllyId() > 0) && this.getAllyId() == var2.getAllyId()) {
                  return false;
               }

               if (this.getClan().isAtWarWith(var2.getClan().getId()) && var2.getClan().isAtWarWith(this.getClan().getId())) {
                  return true;
               }
            } else if ((this.getClan() == null || var2.getClan() == null) && var2.getPvpFlag() == 0 && var2.getKarma() == 0) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isInventoryUnder90(boolean var1) {
      return (var1 ? this._inventory.getSize() : this._inventory.getNonQuestSize()) <= this.getInventoryLimit() * 0.9;
   }

   public boolean isInventoryUnder80(boolean var1) {
      return (var1 ? this._inventory.getSize() : this._inventory.getNonQuestSize()) <= this.getInventoryLimit() * 0.8;
   }

   public boolean havePetInvItems() {
      return this._petItems;
   }

   public void setPetInvItems(boolean var1) {
      this._petItems = var1;
   }

   private void restorePetInventoryItems() {
      try (
         Connection var1 = DatabaseFactory.getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;");
      ) {
         var2.setInt(1, this.getObjectId());

         try (ResultSet var3 = var2.executeQuery()) {
            this.setPetInvItems(var3.next() && var3.getInt("object_id") > 0);
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + this.getObjectId(), (Throwable)var12);
      }
   }

   public String getAdminConfirmCmd() {
      return this._adminConfirmCmd;
   }

   public void setAdminConfirmCmd(String var1) {
      this._adminConfirmCmd = var1;
   }

   public void setPremiumStatus(boolean var1) {
      this._premiumStatus = var1;
   }

   public boolean hasPremiumStatus() {
      return PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && this._premiumStatus;
   }

   public void setLastPetitionGmName(String var1) {
      this._lastPetitionGmName = var1;
   }

   public String getLastPetitionGmName() {
      return this._lastPetitionGmName;
   }

   public ContactList getContactList() {
      return this._contactList;
   }

   public long getNotMoveUntil() {
      return this._notMoveUntil;
   }

   public void updateNotMoveUntil() {
      this._notMoveUntil = System.currentTimeMillis() + PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME;
   }

   public boolean isPlayer() {
      return true;
   }

   public Player asPlayer() {
      return this;
   }

   public boolean isChargedShot(ShotType var1) {
      Item var2 = this.getActiveWeaponInstance();
      return var2 != null && var2.isChargedShot(var1);
   }

   public void setChargedShot(ShotType var1, boolean var2) {
      Item var3 = this.getActiveWeaponInstance();
      if (var3 != null) {
         var3.setChargedShot(var1, var2);
      }
   }

   public Skill getCustomSkill(int var1) {
      return this._customSkills != null ? this._customSkills.get(var1) : null;
   }

   private void addCustomSkill(Skill var1) {
      if (var1 != null && var1.getDisplayId() != var1.getId()) {
         if (this._customSkills == null) {
            this._customSkills = new ConcurrentHashMap<>();
         }

         this._customSkills.put(var1.getDisplayId(), var1);
      }
   }

   private void removeCustomSkill(Skill var1) {
      if (var1 != null && this._customSkills != null && var1.getDisplayId() != var1.getId()) {
         this._customSkills.remove(var1.getDisplayId());
      }
   }

   public boolean canRevive() {
      return this._canRevive;
   }

   public void setCanRevive(boolean var1) {
      this._canRevive = var1;
   }

   public boolean isRegisteredOnEvent() {
      return this._isRegisteredOnEvent || this._isOnEvent;
   }

   public void setRegisteredOnEvent(boolean var1) {
      this._isRegisteredOnEvent = var1;
   }

   public boolean isOnEvent() {
      return this._isOnEvent;
   }

   public void setOnEvent(boolean var1) {
      this._isOnEvent = var1;
   }

   public boolean isOnSoloEvent() {
      return this._isOnSoloEvent;
   }

   public void setOnSoloEvent(boolean var1) {
      this._isOnSoloEvent = var1;
   }

   public void setBlockCheckerArena(byte var1) {
      this._handysBlockCheckerEventArena = var1;
   }

   public int getBlockCheckerArena() {
      return this._handysBlockCheckerEventArena;
   }

   public KrateiArena getKrateiArena() {
      return this._krateiArena;
   }

   public void setKrateiArena(KrateiArena var1) {
      this._krateiArena = var1;
   }

   public int getUCKills() {
      return this._ucKills;
   }

   public void addKillCountUC() {
      this._ucKills++;
   }

   public int getUCDeaths() {
      return this._ucDeaths;
   }

   public void addDeathCountUC() {
      this._ucDeaths++;
   }

   public void cleanUCStats() {
      this._ucDeaths = 0;
      this._ucKills = 0;
   }

   public void setUCState(int var1) {
      this.setRegisteredOnEvent(var1 != 0);
      this._ucState = var1;
   }

   public int getUCState() {
      return this._ucState;
   }

   public void setOriginalCpHpMp(double var1, double var3, double var5) {
      this._originalCp = var1;
      this._originalHp = var3;
      this._originalMp = var5;
   }

   public boolean hasVariables() {
      return this.getScript(PlayerVariables.class) != null;
   }

   public PlayerVariables getVariables() {
      PlayerVariables var1 = (PlayerVariables)this.getScript(PlayerVariables.class);
      return var1 != null ? var1 : (PlayerVariables)this.addScript(new PlayerVariables(this.getObjectId()));
   }

   public boolean hasAccountVariables() {
      return this.getScript(AccountVariables.class) != null;
   }

   public AccountVariables getAccountVariables() {
      AccountVariables var1 = (AccountVariables)this.getScript(AccountVariables.class);
      return var1 != null ? var1 : (AccountVariables)this.addScript(new AccountVariables(this.getAccountName()));
   }

   public int getId() {
      return this.getPlayerClass().getId();
   }

   public boolean isPartyBanned() {
      return PunishmentManager.getInstance().hasPunishment(this.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
   }

   public boolean addAction(PlayerAction var1) {
      if (!this.hasAction(var1)) {
         this._actionMask = this._actionMask | var1.getMask();
         return true;
      } else {
         return false;
      }
   }

   public boolean removeAction(PlayerAction var1) {
      if (this.hasAction(var1)) {
         this._actionMask = this._actionMask & ~var1.getMask();
         return true;
      } else {
         return false;
      }
   }

   public boolean hasAction(PlayerAction var1) {
      return (this._actionMask & var1.getMask()) == var1.getMask();
   }

   public void setCharmOfCourage(boolean var1) {
      this._hasCharmOfCourage = var1;
   }

   public boolean hasCharmOfCourage() {
      return this._hasCharmOfCourage;
   }

   public boolean isGood() {
      return this._isGood;
   }

   public boolean isEvil() {
      return this._isEvil;
   }

   public void setGood() {
      this._isGood = true;
      this._isEvil = false;
   }

   public void setEvil() {
      this._isGood = false;
      this._isEvil = true;
   }

   public boolean isAtWarWith(Creature var1) {
      if (var1 == null) {
         return false;
      } else {
         return this._clan != null && !this.isAcademyMember() && var1.getClan() != null && !var1.isAcademyMember()
            ? this._clan.isAtWarWith(var1.getClan())
            : false;
      }
   }

   public boolean isInPartyWith(Creature var1) {
      return this.isInParty() && var1.isInParty() && this.getParty().getLeaderObjectId() == var1.getParty().getLeaderObjectId();
   }

   public boolean isInCommandChannelWith(Creature var1) {
      return this.isInParty()
         && var1.isInParty()
         && this.getParty().isInCommandChannel()
         && var1.getParty().isInCommandChannel()
         && this.getParty().getCommandChannel().getLeaderObjectId() == var1.getParty().getCommandChannel().getLeaderObjectId();
   }

   public boolean isInClanWith(Creature var1) {
      return this.getClanId() != 0 && var1.getClanId() != 0 && this.getClanId() == var1.getClanId();
   }

   public boolean isInAllyWith(Creature var1) {
      return this.getAllyId() != 0 && var1.getAllyId() != 0 && this.getAllyId() == var1.getAllyId();
   }

   public boolean isInDuelWith(Creature var1) {
      return this.isInDuel() && var1.isInDuel() && this.getDuelId() == var1.getDuelId();
   }

   public boolean isOnSameSiegeSideWith(Creature var1) {
      return this.getSiegeState() > 0
         && this.isInsideZone(ZoneId.SIEGE)
         && this.getSiegeState() == var1.getSiegeState()
         && this.getSiegeSide() == var1.getSiegeSide();
   }

   public long getGamePoints() {
      return this.getAccountVariables().getInt("PRIME_POINTS", 0);
   }

   public void setGamePoints(long var1) {
      AccountVariables var3 = this.getAccountVariables();
      var3.set("PRIME_POINTS", Math.max(var1, 0L));
      var3.storeMe();
   }

   private TerminateReturn onExperienceReceived() {
      return this.isDead() ? new TerminateReturn(false, false, false) : new TerminateReturn(true, true, true);
   }

   public void disableExpGain() {
      this.addListener(new FunctionEventListener(this, EventType.ON_PLAYABLE_EXP_CHANGED, var1 -> this.onExperienceReceived(), this));
   }

   public void enableExpGain() {
      this.removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, var1 -> var1.getOwner() == this);
   }

   public void sendInventoryUpdate(InventoryUpdate var1) {
      if (this._inventoryUpdateTask != null) {
         this._inventoryUpdateTask.cancel(false);
      }

      this._inventoryUpdate.putAll(var1.getItemEntries());
      this._inventoryUpdateTask = ThreadPool.schedule(() -> this.sendPacket(this._inventoryUpdate), 100L);
   }

   public void sendItemList(boolean var1) {
      if (this._itemListTask != null) {
         this._itemListTask.cancel(false);
      }

      this._itemListTask = ThreadPool.schedule(() -> this.sendPacket(new ItemList(this, var1)), 250L);
   }

   public void stopAllTasks() {
      if (this._mountFeedTask != null && !this._mountFeedTask.isDone() && !this._mountFeedTask.isCancelled()) {
         this._mountFeedTask.cancel(false);
         this._mountFeedTask = null;
      }

      if (this._dismountTask != null && !this._dismountTask.isDone() && !this._dismountTask.isCancelled()) {
         this._dismountTask.cancel(false);
         this._dismountTask = null;
      }

      if (this._fameTask != null && !this._fameTask.isDone() && !this._fameTask.isCancelled()) {
         this._fameTask.cancel(false);
         this._fameTask = null;
      }

      if (this._vitalityTask != null && !this._vitalityTask.isDone() && !this._vitalityTask.isCancelled()) {
         this._vitalityTask.cancel(false);
         this._vitalityTask = null;
      }

      if (this._teleportWatchdog != null && !this._teleportWatchdog.isDone() && !this._teleportWatchdog.isCancelled()) {
         this._teleportWatchdog.cancel(false);
         this._teleportWatchdog = null;
      }

      if (this._taskForFish != null && !this._taskForFish.isDone() && !this._taskForFish.isCancelled()) {
         this._taskForFish.cancel(false);
         this._taskForFish = null;
      }

      if (this._chargeTask != null && !this._chargeTask.isDone() && !this._chargeTask.isCancelled()) {
         this._chargeTask.cancel(false);
         this._chargeTask = null;
      }

      if (this._soulTask != null && !this._soulTask.isDone() && !this._soulTask.isCancelled()) {
         this._soulTask.cancel(false);
         this._soulTask = null;
      }

      if (this._taskRentPet != null && !this._taskRentPet.isDone() && !this._taskRentPet.isCancelled()) {
         this._taskRentPet.cancel(false);
         this._taskRentPet = null;
      }

      if (this._taskWater != null && !this._taskWater.isDone() && !this._taskWater.isCancelled()) {
         this._taskWater.cancel(false);
         this._taskWater = null;
      }

      if (this._fallingDamageTask != null && !this._fallingDamageTask.isDone() && !this._fallingDamageTask.isCancelled()) {
         this._fallingDamageTask.cancel(false);
         this._fallingDamageTask = null;
      }

      if (this._taskWarnUserTakeBreak != null && !this._taskWarnUserTakeBreak.isDone() && !this._taskWarnUserTakeBreak.isCancelled()) {
         this._taskWarnUserTakeBreak.cancel(false);
         this._taskWarnUserTakeBreak = null;
      }

      synchronized (this._questTimers) {
         for (QuestTimer var3 : this._questTimers) {
            var3.cancelTask();
         }

         this._questTimers.clear();
      }

      synchronized (this._timerHolders) {
         for (TimerHolder var10 : this._timerHolders) {
            var10.cancelTask();
         }

         this._timerHolders.clear();
      }
   }

   public void addQuestTimer(QuestTimer var1) {
      synchronized (this._questTimers) {
         this._questTimers.add(var1);
      }
   }

   public void removeQuestTimer(QuestTimer var1) {
      synchronized (this._questTimers) {
         this._questTimers.remove(var1);
      }
   }

   public void addTimerHolder(TimerHolder<?> var1) {
      synchronized (this._timerHolders) {
         this._timerHolders.add(var1);
      }
   }

   public void removeTimerHolder(TimerHolder<?> var1) {
      synchronized (this._timerHolders) {
         this._timerHolders.remove(var1);
      }
   }

   public void setServitorShare(Map<Stat, Double> var1) {
      this._servitorShare = var1;
   }

   public double getServitorShareBonus(Stat var1) {
      Map var2 = this._servitorShare;
      if (var2 == null) {
         return 1.0;
      } else {
         Double var3 = (Double)var2.get(var1);
         return var3 == null ? 1.0 : var3;
      }
   }

   public AutoPlaySettingsHolder getAutoPlaySettings() {
      return this._autoPlaySettings;
   }

   public AutoUseSettingsHolder getAutoUseSettings() {
      return this._autoUseSettings;
   }

   public void setAutoPlaying(boolean var1) {
      this._autoPlaying.set(var1);
      if (!var1 && this._offlinePlay && OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS) {
         OfflinePlayTable.getInstance().removeOfflinePlay(this);
      }
   }

   public boolean isAutoPlaying() {
      return this._autoPlaying.get();
   }
}
