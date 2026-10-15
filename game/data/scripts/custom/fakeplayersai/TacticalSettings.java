package custom.fakeplayersai;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Logger;

/** Independent settings; never rewrites the existing FakePlayersAI admin config. */
public final class TacticalSettings
{
    private static final Logger LOGGER = Logger.getLogger(TacticalSettings.class.getName());
    private static final Path FILE = Path.of("config/Custom/OlympiadTactics.ini");
    private static boolean initialized;
    private static volatile boolean enabled = true;
    private static volatile boolean trainingEnabled = true;
    private TacticalSettings() {}
    public static synchronized void init()
    {
        if (initialized) return;
        Properties values = new Properties();
        if (Files.isRegularFile(FILE))
        {
            try (InputStream input = Files.newInputStream(FILE)) { values.load(input); }
            catch (Exception e) { LOGGER.warning("Olympiad tactics settings: " + e); }
        }
        enabled = Boolean.parseBoolean(values.getProperty("Enabled", "true"));
        trainingEnabled = Boolean.parseBoolean(values.getProperty("TrainingEnabled", "true"));
        String difficulty = values.getProperty("DefaultDifficulty", "experienced");
        if (!validDifficulty(difficulty)) difficulty = "experienced";
        BotTactics.setDefaultDifficulty(difficulty);
        initialized = true;
    }
    public static boolean isEnabled() { init(); return enabled; }
    public static boolean isTrainingEnabled() { init(); return trainingEnabled; }
    public static boolean validDifficulty(String value)
    {
        return "normal".equals(value) || "experienced".equals(value) || "strong".equals(value);
    }
    public static synchronized boolean saveDifficulty(String value)
    {
        if (!validDifficulty(value)) return false;
        init();
        Properties data = new Properties();
        data.setProperty("Enabled", Boolean.toString(enabled));
        data.setProperty("TrainingEnabled", Boolean.toString(trainingEnabled));
        data.setProperty("DefaultDifficulty", value);
        try
        {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) { data.store(out, "Olympiad tactical decisions; no stat multipliers"); }
            try { Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING); }
            BotTactics.setDefaultDifficulty(value);
            return true;
        }
        catch (Exception e) { LOGGER.warning("Cannot save Olympiad tactical settings: " + e); return false; }
    }
}
