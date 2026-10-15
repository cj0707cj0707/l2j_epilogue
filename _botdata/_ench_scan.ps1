# FakePlayersAI: static scan of bot skills x enchant routes.
# Builds the union of class-lineage skill trees for the 18 olympiad bot classes,
# parses skill XMLs (routes, effects, runSpd penalties) and simulates the OLD
# (Power>Chance) and the NEW (damage: Duel>Power, self-slow: Decrease Penalty) policies.
# Read-only research helper (like _extract.ps1).

$ErrorActionPreference = 'Stop'
$root = "C:\EpilogueServer"

$damageEffects = @('PhysicalDamage','EnergyDamage','Backstab','FatalBlow','SoulBlow','PhysicalSoulDamage','PhysicalDamageHpLink','MagicalDamage','MagicalDamageByAbnormal','MagicalDamageMp','MagicalSoulDamage','HpDrain','DeathLink')

# ---------------- skill trees ----------------
$trees = @{}
Get-ChildItem "$root\game\data\stats\players\skillTrees" -Recurse -Filter *.xml | ForEach-Object {
	$t = [IO.File]::ReadAllText($_.FullName)
	$h = [regex]::Match($t, '<skillTree type="classSkillTree" classId="(\d+)"')
	if ($h.Success) {
		$cid = [int]$h.Groups[1].Value
		if (-not $trees.ContainsKey($cid)) { $trees[$cid] = New-Object 'System.Collections.Generic.HashSet[int]' }
		foreach ($m in [regex]::Matches($t, '<skill skillName="[^"]*" skillId="(\d+)"')) { [void]$trees[$cid].Add([int]$m.Groups[1].Value) }
	}
}

# ---------------- class lineage ----------------
$parent = @{}
foreach ($m in [regex]::Matches([IO.File]::ReadAllText("$root\game\data\stats\players\classList.xml"), '<class classId="(\d+)" name="[^"]*"(\s+parentClassId="(\d+)")?\s*/>')) {
	$parent[[int]$m.Groups[1].Value] = if ($m.Groups[3].Success) { [int]$m.Groups[3].Value } else { -1 }
}

$botClasses = @(88,90,91,92,93,94,95,101,102,103,106,108,109,110,113,114,115,131)
$botNames = @{88='Duelist';90='PhoenixKnight';91='HellKnight';92='Sagittarius';93='Adventurer';94='Archmage';95='Soultaker';101='WindRider';102='MoonlightSentinel';103='MysticMuse';106='ShillienTemplar';108='GhostHunter';109='GhostSentinel';110='StormScreamer';113='Titan';114='GrandKhavatari';115='Dominator';131='Doombringer'}

$union = New-Object 'System.Collections.Generic.HashSet[int]'
foreach ($c in $botClasses) {
	$x = $c
	while ($x -ge 0) {
		if ($trees.ContainsKey($x)) { foreach ($s in $trees[$x]) { [void]$union.Add($s) } }
		$x = if ($parent.ContainsKey($x)) { $parent[$x] } else { -1 }
	}
}

# ---------------- helpers ----------------
function Resolve-Val([string]$v, $tables) {
	if ([string]::IsNullOrWhiteSpace($v)) { return $null }
	$v = $v.Trim()
	if ($v.StartsWith('#')) {
		if ($tables.ContainsKey($v)) { $v = $tables[$v] } else { return $null }
	}
	$d = 0.0
	if ([double]::TryParse($v, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$d)) { return $d }
	return $null
}

