# Verify 1:1 sync between OFFERED_SKILLS (Java) and deployed option XMLs.
$ErrorActionPreference = 'Stop'
$java = Get-Content 'C:\EpilogueServer\game\data\scripts\custom\BlacksmithAug\BlacksmithAug.java' -Raw
$rows = [regex]::Matches($java, '\{ (\d{4}), (\d+), "([^"]+)" \}, // ([^\r\n]+)')
"java rows: $($rows.Count)"

$opts = @{}
foreach ($f in Get-ChildItem 'C:\EpilogueServer\game\data\stats\augmentation\options\25000-25099.xml','C:\EpilogueServer\game\data\stats\augmentation\options\25100-25199.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($o in $doc.list.option) {
        $child = $o.ChildNodes | Where-Object { $_.Name -in @('active_skill','passive_skill','attack_skill','magic_skill','critical_skill') } | Select-Object -First 1
        $opts[[int]$o.id] = [pscustomobject]@{ kind = $child.Name; skillId = [int]$child.id; level = [int]$child.level; chance = $child.chance }
    }
}
"options loaded: $($opts.Count)"

function Get-ExpectedKind([string]$label) {
    if ($label -eq 'Passive') { return 'passive_skill' }
    if ($label -like 'Chance*') {
        if ($label -like '*(attack)*') { return 'attack_skill' }
        if ($label -like '*(magic)*') { return 'magic_skill' }
        return 'critical_skill'
    }
    return 'active_skill'
}

$bad = 0
for ($i = 0; $i -lt $rows.Count; $i++) {
    $skillId = [int]$rows[$i].Groups[1].Value
    $level = [int]$rows[$i].Groups[2].Value
    $label = $rows[$i].Groups[3].Value
    $name = $rows[$i].Groups[4].Value.Trim()
    $oid = 25000 + $i
    if (-not $opts.ContainsKey($oid)) { "MISSING option ${oid} (row ${i}, ${name})"; $bad++; continue }
    $o = $opts[$oid]
    if ($o.skillId -ne $skillId) { "MISMATCH option ${oid}: xml skill $($o.skillId) != java ${skillId} (${name})"; $bad++ }
    if ($o.level -ne $level) { "LEVELMISMATCH option ${oid}: xml level $($o.level) != java ${skillId} (${name})"; $bad++ }
    $expectedKind = Get-ExpectedKind $label
    if ($o.kind -ne $expectedKind) { "KINDMISMATCH option ${oid}: xml $($o.kind) != label '${label}' (${name})"; $bad++ }
}
"mismatches: $bad"

# Every Java skill id must be an existing "Item Skill:*" skill in the datapack
$skillNames = @{}
$skillLevels = @{}
foreach ($f in Get-ChildItem 'C:\EpilogueServer\game\data\stats\skills' -Filter '*.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($s in $doc.list.skill) {
        $skillNames[[int]$s.id] = $s.name
        $skillLevels[[int]$s.id] = [int]$s.levels
    }
}
$notItem = 0
$badLv = 0
for ($i = 0; $i -lt $rows.Count; $i++) {
    $sid = [int]$rows[$i].Groups[1].Value
    $lvl = [int]$rows[$i].Groups[2].Value
    $name = $skillNames[$sid]
    if (-not $name -or -not $name.StartsWith('Item Skill:')) { "NOT ITEM SKILL: ${sid} ($name)"; $notItem++ }
    if ($skillLevels[$sid] -lt $lvl) { "LEVEL too high for ${sid}: max=$($skillLevels[$sid]) used=${lvl}"; $badLv++ }
}
"non-Item-Skill entries: $notItem"
"level violations: $badLv"
if (($bad -eq 0) -and ($notItem -eq 0) -and ($badLv -eq 0)) { "ALL CHECKS PASSED" } else { "CHECKS FAILED" }