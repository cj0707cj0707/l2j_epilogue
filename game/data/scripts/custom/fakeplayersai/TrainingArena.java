/* Isolated, unranked, clientless sparring. No global roster membership. */
package custom.fakeplayersai;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.*;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.*;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Public integration: init/start/repeat/leave/isParticipant/isTrainingBot/describe.
 * start must be exposed only through a nearby trainer; leave may be exposed remotely.
 * Uses the REAL single-player DuelManager lifecycle, including its ~6s preparation,
 * 120s limit and nonlethal defeat, never Olympiad mode/points/queues/rewards.
 * No human items, progress, level, skills, reuse timers or buffs are granted/reset.
 * The core duel restores its captured HP/MP/CP and duel debuffs on normal completion.
 * Admission requires full resources, no summon, no combat, no existing instance.
 * Equal is a comparative BOT level/enchant baseline, NOT identical gear/attributes.
 * Recovery journal is flushed before Player.create AND before any human teleport.
 * Keep journals on failed cleanup; init/login/reaper retry. No database migrations.
 */
public final class TrainingArena
{
    private static final Logger LOG = Logger.getLogger(TrainingArena.class.getName());
    private static final Path DIR = Paths.get("data/training-arena-recovery");
    private static final String ACCOUNT = "__training_arena__";
    // Exact normal teleport destination, not an Olympiad geographic zone:
    // data/teleporters/town/30059.xml -> Plains of Dion. Empty private copy.
    private static final int CX = 630, CY = 179184, CZ = -3720;
    private static final double RADIUS = 650;
    private static final Map<Integer, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Set<Integer> BOTS = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Choice> RECENT = Collections.synchronizedMap(new LinkedHashMap<Integer, Choice>(128, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, Choice> e) { return size() > 2048; }
    });
    private static boolean initialized;
    private TrainingArena() { }

    static String option(String value, String... allowed)
    {
        if (value != null) for (String a : allowed) if (a.equalsIgnoreCase(value.trim())) return a;
        return null;
    }
    static boolean inside(int x, int y) { return Math.hypot((double)x - CX, (double)y - CY) <= RADIUS; }
    private record Choice(int classId, String difficulty, String profile) { }
    private static final class Session
    {
        final Player player;
        final Choice choice;
        final Properties journal;
        final long created = System.nanoTime();
        Player bot;
        Instance instance;
        Duel duel;
        boolean duelRequested, active, stopping, closed, botDisposed;
        String reason = "Training finished";
        long nextAction, finishAt;
        int countdown = -1, winnerId;
        Session(Player p, Choice c, Properties j) { player = p; choice = c; journal = j; }
    }
    public static synchronized void init()
    {
        if (initialized) return;
        try { Files.createDirectories(DIR); }
        catch (IOException e) { throw new IllegalStateException("Training recovery directory unavailable", e); }
        CombatAnalytics.init();
        Consumer<OnPlayerLogin> login = e -> ThreadPool.schedule(() -> recover(e.getPlayer()), 1000);
        Consumer<OnPlayerLogout> logout = e -> requestStop(e.getPlayer(), "Logout");
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, login, TrainingArena.class));
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, logout, TrainingArena.class));
        ThreadPool.scheduleAtFixedRate(TrainingArena::pulse, 250, 250);
        ThreadPool.scheduleAtFixedRate(TrainingArena::reap, 2000, 30000);
        initialized = true;
    }
    public static boolean isParticipant(Player p) { return p != null && SESSIONS.containsKey(p.getObjectId()); }
    public static boolean isTrainingBot(Player p) { return p != null && BOTS.contains(p.getObjectId()); }
    public static String repeat(Player p)
    {
        if (p == null) return "Player unavailable.";
        Choice c = RECENT.get(p.getObjectId());
        return c == null ? "Choose a class, difficulty and profile first." : start(p, c.classId, c.difficulty, c.profile);
    }
    public static String describe(Player p)
    {
        if (p == null) return "Player unavailable.";
        Session s = SESSIONS.get(p.getObjectId());
        if (s == null) return "Unranked private sparring. Real duel preparation: approximately 6 seconds. No rewards or Olympiad points. Equal: bot level and enchant baseline only; your equipment is never changed. Core duel resource restoration applies. Use the training leave command to exit.";
        return (s.stopping ? "Leaving: " : s.active ? "Sparring: " : "Preparing: ") + BotPresets.getPresetByClassId(s.choice.classId).getClassName() + ", " + s.choice.difficulty + ", " + s.choice.profile + ". Private instance " + (s.instance == null ? "pending" : s.instance.getId()) + ". No ranking or rewards.";
    }
    public static String leave(Player p)
    {
        if (!isParticipant(p)) { recover(p); return "No active training; any pending return has been checked."; }
        requestStop(p, "Left training");
        return "Leaving training. Please wait for the core duel countdown/end cleanup (up to several seconds).";
    }
    private static void requestStop(Player p, String why)
    {
        if (p == null) return;
        Session s = SESSIONS.get(p.getObjectId());
        if (s != null) synchronized (s) { s.stopping = true; s.reason = why; }
    }
    private static boolean duelEligibleBeforeTeleport(Player p)
    {
        if (p.canDuel()) return true;
        // Giran trainer stands in peace. Accept ONLY the zone prohibition reason;
        // still reject core's private starting-duel flag, jail/combat/low resources.
        int reason = p.getNoDuelReason().getId();
        return p.isInsideZone(ZoneId.PEACE) && !p.isInsideZone(ZoneId.SIEGE) && !p.isInsideZone(ZoneId.PVP)
            && reason == SystemMessageId.C1_CANNOT_MAKE_A_CHALLENGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA_PEACEFUL_ZONE_SEVEN_SIGNS_ZONE_NEAR_WATER_RESTART_PROHIBITED_AREA.getId();
    }
    public static String start(Player p, int classId, String difficulty, String profile)
    {
        init();
        if (p == null) return "Player unavailable.";
        if (!TacticalSettings.isTrainingEnabled()) return "New training sessions are disabled. Recovery and leave remain available.";
        if (!reserveAccount()) return "Training account is not safely locked. Ask the administrator to check the server log.";
        synchronized (p)
        {
            if (isParticipant(p)) return "You already have a training session. Leave it first.";
            if (Files.exists(path(p.getObjectId()))) { recover(p); return "A previous training return is pending. Wait a moment and try again."; }
            BotClassPreset preset = BotPresets.getPresetByClassId(classId);
            String d = option(difficulty, "normal", "experienced", "strong"), f = option(profile, "custom", "equal");
            if (preset == null || d == null || f == null) return "Select a supported class, difficulty normal/experienced/strong, and profile custom/equal.";
            if (p.getClient() == null || !p.isOnline() || p.isDead() || p.isTeleporting() || p.isInCombat() || p.isAttackingOrCastingNow()
                || p.isInDuel() || !duelEligibleBeforeTeleport(p) || p.isInOlympiadMode() || Olympiad.getInstance().isRegistered(p)
                || p.inObserverMode() || p.getInstanceId() != 0 || p.isRegisteredOnEvent() || p.isOnEvent()
                || p.isInStoreMode() || p.isProcessingTransaction() || p.getActiveTradeList() != null || p.isInParty()
                || p.isMounted() || p.isFishing() || p.isInVehicle() || p.isFlying() || p.isCursedWeaponEquipped()
                || p.getSummon() != null || p.getKarma() != 0 || p.getPvpFlag() != 0 || p.isAutoPlaying()
                || p.isInvul() || p.isImmobilized() || p.isAllSkillsDisabled() || p.isInventoryDisabled())
                return "Training requires a free, living character outside combat, parties, duels, events, stores, summons and instances.";
            if (p.getCurrentHp() < p.getMaxHp() - 1 || p.getCurrentMp() < p.getMaxMp() - 1 || p.getCurrentCp() < p.getMaxCp() - 1)
                return "Restore HP, MP and CP before training. Training is not a recovery service.";
            Choice c = new Choice(classId, d, f);
            Properties j = new Properties();
            j.setProperty("player", Integer.toString(p.getObjectId()));
            j.setProperty("x", Integer.toString(p.getX())); j.setProperty("y", Integer.toString(p.getY())); j.setProperty("z", Integer.toString(p.getZ()));
            j.setProperty("heading", Integer.toString(p.getHeading())); j.setProperty("instance", Integer.toString(p.getInstanceId()));
            j.setProperty("botName", "Tr" + UUID.randomUUID().toString().replace("-", "").substring(0, 13));
            j.setProperty("account", ACCOUNT);
            j.setProperty("tradeRefusal", Boolean.toString(p.getTradeRefusal()));
            Session s = new Session(p, c, j);
            synchronized (SESSIONS)
            {
                if (SESSIONS.size() >= 8) return "All eight training slots are occupied. Try again after another duel ends.";
                SESSIONS.put(p.getObjectId(), s);
            }
            synchronized (s)
            {
                try
                {
                    save(p.getObjectId(), j); // MUST precede Player.create; recovers even a crash before create returns.
                    s.instance = InstanceManager.getInstance().createDynamicInstance(0);
                    s.instance.setPvP(false); s.instance.setAllowSummon(false); s.instance.setEmptyDestroyTime(0);
                    s.instance.setExitLoc(new Location(p.getX(), p.getY(), p.getZ()));
                    s.bot = createBot(p, preset, c, j.getProperty("botName"));
                    if (s.bot == null) throw new IllegalStateException("Training character creation failed");
                    BOTS.add(s.bot.getObjectId());
                    j.setProperty("botId", Integer.toString(s.bot.getObjectId()));
                    j.setProperty("arenaInstance", Integer.toString(s.instance.getId()));
                    save(p.getObjectId(), j);
                    s.bot.setInstanceId(s.instance.getId());
                    BotSpawner.enterWorld(s.bot, CX + 250, CY, CZ);
                    s.bot.setInvul(true); s.bot.setImmobilized(true);
                    p.setTradeRefusal(true); p.setInventoryBlockingStatus(true); p.disableAllSkills(); p.setInvul(true); p.setImmobilized(true);
                    p.teleToLocation(CX - 250, CY, CZ, p.getHeading(), s.instance.getId());
                    // The normal roster's maintenance/auto engines NEVER own this bot.
                    BotTactics.setDifficulty(s.bot.getObjectId(), d);
                    if (!BotTactics.initializeTraining(s.bot, p)) throw new IllegalStateException("Training tactics initialization rejected");
                    OlympiadChase.registerTrainingPair(s.bot, p);
                    RECENT.put(p.getObjectId(), c);
                    p.sendMessage("Unranked training: core duel preparation (~6 seconds); no rewards. Leave command remains available.");
                    return "Private training created. Preparing the real nonlethal duel (~6 seconds).";
                }
                catch (Exception e)
                {
                    LOG.log(Level.WARNING, "Training setup failed for " + p.getObjectId(), e);
                    s.stopping = true; s.reason = "Setup failed";
                    finish(s);
                    return "Training could not start safely. Your return is being recovered; no charge or reward was applied.";
                }
            }
        }
    }
    /** This datapack shares Login/Game DB. Never adopt or modify an existing account.
     * Durable random password proof is written BEFORE INSERT. Missing accounts table,
     * name collision, changed password or unban all fail closed for NEW sessions.
     * Recovery does not depend on this lock, and must remain available. */
    private static synchronized boolean reserveAccount()
    {
        Path proof = DIR.resolve(".account-lock");
        try
        {
            if (!Files.exists(proof))
            {
                // Valid SHA-1-shaped login hash of unpredictable bytes, never a usable password.
                byte[] random = new byte[32]; new java.security.SecureRandom().nextBytes(random);
                String hash = Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-1").digest(random));
                Path tmp = DIR.resolve(".account-lock.tmp");
                try (FileOutputStream out = new FileOutputStream(tmp.toFile())) { out.write(hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII)); out.getFD().sync(); }
                Files.move(tmp, proof, StandardCopyOption.ATOMIC_MOVE);
            }
            String hash = Files.readString(proof, java.nio.charset.StandardCharsets.US_ASCII).trim();
            if (!hash.matches("[A-Za-z0-9+/]{27}=")) throw new IOException("Invalid reserved-account proof");
            try (Connection con = DatabaseFactory.getConnection())
            {
                boolean exists;
                try (PreparedStatement ps = con.prepareStatement("SELECT password, accessLevel FROM accounts WHERE login=?"))
                {
                    ps.setString(1, ACCOUNT);
                    try (ResultSet rs = ps.executeQuery())
                    {
                        exists = rs.next();
                        if (exists && (!hash.equals(rs.getString(1)) || rs.getInt(2) >= 0))
                            throw new SQLException("Reserved name belongs to another account or was unlocked; refusing training");
                    }
                }
                if (!exists)
                {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,accessLevel,lastIP) VALUES (?,?,0,-100,?)"))
                    { ps.setString(1, ACCOUNT); ps.setString(2, hash); ps.setString(3, "127.0.0.1"); if (ps.executeUpdate()!=1) throw new SQLException("Account reservation not confirmed"); }
                }
            }
            return true;
        }
        catch (Exception e) { LOG.log(Level.WARNING, "Training account lock unavailable; new training disabled for this request", e); return false; }
    }
    private static Player createBot(Player human, BotClassPreset preset, Choice c, String name)
    {
        // Recheck immediately before persistence; never create a selectable human account.
        if (!reserveAccount()) throw new IllegalStateException("Training account lock lost");
        // Do NOT use BotSpawner.createBot: it chooses global-roster accounts/nobles/max level.
        Player b = Player.create(PlayerTemplateData.getInstance().getTemplate(preset.getClassId()), ACCOUNT, name,
            new PlayerAppearance((byte)0, (byte)0, (byte)0, preset.isFemale()));
        if (b == null) return null;
        // Assign immediately so all later exceptions can stop the newly constructed actor.
        Session session = SESSIONS.get(human.getObjectId());
        session.bot = b; BOTS.add(b.getObjectId());
        int level = c.profile.equals("equal") ? human.getLevel() : 85;
        long exp = ExperienceData.getInstance().getExpForLevel(level);
        b.addExpAndSp(Math.max(0, exp - b.getExp()), 0);
        b.giveAvailableSkills(true, true, true);
        b.setNoble(false); b.setTitle("Unranked Training"); b.setTradeRefusal(true);
        BotSpawner.equipGear(b, preset);
        if (c.profile.equals("equal"))
        {
            int weapon = human.getActiveWeaponInstance() == null ? 0 : human.getActiveWeaponInstance().getEnchantLevel();
            Item chest = human.getChestArmorInstance(); int armor = chest == null ? 0 : chest.getEnchantLevel();
            for (Item item : b.getInventory().getItems()) item.setEnchantLevel(item.isWeapon() ? weapon : armor);
        }
        // One bounded initial shot stock. No potions or automatic MP restoration.
        b.addItem(ItemProcessType.REWARD, BotPresets.getFighterShotId(), 10000, null, false);
        b.addItem(ItemProcessType.REWARD, BotPresets.getBlessedSpiritshotId(), 10000, null, false);
        b.addItem(ItemProcessType.REWARD, 3031, 1000, null, false);
        b.addAutoSoulShot(BotPresets.getFighterShotId());
        b.addAutoSoulShot(BotPresets.getBlessedSpiritshotId());
        b.rechargeShots(true, true);
        // Only arrows/bolts beyond that stock; all training items die with the bot.
        if (preset.useArrows()) b.addItem(ItemProcessType.REWARD,
            preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW ? BotPresets.getBoltId() : BotPresets.getArrowId(), 5000, null, false);
        b.setCurrentHp(b.getMaxHp()); b.setCurrentMp(b.getMaxMp()); b.setCurrentCp(b.getMaxCp());
        b.storeMe();
        return b;
    }
    private static void pulse()
    {
        for (Session s : SESSIONS.values()) synchronized (s)
        {
            try { tick(s); }
            catch (Exception e) { LOG.log(Level.WARNING, "Training tick failed", e); s.stopping = true; s.reason = "Safety stop"; }
        }
    }
    private static void tick(Session s)
    {
        if (s.closed) return;
        Player p = s.player, b = s.bot;
        long now = System.nanoTime();
        if (b == null || s.instance == null) { finish(s); return; }
        if (!p.isOnline() || p.isDead() || b.isDead() || p.getInstanceId() != s.instance.getId() || b.getInstanceId() != s.instance.getId()
            || !inside(p.getX(), p.getY()) || !inside(b.getX(), b.getY()) || now - s.created > 135_000_000_000L)
        { s.stopping = true; s.reason = "Death, logout, escape or timeout"; }
        if (!s.duelRequested)
        {
            if (s.stopping) { finish(s); return; }
            if (p.isTeleporting() || b.isTeleporting()) return;
            if (blockedZone(p) || blockedZone(b))
            {
                s.stopping = true; s.reason = "Training location has a conflicting zone"; finish(s); return;
            }
            // Constructor may schedule work before a later failure: mark pending first.
            s.duelRequested = true;
            DuelManager.getInstance().addDuel(p, b, 0);
            s.nextAction = now;
            return;
        }
        if (s.duel == null && p.isInDuel()) s.duel = DuelManager.getInstance().getDuel(p.getDuelId());
        if (s.duel == null && b.isInDuel()) s.duel = DuelManager.getInstance().getDuel(b.getDuelId());
        if (s.duel == null)
        {
            // Core scheduled start is not cancellable. Never delete/return an actor underneath it.
            if (now - s.created > 15_000_000_000L && !s.stopping)
            {
                s.stopping = true; s.reason = "Pending core duel startup; quarantined until completion/restart";
                LOG.warning("Training " + p.getObjectId() + ": delayed uncancellable core startup; retaining actors, instance and recovery journal.");
            }
            return;
        }
        // Core startDuel sets participant flags sequentially on another task.
        // A half-published start is waiting, never a finished/cancelled duel.
        if (!s.active && !s.duel.getFinished() && (!p.isInDuel() || !b.isInDuel()
            || p.getDuelId() != s.duel.getId() || b.getDuelId() != s.duel.getId()
            || p.getDuelState() != Duel.DUELSTATE_DUELLING || b.getDuelState() != Duel.DUELSTATE_DUELLING)) return;
        if (!s.active && !s.stopping && !s.duel.getFinished())
        {
            s.active = true;
            p.setInvul(false); p.setImmobilized(false); p.enableAllSkills(); b.setInvul(false); b.setImmobilized(false);
            CombatAnalytics.beginTraining(p, b, s.choice.difficulty + "/" + s.choice.profile);
            p.sendMessage("Training started. No ranking, rewards or Olympiad points.");
        }
        if (!p.isInDuel() || !b.isInDuel() || s.duel.getFinished() || p.getDuelState() != Duel.DUELSTATE_DUELLING || b.getDuelState() != Duel.DUELSTATE_DUELLING)
        {
            s.stopping = true;
            if (s.reason.equals("Training finished")) s.reason = "Duel completed";
        }
        if (s.stopping)
        {
            Player winner = s.duel.getWinner();
            if (winner == p || winner == b) s.winnerId = winner.getObjectId();
            stop(p); stop(b);
            if (s.finishAt == 0)
            {
                // Surrender uses the real core match; let its delayed 5s end task drain.
                if (p.isInDuel() && !s.duel.getFinished()) DuelManager.getInstance().doSurrender(p);
                s.finishAt = now + 7_000_000_000L;
            }
            // Elapsed wall time is NOT proof that a delayed core task has drained.
            // finish() centrally requires the actual manager removal after core restore.
            if (s.duel.getFinished() && !p.isInDuel() && !b.isInDuel()) finish(s);
            return;
        }
        if (now < s.nextAction) return;
        s.nextAction = now + 250_000_000L; // Difficulty/reaction timing is owned by BotTactics.
        BotClassPreset preset = BotPresets.getPresetByClassId(s.choice.classId);
        if (!BotTactics.tick(b, p, preset, false))
        {
            boolean ranged = preset.isCaster() || preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW || preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW;
            int range = ranged ? 600 : 60;
            b.setTarget(p);
            OlympiadChase.updateTraining(b, p, range, !preset.isCaster(), CX, CY, RADIUS);
            if (!preset.isCaster() && !b.isAttackingOrCastingNow() && !b.isMovementDisabled() && !p.isSleeping()
                && p.isAutoAttackable(b) && b.calculateDistance2D(p) <= range + 20 && GeoEngine.getInstance().canSeeTarget(b, p))
                b.getAI().setIntentionAttack(p);
        }
    }
    private static boolean blockedZone(Player p)
    {
        return p.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.PEACE)
            || p.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.PVP)
            || p.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.NO_PVP)
            || p.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.SIEGE)
            || p.isInsideZone(org.l2jmobius.gameserver.entity.zone.ZoneId.WATER);
    }

    private static void stop(Player p)
    {
        p.abortAttack(); p.abortCast(); p.stopMove(null); p.setTarget(null); p.getAI().setIntentionActive();
    }
    static boolean canDispose(boolean requested, boolean hasDuel, boolean finished, boolean playerInDuel, boolean botInDuel, boolean managerHasDuel)
    {
        return !requested || hasDuel && finished && !playerInDuel && !botInDuel && !managerHasDuel;
    }

    private static void finish(Session s)
    {
        if (s.closed) return;
        Player p = s.player;
        if (s.duelRequested)
        {
            if (s.duel == null && p.isInDuel()) s.duel = DuelManager.getInstance().getDuel(p.getDuelId());
            if (s.duel == null && s.bot != null && s.bot.isInDuel()) s.duel = DuelManager.getInstance().getDuel(s.bot.getDuelId());
            if (!canDispose(true, s.duel != null, s.duel != null && s.duel.getFinished(), p.isInDuel(), s.bot != null && s.bot.isInDuel(),
                s.duel != null && DuelManager.getInstance().getDuel(s.duel.getId()) != null)) return;
        }
        try
        {
            CombatAnalytics.endTraining(p, s.reason, s.winnerId);
            stop(p);
            if (s.bot != null && !s.botDisposed)
            {
                BotTactics.forget(s.bot.getObjectId());
                OlympiadChase.unregisterTrainingPair(s.bot.getObjectId());
                BotSpawner.stopAutoCombat(s.bot);
                stop(s.bot);
                if (s.bot.getSummon() != null) s.bot.getSummon().unSummon(s.bot);
                s.bot.stopAllTasks();
                s.bot.deleteMe();
                s.bot.getInventory().destroyAllItems(ItemProcessType.DESTROY, s.bot, TrainingArena.class);
                s.botDisposed = true;
            }
            // Return before destroying instance. Offline return is retained for login recovery.
            if (p.isOnline()) returnPlayer(p, s.journal);
            if (s.instance != null) InstanceManager.getInstance().destroyInstance(s.instance.getId());
            s.closed = true;
            SESSIONS.remove(p.getObjectId(), s);
            if (s.bot != null) BOTS.remove(s.bot.getObjectId());
            if (p.isOnline() && !p.isTeleporting() && returned(p, s.journal) && confirmReturnStored(p))
            { s.journal.setProperty("humanReturned", "true"); save(p.getObjectId(), s.journal); }
            cleanupBot(s.journal);
            if (Boolean.parseBoolean(s.journal.getProperty("humanReturned", "false"))) Files.deleteIfExists(path(p.getObjectId()));
            p.sendMessage("Training ended. No rewards or ranked points were applied.");
        }
        catch (Exception e)
        {
            LOG.log(Level.WARNING, "Training cleanup retained for recovery " + p.getObjectId(), e);
            try
            {
                if (p.isOnline() && !p.isTeleporting() && !returned(p,s.journal)) returnPlayer(p,s.journal);
            }
            catch(Exception back) { LOG.log(Level.WARNING,"Training return journal retained",back); }
        }
    }
    private static boolean returned(Player p, Properties j)
    {
        int inst = Integer.parseInt(j.getProperty("instance"));
        if (inst > 0 && InstanceManager.getInstance().getInstance(inst) == null) inst = 0;
        return p.getInstanceId() == inst && ((p.getX() == Integer.parseInt(j.getProperty("x")) && p.getY() == Integer.parseInt(j.getProperty("y")))
            || Boolean.parseBoolean(j.getProperty("returnRequested", "false")) && !inside(p.getX(), p.getY()));
    }
    private static void returnPlayer(Player p, Properties j) throws IOException
    {
        j.setProperty("returnRequested", "true");
        save(p.getObjectId(), j); // Preserve evidence before returning/acknowledging.
        int inst = Integer.parseInt(j.getProperty("instance"));
        if (inst > 0 && InstanceManager.getInstance().getInstance(inst) == null) inst = 0;
        p.setInvul(false); p.setImmobilized(false); p.enableAllSkills(); p.setInventoryBlockingStatus(false);
        p.setTradeRefusal(Boolean.parseBoolean(j.getProperty("tradeRefusal", "false")));
        p.teleToLocation(Integer.parseInt(j.getProperty("x")), Integer.parseInt(j.getProperty("y")), Integer.parseInt(j.getProperty("z")), Integer.parseInt(j.getProperty("heading")), inst);
        p.storeMe();
    }
    private static Path path(int id) { return DIR.resolve(id + ".properties"); }
    private static void save(int id, Properties j) throws IOException
    {
        Path dest = path(id), tmp = DIR.resolve(id + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp.toFile())) { j.store(out, "Training recovery; do not remove until returned"); out.getFD().sync(); }
        Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        // Windows may not allow opening a directory as FileChannel. The file itself
        // has already been fsynced and atomically renamed; do not break all training.
        try (FileChannel dir = FileChannel.open(DIR, StandardOpenOption.READ)) { dir.force(true); }
        catch (IOException | UnsupportedOperationException e) { LOG.fine("Directory fsync unavailable: " + e); }
    }
    private static Properties read(Path path) throws IOException
    {
        Properties j = new Properties(); try (InputStream in = Files.newInputStream(path)) { j.load(in); }
        if (!ACCOUNT.equals(j.getProperty("account")) || !j.getProperty("botName", "").matches("Tr[a-f0-9]{13}")) throw new IOException("Invalid training ownership journal");
        return j;
    }
    public static void recover(Player p)
    {
        if (p == null || isParticipant(p) || !Files.exists(path(p.getObjectId()))) return;
        // A deferred journal must never interrupt a new legitimate duel/event/Olympiad.
        if (p.isInDuel() || p.isInOlympiadMode() || p.isRegisteredOnEvent() || p.isOnEvent()
            || Olympiad.getInstance().isRegistered(p) || Olympiad.getInstance().isRegisteredInComp(p)) return;
        synchronized (p)
        {
            try
            {
                Properties j = read(path(p.getObjectId()));
                if (Integer.parseInt(j.getProperty("player")) != p.getObjectId()) throw new IOException("Wrong recovery owner");
                if (Boolean.parseBoolean(j.getProperty("humanReturned", "false")))
                { cleanupBot(j); Files.deleteIfExists(path(p.getObjectId())); return; } // GC ONLY: never alter a returned human again.
                if (!p.isOnline() || p.isTeleporting()) return;
                if (!returned(p, j)) { stop(p); returnPlayer(p, j); return; }
                // Core teleport ack completed; store outside instance before journal removal.
                // returnPlayer already released our flags before teleport. Never stomp
                // new ordinary combat/control/trade state acquired after the return ack.
                if (confirmReturnStored(p))
                {
                    j.setProperty("humanReturned", "true"); save(p.getObjectId(), j);
                    cleanupBot(j); // Player is already outside, flags cleared, coordinates confirmed.
                    Files.deleteIfExists(path(p.getObjectId()));
                }
            }
            catch (Exception e) { LOG.log(Level.WARNING, "Training recovery failed for " + p.getObjectId(), e); }
        }
    }
    private static void reap()
    {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(DIR, "*.properties"))
        {
            for (Path file : files)
            {
                try
                {
                    Properties j = read(file); int id = Integer.parseInt(j.getProperty("player"));
                    if (SESSIONS.containsKey(id)) continue;
                    Player p = World.getPlayer(id);
                    if (p != null) recover(p); else cleanupBot(j);
                }
                catch (Exception e) { LOG.log(Level.WARNING, "Retained training recovery journal " + file.getFileName(), e); }
            }
        }
        catch (Exception e) { LOG.log(Level.WARNING, "Training recovery scan failed", e); }
    }
    /** Player.storeMe() swallows SQL errors in this core. Confirm only safe world
     * coordinates with a checked transaction before deleting the sole recovery copy. */
    private static boolean confirmReturnStored(Player p) throws SQLException
    {
        if (p.isTeleporting() || p.getInstanceId() != 0 || inside(p.getX(), p.getY())) return false;
        final int x = p.getX(), y = p.getY(), z = p.getZ(), heading = p.getHeading();
        try (Connection con = DatabaseFactory.getConnection())
        {
            con.setAutoCommit(false);
            try
            {
                try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET x=?,y=?,z=?,heading=? WHERE charId=?"))
                {
                    ps.setInt(1,x); ps.setInt(2,y); ps.setInt(3,z); ps.setInt(4,heading); ps.setInt(5,p.getObjectId());
                    if (ps.executeUpdate() > 1) throw new SQLException("Unexpected recovery row count");
                }
                try (PreparedStatement ps = con.prepareStatement("SELECT x,y,z,heading FROM characters WHERE charId=?"))
                {
                    ps.setInt(1,p.getObjectId());
                    try (ResultSet rs = ps.executeQuery())
                    {
                        if (!rs.next() || rs.getInt(1)!=x || rs.getInt(2)!=y || rs.getInt(3)!=z || rs.getInt(4)!=heading)
                            throw new SQLException("Return coordinates not stored; retaining journal");
                    }
                }
                con.commit(); return true;
            }
            catch(SQLException e) { con.rollback(); throw e; }
        }
    }

    private static void cleanupBot(Properties j) throws SQLException
    {
        // Scope every lookup to BOTH journal-generated name AND reserved account. Never touch a roster bot.
        int id = 0;
        try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE char_name=? AND account_name=?"))
        {
            ps.setString(1, j.getProperty("botName")); ps.setString(2, ACCOUNT);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) id = rs.getInt(1); }
        }
        if (id == 0) return;
        if (World.getPlayer(id) != null && !BOTS.contains(id)) throw new SQLException("Training actor still present; defer deletion");
        // Crash path may bypass Item.destroy's auxiliary-table deletion. Verified column names
        // from the actual Item bytecode: item_elementals.itemId / item_variables.id.
        try (Connection con = DatabaseFactory.getConnection())
        {
            con.setAutoCommit(false);
            try
            {
                for (String sql : new String[]{
                    "DELETE FROM item_elementals WHERE itemId IN (SELECT object_id FROM items WHERE owner_id=?)",
                    "DELETE FROM item_variables WHERE id IN (SELECT object_id FROM items WHERE owner_id=?)"})
                    try (PreparedStatement ps = con.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); }
                con.commit();
            }
            catch (SQLException e) { con.rollback(); throw e; }
        }
        GameClient.deleteCharByObjId(id); // pack's own item attributes/items/skills/variables/character cascade.
        CharInfoTable.getInstance().removeName(id);
        try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE charId=?"))
        {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) throw new SQLException("Core deletion incomplete; journal retained"); }
        }
    }
}