# movement penalty of one effects-block: Immobile* -> 1.0; mul runSpd v<1 -> 1-v; sub v>0 -> v/100
function Block-Penalty([string]$block, $tables) {
	if (-not $block) { return 0.0 }
	$p = 0.0
	foreach ($m in [regex]::Matches($block, '<effect name="(\w+)"')) {
		if ($m.Groups[1].Value.StartsWith('Immobile')) { $p = [Math]::Max($p, 1.0) }
	}
	foreach ($m in [regex]::Matches($block, '<(mul|sub|add)\s+stat="runSpd"\s*(?:val="([^"]*)")?[^>]*>((?:(?!</\1>).)*)</\1>', 'Singleline')) {
		$op = $m.Groups[1].Value
		$raw = if ($m.Groups[2].Success -and $m.Groups[2].Value) { $m.Groups[2].Value } else { [regex]::Replace($m.Groups[3].Value, '<[^>]+>', '') }
		$val = Resolve-Val $raw $tables
		if ($null -eq $val) { continue }
		if ($op -eq 'mul' -and $val -lt 1.0) { $p = [Math]::Max($p, 1.0 - $val) }
		elseif ($op -eq 'sub' -and $val -gt 0) { $p = [Math]::Max($p, $val / 100.0) }
		elseif ($op -eq 'add' -and $val -lt 0) { $p = [Math]::Max($p, -$val / 100.0) }
	}
	return $p
}

# parse an effects block into a comparable signature string (effect names + power values)
function Block-Signature([string]$block, $tables) {
	if (-not $block) { return '' }
	$sig = @()
	foreach ($em in [regex]::Matches($block, '(?s)<effect name="(\w+)"[^>]*>(.*?)</effect>|<effect name="(\w+)"[^>]*/>')) {
		$nm = if ($em.Groups[1].Success) { $em.Groups[1].Value } else { $em.Groups[3].Value }
		$body = if ($em.Groups[2].Success) { $em.Groups[2].Value } else { '' }
		$pm = [regex]::Match($body, '<power>\s*(?:<value>)?([^<]*?)(?:</value>)?\s*</power>')
		if (-not $pm.Success) { $pm = [regex]::Match($body, '<power\s+val="([^"]*)"') }
		$pv = ''
		if ($pm.Success) { $d = Resolve-Val $pm.Groups[1].Value $tables; if ($null -ne $d) { $pv = "$d" } else { $pv = $pm.Groups[1].Value.Trim() } }
		$sig += "$nm=$pv"
	}
	return ($sig -join '|')
}

