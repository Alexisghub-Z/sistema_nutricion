# Continuidad de sistema_nutricion / Unidad 4

## Continuación del 26 de noviembre de 2026

Se reprodujeron cuatro problemas de actualización en el despliegue del 25 de noviembre. Si se guardaba una ficha de paciente abierta antes de escribir un antecedente, el antecedente podía desaparecer. La agenda retenía el nombre y teléfono anteriores en el selector. El panel y el portal abiertos podían seguir mostrando una cita después de completarla. Al cerrar una consulta, la cabecera del expediente conservaba el peso anterior. Evidencia en `.cache/revision-continuacion/agenda-5670239286.txt`.

- La ficha general, los antecedentes y las alergias escriben sus propios campos. La edición del paciente conserva los datos clínicos recién guardados y el expediente actualiza la cabecera al cerrar la consulta. **Actualizar datos** vuelve a leer la ficha sin volver a renderizar los formularios clínicos.
- La agenda relee el selector al abrir una cita. Al seleccionar un evento, el calendario y la tabla muestran la misma fecha. La agenda puede actualizarse conservando el rango visible y la edición de duración se hace en el diálogo de cita.
- El panel distingue citas por atender hoy y completadas. La agenda, el panel y el portal se actualizan con su botón o al volver a la vista. En Edge se comprobó el manejador de foco y se probó que un Ajax con la sesión revocada devuelve al acceso.

### Google Calendar (HU-16)

Se añadió la vinculación por consultorio, el cliente de Calendar y una tarea EJB cada cinco minutos. El ciclo cubre los próximos 90 días del calendario principal. Los bloqueos se guardan en `bloqueos_calendario` y afectan tanto el calendario de la agenda como la confirmación de una reserva. La aplicación publica citas con el título **Consulta NutriVida**. El token se guarda cifrado en `vinculos_google_calendar`.

