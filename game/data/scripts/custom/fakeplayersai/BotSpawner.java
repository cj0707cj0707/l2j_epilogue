/*
 * Fake Players AI - bot creation, world entry, equipment and auto-combat setup.
 */
package custom.fakeplayersai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author FakePlayersAI
 */
public class BotSpawner
{
	private static final Logger LOGGER = Logger.getLogger(BotSpawner.class.getName());
	
	private static final int SPIRIT_ORE_ID = 3031;

	/**
	 * Universal default passive: the Subclass Counter Celestial (Enchanter Ability -
	 * Barrier, skill 655) - while being attacked there is a 2% chance to trigger skill
	 * 5576, 5 seconds of invincibility (the Celestial Shield icon). Every bot and every
	 * real player receives it by default (players - from the core Player.restoreSkills;
	 * bots - also here at registration, because freshly created bots never run
	 * restoreSkills). Runtime-only: not stored in character_skills, re-granted on every
	 * load; the SubClassSkills validator whitelists the id.
	 */
	public static final int COUNTER_CELESTIAL_ID = 655;

	/** Largest enchant route length in EnchantSkillGroups.xml (2nd class groups hold 30 levels, 3rd class - 15). */
	private static final int MAX_SKILL_ENCHANT_LEVEL = 30;

	/**
	 * FuncTemplate "order" codes assigned by DocumentBase to the effect stat operations
	 * (documented mapping verified in the core: add -> 1, sub -> 2, mul -> 3, div -> 4).
	 * Explicit order="..." attributes are not used by the skill data of this pack.
	 */
	private static final int FUNC_ORDER_ADD = 1;
	private static final int FUNC_ORDER_SUB = 2;
	private static final int FUNC_ORDER_MUL = 3;

	/** AutoPlay "auto attack" action id (same constant as AutoPlayTaskManager.AUTO_ATTACK_ACTION). */
	private static final int AUTO_ATTACK_ACTION = 2;

	/** Admin charge window ceiling of the sonic/force charge classes (mirror of BotManager.CHARGE_MAX = 7; the retail cap is 8). */
	private static final int CHARGE_WINDOW_MAX = 7;

	/**
	 * Basic buff set (skillId, level) every bot receives for an olympiad match.
	 * Applied instantly (no cast time), mirroring how the core applies its standard
	 * olympiad buffs in OlympiadGameTask.applyBuffs(); levels are the maximum
	 * available for Gracia Epilogue. The core's own pair of olympiad buffs
	 * (Wind Walk 2 + Haste 1, now for both warriors and mages) is
	 * refreshed/overwritten by the higher-level entries of this set.
	 *
	 * The set is the ADMIN MANDATORY LIST of 13 buffs (rule of 04.10.2026): Shield,
	 * Might, Magic Barrier, Blessed Body, Acumen, Empower, Wild Magic, Berserker
	 * Spirit, Concentration, Death Whisper, Focus, Haste, Wind Walk - every
	 * participant (bots and real players alike) enters the match with all of them.
	 */
	private static final int[][] OLYMPIAD_BUFFS =
	{
		{ 1204, 2 }, // Wind Walk 2
		{ 1086, 2 }, // Haste 2
		{ 1062, 2 }, // Berserker Spirit 2
		{ 1068, 3 }, // Might 3
		{ 1059, 3 }, // Empower 3
		{ 1077, 3 }, // Focus 3
		{ 1242, 3 }, // Death Whisper 3
		{ 1085, 3 }, // Acumen 3
		{ 1045, 6 }, // Blessed Body 6
		{ 1303, 2 }, // Wild Magic 2
		{ 1040, 3 }, // Shield 3
		{ 1036, 2 }, // Magic Barrier 2
		{ 1078, 6 }, // Concentration 6
	};

	/**
	 * Full "warrior" buff profile, kept up on every non-caster bot outside olympiad matches
	 * (Coliseum world PvP and while waiting for the olympiad; inside matches the olympiad
	 * profile from getOlympiadBuffs is used - no CoV, no dances/songs). Applied instantly
	 * when missing, like OLYMPIAD_BUFFS. The set plus the largest class self-buff set plus
	 * the olympiad extras stays within the 34 buff slots of this server (MaxBuffAmount = 34),
	 * so no eviction churn happens.
	 */
	private static final int[][] FULL_BUFFS_FIGHTER =
	{
		{ 1204, 2 }, // Wind Walk 2
		{ 1086, 2 }, // Haste 2
		{ 1068, 3 }, // Might 3
		{ 1077, 3 }, // Focus 3
		{ 1242, 3 }, // Death Whisper 3
		{ 1240, 3 }, // Guidance 3
		{ 1062, 2 }, // Berserker Spirit 2
		{ 1045, 6 }, // Blessed Body 6
		{ 1048, 6 }, // Blessed Soul 6
		{ 1040, 3 }, // Shield 3
		{ 1036, 2 }, // Magic Barrier 2
		{ 1268, 4 }, // Vampiric Rage 4
		{ 1044, 3 }, // Regeneration 3
		{ 1363, 1 }, // Chant of Victory
		{ 271, 1 }, // Dance of the Warrior
		{ 274, 1 }, // Dance of Fire
		{ 264, 1 }, // Song of Earth
		{ 267, 1 }, // Song of Warding
		{ 268, 1 }, // Song of Wind
		{ 269, 1 }, // Song of Hunter
		{ 304, 1 }, // Song of Vitality
	};

	/**
	 * Full "mage" buff profile, kept up on every caster bot (isCaster() presets, Dominator included).
	 * Chant of Victory (1363) is NOT in this list on purpose: it shares the abnormalType
	 * MULTI_BUFF (level 1) with Prophecy of Water (1355) - both entries cannot coexist, and
	 * keeping both made maintainFullBuffs re-apply them endlessly (each application replaced
	 * the other, the log showed "applied 2 missing buff(s)" on every 2-second combat tick).
	 * Prophecy of Water stays as the MULTI_BUFF holder of the Coliseum profile; inside
	 * olympiad matches the whole Chant of Victory family is banned - the olympiad mage
	 * profile (getOlympiadBuffs) filters it out like CoV itself.
	 */
	private static final int[][] FULL_BUFFS_MAGE =
	{
		{ 1204, 2 }, // Wind Walk 2
		{ 1085, 3 }, // Acumen 3
		{ 1059, 3 }, // Empower 3
		{ 1078, 6 }, // Concentration 6
		{ 1303, 2 }, // Wild Magic 2
		{ 1062, 2 }, // Berserker Spirit 2
		{ 1045, 6 }, // Blessed Body 6
		{ 1048, 6 }, // Blessed Soul 6
		{ 1040, 3 }, // Shield 3
		{ 1036, 2 }, // Magic Barrier 2
		{ 1355, 1 }, // Prophecy of Water
		{ 276, 1 }, // Dance of Concentration
		{ 363, 1 }, // Song of Meditation
		{ 304, 1 }, // Song of Vitality
		{ 267, 1 }, // Song of Warding
	};

	/**
	 * Chant of Victory (1363) and its analogs - the "prophecy/chant" buff family
	 * (one shared abnormalType MULTI_BUFF slot): Prophecy of Water (1355), Prophecy of
	 * Fire (1356), Prophecy of Wind (1357), Chant of Victory (1363), Magnus' Chant (1413),
	 * Victory of Pa'agrio (1414) and the GM "Master's Blessing" versions of the same buffs
	 * (7061-7064). NONE of them may exist on an olympiad participant (bots and real players
	 * alike, admin rule): they are filtered out of the olympiad buff profiles, kept out of
	 * the bots' live auto lists inside olympiad matches and stripped from every olympiad
	 * participant's effect list (see stripChantBuffs). The Coliseum (world PvP) profiles
	 * are NOT touched by this ban - outside olympiad matches the full profiles work as
	 * before.
	 */
	private static final int[] CHANT_OF_VICTORY_FAMILY =
	{
		1355, // Prophecy of Water
		1356, // Prophecy of Fire
		1357, // Prophecy of Wind
		1363, // Chant of Victory
		1413, // Magnus' Chant
		1414, // Victory of Pa'agrio
		7061, // Master's Blessing - Prophecy of Water
		7062, // Master's Blessing - Prophecy of Fire
		7063, // Master's Blessing - Prophecy of Wind
		7064, // Master's Blessing - Chant of Victory
	};

	/**
	 * Olympiad buff profiles: the full profiles WITHOUT any Chant of Victory family
	 * buff (CoV and its analogs - Prophecy of Water included) and WITHOUT any
	 * dances/songs (all isMagic=3 skills - isDance() covers both
	 * dances and songs in this core). maintainFullBuffs switches to these
	 * lists while the bot is inside an olympiad match, so the arena buff list
	 * contains neither CoV nor dances/songs. Built lazily on first use
	 * (SkillData must be loaded by then; combat ticks always run after load).
	 */
	private static volatile int[][] OLYMPIAD_BUFFS_FIGHTER = null;
	private static volatile int[][] OLYMPIAD_BUFFS_MAGE = null;

	/**
	 * @return true when the skill id belongs to the Chant of Victory buff family
	 * (CoV itself or one of its analogs - see CHANT_OF_VICTORY_FAMILY).
	 */
	public static boolean isChantOfVictoryFamily(int skillId)
	{
		return containsId(CHANT_OF_VICTORY_FAMILY, skillId);
	}

	/**
	 * Builds an olympiad buff profile from a full profile: drops every Chant of
	 * Victory family buff (CoV and its analogs) and every dance/song entry.
	 * @param profile the full buff profile (skillId, level entries)
	 * @return the filtered olympiad profile
	 */
	private static int[][] buildOlympiadProfile(int[][] profile)
	{
		final List<int[]> result = new ArrayList<>(profile.length);
		for (int[] buff : profile)
		{
			if (isChantOfVictoryFamily(buff[0]))
			{
				continue;
			}

			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if ((skill != null) && skill.isDance())
			{
				continue;
			}

			result.add(buff);
		}
		return result.toArray(new int[result.size()][]);
	}

	/**
	 * Gets the olympiad buff profile for the given bot type (built on first use).
	 * @param caster true for the mage profile, false for the warrior profile
	 * @return the olympiad buff profile (no Chant of Victory family, no dances/songs)
	 */
	private static int[][] getOlympiadBuffs(boolean caster)
	{
		if (caster)
		{
			if (OLYMPIAD_BUFFS_MAGE == null)
			{
				OLYMPIAD_BUFFS_MAGE = buildOlympiadProfile(FULL_BUFFS_MAGE);
			}
			return OLYMPIAD_BUFFS_MAGE;
		}

		if (OLYMPIAD_BUFFS_FIGHTER == null)
		{
			OLYMPIAD_BUFFS_FIGHTER = buildOlympiadProfile(FULL_BUFFS_FIGHTER);
		}
		return OLYMPIAD_BUFFS_FIGHTER;
	}

	/** S-grade blessed spiritshot: every bot always stocks and auto-uses it (alongside the soulshot). */
	private static final int BLESSED_SPIRITSHOT_S = 3952;

	/**
	 * Olympiad summoner support (Soultaker / Hell Knight / Phoenix Knight): the summoner classes
	 * of the roster and their summon/toggle skills. The combat driver keeps the class summon
	 * always alive during olympiad matches, the Soultaker keeps Transfer Pain toggled on, and
	 * the knights keep a beast soulshot stock for their servitor and send it into the fight.
	 */
	private static final int CLASS_PHOENIX_KNIGHT = 90;
	private static final int CLASS_HELL_KNIGHT = 91;
	private static final int CLASS_SOULTAKER = 95;

	private static final int SUMMON_CURSED_MAN_ID = 1334; // Necromancer -> Soultaker
	private static final int TRANSFER_PAIN_ID = 1262; // Soultaker damage redirect toggle
	private static final int SUMMON_DARK_PANTHER_ID = 283; // Dark Avenger -> Hell Knight
	private static final int SUMMON_IMPERIAL_PHOENIX_ID = 912; // Phoenix Knight (Forgotten Scroll)

	/** Beast Soulshot (6645): pet/servitor soulshot, used from the master's inventory. */
	private static final int BEAST_SOULSHOT_ID = 6645;
	/** Required beast soulshot stock for the summoner knights (Hell Knight, Phoenix Knight). */
	private static final int PET_SOULSHOT_COUNT = 2000;

	/** Servitor upkeep crystals (the servitor life task drains them; empty stock unsummons it). */
	private static final int CRYSTAL_C_ID = 1459; // Cursed Man / Dark Panther
	private static final int CRYSTAL_A_ID = 1461; // Imperial Phoenix
	private static final int SERVITOR_CRYSTAL_STOCK = 100;

	/** Spirit Ore stock per bot: kept full, never above the cap. */
	private static final int SPIRIT_ORE_CAP = 1000;

	/** Arcane Power (all caster presets) and Enlightenment (wizard variant 1532 for every caster - the Dominator uses it too since 03.10.2026). */
	private static final int ARCANE_POWER_ID = 337;
	private static final int ENLIGHTENMENT_ID = 1532;

