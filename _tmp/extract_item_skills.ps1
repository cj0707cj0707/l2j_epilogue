# Temporary analysis: extract all "Item Skill:" skills, their skillmap types and retail option encodings.
$ErrorActionPreference = 'Stop'
$root = 'C:\EpilogueServer\game\data\stats'

# 1. Parse skills XML for Item Skill entries
$skills = @{}
foreach ($f in Get-ChildItem "$root\skills" -Filter '*.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($s in $doc.list.skill) {
        if ($s.name -like 'Item Skill:*') {
            $elementTag = $null
            $targetType = $null
            $operateType = $null
            foreach ($child in $s.ChildNodes) {
                if ($child.Name -eq 'element') { $elementTag = $child.InnerText }
                if ($child.Name -eq 'targetType') { $targetType = $child.InnerText }
                if ($child.Name -eq 'operateType') { $operateType = $child.InnerText }
            }
            $skills[[int]$s.id] = [pscustomobject]@{
                id = [int]$s.id; name = $s.name; levels = [int]$s.levels
                operateType = $operateType; targetType = $targetType; element = $elementTag
            }
        }
    }
}
"Skills found: $($skills.Count)"

# 2. Parse skillmap: skillId -> types, levels
$mapTypes = @{}
$mapLevels = @{}
[xml]$sm = Get-Content "$root\augmentation\augmentation_skillmap.xml" -Raw
foreach ($a in $sm.list.augmentation) {
    $sid = [int]$a.skillId.val
    if (-not $mapTypes.ContainsKey($sid)) { $mapTypes[$sid] = New-Object System.Collections.Generic.HashSet[string] }
    [void]$mapTypes[$sid].Add($a.type.val)
    if (-not $mapLevels.ContainsKey($sid)) { $mapLevels[$sid] = New-Object System.Collections.Generic.HashSet[int] }
    [void]$mapLevels[$sid].Add([int]$a.skillLevel.val)
}
"In skillmap: $(($skills.Keys | Where-Object { $mapTypes.ContainsKey($_) }).Count) / $($skills.Count)"

# 3. Parse retail options: skillId -> kinds, chances
$optKinds = @{}
$optChances = @{}
foreach ($f in Get-ChildItem "$root\augmentation\options" -Filter '*.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($o in $doc.list.option) {
        foreach ($child in $o.ChildNodes) {
            if ($child.Name -in @('active_skill','passive_skill','attack_skill','magic_skill','critical_skill')) {
                $sid = [int]$child.id
                if (-not $skills.ContainsKey($sid)) { continue }
                if (-not $optKinds.ContainsKey($sid)) { $optKinds[$sid] = New-Object System.Collections.Generic.HashSet[string] }
                [void]$optKinds[$sid].Add($child.Name)
                if ($child.chance) {
                    if (-not $optChances.ContainsKey($sid)) { $optChances[$sid] = New-Object System.Collections.Generic.HashSet[double] }
                    [void]$optChances[$sid].Add([double]$child.chance)
                }
            }
        }
    }
}
"In retail options: $(($skills.Keys | Where-Object { $optKinds.ContainsKey($_) }).Count) / $($skills.Count)"

# 4. Report rows
$rows = foreach ($id in ($skills.Keys | Sort-Object)) {
    $s = $skills[$id]
    $types = if ($mapTypes.ContainsKey($id)) { ($mapTypes[$id] | Sort-Object) -join ',' } else { '-' }
    $mapLv = if ($mapLevels.ContainsKey($id)) { (($mapLevels[$id] | Sort-Object) -join '/') } else { '-' }
    $kinds = if ($optKinds.ContainsKey($id)) { ($optKinds[$id] | Sort-Object) -join ',' } else { '-' }
    $ch = if ($optChances.ContainsKey($id)) { (($optChances[$id] | Sort-Object) -join '/') } else { '-' }
    '{0};{1};{2};{3};{4};{5};{6};{7};{8}' -f $id, $s.name, $s.levels, $s.operateType, $s.targetType, $s.element, $types, $kinds, $ch
}
$rows | Set-Content 'C:\EpilogueServer\_tmp\item_skills.csv' -Encoding UTF8
"CSV written"
# Distinct combos of (operateType, targetType, element-tag-kind, types, kinds)
$combos = @{}
foreach ($id in ($skills.Keys | Sort-Object)) {
    $s = $skills[$id]
    $types = if ($mapTypes.ContainsKey($id)) { ($mapTypes[$id] | Sort-Object) -join ',' } else { '-' }
    $kinds = if ($optKinds.ContainsKey($id)) { ($optKinds[$id] | Sort-Object) -join ',' } else { '-' }
    $key = "$($s.operateType) | $($s.targetType) | types=$types | kinds=$kinds"
    if (-not $combos.ContainsKey($key)) { $combos[$key] = New-Object System.Collections.Generic.List[int] }
    $combos[$key].Add($id)
}
"=== combos ==="
foreach ($k in ($combos.Keys | Sort-Object)) { "{0}  ids: {1}" -f $k, (($combos[$k] | Select-Object -First 8) -join ',') }