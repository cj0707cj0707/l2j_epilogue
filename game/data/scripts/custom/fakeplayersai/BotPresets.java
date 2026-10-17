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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
	// ------------------------------------------------------------------
	
	private static final Map<Integer, BotClassPreset> PRESETS = new TreeMap<>();
	private static int _presetCycle = 0;
	
	private static void addPreset(int classId, String name, BotClassPreset.GearKind gear, BotClassPreset.WeaponKind weapon, boolean female, boolean arrows, boolean spiritshot, int[] rotation, int[] buffs)
	{
		PRESETS.put(classId, new BotClassPreset(classId, name, gear, weapon, female, arrows, spiritshot, rotation, buffs));
	}
	
	static
	{
		addPreset(88, "Duelist", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DUAL, false, false, false,
			new int[]
			{
				261, 5, 345, 6, 7, 9, 775, 8
			},
			new int[]
			{
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
				821, 30, 358, 531, 263, 344, 409, 96, 412
			},
			new int[]
			{
				768, 356, 357, 111, 312, 4
			});
		addPreset(94, "Archmage", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				1071, 1072, 1073, 369, 370, 376, 377
			},
			new int[]
			{
				1036, 1059, 1085, 786
			});
		addPreset(95, "Soultaker", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				366, 168, 167, 366, 745, 172, 364
			},
			new int[]
			{
				1036, 1059, 1085, 786
			});
		addPreset(96, "Mystic Muse", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				1225, 1226, 1222, 1223, 1069, 1227
			},
			new int[]
			{
				1218, 1219
			});
		addPreset(97, "Cardinal", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				1394, 1398, 1230, 1396
			},
			new int[]
			{
				1307, 1218
			});
		addPreset(103, "Doombringer", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DUAL, false, false, false,
			new int[]
			{
				263, 344, 345, 530, 531, 261, 775, 8, 833
			},
			new int[]
			{
				445, 768, 297, 440, 78
			});
		addPreset(105, "Soul Hound", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.RAPIER, false, false, false,
			new int[]
			{
				500, 482, 499, 625, 502
			},
			new int[]
			{
				497, 505, 507
			});
		addPreset(107, "Trickster", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.CROSSBOW, false, false, false,
			new int[]
			{
				542, 921, 544, 543, 547, 546, 548
			},
			new int[]
			{
				538, 549, 551
			});
		addPreset(108, "Inspector", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.ANCIENT, false, false, false,
			new int[]
			{
				577, 578, 587, 584, 579
			},
			new int[]
			{
				570, 576, 580
			});
		addPreset(110, "Storm Screamer", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				608, 609, 1097, 610, 614, 615
			},
			new int[]
			{
				1218, 1219
			});
		addPreset(111, "Dominator", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				1305, 617, 619, 618, 616, 1414
			},
			new int[]
			{
				1218, 623
			});
		addPreset(112, "Doomcryer", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				630, 631, 632, 633, 634, 635
			},
			new int[]
			{
				627, 625, 1218
			});
		addPreset(113, "Maestro", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.FIST, false, false, false,
			new int[]
			{
				176, 907, 660, 661, 663, 664, 420
			},
			new int[]
			{
				658, 659
			});
		addPreset(114, "Fortune Seeker", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.TWO_HAND, false, false, false,
			new int[]
			{
				176, 420, 670, 671, 672
			},
			new int[]
			{
				668, 669
			});
		addPreset(115, "Judicator", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				616, 617, 618, 619, 1305, 1414
			},
			new int[]
			{
				1218, 623
			});
		addPreset(116, "Titan", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.TWO_HAND, false, false, false,
			new int[]
			{
				689, 362, 363, 367, 690, 691
			},
			new int[]
			{
				686, 687, 688
			});
		addPreset(118, "Grand Khavatari", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.FIST, false, false, false,
			new int[]
			{
				695, 700, 703, 704, 705, 706
			},
			new int[]
			{
				692, 693, 697
			});
		addPreset(131, "Doombringer", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DUAL, false, false, false,
			new int[]
			{
				793, 263, 344, 345, 530, 531, 261, 775, 8, 833
			},
			new int[]
			{
				500, 482, 499, 625, 502, 917, 833
			});
	}
	
	// ------------------------------------------------------------------
	// Preset access
	// ------------------------------------------------------------------
	
	public static BotClassPreset getPresetByClassId(int classId)
	{
		return PRESETS.get(classId);
	}
	
	public static synchronized BotClassPreset getNextPreset()
	{
		if (PRESETS.isEmpty()) { return null; }
		List<BotClassPreset> list = new ArrayList<>(PRESETS.values());
		BotClassPreset preset = list.get(_presetCycle % list.size());
		_presetCycle++;
		return preset;
	}
	
	public static Collection<BotClassPreset> getAllPresets()
	{
		return Collections.unmodifiableCollection(PRESETS.values());
	}
	
	/**
	 * Returns all registered presets. Alias for {@link #getAllPresets()} used by AdminBot.
	 */
	public static Collection<BotClassPreset> getPresets()
	{
		return getAllPresets();
	}
	
	public static Set<Integer> getRegisteredClassIds()
	{
		return Collections.unmodifiableSet(PRESETS.keySet());
	}
	
	// ------------------------------------------------------------------
	// Skill helpers (used by AdminBot to build the skill-management page)
	// ------------------------------------------------------------------
	
	/**
	 * Returns the display name of a skill, or "Skill#id" when the skill is not found.
	 */
	public static String getSkillName(int skillId)
	{
		final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
		return skill != null ? skill.getName() : "Skill#" + skillId;
	}
	
	/**
	 * Returns the full set of skill IDs available to a class (from its skill tree)
	 * plus any skills currently in its rotation or buffs override.
	 * Used by AdminBot to display the "allow/forbid" skill management page.
	 */
	public static int[] getClassFullSkillPool(int classId)
	{
		final Set<Integer> ids = new LinkedHashSet<>();
		
		// Skills from the class skill tree.
		try
		{
			PlayerClass pc = null;
			for (PlayerClass c : PlayerClass.values())
			{
				if (c.getId() == classId)
				{
					pc = c;
					break;
				}
			}
			if (pc != null)
			{
				final Map<Long, SkillLearn> tree = SkillTreeData.getInstance().getClassSkillTree(pc);
				if (tree != null)
				{
					for (SkillLearn learn : tree.values())
					{
						ids.add(learn.getSkillId());
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("FakePlayersAI: getClassFullSkillPool failed for classId " + classId + ": " + e.getMessage());
		}
		
		// Always include the preset's current rotation and buffs so admin-added overrides appear.
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			for (int id : preset.getRotation())
			{
				ids.add(id);
			}
			for (int id : preset.getBuffs())
			{
				ids.add(id);
			}
		}
		
		final int[] result = new int[ids.size()];
		int i = 0;
		for (int id : ids)
		{
			result[i++] = id;
		}
		return result;
	}
	
	/**
	 * Returns true if a skill should be categorised as a self-buff (cast on self automatically)
	 * rather than an offensive rotation skill. Used by AdminBot to decide whether clicking
	 * "allow" adds the skill to buffs or rotation.
	 */
	public static boolean isSelfBuffSkill(int skillId)
	{
		try
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (skill == null)
			{
				return false;
			}
			return skill.getTargetType() == TargetType.SELF;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	// ------------------------------------------------------------------
	// Rotation / buff overrides (in-memory mutations, AdminBot API)
	// ------------------------------------------------------------------
	
	/**
	 * Adds a skill to the rotation override for the given class.
	 * The skill must be in the class full skill pool and must not already be present.
	 * @return true if the skill was added, false if invalid or already present.
	 */
	public static boolean rotationAdd(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if ((preset == null) || (skillId <= 0))
		{
			return false;
		}
		final int[] current = preset.getRotation();
		for (int id : current)
		{
			if (id == skillId)
			{
				return false; // already present
			}
		}
		// Validate: skill must exist in the class pool.
		boolean valid = false;
		for (int id : getClassFullSkillPool(classId))
		{
			if (id == skillId)
			{
				valid = true;
				break;
			}
		}
		if (!valid)
		{
			return false;
		}
		final int[] next = Arrays.copyOf(current, current.length + 1);
		next[current.length] = skillId;
		preset.setRotationOverride(next);
		return true;
	}
	
	/**
	 * Removes a skill from the rotation override for the given class.
	 * @return true if the skill was found and removed.
	 */
	public static boolean rotationRemove(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset == null)
		{
			return false;
		}
		final int[] current = preset.getRotation();
		int idx = -1;
		for (int i = 0; i < current.length; i++)
		{
			if (current[i] == skillId)
			{
				idx = i;
				break;
			}
		}
		if (idx < 0)
		{
			return false;
		}
		final int[] next = new int[current.length - 1];
		for (int i = 0, j = 0; i < current.length; i++)
		{
			if (i != idx)
			{
				next[j++] = current[i];
			}
		}
		// setRotationOverride converts empty -> null (falls back to preset default).
		preset.setRotationOverride(next.length == 0 ? null : next);
		return true;
	}
	
	/**
	 * Resets the rotation override for the given class to the preset default.
	 */
	public static void rotationReset(int classId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setRotationOverride(null);
		}
	}
	
	/**
	 * Adds a skill to the self-buff override for the given class.
	 * The skill must be a self-targeting skill and must not already be present.
	 * @return true if the skill was added.
	 */
	public static boolean buffAdd(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if ((preset == null) || (skillId <= 0))
		{
			return false;
		}
		if (!isSelfBuffSkill(skillId))
		{
			return false;
		}
		final int[] current = preset.getBuffs();
		for (int id : current)
		{
			if (id == skillId)
			{
				return false; // already present
			}
		}
		final int[] next = Arrays.copyOf(current, current.length + 1);
		next[current.length] = skillId;
		preset.setBuffsOverride(next);
		return true;
	}
	
	/**
	 * Removes a skill from the self-buff override for the given class.
	 * @return true if the skill was found and removed.
	 */
	public static boolean buffRemove(int classId, int skillId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset == null)
		{
			return false;
		}
		final int[] current = preset.getBuffs();
		int idx = -1;
		for (int i = 0; i < current.length; i++)
		{
			if (current[i] == skillId)
			{
				idx = i;
				break;
			}
		}
		if (idx < 0)
		{
			return false;
		}
		final int[] next = new int[current.length - 1];
		for (int i = 0, j = 0; i < current.length; i++)
		{
			if (i != idx)
			{
				next[j++] = current[i];
			}
		}
		// An empty buff array IS a valid override (explicitly no self-buffs for this class).
		preset.setBuffsOverride(next);
		return true;
	}
	
	/**
	 * Resets the self-buff override for the given class to the preset default.
	 */
	public static void buffReset(int classId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setBuffsOverride(null);
		}
	}
	
	/**
	 * Sets the weapon item-id override for the given class (persisted by the //bot menu).
	 * Pass 0 to clear the override and revert to the default weapon.
	 */
	public static void setWeaponOverride(int classId, int itemId)
	{
		final BotClassPreset preset = PRESETS.get(classId);
		if (preset != null)
		{
			preset.setWeaponOverride(itemId);
		}
	}
	
	// ------------------------------------------------------------------
	// Gear assignment — new API (individual per-slot methods)
	// ------------------------------------------------------------------
	
	public static int getArmorChest(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()][0];
	}
	
	public static int getArmorLegs(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()][1];
	}
	
	public static int getArmorHelm(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()][2];
	}
	
	public static int getArmorGloves(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()][3];
	}
	
	public static int getArmorBoots(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()][4];
	}
	
	public static int getWeapon(BotClassPreset preset, boolean vesper)
	{
		return WEAPONS[preset.getWeaponKind().ordinal()][vesper ? 0 : 1];
	}
	
	public static int getShield(boolean vesper)
	{
		return vesper ? VESPER_SHIELD : DYNASTY_SHIELD;
	}
	
	public static int[] getJewels(boolean vesper)
	{
		return vesper ? VESPER_JEWELS : DYNASTY_JEWELS;
	}
	
	public static int getAmmo(BotClassPreset preset)
	{
		return preset.useArrows() ? ARROW_S : preset.usesBolts() ? BOLT_S : 0;
	}
	
	public static int getShot(BotClassPreset preset)
	{
		return preset.useSpiritshot() ? BLESSED_SPIRITSHOT_S : SOULSHOT_S;
	}
	
	public static int getHealPotion()
	{
		return POTION_HEAL;
	}
	
	public static int getCpPotion()
	{
		return POTION_CP;
	}
	
	// ------------------------------------------------------------------
	// Gear assignment — compatibility API (old method names called by
	// BotSpawner.java, BotManager.java, TrainingArena.java)
	// ------------------------------------------------------------------
	
	public static int[] getArmorSet(BotClassPreset preset, boolean vesper)
	{
		return (vesper ? VESPER_SETS : DYNASTY_SETS)[preset.getGear().ordinal()].clone();
	}
	
	public static int getShieldId(BotClassPreset preset, boolean vesper)
	{
		switch (preset.getWeaponKind())
		{
			case SWORD:
				return getShield(vesper);
			default:
				return 0;
		}
	}
	
	public static int getWeaponId(BotClassPreset preset, boolean vesper)
	{
		if (preset.getWeaponOverride() > 0)
		{
			return preset.getWeaponOverride();
		}
		return getWeapon(preset, vesper);
	}
	
	public static int[] getJewelryIds()
	{
		return getJewels(BotManager.getSettings().isUseS84());
	}
	
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
		for (int id : DYNASTY_JEWELS)
		{
			ids.add(id);
		}
		for (int id : VESPER_JEWELS)
		{
			ids.add(id);
		}
		for (int[] weapon : WEAPONS)
		{
			for (int id : weapon)
			{
				ids.add(id);
			}
		}
		final int[] result = new int[ids.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = ids.get(i);
		}
		return result;
	}
	
	public static int getPotionId()
	{
		return POTION_HEAL;
	}
	
	public static int getCpPotionId()
	{
		return POTION_CP;
	}
	
	public static int getFighterShotId()
	{
		return SOULSHOT_S;
	}
	
	public static int getBlessedSpiritshotId()
	{
		return BLESSED_SPIRITSHOT_S;
	}
	
	public static int getBoltId()
	{
		return BOLT_S;
	}
	
	public static int getArrowId()
	{
		return ARROW_S;
	}
	
	public static List<String> getNames(boolean female)
	{
		return Arrays.asList(female ? FEMALE_NAMES : MALE_NAMES);
	}
	
	// ------------------------------------------------------------------
	// Override persistence (FakePlayersAIOverrides.ini)
	// ------------------------------------------------------------------
	
	/**
	 * Loads all per-class overrides from FakePlayersAIOverrides.ini and applies
	 * them to the corresponding presets in memory.
	 * Called once by BotManager.init() after the INI file is known to exist.
	 * Compatibility method; the single-key variant is {@link #loadOverride(int, String, String)}.
	 */
	public static void loadOverrides()
	{
		final File file = new File(OVERRIDES_PATH);
		if (!file.exists())
		{
			return;
		}
		final Properties props = new Properties();
		try (InputStream in = new FileInputStream(file))
		{
			props.load(in);
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not read " + OVERRIDES_PATH + ": " + e.getMessage());
			return;
		}
		for (String key : props.stringPropertyNames())
		{
			try
			{
				final int dot = key.indexOf('.');
				if (dot < 0)
				{
					continue;
				}
				final int classId = Integer.parseInt(key.substring(0, dot));
				final String subKey = key.substring(dot + 1);
				final BotClassPreset preset = PRESETS.get(classId);
				if (preset == null)
				{
					continue;
				}
				final String value = props.getProperty(key, "").trim();
				switch (subKey)
				{
					case "rotation":
					{
						if (value.isEmpty())
						{
							preset.setRotationOverride(null);
						}
						else
						{
							final String[] parts = value.split(",");
							final int[] ids = new int[parts.length];
							for (int i = 0; i < parts.length; i++)
							{
								ids[i] = Integer.parseInt(parts[i].trim());
							}
							preset.setRotationOverride(ids);
						}
						break;
					}
					case "buffs":
					{
						if (value.isEmpty())
						{
							preset.setBuffsOverride(new int[0]);
						}
						else
						{
							final String[] parts = value.split(",");
							final int[] ids = new int[parts.length];
							for (int i = 0; i < parts.length; i++)
							{
								ids[i] = Integer.parseInt(parts[i].trim());
							}
							preset.setBuffsOverride(ids);
						}
						break;
					}
					case "weapon":
					{
						preset.setWeaponOverride(Integer.parseInt(value));
						break;
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("FakePlayersAI: bad override entry '" + key + "': " + e.getMessage());
			}
		}
		LOGGER.info("FakePlayersAI: loaded overrides from " + OVERRIDES_PATH + ".");
	}
	
	/**
	 * Saves ALL current in-memory overrides (rotation, buffs, weapon) for every
	 * preset that has an active override to FakePlayersAIOverrides.ini.
	 * Replaces the entire file content; called by AdminBot after any mutation.
	 */
	public static void saveOverrides()
	{
		final Properties props = new Properties();
		for (BotClassPreset preset : PRESETS.values())
		{
			final int classId = preset.getClassId();
			if (preset.hasRotationOverride())
			{
				final int[] rotation = preset.getRotation();
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < rotation.length; i++)
				{
					if (i > 0)
					{
						sb.append(',');
					}
					sb.append(rotation[i]);
				}
				props.setProperty(classId + ".rotation", sb.toString());
			}
			if (preset.hasBuffsOverride())
			{
				final int[] buffs = preset.getBuffs();
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < buffs.length; i++)
				{
					if (i > 0)
					{
						sb.append(',');
					}
					sb.append(buffs[i]);
				}
				props.setProperty(classId + ".buffs", sb.toString());
			}
			if (preset.getWeaponOverride() > 0)
			{
				props.setProperty(classId + ".weapon", String.valueOf(preset.getWeaponOverride()));
			}
		}
		try
		{
			final File file = new File(OVERRIDES_PATH);
			file.getParentFile().mkdirs();
			try (OutputStream out = new FileOutputStream(file))
			{
				props.store(out, "FakePlayersAI runtime overrides");
			}
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not save overrides: " + e.getMessage());
		}
	}
	
	public static void saveOverride(int classId, String key, String value)
	{
		try
		{
			final File file = new File(OVERRIDES_PATH);
			final Properties props = new Properties();
			if (file.exists())
			{
				try (InputStream in = new FileInputStream(file))
				{
					props.load(in);
				}
			}
			props.setProperty(classId + "." + key, value);
			file.getParentFile().mkdirs();
			try (OutputStream out = new FileOutputStream(file))
			{
				props.store(out, "FakePlayersAI runtime overrides");
			}
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not save override " + classId + "." + key + ": " + e.getMessage());
		}
	}
	
	public static String loadOverride(int classId, String key, String defaultValue)
	{
		try
		{
			final File file = new File(OVERRIDES_PATH);
			if (!file.exists()) { return defaultValue; }
			final Properties props = new Properties();
			try (InputStream in = new FileInputStream(file))
			{
				props.load(in);
			}
			return props.getProperty(classId + "." + key, defaultValue);
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not load override " + classId + "." + key + ": " + e.getMessage());
			return defaultValue;
		}
	}
	
	// ------------------------------------------------------------------
	// Skill enchant
	// ------------------------------------------------------------------
	
	public static char getSkillEnchantRoute(int classId, int skillId)
	{
		if (classId == 97) { return 'p'; }
		return 'p';
	}
}
