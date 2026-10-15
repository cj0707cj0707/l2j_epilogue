# Read-only extraction of class skill trees (L2J Mobius Gracia Epilogue)
$ErrorActionPreference = 'Stop'
$data = 'C:\EpilogueServer\game\data'

# ---- 1. classList ----
$classListText = Get-Content "$data\stats\players\classList.xml" -Raw
$classes = @{}
[regex]::Matches($classListText, '<class classId="(\d+)" name="([^"]+)"(?: parentClassId="(\d+)")?\s*/>') | ForEach-Object {
    $classes[[int]$_.Groups[1].Value] = @{
        Id     = [int]$_.Groups[1].Value
        Name   = $_.Groups[2].Value
        Parent = if ($_.Groups[3].Success) { [int]$_.Groups[3].Value } else { $null }
    }
}

# ---- 2. tree file map (classId -> file) ----
$treeMap = @{}
foreach ($folder in 'StartingClass','1stClass','2ndClass','3rdClass') {
    Get-ChildItem "$data\stats\players\skillTrees\$folder\*.xml" | ForEach-Object {
        $head = (Get-Content $_.FullName -TotalCount 6) -join ' '
        if ($head -match 'classId="(\d+)"') {
            $treeMap[[int]$Matches[1]] = @{ File = $_.FullName; Folder = $folder }
        }
    }
}

# ---- 3. combat classes (3rd prof, curated) ----
$combatIds = @(88,89,90,91,92,93,94,95,99,101,102,103,106,108,109,110,113,114,131,133,134)
$combat = @{}
foreach ($id in $combatIds) { $combat[$id] = $classes[$id] }

# ---- 4. parse trees per class (merge lineage) ----
function Parse-Tree($path) {
    $t = Get-Content $path -Raw
    $list = @()
    [regex]::Matches($t, '<skill skillName="([^"]*)" skillId="(\d+)" skillLevel="(\d+)" getLevel="(\d+)"') | ForEach-Object {
        $list += @{
            Name     = $_.Groups[1].Value
            SkillId  = [int]$_.Groups[2].Value
            SkillLvl = [int]$_.Groups[3].Value
            GetLevel = [int]$_.Groups[4].Value
        }
    }
    return $list
}

$merged = @{}   # classId -> arraylist of @{Id;Lvl;GetLevel;TreeName}
foreach ($id in $combatIds) {
    $lineage = @()
    $c = $classes[$id]
    while ($null -ne $c) {
        $lineage = @($c.Id) + $lineage
        $c = if ($null -ne $c.Parent) { $classes[$c.Parent] } else { $null }
    }
    $acc = @{}
    foreach ($lid in $lineage) {
        if ($treeMap.ContainsKey($lid)) {
            foreach ($s in (Parse-Tree $treeMap[$lid].File)) {
                if ($s.SkillLvl -ge 100) { continue }        # enchant levels in trees: ignore
                if (-not $acc.ContainsKey($s.SkillId)) {
                    $acc[$s.SkillId] = @{ Id = $s.SkillId; Lvl = $s.SkillLvl; GetLevel = $s.GetLevel; TreeName = $s.Name }
                } elseif ($s.SkillLvl -gt $acc[$s.SkillId].Lvl) {
                    $acc[$s.SkillId].Lvl = $s.SkillLvl
                    $acc[$s.SkillId].GetLevel = $s.GetLevel
                }
            }
        } else {
            Write-Warning "no tree file for classId $lid (lineage of $id)"
        }
    }
    $merged[$id] = @($acc.Values | Sort-Object Id)
}

# ---- 5. skill definitions ----
$allIds = @{}; foreach ($id in $combatIds) { foreach ($s in $merged[$id]) { $allIds[$s.Id] = $true } }
$needIds = @($allIds.Keys)
$needSet = @{}; foreach ($i in $needIds) { $needSet[$i] = $true }
$skills = @{}

$tableRe = [regex]'(?m)^\t\t<table name="([^"]+)">([^<]*)</table>'
function Resolve-Val($v, $tables) {
    if ($v -match '^#') {
        if ($tables.ContainsKey($v)) {
            $vals = $tables[$v] -split '\s+' | Where-Object { $_ -ne '' }
            return $vals[-1]
        }
        return $v
    }
    return $v
}

foreach ($f in (Get-ChildItem "$data\stats\skills\0*.xml")) {
    $text = Get-Content $f.FullName -Raw
    # quick containment check
    $fileIds = [regex]::Matches($text, '(?m)^\t<skill id="(\d+)"') | ForEach-Object { [int]$_.Groups[1].Value }
    $has = $false
    foreach ($i in $fileIds) { if ($needSet.ContainsKey($i)) { $has = $true; break } }
    if (-not $has) { continue }

    $blocks = [regex]::Matches($text, '(?ms)^\t<skill id="(\d+)"\s+levels="(\d+)"\s+name="([^"]*)"[^>]*>(.*?)^\t</skill>')
    foreach ($b in $blocks) {
        $sid = [int]$b.Groups[1].Value
        if (-not $needSet.ContainsKey($sid)) { continue }
        $body = $b.Groups[4].Value
        $tables = @{}
        foreach ($tm in $tableRe.Matches($body)) { $tables[$tm.Groups[1].Value] = $tm.Groups[2].Value }
        $def = @{
            Id = $sid; Name = $b.Groups[3].Value; Levels = [int]$b.Groups[2].Value
            Operate = '?'; Target = '?'; CastRange = '-'; HitTime = '-'; Reuse = '-'; Mp = '-'
        }
        foreach ($tag in 'operateType','targetType','castRange','hitTime','reuseDelay','mpConsume') {
            $m = [regex]::Match($body, "(?m)^\t\t<$tag>([^<]*)</$tag>")
            if ($m.Success) { $def[$tag.Substring(0,1).ToUpper() + $tag.Substring(1)] = Resolve-Val $m.Groups[1].Value $tables }
        }
        # attribute-style fallback (some entries use attributes)
        $openTag = [regex]::Match($body, '(?s)^[^>]*')
        $skills[$sid] = $def
    }
}

# ---- 6. dump ----
$out = New-Object System.Text.StringBuilder
foreach ($id in $combatIds) {
    [void]$out.AppendLine("=== CLASS $id $($combat[$id].Name) (parent: $($classes[$combat[$id].Parent].Name)) ===")
    foreach ($s in ($merged[$id] | Sort-Object Id)) {
        $d = $skills[$s.Id]
        $op = if ($d) { $d.OperateType } else { 'NODATA' }
        $tg = if ($d) { $d.TargetType } else { '?' }
        $cr = if ($d) { $d.CastRange } else { '?' }
        $nm = if ($d) { $d.Name } else { $s.TreeName }
        [void]$out.AppendLine("$($s.Id)`t$($s.Lvl)`tGL$($s.GetLevel)`t$op`t$tg`tCR$cr`t$nm")
    }
    [void]$out.AppendLine('')
}
$outFile = 'C:\EpilogueServer\_botdata\_extract_raw.txt'
New-Item -ItemType Directory -Force -Path (Split-Path $outFile) | Out-Null
[System.IO.File]::WriteAllText($outFile, $out.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "skills resolved: $($skills.Count) / $($needIds.Count) needed"
Write-Host "raw dump: $outFile"
# report missing
$missing = @($needIds | Where-Object { -not $skills.ContainsKey($_) })
if ($missing) { Write-Host ("MISSING: " + ($missing -join ',')) }
