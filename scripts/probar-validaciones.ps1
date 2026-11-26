param(
    [string]$Jdk = "$env:LOCALAPPDATA\Programs\EclipseAdoptium\jdk-17.0.21+9",
    [string]$JavaEeApi = 'C:\Program Files\NetBeans-25\netbeans\enterprise\modules\ext\javaee-api-8.0.jar',
    [string]$ServletApi = "$env:USERPROFILE\payara5-nutricion\glassfish\modules\jakarta.servlet-api.jar"
)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$salida = Join-Path $raizProyecto '.cache\pruebas-validaciones'
New-Item -ItemType Directory -Force -Path $salida | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath (Join-Path $raizProyecto 'sistema_nutricion-ejb\src\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
$fuentes += Join-Path $raizProyecto 'tests\java\com\nutricion\servicios\PacienteFacadeTest.java'
$fuentes += Join-Path $raizProyecto 'tests\java\com\nutricion\servicios\PlanTratamientoFacadeTest.java'
$raizWar = Join-Path $raizProyecto 'sistema_nutricion-war\src\java\com\nutricion\web'
$fuentes += Join-Path $raizWar 'util\Mensajes.java'
$fuentes += Join-Path $raizWar 'beans\LoginBean.java'
$fuentes += Join-Path $raizWar 'filtros\FiltroAcceso.java'
$fuentes += Join-Path $raizProyecto 'tests\java\com\nutricion\web\filtros\FiltroAccesoTest.java'
& (Join-Path $Jdk 'bin\javac.exe') -encoding UTF-8 -cp $JavaEeApi -d $salida @fuentes
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
& (Join-Path $Jdk 'bin\java.exe') -cp "$salida;$JavaEeApi" com.nutricion.servicios.PacienteFacadeTest
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de validación.' }
& (Join-Path $Jdk 'bin\java.exe') -cp "$salida;$JavaEeApi" com.nutricion.servicios.PlanTratamientoFacadeTest
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de macronutrientes.' }
if (!(Test-Path -LiteralPath $ServletApi)) { throw 'Indica con -ServletApi el JAR de Servlet 4 de Payara.' }
& (Join-Path $Jdk 'bin\java.exe') -cp "$salida;$ServletApi;$JavaEeApi" com.nutricion.web.filtros.FiltroAccesoTest
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de actividad de sesion.' }
