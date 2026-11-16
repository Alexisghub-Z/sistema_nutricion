# Continuidad de sistema_nutricion / Unidad 4

**Leer este archivo al iniciar otro chat.** Revisión realizada el 15 de noviembre de 2026, a petición de revisar la explicación del profesor, todo `ARCHIVOS`, instalaciones necesarias, modelos e implementación.

El 16-11-2026 se agregó `AGENTS.md` en la raíz para indicar a futuros chats de este proyecto que lean esta guía y el README, y consulten los materiales pertinentes sin pedir al usuario que repita sus rutas. Abrir la conversación en esta misma carpeta; esto conserva instrucciones y contexto en archivos, no una garantía de recordar íntegramente conversaciones anteriores.

## Rutas y orden de lectura

Raíz del proyecto:

```text
C:\Users\yaxti_cm5rg3t\Documents\MI LAP 2\ESCUELA\NOVENO SEMESTRE\ENFOQUE AGIL\UNIDAD 4\sistema_nutricion
```

Material original del profesor:

```text
C:\Users\yaxti_cm5rg3t\Documents\MI LAP 2\ESCUELA\NOVENO SEMESTRE\ENFOQUE AGIL\UNIDAD 4\ARCHIVOS
```

1. `README.md`: contrato de stack, especificación objetivo y 44 HU. Mantener Java EE 8 `javax.*`, Ant/NetBeans, JDK 17, Payara 5.2022.3, JSF 2.3 y PrimeFaces 5.0. No migrar tecnologías para resolver tareas de clase.
2. `docs/MATERIALES_PROFESOR.md`: índice de todos los archivos del profesor, páginas concretas, herramientas mencionadas y qué ya está instalado.
3. `docs/UNIDAD_4_REVISION_Y_ENTREGA.md`: modelo actual de cuatro tablas, diccionario, comportamiento, estados, navegación, secuencias, validaciones, matriz de las 44 HU y guion demostrable.
4. `docs/GUIA_CUESTIONARIO.md`: 35 preguntas con respuestas y un ejercicio para el lunes.
5. `docs/consultas-demostracion.sql`: consultas de solo lectura de estructura, FK e integridad, y búsqueda de datos ficticios.

## Qué pidió el profesor y qué permanece incierto

Mostrar para el martes modelo de datos implementado con BD, plantillas/Facelets, comportamiento, secuencias y funcionamiento; al final relacionarlo con historias, modelos y validadores. La revisión del programa se basará en la documentación. El lunes habrá cuestionario de esos fundamentos.

Las fechas 16/17-11-2026 son inferidas como lunes/martes siguientes, no una fecha expresamente dicha por el profesor. «Fanpages» se interpreta provisionalmente como **Facelets**: el estudiante respondió que no se le escuchaba bien y supone que era eso. No hay autorización o evidencia para crear una página de Facebook. No hay rúbrica separada en la carpeta revisada; confirmar formato final y alcance si el profesor lo precisa.

## Material disponible y búsqueda rápida

Son **6 archivos**: Head First Web Design (553 p.), configuración del servidor (3 p.), manual PrimeFaces 5.0 (573 p.), selección Web Engineering (72 p.), `itext-2.1.7.jar` y `poi-3.16.jar`.

La revisión inventarió todos y consultó las secciones pertinentes. No fue una inspección visual exhaustiva de las 1201 páginas ni una auditoría integral de dependencias. En particular:

- Ingeniería es un PDF de imágenes. Se hizo OCR **local** de sus 72 páginas con Windows y se guardó cada página PNG. El idioma instalado es `es-MX`; revisar visualmente diagramas/citas porque el texto del libro es inglés.
- PrimeFaces tiene extracción inicial con letras separadas: para búsquedas normalizar espacios o extraer `extraction_mode="layout"`. Ya hay extractos legibles de páginas importantes.
- Caché persistente local: `.cache/materiales-profesor/` (ignorada por Git). `inventario.json` contiene nombres, tamaños y SHA-256; `*.json` contiene texto por página; `web-engineering-ocr/ocr.json` y `pagina-NNN.png` contienen OCR e imágenes.
- Buscar con `python scripts/buscar-materiales.py 'sequence'` o `'DataExporter'`; no volver a extraer todos los libros para cada consulta.
- Web Engineering PDF p. 15: casos de uso/actividades; 16-17: clases y estados; 18-21: navegación; 22-23: presentación; 24-25: secuencias. Es selección de páginas impresas discontinuas.
- PrimeFaces p. 12: dependencias opcionales; 128-130: exportación; 365-366: resetInput. Las referencias son páginas PDF desde 1.

