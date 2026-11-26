# Smoke HTTP contra API local (requiere run-api.ps1 en otra terminal).
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"

Write-Host "== 1) Login =="
$login = Invoke-RestMethod -Method POST -Uri "$base/auth/login" -ContentType "application/json" -Body '{"username":"demo"}'
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }
Write-Host "OK token length=$($token.Length)"

Write-Host "== 2) POST /lists (201) =="
$body = @{
    nombre = "Lista Smoke"
    descripcion = "Prueba smoke"
    canciones = @(
        @{ titulo = "Bohemian Rhapsody"; artista = "Queen"; album = "A Night at the Opera"; anno = "1975"; genero = "rock" }
    )
} | ConvertTo-Json -Depth 5
$created = Invoke-WebRequest -Method POST -Uri "$base/lists" -Headers $headers -ContentType "application/json" -Body $body
Write-Host "Status=$($created.StatusCode) Location=$($created.Headers.Location)"

Write-Host "== 3) GET /lists =="
Invoke-RestMethod -Method GET -Uri "$base/lists" -Headers $headers | ConvertTo-Json -Depth 5

Write-Host "== 4) GET /lists/Lista Smoke =="
Invoke-RestMethod -Method GET -Uri "$base/lists/Lista%20Smoke" -Headers $headers | ConvertTo-Json -Depth 5

Write-Host "== 5) GET recommendations (IA o FALLBACK) =="
$rec = Invoke-RestMethod -Method GET -Uri "$base/lists/Lista%20Smoke/recommendations" -Headers $headers
$rec | ConvertTo-Json -Depth 5
Write-Host "source=$($rec.source)"

Write-Host "== 6) Sin JWT debe fallar (401/403) =="
try {
    Invoke-WebRequest -Method GET -Uri "$base/lists" -ErrorAction Stop | Out-Null
    throw "Se esperaba 401/403"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -ne 401 -and $code -ne 403) { throw "Status inesperado: $code" }
    Write-Host "OK status=$code (protegido)"
}

Write-Host "== 7) Lista inexistente 404 =="
try {
    Invoke-WebRequest -Method GET -Uri "$base/lists/no-existe" -Headers $headers -ErrorAction Stop | Out-Null
    throw "Se esperaba 404"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "OK status=$code"
}

Write-Host "== 8) DELETE 204 =="
$del = Invoke-WebRequest -Method DELETE -Uri "$base/lists/Lista%20Smoke" -Headers $headers
Write-Host "Status=$($del.StatusCode)"

Write-Host "SMOKE OK"
