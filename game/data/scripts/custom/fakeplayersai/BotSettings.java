/*
 * Fake Players AI - bot system for L2J-Mobius Gracia Epilogue.
 * Settings holder: reads/writes game/config/Custom/FakePlayersAI.ini
 */
package custom.fakeplayersai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author FakePlayersAI
 */
public class BotSettings
{
	private static final Logger LOGGER = Logger.getLogger(BotSettings.class.getName());
	
	public static final String FILE_PATH = "config/Custom/FakePlayersAI.ini";
	
	// Master
	private boolean _enabled = true;
	private boolean _spawnOnStart = true;
	private int _spawnDelaySeconds = 20;
	private int _botCount = 19; // one bot per olympiad roster class, including Cardinal
	
	// Combat area
	private int _spawnX = 149450;
	private int _spawnY = 46050;
	private int _spawnZ = -3400;
	private int _spawnRadius = 500;
	
	// Gear
	private int _enchantArmor = 10;
	private int _enchantWeapon = 16;
	private int _enchantJewel = 10;
	private boolean _useS84 = true; // true = Vesper (S84), false = Dynasty (S80)
	private int _soulshotCount = 1500;
	private int _arrowCount = 5000;
	private int _potionCount = 60;
	private String _title = "Bot";
	
	// Behavior
	private boolean _autoRegisterOlympiad = true;
	private int _olympiadMinPoints = 18; // keep bot olympiad points at this floor so the core always accepts registration
	private int _olympiadResetPoints = 200; // every bot's olympiad points are reset to this value at every server start (= OLYMPIAD_LOGIN_POINTS, the game entry rule of 04.10.2026)
	private boolean _teleportHome = true;
	private boolean _healBots = true;
	private boolean _restoreMp = true;
	private boolean _clearKarma = true;
	private boolean _fixLevel = true;
	private int _respawnSeconds = 1; // dead bots are revived this many seconds after death (1 s revive pass)
	private int _autoPotionPercent = 50;
	
	// Account metadata
	private String _accountPrefix = "fakeai";
	
	public void load()
	{
		final File file = new File(FILE_PATH);
		if (!file.exists())
		{
			save();
			LOGGER.info("FakePlayersAI: created default config " + FILE_PATH);
			return;
		}
		
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(file))
		{
			props.load(is);
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not read " + FILE_PATH + ": " + e.getMessage());
			return;
		}
		
		_enabled = get(props, "Enabled", _enabled);
		_spawnOnStart = get(props, "SpawnOnStart", _spawnOnStart);
		_spawnDelaySeconds = get(props, "SpawnDelaySeconds", _spawnDelaySeconds);
		_botCount = get(props, "BotCount", _botCount);
		
		_spawnX = get(props, "SpawnX", _spawnX);
		_spawnY = get(props, "SpawnY", _spawnY);
		_spawnZ = get(props, "SpawnZ", _spawnZ);
		_spawnRadius = get(props, "SpawnRadius", _spawnRadius);
		
		_enchantArmor = get(props, "EnchantArmor", _enchantArmor);
		_enchantWeapon = get(props, "EnchantWeapon", _enchantWeapon);
		_enchantJewel = get(props, "EnchantJewel", _enchantJewel);
		_useS84 = get(props, "UseS84Vesper", _useS84);
		_soulshotCount = get(props, "SoulshotCount", _soulshotCount);
		_arrowCount = get(props, "ArrowCount", _arrowCount);
		_potionCount = get(props, "PotionCount", _potionCount);
		_title = props.getProperty("BotTitle", _title);
		
		_autoRegisterOlympiad = get(props, "AutoRegisterOlympiad", _autoRegisterOlympiad);
		_olympiadMinPoints = get(props, "OlympiadMinPoints", _olympiadMinPoints);
		_olympiadResetPoints = get(props, "OlympiadResetPoints", _olympiadResetPoints);
		_teleportHome = get(props, "TeleportHome", _teleportHome);
		_healBots = get(props, "HealBots", _healBots);
		_restoreMp = get(props, "RestoreMp", _restoreMp);
		_clearKarma = get(props, "ClearKarma", _clearKarma);
		_fixLevel = get(props, "FixLevel", _fixLevel);
		_respawnSeconds = get(props, "RespawnSeconds", _respawnSeconds);
		_autoPotionPercent = get(props, "AutoPotionPercent", _autoPotionPercent);
		
		_accountPrefix = props.getProperty("AccountPrefix", _accountPrefix);
		
