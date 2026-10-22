
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
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
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

public class BotSpawner
{
	private static final Logger LOGGER = Logger.getLogger(BotSpawner.class.getName());
	private static final int SPIRIT_ORE_ID = 3031;
	private static final int EINHASAD_HOLY_WATER_ID = 8874;
	private static final int ESCAPE_SHACKLE_ID = 453;
	public static final int COUNTER_CELESTIAL_ID = 655;
	private static final int MAX_SKILL_ENCHANT_LEVEL = 30;
	private static final int FUNC_ORDER_ADD = 1;
	private static final int FUNC_ORDER_SUB = 2;
	private static final int FUNC_ORDER_MUL = 3;
	private static final int AUTO_ATTACK_ACTION = 2;
	private static final int CHARGE_WINDOW_MAX = 7;
	private static final int[][] OLYMPIAD_BUFFS =
	{
		{ 1204, 2 }, { 1086, 2 }, { 1062, 2 }, { 1068, 3 }, { 1059, 3 },
		{ 1077, 3 }, { 1242, 3 }, { 1085, 3 }, { 1045, 6 }, { 1303, 2 },
		{ 1040, 3 }, { 1036, 2 }, { 1078, 6 },
	};
	private static final int[][] FULL_BUFFS_FIGHTER =
	{
		{ 1204, 2 }, { 1086, 2 }, { 1068, 3 }, { 1077, 3 }, { 1242, 3 },
		{ 1240, 3 }, { 1062, 2 }, { 1045, 6 }, { 1048, 6 }, { 1040, 3 },
		{ 1036, 2 }, { 1268, 4 }, { 1044, 3 }, { 1363, 1 }, { 271, 1 },
		{ 274, 1 }, { 264, 1 }, { 267, 1 }, { 268, 1 }, { 269, 1 }, { 304, 1 },
	};
	private static final int[][] FULL_BUFFS_MAGE =
	{
		{ 1204, 2 }, { 1085, 3 }, { 1059, 3 }, { 1078, 6 }, { 1303, 2 },
		{ 1062, 2 }, { 1045, 6 }, { 1048, 6 }, { 1040, 3 }, { 1036, 2 },
		{ 1355, 1 }, { 276, 1 }, { 363, 1 }, { 304, 1 }, { 267, 1 },
	};
	private static final int[] CHANT_OF_VICTORY_FAMILY =
	{
		1355, 1356, 1357, 1363, 1413, 1414, 7061, 7062, 7063, 7064,
	};
	private static volatile int[][] OLYMPIAD_BUFFS_FIGHTER = null;
	private static volatile int[][] OLYMPIAD_BUFFS_MAGE = null;

	public static boolean isChantOfVictoryFamily(int skillId)
	{
		return containsId(CHANT_OF_VICTORY_FAMILY, skillId);
	}

	private static boolean isDominatorVictory(Player player, int skillId)
	{
		return player != null && skillId == 1414 && player.getPlayerClass().getId() == 115;
	}

	private static boolean isOlympiadChantBanned(Player player, int skillId)
	{
		return isChantOfVictoryFamily(skillId) && !isDominatorVictory(player, skillId);
	}

	/** Class-specific server preparation buff, not a configurable rotation entry.
	 * Preserve a learned enchant; without the skill use the native base effect only.
	 * Callers run before combat, never as a free in-combat rebuff. */
	private static void applyDominatorArenaBuff(Player player)
	{
		if (!isDominatorVictory(player, 1414) || player.isAlikeDead() || player.getEffectList().isAffectedBySkill(1414)) return;
		Skill skill = player.getKnownSkill(1414);
		if (skill == null) skill = SkillData.getInstance().getSkill(1414, 1);
		if (skill == null) { LOGGER.warning("FakePlayersAI: Dominator arena buff unavailable: 1414/1"); return; }
		// Apply solely to the recipient: PARTY_CLAN must not buff the opponent.
		skill.applyEffects(player, player);
		LOGGER.info("FakePlayersAI: arena-buff " + player.getName() + ": Victory of Pa'agrio applied.");
	}

	public static int[] getTacticalCpSupport(Player player, Player enemy)
	{
		if (player == null || player.getPlayerClass().getId() != 115) return new int[0];
		// CP recovery is mandatory learned support, independent of damage/buff menu overrides.
		// Selection still enforces native cost, reuse, self-only targets and invulnerability.
		return java.util.stream.IntStream.of(1305, 1306)
			.filter(id -> player.getKnownSkill(id) != null).toArray();
	}

	private static int[][] buildOlympiadProfile(int[][] profile)
	{
		final List<int[]> result = new ArrayList<>(profile.length);
		for (int[] buff : profile)
		{
			if (isChantOfVictoryFamily(buff[0]) || buff[0]==1268) continue;
			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if ((skill != null) && skill.isDance()) continue;
			result.add(buff);
		}
		return result.toArray(new int[result.size()][]);
	}

	private static int[][] getOlympiadBuffs(boolean caster)
	{
		if (caster)
		{
			if (OLYMPIAD_BUFFS_MAGE == null) OLYMPIAD_BUFFS_MAGE = buildOlympiadProfile(FULL_BUFFS_MAGE);
			return OLYMPIAD_BUFFS_MAGE;
		}
		if (OLYMPIAD_BUFFS_FIGHTER == null) OLYMPIAD_BUFFS_FIGHTER = buildOlympiadProfile(FULL_BUFFS_FIGHTER);
		return OLYMPIAD_BUFFS_FIGHTER;
	}

	private static final int BLESSED_SPIRITSHOT_S = 3952;
	private static final int CLASS_PHOENIX_KNIGHT = 90;
	private static final int CLASS_HELL_KNIGHT = 91;
	private static final int CLASS_SOULTAKER = 95;
	private static final int SUMMON_CURSED_MAN_ID = 1334;
	private static final int TRANSFER_PAIN_ID = 1262;
	private static final int SUMMON_DARK_PANTHER_ID = 283;
	private static final int SUMMON_IMPERIAL_PHOENIX_ID = 912;
	private static final int BEAST_SOULSHOT_ID = 6645;
	private static final int PET_SOULSHOT_COUNT = 2000;
	private static final int CRYSTAL_C_ID = 1459;
	private static final int CRYSTAL_A_ID = 1461;
	private static final int SERVITOR_CRYSTAL_STOCK = 100;
	private static final int SPIRIT_ORE_CAP = 1000;
	private static final int CARDINAL_CLEANSE_REAGENT_CAP = 1000;
	private static final int ARCANE_POWER_ID = 337;
	private static final int ENLIGHTENMENT_ID = 1532;
	private static final int FINAL_SECRET_ID = 917;
	private static final int CLASS_ADVENTURER = 93;
	private static final int CLASS_WIND_RIDER = 101;
	private static final int CLASS_GHOST_HUNTER = 108;
	private static final int MIRAGE_ID = 445;
	private static final int DODGE_ID = 446;
	private static final int GHOST_WALKING_ID = 770;
	private static final int EXCITING_ADVENTURE_ID = 768;
	private static final int WIND_RIDING_ID = 769;
	private static final int OLYMPIAD_MAX_SELFBUFF_SECONDS = 120;
	private static final int[] OPENER_ONLY_SKILLS =
	{
		MIRAGE_ID, DODGE_ID, GHOST_WALKING_ID, FINAL_SECRET_ID, EXCITING_ADVENTURE_ID,
		WIND_RIDING_ID, ENLIGHTENMENT_ID, 1533, ARCANE_POWER_ID,
	};
	private static final int VENGEANCE_ID = 368;
	private static final int[] TANK_DEFENSE_PHYSICAL =
	{
		VENGEANCE_ID, 110, 350, 439, 527, 112, 82, 86,
	};
	private static final int[] TANK_DEFENSE_MAGIC =
	{
		760, VENGEANCE_ID, 913, 351, 72, 916,
	};
	private static final int[] TANK_DEFENSE_ALL =
	{
		110, VENGEANCE_ID, 350, 439, 527, 112, 82, 86, 760, 913, 351, 72, 916
	};

	public static boolean isTankDefenseSkill(int skillId)
	{
		return containsId(TANK_DEFENSE_ALL, skillId);
	}

	private static final int[] MAGE_ATTACK_SKILLS =
	{
		1235, 1340, 1453, 1230, 1339, 1451, 1234, 1343, 1239, 1456, 1341,
	};
	private static final int[] MAGE_CURSE_SKILLS =
	{
		1083, 1071, 1074, 1338, 1056, 1169, 1069, 1160, 1263, 1064, 1170,
		1222, 1337, 1367, 1097, 1092, 1105, 1246, 1247, 1248, 1208, 1099, 1104, 1206,
	};

