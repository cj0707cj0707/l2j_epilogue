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
    public static void main(String[] args)
    {
        check(Preparation.parse(null).isEmpty()); check(Preparation.parse("").isEmpty());
        check(Preparation.parse("1,42,2147483647").size()==3);
        for (String value:new String[]{"0","-1","1,1","x","1,","2147483648","1, 2"}) invalid(value);
        invalid(String.join(",", java.util.Collections.nCopies(33,"1")));
        check(GiranServices.escape(null).equals(""));
        check(GiranServices.escape("<&\"test>").equals("&lt;&amp;&quot;test&gt;"));
        check(GiranServices.escape("Тренировка").equals("Тренировка"));
        System.out.println("PASS: "+checks+" production preparation parser/HTML checks (no live NPC)");
    }
}
