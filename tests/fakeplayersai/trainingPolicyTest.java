package custom.fakeplayersai;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;

/** Production helper tests only: no server/database/player construction, no live duel. */
public final class trainingPolicyTest
{
    private static int checks;
    private static void check(boolean yes, String why) { checks++; if (!yes) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception
    {
        check("normal".equals(TrainingArena.option(" Normal ", "normal", "experienced", "strong")), "difficulty normalization");
        check("experienced".equals(TrainingArena.option("EXPERIENCED", "normal", "experienced", "strong")), "experienced accepted");
        check("strong".equals(TrainingArena.option("strong", "normal", "experienced", "strong")), "strong accepted");
        check(TrainingArena.option("master", "normal", "experienced", "strong") == null, "no invented difficulty");
        check(TrainingArena.option(null, "normal") == null, "no null choice default");
        check(TrainingArena.option("equal; DROP TABLE", "custom", "equal") == null, "untrusted selection rejected");
        check("equal".equals(TrainingArena.option(" equal ", "custom", "equal")), "equal profile accepted");
        check(TrainingArena.inside(630, 179184), "arena center");
        check(TrainingArena.inside(1280, 179184), "radius boundary");
        check(!TrainingArena.inside(1281, 179184), "outside boundary");
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
        check(TrainingArena.repeat(null).contains("unavailable"), "null repeat");
        check(CombatAnalytics.report(null).contains("unavailable"), "null report");
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
