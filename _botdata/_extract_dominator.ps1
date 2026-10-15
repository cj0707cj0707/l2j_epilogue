# Read-only extraction of Dominator (115) lineage skills with skill metadata
$ErrorActionPreference = 'Stop'
$data = 'C:\EpilogueServer\game\data'

# lineage: 115 Dominator <- 51 Overlord <- 50 Orc Shaman <- 49 Orc Mystic
$lineage = @(49, 50, 51, 115)
$files = @{
    49  = "$data\stats\players\skillTrees\StartingClass\OrcMystic.xml"
    50  = "$data\stats\players\skillTrees\1stClass\OrcShaman.xml"
    51  = "$data\stats\players\skillTrees\2ndClass\Overlord.xml"
    115 = "$data\stats\players\skillTrees\3rdClass\Dominator.xml"
}

$acc = @{}
foreach ($lid in $lineage) {
    $t = Get-Content $files[$lid] -Raw
    [regex]::Matches($t, '<skill skillName="([^"]*)" skillId="(\d+)" skillLevel="(\d+)" getLevel="(\d+)"') | ForEach-Object {
        $id = [int]$_.Groups[2].Value
        $lvl = [int]$_.Groups[3].Value
        if ($lvl -ge 100) { return }
        if (-not $acc.ContainsKey($id) -or $lvl -gt $acc[$id].Lvl) {
            $acc[$id] = @{ Id = $id; Lvl = $lvl; GetLevel = [int]$_.Groups[4].Value; Name = $_.Groups[1].Value }
        }
    }
}

$needSet = @{}
foreach ($k in $acc.Keys) { $needSet[$k] = $true }

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

$skills = @{}
foreach ($f in (Get-ChildItem "$data\stats\skills\0*.xml")) {
    $text = Get-Content $f.FullName -Raw
    $blocks = [regex]::Matches($text, '(?ms)^\t<skill id="(\d+)"\s+levels="(\d+)"\s+name="([^"]*)"[^>]*>(.*?)^\t</skill>')
    foreach ($b in $blocks) {
        $sid = [int]$b.Groups[1].Value
        if (-not $needSet.ContainsKey($sid)) { continue }
        $body = $b.Groups[4].Value
        $tables = @{}
        foreach ($tm in $tableRe.Matches($body)) { $tables[$tm.Groups[1].Value] = $tm.Groups[2].Value }
        $def = @{ Id = $sid; Name = $b.Groups[3].Value; Operate = '?'; Target = '?'; CastRange = '-'; Mp = '-' }
        foreach ($tag in 'operateType','targetType','castRange','mpConsume') {
            $m = [regex]::Match($body, "(?m)^\t\t<$tag>([^<]*)</$tag>")
            if ($m.Success) { $def[$tag.Substring(0,1).ToUpper() + $tag.Substring(1)] = Resolve-Val $m.Groups[1].Value $tables }
        }
        $skills[$sid] = $def
    }
}

$out = New-Object System.Text.StringBuilder
[void]$out.AppendLine("=== CLASS 115 Dominator (lineage 49 Orc Mystic -> 50 Orc Shaman -> 51 Overlord -> 115) ===")
foreach ($s in ($acc.Values | Sort-Object Id)) {
    $d = $skills[$s.Id]
    if ($null -eq $d) {
        [void]$out.AppendLine("$($s.Id)`tL$($s.Lvl)`tGL$($s.GetLevel)`tNODATA`t$($s.Name)")
        continue
    }
    [void]$out.AppendLine("$($s.Id)`tL$($s.Lvl)`tGL$($s.GetLevel)`t$($d.OperateType)`t$($d.TargetType)`tCR$($d.CastRange)`tMP$($d.Mp)`t$($d.Name)")
}
Write-Host $out.ToString()
Write-Host "resolved: $($skills.Count) / $($needSet.Count)"
