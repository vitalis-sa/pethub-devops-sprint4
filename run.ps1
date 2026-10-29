# Carrega .env e sobe o pethub-java.
#
#   cp .env.example .env      (uma vez só; preencha os valores)
#   .\run.ps1
#
# O Spring Boot não le arquivos .env sozinho — este script exporta cada
# linha do .env como variavel de ambiente do processo antes de chamar o
# Maven.
#
# Swagger: http://localhost:8080/swagger-ui.html

$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Host "Arquivo .env nao encontrado em $envFile" -ForegroundColor Red
    Write-Host "Copie o modelo primeiro:  cp .env.example .env" -ForegroundColor Yellow
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $linha = $_.Trim()
    # Ignora linha em branco e comentario (#).
    if ($linha -eq "" -or $linha.StartsWith("#")) { return }

    $partes = $linha.Split("=", 2)
    if ($partes.Length -ne 2) { return }

    $chave = $partes[0].Trim()
    $valor = $partes[1].Trim()
    if ($valor -eq "") { return }   # chave presente mas sem valor: nao sobrescreve

    [System.Environment]::SetEnvironmentVariable($chave, $valor, "Process")
}

foreach ($obrigatoria in @("DB_USER", "DB_PASSWORD", "JWT_SECRET")) {
    if (-not [System.Environment]::GetEnvironmentVariable($obrigatoria, "Process")) {
        Write-Host "Faltando no .env: $obrigatoria" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Variaveis carregadas do .env. Subindo a aplicacao..." -ForegroundColor Cyan
Set-Location $PSScriptRoot
& .\mvnw.cmd spring-boot:run