# ---------------- parse skills ----------------
$skills = @{}
foreach ($f in (Get-ChildItem "$root\game\data\stats\skills" -Filter *.xml)) {
	$t = [IO.File]::ReadAllText($f.FullName)
	$ms = [regex]::Matches($t, '(?m)^\t<skill\s+id="(\d+)"[^>]*>')
	for ($i = 0; $i -lt $ms.Count; $i++) {
		$id = [int]$ms[$i].Groups[1].Value
		$start = $ms[$i].Index
		$end = if ($i + 1 -lt $ms.Count) { $ms[$i + 1].Index } else { $t.Length }
		$block = $t.Substring($start, $end - $start)
		if (-not ($union.Contains($id))) { continue }   # only bot skills are needed
		$openTag = $ms[$i].Value
		$name = [regex]::Match($openTag, 'name="([^"]*)"').Groups[1].Value
		$op = [regex]::Match($block, '<operateType>(\w+)</operateType>').Groups[1].Value
		$tt = [regex]::Match($block, '<targetType>(\w+)</targetType>').Groups[1].Value
		$routes = @{}
		foreach ($m in [regex]::Matches($openTag, 'enchantGroup(\d+)="(\d+)"')) { $routes[[int]$m.Groups[1].Value] = [int]$m.Groups[2].Value }
		$tables = @{}
		foreach ($m in [regex]::Matches($block, '<table name="(#\w+)">([^<]*)</table>')) { $tables[$m.Groups[1].Value] = ($m.Groups[2].Value.Trim() -split '\s+') | Select-Object -Last 1 }

		$baseG = [regex]::Match($block, '<effects>(.*?)</effects>', 'Singleline').Groups[1].Value
		$baseS = [regex]::Match($block, '<selfEffects>(.*?)</selfEffects>', 'Singleline').Groups[1].Value
		$basePvp = [regex]::Match($block, '(?<!\w)<pvpEffects>', 'Singleline').Success

		$basePen = [Math]::Max((Block-Penalty $baseG $tables), (Block-Penalty $baseS $tables))
		$namesG = [regex]::Matches($baseG, '<effect name="(\w+)"') | ForEach-Object { $_.Groups[1].Value }
		$isDamage = ($namesG | Where-Object { $damageEffects -contains $_ }).Count -gt 0

		$routeInfo = @{}
		foreach ($n in $routes.Keys) {
			$attrs = [regex]::Matches($block, [string]::Format('<enchant{0}\s+name="(\w+)"', $n)) | ForEach-Object { $_.Groups[1].Value }
			$g = [regex]::Match($block, ('<enchant{0}effects>(.*?)</enchant{0}effects>' -f $n), 'Singleline').Groups[1].Value
			$s = [regex]::Match($block, ('<enchant{0}selfEffects>(.*?)</enchant{0}selfEffects>' -f $n), 'Singleline').Groups[1].Value
			$pvp = [regex]::Match($block, ('<enchant{0}pvpEffects>(.*?)</enchant{0}pvpEffects>' -f $n), 'Singleline').Groups[1].Value
			$pve = [regex]::Match($block, ('<enchant{0}pveEffects>(.*?)</enchant{0}pveEffects>' -f $n), 'Singleline').Groups[1].Value
			$label = 'Other'
			if ($attrs -contains 'pvpPower') { $label = 'Duel' }
			elseif ($attrs -contains 'power') { $label = 'Power' }
			elseif (($attrs -contains 'mpConsume') -or ($attrs -contains 'mpInitialConsume') -or ($attrs -contains 'hpConsume')) { $label = 'Cost' }
			elseif (($attrs -contains 'element') -or ($attrs -contains 'elementPower')) { $label = 'Element' }
			elseif ($attrs -contains 'abnormalType') { $label = 'Debuff' }
			elseif ($attrs -contains 'activateRate') { $label = 'Chance' }
			elseif ($attrs -contains 'abnormalTime') { $label = 'Time' }
			elseif ($attrs -contains 'reuseDelay') { $label = 'Reuse' }
			elseif ($attrs -contains 'effectPoint') { $label = 'Hate' }
			$note = ''
			if (-not $pvp -and $basePvp) { $note = 'pvpRemoved' }
			if ($pvp -and -not $basePvp) {
				$label = 'Duel'; $note = 'pvpEffects'
			}
			# runtime checks the movement penalty on EVERY route regardless of its label;
			# scopes not overridden by the route are inherited from the base skill
			$gEff = if ($g) { $g } else { $baseG }
			$sEff = if ($s) { $s } else { $baseS }
			if ($basePen -gt 0) {
				$pen = [Math]::Max((Block-Penalty $gEff $tables), (Block-Penalty $sEff $tables))
				if ($pen -lt $basePen) { $label = 'PenaltyDown'; $note = "pen $basePen -> $pen" }
			}
			if ($label -eq 'Other') {
				$sigR = (Block-Signature $gEff $tables) + ';' + (Block-Signature $sEff $tables)
				$sigB = (Block-Signature $baseG $tables) + ';' + (Block-Signature $baseS $tables)
				if ($sigR -ne $sigB) { $label = 'EffectChange' }
			}
			$routeInfo[$n] = @{ label = $label; note = $note }
		}

		$skills[$id] = @{ name = $name; op = $op; tt = $tt; routes = $routes; basePen = $basePen; isDamage = $isDamage; routeInfo = $routeInfo }
	}
}

