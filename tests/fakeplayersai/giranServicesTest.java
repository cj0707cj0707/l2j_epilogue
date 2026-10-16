package custom.GiranServices;

public final class giranServicesTest
{
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static void invalid(String text)
    {
        boolean failed=false; try { Preparation.parse(text); } catch (IllegalArgumentException e) { failed=true; }
        check(failed);
    }
    public static void main(String[] args) throws Exception
    {
        check(Preparation.parse(null).isEmpty()); check(Preparation.parse("").isEmpty());
        check(Preparation.parse("1,42,2147483647").size()==3);
        for (String value:new String[]{"0","-1","1,1","x","1,","2147483648","1, 2"}) invalid(value);
        invalid(String.join(",", java.util.Collections.nCopies(33,"1")));
        check(GiranServices.escape(null).equals(""));
        check(GiranServices.escape("<&\"test>").equals("&lt;&amp;&quot;test&gt;"));
        check(GiranServices.escape("Тренировка").equals("Тренировка"));
        check(GiranServices.npcEvent(null)==null);
        check(GiranServices.npcEvent("Script GiranServices start")==null);
        check(GiranServices.npcEvent("GiranServiceX start")==null);
        check(GiranServices.npcEvent("GiranService ")==null);
        check(GiranServices.npcEvent("GiranService "+"x".repeat(101))==null);
        check(GiranServices.npcEvent("GiranService start").equals("start"));
        check(GiranServices.npcEvent("GiranService class 131").equals("class 131"));
        check(GiranServices.npcEvent("GiranService   main  ").equals("main"));
        java.lang.reflect.Field helpField = GiranServices.class.getDeclaredField("HELP"); helpField.setAccessible(true);
        java.util.Map<?,?> help = (java.util.Map<?,?>)helpField.get(null);
        String trainingHelp = (String)help.get("training");
        check(trainingHelp.contains("10–60 сек.") && trainingHelp.contains("по умолчанию 10"));
        check(trainingHelp.contains("85 уровня") && trainingHelp.contains("настройками снаряжения и заточки умений"));
        check(trainingHelp.contains("за 1 сек. до начала боя") && trainingHelp.contains("не после боя"));
        check(trainingHelp.contains("Откаты обоих сбрасываются ДО таймера селфбафов"));
        check(trainingHelp.contains("Frenzy + Zealot") && trainingHelp.contains("кастетчик — Zealot"));
        check(trainingHelp.contains("без минимального HP/CP/MP") && trainingHelp.contains("бафы и дебафы снимаются"));
        check(trainingHelp.contains("42705/188869/-3513") && trainingHelp.contains("44112/188116/-3513"));
        check(trainingHelp.contains("кастует разрешённые классовые селфбафы") && trainingHelp.contains("1600"));
        check(trainingHelp.contains("до остатка общего лимита 120 сек.") && trainingHelp.contains("победа боту автоматически не присуждается"));
        check(!trainingHelp.contains("30–60") && !trainingHelp.contains("Сброса откатов игрока нет"));
        System.out.println("PASS: "+checks+" production preparation parser/HTML checks (no live NPC)");
    }
}
