#!/usr/bin/env python3
"""Fail early on broken skill XML; exercise the shipped core's loading/selection offline.
Requires Python+lxml and JDK25. Does not start GameServer, connect to MySQL or modify JARs.
"""
from pathlib import Path
import os
import subprocess
import tempfile
from lxml import etree

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'game/data'
RECOVERED = Path('stats/skills/01300-01399.xml')


def validate(data):
    schema = etree.XMLSchema(etree.parse(str(data / 'xsd/skills.xsd')))
    files = sorted((data / 'stats/skills').rglob('*.xml'))
    assert files, 'No skill XML files found'
    for file in files:
        schema.assertValid(etree.parse(str(file)))
    skills = {int(s.get('id')): s for s in etree.parse(str(data / RECOVERED)).getroot().findall('skill')}
    assert len(skills) == 99, f'Incomplete recovered range: {len(skills)} definitions'
    assert all(1300 <= sid <= 1399 for sid in skills), 'Unexpected skill ID range'
    assert skills[1320].get('levels') == '9'
    assert skills[1322].get('levels') == '1'
    assert skills[1322].find('./effects/effect').get('name') == 'OpenCommonRecipeBook'
    references = 0
    for file in (data / 'stats/players/skillTrees').rglob('*.xml'):
        for learn in etree.parse(str(file)).getroot().findall('.//skill'):
            sid = int(learn.get('skillId', '0'))
            if 1300 <= sid <= 1399:
                level = int(learn.get('skillLevel'))
                assert sid in skills and 1 <= level <= int(skills[sid].get('levels')), (file, sid, level)
                references += 1
    assert references > 0
    return len(files), references


def main():
    files, references = validate(DATA)
    # Controlled corruption must fail the same preflight before any Java/server startup.
    with tempfile.TemporaryDirectory(prefix='login-skills-') as directory:
        temp = Path(directory)
        bad_data = temp / 'broken'
        (bad_data / 'xsd').mkdir(parents=True)
        (bad_data / RECOVERED).parent.mkdir(parents=True)
        (bad_data / 'xsd/skills.xsd').write_bytes((DATA / 'xsd/skills.xsd').read_bytes())
        (bad_data / RECOVERED).write_text('__XML_B64__')
        try:
            validate(bad_data)
        except etree.XMLSyntaxError:
            pass
        else:
            raise AssertionError('Placeholder corruption was not rejected')
        print(f'PASS: {files} skill XML/XSD files; {references} skill-tree references; placeholder rejected', flush=True)
        cp = os.pathsep.join(str(p) for p in sorted((ROOT / 'libs').glob('*.jar')))
        compiler = temp / 'Compile.java'
        compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        classes = temp / 'classes'
        sources = [ROOT / 'game/data/scripts/handlers/EffectMasterHandler.java',
                   *sorted((DATA / 'scripts/handlers/skill/effects').glob('*.java')),
                   *sorted((DATA / 'scripts/custom').rglob('*.java')),
                   Path(__file__).with_name('CharacterLoginSkillDataTest.java')]
        subprocess.run(['java', str(compiler), '-cp', cp, '-d', str(classes), *map(str, sources)], cwd=ROOT, check=True, timeout=180)
        result = subprocess.run(['java', '-cp', str(classes) + os.pathsep + cp,
                                 'CharacterLoginSkillDataTest', str(ROOT / 'game')],
                                cwd=ROOT / 'game', capture_output=True, text=True, timeout=120)
        # The negative control intentionally logs exactly one missing1322 warning.
        if result.returncode:
            print(result.stdout)
            print(result.stderr)
            result.check_returncode()
        assert result.stderr.count('No skill info found') == 1, result.stderr
        assert 'No skill info found for skill id 1322 and skill level 1.' in result.stderr
        assert 'SEVERE:' not in result.stderr, result.stderr
        print(result.stdout.strip())
    print('PASS: offline regression only; live Windows GameServer/DB/client acceptance is still required')


if __name__ == '__main__':
    main()
