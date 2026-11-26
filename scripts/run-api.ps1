$ErrorActionPreference = "Stop"
. "$PSScriptRoot\env.ps1"
Set-Location (Join-Path (Split-Path $PSScriptRoot -Parent) "api-lists")
Write-Host "Arrancando API en http://localhost:8080 ..."
Write-Host "Login: POST /auth/login  |  Lists: /lists  |  IA: /lists/{name}/recommendations"
mvn spring-boot:run
