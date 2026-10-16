#!/usr/bin/env python3
"""Compile production adapter + isolated actual-GameServer.jar tests; no API doubles."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
source = root / 'game/data/scripts/custom/fakeplayersai/TrainingPeaceZones.java'
test = root / 'tests/fakeplayersai/TrainingPeaceZonesActualJarTest.java'
classpath = os.pathsep.join(str(p) for p in sorted((root / 'libs').glob('*.jar')))
with tempfile.TemporaryDirectory(prefix='training-peace-zones-') as temp:
    temp = Path(temp)
    compiler = temp / 'TrainingPeaceZonesCompiler.java'
    compiler.write_text('''import javax.tools.ToolProvider;
public class TrainingPeaceZonesCompiler {
 public static void main(String[] args) {
  System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,args));
 }
}''')
    subprocess.run(['java', str(compiler), '-cp', classpath, '-d', str(temp), str(source), str(test)], cwd=root, check=True)
    subprocess.run(['java', '-ea', '-cp', str(temp) + os.pathsep + classpath,
                    'custom.fakeplayersai.TrainingPeaceZonesActualJarTest'], cwd=root, check=True, timeout=90)
print('PASS: production source compiled against actual jars and real zone membership tests passed')