Se comprobó con un proveedor HTTP ficticio la autorización, importación paginada, creación, reprogramación y cancelación de citas, conservación de bloqueos ante una caída y recuperación de un evento borrado. En Edge, un bloqueo de 09:00 a 10:00 dejó libre el turno siguiente, rechazó una reserva que ya se había seleccionado y afectó solo al consultorio indicado. A las 08:45 Payara registró una ejecución automática correcta del temporizador local. Evidencia en `.cache/revision-continuacion/calendario-timer.txt`. La validación de OAuth y de la sincronización con una cuenta real queda pendiente. La [sección de Calendar](README.md#vincular-google-calendar) indica las propiedades del dominio y cómo probar el flujo.

### Estado entregado

El EAR activo está en `.cache/despliegues/20261126-083353-223`. Ant reportó `BUILD SUCCESSFUL`, la portada respondió HTTP 200 y `NutricionMySQLPool` pasó el ping. La inspección de MySQL 8.0.41 confirmó 13 tablas y `historia_dietetica` como `LONGTEXT`. El trabajo sigue local sobre `main` en `53ad585342762ca74131e5ee723d30e6a5e94c17`.

| Verificación | Comprobaciones | Evidencia en `.cache/revision-continuacion/` |
|---|---:|---|
| Java y validaciones de pacientes | 22 | `scripts/probar-validaciones.ps1` |
| Calendar con proveedor ficticio | 15 | `calendario-protocolo-final.txt` |
| Cambios de paciente, agenda, panel y portal | 14 | `agenda-5703812745.txt` |
| Bloqueos de Calendar en Edge y MySQL | 7 | `calendar-final-5703711699.txt` |
| Agenda, expediente, portal y recepción | 38 | `agenda-5703717729.txt` |
| Adjuntos | 24 | `agenda-5703813769.txt` |
| Contactos entre consultorios | 10 | `agenda-5703813123.txt` |
| CRUD de pacientes | 17 | `crud-26-noviembre.txt` |

Se limpiaron los registros ficticios de cada ejecución. La aplicación queda disponible en [NutriVida local](http://localhost:8080/sistema_nutricion-war/). Quedan por verificar la cuenta real de Calendar, la expiración por inactividad y la impresión del portal. El siguiente módulo de agenda pendiente es recordatorios (HU-18).

### Revisión para publicación

Se recompiló y desplegó el EAR sobre la base `53ad585` en `.cache/despliegues/20261126-085842-885`. Payara 5.2022.3 respondió HTTP 200 y pasó el ping de `NutricionMySQLPool`. Volvieron a pasar las 22 pruebas Java de pacientes, las 15 de Calendar con proveedor ficticio y las 110 comprobaciones en Edge. Las pruebas crearon y limpiaron sus consultorios y pacientes ficticios. La evidencia de esta repetición está en `.cache/revision-continuacion/revision-git-despliegue.txt`, `revision-git-crud.txt`, `agenda-5705163907.txt`, `agenda-5705189135.txt`, `agenda-5705221107.txt`, `agenda-5705233588.txt` y `calendar-final-5705160321.txt`.

Se publicó ese lote como `24cd4c9902329e9b13d336ff9ef9fc94a9eaf2e0` en `main` con la identidad Git configurada. GitHub devolvió el mismo SHA.

### Recordatorios de consulta (HU-18)

En **Agenda → Horarios laborales** se puede guardar el margen de 5 a 1440 minutos por consultorio. Las configuraciones nuevas y las filas anteriores con margen nulo usan 30 minutos. El temporizador de Payara activa cada minuto los avisos de citas pendientes y confirmadas. Las vistas clínicas visibles consultan las publicaciones cada 30 segundos y muestran fecha, hora, paciente y motivo a administración y nutrición del mismo consultorio.

El aviso se oculta al cancelar, completar, vencer o mover la cita fuera del margen. Si la cita reprogramada vuelve a quedar dentro del margen, el temporizador actualiza la hora programada en la misma fila. La llave única de MySQL conserva una fila por cita y tipo. El filtro de acceso conserva el instante de la última actividad de la sesión cuando llegan sondeos automáticos.

El EAR activo quedó en `.cache/despliegues/20261126-090802-016` con compilación Ant, HTTP 200 y ping correcto del pool. MySQL 8.0.41 mostró las 14 tablas, la nueva columna de configuración y la FK e índice único de `recordatorios`.

| Verificación | Comprobaciones | Evidencia en `.cache/revision-continuacion/` |
|---|---:|---|
| Recordatorios en Edge y Payara con MySQL | 21 | `recordatorios-verificar-5706249175.txt`, `recordatorios-jdbc-5706249175.txt` y `recordatorios-390.png` |
| Agenda, expediente, portal, adjuntos, contactos y bloqueos de Calendar | 93 | `agenda-5706090285.txt`, `agenda-5706122951.txt`, `agenda-5706155317.txt`, `agenda-5706167607.txt` y `calendar-final-5706086864.txt` |
| CRUD de pacientes en Edge | 17 | `recordatorios-crud.txt` |
| Validación Java de pacientes y filtro con tiempo simulado | 22 + 4 | `recordatorios-validaciones.txt` |

También pasaron las 15 pruebas de Calendar con el proveedor ficticio. Se limpiaron los consultorios, usuarios, citas y pacientes de las pruebas. Quedan la autorización y sincronización real de Calendar (HU-16), la espera real de expiración de sesión y la impresión del portal. La siguiente historia de reserva pendiente es el envío de confirmaciones (HU-21). [Guion de recordatorios](README.md#probar-los-recordatorios-de-consulta-hu-18) y [matriz de historias](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#7-trazabilidad-de-historias-y-pendientes).

## Continuación del 25 de noviembre de 2026

Se ejecutó `git fetch --prune origin` y `git pull --ff-only`. `main` y `origin/main` están en `53ad585342762ca74131e5ee723d30e6a5e94c17`. Al retomar había 24 archivos modificados y 26 nuevos en el árbol local. Ese trabajo y las correcciones de esta sesión siguen en el árbol de trabajo.

- El alta de un consultorio crea su primera cuenta de administración en la misma transacción. El consultorio de la cuenta determina los accesos, las búsquedas y los cambios de agenda. Recepción abre la agenda al entrar.
- Los horarios y la duración se guardan juntos por consultorio. La disponibilidad pública y el guardado consultan las mismas citas activas. La reserva comprueba el horario dentro de una transacción que bloquea la fila del consultorio.
- La reserva muestra el código de cita. El nutriólogo encuentra el código del portal en el expediente. El portal presenta la próxima cita con confirmación y cancelación, y la evolución de mediciones de consultas cerradas o históricas completadas.
- El historial del expediente consulta las filas persistidas de MySQL. Antes podía conservar vacía una colección de consultas que ya se había cargado y ocultar una consulta recién guardada.
- Se vinculó la configuración antigua de agenda al consultorio principal y se inicializó la versión de las citas antiguas. La estructura se conserva con la ampliación de tablas de EclipseLink. MySQL devuelve `historia_dietetica` como `LONGTEXT`.

La aplicación se ejecuta en [NutriVida local](http://localhost:8080/sistema_nutricion-war/). Usar JDK 17, Payara 5.2022.3 y el EAR raíz. Para repetir la verificación, ejecutar `scripts/probar-validaciones.ps1` y luego `scripts/desplegar-local.ps1`. El acceso de demostración se describe en el README. Los recorridos de pacientes, agenda y consultorio usan registros ficticios que se eliminan al terminar.

El despliegue activo al pedir el traspaso es `.cache/despliegues/20261125-211908-023`. Ant compiló ese EAR y la página respondió HTTP 200. El pool pasó el ping y se verificaron diez tablas en MySQL. Las 22 pruebas Java pasaron antes del último ajuste XHTML. El recorrido de 17 comprobaciones de pacientes pasó en un despliegue previo de esta sesión.

El recorrido en `.cache/revision-continuacion/flujo-agenda.js` confirmó el aislamiento de dos consultorios, reservas simultáneas, reprogramación por recepción, confirmación y cancelación desde el portal, persistencia de los siete apartados y reapertura en solo lectura. Encontró las seis pestañas como paneles independientes después de cambiar el selector de alergias a `pageDirection`. Después venció la espera del mensaje de guardado de antecedentes. La prueba aislada `.cache/revision-continuacion/diagnostico-antecedentes.js` sí guardó y mostró el mensaje. El diagnóstico de evolución mostró las cuatro gráficas. Queda repetir el recorrido integral y confirmar catálogo, filtro de fechas, cálculo, portal con mediciones y bloqueo/desactivación. Se eliminaron los dos consultorios de la ejecución y sus registros ficticios con `FixtureNutricion`.

Al recibir ese traspaso quedaban pendientes los adjuntos del expediente (HU-12 a HU-14) y la unicidad de correo y teléfono de pacientes por consultorio. La [matriz vigente](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#7-trazabilidad-de-historias-y-pendientes) recoge la evidencia de ambos avances.

### Recorrido integral retomado

El guardado de una consulta procesaba otra vez el parámetro JSF `id` y podía dejar el expediente sin paciente para las peticiones siguientes. Se fija el identificador al abrir el expediente y el guardado procesa el formulario de la consulta. La pestaña cambia a solo lectura en la respuesta Ajax y luego se pueden guardar antecedentes en la misma página.

El campo libre de alergias desplazaba el botón Guardar al redimensionarse al perder el foco. Se desactivó ese redimensionamiento en los tres campos de la pestaña. En el portal, las gráficas se redibujan al cambiar el ancho de la ventana.

- Ant compiló y Payara sirvió HTTP 200 desde `.cache/despliegues/20261125-223958-837`. El ping de `NutricionMySQLPool` y las 22 pruebas Java de validación pasaron.
- Edge ejecutó 38 comprobaciones con dos consultorios ficticios. Además de agenda, portal de citas y notas clínicas, guardó y volvió a leer antecedentes y alergias, filtró las cuatro gráficas, comprobó el plan dietético tras recargar, mostró las mediciones cerradas en el portal a 390 px y verificó el bloqueo, desbloqueo y revocación de la sesión de recepción.
- El recorrido quedó en `.cache/revision-continuacion/flujo-agenda-base.js`, el resultado en `.cache/revision-continuacion/agenda-5668168864.txt` y la captura ficticia del portal en `.cache/revision-continuacion/portal-390.png`. `FixtureNutricion` eliminó los dos consultorios creados para esa ejecución.

### Adjuntos y contactos por consultorio

- HU-12 a HU-14. La pestaña **Archivos** carga PDF, JPG y PNG de hasta 10 MB, filtra por Laboratorio, Estudio y Fotografía de progreso, y compara dos fotos de fechas distintas. Se eligió almacenar los archivos en `data/nutricion-adjuntos` del dominio de Payara por defecto y la metadata en `archivos_adjuntos`. La fecha de foto toma el día de carga cuando se deja vacía.
- La descarga requiere una cuenta clínica del mismo consultorio. En Edge se comprobó carga, recarga, descarga, filtro, comparación y denegación con recepción, otra sede y sin sesión. Al borrar el expediente ficticio, su descarga empezó a devolver 404 y el almacenamiento volvió a su conteo anterior.
- Correo y teléfono se validan en el consultorio desde el panel y la reserva pública. `MigracionContactosPacientes` cambia los índices únicos globales de esos campos a `(consultorio_id, email)` y `(consultorio_id, telefono)` al arrancar. MySQL confirmó que `codigo_acceso` conserva el índice único global. Se probó el mismo contacto en dos sedes, rechazo de duplicados dentro de una sede y reutilización del expediente local al volver a reservar.

### Estado entregado

`main`, `origin/main` y la rama `main` del remoto coinciden en `53ad585342762ca74131e5ee723d30e6a5e94c17`. Los cambios anteriores y los de esta continuación están en el árbol local. El EAR activo es `.cache/despliegues/20261125-225514-900`. Ant reportó `BUILD SUCCESSFUL`, la portada respondió HTTP 200, el pool `NutricionMySQLPool` pasó el ping y volvieron a pasar las 22 pruebas Java.

La corrida final utilizó Edge, Payara y MySQL locales. Los siguientes resultados quedan en `.cache/revision-continuacion/`.

| Recorrido | Comprobaciones | Resultado |
|---|---:|---|
| Agenda, expediente, portal y recepción | 38 | `agenda-5669014772.txt` |
| Adjuntos | 24 | `agenda-5669018664.txt` y `archivos-comparacion.png` |
| Contactos entre consultorios | 10 | `agenda-5669043806.txt` |
| CRUD de pacientes | 17 | `crud-final.txt` |

Se limpiaron los dos consultorios ficticios de cada recorrido integral y el paciente ficticio del CRUD. MySQL 8.0.41 mostró 11 tablas, los dos índices compuestos y `historia_dietetica` como `LONGTEXT`. El esquema coincidió al redesplegar. Metadatos en `esquema-antes-contactos.txt` y `esquema-final.txt`. El README tiene un guion manual de adjuntos y los archivos de prueba están en `.cache/revision-continuacion/adjuntos-qa/`.

Quedan por verificar la expiración real de sesión a 30 minutos, la impresión del resumen del portal y los criterios parciales de la matriz. El siguiente módulo pendiente en el orden del README es sincronización de agenda con Google Calendar (HU-16).

## Revisión de referencia del 15 de noviembre de 2026

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

Ensayar el guion y el cuestionario, comprobar qué formato de entrega pide el profesor y retomar los criterios pendientes de la matriz de historias antes de ampliar funcionalidades.

## Prompt listo para otro chat

> Continúa sistema_nutricion en la ruta de Windows indicada arriba. Lee AGENTS.md, CONTINUIDAD_UNIDAD_4.md, README.md y la matriz de historias. Retoma la publicación del 26 de noviembre y HU-18. Comprueba main, origin/main y los últimos commits. El lote de expediente, agenda y portal se publicó en 24cd4c9, antes del commit de recordatorios. Conserva los cambios locales y usa el EAR raíz con Java EE 8 javax.*, NetBeans/Ant, JDK 17, Payara 5.2022.3, JSF 2.3, PrimeFaces 5.0 y MySQL. El EAR activo quedó en .cache/despliegues/20261126-090802-016 y respondió en http://localhost:8080/sistema_nutricion-war/. HU-18 muestra avisos de consulta por consultorio con un margen inicial de 30 minutos. Pasaron 21 comprobaciones de recordatorios y 110 de regresión en Edge, 22 pruebas de pacientes, cuatro de sesión con tiempo simulado y 15 de Calendar ficticio. Usa scripts/probar-validaciones.ps1, scripts/probar-calendario-google.ps1 y scripts/desplegar-local.ps1. Usa únicamente datos ficticios y limpia los de cada prueba. HU-16 requiere una cuenta de Google autorizada. Retoma HU-21 según la matriz para las confirmaciones de cita.
