/* Local bounded combat telemetry. Never observes chat, mail, or private text. */
package custom.fakeplayersai;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.*;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.*;
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;

/**
 * report is an escaped NPC HTML fragment, current user only. init idempotent.
 * watch(bot,enemy) is an optional fast ranked hook; periodic discovery also works.
 * tickMetrics is a compatibility hook: sampling is timer-owned, avoiding double counts.
 * Damage is the synchronous ON_CREATURE_DAMAGE_DEALT payload (one side only),
 * NOT an invented HP delta; may include CP, overkill/reflection/DoT per server semantics.
 * ON_CREATURE_SKILL_USE means cast request, NOT successful hit/heal/effect application.
 * No healing quantity/defense success event exists: self-target casts are labeled only.
 * Deaths are observed; ranked winners come ONLY from ON_OLYMPIAD_MATCH_RESULT.
 * Unknown completion, timeout/disconnect are never inferred to be a victory.
 * Pursuit is sampled moving while not attacking/casting; idle is sampled no action.
 * Samples include CC/root etc: do not interpret them as bad AI without replay review.
 * Last ten bouts per human, max 2048 humans, memory only; no SQL/privacy collection.
 */
public final class CombatAnalytics
{
    private static final Logger LOG = Logger.getLogger(CombatAnalytics.class.getName());
    private static boolean initialized;
    private static final Map<Integer, Bout> ACTIVE = new HashMap<>();
    private static final Map<Integer, Deque<Summary>> HISTORY = new LinkedHashMap<Integer, Deque<Summary>>(128, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, Deque<Summary>> e) { return size() > 2048; }
    };
    private CombatAnalytics() { }
    static String escape(String s)
    {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private static final class Side
    {
        final int id, classId;
        final String name;
        double damage;
        long casts, selfCasts, deaths, movingMs, idleMs, noGainMs, healRequests, defenseRequests, controlRequests, controlEffects;
        double previousDistance = -1;
        final Map<Integer, Long> skills = new TreeMap<>();
        Side(Player p) { id = p.getObjectId(); classId = p.getPlayerClass().getId(); name = p.getName(); }
    }
    private static final class Bout
    {
        final boolean training;
        final int gameId;
        final String profile;
        final Side a, b;
        final long began = System.nanoTime();
        long lastSample = began;
        long inactiveAt;
        boolean ended;
        Bout(Player a, Player b, boolean training, String profile)
        { this.a = new Side(a); this.b = new Side(b); this.training = training; this.profile = profile; gameId = a.getOlympiadGameId(); }
        Side side(int id) { return a.id == id ? a : b.id == id ? b : null; }
    }
    private record Summary(boolean training, String opponent, int opponentClass, String profile, String result, long durationMs,
        double dealt, double received, long casts, long selfCasts, long deaths, long movingMs, long idleMs, long noGainMs, long heals, long defenses, long controls, long controlEffects, String skills) { }
    public static synchronized void init()
    {
        if (initialized) return;
        Consumer<OnCreatureDamageDealt> damage = CombatAnalytics::damage;
        Consumer<OnCreatureSkillUse> skill = CombatAnalytics::skill;
        Consumer<OnCreatureDeath> death = CombatAnalytics::death;
        Consumer<OnOlympiadMatchResult> result = CombatAnalytics::result;
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_DAMAGE_DEALT, damage, CombatAnalytics.class));
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_SKILL_USE, skill, CombatAnalytics.class));
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_DEATH, death, CombatAnalytics.class));
        Olympiad.getInstance().addListener(new ConsumerEventListener(Olympiad.getInstance(), EventType.ON_OLYMPIAD_MATCH_RESULT, result, CombatAnalytics.class));
        ThreadPool.scheduleAtFixedRate(CombatAnalytics::sample, 1000, 1000);
        initialized = true;
    }
    private static Player owner(Creature c)
    {
        if (c == null) return null;
        if (c.isPlayer()) return c.asPlayer();
        Creature summoner = c.getSummoner();
        return summoner != null && summoner.isPlayer() ? summoner.asPlayer() : null;
    }
    /** Optional parent hook, ranked only; no fabricated ranked state. */
    public static synchronized void watch(Player a, Player b)
    {
        init();
        if (a == null || b == null || a == b || TrainingArena.isParticipant(a) || TrainingArena.isParticipant(b)
            || TrainingArena.isTrainingBot(a) || TrainingArena.isTrainingBot(b)
            || !a.isInOlympiadMode() || !b.isInOlympiadMode() || !a.isOlympiadStart() || !b.isOlympiadStart()
            || a.getOlympiadGameId() != b.getOlympiadGameId()) return;
        begin(a, b, false, "ranked");
    }
    public static void tickMetrics(Player a, Player b) { watch(a, b); }
    public static synchronized void beginTraining(Player human, Player bot, String profile)
    {
        init();
        if (human != null && bot != null && TrainingArena.isParticipant(human) && TrainingArena.isTrainingBot(bot)
            && human.getInstanceId() > 0 && human.getInstanceId() == bot.getInstanceId()) begin(human, bot, true, profile);
    }
    private static void begin(Player a, Player b, boolean training, String profile)
    {
        Bout existing = ACTIVE.get(a.getObjectId());
        if (existing != null && existing.side(b.getObjectId()) != null && existing.training == training
            && (training || existing.gameId == a.getOlympiadGameId())) return;
        if (existing != null) finish(existing, "Unknown: replaced observation", 0);
        Bout other = ACTIVE.get(b.getObjectId()); if (other != null) finish(other, "Unknown: replaced observation", 0);
        Bout bout = new Bout(a, b, training, profile);
        ACTIVE.put(a.getObjectId(), bout); ACTIVE.put(b.getObjectId(), bout);
    }
    public static synchronized void endTraining(Player human, String reason, int winnerId)
    {
        if (human == null) return;
        Bout b = ACTIVE.get(human.getObjectId());
        if (b != null && b.training) finish(b, reason, b.side(winnerId) == null ? 0 : winnerId);
    }
    public static synchronized void tacticalRequest(int id, String kind)
    {
        Bout b = ACTIVE.get(id); if (b == null || b.ended) return;
        Side side = b.side(id); if (side == null) return;
        if ("HEAL".equals(kind)) side.healRequests++;
        else if ("DEFENSE".equals(kind)) side.defenseRequests++;
        else if ("CONTROL".equals(kind)) side.controlRequests++;
    }
    public static synchronized void controlEffectObserved(int id)
    {
        Bout b = ACTIVE.get(id); if (b != null && !b.ended && b.side(id) != null) b.side(id).controlEffects++;
    }

    public static synchronized void endTraining(Player human, String reason)
    {
        if (human == null) return;
        Bout b = ACTIVE.get(human.getObjectId()); if (b != null && b.training) finish(b, reason, 0);
    }
    private static synchronized void damage(OnCreatureDamageDealt e)
    {
        // Mutable pooled core payload: extract scalars immediately, never retain e or enqueue it.
        Player from = owner(e.getAttacker()), to = owner(e.getTarget());
        if (from == null || to == null || from == to || !Double.isFinite(e.getDamage()) || e.getDamage() <= 0) return;
        Bout b = ACTIVE.get(from.getObjectId());
        if (b == null || b != ACTIVE.get(to.getObjectId()) || b.ended) return;
        Side side = b.side(from.getObjectId()); if (side != null) side.damage += e.getDamage();
    }
    private static synchronized void skill(OnCreatureSkillUse e)
    {
        Player caster = owner(e.getCaster()); if (caster == null || e.getSkill() == null) return;
        Bout b = ACTIVE.get(caster.getObjectId()); if (b == null || b.ended) return;
        Side side = b.side(caster.getObjectId()); side.casts++;
        if (side.skills.size() < 64 || side.skills.containsKey(e.getSkill().getId())) side.skills.merge(e.getSkill().getId(), 1L, Long::sum);
        Player target = owner(e.getTarget());
        if (target == caster) side.selfCasts++;
    }
    private static synchronized void death(OnCreatureDeath e)
    {
        Player dead = owner(e.getTarget());
        // Pet death is not a human death.
        if (dead == null || e.getTarget() != dead) return;
        Bout b = ACTIVE.get(dead.getObjectId()); if (b != null && !b.ended) b.side(dead.getObjectId()).deaths++;
    }
    private static synchronized void result(OnOlympiadMatchResult e)
    {
        Player winner = e.getWinner(), loser = e.getLoser();
        Bout b = winner == null ? null : ACTIVE.get(winner.getObjectId());
        if (b == null && loser != null) b = ACTIVE.get(loser.getObjectId());
        if (b != null && !b.training && (winner == null || b.side(winner.getObjectId()) != null)
            && (loser == null || b.side(loser.getObjectId()) != null))
            finish(b, winner == null ? "Unknown result (no winner)" : "Official result", winner == null ? 0 : winner.getObjectId());
    }
    private static void sample()
    {
        try
        {
            // Discover every active ranked pair, not only roster bots. Optional watch reduces initial lag.
            for (Player p : World.getPlayers()) if (p.isInOlympiadMode() && p.isOlympiadStart())
            {
                Player[] pair = Olympiad.getInstance().getPlayers(p.getOlympiadGameId());
                if (pair != null && pair.length >= 2 && pair[0] != null && pair[1] != null) watch(pair[0], pair[1]);
            }
            synchronized (CombatAnalytics.class)
            {
                long now = System.nanoTime();
                for (Bout b : new HashSet<>(ACTIVE.values()))
                {
                    Player a = World.getPlayer(b.a.id), c = World.getPlayer(b.b.id);
                    boolean valid = a != null && c != null && a.isOnline() && c.isOnline()
                        && (b.training ? TrainingArena.isParticipant(a) || TrainingArena.isParticipant(c)
                            : a.isInOlympiadMode() && c.isInOlympiadMode() && a.isOlympiadStart() && c.isOlympiadStart()
                              && a.getOlympiadGameId() == b.gameId && c.getOlympiadGameId() == b.gameId);
                    if (!valid)
                    {
                        if (b.inactiveAt == 0) b.inactiveAt = now;
                        // Allow the asynchronous official result to arrive before declaring unknown.
                        if (now - b.inactiveAt > 10_000_000_000L) finish(b, "Unknown: ended/disconnected; no official result", 0);
                        continue;
                    }
                    b.inactiveAt = 0;
                    long ms = Math.min(1500, Math.max(0, (now - b.lastSample) / 1_000_000)); b.lastSample = now;
                    sampleSide(a, c, b.a, ms); sampleSide(c, a, b.b, ms);
                    if (now - b.began > 900_000_000_000L) finish(b, "Unknown: observation timeout", 0);
                }
            }
        }
        catch (Exception e) { LOG.log(Level.WARNING, "Combat analytics sampling failed", e); }
    }
    private static void sampleSide(Player p, Player enemy, Side side, long ms)
    {
        double distance = p.calculateDistance2D(enemy);
        if (p.isMoving() && !p.isAttackingOrCastingNow())
        {
            side.movingMs += ms;
            if (distance > 150 && side.previousDistance >= 0 && distance >= side.previousDistance - 10) side.noGainMs += ms;
        }
        side.previousDistance = distance;
        if (!p.isMoving() && !p.isAttackingOrCastingNow()) side.idleMs += ms;
    }
    private static void finish(Bout b, String reason, int winnerId)
    {
        if (b.ended) return;
        b.ended = true;
        ACTIVE.remove(b.a.id, b); ACTIVE.remove(b.b.id, b);
        long duration = Math.max(0, ((b.inactiveAt == 0 ? System.nanoTime() : b.inactiveAt) - b.began) / 1_000_000);
        remember(b, b.a, b.b, reason, winnerId, duration); remember(b, b.b, b.a, reason, winnerId, duration);
    }
    private static void remember(Bout b, Side own, Side opponent, String reason, int winnerId, long duration)
    {
        // No ephemeral training-bot history; no Player references are retained.
        if (b.training && TrainingArena.isTrainingBot(World.getPlayer(own.id))) return;
        String result = winnerId == 0 ? reason : winnerId == own.id ? (b.training ? "Duel win" : "Official win") : (b.training ? "Duel loss" : "Official loss");
        Deque<Summary> rows = HISTORY.computeIfAbsent(own.id, k -> new ArrayDeque<>());
        rows.addFirst(new Summary(b.training, opponent.name, opponent.classId, b.profile, result, duration, own.damage, opponent.damage,
            own.casts, own.selfCasts, own.deaths, own.movingMs, own.idleMs, own.noGainMs, own.healRequests, own.defenseRequests, own.controlRequests, own.controlEffects, skillSummary(own.skills)));
        while (rows.size() > 10) rows.removeLast();
    }
    private static String skillSummary(Map<Integer, Long> skills)
    {
        StringBuilder text = new StringBuilder();
        int n = 0;
        for (Map.Entry<Integer, Long> e : skills.entrySet())
        {
            if (n++ == 8) { text.append(" ..."); break; }
            if (text.length() > 0) text.append(", ");
            text.append(e.getKey()).append('=').append(e.getValue());
        }
        return text.toString();
    }
    public static synchronized String report(Player p)
    {
        if (p == null) return "<br>Player unavailable.<br>";
        StringBuilder out = new StringBuilder("<br><font color=LEVEL>Combat analytics</font><br>");
        Bout active = ACTIVE.get(p.getObjectId());
        if (active != null) out.append(active.training ? "Training" : "Ranked").append(" observation active: ")
            .append((System.nanoTime() - active.began) / 1_000_000_000L).append("s<br>");
        Deque<Summary> rows = HISTORY.get(p.getObjectId());
        if (rows == null || rows.isEmpty()) out.append("No completed observations this server run.<br>");
        else for (boolean training : new boolean[]{false, true})
        {
            out.append(training ? "<br>TRAINING (unranked)<br>" : "<br>RANKED<br>");
            Map<Integer,int[]> totals = new TreeMap<>();
            for (Summary r : rows) if (r.training == training)
            {
                int[] v=totals.computeIfAbsent(r.opponentClass,k->new int[3]); v[0]++;
                if (r.result.endsWith("win")) v[1]++; else if (r.result.endsWith("loss")) v[2]++;
            }
            for (Map.Entry<Integer,int[]> e:totals.entrySet())
            {
                BotClassPreset preset=BotPresets.getPresetByClassId(e.getKey());
                out.append("Класс ").append(preset==null?e.getKey():escape(preset.getClassName()))
                    .append(": боёв ").append(e.getValue()[0]).append(", побед ").append(e.getValue()[1]).append(", поражений ").append(e.getValue()[2]).append("<br>");
            }
            for (Summary r : rows) if (r.training == training)
            {
                out.append("vs ").append(escape(r.opponent)).append(" [").append(escape(r.profile)).append("]<br>")
                   .append(escape(r.result)).append("; observed ").append(r.durationMs / 1000).append("s<br>")
                   .append("Damage events dealt/received: ").append(Math.round(r.dealt)).append(" / ").append(Math.round(r.received)).append("<br>")
                   .append("Cast requests/self-target/deaths: ").append(r.casts).append(" / ").append(r.selfCasts).append(" / ").append(r.deaths).append("<br>")
                   .append("Moving-not-casting / no-action samples: ").append(r.movingMs / 1000).append("s / ").append(r.idleMs / 1000).append("s<br>")
                   .append("Движение без сближения (выборка): ").append(r.noGainMs/1000).append("s<br>")
                   .append("Запросы лечения/защиты/контроля; наблюдённые эффекты контроля: ")
                   .append(r.heals).append(" / ").append(r.defenses).append(" / ").append(r.controls).append("; ").append(r.controlEffects).append("<br>")
                   .append("Skill ID=request count: ").append(escape(r.skills)).append("<br>");
            }
        }
        return out.append("<br>Memory-only: last 10 bouts. Duration starts at observation, not official match start. Damage is server event payload, not HP loss. Cast requests are not confirmed hits/heals. Defense success and healing amounts unavailable. Tactical requests count only accepted useMagic; a control effect observed later is not a mathematically exact landing probability. Movement/idle are sampled, including control effects.<br>").toString();
    }
}