	private static boolean isMageCurse(int skillId)
	{
		for (int id : MAGE_CURSE_SKILLS)
			if (id == skillId) return true;
		return false;
	}

	private static final int CLASS_ARCHMAGE = 94;
	private static final int CLASS_MYSTIC_MUSE = 103;
	private static final int CLASS_STORM_SCREAMER = 110;
	// Cardinal (classId 97): opening MP-regen debuff and pre-combat Prayer.
	private static final int CLASS_CARDINAL = 97;
	private static final int CARDINAL_PROMINENCE_ID = 1230;
	private static final int MANA_STORM_ID = 1399;
	private static final int CARDINAL_PRAYER_ID = 1307;
	private static final int SURRENDER_TO_FIRE_ID = 1083;
	private static final int SURRENDER_TO_WATER_ID = 1071;
	private static final int SURRENDER_TO_WIND_ID = 1074;
	private static final int CURSE_GLOOM_ID = 1263;

	public static int getOpeningCurseSkillId(BotClassPreset preset)
	{
		if (preset == null) return 0;
		switch (preset.getClassId())
		{
			case CLASS_ARCHMAGE: return SURRENDER_TO_FIRE_ID;
			case CLASS_MYSTIC_MUSE: return SURRENDER_TO_WATER_ID;
			case CLASS_STORM_SCREAMER: return SURRENDER_TO_WIND_ID;
			case CLASS_SOULTAKER: return CURSE_GLOOM_ID;
			case CLASS_CARDINAL: return MANA_STORM_ID;
			default: return 0;
		}
	}

	public static boolean ensureOpeningCurseEnchant(Player player, BotClassPreset preset)
	{
		final int skillId = getOpeningCurseSkillId(preset);
		if ((player == null) || (skillId == 0)) return false;
		final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
		if (learn == null) return false;
		final Skill baseSkill = SkillData.getInstance().getSkill(skillId, learn.getBaseLevel());
		if (baseSkill == null) return false;
		int chanceLevel = 0;
		final List<Integer> routes = new ArrayList<>(learn.getAllRoutes());
		Collections.sort(routes);
		for (int route : routes)
		{
			final int maxLevel = getRouteMaxLevel(learn, route);
			if (maxLevel == 0) continue;
			final Skill routeSkill = SkillData.getInstance().getSkill(skillId, maxLevel);
			if ((routeSkill != null) && getRouteLabel(baseSkill, routeSkill, route).equals("Chance"))
			{
				chanceLevel = maxLevel;
				break;
			}
		}
		if ((chanceLevel == 0) || (player.getSkillLevel(skillId) == chanceLevel)) return false;
		final Skill targetSkill = SkillData.getInstance().getSkill(skillId, chanceLevel);
		if (targetSkill == null) return false;
		player.addSkill(targetSkill, true);
		return true;
	}