**No se encontró instalación adicional obligatoria para este avance.** MySQL, Connector/J, NetBeans/Ant, JDK 17, Payara y PrimeFaces ya estaban. iText/POI son JAR opcionales para PDF/XLS, no programas instalables con doble clic. Se conservaron en ARCHIVOS sin incorporarlos. Las herramientas de modelado/diseño citadas por los libros son ejemplos, no órdenes de instalación. POI 3.16 está fuera del soporte actual de Apache; no introducirlo por mera presencia en la carpeta.

## Estado real del proyecto

Base al comenzar la revisión: `main`, commit `cb632b1`, árbol limpio. El 16-11-2026 el usuario autorizó subir el trabajo a Git. Se comprobó mediante fetch que `origin/main` seguía en esa base y se preparó la publicación de correcciones, pruebas, scripts y documentos con la identidad Git configurada. Consultar `git log -1`, `git status` y el SHA remoto para verificar el resultado actual de la publicación.

Se excluyó de la publicación el ajuste local de `nbproject/project.properties` que deja `j2ee.platform.classpath` vacío. Se conservó en el árbol de trabajo sin sobrescribirlo; no forma parte de las correcciones de esta revisión. Los PDF originales, la caché/OCR, las capturas, las configuraciones privadas de NetBeans y las credenciales tampoco se publican.

Hay cuatro entidades/tablas: Usuario/usuarios, Paciente/pacientes, Cita/citas y Consulta/consultas. Se verificaron sus columnas, FK y UNIQUE en **MySQL 8.0.41**, con el pool `NutricionMySQLPool`, recurso `jdbc/nutricion` y unidad `NutricionPU`. Se leyeron solo metadatos, sin imprimir credenciales.

El recorrido demostrado cubre sitio público, acceso, panel y CRUD **básico** de pacientes. El documento de entrega distingue cada HU parcial/pendiente. Faltan, entre otros, Consultorio y aislamiento, roles NUTRIOLOGO/RECEPCION, antecedentes completos, catálogo, agenda/consultas con interfaces, gráficas, adjuntos, portal e integraciones. No declarar que el sistema completo o HU-06 está terminado.

### Cambios implementados

- `PacienteFacade.guardar/validar`: nulos, nombre recortado, correo básico/longitud, normalización con Locale.ROOT, teléfono sin letras/10-15 dígitos, nacimiento requerido/no futuro y longitudes de textos; conservar las comprobaciones de duplicados.
- `vistas/pacientes/lista.xhtml`: validadores JSF y longitudes; `p:resetInput` en Nuevo/Editar para descartar valores inválidos cancelados. El fallo se reprodujo antes de corregir.
- `plantillas/plantillamenu.xhtml`: formulario alrededor de los botones de confirmación para quitar avisos JSF de componentes sin formulario.
- `src/conf/application.xml`: declaración explícita EAR/EJB/WAR y contexto web para el despliegue expandido.
- Scripts, pruebas y documentación enlazados desde el README. `.cache` y `.playwright-cli` excluidos de Git.

### Verificación obtenida

- Ant con JDK 17: **BUILD SUCCESSFUL**. Persisten avisos de source 8 sin bootstrap classpath; no se cambió el contrato de compilación.
- Java sin servidor/BD, EntityManager simulado: **22/22 pruebas** de validación y persist/merge. Comando `scripts/probar-validaciones.ps1`.
- Pool real: Ping correcto; cuatro tablas, FK y UNIQUE inspeccionadas por JDBC con `scripts/InspeccionarBase.java` (solo lectura).
- Edge/Playwright contra Payara y MySQL reales: **17 comprobaciones** de autenticación, campos requeridos, formatos, reglas, duplicados, reinicio de diálogo, CRUD, persistencia con recarga/nueva sesión y logout. Archivo `tests/recorrido-pacientes.js`.
- Se usó un paciente ficticio propio por ejecución con correo `u4.qa.*@example.invalid` y se eliminó al finalizar. No se modificaron las tres muestras existentes. La prueba automatizada escribe en la BD de desarrollo; no correrla en producción.
- Capturas locales en `.cache/evidencias-u4/`. No son evidencia de agenda, consulta clínica completa, roles, IA, producción, concurrencia o aceptación externa.
- No hay medición de cobertura global ni CI verificada. La sesión de 30 min está configurada; no se probó expiración mediante una espera real.

## Entorno y ejecución actual

```text
NetBeans: C:\Program Files\NetBeans-25\netbeans
JDK 17: C:\Users\yaxti_cm5rg3t\AppData\Local\Programs\EclipseAdoptium\jdk-17.0.21+9
Payara: C:\Users\yaxti_cm5rg3t\payara5-nutricion
Dominio: glassfish/domains/domain1
MySQL: servicio MySQL80, puerto 3306
Connector/J: glassfish/domains/domain1/lib/mysql-connector-j-8.3.0.jar
Aplicación: http://localhost:8080/sistema_nutricion-war/
Consola: http://localhost:4848/
```

