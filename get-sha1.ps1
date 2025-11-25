# Script para obtener SHA-1 Fingerprint de tu app Android TrailynSafe
# Ejecuta este script en PowerShell desde la carpeta TrailynApp

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  OBTENIENDO SHA-1 FINGERPRINT" -ForegroundColor Cyan
Write-Host "  TrailynSafe - Google Sign-In Setup" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que estamos en la carpeta correcta
$currentPath = Get-Location
Write-Host "Ubicacion actual: $currentPath" -ForegroundColor Yellow

if (-Not (Test-Path ".\gradlew.bat")) {
    Write-Host ""
    Write-Host "ERROR: No se encontro gradlew.bat" -ForegroundColor Red
    Write-Host "Asegurate de ejecutar este script desde la carpeta TrailynApp" -ForegroundColor Red
    Write-Host ""
    Write-Host "Ejemplo:" -ForegroundColor Yellow
    Write-Host '  cd "C:\Users\jjzg_\Desktop\WEB APPS\TrailynSafe_WEB\TrailynApp"' -ForegroundColor Yellow
    Write-Host "  .\get-sha1.ps1" -ForegroundColor Yellow
    Write-Host ""
    pause
    exit
}

Write-Host ""
Write-Host "✓ Archivo gradlew encontrado" -ForegroundColor Green
Write-Host ""
Write-Host "Ejecutando gradlew signingReport..." -ForegroundColor Yellow
Write-Host "Esto puede tardar 1-2 minutos..." -ForegroundColor Yellow
Write-Host ""

# Ejecutar gradlew signingReport y capturar la salida
$output = & .\gradlew.bat signingReport 2>&1 | Out-String

# Buscar el SHA-1 en la salida
$sha1Pattern = "SHA1:\s*([A-F0-9:]+)"
$sha256Pattern = "SHA-256:\s*([A-F0-9:]+)"

$sha1Matches = [regex]::Matches($output, $sha1Pattern)
$sha256Matches = [regex]::Matches($output, $sha256Pattern)

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  RESULTADOS" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

if ($sha1Matches.Count -gt 0) {
    Write-Host "✓ SHA-1 Fingerprints encontrados:" -ForegroundColor Green
    Write-Host ""
    
    $debugSHA1 = ""
    $releaseSHA1 = ""
    
    foreach ($match in $sha1Matches) {
        $sha1Value = $match.Groups[1].Value
        
        # Intentar determinar si es debug o release basado en el contexto
        $contextStart = [Math]::Max(0, $match.Index - 200)
        $contextLength = [Math]::Min(400, $output.Length - $contextStart)
        $context = $output.Substring($contextStart, $contextLength)
        
        if ($context -match "Variant:\s*debug") {
            $debugSHA1 = $sha1Value
            Write-Host "DEBUG SHA-1:" -ForegroundColor Yellow
            Write-Host "  $sha1Value" -ForegroundColor White
            Write-Host ""
        }
        elseif ($context -match "Variant:\s*release") {
            $releaseSHA1 = $sha1Value
            Write-Host "RELEASE SHA-1:" -ForegroundColor Magenta
            Write-Host "  $sha1Value" -ForegroundColor White
            Write-Host ""
        }
        else {
            Write-Host "SHA-1 (tipo desconocido):" -ForegroundColor Gray
            Write-Host "  $sha1Value" -ForegroundColor White
            Write-Host ""
        }
    }
    
    Write-Host "=============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "SIGUIENTE PASO:" -ForegroundColor Green
    Write-Host ""
    Write-Host "1. Ve a Google Cloud Console:" -ForegroundColor Yellow
    Write-Host "   https://console.cloud.google.com/" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "2. Ve a: APIs & Services > Credentials" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "3. Clic en: + CREATE CREDENTIALS > OAuth client ID" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "4. Selecciona: Android" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "5. Llena:" -ForegroundColor Yellow
    Write-Host "   Name: TrailynSafe Android (Debug)" -ForegroundColor White
    Write-Host "   Package name: com.example.trailynapp" -ForegroundColor White
    
    if ($debugSHA1 -ne "") {
        Write-Host "   SHA-1: $debugSHA1" -ForegroundColor White
        # Copiar al clipboard si es posible
        try {
            Set-Clipboard -Value $debugSHA1
            Write-Host ""
            Write-Host "✓ DEBUG SHA-1 copiado al portapapeles!" -ForegroundColor Green
        } catch {
            # Si falla, no pasa nada
        }
    }
    
    Write-Host ""
    Write-Host "6. Despues, crea otra credencial Web para el backend" -ForegroundColor Yellow
    Write-Host ""
    
    # Guardar en archivo
    $timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
    $outputFile = "sha1-fingerprints-$timestamp.txt"
    
    $fileContent = @"
TrailynSafe - SHA-1 Fingerprints
Generado: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

DEBUG SHA-1:
$debugSHA1

RELEASE SHA-1:
$releaseSHA1

Package Name:
com.example.trailynapp

INSTRUCCIONES:
1. Ve a https://console.cloud.google.com/
2. APIs & Services > Credentials
3. CREATE CREDENTIALS > OAuth client ID
4. Tipo: Android
5. Name: TrailynSafe Android (Debug)
6. Package: com.example.trailynapp
7. SHA-1: (usa el DEBUG SHA-1 de arriba)

Para el backend, crea otra credencial:
1. CREATE CREDENTIALS > OAuth client ID
2. Tipo: Web application
3. Name: TrailynSafe Backend
4. Authorized JavaScript origins:
   - https://web-production-86356.up.railway.app
   - http://localhost:8000
5. Authorized redirect URIs:
   - https://web-production-86356.up.railway.app/auth/callback
   - http://localhost:8000/auth/callback

COPIA el Web Client ID y Client Secret generados.
"@
    
    $fileContent | Out-File -FilePath $outputFile -Encoding UTF8
    Write-Host "✓ Fingerprints guardados en: $outputFile" -ForegroundColor Green
    Write-Host ""
    
} else {
    Write-Host "✗ No se encontraron SHA-1 fingerprints" -ForegroundColor Red
    Write-Host ""
    Write-Host "La salida completa del comando fue:" -ForegroundColor Yellow
    Write-Host $output
    Write-Host ""
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Presiona cualquier tecla para cerrar..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
