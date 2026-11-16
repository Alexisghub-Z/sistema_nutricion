param(
    [string]$Jdk = "$env:LOCALAPPDATA\Programs\EclipseAdoptium\jdk-17.0.21+9",
    [string]$JavaEeApi = 'C:\Program Files\NetBeans-25\netbeans\enterprise\modules\ext\javaee-api-8.0.jar'
)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$salida = Join-Path $raizProyecto '.cache\pruebas-validaciones'
New-Item -ItemType Directory -Force -Path $salida | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath (Join-Path $raizProyecto 'sistema_nutricion-ejb\src\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
$fuentes += Join-Path $raizProyecto 'tests\java\com\nutricion\servicios\PacienteFacadeTest.java'
& (Join-Path $Jdk 'bin\javac.exe') -encoding UTF-8 -cp $JavaEeApi -d $salida @fuentes
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
& (Join-Path $Jdk 'bin\java.exe') -cp "$salida;$JavaEeApi" com.nutricion.servicios.PacienteFacadeTest
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de validación.' }
