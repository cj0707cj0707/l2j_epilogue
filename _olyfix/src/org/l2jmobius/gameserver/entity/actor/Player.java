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

   private Player(int objectId, PlayerTemplate template, String accountName, PlayerAppearance app) {
      super(objectId, template);
      this.setInstanceType(InstanceType.Player);
      this.initCharStatusUpdateValues();
      this.initPcStatusUpdateValues();

      for (int i = 0; i < this._htmlActionCaches.length; i++) {
         this._htmlActionCaches[i] = new ArrayList<>();
      }

      this._accountName = accountName;
      app.setOwner(this);
      this._appearance = app;
      this.getAI();
      this._radar = new Radar(this);
      this.startVitalityTask();
   }

   private Player(PlayerTemplate template, String accountName, PlayerAppearance app) {
      this(IdManager.getInstance().getNextId(), template, accountName, app);
   }

   public void setPvpFlagLasts(long time) {
      this._pvpFlagLasts = time;
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

   public void setSellingBuffs(boolean value) {
      this._isSellingBuffs = value;
   }

   public List<SellBuffHolder> getSellingBuffs() {
      if (this._sellingBuffs == null) {
         this._sellingBuffs = new ArrayList<>();
      }

      return this._sellingBuffs;
   }

   public static Player create(PlayerTemplate template, String accountName, String name, PlayerAppearance app) {
      Player player = new Player(template, accountName, app);
      player.setName(name);
      player.setCreateDate(Calendar.getInstance());
      player.setBaseClass(player.getPlayerClass());
      player.setNewbie(1);
      return player.createDb() ? player : null;
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

   public int getRelation(Player target) {
      int result = 0;
      if (this._clan != null) {
         result |= 64;
         if (this.getClan() == target.getClan()) {
            result |= 256;
         }

         if (this.getAllyId() != 0) {
            result |= 65536;
         }
      }

      if (this.isClanLeader()) {
         result |= 128;
      }

      if (this.getParty() != null && this.getParty() == target.getParty()) {
         result |= 32;

         for (int i = 0; i < this._party.getMembers().size(); i++) {
            if (this._party.getMembers().get(i) == this) {
               switch (i) {
                  case 0:
                     result |= 16;
                     break;
                  case 1:
                     result |= 8;
                     break;
                  case 2:
                     result |= 7;
                     break;
                  case 3:
                     result |= 6;
                     break;
                  case 4:
                     result |= 5;
                     break;
                  case 5:
                     result |= 4;
                     break;
                  case 6:
                     result |= 3;
                     break;
                  case 7:
                     result |= 2;
                     break;
                  case 8:
                     result |= 1;
               }
            }
         }
      }

      if (this._siegeState != 0) {
         if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(this) != 0) {
            result |= 524288;
         } else {
            result |= 512;
            if (this.getSiegeState() != target.getSiegeState()) {
               result |= 4096;
            } else {
               result |= 2048;
            }

            if (this._siegeState == 1) {
               result |= 1024;
            }
         }
      }

      if (this.getClan() != null
         && target.getClan() != null
         && target.getPledgeType() != -1
         && this.getPledgeType() != -1
         && target.getClan().isAtWarWith(this.getClan().getId())) {
         result |= 32768;
         if (this.getClan().isAtWarWith(target.getClan().getId())) {
            result |= 16384;
         }
      }

      if (this._handysBlockCheckerEventArena != -1) {
         result |= 512;
         ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(this.getBlockCheckerArena());
         if (holder.getPlayerTeam(this) == 0) {
            result |= 4096;
         } else {
            result |= 2048;
         }

         result |= 1024;
      }

      return result;
   }

   public static Player load(int objectId) {
      return restore(objectId);
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

   @Override
   public void initCharStat() {
      this.setStat(new PlayerStat(this));
   }

   public PlayerStatus getStatus() {
      return (PlayerStatus)super.getStatus();
   }

   @Override
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

   public void setTemplate(PlayerClass newclass) {
      super.setTemplate(PlayerTemplateData.getInstance().getTemplate(newclass));
   }

   @Override
   protected CreatureAI initAI() {
      return new PlayerAI(this);
   }

   @Override
   public int getLevel() {
      return this.getStat().getLevel();
   }

   @Override
   public double getLevelMod() {
      if (this.isTransformed()) {
         double levelMod = this._transformation.getLevelMod(this);
         if (levelMod > -1.0) {
            return levelMod;
         }
      }

      return super.getLevelMod();
   }

   public int getNewbie() {
      return this._newbie;
   }

   public void setNewbie(int newbieRewards) {
      this._newbie = newbieRewards;
   }

   public void setBaseClass(int baseClass) {
      this._baseClass = baseClass;
   }

   public void setBaseClass(PlayerClass playerClass) {
      this._baseClass = playerClass.getId();
   }

   public boolean isInStoreMode() {
      return this._privateStoreType != PrivateStoreType.NONE;
   }

   public boolean isCrafting() {
      return this._isCrafting;
   }

   public void setCrafting(boolean isCrafting) {
      this._isCrafting = isCrafting;
   }

   public Collection<RecipeList> getCommonRecipeBook() {
      return this._commonRecipeBook.values();
   }

   public Collection<RecipeList> getDwarvenRecipeBook() {
      return this._dwarvenRecipeBook.values();
   }

   public void registerCommonRecipeList(RecipeList recipe, boolean saveToDb) {
      this._commonRecipeBook.put(recipe.getId(), recipe);
      if (saveToDb) {
         this.insertNewRecipeData(recipe.getId(), false);
      }
   }

   public void registerDwarvenRecipeList(RecipeList recipe, boolean saveToDb) {
      this._dwarvenRecipeBook.put(recipe.getId(), recipe);
      if (saveToDb) {
         this.insertNewRecipeData(recipe.getId(), true);
      }
   }

   public boolean hasRecipeList(int recipeId) {
      return this._dwarvenRecipeBook.containsKey(recipeId) || this._commonRecipeBook.containsKey(recipeId);
   }

   public void unregisterRecipeList(int recipeId) {
      if (this._dwarvenRecipeBook.remove(recipeId) != null) {
         this.deleteRecipeData(recipeId, true);
      } else if (this._commonRecipeBook.remove(recipeId) != null) {
         this.deleteRecipeData(recipeId, false);
      } else {
         LOGGER.warning("Attempted to remove unknown RecipeList: " + recipeId);
      }

      for (Shortcut sc : this._shortcuts.getAllShortcuts()) {
         if (sc != null && sc.getId() == recipeId && sc.getType() == ShortcutType.RECIPE) {
            this.deleteShortcut(sc.getSlot(), sc.getPage());
         }
      }
   }

   private void insertNewRecipeData(int recipeId, boolean isDwarf) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)");
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, recipeId);
         ps.setInt(3, isDwarf ? this._classIndex : 0);
         ps.setInt(4, isDwarf ? 1 : 0);
         ps.execute();
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "SQL exception while inserting recipe: " + recipeId + " from character " + this.getObjectId(), (Throwable)var11);
      }
   }

   private void deleteRecipeData(int recipeId, boolean isDwarf) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?");
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, recipeId);
         ps.setInt(3, isDwarf ? this._classIndex : 0);
         ps.execute();
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "SQL exception while deleting recipe: " + recipeId + " from character " + this.getObjectId(), (Throwable)var11);
      }
   }

   public int getLastQuestNpcObject() {
      return this._questNpcObject;
   }

   public void setLastQuestNpcObject(int npcId) {
      this._questNpcObject = npcId;
   }

   public QuestState getQuestState(String quest) {
      return this._quests.get(quest);
   }

   public void setQuestState(QuestState qs) {
      this._quests.put(qs.getQuestName(), qs);
   }

   public boolean hasQuestState(String quest) {
      return this._quests.containsKey(quest);
   }

   public void delQuestState(String quest) {
      this._quests.remove(quest);
   }

   public List<Quest> getAllActiveQuests() {
      List<Quest> quests = new ArrayList<>();

      for (QuestState qs : this._quests.values()) {
         if (qs != null && qs.getQuest() != null && qs.isStarted()) {
            int questId = qs.getQuest().getId();
            if (questId <= 19999 && questId >= 1) {
               quests.add(qs.getQuest());
            }
         }
      }

      return quests;
   }

   public void processScriptEvent(String scriptName, String event) {
      Quest quest = ScriptManager.getInstance().getScript(scriptName);
      if (quest != null && event != null && !event.isEmpty()) {
         Npc target = this._lastFolkNpc;
         if (target != null && this.isInsideRadius2D(target, 250)) {
            quest.notifyEvent(event, target, this);
         } else if (this._questNpcObject > 0) {
            WorldObject object = World.findObject(this.getLastQuestNpcObject());
            if (object != null && object.isNpc() && this.isInsideRadius2D(object, 250)) {
               Npc npc = object.asNpc();
               quest.notifyEvent(event, npc, this);
            }
         }
      }
   }

   public void addNotifyQuestOfDeath(QuestState qs) {
      if (qs != null) {
         if (!this._notifyQuestOfDeathList.contains(qs)) {
            this._notifyQuestOfDeathList.add(qs);
         }
      }
   }

   public void removeNotifyQuestOfDeath(QuestState qs) {
      if (qs != null) {
         this._notifyQuestOfDeathList.remove(qs);
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

   public Shortcut getShortcut(int slot, int page) {
      return this._shortcuts.getShortcut(slot, page);
   }

   public void registerShortcut(Shortcut shortcut) {
      this._shortcuts.registerShortcut(shortcut);
   }

   public void updateShortcuts(int skillId, int skillLevel) {
      this._shortcuts.updateShortcuts(skillId, skillLevel);
   }

   public void deleteShortcut(int slot, int page) {
      this._shortcuts.deleteShortcut(slot, page);
   }

   public void registerMacro(Macro macro) {
      this._macros.registerMacro(macro);
   }

   public void deleteMacro(int id) {
      this._macros.deleteMacro(id);
   }

   public MacroList getMacros() {
      return this._macros;
   }

   public void setSiegeState(byte siegeState) {
      this._siegeState = siegeState;
   }

   @Override
   public byte getSiegeState() {
      return this._siegeState;
   }

   public void setSiegeSide(int value) {
      this._siegeSide = value;
   }

   public boolean isRegisteredOnThisSiegeField(int value) {
      return this._siegeSide == value || this._siegeSide >= 81 && this._siegeSide <= 89;
   }

   @Override
   public int getSiegeSide() {
      return this._siegeSide;
   }

   public boolean isSiegeFriend(WorldObject target) {
      if (this._siegeState != 0 && this.isInsideZone(ZoneId.SIEGE)) {
         Castle castle = CastleManager.getInstance().getCastleById(this._siegeSide);
         if (castle == null) {
            return false;
         } else {
            Player targetPlayer = target.asPlayer();
            if (targetPlayer != null && targetPlayer != this) {
               if (targetPlayer.getSiegeSide() != this._siegeSide || this._siegeState != targetPlayer.getSiegeState()) {
                  return false;
               } else if (this._siegeState != 1) {
                  return true;
               } else {
                  return !castle.isFirstMidVictory() && this._siegeState == targetPlayer.getSiegeState() ? true : castle.getOwner() == null;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void setPvpFlag(int pvpFlag) {
      this._pvpFlag = (byte)pvpFlag;
   }

   @Override
   public byte getPvpFlag() {
      return this._pvpFlag;
   }

   @Override
   public void updatePvPFlag(int value) {
      if (this._pvpFlag != value) {
         this.setPvpFlag(value);
         this.updateUserInfo();
         if (this.hasSummon()) {
            this.sendPacket(new RelationChanged(this._summon, this.getRelation(this), false));
         }

         World.forEachVisibleObject(this, Player.class, target -> {
            target.sendPacket(new RelationChanged(this, this.getRelation(target), this.isAutoAttackable(target)));
            if (this.hasSummon()) {
               target.sendPacket(new RelationChanged(this._summon, this.getRelation(target), this.isAutoAttackable(target)));
            }
         });
      }
   }

   @Override
   public void revalidateZone(boolean force) {
      if (this.getWorldRegion() != null) {
         if (force || !(this.calculateDistance3D(this._lastZoneValidateLocation) < 100.0)) {
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

   public void setPkKills(int pkKills) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PK_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPKChanged(this, this._pkKills, pkKills), this);
      }

      this._pkKills = pkKills;
   }

   public long getDeleteTimer() {
      return this._deleteTimer;
   }

   public void setDeleteTimer(long deleteTimer) {
      this._deleteTimer = deleteTimer;
   }

   public long getLastRecomUpdate() {
      return this._lastRecomUpdate;
   }

   public void setLastRecomUpdate(long date) {
      this._lastRecomUpdate = date;
   }

   public int getRecomHave() {
      return this._recomHave;
   }

   protected void incRecomHave() {
      if (this._recomHave < 255) {
         this._recomHave++;
      }
   }

   public void setRecomHave(int value) {
      this._recomHave = Math.clamp((long)value, 0, 255);
   }

   public int getRecomLeft() {
      return this._recomLeft;
   }

   protected void decRecomLeft() {
      if (this._recomLeft > 0) {
         this._recomLeft--;
      }
   }

   public void giveRecom(Player target) {
      target.incRecomHave();
      this.decRecomLeft();
      this._recomChars.add(target.getObjectId());
      if (!PlayerConfig.ALT_RECOMMEND) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO character_recommends (charId,target_id) VALUES (?,?)");
         ) {
            ps.setInt(1, this.getObjectId());
            ps.setInt(2, target.getObjectId());
            ps.execute();
         } catch (Exception var10) {
            LOGGER.log(Level.SEVERE, "Failed updating character recommendations for player: " + this.getObjectId(), (Throwable)var10);
         }
      }
   }

   public boolean canRecom(Player target) {
      return !this._recomChars.contains(target.getObjectId());
   }

   private void restoreRecom() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT charId,target_id FROM character_recommends WHERE charId=?");
      ) {
         ps.setInt(1, this.getObjectId());
         ResultSet rset = ps.executeQuery();

         while (rset.next()) {
            this._recomChars.add(rset.getInt("target_id"));
         }
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Could not restore Recommendations for player: " + this.getObjectId(), (Throwable)var9);
      }
   }

   private void checkRecom(int recsHave, int recsLeft) {
      Calendar check = Calendar.getInstance();
      check.setTimeInMillis(this._lastRecomUpdate);
      check.add(5, 1);
      Calendar min = Calendar.getInstance();
      this._recomHave = recsHave;
      this._recomLeft = recsLeft;
      if (this.getStat().getLevel() >= 10 && !check.after(min)) {
         this.restartRecom();
      }
   }

   public void restartRecom() {
      this._recomChars.clear();
      if (!PlayerConfig.ALT_RECOMMEND) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM character_recommends WHERE charId=?");
         ) {
            ps.setInt(1, this.getObjectId());
            ps.execute();
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

      Calendar update = Calendar.getInstance();
      if (update.get(11) < 13) {
         update.add(5, -1);
      }

      update.set(11, 13);
      this._lastRecomUpdate = update.getTimeInMillis();
   }

   public void setExpBeforeDeath(long exp) {
      this._expBeforeDeath = exp;
   }

   public long getExpBeforeDeath() {
      return this._expBeforeDeath;
   }

   @Override
   public void setKarma(int value) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_KARMA_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerKarmaChanged(this, this.getKarma(), value), this);
      }

      int karma = value;
      if (value < 0) {
         karma = 0;
      }

      if (this.getKarma() == 0 && karma > 0) {
         World.forEachVisibleObject(this, Guard.class, object -> {
            if (object.getAI().getIntention() == Intention.IDLE) {
               object.getAI().setIntentionActive();
            }
         });
      } else if (this.getKarma() > 0 && karma == 0) {
         this.setKarmaFlag();
      }

      super.setKarma(karma);
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

   public void setExpertisePenaltyBonus(int bonus) {
      this._expertisePenaltyBonus = bonus;
   }

   public int getWeightPenalty() {
      return this._dietMode ? 0 : this._curWeightPenalty;
   }

   public void refreshOverloaded() {
      int maxLoad = this.getMaxLoad();
      if (maxLoad > 0) {
         long weightproc = (this.getCurrentLoad() - this.getBonusWeightPenalty()) * 1000 / this.getMaxLoad();
         int newWeightPenalty;
         if (weightproc < 500L || this._dietMode) {
            newWeightPenalty = 0;
         } else if (weightproc < 666L) {
            newWeightPenalty = 1;
         } else if (weightproc < 800L) {
            newWeightPenalty = 2;
         } else if (weightproc < 1000L) {
            newWeightPenalty = 3;
         } else {
            newWeightPenalty = 4;
         }

         if (this._curWeightPenalty != newWeightPenalty) {
            this._curWeightPenalty = newWeightPenalty;
            if (newWeightPenalty > 0 && !this._dietMode) {
               this.addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
               this.setOverloaded(this.getCurrentLoad() > maxLoad);
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
         int expertiseLevel = this.getExpertiseLevel();
         int armorPenalty = 0;
         int weaponPenalty = 0;

         for (Item item : this._inventory.getItems()) {
            if (item != null && item.isEquipped() && item.getItemType() != EtcItemType.ARROW && item.getItemType() != EtcItemType.BOLT) {
               int crystaltype = item.getTemplate().getCrystalType().getLevel();
               if (crystaltype > expertiseLevel) {
                  if (item.isWeapon() && crystaltype > weaponPenalty) {
                     weaponPenalty = crystaltype;
                  } else if (crystaltype > armorPenalty) {
                     armorPenalty = crystaltype;
                  }
               }
            }
         }

         boolean changed = false;
         weaponPenalty = weaponPenalty - expertiseLevel - this._expertisePenaltyBonus;
         weaponPenalty = Math.clamp((long)weaponPenalty, 0, 4);
         if (this._expertiseWeaponPenalty != weaponPenalty || this.getSkillLevel(CommonSkill.WEAPON_GRADE_PENALTY.getId()) != weaponPenalty) {
            this._expertiseWeaponPenalty = weaponPenalty;
            if (this._expertiseWeaponPenalty > 0) {
               this.addSkill(SkillData.getInstance().getSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId(), this._expertiseWeaponPenalty));
            } else {
               this.removeSkill(this.getKnownSkill(CommonSkill.WEAPON_GRADE_PENALTY.getId()), false, true);
            }

            changed = true;
         }

         armorPenalty = armorPenalty - expertiseLevel - this._expertisePenaltyBonus;
         armorPenalty = Math.clamp((long)armorPenalty, 0, 4);
         if (this._expertiseArmorPenalty != armorPenalty || this.getSkillLevel(CommonSkill.ARMOR_GRADE_PENALTY.getId()) != armorPenalty) {
            this._expertiseArmorPenalty = armorPenalty;
            if (this._expertiseArmorPenalty > 0) {
               this.addSkill(SkillData.getInstance().getSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId(), this._expertiseArmorPenalty));
            } else {
               this.removeSkill(this.getKnownSkill(CommonSkill.ARMOR_GRADE_PENALTY.getId()), false, true);
            }

            changed = true;
         }

         if (changed) {
            this.sendPacket(new EtcStatusUpdate(this));
         }
      }
   }

   public void useEquippableItem(Item item, boolean abortAttack) {
      if (item != null) {
         if (item.getOwnerId() == this.getObjectId()) {
            ItemLocation itemLocation = item.getItemLocation();
            if (itemLocation == ItemLocation.INVENTORY || itemLocation == ItemLocation.PAPERDOLL) {
               Collection<Item> items = null;
               boolean isEquiped = item.isEquipped();
               int oldInvLimit = this.getInventoryLimit();
               SystemMessage sm = null;
               if (isEquiped) {
                  BodyPart bodyPart = BodyPart.fromItem(item);
                  if (item.isEnchanted()) {
                     sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
                     sm.addInt(item.getEnchantLevel());
                  } else {
                     sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
                  }

                  sm.addItemName(item);
                  this.sendPacket(sm);
                  if (bodyPart == BodyPart.DECO) {
                     items = this._inventory.unEquipItemInSlotAndRecord(item.getLocationSlot());
                  } else {
                     items = this._inventory.unEquipItemInBodySlotAndRecord(bodyPart);
                  }
               } else {
                  items = this._inventory.equipItemAndRecord(item);
                  if (item.isEquipped()) {
                     BodyPart bodyPartx = item.getTemplate().getBodyPart();
                     if (item.isEnchanted()) {
                        sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
                        sm.addInt(item.getEnchantLevel());
                     } else {
                        sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
                     }

                     sm.addItemName(item);
                     this.sendPacket(sm);
                     item.decreaseMana(false);
                     if (bodyPartx == BodyPart.R_HAND || bodyPartx == BodyPart.LR_HAND) {
                        this.rechargeShots(true, true);
                     }

                     if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_EQUIP, item.getTemplate())) {
                        EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemEquip(this, item), item.getTemplate());
                     }
                  } else {
                     this.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
                  }
               }

               this.refreshExpertisePenalty();
               this.broadcastUserInfo();
               InventoryUpdate iu = new InventoryUpdate();
               iu.addItems(items);
               this.sendInventoryUpdate(iu);
               if (abortAttack) {
                  this.abortAttack();
               }

               if (this.getInventoryLimit() != oldInvLimit) {
                  this.sendPacket(new ExStorageMaxCount(this));
               }
            }
         }
      }
   }

   public int getPvpKills() {
      return this._pvpKills;
   }

   public void setPvpKills(int pvpKills) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPChanged(this, this._pvpKills, pvpKills), this);
      }

      this._pvpKills = pvpKills;
   }

   public int getFame() {
      return this._fame;
   }

   public void setFame(int fame) {
      int newFame = fame;
      if (fame > PlayerConfig.MAX_PERSONAL_FAME_POINTS) {
         newFame = PlayerConfig.MAX_PERSONAL_FAME_POINTS;
      } else if (fame < 0) {
         newFame = 0;
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_FAME_CHANGED, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFameChanged(this, this._fame, newFame), this);
      }

      this._fame = newFame;
   }

   public PlayerClass getPlayerClass() {
      return this.getTemplate().getPlayerClass();
   }

   public void setPlayerClass(int id) {
      if (!this._subclassLock) {
         this._subclassLock = true;

         try {
            if (this._lvlJoinedAcademy != 0 && this._clan != null && CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, id)) {
               if (this._lvlJoinedAcademy <= 16) {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE);
               } else if (this._lvlJoinedAcademy >= 39) {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MIN_REP_SCORE);
               } else {
                  this._clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE - (this._lvlJoinedAcademy - 16) * 20);
               }

               this.setLvlJoinedAcademy(0);
               SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED);
               msg.addPcName(this);
               this._clan.broadcastToOnlineMembers(msg);
               this._clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(this.getName()));
               this._clan.removeClanMember(this.getObjectId(), 0L);
               this.sendPacket(
                  SystemMessageId.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES
               );
               this._inventory.addItem(ItemProcessType.REWARD, 8181, 1L, this, null);
            }

            if (this.isSubClassActive()) {
               this.getSubClasses().get(this._classIndex).setPlayerClass(id);
            }

            this.setTarget(this);
            this.broadcastSkillPacket(new MagicSkillUse(this, 5103, 1, 0, 0), this);
            this.setClassTemplate(id);
            if (this.getPlayerClass().level() == 3) {
               this.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_THE_THIRD_CLASS_TRANSFER_QUEST);
            } else {
               this.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
            }

            for (int slot = 1; slot < 4; slot++) {
               Henna henna = this.getHenna(slot);
               if (henna != null && !henna.isAllowedClass(this.getPlayerClass())) {
                  this.removeHenna(slot);
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

   public void setLearningClass(PlayerClass learningClass) {
      this._learningClass = learningClass;
   }

   public long getExp() {
      return this.getStat().getExp();
   }

   public void setActiveEnchantAttrItemId(int objectId) {
      this._activeEnchantAttrItemId = objectId;
   }

   public int getActiveEnchantAttrItemId() {
      return this._activeEnchantAttrItemId;
   }

   public void setActiveEnchantItemId(int objectId) {
      if (objectId == -1) {
         this.setActiveEnchantSupportItemId(-1);
         this.setActiveEnchantTimestamp(0L);
         this.setEnchanting(false);
      }

      this._activeEnchantItemId = objectId;
   }

   public int getActiveEnchantItemId() {
      return this._activeEnchantItemId;
   }

   public void setActiveEnchantSupportItemId(int objectId) {
      this._activeEnchantSupportItemId = objectId;
   }

   public int getActiveEnchantSupportItemId() {
      return this._activeEnchantSupportItemId;
   }

   public long getActiveEnchantTimestamp() {
      return this._activeEnchantTimestamp;
   }

   public void setActiveEnchantTimestamp(long value) {
      this._activeEnchantTimestamp = value;
   }

   public void setEnchanting(boolean value) {
      this._isEnchanting = value;
   }

   public boolean isEnchanting() {
      return this._isEnchanting;
   }

   public void setFistsWeaponItem(Weapon weaponItem) {
      this._fistsWeaponItem = weaponItem;
   }

   public Weapon getFistsWeaponItem() {
      return this._fistsWeaponItem;
   }

   public Weapon findFistsWeaponItem(int classId) {
      Weapon weaponItem;
      if (classId >= 0 && classId <= 9) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(246);
      } else if (classId >= 10 && classId <= 17) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(251);
      } else if (classId >= 18 && classId <= 24) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(244);
      } else if (classId >= 25 && classId <= 30) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(249);
      } else if (classId >= 31 && classId <= 37) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(245);
      } else if (classId >= 38 && classId <= 43) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(250);
      } else if (classId >= 44 && classId <= 48) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(248);
      } else if (classId >= 49 && classId <= 52) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(252);
      } else if (classId >= 53 && classId <= 57) {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(247);
      } else {
         weaponItem = (Weapon)ItemData.getInstance().getTemplate(246);
      }

      return weaponItem;
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
         Skill skill = SkillData.getInstance().getSkill(49991, 1);
         if (skill != null) {
            this.addSkill(skill, false);
         }
      }

      if (FreeMountsConfig.ENABLE_FREE_WYVERN) {
         Skill skill = SkillData.getInstance().getSkill(49992, 1);
         if (skill != null) {
            this.addSkill(skill, false);
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
            Castle castle = CastleManager.getInstance().getCastleByOwner(this._clan);
            if (castle != null) {
               castle.giveResidentialSkills(this);
            }
         }

         if (this._clan.getFortId() > 0) {
            Fort fort = FortManager.getInstance().getFortByOwner(this._clan);
            if (fort != null) {
               fort.giveResidentialSkills(this);
            }
         }
      }

      if (RebirthConfig.REBIRTH_ALLOW_REBIRTH) {
         RebirthManager.getInstance().grantRebirthSkills(this);
      }

      this.getInventory().reloadEquippedItems();
      this.restoreDeathPenaltyBuffLevel();
   }

   public int giveAvailableSkills(boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems) {
      int skillCounter = 0;
      Collection<Skill> skills = SkillTreeData.getInstance()
         .getAllAvailableSkills(this, this.getPlayerClass(), includeByFs, includeAutoGet, includeRequiredItems);
      List<Skill> skillsForStore = new ArrayList<>();

      for (Skill skill : skills) {
         if (this.getKnownSkill(skill.getId()) != skill) {
            if (this.getSkillLevel(skill.getId()) == 0) {
               skillCounter++;
            }

            if (skill.isToggle() && this.isAffectedBySkill(skill.getId())) {
               this.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
            }

            this.addSkill(skill, false);
            skillsForStore.add(skill);
            if (PlayerConfig.AUTO_LEARN_SKILLS) {
               this.updateShortcuts(skill.getId(), skill.getLevel());
            }
         }
      }

      this.storeSkills(skillsForStore, -1);
      if (PlayerConfig.AUTO_LEARN_SKILLS && skillCounter > 0) {
         this.sendPacket(new ShortcutInit(this));
         this.sendMessage("You have learned " + skillCounter + " new skills.");
      }

      return skillCounter;
   }

   public void giveAvailableAutoGetSkills() {
      List<SkillLearn> autoGetSkills = SkillTreeData.getInstance().getAvailableAutoGetSkills(this);
      SkillData st = SkillData.getInstance();

      for (SkillLearn s : autoGetSkills) {
         Skill skill = st.getSkill(s.getSkillId(), s.getSkillLevel());
         if (skill != null) {
            this.addSkill(skill, true);
         } else {
            LOGGER.warning("Skipping null auto-get skill for " + this);
         }
      }
   }

   public void setExp(long exp) {
      this.getStat().setExp(Math.max(0L, exp));
   }

   @Override
   public Race getRace() {
      return !this.isSubClassActive() ? this.getTemplate().getRace() : PlayerTemplateData.getInstance().getTemplate(this._baseClass).getRace();
   }

   public Radar getRadar() {
      return this._radar;
   }

   public boolean isMinimapAllowed() {
      return this._minimapAllowed;
   }

   public void setMinimapAllowed(boolean value) {
      this._minimapAllowed = value;
   }

   public long getSp() {
      return this.getStat().getSp();
   }

   public void setSp(long sp) {
      super.getStat().setSp(Math.max(0L, sp));
   }

   public boolean isCastleLord(int castleId) {
      if (this._clan != null && this._clan.getLeader().getPlayer() == this) {
         Castle castle = CastleManager.getInstance().getCastleByOwner(this._clan);
         if (castle != null && castle == CastleManager.getInstance().getCastleById(castleId)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int getClanId() {
      return this._clanId;
   }

   public int getClanCrestId() {
      return this._clan != null ? this._clan.getCrestId() : 0;
   }

   public int getClanCrestLargeId() {
      return this._clan == null || this._clan.getCastleId() == 0 && this._clan.getHideoutId() == 0 ? 0 : this._clan.getCrestLargeId();
   }

   public long getClanJoinExpiryTime() {
      return this._clanJoinExpiryTime;
   }

   public void setClanJoinExpiryTime(long time) {
      this._clanJoinExpiryTime = time;
   }

   public long getClanCreateExpiryTime() {
      return this._clanCreateExpiryTime;
   }

   public void setClanCreateExpiryTime(long time) {
      this._clanCreateExpiryTime = time;
   }

   public void setOnlineTime(long time) {
      this._onlineTime = time;
      this._onlineBeginTime = System.currentTimeMillis();
   }

   public int getOnlineTimeMillis() {
      return (int)(System.currentTimeMillis() - this._onlineBeginTime);
   }

   public PlayerInventory getInventory() {
      return this._inventory;
   }

   public void removeItemFromShortcut(int objectId) {
      this._shortcuts.deleteShortcutByObjectId(objectId);
   }

   public boolean isSitting() {
      return this._waitTypeSitting;
   }

   public void setSitting(boolean value) {
      if (value && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SIT, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSit(this), this);
      }

      this._waitTypeSitting = value;
   }

   public void setSittingProgress(boolean value) {
      this._sittingInProgress = value;
   }

   public void sitDown() {
      this.sitDown(true);
   }

   public void sitDown(boolean checkCast) {
      if (!this._sittingInProgress) {
         if (checkCast && this.isCastingNow()) {
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
      if (!this._sittingInProgress) {
         if (this._waitTypeSitting && !this.isInStoreMode() && !this.isAlikeDead()) {
            this.setSittingProgress(true);
            if (this.getEffectList().isAffected(EffectFlag.RELAXING)) {
               this.stopEffects(EffectType.RELAXING);
            }

            this.broadcastPacket(new ChangeWaitType(this, 1));
            ThreadPool.schedule(new StandUpTask(this), 2500L);
         }
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

   public void addAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage) {
      long currentAdena = this._inventory.getAdena();
      long limitRemaining = PlayerConfig.MAX_ADENA - currentAdena;
      long amountToAdd = count;
      if (count > limitRemaining) {
         count = limitRemaining;
      }

      if (sendMessage) {
         if (count == 0L && amountToAdd > 0L) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_EARN_S1_ADENA);
            sm.addLong(amountToAdd);
            this.sendPacket(sm);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
            sm.addLong(count);
            this.sendPacket(sm);
         }
      }

      if (count > 0L) {
         this._inventory.addAdena(process, count, this, reference);
         if (count == this.getAdena()) {
            this.sendItemList(false);
         } else {
            InventoryUpdate iu = new InventoryUpdate();
            iu.addModifiedItem(this._inventory.getAdenaInstance());
            this.sendInventoryUpdate(iu);
         }
      }

      if (this._inventory.getAdena() == PlayerConfig.MAX_ADENA && sendMessage) {
         this.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
      }
   }

   public boolean reduceAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage) {
      if (count > this._inventory.getAdena()) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
         }

         return false;
      } else {
         if (count > 0L) {
            if (!this._inventory.reduceAdena(process, count, this, reference)) {
               return false;
            }

            Item adenaItem = this._inventory.getAdenaInstance();
            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(adenaItem);
            this.sendInventoryUpdate(iu);
            if (sendMessage) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
               sm.addLong(count);
               this.sendPacket(sm);
            }
         }

         return true;
      }
   }

   public void addAncientAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage) {
      if (sendMessage) {
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
         sm.addItemName(5575);
         sm.addLong(count);
         this.sendPacket(sm);
      }

      if (count > 0L) {
         this._inventory.addAncientAdena(process, count, this, reference);
         InventoryUpdate iu = new InventoryUpdate();
         iu.addItem(this._inventory.getAncientAdenaInstance());
         this.sendInventoryUpdate(iu);
      }
   }

   public boolean reduceAncientAdena(ItemProcessType process, long count, WorldObject reference, boolean sendMessage) {
      if (count > this._inventory.getAncientAdena()) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
         }

         return false;
      } else {
         if (count > 0L) {
            if (!this._inventory.reduceAncientAdena(process, count, this, reference)) {
               return false;
            }

            Item ancientAdenaItem = this._inventory.getAncientAdenaInstance();
            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(ancientAdenaItem);
            this.sendInventoryUpdate(iu);
            if (sendMessage) {
               SystemMessage sm;
               if (count > 1L) {
                  sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                  sm.addItemName(5575);
                  sm.addLong(count);
               } else {
                  sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  sm.addItemName(5575);
               }

               this.sendPacket(sm);
            }
         }

         return true;
      }
   }

   public void addItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage) {
      if (item.getCount() > 0L) {
         if (sendMessage) {
            SystemMessage sm;
            if (item.getCount() > 1L) {
               sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
               sm.addItemName(item);
               sm.addLong(item.getCount());
            } else if (item.isEnchanted()) {
               sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_A_S1_S2);
               sm.addInt(item.getEnchantLevel());
               sm.addItemName(item);
            } else {
               sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
               sm.addItemName(item);
            }

            this.sendPacket(sm);
         }

         Item newitem = this._inventory.addItem(process, item, this, reference);
         InventoryUpdate playerIU = new InventoryUpdate();
         playerIU.addItem(newitem);
         this.sendInventoryUpdate(playerIU);
         StatusUpdate su = new StatusUpdate(this);
         su.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(su);
         if (this.isGM()
            || this._inventory.validateCapacity(0L, item.isQuestItem())
            || !newitem.isDropable()
            || newitem.isStackable() && newitem.getLastChange() == 2) {
            if (CursedWeaponsManager.getInstance().isCursed(newitem.getId())) {
               CursedWeaponsManager.getInstance().activate(this, newitem);
            } else if (FortSiegeManager.getInstance().isCombat(item.getId())) {
               if (FortSiegeManager.getInstance().activateCombatFlag(this, item)) {
                  Fort fort = FortManager.getInstance().getFort(this);
                  fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_HAS_ACQUIRED_THE_FLAG), this.getName());
               }
            } else if (item.getId() >= 13560 && item.getId() <= 13568) {
               TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(item.getId() - 13479);
               if (ward != null) {
                  ward.activate(this, item);
               }
            }
         } else {
            this.dropItem(ItemProcessType.DROP, newitem, null, true, true);
         }
      }
   }

   public Item addItem(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage) {
      return this.addItem(process, itemId, count, -1, reference, sendMessage);
   }

   public Item addItem(ItemProcessType process, int itemId, long count, int enchantLevel, WorldObject reference, boolean sendMessage) {
      if (count > 0L) {
         ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
         if (item == null) {
            LOGGER.severe("Item doesn't exist so cannot be added. Item ID: " + itemId);
            return null;
         }

         if (sendMessage && (!this.isCastingNow() && item.hasExImmediateEffect() || !item.hasExImmediateEffect())) {
            if (count > 1L) {
               if (process != ItemProcessType.SWEEP && process != ItemProcessType.QUEST) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
                  sm.addItemName(itemId);
                  sm.addLong(count);
                  this.sendPacket(sm);
               } else {
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
                  sm.addItemName(itemId);
                  sm.addLong(count);
                  this.sendPacket(sm);
               }
            } else if (process != ItemProcessType.SWEEP && process != ItemProcessType.QUEST) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
               sm.addItemName(itemId);
               this.sendPacket(sm);
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
               sm.addItemName(itemId);
               this.sendPacket(sm);
            }
         }

         if (!item.hasExImmediateEffect() || !item.isEtcItem()) {
            Item createdItem = this._inventory.addItem(process, itemId, count, this, reference);
            if (enchantLevel > -1) {
               createdItem.setEnchantLevel(enchantLevel);
               InventoryUpdate playerIU = new InventoryUpdate();
               if (createdItem.isStackable() && createdItem.getCount() > count) {
                  playerIU.addModifiedItem(createdItem);
               } else {
                  playerIU.addNewItem(createdItem);
               }

               this.sendInventoryUpdate(playerIU);
            }

            if (this.isGM()
               || this._inventory.validateCapacity(0L, item.isQuestItem())
               || !createdItem.isDropable()
               || createdItem.isStackable() && createdItem.getLastChange() == 2) {
               if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId())) {
                  CursedWeaponsManager.getInstance().activate(this, createdItem);
               } else if (createdItem.getId() >= 13560 && createdItem.getId() <= 13568) {
                  TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getId() - 13479);
                  if (ward != null) {
                     ward.activate(this, createdItem);
                  }
               }
            } else {
               this.dropItem(ItemProcessType.DROP, createdItem, null, true);
            }

            return createdItem;
         }

         for (SkillHolder skillHolder : item.getSkills()) {
            this.doSimultaneousCast(skillHolder.getSkill());
         }

         this.broadcastInfo();
      }

      return null;
   }

   public void addItem(ItemProcessType process, ItemHolder item, WorldObject reference, boolean sendMessage) {
      this.addItem(process, item.getId(), item.getCount(), reference, sendMessage);
   }

   public boolean destroyItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage) {
      return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
   }

   public boolean destroyItem(ItemProcessType process, Item item, long count, WorldObject reference, boolean sendMessage) {
      Item destoyedItem = this._inventory.destroyItem(process, item, count, this, reference);
      if (destoyedItem == null) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         InventoryUpdate playerIU = new InventoryUpdate();
         playerIU.addItem(destoyedItem);
         this.sendInventoryUpdate(playerIU);
         StatusUpdate su = new StatusUpdate(this);
         su.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(su);
         if (sendMessage) {
            SystemMessage sm;
            if (count > 1L) {
               sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
               sm.addItemName(destoyedItem);
               sm.addLong(count);
            } else {
               sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
               sm.addItemName(destoyedItem);
            }

            this.sendPacket(sm);
         }

         return true;
      }
   }

   @Override
   public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage) {
      Item item = this._inventory.getItemByObjectId(objectId);
      if (item != null && item.getCount() >= count) {
         return this.destroyItem(process, item, count, reference, sendMessage);
      } else {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      }
   }

   @Override
   public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage) {
      if (itemId == 57) {
         return this.reduceAdena(process, count, reference, sendMessage);
      } else {
         Item item = this._inventory.getItemByItemId(itemId);
         if (item == null) {
            if (sendMessage) {
               this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
            }

            return false;
         } else {
            long itemCount = item.isStackable() ? item.getCount() : this._inventory.getInventoryItemCount(itemId, -1);
            long removeCount = count < 0L ? itemCount : count;
            if (removeCount > 0L && itemCount >= removeCount && this._inventory.destroyItemByItemId(process, itemId, removeCount, this, reference) != null) {
               InventoryUpdate playerIU = new InventoryUpdate();
               playerIU.addItem(item);
               this.sendInventoryUpdate(playerIU);
               StatusUpdate su = new StatusUpdate(this);
               su.addAttribute(14, this.getCurrentLoad());
               this.sendPacket(su);
               if (sendMessage) {
                  SystemMessage sm;
                  if (removeCount > 1L) {
                     sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                     sm.addItemName(itemId);
                     sm.addLong(removeCount);
                  } else {
                     sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                     sm.addItemName(itemId);
                  }

                  this.sendPacket(sm);
               }

               return true;
            } else {
               if (sendMessage) {
                  this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
               }

               return false;
            }
         }
      }
   }

   public Item transferItem(ItemProcessType process, int objectId, long count, Inventory target, WorldObject reference) {
      Item oldItem = this.checkItemManipulation(objectId, count, "transfer");
      if (oldItem == null) {
         return null;
      } else {
         Item newItem = this._inventory.transferItem(process, objectId, count, target, this, reference);
         if (newItem == null) {
            return null;
         } else {
            InventoryUpdate playerIU = new InventoryUpdate();
            if (oldItem.getCount() > 0L && oldItem != newItem) {
               playerIU.addModifiedItem(oldItem);
            } else {
               playerIU.addRemovedItem(oldItem);
            }

            this.sendInventoryUpdate(playerIU);
            StatusUpdate playerSU = new StatusUpdate(this);
            playerSU.addAttribute(14, this.getCurrentLoad());
            this.sendPacket(playerSU);
            if (target instanceof PlayerInventory) {
               Player targetPlayer = ((PlayerInventory)target).getOwner();
               InventoryUpdate targetIU = new InventoryUpdate();
               if (newItem.getCount() > count) {
                  targetIU.addModifiedItem(newItem);
               } else {
                  targetIU.addNewItem(newItem);
               }

               targetPlayer.sendInventoryUpdate(targetIU);
               playerSU = new StatusUpdate(targetPlayer);
               playerSU.addAttribute(14, targetPlayer.getCurrentLoad());
               targetPlayer.sendPacket(playerSU);
            } else if (target instanceof PetInventory) {
               PetInventoryUpdate petIU = new PetInventoryUpdate();
               if (newItem.getCount() > count) {
                  petIU.addModifiedItem(newItem);
               } else {
                  petIU.addNewItem(newItem);
               }

               ((PetInventory)target).getOwner().sendPacket(petIU);
            }

            return newItem;
         }
      }
   }

   public boolean exchangeItemsById(ItemProcessType process, WorldObject reference, int coinId, long cost, int rewardId, long count, boolean sendMessage) {
      if (!this._inventory.validateCapacityByItemId(rewardId, count)) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
         }

         return false;
      } else if (!this._inventory.validateWeightByItemId(rewardId, count)) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
         }

         return false;
      } else if (this.destroyItemByItemId(process, coinId, cost, reference, sendMessage)) {
         this.addItem(process, rewardId, count, reference, sendMessage);
         return true;
      } else {
         return false;
      }
   }

   public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage, boolean protectItem) {
      Item droppedItem = this._inventory.dropItem(process, item, this, reference);
      if (droppedItem == null) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         droppedItem.dropMe(this, this.getX() + Rnd.get(50) - 25, this.getY() + Rnd.get(50) - 25, this.getZ() + 20);
         if (GeneralConfig.AUTODESTROY_ITEM_AFTER > 0
            && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM
            && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(droppedItem.getId())
            && (droppedItem.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM || !droppedItem.isEquipable())) {
            ItemsAutoDestroyTaskManager.getInstance().addItem(droppedItem);
         }

         if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM) {
            droppedItem.setProtected(droppedItem.isEquipable() && (!droppedItem.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
         } else {
            droppedItem.setProtected(true);
         }

         if (protectItem) {
            droppedItem.getDropProtection().protect(this);
         }

         InventoryUpdate playerIU = new InventoryUpdate();
         playerIU.addItem(droppedItem);
         this.sendInventoryUpdate(playerIU);
         StatusUpdate su = new StatusUpdate(this);
         su.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(su);
         if (sendMessage) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
            sm.addItemName(droppedItem);
            this.sendPacket(sm);
         }

         return true;
      }
   }

   public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage) {
      return this.dropItem(process, item, reference, sendMessage, false);
   }

   public Item dropItem(ItemProcessType process, int objectId, long count, int x, int y, int z, WorldObject reference, boolean sendMessage, boolean protectItem) {
      Item invitem = this._inventory.getItemByObjectId(objectId);
      Item item = this._inventory.dropItem(process, objectId, count, this, reference);
      if (item == null) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return null;
      } else {
         item.dropMe(this, x, y, z);
         if (GeneralConfig.AUTODESTROY_ITEM_AFTER > 0
            && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM
            && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(item.getId())
            && (item.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM || !item.isEquipable())) {
            ItemsAutoDestroyTaskManager.getInstance().addItem(item);
         }

         if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM) {
            item.setProtected(item.isEquipable() && (!item.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
         } else {
            item.setProtected(true);
         }

         if (protectItem) {
            item.getDropProtection().protect(this);
         }

         InventoryUpdate playerIU = new InventoryUpdate();
         playerIU.addItem(invitem);
         this.sendInventoryUpdate(playerIU);
         StatusUpdate su = new StatusUpdate(this);
         su.addAttribute(14, this.getCurrentLoad());
         this.sendPacket(su);
         if (sendMessage) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
            sm.addItemName(item);
            this.sendPacket(sm);
         }

         return item;
      }
   }

   public Item checkItemManipulation(int objectId, long count, String action) {
      if (World.findObject(objectId) == null) {
         LOGGER.finest(this.getObjectId() + ": player tried to " + action + " item not available in World");
         return null;
      } else {
         Item item = this._inventory.getItemByObjectId(objectId);
         if (item == null || item.getOwnerId() != this.getObjectId()) {
            LOGGER.finest(this.getObjectId() + ": player tried to " + action + " item he is not owner of");
            return null;
         } else if (count >= 0L && (count <= 1L || item.isStackable())) {
            if (count > item.getCount()) {
               LOGGER.finest(this.getObjectId() + ": player tried to " + action + " more items than he owns");
               return null;
            } else if ((!this.hasSummon() || this._summon.getControlObjectId() != objectId) && this._mountObjectID != objectId) {
               if (this._activeEnchantItemId == objectId) {
                  return null;
               } else {
                  return !item.isAugmented() || !this.isCastingNow() && !this.isCastingSimultaneouslyNow() ? item : null;
               }
            } else {
               return null;
            }
         } else {
            LOGGER.finest(this.getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
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

   public void setSpawnProtection(boolean protect) {
      this._spawnProtectEndTime = protect ? System.currentTimeMillis() + PlayerConfig.PLAYER_SPAWN_PROTECTION * 1000 : 0L;
   }

   public void setTeleportProtection(boolean protect) {
      this._teleportProtectEndTime = protect ? System.currentTimeMillis() + PlayerConfig.PLAYER_TELEPORT_PROTECTION * 1000 : 0L;
   }

   public void setRecentFakeDeath(boolean protect) {
      this._recentFakeDeathEndTime = protect ? GameTimeTaskManager.getInstance().getGameTicks() + PlayerConfig.PLAYER_FAKEDEATH_UP_PROTECTION * 10 : 0L;
   }

   public boolean isRecentFakeDeath() {
      return this._recentFakeDeathEndTime > GameTimeTaskManager.getInstance().getGameTicks();
   }

   public boolean isFakeDeath() {
      return this._isFakeDeath;
   }

   public void setFakeDeath(boolean value) {
      this._isFakeDeath = value;
   }

   @Override
   public boolean isAlikeDead() {
      return super.isAlikeDead() || this._isFakeDeath;
   }

   public GameClient getClient() {
      return this._client;
   }

   public void setClient(GameClient client) {
      this._client = client;
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

   public void setCurrentSkillWorldPosition(Location worldPosition) {
      this._currentSkillWorldPosition = worldPosition;
   }

   @Override
   public void enableSkill(Skill skill) {
      super.enableSkill(skill);
      this.removeTimeStamp(skill);
   }

   @Override
   public boolean checkDoCastConditions(Skill skill) {
      if (!super.checkDoCastConditions(skill) || this._observerMode) {
         return false;
      } else if (this._inOlympiadMode && skill.isBlockedInOlympiad()) {
         this.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
         return false;
      } else if (this._charges.get() >= skill.getChargeConsumeCount() && (!this.isInAirShip() || skill.hasEffectType(EffectType.REFUEL_AIRSHIP))) {
         return true;
      } else {
         SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
         sm.addSkillName(skill);
         this.sendPacket(sm);
         return false;
      }
   }

   private boolean needCpUpdate() {
      double currentCp = this.getCurrentCp();
      if (currentCp <= 1.0 || this.getMaxCp() < 352.0) {
         return true;
      } else if (!(currentCp <= this._cpUpdateDecCheck) && !(currentCp >= this._cpUpdateIncCheck)) {
         return false;
      } else {
         if (currentCp == this.getMaxCp()) {
            this._cpUpdateIncCheck = currentCp + 1.0;
            this._cpUpdateDecCheck = currentCp - this._cpUpdateInterval;
         } else {
            double doubleMulti = currentCp / this._cpUpdateInterval;
            int intMulti = (int)doubleMulti;
            this._cpUpdateDecCheck = this._cpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
            this._cpUpdateIncCheck = this._cpUpdateDecCheck + this._cpUpdateInterval;
         }

         return true;
      }
   }

   private boolean needMpUpdate() {
      double currentMp = this.getCurrentMp();
      if (currentMp <= 1.0 || this.getMaxMp() < 352.0) {
         return true;
      } else if (!(currentMp <= this._mpUpdateDecCheck) && !(currentMp >= this._mpUpdateIncCheck)) {
         return false;
      } else {
         if (currentMp == this.getMaxMp()) {
            this._mpUpdateIncCheck = currentMp + 1.0;
            this._mpUpdateDecCheck = currentMp - this._mpUpdateInterval;
         } else {
            double doubleMulti = currentMp / this._mpUpdateInterval;
            int intMulti = (int)doubleMulti;
            this._mpUpdateDecCheck = this._mpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
            this._mpUpdateIncCheck = this._mpUpdateDecCheck + this._mpUpdateInterval;
         }

         return true;
      }
   }

   @Override
   public void broadcastStatusUpdate() {
      if (this._broadcastStatusUpdateTask == null) {
         this._broadcastStatusUpdateTask = ThreadPool.schedule(() -> {
            StatusUpdate su = new StatusUpdate(this);
            su.addAttribute(10, this.getMaxHp());
            su.addAttribute(9, (int)this.getCurrentHp());
            su.addAttribute(12, this.getMaxMp());
            su.addAttribute(11, (int)this.getCurrentMp());
            su.addAttribute(34, this.getMaxCp());
            su.addAttribute(33, (int)this.getCurrentCp());
            this.sendPacket(su);
            boolean needCpUpdate = this.needCpUpdate();
            boolean needHpUpdate = this.needHpUpdate();
            Party party = this.getParty();
            if (party != null && (needCpUpdate || needHpUpdate || this.needMpUpdate())) {
               party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
            }

            if (this._inOlympiadMode && this._olympiadStart && (needCpUpdate || needHpUpdate)) {
               ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this, 2);
               World.forEachVisibleObject(this, Player.class, player -> {
                  if (player != null && player.isInOlympiadMode() && player.getOlympiadGameId() == this._olympiadGameId) {
                     player.sendPacket(olyInfo);
                  }
               });
               Collection<Player> players = Olympiad.getInstance().getSpectators(this._olympiadGameId);
               if (players != null && !players.isEmpty()) {
                  ExOlympiadUserInfo olyInfo2 = new ExOlympiadUserInfo(this, this.getOlympiadSide());

                  for (Player spectator : players) {
                     if (spectator != null) {
                        spectator.sendPacket(olyInfo2);
                     }
                  }
               }
            }

            if (this._isInDuel && (needCpUpdate || needHpUpdate)) {
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
      if (this.isOnlineInt() != 0) {
         if (this._broadcastCharInfoTask == null) {
            this._broadcastCharInfoTask = ThreadPool.schedule(() -> {
               CharInfo charInfo = new CharInfo(this, false);
               charInfo.sendInBroadcast();
               World.forEachVisibleObject(this, Player.class, player -> {
                  if (this.isVisibleFor(player)) {
                     if (this.isInvisible() && this.isGM()) {
                        player.sendPacket(new CharInfo(this, true));
                     } else {
                        player.sendPacket(charInfo);
                     }

                     player.sendPacket(new ExBrExtraUserInfo(this));
                     int relation = this.getRelation(player);
                     boolean isAutoAttackable = this.isAutoAttackable(player);
                     RelationCache oldrelation = this.getKnownRelations().get(player.getObjectId());
                     if (oldrelation == null || oldrelation.getRelation() != relation || oldrelation.isAutoAttackable() != isAutoAttackable) {
                        player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
                        if (this.hasSummon()) {
                           player.sendPacket(new RelationChanged(this._summon, relation, isAutoAttackable));
                        }

                        this.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
                     }
                  }
               });
               this._broadcastCharInfoTask = null;
            }, 100L);
         }
      }
   }

   public void broadcastTitleInfo() {
      this.updateUserInfo();
      this.broadcastPacket(new NicknameChanged(this));
   }

   @Override
   public void broadcastPacket(ServerPacket packet) {
      packet.sendInBroadcast();
      boolean isCharInfo = packet instanceof CharInfo;
      if (!isCharInfo) {
         this.sendPacket(packet);
      }

      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            player.sendPacket(packet);
            if (isCharInfo) {
               int relation = this.getRelation(player);
               boolean isAutoAttackable = this.isAutoAttackable(player);
               RelationCache cache = this.getKnownRelations().get(player.getObjectId());
               if (cache == null || cache.getRelation() != relation || cache.isAutoAttackable() != isAutoAttackable) {
                  player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
                  if (this.hasSummon()) {
                     player.sendPacket(new RelationChanged(this._summon, relation, isAutoAttackable));
                  }

                  this.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
               }
            }
         }
      });
   }

   @Override
   public int getAllyId() {
      return this._clan == null ? 0 : this._clan.getAllyId();
   }

   public int getAllyCrestId() {
      return this.getAllyId() == 0 ? 0 : this._clan.getAllyCrestId();
   }

   @Override
   public void sendPacket(ServerPacket packet) {
      if (this._client != null) {
         this._client.sendPacket(packet);
      }
   }

   @Override
   public void sendPacket(SystemMessageId id) {
      this.sendPacket(new SystemMessage(id));
   }

   public void doInteract(Creature target) {
      if (target != null) {
         if (target.isPlayer()) {
            Player temp = target.asPlayer();
            this.sendPacket(ActionFailed.STATIC_PACKET);
            if (temp.getPrivateStoreType() == PrivateStoreType.SELL || temp.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL) {
               this.sendPacket(new PrivateStoreListSell(this, temp));
            } else if (temp.getPrivateStoreType() == PrivateStoreType.BUY) {
               this.sendPacket(new PrivateStoreListBuy(this, temp));
            } else if (temp.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
               this.sendPacket(new RecipeShopSellList(this, temp));
            }
         } else {
            target.onAction(this);
         }
      }
   }

   public void doAutoLoot(Attackable target, int itemId, long itemCount) {
      if (this.isInParty() && !ItemData.getInstance().getTemplate(itemId).hasExImmediateEffect()) {
         this._party.distributeItem(this, itemId, itemCount, false, target);
      } else if (itemId == 57) {
         this.addAdena(ItemProcessType.LOOT, itemCount, target, true);
      } else {
         this.addItem(ItemProcessType.LOOT, itemId, itemCount, target, true);
      }
   }

   public void doAutoLoot(Attackable target, ItemHolder item) {
      this.doAutoLoot(target, item.getId(), item.getCount());
   }

   @Override
   public void doPickupItem(WorldObject object) {
      if (!this.isAlikeDead() && !this._isFakeDeath) {
         if (this.getActiveTradeList() != null) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING);
            this.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            this.getAI().setIntentionIdle();
            if (!object.isItem()) {
               LOGGER.warning(this + " trying to pickup wrong target." + this.getTarget());
            } else {
               Item target = (Item)object;
               this.sendPacket(new StopMove(this));
               SystemMessage smsg = null;
               synchronized (target) {
                  if (!target.isSpawned()) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (!target.getDropProtection().tryPickUp(this)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                     smsg.addItemName(target);
                     this.sendPacket(smsg);
                     return;
                  }

                  if ((this.isInParty() && this._party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS || !this.isInParty())
                     && !this._inventory.validateCapacity(target)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
                     return;
                  }

                  if (this.isInvisible() && !this.isGM()) {
                     return;
                  }

                  if (target.getOwnerId() != 0 && target.getOwnerId() != this.getObjectId() && !this.isInLooterParty(target.getOwnerId())) {
                     if (target.getId() == 57) {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
                        smsg.addLong(target.getCount());
                     } else if (target.getCount() > 1L) {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
                        smsg.addItemName(target);
                        smsg.addLong(target.getCount());
                     } else {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                        smsg.addItemName(target);
                     }

                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(smsg);
                     return;
                  }

                  if (FortSiegeManager.getInstance().isCombat(target.getId()) && !FortSiegeManager.getInstance().checkIfCanPickup(this)) {
                     return;
                  }

                  if (target.getItemLootShedule() != null && (target.getOwnerId() == this.getObjectId() || this.isInLooterParty(target.getOwnerId()))) {
                     target.resetOwnerTimer();
                  }

                  target.pickupMe(this);
                  if (GeneralConfig.SAVE_DROPPED_ITEM) {
                     ItemsOnGroundManager.getInstance().removeObject(target);
                  }
               }

               if (target.getTemplate().hasExImmediateEffect()) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
                  if (handler != null) {
                     handler.onItemUse(this, target, false);
                  } else {
                     LOGGER.warning("No item handler registered for item ID: " + target.getId() + ".");
                  }

                  ItemManager.destroyItem(ItemProcessType.NONE, target, this, null);
               } else if (CursedWeaponsManager.getInstance().isCursed(target.getId())) {
                  this.addItem(ItemProcessType.PICKUP, target, null, true);
               } else if (FortSiegeManager.getInstance().isCombat(target.getId())) {
                  this.addItem(ItemProcessType.PICKUP, target, null, true);
               } else {
                  if (target.getItemType() instanceof ArmorType || target.getItemType() instanceof WeaponType) {
                     if (target.isEnchanted()) {
                        smsg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_HAS_PICKED_UP_S2_S3);
                        smsg.addPcName(this);
                        smsg.addInt(target.getEnchantLevel());
                     } else {
                        smsg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_HAS_PICKED_UP_S2);
                        smsg.addPcName(this);
                     }

                     smsg.addItemName(target.getId());
                     this.broadcastPacket(smsg);
                  }

                  if (this.isInParty()) {
                     this._party.distributeItem(this, target);
                  } else if (target.getId() == 57 && this._inventory.getAdenaInstance() != null) {
                     this.addAdena(ItemProcessType.PICKUP, target.getCount(), null, true);
                     ItemManager.destroyItem(ItemProcessType.PICKUP, target, this, null);
                  } else {
                     this.addItem(ItemProcessType.PICKUP, target, null, true);
                     Item weapon = this._inventory.getPaperdollItem(5);
                     if (weapon != null) {
                        EtcItem etcItem = target.getEtcItem();
                        if (etcItem != null) {
                           EtcItemType itemType = etcItem.getItemType();
                           if (weapon.getItemType() == WeaponType.BOW && itemType == EtcItemType.ARROW) {
                              this.checkAndEquipArrows();
                           } else if (weapon.getItemType() == WeaponType.CROSSBOW && itemType == EtcItemType.BOLT) {
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

   @Override
   public void doAttack(Creature target) {
      super.doAttack(target);
      this.setRecentFakeDeath(false);
      if (target.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE) {
         this.updatePvPStatus();
      }
   }

   @Override
   public void doCast(Skill skill) {
      if (this._currentSkill != null && !this.checkUseMagicConditions(skill, this._currentSkill.isCtrlPressed(), this._currentSkill.isShiftPressed())) {
         this.setCastingNow(false);
         this.setCastingSimultaneouslyNow(false);
      } else {
         super.doCast(skill);
         this.setRecentFakeDeath(false);
      }
   }

   public boolean canOpenPrivateStore() {
      if ((PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC > 0 || PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER > 0)
         && World.getFirstVisibleObject(
               this,
               Creature.class,
               creature -> creature.getMinShopDistance() > 0 && LocationUtil.checkIfInRange(creature.getMinShopDistance(), this, creature, true)
            )
            != null) {
         this.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE));
         return false;
      } else {
         return !this.isAlikeDead() && !this._inOlympiadMode && !this.isMounted() && !this.isInsideZone(ZoneId.NO_STORE) && !this.isCastingNow();
      }
   }

   @Override
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

   public void tryOpenPrivateSellStore(boolean isPackageSale) {
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
            this.sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
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

   public void setMultiSell(PreparedListContainer list) {
      this._currentMultiSell = list;
   }

   @Override
   public boolean isTransformed() {
      return this._transformation != null && !this._transformation.isStance();
   }

   public boolean isInStance() {
      return this._transformation != null && this._transformation.isStance();
   }

   public void transform(Transform transformation) {
      if (!transformation.isFlying() || this.getX() <= -166168) {
         if (FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE || !transformation.isRiding() || !this.isInsideZone(ZoneId.SIEGE)) {
            if (this._transformation != null) {
               this.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN));
            } else {
               this.setQueuedSkill(null, false, false);
               if (this.isMounted()) {
                  this.dismount();
               }

               this._transformation = transformation;
               this.getEffectList().stopAllToggles();
               transformation.onTransform(this);
               this.sendSkillList();
               this.sendPacket(new SkillCoolTime(this));
               this.broadcastUserInfo();
               if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TRANSFORM, this)) {
                  EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTransform(this, transformation.getId()), this);
               }
            }
         }
      }
   }

   @Override
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

   @Override
   public Transform getTransformation() {
      return this._transformation;
   }

   public int getTransformationId() {
      return this.isTransformed() ? this._transformation.getId() : 0;
   }

   public int getTransformationDisplayId() {
      return this.isTransformed() ? this._transformation.getDisplayId() : 0;
   }

   @Override
   public void setTarget(WorldObject worldObject) {
      WorldObject newTarget = worldObject;
      if (worldObject != null) {
         boolean isInParty = worldObject.isPlayer() && this.isInParty() && this._party.containsPlayer(worldObject.asPlayer());
         if (!isInParty && Math.abs(worldObject.getZ() - this.getZ()) > 3000) {
            newTarget = null;
         }

         if (newTarget != null && !isInParty && !newTarget.isSpawned()) {
            newTarget = null;
         }

         if (!this.isGM() && newTarget instanceof Vehicle) {
            newTarget = null;
         }
      }

      WorldObject oldTarget = this.getTarget();
      if (oldTarget != null) {
         if (oldTarget.equals(newTarget)) {
            if (newTarget != null && newTarget.getObjectId() != this.getObjectId()) {
               this.sendPacket(new ValidateLocation(newTarget));
            }

            return;
         }

         oldTarget.removeStatusListener(this);
      }

      if (newTarget != null && newTarget.isCreature()) {
         Creature target = newTarget.asCreature();
         if (newTarget.getObjectId() != this.getObjectId()) {
            this.sendPacket(new ValidateLocation(target));
         }

         this.sendPacket(new MyTargetSelected(this, target));
         target.addStatusListener(this);
         StatusUpdate su = new StatusUpdate(target);
         su.addAttribute(10, target.getMaxHp());
         su.addAttribute(9, (int)target.getCurrentHp());
         this.sendPacket(su);
         World.broadcastToVisiblePlayers(this, new TargetSelected(this.getObjectId(), newTarget.getObjectId(), this.getX(), this.getY(), this.getZ()));
      }

      if (newTarget == null && this.getTarget() != null) {
         this.broadcastPacket(new TargetUnselected(this));
      }

      super.setTarget(newTarget);
   }

   @Override
   public Item getActiveWeaponInstance() {
      return this._inventory.getPaperdollItem(5);
   }

   @Override
   public Weapon getActiveWeaponItem() {
      Item weapon = this.getActiveWeaponInstance();
      return weapon == null ? this._fistsWeaponItem : (Weapon)weapon.getTemplate();
   }

   public Item getChestArmorInstance() {
      return this._inventory.getPaperdollItem(6);
   }

   public Item getLegsArmorInstance() {
      return this._inventory.getPaperdollItem(11);
   }

   public Armor getActiveChestArmorItem() {
      Item armor = this.getChestArmorInstance();
      return armor == null ? null : (Armor)armor.getTemplate();
   }

   public Armor getActiveLegsArmorItem() {
      Item legs = this.getLegsArmorInstance();
      return legs == null ? null : (Armor)legs.getTemplate();
   }

   public boolean isWearingHeavyArmor() {
      Item legs = this.getLegsArmorInstance();
      Item armor = this.getChestArmorInstance();
      return armor != null && legs != null && legs.getItemType() == ArmorType.HEAVY && armor.getItemType() == ArmorType.HEAVY
         ? true
         : armor != null
            && this.getInventory().getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR
            && armor.getItemType() == ArmorType.HEAVY;
   }

   public boolean isWearingLightArmor() {
      Item legs = this.getLegsArmorInstance();
      Item armor = this.getChestArmorInstance();
      return armor != null && legs != null && legs.getItemType() == ArmorType.LIGHT && armor.getItemType() == ArmorType.LIGHT
         ? true
         : armor != null && this._inventory.getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR && armor.getItemType() == ArmorType.LIGHT;
   }

   public boolean isWearingMagicArmor() {
      Item legs = this.getLegsArmorInstance();
      Item armor = this.getChestArmorInstance();
      return armor != null && legs != null && legs.getItemType() == ArmorType.MAGIC && armor.getItemType() == ArmorType.MAGIC
         ? true
         : armor != null && this._inventory.getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR && armor.getItemType() == ArmorType.MAGIC;
   }

   public boolean isMarried() {
      return this._married;
   }

   public void setMarried(boolean value) {
      this._married = value;
   }

   public boolean isEngageRequest() {
      return this._engagerequest;
   }

   public void setEngageRequest(boolean state, int playerid) {
      this._engagerequest = state;
      this._engageid = playerid;
   }

   public void setMarryRequest(boolean value) {
      this._marryrequest = value;
   }

   public boolean isMarryRequest() {
      return this._marryrequest;
   }

   public void setMarryAccepted(boolean value) {
      this._marryaccepted = value;
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

   public void setPartnerId(int partnerid) {
      this._partnerId = partnerid;
   }

   public int getCoupleId() {
      return this._coupleId;
   }

   public void setCoupleId(int coupleId) {
      this._coupleId = coupleId;
   }

   public void engageAnswer(int answer) {
      if (this._engagerequest && this._engageid != 0) {
         Player ptarget = World.getPlayer(this._engageid);
         this.setEngageRequest(false, 0);
         if (ptarget != null) {
            if (answer == 1) {
               CoupleManager.getInstance().createCouple(ptarget, this);
               ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
            } else {
               ptarget.sendMessage("Request to Engage has been >DENIED<!");
            }
         }
      }
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return this._inventory.getPaperdollItem(7);
   }

   @Override
   public ItemTemplate getSecondaryWeaponItem() {
      Item item = this._inventory.getPaperdollItem(7);
      return item != null ? item.getTemplate() : null;
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this.isMounted()) {
            this.stopFeed();
         }

         if (this._isFakeDeath) {
            this.stopFakeDeath(true);
         }

         KrateiArena arena = this.getKrateiArena();
         if (arena != null) {
            arena.setRespawnTask(this);
            if (killer != null && killer.isPlayable()) {
               Player player = killer.asPlayer();
               if (player != null) {
                  KrateiArena killerArena = player.getKrateiArena();
                  if (killerArena != null) {
                     killerArena.addPoints(player, true);
                  }
               }
            }
         }

         if (killer != null) {
            Player pk = killer.asPlayer();
            boolean fpcKill = killer.isFakePlayer();
            if (pk != null || fpcKill) {
               if (this.getParty() != null && this.getParty().getUCState() instanceof UCTeam) {
                  ((UCTeam)this.getParty().getUCState()).onKill(this, pk);
               }

               if (pk != null) {
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_KILL, this)) {
                     EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPKill(pk, this), this);
                  }

                  if ((!PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES || this.getInstanceId() == 0)
                     && (!PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES || !this.isInsideZone(ZoneId.PVP))) {
                     if (PvpRewardItemConfig.REWARD_PVP_ITEM && this._pvpFlag != 0) {
                        pk.addItem(
                           ItemProcessType.REWARD,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_ID,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT,
                           this,
                           PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE
                        );
                     }

                     if (PvpRewardItemConfig.REWARD_PK_ITEM && this._pvpFlag == 0) {
                        pk.addItem(
                           ItemProcessType.REWARD,
                           PvpRewardItemConfig.REWARD_PK_ITEM_ID,
                           PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT,
                           this,
                           PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE
                        );
                     }
                  }
               }

               if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && (pk != null && !pk.isGM() || fpcKill)) {
                  String msg = "";
                  if (this._pvpFlag == 0) {
                     msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG
                        .replace("$killer", pk != null ? pk.getName() : killer.getName())
                        .replace("$target", this.getName());
                     if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.S1);
                        sm.addString(msg);
                        World.broadcastToAllOnlinePlayers(sm);
                     } else {
                        World.broadcastToAllOnlinePlayers(msg, false);
                     }
                  } else if (this._pvpFlag != 0) {
                     msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", killer.getName()).replace("$target", this.getName());
                     if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.S1);
                        sm.addString(msg);
                        World.broadcastToAllOnlinePlayers(sm);
                     } else {
                        World.broadcastToAllOnlinePlayers(msg, false);
                     }
                  }
               }

               if (fpcKill && FakePlayersConfig.FAKE_PLAYER_KILL_KARMA && this._pvpFlag == 0 && this.getKarma() <= 0) {
                  killer.setKarma(killer.getKarma() + 150);
                  killer.broadcastInfo();
               }
            }

            this.broadcastStatusUpdate();
            this.setExpBeforeDeath(0L);
            if (this.isCursedWeaponEquipped()) {
               CursedWeaponsManager.getInstance().drop(this._cursedWeaponEquippedId, killer);
            } else if (this._combatFlagEquippedId) {
               if (TerritoryWarManager.getInstance().isTWInProgress()) {
                  TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
               } else {
                  Fort fort = FortManager.getInstance().getFort(this);
                  if (fort != null) {
                     FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
                  } else {
                     BodyPart bodyPart = BodyPart.fromItem(this._inventory.getItemByItemId(9819));
                     this._inventory.unEquipItemInBodySlot(bodyPart);
                     this.destroyItem(ItemProcessType.DESTROY, this._inventory.getItemByItemId(9819), null, true);
                  }
               }
            } else {
               boolean insidePvpZone = this.isInsideZone(ZoneId.PVP);
               boolean insideSiegeZone = this.isInsideZone(ZoneId.SIEGE);
               if (pk == null || !pk.isCursedWeaponEquipped()) {
                  this.onDieDropItem(killer);
                  if (!insidePvpZone
                     && !insideSiegeZone
                     && pk != null
                     && pk.getClan() != null
                     && this.getClan() != null
                     && !this.isAcademyMember()
                     && !pk.isAcademyMember()
                     && (this._clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(this._clan.getId()) || this.isInSiege() && pk.isInSiege())
                     && AntiFeedManager.getInstance().check(killer, this)) {
                     if (this._clan.getReputationScore() > 0) {
                        pk.getClan().addReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
                     }

                     if (pk.getClan().getReputationScore() > 0) {
                        this._clan.takeReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
                     }
                  }

                  if (PlayerConfig.PLAYER_DELEVEL && !this.isLucky() && (insideSiegeZone || !insidePvpZone) && !this.isOnEvent()) {
                     this.calculateDeathExpPenalty(killer, this.isAtWarWith(pk));
                  }
               }
            }
         }

         if (!this._cubics.isEmpty()) {
            for (Cubic cubic : this._cubics.values()) {
               cubic.stopAction();
               cubic.cancelDisappear();
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

         this.calculateDeathPenaltyBuffLevel(killer);
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

   private void onDieDropItem(Creature killer) {
      if (!this.isOnEvent() && killer != null) {
         Player pk = killer.asPlayer();
         if (this.getKarma() > 0 || pk == null || pk.getClan() == null || this.getClan() == null || !pk.getClan().isAtWarWith(this._clanId)) {
            if ((!this.isInsideZone(ZoneId.PVP) || pk == null) && (!this.isGM() || PvpConfig.KARMA_DROP_GM)) {
               boolean isKarmaDrop = false;
               boolean isKillerNpc = killer instanceof Npc;
               int pkLimit = PvpConfig.KARMA_PK_LIMIT;
               int dropEquip = 0;
               int dropEquipWeapon = 0;
               int dropItem = 0;
               int dropLimit = 0;
               int dropPercent = 0;
               if (this.getKarma() > 0 && this._pkKills >= pkLimit) {
                  isKarmaDrop = true;
                  dropPercent = RatesConfig.KARMA_RATE_DROP;
                  dropEquip = RatesConfig.KARMA_RATE_DROP_EQUIP;
                  dropEquipWeapon = RatesConfig.KARMA_RATE_DROP_EQUIP_WEAPON;
                  dropItem = RatesConfig.KARMA_RATE_DROP_ITEM;
                  dropLimit = RatesConfig.KARMA_DROP_LIMIT;
               } else if (isKillerNpc && this.getLevel() > 4 && !this.isFestivalParticipant()) {
                  dropPercent = RatesConfig.PLAYER_RATE_DROP;
                  dropEquip = RatesConfig.PLAYER_RATE_DROP_EQUIP;
                  dropEquipWeapon = RatesConfig.PLAYER_RATE_DROP_EQUIP_WEAPON;
                  dropItem = RatesConfig.PLAYER_RATE_DROP_ITEM;
                  dropLimit = RatesConfig.PLAYER_DROP_LIMIT;
               }

               if (dropPercent > 0 && Rnd.get(100) < dropPercent) {
                  int dropCount = 0;
                  int itemDropPercent = 0;

                  for (Item itemDrop : this._inventory.getItems()) {
                     if (!itemDrop.isShadowItem()
                        && !itemDrop.isTimeLimitedItem()
                        && itemDrop.isDropable()
                        && itemDrop.getId() != 57
                        && itemDrop.getTemplate().getType2() != 3
                        && (!this.hasSummon() || this._summon.getControlObjectId() != itemDrop.getId())
                        && Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) < 0
                        && Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) < 0) {
                        if (itemDrop.isEquipped()) {
                           itemDropPercent = itemDrop.getTemplate().getType2() == 0 ? dropEquipWeapon : dropEquip;
                        } else {
                           itemDropPercent = dropItem;
                        }

                        if (Rnd.get(100) < itemDropPercent) {
                           if (itemDrop.isEquipped()) {
                              this._inventory.unEquipItemInSlot(itemDrop.getLocationSlot());
                           }

                           this.dropItem(ItemProcessType.DEATH, itemDrop, killer, true);
                           this.sendItemList(false);
                           if (isKarmaDrop) {
                              LOGGER.warning(this.getName() + " has karma and dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
                           } else {
                              LOGGER.warning(this.getName() + " dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
                           }

                           if (++dropCount >= dropLimit) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onKillUpdatePvPKarma(Creature target) {
      if (target != null && target.isPlayable()) {
         Player killedPlayer = target.asPlayer();
         if (killedPlayer != null && killedPlayer != this) {
            if (this.isCursedWeaponEquipped() && target.isPlayer()) {
               CursedWeaponsManager.getInstance().increaseKills(this._cursedWeaponEquippedId);
            } else if (!this.isInOlympiadMode() && !killedPlayer.isInOlympiadMode()) {
               if (!this.isInDuel() || !killedPlayer.isInDuel()) {
                  if (target.isPlayer() && this.isInsideZone(ZoneId.SIEGE) && killedPlayer.isInsideZone(ZoneId.SIEGE)) {
                     if (!this.isSiegeFriend(killedPlayer)) {
                        Clan targetClan = killedPlayer.getClan();
                        if (this._clan != null && targetClan != null) {
                           this._clan.addSiegeKill();
                           targetClan.addSiegeDeath();
                        }
                     }
                  } else if (!this.isInsideZone(ZoneId.PVP) && !target.isInsideZone(ZoneId.PVP)) {
                     if (this.checkIfPvP(target) && killedPlayer.getPvpFlag() != 0) {
                        this.increasePvpKills(target);
                     } else {
                        if (killedPlayer.getClan() != null
                           && this.getClan() != null
                           && this.getClan().isAtWarWith(killedPlayer.getClanId())
                           && killedPlayer.getClan().isAtWarWith(this.getClanId())
                           && killedPlayer.getPledgeType() != -1
                           && this.getPledgeType() != -1) {
                           this.increasePvpKills(target);
                           return;
                        }

                        if (killedPlayer.getKarma() > 0) {
                           if (PvpConfig.KARMA_AWARD_PK_KILL) {
                              this.increasePvpKills(target);
                           }
                        } else if (killedPlayer.getPvpFlag() == 0) {
                           if (!FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
                              this.increasePkKillsAndKarma(target);
                           } else if (this._isGood && killedPlayer.isGood() || this._isEvil && killedPlayer.isEvil()) {
                              this.increasePkKillsAndKarma(target);
                           }

                           this.checkItemRestriction();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void increasePvpKills(Creature target) {
      if (target.isPlayer() && AntiFeedManager.getInstance().check(this, target)) {
         this.setPvpKills(this._pvpKills + 1);
         this.updatePvpTitleAndColor(true);
         this.updateUserInfo();
      }
   }

   public void increasePkKillsAndKarma(Creature target) {
      if (target != null && target.isPlayable()) {
         this.setKarma(this.getKarma() + Formulas.calculateKarmaGain(this._pkKills, target.isSummon()));
         if (target.isPlayer()) {
            this.setPkKills(this._pkKills + 1);
         }

         this.updateUserInfo();
      }
   }

   public void updatePvpTitleAndColor(boolean broadcastInfo) {
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

         if (broadcastInfo) {
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

   public void updatePvPStatus(Creature target) {
      Player targetPlayer = target.asPlayer();
      if (targetPlayer != null) {
         if (this != targetPlayer) {
            if (!FactionSystemConfig.FACTION_SYSTEM_ENABLED
               || !target.isPlayer()
               || (!this.isGood() || !targetPlayer.isEvil()) && (!this.isEvil() || !targetPlayer.isGood())) {
               if (!this._isInDuel || targetPlayer.getDuelId() != this.getDuelId()) {
                  if ((!this.isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP)) && targetPlayer.getKarma() == 0) {
                     if (this.checkIfPvP(targetPlayer)) {
                        this.setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_PVP_TIME);
                     } else {
                        this.setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
                     }

                     if (this._pvpFlag == 0) {
                        this.startPvPFlag();
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isLucky() {
      return this.getLevel() <= 9 && this.isAffectedBySkill(CommonSkill.LUCKY.getId());
   }

   public void restoreExp(double restorePercent) {
      if (this._expBeforeDeath > 0L) {
         this.getStat().addExp(Math.round((this._expBeforeDeath - this.getExp()) * restorePercent / 100.0));
         this.setExpBeforeDeath(0L);
      }
   }

   public void calculateDeathExpPenalty(Creature killer, boolean atWar) {
      int level = this.getLevel();
      double percentLost = ExperienceLossData.getInstance().getPercentLost(this.getLevel());
      if (killer != null) {
         if (killer.isRaid()) {
            percentLost *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_RAID, 1.0);
         } else if (killer.isMonster()) {
            percentLost *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_MOB, 1.0);
         } else if (killer.isPlayable()) {
            percentLost *= this.calcStat(Stat.REDUCE_EXP_LOST_BY_PVP, 1.0);
         }
      }

      if (this.getKarma() > 0) {
         percentLost *= RatesConfig.RATE_KARMA_EXP_LOST;
      }

      long currentLevelExp = 0L;
      if (!this.isOnEvent()) {
         int maxLevel = ExperienceData.getInstance().getMaxLevel();
         if (level < maxLevel) {
            currentLevelExp = this.getStat().getExpForLevel(level + 1) - this.getStat().getExpForLevel(level);
         } else {
            currentLevelExp = this.getStat().getExpForLevel(maxLevel) - this.getStat().getExpForLevel(maxLevel - 1);
         }
      }

      long lostExp = 0L;
      if (currentLevelExp > 0L) {
         lostExp = Math.round(currentLevelExp * percentLost / 100.0);
         lostExp = Math.min(lostExp, currentLevelExp / 10L);
         if (this.isFestivalParticipant() || atWar) {
            lostExp /= 4L;
         }
      }

      this.setExpBeforeDeath(this.getExp());
      this.getStat().removeExp(lostExp);
   }

   public boolean isPartyWaiting() {
      return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
   }

   public void setPartyRoom(int id) {
      this._partyroom = id;
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

   public void setPet(Summon summon) {
      this._summon = summon;
   }

   public void setDecoy(Decoy decoy) {
      this._decoy = decoy;
   }

   public void setTrap(Trap trap) {
      this._trap = trap;
   }

   public Collection<TamedBeast> getTrainedBeasts() {
      return this._tamedBeast;
   }

   public void addTrainedBeast(TamedBeast tamedBeast) {
      if (this._tamedBeast == null) {
         this._tamedBeast = ConcurrentHashMap.newKeySet();
      }

      this._tamedBeast.add(tamedBeast);
   }

   public RequestPartner getRequest() {
      return this._request;
   }

   public void setActiveRequester(Player requester) {
      this._activeRequester = requester;
   }

   public Player getActiveRequester() {
      Player requester = this._activeRequester;
      if (requester != null && requester.isRequestExpired() && this._activeTradeList == null) {
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

   public void onTransactionRequest(Player partner) {
      this._requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + 150;
      partner.setActiveRequester(this);
   }

   public boolean isRequestExpired() {
      return this._requestExpireTime <= GameTimeTaskManager.getInstance().getGameTicks();
   }

   public void onTransactionResponse() {
      this._requestExpireTime = 0L;
   }

   public void setActiveWarehouse(ItemContainer warehouse) {
      this._activeWarehouse = warehouse;
   }

   public ItemContainer getActiveWarehouse() {
      return this._activeWarehouse;
   }

   public void setActiveTradeList(TradeList tradeList) {
      this._activeTradeList = tradeList;
   }

   public TradeList getActiveTradeList() {
      return this._activeTradeList;
   }

   public void onTradeStart(Player partner) {
      this._activeTradeList = new TradeList(this);
      this._activeTradeList.setPartner(partner);
      SystemMessage msg = new SystemMessage(SystemMessageId.YOU_BEGIN_TRADING_WITH_C1);
      msg.addPcName(partner);
      this.sendPacket(msg);
      this.sendPacket(new TradeStart(this));
   }

   public void onTradeConfirm(Player partner) {
      SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CONFIRMED_THE_TRADE);
      msg.addPcName(partner);
      this.sendPacket(msg);
      this.sendPacket(TradeOtherDone.STATIC_PACKET);
   }

   public void onTradeCancel(Player partner) {
      if (this._activeTradeList != null) {
         this._activeTradeList.lock();
         this._activeTradeList = null;
         this.sendPacket(new TradeDone(0));
         SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_CANCELLED_THE_TRADE);
         msg.addPcName(partner);
         this.sendPacket(msg);
      }
   }

   public void onTradeFinish(boolean successfull) {
      this._activeTradeList = null;
      this.sendPacket(new TradeDone(1));
      if (successfull) {
         this.sendPacket(SystemMessageId.YOUR_TRADE_IS_SUCCESSFUL);
      }
   }

   public void startTrade(Player partner) {
      this.onTradeStart(partner);
      partner.onTradeStart(this);
   }

   public void cancelActiveTrade() {
      if (this._activeTradeList != null) {
         Player partner = this._activeTradeList.getPartner();
         if (partner != null) {
            partner.onTradeCancel(this);
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

   public void setStoreName(String name) {
      this._storeName = name == null ? "" : name;
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

   public void setPrivateStoreType(PrivateStoreType privateStoreType) {
      this._privateStoreType = privateStoreType;
      if (OfflineTradeConfig.OFFLINE_DISCONNECT_FINISHED && privateStoreType == PrivateStoreType.NONE && (this._client == null || this._client.isDetached())) {
         OfflineTraderTable.getInstance().removeTrader(this.getObjectId());
         Disconnection.of(this).storeAndDelete();
      }
   }

   public PrivateStoreType getPrivateStoreType() {
      return this._privateStoreType;
   }

   public void setClan(Clan clan) {
      this._clan = clan;
      if (clan == null) {
         this.setTitle("");
         this._clanId = 0;
         this._clanPrivileges = new ClanPrivileges();
         this._pledgeType = 0;
         this._powerGrade = 0;
         this._lvlJoinedAcademy = 0;
         this._apprentice = 0;
         this._sponsor = 0;
         this._activeWarehouse = null;
      } else if (!clan.isMember(this.getObjectId())) {
         this.setClan(null);
      } else {
         this._clanId = clan.getId();
      }
   }

   @Override
   public Clan getClan() {
      return this._clan;
   }

   public boolean isClanLeader() {
      return this._clan != null && this.getObjectId() == this._clan.getLeaderId();
   }

   @Override
   protected void reduceArrowCount(boolean bolts) {
      Item arrows = this._inventory.getPaperdollItem(7);
      if (arrows == null) {
         this._inventory.unEquipItemInSlot(7);
         if (bolts) {
            this._boltItem = null;
         } else {
            this._arrowItem = null;
         }

         this.sendItemList(false);
      } else if (arrows.getCount() > 1L) {
         synchronized (arrows) {
            arrows.changeCount(null, -1L, this, null);
            arrows.setLastChange(2);
            this._inventory.refreshWeight();
         }

         InventoryUpdate iu = new InventoryUpdate();
         iu.addModifiedItem(arrows);
         this.sendInventoryUpdate(iu);
      } else {
         this._inventory.destroyItem(ItemProcessType.NONE, arrows, this, null);
         this._inventory.unEquipItemInSlot(7);
         if (bolts) {
            this._boltItem = null;
         } else {
            this._arrowItem = null;
         }

         this.sendItemList(false);
      }
   }

   @Override
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

   @Override
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
      Item wpn = this._inventory.getPaperdollItem(5);
      if (wpn == null) {
         return true;
      } else if (this.isCursedWeaponEquipped()) {
         return false;
      } else if (this._combatFlagEquippedId) {
         return false;
      } else if (wpn.getWeaponItem().isForceEquip()) {
         return false;
      } else {
         Collection<Item> unequipped = this._inventory.unEquipItemInBodySlotAndRecord(wpn.getTemplate().getBodyPart());
         InventoryUpdate iu = new InventoryUpdate();

         for (Item itm : unequipped) {
            iu.addModifiedItem(itm);
         }

         this.sendInventoryUpdate(iu);
         this.abortAttack();
         this.broadcastUserInfo();
         if (!unequipped.isEmpty()) {
            Item unequippedItem = unequipped.iterator().next();
            SystemMessage sm;
            if (unequippedItem.isEnchanted()) {
               sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               sm.addInt(unequippedItem.getEnchantLevel());
            } else {
               sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            sm.addItemName(unequippedItem);
            this.sendPacket(sm);
         }

         return true;
      }
   }

   public boolean disarmShield() {
      Item sld = this._inventory.getPaperdollItem(7);
      if (sld != null) {
         Collection<Item> unequipped = this._inventory.unEquipItemInBodySlotAndRecord(sld.getTemplate().getBodyPart());
         InventoryUpdate iu = new InventoryUpdate();

         for (Item itm : unequipped) {
            iu.addModifiedItem(itm);
         }

         this.sendInventoryUpdate(iu);
         this.abortAttack();
         this.broadcastUserInfo();
         if (!unequipped.isEmpty()) {
            SystemMessage sm = null;
            Item unequippedItem = unequipped.iterator().next();
            if (unequippedItem.isEnchanted()) {
               sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               sm.addInt(unequippedItem.getEnchantLevel());
            } else {
               sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            sm.addItemName(unequippedItem);
            this.sendPacket(sm);
         }
      }

      return true;
   }

   public boolean mount(Summon pet) {
      if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE && this.isInsideZone(ZoneId.SIEGE)) {
         MountType type = MountType.findByNpcId(pet.getId());
         boolean isAllowed = type == MountType.STRIDER
            && CastleManager.getInstance().getCastle(this) != null
            && this.isClanLeader()
            && this.getClanId() == CastleManager.getInstance().getCastle(this).getOwnerId();
         if (!isAllowed) {
            this.sendMessage("You cannot mount during a siege.");
            return false;
         }
      }

      if (this.disarmWeapons() && this.disarmShield() && !this.isTransformed()) {
         this.getEffectList().stopAllToggles();
         this.setMount(pet.getId(), pet.getLevel());
         this.setMountObjectID(pet.getControlObjectId());
         this.startFeed(pet.getId());
         this.broadcastPacket(new Ride(this));
         this.broadcastUserInfo();
         pet.unSummon(this);
         return true;
      } else {
         return false;
      }
   }

   public boolean mount(int npcId, int controlItemObjId, boolean useFood) {
      if (this.disarmWeapons() && this.disarmShield() && !this.isTransformed()) {
         this.getEffectList().stopAllToggles();
         this.setMount(npcId, this.getLevel());
         this.setMountObjectID(controlItemObjId);
         this.broadcastPacket(new Ride(this));
         this.broadcastUserInfo();
         if (useFood) {
            this.startFeed(npcId);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mountPlayer(Summon pet) {
      if (pet != null && pet.isMountable() && !this.isMounted() && !this.isBetrayed()) {
         if (this.isDead()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHEN_DEAD);
            return false;
         }

         if (pet.isDead()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_DEAD_STRIDER_CANNOT_BE_RIDDEN);
            return false;
         }

         if (pet.isInCombat() || pet.isRooted()) {
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

         if (pet.isHungry()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
            return false;
         }

         if (!LocationUtil.checkIfInRange(200, this, pet, true)) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            this.sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_AWAY_FROM_YOUR_MOUNT_TO_RIDE);
            return false;
         }

         if (!pet.isDead() && !this.isMounted()) {
            this.mount(pet);
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

      boolean wasFlying = this.isFlying();
      this.sendPacket(new SetupGauge(this.getObjectId(), 3, 0, 0));
      int petId = this._mountNpcId;
      this.setMount(0, 0);
      this.stopFeed();
      if (wasFlying) {
         this.removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
      }

      this.broadcastPacket(new Ride(this));
      this.setMountObjectID(0);
      this.storePetFood(petId);
      this.broadcastUserInfo();
      return true;
   }

   public void setUptime(long time) {
      this._uptime = time;
   }

   public long getUptime() {
      return System.currentTimeMillis() - this._uptime;
   }

   @Override
   public boolean isInvul() {
      return super.isInvul() || this.isTeleportProtected();
   }

   @Override
   public boolean isInParty() {
      return this._party != null;
   }

   public void setParty(Party party) {
      this._party = party;
   }

   public void joinParty(Party party) {
      if (party != null) {
         this._party = party;
         party.addPartyMember(this);
      }
   }

   public void leaveParty() {
      if (this.isInParty()) {
         this._party.removePartyMember(this, PartyMessageType.DISCONNECTED);
         this._party = null;
      }
   }

   @Override
   public Party getParty() {
      return this._party;
   }

   public void setPartyDistributionType(PartyDistributionType pdt) {
      this._partyDistributionType = pdt;
   }

   public PartyDistributionType getPartyDistributionType() {
      return this._partyDistributionType;
   }

   @Override
   public boolean isGM() {
      return this.getAccessLevel().isGm();
   }

   public void setAccessLevel(int level) {
      this._accessLevel = AdminData.getInstance().getAccessLevel(level);
      this._appearance.setNameColor(this._accessLevel.getNameColor());
      this._appearance.setTitleColor(this._accessLevel.getTitleColor());
      this.broadcastUserInfo();
      CharInfoTable.getInstance().addName(this);
      if (!AdminData.getInstance().hasAccessLevel(level)) {
         LOGGER.warning("Tried to set unregistered access level " + level + " for " + this + ". Setting access level without privileges!");
      } else if (level > 0) {
         LOGGER.warning(this._accessLevel.getName() + " access level set for character " + this.getName() + "! Just a warning to be careful ;)");
      }
   }

   public void setAccountAccesslevel(int level) {
      LoginServerThread.getInstance().sendAccessLevel(this.getAccountName(), level);
   }

   @Override
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
      World.forEachVisibleObject(this, Player.class, player -> {
         player.sendPacket(new RelationChanged(this, this.getRelation(player), this.isAutoAttackable(player)));
         if (this.hasSummon()) {
            player.sendPacket(new RelationChanged(this._summon, this.getRelation(player), this.isAutoAttackable(player)));
         }
      });
   }

   public void broadcastKarma() {
      StatusUpdate su = new StatusUpdate(this);
      su.addAttribute(27, this.getKarma());
      this.sendPacket(su);
      World.forEachVisibleObject(this, Player.class, player -> {
         player.sendPacket(new RelationChanged(this, this.getRelation(player), this.isAutoAttackable(player)));
         if (this.hasSummon()) {
            player.sendPacket(new RelationChanged(this._summon, this.getRelation(player), this.isAutoAttackable(player)));
         }
      });
   }

   public void setOnlineStatus(boolean isOnline, boolean updateInDb) {
      if (this._isOnline != isOnline) {
         this._isOnline = isOnline;
      }

      if (updateInDb) {
         this.updateOnlineStatus();
      }
   }

   public void setIn7sDungeon(boolean isIn7sDungeon) {
      this._isIn7sDungeon = isIn7sDungeon;
   }

   public void updateOnlineStatus() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
      ) {
         ps.setInt(1, this.isOnlineInt());
         ps.setLong(2, System.currentTimeMillis());
         ps.setInt(3, this.getObjectId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Failed updating character online status.", (Throwable)var9);
      }
   }

   private boolean createDb() {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate,lastAccess,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            );
         ) {
            ps.setString(1, this._accountName);
            ps.setInt(2, this.getObjectId());
            ps.setString(3, this.getName());
            ps.setInt(4, this.getLevel());
            ps.setInt(5, this.getMaxHp());
            ps.setDouble(6, this.getCurrentHp());
            ps.setInt(7, this.getMaxCp());
            ps.setDouble(8, this.getCurrentCp());
            ps.setInt(9, this.getMaxMp());
            ps.setDouble(10, this.getCurrentMp());
            ps.setInt(11, this._appearance.getFace());
            ps.setInt(12, this._appearance.getHairStyle());
            ps.setInt(13, this._appearance.getHairColor());
            ps.setInt(14, this._appearance.isFemale() ? 1 : 0);
            ps.setLong(15, this.getExp());
            ps.setLong(16, this.getSp());
            ps.setInt(17, this.getKarma());
            ps.setInt(18, this._fame);
            ps.setInt(19, this._pvpKills);
            ps.setInt(20, this._pkKills);
            ps.setInt(21, this._clanId);
            ps.setInt(22, this.getRace().ordinal());
            ps.setInt(23, this.getPlayerClass().getId());
            ps.setLong(24, this._deleteTimer);
            ps.setInt(25, this.hasDwarvenCraft() ? 1 : 0);
            ps.setString(26, this.getTitle());
            ps.setInt(27, this._appearance.getTitleColor());
            ps.setInt(28, this.getAccessLevel().getLevel());
            ps.setInt(29, this.isOnlineInt());
            ps.setInt(30, this._isIn7sDungeon ? 1 : 0);
            ps.setInt(31, this._clanPrivileges.getMask());
            ps.setInt(32, this._wantsPeace);
            ps.setInt(33, this._baseClass);
            ps.setInt(34, this._newbie);
            ps.setInt(35, this._noble ? 1 : 0);
            ps.setLong(36, 0L);
            ps.setTimestamp(37, new Timestamp(this._createDate.getTimeInMillis()));
            ps.setLong(38, System.currentTimeMillis());
            ps.setLong(39, System.currentTimeMillis());
            ps.executeUpdate();
         }

         return true;
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Could not insert char data: " + var9.getMessage(), (Throwable)var9);
         return false;
      }
   }

   private static Player restore(int objectId) {
      Player player = null;
      double currentCp = 0.0;
      double currentHp = 0.0;
      double currentMp = 0.0;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE charId=?");
      ) {
         ps.setInt(1, objectId);

         try (ResultSet rset = ps.executeQuery()) {
            if (rset.next()) {
               int activeClassId = rset.getInt("classid");
               boolean female = rset.getInt("sex") != Sex.MALE.ordinal();
               PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(activeClassId);
               PlayerAppearance app = new PlayerAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
               player = new Player(objectId, template, rset.getString("account_name"), app);
               player.setName(rset.getString("char_name"));
               player._lastAccess = rset.getLong("lastAccess");
               PlayerStat stat = player.getStat();
               stat.setExp(rset.getLong("exp"));
               player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
               stat.setLevel(rset.getByte("level"));
               stat.setSp(rset.getLong("sp"));
               player.setWantsPeace(rset.getInt("wantspeace"));
               player.setHeading(rset.getInt("heading"));
               player.setKarma(rset.getInt("karma"));
               player.setFame(rset.getInt("fame"));
               player.setPvpKills(rset.getInt("pvpkills"));
               player.setPkKills(rset.getInt("pkkills"));
               player.setOnlineTime(rset.getLong("onlinetime"));
               player.setNewbie(rset.getInt("newbie"));
               player.setNoble(rset.getInt("nobless") == 1);
               int factionId = rset.getInt("faction");
               if (factionId == 1) {
                  player.setGood();
               }

               if (factionId == 2) {
                  player.setEvil();
               }

               player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
               if (player.getClanJoinExpiryTime() < System.currentTimeMillis()) {
                  player.setClanJoinExpiryTime(0L);
               }

               player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
               if (player.getClanCreateExpiryTime() < System.currentTimeMillis()) {
                  player.setClanCreateExpiryTime(0L);
               }

               player.setPcCafePoints(rset.getInt("pccafe_points"));
               player.setPowerGrade(rset.getInt("power_grade"));
               player.setPledgeType(rset.getInt("subpledge"));
               player.setDeleteTimer(rset.getLong("deletetime"));
               player.setTitle(rset.getString("title"));
               player.setAccessLevel(rset.getInt("accesslevel"));
               int titleColor = rset.getInt("title_color");
               if (titleColor != 15530402) {
                  player.getAppearance().setTitleColor(titleColor);
               }

               player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
               player.setUptime(System.currentTimeMillis());
               currentHp = rset.getDouble("curHp");
               currentCp = rset.getDouble("curCp");
               currentMp = rset.getDouble("curMp");
               player.setLastRecomUpdate(rset.getLong("last_recom_date"));
               player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
               player._classIndex = 0;

               try {
                  player.setBaseClass(rset.getInt("base_class"));
               } catch (Exception var32) {
                  player.setBaseClass(activeClassId);
               }

               if (restoreSubClassData(player) && activeClassId != player.getBaseClass()) {
                  for (SubClassHolder subClass : player.getSubClasses().values()) {
                     if (subClass.getId() == activeClassId) {
                        player._classIndex = subClass.getClassIndex();
                     }
                  }
               }

               if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass()) {
                  player.setPlayerClass(player.getBaseClass());
                  LOGGER.warning(player + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
               } else {
                  player._activeClass = activeClassId;
               }

               player.setApprentice(rset.getInt("apprentice"));
               player.setSponsor(rset.getInt("sponsor"));
               player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
               player.setIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
               CursedWeaponsManager.getInstance().checkPlayer(player);
               player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
               player.setVitalityPoints(rset.getInt("vitality_points"), true);
               int x = rset.getInt("x");
               int y = rset.getInt("y");
               int z = GeoEngine.getInstance().getHeight(x, y, rset.getInt("z"));
               player.setXYZInvisible(x, y, z);
               player.setLastServerPosition(x, y, z);
               player.setBookmarkSlot(rset.getInt("BookmarkSlot"));
               player.getCreateDate().setTimeInMillis(rset.getTimestamp("createDate").getTime());
               player.setLang(rset.getString("language"));
               player.setHero(Hero.getInstance().isHero(objectId));
               int clanId = rset.getInt("clanid");
               Clan clan = null;
               if (clanId > 0) {
                  clan = ClanTable.getInstance().getClan(clanId);
                  player.setClan(clan);
                  if (clan != null && clan.isMember(objectId)) {
                     if (clan.getLeaderId() != player.getObjectId()) {
                        if (player.getPowerGrade() == 0) {
                           player.setPowerGrade(5);
                        }

                        player.setClanPrivileges(clan.getRankPrivs(player.getPowerGrade()));
                     } else {
                        player.getClanPrivileges().enableAll();
                        player.setPowerGrade(1);
                     }

                     player.setPledgeClass(ClanMember.calculatePledgeClass(player));
                  }
               }

               if (clan == null) {
                  if (player.isNoble()) {
                     player.setPledgeClass(5);
                  }

                  if (player.isHero()) {
                     player.setPledgeClass(8);
                  }

                  player.getClanPrivileges().disableAll();
               }

               try (PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?")) {
                  stmt.setString(1, player._accountName);
                  stmt.setInt(2, objectId);

                  try (ResultSet chars = stmt.executeQuery()) {
                     while (chars.next()) {
                        player._chars.put(chars.getInt("charId"), chars.getString("char_name"));
                     }
                  }
               }
            }
         }
      } catch (Exception var38) {
         LOGGER.log(Level.SEVERE, "Failed loading character.", (Throwable)var38);
      }

      if (player == null) {
         return null;
      } else {
         player.getInventory().restore();
         player.getWarehouse().restore();
         player.getFreight().restore();
         player.restoreSkills();
         player.getMacros().restoreMe();
         player.restoreShortcuts();
         player.restoreHenna();
         player.restoreTeleportBookmark();
         if (!PlayerConfig.ALT_RECOMMEND) {
            player.restoreRecom();
         }

         player.restoreRecipeBook(true);
         if (PlayerConfig.STORE_RECIPE_SHOPLIST) {
            player.restoreRecipeShopList();
         }

         player.loadPremiumItemList();
         player.restorePetInventoryItems();
         player.rewardSkills();
         player.restoreItemReuse();
         player.setCurrentCp(currentCp);
         player.setCurrentHp(currentHp);
         player.setCurrentMp(currentMp);
         player.setOriginalCpHpMp(currentCp, currentHp, currentMp);
         if (currentHp < 0.5) {
            player.setDead(true);
            player.stopHpMpRegeneration();
         }

         player.setPet(World.getPet(player.getObjectId()));
         if (player.hasSummon()) {
            player.getSummon().setOwner(player);
         }

         player.refreshOverloaded();
         player.refreshExpertisePenalty();
         player.restoreFriendList();
         player.setOnlineStatus(true, false);
         PlayerAutoSaveTaskManager.getInstance().add(player);
         return player;
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

   public void setMail(Forum forum) {
      this._forumMail = forum;
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

   public void setMemo(Forum forum) {
      this._forumMemo = forum;
   }

   private static boolean restoreSubClassData(Player player) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC"
         );
      ) {
         ps.setInt(1, player.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               SubClassHolder subClass = new SubClassHolder();
               subClass.setPlayerClass(rs.getInt("class_id"));
               subClass.setLevel(rs.getByte("level"));
               subClass.setExp(rs.getLong("exp"));
               subClass.setSp(rs.getLong("sp"));
               subClass.setClassIndex(rs.getInt("class_index"));
               player.getSubClasses().put(subClass.getClassIndex(), subClass);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + var12.getMessage(), (Throwable)var12);
      }

      return true;
   }

   private void restoreRecipeBook(boolean loadCommon) {
      String sql = loadCommon
         ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?"
         : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
      ) {
         ps.setInt(1, this.getObjectId());
         if (!loadCommon) {
            ps.setInt(2, this._classIndex);
         }

         try (ResultSet rs = ps.executeQuery()) {
            this._dwarvenRecipeBook.clear();
            RecipeData rd = RecipeData.getInstance();

            while (rs.next()) {
               RecipeList recipe = rd.getRecipeList(rs.getInt("id"));
               if (loadCommon) {
                  if (rs.getInt(2) == 1) {
                     if (rs.getInt(3) == this._classIndex) {
                        this.registerDwarvenRecipeList(recipe, false);
                     }
                  } else {
                     this.registerCommonRecipeList(recipe, false);
                  }
               } else {
                  this.registerDwarvenRecipeList(recipe, false);
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
      String sql = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._premiumItems.put(rs.getInt("itemNum"), new PremiumItem(rs.getInt("itemId"), rs.getLong("itemCount"), rs.getString("itemSender")));
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "Could not restore premium items: " + var13.getMessage(), (Throwable)var13);
      }
   }

   public void updatePremiumItem(int itemNum, long newcount) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? ");
      ) {
         ps.setLong(1, newcount);
         ps.setInt(2, this.getObjectId());
         ps.setInt(3, itemNum);
         ps.execute();
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Could not update premium items: " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void deletePremiumItem(int itemNum) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? ");
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, itemNum);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Could not delete premium item: " + var10);
      }
   }

   public synchronized void store(boolean storeActiveEffects) {
      this.storeCharBase();
      this.storeCharSub();
      this.storeEffect(storeActiveEffects);
      this.storeItemReuseDelay();
      if (PlayerConfig.STORE_RECIPE_SHOPLIST) {
         this.storeRecipeShopList();
      }

      SevenSigns.getInstance().saveSevenSignsData(this.getObjectId());
      PlayerVariables vars = this.getScript(PlayerVariables.class);
      if (vars != null) {
         vars.storeMe();
      }

      AccountVariables aVars = this.getScript(AccountVariables.class);
      if (aVars != null) {
         aVars.storeMe();
      }

      this.getInventory().updateDatabase();
      this.getWarehouse().updateDatabase();
      this.getFreight().updateDatabase();
   }

   @Override
   public void storeMe() {
      this.store(true);
   }

   private void storeCharBase() {
      long exp = this.getStat().getBaseExp();
      int level = this.getStat().getBaseLevel();
      long sp = this.getStat().getBaseSp();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=?,faction=?,pccafe_points=?,last_recom_date=?,rec_have=?,rec_left=? WHERE charId=?"
         );
      ) {
         ps.setInt(1, level);
         ps.setInt(2, this.getMaxHp());
         ps.setDouble(3, this.getCurrentHp());
         ps.setInt(4, this.getMaxCp());
         ps.setDouble(5, this.getCurrentCp());
         ps.setInt(6, this.getMaxMp());
         ps.setDouble(7, this.getCurrentMp());
         ps.setInt(8, this._appearance.getFace());
         ps.setInt(9, this._appearance.getHairStyle());
         ps.setInt(10, this._appearance.getHairColor());
         ps.setInt(11, this._appearance.isFemale() ? 1 : 0);
         ps.setInt(12, this.getHeading());
         ps.setInt(13, this._observerMode ? this._lastLoc.getX() : this.getX());
         ps.setInt(14, this._observerMode ? this._lastLoc.getY() : this.getY());
         ps.setInt(15, this._observerMode ? this._lastLoc.getZ() : this.getZ());
         ps.setLong(16, exp);
         ps.setLong(17, this._expBeforeDeath);
         ps.setLong(18, sp);
         ps.setInt(19, this.getKarma());
         ps.setInt(20, this._fame);
         ps.setInt(21, this._pvpKills);
         ps.setInt(22, this._pkKills);
         ps.setInt(23, this._clanId);
         ps.setInt(24, this.getRace().ordinal());
         ps.setInt(25, this.getPlayerClass().getId());
         ps.setLong(26, this._deleteTimer);
         ps.setString(27, this.getTitle());
         ps.setInt(28, this._appearance.getTitleColor());
         ps.setInt(29, this.getAccessLevel().getLevel());
         ps.setInt(30, this.isOnlineInt());
         ps.setInt(31, this._isIn7sDungeon ? 1 : 0);
         ps.setInt(32, this._clanPrivileges.getMask());
         ps.setInt(33, this._wantsPeace);
         ps.setInt(34, this._baseClass);
         long totalOnlineTime = this._onlineTime;
         if (this._onlineBeginTime > 0L) {
            totalOnlineTime += (System.currentTimeMillis() - this._onlineBeginTime) / 1000L;
         }

         ps.setLong(35, this._offlineShopStart > 0L ? this._onlineTime : totalOnlineTime);
         ps.setInt(36, this._newbie);
         ps.setInt(37, this._noble ? 1 : 0);
         ps.setInt(38, this._powerGrade);
         ps.setInt(39, this._pledgeType);
         ps.setInt(40, this._lvlJoinedAcademy);
         ps.setLong(41, this._apprentice);
         ps.setLong(42, this._sponsor);
         ps.setLong(43, this._clanJoinExpiryTime);
         ps.setLong(44, this._clanCreateExpiryTime);
         ps.setString(45, this.getName());
         ps.setLong(46, this._deathPenaltyBuffLevel);
         ps.setInt(47, this._bookmarkSlot);
         ps.setInt(48, this.getVitalityPoints());
         ps.setString(49, this._lang);
         int factionId = 0;
         if (this._isGood) {
            factionId = 1;
         }

         if (this._isEvil) {
            factionId = 2;
         }

         ps.setInt(50, factionId);
         ps.setInt(51, this._pcCafePoints);
         ps.setLong(52, this.getLastRecomUpdate());
         ps.setInt(53, this.getRecomHave());
         ps.setInt(54, this.getRecomLeft());
         ps.setInt(55, this.getObjectId());
         ps.execute();
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Could not store char base data: " + this + " - " + var15.getMessage(), (Throwable)var15);
      }
   }

   private void storeCharSub() {
      if (this.getTotalSubClasses() > 0) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?");
         ) {
            for (SubClassHolder subClass : this.getSubClasses().values()) {
               ps.setLong(1, subClass.getExp());
               ps.setLong(2, subClass.getSp());
               ps.setInt(3, subClass.getLevel());
               ps.setInt(4, subClass.getId());
               ps.setInt(5, this.getObjectId());
               ps.setInt(6, subClass.getClassIndex());
               ps.addBatch();
            }

            ps.executeBatch();
         } catch (Exception var9) {
            LOGGER.log(Level.WARNING, "Could not store sub class data for " + this.getName() + ": " + var9.getMessage(), (Throwable)var9);
         }
      }
   }

   @Override
   public void storeEffect(boolean storeEffects) {
      if (PlayerConfig.STORE_SKILL_COOLTIME) {
         try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement delete = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?")) {
               delete.setInt(1, this.getObjectId());
               delete.setInt(2, this._classIndex);
               delete.execute();
            }

            int buffIndex = 0;
            List<Integer> storedSkills = new ArrayList<>();
            long currentTime = System.currentTimeMillis();

            try (PreparedStatement statement = con.prepareStatement(
                  "REPLACE INTO character_skills_save (charId,skill_id,skill_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)"
               )) {
               if (storeEffects) {
                  for (BuffInfo info : this.getEffectList().getEffects()) {
                     if (info != null) {
                        Skill skill = info.getSkill();
                        if (skill.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS
                           && (!skill.isToggle() || PlayerConfig.ALT_STORE_TOGGLES)
                           && (!skill.isDance() || PlayerConfig.ALT_STORE_DANCES)
                           && !storedSkills.contains(skill.getReuseHashCode())) {
                           storedSkills.add(skill.getReuseHashCode());
                           statement.setInt(1, this.getObjectId());
                           statement.setInt(2, skill.getId());
                           statement.setInt(3, skill.getLevel());
                           statement.setInt(4, info.getTime());
                           TimeStamp t = this.getSkillReuseTimeStamp(skill.getReuseHashCode());
                           statement.setLong(5, t != null && currentTime < t.getStamp() ? t.getReuse() : 0L);
                           statement.setLong(6, t != null && currentTime < t.getStamp() ? t.getStamp() : 0L);
                           statement.setInt(7, 0);
                           statement.setInt(8, this._classIndex);
                           statement.setInt(9, ++buffIndex);
                           statement.addBatch();
                        }
                     }
                  }
               }

               for (Entry<Integer, TimeStamp> ts : this.getSkillReuseTimeStamps().entrySet()) {
                  int hash = ts.getKey();
                  if (!storedSkills.contains(hash)) {
                     TimeStamp t = ts.getValue();
                     if (t != null && currentTime < t.getStamp()) {
                        storedSkills.add(hash);
                        statement.setInt(1, this.getObjectId());
                        statement.setInt(2, t.getSkillId());
                        statement.setInt(3, t.getSkillLevel());
                        statement.setInt(4, -1);
                        statement.setLong(5, t.getReuse());
                        statement.setLong(6, t.getStamp());
                        statement.setInt(7, 1);
                        statement.setInt(8, this._classIndex);
                        statement.setInt(9, ++buffIndex);
                        statement.addBatch();
                     }
                  }
               }

               statement.executeBatch();
            }
         } catch (Exception var18) {
            LOGGER.log(Level.WARNING, "Could not store char effect data: ", (Throwable)var18);
         }
      }
   }

   private void storeItemReuseDelay() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement("DELETE FROM character_item_reuse_save WHERE charId=?");
         PreparedStatement ps2 = con.prepareStatement("INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)");
      ) {
         ps1.setInt(1, this.getObjectId());
         ps1.execute();
         long currentTime = System.currentTimeMillis();

         for (TimeStamp ts : this.getItemReuseTimeStamps().values()) {
            if (ts != null && currentTime < ts.getStamp()) {
               ps2.setInt(1, this.getObjectId());
               ps2.setInt(2, ts.getItemId());
               ps2.setInt(3, ts.getItemObjectId());
               ps2.setLong(4, ts.getReuse());
               ps2.setLong(5, ts.getStamp());
               ps2.addBatch();
            }
         }

         ps2.executeBatch();
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
         String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
         this.sendMessage("Only " + limit + " offline players allowed per IP.");
      } else if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP > 0
         && !AntiFeedManager.getInstance().tryAddPlayer(4, this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP)) {
         String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
         this.sendMessage("Only " + limit + " offline players allowed per IP.");
      } else {
         AntiFeedManager.getInstance().removePlayer(0, this);
         this.sendPacket(LeaveWorld.STATIC_PACKET);
         if (OfflinePlayConfig.OFFLINE_PLAY_SET_NAME_COLOR) {
            this.getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
         }

         if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty()) {
            this.startAbnormalVisualEffect(
               true,
               new AbnormalVisualEffect[]{OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size()))}
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

   public void setOfflinePlay(boolean value) {
      this._offlinePlay = value;
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

   @Override
   public Skill addSkill(Skill newSkill) {
      this.addCustomSkill(newSkill);
      return super.addSkill(newSkill);
   }

   public Skill addSkill(Skill newSkill, boolean store) {
      Skill oldSkill = this.addSkill(newSkill);
      if (store) {
         this.storeSkill(newSkill, oldSkill, -1);
      }

      return oldSkill;
   }

   @Override
   public Skill removeSkill(Skill skill, boolean store) {
      this.removeCustomSkill(skill);
      return store ? this.removeSkill(skill) : super.removeSkill(skill, true);
   }

   public Skill removeSkill(Skill skill, boolean store, boolean cancelEffect) {
      this.removeCustomSkill(skill);
      return store ? this.removeSkill(skill) : super.removeSkill(skill, cancelEffect);
   }

   public Skill removeSkill(Skill skill) {
      this.removeCustomSkill(skill);
      Skill oldSkill = super.removeSkill(skill, true);
      if (oldSkill != null) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?");
         ) {
            ps.setInt(1, oldSkill.getId());
            ps.setInt(2, this.getObjectId());
            ps.setInt(3, this._classIndex);
            ps.execute();
         } catch (Exception var11) {
            LOGGER.log(Level.WARNING, "Error could not delete skill: " + var11.getMessage(), (Throwable)var11);
         }
      }

      if (this.getTransformationId() <= 0 && !this.isCursedWeaponEquipped()) {
         if (skill != null) {
            for (Shortcut sc : this._shortcuts.getAllShortcuts()) {
               if (sc != null && sc.getId() == skill.getId() && sc.getType() == ShortcutType.SKILL && (skill.getId() < 3080 || skill.getId() > 3259)) {
                  this.deleteShortcut(sc.getSlot(), sc.getPage());
               }
            }
         }

         return oldSkill;
      } else {
         return oldSkill;
      }
   }

   private void storeSkill(Skill newSkill, Skill oldSkill, int newClassIndex) {
      int classIndex = newClassIndex > -1 ? newClassIndex : this._classIndex;

      try (Connection con = DatabaseFactory.getConnection()) {
         if (oldSkill != null && newSkill != null) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?")) {
               ps.setInt(1, newSkill.getLevel());
               ps.setInt(2, oldSkill.getId());
               ps.setInt(3, this.getObjectId());
               ps.setInt(4, classIndex);
               ps.execute();
            }
         } else if (newSkill != null) {
            try (PreparedStatement ps = con.prepareStatement("REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)")) {
               ps.setInt(1, this.getObjectId());
               ps.setInt(2, newSkill.getId());
               ps.setInt(3, newSkill.getLevel());
               ps.setInt(4, classIndex);
               ps.execute();
            }
         }
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Error could not store char skills: " + var15.getMessage(), (Throwable)var15);
      }
   }

   private void storeSkills(List<Skill> newSkills, int newClassIndex) {
      if (!newSkills.isEmpty()) {
         int classIndex = newClassIndex > -1 ? newClassIndex : this._classIndex;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)");
         ) {
            for (Skill addSkill : newSkills) {
               ps.setInt(1, this.getObjectId());
               ps.setInt(2, addSkill.getId());
               ps.setInt(3, addSkill.getLevel());
               ps.setInt(4, classIndex);
               ps.addBatch();
            }

            ps.executeBatch();
         } catch (SQLException var12) {
            LOGGER.log(Level.WARNING, "Error could not store char skills: " + var12.getMessage(), (Throwable)var12);
         }
      }
   }

   private void restoreSkills() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?");
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, this._classIndex);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int id = rs.getInt("skill_id");
               int level = rs.getInt("skill_level");
               Skill skill = SkillData.getInstance().getSkill(id, level);
               if (skill == null) {
                  LOGGER.warning("Skipped null skill Id: " + id + " Level: " + level + " while restoring player skills for playerObjId: " + this.getObjectId());
               } else {
                  this.addSkill(skill);
                  if (GeneralConfig.SKILL_CHECK_ENABLE
                     && (!this.isGM() || GeneralConfig.SKILL_CHECK_GM)
                     && !SkillTreeData.getInstance().isSkillAllowed(this, skill)) {
                     PunishmentManager.handleIllegalPlayerAction(
                        this,
                        "Player "
                           + this.getName()
                           + " has invalid skill "
                           + skill.getName()
                           + " ("
                           + skill.getId()
                           + "/"
                           + skill.getLevel()
                           + "), class:"
                           + ClassListData.getInstance().getClass(this.getPlayerClass()).getClassName(),
                        IllegalActionPunishmentType.BROADCAST
                     );
                     if (GeneralConfig.SKILL_CHECK_REMOVE) {
                        this.removeSkill(skill);
                     }
                  }
               }
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Could not restore character " + this + " skills: " + var13.getMessage(), (Throwable)var13);
      }
   }

   @Override
   public void restoreEffects() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT skill_id,skill_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC"
         );
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, this._classIndex);

         try (ResultSet rs = ps.executeQuery()) {
            long currentTime = System.currentTimeMillis();

            while (rs.next()) {
               Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
               if (skill != null) {
                  long systime = rs.getLong("systime");
                  long time = systime - currentTime;
                  if (time > 10L) {
                     this.disableSkill(skill, time);
                     this.addTimeStamp(skill, rs.getLong("reuse_delay"), systime);
                  }

                  if (rs.getInt("restore_type") <= 0) {
                     skill.applyEffects(this, this, false, rs.getInt("remaining_time"));
                  }
               }
            }
         }

         try (PreparedStatement delete = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?")) {
            delete.setInt(1, this.getObjectId());
            delete.setInt(2, this._classIndex);
            delete.executeUpdate();
         }
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + var19.getMessage(), (Throwable)var19);
      }
   }

   private void restoreItemReuse() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?");
         PreparedStatement delete = con.prepareStatement("DELETE FROM character_item_reuse_save WHERE charId=?");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            long currentTime = System.currentTimeMillis();

            while (rs.next()) {
               int itemId = rs.getInt("itemId");
               long reuseDelay = rs.getLong("reuseDelay");
               long systime = rs.getLong("systime");
               boolean isInInventory = true;
               Item item = this._inventory.getItemByItemId(itemId);
               if (item == null) {
                  item = this.getWarehouse().getItemByItemId(itemId);
                  isInInventory = false;
               }

               if (item != null && item.getId() == itemId && item.getReuseDelay() > 0) {
                  long remainingTime = systime - currentTime;
                  if (remainingTime > 10L) {
                     this.addTimeStampItem(item, reuseDelay, systime);
                     if (isInInventory && item.isEtcItem()) {
                        int group = item.getSharedReuseGroup();
                        if (group > 0) {
                           this.sendPacket(new ExUseSharedGroupItem(itemId, group, (int)remainingTime, (int)reuseDelay));
                        }
                     }
                  }
               }
            }
         }

         delete.setInt(1, this.getObjectId());
         delete.executeUpdate();
      } catch (Exception var25) {
         LOGGER.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + var25.getMessage(), (Throwable)var25);
      }
   }

   private void restoreHenna() {
      for (int i = 0; i < 3; i++) {
         this._henna[i] = null;
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?");
      ) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, this._classIndex);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int slot = rs.getInt("slot");
               if (slot >= 1 && slot <= 3) {
                  int symbolId = rs.getInt("symbol_id");
                  if (symbolId != 0) {
                     this._henna[slot - 1] = HennaData.getInstance().getHenna(symbolId);
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
      int totalSlots = 0;
      if (this.getPlayerClass().level() == 1) {
         totalSlots = 2;
      } else if (this.getPlayerClass().level() > 1) {
         totalSlots = 3;
      }

      for (int i = 0; i < 3; i++) {
         if (this._henna[i] != null) {
            totalSlots--;
         }
      }

      return totalSlots <= 0 ? 0 : totalSlots;
   }

   public boolean removeHenna(int slot) {
      if (slot >= 1 && slot <= 3) {
         Henna henna = this._henna[slot - 1];
         if (henna == null) {
            return false;
         } else {
            this._henna[slot - 1] = null;

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?");
            ) {
               statement.setInt(1, this.getObjectId());
               statement.setInt(2, slot);
               statement.setInt(3, this._classIndex);
               statement.execute();
            } catch (Exception var11) {
               LOGGER.log(Level.SEVERE, "Failed removing character henna.", (Throwable)var11);
            }

            this.recalcHennaStats();
            this.sendPacket(new HennaInfo(this));
            this.updateUserInfo();
            this._inventory.addItem(ItemProcessType.RESTORE, henna.getDyeItemId(), henna.getCancelCount(), this, null);
            this.reduceAdena(ItemProcessType.FEE, henna.getCancelFee(), this, false);
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
            sm.addItemName(henna.getDyeItemId());
            sm.addLong(henna.getCancelCount());
            this.sendPacket(sm);
            this.sendPacket(SystemMessageId.THE_SYMBOL_HAS_BEEN_DELETED);
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean addHenna(Henna henna) {
      for (int i = 0; i < 3; i++) {
         if (this._henna[i] == null) {
            this._henna[i] = henna;
            this.recalcHennaStats();

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement("INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)");
            ) {
               ps.setInt(1, this.getObjectId());
               ps.setInt(2, henna.getDyeId());
               ps.setInt(3, i + 1);
               ps.setInt(4, this._classIndex);
               ps.execute();
            } catch (Exception var11) {
               LOGGER.log(Level.SEVERE, "Failed saving character henna.", (Throwable)var11);
            }

            this.sendPacket(new HennaInfo(this));
            this.updateUserInfo();
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
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

      for (Henna h : this._henna) {
         if (h != null) {
            this._hennaINT = this._hennaINT + (this._hennaINT + h.getStatINT() > 5 ? 5 - this._hennaINT : h.getStatINT());
            this._hennaSTR = this._hennaSTR + (this._hennaSTR + h.getStatSTR() > 5 ? 5 - this._hennaSTR : h.getStatSTR());
            this._hennaMEN = this._hennaMEN + (this._hennaMEN + h.getStatMEN() > 5 ? 5 - this._hennaMEN : h.getStatMEN());
            this._hennaCON = this._hennaCON + (this._hennaCON + h.getStatCON() > 5 ? 5 - this._hennaCON : h.getStatCON());
            this._hennaWIT = this._hennaWIT + (this._hennaWIT + h.getStatWIT() > 5 ? 5 - this._hennaWIT : h.getStatWIT());
            this._hennaDEX = this._hennaDEX + (this._hennaDEX + h.getStatDEX() > 5 ? 5 - this._hennaDEX : h.getStatDEX());
         }
      }
   }

   public Henna getHenna(int slot) {
      return slot >= 1 && slot <= 3 ? this._henna[slot - 1] : null;
   }

   public boolean hasHennas() {
      for (Henna henna : this._henna) {
         if (henna != null) {
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

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (attacker == null) {
         return false;
      } else if (this.isInvisible() && this.isGM()) {
         return false;
      } else if (attacker == this || attacker == this._summon) {
         return false;
      } else if (attacker instanceof FriendlyMob) {
         return false;
      } else if (attacker.isMonster()) {
         return true;
      } else {
         Player attackerPlayer = attacker.asPlayer();
         if (attacker.isPlayable() && this._duelState == 1 && this.getDuelId() == attackerPlayer.getDuelId()) {
            return true;
         } else if (this.isInParty() && this._party.getMembers().contains(attacker)) {
            return false;
         } else if (attacker.isPlayer() && attackerPlayer.isInOlympiadMode()) {
            return this._inOlympiadMode && this._olympiadStart && attacker.asPlayer().getOlympiadGameId() == this.getOlympiadGameId();
         } else if (this.isOnEvent()) {
            return this.isOnSoloEvent() || this.getTeam() != attacker.getTeam();
         } else {
            if (attacker.isPlayable()) {
               if (this.isInsideZone(ZoneId.PEACE) || this.isInsideZone(ZoneId.NO_PVP)) {
                  return false;
               }

               Clan clan = this.getClan();
               Clan attackerClan = attackerPlayer.getClan();
               if (clan != null && attackerClan != null) {
                  if (clan != attackerClan) {
                     Siege siege = SiegeManager.getInstance().getSiege(this.getX(), this.getY(), this.getZ());
                     if (siege != null) {
                        if (siege.checkIsDefender(attackerClan) && siege.checkIsDefender(clan)) {
                           return false;
                        }

                        if (siege.checkIsAttacker(attackerClan) && siege.checkIsAttacker(clan)) {
                           Castle castle = CastleManager.getInstance().getCastleById(this._siegeSide);
                           return castle != null && castle.isFirstMidVictory();
                        }
                     }
                  }

                  if (this.getWantsPeace() == 0
                     && attackerPlayer.getWantsPeace() == 0
                     && !this.isAcademyMember()
                     && clan.isAtWarWith(attackerPlayer.getClanId())
                     && attackerClan.isAtWarWith(this.getClanId())) {
                     return true;
                  }
               }

               if (this.isInsideZone(ZoneId.PVP)
                  && attackerPlayer.isInsideZone(ZoneId.PVP)
                  && (!this.isInsideZone(ZoneId.SIEGE) || !attackerPlayer.isInsideZone(ZoneId.SIEGE))) {
                  return true;
               }

               if (clan != null && clan.isMember(attacker.getObjectId())) {
                  return false;
               }

               if (attacker.isPlayer() && this.getAllyId() != 0 && this.getAllyId() == attackerPlayer.getAllyId()) {
                  return false;
               }

               if (this.isInsideZone(ZoneId.PVP)
                  && attackerPlayer.isInsideZone(ZoneId.PVP)
                  && this.isInsideZone(ZoneId.SIEGE)
                  && attackerPlayer.isInsideZone(ZoneId.SIEGE)) {
                  return true;
               }

               if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && (this.isGood() && attackerPlayer.isEvil() || this.isEvil() && attackerPlayer.isGood())) {
                  return true;
               }
            }

            if (attacker instanceof Defender && this._clan != null) {
               Siege siege = SiegeManager.getInstance().getSiege((WorldObject)this);
               return siege != null && siege.checkIsAttacker(this._clan);
            } else if (!(attacker instanceof Guard)) {
               return this.getKarma() > 0 || this._pvpFlag > 0;
            } else {
               return !FactionSystemConfig.FACTION_SYSTEM_ENABLED
                     || !FactionSystemConfig.FACTION_GUARDS_ENABLED
                     || (!this._isGood || !attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME))
                        && (!this._isEvil || !attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))
                  ? this.getKarma() > 0
                  : true;
            }
         }
      }
   }

   @Override
   public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove) {
      if (skill.isPassive()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this.isCastingNow()) {
         if (this._currentSkill != null && skill.getId() == this._currentSkill.getSkillId()) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else if (this.isSkillDisabled(skill)) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else {
            this.setQueuedSkill(skill, forceUse, dontMove);
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         }
      } else {
         this.setCastingNow(true);
         this.setCurrentSkill(skill, forceUse, dontMove);
         if (this._queuedSkill != null) {
            this.setQueuedSkill(null, false, false);
         }

         if (!this.checkUseMagicConditions(skill, forceUse, dontMove)) {
            this.setCastingNow(false);
            return false;
         } else {
            WorldObject target = null;

            this.getAI().setIntentionCast(skill, (WorldObject)(switch (skill.getTargetType()) {
               case AURA, FRONT_AURA, BEHIND_AURA, GROUND, SELF, AURA_CORPSE_MOB, COMMAND_CHANNEL, AURA_FRIENDLY -> this;
               default -> skill.getFirstOfTargetList(this);
            }));
            return true;
         }
      }
   }

   private boolean checkUseMagicConditions(Skill skill, boolean forceUse, boolean dontMove) {
      if (this.isOutOfControl() || this.isParalyzed() || this.isStunned() || this.isSleeping()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this.isDead()) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this._fishing && !skill.hasEffectType(EffectType.FISHING, EffectType.FISHING_START)) {
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
      } else if (skill.isToggle() && this.isAffectedBySkill(skill.getId())) {
         this.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else if (this._isFakeDeath) {
         this.sendPacket(ActionFailed.STATIC_PACKET);
         return false;
      } else {
         WorldObject target = null;
         TargetType sklTargetType = skill.getTargetType();
         if (sklTargetType == TargetType.GROUND && this._currentSkillWorldPosition == null) {
            this.sendPacket(ActionFailed.STATIC_PACKET);
            return false;
         } else {
            target = (WorldObject)(switch (sklTargetType) {
               case AURA, FRONT_AURA, BEHIND_AURA, GROUND, SELF, AURA_CORPSE_MOB, COMMAND_CHANNEL, AURA_FRIENDLY, PARTY, CLAN, PARTY_CLAN, AREA_SUMMON -> this;
               case PET, SERVITOR, SUMMON -> this._summon;
               default -> this.getTarget();
            });
            if (target == null) {
               this.sendPacket(ActionFailed.STATIC_PACKET);
               return false;
            } else {
               if (target.isDoor()) {
                  Door door = target.asDoor();
                  if (door.isInsideZone(ZoneId.SIEGE)) {
                     Castle castle = door.getCastle();
                     if (castle != null && castle.getResidenceId() > 0 && !castle.getSiege().isInProgress()) {
                        this.sendPacket(SystemMessageId.INVALID_TARGET);
                        return false;
                     }

                     Fort fort = door.getFort();
                     if (fort != null && fort.getResidenceId() > 0 && (!fort.getSiege().isInProgress() || !door.isShowHp())) {
                        this.sendPacket(SystemMessageId.INVALID_TARGET);
                        return false;
                     }
                  } else if (skill.getTargetType() != TargetType.UNLOCKABLE) {
                     this.sendPacket(SystemMessageId.INVALID_TARGET);
                     return false;
                  }
               }

               if (this._isInDuel && target.isPlayable()) {
                  Player cha = target.asPlayer();
                  if (cha.getDuelId() != this.getDuelId()) {
                     this.sendMessage("You cannot do this while duelling.");
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return false;
                  }
               }

               if (this.isSkillDisabled(skill)) {
                  SystemMessage sm;
                  if (this.hasSkillReuse(skill.getReuseHashCode())) {
                     int remainingTime = (int)(this.getSkillRemainingReuseTime(skill.getReuseHashCode()) / 1000L);
                     int hours = remainingTime / 3600;
                     int minutes = remainingTime % 3600 / 60;
                     int seconds = remainingTime % 60;
                     if (hours > 0) {
                        sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        sm.addSkillName(skill);
                        sm.addInt(hours);
                        sm.addInt(minutes);
                     } else if (minutes > 0) {
                        sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        sm.addSkillName(skill);
                        sm.addInt(minutes);
                     } else {
                        sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
                        sm.addSkillName(skill);
                     }

                     sm.addInt(seconds);
                  } else {
                     sm = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
                     sm.addSkillName(skill);
                  }

                  this.sendPacket(sm);
                  return false;
               } else if (!skill.checkCondition(this, target, false)) {
                  this.sendPacket(ActionFailed.STATIC_PACKET);
                  return false;
               } else {
                  if (skill.hasNegativeEffect()) {
                     if (this.isInsidePeaceZone(this, target) && !this.getAccessLevel().allowPeaceAttack()) {
                        this.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (this._inOlympiadMode && !this._olympiadStart) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (this.isSiegeFriend(target)) {
                        if (TerritoryWarManager.getInstance().isTWInProgress()) {
                           this.sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
                        } else {
                           this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
                        }

                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (!target.canBeAttacked() && !this.getAccessLevel().allowPeaceAttack() && !target.isDoor()) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (target instanceof EventMonster && ((EventMonster)target).eventSkillAttackBlocked()) {
                        this.sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                     }

                     if (!target.isAutoAttackable(this) && !forceUse) {
                        switch (sklTargetType) {
                           case AURA:
                           case FRONT_AURA:
                           case BEHIND_AURA:
                           case GROUND:
                           case SELF:
                           case AURA_CORPSE_MOB:
                           case AURA_FRIENDLY:
                           case PARTY:
                           case CLAN:
                           case AREA_SUMMON:
                           case UNLOCKABLE:
                              break;
                           case COMMAND_CHANNEL:
                           case PARTY_CLAN:
                           case PET:
                           case SERVITOR:
                           case SUMMON:
                           default:
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                        }
                     }

                     if (dontMove) {
                        if (sklTargetType == TargetType.GROUND) {
                           if (!this.isInsideRadius2D(
                              this._currentSkillWorldPosition.getX(),
                              this._currentSkillWorldPosition.getY(),
                              this._currentSkillWorldPosition.getZ(),
                              skill.getCastRange() + this.getTemplate().getCollisionRadius()
                           )) {
                              this.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                           }
                        } else if (skill.getCastRange() > 0 && !this.isInsideRadius2D(target, skill.getCastRange() + this.getTemplate().getCollisionRadius())) {
                           this.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
                           this.sendPacket(ActionFailed.STATIC_PACKET);
                           return false;
                        }
                     }
                  }

                  if (skill.getEffectPoint() > 0 && target.isMonster() && !forceUse) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return false;
                  } else {
                     switch (sklTargetType) {
                        case AURA_CORPSE_MOB:
                        case COMMAND_CHANNEL:
                        case AURA_FRIENDLY:
                        default:
                           if (target.isPlayable() && !this.getAccessLevel().allowPeaceAttack() && !this.checkPvpSkill(target, skill)) {
                              this.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
                              this.sendPacket(ActionFailed.STATIC_PACKET);
                              return false;
                           }
                        case AURA:
                        case FRONT_AURA:
                        case BEHIND_AURA:
                        case GROUND:
                        case SELF:
                        case PARTY:
                        case CLAN:
                        case PARTY_CLAN:
                        case AREA_SUMMON:
                           if (skill.getCastRange() > 0) {
                              if (sklTargetType == TargetType.GROUND) {
                                 if (!GeoEngine.getInstance().canSeeTarget(this, this._currentSkillWorldPosition)) {
                                    this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                                    this.sendPacket(ActionFailed.STATIC_PACKET);
                                    return false;
                                 }
                              } else if (!GeoEngine.getInstance().canSeeTarget(this, target)) {
                                 this.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
                                 this.sendPacket(ActionFailed.STATIC_PACKET);
                                 return false;
                              }
                           }

                           if (skill.getFlyType() == FlyType.CHARGE
                              && !GeoEngine.getInstance()
                                 .canMoveToTarget(this.getX(), this.getY(), this.getZ(), target.getX(), target.getY(), target.getZ(), this.getInstanceId())) {
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

   public boolean isInLooterParty(int looterId) {
      Player looter = World.getPlayer(looterId);
      return this.isInParty() && this._party.isInCommandChannel() && looter != null
         ? this._party.getCommandChannel().getMembers().contains(looter)
         : this.isInParty() && looter != null && this._party.getMembers().contains(looter);
   }

   public boolean checkPvpSkill(WorldObject target, Skill skill) {
      if (skill == null || target == null) {
         return false;
      } else if (!target.isPlayable()) {
         return true;
      } else if (!skill.isDebuff() && !skill.hasEffectType(EffectType.STEAL_ABNORMAL) && !skill.hasNegativeEffect()) {
         return true;
      } else {
         Player targetPlayer = target.asPlayer();
         if (targetPlayer != null && this != target) {
            if (this.isInDuel() && targetPlayer.isInDuel() && this.getDuelId() == targetPlayer.getDuelId()) {
               return true;
            } else if (this.isInOlympiadMode() && targetPlayer.isInOlympiadMode() && this.getOlympiadGameId() == targetPlayer.getOlympiadGameId()) {
               return true;
            } else if (target.isInsideZone(ZoneId.PEACE)) {
               return false;
            } else if (skill.isPvPOnly()
               && targetPlayer.getPvpFlag() == 0
               && targetPlayer.getKarma() == 0
               && !this.isInsideZone(ZoneId.PVP)
               && !this.isInsideZone(ZoneId.SIEGE)) {
               return false;
            } else if (this.isSiegeFriend(targetPlayer)) {
               this.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
               return false;
            } else {
               boolean isCtrlPressed = this._currentSkill != null && this._currentSkill.isCtrlPressed();
               if (!this.isInParty()
                  || !targetPlayer.isInParty()
                  || this.getParty().getLeader() != targetPlayer.getParty().getLeader()
                     && (this._party.getCommandChannel() == null || !this._party.getCommandChannel().containsPlayer(targetPlayer))) {
                  if (this.isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)) {
                     return true;
                  } else {
                     Clan tClan = targetPlayer.getClan();
                     if (this._clan != null && tClan != null) {
                        if (this._clan.isAtWarWith(tClan.getId()) && tClan.isAtWarWith(this._clan.getId())) {
                           return true;
                        }

                        if (this.getClanId() == targetPlayer.getClanId() || this.getAllyId() > 0 && this.getAllyId() == targetPlayer.getAllyId()) {
                           return skill.getEffectRange() > 0 && isCtrlPressed && this.getTarget() == target && skill.isDamage();
                        }
                     }

                     return targetPlayer.getPvpFlag() == 0 && targetPlayer.getKarma() == 0
                        ? skill.getEffectRange() > 0 && isCtrlPressed && this.getTarget() == target && skill.isDamage()
                        : targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0;
                  }
               } else {
                  return skill.getEffectRange() > 0 && isCtrlPressed && this.getTarget() == target && skill.isDamage();
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

   public void setMount(int npcId, int npcLevel) {
      MountType type = MountType.findByNpcId(npcId);
      switch (type) {
         case NONE:
            if (this.isFlying()) {
               this.removeSkill(CommonSkill.WYVERN_BREATH.getSkill().getId(), false);
               this.setFlying(false);
               this.sendSkillList();
            }
            break;
         case WYVERN:
            this.setFlying(true);
            this.addSkill(CommonSkill.WYVERN_BREATH.getSkill(), false);
            this.sendSkillList();
      }

      this._mountType = type;
      this._mountNpcId = npcId;
      this._mountLevel = npcLevel;
   }

   public MountType getMountType() {
      return this._mountType;
   }

   @Override
   public void stopAllEffects() {
      super.stopAllEffects();
      this.updateAndBroadcastStatus();
   }

   @Override
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
         for (Cubic cubic : this._cubics.values()) {
            cubic.stopAction();
            cubic.cancelDisappear();
         }

         this._cubics.clear();
         this._cubicOrder.clear();
         this.broadcastUserInfo();
      }
   }

   public void stopCubicsByOthers() {
      if (!this._cubics.isEmpty()) {
         boolean broadcast = false;

         for (Cubic cubic : this._cubics.values()) {
            if (cubic.givenByOther()) {
               this.removeCubic(cubic.getId());
               broadcast = true;
            }
         }

         if (broadcast) {
            this.broadcastUserInfo();
         }
      }
   }

   @Override
   public void updateAbnormalEffect() {
      this.broadcastUserInfo();
   }

   public void setInventoryBlockingStatus(boolean value) {
      this._inventoryDisable = value;
      if (value) {
         ThreadPool.schedule(new InventoryEnableTask(this), 1500L);
      }
   }

   public boolean isInventoryDisabled() {
      return this._inventoryDisable;
   }

   public Cubic addCubic(
      int cubicId, int level, double cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther
   ) {
      Cubic cubic = new Cubic(this, cubicId, level, (int)cubicPower, cubicDelay, cubicSkillChance, cubicMaxCount, cubicDuration, givenByOther);
      Cubic oldCubic = this._cubics.put(cubicId, cubic);
      this._cubicOrder.remove(cubicId);
      this._cubicOrder.add(cubicId);
      return oldCubic;
   }

   public Cubic removeCubic(int cubicId) {
      Cubic cubic = this._cubics.remove(cubicId);
      this._cubicOrder.remove(cubicId);
      if (cubic != null) {
         cubic.stopAction();
         cubic.cancelDisappear();
      }

      return cubic;
   }

   public Cubic removeFirstCubic() {
      while (!this._cubicOrder.isEmpty()) {
         Integer cubicId = this._cubicOrder.poll();
         if (cubicId == null) {
            return null;
         }

         Cubic cubic = this._cubics.remove(cubicId);
         if (cubic != null) {
            cubic.stopAction();
            cubic.cancelDisappear();
            return cubic;
         }
      }

      return null;
   }

   public Map<Integer, Cubic> getCubics() {
      return Collections.unmodifiableMap(this._cubics);
   }

   public Cubic getCubicById(int cubicId) {
      return this._cubics.get(cubicId);
   }

   public int getEnchantEffect() {
      Item wpn = this.getActiveWeaponInstance();
      return wpn == null ? 0 : Math.min(127, wpn.getEnchantLevel());
   }

   public void setLastFolkNPC(Npc folkNpc) {
      this._lastFolkNpc = folkNpc;
   }

   public Npc getLastFolkNPC() {
      return this._lastFolkNpc;
   }

   public boolean isFestivalParticipant() {
      return SevenSignsFestival.getInstance().isParticipant(this);
   }

   public void addAutoSoulShot(int itemId) {
      this._activeSoulShots.add(itemId);
   }

   public boolean removeAutoSoulShot(int itemId) {
      return this._activeSoulShots.remove(itemId);
   }

   public Set<Integer> getAutoSoulShot() {
      return this._activeSoulShots;
   }

   @Override
   public void rechargeShots(boolean physical, boolean magic) {
      if (this._activeSoulShots != null && !this._activeSoulShots.isEmpty()) {
         for (int itemId : this._activeSoulShots) {
            Item item = this._inventory.getItemByItemId(itemId);
            if (item != null) {
               if (magic && item.getTemplate().getDefaultAction() == ActionType.SPIRITSHOT) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                  if (handler != null) {
                     handler.onItemUse(this, item, false);
                  }
               }

               if (physical && item.getTemplate().getDefaultAction() == ActionType.SOULSHOT) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                  if (handler != null) {
                     handler.onItemUse(this, item, false);
                  }
               }
            } else {
               this.removeAutoSoulShot(itemId);
            }
         }
      }
   }

   public void disableAutoShotByCrystalType(int crystalType) {
      for (int itemId : this._activeSoulShots) {
         if (ItemData.getInstance().getTemplate(itemId).getCrystalType().getLevel() == crystalType) {
            this.disableAutoShot(itemId);
         }
      }
   }

   public boolean disableAutoShot(int itemId) {
      if (this._activeSoulShots.contains(itemId)) {
         this.removeAutoSoulShot(itemId);
         this.sendPacket(new ExAutoSoulShot(itemId, 0));
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
         sm.addItemName(itemId);
         this.sendPacket(sm);
         return true;
      } else {
         return false;
      }
   }

   public void disableAutoShotsAll() {
      for (int itemId : this._activeSoulShots) {
         this.sendPacket(new ExAutoSoulShot(itemId, 0));
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
         sm.addItemName(itemId);
         this.sendPacket(sm);
      }

      this._activeSoulShots.clear();
   }

   public ClanPrivileges getClanPrivileges() {
      return this._clanPrivileges;
   }

   public void setClanPrivileges(ClanPrivileges clanPrivileges) {
      this._clanPrivileges = clanPrivileges.clone();
   }

   public boolean hasAccess(ClanAccess access) {
      return this._clanPrivileges.hasMinimumPrivileges(access);
   }

   public void setPledgeClass(int id) {
      this._pledgeClass = id;
      this.checkItemRestriction();
   }

   public int getPledgeClass() {
      return this._pledgeClass;
   }

   public void setPledgeType(int typeId) {
      this._pledgeType = typeId;
   }

   @Override
   public int getPledgeType() {
      return this._pledgeType;
   }

   public int getApprentice() {
      return this._apprentice;
   }

   public void setApprentice(int apprenticeId) {
      this._apprentice = apprenticeId;
   }

   public int getSponsor() {
      return this._sponsor;
   }

   public void setSponsor(int sponsorId) {
      this._sponsor = sponsorId;
   }

   @Override
   public void sendMessage(String message) {
      this.sendPacket(new SystemMessage(SendMessageLocalisationData.getLocalisation(this, message)));
   }

   public void sendSysMessage(String message) {
      if (GeneralConfig.GM_STARTUP_BUILDER_HIDE) {
         this.sendPacket(new CreatureSay(null, ChatType.GENERAL, "SYS", SendMessageLocalisationData.getLocalisation(this, message)));
      } else {
         this.sendMessage(message);
      }
   }

   public boolean setHiding(boolean hide) {
      if (!this.isGM()) {
         return false;
      } else {
         if (this.hasEnteredWorld()) {
            if (this.isInvisible() && hide) {
               return false;
            }

            if (!this.isInvisible() && !hide) {
               return false;
            }
         }

         this.setSilenceMode(hide);
         this.setInvul(hide);
         this.setInvisible(hide);
         if (hide && this.hasEnteredWorld()) {
            World.forEachVisibleObject(this, Player.class, player -> {
               if (player.getTarget() == this) {
                  player.setTarget(null);
               }
            });
         }

         this.broadcastUserInfo();
         return true;
      }
   }

   public void enterObserverMode(Location loc) {
      this.setLastLocation();
      this.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
      this._observerMode = true;
      this.setTarget(null);
      this.setParalyzed(true);
      this.startParalyze();
      this.setInvul(true);
      this.setInvisible(true);
      this.sendPacket(new ObservationMode(loc));
      this.teleToLocation(loc, false);
      this.broadcastUserInfo();
   }

   public void setLastLocation() {
      this._lastLoc.setXYZ(this.getX(), this.getY(), this.getZ());
   }

   public void unsetLastLocation() {
      this._lastLoc.setXYZ(0, 0, 0);
   }

   public void enterOlympiadObserverMode(Location loc, int id, boolean storeCoords) {
      if (this.hasSummon()) {
         this._summon.unSummon(this);
      }

      this.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
      if (!this._cubics.isEmpty()) {
         for (Cubic cubic : this._cubics.values()) {
            cubic.stopAction();
            cubic.cancelDisappear();
         }

         this._cubics.clear();
         this._cubicOrder.clear();
      }

      if (this._party != null) {
         this._party.removePartyMember(this, PartyMessageType.EXPELLED);
      }

      this._olympiadGameId = id;
      if (this._waitTypeSitting) {
         this.standUp();
      }

      if (storeCoords) {
         this.setLastLocation();
      }

      this._observerMode = true;
      this.setTarget(null);
      this.setInvul(true);
      this.setInvisible(true);
      this.teleToLocation(loc, 0);
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

   public void setOlympiadSide(int i) {
      this._olympiadSide = i;
   }

   public int getOlympiadSide() {
      return this._olympiadSide;
   }

   public void setOlympiadGameId(int id) {
      this._olympiadGameId = id;
   }

   public int getOlympiadGameId() {
      return this._olympiadGameId;
   }

   public int getOlympiadBuffCount() {
      return this._olyBuffsCount;
   }

   public void setOlympiadBuffCount(int buffs) {
      this._olyBuffsCount = buffs;
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

   public void setTeleMode(int mode) {
      this._telemode = mode;
   }

   public void setLoto(int i, int value) {
      this._loto[i] = value;
   }

   public int getLoto(int i) {
      return this._loto[i];
   }

   public void setRaceTicket(int i, int value) {
      this._raceTickets[i] = value;
   }

   public int getRaceTicket(int i) {
      return this._raceTickets[i];
   }

   public boolean getMessageRefusal() {
      return this._messageRefusal;
   }

   public void setMessageRefusal(boolean mode) {
      this._messageRefusal = mode;
      this.sendPacket(new EtcStatusUpdate(this));
   }

   public void setDietMode(boolean mode) {
      this._dietMode = mode;
   }

   public boolean getDietMode() {
      return this._dietMode;
   }

   public void setTradeRefusal(boolean mode) {
      this._tradeRefusal = mode;
   }

   public boolean getTradeRefusal() {
      return this._tradeRefusal;
   }

   public void setExchangeRefusal(boolean mode) {
      this._exchangeRefusal = mode;
   }

   public boolean getExchangeRefusal() {
      return this._exchangeRefusal;
   }

   public BlockList getBlockList() {
      return this._blockList;
   }

   public void setHero(boolean hero) {
      if (hero && this._baseClass == this._activeClass) {
         for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values()) {
            this.addSkill(skill, false);
         }
      } else {
         for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values()) {
            this.removeSkill(skill, false, true);
         }
      }

      this._hero = hero;
      this.sendSkillList();
   }

   public void setInOlympiadMode(boolean value) {
      this._inOlympiadMode = value;
   }

   public void setOlympiadStart(boolean value) {
      this._olympiadStart = value;
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

   @Override
   public boolean isInDuel() {
      return this._isInDuel;
   }

   public void setStartingDuel() {
      this._startingDuel = true;
   }

   @Override
   public int getDuelId() {
      return this._duelId;
   }

   public void setDuelState(int mode) {
      this._duelState = mode;
   }

   public int getDuelState() {
      return this._duelState;
   }

   public void setInDuel(int duelId) {
      if (duelId > 0) {
         this._isInDuel = true;
         this._duelState = 1;
         this._duelId = duelId;
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
      SystemMessage sm = new SystemMessage(this._noDuelReason);
      sm.addPcName(this);
      this._noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
      return sm;
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

   public void setNoble(boolean value) {
      Collection<Skill> nobleSkillTree = SkillTreeData.getInstance().getNobleSkillTree().values();
      if (value) {
         for (Skill skill : nobleSkillTree) {
            this.addSkill(skill, false);
         }
      } else {
         for (Skill skill : nobleSkillTree) {
            this.removeSkill(skill, false, true);
         }
      }

      this._noble = value;
      this.sendSkillList();
   }

   public void setLvlJoinedAcademy(int lvl) {
      this._lvlJoinedAcademy = lvl;
   }

   public int getLvlJoinedAcademy() {
      return this._lvlJoinedAcademy;
   }

   @Override
   public boolean isAcademyMember() {
      return this._lvlJoinedAcademy > 0;
   }

   @Override
   public void setTeam(Team team) {
      super.setTeam(team);
      this.broadcastUserInfo();
      if (this.hasSummon()) {
         this._summon.broadcastStatusUpdate();
      }
   }

   public void setWantsPeace(int wantsPeace) {
      this._wantsPeace = wantsPeace;
   }

   public int getWantsPeace() {
      return this._wantsPeace;
   }

   public boolean isFishing() {
      return this._fishing;
   }

   public void setFishing(boolean fishing) {
      this._fishing = fishing;
   }

   public synchronized void sendSkillList() {
      if (this._skillListTask == null) {
         this._skillListTask = ThreadPool.schedule(
            () -> {
               boolean isDisabled = false;
               SkillList skillList = new SkillList();

               for (Skill skill : this.getAllSkills()) {
                  if (skill != null && (this._transformation == null || skill.isPassive()) && (!this.hasTransformSkill(skill.getId()) || !skill.isPassive())) {
                     if (this._clan != null) {
                        isDisabled = skill.isClanSkill() && this._clan.getReputationScore() < 0;
                     }

                     boolean isEnchantable = SkillData.getInstance().isEnchantable(skill.getId());
                     if (isEnchantable) {
                        EnchantSkillLearn esl = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skill.getId());
                        if (esl == null || skill.getLevel() < esl.getBaseLevel()) {
                           isEnchantable = false;
                        }
                     }

                     skillList.addSkill(skill.getDisplayId(), skill.getDisplayLevel(), skill.isPassive(), isDisabled, isEnchantable);
                  }
               }

               if (this._transformation != null) {
                  Map<Integer, Integer> ts = new TreeMap<>();

                  for (SkillHolder holder : this._transformation.getTemplate(this).getSkills()) {
                     ts.putIfAbsent(holder.getSkillId(), holder.getSkillLevel());
                     if (ts.get(holder.getSkillId()) < holder.getSkillLevel()) {
                        ts.put(holder.getSkillId(), holder.getSkillLevel());
                     }
                  }

                  for (AdditionalSkillHolder holderx : this._transformation.getTemplate(this).getAdditionalSkills()) {
                     if (this.getLevel() >= holderx.getMinLevel()) {
                        ts.putIfAbsent(holderx.getSkillId(), holderx.getSkillLevel());
                        if (ts.get(holderx.getSkillId()) < holderx.getSkillLevel()) {
                           ts.put(holderx.getSkillId(), holderx.getSkillLevel());
                        }
                     }
                  }

                  if (this._transformation.isFlying()) {
                     for (SkillLearn skillx : SkillTreeData.getInstance().getCollectSkillTree().values()) {
                        if (this.getKnownSkill(skillx.getSkillId()) != null
                           && (!ts.containsKey(skillx.getSkillId()) || ts.get(skillx.getSkillId()) < skillx.getSkillLevel())) {
                           ts.put(skillx.getSkillId(), skillx.getSkillLevel());
                        }
                     }
                  }

                  for (Entry<Integer, Integer> transformSkill : ts.entrySet()) {
                     Skill sk = SkillData.getInstance().getSkill(transformSkill.getKey(), transformSkill.getValue());
                     this.addTransformSkill(sk);
                     skillList.addSkill(transformSkill.getKey(), transformSkill.getValue(), false, false, false);
                  }
               }

               this.sendPacket(skillList);
               this._skillListTask = null;
            },
            300L
         );
      }
   }

   public boolean addSubClass(int classId, int classIndex) {
      if (this._subclassLock) {
         return false;
      } else {
         this._subclassLock = true;

         try {
            if (this.getTotalSubClasses() == PlayerConfig.MAX_SUBCLASS || classIndex == 0) {
               return false;
            } else if (this.getSubClasses().containsKey(classIndex)) {
               return false;
            } else {
               SubClassHolder newClass = new SubClassHolder();
               newClass.setPlayerClass(classId);
               newClass.setClassIndex(classIndex);

               try (
                  Connection con = DatabaseFactory.getConnection();
                  PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)"
                  );
               ) {
                  ps.setInt(1, this.getObjectId());
                  ps.setInt(2, newClass.getId());
                  ps.setLong(3, newClass.getExp());
                  ps.setLong(4, newClass.getSp());
                  ps.setInt(5, newClass.getLevel());
                  ps.setInt(6, newClass.getClassIndex());
                  ps.execute();
               } catch (Exception var22) {
                  LOGGER.log(Level.WARNING, "WARNING: Could not add character sub class for " + this.getName() + ": " + var22.getMessage(), (Throwable)var22);
                  return false;
               }

               this.getSubClasses().put(newClass.getClassIndex(), newClass);
               PlayerClass subTemplate = PlayerClass.getPlayerClass(classId);
               Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(subTemplate);
               Map<Integer, Skill> prevSkillList = new HashMap<>();

               for (SkillLearn skillInfo : skillTree.values()) {
                  if (skillInfo.getGetLevel() <= 40) {
                     Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
                     Skill newSkill = SkillData.getInstance().getSkill(skillInfo.getSkillId(), skillInfo.getSkillLevel());
                     if (prevSkill == null || prevSkill.getLevel() <= newSkill.getLevel()) {
                        prevSkillList.put(newSkill.getId(), newSkill);
                        this.storeSkill(newSkill, prevSkill, classIndex);
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

   public boolean modifySubClass(int classIndex, int newClassId) {
      SubClassHolder subClass = this.getSubClasses().get(classIndex);
      if (subClass == null) {
         return false;
      } else {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(this, subClass.getId()), this);
         }

         this.getSubClasses().remove(classIndex);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement deleteHennas = con.prepareStatement("DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?");
            PreparedStatement deleteShortcuts = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND class_index=?");
            PreparedStatement deleteSkillReuse = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=? AND class_index=?");
            PreparedStatement deleteSkills = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND class_index=?");
            PreparedStatement deleteSubclass = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=? AND class_index=?");
         ) {
            for (int slot = 1; slot < 4; slot++) {
               Henna henna = this.getHenna(slot);
               if (henna != null) {
                  deleteHennas.setInt(1, this.getObjectId());
                  deleteHennas.setInt(2, slot);
                  deleteHennas.setInt(3, classIndex);
                  deleteHennas.execute();
               }
            }

            deleteShortcuts.setInt(1, this.getObjectId());
            deleteShortcuts.setInt(2, classIndex);
            deleteShortcuts.execute();
            deleteSkillReuse.setInt(1, this.getObjectId());
            deleteSkillReuse.setInt(2, classIndex);
            deleteSkillReuse.execute();
            deleteSkills.setInt(1, this.getObjectId());
            deleteSkills.setInt(2, classIndex);
            deleteSkills.execute();
            deleteSubclass.setInt(1, this.getObjectId());
            deleteSubclass.setInt(2, classIndex);
            deleteSubclass.execute();
         } catch (Exception var24) {
            LOGGER.log(
               Level.WARNING,
               "Could not modify sub class for " + this.getName() + " to class index " + classIndex + ": " + var24.getMessage(),
               (Throwable)var24
            );
            return false;
         }

         return this.addSubClass(newClassId, classIndex);
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

   private void setClassTemplate(int classId) {
      this._activeClass = classId;
      PlayerTemplate pcTemplate = PlayerTemplateData.getInstance().getTemplate(classId);
      if (pcTemplate == null) {
         LOGGER.severe("Missing template for classId: " + classId);
         throw new Error();
      } else {
         this.setTemplate(pcTemplate);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CHANGE, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionChange(this, pcTemplate, this.isSubClassActive()), this);
         }
      }
   }

   public void setActiveClass(int classIndex) {
      if (!this._subclassLock) {
         this._subclassLock = true;

         try {
            if (this._transformation == null) {
               for (Item item : this._inventory.getAugmentedItems()) {
                  if (item != null && item.isEquipped()) {
                     item.getAugmentation().removeBonus(this);
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

               if (classIndex == 0) {
                  this.setClassTemplate(this._baseClass);
               } else {
                  try {
                     this.setClassTemplate(this.getSubClasses().get(classIndex).getId());
                  } catch (Exception var7) {
                     LOGGER.log(
                        Level.WARNING,
                        "Could not switch " + this.getName() + "'s sub class to class index " + classIndex + ": " + var7.getMessage(),
                        (Throwable)var7
                     );
                     return;
                  }
               }

               this._classIndex = classIndex;
               this.setLearningClass(this.getPlayerClass());
               if (this.isInParty()) {
                  this._party.recalculatePartyLevel();
               }

               this._autoUseSettings.getAutoSkills().clear();
               this._autoUseSettings.getAutoBuffs().clear();

               for (Skill oldSkill : this.getAllSkills()) {
                  this.removeSkill(oldSkill, false, true);
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
               QuestState st = this.getQuestState("Q00422_RepentYourSins");
               if (st != null) {
                  st.exitQuest(true);
               }

               for (int i = 0; i < 3; i++) {
                  this._henna[i] = null;
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

   public void startRentPet(int seconds) {
      if (this._taskRentPet == null) {
         this._taskRentPet = ThreadPool.scheduleAtFixedRate(new RentPetTask(this), seconds * 1000, seconds * 1000);
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
         int baseTimeInWater = 60000;
         double breathPercentage = this.calcStat(Stat.BREATH, this.getBaseTemplate().getBaseBreath()) / 100.0;
         int timeInWater = (int)(60000.0 * breathPercentage);
         this.sendPacket(new SetupGauge(this.getObjectId(), 2, timeInWater));
         this._taskWater = ThreadPool.scheduleAtFixedRate(new WaterTask(this), timeInWater, 1000L);
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
         for (ZoneType zone : ZoneManager.getInstance().getZones(this)) {
            zone.onPlayerLoginInside(this);
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

   @Override
   public void fullRestore() {
      this.setCurrentHp(this.getMaxHp());
      this.setCurrentMp(this.getMaxMp());
      this.setCurrentCp(this.getMaxCp());
   }

   public long getLastAccess() {
      return this._lastAccess;
   }

   @Override
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
         Instance instance = InstanceManager.getInstance().getInstance(this.getInstanceId());
         if (instance != null) {
            instance.cancelEjectDeadPlayer(this);
         }
      }
   }

   @Override
   public void setName(String value) {
      super.setName(value);
      CharInfoTable.getInstance().addName(this);
   }

   @Override
   public void doRevive(double revivePower) {
      this.doRevive();
      this.restoreExp(revivePower);
   }

   public void reviveRequest(Player reviver, boolean pet, int power) {
      if (!this.isResurrectionBlocked()) {
         if (this._reviveRequested == 1) {
            if (this._revivePet == pet) {
               reviver.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED);
            } else if (pet) {
               reviver.sendPacket(SystemMessageId.A_PET_CANNOT_BE_RESURRECTED_WHILE_IT_S_OWNER_IS_IN_THE_PROCESS_OF_RESURRECTING);
            } else {
               reviver.sendPacket(SystemMessageId.WHILE_A_PET_IS_ATTEMPTING_TO_RESURRECT_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER);
            }
         } else {
            if (pet && this.hasPet() && this._summon.isDead() || !pet && this.isDead()) {
               this._reviveRequested = 1;
               int restoreExp = 0;
               this._revivePower = Formulas.calculateSkillResurrectRestorePercent(power, reviver);
               restoreExp = (int)Math.round((this._expBeforeDeath - this.getExp()) * this._revivePower / 100.0);
               this._revivePet = pet;
               if (this._hasCharmOfCourage) {
                  ConfirmDlg dlg = new ConfirmDlg(
                     SystemMessageId.RESURRECTION_IS_POSSIBLE_BECAUSE_OF_THE_COURAGE_CHARM_S_EFFECT_WOULD_YOU_LIKE_TO_RESURRECT_NOW.getId()
                  );
                  dlg.addTime(60000);
                  this.sendPacket(dlg);
                  return;
               }

               ConfirmDlg dlg = new ConfirmDlg(
                  SystemMessageId.C1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_WITH_S2_EXPERIENCE_POINTS_DO_YOU_WANT_TO_BE_RESURRECTED.getId()
               );
               dlg.getSystemMessage().addPcName(reviver);
               dlg.getSystemMessage().addString(Integer.toString(restoreExp));
               this.sendPacket(dlg);
            }
         }
      }
   }

   public void reviveAnswer(int answer) {
      if (this._reviveRequested == 1 && (this.isDead() || this._revivePet) && (!this._revivePet || !this.hasPet() || this._summon.isDead())) {
         if (answer == 1) {
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

   @Override
   public void teleToLocation(ILocational loc, boolean allowRandomOffset) {
      if (this._vehicle != null && !this._vehicle.isTeleporting()) {
         this.setVehicle(null);
      }

      if (this.isFlyingMounted() && loc.getZ() < -1005) {
         super.teleToLocation(loc.getX(), loc.getY(), -1005, loc.getHeading(), loc.getInstanceId());
      }

      super.teleToLocation(loc, allowRandomOffset);
   }

   @Override
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
         for (TamedBeast tamedBeast : this._tamedBeast) {
            tamedBeast.deleteMe();
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

   @Override
   public void setTeleporting(boolean teleport) {
      this.setTeleporting(teleport, true);
   }

   public void setTeleporting(boolean teleport, boolean useWatchDog) {
      super.setTeleporting(teleport);
      if (useWatchDog) {
         if (teleport) {
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

   public void setLastServerPosition(int x, int y, int z) {
      this._lastServerPosition.setXYZ(x, y, z);
   }

   public Location getLastServerPosition() {
      return this._lastServerPosition;
   }

   public void setBlinkActive(boolean value) {
      this._blinkActive.set(value);
   }

   public boolean isBlinkActive() {
      return this._blinkActive.get();
   }

   @Override
   public synchronized void addExpAndSp(double addToExp, double addToSp) {
      this.getStat().addExpAndSp(addToExp, addToSp, false);
   }

   public synchronized void addExpAndSp(double addToExp, double addToSp, boolean useVitality) {
      this.getStat().addExpAndSp(addToExp, addToSp, useVitality);
   }

   public void removeExpAndSp(long removeExp, long removeSp) {
      this.getStat().removeExpAndSp(removeExp, removeSp, true);
   }

   public void removeExpAndSp(long removeExp, long removeSp, boolean sendMessage) {
      this.getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
   }

   @Override
   public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (skill != null) {
         this.getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
      } else {
         this.getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
      }

      if (this._tamedBeast != null) {
         for (TamedBeast tamedBeast : this._tamedBeast) {
            tamedBeast.onOwnerGotAttacked(attacker);
         }
      }
   }

   public void broadcastSnoop(ChatType type, String name, String text, CreatureSay cs) {
      if (!this._snoopListener.isEmpty()) {
         Snoop sn = new Snoop(this.getObjectId(), this.getName(), type, name, text);

         for (Player pci : this._snoopListener) {
            if (pci != null) {
               pci.sendPacket(cs);
               pci.sendPacket(sn);
            }
         }
      }
   }

   public void addSnooper(Player pci) {
      this._snoopListener.add(pci);
   }

   public void removeSnooper(Player pci) {
      this._snoopListener.remove(pci);
   }

   public void addSnooped(Player pci) {
      this._snoopedPlayer.add(pci);
   }

   public void removeSnooped(Player pci) {
      this._snoopedPlayer.remove(pci);
   }

   public void addHtmlAction(HtmlActionScope scope, String action) {
      this._htmlActionCaches[scope.ordinal()].add(action);
   }

   public void clearHtmlActions(HtmlActionScope scope) {
      this._htmlActionCaches[scope.ordinal()].clear();
   }

   public void setHtmlActionOriginObjectId(HtmlActionScope scope, int npcObjId) {
      if (npcObjId < 0) {
         throw new IllegalArgumentException();
      } else {
         this._htmlActionOriginObjectIds[scope.ordinal()] = npcObjId;
      }
   }

   public int getLastHtmlActionOriginId() {
      return this._lastHtmlActionOriginObjId;
   }

   private boolean validateHtmlAction(Iterable<String> actionIter, String action) {
      for (String cachedAction : actionIter) {
         if (cachedAction.charAt(cachedAction.length() - 1) == '$') {
            if (action.startsWith(cachedAction.substring(0, cachedAction.length() - 1).trim())) {
               return true;
            }
         } else if (cachedAction.equals(action)) {
            return true;
         }
      }

      return false;
   }

   public int validateHtmlAction(String action) {
      for (int i = 0; i < this._htmlActionCaches.length; i++) {
         if (this.validateHtmlAction(this._htmlActionCaches[i], action)) {
            this._lastHtmlActionOriginObjId = this._htmlActionOriginObjectIds[i];
            return this._lastHtmlActionOriginObjId;
         }
      }

      return -1;
   }

   public boolean validateItemManipulation(int objectId, ItemProcessType itemProcessType) {
      Item item = this._inventory.getItemByObjectId(objectId);
      if (item != null && item.getOwnerId() == this.getObjectId()) {
         if ((!this.hasSummon() || this._summon.getControlObjectId() != objectId) && this._mountObjectID != objectId) {
            return this._activeEnchantItemId == objectId ? false : !CursedWeaponsManager.getInstance().isCursed(item.getId());
         } else {
            return false;
         }
      } else {
         LOGGER.finest(this.getObjectId() + ": player tried to " + itemProcessType + " item he is not owner of.");
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

   public void setVehicle(Vehicle v) {
      if (v == null && this._vehicle != null) {
         this._vehicle.removePassenger(this);
      }

      this._vehicle = v;
   }

   public boolean isInVehicle() {
      return this._vehicle != null;
   }

   public void setInCrystallize(boolean inCrystallize) {
      this._inCrystallize = inCrystallize;
   }

   public boolean isInCrystallize() {
      return this._inCrystallize;
   }

   public Location getInVehiclePosition() {
      return this._inVehiclePosition;
   }

   public void setInVehiclePosition(Location pt) {
      this._inVehiclePosition = pt;
   }

   @Override
   public boolean deleteMe() {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGOUT, this)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogout(this), this);
      }

      try {
         for (ZoneType zone : ZoneManager.getInstance().getZones(this)) {
            zone.onPlayerLogoutInside(this);
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
            Fort fort = FortManager.getInstance().getFort(this);
            if (fort != null) {
               FortSiegeManager.getInstance().dropCombatFlag(this, fort.getResidenceId());
            } else {
               BodyPart bodyPart = BodyPart.fromItem(this._inventory.getItemByItemId(9819));
               this._inventory.unEquipItemInBodySlot(bodyPart);
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
            PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(this._partyroom);
            if (room != null) {
               room.deleteMember(this);
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
      ZoneRegion region = ZoneManager.getInstance().getRegion(this);
      if (region != null) {
         region.removeFromZones(this);
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
            ClanMember clanMember = this._clan.getClanMember(this.getObjectId());
            if (clanMember != null) {
               clanMember.setPlayer(null);
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
         int instanceId = this.getInstanceId();
         if (instanceId != 0 && !GeneralConfig.RESTORE_PLAYER_INSTANCE) {
            Instance inst = InstanceManager.getInstance().getInstance(instanceId);
            if (inst != null) {
               inst.removePlayer(this.getObjectId());
               Location loc = inst.getExitLoc();
               if (loc != null) {
                  int x = loc.getX() + Rnd.get(-30, 30);
                  int y = loc.getY() + Rnd.get(-30, 30);
                  this.setXYZInvisible(x, y, loc.getZ());
                  if (this.hasSummon()) {
                     this._summon.teleToLocation(loc, true);
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

      for (Player player : this._snoopedPlayer) {
         player.removeSnooper(this);
      }

      for (Player player : this._snoopListener) {
         player.removeSnooped(this);
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

   public void startFishing(int x, int y, int z) {
      this.stopMove(null);
      this.setImmobilized(true);
      this._fishing = true;
      this._fishX = x;
      this._fishY = y;
      this._fishZ = z;
      int lvl = this.getRandomFishLvl();
      int grade = this.getRandomFishGrade();
      int group = this.getRandomFishGroup(grade);
      List<Fish> fish = FishData.getInstance().getFish(lvl, group, grade);
      if (fish != null && !fish.isEmpty()) {
         this._fish = fish.get(Rnd.get(fish.size())).clone();
         fish.clear();
         this.sendPacket(SystemMessageId.YOU_CAST_YOUR_LINE_AND_START_TO_FISH);
         if (!GameTimeTaskManager.getInstance().isNight() && this._lure.isNightLure()) {
            this._fish.setFishGroup(-1);
         }

         this.broadcastPacket(new ExFishingStart(this, this._fish.getFishGroup(), x, y, z, this._lure.isNightLure()));
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
         int checkDelay = 0;
         boolean isNoob = false;
         boolean isUpperGrade = false;
         if (this._lure != null) {
            int lureid = this._lure.getId();
            isNoob = this._fish.getFishGrade() == 0;
            isUpperGrade = this._fish.getFishGrade() == 2;
            if (lureid != 6519 && lureid != 6522 && lureid != 6525 && lureid != 8505 && lureid != 8508 && lureid != 8511) {
               if (lureid != 6520
                  && lureid != 6523
                  && lureid != 6526
                  && (lureid < 8505 || lureid > 8513)
                  && (lureid < 7610 || lureid > 7613)
                  && (lureid < 7807 || lureid > 7809)
                  && (lureid < 8484 || lureid > 8486)) {
                  if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) {
                     checkDelay = this._fish.getGutsCheckTime() * 66;
                  }
               } else {
                  checkDelay = this._fish.getGutsCheckTime() * 100;
               }
            } else {
               checkDelay = this._fish.getGutsCheckTime() * 133;
            }
         }

         this._taskForFish = ThreadPool.scheduleAtFixedRate(
            new LookingForFishTask(this, this._fish.getStartCombatTime(), this._fish.getFishGuts(), this._fish.getFishGroup(), isNoob, isUpperGrade),
            10000L,
            checkDelay
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

   private int getRandomFishGroup(int group) {
      int check = Rnd.get(100);
      int type = 1;
      switch (group) {
         case 0:
            switch (this._lure.getId()) {
               case 7807:
                  if (check <= 54) {
                     return 5;
                  } else {
                     byte typex;
                     if (check <= 77) {
                        typex = 4;
                     } else {
                        typex = 6;
                     }

                     return typex;
                  }
               case 7808:
                  if (check <= 54) {
                     return 4;
                  } else {
                     byte typex;
                     if (check <= 77) {
                        typex = 6;
                     } else {
                        typex = 5;
                     }

                     return typex;
                  }
               case 7809:
                  if (check <= 54) {
                     return 6;
                  } else {
                     byte typex;
                     if (check <= 77) {
                        typex = 5;
                     } else {
                        typex = 4;
                     }

                     return typex;
                  }
               case 8486:
                  if (check <= 33) {
                     return 4;
                  } else {
                     byte typex;
                     if (check <= 66) {
                        typex = 5;
                     } else {
                        typex = 6;
                     }

                     return typex;
                  }
               default:
                  return type;
            }
         case 1:
            switch (this._lure.getId()) {
               case 6519:
               case 6520:
               case 6521:
               case 8505:
               case 8507:
                  if (check <= 54) {
                     return 1;
                  } else if (check <= 74) {
                     return 0;
                  } else {
                     byte typex;
                     if (check <= 94) {
                        typex = 2;
                     } else {
                        typex = 3;
                     }

                     return typex;
                  }
               case 6522:
               case 6523:
               case 6524:
               case 8508:
               case 8510:
                  if (check <= 54) {
                     return 0;
                  } else if (check <= 74) {
                     return 1;
                  } else {
                     byte typex;
                     if (check <= 94) {
                        typex = 2;
                     } else {
                        typex = 3;
                     }

                     return typex;
                  }
               case 6525:
               case 6526:
               case 6527:
               case 8511:
               case 8513:
                  if (check <= 55) {
                     return 2;
                  } else if (check <= 74) {
                     return 1;
                  } else {
                     byte typex;
                     if (check <= 94) {
                        typex = 0;
                     } else {
                        typex = 3;
                     }

                     return typex;
                  }
               case 7610:
               case 7611:
               case 7612:
               case 7613:
                  return 3;
               case 8484:
                  if (check <= 33) {
                     return 0;
                  } else {
                     byte typex;
                     if (check <= 66) {
                        typex = 1;
                     } else {
                        typex = 2;
                     }

                     return typex;
                  }
               default:
                  return type;
            }
         case 2:
            switch (this._lure.getId()) {
               case 8485:
                  if (check <= 33) {
                     type = 7;
                  } else if (check <= 66) {
                     type = 8;
                  } else {
                     type = 9;
                  }
                  break;
               case 8506:
                  if (check <= 54) {
                     type = 8;
                  } else if (check <= 77) {
                     type = 7;
                  } else {
                     type = 9;
                  }
                  break;
               case 8509:
                  if (check <= 54) {
                     type = 7;
                  } else if (check <= 77) {
                     type = 9;
                  } else {
                     type = 8;
                  }
                  break;
               case 8512:
                  if (check <= 54) {
                     type = 9;
                  } else if (check <= 77) {
                     type = 8;
                  } else {
                     type = 7;
                  }
            }
      }

      return type;
   }

   private int getRandomFishLvl() {
      int skillLevel = this.getSkillLevel(1315);
      BuffInfo info = this.getEffectList().getBuffInfoBySkillId(2274);
      if (info != null) {
         skillLevel = (int)info.getSkill().getPower();
      }

      if (skillLevel <= 0) {
         return 1;
      } else {
         int check = Rnd.get(100);
         int randomlvl;
         if (check <= 50) {
            randomlvl = skillLevel;
         } else if (check <= 85) {
            randomlvl = skillLevel - 1;
            if (randomlvl <= 0) {
               randomlvl = 1;
            }
         } else {
            randomlvl = skillLevel + 1;
            if (randomlvl > 27) {
               randomlvl = 27;
            }
         }

         return randomlvl;
      }
   }

   public void startFishCombat(boolean isNoob, boolean isUpperGrade) {
      this._fishCombat = new Fishing(this, this._fish, isNoob, isUpperGrade, this._lure.getId());
   }

   public void endFishing(boolean win) {
      this._fishing = false;
      this._fishX = 0;
      this._fishY = 0;
      this._fishZ = 0;
      if (this._fishCombat == null) {
         this.sendPacket(SystemMessageId.BAITS_HAVE_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
      }

      this._fishCombat = null;
      this._lure = null;
      this.broadcastPacket(new ExFishingEnd(win, this));
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

   public void setLure(Item lure) {
      this._lure = lure;
   }

   public Item getLure() {
      return this._lure;
   }

   public int getInventoryLimit() {
      int ivlim;
      if (this.isGM()) {
         ivlim = PlayerConfig.INVENTORY_MAXIMUM_GM;
      } else {
         ivlim = this.getRace() == Race.DWARF ? PlayerConfig.INVENTORY_MAXIMUM_DWARF : PlayerConfig.INVENTORY_MAXIMUM_NO_DWARF;
      }

      return ivlim + (int)this.getStat().calcStat(Stat.INV_LIM, 0.0, null, null);
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

   public void setMountObjectID(int newID) {
      this._mountObjectID = newID;
   }

   public int getMountObjectID() {
      return this._mountObjectID;
   }

   public SkillUseHolder getCurrentSkill() {
      return this._currentSkill;
   }

   public void setCurrentSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed) {
      if (currentSkill == null) {
         this._currentSkill = null;
      } else {
         this._currentSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
      }
   }

   public SkillUseHolder getCurrentPetSkill() {
      return this._currentPetSkill;
   }

   public void setCurrentPetSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed) {
      if (currentSkill == null) {
         this._currentPetSkill = null;
      } else {
         this._currentPetSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
      }
   }

   public SkillUseHolder getQueuedSkill() {
      return this._queuedSkill;
   }

   public void setQueuedSkill(Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed) {
      if (queuedSkill == null) {
         this._queuedSkill = null;
      } else {
         this._queuedSkill = new SkillUseHolder(queuedSkill, ctrlPressed, shiftPressed);
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

   public void startFameTask(long delay, int fameFixRate) {
      if (PlayerConfig.FAME_SYSTEM_ENABLED) {
         if (this.getLevel() >= 40 && this.getPlayerClass().level() >= 2) {
            if (this._fameTask == null) {
               this._fameTask = ThreadPool.scheduleAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
            }
         }
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

   public void setPowerGrade(int power) {
      this._powerGrade = power;
   }

   public boolean isCursedWeaponEquipped() {
      return this._cursedWeaponEquippedId != 0;
   }

   public void setCursedWeaponEquippedId(int value) {
      this._cursedWeaponEquippedId = value;
   }

   public int getCursedWeaponEquippedId() {
      return this._cursedWeaponEquippedId;
   }

   public boolean isCombatFlagEquipped() {
      return this._combatFlagEquippedId;
   }

   public void setCombatFlagEquipped(boolean value) {
      this._combatFlagEquippedId = value;
   }

   public int getChargedSouls() {
      return this._souls;
   }

   public void increaseSouls(int count) {
      this._souls += count;
      SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_COUNT_HAS_INCREASED_BY_S1_IT_IS_NOW_AT_S2);
      sm.addInt(count);
      sm.addInt(this._souls);
      this.sendPacket(sm);
      this.restartSoulTask();
      this.sendPacket(new EtcStatusUpdate(this));
   }

   public boolean decreaseSouls(int count) {
      this._souls -= count;
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

   public void setDeathPenaltyBuffLevel(int level) {
      this._deathPenaltyBuffLevel = level;
   }

   public void calculateDeathPenaltyBuffLevel(Creature killer) {
      if (killer == null) {
         LOGGER.warning(this + " called calculateDeathPenaltyBuffLevel with killer null!");
      } else if (!this.isResurrectSpecialAffected()
         && !this.isLucky()
         && !this.isOnEvent()
         && !this.isInsideZone(ZoneId.PVP)
         && !this.isInsideZone(ZoneId.SIEGE)
         && !this.isGM()) {
         double percent = 1.0;
         if (killer.isRaid()) {
            percent *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_RAID, 1.0);
         } else if (killer.isMonster()) {
            percent *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_MOB, 1.0);
         } else if (killer.isPlayable()) {
            percent *= this.calcStat(Stat.REDUCE_DEATH_PENALTY_BY_PVP, 1.0);
         }

         if (Rnd.get(1, 100) <= PlayerConfig.DEATH_PENALTY_CHANCE * percent && (!killer.isPlayable() || this.getKarma() > 0)) {
            this.increaseDeathPenaltyBuffLevel();
         }
      }
   }

   public void increaseDeathPenaltyBuffLevel() {
      if (this._deathPenaltyBuffLevel < 15) {
         if (this._deathPenaltyBuffLevel != 0) {
            Skill skill = SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel());
            if (skill != null) {
               this.removeSkill(skill, true);
            }
         }

         this._deathPenaltyBuffLevel++;
         this.addSkill(SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel()), false);
         this.sendPacket(new EtcStatusUpdate(this));
         SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
         sm.addInt(this._deathPenaltyBuffLevel);
         this.sendPacket(sm);
      }
   }

   public void reduceDeathPenaltyBuffLevel() {
      if (this._deathPenaltyBuffLevel > 0) {
         Skill skill = SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel());
         if (skill != null) {
            this.removeSkill(skill, true);
         }

         this._deathPenaltyBuffLevel--;
         if (this._deathPenaltyBuffLevel > 0) {
            this.addSkill(SkillData.getInstance().getSkill(5076, this.getDeathPenaltyBuffLevel()), false);
            this.sendPacket(new EtcStatusUpdate(this));
            SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
            sm.addInt(this._deathPenaltyBuffLevel);
            this.sendPacket(sm);
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

   @Override
   public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
      if (miss) {
         if (target.isPlayer()) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
            sm.addPcName(target.asPlayer());
            sm.addString(this.getName());
            target.sendPacket(sm);
         }

         SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_ATTACK_WENT_ASTRAY);
         sm.addPcName(this);
         this.sendPacket(sm);
      } else {
         if (pcrit) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAD_A_CRITICAL_HIT);
            sm.addPcName(this);
            this.sendPacket(sm);
         }

         if (mcrit) {
            this.sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
         }

         if (this.isInOlympiadMode()
            && target.isPlayer()
            && target.asPlayer().isInOlympiadMode()
            && target.asPlayer().getOlympiadGameId() == this.getOlympiadGameId()) {
            Olympiad.getInstance().notifyCompetitorDamage(this, damage, this.getOlympiadGameId());
         }

         if (target.isInvul() && !target.isNpc()) {
            this.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
         } else if (target.isDoor() || target instanceof ControlTower) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HIT_FOR_S1_DAMAGE);
            sm.addInt(damage);
            this.sendPacket(sm);
         } else if (this != target) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_GIVEN_C2_DAMAGE_OF_S3);
            sm.addPcName(this);
            String targetName = target.getName();
            if (MultilingualSupportConfig.MULTILANG_ENABLE && target.isNpc()) {
               String[] localisation = NpcNameLocalisationData.getInstance().getLocalisation(this._lang, target.getId());
               if (localisation != null) {
                  targetName = localisation[0];
               }
            }

            sm.addString(targetName);
            sm.addInt(damage);
            this.sendPacket(sm);
         }
      }
   }

   public void setAgathionId(int npcId) {
      this._agathionId = npcId;
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

   public void setVitalityPoints(int points, boolean quiet) {
      this.getStat().setVitalityPoints(points, quiet);
   }

   public void updateVitalityPoints(float points, boolean useRates, boolean quiet) {
      this.getStat().updateVitalityPoints(points, useRates, quiet);
   }

   public void checkItemRestriction() {
      for (int i = 0; i < 25; i++) {
         Item equippedItem = this._inventory.getPaperdollItem(i);
         if (equippedItem != null && !equippedItem.getTemplate().checkCondition(this, this, false)) {
            this._inventory.unEquipItemInSlot(i);
            InventoryUpdate iu = new InventoryUpdate();
            iu.addModifiedItem(equippedItem);
            this.sendInventoryUpdate(iu);
            SystemMessage sm = null;
            if (equippedItem.getTemplate().getBodyPart() == BodyPart.BACK) {
               this.sendPacket(SystemMessageId.THE_CLOAK_EQUIP_HAS_BEEN_REMOVED_BECAUSE_THE_ARMOR_SET_EQUIP_HAS_BEEN_REMOVED);
               return;
            }

            if (equippedItem.isEnchanted()) {
               sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
               sm.addInt(equippedItem.getEnchantLevel());
            } else {
               sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
            }

            sm.addItemName(equippedItem);
            this.sendPacket(sm);
         }
      }
   }

   public void addTransformSkill(Skill sk) {
      this._transformSkills.put(sk.getId(), sk);
      if (sk.isPassive()) {
         this.addSkill(sk, false);
      }
   }

   public Skill getTransformSkill(int id) {
      return this._transformSkills.get(id);
   }

   public boolean hasTransformSkill(int id) {
      return this._transformSkills.containsKey(id);
   }

   public void removeAllTransformSkills() {
      this._transformSkills.clear();
   }

   protected void startFeed(int npcId) {
      this._canFeed = npcId > 0;
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

   private PetLevelData getPetLevelData(int npcId) {
      if (this._leveldata == null) {
         this._leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(this.getMountLevel());
      }

      return this._leveldata;
   }

   public int getCurrentFeed() {
      return this._curFeed;
   }

   public int getFeedConsume() {
      return this.isAttackingNow() ? this.getPetLevelData(this._mountNpcId).getPetFeedBattle() : this.getPetLevelData(this._mountNpcId).getPetFeedNormal();
   }

   public void setCurrentFeed(int num) {
      boolean lastHungryState = this.isHungry();
      this._curFeed = num > this.getMaxFeed() ? this.getMaxFeed() : num;
      this.sendPacket(new SetupGauge(this.getObjectId(), 3, this._curFeed * 10000 / this.getFeedConsume(), this.getMaxFeed() * 10000 / this.getFeedConsume()));
      if (lastHungryState != this.isHungry()) {
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

   public void enteredNoLanding(int delay) {
      this._dismountTask = ThreadPool.schedule(new DismountTask(this), delay * 1000);
   }

   public void exitedNoLanding() {
      if (this._dismountTask != null) {
         this._dismountTask.cancel(true);
         this._dismountTask = null;
      }
   }

   public void storePetFood(int petId) {
      if (this._controlItemId != 0 && petId != 0) {
         String req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
         ) {
            ps.setInt(1, this._curFeed);
            ps.setInt(2, this._controlItemId);
            ps.executeUpdate();
            this._controlItemId = 0;
         } catch (Exception var11) {
            LOGGER.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", (Throwable)var11);
         }
      }
   }

   public void setInSiege(boolean value) {
      this._isInSiege = value;
   }

   public boolean isInSiege() {
      return this._isInSiege;
   }

   public void setInHideoutSiege(boolean isInHideoutSiege) {
      this._isInHideoutSiege = isInHideoutSiege;
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

   public void increaseCharges(int count, int max) {
      if (this._charges.get() >= max) {
         this.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
      } else {
         this.restartChargeTask();
         if (this._charges.addAndGet(count) >= max) {
            this._charges.set(max);
            this.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FORCE_HAS_INCREASED_TO_S1_LEVEL);
            sm.addInt(this._charges.get());
            this.sendPacket(sm);
         }

         this.sendPacket(new EtcStatusUpdate(this));
      }
   }

   public boolean decreaseCharges(int count) {
      if (this._charges.get() < count) {
         return false;
      } else {
         if (this._charges.addAndGet(-count) == 0) {
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

   public void teleportBookmarkModify(int id, int icon, String tag, String name) {
      TeleportBookmark bookmark = this._tpbookmarks.get(id);
      if (bookmark != null) {
         bookmark.setIcon(icon);
         bookmark.setTag(tag);
         bookmark.setName(name);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?");
         ) {
            ps.setInt(1, icon);
            ps.setString(2, tag);
            ps.setString(3, name);
            ps.setInt(4, this.getObjectId());
            ps.setInt(5, id);
            ps.execute();
         } catch (Exception var14) {
            LOGGER.log(Level.WARNING, "Could not update character teleport bookmark data: " + var14.getMessage(), (Throwable)var14);
         }
      }

      this.sendPacket(new ExGetBookMarkInfoPacket(this));
   }

   public void teleportBookmarkDelete(int id) {
      if (this._tpbookmarks.remove(id) != null) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM character_tpbookmark WHERE charId=? AND Id=?");
         ) {
            ps.setInt(1, this.getObjectId());
            ps.setInt(2, id);
            ps.execute();
         } catch (Exception var10) {
            LOGGER.log(Level.WARNING, "Could not delete character teleport bookmark data: " + var10.getMessage(), (Throwable)var10);
         }

         this.sendPacket(new ExGetBookMarkInfoPacket(this));
      }
   }

   public void teleportBookmarkGo(int id) {
      if (this.teleportBookmarkCondition(0)) {
         if (this._inventory.getInventoryItemCount(13016, 0) == 0L) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
            sm.addItemName(13016);
            this.sendPacket(sm);
            TeleportBookmark bookmark = this._tpbookmarks.get(id);
            if (bookmark != null) {
               this.destroyItem(ItemProcessType.NONE, this._inventory.getItemByItemId(13016).getObjectId(), 1L, null, false);
               this.teleToLocation(bookmark, false);
            }

            this.sendPacket(new ExGetBookMarkInfoPacket(this));
         }
      }
   }

   public boolean teleportBookmarkCondition(int type) {
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
      } else if (type != 1 || !this._isIn7sDungeon && (!this.isInParty() || !this._party.isInDimensionalRift())) {
         if (this.isInWater()) {
            this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
            return false;
         } else if (type != 1
            || !this.isInsideZone(ZoneId.SIEGE)
               && !this.isInsideZone(ZoneId.CLAN_HALL)
               && !this.isInsideZone(ZoneId.JAIL)
               && !this.isInsideZone(ZoneId.CASTLE)
               && !this.isInsideZone(ZoneId.NO_SUMMON_FRIEND)
               && !this.isInsideZone(ZoneId.FORT)) {
            if (!this.isInsideZone(ZoneId.NO_BOOKMARK) && !this.isInBoat() && !this.isInAirShip()) {
               return true;
            } else {
               if (type == 0) {
                  this.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
               } else if (type == 1) {
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

   public void teleportBookmarkAdd(int x, int y, int z, int icon, String tag, String name) {
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

            int id = 1;

            while (id <= this._bookmarkSlot && this._tpbookmarks.containsKey(id)) {
               id++;
            }

            this._tpbookmarks.put(id, new TeleportBookmark(id, x, y, z, icon, tag, name));

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement("INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)");
            ) {
               ps.setInt(1, this.getObjectId());
               ps.setInt(2, id);
               ps.setInt(3, x);
               ps.setInt(4, y);
               ps.setInt(5, z);
               ps.setInt(6, icon);
               ps.setString(7, tag);
               ps.setString(8, name);
               ps.execute();
            } catch (Exception var16) {
               LOGGER.log(Level.WARNING, "Could not insert character teleport bookmark data: " + var16.getMessage(), (Throwable)var16);
            }

            this.sendPacket(new ExGetBookMarkInfoPacket(this));
         }
      }
   }

   public void restoreTeleportBookmark() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._tpbookmarks
                  .put(
                     rs.getInt("Id"),
                     new TeleportBookmark(
                        rs.getInt("Id"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("icon"), rs.getString("tag"), rs.getString("name")
                     )
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Failed restoing character teleport bookmark.", (Throwable)var12);
      }
   }

   @Override
   public void sendInfo(Player player) {
      if (this.isInBoat()) {
         this.setXYZ(this.getBoat().getLocation());
         player.sendPacket(new CharInfo(this, this.isInvisible() && player.isGM()));
         player.sendPacket(new ExBrExtraUserInfo(this));
         player.sendPacket(new GetOnVehicle(this.getObjectId(), this.getBoat().getObjectId(), this._inVehiclePosition));
      } else if (this.isInAirShip()) {
         this.setXYZ(this.getAirShip().getLocation());
         player.sendPacket(new CharInfo(this, this.isInvisible() && player.isGM()));
         player.sendPacket(new ExBrExtraUserInfo(this));
         player.sendPacket(new ExGetOnAirShip(this, this.getAirShip()));
      } else {
         player.sendPacket(new CharInfo(this, this.isInvisible() && player.isGM()));
         player.sendPacket(new ExBrExtraUserInfo(this));
      }

      int relation = this.getRelation(player);
      boolean isAutoAttackable = this.isAutoAttackable(player);
      RelationCache cache = this.getKnownRelations().get(player.getObjectId());
      if (cache == null || cache.getRelation() != relation || cache.isAutoAttackable() != isAutoAttackable) {
         player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
         if (this.hasSummon()) {
            player.sendPacket(new RelationChanged(this._summon, relation, isAutoAttackable));
         }

         this.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
      }

      switch (this._privateStoreType) {
         case SELL:
            player.sendPacket(new PrivateStoreMsgSell(this));
            break;
         case PACKAGE_SELL:
            player.sendPacket(new ExPrivateStoreSetWholeMsg(this));
            break;
         case BUY:
            player.sendPacket(new PrivateStoreMsgBuy(this));
            break;
         case MANUFACTURE:
            player.sendPacket(new RecipeShopMsg(this));
      }

      if (this.isTransformed()) {
         player.sendPacket(new CharInfo(this, this.isInvisible() && player.isGM()));
      }
   }

   public void playMovie(MovieHolder holder) {
      if (this._movieHolder == null) {
         this.abortAttack();
         this.stopMove(null);
         this.setMovieHolder(holder);
         if (!this.isTeleporting()) {
            this.sendPacket(new ExStartScenePlayer(holder.getMovie()));
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
            return this.isCastingNow() || this.isCastingSimultaneouslyNow() ? false : !this.isInBoat() && !this.isInAirShip();
         }
      } else {
         return false;
      }
   }

   public void setCreateDate(Calendar createDate) {
      this._createDate = createDate;
   }

   public Calendar getCreateDate() {
      return this._createDate;
   }

   public int checkBirthDay() {
      Calendar now = Calendar.getInstance();
      if (this._createDate.get(5) == 29 && this._createDate.get(2) == 1) {
         this._createDate.add(11, -24);
      }

      if (now.get(2) == this._createDate.get(2) && now.get(5) == this._createDate.get(5) && now.get(1) != this._createDate.get(1)) {
         return 0;
      } else {
         for (int i = 1; i < 6; i++) {
            now.add(11, 24);
            if (now.get(2) == this._createDate.get(2) && now.get(5) == this._createDate.get(5) && now.get(1) != this._createDate.get(1)) {
               return i;
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
      String sqlQuery = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=0");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int friendId = rs.getInt("friendId");
               if (friendId != this.getObjectId()) {
                  this._friendList.add(friendId);
               }
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Error found in " + this.getName() + "'s FriendList: " + var13.getMessage(), (Throwable)var13);
      }
   }

   private void notifyFriends() {
      FriendStatusPacket pkt = new FriendStatusPacket(this.getObjectId());

      for (int id : this._friendList) {
         Player friend = World.getPlayer(id);
         if (friend != null) {
            friend.sendPacket(pkt);
         }
      }
   }

   public boolean isSilenceMode() {
      return this._silenceMode;
   }

   public boolean isSilenceMode(int playerObjId) {
      return PlayerConfig.SILENCE_MODE_EXCLUDE && this._silenceMode && this._silenceModeExcluded != null
         ? !this._silenceModeExcluded.contains(playerObjId)
         : this._silenceMode;
   }

   public void setSilenceMode(boolean mode) {
      this._silenceMode = mode;
      if (this._silenceModeExcluded != null) {
         this._silenceModeExcluded.clear();
      }

      this.sendPacket(new EtcStatusUpdate(this));
   }

   public void addSilenceModeExcluded(int playerObjId) {
      if (this._silenceModeExcluded == null) {
         this._silenceModeExcluded = new ArrayList<>(1);
      }

      this._silenceModeExcluded.add(playerObjId);
   }

   private void storeRecipeShopList() {
      if (this.hasManufactureShop()) {
         try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement st = con.prepareStatement("DELETE FROM character_recipeshoplist WHERE charId=?")) {
               st.setInt(1, this.getObjectId());
               st.execute();
            }

            try (PreparedStatement st = con.prepareStatement(
                  "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)"
               )) {
               AtomicInteger slot = new AtomicInteger(1);

               for (ManufactureItem item : this._manufactureItems.values()) {
                  st.setInt(1, this.getObjectId());
                  st.setInt(2, item.getRecipeId());
                  st.setLong(3, item.getCost());
                  st.setInt(4, slot.getAndIncrement());
                  st.addBatch();
               }

               st.executeBatch();
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
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this.getManufactureItems().put(rs.getInt("recipeId"), new ManufactureItem(rs.getInt("recipeId"), rs.getLong("price")));
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

   public void setClientX(int value) {
      this._clientX = value;
   }

   public void setClientY(int value) {
      this._clientY = value;
   }

   public void setClientZ(int value) {
      this._clientZ = value;
   }

   public void setClientHeading(int value) {
      this._clientHeading = value;
   }

   public boolean isFalling(int z) {
      if (!this.isDead() && !this.isFlying() && !this.isFlyingMounted() && !this.isInsideZone(ZoneId.WATER)) {
         if (this._fallingTimestamp != 0L && System.currentTimeMillis() < this._fallingTimestamp) {
            return true;
         } else {
            int deltaZ = this.getZ() - z;
            if (deltaZ <= this.getBaseTemplate().getSafeFallHeight()) {
               this._fallingTimestamp = 0L;
               return false;
            } else if (!GeoEngine.getInstance().hasGeo(this.getX(), this.getY())) {
               this._fallingTimestamp = 0L;
               return false;
            } else {
               if (this._fallingDamage == 0) {
                  this._fallingDamage = (int)Formulas.calcFallDam(this, deltaZ);
               }

               if (this._fallingDamageTask != null) {
                  this._fallingDamageTask.cancel(true);
               }

               this._fallingDamageTask = ThreadPool.schedule(() -> {
                  if (this._fallingDamage > 0 && !this.isInvul()) {
                     this.reduceCurrentHp(Math.min((double)this._fallingDamage, this.getCurrentHp() - 1.0), null, false, true, null);
                     SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RECEIVED_S1_DAMAGE_FROM_TAKING_A_HIGH_FALL);
                     sm.addInt(this._fallingDamage);
                     this.sendPacket(sm);
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

   public void setMovieHolder(MovieHolder movie) {
      this._movieHolder = movie;
   }

   public void updateLastItemAuctionRequest() {
      this._lastItemAuctionInfoRequest = System.currentTimeMillis();
   }

   public boolean isItemAuctionPolling() {
      return System.currentTimeMillis() - this._lastItemAuctionInfoRequest < 2000L;
   }

   @Override
   public boolean isMovementDisabled() {
      return super.isMovementDisabled() || this._movieHolder != null;
   }

   public String getHtmlPrefix() {
      return MultilingualSupportConfig.MULTILANG_ENABLE ? this._htmlPrefix : "";
   }

   public String getLang() {
      return this._lang;
   }

   public boolean setLang(String lang) {
      boolean result = false;
      if (MultilingualSupportConfig.MULTILANG_ENABLE) {
         if (MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang)) {
            this._lang = lang;
            result = true;
         } else {
            this._lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
         }

         this._htmlPrefix = this._lang.equals("en") ? "" : "data/lang/" + this._lang + "/";
      } else {
         this._lang = null;
         this._htmlPrefix = "";
      }

      return result;
   }

   public long getOfflineStartTime() {
      return this._offlineShopStart;
   }

   public void setOfflineStartTime(long time) {
      this._offlineShopStart = time;
   }

   public int getPcCafePoints() {
      return this._pcCafePoints;
   }

   public void setPcCafePoints(int count) {
      this._pcCafePoints = count < PremiumSystemConfig.PC_CAFE_MAX_POINTS ? count : PremiumSystemConfig.PC_CAFE_MAX_POINTS;
   }

   public void removeFromBossZone() {
      try {
         for (BossZone zone : GrandBossManager.getInstance().getZones().values()) {
            zone.removePlayer(this);
         }
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Exception on removeFromBossZone(): " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void checkPlayerSkills() {
      for (Entry<Integer, Skill> e : this.getSkills().entrySet()) {
         SkillLearn learn = SkillTreeData.getInstance().getClassSkill(e.getKey(), e.getValue().getLevel() % 100, this.getPlayerClass());
         if (learn != null) {
            int levelDiff = e.getKey() == CommonSkill.EXPERTISE.getId() ? 0 : 9;
            if (this.getLevel() < learn.getGetLevel() - levelDiff) {
               this.deacreaseSkillLevel(e.getValue(), levelDiff);
            }
         }
      }
   }

   private void deacreaseSkillLevel(Skill skill, int levelDiff) {
      int nextLevel = -1;
      Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(this.getPlayerClass());

      for (SkillLearn sl : skillTree.values()) {
         if (sl.getSkillId() == skill.getId() && nextLevel < sl.getSkillLevel() && this.getLevel() >= sl.getGetLevel() - levelDiff) {
            nextLevel = sl.getSkillLevel();
         }
      }

      if (nextLevel == -1) {
         LOGGER.info("Removing skill " + skill + " from " + this);
         this.removeSkill(skill, true);
      } else {
         LOGGER.info("Decreasing skill " + skill + " to " + nextLevel + " for " + this);
         this.addSkill(SkillData.getInstance().getSkill(skill.getId(), nextLevel), true);
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

   public void setMultiSocialAction(int id, int targetId) {
      this._multiSociaAction = id;
      this._multiSocialTarget = targetId;
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

   public void setBookmarkSlot(int slot) {
      this._bookmarkSlot = slot;
      this.sendPacket(new ExGetBookMarkInfoPacket(this));
   }

   public boolean canAttackCreature(Creature creature) {
      if (creature == null) {
         return false;
      } else if (creature.isAttackable()) {
         return true;
      } else {
         if (creature.isPlayable()) {
            if (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE)) {
               return true;
            }

            Player target;
            if (creature.isSummon()) {
               target = creature.asSummon().getOwner();
            } else {
               target = creature.asPlayer();
            }

            if (this.isInDuel() && target.isInDuel() && target.getDuelId() == this.getDuelId()) {
               return true;
            }

            if (this.isInParty() && target.isInParty()) {
               if (this.getParty() == target.getParty()) {
                  return false;
               }

               if ((this.getParty().getCommandChannel() != null || target.getParty().getCommandChannel() != null)
                  && this.getParty().getCommandChannel() == target.getParty().getCommandChannel()) {
                  return false;
               }
            } else if (this.getClan() != null && target.getClan() != null) {
               if (this.getClanId() == target.getClanId()) {
                  return false;
               }

               if ((this.getAllyId() > 0 || target.getAllyId() > 0) && this.getAllyId() == target.getAllyId()) {
                  return false;
               }

               if (this.getClan().isAtWarWith(target.getClan().getId()) && target.getClan().isAtWarWith(this.getClan().getId())) {
                  return true;
               }
            } else if ((this.getClan() == null || target.getClan() == null) && target.getPvpFlag() == 0 && target.getKarma() == 0) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isInventoryUnder90(boolean includeQuestInv) {
      return (includeQuestInv ? this._inventory.getSize() : this._inventory.getNonQuestSize()) <= this.getInventoryLimit() * 0.9;
   }

   public boolean isInventoryUnder80(boolean includeQuestInv) {
      return (includeQuestInv ? this._inventory.getSize() : this._inventory.getNonQuestSize()) <= this.getInventoryLimit() * 0.8;
   }

   public boolean havePetInvItems() {
      return this._petItems;
   }

   public void setPetInvItems(boolean haveit) {
      this._petItems = haveit;
   }

   private void restorePetInventoryItems() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;");
      ) {
         ps.setInt(1, this.getObjectId());

         try (ResultSet rs = ps.executeQuery()) {
            this.setPetInvItems(rs.next() && rs.getInt("object_id") > 0);
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + this.getObjectId(), (Throwable)var12);
      }
   }

   public String getAdminConfirmCmd() {
      return this._adminConfirmCmd;
   }

   public void setAdminConfirmCmd(String adminConfirmCmd) {
      this._adminConfirmCmd = adminConfirmCmd;
   }

   public void setPremiumStatus(boolean premiumStatus) {
      this._premiumStatus = premiumStatus;
   }

   public boolean hasPremiumStatus() {
      return PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && this._premiumStatus;
   }

   public void setLastPetitionGmName(String gmName) {
      this._lastPetitionGmName = gmName;
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

   @Override
   public boolean isPlayer() {
      return true;
   }

   @Override
   public Player asPlayer() {
      return this;
   }

   @Override
   public boolean isChargedShot(ShotType type) {
      Item weapon = this.getActiveWeaponInstance();
      return weapon != null && weapon.isChargedShot(type);
   }

   @Override
   public void setChargedShot(ShotType type, boolean charged) {
      Item weapon = this.getActiveWeaponInstance();
      if (weapon != null) {
         weapon.setChargedShot(type, charged);
      }
   }

   public Skill getCustomSkill(int skillId) {
      return this._customSkills != null ? this._customSkills.get(skillId) : null;
   }

   private void addCustomSkill(Skill skill) {
      if (skill != null && skill.getDisplayId() != skill.getId()) {
         if (this._customSkills == null) {
            this._customSkills = new ConcurrentHashMap<>();
         }

         this._customSkills.put(skill.getDisplayId(), skill);
      }
   }

   private void removeCustomSkill(Skill skill) {
      if (skill != null && this._customSkills != null && skill.getDisplayId() != skill.getId()) {
         this._customSkills.remove(skill.getDisplayId());
      }
   }

   @Override
   public boolean canRevive() {
      return this._canRevive;
   }

   @Override
   public void setCanRevive(boolean value) {
      this._canRevive = value;
   }

   public boolean isRegisteredOnEvent() {
      return this._isRegisteredOnEvent || this._isOnEvent;
   }

   public void setRegisteredOnEvent(boolean value) {
      this._isRegisteredOnEvent = value;
   }

   @Override
   public boolean isOnEvent() {
      return this._isOnEvent;
   }

   public void setOnEvent(boolean value) {
      this._isOnEvent = value;
   }

   public boolean isOnSoloEvent() {
      return this._isOnSoloEvent;
   }

   public void setOnSoloEvent(boolean value) {
      this._isOnSoloEvent = value;
   }

   public void setBlockCheckerArena(byte arena) {
      this._handysBlockCheckerEventArena = arena;
   }

   public int getBlockCheckerArena() {
      return this._handysBlockCheckerEventArena;
   }

   public KrateiArena getKrateiArena() {
      return this._krateiArena;
   }

   public void setKrateiArena(KrateiArena arena) {
      this._krateiArena = arena;
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

   public void setUCState(int state) {
      this.setRegisteredOnEvent(state != 0);
      this._ucState = state;
   }

   public int getUCState() {
      return this._ucState;
   }

   public void setOriginalCpHpMp(double cp, double hp, double mp) {
      this._originalCp = cp;
      this._originalHp = hp;
      this._originalMp = mp;
   }

   public boolean hasVariables() {
      return this.getScript(PlayerVariables.class) != null;
   }

   public PlayerVariables getVariables() {
      PlayerVariables vars = this.getScript(PlayerVariables.class);
      return vars != null ? vars : this.addScript(new PlayerVariables(this.getObjectId()));
   }

   public boolean hasAccountVariables() {
      return this.getScript(AccountVariables.class) != null;
   }

   public AccountVariables getAccountVariables() {
      AccountVariables vars = this.getScript(AccountVariables.class);
      return vars != null ? vars : this.addScript(new AccountVariables(this.getAccountName()));
   }

   @Override
   public int getId() {
      return this.getPlayerClass().getId();
   }

   public boolean isPartyBanned() {
      return PunishmentManager.getInstance().hasPunishment(this.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
   }

   public boolean addAction(PlayerAction act) {
      if (!this.hasAction(act)) {
         this._actionMask = this._actionMask | act.getMask();
         return true;
      } else {
         return false;
      }
   }

   public boolean removeAction(PlayerAction act) {
      if (this.hasAction(act)) {
         this._actionMask = this._actionMask & ~act.getMask();
         return true;
      } else {
         return false;
      }
   }

   public boolean hasAction(PlayerAction act) {
      return (this._actionMask & act.getMask()) == act.getMask();
   }

   public void setCharmOfCourage(boolean value) {
      this._hasCharmOfCourage = value;
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

   public boolean isAtWarWith(Creature target) {
      if (target == null) {
         return false;
      } else {
         return this._clan != null && !this.isAcademyMember() && target.getClan() != null && !target.isAcademyMember()
            ? this._clan.isAtWarWith(target.getClan())
            : false;
      }
   }

   public boolean isInPartyWith(Creature target) {
      return this.isInParty() && target.isInParty() && this.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId();
   }

   public boolean isInCommandChannelWith(Creature target) {
      return this.isInParty()
         && target.isInParty()
         && this.getParty().isInCommandChannel()
         && target.getParty().isInCommandChannel()
         && this.getParty().getCommandChannel().getLeaderObjectId() == target.getParty().getCommandChannel().getLeaderObjectId();
   }

   public boolean isInClanWith(Creature target) {
      return this.getClanId() != 0 && target.getClanId() != 0 && this.getClanId() == target.getClanId();
   }

   public boolean isInAllyWith(Creature target) {
      return this.getAllyId() != 0 && target.getAllyId() != 0 && this.getAllyId() == target.getAllyId();
   }

   public boolean isInDuelWith(Creature target) {
      return this.isInDuel() && target.isInDuel() && this.getDuelId() == target.getDuelId();
   }

   public boolean isOnSameSiegeSideWith(Creature target) {
      return this.getSiegeState() > 0
         && this.isInsideZone(ZoneId.SIEGE)
         && this.getSiegeState() == target.getSiegeState()
         && this.getSiegeSide() == target.getSiegeSide();
   }

   public long getGamePoints() {
      return this.getAccountVariables().getInt("PRIME_POINTS", 0);
   }

   public void setGamePoints(long points) {
      AccountVariables vars = this.getAccountVariables();
      vars.set("PRIME_POINTS", Math.max(points, 0L));
      vars.storeMe();
   }

   private TerminateReturn onExperienceReceived() {
      return this.isDead() ? new TerminateReturn(false, false, false) : new TerminateReturn(true, true, true);
   }

   public void disableExpGain() {
      this.addListener(new FunctionEventListener(this, EventType.ON_PLAYABLE_EXP_CHANGED, var1 -> this.onExperienceReceived(), this));
   }

   public void enableExpGain() {
      this.removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
   }

   public void sendInventoryUpdate(InventoryUpdate iu) {
      if (this._inventoryUpdateTask != null) {
         this._inventoryUpdateTask.cancel(false);
      }

      this._inventoryUpdate.putAll(iu.getItemEntries());
      this._inventoryUpdateTask = ThreadPool.schedule(() -> this.sendPacket(this._inventoryUpdate), 100L);
   }

   public void sendItemList(boolean open) {
      if (this._itemListTask != null) {
         this._itemListTask.cancel(false);
      }

      this._itemListTask = ThreadPool.schedule(() -> this.sendPacket(new ItemList(this, open)), 250L);
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
         for (QuestTimer timer : this._questTimers) {
            timer.cancelTask();
         }

         this._questTimers.clear();
      }

      synchronized (this._timerHolders) {
         for (TimerHolder<?> timer : this._timerHolders) {
            timer.cancelTask();
         }

         this._timerHolders.clear();
      }
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

   public void setServitorShare(Map<Stat, Double> map) {
      this._servitorShare = map;
   }

   public double getServitorShareBonus(Stat stat) {
      Map<Stat, Double> stats = this._servitorShare;
      if (stats == null) {
         return 1.0;
      } else {
         Double val = stats.get(stat);
         return val == null ? 1.0 : val;
      }
   }

   public AutoPlaySettingsHolder getAutoPlaySettings() {
      return this._autoPlaySettings;
   }

   public AutoUseSettingsHolder getAutoUseSettings() {
      return this._autoUseSettings;
   }

   public void setAutoPlaying(boolean value) {
      this._autoPlaying.set(value);
      if (!value && this._offlinePlay && OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS) {
         OfflinePlayTable.getInstance().removeOfflinePlay(this);
      }
   }

   public boolean isAutoPlaying() {
      return this._autoPlaying.get();
   }
}
