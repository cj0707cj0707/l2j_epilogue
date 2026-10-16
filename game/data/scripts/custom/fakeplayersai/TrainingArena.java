/* Isolated, unranked, clientless sparring. No global roster membership. */
package custom.fakeplayersai;

import java.io.*;
import java.lang.reflect.Field;
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
import org.l2jmobius.gameserver.entity.actor.enums.player.DuelResult;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.mechanics.events.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.*;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Public integration: init/start/repeat/leave/isParticipant/isTrainingBot/describe.
 * start must be exposed only through a nearby trainer; leave may be exposed remotely.
 * Uses the REAL DuelManager lifecycle (~6s startup, 120s INCLUDING training prep),
 * nonlethal defeat, never Olympiad mode/points/queues/rewards. After core start:
 * 10..60s protected selfbuff preparation, shared 13 buffs and Olympiad class profile.
 * Entry explicitly strips prior effects and fills resources BEFORE core snapshots.
 * No resource admission minimum; all nonresource core exclusions remain enforced.
 * Human/bot safe selfcasts are admitted during prep, never during the final lock.
 * Both skill cooldown maps reset ONCE before selfbuff preparation starts.
 * After casts drain, only resources refill, followed by a protected 1s interval.
 * No reset/strip on return; skills used during preparation keep normal reuse.
 * Every bot uses the server's ranked level85 gear/skill-enchant settings.
 * Recovery journal is flushed before Player.create AND before any human teleport.
 * Keep journals on failed cleanup; init/login/reaper retry. No database migrations.
 */
