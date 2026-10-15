# Generator: builds OFFERED_SKILLS (Java) + custom augmentation option XMLs
# for ALL skills named "Item Skill:*", using retail option encodings.
$ErrorActionPreference = 'Stop'
$root = 'C:\EpilogueServer\game\data\stats'

$elements = @{ '0'='Fire'; '1'='Water'; '2'='Wind'; '3'='Earth'; '4'='Holy'; '5'='Dark' }

# --- 1. Item Skill definitions from skills XML ---
$skills = @{}
foreach ($f in Get-ChildItem "$root\skills" -Filter '*.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($s in $doc.list.skill) {
        if ($s.name -like 'Item Skill:*') {
            $elementTag = $null; $targetType = $null
            foreach ($child in $s.ChildNodes) {
                if ($child.Name -eq 'element') { $elementTag = $child.InnerText }
                if ($child.Name -eq 'targetType') { $targetType = $child.InnerText }
            }
            $skills[[int]$s.id] = [pscustomobject]@{
                id = [int]$s.id; name = $s.name; levels = [int]$s.levels
                element = $elementTag; targetType = $targetType
            }
        }
    }
}

# --- 2. Retail option encodings per skill: kinds + fixed chance ---
$optKinds = @{}   # id -> list of kinds
$optChance = @{}  # id -> chance
foreach ($f in Get-ChildItem "$root\augmentation\options" -Filter '*.xml') {
    [xml]$doc = Get-Content $f.FullName -Raw
    foreach ($o in $doc.list.option) {
        foreach ($child in $o.ChildNodes) {
            if ($child.Name -in @('active_skill','passive_skill','attack_skill','magic_skill','critical_skill')) {
                $sid = [int]$child.id
                if (-not $skills.ContainsKey($sid)) { continue }
                if (-not $optKinds.ContainsKey($sid)) { $optKinds[$sid] = New-Object System.Collections.Generic.List[string] }
                [void]$optKinds[$sid].Add($child.Name)
                if ($child.chance) { $optChance[$sid] = [double]$child.chance }
            }
        }
    }
}

# --- 3. Decide kind/level/label per skill ---
$order = @('active_skill','passive_skill','attack_skill','magic_skill','critical_skill')
$rows = New-Object System.Collections.Generic.List[object]
foreach ($id in ($skills.Keys | Sort-Object)) {
    $s = $skills[$id]
    $kinds = if ($optKinds.ContainsKey($id)) { $optKinds[$id] } else { @() }
    $kind = $null
    foreach ($k in $order) { if ($kinds -contains $k) { $kind = $k; break } }
    if (-not $kind) { $kind = 'active_skill' } # fallback, should not happen

    $level = $s.levels
    $chance = if ($optChance.ContainsKey($id)) { $optChance[$id] } else { 5.0 }

    if ($kind -eq 'active_skill') {
        $label = if ($s.element -and $elements.ContainsKey($s.element)) { $elements[$s.element] } else { 'Active' }
        if ($s.targetType -in @('AREA','AURA')) { $label = "$label AoE" }
    }
    elseif ($kind -eq 'passive_skill') { $label = 'Passive' }
    elseif ($kind -eq 'attack_skill')  { $label = "Chance $chance% (attack)" }
    elseif ($kind -eq 'magic_skill')   { $label = "Chance $chance% (magic)" }
    else                               { $label = "Chance $chance% (crit)" }

    $rows.Add([pscustomobject]@{
        id = $id; name = $s.name; level = $level; kind = $kind
        chance = if ($kind -in @('attack_skill','magic_skill','critical_skill')) { $chance } else { $null }
        label = $label; targetType = $s.targetType
    })
}
"rows: $($rows.Count)"

