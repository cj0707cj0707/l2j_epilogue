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
        check(TrainingArena.repeat(null).contains("недоступен"), "null repeat");
        check(CombatAnalytics.report(null).contains("unavailable"), "null report");
        check(TrainingArena.preparationSeconds(Integer.MIN_VALUE) == 30, "negative prep is clamped to minimum");
        check(TrainingArena.preparationSeconds(0) == 30, "zero prep cannot bypass protection");
        check(TrainingArena.preparationSeconds(29) == 30, "minimum prep 30 seconds");
        check(TrainingArena.preparationSeconds(30) == 30, "default prep");
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