`java` del PATH es JDK 23. Para este proyecto elegir JDK 17 explícitamente. La ruta abreviada de Payara apunta a la instalación existente; no borrar dominio ni recursos. Al cerrar esta revisión se dejó el servidor local activo con la aplicación desplegada; comprobar puertos/procesos en el próximo chat, sin asumir que siguen activos.

### Incidencia de despliegue y solución comprobada

`asadmin deploy dist/sistema_nutricion.ear` informó éxito, pero produjo HTTP 404 y ClassNotFoundException de clases web que sí estaban en el archivo. Repetir despliegue, reiniciar y agregar solamente application.xml no resolvieron el EAR empaquetado en este dominio. Desplegar un directorio con **ambos módulos EJB y WAR expandidos**, más el descriptor explícito, sí funcionó.

`scripts/desplegar-local.ps1` compila y prepara esa copia en `.cache/despliegues/<fecha>/`, despliega **solo** `sistema_nutricion`, y verifica la página pública HTTP 200. La ruta activa al completar las correcciones del formulario fue `.cache/despliegues/20261115-221932-138`; consultar la configuración local de Payara antes de cualquier limpieza o redeploy. Una ejecución posterior cambia esa ruta.

No borrar la copia activa. El script conserva copias anteriores para evitar eliminar una que el servidor tenga abierta. Si se decide limpiarlas, comprobar primero ubicación activa y bloqueos. Los intentos diagnósticos anteriores en `.cache/despliegue/` y `.cache/despliegue-u4-explicito/` no son fuentes del proyecto.

El primer `clean dist` encontró un JAR bloqueado en `dist/gfdeploy` del despliegue anterior: se retiró solo esta aplicación y se compiló de nuevo. No se desinstaló ni borró Payara. El nivel de log de classloading se aumentó temporalmente y se devolvió a INFO. El OCR de ingeniería terminó sus 72 páginas, pero PowerShell terminó con error al liberar WinRT; la caché quedó completa. No usar su código de salida como prueba de que falten páginas. El renderizado experimental de PrimeFaces se detuvo al quedarse esperando; la extracción textual sí se obtuvo.

El auxiliar experimental de OCR se conservó únicamente en `.cache/herramientas/leer-pdf-ocr.ps1`, fuera de los scripts entregados/versionables, por ese fallo de liberación de WinRT. Para continuar leyendo se dispone de las 72 imágenes y el OCR completo; no es necesario ejecutarlo de nuevo.

### Comandos útiles

```powershell
git status --short
git log -1 --oneline
Get-NetTCPConnection -State Listen -LocalPort 8080,4848,3306 -ErrorAction SilentlyContinue
.\scripts\probar-validaciones.ps1
.\scripts\desplegar-local.ps1
python scripts\buscar-materiales.py 'Sequence'
```

Para inspeccionar la base, compilar `scripts/InspeccionarBase.java` **en `.cache/revision-base`**, añadir Connector/J al classpath y pasar el `domain.xml` local como argumento. No mostrar el XML completo: contiene secretos. No poner resultados de análisis dentro de `build/`, porque el empaquetado raíz incluye esa carpeta en el EAR.

## Próximo paso académico

Ensayar el guion y el cuestionario, comprobar qué formato de entrega pide el profesor y mantener trazabilidad real al ampliar funcionalidades. Antes de implementar módulos adicionales, retomar la matriz y el README; no confundir el avance básico demostrado con la totalidad del SaaS objetivo. Para completar el fundamento faltan primero consultorio/aislamiento y roles, seguidos por antecedentes, catálogos, consultas y agenda.

## Prompt listo para otro chat

> Estamos en sistema_nutricion de Unidad 4. Lee primero CONTINUIDAD_UNIDAD_4.md, README.md y los documentos enlazados. Ya se revisaron los seis archivos de ../ARCHIVOS; usa el índice y la caché local antes de repetir búsquedas. Conserva Java EE 8 javax.*, Ant/NetBeans, JDK 17, Payara 5 y PrimeFaces 5. Se demostraron cuatro tablas MySQL, login, panel y CRUD básico de pacientes con 22 pruebas Java y 17 comprobaciones de navegador. El resto de las 44 HU tiene pendientes documentados. Revalida HEAD, cambios locales y servidor; no publiques ni reinstales herramientas por suposición. Continúa desde el alcance concreto que te indique, manteniendo modelos, historia, implementación y evidencia alineados.