# ---------------- policy simulation ----------------
$rows = @()
foreach ($id in ($skills.Keys | Sort-Object)) {
	$sk = $skills[$id]
	if ($sk.routes.Count -eq 0) { continue }
	$knownBy = ($botClasses | Where-Object { $perClassSet = $null; $true }) | Out-Null
	$knownBy = @()
	foreach ($c in $botClasses) {
		$x = $c
		while ($x -ge 0) {
			if ($trees.ContainsKey($x) -and $trees[$x].Contains($id)) { $knownBy += $botNames[$c]; break }
			$x = if ($parent.ContainsKey($x)) { $parent[$x] } else { -1 }
		}
	}

	$routeLabels = ($sk.routeInfo.GetEnumerator() | Sort-Object Key | ForEach-Object { "$($_.Key)=$($_.Value.label)$(if($_.Value.note){'('+ $_.Value.note +')'})" }) -join ' '

	# OLD policy: Power route, else Chance
	$old = '-'
	foreach ($v in $sk.routeInfo.Values) { if ($v.label -eq 'Power') { $old = 'Power'; break } }
	if ($old -eq '-') { foreach ($v in $sk.routeInfo.Values) { if ($v.label -eq 'Chance') { $old = 'Chance'; break } } }

	# NEW policy
	$new = '-'
	if ($sk.isDamage) {
		$duel = ($sk.routeInfo.Values | Where-Object { $_.label -eq 'Duel' }).Count -gt 0
		$pow = ($sk.routeInfo.Values | Where-Object { $_.label -eq 'Power' -or $_.label -eq 'EffectChange' }).Count -gt 0
		if ($duel) { $new = 'Duel' } elseif ($pow) { $new = 'Power' } else { $new = "Chance$(if ($old -ne 'Chance') {'(was -)'})" }
	}
	elseif ($sk.basePen -gt 0) {
		$best = $null
		foreach ($v in $sk.routeInfo.Values) {
			if ($v.label -eq 'PenaltyDown') { $best = $v; break }
		}
		if ($best) { $new = 'PenaltyDown' } else { $new = "- (no penalty route; old: $old)" }
	}
	else { $new = $old }

	$rows += [pscustomobject]@{
		Id = $id; Name = $sk.name; Op = $sk.op; Target = $sk.tt; Dmg = $sk.isDamage; BasePen = [Math]::Round($sk.basePen, 2)
		KnownBy = ($knownBy -join ','); Routes = $routeLabels; Old = $old; New = $new
	}
}

Write-Host ("Bot skill union: {0} skills; with enchant routes: {1}" -f $union.Count, $rows.Count)
Write-Host ""
Write-Host "=== COMPACT (id|name|op|target|dmg|basePen|old|new|routes) ==="
foreach ($r in $rows) { Write-Host ("{0}|{1}|{2}|{3}|{4}|{5}|{6}|{7}|{8}" -f $r.Id, $r.Name, $r.Op, $r.Target, $r.Dmg, $r.BasePen, $r.Old, $r.New, $r.Routes) }
Write-Host ""
Write-Host "=== CHANGED (Old -> New differ) ==="
$rows | Where-Object { ($_.Old -ne ($_.New -replace '\(.*\)','')) -and ($_.Old -ne $_.New) } | Format-Table Id, Name, Op, Dmg, BasePen, KnownBy, Routes, Old, New -AutoSize -Wrap
Write-Host "=== SELF-SLOW SKILLS (basePen > 0) ==="
$rows | Where-Object { $_.BasePen -gt 0 } | Format-Table Id, Name, Op, Target, Dmg, BasePen, KnownBy, Routes, New -AutoSize -Wrap
Write-Host "=== DAMAGE SKILLS ==="
$rows | Where-Object { $_.Dmg } | Format-Table Id, Name, Op, KnownBy, Routes, Old, New -AutoSize -Wrap
Write-Host "=== NON-DAMAGE, NO PENALTY (unchanged candidates) ==="
$rows | Where-Object { -not $_.Dmg -and $_.BasePen -le 0 } | Format-Table Id, Name, Op, Target, KnownBy, Routes, Old, New -AutoSize -Wrap
