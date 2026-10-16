/* Scoped membership adapter: no XML, geometry, PvP or raw zone-flag changes. */
package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Install once, before admitting any training sessions. Only explicitly registered
 * positive instance IDs omit giran_port3/4 membership. The original PeaceZone owns
 * all membership, enter/exit counters, listeners, settings and ordinary behavior.
 * No other PeaceZone and no coordinate query is excluded.
 *
 * registerInstance must follow successful private Instance creation, before entry.
 * unregisterInstance must follow participant evacuation; if used in place, force
 * ordinary zone revalidation afterwards. It never edits a character's flags.
 * Admission AND every active-session pulse must check isInstalled/isExcluded.
 * A zone reload/reference replacement permanently revokes this installation and
 * all authorizations; do not automatically reinstall while sessions are running.
 * There is no core reload lock: these checks detect replacement, not an atomic
 * exclusion of concurrent admin reload. The caller must stop/evacuate on failure.
 */
public final class TrainingPeaceZones
{
    private static final Logger LOG = Logger.getLogger(TrainingPeaceZones.class.getName());
    private static final Set<String> NAMES = Set.of("giran_port3", "giran_port4");
    // All authority and publication are protected by this class's monitor.
    private static final Set<Integer> INSTANCES = new HashSet<>();
    private static Installation installation;
    private TrainingPeaceZones() { }

    public static synchronized boolean install()
    {
        if (installation != null) return healthy();
        try { return install(ZoneManager.getInstance()); }
        catch (RuntimeException | LinkageError failure)
        {
            LOG.log(Level.SEVERE, "Training peace-zone manager unavailable; admission must stay closed", failure);
            return false;
        }
    }

    // Separate acquisition from installation; permits isolated actual-JAR fixtures
    // without initializing ZoneManager's server-loading singleton holder.
    private static synchronized boolean install(ZoneManager manager)
    {
        if (installation != null) return healthy();
        final List<Slot> changed = new ArrayList<>();
        try
        {
            checkDelegationSurface();
            Field classes = field("_classZones", Map.class);
            Field regions = field("_zoneRegions", ZoneRegion[][].class);
            Object rawClasses = classes.get(manager);
            Object rawRegions = regions.get(manager);
            if (!(rawClasses instanceof ConcurrentMap<?, ?>) || !(rawRegions instanceof ZoneRegion[][]))
                throw new IllegalStateException("ZoneManager maps/regions are incompatible");
            Map<?, ?> classMaps = (Map<?, ?>)rawClasses;
            Object rawPeace = classMaps.get(PeaceZone.class);
            if (!(rawPeace instanceof ConcurrentMap<?, ?>))
                throw new IllegalStateException("PeaceZone class map is not concurrent");
            @SuppressWarnings("unchecked")
            ConcurrentMap<Integer, ZoneType> peace = (ConcurrentMap<Integer, ZoneType>)rawPeace;
            List<Adapter> adapters = new ArrayList<>();
            Set<String> found = new HashSet<>();
            for (Map.Entry<Integer, ZoneType> entry : peace.entrySet())
            {
                ZoneType zone = entry.getValue();
                if (!NAMES.contains(zone.getName())) continue;
                if (zone.getClass() != PeaceZone.class || !entry.getKey().equals(zone.getId()) ||
                    zone.getZone() == null || zone.getInstanceId() != -1 || zone.getInstanceTemplateId() != -1 ||
                    !found.add(zone.getName()))
                    throw new IllegalStateException("Unexpected/duplicate targeted PeaceZone: " + zone);
                adapters.add(new Adapter((PeaceZone)zone));
            }
            if (!found.equals(NAMES)) throw new IllegalStateException("Both named harbor peace zones are required");
            ZoneRegion[][] grid = (ZoneRegion[][])rawRegions;
            List<Slot> slots = new ArrayList<>();
            for (Adapter adapter : adapters) slots.add(new Slot(peace, adapter, null, -1, -1));
            int[] occurrences = new int[adapters.size()];
            // Inspect every region, not merely the two spawn-point regions.
            for (int x = 0; x < grid.length; x++)
            {
                if (grid[x] == null) throw new IllegalStateException("Null region row");
                for (int y = 0; y < grid[x].length; y++)
                {
                    ZoneRegion region = grid[x][y];
                    if (region == null || !(region.getZones() instanceof ConcurrentMap<?, ?>))
                        throw new IllegalStateException("Nonconcurrent/null zone region");
                    @SuppressWarnings("unchecked")
                    ConcurrentMap<Integer, ZoneType> map = (ConcurrentMap<Integer, ZoneType>)region.getZones();
                    for (Map.Entry<Integer, ZoneType> entry : map.entrySet())
                        for (int i = 0; i < adapters.size(); i++)
                        {
                            Adapter adapter = adapters.get(i);
                            if (entry.getValue() == adapter.original || entry.getKey().equals(adapter.getId()))
                            {
                                if (entry.getValue() != adapter.original || !entry.getKey().equals(adapter.getId()))
                                    throw new IllegalStateException("Inconsistent targeted region reference");
                                slots.add(new Slot(map, adapter, region, x, y));
                                occurrences[i]++;
                            }
                        }
                }
            }
            for (int n : occurrences) if (n == 0) throw new IllegalStateException("Target not indexed in regions");
            // No extra class-map aliases may retain an unadapted targeted zone.
            for (Map.Entry<?, ?> entry : classMaps.entrySet())
            {
                if (!(entry.getValue() instanceof ConcurrentMap<?, ?>))
                    throw new IllegalStateException("Nonconcurrent zone class map");
                for (Map.Entry<?, ?> z : ((Map<?, ?>)entry.getValue()).entrySet())
                    for (Adapter a : adapters)
                        if (z.getValue() == a.original &&
                            (entry.getKey() != PeaceZone.class || !Integer.valueOf(a.getId()).equals(z.getKey())))
                            throw new IllegalStateException("Unexpected class-map alias");
            }
            Installation next = new Installation(manager, classes, regions, classMaps, peace, grid, adapters, slots);
            for (Slot slot : slots)
            {
                if (!slot.map.replace(slot.adapter.getId(), slot.adapter.original, slot.adapter))
                    throw new IllegalStateException("Zone references changed during installation");
                changed.add(slot);
            }
            if (!next.valid()) throw new IllegalStateException("Zone references changed after installation");
            installation = next; // publish only after ALL compare-and-replaces succeed
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure)
        {
            INSTANCES.clear();
            for (int i = changed.size() - 1; i >= 0; i--)
            {
                Slot slot = changed.get(i);
                slot.map.replace(slot.adapter.getId(), slot.adapter, slot.adapter.original);
            }
            LOG.log(Level.SEVERE, "Training peace-zone exclusion unavailable; admission must stay closed", failure);
            return false;
        }
    }