public final class TrainingArena
{
    private static final Logger LOG = Logger.getLogger(TrainingArena.class.getName());
    private static final Path DIR = Paths.get("data/training-arena-recovery");
    private static final String ACCOUNT = "__training_arena__";
    // User-requested exact spawns, in a private non-Olympiad instance. World
    // harbor peace zones MUST be scoped out by the authorized instance delegate;
    // blockedZone remains fail-closed if that integration is absent.
    private static final int BX = 44112, BY = 188116, HX = 42705, HY = 188869;
    private static final int CX = 43409, CY = 188493, CZ = -3513;
    private static final double RADIUS = 1000;
    // Initial separation is 1595.8258: only ~4 units inside core's 1600 cancel
    // radius. Keep the exact requested coordinates; do not forge duel flags.
    private static final Field STARTING_DUEL = startingDuelField();
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
    static String trainingProfile(String value)
    {
        String profile = option(value, "custom", "equal");
        return profile == null ? null : "custom"; // legacy equal links/repeat use ranked baseline
    }
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
        boolean duelRequested, closed, botDisposed, openersApplied, officialResultCaptured, finalSnapshotApplied, humanStopApplied;
        volatile boolean active, stopping, preparationStarted, finalizing;
        String reason = "Training finished", officialReason;
        long nextAction, preparationUntil, finalDrainUntil, combatAt;
        int countdown = -1, winnerId, pendingWinnerId;
        Session(Player p, Choice c, Properties j) { player = p; choice = c; journal = j; }
    }
    private static Field startingDuelField()
    {
        try
        {
            Field field = Player.class.getDeclaredField("_startingDuel");
            if (field.getType() != boolean.class) throw new NoSuchFieldException("_startingDuel boolean");
            field.setAccessible(true); return field;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        { LOG.log(Level.WARNING, "Cannot inspect core pending duel; new training must fail closed", e); return null; }
    }
    static boolean startingDuelPending(Player player, Field field)
    {
        if (player == null || field == null) return true;
        try { return field.getBoolean(player); }
        catch (ReflectiveOperationException | RuntimeException e) { return true; }
    }
    public static boolean isTrainingInstance(int instanceId)
    {
        if (instanceId <= 0) return false;
        for (Session s : SESSIONS.values()) if (!s.closed && s.instance != null && s.instance.getId() == instanceId) return true;
        return false;
    }
    public static synchronized void init()
    {
        if (initialized) return;
        try { Files.createDirectories(DIR); }
        catch (IOException e) { throw new IllegalStateException("Training recovery directory unavailable", e); }
        // Only two identity-verified harbor zones may omit THIS authorized
        // instance. A failed install closes new admission but never recovery.
        if (!TrainingPeaceZones.install()) LOG.warning("Training disabled: scoped harbor-zone exclusion unavailable");
        CombatAnalytics.init();
        Consumer<OnPlayerLogin> login = e -> ThreadPool.schedule(() -> recover(e.getPlayer()), 1000);
        Consumer<OnPlayerLogout> logout = e -> requestStop(e.getPlayer(), "Logout");
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, login, TrainingArena.class));
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, logout, TrainingArena.class));
        // These events are synchronous and return TerminateReturn BEFORE attack/cast
        // execution in this jar. Invulnerability alone would still allow debuffs.
        Containers.Global().addListener(new FunctionEventListener(Containers.Global(), EventType.ON_CREATURE_ATTACK,
            (OnCreatureAttack e) -> protectedParticipant(e.getAttacker()) || protectedParticipant(e.getTarget())
                ? new TerminateReturn(true, true, true) : null, TrainingArena.class));
        Containers.Global().addListener(new FunctionEventListener(Containers.Global(), EventType.ON_CREATURE_SKILL_USE,
            (OnCreatureSkillUse e) -> preparationSkill(e), TrainingArena.class));
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_TELEPORTED,
            (OnCreatureTeleported e) -> { if (e.getCreature() instanceof Player) returnAcknowledged((Player)e.getCreature()); }, TrainingArena.class));
        ThreadPool.scheduleAtFixedRate(TrainingArena::pulse, 100, 100);
        ThreadPool.scheduleAtFixedRate(TrainingArena::reap, 2000, 30000);
        initialized = true;
    }
    static int preparationSeconds(int configured) { return Math.max(10, Math.min(60, configured)); }
    static int remainingSeconds(long deadline, long now)
    { return (int)Math.max(0, (deadline - now + 999_999_999L) / 1_000_000_000L); }
    static boolean prepSkillAllowed(boolean human, boolean preparing, boolean stopping, boolean negative, boolean onlySelf)
    { return human && preparing && !stopping && !negative && onlySelf; }
    /** Finished is published before the core's 5s restore; never guess from HP/death. */
    static int officialWinnerId(boolean finished, DuelResult result, int winnerId, int humanId, int botId)
    {
        if (!finished || result == null) return 0;
        // addDuel(human, bot, 0) fixes the actual core team order.
        int expected = switch (result)
        {
            case TEAM_1_WIN, TEAM_2_SURRENDER -> humanId;
            case TEAM_2_WIN, TEAM_1_SURRENDER -> botId;
            default -> 0;
        };
        return expected != 0 && winnerId == expected ? expected : 0;
    }
    static int naturalWinnerId(int humanState, int botState, int humanId, int botId)
    {
        if (humanState == Duel.DUELSTATE_WINNER && botState == Duel.DUELSTATE_DEAD) return humanId;
        if (humanState == Duel.DUELSTATE_DEAD && botState == Duel.DUELSTATE_WINNER) return botId;
        return 0;
    }
    static int approachWithMargin(int tacticalRange)
    { return Math.max(10, tacticalRange - 10); } // inside effective learned offense, not an autoattack envelope
    static boolean openerWindow(boolean applied, boolean stopping, boolean active, int remaining, boolean humanCasting)
    { return !applied && !stopping && !active && remaining <= 3 && !humanCasting; }

    private static Session participantSession(Creature actor)
    {
        Player owner = actor instanceof Player ? (Player)actor : actor instanceof Summon ? ((Summon)actor).getOwner() : null;
        if (owner == null) return null;
        Session s = SESSIONS.get(owner.getObjectId());
        if (s != null) return s;
        if (BOTS.contains(owner.getObjectId()))
            for (Session candidate : SESSIONS.values()) if (candidate.bot == owner) return candidate;
        return null;
    }
    private static boolean protectedParticipant(Creature actor)
    {
        Session s = participantSession(actor);
        return s != null && (!s.active || s.stopping);
    }
    private static TerminateReturn preparationSkill(OnCreatureSkillUse e)
    {
        Session s = participantSession(e.getCaster());
        if (s == null || s.active && !s.stopping) return null;
        boolean onlySelf = e.getTarget() == e.getCaster() && e.getTargets() != null && !e.getTargets().isEmpty();
        if (onlySelf) for (var target : e.getTargets()) if (target != e.getCaster()) { onlySelf = false; break; }
        boolean negative = e.getSkill() == null || e.getSkill().hasNegativeEffect() || e.getSkill().isDebuff()
            || e.getSkill().isSuicideAttack() || e.getSkill().nextActionIsAttack();
        if (prepSkillAllowed(e.getCaster() == s.player || e.getCaster() == s.bot,
            s.preparationStarted && !s.finalizing, s.stopping, negative, onlySelf)) return null;
        return new TerminateReturn(true, true, true);
    }
    public static boolean isParticipant(Player p) { return p != null && SESSIONS.containsKey(p.getObjectId()); }
    /** Read-only authorization for the bounded real-selfcast preparation helper. */
    public static boolean isPreparingBot(Player bot)
    {
        Session s = participantSession(bot);
        return s != null && s.bot == bot && s.preparationStarted && !s.active && !s.stopping && !s.finalizing && !s.closed;
    }
    public static boolean isTrainingBot(Player p) { return p != null && BOTS.contains(p.getObjectId()); }
    public static String repeat(Player p)
    {
        if (p == null) return "Персонаж недоступен. Перезайдите в игру.";
        Choice c = RECENT.get(p.getObjectId());
        return c == null ? "Сначала выберите класс, сложность и профиль тренировки." : start(p, c.classId, c.difficulty, c.profile);
    }
    public static String describe(Player p)
    {
        if (p == null) return "Персонаж недоступен. Перезайдите в игру.";
        Session s = SESSIONS.get(p.getObjectId());
        if (s == null) return "Личная нерейтинговая тренировка: " + preparationSeconds(TacticalSettings.getTrainingPreparationSeconds()) + " сек. на селфбаф после запуска дуэли. При входе прежние эффекты очищаются. Оба участника получают 13 базовых бафов и олимпийский профиль класса; бот кастует разрешённые селфбафы во время подготовки. Общий лимит дуэли 120 сек., включая подготовку. Наград и очков нет. Бот 85 уровня использует те же настройки снаряжения и заточки умений, что и рейтинговые боты. Порога HP/MP/CP нет. Откаты обоих сбрасываются перед таймером селфбафов, не после него. За секунду до боя: Титан/кастетчик — 70% HP, 15% CP, 100% MP; остальные — 100%. И игрок, и бот своего класса получают: Титан — Frenzy и Zealot, кастетчик — Zealot. При выходе сброса нет.";
        return (s.stopping ? "Выход: " : s.active ? "Бой: " : "Подготовка: ") + BotPresets.getPresetByClassId(s.choice.classId).getClassName() + ", " + s.choice.difficulty + ", " + s.choice.profile + ". Личная арена " + (s.instance == null ? "создаётся" : s.instance.getId()) + ". Без рейтинга и наград.";
    }
    public static String leave(Player p)
    {
        if (!isParticipant(p)) { recover(p); return "Активной тренировки нет. Проверен незавершённый возврат с арены."; }
        requestStop(p, "Left training");
        return "Выход с тренировки: бой остановлен. Дождитесь штатного завершения дуэли и возврата (до остатка общего лимита 120 сек.). Победа сопернику автоматически не присуждается.";
    }
    private static void requestStop(Player p, String why)
    {
        if (p == null) return;
        Session s = SESSIONS.get(p.getObjectId());
        if (s != null) synchronized (s) { s.stopping = true; s.reason = why; }
    }
    private static String duelRefusalBeforeTeleport(Player p)
    {
        // canDuel() tests HP/MP BEFORE private _startingDuel. A low-resource
        // reason is never evidence that no other duel is already starting.
        if (startingDuelPending(p, STARTING_DUEL))
            return "Уже идёт подготовка другой дуэли или проверка ядра недоступна. Дождитесь её завершения/отмены.";
        if (p.isInsideZone(ZoneId.SIEGE) || p.isInsideZone(ZoneId.PVP) || p.isInsideZone(ZoneId.NO_PVP) || p.isInsideZone(ZoneId.WATER))
            return "Выйдите из зоны запрета тренировок и повторите Старт.";
        if (p.canDuel()) return null;
        int reason = p.getNoDuelReason().getId();
        // All actual nonresource canDuel checks were independently verified by
        // admissionRefusal above, including boat/store/combat/pending duel.
        if (reason == SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_S_HP_OR_MP_IS_BELOW_50.getId()) return null;
        if (reason == SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL.getId())
            return "Уже идёт подготовка другой дуэли. Дождитесь её завершения/отмены.";
        if (reason == SystemMessageId.C1_CANNOT_MAKE_A_CHALLENGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA_PEACEFUL_ZONE_SEVEN_SIGNS_ZONE_NEAR_WATER_RESTART_PROHIBITED_AREA.getId()
            && p.isInsideZone(ZoneId.PEACE)) return null; // trainer's peace zone ONLY
        return "Ядро отклонило вход в дуэль (код " + reason + "). Сообщите код администратору.";
    }
    private static String admissionRefusal(Player p)
    {
        if (p.getClient() == null || !p.isOnline()) return "Нет активного подключения. Перезайдите в игру.";
        if (p.isDead() || p.isAlikeDead()) return "Персонаж мёртв или притворяется мёртвым. Воскресните/встаньте.";
        if (p.isJailed()) return "Тренировка недоступна в тюрьме. Дождитесь освобождения.";
        if (p.isTeleporting()) return "Дождитесь завершения телепортации и нажмите Старт ещё раз.";
        if (p.isInCombat() || p.isAttackingOrCastingNow()) return "Завершите атаку/каст и дождитесь выхода из боевого режима.";
        if (p.isInDuel()) return "Сначала завершите текущую дуэль и её обратный отсчёт.";
        if (p.isInOlympiadMode() || Olympiad.getInstance().isRegistered(p) || Olympiad.getInstance().isRegisteredInComp(p))
            return "Отмените регистрацию на Олимпиаду или дождитесь окончания олимпийского боя.";
        if (p.inObserverMode()) return "Сначала выйдите из режима наблюдателя.";
        if (p.getInstanceId() != 0) return "Выйдите из текущей временной зоны (инстанса).";
        if (p.isRegisteredOnEvent() || p.isOnEvent()) return "Сначала выйдите из события и отмените регистрацию на него.";
        if (p.isInStoreMode()) return "Закройте личную лавку/мастерскую.";
        if (p.isProcessingTransaction() || p.getActiveTradeList() != null) return "Завершите обмен или текущую торговую операцию.";
        if (p.isInParty()) return "Выйдите из группы: тренировка рассчитана на одного игрока.";
        if (p.isMounted() || p.isInBoat() || p.isInVehicle() || p.isFlying()) return "Сойдите с транспорта/ездового животного и завершите полёт.";
        if (p.isFishing()) return "Сначала прекратите рыбалку.";
        if (p.isCursedWeaponEquipped()) return "Тренировка недоступна с проклятым оружием.";
        if (p.getSummon() != null) return "Отзовите питомца/слугу перед входом.";
        if (p.getKarma() != 0 || p.getPvpFlag() != 0) return "Дождитесь исчезновения PvP-флага и уберите карму.";
        if (p.isAutoPlaying()) return "Выключите автоохоту перед тренировкой.";
        if (p.isInvul()) return "Дождитесь окончания неуязвимости/защиты после телепорта (GM: выключите invul).";
        if (p.isImmobilized()) return "Дождитесь окончания обездвиживания.";
        if (p.isAllSkillsDisabled()) return "Дождитесь снятия блокировки умений.";
        if (p.isInventoryDisabled()) return "Закройте операцию, блокирующую инвентарь.";
        return duelRefusalBeforeTeleport(p);
    }
    public static String start(Player p, int classId, String difficulty, String profile)
    {
        init();
        if (p == null) return "Персонаж недоступен. Перезайдите в игру.";
        if (!TrainingPeaceZones.isInstalled()) return "Тренировка недоступна: безопасная изоляция зон не установлена. Обратитесь к администратору.";
        if (!TacticalSettings.isTrainingEnabled()) return "Новые тренировки отключены администратором. Выход и восстановление доступны.";
        if (!reserveAccount()) return "Старт отклонён: служебный аккаунт не заблокирован безопасно. Администратору: проверьте журнал сервера и accounts/__training_arena__.";
        synchronized (p)
        {
            if (isParticipant(p)) return "Тренировка уже запущена. Нажмите Выход перед новым стартом.";
            if (Files.exists(path(p.getObjectId()))) { recover(p); return "Не завершён возврат с прошлой тренировки. Подождите несколько секунд и повторите; если не помогло, обратитесь к администратору."; }
            BotClassPreset preset = BotPresets.getPresetByClassId(classId);
            String d = option(difficulty, "normal", "experienced", "strong"), f = trainingProfile(profile);
            if (preset == null || d == null || f == null) return "Выберите класс, сложность normal/experienced/strong и нажмите Старт. Бот использует рейтинговые настройки снаряжения и заточки умений.";
            String refusal = admissionRefusal(p);
            if (refusal != null) return refusal;
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
                if (SESSIONS.size() >= 8) return "Все 8 тренировочных арен заняты. Повторите после окончания другой тренировки.";
                SESSIONS.put(p.getObjectId(), s);
            }
            synchronized (s)
            {
                try
                {
                    save(p.getObjectId(), j); // MUST precede Player.create; recovers even a crash before create returns.
                    s.instance = InstanceManager.getInstance().createDynamicInstance(0);
                    if (!TrainingPeaceZones.registerInstance(s.instance.getId())) throw new IllegalStateException("Training zone authority unavailable");
                    s.instance.setPvP(false); s.instance.setAllowSummon(false); s.instance.setEmptyDestroyTime(0);
                    s.instance.setExitLoc(new Location(p.getX(), p.getY(), p.getZ()));
                    s.bot = createBot(p, preset, c, j.getProperty("botName"));
                    if (s.bot == null) throw new IllegalStateException("Training character creation failed");
                    BOTS.add(s.bot.getObjectId());
                    j.setProperty("botId", Integer.toString(s.bot.getObjectId()));
                    j.setProperty("arenaInstance", Integer.toString(s.instance.getId()));
                    j.setProperty("arenaX", Integer.toString(CX)); j.setProperty("arenaY", Integer.toString(CY));
                    j.setProperty("arenaRadius", Double.toString(RADIUS));
                    save(p.getObjectId(), j);
                    s.bot.setInstanceId(s.instance.getId());
                    BotSpawner.enterWorld(s.bot, BX, BY, CZ);
                    // Recheck before the explicitly destructive, journaled entry.
                    if (startingDuelPending(p, STARTING_DUEL) || p.isInDuel()) throw new IllegalStateException("Another core duel began during setup");
                    p.stopAllEffects(); s.bot.stopAllEffects();
                    restoreResources(p); restoreResources(s.bot); // class resource baseline for the core snapshot, not old entry values
                    s.bot.setInvul(true); s.bot.setImmobilized(true); s.bot.disableAllSkills();
                    p.setTradeRefusal(true); p.setInventoryBlockingStatus(true); p.disableAllSkills(); p.setInvul(true); p.setImmobilized(true);
                    p.teleToLocation(HX, HY, CZ, p.getHeading(), s.instance.getId());
                    // The normal roster's maintenance/auto engines NEVER own this bot.
                    BotTactics.setDifficulty(s.bot.getObjectId(), d);
                    if (!BotTactics.initializeTraining(s.bot, p)) throw new IllegalStateException("Training tactics initialization rejected");
                    OlympiadChase.registerTrainingPair(s.bot, p);
                    RECENT.put(p.getObjectId(), c);
                    p.sendMessage("Тренировка создана. Запуск безопасной дуэли (~6 сек.), затем " + preparationSeconds(TacticalSettings.getTrainingPreparationSeconds()) + " сек. на селфбаф. Выход доступен.");
                    return "Перенос на личную арену. Дождитесь сообщения о подготовке; бой начнётся только после её отсчёта.";
                }
                catch (Exception e)
                {
                    LOG.log(Level.WARNING, "Training setup failed for " + p.getObjectId(), e);
                    s.stopping = true; s.reason = "Setup failed";
                    finish(s);
                    return "Безопасный старт не удался. Выполняется возврат; плата не списана. Администратору: проверьте Training setup failed в журнале.";
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
        int level = 85; // same server baseline for every training class/profile
        long exp = ExperienceData.getInstance().getExpForLevel(level);
        b.addExpAndSp(Math.max(0, exp - b.getExp()), 0);
        b.giveAvailableSkills(true, true, true);
        BotSpawner.applySkillEnchants(b);
        b.setNoble(false); b.setTitle("Unranked Training"); b.setTradeRefusal(true);
        BotSpawner.equipGear(b, preset);
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
        if (!s.stopping && !zoneAuthority(s.instance.getId()))
        { s.stopping = true; s.reason = "Training zone exclusion revoked/reloaded"; }
        if (!s.stopping && s.preparationStarted && (blockedZone(p) || blockedZone(b)))
        { s.stopping = true; s.reason = "Training entered an unauthorized zone"; }
        if (!s.stopping && (!p.isOnline() || p.isDead() || b.isDead() || p.getInstanceId() != s.instance.getId() || b.getInstanceId() != s.instance.getId()
            || !inside(p.getX(), p.getY()) || !inside(b.getX(), b.getY()) || now - s.created > 135_000_000_000L))
        { s.stopping = true; s.reason = "Death, logout, escape or timeout"; }
        if (!s.duelRequested)
        {
            if (s.stopping) { finish(s); return; }
            if (p.isTeleporting() || b.isTeleporting()) return;
            if (blockedZone(p) || blockedZone(b))
            {
                s.stopping = true; s.reason = "Training location has a conflicting zone"; p.sendMessage("Арена пересекается с запретной зоной. Выполняется возврат; сообщите администратору."); finish(s); return;
            }
            // Real core validation AFTER neutral teleport and entry resources;
            // never fabricate its flags or let low HP mask a pending challenge.
            if (!p.canDuel() || !b.canDuel())
            { s.stopping = true; s.reason = "Core refused acknowledged arena duel"; finish(s); return; }
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
        // Defeat is authoritative BEFORE getWinner() becomes available. Retain
        // that observation, but do not call it an official completed victory yet.
        if (s.pendingWinnerId == 0 && p.getDuelId() == s.duel.getId() && b.getDuelId() == s.duel.getId())
            s.pendingWinnerId = naturalWinnerId(p.getDuelState(), b.getDuelState(), p.getObjectId(), b.getObjectId());
        // Core startDuel sets participant flags sequentially on another task.
        // A half-published start is waiting, never a finished/cancelled duel.
        if (!s.preparationStarted && !s.stopping && s.pendingWinnerId == 0 && !s.duel.getFinished() && (!p.isInDuel() || !b.isInDuel()
            || p.getDuelId() != s.duel.getId() || b.getDuelId() != s.duel.getId()
            || p.getDuelState() != Duel.DUELSTATE_DUELLING || b.getDuelState() != Duel.DUELSTATE_DUELLING)) return;
        if (!s.preparationStarted && !s.stopping && s.pendingWinnerId == 0 && !s.duel.getFinished())
        {
            // The REAL core snapshot/start has happened; never forge duel/Olympiad flags.
            // Prior effects were explicitly removed at journaled entry. No
            // guessed effect restoration; ranked profile plus real prep selfcasts.
            // Do NOT call maintainFullBuffs: without Olympiad mode it selects WORLD buffs.
            BotClassPreset humanPreset = BotPresets.getPresetByClassId(p.getPlayerClass().getId());
            BotSpawner.applyTrainingOlympiadBuffs(p, humanPreset != null ? humanPreset.isCaster() : p.isMageClass());
            BotSpawner.applyTrainingOlympiadBuffs(b, BotPresets.getPresetByClassId(s.choice.classId).isCaster());
            // Ready BEFORE the player is invited to selfbuff, not at the end.
            // Keep the cooldowns of skills actually used during this preparation.
            resetPreparationCooldowns(p); resetPreparationCooldowns(b);
            p.enableAllSkills(); b.enableAllSkills(); // both may cast safe self-only skills; inventory stays locked.
            // Start AFTER buff work and skill unlock: a slow application never shortens prep.
            s.preparationUntil = System.nanoTime() + preparationSeconds(TacticalSettings.getTrainingPreparationSeconds()) * 1_000_000_000L;
            s.preparationStarted = true;
            p.sendMessage("Выданы 13 базовых бафов и олимпийский профиль класса. Откаты обоих сброшены ДО таймера селфбафов. Подготовка: бафайте себя, атаковать нельзя. Перед боем — восстановление без повторного сброса откатов: Титан/кастетчик 70% HP, 15% CP, 100% MP; остальные 100%. Затем защищённая 1 секунда.");
        }
        if (!p.isInDuel() || !b.isInDuel() || s.duel.getFinished() || p.getDuelState() != Duel.DUELSTATE_DUELLING || b.getDuelState() != Duel.DUELSTATE_DUELLING)
        {
            s.stopping = true;
            if (s.reason.equals("Training finished")) s.reason = "Duel completed";
        }
        if (s.stopping)
        {
            captureOfficialResult(s);
            stopParticipantsOnce(s);
            // NEVER synthesize a surrender here. A lethal duel hit leaves HP=1,
            // sets DEAD/WINNER, and the core may not publish finished until its
            // next 1s poll. Surrender outranks that winner and reverses the HUD.
            // Even safety/leave stops drain the core's own timeout/cancel task.
            // No periodic human stop/cancel/flag changes during core drain.
            // Elapsed wall time is NOT proof that a delayed core task has drained.
            // finish() centrally requires the actual manager removal after core restore.
            if (s.duel.getFinished() && !p.isInDuel() && !b.isInDuel()) finish(s);
            return;
        }
        if (!s.active)
        {
            long phaseNow = System.nanoTime();
            if (!s.finalizing && phaseNow >= s.preparationUntil - 1_000_000_000L)
            {
                s.finalizing = true; // synchronous gate rejects ALL new casts now
                s.finalDrainUntil = phaseNow + 100_000_000L; // settle an already admitted request
            }
            boolean casting = castsRunning(p) || castsRunning(b);
            int remaining = remainingSeconds(s.preparationUntil, phaseNow);
            if (openerWindow(s.openersApplied, s.stopping, s.active, remaining, casting))
            {
                s.openersApplied = true;
                BotSpawner.applyTrainingClassBuffs(p, BotPresets.getPresetByClassId(p.getPlayerClass().getId()));
                BotSpawner.applyTrainingClassBuffs(b, BotPresets.getPresetByClassId(s.choice.classId));
                BotSpawner.applyPreCombatOpeners(b, BotPresets.getPresetByClassId(s.choice.classId));
                s.preparationUntil = Math.max(s.preparationUntil, System.nanoTime() + 3_000_000_000L);
                remaining = remainingSeconds(s.preparationUntil, System.nanoTime());
            }
            if (!s.finalizing)
            {
                // Real, shift/self-only skill requests; never abort/idle an
                // already running human or bot cast on the 100ms pulse.
                BotSpawner.applyTrainingPreparationSelfBuffs(b, BotPresets.getPresetByClassId(s.choice.classId));
                if (remaining != s.countdown && (s.countdown == -1 || remaining % 10 == 0 || remaining <= 5))
                    p.sendMessage("До начала тренировочного боя: " + remaining + " сек. Бафайте себя.");
                s.countdown = remaining;
                return;
            }
            if (!s.finalSnapshotApplied)
            {
                if (!finalSnapshotReady(phaseNow, s.preparationUntil, s.finalDrainUntil, casting)) return;
                // Both casts have drained; disable new skills under the final
                // event gate, then refill resources ONLY. Preparation casts
                // retain their ordinary reuse: no second skill cooldown reset.
                p.disableAllSkills(); b.disableAllSkills();
                s.finalSnapshotApplied = true;
                restoreResources(p); restoreResources(b);
                s.combatAt = System.nanoTime() + 1_000_000_000L;
                p.sendMessage("Ресурсы восстановлены по классу: Титан/кастетчик 70% HP, 15% CP, 100% MP; остальные 100%. Бой через 1 секунду. Откаты использованных селфбафов сохраняются.");
                return;
            }
            if (System.nanoTime() < s.combatAt || castsRunning(p) || castsRunning(b)) return;
            p.setInvul(false); p.setImmobilized(false); p.enableAllSkills();
            b.setInvul(false); b.setImmobilized(false); b.enableAllSkills();
            CombatAnalytics.beginTraining(p, b, s.choice.difficulty + "/" + s.choice.profile);
            s.active = true;
            p.sendMessage("Подготовка завершена. Бой начался! Наград, рейтинга и очков Олимпиады нет.");
        }
        if (now < s.nextAction) return;
        s.nextAction = now + 100_000_000L; // Difficulty/reaction timing is owned by BotTactics.
        BotClassPreset preset = BotPresets.getPresetByClassId(s.choice.classId);
        if (!BotTactics.tick(b, p, preset, false))
        {
            int range = approachWithMargin(BotTactics.tacticalApproachRange(b, p, preset));
            b.setTarget(p);
            boolean basicAttack = BotTactics.allowsBasicAttack(b, preset);
            // Training 131 casts at 40+collision. Chase's attack hysteresis
            // (+60) may otherwise retain ENGAGED outside both this strict
            // manual attack gate and skill range, with no old route to resume.
            // Let pursuit reach the exact range; THIS caller owns its autoattack.
            boolean chaseAttack = basicAttack && preset.getClassId() != 131;
            OlympiadChase.updateTraining(b, p, range, chaseAttack, CX, CY, RADIUS);
            if (basicAttack && !b.isAttackingOrCastingNow() && !b.isMovementDisabled() && !p.isSleeping()
                && p.isAutoAttackable(b) && b.calculateDistance2D(p) <= range && GeoEngine.getInstance().canSeeTarget(b, p))
                b.getAI().setIntentionAttack(p);
        }
    }
    private static void captureOfficialResult(Session s)
    {
        if (s.officialResultCaptured || !s.duel.getFinished()) return;
        Player p = s.player, b = s.bot;
        // After restore, duel states reset and getWinner() becomes null. Missing
        // that observation is UNKNOWN, never a fabricated result from damage/HP.
        if (!p.isInDuel() || !b.isInDuel() || p.getDuelId() != s.duel.getId() || b.getDuelId() != s.duel.getId()) return;
        DuelResult result = s.duel.checkEndDuelCondition();
        if (result == DuelResult.CONTINUE) return;
        Player winner = s.duel.getWinner();
        s.winnerId = officialWinnerId(true, result, winner == null ? 0 : winner.getObjectId(), p.getObjectId(), b.getObjectId());
        s.officialReason = "Official duel result: " + result.name();
        s.officialResultCaptured = true;
    }

    static boolean finalSnapshotReady(long now, long preparationUntil, long drainUntil, boolean casting)
    { return !casting && now >= drainUntil && now >= preparationUntil - 1_000_000_000L; }
    private static boolean castsRunning(Player p)
    { return p.isCastingNow() || p.isCastingSimultaneouslyNow(); }
    private static void restoreResources(Player p)
    {
        int classId = p.getPlayerClass().getId();
        p.setCurrentHp(p.getMaxHp() * trainingHpFraction(classId));
        p.setCurrentMp(p.getMaxMp());
        p.setCurrentCp(p.getMaxCp() * trainingCpFraction(classId));
        p.broadcastUserInfo();
    }
    static double trainingHpFraction(int classId)
    { return classId == 113 || classId == 114 ? 0.70 : 1.0; }
    static double trainingCpFraction(int classId)
    { return classId == 113 || classId == 114 ? 0.15 : 1.0; }

    static void resetPreparationCooldowns(Player p)
    {
        // Actual core APIs clear separate skill disabled/reuse maps. Item reuse,
        // active effects and skill levels are intentionally untouched.
        p.resetDisabledSkills();
        p.resetTimeStamps();
        p.sendPacket(new SkillCoolTime(p));
    }

    private static boolean zoneAuthority(int instanceId)
    {
        if (!TrainingPeaceZones.isInstalled() || instanceId <= 0) return false;
        // Coordinate queries return the real map zone identities even though
        // membership is excluded for the authorized private instance only.
        boolean botPeace = false, humanPeace = false;
        for (ZoneType zone : ZoneManager.getInstance().getZones(BX, BY, CZ)) if (zone instanceof PeaceZone)
        { if (!TrainingPeaceZones.isExcluded(zone, instanceId)) return false; botPeace = true; }
        for (ZoneType zone : ZoneManager.getInstance().getZones(HX, HY, CZ)) if (zone instanceof PeaceZone)
        { if (!TrainingPeaceZones.isExcluded(zone, instanceId)) return false; humanPeace = true; }
        return botPeace && humanPeace;
    }
    private static boolean blockedZone(Player p)
    {
        for (ZoneType zone : ZoneManager.getInstance().getZones(p.getX(), p.getY(), p.getZ()))
            if (zone instanceof PeaceZone && !TrainingPeaceZones.isExcluded(zone, p.getInstanceId())) return true;
        // Actual membership flags must also remain neutral. Never zero them or
        // bypass unrelated peace/PvP/siege/water zones by name or coordinates.
        return p.isInsideZone(ZoneId.PEACE) || p.isInsideZone(ZoneId.PVP) || p.isInsideZone(ZoneId.NO_PVP)
            || p.isInsideZone(ZoneId.SIEGE) || p.isInsideZone(ZoneId.WATER);
    }

    private static void stop(Player p)
    {
        p.abortAttack(); p.abortCast(); p.stopMove(null); p.setTarget(null);
        p.getAI().setIntentionIdle(); cancelSavedActions(p);
    }
    static void cancelSavedActions(Player p)
    {
        // PlayerAI Idle/Active alone does NOT clear _savedMoveTo. CANCEL does.
        p.getAI().setNextAction(null);
        p.getAI().notifyActionCancel();
    }
    private static void stopParticipantsOnce(Session s)
    {
        if (s.humanStopApplied) return;
        s.humanStopApplied = true;
        stop(s.player); if (s.bot != null) stop(s.bot);
        s.player.setInvul(true); if (s.bot != null) s.bot.setInvul(true);
        if (!s.active) s.player.setImmobilized(false); // release ONLY our preparation hold
    }
    static boolean canDispose(boolean requested, boolean hasDuel, boolean finished, boolean playerInDuel, boolean botInDuel, boolean managerHasDuel)
    {
        return !requested || hasDuel && finished && !playerInDuel && !botInDuel && !managerHasDuel;
    }

    static String trainingEndReason(String officialReason, String stopReason)
    {
        String official = officialReason == null ? "Unknown: core result was not observed before restore" : officialReason;
        return "Training finished".equals(stopReason) || "Duel completed".equals(stopReason) ? official : official + "; stop: " + stopReason;
    }

    private static void finish(Session s)
    {
        if (s.closed) return;
        Player p = s.player;
        // A competing core challenge can race admission. If OUR request was
        // never made, do not return/delete actors underneath ITS pending task.
        if (!s.duelRequested && (startingDuelPending(p, STARTING_DUEL) || p.isInDuel()
            || s.bot != null && (startingDuelPending(s.bot, STARTING_DUEL) || s.bot.isInDuel()))) return;
        if (s.duelRequested)
        {
            if (s.duel == null && p.isInDuel()) s.duel = DuelManager.getInstance().getDuel(p.getDuelId());
            if (s.duel == null && s.bot != null && s.bot.isInDuel()) s.duel = DuelManager.getInstance().getDuel(s.bot.getDuelId());
            if (!canDispose(true, s.duel != null, s.duel != null && s.duel.getFinished(), p.isInDuel(), s.bot != null && s.bot.isInDuel(),
                s.duel != null && DuelManager.getInstance().getDuel(s.duel.getId()) != null)) return;
        }
        try
        {
            String officialReason = s.officialReason;
            if (officialReason == null && s.pendingWinnerId != 0)
                officialReason = "Core DEAD/WINNER observed (winner ID " + s.pendingWinnerId + "); official completion not observed";
            CombatAnalytics.endTraining(p, trainingEndReason(officialReason, s.reason), s.winnerId);
            stopParticipantsOnce(s);
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
            if (p.isOnline() && !p.isTeleporting() && returned(p, s.journal))
            {
                synchronizeReturnOnce(p, s.journal);
                if (confirmReturnStored(p)) { s.journal.setProperty("humanReturned", "true"); save(p.getObjectId(), s.journal); }
            }
            cleanupBot(s.journal);
            if (Boolean.parseBoolean(s.journal.getProperty("humanReturned", "false")))
            { retireZoneAuthority(s.journal); Files.deleteIfExists(path(p.getObjectId())); }
            p.sendMessage("Тренировка завершена. Награды и рейтинговые очки не начислялись.");
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
    private static void retireZoneAuthority(Properties j)
    {
        int instanceId = Integer.parseInt(j.getProperty("arenaInstance", "0"));
        // Retain exclusion throughout outgoing teleport. Only acknowledged
        // evacuation AND destroyed instance make retirement safe.
        if (instanceId > 0 && Boolean.parseBoolean(j.getProperty("humanReturned", "false"))
            && InstanceManager.getInstance().getInstance(instanceId) == null)
            TrainingPeaceZones.unregisterInstance(instanceId);
    }
    private static boolean returned(Player p, Properties j)
    {
        int inst = Integer.parseInt(j.getProperty("instance"));
        if (inst > 0 && InstanceManager.getInstance().getInstance(inst) == null) inst = 0;
        return p.getInstanceId() == inst && ((p.getX() == Integer.parseInt(j.getProperty("x")) && p.getY() == Integer.parseInt(j.getProperty("y")))
            || Boolean.parseBoolean(j.getProperty("returnRequested", "false")) && !insideJournalArena(p.getX(), p.getY(), j));
    }
    private static boolean insideJournalArena(int x, int y, Properties j)
    {
        // Preserve recovery of pre-relocation journals from the old Dion arena.
        int cx = Integer.parseInt(j.getProperty("arenaX", "630")), cy = Integer.parseInt(j.getProperty("arenaY", "179184"));
        double radius = Double.parseDouble(j.getProperty("arenaRadius", "650"));
        return Math.hypot((double)x - cx, (double)y - cy) <= radius;
    }
    private static void returnAcknowledged(Player p)
    {
        Session s = SESSIONS.get(p.getObjectId());
        if (s != null) synchronized (s) { if (!s.closed) return; } // entry ACK never alters preparation
        recover(p); // the existing durable return/GC guard owns this path
    }
    static boolean returnMotionAllowed(boolean online, boolean teleporting, boolean arrived, boolean moving, boolean busy, boolean controlled)
    { return online && !teleporting && arrived && !moving && !busy && !controlled; }
    private static void synchronizeReturnOnce(Player p, Properties j) throws IOException
    {
        if (!Boolean.parseBoolean(j.getProperty("returnRequested", "false")) || Boolean.parseBoolean(j.getProperty("returnMotionSynced", "false"))) return;
        // Mark BEFORE work, so failed storage/GC retries cannot repeatedly stop
        // a returned human. If fresh gameplay began, skip the reset altogether.
        j.setProperty("returnMotionSynced", "true"); save(p.getObjectId(), j);
        if (!returnMotionAllowed(p.isOnline(), p.isTeleporting(), returned(p, j), p.isMoving(),
            p.isInCombat() || p.isAttackingOrCastingNow() || p.isInDuel() || p.isInOlympiadMode() || p.isProcessingTransaction(),
            p.isOutOfControl() || p.isMovementDisabled())) return;
        p.getAI().setIntentionIdle(); cancelSavedActions(p);
        p.stopMove(null);
        p.setLastServerPosition(p.getX(), p.getY(), p.getZ());
        StopMove packet = new StopMove(p); p.sendPacket(packet); p.broadcastPacket(packet);
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
                { retireZoneAuthority(j); cleanupBot(j); Files.deleteIfExists(path(p.getObjectId())); return; } // GC ONLY: never alter a returned human again.
                if (!p.isOnline() || p.isTeleporting()) return;
                if (!returned(p, j)) { stop(p); returnPlayer(p, j); return; }
                synchronizeReturnOnce(p, j);
                // Core teleport ack completed; store outside instance before journal removal.
                // returnPlayer already released our flags before teleport. Never stomp
                // new ordinary combat/control/trade state acquired after the return ack.
                if (confirmReturnStored(p))
                {
                    j.setProperty("humanReturned", "true"); save(p.getObjectId(), j);
                    retireZoneAuthority(j);
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