		LOGGER.info("FakePlayersAI: config loaded from " + FILE_PATH);
	}
	
	public void save()
	{
		final Properties props = new Properties();
		props.setProperty("Enabled", String.valueOf(_enabled));
		props.setProperty("SpawnOnStart", String.valueOf(_spawnOnStart));
		props.setProperty("SpawnDelaySeconds", String.valueOf(_spawnDelaySeconds));
		props.setProperty("BotCount", String.valueOf(_botCount));
		
		props.setProperty("SpawnX", String.valueOf(_spawnX));
		props.setProperty("SpawnY", String.valueOf(_spawnY));
		props.setProperty("SpawnZ", String.valueOf(_spawnZ));
		props.setProperty("SpawnRadius", String.valueOf(_spawnRadius));
		
		props.setProperty("EnchantArmor", String.valueOf(_enchantArmor));
		props.setProperty("EnchantWeapon", String.valueOf(_enchantWeapon));
		props.setProperty("EnchantJewel", String.valueOf(_enchantJewel));
		props.setProperty("UseS84Vesper", String.valueOf(_useS84));
		props.setProperty("SoulshotCount", String.valueOf(_soulshotCount));
		props.setProperty("ArrowCount", String.valueOf(_arrowCount));
		props.setProperty("PotionCount", String.valueOf(_potionCount));
		props.setProperty("BotTitle", _title);
		
		props.setProperty("AutoRegisterOlympiad", String.valueOf(_autoRegisterOlympiad));
		props.setProperty("OlympiadMinPoints", String.valueOf(_olympiadMinPoints));
		props.setProperty("OlympiadResetPoints", String.valueOf(_olympiadResetPoints));
		props.setProperty("TeleportHome", String.valueOf(_teleportHome));
		props.setProperty("HealBots", String.valueOf(_healBots));
		props.setProperty("RestoreMp", String.valueOf(_restoreMp));
		props.setProperty("ClearKarma", String.valueOf(_clearKarma));
		props.setProperty("FixLevel", String.valueOf(_fixLevel));
		props.setProperty("RespawnSeconds", String.valueOf(_respawnSeconds));
		props.setProperty("AutoPotionPercent", String.valueOf(_autoPotionPercent));
		
		props.setProperty("AccountPrefix", _accountPrefix);
		
		try (OutputStream os = new FileOutputStream(FILE_PATH))
		{
			props.store(os, "# Fake Players AI - bot system configuration (managed by //bot menu)");
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: could not save " + FILE_PATH + ": " + e.getMessage());
		}
	}
	
	private static boolean get(Properties props, String key, boolean def)
	{
		return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
	}
	
	private static int get(Properties props, String key, int def)
	{
		try
		{
			return Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim());
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public void setEnabled(boolean v)
	{
		_enabled = v;
	}
	
	public boolean isSpawnOnStart()
	{
		return _spawnOnStart;
	}
	
	public void setSpawnOnStart(boolean v)
	{
		_spawnOnStart = v;
	}
	
	public int getSpawnDelaySeconds()
	{
		return _spawnDelaySeconds;
	}
	
	public int getBotCount()
	{
		return _botCount;
	}
	
	public void setBotCount(int v)
	{
		_botCount = Math.max(0, Math.min(60, v));
	}
	
	public int getSpawnX()
	{
		return _spawnX;
	}
	
	public int getSpawnY()
	{
		return _spawnY;
	}
	
	public int getSpawnZ()
	{
		return _spawnZ;
	}
	
	public int getSpawnRadius()
	{
		return _spawnRadius;
	}
	
	public void setSpawnPoint(int x, int y, int z, int radius)
	{
		_spawnX = x;
		_spawnY = y;
		_spawnZ = z;
		_spawnRadius = Math.max(50, Math.min(1500, radius));
	}
	
	public int getEnchantArmor()
	{
		return _enchantArmor;
	}
	
	public void setEnchantArmor(int v)
	{
		_enchantArmor = Math.max(0, Math.min(25, v));
	}
	
	public int getEnchantWeapon()
	{
		return _enchantWeapon;
	}

	public void setEnchantWeapon(int v)
	{
		_enchantWeapon = Math.max(0, Math.min(25, v));
	}

	public int getEnchantJewel()
	{
		return _enchantJewel;
	}

	public void setEnchantJewel(int v)
	{
		_enchantJewel = Math.max(0, Math.min(25, v));
	}
	
	public boolean isUseS84()
	{
		return _useS84;
	}
	
	public void setUseS84(boolean v)
	{
		_useS84 = v;
	}
	
	public int getSoulshotCount()
	{
		return _soulshotCount;
	}
	
	public void setSoulshotCount(int v)
	{
		_soulshotCount = Math.max(0, Math.min(20000, v));
	}
	
	public int getArrowCount()
	{
		return _arrowCount;
	}
	
	public void setArrowCount(int v)
	{
		_arrowCount = Math.max(0, Math.min(40000, v));
	}
	
	public int getPotionCount()
	{
		return _potionCount;
	}
	
	public void setPotionCount(int v)
	{
		_potionCount = Math.max(0, Math.min(500, v));
	}
	
	public String getBotTitle()
	{
		return _title;
	}
	
	public void setBotTitle(String v)
	{
		_title = v == null ? "" : v;
	}
	
	public boolean isAutoRegisterOlympiad()
	{
		return _autoRegisterOlympiad;
	}
	
	public void setAutoRegisterOlympiad(boolean v)
	{
		_autoRegisterOlympiad = v;
	}
	
	public int getOlympiadMinPoints()
	{
		return _olympiadMinPoints;
	}
	
	public void setOlympiadMinPoints(int v)
	{
		_olympiadMinPoints = Math.max(0, Math.min(1000, v));
	}

	public int getOlympiadResetPoints()
	{
		return _olympiadResetPoints;
	}

	public void setOlympiadResetPoints(int v)
	{
		_olympiadResetPoints = Math.max(0, Math.min(1000, v));
	}
	
	public boolean isTeleportHome()
	{
		return _teleportHome;
	}
	
	public boolean isHealBots()
	{
		return _healBots;
	}
	
	public boolean isRestoreMp()
	{
		return _restoreMp;
	}
	
	public boolean isClearKarma()
	{
		return _clearKarma;
	}
	
	public boolean isFixLevel()
	{
		return _fixLevel;
	}
	
	public int getRespawnSeconds()
	{
		return _respawnSeconds;
	}
	
	public void setRespawnSeconds(int v)
	{
		_respawnSeconds = Math.max(1, Math.min(600, v));
	}
	
	public int getAutoPotionPercent()
	{
		return _autoPotionPercent;
	}
	
	public void setAutoPotionPercent(int v)
	{
		_autoPotionPercent = Math.max(0, Math.min(99, v));
	}
	
	public String getAccountPrefix()
	{
		return _accountPrefix;
	}
}