    public static synchronized boolean isInstalled() { return healthy(); }

    public static synchronized boolean registerInstance(int instanceId)
    {
        if (instanceId <= 0 || !healthy()) return false;
        INSTANCES.add(instanceId);
        return healthy(); // do not retain authority if a reload raced publication
    }

    public static synchronized void unregisterInstance(int instanceId) { INSTANCES.remove(instanceId); }

    /** Identity check, NOT a name/type-only bypass. Also accepts the retained original. */
    public static synchronized boolean isExcluded(ZoneType zone, int instanceId)
    {
        if (instanceId <= 0 || !INSTANCES.contains(instanceId) || !healthy()) return false;
        for (Adapter a : installation.adapters) if (zone == a || zone == a.original) return true;
        return false;
    }

    private static boolean healthy()
    {
        if (installation == null || installation.broken) return false;
        try
        {
            if (installation.valid()) return true;
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure)
        {
            LOG.log(Level.WARNING, "Training peace-zone reference check failed", failure);
        }
        installation.broken = true;
        INSTANCES.clear();
        LOG.severe("Training peace-zone references lost (reload?): all instance exclusions revoked; evacuate sessions");
        return false;
    }

    private static Field field(String name, Class<?> type) throws ReflectiveOperationException
    {
        Field f = ZoneManager.class.getDeclaredField(name);
        if (f.getType() != type || Modifier.isStatic(f.getModifiers()))
            throw new IllegalStateException("Incompatible ZoneManager field: " + name);
        f.setAccessible(true);
        return f;
    }

