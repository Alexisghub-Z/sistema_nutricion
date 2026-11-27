# Continuidad de sistema_nutricion / Unidad 4

## Informe de Word para futuras sesiones

El informe detallado y su resumen de exposición están en `C:\Users\yaxti_cm5rg3t\Downloads\NutriVida - Avance y resumen.docx`. Después de cada avance, actualizar ese mismo archivo con la fecha, el cambio y sus pruebas antes de «Resumen para exponer». Mantener el resumen al final con el estado vigente.

## Continuación del 26 de noviembre de 2026

### Consulta del catálogo SMAE y los márgenes del borrador

- **HU-25.** En **Pacientes → Expediente → Plan alimenticio → Revisar catálogo SMAE** se pueden consultar los 16 alimentos y su medida por equivalente antes de guardar una meta. Se cotejaron las cantidades con la Cartera de Alimentación del IMSS. El borrador y el plan aprobado muestran los márgenes calculados a partir de su propia meta. La búsqueda y la validación usan la misma regla. [Cartera de Alimentación, pp. 9–11](https://www.imss.gob.mx/sites/all/statics/salud/guias_salud/CARTERA_ALIMENTACION_2024_DIGITAL.pdf#page=9).
- **HU-26.** La consulta identifica el motivo de exclusión de un alimento y distingue las restricciones sin ingrediente coincidente de las que requieren verificación. Edge comprobó que la revisión vuelve a leer las restricciones guardadas, muestra la exclusión por lactosa y señala fructosa y sulfitos como pendientes. Al guardar cambios en la misma vista se retira la consulta anterior. El paciente continúa viendo las porciones del plan entregado.

Ant compiló el EAR raíz con JDK 17. Payara 5.2022.3 sirve `.cache/despliegues/20261126-235250-067`. La portada respondió HTTP 200, el pool `NutricionMySQLPool` pasó el ping y MySQL 8.0.41 mostró 16 tablas. [NutriVida local](http://localhost:8080/sistema_nutricion-war/).

Pasaron 41 comprobaciones de Plan alimenticio, 28 de cálculo y 40 de agenda, expediente y portal en Edge. Se revisó la tabla a 390 px y en escritorio. Pasaron 53 comprobaciones Java, incluidas 20 de planes. MySQL confirmó dos planes aprobados de prueba, uno entregado, y cero después de retirar los registros ficticios.

| Comprobación | Evidencia en `.cache/revision-continuacion/revision-smae-20261126/` |
|---|---|
| Plan alimenticio y catálogo en Edge | `5758852116/recorrido.txt`, `5758852116/catalogo-390.png` y `5758852116/catalogo-restricciones.png` |
| Cálculo, agenda, expediente y portal | `calculo-criterios-limite-superior-5758921585.txt` y `regresion-portal-5758941175.txt` |
| Persistencia y limpieza | `persistencia-5758852116.txt`, `limpieza-5758852116.txt` y `persistencia-tras-limpieza-5758852116.txt` |
| Build, despliegue, JDBC y pruebas Java | `despliegue-final.txt`, `ear-activo-final.txt`, `ping-jdbc-final.txt`, `mysql.txt` y `java.txt` |

El primer flujo SMAE se publicó en `main` como `875f856`. La matriz registra **26 historias probadas en local, 8 parciales y 10 pendientes**. Queda confirmar con el consultorio los subgrupos de alimentos, la cobertura de restricciones y las tolerancias de HU-25 y HU-26. HU-27 y HU-29 siguen pendientes. [Casos ficticios de revisión](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#revisión-con-el-consultorio).

### Primer flujo de plan alimenticio SMAE

- **HU-07, HU-25, HU-26 y HU-28.** En **Pacientes → Expediente → Plan alimenticio**, nutrición genera un borrador de cinco tiempos con la última meta y los macros guardados. El catálogo inicial contiene 16 alimentos de ocho grupos. La pantalla muestra las medidas caseras, los equivalentes y el aporte estimado frente a la meta.
- La generación excluye alimentos identificados en las alergias de catálogo y los campos de texto. Avisa cuando falta una combinación compatible o no puede verificar una restricción. El borrador se pierde al recargar o descartar. La aprobación exige confirmar la revisión. Aprobar y entregar vuelven a comprobar el cálculo y las alergias vigentes.
- MySQL conservó dos planes aprobados del paciente ficticio, con cinco tiempos en cada uno. Solo el segundo tuvo fecha de entrega. El portal mostró los alimentos y las cantidades al paciente correcto y retiró el plan al registrar una alergia incompatible. Recepción y otra sede quedaron fuera del expediente. Al eliminar el paciente ficticio desde la lista se retiraron sus planes.

Ant compiló el EAR raíz con JDK 17. Payara 5.2022.3 sirve `.cache/despliegues/20261126-203140-371`. La portada respondió HTTP 200 y `NutricionMySQLPool` pasó el ping. MySQL 8.0.41 presentó 16 tablas con `planes_alimenticios` y `porciones_plan`. [NutriVida local](http://localhost:8080/sistema_nutricion-war/).

Pasaron 30 comprobaciones del flujo nuevo, 28 de cálculo y 40 de agenda, expediente y portal en Edge. El mayor tiempo de generación observado fue de 2,14 segundos. Pasaron 47 comprobaciones Java, incluidas 14 del generador y las restricciones. Se limpiaron las sedes y los datos ficticios de las pruebas.

| Comprobación | Evidencia en `.cache/revision-continuacion/plan-alimenticio-20261126/` |
|---|---|
| Generación, aprobación, entrega y vista a 390 px | `5746701020/recorrido.txt`, `5746701020/borrador-390.png` y `5746701020/portal-390.png` |
| Persistencia y eliminación del paciente | `5746701020/persistencia.txt`, `5746701020/eliminacion-edge.txt` y `5746701020/persistencia-tras-eliminar.txt` |
| Regresiones Edge | `calculo-criterios-limite-superior-5746701022.txt` y `regresion-portal-5746701021.txt` |
| Build, MySQL, JDBC y pruebas Java | `despliegue-final.txt`, `mysql.txt`, `ping-jdbc-final.txt` y `java-final.txt` |

La matriz de 44 historias registra **26 probadas en local, 8 parciales y 10 pendientes**. HU-25 y HU-26 quedan parciales hasta revisar el catálogo, las tolerancias y la cobertura con el consultorio. Siguen pendientes el proveedor de IA, los ajustes en lenguaje natural y el repertorio oaxaqueño. El corte parte de `8a3c092`. [Matriz de historias](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#7-trazabilidad-de-historias-y-pendientes).

### Panel, pagos, comparativo y recuperación de contraseña

- **HU-37 y HU-41.** Inicio muestra las citas de hoy y cuenta pacientes distintos con consulta atendida en el periodo configurado en **Horarios**. Los indicadores y citas se actualizan cada 20 segundos mientras se ve el panel. El sondeo conserva la última actividad de sesión. La asistencia se calcula con citas pasadas completadas y ausencias registradas. El panel resalta los porcentajes inferiores a 80.
- **HU-38 y HU-39.** El panel compara ingresos del mes actual y anterior. Administración, nutrición y recepción pueden consultar cobros de consultas cerradas y liquidarlos desde **Pagos**. Se registra un cobro por consulta con método, estado y fecha de liquidación en `consultas.fecha_pago`. Los cobros históricos que ya estaban pagados conservan la fecha de consulta como referencia cuando falta la fecha de liquidación.
- **HU-11.** Evolución compara nueve indicadores. El último objetivo guardado resalta el cambio de peso en pérdida o mantenimiento y de masa muscular en ganancia. Edge probó los tres objetivos.
- **HU-04.** Acceso enlaza a recuperación por correo. MySQL guarda una huella del código, su vencimiento y la revisión de credencial. El enlace dura 30 minutos y se consume al cambiar la contraseña. El cambio revoca sesiones anteriores y conserva el reingreso por Acceso. La entrega se comprobó en SMTP ficticio y se retiró la configuración temporal.

Ant compiló el EAR raíz con JDK 17. Payara 5.2022.3 sirve `.cache/despliegues/20261126-195010-351`. La portada devolvió HTTP 200, el ping de `NutricionMySQLPool` pasó y MySQL 8.0.41 mostró las 14 tablas y las columnas nuevas. [NutriVida local](http://localhost:8080/sistema_nutricion-war/).

Pasaron 27 comprobaciones de panel, pagos y comparativo, 15 de recuperación en Edge y 40 de regresión de agenda, expediente y portal. La prueba de recuperación adelantó el vencimiento de un enlace ficticio en la base local. En Java pasaron 22 comprobaciones de pacientes, 5 de macros y 6 de sesión. Los recorridos retiraron sus propios registros ficticios.

| Comprobación | Evidencia en `.cache/revision-continuacion/` |
|---|---|
| Panel, pagos y comparativo a 390 px y en escritorio | `indicadores-pagos-20261126/recorrido-final.txt`, `pagos-390.png`, `pagos-escritorio.png` y `panel-escritorio.png` en esa carpeta |
| Recuperación y regresión del portal | `recuperacion-20261126/recorrido-final.txt` y `regresion-portal.txt` en esa carpeta |
| Build, servicios y publicación | `recuperacion-20261126/despliegue-final.txt`, `ping-jdbc.txt`, `mysql-final.txt` y `publicacion.txt` en esa carpeta |

La matriz de 44 historias registra **24 probadas en local, 7 parciales y 13 pendientes**. Siguen pendientes la validación con el consultorio de HU-10 y HU-22, el catálogo SMAE y el proveedor para planes, y las cuentas autorizadas para conectar Calendar, correo y WhatsApp. [Matriz y revisión con el consultorio](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#7-trazabilidad-de-historias-y-pendientes).

### Caducidad real de sesión y validación de macronutrientes

- **HU-02.** En el EAR de `70496e8`, Edge recibió 59 sondeos automáticos durante la espera sin acciones manuales. Volvió a `/acceso.xhtml` a los 1800,20 segundos. La solicitud posterior a la vista privada devolvió HTTP 302 y la misma cuenta ficticia pudo iniciar sesión de nuevo. El ensayo terminó antes del redespliegue y se eliminó su consultorio ficticio.
- **HU-24.** En el EAR corregido, Edge verificó la distribución de porcentajes y gramos para los tres objetivos, el ajuste manual de la meta y el recálculo de gramos al pegar valores y salir del campo. Rechazó porcentajes negativos, mayores de 100, omitidos y distribuciones con suma distinta de 100. Al recargar reaparecieron la meta y los gramos del único plan válido.
- **Regresión.** Pasaron 28 comprobaciones del cálculo y 40 de agenda, expediente y portal en Edge. Cada recorrido usó su propia marca. Se retiraron un consultorio ficticio del cálculo y dos de la regresión del portal.

**Entorno del cierre.** Ant compiló el EAR raíz con JDK 17. Payara 5.2022.3 quedó sirviendo `.cache/despliegues/20261126-144514-239` y la portada devolvió HTTP 200. El pool `NutricionMySQLPool` pasó el ping. MySQL 8.0.41 mostró las 14 tablas esperadas. [NutriVida local](http://localhost:8080/sistema_nutricion-war/).

En Java pasaron 22 pruebas de pacientes, cinco de macros y cuatro de sesión con tiempo simulado. También pasaron 15 de Calendar y nueve de confirmaciones con proveedores ficticios.

La matriz queda con **18 historias probadas en local, 10 parciales y 16 pendientes**. HU-10 sigue parcial hasta confirmar los siete apartados con el consultorio. HU-22 sigue parcial hasta validar los coeficientes, factores y rangos con el nutriólogo. Calendar y la entrega real de avisos requieren cuentas autorizadas. [Matriz de historias](docs/UNIDAD_4_REVISION_Y_ENTREGA.md#7-trazabilidad-de-historias-y-pendientes).

Evidencia local en `.cache/revision-continuacion/criterios-20261126/`.

| Verificación | Archivos |
|---|---|
| Caducidad real y limpieza | `sesion-5717680423.jsonl`, `limpieza-sesion-5717680423.txt` |
| Cálculo y regresión en Edge | `calculo-criterios-final-edge.txt`, `calculo-criterios-limite-superior-edge.txt`, `regresion-portal-edge.txt` |
| Compilación, HTTP y MySQL | `despliegue-macros-final.txt`, `servicios-macros-final.txt`, `mysql-metadatos-macros.txt` |
| Pruebas Java | `probar-validaciones-publicacion.txt`, `probar-calendario-google-verificado.txt`, `probar-confirmaciones-verificado.txt` |
| Publicación y comparación de SHA | `git-publicacion-macros.txt` |

### Recepción, expediente y cálculo dietético

El lote de confirmaciones, impresión y administración se publicó en `main` como `70496e80bdeee13277498303cd90f2d3be2a62d8`. Antes de las pruebas se verificó el mismo SHA en GitHub. Ant recompiló esa revisión y se compararon 53 clases del EJB y 34 clases y vistas del WAR con el EAR activo en `.cache/despliegues/20261126-103113-060`.

- **HU-03 y HU-17.** Pasaron 17 comprobaciones de recepción con Edge, Payara y MySQL. Administración editó y desactivó una cuenta ficticia. Recepción recibió HTTP 302 al abrir el expediente con el cálculo dietético. En la agenda vio los tres estados, rechazó un empalme, reprogramó a una hora libre y liberó el turno al cancelar.
- **HU-06, HU-08 y HU-10.** Pasaron 38 comprobaciones del expediente. Nutrición registró una ficha ficticia y recuperó los tres antecedentes en una nueva sesión. Se rechazaron los valores fuera de ambos extremos del rango para los seis campos de medición. El IMC se actualizó al cambiar el peso. La consulta fechada conservó las mediciones y sus siete apartados quedaron en solo lectura tras el cierre. Queda confirmar el contenido de los apartados con el consultorio.
- **HU-22 y HU-23.** El cálculo recuperó peso, talla y grasa de la última consulta. En Edge coincidieron los resultados esperados de las cinco fórmulas para el paciente ficticio y se verificaron el factor de actividad, los tres objetivos y el ajuste editable. Katch-McArdle y Cunningham mostraron su aviso al faltar grasa. Queda validar los coeficientes y factores con el nutriólogo.
- **HU-24.** El ensayo reprodujo que un valor negativo podía guardarse si los tres porcentajes sumaban 100. La fachada y el formulario requieren ahora un entero entre 0 y 100 en cada campo y se conserva la comprobación de la suma. En Java pasaron cinco comprobaciones de los rangos y la persistencia.

Evidencia en `.cache/revision-continuacion/criterios-20261126/`. Los recorridos iniciales y su limpieza están en `recepcion-agenda-5717302873.txt`, `limpieza-recepcion-5717302873.txt`, `expediente-criterios-v3.txt` y `calculo-criterios-v3.txt`. Los resultados Java están en `macros-despues.txt`, `probar-calendario-google-final.txt` y `probar-confirmaciones-final.txt`.

### Confirmaciones de reserva, impresión y administración

- **HU-21.** Después de guardar una reserva, NutriVida intenta confirmar por correo con JavaMail. WhatsApp está disponible si se configura una plantilla y el paciente elige recibirla en su teléfono internacional. Los avisos contienen fecha, hora, ubicación y código de cita. En Edge se comprobó que ante un rechazo SMTP la cita conserva el código y se puede consultar. Dos solicitudes simultáneas produjeron una cita y un correo capturado por el servidor ficticio.
- **HU-44.** El primer PDF partía la gráfica de IMC entre páginas. El portal ya aplica un formato de impresión que oculta controles y navegación. Edge produjo un PDF de una página con las mediciones y las dos gráficas. Se verificaron tanto el contenido como la imagen del PDF.
- **HU-01 y HU-05.** El listado de edición en Administración usaba solo el consultorio de la sesión. Se añadió **Administración → Consultorio → Consultorios con reservación pública**, con los nombres y enlaces que corresponden a las sedes activas. La lista se actualiza al guardar y con **Actualizar lista**. El acceso a edición y usuarios sigue ligado a la sede de la cuenta.

La prueba de Administración reprodujo la omisión en la versión anterior. La versión corregida pasó 10 comprobaciones en Edge con dos sedes ficticias, incluida la recarga de la página con HTTP 200. En el redespliegue controlado apareció HTTP 404 entre las 10:31:14 y las 10:31:19. Una vez cargado el EAR, el recorrido de alta y recarga quedó sin 404 ni 500. La captura del usuario corresponde a un 404 de Payara y su causa exacta no se pudo confirmar a partir de la imagen.

En ese corte, el EAR estaba en `.cache/despliegues/20261126-103113-060`. Ant compiló y Payara sirvió HTTP 200. El ping JDBC y el esquema de 14 tablas de MySQL 8.0.41 fueron verificados. Los cambios partieron de `e5a4b45475837660afad9f28ab9e150f5ea08370` y se publicaron en `70496e8`.

Pasaron 9 comprobaciones de confirmaciones en Java, 11 del flujo SMTP en Edge y 40 del recorrido de agenda, expediente y portal. También pasaron 22 de pacientes, 4 de sesión con tiempo simulado y 15 de Calendar con proveedor ficticio. La evidencia está en `.cache/revision-continuacion/hu21-5709482589`, incluidos `edge-confirmaciones.txt`, `verificacion-smtp.txt`, `portal-regresion-final.txt`, `portal-impresion-visual.png`, `admin-5710610342.txt`, `admin-5710704599.txt` y `http-durante-despliegue.txt`. Se retiró el SMTP temporal y se limpiaron los consultorios ficticios de cada prueba.

En el corte de confirmaciones, la matriz registraba **11 historias probadas en local, 17 parciales y 16 pendientes**.

Se reprodujeron cuatro problemas de actualización en el despliegue del 25 de noviembre. Si se guardaba una ficha de paciente abierta antes de escribir un antecedente, el antecedente podía desaparecer. La agenda retenía el nombre y teléfono anteriores en el selector. El panel y el portal abiertos podían seguir mostrando una cita después de completarla. Al cerrar una consulta, la cabecera del expediente conservaba el peso anterior. Evidencia en `.cache/revision-continuacion/agenda-5670239286.txt`.

- La ficha general, los antecedentes y las alergias escriben sus propios campos. La edición del paciente conserva los datos clínicos recién guardados y el expediente actualiza la cabecera al cerrar la consulta. **Actualizar datos** vuelve a leer la ficha sin volver a renderizar los formularios clínicos.
- La agenda relee el selector al abrir una cita. Al seleccionar un evento, el calendario y la tabla muestran la misma fecha. La agenda puede actualizarse conservando el rango visible y la edición de duración se hace en el diálogo de cita.
- El panel distingue citas por atender hoy y completadas. La agenda, el panel y el portal se actualizan con su botón o al volver a la vista. En Edge se comprobó el manejador de foco y se probó que un Ajax con la sesión revocada devuelve al acceso.

### Google Calendar (HU-16)

Se añadió la vinculación por consultorio, el cliente de Calendar y una tarea EJB cada cinco minutos. El ciclo cubre los próximos 90 días del calendario principal. Los bloqueos se guardan en `bloqueos_calendario` y afectan tanto el calendario de la agenda como la confirmación de una reserva. La aplicación publica citas con el título **Consulta NutriVida**. El token se guarda cifrado en `vinculos_google_calendar`.

Se comprobó con un proveedor HTTP ficticio la autorización, importación paginada, creación, reprogramación y cancelación de citas, conservación de bloqueos ante una caída y recuperación de un evento borrado. En Edge, un bloqueo de 09:00 a 10:00 dejó libre el turno siguiente, rechazó una reserva que ya se había seleccionado y afectó solo al consultorio indicado. A las 08:45 Payara registró una ejecución automática correcta del temporizador local. Evidencia en `.cache/revision-continuacion/calendario-timer.txt`. La validación de OAuth y de la sincronización con una cuenta real queda pendiente. La [sección de Calendar](README.md#vincular-google-calendar) indica las propiedades del dominio y cómo probar el flujo.

### Corte de Calendar antes de la publicación

En ese corte, el EAR estaba en `.cache/despliegues/20261126-083353-223`. Ant reportó `BUILD SUCCESSFUL`, la portada respondió HTTP 200 y `NutricionMySQLPool` pasó el ping. La inspección de MySQL 8.0.41 confirmó 13 tablas y `historia_dietetica` como `LONGTEXT`. El trabajo seguía local sobre `main` en `53ad585342762ca74131e5ee723d30e6a5e94c17`.

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

Se limpiaron los registros ficticios de cada ejecución. En ese corte quedaban por verificar la cuenta real de Calendar, la expiración por inactividad y la impresión del portal. El siguiente módulo de agenda era recordatorios (HU-18).

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

### Corte de publicación de los recordatorios

`main`, `origin/main` y `main` en GitHub coincidieron en `e5a4b45475837660afad9f28ab9e150f5ea08370`, que incluye HU-18. Payara 5.2022.3 sirvió `.cache/despliegues/20261126-090802-016` con JDK 17. La portada, el acceso, la reserva y `portal.xhtml` respondieron HTTP 200. Sin sesión, las vistas privadas redirigieron al acceso. `NutricionMySQLPool` pasó el ping y una consulta de metadatos confirmó las 14 tablas esperadas de MySQL 8.0.41.

Se repitieron 22 pruebas de pacientes, cuatro de sesión con tiempo simulado y 15 de Calendar con proveedor ficticio. Las 131 comprobaciones en Edge corresponden a los recorridos registrados en la sección de HU-18. La matriz cuenta 10 historias probadas en local, 17 parciales y 17 pendientes. El diagrama de cuatro tablas y el guion de CRUD del documento de entrega corresponden al primer avance de noviembre.

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

> Continúa NutriVida en este repositorio de Windows. Lee AGENTS.md, CONTINUIDAD_UNIDAD_4.md, README.md y docs/UNIDAD_4_REVISION_Y_ENTREGA.md. Comprueba HEAD, cambios locales, Payara y MySQL. El primer flujo SMAE se publicó en `main` como `875f856`. El corte siguiente incorpora la consulta de medidas y restricciones en el expediente y muestra los márgenes que aplica el generador.
>
> Mantén el EAR raíz con Java EE 8 `javax.*`, NetBeans/Ant, JDK 17, Payara 5.2022.3, JSF 2.3 y PrimeFaces 5.0. Al cierre, el EAR de `.cache/despliegues/20261126-235250-067` devolvió HTTP 200. `NutricionMySQLPool` pasó el ping y MySQL 8.0.41 mostró 16 tablas. Pasaron 41 comprobaciones del plan, 28 de cálculo y 40 de agenda, expediente y portal en Edge, y 53 Java. Usa datos ficticios y limpia solo los registros de cada prueba.
>
> La matriz registra 26 historias probadas en local, 8 parciales y 10 pendientes. Usa los casos ficticios documentados para confirmar con el consultorio los subgrupos, la cobertura de alérgenos y las tolerancias de HU-25 y HU-26. Quedan los ajustes en lenguaje natural y el repertorio regional. Conserva el mismo Word de Descargas con el avance fechado antes de «Resumen para exponer» y el resumen al final.
