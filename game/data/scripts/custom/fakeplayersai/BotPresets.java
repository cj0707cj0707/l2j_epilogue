/*
 * Fake Players AI - static data: gear tables (verified from datapack), class presets,
 * skill rotations, name lists, override persistence.
 */
package custom.fakeplayersai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * @author FakePlayersAI
 */
public class BotPresets
{
	private static final Logger LOGGER = Logger.getLogger(BotPresets.class.getName());
	
	public static final String OVERRIDES_PATH = "config/Custom/FakePlayersAIOverrides.ini";
	
	// ------------------------------------------------------------------
	// Gear tables (IDs verified against game/data/stats/items/*.xml)
	// ------------------------------------------------------------------
	
	// Armor sets: {chest, legs, helm, gloves, boots}; index = GearKind ordinal (0 heavy, 1 light, 2 robe).
	private static final int[][] DYNASTY_SETS =
	{
		{
			9416, 9421, 9422, 9423, 9424
		},
		{
			9425, 9428, 9429, 9430, 9431
		},
		{
			9432, 9437, 9438, 9439, 9440
		}
	};
	
	private static final int[][] VESPER_SETS =
	{
		{
			13432, 13438, 13137, 13439, 13440
		},
		{
			13433, 13441, 13138, 13442, 13443
		},
		{
			13434, 13444, 13139, 13445, 13446
		}
	};
	
	private static final int DYNASTY_SHIELD = 9441;
	private static final int VESPER_SHIELD = 13471;
	
	private static final int[] DYNASTY_JEWELS =
	{
		9457, 9455, 9456 // ring, earring, necklace
	};
	
	private static final int[] VESPER_JEWELS =
	{
		14165, 14163, 14164
	};
	
	// Weapons per WeaponKind: {vesper, dynasty}.
	private static final int[][] WEAPONS =
	{
		{
			13457, 9442
		}, // SWORD (one-handed)
		{
			13464, 10253
		}, // TWO_HAND (two-handed blunt/sword)
		{
			13462, 9447
		}, // POLE
		{
			13460, 9446
		}, // DAGGER
		{
			13467, 9445
		}, // BOW
		{
			52, 10004
		}, // DUAL (Vesper Dual Sword is id 52 in this pack)
		{
			13461, 9450
		}, // FIST
		{
			13468, 9376
		}, // RAPIER (Kamael)
		{
			13470, 9380
		}, // ANCIENT (Kamael)
		{
			13469, 9384
		}, // CROSSBOW (Kamael)
		{
			13465, 9449
		} // MAGIC (one-handed magic weapon)
	};
	
	private static final int SOULSHOT_S = 1467;
	private static final int SPIRITSHOT_S = 2514;
	private static final int BLESSED_SPIRITSHOT_S = 3952;
	private static final int ARROW_S = 1345;
	private static final int BOLT_S = 9637;
	private static final int POTION_HEAL = 1539; // Greater Healing Potion
	private static final int POTION_CP = 5592; // Greater CP Potion
	
	// ------------------------------------------------------------------
	// Names
	// ------------------------------------------------------------------
	
	private static final String[] MALE_NAMES =
	{
		"Kain", "Darius", "Varen", "Mordekai", "Talon", "Grimm", "Ragnar", "Theron", "Balin", "Corvin",
		"Doran", "Ektor", "Fennik", "Garrik", "Haldor", "Ivar", "Jarek", "Keldor", "Lukan", "Merrick",
		"Nero", "Orin", "Perrin", "Quorin", "Ravan", "Sten", "Torgal", "Ulric", "Vane", "Wulfric",
		"Xandor", "Yorik", "Zarek", "Aldric", "Bors", "Cedric", "Draven", "Edric", "Falken", "Gorm"
	};
	
	private static final String[] FEMALE_NAMES =
	{
		"Lyra", "Selene", "Aria", "Nyx", "Iris", "Vela", "Kira", "Sasha", "Elara", "Mira",
		"Tessa", "Runa", "Freya", "Astrid", "Ingrid", "Sigrun", "Alva", "Brynn", "Cleo", "Dahlia",
		"Esme", "Fiora", "Gilda", "Hanna", "Isolde", "Juna", "Katja", "Lena", "Merle", "Nadia",
		"Oona", "Petra", "Rhea", "Sanna", "Thea", "Ulla", "Vera", "Wanda", "Yara", "Zola"
	};
	