	public static boolean applyOpeningCurse(Player player, BotClassPreset preset, Creature target)
	{
		if ((player == null) || (preset == null) || (target == null) || target.isAlikeDead()) return false;
		final int skillId = getOpeningCurseSkillId(preset);
		if (skillId == 0) return false;
		if (!player.getAutoUseSettings().getAutoSkills().contains(skillId)) return false;
		Skill skill = player.getKnownSkill(skillId);
		if (skill == null) skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: opening curse skill " + skillId + " not found in SkillData.");
			return false;
		}
		if (player.calculateDistance2D(target) > (skill.getCastRange() + 100)) return false;
		if (player.isSkillDisabled(skill)) return false;
		skill.applyEffects(player, target);
		if (skill.getReuseDelay() > 0) player.addTimeStamp(skill, skill.getReuseDelay());
		LOGGER.info("FakePlayersAI: open-curse " + player.getName() + ": " + skill.getName() + " (level " + skill.getLevel() + ") on " + target.getName() + ".");
		return true;
	}

	public static String classNickname(BotClassPreset preset)
	{
		return preset.getClassName().replace(" ", "");
	}

	public static String generateClassNickname(BotClassPreset preset)
	{
		final String base = classNickname(preset);
		if (staticClassNickname(preset))
		{
			final String name = fixedClassNickname(base, 0, CharInfoTable.getInstance()::getIdByName);
			if (!name.equals(base)) LOGGER.warning("FakePlayersAI: name " + base + " occupied; using stable class name " + name + " (no suffix, foreign character unchanged).");
			return name;
		}
		if (!CharInfoTable.getInstance().doesCharNameExist(base)) return base;
		for (int i = 2; i < 100; i++)
		{
			final String candidate = base + i;
			if (!CharInfoTable.getInstance().doesCharNameExist(candidate)) return candidate;
		}
		return base + Rnd.get(100, 999);
	}

	static boolean staticClassNickname(BotClassPreset preset)
	{
		return preset != null && (preset.getClassId() == CLASS_CARDINAL || preset.getClassId() == 88);
	}

	/** Semantic class names, never generated numeric/alphabetic suffixes. */
	static List<String> fixedClassNames(String base)
	{
		return switch (base)
		{
			case "Cardinal" -> List.of("Cardinal", "Bishop");
			case "Duelist" -> List.of("Duelist", "Gladiator");
			default -> List.of(base);
		};
	}

	static String fixedClassNickname(String base, int objectId, java.util.function.ToIntFunction<String> ownerOfName)
	{
		for (String candidate : fixedClassNames(base))
		{
			final int owner = ownerOfName.applyAsInt(candidate);
			if (owner < 0 || objectId > 0 && owner == objectId) return candidate;
		}
		throw new IllegalStateException("Static bot names " + fixedClassNames(base) + " are all occupied by other characters");
	}

	public static boolean ensureClassNickname(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null)) return false;
		final String desired = classNickname(preset);
		final String current = player.getName();
		if (desired.equals(current)) return false;
		// Once a semantic fallback was chosen, retain it even if the primary name frees up.
		if (staticClassNickname(preset) && fixedClassNames(desired).stream().anyMatch(n -> n.equalsIgnoreCase(current))) return false;
		final String name;
		try
		{
			name = staticClassNickname(preset)
				? fixedClassNickname(desired, player.getObjectId(), CharInfoTable.getInstance()::getIdByName)
				: generateClassNickname(preset);
		}
		catch (IllegalStateException conflict)
		{
			LOGGER.warning("FakePlayersAI: " + conflict.getMessage() + "; keeping " + current + " unchanged (no suffix cycling).");
			return false;
		}
		if (name.equals(current)) return false;
		player.setName(name);
		player.storeMe();
		LOGGER.info("FakePlayersAI: rename " + current + " -> " + name + " (" + preset.getClassName() + ").");
		return true;
	}

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
		player.setXYZInvisible(settings.getSpawnX(), settings.getSpawnY(), settings.getSpawnZ());
		player.setTitle(settings.getBotTitle());
		final long expFor85 = ExperienceData.getInstance().getExpForLevel(85);
		if (player.getExp() < expFor85) player.addExpAndSp(expFor85 - player.getExp(), 0);
		player.giveAvailableSkills(true, true, true);
		applyBotOnlySkills(player, preset);
		player.setNoble(true);
		equipGear(player, preset);
		giveConsumables(player, preset);
		player.storeMe();
		return player;
	}

	public static void applyClassSkills(Player player)
	{
		if (player == null) return;
		final int learned = player.giveAvailableSkills(true, true, true);
		if (learned > 0)
			LOGGER.info("FakePlayersAI: class-skills " + player.getName() + ": learned " + learned + " new skills.");
	}

	/** Called only by roster/training bot creation or restoration, never by player login.
	 * Reapplied after Player.load; no class-tree edit and no downgrade of an enchanted skill. */
	public static void applyBotOnlySkills(Player player, BotClassPreset preset)
	{
		if (player == null || preset == null || player.getClient() != null
			|| preset.getClassId() != CLASS_CARDINAL || player.getPlayerClass().getId() != CLASS_CARDINAL
			|| player.getKnownSkill(CARDINAL_PROMINENCE_ID) != null) return;
		final SkillData data = SkillData.getInstance();
		final int level = data.getMaxLevel(CARDINAL_PROMINENCE_ID);
		final Skill skill = level > 0 ? data.getSkill(CARDINAL_PROMINENCE_ID, level) : null;
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: Cardinal Prominence template is missing; no skill granted.");
			return;
		}
		player.addSkill(skill, false);
		LOGGER.info("FakePlayersAI: bot-only " + player.getName() + ": Prominence " + skill.getLevel() + " granted.");
	}

	public static void applyCounterCelestial(Player player)
	{
		if (player == null) return;
		if (player.getKnownSkill(COUNTER_CELESTIAL_ID) != null) return;
		final Skill skill = SkillData.getInstance().getSkill(COUNTER_CELESTIAL_ID, 1);
		if (skill == null)
		{
			LOGGER.warning("FakePlayersAI: Counter Celestial skill " + COUNTER_CELESTIAL_ID + " not found.");
			return;
		}
		player.addSkill(skill, false);
		LOGGER.info("FakePlayersAI: universal-passive " + player.getName() + ": Counter Celestial (skill " + COUNTER_CELESTIAL_ID + ") granted.");
	}

	public static int applySkillEnchants(Player player)
	{
		if (player == null || player.getClient() != null) return 0;
		// Legacy core hook is dormant: no managed bot may retain the old fixed25% gate.
		org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PLAYERS.remove(player.getObjectId());
		final List<Skill> knownSkills = new ArrayList<>();
		for (Skill skill : player.getAllSkills())
			if (skill != null) knownSkills.add(skill);
		int enchanted = 0, skipped = 0, duelCount = 0, penaltyCount = 0, chanceCount = 0;
		for (Skill skill : knownSkills)
		{
			final int skillId = skill.getId();
			final EnchantSkillLearn learn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
			if (learn == null) continue;
			final Skill baseSkill = SkillData.getInstance().getSkill(skillId, learn.getBaseLevel());
			if (baseSkill == null) continue;
			final boolean damage = baseSkill.isDamage();
			final double basePenalty = getMovePenalty(baseSkill);
			int duelLevel = 0, powerLevel = 0, chanceLevel = 0, penaltyLevel = 0;
			double penaltyLeft = basePenalty;
			final List<Integer> routes = new ArrayList<>(learn.getAllRoutes());
			Collections.sort(routes);
			for (int route : routes)
			{
				final int maxLevel = getRouteMaxLevel(learn, route);
				if (maxLevel == 0) continue;
				final Skill routeSkill = SkillData.getInstance().getSkill(skillId, maxLevel);
				if (routeSkill == null) continue;
				if (basePenalty > 0.0)
				{
					final double routePenalty = getMovePenalty(routeSkill);
					if ((routePenalty < basePenalty) && (routePenalty < penaltyLeft)) { penaltyLeft = routePenalty; penaltyLevel = maxLevel; }
				}
				final String label = getRouteLabel(baseSkill, routeSkill, route);
				if (label.equals("Duel") || (!baseSkill.hasEffects(EffectScope.PVP) && routeSkill.hasEffects(EffectScope.PVP)))
				{
					if (duelLevel == 0) duelLevel = maxLevel;
				}
				else if (label.equals("Power") && (powerLevel == 0)) powerLevel = maxLevel;
				else if (label.equals("Chance") && (chanceLevel == 0)) chanceLevel = maxLevel;
				else if (damage && (powerLevel == 0) && label.startsWith("Enchant ")) powerLevel = maxLevel;
			}
			final int targetLevel;
			final int debuffChance = debuffChanceLevel(baseSkill, learn);
			if (debuffChance > 0) targetLevel = debuffChance;
			else if (damage) targetLevel = (duelLevel > 0) ? duelLevel : ((powerLevel > 0) ? powerLevel : chanceLevel);
			else if (basePenalty > 0.0) targetLevel = penaltyLevel;
			else targetLevel = (powerLevel > 0) ? powerLevel : chanceLevel;
			if (targetLevel == 0) { skipped++; continue; }
			if (player.getSkillLevel(skillId) == targetLevel) continue;
			final Skill targetSkill = SkillData.getInstance().getSkill(skillId, targetLevel);
			if (targetSkill == null) continue;
			player.addSkill(targetSkill, true);
			enchanted++;
			if (debuffChance > 0) chanceCount++;
			else if (damage && (targetLevel == duelLevel)) duelCount++;
			else if (!damage && (targetLevel == penaltyLevel)) penaltyCount++;
		}
		BotRecoveryRules.applyPhoenixSacrifice(player);
		CardinalTrance.apply(player);
		LOGGER.info("FakePlayersAI: skill-ench " + player.getName() + ": enchanted " + enchanted + " skills (" + duelCount + " damage->Duel, " + chanceCount + " debuff->Chance, " + penaltyCount + " self-restricting->Decrease Penalty), " + skipped + " unenchanted.");
		return enchanted;
	}

	/** Chance wins over damage/Duel for harmful skills, including damage+control.
	 * Only real loaded routes up to the native maximum (+30 or +15); no fake levels. */
	static int debuffChanceLevel(Skill base, EnchantSkillLearn learn)
	{
		if (base == null || learn == null || base.isPassive() || base.getTargetType() == TargetType.SELF
			|| !(base.isDebuff() || base.hasNegativeEffect())) return 0;
		final List<Integer> routes = new ArrayList<>(learn.getAllRoutes());
		Collections.sort(routes);
		for (int route : routes)
		{
			int level = getRouteMaxLevel(learn, route);
			Skill candidate = level == 0 ? null : SkillData.getInstance().getSkill(base.getId(), level);
			if (candidate != null && getRouteLabel(base, candidate, route).equals("Chance")
				&& candidate.getActivateRate() > base.getActivateRate()) return level;
		}
		return 0;
	}

	private static int getRouteMaxLevel(EnchantSkillLearn learn, int route)
	{
		for (int i = 1; i <= MAX_SKILL_ENCHANT_LEVEL; i++)
			if (learn.isMaxEnchant((route * 100) + i)) return (route * 100) + i;
		return 0;
	}

	private static String getRouteLabel(Skill baseSkill, Skill enchantedSkill, int route)
	{
		if ((enchantedSkill.getMpConsume() < baseSkill.getMpConsume()) || (enchantedSkill.getMpInitialConsume() < baseSkill.getMpInitialConsume())) return "Cost";
		if ((enchantedSkill.getElementPower() != baseSkill.getElementPower()) || (enchantedSkill.getElement() != baseSkill.getElement())) return "Element";
		if (enchantedSkill.getAbnormalType() != baseSkill.getAbnormalType()) return "Debuff";
		if (enchantedSkill.getActivateRate() != baseSkill.getActivateRate()) return "Chance";
		if (enchantedSkill.getAbnormalTime() != baseSkill.getAbnormalTime()) return "Time";
		if (enchantedSkill.getPower() != baseSkill.getPower()) return "Power";
		if (enchantedSkill.getPower(true, false) != baseSkill.getPower(true, false)) return "Duel";
		return "Enchant " + route;
	}

	private static double getMovePenalty(Skill skill)
	{
		final double general = scanMovePenalty(skill, EffectScope.GENERAL, skill.getTargetType() == TargetType.SELF);
		final double self = scanMovePenalty(skill, EffectScope.SELF, true);
		return Math.max(general, self);
	}

	private static double scanMovePenalty(Skill skill, EffectScope scope, boolean appliesToCaster)
	{
		if (!appliesToCaster || !skill.hasEffects(scope)) return 0.0;
		double penalty = 0.0;
		for (AbstractEffect effect : skill.getEffects(scope))
		{
			if (effect == null) continue;
			final String name = effect.getName();
			if ((name != null) && name.startsWith("Immobile")) { penalty = Math.max(penalty, 1.0); continue; }
			final List<FuncTemplate> funcs = effect.getFuncTemplates();
			if (funcs == null) continue;
			for (FuncTemplate template : funcs)
			{
				if ((template == null) || (template.getStat() != Stat.MOVE_SPEED)) continue;
				final double value = template.getValue();
				final int order = template.getOrder();
				if ((order == FUNC_ORDER_MUL) && (value < 1.0)) penalty = Math.max(penalty, 1.0 - value);
				else if ((order == FUNC_ORDER_SUB) && (value > 0.0)) penalty = Math.max(penalty, value / 100.0);
				else if ((order == FUNC_ORDER_ADD) && (value < 0.0)) penalty = Math.max(penalty, -value / 100.0);
			}
		}
		return penalty;
	}

	public static boolean enterWorld(Player player, int x, int y, int z)
	{
		if (player == null) return false;
		final int sz = GeoEngine.getInstance().getSpawnHeight(x, y, z);
		player.setOnlineStatus(true, false);
		player.spawnMe(x, y, sz);
		player.setOnlineStatus(true, true);
		player.restoreEffects();
		player.setRunning();
		player.updateAndBroadcastStatus();
		return true;
	}

	public static void applyAutoCombat(Player player, BotClassPreset preset)
	{
		if (player.isInOlympiadMode() && !OlympiadCombatGate.ready(player)) { OlympiadCombatGate.hold(player); return; }
		final BotSettings settings = BotManager.getSettings();
		player.setOfflinePlay(true);
		player.getAutoUseSettings().setSelfCastOnly(true);
		final AutoPlaySettingsHolder aps = player.getAutoPlaySettings();
		aps.setNextTargetMode(2);
		aps.setPickup(false);
		aps.setShortRange(false);
		aps.setRespectfulHunting(false);
		aps.setAutoPotionPercent(settings.getAutoPotionPercent());
		final AutoUseSettingsHolder aus = player.getAutoUseSettings();
		aus.getAutoSkills().clear();
		if (preset.isCaster())
			for (int skillId : buildMageRotation(preset.getRotation())) aus.getAutoSkills().add(skillId);
		else
			for (int skillId : preset.getRotation()) aus.getAutoSkills().add(skillId);
		// Escape Shackle is exclusively driven by BotTactics while rooted, even with overrides.
		aus.getAutoSkills().remove(Integer.valueOf(ESCAPE_SHACKLE_ID));
        aus.getAutoSkills().removeIf(TankClassMechanics::skill);
		aus.getAutoBuffs().clear();
		for (int skillId : preset.getBuffs()) if (skillId != ESCAPE_SHACKLE_ID && !TankClassMechanics.skill(skillId)) aus.getAutoBuffs().add(skillId);
		refreshAutoBuffs(player, preset);
		pruneRangedTankSupports(player, player.getTarget());
		aus.setAutoPotionItem(BotPresets.getPotionId());
		aus.getAutoSupplyItems().clear();
		if (!preset.isCaster() && (!player.isInOlympiadMode() || BotTactics.allowsBasicAttack(player, preset)))
			aus.getAutoActions().add(AUTO_ATTACK_ACTION);
		else
			aus.getAutoActions().remove(AUTO_ATTACK_ACTION);
		player.addAutoSoulShot(BotPresets.getFighterShotId());
		player.addAutoSoulShot(BLESSED_SPIRITSHOT_S);
		if (player.isInOlympiadMode()) trimOlympiadBannedSkillsFromAutoLists(player);
		if (player.isInOlympiadMode() && TacticalSettings.isEnabled() && preset.getClassId() != 131)
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(player);
			AutoUseTaskManager.getInstance().stopAutoUseTask(player);
			return;
		}
		if (!BotTactics.isOwned(player.getObjectId()))
		{
			OlympiadChase.startAutoPlayUnlessPursuing(player);
			AutoUseTaskManager.getInstance().startAutoUseTask(player);
		}
	}

	public static void stopAutoCombat(Player player)
	{
		BotTactics.forget(player.getObjectId());
		OlympiadChase.forget(player.getObjectId());
		player.setOfflinePlay(false);
		player.getAutoUseSettings().setSelfCastOnly(false);
		AutoPlayTaskManager.getInstance().stopAutoPlay(player);
		AutoUseTaskManager.getInstance().stopAutoUseTask(player);
	}

	public static void refreshAutoBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline()) return;
		pruneRangedTankSupports(player, player.getTarget());
		final Collection<Integer> autoBuffs = player.getAutoUseSettings().getAutoBuffs();
        player.getAutoUseSettings().getAutoSkills().removeIf(id -> !CardinalSurvival.skillAllowed(player,id));
        autoBuffs.removeIf(id -> !CardinalSurvival.skillAllowed(player,id));
		if (OlympiadChase.isPursuing(player.getObjectId()) || BotTactics.isOwned(player.getObjectId()))
		{
			autoBuffs.clear();
			return;
		}
		final int[] desired = preset.getBuffs();
		final boolean inOlympiad = player.isInOlympiadMode();
		final boolean activeMatch = inOlympiad && player.isOlympiadStart();
		if (inOlympiad && !activeMatch) { autoBuffs.clear(); return; }
		final boolean tankBot = BotManager.isTankBot(player);
		autoBuffs.removeIf(skillId -> skillId == ESCAPE_SHACKLE_ID || TankClassMechanics.skill(skillId) || !containsId(desired, skillId)
			|| isSelfBuffApplied(player, skillId)
			|| (inOlympiad && isOpenerOnlySkill(skillId))
			|| (inOlympiad && isOlympiadChantBanned(player, skillId))
			|| (activeMatch && isLongSelfBuff(player, skillId))
			|| (tankBot && isTankDefenseSkill(skillId)));
		for (int skillId : desired)
		{
			if (skillId == ESCAPE_SHACKLE_ID || TankClassMechanics.skill(skillId) || !CardinalSurvival.skillAllowed(player,skillId)) continue;
			if (inOlympiad && isOlympiadChantBanned(player, skillId)) continue;
			if (tankBot && isTankDefenseSkill(skillId)) continue;
			if (!autoBuffs.contains(skillId) && !isSelfBuffApplied(player, skillId) && (player.getKnownSkill(skillId) != null)
				&& !(inOlympiad && isOpenerOnlySkill(skillId))
				&& !(activeMatch && isLongSelfBuff(player, skillId))
				&& (!tankBot || tankMovementDefenseAllowed(player.getTarget(), player.getKnownSkill(skillId))))
				autoBuffs.add(skillId);
		}
	}

	public static void refreshTankDefenses(Player player, BotClassPreset preset, WorldObject opponent)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead() || !BotManager.isTankBot(player)) return;
		pruneRangedTankSupports(player, opponent);
		player.getAutoUseSettings().getAutoSkills().removeIf(id -> id == VENGEANCE_ID);
		final Collection<Integer> autoBuffs = player.getAutoUseSettings().getAutoBuffs();
        player.getAutoUseSettings().getAutoSkills().removeIf(id -> !CardinalSurvival.skillAllowed(player,id));
        autoBuffs.removeIf(id -> !CardinalSurvival.skillAllowed(player,id));
		if (OlympiadChase.isPursuing(player.getObjectId()) || BotTactics.isOwned(player.getObjectId())) { autoBuffs.clear(); return; }
		if (player.isInOlympiadMode() && !player.isOlympiadStart()) { autoBuffs.removeIf(BotSpawner::isTankDefenseSkill); return; }
		final boolean opponentMage = (opponent != null) && opponent.isCreature() && opponent.asCreature().isPlayer() && opponent.asCreature().asPlayer().isMageClass();
		final int[] desiredPool = opponentMage ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
		final boolean activeMatch = player.isInOlympiadMode() && player.isOlympiadStart();
		int keep = 0;
		for (int skillId : desiredPool)
			if ((keep == 0) && player.getEffectList().isAffectedBySkill(skillId)) keep = skillId;
		for (int skillId : TANK_DEFENSE_ALL)
			if ((skillId != keep) && player.getEffectList().isAffectedBySkill(skillId))
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skillId);
		if (keep != 0) { autoBuffs.removeIf(BotSpawner::isTankDefenseSkill); return; }
		int candidate = 0;
		for (int skillId : desiredPool)
		{
			if ((skillId != VENGEANCE_ID) && !(skillId == 760 && opponentMage && !preset.hasBuffsOverride() && !preset.hasRotationOverride())
				&& !containsId(preset.getBuffs(), skillId)) continue;
			final Skill skill = player.getKnownSkill(skillId);
			if (skill == null || !tankMovementDefenseAllowed(opponent, skill)) continue;
			if (activeMatch && isLongSelfBuff(player, skillId)) continue;
			if (player.isSkillDisabled(skill)) continue;
			candidate = skillId;
			break;
		}
		autoBuffs.removeIf(BotSpawner::isTankDefenseSkill);
		if ((candidate > 0) && !isSelfBuffApplied(player, candidate)) autoBuffs.add(candidate);
	}

	public static boolean isOpenerOnlySkill(int skillId)
	{
		for (int id : OPENER_ONLY_SKILLS)
			if (id == skillId) return true;
		return false;
	}

	private static boolean isLongSelfBuff(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		return (skill != null) && !isDominatorVictory(player, skillId) && (skill.getAbnormalTime() > OLYMPIAD_MAX_SELFBUFF_SECONDS);
	}

	private static boolean isSelfBuffApplied(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		if (skill == null) return false;
		if (player.getEffectList().isAffectedBySkill(skillId)) return true;
		return isChargeBuilder(skill) && (player.getCharges() >= CHARGE_WINDOW_MAX);
	}

	private static boolean isChargeBuilder(Skill skill)
	{
		if (!skill.hasEffects(EffectScope.GENERAL)) return false;
		for (AbstractEffect effect : skill.getEffects(EffectScope.GENERAL))
		{
			final String name = (effect != null) ? effect.getName() : null;
			if ((name != null) && name.startsWith("Focus") && !name.equals("FocusSouls")) return true;
		}
		return false;
	}

	private static boolean containsId(int[] ids, int id)
	{
		if (ids == null) return false;
		for (int i : ids)
			if (i == id) return true;
		return false;
	}

	public static void applyOlympiadBuffs(Player player)
	{
		if (player == null) return;
		int applied = 0;
		for (int[] buff : OLYMPIAD_BUFFS)
		{
			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null) { LOGGER.warning("FakePlayersAI: olympiad buff skill " + buff[0] + "/" + buff[1] + " not found."); continue; }
			skill.applyEffects(player, player);
			applied++;
		}
		LOGGER.info("FakePlayersAI: oly-buffs " + player.getName() + ": applied " + applied + "/" + OLYMPIAD_BUFFS.length + " olympiad buffs.");
	}

	public static int stripChantBuffs(Player player)
	{
		if (player == null) return 0;
		int removed = 0;
		for (int skillId : CHANT_OF_VICTORY_FAMILY)
			if (isOlympiadChantBanned(player, skillId) && player.getEffectList().isAffectedBySkill(skillId))
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skillId);
				removed++;
			}
		if (removed > 0) LOGGER.info("FakePlayersAI: oly-chant " + player.getName() + ": removed " + removed + " Chant-family buff(s).");
		return removed;
	}

	public static int[] getTacticalRotation(Player player, Player enemy)
	{
		BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(player));
		return preset == null ? new int[0] : java.util.Arrays.stream(preset.getRotation()).filter(id -> CardinalSurvival.skillAllowed(player,id)).boxed().sorted(java.util.Comparator.comparingInt(id -> RoleCombatPolicy.builder(id)?1:0)).mapToInt(Integer::intValue).toArray();
	}

	private static boolean isTacticalTank(Player player)
	{
		if (player == null) return false;
		final int id = BotManager.getBotClassId(player);
		return id == 90 || id == 91 || id == 106;
	}

	public static boolean isStrongTankDefenseSkill(int skillId)
	{
		return skillId == 110 || skillId == VENGEANCE_ID || skillId == 760;
	}

	private static boolean isRangedTankOpponent(WorldObject opponent)
	{
		if (opponent == null || !opponent.isCreature()) return false;
		final Creature enemy = opponent.asCreature();
		if (!enemy.isPlayer()) return enemy.getPhysicalAttackRange() >= 250;
		final Player player = enemy.asPlayer();
		final PlayerClass playerClass = player.getPlayerClass();
		if (playerClass.equalsOrChildOf(PlayerClass.HAWKEYE) || playerClass.equalsOrChildOf(PlayerClass.SILVER_RANGER)
			|| playerClass.equalsOrChildOf(PlayerClass.PHANTOM_RANGER) || playerClass.equalsOrChildOf(PlayerClass.ARBALESTER)) return true;
		final BotClassPreset preset = BotPresets.getPresetByClassId(player.getPlayerClass().getId());
		return !TacticsPolicy.meleeOpponent(preset != null, preset != null && preset.isCaster(),
			preset != null && (preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW || preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW),
			player.isMageClass(), player.getPhysicalAttackRange());
	}

	private static boolean isMovementRestrictingTankSupport(Skill skill)
	{
		if (skill == null) return false;
		if (skill.getId() == 110 || skill.getId() == VENGEANCE_ID) return true;
		for (EffectScope scope : new EffectScope[] { EffectScope.GENERAL, EffectScope.SELF, EffectScope.PVP })
		{
			final var effects = skill.getEffects(scope);
			if (effects != null) for (AbstractEffect effect : effects)
				if (effect != null && ("ImmobileBuff".equals(effect.getName()) || "Root".equals(effect.getName()))) return true;
		}
		return false;
	}

	private static boolean tankMovementDefenseAllowed(WorldObject opponent, Skill skill)
	{
		if (skill != null && skill.getId() == VENGEANCE_ID) return true;
		return !isRangedTankOpponent(opponent) || !isMovementRestrictingTankSupport(skill);
	}

	private static void pruneRangedTankSupports(Player player, WorldObject opponent)
	{
		if (!isTacticalTank(player) || !isRangedTankOpponent(opponent)) return;
		player.getAutoUseSettings().getAutoBuffs().removeIf(id -> !tankMovementDefenseAllowed(opponent, player.getKnownSkill(id)));
		player.getAutoUseSettings().getAutoSkills().removeIf(id -> {
			final Skill skill = player.getKnownSkill(id);
			final boolean selfSupport = skill != null && (isTankDefenseSkill(id) || skill.getTargetType() == TargetType.SELF || skill.hasEffects(EffectScope.SELF));
			return selfSupport && !tankMovementDefenseAllowed(opponent, skill);
		});
	}

	public static int[] getTacticalDefenses(Player player, Player enemy)
	{
		if (!isTacticalTank(player)) return new int[0];
		BotClassPreset preset = BotPresets.getPresetByClassId(BotManager.getBotClassId(player));
		if (preset == null) return new int[0];
		int[] pool = enemy != null && enemy.isMageClass() ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
		java.util.List<Integer> result = new java.util.ArrayList<>();
		for (int id : pool)
			if (player.getKnownSkill(id) != null && tankMovementDefenseAllowed(enemy, player.getKnownSkill(id))
				&& (id == VENGEANCE_ID || (!preset.hasBuffsOverride() && !preset.hasRotationOverride())
				|| containsId(preset.getBuffs(), id) || containsId(preset.getRotation(), id))) result.add(id);
		return result.stream().mapToInt(Integer::intValue).toArray();
	}

    /** Preparation only. The native toggle keeps its MP drain; no combat-time free rebuff. */
    public static void prepareSoulGuard(Player bot,Player enemy)
    {
        if(!RoleCombatPolicy.soulGuard(bot,enemy)||bot.isAlikeDead()||CardinalSurvival.live(bot,1283))return;
        Skill skill=bot.getKnownSkill(1283);
        if(skill!=null)skill.applyEffects(bot,bot);
    }

    static boolean tankMirrorDefenseAllowed(Player player,Player enemy,Skill skill)
    {
        if(player==null||player.getClient()!=null||enemy==null||!TankClassMechanics.tank(player)||!TankClassMechanics.tank(enemy)||!isStrongTankDefenseSkill(skill.getId()))return true;
        double reach=TacticsPolicy.tankApproachRange(player.getPhysicalAttackRange(),player.getTemplate().getCollisionRadius())+enemy.getTemplate().getCollisionRadius();
        return player.calculateDistance3D(enemy)<=reach&&GeoEngine.getInstance().canSeeTarget(player,enemy);
    }

	public static boolean tacticalSkillAllowed(Player player, Player enemy, Skill skill, boolean self)
	{
        if(!tankMirrorDefenseAllowed(player,enemy,skill)||!RoleCombatPolicy.candidate(player,enemy,skill,self)||!BotTactics.dominatorRootAllowed(player,enemy,skill.getId()))return false;
        if(player.getClient()==null&&TacticsPolicy.chargeMeleeClass(player.getPlayerClass().getId())&&RoleCombatPolicy.builder(skill.getId())&&!RoleCombatPolicy.needsCharge(player))return false;
        if(player.getClient()==null && !self && enemy!=null && enemy.isInvul())return false;
        if (!CardinalSurvival.skillAllowed(player,skill.getId())) return false;
        if (!BotTactics.dominatorStunAllowed(player, enemy, skill)) return false;
        if (skill.getId()==1230 && !CardinalSurvival.prominenceAllowed(player,enemy)) return false;
        if (player.getClient() == null && player.getPlayerClass().getId() == 97 && enemy != null && enemy.isSleeping() && !self && !SleepRetreat.wakeSkillAllowed(player,enemy,skill)) return false;
        if (enemy != null && !BluffRear.skillAllowed(player, enemy, skill)) return false;
		if (CardinalSurvival.active(player, enemy) && (!CardinalSurvival.allowed(skill.getId()) || (skill.getId() == 1398 || skill.getId() == 1230) && enemy.isSleeping() && !SleepRetreat.wakeSkillAllowed(player,enemy,skill))) return false;
        if (!TankClassMechanics.allowed(player, skill.getId()) || !TankClassMechanics.castAllowed(player, enemy, skill)) return false;
		if (skill.getId() == ESCAPE_SHACKLE_ID || isOpenerOnlySkill(skill.getId())) return false;
		if (player.isInOlympiadMode())
		{
			if (!player.isOlympiadStart() || isOlympiadChantBanned(player, skill.getId()) || skill.isDance()
				|| (self && isLongSelfBuff(player, skill.getId()) && !TankClassMechanics.skill(skill.getId()))) return false;
		}
		if (isTacticalTank(player) && self && !tankMovementDefenseAllowed(enemy, skill)) return false;
		if (isTacticalTank(player) && isTankDefenseSkill(skill.getId()))
		{
			final int[] pool = enemy != null && enemy.isMageClass() ? TANK_DEFENSE_MAGIC : TANK_DEFENSE_PHYSICAL;
			if (!containsId(pool, skill.getId())) return false;
			for (int id : TANK_DEFENSE_ALL)
				if (player.isAffectedBySkill(id) && (!isStrongTankDefenseSkill(skill.getId()) || isStrongTankDefenseSkill(id))) return false;
		}
		return true;
	}

	public static void trimOlympiadBannedSkillsFromAutoLists(Player player)
	{
		if (player == null) return;
		final AutoUseSettingsHolder aus = player.getAutoUseSettings();
        aus.getAutoBuffs().removeIf(id -> !CardinalSurvival.skillAllowed(player,id)||player.getPlayerClass().getId()==115&&id==1283);
        if(player.getClient()==null&&player.getPlayerClass().getId()==88)aus.getAutoSkills().removeIf(id -> id==5||id==6);
        aus.getAutoSkills().removeIf(id -> !CardinalSurvival.skillAllowed(player,id));
		aus.getAutoBuffs().removeIf(id -> isOlympiadChantBanned(player, id));
		aus.getAutoSkills().removeIf(id -> isOlympiadChantBanned(player, id));
		aus.getAutoBuffs().removeIf(BotSpawner::isOpenerOnlySkill);
		aus.getAutoSkills().removeIf(BotSpawner::isOpenerOnlySkill);
        aus.getAutoBuffs().removeIf(TankClassMechanics::skill);
        aus.getAutoSkills().removeIf(TankClassMechanics::skill);
		aus.getAutoBuffs().removeIf(id -> isDanceOrSong(player, id));
		aus.getAutoSkills().removeIf(id -> isDanceOrSong(player, id));
	}

	private static boolean isDanceOrSong(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		return (skill != null) && skill.isDance();
	}

	private static List<Integer> buildMageRotation(int[] rotation)
	{
		final List<Integer> attacks = new ArrayList<>();
		final List<Integer> curses = new ArrayList<>();
		for (int skillId : rotation)
			if (isMageCurse(skillId)) curses.add(skillId);
			else attacks.add(skillId);
		if (curses.isEmpty() || attacks.isEmpty()) return attacks.isEmpty() ? curses : attacks;
		final List<Integer> result = new ArrayList<>(10);
		int attackIndex = 0, curseIndex = 0;
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

	public static void applyTrainingOlympiadBuffs(Player player, boolean caster)
	{
		if (player == null || player.isAlikeDead()) return;
		applyOlympiadBuffs(player);
		for (int[] buff : getOlympiadBuffs(caster))
		{
			if (isOpenerOnlySkill(buff[0]) || isTankDefenseSkill(buff[0])) continue;
			if (player.getEffectList().isAffectedBySkill(buff[0])) continue;
			Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null) { LOGGER.warning("Training Olympiad buff unavailable: " + buff[0] + "/" + buff[1]); continue; }
			BuffInfo stacked = player.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
			if (stacked != null && stacked.getSkill().getId() != buff[0] && stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel()) continue;
			skill.applyEffects(player, player);
		}
		applyDominatorArenaBuff(player);
	}

	public static void applyTrainingClassBuffs(Player bot, BotClassPreset preset)
	{
		if (bot == null || preset == null || !bot.isOnline() || bot.isAlikeDead()
			|| !(TrainingArena.isTrainingBot(bot) || TrainingArena.isParticipant(bot))
			|| bot.getPlayerClass().getId() != preset.getClassId()) return;
		for (int id : trainingClassBuffIds(preset.getClassId())) applyOpeningSelfBuff(bot, id);
	}

	static int[] trainingClassBuffIds(int classId)
	{
		return switch (classId)
		{
			case 113 -> new int[] {176, 420};
			case 114 -> new int[] {420};
			default -> new int[0];
		};
	}

	public static boolean applyTrainingPreparationSelfBuffs(Player bot, BotClassPreset preset)
	{
		if (bot == null || preset == null || bot.getClient() != null || !bot.isOnline()
			|| !TrainingArena.isPreparingBot(bot) || bot.isAlikeDead() || bot.isCastingNow()
			|| bot.isCastingSimultaneouslyNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) return false;
		for (int id : preset.getBuffs())
		{
			Skill skill = bot.getKnownSkill(id);
			if (id==1283 || !trainingPreparationSkillAllowed(skill) || isSelfBuffApplied(bot, id) || bot.isSkillDisabled(skill)) continue;
			BuffInfo stacked = skill.getAbnormalType() == AbnormalType.NONE ? null
				: bot.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
			if (stacked != null && stacked.getSkill().getId() != id && stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel()) continue;
			if (bot.getCurrentMp() < bot.getStat().getMpInitialConsume(skill) + bot.getStat().getMpConsume(skill)
				|| bot.getCurrentHp() <= skill.getHpConsume()
				|| !skill.isStatic() && (skill.isMagic() ? bot.isMuted() : bot.isPhysicalMuted())
				|| bot.getCharges() < skill.getChargeConsumeCount()
				|| skill.getItemConsumeCount() > 0 && bot.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()
				|| !skill.checkCondition(bot, bot, false)) continue;
			bot.setTarget(bot);
			if (bot.useMagic(skill, true, true)) return true;
		}
		return false;
	}

	static boolean trainingPreparationSkillAllowed(Skill skill)
	{
		if (skill == null || skill.getId() == ESCAPE_SHACKLE_ID || TankClassMechanics.skill(skill.getId()) || skill.isPassive() || skill.isBlockedInOlympiad() || skill.isDance()
			|| skill.isSuicideAttack() || skill.isDebuff() || skill.hasNegativeEffect() || skill.isDamage()
			|| skill.nextActionIsAttack() || isOpenerOnlySkill(skill.getId()) || isTankDefenseSkill(skill.getId())
			|| isChantOfVictoryFamily(skill.getId())) return false;
		boolean enhancement = skill.isContinuous() || skill.isToggle() || isChargeBuilder(skill);
		if (!enhancement && skill.hasEffects(EffectScope.GENERAL))
			for (AbstractEffect effect : skill.getEffects(EffectScope.GENERAL))
				if (effect != null && "FocusSouls".equals(effect.getName())) { enhancement = true; break; }
		if (!enhancement) return false;
		return skill.getTargetType() == TargetType.SELF || skill.getTargetType() == TargetType.ONE;
	}

	public static void maintainFullBuffs(Player player, boolean caster)
	{
		if ((player == null) || !player.isOnline() || player.isAlikeDead()) return;
		final boolean olympiad = player.isInOlympiadMode();
		final boolean activeMatch = olympiad && player.isOlympiadStart();
		// Server buffs are applied once before combat only - block all re-buffing after match/training combat starts.
		if (activeMatch) return;
		if (!olympiad && TrainingArena.isParticipant(player)) return;
		final int[][] profile = caster
			? (olympiad ? getOlympiadBuffs(true) : FULL_BUFFS_MAGE)
			: (olympiad ? getOlympiadBuffs(false) : FULL_BUFFS_FIGHTER);
		int applied = 0;
		for (int[] buff : profile)
		{
			if (olympiad && (isOpenerOnlySkill(buff[0]) || isTankDefenseSkill(buff[0]))) continue;
			if (player.getEffectList().isAffectedBySkill(buff[0])) continue;
			final Skill skill = SkillData.getInstance().getSkill(buff[0], buff[1]);
			if (skill == null) { LOGGER.warning("FakePlayersAI: full buff skill " + buff[0] + "/" + buff[1] + " not found."); continue; }
			if (activeMatch && (skill.getAbnormalTime() > OLYMPIAD_MAX_SELFBUFF_SECONDS)) continue;
			final BuffInfo stacked = player.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
			if ((stacked != null) && (stacked.getSkill().getId() != buff[0]) && (stacked.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel())) continue;
			skill.applyEffects(player, player);
			applied++;
		}
		if (olympiad && !activeMatch) applyDominatorArenaBuff(player);
		if (applied > 0) LOGGER.info("FakePlayersAI: full-buffs " + player.getName() + ": applied " + applied + " missing buff(s).");
	}

	public static void maintainFullBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null)) return;
		maintainFullBuffs(player, preset.isCaster());
	}

	public static void maintainConsumables(Player player)
	{
		maintainConsumables(player, null);
	}

	public static void maintainConsumables(Player player, BotClassPreset preset)
	{
		if ((player == null) || !player.isOnline()) return;
		ensureShot(player, BotPresets.getFighterShotId());
		ensureShot(player, BLESSED_SPIRITSHOT_S);
		if ((preset != null) && preset.useArrows())
		{
			final int ammoId = preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW ? BotPresets.getBoltId() : BotPresets.getArrowId();
			if (ammoId > 0)
			{
				final int target = BotManager.getSettings().getArrowCount();
				final int have = (int) player.getInventory().getInventoryItemCount(ammoId, -1);
				if (have < target) player.getInventory().addItem(ItemProcessType.REWARD, ammoId, target - have, player, null);
			}
		}
		final int oreCount = (int) player.getInventory().getInventoryItemCount(SPIRIT_ORE_ID, -1);
		if (oreCount < SPIRIT_ORE_CAP)
			player.getInventory().addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, SPIRIT_ORE_CAP - oreCount, player, null);
		else if (oreCount > SPIRIT_ORE_CAP)
		{
			final Item ore = player.getInventory().getItemByItemId(SPIRIT_ORE_ID);
			if (ore != null) player.getInventory().destroyItem(ItemProcessType.DESTROY, ore, oreCount - SPIRIT_ORE_CAP, player, null);
		}
		if (player.getPlayerClass().getId() == CLASS_CARDINAL)
		{
			final long holyWater = player.getInventory().getInventoryItemCount(EINHASAD_HOLY_WATER_ID, -1);
			if (holyWater < CARDINAL_CLEANSE_REAGENT_CAP)
				player.getInventory().addItem(ItemProcessType.REWARD, EINHASAD_HOLY_WATER_ID, CARDINAL_CLEANSE_REAGENT_CAP - holyWater, player, null);
		}
	}

	private static void ensureShot(Player player, int itemId)
	{
		ensureShot(player, itemId, BotManager.getSettings().getSoulshotCount());
	}

	private static void ensureShot(Player player, int itemId, int targetCount)
	{
		if (!player.getAutoSoulShot().contains(itemId)) player.addAutoSoulShot(itemId);
		final int have = (int) player.getInventory().getInventoryItemCount(itemId, -1);
		if (have < targetCount) player.getInventory().addItem(ItemProcessType.REWARD, itemId, targetCount - have, player, null);
	}

	public static void applyMageOpeningAuras(Player player, boolean caster)
	{
		if ((player == null) || !caster) return;
		// Cardinal (class 97) has no Arcane Power skill - skip this aura for Cardinal.
		if (player.getPlayerClass().getId() == CLASS_CARDINAL) return;
		final int[][] auras = { { ARCANE_POWER_ID, 1 } };
		int applied = 0;
		for (int[] aura : auras)
		{
			final Skill skill = SkillData.getInstance().getSkill(aura[0], aura[1]);
			if (skill == null) { LOGGER.warning("FakePlayersAI: opening aura skill " + aura[0] + "/" + aura[1] + " not found."); continue; }
			skill.applyEffects(player, player);
			applied++;
		}
		LOGGER.info("FakePlayersAI: oly-auras " + player.getName() + ": applied " + applied + "/1 opening mage aura.");
	}

	public static void applyEnlightenmentOpener(Player player)
	{
		if ((player == null) || !player.isOnline() || player.isAlikeDead()) return;
		if (player.getEffectList().isAffectedBySkill(ENLIGHTENMENT_ID)) return;
		Skill skill = player.getKnownSkill(ENLIGHTENMENT_ID);
		if (skill == null) skill = SkillData.getInstance().getSkill(ENLIGHTENMENT_ID, 1);
		if (skill == null) { LOGGER.warning("FakePlayersAI: enlightenment opener skill " + ENLIGHTENMENT_ID + " not found."); return; }
		skill.applyEffects(player, player);
		LOGGER.info("FakePlayersAI: oly-openers " + player.getName() + ": " + skill.getName() + " applied.");
	}

	public static void applyMageOpeningAuras(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null)) return;
		applyMageOpeningAuras(player, preset.isCaster());
	}

	public static void applyPreCombatOpeners(Player player, BotClassPreset preset)
	{
		if (player == null || preset == null || player.isAlikeDead() || !player.isOnline()) return;
		applyOpeningSelfBuffs(player, preset);
		if (preset.isCaster()) { applyMageOpeningAuras(player, preset); applyEnlightenmentOpener(player); }
	}

	public static void applyOpeningSelfBuffs(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead()) return;
		applyOpeningSelfBuff(player, FINAL_SECRET_ID);
		final int opener;
		final int adventure;
		switch (preset.getClassId())
		{
			case CLASS_ADVENTURER: opener = MIRAGE_ID; adventure = EXCITING_ADVENTURE_ID; break;
			case CLASS_WIND_RIDER: opener = DODGE_ID; adventure = WIND_RIDING_ID; break;
			case CLASS_GHOST_HUNTER: opener = GHOST_WALKING_ID; adventure = 0; break;
			default: opener = 0; adventure = 0;
		}
		if (opener > 0) applyOpeningSelfBuff(player, opener);
		if (adventure > 0) applyOpeningSelfBuff(player, adventure);
		// Cardinal: Prayer (1307) applied as a pre-combat self-buff opener.
		if (preset.getClassId() == CLASS_CARDINAL) applyOpeningSelfBuff(player, CARDINAL_PRAYER_ID);
	}

	private static void applyOpeningSelfBuff(Player player, int skillId)
	{
		final Skill skill = player.getKnownSkill(skillId);
		if (skill == null) return;
		if (player.getEffectList().isAffectedBySkill(skillId)) return;
		skill.applyEffects(player, player);
		LOGGER.info("FakePlayersAI: oly-openers " + player.getName() + ": " + skill.getName() + " (skill " + skill.getId() + ") applied.");
	}

	public static int getOlympiadSummonSkillId(BotClassPreset preset)
	{
		if (preset == null) return 0;
		switch (preset.getClassId())
		{
			case CLASS_SOULTAKER: return SUMMON_CURSED_MAN_ID;
			case CLASS_HELL_KNIGHT: return SUMMON_DARK_PANTHER_ID;
			case CLASS_PHOENIX_KNIGHT: return SUMMON_IMPERIAL_PHOENIX_ID;
			default: return 0;
		}
	}

	public static void maintainOlympiadSummon(Player player, BotClassPreset preset, WorldObject target)
	{
		if ((player == null) || (preset == null) || !player.isOnline() || player.isAlikeDead()
			|| !player.isInOlympiadMode() || player.getClient() != null || !BotManager.isBot(player)) return;
		if (target != null && target.isPlayer()) BotTactics.tickSummonAttack(player, target.asPlayer(), true);
		final int summonSkillId = getOlympiadSummonSkillId(preset);
		if (summonSkillId == 0) return;
		// Never create here: a dead/dismissed servitor consumes this match's only summon.
		// Creation is authorized only by the validated, once-per-game PRE_START hook.
		if (summonSkillId == SUMMON_CURSED_MAN_ID)
		{
			if (!player.getEffectList().isAffectedBySkill(TRANSFER_PAIN_ID))
			{
				Skill toggle = player.getKnownSkill(TRANSFER_PAIN_ID);
				if (toggle == null) toggle = SkillData.getInstance().getSkill(TRANSFER_PAIN_ID, 5);
				if (toggle != null) { toggle.applyEffects(player, player); LOGGER.info("FakePlayersAI: oly-summon " + player.getName() + ": Transfer Pain enabled."); }
			}
		}
		else if ((summonSkillId == SUMMON_DARK_PANTHER_ID) || (summonSkillId == SUMMON_IMPERIAL_PHOENIX_ID))
			ensureShot(player, BEAST_SOULSHOT_ID, PET_SOULSHOT_COUNT);
		final int crystalId = (summonSkillId == SUMMON_IMPERIAL_PHOENIX_ID) ? CRYSTAL_A_ID : CRYSTAL_C_ID;
		final int crystals = (int) player.getInventory().getInventoryItemCount(crystalId, -1);
		if (crystals < SERVITOR_CRYSTAL_STOCK)
			player.getInventory().addItem(ItemProcessType.REWARD, crystalId, SERVITOR_CRYSTAL_STOCK - crystals, player, null);
	}

	public static void ensurePresetGear(Player player, BotClassPreset preset)
	{
		if ((player == null) || (preset == null) || !player.isOnline()) return;
		final BotSettings settings = BotManager.getSettings();
		final List<Integer> required = new ArrayList<>();
		for (int id : BotPresets.getArmorSet(preset, settings.isUseS84())) required.add(id);
		final int shield = BotPresets.getShieldId(preset, settings.isUseS84());
		if (shield > 0) required.add(shield);
		final int weapon = BotPresets.getWeaponId(preset, settings.isUseS84());
		if (weapon > 0) required.add(weapon);
		// Jewelry migration must not destroy/recreate the existing weapon or armor.
		ensurePresetJewelry(player);
		String reason = null;
		for (int itemId : required)
		{
			final Item item = player.getInventory().getItemByItemId(itemId);
			if ((item == null) || !item.isEquipped()) { reason = "missing/un-equipped item " + itemId; break; }
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
        // Existing persisted bots must adopt current settings, not only newly created items.
        reenchantGear(player);
	}

	/** Persistent five-slot preset. Reuse owned epic items; never reset armor/weapon to migrate jewels. */
	public static void ensurePresetJewelry(Player player)
	{
		if (player == null || player.getClient() != null) return;
		final int[] ids = BotPresets.getJewelryIds();
		final int enchant = BotManager.getSettings().getEnchantJewel();
		boolean correct = true;
		for (int id : ids)
		{
			Item item = player.getInventory().getPaperdollItemByItemId(id);
			if (item == null || item.getEnchantLevel() != enchant) { correct = false; break; }
		}
		if (correct) return;
		// Paperdoll listeners remove old jewelry stats normally; old items remain in inventory.
		for (Item item : new ArrayList<>(player.getInventory().getPaperdollItems()))
			if (item != null && isJewelry(item))
			{
				player.getInventory().unEquipItemInSlot(item.getLocationSlot());
				item.updateDatabase();
			}
		for (int id : ids)
		{
			Item item = player.getInventory().getItemByItemId(id);
			if (item == null) item = player.getInventory().addItem(ItemProcessType.REWARD, id, 1, player, null);
			if (item == null) throw new IllegalStateException("Missing epic jewelry template " + id);
			item.setEnchantLevel(enchant);
			player.getInventory().equipItem(item);
			item.updateDatabase();
			if (!item.isEquipped()) throw new IllegalStateException("Cannot equip epic jewelry " + id);
		}
		player.refreshOverloaded();
		player.updateAndBroadcastStatus();
	}

	public static void equipGear(Player player, BotClassPreset preset)
	{
		final BotSettings settings = BotManager.getSettings();
		final int enchantArmor = settings.getEnchantArmor();
		final int enchantWeapon = settings.getEnchantWeapon();
		final int enchantJewel = settings.getEnchantJewel();
		destroyOldGear(player);
		for (int itemId : BotPresets.getArmorSet(preset, settings.isUseS84())) giveAndEquip(player, itemId, 1, enchantArmor);
		final int shield = BotPresets.getShieldId(preset, settings.isUseS84());
		if (shield > 0) giveAndEquip(player, shield, 1, enchantArmor);
		final int weapon = BotPresets.getWeaponId(preset, settings.isUseS84());
		if (weapon > 0) giveAndEquip(player, weapon, 1, enchantWeapon);
		for (int jewel : BotPresets.getJewelryIds()) giveAndEquip(player, jewel, 1, enchantJewel);
		player.refreshOverloaded();
		player.updateAndBroadcastStatus();
	}

	public static int reenchantGear(Player player)
	{
		return reenchantGear(player, true, true, true);
	}

	public static int reenchantGear(Player player, boolean enchantWeaponItems, boolean enchantArmorItems)
	{
		return reenchantGear(player, enchantWeaponItems, enchantArmorItems, false);
	}

	public static int reenchantGear(Player player, boolean enchantWeaponItems, boolean enchantArmorItems, boolean enchantJewelItems)
	{
		if (player == null) return 0;
		final BotSettings settings = BotManager.getSettings();
		final int enchantArmor = settings.getEnchantArmor();
		final int enchantWeapon = settings.getEnchantWeapon();
		final int enchantJewel = settings.getEnchantJewel();
		int changed = 0;
		for (Item item : player.getInventory().getPaperdollItems())
		{
			if (item == null) continue;
			final int targetEnchant;
			if (isJewelry(item)) { if (!enchantJewelItems) continue; targetEnchant = enchantJewel; }
			else if (item.isWeapon()) { if (!enchantWeaponItems) continue; targetEnchant = enchantWeapon; }
			else if (item.isArmor()) { if (!enchantArmorItems) continue; targetEnchant = enchantArmor; }
			else continue;
			if (item.getEnchantLevel() == targetEnchant) continue;
			applyEnchantInPlace(player, item, targetEnchant);
			changed++;
		}
		if (changed > 0) player.updateAndBroadcastStatus();
		return changed;
	}

	private static boolean isJewelry(Item item)
	{
		switch (item.getTemplate().getBodyPart())
		{
			case L_EAR: case R_EAR: case LR_EAR:
			case L_FINGER: case R_FINGER: case LR_FINGER:
			case NECK: return true;
			default: return false;
		}
	}

	private static void applyEnchantInPlace(Player player, Item item, int enchant)
	{
		final Skill enchant4Skill = item.getTemplate().getEnchant4Skill();
		final boolean hadEnchant4Skill = (enchant4Skill != null) && (item.getEnchantLevel() >= 4);
		item.setEnchantLevel(enchant);
		item.updateDatabase();
		final boolean hasEnchant4Skill = (enchant4Skill != null) && (item.getEnchantLevel() >= 4);
		if ((enchant4Skill != null) && (hadEnchant4Skill != hasEnchant4Skill))
		{
			if (hasEnchant4Skill) player.addSkill(enchant4Skill, false);
			else player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
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
			if (item != null) player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
		}
	}

	private static void giveAndEquip(Player player, int itemId, int count, int enchant)
	{
		if (itemId <= 0) return;
		final Item item = player.getInventory().addItem(ItemProcessType.REWARD, itemId, count, player, null);
		if (item == null) { LOGGER.warning("FakePlayersAI: item " + itemId + " not found in ItemData."); return; }
		if (enchant > 0) item.setEnchantLevel(enchant);
		if (item.isEquipable()) player.getInventory().equipItem(item);
		item.updateDatabase(); // persist enchant and paperdoll slot, not only the initial item insert
	}

	private static void giveConsumables(Player player, BotClassPreset preset)
	{
		final BotSettings settings = BotManager.getSettings();
		player.getInventory().addItem(ItemProcessType.REWARD, BotPresets.getFighterShotId(), settings.getSoulshotCount(), player, null);
		player.getInventory().addItem(ItemProcessType.REWARD, BLESSED_SPIRITSHOT_S, settings.getSoulshotCount(), player, null);
		player.getInventory().addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, SPIRIT_ORE_CAP, player, null);
		if (preset.getClassId() == CLASS_CARDINAL)
			player.getInventory().addItem(ItemProcessType.REWARD, EINHASAD_HOLY_WATER_ID, CARDINAL_CLEANSE_REAGENT_CAP, player, null);
		if (preset.useArrows())
		{
			final int ammoId = preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW ? BotPresets.getBoltId() : BotPresets.getArrowId();
			if (ammoId > 0) player.getInventory().addItem(ItemProcessType.REWARD, ammoId, settings.getArrowCount(), player, null);
		}
		final int potionId = BotPresets.getPotionId();
		if (potionId > 0) player.getInventory().addItem(ItemProcessType.REWARD, potionId, settings.getPotionCount(), player, null);
		final int cpPotionId = BotPresets.getCpPotionId();
		if (cpPotionId > 0) player.getInventory().addItem(ItemProcessType.REWARD, cpPotionId, settings.getPotionCount(), player, null);
	}

	public static String generateName(boolean female)
	{
		final List<String> names = BotPresets.getNames(female);
		for (int attempt = 0; attempt < 100; attempt++)
		{
			final String base = names.get(Rnd.get(names.size()));
			final String name = attempt == 0 ? base : base + Rnd.get(10, 99);
			if (!CharInfoTable.getInstance().doesCharNameExist(name)) return name;
		}
		return "Bot" + Rnd.get(1000000, 9999999);
	}

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
			points.add(new int[] { x, y, settings.getSpawnZ() });
		}
		return points;
	}
}
