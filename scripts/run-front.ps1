$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$front = Join-Path $root "frontend"

Set-Location $front

if (-not (Test-Path (Join-Path $front "node_modules"))) {
  Write-Host "Instalando dependencias del front..."
  npm install
}

Write-Host "Front en http://localhost:5173 (proxy -> API :8080)"
Write-Host "Asegurate de tener la API arriba: .\scripts\run-api.ps1"
npm run dev
