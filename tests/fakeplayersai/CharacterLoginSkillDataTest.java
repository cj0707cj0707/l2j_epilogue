import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import sun.misc.Unsafe;

/** Real core loader/skill-selection code. No DB, network, Player constructor or bot spawning. */
public final class CharacterLoginSkillDataTest
{
    private static void check(boolean ok, String label)
    {
        if (!ok) throw new AssertionError(label);
    }
    private static Field field(Class<?> owner, String name) throws Exception
    {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); return f;
    }
    public static void main(String[] args) throws Exception
    {
        // IXmlReader resolves data/ from cwd; Python launcher uses game/.
        ServerConfig.DATAPACK_ROOT = new File(args[0]);
        ThreadConfig.THREADS_FOR_LOADING = false;
        PlayerConfig.AUTO_LEARN_DIVINE_INSPIRATION = true;
        handlers.EffectMasterHandler.main(new String[0]);
        SkillData data = SkillData.getInstance();
        for (int level = 1; level <= 9; level++)
        {
            Skill s = data.getSkill(1320, level);
            check(s != null && s.getId() == 1320 && s.getLevel() == level, "1320 level " + level);
        }
        Skill craft = data.getSkill(1322, 1);
        check(craft != null && craft.getId() == 1322, "1322 level 1");
        check(craft.getEffects(EffectScope.GENERAL).stream().anyMatch(e -> e.getName().equals("OpenCommonRecipeBook")), "1322 actual recipe-book handler");
        // Assert every base level in the recovered range was really parsed, not just well-formed XML.
        var dom = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new File(args[0], "data/stats/skills/01300-01399.xml"));
        var definitions = dom.getDocumentElement().getElementsByTagName("skill");
        int loaded = 0;
        for (int i = 0; i < definitions.getLength(); i++)
        {
            var node = (org.w3c.dom.Element)definitions.item(i);
            int id = Integer.parseInt(node.getAttribute("id"));
            for (int level = 1; level <= Integer.parseInt(node.getAttribute("levels")); level++)
            {
                Skill s = data.getSkill(id, level);
                check(s != null && s.getLevel() == level, "base skill " + id + "/" + level);
                loaded++;
            }
        }
        // Inert Player: real getLevel/getSkills, but constructors never touch DB/world/tasks.
        Unsafe unsafe = (Unsafe)field(Unsafe.class, "theUnsafe").get(null);
        Player player = (Player)unsafe.allocateInstance(Player.class);
        PlayerStat stat = new PlayerStat(player);
        field(Creature.class, "_stat").set(player, stat);
        field(Creature.class, "_skills").set(player, new HashMap<Integer, Skill>());
        SkillTreeData trees = SkillTreeData.getInstance();
        int selections = 0;
        PlayerClass reproducer = null;
        for (PlayerClass pc : PlayerClass.values())
        {
            Map<Integer, SkillLearn> tree = trees.getCompleteClassSkillTree(pc);
            if (tree.isEmpty()) continue;
            for (int level : new int[]{1, 5, 70, 85})
            {
                field(CreatureStat.class, "_level").setByte(stat, (byte)level);
                Collection<Skill> selected = trees.getAllAvailableSkills(player, pc, false, true, false);
                check(selected.stream().noneMatch(s -> s == null), "non-null selection " + pc + "/" + level);
                if (tree.values().stream().anyMatch(s -> s.getSkillId() == 1322))
                {
                    check(selected.stream().anyMatch(s -> s.getId() == 1322 && s.getLevel() == 1), "Common Craft " + pc);
                    reproducer = pc;
                }
                if (level >= 70 && tree.values().stream().anyMatch(s -> s.getSkillId() == 1320 && s.getSkillLevel() == 9))
                    check(selected.stream().anyMatch(s -> s.getId() == 1320 && s.getLevel() == 9), "Create Common Item9 " + pc);
                selections++;
            }
        }
        check(reproducer != null && selections > 100, "actual class-tree coverage");
        // Negative control: the exact missing template in the supplied CharacterSelect stack.
        @SuppressWarnings("unchecked")
        Map<Integer, Skill> templates = (Map<Integer, Skill>)field(SkillData.class, "_skillsByHash").get(data);
        int key = SkillData.getSkillHashCode(1322, 1);
        Skill removed = templates.remove(key);
        boolean reproduced = false;
        try { trees.getAllAvailableSkills(player, reproducer, false, true, false); }
        catch (NullPointerException expected)
        {
            reproduced = java.util.Arrays.stream(expected.getStackTrace()).anyMatch(e ->
                e.getClassName().equals(SkillTreeData.class.getName()) && e.getMethodName().equals("getAllAvailableSkills"));
        }
        finally { templates.put(key, removed); }
        check(reproduced, "missing1322 reproduces actual SkillTreeData NPE");
        check(trees.getAllAvailableSkills(player, reproducer, false, true, false).stream().anyMatch(s -> s.getId() == 1322), "restoration repairs same selection");
        System.out.println("PASS: " + definitions.getLength() + " restored definitions, " + loaded
            + " actual base templates, " + selections + " real-core class/level selections; missing1322 reproduces NPE and restoration fixes it");
    }
}
