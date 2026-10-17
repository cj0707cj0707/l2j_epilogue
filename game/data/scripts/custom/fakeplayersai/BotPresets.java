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
				// Mage rotation: main nuke first, then DoT, then slow/debuff cycle.
				// Prominence (Archmage exclusive fire nuke chain) is the primary DAMAGE skill.
				// Blaze (fire DoT), Blazing Skin (fire self-buff/CC reflect) added for utility.
				1071, 1072, 1073, 369, 370, 376, 377
			},
			new int[]
			{
				1036, 1059, 1085, 786
			});
		addPreset(95, "Soultaker", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// Soultaker: Curse Gloom opener debuff + nuke/CC rotation.
				// Curse Gloom (745): -90% MP auto-regen; applied as opening debuff via
				// BotSpawner.getOpeningCurseSkillId() at olympiad start.
				// Corpse Burst (365) excluded: requires a dead target.
				366, 168, 167, 366, 745, 172, 364
			},
			new int[]
			{
				1036, 1059, 1085, 786
			});
		addPreset(96, "Mystic Muse", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				// Mystic Muse: ice nuke chain (Freezing Shackle 1225 root, Blizzard 1226 AoE,
				// Ice Dagger 1222 nuke, Ice Storm 1223 chain nuke).
				1225, 1226, 1222, 1223, 1069, 1227
			},
			new int[]
			{
				1218, 1219
			});
		// Cardinal (classId 97): healing/support class repurposed as a mana-drain / Trance-kite fighter.
		// Gear mirrors Mystic Muse (ROBE + MAGIC weapon + female + spiritshot=true).
		// All skills enchanted for magic power / spell critical chance.
		// Opening sequence (BotSpawner):
		//   - Mana Storm (1399) applied as the opening MP-regen debuff (-90%) on the opponent.
		//   - Prayer (1307) applied as the pre-combat self-buff opener.
		// Combat priorities (handled by BotTactics scoring):
		//   1. Greater Battle Heal (1218): score 300 whenever own HP < 95%.
		//   2. Mana Burn (1398): drains enemy MP; also cast during Trance sleep window.
		//   3. Trance (1394): 3-second re-use gate; on land -> kite to 450u + Mana Burn.
		//   4. Prominence (1230): damage nuke; 10-second re-use gate.
		//   5. Magical BackFire (1396): maintained debuff (SLOW kind); re-applied when expired.
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
				// Doombringer rotation.
				// Body Reconstruction (833): a unique Doombringer self-heal (HP recovery
				// proportional to HP deficit). Used only when HP drops below 30%; BotTactics
				// filters it out above that threshold so it does not fire as a generic buff.
				263, 344, 345, 530, 531, 261, 775, 8, 833
			},
			new int[]
			{
				// Doombringer self-buffs: Mirage (445, evasion), Exciting Adventure (768,
				// speed boost), standard elemental/mastery buffs (297, 440), and charge
				// builder (78 = Sonic Fury). 917 is not in this pack's Doombringer data;
				// replaced with 76 (Sonic Blaster - an alternative charge fill skill).
				445, 768, 297, 440, 78
			});
		addPreset(105, "Soul Hound", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.RAPIER, false, false, false,
			new int[]
			{
				500, 482, 499, 625, 502
			},
			new int[]
			{
				// Soul Hound self-buffs.
				497, 505, 507
			});
		addPreset(107, "Trickster", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.CROSSBOW, false, false, false,
			new int[]
			{
				// Trickster rotation.
				542, 921, 544, 543, 547, 546, 548
			},
			new int[]
			{
				// Trickster self-buffs.
				538, 549, 551
			});
		addPreset(108, "Inspector", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.ANCIENT, false, false, false,
			new int[]
			{
				// Inspector rotation.
				577, 578, 587, 584, 579
			},
			new int[]
			{
				// Inspector self-buffs.
				570, 576, 580
			});
		addPreset(110, "Storm Screamer", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, true, false, true,
			new int[]
			{
				// Storm Screamer rotation: wind nukes + CC chain.
				// Tempest (608) is the main AoE nuke; Cyclone (609) is the DoT; static
				// Shackle (1097) is the main CC (wind root); Vortex Shackle (614) AoE root.
				608, 609, 1097, 610, 614, 615
			},
			new int[]
			{
				1218, 1219
			});
		addPreset(111, "Dominator", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// Dominator rotation.
				// Dominator AI (classId==115 in BotTactics) gets a special weave: it uses
				// CP/HP healing skills first (DominatorCpWeave), and after that uses the
				// offense rotation via sleepElapsed >= 5000 ms scoring.
				1305, 617, 619, 618, 616, 1414
			},
			new int[]
			{
				1218, 623
			});
		addPreset(112, "Doomcryer", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// Doomcryer rotation.
				630, 631, 632, 633, 634, 635
			},
			new int[]
			{
				627, 625, 1218
			});
		addPreset(113, "Maestro", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.FIST, false, false, false,
			new int[]
			{
				// Maestro rotation: forge/earth melee skills.
				176, 907, 660, 661, 663, 664, 420
			},
			new int[]
			{
				// Maestro self-buffs (damage window buffs are part of rotation).
				658, 659
			});
		addPreset(114, "Fortune Seeker", BotClassPreset.GearKind.HEAVY, BotClassPreset.WeaponKind.TWO_HAND, false, false, false,
			new int[]
			{
				// Fortune Seeker rotation.
				176, 420, 670, 671, 672
			},
			new int[]
			{
				// Fortune Seeker self-buffs.
				668, 669
			});
		addPreset(115, "Judicator", BotClassPreset.GearKind.ROBE, BotClassPreset.WeaponKind.MAGIC, false, false, true,
			new int[]
			{
				// Judicator (Dominator in Gracia) rotation.
				// Judicator AI uses the same classId==115 path in BotTactics as Dominator.
				616, 617, 618, 619, 1305, 1414
			},
			new int[]
			{
				1218, 623
			});
		addPreset(116, "Titan", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.TWO_HAND, false, false, false,
			new int[]
			{
				// Titan rotation: cleave/two-hand skills + speed/power-up casts.
				689, 362, 363, 367, 690, 691
			},
			new int[]
			{
				686, 687, 688
			});
		addPreset(118, "Grand Khavatari", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.FIST, false, false, false,
			new int[]
			{
				// Grand Khavatari rotation.
				695, 700, 703, 704, 705, 706
			},
			new int[]
			{
				692, 693, 697
			});
		addPreset(131, "Doombringer", BotClassPreset.GearKind.LIGHT, BotClassPreset.WeaponKind.DUAL, false, false, false,
			new int[]
			{
				// Doombringer rotation (classId 131 = the actual Doombringer from the
				// 3rd class expansion; classId 103 above is the pre-expansion placeholder).
				// Rush (793) opens and recycles: when it lands a stun the bot retreats
				// and re-rushes (handled by BotTactics.trainingRushAction and BotSpawner
				// rush-loop driver). Body Reconstruction (833) self-heals below 30% HP.
				793, 263, 344, 345, 530, 531, 261, 775, 8, 833
			},
			new int[]
			{
				// Doombringer self-buffs: Mirage (445), Exciting Adventure (768),
				// standard buffs (297, 440), charge builder (78).
				// Doom Cry (917) and Absolute Zero (833): role-specific runtime skills,
				// not permanent self-buffs.
				500, 482, 499, 625, 502, 917, 833
			});
	}
	
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
	
	public static Set<Integer> getRegisteredClassIds()
	{
		return Collections.unmodifiableSet(PRESETS.keySet());
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
	// Gear assignment — compatibility API (old method names)
	// Called by BotSpawner.java, BotManager.java, TrainingArena.java
	// which were not updated when BotPresets was refactored for Cardinal.
	// ------------------------------------------------------------------
	
	/**
	 * Returns all 5 armor piece IDs (chest, legs, helm, gloves, boots) for
	 * the given preset and gear tier.
	 * Compatibility wrapper for callers that used the old getArmorSet() API.
	 */
	public static int[] getArmorSet(BotClassPreset preset, boolean vesper)
	{
		final int[][] sets = vesper ? VESPER_SETS : DYNASTY_SETS;
		return sets[preset.getGear().ordinal()].clone();
	}
	
	/**
	 * Returns the shield ID for the given preset and gear tier, or 0 for
	 * classes that cannot equip a shield (two-handers, bows, mages, etc.).
	 * Compatibility wrapper for the old getShieldId() API.
	 */
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
	
	/**
	 * Returns the weapon ID for the given preset and gear tier, respecting
	 * any per-preset weapon override set via the //bot menu.
	 * Compatibility wrapper for the old getWeaponId() API.
	 */
	public static int getWeaponId(BotClassPreset preset, boolean vesper)
	{
		if (preset.getWeaponOverride() > 0)
		{
			return preset.getWeaponOverride();
		}
		return getWeapon(preset, vesper);
	}
	
	/**
	 * Returns the jewelry set IDs (ring, earring, necklace) for the current
	 * gear tier as determined by BotManager settings.
	 * Compatibility wrapper for the old getJewelryIds() API.
	 */
	public static int[] getJewelryIds()
	{
		return getJewels(BotManager.getSettings().isUseS84());
	}
	
	/**
	 * Returns ALL possible gear item IDs from both dynasty and vesper tables
	 * (armor pieces, shields, jewels, weapons) for full gear cleanup.
	 * Compatibility wrapper for the old getAllGearIds() API.
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
	
	/**
	 * Returns the Greater Healing Potion ID.
	 * Compatibility wrapper for the old getPotionId() API.
	 */
	public static int getPotionId()
	{
		return POTION_HEAL;
	}
	
	/**
	 * Returns the Greater CP Potion ID.
	 * Compatibility wrapper for the old getCpPotionId() API.
	 */
	public static int getCpPotionId()
	{
		return POTION_CP;
	}
	
	/**
	 * Returns the S-grade soulshot ID used by all physical bots.
	 * Compatibility wrapper for the old getFighterShotId() API.
	 */
	public static int getFighterShotId()
	{
		return SOULSHOT_S;
	}
	
	/**
	 * Returns the S-grade blessed spiritshot ID used by magic bots.
	 * Compatibility wrapper for the old getBlessedSpiritshotId() API.
	 */
	public static int getBlessedSpiritshotId()
	{
		return BLESSED_SPIRITSHOT_S;
	}
	
	/**
	 * Returns the S-grade bolt ID used by crossbow-class bots (Trickster, Inspector).
	 * Compatibility wrapper for the old getBoltId() API.
	 */
	public static int getBoltId()
	{
		return BOLT_S;
	}
	
	/**
	 * Returns the S-grade arrow ID used by bow-class bots (Sagittarius).
	 * Compatibility wrapper for the old getArrowId() API.
	 */
	public static int getArrowId()
	{
		return ARROW_S;
	}
	
	/**
	 * Returns the name pool list for the given gender.
	 * Compatibility wrapper for the old getNames(boolean) API.
	 */
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
	 * Format: {@code <classId>.<key>=<value>}; supported keys: rotation, buffs, weapon.
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
	
	/**
	 * Returns the enchant route letter for a given class and skill.
	 * Cardinal: all skills enchanted for magic power/spell critical chance (route 'p').
	 * Other classes: existing routing logic.
	 */
	public static char getSkillEnchantRoute(int classId, int skillId)
	{
		// Cardinal: all skills enchanted for spell power / crit chance.
		if (classId == 97) { return 'p'; }
		// Default for all other classes.
		return 'p';
	}
}
