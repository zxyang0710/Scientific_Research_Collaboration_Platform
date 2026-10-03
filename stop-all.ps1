$ErrorActionPreference = "SilentlyContinue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtime = Join-Path $root ".runtime"
$stateFile = Join-Path $runtime "processes.json"

function Stop-ProcessTree([int]$ProcessId) {
  if (-not $ProcessId) {
    return
  }

  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId"
  foreach ($child in $children) {
    Stop-ProcessTree -ProcessId $child.ProcessId
  }

  Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

if (Test-Path -LiteralPath $stateFile) {
  $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
  foreach ($id in @($state.backend, $state.frontend)) {
    Stop-ProcessTree -ProcessId ([int]$id)
  }
}

# Also clean up listeners in case the launcher process was already closed.
$listeners = Get-NetTCPConnection -LocalPort 8080,5173 -State Listen -ErrorAction SilentlyContinue
foreach ($listener in $listeners) {
  Stop-ProcessTree -ProcessId ([int]$listener.OwningProcess)
}

Start-Sleep -Seconds 1
Remove-Item -LiteralPath $stateFile -Force -ErrorAction SilentlyContinue

$remaining = Get-NetTCPConnection -LocalPort 8080,5173 -State Listen -ErrorAction SilentlyContinue
if ($remaining) {
  Write-Host "Some service ports are still in use:" -ForegroundColor Red
  $remaining | Select-Object LocalPort,OwningProcess | Format-Table
  exit 1
}

Write-Host "Scientific Research Collaboration Platform services have stopped." -ForegroundColor Green
