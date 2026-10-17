package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import sun.misc.Unsafe;

/** Actual production nickname/timing/grant methods and actual skill templates; no DB/server. */
public final class CardinalNameProminenceTest
{
    private static int checks;
    private static Unsafe unsafe;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    private static Field field(Class<?> c, String name) throws Exception
    { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Player actor(int classId, boolean human) throws Exception
    {
        Player p = (Player)unsafe.allocateInstance(Player.class);
        PlayerTemplate t = (PlayerTemplate)unsafe.allocateInstance(PlayerTemplate.class);
        field(PlayerTemplate.class, "_playerClass").set(t, PlayerClass.getPlayerClass(classId));
        field(Creature.class, "_template").set(p, t);
        field(Creature.class, "_skills").set(p, new ConcurrentHashMap<Integer, Skill>());
        if (human) field(Player.class, "_client").set(p, unsafe.allocateInstance(GameClient.class));
        return p;
    }
    private static BotClassPreset preset(int id)
    { return new BotClassPreset(id, id == 97 ? "Cardinal" : "Archmage", BotClassPreset.GearKind.ROBE,
        BotClassPreset.WeaponKind.MAGIC, true, false, true, new int[]{1394, 1398, 1230}, new int[]{1218}); }
    private static void names()
    {
        Map<String,Integer> owners = new HashMap<>();
        java.util.function.ToIntFunction<String> lookup = n -> owners.getOrDefault(n.toLowerCase(Locale.ROOT), -1);
        for (String base : List.of("Cardinal", "Duelist"))
        {
            owners.clear();
            check(BotSpawner.fixedClassNickname(base,26,lookup).equals(base), "legacy suffix migrates to free canonical name");
            owners.put(base.toLowerCase(Locale.ROOT),26);
            for (int i=0;i<100;i++)
                check(BotSpawner.fixedClassNickname(base,26,lookup).equals(base), "own canonical name never increments");
            String alternative=base.equals("Cardinal")?"Bishop":"Gladiator";
            for (int id : new int[]{0,27,100})
                check(BotSpawner.fixedClassNickname(base,id,lookup).equals(alternative), "foreign primary name uses semantic fallback without suffix");
            check(owners.size()==1 && owners.get(base.toLowerCase(Locale.ROOT))==26, "foreign records never mutated");
            owners.put(alternative.toLowerCase(Locale.ROOT),27);
            check(BotSpawner.fixedClassNickname(base,27,lookup).equals(alternative), "own fallback available");
            try { BotSpawner.fixedClassNickname(base,0,lookup); throw new AssertionError("all occupied must fail closed"); }
            catch (IllegalStateException expected) { checks++; }
            owners.clear();
            check(BotSpawner.fixedClassNickname(base,0,lookup).equals(base), "new bot has no numeric/alphabetic suffix");
        }
        for (BotClassPreset p : BotPresets.getPresets())
            check(BotSpawner.staticClassNickname(p)==(p.getClassId()==97||p.getClassId()==88), "only requested classes change policy");
    }
    private static void timing()
    {
        var nuke = new TacticsPolicy.Option(1230, TacticsPolicy.Kind.DAMAGE, 62);
        var drain = new TacticsPolicy.Option(1398, TacticsPolicy.Kind.DAMAGE, 90);
        var trance = new TacticsPolicy.Option(1394, TacticsPolicy.Kind.CONTROL, 54);
        var clock = new TacticsPolicy.CardinalProminence();
        var legal = List.of(drain, trance, nuke);
        check(clock.choose(97, legal, 1000)==1230, "due nuke priority, not random selection");
        check(clock.choose(94, legal, 1000)==-1, "other classes unchanged");
        check(clock.choose(97, List.of(drain,trance), 1000)==-1, "missing/illegal nuke never forced");
        // Failed useMagic does not invoke accepted: due cast remains due.
        check(clock.choose(97, legal, 1100)==1230, "rejected request does not consume 10s");
        clock.accepted(1100);
        check(!clock.ready(11099) && clock.choose(97,legal,11099)==-1, "no cast before exactly10s");
        check(clock.ready(11100) && clock.choose(97,legal,11100)==1230, "due at10s");
        var emergency = new ArrayList<>(legal);
        for (var kind : List.of(TacticsPolicy.Kind.HEAL, TacticsPolicy.Kind.DEFENSE, TacticsPolicy.Kind.CONTROL))
        {
            emergency.add(new TacticsPolicy.Option(1218, kind, 160));
            check(clock.choose(97,emergency,11100)==-1, "urgent recovery/interrupt retained");
            emergency.remove(emergency.size()-1);
        }
        clock.accepted(11100); clock.reset();
        check(clock.choose(97,legal,500)==1230, "new match resets timer");
        for (int i=0;i<100;i++)
        {
            long now=1000L+i*10000L;
            check(clock.choose(97,legal,now)==1230, "repeated10s due cycle");
            clock.accepted(now);
            check(clock.choose(97,legal,now+9999)==-1, "repeated no early cycle");
        }
        for (int cid : new int[]{97,94,115}) for (int sid : new int[]{1230,1398,1394,1399,1097})
        {
            boolean exception = TacticsPolicy.cardinalSleepException(cid,sid);
            check(exception == (cid==97&&(sid==1230||sid==1398)), "narrow sleep exception");
            boolean blocked = true && cid!=115 && !exception;
            if (cid!=115) check(TacticsPolicy.allowed(TacticsPolicy.Kind.DAMAGE,true,true,true,false,blocked,true,true)==exception, "second sleep gate agrees");
        }
        for (int mask=0;mask<16;mask++)
        {
            boolean ready=(mask&1)!=0, range=(mask&2)!=0, los=(mask&4)!=0, resources=(mask&8)!=0;
            boolean allowed = TacticsPolicy.allowed(TacticsPolicy.Kind.DAMAGE,ready,range,los,false,false,resources,true);
            check(allowed == (ready&&range&&los&&resources), "native ready/range/LOS/resources remain mandatory");
            check(clock.choose(97, allowed?List.of(nuke):List.of(), 2000000)==(allowed?1230:-1), "priority never invents an illegal option");
        }
    }
    private static void grants() throws Exception
    {
        BotClassPreset cardinal=preset(97);
        Player bot=actor(97,false);
        BotSpawner.applyBotOnlySkills(bot,cardinal);
        Skill learned=bot.getKnownSkill(1230);
        check(learned!=null && learned.getLevel()==28 && learned.isDamage(), "actual Prominence28 granted via real Player.addSkill");
        BotSpawner.applyBotOnlySkills(bot,cardinal);
        check(bot.getKnownSkill(1230)==learned && bot.getSkills().size()==1, "idempotent grant");
        // Existing learned/enchant version is never replaced with the base template.
        Skill enchanted=SkillData.getInstance().getSkill(1230,101);
        check(enchanted!=null && enchanted.getLevel()==101, "real enchant route loaded");
        bot.getSkills().put(1230,enchanted);
        BotSpawner.applyBotOnlySkills(bot,cardinal);
        check(bot.getKnownSkill(1230)==enchanted, "no downgrade");
        Player human=actor(97,true), other=actor(94,false), mismatch=actor(97,false);
        BotSpawner.applyBotOnlySkills(human,cardinal);
        BotSpawner.applyBotOnlySkills(other,cardinal);
        BotSpawner.applyBotOnlySkills(mismatch,preset(94));
        check(human.getKnownSkill(1230)==null, "client-backed Cardinal never receives bot skill");
        check(other.getKnownSkill(1230)==null && mismatch.getKnownSkill(1230)==null, "actual and preset classes must match");
        BotSpawner.applyBotOnlySkills(null,cardinal); BotSpawner.applyBotOnlySkills(bot,null);
        // A newly allocated actor models regrant after restoration without persisting a class-tree change.
        Player restored=actor(97,false); BotSpawner.applyBotOnlySkills(restored,cardinal);
        check(restored.getKnownSkill(1230)!=null, "restored/training actor receives missing skill");
    }
    public static void main(String[] args) throws Exception
    {
        unsafe=(Unsafe)field(Unsafe.class,"theUnsafe").get(null);
        names(); timing();
        ServerConfig.DATAPACK_ROOT=new File(args[0]); ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]); SkillData.getInstance();
        grants();
        System.out.println("PASS: " + checks + " Cardinal nickname, uniqueness/idempotence, real Prominence grant,10s priority and sleep/native legality checks; no live DB/client");
    }
}
