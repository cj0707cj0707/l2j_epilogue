/*
 * Fake Players AI - per class preset (gear kind, weapon, skill rotation).
 */
package custom.fakeplayersai;

/**
 * @author FakePlayersAI
 */
public class BotClassPreset
{
	public enum GearKind
	{
		HEAVY,
		LIGHT,
		ROBE
	}
	
	public enum WeaponKind
	{
		SWORD, // one-handed sword/blunt + shield (tanks)
		TWO_HAND, // two-handed melee
		POLE,
		DAGGER,
		BOW,
		DUAL,
		FIST,
		RAPIER,
		ANCIENT,
		CROSSBOW,
		MAGIC // one-handed magic weapon
	}
	
	private final int _classId;
	private final String _className;
	private final GearKind _gear;
	private final WeaponKind _weapon;
	private final boolean _female;
	private final boolean _useArrows;
	private final boolean _useSpiritshot;
	
	/** Skill rotation (enemy-target actives), applied in order. */
	private final int[] _rotation;
	/** Self buffs, cast automatically when missing. */
	private final int[] _buffs;
	
	/** Optional user overrides (persisted by the //bot menu). */
	private int[] _rotationOverride;
	private int[] _buffsOverride;
	/** Optional weapon item id override (persisted by the //bot menu). */
	private int _weaponOverride;
	
	public BotClassPreset(int classId, String className, GearKind gear, WeaponKind weapon, boolean female, boolean useArrows, boolean useSpiritshot, int[] rotation, int[] buffs)
	{
		_classId = classId;
		_className = className;
		_gear = gear;
		_weapon = weapon;
		_female = female;
		_useArrows = useArrows;
		_useSpiritshot = useSpiritshot;
		_rotation = rotation;
		_buffs = buffs;
		_rotationOverride = null;
		_buffsOverride = null;
		_weaponOverride = 0;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public String getClassName()
	{
		return _className;
	}
	
	public GearKind getGear()
	{
		return _gear;
	}
	
	public WeaponKind getWeaponKind()
	{
		return _weapon;
	}
	
	public boolean isFemale()
	{
		return _female;
	}
	
	public boolean useArrows()
	{
		return _useArrows;
	}
	
	public boolean useSpiritshot()
	{
		return _useSpiritshot;
	}
	
	/**
	 * @return true for magic-casting classes (they keep their distance instead of auto-attacking).
	 */
	public boolean isCaster()
	{
		return _weapon == WeaponKind.MAGIC;
	}
	
	public int[] getRotation()
	{
		return _rotationOverride != null ? _rotationOverride : _rotation;
	}
	
	public int[] getBuffs()
	{
		return _buffsOverride != null ? _buffsOverride : _buffs;
	}
	
	public int[] getDefaultRotation()
	{
		return _rotation;
	}
	
	public int[] getDefaultBuffs()
	{
		return _buffs;
	}
	
	public boolean hasRotationOverride()
	{
		return _rotationOverride != null;
	}
	
	public boolean hasBuffsOverride()
	{
		return _buffsOverride != null;
	}
	
	public void setRotationOverride(int[] skills)
	{
		_rotationOverride = (skills == null) || (skills.length == 0) ? null : skills;
	}
	
	public void setBuffsOverride(int[] skills)
	{
		// null = no override (preset defaults apply); an EMPTY array is a valid override:
		// the class was explicitly stripped of all auto-buffs (persisted as "buffs.<id>=").
		// (The rotation override keeps the old empty->null behavior: an empty rotation is
		// not viable - the combat driver re-fills it from the preset.)
		_buffsOverride = skills == null ? null : skills;
	}
	
	public void resetOverrides()
	{
		_rotationOverride = null;
		_buffsOverride = null;
		_weaponOverride = 0;
	}
	
	public int getWeaponOverride()
	{
		return _weaponOverride;
	}
	
	public void setWeaponOverride(int itemId)
	{
		_weaponOverride = Math.max(0, itemId);
	}
}
