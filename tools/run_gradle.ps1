param(
    [string]$Task = 'build',
    [switch]$Clean
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($javaCommand) { $candidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source) }
$jdkCache = Join-Path $env:USERPROFILE '.gradle\jdks'
if (Test-Path -LiteralPath $jdkCache) {
    $candidates += Get-ChildItem -LiteralPath $jdkCache -Directory | ForEach-Object FullName
}
$javaHomeForBuild = $null
foreach ($candidate in $candidates | Select-Object -Unique) {
    $release = Join-Path $candidate 'release'
    if ((Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe')) -and (Test-Path -LiteralPath $release)) {
        if ((Get-Content -LiteralPath $release -Raw) -match 'JAVA_VERSION="25(?:[.+-]|\")') {
            $javaHomeForBuild = $candidate
            break
        }
    }
}
if (-not $javaHomeForBuild) { throw 'Java 25 JDK not found. Install a 64-bit JDK 25 and set JAVA_HOME.' }
$env:JAVA_HOME = $javaHomeForBuild
$env:PATH = (Join-Path $javaHomeForBuild 'bin') + ';' + $env:PATH
# Keep Windows Unix-domain socket paths below the platform length limit.
$socketDir = if ($env:MYSTCRAFT_JAVA_TMPDIR) { $env:MYSTCRAFT_JAVA_TMPDIR } else { Join-Path $projectRoot '.jt' }
New-Item -ItemType Directory -Path $socketDir -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = ($env:JAVA_TOOL_OPTIONS + ' -Djdk.net.unixdomain.tmpdir="' + $socketDir + '"').Trim()
Set-Location -LiteralPath $projectRoot
$arguments = @()
if ($Clean) { $arguments += 'clean' }
$arguments += $Task
$arguments += '--console=plain'
Write-Host "Using Java 25: $javaHomeForBuild"
$ErrorActionPreference = 'Continue'
& (Join-Path $projectRoot 'gradlew.bat') @arguments 2>&1 | ForEach-Object { $_.ToString() } | Tee-Object -FilePath (Join-Path $projectRoot ($Task.TrimStart('-') + '_LAST.log'))
$code = $LASTEXITCODE
exit $code
