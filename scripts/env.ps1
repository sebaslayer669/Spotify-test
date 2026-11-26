# Configura JDK 17 + Maven local del proyecto (sin instalar global).
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$Jdk = "C:\Program Files\Microsoft\jdk-17.0.21.102-hotspot"
$Maven = Join-Path $ProjectRoot ".tools\apache-maven-3.10.2"

if (-not (Test-Path "$Jdk\bin\java.exe")) {
    throw "JDK 17 no encontrado en $Jdk. Instala Microsoft OpenJDK 17."
}
if (-not (Test-Path "$Maven\bin\mvn.cmd")) {
    throw "Maven no encontrado en $Maven."
}

$env:JAVA_HOME = $Jdk
$env:MAVEN_HOME = $Maven
$env:Path = "$Jdk\bin;$Maven\bin;" + (($env:Path -split ';' | Where-Object { $_ -and $_ -notmatch 'Oracle\\Java\\javapath' } | Select-Object -Unique) -join ';')

# Defaults seguros para prueba local (sin secretos reales)
if (-not $env:JWT_SECRET) { $env:JWT_SECRET = "local-dev-jwt-secret-change-me-32chars" }
if (-not $env:AI_ENABLED) { $env:AI_ENABLED = "false" }  # true + AI_API_KEY para LLM real

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
Write-Host "AI_ENABLED=$env:AI_ENABLED"
java -version
mvn -version
