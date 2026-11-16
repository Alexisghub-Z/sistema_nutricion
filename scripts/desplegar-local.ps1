param(
    [string]$Jdk = "$env:LOCALAPPDATA\Programs\EclipseAdoptium\jdk-17.0.21+9",
    [string]$NetBeans = 'C:\Program Files\NetBeans-25\netbeans',
    [string]$Payara = "$env:USERPROFILE\payara5-nutricion"
)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$javaAnterior = $env:JAVA_HOME
$asJavaAnterior = $env:AS_JAVA
Add-Type -AssemblyName System.IO.Compression.FileSystem
Push-Location $raizProyecto
try {
    $env:JAVA_HOME = $Jdk
    $env:AS_JAVA = $Jdk
    foreach ($necesario in @("$Jdk\bin\java.exe", "$NetBeans\extide\ant\bin\ant.bat", "$Payara\bin\asadmin.bat")) {
        if (!(Test-Path -LiteralPath $necesario)) { throw "No existe: $necesario" }
    }
    # El despliegue debe apuntar a una copia independiente para no bloquear Clean and Build.
    & "$NetBeans\extide\ant\bin\ant.bat" -quiet "-Dj2ee.platform.classpath=$NetBeans\enterprise\modules\ext\javaee-api-8.0.jar" "-Dlibs.CopyLibs.classpath=$NetBeans\java\ant\extra\org-netbeans-modules-java-j2seproject-copylibstask.jar" clean dist
    if ($LASTEXITCODE -ne 0) { throw 'No se despliega: fallo la compilacion.' }
    $destino = Join-Path $raizProyecto ('.cache\despliegues\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    [System.IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $raizProyecto 'dist\sistema_nutricion.ear'), $destino)
    foreach ($modulo in @('sistema_nutricion-ejb.jar', 'sistema_nutricion-war.war')) {
        $rutaModulo = Join-Path $destino $modulo
        $archivoModulo = "$rutaModulo.zip"
        Move-Item -LiteralPath $rutaModulo -Destination $archivoModulo
        [System.IO.Compression.ZipFile]::ExtractToDirectory($archivoModulo, $rutaModulo)
        Remove-Item -LiteralPath $archivoModulo
    }
    & "$Payara\bin\asadmin.bat" deploy --force=true --name sistema_nutricion $destino
    if ($LASTEXITCODE -ne 0) { throw 'No se completo el despliegue. Revisar server.log.' }
    $respuesta = Invoke-WebRequest -Uri 'http://localhost:8080/sistema_nutricion-war/index.xhtml' -UseBasicParsing
    if ($respuesta.StatusCode -ne 200 -or $respuesta.Content -notmatch 'Come mejor') {
        throw 'El comando termino, pero la pagina publica no respondio como se esperaba.'
    }
    Write-Output 'Pagina publica verificada: http://localhost:8080/sistema_nutricion-war/'
    Write-Output "Copia desplegada (conservar mientras este activa): $destino"
    Write-Output 'El login y el CRUD requieren ademas la prueba de navegador documentada.'
} finally {
    $env:JAVA_HOME = $javaAnterior
    $env:AS_JAVA = $asJavaAnterior
    Pop-Location
}
