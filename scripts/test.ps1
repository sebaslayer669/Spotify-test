$ErrorActionPreference = "Stop"
. "$PSScriptRoot\env.ps1"
Set-Location (Join-Path (Split-Path $PSScriptRoot -Parent) "api-lists")
mvn test