# --- 4. Emit Java table ---
$java = New-Object System.Collections.Generic.List[string]
foreach ($r in $rows) {
    $esc = $r.label -replace '"',''
    $line = "{0}" -f $r.id
    $java.Add("`t`t{ $line, $($r.level), `"$esc`" }, // $($r.name)")
}

# --- 5. Emit option XML files (max 100 options per file, xsd limit) ---
function Emit-OptionsFile([int]$startIdx, [int]$endIdx, [string]$path) {
    $firstId = 25000 + $startIdx
    $lastId  = 25000 + $endIdx
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine("<!-- Custom augmentation options for the Blacksmith Aug NPC (Giran, npcId 1006000).")
    [void]$sb.AppendLine("	Each option augments one of the retail augmentation skills (all skills named")
    [void]$sb.AppendLine("""Item Skill:*"") into a weapon. Encoding follows the retail option data:")
    [void]$sb.AppendLine("	active_skill = castable active augment, passive_skill = passive augment,")
    [void]$sb.AppendLine("	attack_skill / magic_skill / critical_skill = chance-triggered augment (retail chance).")
    [void]$sb.AppendLine("	Levels are the maximum base levels of the skills. Option ids 25000+ are unused")
    [void]$sb.AppendLine("	by retail augmentation/enchant options (retail ends at 24949).")
    [void]$sb.AppendLine("	Keep these ids in sync with the OFFERED_SKILLS list in custom\BlacksmithAug\BlacksmithAug.java:")
    [void]$sb.AppendLine("	option id = 25000 + index in that list (index 0 = option 25000). -->")
    [void]$sb.AppendLine('<list xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../../../xsd/optionsData.xsd">')
    for ($i = $startIdx; $i -le $endIdx; $i++) {
        $r = $rows[$i]
        $oid = 25000 + $i
        $skillTag = switch ($r.kind) {
            'active_skill'   { '<active_skill id="{0}" level="{1}" />' -f $r.id, $r.level }
            'passive_skill'  { '<passive_skill id="{0}" level="{1}" />' -f $r.id, $r.level }
            'attack_skill'   { '<attack_skill id="{0}" level="{1}" chance="{2}" />' -f $r.id, $r.level, ($r.chance.ToString('0.0', [System.Globalization.CultureInfo]::InvariantCulture)) }
            'magic_skill'    { '<magic_skill id="{0}" level="{1}" chance="{2}" />' -f $r.id, $r.level, ($r.chance.ToString('0.0', [System.Globalization.CultureInfo]::InvariantCulture)) }
            'critical_skill' { '<critical_skill id="{0}" level="{1}" chance="{2}" />' -f $r.id, $r.level, ($r.chance.ToString('0.0', [System.Globalization.CultureInfo]::InvariantCulture)) }
        }
        $name = $r.name
        if ($r.chance) { $comment = "<!-- $name - chance-triggered, lv $($r.level), chance $($r.chance)% -->" }
        elseif ($r.kind -eq 'passive_skill') { $comment = "<!-- $name - passive, lv $($r.level) -->" }
        else { $comment = "<!-- $name - active, lv $($r.level) -->" }
        [void]$sb.AppendLine("`t<option id=`"$oid`" name=`"o_aug_$($r.id)`">")
        [void]$sb.AppendLine("`t`t$skillTag $comment")
        [void]$sb.AppendLine("`t</option>")
    }
    [void]$sb.AppendLine('</list>')
    Set-Content -Path $path -Value $sb.ToString() -Encoding UTF8
    "written: $path ($($endIdx - $startIdx + 1) options, ids $firstId..$lastId)"
}

Emit-OptionsFile 0 99  'C:\EpilogueServer\_tmp\25000-25099.xml'
Emit-OptionsFile 100 ($rows.Count - 1) 'C:\EpilogueServer\_tmp\25100-25199.xml'

# --- 6. Emit Java block + CSV for review ---
$javaText = $java -join "`r`n"
Set-Content 'C:\EpilogueServer\_tmp\offered_skills_block.txt' -Value $javaText -Encoding UTF8
$rows | Export-Csv 'C:\EpilogueServer\_tmp\item_skills_final.csv' -NoTypeInformation -Encoding UTF8

# --- 7. Sanity checks ---
$dupNames = $rows | Group-Object name | Where-Object { $_.Count -gt 1 } | ForEach-Object { "$($_.Name): $($_.Group.id -join ',')" }
"=== duplicated names (info) ==="
$dupNames
"=== kind distribution ==="
$rows | Group-Object kind | Select-Object Count, Name
"=== first/last ==="
$rows[0] | Format-List | Out-String
$rows[-1] | Format-List | Out-String
"=== sample labels ==="
$rows | Select-Object -First 6 | ForEach-Object { "$($_.id) | $($_.name) | lv $($_.level) | $($_.kind) | $($_.label)" }
"done"