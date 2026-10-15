#!/usr/bin/env python3
"""All local bot-combat checks. No database or GameServer is started."""
from pathlib import Path
import subprocess
import tempfile
import sys
sys.dont_write_bytecode = True
import run_olympiad_chase_tests as chase


def main():
    root = chase.ROOT
    # Guard the exact visual regression: instant effects must not forge cast packets.
    spawner = (root / "game/data/scripts/custom/fakeplayersai/BotSpawner.java").read_text()
    assert "new MagicSkillUse(" not in spawner, "Instant BotSpawner effects announce a fake cast again"
    assert "skill.applyEffects(player, target);" in spawner
    assert "skill.applyEffects(player, player);" in spawner
    manager = (root / "game/data/scripts/custom/fakeplayersai/BotManager.java").read_text()
    assert "return bot.useMagic(skill, true, true);" in manager, "Driver must not create walk-into-range casts"
    assert "synchronized (state)" in manager, "Concurrent driver passes need phase serialization"
    assert "OlympiadChase.startAutoPlayUnlessPursuing(player);" in spawner
    assert spawner.count("if (OlympiadChase.isPursuing(player.getObjectId()) || BotTactics.isOwned(player.getObjectId()))") == 2, "Both self-buff sync paths must respect pursuit"
    assert "OLYMPIAD_ARENA_MAX_DISTANCE, !BERSERKER_STATES.containsKey(bot.getObjectId())" in manager
    assert "if ((bot.getAI().getIntention() == Intention.CAST) && (dist > CHASE_RANGE))" in manager
    driver = manager.split("private static boolean driveBerserkerStep(", 1)[1].split("private static Location berserkerRetreatPoint(", 1)[0]
    assert "stopBerserkerEngines(bot, state);\n\t\t\tmoveForOlympiadChase(bot, CHASE_RANGE, true);\n\t\t\treturn true;" in driver.rsplit("AutoUseTaskManager.getInstance().startAutoUseTask(bot);", 1)[0]
    print("PASS: 10 source regression guards", flush=True)
    chase.main()
    with tempfile.TemporaryDirectory(prefix="berserker-cycle-") as tmp:
        tmp = Path(tmp)
        launcher = tmp / "Compile.java"
        launcher.write_text("""import javax.tools.ToolProvider;
public class Compile { public static void main(String[] a) {
System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }""")
        subprocess.run(["java", str(launcher), "-d", str(tmp / "classes"),
                        str(root / "game/data/scripts/custom/fakeplayersai/BerserkerCycle.java"),
                        str(root / "tests/fakeplayersai/BerserkerCycleTest.java")], check=True)
        subprocess.run(["java", "-cp", str(tmp / "classes"), "custom.fakeplayersai.BerserkerCycleTest"], check=True)


if __name__ == "__main__":
    main()
