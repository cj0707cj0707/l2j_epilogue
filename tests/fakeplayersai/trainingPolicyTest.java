package custom.fakeplayersai;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.actor.enums.player.DuelResult;

/** Production helper tests only: no server/database/player construction, no live duel. */
public final class trainingPolicyTest
{
    private static int checks;
    private static void check(boolean yes, String why) { checks++; if (!yes) throw new AssertionError(why); }
    // Constructor-free fixtures ONLY in isolated test JVM. Core constructors
    // schedule tasks / require DB; these fixtures do not represent a live duel.
    private static Object allocate(Class<?> type) throws Exception
    {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
    private static void field(Object target, String name, Object value) throws Exception
    {
        Class<?> type = target.getClass();
        while (type != null)
        {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; }
            catch (NoSuchFieldException e) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    private static Object field(Object target, String name) throws Exception
    {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    private static void captureOnceContract(Player human, Player bot, Duel duel) throws Exception
    {
        Class<?> choiceClass = Class.forName("custom.fakeplayersai.TrainingArena$Choice");
        Constructor<?> choiceCtor = choiceClass.getDeclaredConstructors()[0]; choiceCtor.setAccessible(true);
        Object choice = choiceCtor.newInstance(108, "normal", "custom");
        Class<?> sessionClass = Class.forName("custom.fakeplayersai.TrainingArena$Session");
        Constructor<?> sessionCtor = sessionClass.getDeclaredConstructors()[0]; sessionCtor.setAccessible(true);
        Object session = sessionCtor.newInstance(human, choice, new Properties());
        field(session, "bot", bot); field(session, "duel", duel);
        field(human, "_objectId", 11); field(bot, "_objectId", 22); field(duel, "_duelId", 7);
        field(human, "_isInDuel", true); field(bot, "_isInDuel", true);
        field(human, "_duelId", 7); field(bot, "_duelId", 7);
        Method capture = TrainingArena.class.getDeclaredMethod("captureOfficialResult", sessionClass); capture.setAccessible(true);
        // Use the actual core getter/resolution, with a preexisting bot-surrender
        // field to avoid calling live AI/network stopFighting in this fixture.
        field(human, "_isOnline", true); field(bot, "_isOnline", true);
        field(duel, "_surrenderRequest", 2); duel.onPlayerDefeat(bot); duel.setFinished(false);
        capture.invoke(null, session);
        check(!(boolean)field(session, "officialResultCaptured") && (int)field(session, "winnerId") == 0, "capture waits for actual core finished flag");
        duel.setFinished(true); capture.invoke(null, session);
        check((boolean)field(session, "officialResultCaptured") && (int)field(session, "winnerId") == 11, "capture resolves actual core winner before restore");
        check("Official duel result: TEAM_2_SURRENDER".equals(field(session, "officialReason")), "capture retains exact observed core reason");
        // Restore erases winner and a later cleanup changes stop reason: neither
        // may replace the captured official winner/reason with bot win/unknown.
        field(human, "_isInDuel", false); field(bot, "_isInDuel", false);
        human.setDuelState(Duel.DUELSTATE_NODUEL); bot.setDuelState(Duel.DUELSTATE_NODUEL);
        field(session, "reason", "Safety stop"); capture.invoke(null, session);
        check((int)field(session, "winnerId") == 11 && "Official duel result: TEAM_2_SURRENDER".equals(field(session, "officialReason")), "official result survives restore and cleanup unchanged");
        Object missed = sessionCtor.newInstance(human, choice, new Properties());
        field(missed, "bot", bot); field(missed, "duel", duel); capture.invoke(null, missed);
        check(!(boolean)field(missed, "officialResultCaptured") && (int)field(missed, "winnerId") == 0, "missed restore window is unknown, never inferred from old damage");
    }
    private static void actualCoreDefeatContract() throws Exception
    {
        Player human = (Player)allocate(Player.class), bot = (Player)allocate(Player.class);
        Duel duel = (Duel)allocate(Duel.class);
        field(duel, "_playerA", human); field(duel, "_playerB", bot);
        field(human, "_isOnline", true); field(bot, "_isOnline", true);
        human.setDuelState(Duel.DUELSTATE_DUELLING); bot.setDuelState(Duel.DUELSTATE_DUELLING);
        duel.onPlayerDefeat(bot); // actual GameServer.jar method, not a model
        check(bot.getDuelState() == Duel.DUELSTATE_DEAD && human.getDuelState() == Duel.DUELSTATE_WINNER, "actual core: bot defeat marks human winner");
        check(!bot.isDead() && !human.isDead(), "duel defeat is not creature death");
        check(duel.getWinner() == null, "actual core: winner unavailable before finished");
        check(TrainingArena.naturalWinnerId(human.getDuelState(), bot.getDuelState(), 11, 22) == 11, "pending core defeat retained while getter null");
        duel.setFinished(true);
        check(duel.getWinner() == human, "actual core: finished exposes human winner");
        duel.setFinished(false); duel.onPlayerDefeat(human);
        check(human.getDuelState() == Duel.DUELSTATE_DEAD && bot.getDuelState() == Duel.DUELSTATE_WINNER, "actual core: human defeat marks bot winner");
        check(duel.getWinner() == null, "actual core: inverse pending getter still null");
        duel.setFinished(true); check(duel.getWinner() == bot, "actual core: inverse finished winner");
        // Actual core selection priority: even existing WINNER loses to timeout,
        // and surrender outranks both. No live AI/network/task initialization.
        Calendar expired = Calendar.getInstance(); expired.setTimeInMillis(System.currentTimeMillis() - 10000);
        field(duel, "_duelEndTime", expired);
        check(duel.checkEndDuelCondition() == DuelResult.TIMEOUT, "actual core: timeout takes precedence over WINNER state");
        field(duel, "_surrenderRequest", 1);
        check(duel.checkEndDuelCondition() == DuelResult.TEAM_1_SURRENDER, "actual core: human surrender outranks timeout and winner");
        field(duel, "_surrenderRequest", 2);
        check(duel.checkEndDuelCondition() == DuelResult.TEAM_2_SURRENDER, "actual core: bot surrender team identity");
        field(bot, "_isOnline", false);
        check(duel.checkEndDuelCondition() == DuelResult.CANCELED, "actual core: disconnect cancels before surrender");
        // Exercise actual core reset methods through production pre-selfbuff helper.
        Map<Integer,Long> disabled = new HashMap<>(); disabled.put(123, Long.MAX_VALUE);
        Map<Integer,Object> timestamps = new HashMap<>(); timestamps.put(123, null);
        Map<Integer,Object> itemReuse = new HashMap<>(); itemReuse.put(456, null);
        field(human, "_disabledSkills", disabled); field(human, "_reuseTimeStampsSkills", timestamps); field(human, "_reuseTimeStampsItems", itemReuse);
        TrainingArena.resetPreparationCooldowns(human);
        check(disabled.isEmpty() && timestamps.isEmpty(), "preparation reset clears actual disabled and skill reuse maps");
        check(itemReuse.size() == 1, "preparation reset preserves item reuse");
        human.setDuelState(Duel.DUELSTATE_NODUEL); bot.setDuelState(Duel.DUELSTATE_NODUEL);
        check(duel.getWinner() == null, "actual core: restored states erase winner getter");
        captureOnceContract(human, bot, duel);
    }
    private static void settingsAndApproachContract() throws Exception
    {
        for (String value : new String[]{null, "", "abc", "2147483648", "-2147483649"})
            check(TacticalSettings.parsePreparationSeconds(value) == 10, "invalid config defaults10: " + value);
        for (int value : new int[]{Integer.MIN_VALUE, -1, 0, 9, 10, 11, 30, 59, 60, 61, Integer.MAX_VALUE})
            check(TacticalSettings.parsePreparationSeconds(" " + value + " ") == Math.max(10, Math.min(60, value)), "config clamp boundary " + value);
        Path file = Path.of("config/Custom/OlympiadTactics.ini"); Files.createDirectories(file.getParent());
        Field initialized = TacticalSettings.class.getDeclaredField("initialized"); initialized.setAccessible(true); initialized.setBoolean(null, false);
        check(!Files.exists(file) && TacticalSettings.getTrainingPreparationSeconds() == 10, "missing temp config defaults10");
        for (String value : new String[]{"0", "10", "45", "999", "bad"})
        {
            Files.writeString(file, "TrainingEnabled=true\nTrainingPreparationSeconds=" + value + "\nDefaultDifficulty=strong\n");
            initialized.setBoolean(null, false);
            int expected = TacticalSettings.parsePreparationSeconds(value);
            check(TacticalSettings.getTrainingPreparationSeconds() == expected, "actual config load " + value);
            check(TacticalSettings.saveDifficulty("normal"), "actual config save " + value);
            Properties saved = new Properties(); try (var input = Files.newInputStream(file)) { saved.load(input); }
            check(Integer.toString(expected).equals(saved.getProperty("TrainingPreparationSeconds")), "save preserves bounded prep " + value);
            initialized.setBoolean(null, false);
            check(TacticalSettings.getTrainingPreparationSeconds() == expected, "load/save roundtrip " + value);
        }
        for (int reach : new int[]{20, 40, 45, 47, 52, 60, 600, 1000})
            check(TrainingArena.approachWithMargin(reach) == reach - 10, "approach inside effective learned range " + reach);
        Files.delete(file); initialized.setBoolean(null, false);
    }
    private static void resetAndReturnContract() throws Exception
    {
        Player p = (Player)allocate(Player.class);
        Field pending = Player.class.getDeclaredField("_startingDuel"); pending.setAccessible(true);
        check(!TrainingArena.startingDuelPending(p, pending), "actual pending-duel flag false");
        p.setStartingDuel();
        check(TrainingArena.startingDuelPending(p, pending), "actual pending-duel flag blocks even with uninitialized/low resources");
        check(TrainingArena.startingDuelPending(p, null) && TrainingArena.startingDuelPending(null, pending), "pending reflection missing/null fails closed");
        Field wrong = Duel.class.getDeclaredField("_surrenderRequest"); wrong.setAccessible(true);
        check(TrainingArena.startingDuelPending(p, wrong), "wrong reflection schema fails closed");
        check(!TrainingArena.isPreparingBot(null) && !TrainingArena.isTrainingInstance(0) && !TrainingArena.isTrainingInstance(-1), "preparation/instance null guards");
        Player bot = (Player)allocate(Player.class); field(p, "_objectId", 11); field(bot, "_objectId", 22);
        Class<?> choiceClass=Class.forName("custom.fakeplayersai.TrainingArena$Choice");
        Constructor<?> choiceCtor=choiceClass.getDeclaredConstructors()[0];choiceCtor.setAccessible(true);
        Class<?> sessionClass=Class.forName("custom.fakeplayersai.TrainingArena$Session");
        Constructor<?> sessionCtor=sessionClass.getDeclaredConstructors()[0];sessionCtor.setAccessible(true);
        Object session=sessionCtor.newInstance(p,choiceCtor.newInstance(108,"normal","custom"),new Properties());field(session,"bot",bot);
        Field sessionsField=TrainingArena.class.getDeclaredField("SESSIONS");sessionsField.setAccessible(true);
        Field botsField=TrainingArena.class.getDeclaredField("BOTS");botsField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer,Object> sessions=(Map<Integer,Object>)sessionsField.get(null);
        @SuppressWarnings("unchecked") java.util.Set<Integer> bots=(java.util.Set<Integer>)botsField.get(null);
        sessions.put(11,session);bots.add(22);
        try
        {
            for(int mask=0;mask<32;mask++)
            {
                boolean preparing=(mask&1)!=0,active=(mask&2)!=0,stopping=(mask&4)!=0,finalizing=(mask&8)!=0,closed=(mask&16)!=0;
                field(session,"preparationStarted",preparing);field(session,"active",active);field(session,"stopping",stopping);field(session,"finalizing",finalizing);field(session,"closed",closed);
                check(TrainingArena.isPreparingBot(bot)==(preparing&&!active&&!stopping&&!finalizing&&!closed),"exact bot preparation authority " + mask);
                check(!TrainingArena.isPreparingBot(p),"human never admitted to bot preparation helper " + mask);
            }
            field(session,"humanStopApplied",true);
            Method stopOnce=TrainingArena.class.getDeclaredMethod("stopParticipantsOnce",sessionClass);stopOnce.setAccessible(true);
            field(session,"bot",null); // repeated stop must not dereference or mutate either actor
            stopOnce.invoke(null,session);stopOnce.invoke(null,session);
            check((boolean)field(session,"humanStopApplied"),"stopping pulses return immediately after one owned human stop");
        }
        finally { sessions.remove(11);bots.remove(22); }
        Method syncOnce=TrainingArena.class.getDeclaredMethod("synchronizeReturnOnce",Player.class,Properties.class);syncOnce.setAccessible(true);
        Properties synced=new Properties();synced.setProperty("returnRequested","true");synced.setProperty("returnMotionSynced","true");
        syncOnce.invoke(null,null,synced);syncOnce.invoke(null,null,synced);
        check("true".equals(synced.getProperty("returnMotionSynced")),"repeated ACK/storage retries never dereference/mutate a returned actor");
        org.l2jmobius.gameserver.ai.PlayerAI ai = (org.l2jmobius.gameserver.ai.PlayerAI)allocate(org.l2jmobius.gameserver.ai.PlayerAI.class);
        field(ai, "_actor", p); field(p, "_ai", ai);
        field(ai, "_savedIntention", org.l2jmobius.gameserver.ai.Intention.MOVE_TO);
        field(ai, "_savedMoveTo", new org.l2jmobius.gameserver.entity.Location(1,2,3));
        ai.setNextAction((org.l2jmobius.gameserver.ai.NextAction)allocate(org.l2jmobius.gameserver.ai.NextAction.class));
        TrainingArena.cancelSavedActions(p); // real PlayerAI.cancel, no spawned actor/network/server
        check(ai.getNextIntention() == null && field(ai, "_savedMoveTo") == null && ai.getNextAction() == null, "actual core CANCEL clears saved move and deferred action; Idle alone did not");
        for (int mask=0; mask<64; mask++)
        {
            boolean online=(mask&1)!=0, teleport=(mask&2)!=0, arrived=(mask&4)!=0, moving=(mask&8)!=0, busy=(mask&16)!=0, controlled=(mask&32)!=0;
            check(TrainingArena.returnMotionAllowed(online,teleport,arrived,moving,busy,controlled) == (online&&!teleport&&arrived&&!moving&&!busy&&!controlled), "ACK cleanup never stomps fresh player actions " + mask);
        }
        long deadline=10_000_000_000L, drain=9_100_000_000L;
        for (long now : new long[]{8_999_999_999L,9_000_000_000L,9_099_999_999L,9_100_000_000L,10_000_000_000L,20_000_000_000L})
            for (boolean casting : new boolean[]{false,true})
                check(TrainingArena.finalSnapshotReady(now,deadline,drain,casting)==(!casting&&now>=drain&&now>=deadline-1_000_000_000L), "final snapshot waits for both casting drain and last1s boundary " + now + "/" + casting);
        check(TrainingArena.inside(44112,188116) && TrainingArena.inside(42705,188869), "both exact requested spawns inside private arena");
        double gap=Math.hypot(44112-42705,188116-188869);
        check(gap<1600 && gap>1595, "exact requested separation remains just inside actual core1600 cancellation limit");
        Method inside = TrainingArena.class.getDeclaredMethod("insideJournalArena", int.class,int.class,Properties.class); inside.setAccessible(true);
        Properties journal=new Properties();
        check((boolean)inside.invoke(null,630,179184,journal), "old Dion arena journal location retained across migration");
        journal.setProperty("arenaX","43409");journal.setProperty("arenaY","188493");journal.setProperty("arenaRadius","1000");
        check((boolean)inside.invoke(null,44112,188116,journal) && !(boolean)inside.invoke(null,630,179184,journal), "new journal records its exact arena bounds");
    }
    public static void main(String[] args) throws Exception
    {
        check("normal".equals(TrainingArena.option(" Normal ", "normal", "experienced", "strong")), "difficulty normalization");
        check("experienced".equals(TrainingArena.option("EXPERIENCED", "normal", "experienced", "strong")), "experienced accepted");
        check("strong".equals(TrainingArena.option("strong", "normal", "experienced", "strong")), "strong accepted");
        check(TrainingArena.option("master", "normal", "experienced", "strong") == null, "no invented difficulty");
        check(TrainingArena.option(null, "normal") == null, "no null choice default");
        check(TrainingArena.option("equal; DROP TABLE", "custom", "equal") == null, "untrusted selection rejected");
        check("equal".equals(TrainingArena.option(" equal ", "custom", "equal")), "equal profile accepted");
        check(TrainingArena.inside(43409, 188493), "arena center");
        check(TrainingArena.inside(44409, 188493), "radius boundary");
        check(!TrainingArena.inside(44410, 188493), "outside boundary");
        check(!TrainingArena.inside(Integer.MAX_VALUE, Integer.MIN_VALUE), "no coordinate overflow");
        for(int mask=0;mask<64;mask++)
        {
            boolean requested=(mask&1)!=0, has=(mask&2)!=0, finished=(mask&4)!=0, human=(mask&8)!=0, bot=(mask&16)!=0, mapped=(mask&32)!=0;
            boolean safe=TrainingArena.canDispose(requested,has,finished,human,bot,mapped);
            check(safe==(!requested || has&&finished&&!human&&!bot&&!mapped),"core-task quarantine state "+mask);
        }
        check(!TrainingArena.canDispose(true,false,false,false,false,false),"unknown pending startup NEVER disposed by elapsed time");
        check(!TrainingArena.canDispose(true,true,true,false,false,true),"manager still finishing forbids disposal");
        check("&lt;bot&gt;&amp;&quot;&#39;".equals(CombatAnalytics.escape("<bot>&\"'")), "escape untrusted labels");
        check("".equals(CombatAnalytics.escape(null)), "null HTML safe");
        check(!TrainingArena.isParticipant(null) && !TrainingArena.isTrainingBot(null), "null guards");
        check(TrainingArena.repeat(null).contains("недоступен"), "null repeat");
        check(CombatAnalytics.report(null).contains("unavailable"), "null report");
        check(TrainingArena.preparationSeconds(Integer.MIN_VALUE) == 10, "negative prep is clamped to minimum");
        check(TrainingArena.preparationSeconds(0) == 10, "zero prep cannot bypass protection");
        check(TrainingArena.preparationSeconds(9) == 10, "minimum prep 10 seconds");
        check(TrainingArena.preparationSeconds(10) == 10, "default prep 10 seconds");
        check(TrainingArena.preparationSeconds(11) == 11, "above minimum preserved");
        check(TrainingArena.preparationSeconds(30) == 30, "legacy configured prep preserved");
        check("custom".equals(TrainingArena.trainingProfile("equal")), "legacy equal normalizes to ranked baseline");
        check("custom".equals(TrainingArena.trainingProfile(" EQUAL ")), "legacy equal normalization case insensitive");
        check("custom".equals(TrainingArena.trainingProfile("custom")), "custom keeps ranked baseline");
        check(TrainingArena.trainingProfile("bogus") == null && TrainingArena.trainingProfile(null) == null, "unknown profile not silently accepted");
        check(TrainingArena.preparationSeconds(45) == 45, "configured prep preserved");
        check(TrainingArena.preparationSeconds(60) == 60, "maximum prep");
        check(TrainingArena.preparationSeconds(Integer.MAX_VALUE) == 60, "prep bounded within core duel lifetime");
        check(TrainingArena.remainingSeconds(31_000_000_000L, 1_000_000_000L) == 30, "full selfbuff window");
        check(TrainingArena.remainingSeconds(31_000_000_000L, 1_000_000_001L) == 30, "countdown rounds up");
        check(TrainingArena.remainingSeconds(31_000_000_000L, 30_999_999_999L) == 1, "last nanosecond still protected");
        check(TrainingArena.remainingSeconds(31_000_000_000L, 31_000_000_000L) == 0, "release at deadline only");
        check(TrainingArena.remainingSeconds(31_000_000_000L, 32_000_000_000L) == 0, "no negative countdown");
        for (int mask = 0; mask < 32; mask++)
        {
            boolean human=(mask&1)!=0, preparing=(mask&2)!=0, stopping=(mask&4)!=0, negative=(mask&8)!=0, self=(mask&16)!=0;
            check(TrainingArena.prepSkillAllowed(human, preparing, stopping, negative, self)
                == (human && preparing && !stopping && !negative && self), "protected selfbuff admission truth table " + mask);
        }
        actualCoreDefeatContract();
        settingsAndApproachContract();
        resetAndReturnContract();
        for (DuelResult result : DuelResult.values())
        {
            int expected = switch (result) { case TEAM_1_WIN, TEAM_2_SURRENDER -> 11; case TEAM_2_WIN, TEAM_1_SURRENDER -> 22; default -> 0; };
            check(TrainingArena.officialWinnerId(false, result, expected, 11, 22) == 0, "pending is never official " + result);
            check(TrainingArena.officialWinnerId(true, result, expected, 11, 22) == expected, "finished result team mapping " + result);
            check(TrainingArena.officialWinnerId(true, result, 99, 11, 22) == 0, "foreign winner rejected " + result);
            check(TrainingArena.officialWinnerId(true, result, 0, 11, 22) == 0, "missing finished getter never guessed " + result);
        }
        check(TrainingArena.officialWinnerId(true, DuelResult.TIMEOUT, 11, 11, 22) == 0, "timeout not inferred win from stale WINNER state");
        check(TrainingArena.officialWinnerId(true, DuelResult.CANCELED, 22, 11, 22) == 0, "cancellation not inferred loss");
        check(TrainingArena.officialWinnerId(true, null, 11, 11, 22) == 0, "null result unknown");
        for (int h = 0; h <= 4; h++) for (int b = 0; b <= 4; b++)
            check(TrainingArena.naturalWinnerId(h,b,11,22) == (h == 3 && b == 2 ? 11 : h == 2 && b == 3 ? 22 : 0), "only complete DEAD/WINNER pairs latch " + h + "/" + b);
        for (int mask=0; mask<16; mask++) for (int remaining : new int[]{-1,0,1,3,4,30})
        {
            boolean applied=(mask&1)!=0, stopping=(mask&2)!=0, active=(mask&4)!=0, casting=(mask&8)!=0;
            check(TrainingArena.openerWindow(applied,stopping,active,remaining,casting) == (!applied&&!stopping&&!active&&remaining<=3&&!casting), "one-shot safe opener window " + mask + "/" + remaining);
        }
        check(TrainingArena.trainingEndReason("Official duel result: TEAM_1_WIN", "Duel completed").equals("Official duel result: TEAM_1_WIN"), "natural win reason retained");
        check(TrainingArena.trainingEndReason("Official duel result: TEAM_1_WIN", "Safety stop").contains("TEAM_1_WIN; stop: Safety stop"), "cleanup cannot erase natural outcome");
        check(TrainingArena.trainingEndReason(null, "Left training").contains("Unknown:") && TrainingArena.trainingEndReason(null, "Left training").contains("Left training"), "unobserved quit is unknown, not loss");
        check(CombatAnalytics.resultLabel(true,11,11,"Official duel result: TEAM_1_WIN").equals("Duel win — Official duel result: TEAM_1_WIN"), "training victory retains official reason");
        check(CombatAnalytics.resultLabel(true,11,22,"Official duel result: TEAM_2_WIN").startsWith("Duel loss — "), "training defeat retains reason");
        check(CombatAnalytics.resultLabel(true,11,0,"Official duel result: TIMEOUT").equals("Official duel result: TIMEOUT"), "no manufactured timeout loser");
        check(CombatAnalytics.resultLabel(false,11,11,"Official result").equals("Official win"), "ranked formatting unchanged");
        // Exercise the exact production atomic journal writer/reader in the test's TEMP cwd.
        Path dir = Paths.get("data/training-arena-recovery"); Files.createDirectories(dir);
        Method save = TrainingArena.class.getDeclaredMethod("save", int.class, Properties.class); save.setAccessible(true);
        Method read = TrainingArena.class.getDeclaredMethod("read", Path.class); read.setAccessible(true);
        Properties j = new Properties(); j.setProperty("account", "__training_arena__"); j.setProperty("botName", "Tr0123456789abc");
        j.setProperty("x", "123"); j.setProperty("player", "19");
        save.invoke(null, 19, j);
        Path file = dir.resolve("19.properties");
        Properties loaded = (Properties)read.invoke(null, file);
        check("123".equals(loaded.getProperty("x")), "durable return journal roundtrip");
        check(!Files.exists(dir.resolve("19.tmp")), "no unfinished temp after atomic rename");
        j.setProperty("x", "456"); save.invoke(null, 19, j);
        check("456".equals(((Properties)read.invoke(null, file)).getProperty("x")), "atomic replacement");
        j.setProperty("account", "real-user"); save.invoke(null, 19, j);
        try { read.invoke(null, file); throw new AssertionError("foreign account accepted"); }
        catch (InvocationTargetException e) { check(e.getCause() instanceof java.io.IOException, "foreign ownership rejected"); }
        j.setProperty("account", "__training_arena__"); j.setProperty("botName", "RegisteredBot"); save.invoke(null, 19, j);
        try { read.invoke(null, file); throw new AssertionError("roster name accepted"); }
        catch (InvocationTargetException e) { check(e.getCause() instanceof java.io.IOException, "foreign bot name rejected"); }
        Files.delete(file);
        System.out.println("PASS: " + checks + " production training policy/HTML/journal checks (no live server or DB)");
    }
}