    /** Fail closed if an updated core adds an undelegated public operation. */
    private static void checkDelegationSurface() throws ReflectiveOperationException
    {
        for (Class<?> type : new Class<?>[]{ZoneType.class, PeaceZone.class, ListenersContainer.class})
            for (Method method : type.getDeclaredMethods())
                if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()))
                {
                    Method delegated = Adapter.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    if (!Modifier.isPublic(delegated.getModifiers()) || delegated.getReturnType() != method.getReturnType())
                        throw new IllegalStateException("Incompatible zone API: " + method);
                }
    }

    private static final class Slot
    {
        final ConcurrentMap<Integer, ZoneType> map;
        final Adapter adapter;
        final ZoneRegion region;
        final int x, y;
        Slot(ConcurrentMap<Integer, ZoneType> map, Adapter adapter, ZoneRegion region, int x, int y)
        { this.map = map; this.adapter = adapter; this.region = region; this.x = x; this.y = y; }
    }

    private static final class Installation
    {
        final ZoneManager manager;
        final Field classesField, regionsField;
        final Map<?, ?> classes;
        final ConcurrentMap<Integer, ZoneType> peace;
        final ZoneRegion[][] grid;
        final List<Adapter> adapters;
        final List<Slot> slots;
        boolean broken;
        Installation(ZoneManager manager, Field classesField, Field regionsField, Map<?, ?> classes,
            ConcurrentMap<Integer, ZoneType> peace, ZoneRegion[][] grid, List<Adapter> adapters, List<Slot> slots)
        {
            this.manager = manager; this.classesField = classesField; this.regionsField = regionsField;
            this.classes = classes; this.peace = peace; this.grid = grid; this.adapters = adapters; this.slots = slots;
        }
        boolean valid() throws IllegalAccessException
        {
            if (classesField.get(manager) != classes || regionsField.get(manager) != grid || classes.get(PeaceZone.class) != peace)
                return false;
            for (Adapter a : adapters)
                if (!a.name.equals(a.getName()) || a.original.getInstanceId() != -1 || a.original.getInstanceTemplateId() != -1)
                    return false;
            // O(number of actual target references), not a whole-world scan per actor.
            for (Slot s : slots)
                if (s.map.get(s.adapter.getId()) != s.adapter ||
                    (s.region != null && (grid[s.x][s.y] != s.region || s.region.getZones() != s.map))) return false;
            return true;
        }
    }

    private static final class Adapter extends PeaceZone
    {
        final PeaceZone original;
        final String name;
        Adapter(PeaceZone original) { super(original.getId()); this.original = original; this.name = original.getName(); }

        @Override public void revalidateInZone(Creature actor)
        {
            // Core removeCharacter uses containsKey then remove/onExit. Serialize our
            // operations on the retained original so repeated/concurrent removal does
            // not fire a second onExit and decrement another overlapping zone's flag.
            synchronized (original)
            {
                if (isExcluded(this, actor.getInstanceId())) original.removeCharacter(actor);
                else original.revalidateInZone(actor);
            }
        }
        @Override public void removeCharacter(Creature actor) { synchronized (original) { original.removeCharacter(actor); } }
        @Override public boolean isCharacterInZone(Creature actor) { return original.isCharacterInZone(actor); }
        @Override public Collection<Creature> getCharactersInside() { return original.getCharactersInside(); }
        @Override public List<Player> getPlayersInside() { return original.getPlayersInside(); }
        @Override public void broadcastPacket(ServerPacket packet) { original.broadcastPacket(packet); }
        @Override public void oustAllPlayers() { original.oustAllPlayers(); }
        @Override public int getId() { return original.getId(); }
        @Override public void setParameter(String name, String value) { original.setParameter(name, value); }
        @Override public void setZone(ZoneForm zone) { original.setZone(zone); }
        @Override public ZoneForm getZone() { return original.getZone(); }
        @Override public void setName(String name) { original.setName(name); }
        @Override public String getName() { return original.getName(); }
        @Override public void setInstanceId(int id) { original.setInstanceId(id); }
        @Override public int getInstanceId() { return original.getInstanceId(); }
        @Override public void setInstanceTemplateId(int id) { original.setInstanceTemplateId(id); }
        @Override public int getInstanceTemplateId() { return original.getInstanceTemplateId(); }
        @Override public boolean isInsideZone(int x, int y) { return original.isInsideZone(x, y); }
        @Override public boolean isInsideZone(int x, int y, int z) { return original.isInsideZone(x, y, z); }
        @Override public boolean isInsideZone(ILocational point) { return original.isInsideZone(point); }
        @Override public double getDistanceToZone(int x, int y) { return original.getDistanceToZone(x, y); }
        @Override public double getDistanceToZone(WorldObject point) { return original.getDistanceToZone(point); }
        @Override public AbstractZoneSettings getSettings() { return original.getSettings(); }
        @Override public void setSettings(AbstractZoneSettings settings) { original.setSettings(settings); }
        @Override public InstanceType getTargetType() { return original.getTargetType(); }
        @Override public void setTargetType(InstanceType type) { original.setTargetType(type); }
        @Override public boolean getAllowStore() { return original.getAllowStore(); }
        @Override public void setEnabled(boolean value) { original.setEnabled(value); }
        @Override public boolean isEnabled() { return original.isEnabled(); }
        @Override public String toString() { return original.toString(); }
        @Override public void visualizeZone(int z) { original.visualizeZone(z); }
        @Override public void onDieInside(Creature actor) { original.onDieInside(actor); }
        @Override public void onReviveInside(Creature actor) { original.onReviveInside(actor); }
        @Override public void onPlayerLoginInside(Player player) { original.onPlayerLoginInside(player); }
        @Override public void onPlayerLogoutInside(Player player) { original.onPlayerLogoutInside(player); }
        @Override public AbstractEventListener addListener(AbstractEventListener listener) { return original.addListener(listener); }
        @Override public AbstractEventListener removeListener(AbstractEventListener listener) { return original.removeListener(listener); }
        @Override public void removeListenerIf(EventType type, Predicate<? super AbstractEventListener> predicate) { original.removeListenerIf(type, predicate); }
        @Override public void removeListenerIf(Predicate<? super AbstractEventListener> predicate) { original.removeListenerIf(predicate); }
        @Override public boolean hasListener(EventType type) { return original.hasListener(type); }
        @Override public Collection<AbstractEventListener> getListeners(EventType type) { return original.getListeners(type); }
    }
}