	// ------------------------------------------------------------------
	// Class presets (classIds verified against data/stats/players/classList.xml)
	// The roster is exactly the olympiad bot classes (18): every preset below is
	// spawned by spawnAll()/getNextPreset() and auto-registers for the olympiad.
	// ------------------------------------------------------------------
	
	private static final Map<Integer, BotClassPreset> PRESETS = new TreeMap<>();
	private static int _presetCycle = 0;
	
	private static void addPreset(int classId, String name, BotClassPreset.GearKind gear, BotClassPreset.WeaponKind weapon, boolean female, boolean arrows, boolean spiritshot, int[] rotation, int[] buffs)
	{
		PRESETS.put(classId, new BotClassPreset(classId, name, gear, weapon, female, arrows, spiritshot, rotation, buffs));
	}
	
	static
	{
		// classId, name, gear, weapon, female, arrows, spiritshot, rotation, selfBuffs
		// Rotations/buffs verified against data/stats/players/skillTrees + skills XMLs (see _botdata/classes_skills.txt).
		// Adaptations: taunts/Hide/Snipe/Vanguard/traps excluded from rotation; self-casts and charge-builders go to buffs.
		addPreset(88, "Duelist", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DUAL, false, false, false,
			new int[]
			{
				// Charge rules of 16.09.2026: the sonic skills require charges again (the
				// datapack chargeConsume/Charges requirements are restored) and the charge
				// builder Sonic Focus (8) is part of the rotation - the rotation itself
				// rebuilds the charges it spends. The charge LEVEL (4..7) is maintained by
				// the combat driver (BotManager CHARGE_MIN/CHARGE_MAX).
				261, 5, 345, 6, 7, 9, 775, 8
			},
			new int[]
			{
				// The charge builders (Sonic Focus 8, Maximum Sonic Focus 919) moved out of
				// the self-buff set: 8 is a rotation skill now, 919 filled the charges up to
				// the retail cap of 8 - above the admin charge window (4..7).
				297, 78, 917, 440
			});
		addPreset(90, "Phoenix Knight", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.SWORD, false, false, false,
			new int[]
			{
				92, 353, 400, 49, 70, 403, 404
			},
			new int[]
			{
				322, 335, 341, 262, 438
			});
		addPreset(91, "Hell Knight", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.SWORD, false, false, false,
			new int[]
			{
				92, 353, 342, 401, 65, 127, 403, 763, 762, 70
			},
			new int[]
			{
				322, 335, 761, 439
			});
		addPreset(92, "Sagittarius", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.BOW, false, true, false,
			new int[]
			{
				101, 354, 19, 343, 924, 771, 24
			},
			new int[]
			{
				99, 131, 111, 415, 416, 303, 417, 324
			});
		addPreset(93, "Adventurer", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DAGGER, false, false, false,
			new int[]
			{
				// Dagger rotation: it must ALWAYS open with Shadow Step (821 - teleport behind
				// the target) followed by Backstab (30, cast range 40 - the teleport puts the
				// dagger exactly where the backstab lands from). Bluff (358) stays after them:
				// its 9 s stun is the follow-up opener. Trick (11) and Switch (12) remain
				// excluded: their TargetCancel effect produced mutual idle loops in bot fights.
				821, 30, 358, 531, 263, 344, 409, 96, 412
			},
			new int[]
			{
				// The class evasion opener (Mirage 445) is NOT an auto-buff: it is applied
				// by the opening self-enhancement burst (BotSpawner.applyOpeningSelfBuffs)
				// exactly 3 seconds after the olympiad gates open, together with Exciting
				// Adventure (768) - casting it during the countdown wastes its short
				// duration before the fight even begins.
				768, 356, 357, 111, 312, 4
			});
		addPreset(94, "Archmage", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// Mage rotation: main attack skills (whitelist) + curses. BotSpawner.buildMageRotation()
				// spreads the allowed set over the cycle as 80% attacks / 20% curses.
				1230, 1339, 1451, 1083, 1338, 1056, 1169, 1069, 1160
			},
			new int[]
			{
				337, 1532, 1492, 1078, 1040, 1157, 1216
			});
		addPreset(95, "Soultaker", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				1343, 1263, 1064, 1170, 1069, 1222, 1337
			},
			new int[]
			{
				337, 1532, 1078, 1157, 1387
			});
		addPreset(101, "Wind Rider", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DAGGER, false, false, false,
			new int[]
			{
				// See the Adventurer preset: the rotation opens with Shadow Step (821) +
				// Backstab (30); Trick/Switch/Charm (target manipulation) stay excluded -
				// they freeze the bot fights instead of driving them.
				821, 30, 358, 321, 263, 344, 531, 96
			},
			new int[]
			{
				// Dodge (446) is opener-burst-only (see the Adventurer preset comment):
				// applied 3 seconds after the gates open together with Wind Riding (769).
				769, 355, 356, 410, 111, 312, 230
			});
		addPreset(102, "Moonlight Sentinel", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.BOW, true, true, false,
			new int[]
			{
				101, 354, 19, 343, 924, 772, 24, 369
			},
			new int[]
			{
				413, 111, 415, 416, 303, 123, 324
			});
		addPreset(103, "Mystic Muse", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				1235, 1340, 1453, 1239, 1071, 1338, 1056, 1069
			},
			new int[]
			{
				337, 1532, 1493, 1078, 1047, 1286, 1238
			});
		addPreset(106, "Shillien Templar", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.SWORD, false, false, false,
			new int[]
			{
				352, 342, 401, 402, 279, 70, 289, 122, 115, 105, 129, 2
			},
			new int[]
			{
				322, 335, 788, 527
			});
		addPreset(108, "Ghost Hunter", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DAGGER, false, false, false,
			new int[]
			{
				// See the Adventurer preset: the rotation opens with Shadow Step (821) +
				// Backstab (30); Trick (11) and Confusion (2) stay excluded - target-cancel /
				// crowd-freeze skills do not belong in a bot rotation.
				821, 30, 358, 321, 263, 344, 531, 96, 122, 115, 105, 129
			},
			new int[]
			{
				// Ghost Walking (770) is opener-burst-only (see the Adventurer preset comment):
				// applied 3 seconds after the gates open.
				355, 357, 410, 111, 447, 312
			});
		addPreset(109, "Ghost Sentinel", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.BOW, true, true, false,
			new int[]
			{
				101, 354, 19, 343, 924, 314, 773, 369, 115, 122, 105
			},
			new int[]
			{
				414, 415, 303, 417, 111, 312, 324
			});
		addPreset(110, "Storm Screamer", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				1239, 1341, 1456, 1074, 1338, 1064, 1206, 1069
			},
			new int[]
			{
				337, 1532, 1494, 1287, 1078, 1157, 1216
			});
		addPreset(113, "Titan", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.TWO_HAND, false, false, false,
			new int[]
			{
				260, 362, 315, 190, 777, 36, 347, 320
			},
			new int[]
			{
				176, 94, 139, 420, 121, 536, 917
			});
		addPreset(114, "Grand Khavatari", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.FIST, false, false, false,
			new int[]
			{
				// Charge rules of 16.09.2026: the force skills require charges again (the
				// datapack chargeConsume/Charges requirements are restored) and the charge
				// builder Focused Force (50) is part of the rotation - the rotation itself
				// rebuilds the charges it spends. The charge LEVEL (4..7) is maintained by
				// the combat driver (BotManager CHARGE_MIN/CHARGE_MAX).
				281, 120, 776, 284, 81, 280, 54, 346, 17, 35, 95, 50
			},
			new int[]
			{
				// The charge builders (Focused Force 50, Maximum Focus Force 918) moved out
				// of the self-buff set: 50 is a rotation skill now, 918 filled the charges
				// up to the retail cap of 8 - above the admin charge window (4..7).
				222, 424, 420, 917, 83
			});
		addPreset(115, "Dominator", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// No whitelisted nukes in the Overlord line: the damage drains (Steal Essence,
				// Life Drain) fill the 80% attack share, the Seals/mind curses the 20% share.
				1245, 1090, 1367, 1097, 1092, 1105, 1246, 1247, 1248, 1208, 1099, 1104
			},
			new int[]
			{
				// Enlightenment of the Dominator is the WIZARD variant (1532, +40% M.Atk),
				// the same buff every other caster class learns (datapack change 03.10.2026:
				// the Dominator left the Healer variant 1533, +10% M.Atk +40% Heal).
				337, 1532
			});
		// Doombringer (Kamael) wears LIGHT: Vesper heavy/robe sets and shields carry the
		// condition races="HUMAN,ELF,DARK_ELF,ORC,DWARF" (no Kamael) in the datapack, and the
		// Kamael class line masters Light Armor only. The core unequips condition-failing gear
		// at the olympiad arena entry (OlympiadGame.removals() -> checkItemRestriction()), which
		// is why the heavy-equipped Doombringer arrived at the olympiad without armor.
		addPreset(131, "Doombringer", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.ANCIENT, false, false, false,
			new int[]
			{
				494, 793, 484, 495, 493, 526, 497, 498, 477, 485, 501, 1435
			},
			new int[]
			{
				500, 482, 499, 625, 502, 917
			});
	}
	
	/** Classes whose skills consume Cursed Bones (item 2508). */
	public static final int[] CLASSES_WITH_CURSED_BONES =
	{
		95, 110
	};
	
	/** Kamael soul classes - skills consume souls, Soul Gathering consumes Spirit Ores (item 3031). */
	public static final int[] KAMAEL_SOUL_CLASSES =
	{
		131, 132, 133, 134
	};
	
	public static boolean usesCursedBones(int classId)
	{
		for (int id : CLASSES_WITH_CURSED_BONES)
		{
			if (id == classId)
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean isKamaelSoulClass(int classId)
	{
		for (int id : KAMAEL_SOUL_CLASSES)
		{
			if (id == classId)
			{
				return true;
			}
		}
		return false;
	}
	
	// ------------------------------------------------------------------
	// Overrides (persisted via //bot menu)
	// ------------------------------------------------------------------
	
	private static final Map<Integer, String> ROTATION_OVERRIDES = new HashMap<>();
	private static final Map<Integer, String> BUFF_OVERRIDES = new HashMap<>();
	private static final Map<Integer, Integer> WEAPON_OVERRIDES = new HashMap<>();
	
	public static void loadOverrides()
	{
		ROTATION_OVERRIDES.clear();
		BUFF_OVERRIDES.clear();
		WEAPON_OVERRIDES.clear();
		
		final File file = new File(OVERRIDES_PATH);
		if (!file.exists())
		{
			return;
		}
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(file))
		{
			props.load(is);
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: overrides load failed: " + e.getMessage());
			return;
		}
		for (String key : props.stringPropertyNames())
		{
			final String value = props.getProperty(key).trim();
			if (key.startsWith("rotation."))
			{
				ROTATION_OVERRIDES.put(Integer.parseInt(key.substring(9)), value);
			}
			else if (key.startsWith("buffs."))
			{
				BUFF_OVERRIDES.put(Integer.parseInt(key.substring(6)), value);
			}
			else if (key.startsWith("weapon."))
			{
				WEAPON_OVERRIDES.put(Integer.parseInt(key.substring(7)), Integer.parseInt(value));
			}
		}
		applyOverridesToPresets();
		
		// Drop overrides of classes that are no longer in the roster (preset removed).
		boolean pruned = false;
		pruned |= ROTATION_OVERRIDES.keySet().removeIf(id -> !PRESETS.containsKey(id));
		pruned |= BUFF_OVERRIDES.keySet().removeIf(id -> !PRESETS.containsKey(id));
		pruned |= WEAPON_OVERRIDES.keySet().removeIf(id -> !PRESETS.containsKey(id));
		if (pruned)
		{
			saveOverrides();
			LOGGER.info("FakePlayersAI: overrides of removed classes pruned.");
		}
		LOGGER.info("FakePlayersAI: overrides loaded (" + ROTATION_OVERRIDES.size() + " rotations, " + BUFF_OVERRIDES.size() + " buff sets).");
	}
	
	public static void saveOverrides()
	{
		final Properties props = new Properties();
		for (Map.Entry<Integer, String> e : ROTATION_OVERRIDES.entrySet())
		{
			props.setProperty("rotation." + e.getKey(), e.getValue());
		}
		for (Map.Entry<Integer, String> e : BUFF_OVERRIDES.entrySet())
		{
			props.setProperty("buffs." + e.getKey(), e.getValue());
		}
		for (Map.Entry<Integer, Integer> e : WEAPON_OVERRIDES.entrySet())
		{
			props.setProperty("weapon." + e.getKey(), String.valueOf(e.getValue()));
		}
		try (OutputStream os = new FileOutputStream(OVERRIDES_PATH))
		{
			props.store(os, "# FakePlayersAI per-class overrides (rotations/buffs/weapons), managed by //bot");
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: overrides save failed: " + e.getMessage());
		}
	}
	
	private static void applyOverridesToPresets()
	{
		for (Map.Entry<Integer, String> e : ROTATION_OVERRIDES.entrySet())
		{
			final BotClassPreset preset = PRESETS.get(e.getKey());
			if (preset != null)
			{
				preset.setRotationOverride(parseIds(e.getValue()));
			}
		}
		for (Map.Entry<Integer, String> e : BUFF_OVERRIDES.entrySet())
		{
			final BotClassPreset preset = PRESETS.get(e.getKey());
			if (preset != null)
			{
				preset.setBuffsOverride(parseIds(e.getValue()));
			}
		}
		for (Map.Entry<Integer, Integer> e : WEAPON_OVERRIDES.entrySet())
		{
			final BotClassPreset preset = PRESETS.get(e.getKey());
			if (preset != null)
			{
				preset.setWeaponOverride(e.getValue());
			}
		}
	}
	
	private static int[] parseIds(String value)
	{
		final List<Integer> ids = new ArrayList<>();
		for (String part : value.split(","))
		{
			try
			{
				final int id = Integer.parseInt(part.trim());
				if (id > 0)
				{
					ids.add(id);
				}
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		final int[] result = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++)
		{
			result[i] = ids.get(i);
		}
		return result;
	}
	
	public static boolean rotationAdd(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if ((preset == null) || !getClassSkillPool(classId).contains(skillId))
		{
			return false;
		}
		final int[] rotation = preset.getRotation();
		for (int s : rotation)
		{
			if (s == skillId)
			{
				return false;
			}
		}
		final int[] updated = new int[rotation.length + 1];
		System.arraycopy(rotation, 0, updated, 0, rotation.length);
		updated[rotation.length] = skillId;
		if (sameIdSet(updated, preset.getDefaultRotation()))
		{
			// The set matches the preset default again - drop the override to restore the default order.
			preset.setRotationOverride(null);
			ROTATION_OVERRIDES.remove(classId);
		}
		else
		{
			preset.setRotationOverride(updated);
			ROTATION_OVERRIDES.put(classId, idsToString(updated));
		}
		return true;
	}
	
	public static boolean buffAdd(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if ((preset == null) || !isSelfBuffSkill(skillId) || !getClassFullSkillPool(classId).contains(skillId))
		{
			return false;
		}
		final int[] buffs = preset.getBuffs();
		for (int s : buffs)
		{
			if (s == skillId)
			{
				return false;
			}
		}
		final int[] updated = new int[buffs.length + 1];
		System.arraycopy(buffs, 0, updated, 0, buffs.length);
		updated[buffs.length] = skillId;
		if (sameIdSet(updated, preset.getDefaultBuffs()))
		{
			// The set matches the preset default again - drop the override.
			preset.setBuffsOverride(null);
			BUFF_OVERRIDES.remove(classId);
		}
		else
		{
			preset.setBuffsOverride(updated);
			BUFF_OVERRIDES.put(classId, idsToString(updated));
		}
		return true;
	}
	
	/**
	 * Removes a skill from the class self-buff set. An explicitly emptied buff set stays
	 * empty (persisted as an empty override) - bots of the class simply stop auto-buffing.
	 */
	public static boolean buffRemove(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset == null)
		{
			return false;
		}
		final int[] buffs = preset.getBuffs();
		boolean found = false;
		final List<Integer> updated = new ArrayList<>();
		for (int s : buffs)
		{
			if (s == skillId)
			{
				found = true;
			}
			else
			{
				updated.add(s);
			}
		}
		if (!found)
		{
			return false;
		}
		final int[] arr = updated.stream().mapToInt(Integer::intValue).toArray();
		preset.setBuffsOverride(arr);
		BUFF_OVERRIDES.put(classId, idsToString(arr)); // may be empty - keeps the "no buffs" state after restart
		return true;
	}
	
	private static boolean sameIdSet(int[] a, int[] b)
	{
		if ((a == null) || (b == null) || (a.length != b.length))
		{
			return false;
		}
		final Set<Integer> setB = new java.util.HashSet<>();
		for (int v : b)
		{
			setB.add(v);
		}
		for (int v : a)
		{
			if (!setB.contains(v))
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean rotationRemove(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset == null)
		{
			return false;
		}
		final int[] rotation = preset.getRotation();
		final List<Integer> updated = new ArrayList<>();
		for (int s : rotation)
		{
			if (s != skillId)
			{
				updated.add(s);
			}
		}
		preset.setRotationOverride(updated.stream().mapToInt(Integer::intValue).toArray());
		if (updated.isEmpty())
		{
			ROTATION_OVERRIDES.remove(classId);
		}
		else
		{
			ROTATION_OVERRIDES.put(classId, idsToString(updated.stream().mapToInt(Integer::intValue).toArray()));
		}
		return true;
	}
	
	public static void rotationReset(int classId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setRotationOverride(null);
			ROTATION_OVERRIDES.remove(classId);
		}
	}
	
	public static void buffReset(int classId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setBuffsOverride(null);
			BUFF_OVERRIDES.remove(classId);
		}
	}
	
	public static void setWeaponOverride(int classId, int itemId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setWeaponOverride(itemId);
			if (itemId > 0)
			{
				WEAPON_OVERRIDES.put(classId, itemId);
			}
			else
			{
				WEAPON_OVERRIDES.remove(classId);
			}
		}
	}
	
	private static String idsToString(int[] ids)
	{
		final StringBuilder sb = new StringBuilder();
		for (int id : ids)
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(id);
		}
		return sb.toString();
	}
	
	// ------------------------------------------------------------------
	// Runtime helpers
	// ------------------------------------------------------------------
	
	private static Map<Integer, Set<Integer>> _skillPools = new HashMap<>();
	private static Map<Integer, Set<Integer>> _fullSkillPools = new HashMap<>();
	private static Map<Integer, String> _skillNames = new HashMap<>();
	
	/**
	 * @return all combat-relevant active skills the class can learn (from its skill tree).
	 */
	public static Set<Integer> getClassSkillPool(int classId)
	{
		Set<Integer> pool = _skillPools.get(classId);
		if (pool == null)
		{
			pool = new java.util.TreeSet<>();
			final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
			if (playerClass != null)
			{
				for (SkillLearn learn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values())
				{
					final Skill skill = SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel());
					if ((skill != null) && skill.isActive() && (skill.getTargetType() != TargetType.SELF))
					{
						pool.add(learn.getSkillId());
					}
				}
			}
			_skillPools.put(classId, pool);
		}
		return pool;
	}
	
	/**
	 * @return every active skill of the class lineage (self-cast buffs and toggles included,
	 *         common non-class skills excluded). This is the "full skill list" shown by //bot.
	 */
	public static Set<Integer> getClassFullSkillPool(int classId)
	{
		Set<Integer> pool = _fullSkillPools.get(classId);
		if (pool == null)
		{
			pool = new java.util.TreeSet<>();
			final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
			if (playerClass != null)
			{
				for (SkillLearn learn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values())
				{
					if (SkillTreeData.getInstance().getCommonSkill(learn.getSkillId(), learn.getSkillLevel()) != null)
					{
						continue; // common (non-class) skill
					}
					final Skill skill = SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel());
					if ((skill != null) && skill.isActive())
					{
						pool.add(learn.getSkillId());
					}
				}
			}
			_fullSkillPools.put(classId, pool);
		}
		return pool;
	}
	
	/**
	 * @return true when the skill is a self-cast active (usable as an automatic bot self-buff).
	 */
	public static boolean isSelfBuffSkill(int skillId)
	{
		Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
		if (skill == null)
		{
			skill = SkillData.getInstance().getSkill(skillId, 1);
		}
		return (skill != null) && skill.isActive() && (skill.getTargetType() == TargetType.SELF);
	}
	
	public static String getSkillName(int skillId)
	{
		String name = _skillNames.get(skillId);
		if (name == null)
		{
			Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
			if (skill == null)
			{
				skill = SkillData.getInstance().getSkill(skillId, 1);
			}
			name = skill != null ? skill.getName() : ("Skill " + skillId);
			_skillNames.put(skillId, name);
		}
		return name;
	}
	
	public static BotClassPreset getPresetByClassId(int classId)
	{
		return PRESETS.get(classId);
	}
	
	public static Collection<BotClassPreset> getPresets()
	{
		return Collections.unmodifiableCollection(PRESETS.values());
	}
	
	public static List<BotClassPreset> getPresetList()
	{
		return new ArrayList<>(PRESETS.values());
	}
	
	/**
	 * Round-robin over all presets so spawned bots are class-diverse.
	 */
	public static BotClassPreset getNextPreset()
	{
		final List<BotClassPreset> list = getPresetList();
		if (list.isEmpty())
		{
			return null;
		}
		_presetCycle = (_presetCycle + 1) % list.size();
		return list.get(_presetCycle);
	}
	
	public static int[] getArmorSet(BotClassPreset preset, boolean useS84)
	{
		final int[][] sets = useS84 ? VESPER_SETS : DYNASTY_SETS;
		return sets[preset.getGear().ordinal()];
	}
	
	public static int getShieldId(BotClassPreset preset, boolean useS84)
	{
		return preset.getWeaponKind() == BotClassPreset.WeaponKind.SWORD ? (useS84 ? VESPER_SHIELD : DYNASTY_SHIELD) : 0;
	}
	
	public static int getWeaponId(BotClassPreset preset, boolean useS84)
	{
		if (preset.getWeaponOverride() > 0)
		{
			return preset.getWeaponOverride();
		}
		final int[] pair = WEAPONS[preset.getWeaponKind().ordinal()];
		return useS84 ? pair[0] : pair[1];
	}
	
	public static int[] getJewelryIds()
	{
		// Depends on current global grade setting.
		return BotManager.getSettings().isUseS84() ? VESPER_JEWELS : DYNASTY_JEWELS;
	}
	
	public static int getShotId(BotClassPreset preset)
	{
		return preset.useSpiritshot() ? SPIRITSHOT_S : SOULSHOT_S;
	}

	/** Every bot always stocks and auto-uses the S-grade soulshot. */
	public static int getFighterShotId()
	{
		return SOULSHOT_S;
	}

	/** Every bot always stocks and auto-uses the S-grade blessed spiritshot. */
	public static int getBlessedSpiritshotId()
	{
		return BLESSED_SPIRITSHOT_S;
	}
	
	public static int getArrowId()
	{
		return ARROW_S;
	}
	
	public static int getBoltId()
	{
		return BOLT_S;
	}
	
	public static int getPotionId()
	{
		return POTION_HEAL;
	}
	
	public static int getCpPotionId()
	{
		return POTION_CP;
	}
	
	/**
	 * @return every gear item id that could have been equipped by bots (for safe re-gear cleanup).
	 */
	public static int[] getAllGearIds()
	{
		final List<Integer> ids = new ArrayList<>();
		for (int[] set : DYNASTY_SETS)
		{
			for (int id : set)
			{
				ids.add(id);
			}
		}
		for (int[] set : VESPER_SETS)
		{
			for (int id : set)
			{
				ids.add(id);
			}
		}
		ids.add(DYNASTY_SHIELD);
		ids.add(VESPER_SHIELD);
		for (int[] pair : WEAPONS)
		{
			ids.add(pair[0]);
			ids.add(pair[1]);
		}
		for (int id : DYNASTY_JEWELS)
		{
			ids.add(id);
		}
		for (int id : VESPER_JEWELS)
		{
			ids.add(id);
		}
		final int[] result = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++)
		{
			result[i] = ids.get(i);
		}
		return result;
	}
	
	public static List<String> getNames(boolean female)
	{
		final List<String> list = new ArrayList<>();
		for (String name : female ? FEMALE_NAMES : MALE_NAMES)
		{
			list.add(name);
		}
		return list;
	}
}