	/**
	 * Opening self-enhancements of an olympiad match (bots), applied ONCE shortly BEFORE
	 * the gates open by the protected preparation phase:
	 * Final Secret (917) for every class that knows it (Duelist, Titan, Grand Khavatari,
	 * Doombringer), the dagger class evasion opener and the dagger adventure self-enhancement.
	 * The evasion opener is the class's own evasion skill from its skill tree: Adventurer (93)
	 * - Mirage (445), Wind Rider (101) - Dodge (446), Ghost Hunter (108) - Ghost Walking (770).
	 * The adventure skill: Adventurer - Exciting Adventure (768), Wind Rider - Wind Riding (769).
	 */
	private static final int FINAL_SECRET_ID = 917;
	private static final int CLASS_ADVENTURER = 93;
	private static final int CLASS_WIND_RIDER = 101;
	private static final int CLASS_GHOST_HUNTER = 108;
	private static final int MIRAGE_ID = 445;
	private static final int DODGE_ID = 446;
	private static final int GHOST_WALKING_ID = 770;
	private static final int EXCITING_ADVENTURE_ID = 768;
	private static final int WIND_RIDING_ID = 769;

	/**
	 * Olympiad gate rule (admin request): once the stadium gates are open (the match has
	 * started), NO self-buff whose duration is longer than 2 minutes may be (re)applied.
	 * Buffs above this abnormalTime (seconds) are held out of the auto-buff list and out
	 * of the buff-profile maintenance while the match is running; the pre-start hook
	 * applied the full set 2 seconds before the gates opened, which stays allowed.
	 */
	private static final int OLYMPIAD_MAX_SELFBUFF_SECONDS = 120;

	/**
	 * Dagger evasion openers (Mirage 445 / Dodge 446 / Ghost Walking 770): the auto-buff
	 * engine must never cast them inside an olympiad match (countdown included) - they are
	 * applied ONLY by the protected pre-combat preparation phase shortly BEFORE the gates
	 * open. In the world (Coliseum PvP) the daggers keep using
	 * them as regular auto-buffs.
	 */
	private static final int[] OPENER_ONLY_SKILLS =
	{
		MIRAGE_ID,
		DODGE_ID,
		GHOST_WALKING_ID,
        FINAL_SECRET_ID, EXCITING_ADVENTURE_ID, WIND_RIDING_ID, ENLIGHTENMENT_ID, 1533, ARCANE_POWER_ID,
	};

	// ------------------------------------------------------------------
	// Tank bots: the defensive self-buff pool (one at a time, by opponent class)
	// ------------------------------------------------------------------

	/**
	 * Vengeance (368) - an AoE taunt + huge self P.Def/M.Def (immobile). The tank
	 * rotation sets (//bot overrides) contain it as an attack-list entry; as a member
	 * of the defense pool it is cast through the auto-buff engine only, and the live
	 * rotation is kept free of it (see refreshTankDefenses).
	 */
	private static final int VENGEANCE_ID = 368;

	/**
	 * The PHYSICAL-defense group of the tank defensive self-buff pool (admin rules):
	 * at most one of them can be active at a time, and they are used against
	 * non-mage (warrior) opponents. Priority order = cast order: the big panic
	 * buttons first, the long situational amplifiers after them.
	 */
	private static final int[] TANK_DEFENSE_PHYSICAL =
	{
		110, // Ultimate Defense (30 s, 900 s reuse - massive P.Def, immobile)
		VENGEANCE_ID, // Vengeance (30 s, 900 s reuse - taunt + huge P.Def/M.Def, immobile)
		350, // Physical Mirror (5 min, 150 s reuse - reflects physical attacks/debuffs)
		439, // Shield of Revenge (5 min, 150 s reuse - counter/block shield stance)
		527, // Iron Shield (20 min, 150 s reuse - triggered block on damage received)
		112, // Deflect Arrow (20 min, 3 s reuse - bow resistance)
		82, // Majesty (5 min, 3 s reuse - P.Def up / evasion down)
		86, // Reflect Damage (20 min, 3 s reuse - reflects melee damage)
	};

	/**
	 * The MAGIC-defense group of the tank defensive self-buff pool: used against mage
	 * opponents (see refreshTankDefenses). Priority order = cast order.
	 */
	private static final int[] TANK_DEFENSE_MAGIC =
	{
		760, // Anti Magic Armor (30 s, 900 s reuse - massive M.Def, immobile)
		913, // Deflect Magic (20 min, 3 s reuse - magic resist)
		351, // Magical Mirror (5 min, 150 s reuse - reflects magic/debuffs)
		72, // Iron Will (20 min, 3 s reuse - M.Def up)
		916, // Shield Deflect Magic (8 s, 45 s reuse - reflects magic damage with shield)
	};

	/** Both defense groups together (the full mutual-exclusion pool). */
	private static final int[] TANK_DEFENSE_ALL =
	{
		110, VENGEANCE_ID, 350, 439, 527, 112, 82, 86, // physical group
		760, 913, 351, 72, 916 // magic group
	};

	/**
	 * @return true when the skill id belongs to the tank defensive self-buff pool
	 *         (either the physical or the magic defense group - see TANK_DEFENSE_ALL).
	 *         The permanent stance toggles (Shield Fortress 322, Fortitude 335, Guard /
	 *         Aegis stances) are NOT part of the pool (admin decision) and keep working
	 *         as regular auto-buffs.
	 */
	public static boolean isTankDefenseSkill(int skillId)
	{
		return containsId(TANK_DEFENSE_ALL, skillId);
	}

	/**
	 * Whitelist of the mage "main attack" skills - the 80% share of the caster bot rotation:
	 * Hydro Blast, Ice Vortex, Ice Vortex Crusher, Prominence, Fire Vortex, Fire Vortex Buster,
	 * Dark Vortex, Hurricane, Wind Vortex Slug, Wind Vortex. (Documentation/classification aid:
	 * the weighted rotation is built from the allowed preset rotation, curse skills go to the
	 * 20% share, everything else to the 80% attack share.)
	 */
	private static final int[] MAGE_ATTACK_SKILLS =
	{
		1235, // Hydro Blast
		1340, // Ice Vortex
		1453, // Ice Vortex Crusher
		1230, // Prominence
		1339, // Fire Vortex
		1451, // Fire Vortex Buster
		1234, // Vampiric Claw (learned Necromancer nuke)
		1343, // Dark Vortex
		1239, // Hurricane
		1456, // Wind Vortex Slug
		1341, // Wind Vortex
	};

	/**
	 * Curse share of the caster bot rotation (enemy debuffs of the caster classes). The datapack
	 * does not mark these skills with isDebuff/effectPoint, so the classification is explicit.
	 * Everything else in the allowed rotation (the whitelisted nukes and any skill allowed by the
	 * admin) belongs to the attack share.
	 */
	private static final int[] MAGE_CURSE_SKILLS =
	{
		1083, // Surrender To Fire
		1071, // Surrender To Water
		1074, // Surrender To Wind
		1338, // Arcane Chaos
		1056, // Cancellation
		1169, // Curse Fear
		1069, // Sleep
		1160, // Slow
		1263, // Curse Gloom
		1064, // Silence
		1170, // Anchor
		1222, // Curse Chaos
		1337, // Curse of Abyss
		1367, // Seal of Disease
		1097, // Dreaming Spirit
		1092, // Fear
		1105, // Madness
		1246, // Seal of Silence
		1247, // Seal of Scourge
		1248, // Seal of Suspension
		1208, // Seal of Binding
		1099, // Seal of Slow
		1104, // Seal of Winter
		1206, // Wind Shackle
	};

	/**
	 * @return true when the skill belongs to the curse (20%) share of the mage rotation.
	 */
	private static boolean isMageCurse(int skillId)
	{
		for (int id : MAGE_CURSE_SKILLS)
		{
			if (id == skillId)
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------
	// Opening curses (mage bots start every fight with a resist-lowering curse)
	// ------------------------------------------------------------------

	private static final int CLASS_ARCHMAGE = 94;
	private static final int CLASS_MYSTIC_MUSE = 103;
	private static final int CLASS_STORM_SCREAMER = 110;
	// CLASS_SOULTAKER (95) is declared in the olympiad summon section.

	/**
	 * The curse each caster bot opens combat with - the elemental resist-lowering
	 * Surrender of the class element (Archmage: fire, Mystic Muse: water, Storm
	 * Screamer: wind); the necromancer (Soultaker) has no element, it opens with
	 * Curse Gloom (M.Def down). The Dominator has neither an element nor a
	 * Surrender-type skill in its tree - no opener. All four skills are part of
	 * the class trees (and rotations) already.
	 */
	private static final int SURRENDER_TO_FIRE_ID = 1083;
	private static final int SURRENDER_TO_WATER_ID = 1071;
	private static final int SURRENDER_TO_WIND_ID = 1074;
	private static final int CURSE_GLOOM_ID = 1263;

	/**
	 * @return the skill id of the class opening curse, or 0 when the class has none.
	 */
	public static int getOpeningCurseSkillId(BotClassPreset preset)
	{
		if (preset == null)
		{
			return 0;
		}
		switch (preset.getClassId())
		{
			case CLASS_ARCHMAGE:
			{
				return SURRENDER_TO_FIRE_ID;
			}
			case CLASS_MYSTIC_MUSE:
			{
				return SURRENDER_TO_WATER_ID;
			}
			case CLASS_STORM_SCREAMER:
			{
				return SURRENDER_TO_WIND_ID;
			}
			case CLASS_SOULTAKER:
			{
				return CURSE_GLOOM_ID;
			}
			default:
			{
				return 0;
			}
		}
	}

	/**
	 * Makes sure the class opening curse is at the maximum level of its Chance
	 * enchant route (the route that raises the trigger rate: activateRate 80 -> 100;
	 * at 100 the core success formula no longer rolls, the debuff can only be
	 * blocked by explicit debuff immunity). Called from registerBot(), right after
	 * the general enchant policy - the general policy already picks Chance for
	 * these skills (they offer no Power route), this call keeps the guarantee
	 * independent of that policy. Idempotent; writes to the database like
	 * applySkillEnchants does.
	 * @param player the bot
	 * @param preset the bot's class preset
	 * @return true when the skill level was actually changed
	 */
	public static boolean ensureOpeningCurseEnchant(Player player, BotClassPreset preset)
	{
		final int skillId = getOpeningCurseSkillId(preset);
		if ((player == null) || (skillId == 0))
		{
			return false;
		}

		final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
		if (learn == null)
		{
			return false;
		}
		final Skill baseSkill = SkillData.getInstance().getSkill(skillId, learn.getBaseLevel());
		if (baseSkill == null)
		{
			return false;
		}

		// Highest level of the Chance route (the same route labeling heuristic as
		// applySkillEnchants: the route that changes the activate rate).
		int chanceLevel = 0;
		final List<Integer> routes = new ArrayList<>(learn.getAllRoutes());
		Collections.sort(routes);
		for (int route : routes)
		{
			final int maxLevel = getRouteMaxLevel(learn, route);
			if (maxLevel == 0)
			{
				continue;
			}
			final Skill routeSkill = SkillData.getInstance().getSkill(skillId, maxLevel);
			if ((routeSkill != null) && getRouteLabel(baseSkill, routeSkill, route).equals("Chance"))
			{
				chanceLevel = maxLevel;
				break;
			}
		}

		if ((chanceLevel == 0) || (player.getSkillLevel(skillId) == chanceLevel))
		{
			return false;
		}

		final Skill targetSkill = SkillData.getInstance().getSkill(skillId, chanceLevel);
		if (targetSkill == null)
		{
			return false;
		}
		player.addSkill(targetSkill, true);
		return true;
	}

	/**
	 * Applies the class opening curse to the bot's current enemy: mage bots open
	 * every fight with the resist-lowering Surrender of their element (Archmage:
	 * Surrender to Fire, Mystic Muse: Surrender to Water, Storm Screamer:
	 * Surrender to Wind), the necromancer (Soultaker) with Curse Gloom (M.Def
	 * down). The driver (BotManager.combatTick) calls this once per enemy, right
	 * when the bot acquires a valid attackable target.
	 * @param player the bot
	 * @param preset the bot's class preset
	 * @param target the current enemy
	 * @return true when the curse was applied (in range)
	 */
	public static boolean applyOpeningCurse(Player player, BotClassPreset preset, Creature target)
	{
		if ((player == null) || (preset == null) || (target == null) || target.isAlikeDead())
		{
			return false;
		}
		final int skillId = getOpeningCurseSkillId(preset);
		if (skillId == 0)
		{
			return false;
		}

		// The opener is part of the class combat set: when the admin forbids the skill
		// on the //bot class page (it leaves the auto rotation), the bot stops opening
		// with it as well. By default all four opener skills are part of the rotation.
		if (!player.getAutoUseSettings().getAutoSkills().contains(skillId))
		{
			return false;
		}

		// The bot's known template - already at the maximum Chance route
		// (ensureOpeningCurseEnchant), i.e. the highest trigger rate. Fallback:
		// the maximum base level template.
		Skill skill = player.getKnownSkill(skillId);
		if (skill == null)
		{
			skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
		}
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: opening curse skill " + skillId + " not found in SkillData.");
			return false;
		}

		// Cast-range gate: the opener fires like a real cast, only when the enemy
		// is within the skill range (small tolerance for the movement lag).
		if (player.calculateDistance2D(target) > (skill.getCastRange() + 100))
		{
			return false;
		}

		// Reuse gate: the opener must never become a free spammable debuff. When the
		// target keeps being re-acquired (Shadow Step target cancels, LOS clears), the
		// old code re-applied the curse instantly every 1-2 seconds. The timestamp set
		// below (right after the application) blocks re-use for the skill's normal
		// reuse delay - exactly like a real cast would; the rotation's own curse casts
		// go through useMagic and respect the same timestamp.
		if (player.isSkillDisabled(skill))
		{
			return false;
		}

		// Instant application (no cast time), the same style as the olympiad buff
		// set - but the landing goes through the full core success formula
		// (Formulas.calcEffectSuccess: debuff immunity, basicProperty resist,
		// trait/element mods), and the +30 Chance enchant maximizes the rate.
		// Instant effect: never announce a timed cast that the server is not executing.
		skill.applyEffects(player, target);
		// Start the skill's normal reuse delay (see the reuse gate above).
		if (skill.getReuseDelay() > 0)
		{
			player.addTimeStamp(skill, skill.getReuseDelay());
		}
		LOGGER.info("FakePlayersAI: open-curse " + player.getName() + ": " + skill.getName() + " (level " + skill.getLevel() + ") on " + target.getName() + ".");
		return true;
	}

	// ------------------------------------------------------------------
	// Class nickname (the bot name displays its class)
	// ------------------------------------------------------------------

	/**
	 * @return the class-based bot nickname: the class name without spaces
	 *         (Duelist, PhoenixKnight, GrandKhavatari, ...).
	 */
	public static String classNickname(BotClassPreset preset)
	{
		return preset.getClassName().replace(" ", "");
	}

	/**
	 * @return the first free class-based nickname: the class name itself, then
	 *         numeric suffixes ("Duelist2".."Duelist99") when the name is taken.
	 */
	public static String generateClassNickname(BotClassPreset preset)
	{
		final String base = classNickname(preset);
		if (!CharInfoTable.getInstance().doesCharNameExist(base))
		{
			return base;
		}
		for (int i = 2; i < 100; i++)
		{
			final String candidate = base + i;
			if (!CharInfoTable.getInstance().doesCharNameExist(candidate))
			{
				return candidate;
			}
		}
		return base + Rnd.get(100, 999);
	}

	/**
	 * Renames the bot so its nickname displays its class ("Kain" -> "Duelist";
	 * "Duelist2" when the class name is occupied by another character). Called
	 * from registerBot() BEFORE world entry - the database row (storeMe) and the
	 * CharInfo broadcast of the spawn carry the final name. Idempotent and safe
	 * on every spawn: a bot already named after its class is not touched.
	 * @param player the bot
	 * @param preset the bot's class preset
	 * @return true when the name was actually changed (and stored)
	 */
	public static boolean ensureClassNickname(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null))
		{
			return false;
		}
		final String desired = classNickname(preset);
		final String current = player.getName();
		if (desired.equals(current))
		{
			return false;
		}

