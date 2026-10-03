$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"
$runtime = Join-Path $root ".runtime"

New-Item -ItemType Directory -Path $runtime -Force | Out-Null

$maven = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
$npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source

if (-not $maven) {
  throw "Maven was not found. Please add mvn.cmd to PATH."
}
if (-not $npm) {
  throw "Node.js/npm was not found. Please add npm.cmd to PATH."
}

$backendRepo = Join-Path $backend ".m2repository"
$backendArgs = @(
  "-q",
  "-Dmaven.repo.local=$backendRepo",
  "spring-boot:run"
)
$backendProcess = Start-Process -FilePath $maven -ArgumentList $backendArgs `
  -WorkingDirectory $backend `
  -RedirectStandardOutput (Join-Path $runtime "backend.log") `
  -RedirectStandardError (Join-Path $runtime "backend.err.log") `
  -WindowStyle Hidden -PassThru

$frontendProcess = Start-Process -FilePath $npm -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1") `
  -WorkingDirectory $frontend `
  -RedirectStandardOutput (Join-Path $runtime "frontend.log") `
  -RedirectStandardError (Join-Path $runtime "frontend.err.log") `
  -WindowStyle Hidden -PassThru

@{
  backend = $backendProcess.Id
  frontend = $frontendProcess.Id
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtime "processes.json") -Encoding UTF8

Write-Host ""
Write-Host "Scientific Research Collaboration Platform is starting" -ForegroundColor Cyan
Write-Host "Backend: http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host ""
Write-Host "Demo account: zhangwei / 123456"
Write-Host "Runtime logs: $runtime"
Write-Host "Stop services: .\stop-all.ps1"

Write-Host ""
Write-Host "Waiting for services..." -ForegroundColor Yellow
$deadline = (Get-Date).AddSeconds(90)
do {
  Start-Sleep -Seconds 2
  $backendReady = [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
  $frontendReady = [bool](Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue)
  if ($backendReady -and $frontendReady) {
    Write-Host "Services are ready. Open http://localhost:5173" -ForegroundColor Green
    exit 0
  }
} while ((Get-Date) -lt $deadline)

Write-Host "Service startup timed out. Check these logs:" -ForegroundColor Red
Write-Host (Join-Path $runtime "backend.err.log")
Write-Host (Join-Path $runtime "frontend.err.log")
if (Test-Path (Join-Path $runtime "backend.err.log")) {
  Get-Content -LiteralPath (Join-Path $runtime "backend.err.log") -Tail 30
}
if (Test-Path (Join-Path $runtime "frontend.err.log")) {
  Get-Content -LiteralPath (Join-Path $runtime "frontend.err.log") -Tail 30
}
exit 1
