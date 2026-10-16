package custom.fakeplayersai;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import sun.misc.Unsafe;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.*;
import org.l2jmobius.gameserver.entity.zone.form.*;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.*;
import org.l2jmobius.gameserver.mechanics.events.listeners.*;

/**
 * REAL server classes, not API doubles. Unsafe bypasses Player/ZoneManager
 * constructors only in this isolated fixture: no DB, singleton load or server.
 * Actual ZoneType/PeaceZone membership/events/counters and ZoneRegion are used.
 */
public final class TrainingPeaceZonesActualJarTest
{
    private static int checks;
    private static Unsafe unsafe;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    private static void set(Object object, Class<?> owner, String name, Object value) throws Exception
    { Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(object, value); }
    private static void reset() throws Exception
    {
        Field f = TrainingPeaceZones.class.getDeclaredField("installation"); f.setAccessible(true); f.set(null, null);
        f = TrainingPeaceZones.class.getDeclaredField("INSTANCES"); f.setAccessible(true); ((Set<?>)f.get(null)).clear();
    }
    private static boolean install(ZoneManager manager) throws Exception
    {
        Method m = TrainingPeaceZones.class.getDeclaredMethod("install", ZoneManager.class); m.setAccessible(true);
        return (boolean)m.invoke(null, manager);
    }
    private static Player actor(int id, int instance, int x, int y) throws Exception
    {
        Player p = (Player)unsafe.allocateInstance(Player.class);
        set(p, WorldObject.class, "_objectId", id);
        set(p, WorldObject.class, "_location", new Location(x, y, -3513, 0, instance));
        set(p, Creature.class, "_zones", new byte[ZoneId.values().length]);
        return p;
    }
    private static int counter(Player p, ZoneId id) throws Exception
    {
        Field f = Creature.class.getDeclaredField("_zones"); f.setAccessible(true);
        return ((byte[])f.get(p))[id.ordinal()];
    }
    private static PeaceZone xmlZone(String name, int id) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(Path.of("game/data/zones/peace.xml").toFile());
        NodeList zones = doc.getElementsByTagName("zone");
        for (int i = 0; i < zones.getLength(); i++)
        {
            Element zone = (Element)zones.item(i);
            if (!name.equals(zone.getAttribute("name"))) continue;
            NodeList nodes = zone.getElementsByTagName("node");
            int[] x = new int[nodes.getLength()], y = new int[nodes.getLength()];
            for (int j = 0; j < x.length; j++)
            {
                x[j] = Integer.parseInt(((Element)nodes.item(j)).getAttribute("X"));
                y[j] = Integer.parseInt(((Element)nodes.item(j)).getAttribute("Y"));
            }
            PeaceZone result = new PeaceZone(id); result.setName(name);
            result.setZone(new ZoneNPoly(x, y, Integer.parseInt(zone.getAttribute("minZ")), Integer.parseInt(zone.getAttribute("maxZ"))));
            return result;
        }
        throw new AssertionError("Missing XML zone " + name);
    }
    private static final class Fixture
    {
        final ZoneManager manager;
        final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Integer, ZoneType>> classes = new ConcurrentHashMap<>();
        final ConcurrentHashMap<Integer, ZoneType> peace = new ConcurrentHashMap<>();
        final ZoneRegion[][] grid = {{new ZoneRegion(0,0), new ZoneRegion(0,1)}, {new ZoneRegion(1,0), new ZoneRegion(1,1)}};
        final PeaceZone port3, port4, other;
        Fixture() throws Exception
        {
            reset(); GeneralConfig.PEACE_ZONE_MODE = 0;
            manager = (ZoneManager)unsafe.allocateInstance(ZoneManager.class);
            port3 = xmlZone("giran_port3", 91001); port4 = xmlZone("giran_port4", 91002);
            // Store prohibition is a real second zone counter; exercise its balanced removal too.
            set(port3, ZoneType.class, "_allowStore", false); set(port4, ZoneType.class, "_allowStore", false);
            other = new PeaceZone(91003); other.setName("unrelated_peace");
            other.setZone(new ZoneCuboid(41000, 47000, 186000, 193000, -4000, -3000));
            peace.put(port3.getId(), port3); peace.put(port4.getId(), port4); peace.put(other.getId(), other);
            classes.put(PeaceZone.class, peace);
            set(manager, ZoneManager.class, "_classZones", classes);
            set(manager, ZoneManager.class, "_zoneRegions", grid);
            for (ZoneRegion[] row : grid) for (ZoneRegion region : row)
            { region.getZones().put(port3.getId(), port3); region.getZones().put(port4.getId(), port4); }
        }
        ZoneType a() { return peace.get(port3.getId()); }
        ZoneType b() { return peace.get(port4.getId()); }
        void revalidate(Player p) { grid[0][0].revalidateZones(p); }
    }
    private static void descriptors() throws Exception
    {
        Field f = ZoneManager.class.getDeclaredField("_classZones"); f.setAccessible(true);
        check(f.getType() == Map.class && Modifier.isPrivate(f.getModifiers()), "actual manager private map");
        f = ZoneManager.class.getDeclaredField("_zoneRegions"); f.setAccessible(true);
        check(f.getType() == ZoneRegion[][].class, "actual region array");
        check(new ZoneRegion(0,0).getZones() instanceof ConcurrentMap<?,?>, "actual concurrent region map");
        Class<?> adapter = Class.forName("custom.fakeplayersai.TrainingPeaceZones$Adapter");
        check(adapter.getSuperclass() == PeaceZone.class, "preserves PeaceZone instanceof dispatch");
        int delegated = 0;
        for (Class<?> type : new Class<?>[]{ZoneType.class, PeaceZone.class, ListenersContainer.class})
            for (Method m : type.getDeclaredMethods())
                if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                {
                    Method d = adapter.getDeclaredMethod(m.getName(), m.getParameterTypes());
                    check(d.getReturnType() == m.getReturnType() && Modifier.isPublic(d.getModifiers()), "delegates " + m);
                    delegated++;
                }
        check(delegated >= 40, "whole public surface checked, not selected methods");
    }
    private static void memberships() throws Exception
    {
        Fixture f = new Fixture();
        Player world = actor(1, 0, 44112, 188116), unrelated = actor(2, 77, 44112, 188116);
        Player bot = actor(3, 123, 44112, 188116), human = actor(4, 123, 42705, 188869);
        // Seed originals before installation to verify that maps/flags are not copied/reset.
        f.revalidate(world); f.revalidate(unrelated); f.revalidate(bot); f.revalidate(human);
        check(counter(world, ZoneId.PEACE) == 2, "actual bot point overlaps both peace polygons");
        check(counter(human, ZoneId.PEACE) >= 1, "actual human point has harbor peace");
        Collection<Creature> retainedMembers = f.port3.getCharactersInside();
        check(install(f.manager), "installation");
        check(TrainingPeaceZones.install(), "public idempotent install avoids singleton reload");
        check(TrainingPeaceZones.isInstalled(), "reference validation");
        check(f.a() != f.port3 && f.a() instanceof PeaceZone, "class index now adapter");
        check(f.a().getCharactersInside() == retainedMembers, "original membership live view retained");
        for (ZoneRegion[] row : f.grid) for (ZoneRegion region : row)
        {
            check(region.getZones().get(f.port3.getId()) == f.a(), "all region references replaced for port3");
            check(region.getZones().get(f.port4.getId()) == f.b(), "all region references replaced for port4");
        }
        check(f.peace.get(f.other.getId()) == f.other, "unrelated zone identity unchanged");
        check(!TrainingPeaceZones.registerInstance(0) && !TrainingPeaceZones.registerInstance(-1), "world/invalid never authorized");
        check(!TrainingPeaceZones.isExcluded(f.a(),123), "unregistered training-like instance not excluded");
        check(TrainingPeaceZones.registerInstance(123) && TrainingPeaceZones.registerInstance(123), "positive registration idempotent");
        check(TrainingPeaceZones.isExcluded(f.a(),123) && TrainingPeaceZones.isExcluded(f.port3,123), "adapter/original identity");
        PeaceZone spoof = new PeaceZone(f.port3.getId()); spoof.setName("giran_port3");
        check(!TrainingPeaceZones.isExcluded(spoof,123), "name/id spoof cannot bypass");
        check(!TrainingPeaceZones.isExcluded(f.other,123) && !TrainingPeaceZones.isExcluded(null,123), "other zone/null denied");
        for (int i=0; i<30; i++) { f.revalidate(world); f.revalidate(unrelated); f.revalidate(bot); f.revalidate(human); }
        check(counter(world,ZoneId.PEACE)==2 && counter(unrelated,ZoneId.PEACE)==2, "world and unrelated instance unchanged");
        check(counter(bot,ZoneId.PEACE)==0 && counter(human,ZoneId.PEACE)==0, "both training participants lose only real harbor membership");
        check(counter(bot,ZoneId.NO_STORE)==0 && counter(human,ZoneId.NO_STORE)==0, "no-store balanced removal");
        check(!f.a().isCharacterInZone(bot) && !f.b().isCharacterInZone(bot), "retained originals no longer contain excluded actor");
        check(f.a().getPlayersInside().contains(world) && f.a().getPlayersInside().contains(unrelated), "delegated player listing");
        // A third independent PeaceZone MUST remain active in a registered instance.
        f.grid[0][0].getZones().put(f.other.getId(),f.other); f.revalidate(bot);
        check(counter(bot,ZoneId.PEACE)==1, "unrelated overlapping zone preserved even in training instance");
        for (int i=0; i<50; i++) { f.a().removeCharacter(bot); f.b().removeCharacter(bot); f.revalidate(bot); }
        check(counter(bot,ZoneId.PEACE)==1, "repeated removal cannot consume third-zone counter");
        // Parallel calls: core containsKey/remove is not atomic, adapter must serialize it.
        bot.getLocation().setInstanceId(0); f.revalidate(bot);
        check(counter(bot,ZoneId.PEACE)==3, "return to world restores all real memberships");
        bot.getLocation().setInstanceId(123);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> tasks = new ArrayList<>();
        for (int t=0;t<8;t++) tasks.add(executor.submit(() -> { for(int i=0;i<200;i++) { f.a().revalidateInZone(bot); f.b().revalidateInZone(bot); f.a().removeCharacter(bot); } }));
        for (Future<?> task : tasks) task.get(10,TimeUnit.SECONDS);
        executor.shutdownNow();
        check(counter(bot,ZoneId.PEACE)==1, "concurrent exclusion never decrements unrelated peace counter");
        check(counter(bot,ZoneId.NO_STORE)==0, "concurrent no-store counter not negative");
        TrainingPeaceZones.unregisterInstance(123); TrainingPeaceZones.unregisterInstance(123);
        check(!TrainingPeaceZones.isExcluded(f.a(),123), "unregister revokes immediately");
        f.revalidate(bot); check(counter(bot,ZoneId.PEACE)==3 && counter(bot,ZoneId.NO_STORE)==2, "normal revalidation restores after unregister");
        f.grid[0][0].removeFromZones(bot); f.grid[0][0].removeFromZones(bot);
        check(counter(bot,ZoneId.PEACE)==0 && counter(bot,ZoneId.NO_STORE)==0, "balanced normal exit, no underflow");
    }
    private static final class Settings extends AbstractZoneSettings
    { int clears; @Override public void clear() { clears++; } }
    private static void delegation() throws Exception
    {
        Fixture f = new Fixture();
        Settings original = new Settings(); f.port3.setSettings(original);
        AtomicInteger callbacks = new AtomicInteger();
        ConsumerEventListener pre = new ConsumerEventListener(f.port3, EventType.ON_CREATURE_ZONE_ENTER, e -> callbacks.incrementAndGet(), f);
        f.port3.addListener(pre);
        check(install(f.manager), "install with existing settings/listener");
        ZoneType a = f.a();
        check(a.getSettings()==original, "preexisting settings identity");
        check(a.hasListener(EventType.ON_CREATURE_ZONE_ENTER) && a.getListeners(EventType.ON_CREATURE_ZONE_ENTER).contains(pre), "preexisting listener retained");
        a.removeListener(pre); check(!f.port3.hasListener(EventType.ON_CREATURE_ZONE_ENTER), "listener removal delegates");
        ConsumerEventListener added = new ConsumerEventListener(a, EventType.ON_CREATURE_ZONE_ENTER, e -> callbacks.incrementAndGet(), f);
        check(a.addListener(added)==added && f.port3.hasListener(EventType.ON_CREATURE_ZONE_ENTER), "new listener stored only on original");
        a.removeListenerIf(EventType.ON_CREATURE_ZONE_ENTER, l -> l==added);
        check(!f.port3.hasListener(EventType.ON_CREATURE_ZONE_ENTER), "typed listener removal delegates");
        a.addListener(added); a.removeListenerIf(l -> true);
        check(!f.port3.hasListener(EventType.ON_CREATURE_ZONE_ENTER), "all listener removal delegates");
        Settings replacement = new Settings(); a.setSettings(replacement);
        check(original.clears==1 && f.port3.getSettings()==replacement, "settings clear/set semantics retained");
        a.setEnabled(false); check(!a.isEnabled() && !f.port3.isEnabled(), "enabled setter/getter delegates");
        a.setEnabled(true); check(a.isEnabled(), "reenable delegates");
        check(!a.getAllowStore(), "original allow-store setting"); a.setParameter("allowStore","true");
        check(a.getAllowStore() && f.port3.getAllowStore(), "parameter setter delegates");
        check(a.getZone()==f.port3.getZone() && a.getName().equals(f.port3.getName()), "geometry/name retained by identity");
        check(a.getDistanceToZone(44112,188116)==f.port3.getDistanceToZone(44112,188116), "distance geometry unchanged");
        check(a.toString().equals(f.port3.toString()), "description delegates");
        for (int x=41500;x<=46500;x+=173) for (int y=186500;y<=193000;y+=227)
            for (int z : new int[]{-4000,-3513,-3000})
                check(a.isInsideZone(x,y,z)==f.port3.isInsideZone(x,y,z), "world geometry invariant");
        check(TrainingPeaceZones.registerInstance(456), "register for geometric query invariant");
        Location point = new Location(44112,188116,-3513,0,456);
        check(a.isInsideZone(point) && a.isInsideZone(44112,188116), "queries retain geometry even for registered instance");
        // Never invoke event-producing lifecycle operations on wrapper-owned state.
        a.onDieInside(null); a.onReviveInside(null); a.onPlayerLoginInside(null); a.onPlayerLogoutInside(null);
        a.broadcastPacket(null); a.oustAllPlayers();
    }
    private static final class FailReplaceMap extends ConcurrentHashMap<Integer,ZoneType>
    {
        final int blocked; FailReplaceMap(int blocked) { this.blocked=blocked; }
        @Override public boolean replace(Integer id, ZoneType old, ZoneType value)
        { return id==blocked ? false : super.replace(id,old,value); }
    }
    private static void failures() throws Exception
    {
        Fixture f = new Fixture(); f.peace.remove(f.port4.getId());
        check(!install(f.manager) && !TrainingPeaceZones.registerInstance(9), "missing named zone closes admission");
        check(f.peace.get(f.port3.getId())==f.port3, "failed preflight no mutation");
        f = new Fixture();
        set(f.grid[0][0],ZoneRegion.class,"_zones",new HashMap<>(f.grid[0][0].getZones()));
        check(!install(f.manager), "nonconcurrent region rejected");
        f = new Fixture();
        FailReplaceMap failing = new FailReplaceMap(f.port4.getId()); failing.putAll(f.grid[1][1].getZones());
        set(f.grid[1][1],ZoneRegion.class,"_zones",failing);
        check(!install(f.manager), "compare-and-replace failure closes installation");
        check(f.peace.get(f.port3.getId())==f.port3 && f.peace.get(f.port4.getId())==f.port4, "class references rolled back");
        for(ZoneRegion[] row:f.grid) for(ZoneRegion region:row)
            check(region.getZones().get(f.port3.getId())==f.port3 && region.getZones().get(f.port4.getId())==f.port4,"all region references rolled back");
        f = new Fixture(); f.grid[0][0].getZones().put(f.port3.getId(),f.other);
        check(!install(f.manager), "inconsistent region target rejected");
        for (int kind=0;kind<7;kind++)
        {
            f = new Fixture(); check(install(f.manager) && TrainingPeaceZones.registerInstance(42), "reload fixture admission " + kind);
            ZoneType adapter = f.a();
            switch(kind)
            {
                case 0: f.grid[0][0].getZones().clear(); break;
                case 1: f.peace.put(f.port3.getId(),f.port3); break;
                case 2: f.grid[0][0]=new ZoneRegion(0,0); break;
                case 3: f.classes.put(PeaceZone.class,new ConcurrentHashMap<>(f.peace)); break;
                case 4: set(f.manager,ZoneManager.class,"_classZones",new ConcurrentHashMap<>(f.classes)); break;
                case 5: f.port3.setName("giran_port4"); break;
                case 6: f.port3.setInstanceId(123); break;
            }
            check(!TrainingPeaceZones.isInstalled(), "reload/lost reference detected " + kind);
            check(!TrainingPeaceZones.isExcluded(adapter,42) && !TrainingPeaceZones.registerInstance(43), "all authority revoked " + kind);
            check(!install(f.manager), "no silent reinstall over running sessions " + kind);
        }
        f = new Fixture(); check(install(f.manager) && TrainingPeaceZones.registerInstance(8), "unregister fixture");
        TrainingPeaceZones.unregisterInstance(8);
        check(TrainingPeaceZones.isInstalled() && !TrainingPeaceZones.isExcluded(f.a(),8), "unregister doesn't disable installation");
    }
    public static void main(String[] args) throws Exception
    {
        Field u = Unsafe.class.getDeclaredField("theUnsafe"); u.setAccessible(true); unsafe=(Unsafe)u.get(null);
        descriptors(); memberships(); delegation(); failures();
        System.out.println("PASS: " + checks + " actual-JAR adapter checks; no live server/DB initialized");
    }
}
