# Convert Codex skill drafts into DSH skills.
# DSH discovers project skills at <projectRoot>/.dsh/skills/<name>/SKILL.md (or flat <name>.md).
# Frontmatter requires: name (kebab-case) + description; optional whenToUse.
# Output files are UTF-8 without BOM (the DSH parser expects the first line to be exactly "---").
# This script is ASCII-only by design; Chinese routing hints live in skill-whenToUse.json.
$ErrorActionPreference = 'Stop'

$repo = 'D:\Computer Science\DailyForge'
$outRoot = Join-Path $repo '.dsh\skills'
$mapFile = Join-Path $repo 'scripts\skill-whenToUse.json'

$whenToUse = Get-Content -Raw -Encoding UTF8 $mapFile | ConvertFrom-Json

$srcRoots = @(
  (Join-Path $repo 'skill-drafts'),
  (Join-Path $repo '.codex\skill-drafts')
)

function Get-CodexSkillInfo([string]$content) {
  $m = [regex]::Match(
    $content,
    "\A---\r?\n(?<fm>.*?)\r?\n---\r?\n(?<body>.*)\z",
    [System.Text.RegularExpressions.RegexOptions]::Singleline
  )
  if (-not $m.Success) { return $null }
  $fm = $m.Groups['fm'].Value
  $body = $m.Groups['body'].Value
  $nameM = [regex]::Match($fm, '^name:\s*([^\r\n]+?)\s*$', [System.Text.RegularExpressions.RegexOptions]::Multiline)
  $descM = [regex]::Match($fm, '^description:\s*([^\r\n]+?)\s*$', [System.Text.RegularExpressions.RegexOptions]::Multiline)
  if (-not $nameM.Success -or -not $descM.Success) { return $null }
  $name = $nameM.Groups[1].Value.Trim()
  $desc = $descM.Groups[1].Value.Trim()
  if ($desc.Length -ge 2 -and $desc[0] -eq '"' -and $desc[$desc.Length - 1] -eq '"') {
    $desc = $desc.Substring(1, $desc.Length - 2)
  }
  return @{ name = $name; desc = $desc; body = $body }
}

function Escape-Yaml([string]$s) {
  return $s.Replace('\', '\\').Replace('"', '\"')
}

$enc = [System.Text.UTF8Encoding]::new($false)
$count = 0
$failed = @()
foreach ($src in $srcRoots) {
  if (-not (Test-Path $src)) { continue }
  foreach ($dir in (Get-ChildItem -Directory $src)) {
    $skillMd = Join-Path $dir.FullName 'SKILL.md'
    if (-not (Test-Path $skillMd)) { continue }
    $raw = [System.IO.File]::ReadAllText($skillMd, [System.Text.Encoding]::UTF8)
    $info = Get-CodexSkillInfo $raw
    if ($null -eq $info) { $failed += $skillMd; continue }

    $name = $info.name
    $desc = $info.desc.Replace('Codex', 'DSH').Replace('codex', 'DSH')
    $body = $info.body.Replace('Codex', 'DSH').Replace('codex', 'DSH')
    $wu = $whenToUse.$name
    if ([string]::IsNullOrEmpty($wu)) { $wu = 'DailyForge project skill' }

    $outDir = Join-Path $outRoot $name
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

    $frontmatter = "---`nname: $name`ndescription: `"$(Escape-Yaml $desc)`"`nwhenToUse: `"$(Escape-Yaml $wu)`"`n---`n`n"
    $out = $frontmatter + $body.TrimStart()
    $outPath = Join-Path $outDir 'SKILL.md'
    [System.IO.File]::WriteAllText($outPath, $out, $enc)

    $refDir = Join-Path $dir.FullName 'references'
    if (Test-Path $refDir) {
      Copy-Item -Path $refDir -Destination $outDir -Recurse -Force
    }

    $count++
    Write-Output "converted: $name"
  }
}

Write-Output '---'
Write-Output "total converted: $count"
if ($failed.Count -gt 0) {
  Write-Output 'failed:'
  $failed | ForEach-Object { Write-Output "  $_" }
}