		final String name = generateClassNickname(preset);
		if (name.equals(current))
		{
			return false; // already on a (suffixed) class-based name that stays valid
		}

		player.setName(name);
		player.storeMe();
		LOGGER.info("FakePlayersAI: rename " + current + " -> " + name + " (" + preset.getClassName() + ").");
		return true;
	}

	/**
	 * Creates a brand new bot character (persisted in database) with third profession, level 85, nobless and gear.
	 * @param preset the class preset
	 * @param name unique character name
	 * @return the bot, or null on failure
	 */
	public static Player createBot(BotClassPreset preset, String name)
	{
		final int classId = preset.getClassId();
		final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(classId);
		if (template == null)
		{
			LOGGER.warning("FakePlayersAI: no player template for classId " + classId);
			return null;
		}
		
		final BotSettings settings = BotManager.getSettings();
		final String accountName = settings.getAccountPrefix() + Rnd.get(100000, 999999);
		
		final PlayerAppearance appearance = new PlayerAppearance((byte) Rnd.get(0, 2), (byte) Rnd.get(0, 4), (byte) Rnd.get(0, 2), preset.isFemale());
		
		final Player player = Player.create(template, accountName, name, appearance);
		if (player == null)
		{
			LOGGER.warning("FakePlayersAI: Player.create failed for name " + name + " (class " + classId + ")");
			return null;
		}
		
		// Spawn location (stored right away so even an interrupted setup leaves a sane row).
		player.setXYZInvisible(settings.getSpawnX(), settings.getSpawnY(), settings.getSpawnZ());
		player.setTitle(settings.getBotTitle());
		
		// Level 85 through exp (same way AdminLevel does it).
		final long expFor85 = ExperienceData.getInstance().getExpForLevel(85);
		if (player.getExp() < expFor85)
		{
			player.addExpAndSp(expFor85 - player.getExp(), 0);
		}
		
		// Learn every skill available for the class at this level. The core
		// giveAvailableSkills() walks the tree until convergence (multi-level chains
		// included: each learned level unlocks the next row) and STORES the result,
		// so actives land at their maximum level and survive reloads. A single pass
		// of getAvailableSkills() is not enough for that: it only returns level-1
		// rows of yet unknown skills, which is how bots ended up with a handful of
		// auto-learned passives and no active skills at all (empty auto rotation).
		// includeByFs = true: Forgotten Scroll skills (getLevel 81-83) are learned
		// as well - otherwise the //bot skill list shows skills the bot can never use.
		player.giveAvailableSkills(true, true, true);
		
		// Third profession bots are nobles from the moment of creation.
		player.setNoble(true);
		
		// Equipment: S84/S80 set + weapon + jewelry + consumables.
		equipGear(player, preset);
		giveConsumables(player, preset);
		
		player.storeMe();
		return player;
	}

	/**
	 * (Re)teaches a bot its complete class skill tree at the current level.
	 * The core giveAvailableSkills() loops until no more rows become learnable and
	 * stores new skills in the database, so it fills freshly created bots and also
	 * heals bots restored from the database without their class actives (those had
	 * only the auto-learned passives, which left the auto skill rotation empty:
	 * AutoUseTaskManager drops unknown skills from it on the first ticks).
	 * Idempotent: already known skills on their maximum level are not touched.
	 * @param player the bot
	 */
	public static void applyClassSkills(Player player)
	{
		if (player == null)
		{
			return;
		}

		final int learned = player.giveAvailableSkills(true, true, true);
		if (learned > 0)
		{
			LOGGER.info("FakePlayersAI: class-skills " + player.getName() + ": learned " + learned + " new skills (complete class tree).");
		}
	}

	/**
	 * Grants the universal default passive Subclass Counter Celestial (Enchanter Ability -
	 * Barrier, skill 655) to the bot: runtime-only (addSkill without a DB store), applied
	 * immediately as a passive (its TriggerSkillByDamageReceived effect registers the
	 * invincibility trigger 5576). Idempotent: a bot that already knows the skill (e.g.
	 * through the core restoreSkills after Player.load) is not touched.
	 * @param player the bot
	 */
	public static void applyCounterCelestial(Player player)
	{
		if (player == null)
		{
			return;
		}

		if (player.getKnownSkill(COUNTER_CELESTIAL_ID) != null)
		{
			return;
		}

		final Skill skill = SkillData.getInstance().getSkill(COUNTER_CELESTIAL_ID, 1);
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: Counter Celestial skill " + COUNTER_CELESTIAL_ID + " not found in SkillData.");
			return;
		}

		player.addSkill(skill, false);
		LOGGER.info("FakePlayersAI: universal-passive " + player.getName() + ": Counter Celestial (skill " + COUNTER_CELESTIAL_ID + ") granted.");
	}

	/**
	 * Applies the FakePlayersAI skill enchant policy to a bot. For every known skill that has enchant
	 * routes (EnchantSkillGroupsData) the target route is chosen with the following priority:
	 * <ol>
	 * <li><b>Damage-dealing skills</b> (Skill.isDamage) are enchanted to the maximum <b>Duel</b> route
	 * (PvP power: pvpPower overrides or route-added pvpEffects scopes) when the skill offers one,
	 * otherwise to the maximum <b>Power</b> route; an effect-only power route (no Skill.getPower
	 * change, e.g. 2nd class attack skills) also counts as Power. Chance stays as the last-resort
	 * fallback for damage skills.</li>
	 * <li><b>Skills that restrict the bot's own movement</b> (ImmobileBuff self-buffs or runSpd mul
	 * penalties applied to the caster: Ultimate Defense, Snipe, Vengeance, Sword Shield, Silent Move,
	 * ...) are enchanted to the maximum level of the route that <b>reduces that movement penalty</b>
	 * (the "Decrease Penalty" enchants) when such a route exists. Enemy debuffs that slow their target
	 * are not affected (their penalty is not applied to the caster).</li>
	 * <li><b>Everything else</b>: maximum Power route, then Chance (previous behavior); skills that
	 * only offer Time/Cost/etc. (typical buffs) stay unenchanted.</li>
	 * </ol>
	 * The maximum enchant level comes from EnchantSkillGroups.xml itself: +30 for 2nd class group
	 * skills (groups 1/2), +15 for 3rd class group skills (groups 5/6). A skill below its maximum base
	 * level is brought up (replaced) in the same step, since enchanting requires the maximum base level
	 * first. Safe to call repeatedly: skills already enchanted to the target level are untouched.
	 * @param player the bot
	 * @return number of skills actually (re)enchanted by this call
	 */
	public static int applySkillEnchants(Player player)
	{
		if (player == null)
		{
			return 0;
		}

		// Snapshot first: addSkill() below replaces entries of the live skill map.
		final List<Skill> knownSkills = new ArrayList<>();
		for (Skill skill : player.getAllSkills())
		{
			if (skill != null)
			{
				knownSkills.add(skill);
			}
		}

		int enchanted = 0;
		int skipped = 0;
		int duelCount = 0;
		int penaltyCount = 0;
		for (Skill skill : knownSkills)
		{
			final int skillId = skill.getId();
			final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
			if (learn == null)
			{
				continue;
			}

			// Base skill at its maximum base level - the prerequisite of any enchant.
			final Skill baseSkill = SkillData.getInstance().getSkill(skillId, learn.getBaseLevel());
			if (baseSkill == null)
			{
				continue;
			}

			final boolean damage = baseSkill.isDamage();
			// Movement restriction the skill applies to its own caster (0.0 = none, 1.0 = immobile).
			final double basePenalty = getMovePenalty(baseSkill);

			// Evaluate every route at its maximum enchant level: route kind (Power/Chance/Duel label,
			// PvP-only effect scopes added by the route) and how much movement penalty it removes.
			// Routes are visited in ascending order so the picked route is deterministic.
			int duelLevel = 0;
			int powerLevel = 0;
			int chanceLevel = 0;
			int penaltyLevel = 0;
			double penaltyLeft = basePenalty;
			final List<Integer> routes = new ArrayList<>(learn.getAllRoutes());
			Collections.sort(routes);
			for (int route : routes)
			{
				final int maxLevel = getRouteMaxLevel(learn, route);
				if (maxLevel == 0)
				{
					continue;
				}
				final Skill routeSkill = SkillData.getInstance().getSkill(skillId, maxLevel);
				if (routeSkill == null)
				{
					continue;
				}

				// Reduces the caster's movement penalty ("Decrease Penalty" enchants)? Checked on every
				// route regardless of its label (a penalty route may also lower MP cost, etc.).
				if (basePenalty > 0.0)
				{
					final double routePenalty = getMovePenalty(routeSkill);
					if ((routePenalty < basePenalty) && (routePenalty < penaltyLeft))
					{
						penaltyLeft = routePenalty;
						penaltyLevel = maxLevel;
					}
				}

				final String label = getRouteLabel(baseSkill, routeSkill, route);
				if (label.equals("Duel") || (!baseSkill.hasEffects(EffectScope.PVP) && routeSkill.hasEffects(EffectScope.PVP)))
				{
					// Duel route: PvP power override or PvP-only effects added by the route.
					if (duelLevel == 0)
					{
						duelLevel = maxLevel;
					}
				}
				else if (label.equals("Power") && (powerLevel == 0))
				{
					powerLevel = maxLevel;
				}
				else if (label.equals("Chance") && (chanceLevel == 0))
				{
					chanceLevel = maxLevel;
				}
				else if (damage && (powerLevel == 0) && label.startsWith("Enchant "))
				{
					// Damage skill with an effect-only power route (no Skill.getPower change).
					powerLevel = maxLevel;
				}
			}

			// Choose the target level by the policy priority.
			final int targetLevel;
			if (damage)
			{
				// Damage skills: Duel (PvP power) when possible, otherwise Power (Chance as last resort).
				targetLevel = (duelLevel > 0) ? duelLevel : ((powerLevel > 0) ? powerLevel : chanceLevel);
			}
			else if (basePenalty > 0.0)
			{
				// Self-restricting skills: the route that lessens the movement penalty, if any.
				targetLevel = penaltyLevel;
			}
			else
			{
				// Everything else: Power, then Chance; Time/Cost-only skills stay unenchanted.
				targetLevel = (powerLevel > 0) ? powerLevel : chanceLevel;
			}

			if (targetLevel == 0)
			{
				// No route matching the policy - stays unenchanted.
				skipped++;
				continue;
			}

			// Already enchanted exactly to the target level of the chosen route?
			if (player.getSkillLevel(skillId) == targetLevel)
			{
				continue;
			}

			final Skill targetSkill = SkillData.getInstance().getSkill(skillId, targetLevel);
			if (targetSkill == null)
			{
				continue;
			}

			// Replaces the current (base or otherwise enchanted) level in one step and stores it in DB.
			player.addSkill(targetSkill, true);
			enchanted++;
			if (damage && (targetLevel == duelLevel))
			{
				duelCount++;
			}
			else if (!damage && (targetLevel == penaltyLevel))
			{
				penaltyCount++;
			}
		}

		LOGGER.info("FakePlayersAI: skill-ench " + player.getName() + ": enchanted " + enchanted + " skills (" + duelCount
			+ " damage -> Duel, " + penaltyCount + " self-restricting -> Decrease Penalty, rest -> Power/Chance), "
			+ skipped + " left unenchanted (no matching route).");
		return enchanted;
	}

	/**
	 * Finds the maximum full skill level of an enchant route, e.g. route 1 of a
	 * 2nd class skill -> 130 (+30), route 1 of a 3rd class skill -> 115 (+15).
	 * The route length is taken from EnchantSkillGroups.xml (size of the group).
	 */
	private static int getRouteMaxLevel(EnchantSkillLearn learn, int route)
	{
		for (int i = 1; i <= MAX_SKILL_ENCHANT_LEVEL; i++)
		{
			if (learn.isMaxEnchant((route * 100) + i))
			{
				return (route * 100) + i;
			}
		}

		return 0;
	}

	/**
	 * Derives the route (enchant parameter) name by comparing the base skill with the
	 * final enchanted level of the route: route names are client-side data, so the
	 * server approximates them from the changed stats (same heuristic as SkillEnchanter).
	 */
	private static String getRouteLabel(Skill baseSkill, Skill enchantedSkill, int route)
	{
		if ((enchantedSkill.getMpConsume() < baseSkill.getMpConsume()) || (enchantedSkill.getMpInitialConsume() < baseSkill.getMpInitialConsume()))
		{
			return "Cost";
		}

		if ((enchantedSkill.getElementPower() != baseSkill.getElementPower()) || (enchantedSkill.getElement() != baseSkill.getElement()))
		{
			return "Element";
		}

		if (enchantedSkill.getAbnormalType() != baseSkill.getAbnormalType())
		{
			return "Debuff";
		}

		if (enchantedSkill.getActivateRate() != baseSkill.getActivateRate())
		{
			return "Chance";
		}

		if (enchantedSkill.getAbnormalTime() != baseSkill.getAbnormalTime())
		{
			return "Time";
		}

		if (enchantedSkill.getPower() != baseSkill.getPower())
		{
			return "Power";
		}

		if (enchantedSkill.getPower(true, false) != baseSkill.getPower(true, false))
		{
			return "Duel";
		}

		return "Enchant " + route;
	}

	/**
	 * Measures how strongly the skill's effects restrict the movement of the character they are applied
	 * to: 0.0 = no restriction, (0.0, 1.0) = fraction of run speed lost (a runSpd mul of 0.6 counts as
	 * 0.4), 1.0 = fully immobile (ImmobileBuff). Only caster-applied effects count: the SELF scope
	 * (selfEffects) always, the GENERAL scope (effects) for self-targeted skills only - so enemy
	 * debuffs with runSpd penalties (Slow, Hamstring, ...) are not treated as self-restricting.
	 */
	private static double getMovePenalty(Skill skill)
	{
		final double general = scanMovePenalty(skill, EffectScope.GENERAL, skill.getTargetType() == TargetType.SELF);
		final double self = scanMovePenalty(skill, EffectScope.SELF, true);
		return Math.max(general, self);
	}

	/**
	 * Movement restriction carried by one effect scope of the skill. Immobile* effects count as a full
	 * restriction; MOVE_SPEED stat functions are interpreted by their operation (mul below 1.0 lowers
	 * run speed, sub/add lower it by an absolute value that is roughly normalized to a fraction).
	 */
	private static double scanMovePenalty(Skill skill, EffectScope scope, boolean appliesToCaster)
	{
		if (!appliesToCaster || !skill.hasEffects(scope))
		{
			return 0.0;
		}
		double penalty = 0.0;
		for (AbstractEffect effect : skill.getEffects(scope))
		{
			if (effect == null)
			{
				continue;
			}
			final String name = effect.getName();
			if ((name != null) && name.startsWith("Immobile"))
			{
				penalty = Math.max(penalty, 1.0); // ImmobileBuff / ImmobilePetBuff: no movement at all
				continue;
			}
			final List<FuncTemplate> funcs = effect.getFuncTemplates();
			if (funcs == null)
			{
				continue;
			}
			for (FuncTemplate template : funcs)
			{
				if ((template == null) || (template.getStat() != Stat.MOVE_SPEED))
				{
					continue;
				}
				final double value = template.getValue();
				final int order = template.getOrder();
				if ((order == FUNC_ORDER_MUL) && (value < 1.0))
				{
					penalty = Math.max(penalty, 1.0 - value);
				}
				else if ((order == FUNC_ORDER_SUB) && (value > 0.0))
				{
					penalty = Math.max(penalty, value / 100.0);
				}
				else if ((order == FUNC_ORDER_ADD) && (value < 0.0))
				{
					penalty = Math.max(penalty, -value / 100.0);
				}
			}
		}
		return penalty;
	}

	/**
	 * Places a bot into the world (same proven sequence used by OfflinePlayTable.restoreOfflinePlayers).
	 */
	public static boolean enterWorld(Player player, int x, int y, int z)
	{
		if (player == null)
		{
			return false;
		}
		final int sx = x;
		final int sy = y;
		final int sz = GeoEngine.getInstance().getSpawnHeight(x, y, z);
		
		player.setOnlineStatus(true, false);
		player.spawnMe(sx, sy, sz);
		player.setOnlineStatus(true, true);
		player.restoreEffects();
		player.setRunning();
		player.updateAndBroadcastStatus();
		return true;
	}
	
	/**
	 * Enables the built-in auto hunt engine (targeting, movement, melee attack) and the auto skill rotation.
	 */
	public static void applyAutoCombat(Player player, BotClassPreset preset)
	{
        if (player.isInOlympiadMode() && !OlympiadCombatGate.ready(player))
        {
            OlympiadCombatGate.hold(player);
            return;
        }
		final BotSettings settings = BotManager.getSettings();
		
		// Bots are clientless players, so Player.isInOfflineMode() (client == null) is always
		// true for them. Both auto engines (AutoPlayTaskManager and AutoUseTaskManager) treat
		// such players as idle offline traders and REMOVE them from their task pools on the
		// first tick, unless the player is flagged for offline play. Without this flag bots
		// never use a single skill anywhere: AutoUseTaskManager is the component that runs
		// the auto skill rotation, self buffs and auto potions (world PvP and olympiad alike),
		// and AutoPlayTaskManager owns the isAutoPlaying() state that gates the rotation.
		// The offline play flag is exactly what the .offlineplay feature sets for real
		// players who keep playing without a client; for bots it has no side effects
		// (no death logout: DisconnectAfterDeath = False, no account relogin: bots never
		// log in, no offline play table rows: they are never stored there).
		player.setOfflinePlay(true);

		// Bots must never cast buffs or healing skills on anyone but themselves: the core
		// auto-use engines read this flag from the auto use settings (AutoUseTaskManager).
		player.getAutoUseSettings().setSelfCastOnly(true);
		
		// AutoPlay: target mode 2 = playable targets only. Bots only ever fight players
		// (world PvP and olympiad matches); this keeps the auto play engine from locking
		// onto nearby NPCs (e.g. the olympiad manager) when it re-targets on its own.
		final AutoPlaySettingsHolder aps = player.getAutoPlaySettings();
		aps.setNextTargetMode(2);
		aps.setPickup(false);
		aps.setShortRange(false);
		aps.setRespectfulHunting(false);
		aps.setAutoPotionPercent(settings.getAutoPotionPercent());
		
		// AutoUse: class skill rotation + self buffs + potions.
		final AutoUseSettingsHolder aus = player.getAutoUseSettings();
		aus.getAutoSkills().clear();
		// Mage bots (Dominator counts as a mage): the rotation cycle is spread 80% main attack
		// skills / 20% curses (buildMageRotation). Fighters use the flat rotation order.
		if (preset.isCaster())
		{
			for (int skillId : buildMageRotation(preset.getRotation()))
			{
				aus.getAutoSkills().add(skillId);
			}
		}
		else
		{
			for (int skillId : preset.getRotation())
			{
				aus.getAutoSkills().add(skillId);
			}
		}
		aus.getAutoBuffs().clear();
		for (int skillId : preset.getBuffs())
		{
			aus.getAutoBuffs().add(skillId);
		}
		// Hold back personal buffs that are already active on the bot: the core
		// auto-buff engine would recast them (its canCastBuff refresh window
		// includes buffs with 3 or fewer seconds left). refreshAutoBuffs prunes
		// them right away; the next combat tick puts them back once they end.
		refreshAutoBuffs(player, preset);
		aus.setAutoPotionItem(BotPresets.getPotionId());
		aus.getAutoSupplyItems().clear();
		
		// Auto attack action: without it the auto play engine treats the player as a caster
		// (keeps 900 distance, never sets attack intention). Fighters/archers must auto-attack.
		if (!preset.isCaster() && (!player.isInOlympiadMode() || BotTactics.allowsBasicAttack(player, preset)))
		{
			aus.getAutoActions().add(AUTO_ATTACK_ACTION);
		}
		else
		{
			aus.getAutoActions().remove(AUTO_ATTACK_ACTION);
		}
		
		// Shots: EVERY bot (caster or fighter) always auto-uses the S-grade soulshot AND the
		// S-grade blessed spiritshot. Both stay stocked by maintainConsumables() (combat tick),
		// which also re-enables the auto shots after the core disables them on olympiad entry.
		player.addAutoSoulShot(BotPresets.getFighterShotId());
		player.addAutoSoulShot(BLESSED_SPIRITSHOT_S);
		
		if (player.isInOlympiadMode()) trimOlympiadBannedSkillsFromAutoLists(player);
        // Do not briefly start legacy engines before the first tactical tick claims them.
        if (player.isInOlympiadMode() && TacticalSettings.isEnabled() && preset.getClassId() != 131)
        {
            AutoPlayTaskManager.getInstance().stopAutoPlay(player);
            AutoUseTaskManager.getInstance().stopAutoUseTask(player);
            return;
        }
        // Legacy pursuit leaves items available; tactical ownership stops BOTH engines.
		if (!BotTactics.isOwned(player.getObjectId()))
		{
			OlympiadChase.startAutoPlayUnlessPursuing(player);
			AutoUseTaskManager.getInstance().startAutoUseTask(player);
		}
	}
	
	public static void stopAutoCombat(Player player)
	{
		// Reset the offline play marker first, so the stop calls below cannot touch
		// the OfflinePlayTable (bots are never stored there).
		BotTactics.forget(player.getObjectId());
		OlympiadChase.forget(player.getObjectId());
		player.setOfflinePlay(false);
		player.getAutoUseSettings().setSelfCastOnly(false);
		AutoPlayTaskManager.getInstance().stopAutoPlay(player);
		AutoUseTaskManager.getInstance().stopAutoUseTask(player);
	}

	/**
	 * Keeps the bot's auto-buff list (AutoUseSettings.autoBuffs) in sync with the
	 * "already applied" state of its personal (self-targeted) buffs:
	 * <ul>
	 * <li>a self-buff that is currently active on the bot is removed from the list,
	 * so the core AutoUseTaskManager cannot cast it again - including the core's own
	 * refresh window (it recasts buffs with 3 or fewer seconds left);</li>
	 * <li>a buff whose effect has ended is put back on the list, so the core
	 * auto-buff engine casts it again as usual;</li>
	 * <li>charge builder self-buffs (FocusEnergy, FocusMaxEnergy, ...) are held back
	 * while the charges they build are full - the cast would be a no-op. Soul builders
	 * (FocusSouls, e.g. Doombringer's Life to Soul / Soul Gathering) leave no charges
	 * behind and are gated by their own skill conditions in the datapack ("souls=40"),
	 * so they are never held back here.</li>
	 * </ul>
	 * The preset (and the //bot overrides) remain the source of truth; this only
	 * filters what is currently live in the auto-buff list. Instant self-skills
	 * without a lasting effect (heals, Braveheart, etc.) are never held back:
	 * there is nothing "applied" to check - their reuse delays gate them.
	 * Idempotent and cheap; called from applyAutoCombat() and every combat tick.
	 * @param player the bot
	 * @param preset the bot's class preset (source of the desired buff set)
	 */
	public static void refreshAutoBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline())
		{
			return;
		}

		final Collection<Integer> autoBuffs = player.getAutoUseSettings().getAutoBuffs();
		if (OlympiadChase.isPursuing(player.getObjectId()) || BotTactics.isOwned(player.getObjectId()))
		{
			autoBuffs.clear(); // Do not create a new stationary cast during an intercept.
			return;
		}
		final int[] desired = preset.getBuffs();

		// Olympiad gates: the dagger evasion openers are opener-burst-only (never cast by the
		// auto-buff engine inside an olympiad match), the Chant of Victory family (CoV and
		// its analogs) is banned inside olympiad matches entirely, and once the gates are
		// open no self-buff longer than 2 minutes may be (re)cast at all - all per admin rules.
		final boolean inOlympiad = player.isInOlympiadMode();
		final boolean activeMatch = inOlympiad && player.isOlympiadStart();

		// Admin rule (gate opening): BEFORE the stadium gates open the bot does not buff
		// itself at all. The whole buff treatment (olympiad set + profile + pre-start
		// skills) is applied instantly by the pre-start hook 2 seconds before the battle;
		// the bot's own self-buff casting starts only when the fight has started (and is
		// limited to self-buffs up to 2 minutes there - see the activeMatch gates below).
		// The list is cleared here, so the core auto-buff engine has nothing to cast
		// during the countdown; it is re-filled by the normal sync as soon as the fight
		// starts (or when the bot is back in the world).
		if (inOlympiad && !activeMatch)
		{
			autoBuffs.clear();
			return;
		}

		// Tank bots (admin rules of the defense pool): the defensive amplifiers
		// (Ultimate Defense, Vengeance, Anti Magic Armor, the mirrors/deflectors, ...)
		// are managed by refreshTankDefenses (one at a time, by the opponent's class) -
		// the generic sync below must never put them back into the list on its own.
		final boolean tankBot = BotManager.isTankBot(player);

		// Drop currently applied self-buffs (no recast while the effect is on the bot),
		// stale entries that are no longer part of the class buff set, and entries the
		// olympiad rules forbid right now.
		autoBuffs.removeIf(skillId -> !containsId(desired, skillId)
			|| isSelfBuffApplied(player, skillId)
			|| (inOlympiad && isOpenerOnlySkill(skillId))
			|| (inOlympiad && isChantOfVictoryFamily(skillId))
			|| (activeMatch && isLongSelfBuff(player, skillId))
			|| (tankBot && isTankDefenseSkill(skillId)));

		// Restore buffs that ended (or were not applied yet) so the core casts them again.
		// Only skills the bot actually knows are listed: unknown ones would be pruned
		// by the core on its next tick (added back here, pruned again - endless churn).
		for (int skillId : desired)
		{
			// The olympiad ban of the Chant of Victory family wins over the preset (and
			// any //bot override): the family is never re-added to the auto-buff list
			// while the bot is inside an olympiad match.
			if (inOlympiad && isChantOfVictoryFamily(skillId))
			{
				continue;
			}
			// Tank defense pool entries are added exclusively by refreshTankDefenses.
			if (tankBot && isTankDefenseSkill(skillId))
			{
				continue;
			}
			if (!autoBuffs.contains(skillId) && !isSelfBuffApplied(player, skillId) && (player.getKnownSkill(skillId) != null)
				&& !(inOlympiad && isOpenerOnlySkill(skillId))
				&& !(activeMatch && isLongSelfBuff(player, skillId)))
			{
				autoBuffs.add(skillId);
			}
		}
	}

	/**
	 * Tank bots (Phoenix Knight 90 / Hell Knight 91 / Shillien Templar 106): defensive
	 * self-buff management (admin rules).
	 * <ul>
	 * <li><b>One defense buff at a time</b>: every skill of the defense pool
	 * ({@link #TANK_DEFENSE_ALL}) is mutually exclusive - while one of them is active,
	 * none of the others is cast. The permanent stance toggles (Shield Fortress,
	 * Fortitude, ...) are NOT part of the pool.</li>
	 * <li><b>By the opponent's class</b>: a mage opponent gets the magic-defense group
	 * ({@link #TANK_DEFENSE_MAGIC}: Anti Magic Armor, Deflect Magic, Magical Mirror,
	 * Iron Will, Shield Deflect Magic), a warrior opponent the physical-defense group
	 * ({@link #TANK_DEFENSE_PHYSICAL}: Ultimate Defense, Vengeance, Physical Mirror,
	 * Shield of Revenge, Iron Shield, Deflect Arrow, Majesty, Reflect Damage). When the
	 * active buff belongs to the WRONG group, it is removed and the right group's
	 * highest-priority skill is queued instead. Priority = the array order: the big
	 * panic buttons first, the long situational amplifiers after them.</li>
	 * <li><b>Vengeance (368) is pool-managed</b>: it sits in the tank rotation sets
	 * (//bot overrides), so the live skill rotation is kept free of it here - otherwise
	 * the rotation engine could put a second defense buff up while one is active.</li>
	 * <li><b>Olympiad gates</b>: before the gates open nothing is cast (the pool is just
	 * kept out of the auto-buff list, existing effects are not cancelled); inside an
	 * active match the general ">2 minutes" self-buff ban holds, so only the short pool
	 * skills (Ultimate Defense / Vengeance / Anti Magic Armor / Shield Deflect Magic)
	 * are eligible there. Outside olympiad the full pool works.</li>
	 * <li><b>No opponent</b> (no target): the pool is kept out of the auto-buff list,
	 * nothing is cast and nothing is cancelled - there is nothing to defend against.</li>
	 * </ul>
	 * The casting itself is done by the core auto-buff engine (between auto-attacks);
	 * this method only keeps exactly one correct candidate in the list. Idempotent and
	 * cheap; called from every combat tick (2 s) for tank bots.
	 * @param player the tank bot
	 * @param preset the bot's class preset (the //bot buff set is respected: a pool
	 *        skill removed from the class buff set is not cast; Vengeance is always
	 *        pool-eligible)
	 * @param opponent the current opponent (the olympiad match opponent or the current
	 *        world target; may be null)
	 */
	public static void refreshTankDefenses(Player player, BotClassPreset preset, WorldObject opponent)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead() || !BotManager.isTankBot(player))
		{
			return;
		}

		// Vengeance belongs to the defense pool from now on: the rotation engine must
		// never cast it (it would stack a second defense buff over the active one).
		player.getAutoUseSettings().getAutoSkills().removeIf(id -> id == VENGEANCE_ID);

		final Collection<Integer> autoBuffs = player.getAutoUseSettings().getAutoBuffs();

		if (OlympiadChase.isPursuing(player.getObjectId()) || BotTactics.isOwned(player.getObjectId()))
		{
			autoBuffs.clear();
			return;
		}

		// Before the gates open: no defensive buffing (the general countdown rule).
		// Already active effects are not cancelled - the rule forbids casting, not holding.
		if (player.isInOlympiadMode() && !player.isOlympiadStart())
		{
			autoBuffs.removeIf(BotSpawner::isTankDefenseSkill);
			return;
		}

		// Desired defense group by the opponent's class (default: physical - the common
		// case; an unknown/non-player opponent is treated as a warrior too).
		final boolean opponentMage = (opponent != null) && opponent.isCreature() && opponent.asCreature().isPlayer()
			&& opponent.asCreature().asPlayer().isMageClass();
		final int[] desiredPool = opponentMage ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
		final boolean activeMatch = player.isInOlympiadMode() && player.isOlympiadStart();

		// Keep at most one defense buff up - and only of the desired group: the first
		// active buff of the right group stays, every other pool effect is removed.
		int keep = 0;
		for (int skillId : desiredPool)
		{
			if ((keep == 0) && player.getEffectList().isAffectedBySkill(skillId))
			{
				keep = skillId;
			}
		}
		for (int skillId : TANK_DEFENSE_ALL)
		{
			if ((skillId != keep) && player.getEffectList().isAffectedBySkill(skillId))
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skillId);
			}
		}
		if (keep != 0)
		{
			// One (correct-group) defense buff is already up: nothing else may be cast.
			autoBuffs.removeIf(BotSpawner::isTankDefenseSkill);
			return;
		}

		// Nothing is up: pick the highest-priority castable skill of the desired group.
		int candidate = 0;
		for (int skillId : desiredPool)
		{
			// Respect the //bot buff set (Vengeance is always pool-eligible - it is
			// managed here regardless of which //bot list it came from).
			if ((skillId != VENGEANCE_ID) && !containsId(preset.getBuffs(), skillId))
			{
				continue;
			}
			final Skill skill = player.getKnownSkill(skillId);
			if (skill == null)
			{
				continue;
			}
			if (activeMatch && isLongSelfBuff(player, skillId))
			{
				continue; // the olympiad ">2 minutes" self-buff ban
			}
			if (player.isSkillDisabled(skill))
			{
				continue; // still on reuse - try the next priority on a later tick
			}
			candidate = skillId;
			break;
		}

		autoBuffs.removeIf(BotSpawner::isTankDefenseSkill);
		if ((candidate > 0) && !isSelfBuffApplied(player, candidate))
		{
			autoBuffs.add(candidate);
		}
	}

	/**
	 * @return true when the skill id is a dagger evasion opener that must only be applied
	 *         by the opening self-enhancement burst inside an olympiad match (see
	 *         OPENER_ONLY_SKILLS).
	 */
	public static boolean isOpenerOnlySkill(int skillId)
	{
		for (int id : OPENER_ONLY_SKILLS)
		{
			if (id == skillId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true when the bot's (possibly enchanted) skill lasts longer than the olympiad
	 *         self-buff limit (2 minutes) - such a buff may not be (re)cast once the gates
	 *         are open. Toggles have no duration (abnormalTime 0) and are never "long".
	 */
	private static boolean isLongSelfBuff(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		return (skill != null) && (skill.getAbnormalTime() > OLYMPIAD_MAX_SELFBUFF_SECONDS);
	}

	/**
	 * @return true when the self-buff skill is currently "applied" to the bot: a timed
	 * buff or toggle of this skill is present in the bot's effect list, or the skill
	 * is a charge builder and the bot's charges are already at the cap - i.e. using
	 * it again right now would recast a buff the bot already has.
	 */
	private static boolean isSelfBuffApplied(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		if (skill == null)
		{
			// Unknown skills are pruned by the core itself; nothing to hold back.
			return false;
		}

		// Timed buffs and toggles sit in the effect list while they are active.
		if (player.getEffectList().isAffectedBySkill(skillId))
		{
			return true;
		}

		// Charge builders leave no effect behind: treat them as applied while
		// the charges they build are at the admin window ceiling (CHARGE_MAX, 7 -
		// the bots' charge level is kept between 4 and 7; at the ceiling another
		// builder cast would be a no-op up to the retail cap). (Soul builders -
		// FocusSouls - are excluded: they fill the soul slots, not charges, and
		// their own skill conditions in the datapack refuse casts at full souls.)
		return isChargeBuilder(skill) && (player.getCharges() >= CHARGE_WINDOW_MAX);
	}

	/**
	 * @return true when the skill builds charges through a Focus* effect
	 * (FocusEnergy, FocusMaxEnergy, ...). FocusSouls (soul slots, not charges)
	 * does not count - soul builders are gated by their skill conditions.
	 */
	private static boolean isChargeBuilder(Skill skill)
	{
		if (!skill.hasEffects(EffectScope.GENERAL))
		{
			return false;
		}
		for (AbstractEffect effect : skill.getEffects(EffectScope.GENERAL))
		{
			final String name = (effect != null) ? effect.getName() : null;
			if ((name != null) && name.startsWith("Focus") && !name.equals("FocusSouls"))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true when the array contains the given id.
	 */
	private static boolean containsId(int[] ids, int id)
	{
		if (ids == null)
		{
			return false;
		}
		for (int i : ids)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Applies the basic olympiad buff set to a bot (instantly, no cast time):
	 * Wind Walk, Haste, Berserker Spirit, Might, Empower, Focus, Death Whisper,
	 * Acumen, Blessed Body, Wild Magic, Shield, Magic Barrier, Concentration - the
	 * admin mandatory list of 13 (rule of 04.10.2026). Called by BotManager once per
	 * olympiad match, at the moment the battle starts.
	 */
	public static void applyOlympiadBuffs(Player player)
	{
		if (player == null)
		{
			return;
		}

		int applied = 0;
		for (int[] buff : OLYMPIAD_BUFFS)
		{
			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null)
			{
				LOGGER.warning("FakePlayersAI: olympiad buff skill " + buff[0] + "/" + buff[1] + " not found, check OLYMPIAD_BUFFS.");
				continue;
			}
			// Same application style as the core's OlympiadGameTask.applyBuffs().
			skill.applyEffects(player, player);
			applied++;
		}
		LOGGER.info("FakePlayersAI: oly-buffs " + player.getName() + ": applied " + applied + "/" + OLYMPIAD_BUFFS.length + " olympiad buffs.");
	}

	/**
	 * Removes every Chant of Victory family buff (CoV and its analogs - see
	 * CHANT_OF_VICTORY_FAMILY) from the character's effect list. The olympiad ban is
	 * enforced on BOTH match participants (bots and real players alike): the core wipes
	 * all effects when the match is created (OlympiadGame.removals()), but a participant
	 * can still (self-)cast a family buff during the 15-second countdown or mid-fight -
	 * this method takes it away again. Works for any character, inside and outside
	 * olympiad; the caller decides when the ban applies (olympiad only). Idempotent.
	 * @param player the character to clear
	 * @return the number of family effects actually removed
	 */
	public static int stripChantBuffs(Player player)
	{
		if (player == null)
		{
			return 0;
		}

		int removed = 0;
		for (int skillId : CHANT_OF_VICTORY_FAMILY)
		{
			if (player.getEffectList().isAffectedBySkill(skillId))
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skillId);
				removed++;
			}
		}

		if (removed > 0)
		{
			LOGGER.info("FakePlayersAI: oly-chant " + player.getName() + ": removed " + removed + " Chant of Victory family buff(s) (olympiad ban).");
		}
		return removed;
	}

	/** Shared tactical gate: the new decision engine must not bypass admin rules
	 * merely because it reads preset lists rather than the filtered auto lists. */
    /** Effective preset getters already include persisted //bot overrides. */
    public static int[] getTacticalRotation(Player player, Player enemy)
    {
        BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(player));
        return preset == null ? new int[0] : preset.getRotation().clone();
    }
    /** Same priority/mutual-exclusion pool as the legacy defense driver. Do not
     * invent skills or bypass an explicit admin buff-list override. */
    private static boolean isTacticalTank(Player player)
    {
        if (player == null) return false;
        final int id = BotManager.getBotClassId(player);
        return id == 90 || id == 91 || id == 106;
    }

    /** Strong immobile amplifiers: never overlap each other. Long weak buffs
     * (e.g. Majesty/Iron Shield) must not suppress the requested UD->Vengeance chain. */
    public static boolean isStrongTankDefenseSkill(int skillId)
    { return skillId == 110 || skillId == VENGEANCE_ID || skillId == 760; }

    public static int[] getTacticalDefenses(Player player, Player enemy)
    {
        if (!isTacticalTank(player)) return new int[0];
        BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(player));
        if (preset == null) return new int[0];
        int[] pool = enemy != null && enemy.isMageClass() ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int id : pool)
            if (player.getKnownSkill(id) != null && (id == VENGEANCE_ID || (!preset.hasBuffsOverride() && !preset.hasRotationOverride()) || containsId(preset.getBuffs(), id) || containsId(preset.getRotation(), id))) result.add(id);
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

	public static boolean tacticalSkillAllowed(Player player, Player enemy, Skill skill, boolean self)
	{
        if (isOpenerOnlySkill(skill.getId())) return false; // opening enhancements never belong to a live tactical rotation
		if (player.isInOlympiadMode())
		{
			if (!player.isOlympiadStart() || isChantOfVictoryFamily(skill.getId()) || skill.isDance()
				|| (self && isLongSelfBuff(player, skill.getId()))) return false;
		}
		if (isTacticalTank(player) && isTankDefenseSkill(skill.getId()))
		{
			final int[] pool = enemy != null && enemy.isMageClass() ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
			if (!containsId(pool, skill.getId())) return false;
			for (int id : TANK_DEFENSE_ALL)
                if (player.isAffectedBySkill(id) && (!isStrongTankDefenseSkill(skill.getId()) || isStrongTankDefenseSkill(id))) return false;
		}
		return true;
	}

	/**
	 * Keeps the OLYMPIAD-BANNED buffs out of a BOT's live auto lists (skill rotation and
	 * auto buffs) inside an olympiad match, so neither the core auto-use engine nor a
	 * {@code //bot} override can cast them on the arena:
	 * <ul>
	 * <li>every Chant of Victory family buff (CoV and its analogs - see
	 * CHANT_OF_VICTORY_FAMILY);</li>
	 * <li>every dance/song (all isMagic=3 skills - isDance() covers both dances and
	 * songs in this core; admin rule of 04.10.2026: no bot may hold a dance/song on the
	 * arena - the olympiad buff profiles are already dance-free, this guards the live
	 * lists against preset/override entries too).</li>
	 * </ul>
	 * The class preset and the //bot overrides themselves are NOT changed - outside
	 * olympiad (Coliseum world PvP) the full sets work exactly as before. Called from
	 * every combat tick of an olympiad bot; applyAutoCombat() re-fills the lists from the
	 * preset, so the removal is re-asserted on every tick. Idempotent.
	 * @param player the bot inside an olympiad match (countdown included)
	 */

	public static void trimOlympiadBannedSkillsFromAutoLists(Player player)
	{
		if (player == null)
		{
			return;
		}

		final AutoUseSettingsHolder aus = player.getAutoUseSettings();
		aus.getAutoBuffs().removeIf(BotSpawner::isChantOfVictoryFamily);
		aus.getAutoSkills().removeIf(BotSpawner::isChantOfVictoryFamily);
        aus.getAutoBuffs().removeIf(BotSpawner::isOpenerOnlySkill);
        aus.getAutoSkills().removeIf(BotSpawner::isOpenerOnlySkill);
		aus.getAutoBuffs().removeIf(id -> isDanceOrSong(player, id));
		aus.getAutoSkills().removeIf(id -> isDanceOrSong(player, id));
	}

	/**
	 * @return true when the skill id is a dance/song of the bot's known skills (the
	 * olympiad ban of 04.10.2026); unknown skills return false (the auto-use engine
	 * drops unknown ids on its own).
	 */
	private static boolean isDanceOrSong(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		return (skill != null) && skill.isDance();
	}

	/**
	 * Builds the mage bot rotation cycle: the allowed rotation set (preset default or //bot edits)
	 * is spread over a 10-slot cycle - 8 attack entries (80%) and 2 curse entries (20%). Skills are
	 * classified by the explicit curse list (isMageCurse); everything else - the whitelisted main
	 * nukes plus any non-curse skill allowed by the admin - belongs to the attack share. Both
	 * shares cycle through their own skills, so a single attack skill still fills the whole 80%.
	 * If one share is empty, the other fills the whole cycle.
	 * @param rotation the allowed rotation skill ids of the preset
	 * @return the weighted rotation cycle (10 entries, 80% attacks / 20% curses)
	 */
	private static List<Integer> buildMageRotation(int[] rotation)
	{
		final List<Integer> attacks = new ArrayList<>();
		final List<Integer> curses = new ArrayList<>();
		for (int skillId : rotation)
		{
			if (isMageCurse(skillId))
			{
				curses.add(skillId);
			}
			else
			{
				attacks.add(skillId);
			}
		}

		if (curses.isEmpty() || attacks.isEmpty())
		{
			return attacks.isEmpty() ? curses : attacks;
		}

		// Deterministic 8/2 interleave: a slot belongs to the attack share while
		// ((i + 1) * 8 / 10) exceeds (i * 8 / 10), else to the curse share
		// -> C A A A A C A A A A over the cycle.
		final List<Integer> result = new ArrayList<>(10);
		int attackIndex = 0;
		int curseIndex = 0;
		for (int i = 0; i < 10; i++)
		{
			if ((((i + 1) * 8) / 10) > ((i * 8) / 10))
			{
				result.add(attacks.get(attackIndex % attacks.size()));
				attackIndex++;
			}
			else
			{
				result.add(curses.get(curseIndex % curses.size()));
				curseIndex++;
			}
		}
		return result;
	}

    /** Same pre-start Olympiad baseline without setting fake Olympiad flags.
     * TrainingArena owns the explicitly requested entry effect wipe; this helper
     * only supplies the baseline once, never a world profile or mid-fight refresh. */
    public static void applyTrainingOlympiadBuffs(Player player, boolean caster)
    {
        if (player == null || player.isAlikeDead()) return;
        applyOlympiadBuffs(player);
        for (int[] buff : getOlympiadBuffs(caster))
        {
            if (isOpenerOnlySkill(buff[0]) || isTankDefenseSkill(buff[0])) continue;
            if (player.getEffectList().isAffectedBySkill(buff[0])) continue;
            Skill skill = SkillData.getInstance().getSkill(buff[0],buff[1]);
            if (skill == null) { LOGGER.warning("Training Olympiad buff unavailable: " + buff[0] + "/" + buff[1]); continue; }
            BuffInfo stacked = player.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
            if (stacked != null && stacked.getSkill().getId() != buff[0] && stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel()) continue;
            skill.applyEffects(player, player);
        }
    }

    /** Explicit training-only bonus requested for the two Orc fighter presets.
     * Apply each participant's learned/enhanced effects in the owning T-3 opener window.
     * This is a supplied buff, not an illegal full-HP cast: no HP manipulation,
     * condition/XML edits, invented skill levels or repeated combat refresh. */
    public static void applyTrainingClassBuffs(Player bot, BotClassPreset preset)
    {
        if (bot == null || preset == null || !bot.isOnline()
            || bot.isAlikeDead() || !(TrainingArena.isTrainingBot(bot) || TrainingArena.isParticipant(bot))
            || bot.getPlayerClass().getId() != preset.getClassId()) return;
        for (int id : trainingClassBuffIds(preset.getClassId())) applyOpeningSelfBuff(bot, id);
    }

    static int[] trainingClassBuffIds(int classId)
    {
        return switch (classId)
        {
            case 113 -> new int[] {176, 420}; // Titan: Frenzy + Zealot
            case 114 -> new int[] {420}; // Grand Khavatari: Zealot, never Frenzy
            default -> new int[0];
        };
    }

    /**
     * One real self-cast during the owning training preparation phase. Unlike the
     * shared baseline and the short T-3 openers, class selfbuffs use normal cast
     * time, resources, conditions and reuse. No auto engine owns this phase.
     * Repeated pulses skip active effects and never interrupt a cast in progress.
     */
    public static boolean applyTrainingPreparationSelfBuffs(Player bot, BotClassPreset preset)
    {
        if (bot == null || preset == null || bot.getClient() != null || !bot.isOnline()
            || !TrainingArena.isPreparingBot(bot) || bot.isAlikeDead() || bot.isCastingNow()
            || bot.isCastingSimultaneouslyNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) return false;
        for (int id : preset.getBuffs())
        {
            Skill skill = bot.getKnownSkill(id);
            if (!trainingPreparationSkillAllowed(skill) || isSelfBuffApplied(bot, id) || bot.isSkillDisabled(skill)) continue;
            // A stronger/equal effect already occupying the same abnormal slot
            // must not be replaced each pulse (e.g. baseline Blessed Body).
            BuffInfo stacked = skill.getAbnormalType() == AbnormalType.NONE ? null
                : bot.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
            if (stacked != null && stacked.getSkill().getId() != id
                && stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel()) continue;
            if (bot.getCurrentMp() < bot.getStat().getMpInitialConsume(skill) + bot.getStat().getMpConsume(skill)
                || bot.getCurrentHp() <= skill.getHpConsume()
                || !skill.isStatic() && (skill.isMagic() ? bot.isMuted() : bot.isPhysicalMuted())
                || bot.getCharges() < skill.getChargeConsumeCount()
                || skill.getItemConsumeCount() > 0 && bot.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()
                || !skill.checkCondition(bot, bot, false)) continue;
            bot.setTarget(bot);
            if (bot.useMagic(skill, true, true)) return true; // shift=true: never create a CAST approach.
        }
        return false;
    }

    static boolean trainingPreparationSkillAllowed(Skill skill)
    {
        if (skill == null || skill.isPassive() || skill.isBlockedInOlympiad() || skill.isDance()
            || skill.isSuicideAttack() || skill.isDebuff() || skill.hasNegativeEffect() || skill.isDamage()
            || skill.nextActionIsAttack() || isOpenerOnlySkill(skill.getId()) || isTankDefenseSkill(skill.getId())
            || isChantOfVictoryFamily(skill.getId())) return false;
        boolean enhancement = skill.isContinuous() || skill.isToggle() || isChargeBuilder(skill);
        if (!enhancement && skill.hasEffects(EffectScope.GENERAL))
            for (AbstractEffect effect : skill.getEffects(EffectScope.GENERAL))
                if (effect != null && "FocusSouls".equals(effect.getName())) { enhancement = true; break; }
        // Do not waste the short preparation on repeated heals, item crafting
        // or HP->MP conversions; both resources are restored at the final lock.
        if (!enhancement) return false;
        // No party/area targets: the training listener permits the bot itself
        // only, not a buff service for the opposing human or incidental actors.
        return skill.getTargetType() == TargetType.SELF || skill.getTargetType() == TargetType.ONE;
    }

	/**
	 * Keeps the buff profile of the character up at all times (world PvP in the Coliseum, waiting
	 * for the olympiad, and during olympiad matches): warrior profile for non-casters, mage
	 * profile for casters (Dominator included). Inside olympiad matches the OLYMPIAD buff
	 * profile is used instead: the same set without any Chant of Victory family buff (CoV
	 * and its analogs) and without any dances/songs, as required for the olympiad buff list.
	 * Missing buffs are applied
	 * instantly (no cast time), the same way as the olympiad buff set; buffs already on the
	 * character are left alone. A profile buff is also skipped when its abnormalType slot is
	 * held by ANOTHER (equally strong or stronger) buff - otherwise the instant profile
	 * application and the auto-buffed class self-buffs of the same abnormalType would keep
	 * replacing each other forever (e.g. Blessed Body vs the Titan's Battle Roar, both
	 * MAX_HP_UP; Wind Walk vs the Wind Rider's Sprint; Blessed Soul vs the archers'
	 * Soul of Sagittarius). The profiles are sized to fit the 34 buff slots together with the
	 * class self-buffs and the olympiad extras, so no slot-eviction churn happens. Called
	 * from every combat tick (2s). Works for bots AND for real olympiad participants (buff
	 * parity support).
	 * @param player the character (bot or olympiad participant)
	 * @param caster the warrior/mage profile selector
	 */
	public static void maintainFullBuffs(Player player, boolean caster)
	{
		if ((player == null) || !player.isOnline() || player.isAlikeDead())
		{
			return;
		}

		final boolean olympiad = player.isInOlympiadMode();
		// Olympiad gate rule: once the gates are open (the match has started), no self-buff
		// longer than 2 minutes may be (re)applied. The pre-start hook (2 seconds before
		// the gates opened) applied the full olympiad profile; mid-fight maintenance keeps
		// only short profile entries (the olympiad profile is all long, so nothing is
		// re-applied during the fight - a buff wiped by Cancellation stays gone).
		final boolean activeMatch = olympiad && player.isOlympiadStart();
		final int[][] profile = caster
			? (olympiad ? getOlympiadBuffs(true) : FULL_BUFFS_MAGE)
			: (olympiad ? getOlympiadBuffs(false) : FULL_BUFFS_FIGHTER);
		int applied = 0;
		for (int[] buff : profile)
		{
            if (olympiad && (isOpenerOnlySkill(buff[0]) || isTankDefenseSkill(buff[0]))) continue;
			if (player.getEffectList().isAffectedBySkill(buff[0]))
			{
				continue;
			}

			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null)
			{
				LOGGER.warning("FakePlayersAI: full buff skill " + buff[0] + "/" + buff[1] + " not found, check FULL_BUFFS_*.");
				continue;
			}

			if (activeMatch && (skill.getAbnormalTime() > OLYMPIAD_MAX_SELFBUFF_SECONDS))
			{
				continue;
			}

			// Abnormal-type aware check: when another (equally strong or stronger) buff
			// occupies the abnormalType slot of this profile buff, applying it would
			// REPLACE that buff (which would then be applied back on the next pass -
			// an endless re-buff churn). The current slot holder wins instead.
			final BuffInfo stacked = player.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
			if ((stacked != null) && (stacked.getSkill().getId() != buff[0]) && (stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel()))
			{
				continue;
			}

			skill.applyEffects(player, player);
			applied++;
		}

		if (applied > 0)
		{
			LOGGER.info("FakePlayersAI: full-buffs " + player.getName() + ": applied " + applied + " missing buff(s).");
		}
	}

	/**
	 * Same as {@link #maintainFullBuffs(Player, boolean)} with the profile selected by the
	 * class preset (warrior/mage).
	 * @param player the bot
	 * @param preset the bot's class preset (selects the warrior or mage profile)
	 */
	public static void maintainFullBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null))
		{
			return;
		}
		maintainFullBuffs(player, preset.isCaster());
	}

	/**
	 * Keeps the bot consumables at the configured stock (called from every combat tick):
	 * <ul>
	 * <li>the S-grade soulshot AND the S-grade blessed spiritshot are always present in the
	 * inventory and always enabled as auto shots - the core clears the auto shot list on
	 * olympiad entry (OlympiadGame.removals()), this restores it within 2 seconds;</li>
	 * <li>Spirit Ores are kept in stock, but never above {@link #SPIRIT_ORE_CAP} (1000).</li>
	 * </ul>
	 * @param player the bot
	 */
	public static void maintainConsumables(Player player)
	{
		maintainConsumables(player, null);
	}

	/**
	 * Same as {@link #maintainConsumables(Player)}, plus: archer/crossbow classes get their
	 * arrow/bolt stock refilled to ArrowCount. A dry quiver left the archer bots unable to
	 * attack at all (auto attacks and bow skills both consume ammunition) - this keeps the
	 * ammo topped up on every combat tick, like the shots and the Spirit Ore stock.
	 * @param player the bot
	 * @param preset the bot's class preset (null - no ammunition maintenance)
	 */
	public static void maintainConsumables(Player player, BotClassPreset preset)
	{
		if ((player == null) || !player.isOnline())
		{
			return;
		}

		ensureShot(player, BotPresets.getFighterShotId());
		ensureShot(player, BLESSED_SPIRITSHOT_S);

		if ((preset != null) && preset.useArrows())
		{
			final int ammoId = preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW ? BotPresets.getBoltId() : BotPresets.getArrowId();
			if (ammoId > 0)
			{
				final int target = BotManager.getSettings().getArrowCount();
				final int have = (int) player.getInventory().getInventoryItemCount(ammoId, -1);
				if (have < target)
				{
					player.getInventory().addItem(ItemProcessType.REWARD, ammoId, target - have, player, null);
				}
			}
		}

		final int oreCount = (int) player.getInventory().getInventoryItemCount(SPIRIT_ORE_ID, -1);
		if (oreCount < SPIRIT_ORE_CAP)
		{
			player.getInventory().addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, SPIRIT_ORE_CAP - oreCount, player, null);
		}
		else if (oreCount > SPIRIT_ORE_CAP)
		{
			final Item ore = player.getInventory().getItemByItemId(SPIRIT_ORE_ID);
			if (ore != null)
			{
				player.getInventory().destroyItem(ItemProcessType.DESTROY, ore, oreCount - SPIRIT_ORE_CAP, player, null);
			}
		}
	}

	/**
	 * Makes sure the auto shot is enabled and the item is stocked at SoulshotCount.
	 */
	private static void ensureShot(Player player, int itemId)
	{
		ensureShot(player, itemId, BotManager.getSettings().getSoulshotCount());
	}

	/**
	 * Makes sure the auto shot is enabled and the item is stocked at the given amount.
	 */
	private static void ensureShot(Player player, int itemId, int targetCount)
	{
		if (!player.getAutoSoulShot().contains(itemId))
		{
			player.addAutoSoulShot(itemId);
		}

		final int have = (int) player.getInventory().getInventoryItemCount(itemId, -1);
		if (have < targetCount)
		{
			player.getInventory().addItem(ItemProcessType.REWARD, itemId, targetCount - have, player, null);
		}
	}

	/**
	 * Pre-combat Arcane Power aura for caster bots; called by the owning preparation phase.
	 * Engine-neutral instant effects, not a combat cast. Real humans cast their own auras.
	 */
	public static void applyMageOpeningAuras(Player player, boolean caster)
	{
		if ((player == null) || !caster)
		{
			return;
		}

		final int[][] auras =
		{
			{
				ARCANE_POWER_ID,
				1
			}
		};

		int applied = 0;
		for (int[] aura : auras)
		{
			final Skill skill = SkillData.getInstance().getSkill(aura[0], aura[1]);
			if (skill == null)
			{
				LOGGER.warning("FakePlayersAI: opening aura skill " + aura[0] + "/" + aura[1] + " not found.");
				continue;
			}

			skill.applyEffects(player, player);
			applied++;
		}

		LOGGER.info("FakePlayersAI: oly-auras " + player.getName() + ": applied " + applied + "/1 opening mage aura.");
	}

	/**
	 * Pre-combat Enlightenment (1532) for the existing caster presets, including Dominator.
	 * Called once by the owning preparation phase, never scheduled after the gates.
	 * Existing effect is not refreshed; no engine is rearmed.
	 */
	public static void applyEnlightenmentOpener(Player player)
	{
		if ((player == null) || !player.isOnline() || player.isAlikeDead())
		{
			return;
		}

		if (player.getEffectList().isAffectedBySkill(ENLIGHTENMENT_ID))
		{
			return; // already up - nothing to apply
		}

		Skill skill = player.getKnownSkill(ENLIGHTENMENT_ID);
		if (skill == null)
		{
			// Fallback: the wizard Enlightenment template (the bot does not know the skill yet,
			// e.g. a Dominator restored from the database before the datapack re-teach).
			skill = SkillData.getInstance().getSkill(ENLIGHTENMENT_ID, 1);
		}
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: enlightenment opener skill " + ENLIGHTENMENT_ID + " not found in SkillData.");
			return;
		}

		skill.applyEffects(player, player);
		LOGGER.info("FakePlayersAI: oly-openers " + player.getName() + ": " + skill.getName() + " (skill " + skill.getId() + ") applied in the protected pre-combat window.");
	}

	/**
	 * Same as {@link #applyMageOpeningAuras(Player, boolean)} with the caster flag from the
	 * class preset.
	 * @param player the bot
	 * @param preset the bot's class preset (must be a caster)
	 */
	public static void applyMageOpeningAuras(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null))
		{
			return;
		}
		applyMageOpeningAuras(player, preset.isCaster());
	}

	/**
	 * Opening self-enhancements applied by the protected preparation phase only, shortly
	 * BEFORE combat. They never belong to automatic combat lists:
	 * <ul>
	 * <li><b>Final Secret (917)</b> for every class that knows it (Duelist, Titan, Grand
	 * Khavatari, Doombringer);</li>
	 * <li>the dagger class evasion opener - the class's own evasion skill from its skill tree:
	 * Adventurer (93) casts Mirage (445), Wind Rider (101) casts Dodge (446), Ghost Hunter
	 * (108) casts Ghost Walking (770);</li>
	 * <li>the dagger adventure self-enhancement - Exciting Adventure (768) for the Adventurer,
	 * Wind Riding (769) for the Wind Rider.</li>
	 * </ul>
	 * Application is instant (no cast time, no reuse check - the same style as the olympiad
	 * buff set); a skill the bot does not know (or that is already active) is skipped.
	 * @param player the bot
	 * @param preset the bot's class preset
	 */
    /** Called ONCE by the owning ranked/training preparation phase, never by a
     * combat rotation or an end-of-match repair. Does not start either engine. */
    public static void applyPreCombatOpeners(Player player, BotClassPreset preset)
    {
        if (player == null || preset == null || player.isAlikeDead() || !player.isOnline()) return;
        applyOpeningSelfBuffs(player,preset);
        if (preset.isCaster())
        {
            applyMageOpeningAuras(player,preset);
            applyEnlightenmentOpener(player);
        }
    }

	public static void applyOpeningSelfBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead())
		{
			return;
		}

		// Final Secret: every class of the roster that knows it (88, 113, 114, 131).
		applyOpeningSelfBuff(player, FINAL_SECRET_ID);

		// Dagger evasion opener + adventure self-enhancement: the class's own skills.
		final int opener;
		final int adventure;
		switch (preset.getClassId())
		{
			case CLASS_ADVENTURER:
			{
				opener = MIRAGE_ID;
				adventure = EXCITING_ADVENTURE_ID;
				break;
			}
			case CLASS_WIND_RIDER:
			{
				opener = DODGE_ID;
				adventure = WIND_RIDING_ID;
				break;
			}
			case CLASS_GHOST_HUNTER:
			{
				opener = GHOST_WALKING_ID;
				adventure = 0;
				break;
			}
			default:
			{
				opener = 0;
				adventure = 0;
			}
		}
		if (opener > 0)
		{
			applyOpeningSelfBuff(player, opener);
		}
		if (adventure > 0)
		{
			applyOpeningSelfBuff(player, adventure);
		}
	}

	/**
	 * Applies one opening self-enhancement instantly (no cast time), skipping skills the
	 * character does not know or that are already active.
	 */
	private static void applyOpeningSelfBuff(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		if (skill == null)
		{
			return;
		}
		if (player.getEffectList().isAffectedBySkill(skillId))
		{
			return; // already up - nothing to apply
		}

		skill.applyEffects(player, player);
		LOGGER.info("FakePlayersAI: oly-openers " + player.getName() + ": " + skill.getName() + " (skill " + skill.getId() + ") applied.");
	}

	/**
	 * @return the olympiad summon skill id of the class preset (Summon Cursed Man for the
	 * Soultaker, Summon Dark Panther for the Hell Knight, Summon Imperial Phoenix for the
	 * Phoenix Knight), or 0 for every other class.
	 */
	public static int getOlympiadSummonSkillId(BotClassPreset preset)
	{
		if (preset == null)
		{
			return 0;
		}
		switch (preset.getClassId())
		{
			case CLASS_SOULTAKER:
			{
				return SUMMON_CURSED_MAN_ID;
			}
			case CLASS_HELL_KNIGHT:
			{
				return SUMMON_DARK_PANTHER_ID;
			}
			case CLASS_PHOENIX_KNIGHT:
			{
				return SUMMON_IMPERIAL_PHOENIX_ID;
			}
			default:
			{
				return 0;
			}
		}
	}

	/**
	 * Olympiad summon support for the summoner classes of the roster (called from the combat
	 * driver during an active match and from the pre-start hook 2 seconds before the battle):
	 * <ul>
	 * <li><b>Soultaker (95)</b>: always keeps Summon Cursed Man up and the Transfer Pain toggle
	 * active (the toggle redirects 50%+ of the damage received by the bot to the servitor,
	 * PlayerStatus.reduceHp -> summon, while the effect holds the transDam stat).</li>
	 * <li><b>Hell Knight (91)</b>: always keeps Summon Dark Panther up, stocks 2000 Beast
	 * Soulshots (used from the master's inventory on every servitor hit through the
	 * SUMMON_SOULSHOT auto shot) and sends the panther at the match opponent.</li>
	 * <li><b>Phoenix Knight (90)</b>: the same as the Hell Knight, but with Summon Imperial
	 * Phoenix (Forgotten Scroll skill, learned with the full class tree).</li>
	 * </ul>
	 * Application is instant and SILENT (no cast time, no fake MagicSkillUse broadcast -
	 * visual bug fix of 04.10.2026, see inside): the Summon effect is instant and spawns the
	 * servitor in onStart (it no-ops while a summon already exists), the toggle lands in the
	 * effect list directly. The servitor upkeep crystal stock
	 * (C-Grade for Cursed Man/Dark Panther, A-Grade for Imperial Phoenix) is maintained too -
	 * the servitor life task unsummons it when the owner runs out. All steps are idempotent.
	 * @param player the bot
	 * @param preset the bot's class preset
	 * @param target the current opponent (or null - then no attack order is given)
	 */
	public static void maintainOlympiadSummon(Player player, BotClassPreset preset, WorldObject target)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead())
		{
			return;
		}

		final int summonSkillId = getOlympiadSummonSkillId(preset);
		if (summonSkillId == 0)
		{
			return;
		}

		// 1) The summon itself: instantly re-created whenever it is missing (match start
		// unsummon/reset, servitor death, life time end, ...).
		if (!player.hasSummon())
		{
			final Skill summonSkill = player.getKnownSkill(summonSkillId);
			if (summonSkill != null)
			{
				// Silent instant application (visual bug fix of 04.10.2026): the fake
				// MagicSkillUse broadcast played the full summon cast animation while the
				// server kept the bot moving/attacking - the client snapped the bot back
				// (a visible "teleport") and the summon animation switched abruptly to the
				// attack-cast animation the moment the rotation fired. The servitor appears
				// through its own spawn broadcast instead: no cast animation at all.
				summonSkill.applyEffects(player, player);
				LOGGER.info("FakePlayersAI: oly-summon " + player.getName() + ": " + summonSkill.getName() + " is up.");
			}
			else
			{
				LOGGER.warning("FakePlayersAI: oly-summon " + player.getName() + ": summon skill " + summonSkillId + " is not known!");
			}
		}

		// 2) Soultaker: Transfer Pain toggle is always on during the match (it is a toggle -
		// applied instantly, stays until stopped; the olympiad cleanEffects wipes it, this
		// restores it on the next tick). Applied silently like the summon (same visual
		// artifact of the fake cast animation).
		if (summonSkillId == SUMMON_CURSED_MAN_ID)
		{
			if (!player.getEffectList().isAffectedBySkill(TRANSFER_PAIN_ID))
			{
				Skill toggle = player.getKnownSkill(TRANSFER_PAIN_ID);
				if (toggle == null)
				{
					toggle = SkillData.getInstance().getSkill(TRANSFER_PAIN_ID, 5);
				}
				if (toggle != null)
				{
					toggle.applyEffects(player, player);
					LOGGER.info("FakePlayersAI: oly-summon " + player.getName() + ": Transfer Pain enabled.");
				}
			}
		}
		// 3) Knights: beast soulshots stocked at PET_SOULSHOT_COUNT and enabled as auto shot.
		else if ((summonSkillId == SUMMON_DARK_PANTHER_ID) || (summonSkillId == SUMMON_IMPERIAL_PHOENIX_ID))
		{
			ensureShot(player, BEAST_SOULSHOT_ID, PET_SOULSHOT_COUNT);
		}

		// Servitor upkeep crystals (4 per 4 minutes for Cursed Man/Dark Panther, 1 for the
		// Imperial Phoenix): without them the servitor life task unsummons it.
		final int crystalId = (summonSkillId == SUMMON_IMPERIAL_PHOENIX_ID) ? CRYSTAL_A_ID : CRYSTAL_C_ID;
		final int crystals = (int) player.getInventory().getInventoryItemCount(crystalId, -1);
		if (crystals < SERVITOR_CRYSTAL_STOCK)
		{
			player.getInventory().addItem(ItemProcessType.REWARD, crystalId, SERVITOR_CRYSTAL_STOCK - crystals, player, null);
		}

		// 4) Send the summon into the fight: at the match opponent, re-asserted whenever the
		// summon stands without the target (its AI falls back to following the owner).
		if ((target != null) && target.isCreature())
		{
			final Creature enemy = target.asCreature();
			if (!enemy.isAlikeDead())
			{
				final Summon summon = player.getSummon();
				if ((summon != null) && !summon.isDead() && (summon.getTarget() != enemy))
				{
					summon.doSummonAttack(enemy);
				}
			}
		}
	}
	
	/**
	 * Makes sure the bot actually wears the preset gear (Vesper weapon/armor/jewelry per the
	 * current grade setting) and that every equipped piece passes its template conditions.
	 * Re-equips the full preset set (equipGear) when:
	 * <ul>
	 * <li>any required piece is missing from the inventory or not equipped;</li>
	 * <li>any equipped piece fails its item conditions - e.g. Vesper heavy/robe sets and
	 * shields carry races="HUMAN,ELF,DARK_ELF,ORC,DWARF" (no Kamael): a Kamael bot wearing
	 * such a piece keeps it in the world, but the core unequips it right at the olympiad
	 * arena entry (OlympiadGame.removals() -> Player.checkItemRestriction()), which is how
	 * the Doombringer ended up fighting the olympiad match without armor.</li>
	 * </ul>
	 * Called from registerBot() for every (re)spawned bot - heals bots restored from the
	 * database after a preset/gear change without any manual //bot regear.
	 * @param player the bot
	 * @param preset the bot's class preset
	 */
	public static void ensurePresetGear(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline())
		{
			return;
		}

		final BotSettings settings = BotManager.getSettings();
		final List<Integer> required = new ArrayList<>();
		for (int id : BotPresets.getArmorSet(preset, settings.isUseS84()))
		{
			required.add(id);
		}
		final int shield = BotPresets.getShieldId(preset, settings.isUseS84());
		if (shield > 0)
		{
			required.add(shield);
		}
		final int weapon = BotPresets.getWeaponId(preset, settings.isUseS84());
		if (weapon > 0)
		{
			required.add(weapon);
		}
		for (int jewel : BotPresets.getJewelryIds())
		{
			required.add(jewel);
		}

		String reason = null;
		for (int itemId : required)
		{
			final Item item = player.getInventory().getItemByItemId(itemId);
			if ((item == null) || !item.isEquipped())
			{
				reason = "missing/un-equipped item " + itemId;
				break;
			}
		}

		if (reason == null)
		{
			for (Item item : player.getInventory().getPaperdollItems())
			{
				if ((item != null) && !item.getTemplate().checkCondition(player, player, false))
				{
					reason = "condition-restricted item " + item.getId() + " (" + item.getTemplate().getName() + ")";
					break;
				}
			}
		}

		if (reason != null)
		{
			LOGGER.info("FakePlayersAI: gear-check " + player.getName() + ": re-equipping (" + reason + ").");
			equipGear(player, preset);
		}
	}

	/**
	 * (Re)equips a bot according to its preset and current global settings (grade + enchants).
	 */
	public static void equipGear(Player player, BotClassPreset preset)
	{
		final BotSettings settings = BotManager.getSettings();
		final int enchantArmor = settings.getEnchantArmor();
		final int enchantWeapon = settings.getEnchantWeapon();
		final int enchantJewel = settings.getEnchantJewel();

		// Remove old bot gear (if re-gearing an existing bot).
		destroyOldGear(player);
		
		// Armor set.
		final int[] set = BotPresets.getArmorSet(preset, settings.isUseS84());
		for (int itemId : set)
		{
			giveAndEquip(player, itemId, 1, enchantArmor);
		}
		
		// Shield for tanks.
		final int shield = BotPresets.getShieldId(preset, settings.isUseS84());
		if (shield > 0)
		{
			giveAndEquip(player, shield, 1, enchantArmor);
		}
		
		// Weapon.
		final int weapon = BotPresets.getWeaponId(preset, settings.isUseS84());
		if (weapon > 0)
		{
			giveAndEquip(player, weapon, 1, enchantWeapon);
		}
		
		// Jewelry (enchanted with EnchantJewel like the rest of the gear).
		for (int jewel : BotPresets.getJewelryIds())
		{
			giveAndEquip(player, jewel, 1, enchantJewel);
		}
		
		player.refreshOverloaded();
		player.updateAndBroadcastStatus();
	}
	
	/**
	 * Re-enchants a bot's currently equipped gear in place to the current global settings:
	 * weapon -> EnchantWeapon, armor pieces and shield -> EnchantArmor, jewelry -> EnchantJewel.
	 * Unlike equipGear() nothing is destroyed or re-created: the enchant level of each
	 * equipped item is changed directly. Returns the number of items actually changed.
	 */
	public static int reenchantGear(Player player)
	{
		return reenchantGear(player, true, true, true);
	}

	/**
	 * Re-enchants a bot's currently equipped gear in place to the current global settings
	 * (jewelry is left untouched - used by the armor/weapon //bot commands).
	 * @param player the bot
	 * @param enchantWeaponItems apply EnchantWeapon to the equipped weapon
	 * @param enchantArmorItems apply EnchantArmor to equipped armor pieces and shield
	 * @return number of items whose enchant level was actually changed by this call
	 */
	public static int reenchantGear(Player player, boolean enchantWeaponItems, boolean enchantArmorItems)
	{
		return reenchantGear(player, enchantWeaponItems, enchantArmorItems, false);
	}

	/**
	 * Re-enchants a bot's currently equipped gear in place to the current global settings.
	 * @param player the bot
	 * @param enchantWeaponItems apply EnchantWeapon to the equipped weapon
	 * @param enchantArmorItems apply EnchantArmor to equipped armor pieces and shield
	 * @param enchantJewelItems apply EnchantJewel to equipped jewelry (earrings, rings, necklace)
	 * @return number of items whose enchant level was actually changed by this call
	 */
	public static int reenchantGear(Player player, boolean enchantWeaponItems, boolean enchantArmorItems, boolean enchantJewelItems)
	{
		if (player == null)
		{
			return 0;
		}
		final BotSettings settings = BotManager.getSettings();
		final int enchantArmor = settings.getEnchantArmor();
		final int enchantWeapon = settings.getEnchantWeapon();
		final int enchantJewel = settings.getEnchantJewel();

		int changed = 0;
		for (Item item : player.getInventory().getPaperdollItems())
		{
			if (item == null)
			{
				continue;
			}

			final int targetEnchant;
			if (isJewelry(item))
			{
				if (!enchantJewelItems)
				{
					continue;
				}
				targetEnchant = enchantJewel;
			}
			else if (item.isWeapon())
			{
				if (!enchantWeaponItems)
				{
					continue;
				}
				targetEnchant = enchantWeapon;
			}
			else if (item.isArmor())
			{
				if (!enchantArmorItems)
				{
					continue;
				}
				targetEnchant = enchantArmor;
			}
			else
			{
				continue;
			}

			if (item.getEnchantLevel() == targetEnchant)
			{
				continue;
			}

			applyEnchantInPlace(player, item, targetEnchant);
			changed++;
		}

		if (changed > 0)
		{
			player.updateAndBroadcastStatus();
		}
		return changed;
	}

	/**
	 * @return true when the item is jewelry (earring, ring or necklace).
	 */
	private static boolean isJewelry(Item item)
	{
		switch (item.getTemplate().getBodyPart())
		{
			case L_EAR:
			case R_EAR:
			case LR_EAR:
			case L_FINGER:
			case R_FINGER:
			case LR_FINGER:
			case NECK:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Changes the enchant level of an item in place, mirroring the core enchant flow
	 * (RequestEnchantItem): item.setEnchantLevel() re-applies enchant option stats itself,
	 * the item row is stored, the "+4 enchant skill" is added/removed when the enchant
	 * level crosses the 4 boundary (same condition as the equip/unequip listeners), and an
	 * InventoryUpdate is sent to the owner (no-op for clientless bots).
	 */
	private static void applyEnchantInPlace(Player player, Item item, int enchant)
	{
		final Skill enchant4Skill = item.getTemplate().getEnchant4Skill();
		final boolean hadEnchant4Skill = (enchant4Skill != null) && (item.getEnchantLevel() >= 4);
		
		item.setEnchantLevel(enchant);
		item.updateDatabase();
		
		final boolean hasEnchant4Skill = (enchant4Skill != null) && (item.getEnchantLevel() >= 4);
		if ((enchant4Skill != null) && (hadEnchant4Skill != hasEnchant4Skill))
		{
			if (hasEnchant4Skill)
			{
				player.addSkill(enchant4Skill, false);
			}
			else
			{
				player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
			}
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendPacket(iu);
	}
	
	private static void destroyOldGear(Player player)
	{
		for (int itemId : BotPresets.getAllGearIds())
		{
			final Item item = player.getInventory().getItemByItemId(itemId);
			if (item != null)
			{
				player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
			}
		}
	}
	
	private static void giveAndEquip(Player player, int itemId, int count, int enchant)
	{
		if (itemId <= 0)
		{
			return;
		}
		final Item item = player.getInventory().addItem(ItemProcessType.REWARD, itemId, count, player, null);
		if (item == null)
		{
			LOGGER.warning("FakePlayersAI: item " + itemId + " not found in ItemData, check BotPresets.");
			return;
		}
		if (enchant > 0)
		{
			item.setEnchantLevel(enchant);
		}
		if (item.isEquipable())
		{
			player.getInventory().equipItem(item);
		}
	}
	
	private static void giveConsumables(Player player, BotClassPreset preset)
	{
		final BotSettings settings = BotManager.getSettings();

		// Every bot stocks BOTH S-grade shots (auto-enabled by applyAutoCombat and kept up
		// by maintainConsumables) and a full Spirit Ore stock (capped at SPIRIT_ORE_CAP).
		player.getInventory().addItem(ItemProcessType.REWARD, BotPresets.getFighterShotId(), settings.getSoulshotCount(), player, null);
		player.getInventory().addItem(ItemProcessType.REWARD, BLESSED_SPIRITSHOT_S, settings.getSoulshotCount(), player, null);
		player.getInventory().addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, SPIRIT_ORE_CAP, player, null);
		
		if (preset.useArrows())
		{
			final int ammoId = preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW ? BotPresets.getBoltId() : BotPresets.getArrowId();
			if (ammoId > 0)
			{
				player.getInventory().addItem(ItemProcessType.REWARD, ammoId, settings.getArrowCount(), player, null);
			}
		}
		
		final int potionId = BotPresets.getPotionId();
		if (potionId > 0)
		{
			player.getInventory().addItem(ItemProcessType.REWARD, potionId, settings.getPotionCount(), player, null);
		}
		
		final int cpPotionId = BotPresets.getCpPotionId();
		if (cpPotionId > 0)
		{
			player.getInventory().addItem(ItemProcessType.REWARD, cpPotionId, settings.getPotionCount(), player, null);
		}
		// NOTE: Cursed Bones (2508) are no longer stocked - Death Spike (1148) lost its
		// reagent requirement in the datapack and no bot-relevant skill consumes them.
	}
	
	/**
	 * Generates a unique unused character name.
	 */
	public static String generateName(boolean female)
	{
		final List<String> names = BotPresets.getNames(female);
		for (int attempt = 0; attempt < 100; attempt++)
		{
			final String base = names.get(Rnd.get(names.size()));
			final String name = attempt == 0 ? base : base + Rnd.get(10, 99);
			if (!CharInfoTable.getInstance().doesCharNameExist(name))
			{
				return name;
			}
		}
		return "Bot" + Rnd.get(1000000, 9999999);
	}
	
	/**
	 * Picks a random spawn point around the configured area.
	 */
	public static List<int[]> getSpawnPoints(int count)
	{
		final BotSettings settings = BotManager.getSettings();
		final List<int[]> points = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int angle = Rnd.get(360);
			final int radius = Rnd.get(0, settings.getSpawnRadius());
			final int x = settings.getSpawnX() + (int) (radius * Math.cos(Math.toRadians(angle)));
			final int y = settings.getSpawnY() + (int) (radius * Math.sin(Math.toRadians(angle)));
			points.add(new int[]
			{
				x,
				y,
				settings.getSpawnZ()
			});
		}
		return points;
	}
}
