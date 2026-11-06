# Sistema de Nutrición — Proyecto empresarial Java EE (Ant)

Proyecto empresarial hecho con **Java - Ant** en NetBeans, del lado de **Java EE** (no Jakarta EE):

| Elemento     | Versión                          |
|--------------|----------------------------------|
| Plataforma   | Java EE 8                        |
| Servidor     | Payara Server 5.2022.3           |
| JDK          | 17                               |
| Interfaz     | JSF 2.3 + PrimeFaces 5.0         |
| Persistencia | JPA 2.2 (EclipseLink de Payara)  |

## Estructura: 3 módulos

```
sistema_nutricion/                 ← 1. Aplicación empresarial (EAR): el "índice" que une los módulos
├── build.xml
├── nbproject/                       project.xml, project.properties, build-impl.xml
├── src/conf/MANIFEST.MF
│
├── sistema_nutricion-ejb/         ← 2. Módulo EJB: lógica de negocio y datos
│   ├── nbproject/
│   └── src/
│       ├── conf/                    persistence.xml, beans.xml, MANIFEST.MF
│       └── java/com/nutricion/
│           ├── entidades/           Entidades JPA: Usuario, Paciente, Cita, Consulta + enums
│           ├── servicios/           Session beans @Stateless (fachadas) + InicioDatos (@Singleton @Startup)
│           └── util/                Cálculos (IMC, etc.), hash de contraseñas, excepciones
│
└── sistema_nutricion-war/         ← 3. Módulo Web (WAR): vistas JSF
    ├── nbproject/
    ├── lib/primefaces-5.0.jar
    ├── src/java/com/nutricion/web/
    │   ├── beans/                   Managed beans (@Named) que llaman a los EJB
    │   ├── filtros/                 Filtro que protege /vistas/*
    │   └── util/
    └── web/                         (Web Pages)
        ├── WEB-INF/                 web.xml, glassfish-web.xml, faces-config.xml, beans.xml
        ├── resources/               css, img, js
        ├── plantillas/              plantilla.xhtml (pública) y plantillamenu.xhtml (panel)
        ├── vistas/                  Páginas privadas del nutriólogo (requieren sesión)
        │   ├── dashboard.xhtml
        │   └── pacientes/lista.xhtml
        ├── index.xhtml              Página pública
        └── acceso.xhtml             Inicio de sesión
```

Cómo fluye una petición:

```
vista .xhtml  ──►  managed bean (WAR)  ──@EJB──►  session bean @Stateless (EJB)  ──►  EntityManager / entidad JPA  ──►  BD
```

### Plantillas

Las dos plantillas usan las mismas zonas `ui:insert` que en clase: `top`, `content` y `bottom`
(`plantillamenu.xhtml` también tiene `menu`). Cada página usa `ui:composition template="..."` y llena
las zonas con `ui:define`.

## Abrir y ejecutar en NetBeans

1. **File → Open Project** y elige la carpeta del repositorio (`sistema_nutricion`, la del triángulo).
   NetBeans abre también `sistema_nutricion-ejb` y `sistema_nutricion-war`.
2. Si pregunta por el servidor: **Resolve Missing Server Problem** → elige *Payara Server*
   (5.2022.3) configurado con **JDK 17** (*Servers → Payara Server → Java → Java Platform*).
3. Clic derecho sobre `sistema_nutricion` → **Clean and Build**, y luego **Run** (o **Deploy**).
4. Abre `http://localhost:8080/sistema_nutricion-war/`.

**Usuario de prueba:** `admin@nutricion.com` / `admin123`. Se crea solo la primera vez que se despliega,
junto con algunos pacientes y citas de ejemplo.

### Arrancar el servidor Payara

**Desde NetBeans:** pestaña **Services → Servers → Payara Server** → clic derecho → **Start**.
Con **Run** sobre `sistema_nutricion` también se arranca solo y despliega la app.

**Desde la terminal** (Payara instalado en `~/payara5`, con JDK 17):

```bash
export AS_JAVA=/usr/lib/jvm/java-17-openjdk-amd64
~/payara5/bin/asadmin start-domain              # arranca el servidor
~/payara5/bin/asadmin deploy --force=true dist/sistema_nutricion.ear   # despliega
~/payara5/bin/asadmin stop-domain               # lo detiene
```

- Aplicación: http://localhost:8080/sistema_nutricion-war/
- Consola de administración de Payara: http://localhost:4848

### Base de datos

No hay que configurar nada: `persistence.xml` usa `java:comp/DefaultDataSource` (la base embebida
`jdbc/__default` que trae Payara 5) y las tablas se crean al desplegar.
Para usar MySQL, PostgreSQL o Java DB, crea un *JDBC Connection Pool* y un *JDBC Resource* en Payara,
y cambia `<jta-data-source>` por su nombre JNDI (por ejemplo `jdbc/nutricion`).

## Compilar sin el IDE (opcional)

Con el Ant que trae NetBeans:

```bash
NB=/ruta/a/netbeans
$NB/extide/ant/bin/ant \
  -Dj2ee.platform.classpath=$NB/enterprise/modules/ext/javaee-api-8.0.jar \
  -Dlibs.CopyLibs.classpath=$NB/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar \
  clean dist
```

Genera `dist/sistema_nutricion.ear`.

---

# Especificación funcional del sistema (guía para desarrollar)

> **Para el agente de IA / desarrollador:** esta sección es la fuente de verdad de *qué* construir.
> Se adaptó del anteproyecto *"Especificación de Características, Épicas e Historias de Usuario, con
> Cronograma"* (TecNM campus Oaxaca, consultorio **OSMO**) al stack **real** de este repositorio.
> **Todo se implementa con NetBeans + Ant + Java EE 8** como se describe abajo.
> No introducir Spring, Maven/Gradle, Node, React, frameworks JS ni otro servidor.

## 0. Reglas de implementación (obligatorias)

| Tema | Regla |
|------|-------|
| IDE / build | NetBeans + **Ant** (`build.xml` / `nbproject/`). No migrar a Maven ni Gradle. Los `.jar` extra van en `sistema_nutricion-war/lib/` (o `-ejb/lib/`) y se registran en `nbproject/project.properties`. |
| Plataforma | **Java EE 8** con paquetes `javax.*` (equivalente a Jakarta EE 8). **No** usar `jakarta.*`: Payara 5 no lo soporta. JDK 17, Payara Server 5.2022.3. |
| Capas | Vista `.xhtml` (JSF 2.3 + PrimeFaces 5.0) → managed bean `@Named` (WAR) → `@EJB` session bean `@Stateless` (EJB) → entidad JPA → BD. Las vistas y los managed beans **nunca** usan `EntityManager`. |
| Persistencia | JPA 2.2 / EclipseLink, unidad `NutricionPU`. Cada entidad nueva se agrega como `<class>` en `persistence.xml` (`exclude-unlisted-classes=true`). |
| Fachadas | Un `XxxFacade extends AbstractFacade<Xxx>` por entidad, en `com.nutricion.servicios`. Las reglas de negocio lanzan `ReglaNegocioException`. |
| Tareas programadas | Recordatorios, liberación de horarios apartados y sincronización con Google Calendar usan **EJB Timer Service** (`@Schedule` en un `@Singleton`). |
| Servicios REST externos | **Cliente JAX-RS** (`javax.ws.rs.client.ClientBuilder`) + **JSON-B / JSON-P** (incluidos en Payara). Evitar SDKs pesados si basta HTTP. |
| Webhooks entrantes | Endpoints **JAX-RS** (`@Path`, `@ApplicationPath("api")`) en el WAR, p. ej. `/api/whatsapp/webhook`. |
| Correo | **JavaMail** con un recurso `mail/nutricion` configurado en Payara (`@Resource(lookup = "mail/nutricion")`). |
| Archivos | Upload con `p:fileUpload` de PrimeFaces; guardar en disco bajo una ruta configurable (fuera del WAR) y la metadata en BD. |
| Gráficas | Gráficas de **PrimeFaces 5.0** (`p:chart` con `LineChartModel`). Sin librerías JS externas. |
| Configuración / secretos | Claves de API (IA, WhatsApp, Google) **no** se versionan: van como *System Properties* / *JNDI custom resources* de Payara o variables de entorno, leídas por un bean `Configuracion`. |
| Seguridad | Contraseñas con `util.Seguridad` (hash). Acceso a `/vistas/*` por `FiltroAcceso`; validar además el **rol** en beans y fachadas. |
| Multiconsultorio | Toda entidad de negocio lleva `consultorio` (FK) y **toda** consulta JPQL filtra por el consultorio del usuario en sesión (ver HU-05). |
| Idioma | Código, paquetes, vistas y mensajes en **español**, igual que el código existente. |
| Plantillas | Páginas privadas usan `plantillas/plantillamenu.xhtml`; públicas (`index`, `acceso`, reservación, portal del paciente) usan `plantillas/plantilla.xhtml`. |

## 1. Contexto del producto

Sistema **SaaS** de gestión integral para consultorios de nutrición con asistencia de inteligencia
artificial, para el consultorio **OSMO** (Oaxaca de Juárez), como Residencia Profesional del TecNM campus
Oaxaca. Metodología **Scrum** (fases de inicio y cierre adaptadas de RUP), priorización **MoSCoW**
(Debe / Debería / Podría) y estimación en puntos Fibonacci.

Periodo: **31-ago-2026 al 4-dic-2026** (14 semanas, 640 h por residente).

## 2. Actores y roles

| Actor | Descripción | Rol en el sistema (`enum Rol`) |
|-------|-------------|------|
| Administrador del consultorio | Control total: da de alta el consultorio, crea usuarios y asigna permisos (recae en el nutriólogo titular). | `ADMIN` (ya existe) |
| Nutriólogo(a) titular | Responsable clínico y administrativo. Valoración, cálculo dietético y seguimiento. | `NUTRIOLOGO` (agregar) |
| Recepción | Agendado, confirmación de citas y control de pagos. **Sin** acceso a expediente clínico ni planes. | `RECEPCION` (agregar) |
| Paciente | Usuario externo: reserva citas, usa el asistente de WhatsApp y el portal con código único. | No es `Usuario`; accede por páginas públicas / código. |

**Matriz de permisos**

| Módulo | ADMIN | NUTRIOLOGO | RECEPCION | Paciente |
|--------|:-----:|:----------:|:---------:|:--------:|
| Usuarios y consultorio | ✔ | — | — | — |
| Expediente, mediciones, notas, adjuntos | ✔ | ✔ | ✘ | ✘ |
| Cálculo dietético y planes con IA | ✔ | ✔ | ✘ | ✘ |
| Agenda (ver/modificar) | ✔ | ✔ | ✔ | Solo su cita |
| Pagos | ✔ | ✔ | ✔ | — |
| Panel de indicadores | ✔ | ✔ | — | — |
| Portal del paciente | — | — | — | ✔ (código único) |

## 3. Estado actual del código vs. objetivo

**Ya existe:** entidades `Usuario`, `Paciente`, `Cita`, `Consulta` y enums (`Rol{ADMIN}`,
`EstadoCita{PENDIENTE, COMPLETADA, CANCELADA, NO_ASISTIO}`, `EstadoPago`, `MetodoPago`, `TipoCita`, `Sexo`);
fachadas CRUD; `InicioDatos` (datos de ejemplo); `CalculosNutricion` (IMC, ICC, GEB Mifflin-St Jeor);
login, filtro de acceso, dashboard básico y lista de pacientes.

**Falta:** entidad `Consultorio` y aislamiento por tenant; roles `NUTRIOLOGO` y `RECEPCION`; bloqueo por
intentos fallidos, expiración de sesión y recuperación de contraseña; antecedentes, catálogo de alergias,
mediciones extendidas (masa muscular, grasa visceral) y notas en 7 apartados; gráficas y comparativo;
adjuntos; configuración de horarios y Google Calendar; reservación pública; cálculo dietético completo
(5 fórmulas + macros); planes con IA sobre SMAE; asistente de WhatsApp; panel completo; portal del paciente.

## 4. Modelo de datos objetivo (paquete `com.nutricion.entidades`)

Entidades nuevas o modificadas. Las marcadas con 🏢 llevan `@ManyToOne Consultorio consultorio`.

| Entidad | Campos clave | Historias |
|---------|--------------|-----------|
| `Consultorio` | nombre, teléfono, email, dirección (para confirmaciones), slug público, activo, fechaRegistro | HU-01, HU-05 |
| `Usuario` 🏢 | + `rol` (ADMIN / NUTRIOLOGO / RECEPCION), `activo`, `intentosFallidos`, `bloqueadoHasta`, `googleRefreshToken` | HU-02, HU-03, HU-16 |
| `TokenRecuperacion` | usuario, token aleatorio, expira, usado | HU-04 |
| `Paciente` 🏢 | + antecedentes heredofamiliares y personales, `codigoAcceso` único, `whatsapp`, `List<Alergia>` + `alergiasOtras` (texto libre) | HU-06, HU-07, HU-42 |
| `Alergia` | catálogo: nombre, tipo (ALERGIA / INTOLERANCIA), grupo de alimento | HU-07, HU-26 |
| `Consulta` 🏢 | + `grasaVisceral`, `cerrada` (boolean), relación 1:1 con `NotaClinica` | HU-08, HU-10 |
| `NotaClinica` | los **7 apartados** como columnas `@Lob` independientes (confirmar nombres con el nutriólogo; propuesta: motivo, historia dietética, actividad física, signos y síntomas, diagnóstico nutricio, intervención, monitoreo). Solo lectura si `consulta.cerrada` | HU-10 |
| `ArchivoAdjunto` 🏢 | paciente, categoría (`LABORATORIO`, `ESTUDIO`, `FOTO_PROGRESO`), nombreOriginal, rutaDisco, mimeType, tamaño, fechaCarga | HU-12 – HU-14 |
| `HorarioLaboral` 🏢 | díaSemana, horaInicio, horaFin (varios bloques por día) | HU-15 |
| `ConfiguracionAgenda` 🏢 | duracionConsultaMin, anticipacionMinimaHoras, minutosRecordatorioNutriologo, minutosExpiracionApartado, diasPacienteActivo | HU-15, HU-18, HU-31, HU-37 |
| `BloqueoCalendario` 🏢 | inicio, fin, googleEventId, origen (GOOGLE / MANUAL) | HU-16 |
| `Cita` 🏢 | + `googleEventId`, `origen` (PANEL / PORTAL / WHATSAPP), `expiraApartado`; agregar `APARTADA` y `CONFIRMADA` a `EstadoCita` | HU-17, HU-20, HU-31, HU-32 |
| `Pago` 🏢 | paciente, consulta, monto, método, estado, fecha (complementa los campos de pago de `Consulta`) | HU-38, HU-39 |
| `PlanTratamiento` | paciente, objetivo (`PERDIDA`, `MANTENIMIENTO`, `GANANCIA`), fórmula, factorActividad, GEB, GET, metaKcal, % y gramos de proteínas / lípidos / HC, fecha | HU-22 – HU-24 |
| `GrupoEquivalente` / `AlimentoSmae` | catálogo **SMAE**: grupo (verduras, frutas, cereales sin/con grasa, leguminosas, AOA muy bajo/bajo/moderado/alto aporte de grasa, leche, aceites y grasas, azúcares…), kcal/prot/líp/HC por equivalente, alimento, porción, unidad, `regionalOaxaca`, alérgenos | HU-25, HU-26, HU-29 |
| `PlanAlimenticio` | paciente, planTratamiento, estado (`BORRADOR`, `APROBADO`), fechaAprobacion | HU-25 – HU-28 |
| `TiempoComida` / `PorcionPlan` | tiempo (desayuno, colación, comida, colación, cena) → porciones en equivalentes SMAE y alimentos sugeridos | HU-25 |
| `ConversacionWhatsapp` 🏢 | paciente (o teléfono si aún no existe), estado (`BOT`, `REQUIERE_HUMANO`, `ATENDIDA`), motivoDerivacion, fechas del último mensaje entrante/saliente | HU-30 – HU-36, HU-40 |
| `MensajeWhatsapp` | conversación, dirección (ENTRANTE / SALIENTE), texto, fecha, waMessageId | HU-30 |
| `Recordatorio` | cita, tipo (`H24`, `H1`, `NUTRIOLOGO`), programadoPara, enviado | HU-18, HU-33 |

## 5. Componentes por épica (cómo implementarlo en este stack)

| Épica | EJB (`com.nutricion.servicios`) | WAR (beans / vistas / REST) |
|-------|-------------------------------|-------------------------------|
| E1 Autenticación y multiconsultorio | `ConsultorioFacade`, `UsuarioFacade` (bloqueo, recuperación); las fachadas reciben el consultorio de la sesión y filtran por él | `registro.xhtml` (público), `recuperar.xhtml`, `vistas/usuarios/*.xhtml`; `FiltroAcceso` valida rol por ruta; `<session-timeout>` en `web.xml` |
| E2 Expediente | `PacienteFacade`, `ConsultaFacade`, `AlergiaFacade`; `CalculosNutricion` (IMC, rangos fisiológicos) | `vistas/pacientes/expediente.xhtml` (`p:tabView`: datos, antecedentes, alergias, consultas, gráficas), `vistas/consultas/captura.xhtml` |
| E3 Adjuntos | `ArchivoFacade`, `AlmacenArchivos` (ruta en disco configurable) | Pestaña "Archivos" con `p:fileUpload` (PDF/JPG/PNG, tamaño máximo), filtro por categoría, línea de tiempo de fotos y comparación lado a lado; servlet `/archivo` que valida consultorio antes de servir |
| E4 Agenda + Google Calendar | `DisponibilidadService` (huecos = horario laboral − citas − bloqueos − anticipación), `GoogleCalendarClient` (JAX-RS + OAuth2), `SincronizadorCalendario` (`@Singleton` + `@Schedule` cada 5 min) | `vistas/agenda/agenda.xhtml` con `p:schedule`, `vistas/agenda/configuracion.xhtml`; callback OAuth `/api/google/callback` |
| E5 Reservación en línea | Reutiliza `DisponibilidadService`; `ReservaService` (bloqueo inmediato verificando disponibilidad dentro de la misma transacción, `@Version` en `Cita`); `NotificacionService` (JavaMail / WhatsApp) | `reservar.xhtml` público sin cuenta, por consultorio: `/reservar.xhtml?c=<slug>` |
| E6 Cálculo dietético | `CalculosNutricion`: 5 fórmulas, factor de actividad, ajuste por objetivo, distribución de macros; `PlanTratamientoFacade` | `vistas/pacientes/calculo.xhtml`; resultados inmediatos con `p:ajax` |
| E7 Planes con IA (SMAE) | `CatalogoSmae` (carga inicial en `InicioDatos`), `IaClient` (JAX-RS al proveedor de LLM configurado), `GeneradorPlanes` (arma el prompt con meta kcal, macros, equivalentes, alergias y preferencia regional; **valida** la respuesta contra SMAE y alergias antes de mostrarla) | `vistas/planes/editor.xhtml`: generar, caja de "ajuste en lenguaje natural", aprobar |
| E8 WhatsApp con IA | `WhatsappClient` (WhatsApp Business Cloud API vía JAX-RS), `AsistenteWhatsapp` (intenciones: agendar, reagendar, cancelar, confirmar, clínico), `DetectorTemasClinicos` (palabras clave + clasificación IA), `ProgramadorRecordatorios` (`@Schedule` cada minuto: 24 h, 1 h, liberar apartados vencidos) | `WebhookWhatsapp` JAX-RS `GET/POST /api/whatsapp/webhook` (verificación de token y firma) |
| E9 Panel de indicadores | `IndicadoresService` (consultas agregadas JPQL por consultorio) | `vistas/dashboard.xhtml` ampliado: citas del día, pacientes activos, ingresos mes vs. anterior, pagos pendientes, conversaciones sin responder, adherencia; "tiempo real" con `p:poll` |
| E10 Portal del paciente | `PortalPacienteService` (busca por `codigoAcceso`, solo datos propios) | `portal/acceso.xhtml` (código, sin contraseña), `portal/inicio.xhtml` (próxima cita: confirmar/cancelar; gráfica de peso). **Nunca** mostrar notas clínicas. Filtro propio `FiltroPortal` para `/portal/*` |

## 6. Historias de usuario y criterios de aceptación

Formato: *Como rol, quiero funcionalidad, para beneficio.* Prioridad MoSCoW y puntos.

### E1 — Autenticación, perfiles y aislamiento multiconsultorio (Fase 2)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-01 | Como administrador, quiero registrar la cuenta de OSMO en la plataforma, para operar aislado de otros consultorios. | Solicita nombre del consultorio, datos de contacto y credenciales del administrador. · Al finalizar se crea un espacio de datos exclusivo (tenant). · No es posible acceder a información de otro consultorio con esas credenciales. | Debe | 5 |
| HU-02 | Como nutriólogo, quiero iniciar sesión con correo y contraseña, para acceder de forma segura. | Rechaza credenciales inválidas con mensaje claro. · Tras **3 intentos fallidos**, bloqueo temporal de la cuenta. · La sesión expira tras un periodo de inactividad. | Debe | 3 |
| HU-03 | Como administrador, quiero crear cuentas de recepción con permisos limitados, para delegar el agendado. | Crear, editar y desactivar cuentas de recepción. · Recepción **no** accede al expediente ni a los planes. · Recepción **sí** ve y modifica la agenda. | Debe | 5 |
| HU-04 | Como usuario, quiero recuperar mi contraseña mediante un enlace enviado a mi correo. | El enlace expira tras un tiempo limitado. · La nueva contraseña cumple la política mínima de seguridad. | Debería | 3 |
| HU-05 | Como administrador, quiero que la información de mi consultorio esté aislada de otros consultorios. | **Toda** consulta a BD se filtra por el identificador del consultorio. · Una prueba de intrusión básica confirma que no se accede a datos de otro consultorio manipulando identificadores (p. ej. `?id=` en la URL). | Debe | 8 |

### E2 — Expediente clínico digital y composición corporal (Sprints 1–2)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-06 | Como nutriólogo, quiero registrar datos personales y antecedentes heredofamiliares y personales de un paciente nuevo. | Valida obligatorios (nombre, fecha de nacimiento, contacto). · Los antecedentes se guardan asociados permanentemente al expediente. | Debe | 5 |
| HU-07 | Como nutriólogo, quiero registrar alergias e intolerancias, para que se respeten al generar planes. | Se seleccionan de un **catálogo** y admiten texto libre. · El módulo de planes (E7) puede consultarlas. | Debe | 3 |
| HU-08 | Como nutriólogo, quiero capturar en cada consulta peso, talla, IMC, % grasa, masa muscular, grasa visceral y % agua. | IMC automático a partir de peso y talla. · Cada medición queda fechada y asociada a una consulta. · No se guardan valores fuera de rangos fisiológicamente posibles. | Debe | 8 |
| HU-09 | Como nutriólogo, quiero ver la evolución del paciente en gráficas por indicador. | Una gráfica por indicador (peso, IMC, % grasa, masa muscular, etc.). · Filtro por rango de fechas. | Debe | 5 |
| HU-10 | Como nutriólogo, quiero notas clínicas en los **siete apartados** definidos por el consultorio. | Los 7 apartados son secciones independientes. · Asociadas a la consulta y de **solo lectura** una vez cerrada. | Debe | 5 |
| HU-11 | Como nutriólogo, quiero comparar automáticamente la consulta actual contra la anterior. | Diferencia numérica y % de cambio por indicador. · Resaltado visual si el cambio va a favor o en contra del objetivo. | Debería | 3 |

### E3 — Gestión de archivos adjuntos (Sprint 2)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-12 | Como nutriólogo, quiero subir estudios de laboratorio en PDF o imagen al expediente. | Acepta PDF, JPG y PNG hasta un tamaño máximo definido. · Vinculado al expediente y a la fecha de carga. | Debe | 3 |
| HU-13 | Como nutriólogo, quiero clasificar adjuntos por categoría (laboratorios, estudios, fotografías de progreso). | Categoría asignada al subir. · El expediente filtra por categoría. | Debería | 3 |
| HU-14 | Como nutriólogo, quiero ver en una línea de tiempo las fotografías de progreso. | Orden cronológico. · Comparar dos fotografías de fechas distintas lado a lado. | Podría | 3 |

### E4 — Agenda y sincronización con Google Calendar (Sprint 1)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-15 | Como nutriólogo, quiero configurar días laborales, horario, duración de consulta y anticipación mínima. | Horarios distintos por día de la semana. · El cambio se refleja de inmediato en los horarios ofrecidos al paciente. | Debe | 5 |
| HU-16 | Como nutriólogo, quiero sincronizar la agenda con mi Google Calendar de forma bidireccional. | Un evento de Google bloquea ese horario en el sistema. · Una cita del sistema aparece como evento en Google. · Sin intervención manual (timer). | Debe | 8 |
| HU-17 | Como recepción, quiero ver y modificar la agenda del día, para reprogramar o cancelar citas. | Muestra citas del día con estatus (confirmada, pendiente, cancelada). · Reprogramar valida que el nuevo horario esté disponible. | Debe | 5 |
| HU-18 | Como nutriólogo, quiero un recordatorio antes de cada consulta. | Margen configurable antes de la cita. · Incluye nombre del paciente y motivo de consulta. | Debería | 2 |

### E5 — Reservación en línea, portal público (Sprint 2)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-19 | Como paciente, quiero consultar los horarios disponibles desde un enlace público. | Solo horarios realmente libres (considera Google Calendar). · No requiere crear cuenta. | Debe | 5 |
| HU-20 | Como paciente, quiero reservar una cita en línea eligiendo día y hora. | Al confirmar, el horario queda bloqueado de inmediato para otros. · El paciente da nombre y datos de contacto mínimos. | Debe | 5 |
| HU-21 | Como paciente, quiero recibir la confirmación de mi cita. | Por correo o WhatsApp según el dato proporcionado. · Incluye fecha, hora y ubicación del consultorio. | Debe | 2 |

### E6 — Cálculo dietético (Sprint 3)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-22 | Como nutriólogo, quiero calcular el gasto energético basal y total con Harris-Benedict, Mifflin-St Jeor, Katch-McArdle, Cunningham y FAO/OMS. | Se elige la fórmula y el resultado es inmediato. · Usa los datos antropométricos del expediente (E2). | Debe | 8 |
| HU-23 | Como nutriólogo, quiero definir la meta calórica y el objetivo (pérdida, mantenimiento o ganancia de peso). | Sugiere un ajuste calórico por objetivo, editable. · La meta queda registrada en el plan de tratamiento. | Debe | 5 |
| HU-24 | Como nutriólogo, quiero la distribución automática de macronutrientes según el objetivo. | En gramos y en % del total calórico. · Ajustable manualmente. | Debe | 5 |

Referencia de fórmulas para `CalculosNutricion` (P = peso kg, T = talla cm, E = edad años, MLG = masa libre de grasa kg):

- **Harris-Benedict:** hombre 66.47 + 13.75·P + 5.003·T − 6.755·E · mujer 655.1 + 9.563·P + 1.850·T − 4.676·E
- **Mifflin-St Jeor:** 10·P + 6.25·T − 5·E + 5 (hombre) / − 161 (mujer)
- **Katch-McArdle:** 370 + 21.6·MLG, con MLG = P·(1 − %grasa/100)
- **Cunningham:** 500 + 22·MLG
- **FAO/OMS:** 18–30 años: H 15.3·P + 679, M 14.7·P + 496 · 30–60: H 11.6·P + 879, M 8.7·P + 829 · >60: H 13.5·P + 487, M 10.5·P + 596
- **GET = GEB × factor de actividad** (sedentario 1.2, ligero 1.375, moderado 1.55, intenso 1.725, muy intenso 1.9).
- Katch-McArdle y Cunningham requieren % de grasa; si falta, deshabilitarlas con un mensaje.
- Verificar coeficientes y factores con el nutriólogo antes de cerrar la historia.

### E7 — Planes alimenticios con IA sobre SMAE (Sprints 3–4)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-25 | Como nutriólogo, quiero generar un plan por tiempo de comida basado en el **Sistema Mexicano de Alimentos Equivalentes**. | Respeta meta calórica y macros (E6). · Cada tiempo de comida especifica porciones exactas en equivalentes SMAE. · Se genera en **menos de un minuto**. | Debe | 13 |
| HU-26 | Como nutriólogo, quiero que el plan respete alergias e intolerancias del paciente. | Ningún alimento pertenece al catálogo de alergias/intolerancias del paciente. · Si no hay alternativa suficiente en el SMAE, se avisa al nutriólogo. | Debe | 5 |
| HU-27 | Como nutriólogo, quiero pedir ajustes en lenguaje natural ("sin lácteos", "más variedad en la cena"). | El ajuste mantiene la meta calórica original. · Se pueden pedir varios ajustes sucesivos sobre el mismo borrador. | Debe | 8 |
| HU-28 | Como nutriólogo, quiero revisar y aprobar el plan antes de entregarlo al paciente. | No se entrega sin estatus "aprobado por el nutriólogo". · El borrador se conserva solo durante la edición, sin historial de versiones intermedias. | Debe | 3 |
| HU-29 | Como nutriólogo, quiero que el plan considere platillos e ingredientes de la cocina regional oaxaqueña. | El catálogo incluye ingredientes y preparaciones regionales. · Se puede indicar preferencia por cocina regional al generar. | Debería | 5 |

**Regla de diseño de IA:** el LLM solo *propone*; el sistema calcula las porciones con SMAE y **valida en
Java** (kcal dentro de tolerancia, macros, alérgenos) antes de mostrar el borrador. La llamada al LLM se
hace con cliente JAX-RS a un endpoint configurable (`ia.endpoint`, `ia.apiKey`, `ia.modelo`), pidiendo
respuesta en JSON y parseándola con JSON-B.

### E8 — Asistente de WhatsApp con IA (Sprints 2–4)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-30 | Como paciente, quiero escribir por WhatsApp para agendar una cita. | Reconoce la intención de agendar en lenguaje natural. · Consulta la disponibilidad real antes de ofrecer horarios. | Debe | 8 |
| HU-31 | Como paciente, quiero que solo me ofrezca horarios realmente disponibles. | Consulta la agenda (E4) en tiempo real. · Un horario ofrecido y no confirmado en un lapso determinado se libera automáticamente. | Debe | 5 |
| HU-32 | Como paciente, quiero confirmar mi cita en **dos pasos**, para evitar agendados accidentales. | La cita queda "pendiente" hasta la segunda confirmación. · Si no confirma en el plazo, el horario se libera. | Debe | 3 |
| HU-33 | Como paciente, quiero recordatorios automáticos 24 h y 1 h antes de mi cita. | El de 24 h permite confirmar o cancelar desde WhatsApp. · El de 1 h se envía solo si la cita sigue confirmada. | Debe | 3 |
| HU-34 | Como paciente, quiero reagendar o cancelar mi cita por WhatsApp. | Ofrece nuevos horarios disponibles al reagendar. · La cancelación libera el horario de inmediato. | Debería | 5 |
| HU-35 | Como nutriólogo, quiero que el asistente derive a un humano cualquier consulta clínica o asunto delicado. | Identifica palabras clave y patrones clínicos o delicados. · Informa al paciente que el consultorio lo contactará y notifica al nutriólogo. | Debe | 5 |
| HU-36 | Como nutriólogo, quiero un aviso cuando una conversación requiera mi atención. | Se muestra en el panel (E9) y opcionalmente por notificación push. · Indica el paciente y el motivo de derivación. | Debería | 3 |

### E9 — Panel de indicadores del consultorio (Sprints 3–4)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-37 | Como nutriólogo, quiero ver las citas del día y los pacientes activos. | Se actualiza en tiempo real conforme cambian las citas. · "Paciente activo" = tuvo consulta dentro de un periodo configurable. | Debe | 3 |
| HU-38 | Como nutriólogo, quiero ver los ingresos del mes y su comparativo contra el anterior. | Variación porcentual respecto al mes anterior. · Se calculan a partir de los pagos registrados. | Debe | 5 |
| HU-39 | Como nutriólogo, quiero ver los pagos pendientes por paciente. | Filtro por paciente y por rango de fechas. · Un pago liquidado desaparece del listado. | Debería | 3 |
| HU-40 | Como nutriólogo, quiero ver las conversaciones de WhatsApp sin responder. | Ordenadas por tiempo de espera (más antiguas primero). · Al atenderla se retira del listado. | Debería | 3 |
| HU-41 | Como nutriólogo, quiero un resumen de adherencia al tratamiento por paciente. | Se estima por asistencia a consultas y cumplimiento de citas de seguimiento. · Los pacientes con baja adherencia se resaltan. | Podría | 5 |

### E10 — Portal del paciente (Sprint 4)

| ID | Historia | Criterios de aceptación | Prio | Pts |
|----|----------|-------------------------|------|-----|
| HU-42 | Como paciente, quiero acceder a un portal con un **código único** sin contraseña. | El código se entrega al registrarse en el consultorio. · Da acceso únicamente a la información del propio paciente. | Debe | 5 |
| HU-43 | Como paciente, quiero consultar mi próxima cita para confirmarla o cancelarla. | Muestra fecha, hora y ubicación. · Confirmar o cancelar actualiza la agenda del consultorio de inmediato. | Debe | 3 |
| HU-44 | Como paciente, quiero ver la evolución de mi peso y mis medidas. | Gráfica simplificada de peso. · **No** muestra información clínica sensible (notas clínicas). | Debería | 3 |

## 7. Historias técnicas

| ID | Fase | Historia | Criterios de aceptación | Pts |
|----|------|----------|-------------------------|-----|
| HT-01 | 1 | Entrevistas y observación directa del proceso de atención de OSMO. | Se documentan los 5 procesos sustantivos. · Hallazgos validados con el nutriólogo titular. | 5 |
| HT-02 | 1 | Redactar y priorizar la lista maestra de historias de usuario. | Cada HU con criterios de aceptación y prioridad MoSCoW. · Backlog validado con el asesor externo. | 5 |
| HT-03 | 2 | Arquitectura y modelo de BD con aislamiento por consultorio. | Identificador de consultorio en las entidades relevantes. · Se documentan los servicios externos (IA, WhatsApp Business, Google Calendar). | 8 |
| HT-04 | 2 | Prototipos navegables de las pantallas principales. | Cubren expediente, agenda y panel. · Se documentan los ajustes pedidos por el usuario. | 5 |
| HT-05 | 2 | Repositorio, entorno de desarrollo y proceso de despliegue. | Control de versiones con ramas definidas. · Entorno de pruebas accesible. | 5 |
| HT-06 | 4 | Pruebas funcionales, de integración y de aceptación con el nutriólogo. | Casos de prueba y defectos documentados. · El nutriólogo valida expediente, agenda y planes. | 8 |
| HT-07 | 4 | Despliegue en el servidor de producción. | Respaldo inicial de la BD. · Integraciones externas verificadas en producción. | 5 |
| HT-08 | 4 | Documentación técnica y manual de usuario. | Manual para los 4 perfiles (administrador, nutriólogo, recepción, paciente). · Documentación de arquitectura, modelo de datos e integraciones. | 5 |
| HT-09 | 4 | Capacitación al nutriólogo y a recepción. | Cubre los flujos más frecuentes de cada perfil. · Se entrega una guía rápida. | 3 |

## 8. Plan de sprints (orden de construcción)

| Fase / Sprint | Semanas | Fechas | Historias | Pts |
|---------------|---------|--------|-----------|-----|
| Fase 1. Inicio y requerimientos | S1–S2 | 31-ago → 11-sep-2026 | HT-01, HT-02 | 10 |
| Fase 2. Arquitectura y autenticación | S3–S4 | 14-sep → 25-sep-2026 | HT-03, HT-04, HT-05, HU-01 … HU-05 | 42 |
| Sprint 1. Expediente y agenda | S5–S6 | 28-sep → 09-oct-2026 | HU-06, 07, 08, 10, 15, 16, 17, 18 | 41 |
| Sprint 2. Seguimiento y reservación | S7–S8 | 12-oct → 23-oct-2026 | HU-09, 11, 12, 13, 14, 19, 20, 21, 30 | 37 |
| Sprint 3. Cálculo dietético e IA conversacional | S9–S10 | 26-oct → 06-nov-2026 | HU-22 … HU-25, HU-31 … HU-38 | 63 |
| Sprint 4. Personalización, panel y portal | S11–S12 | 09-nov → 20-nov-2026 | HU-26 … HU-29, HU-39 … HU-44 | 43 |
| Fase 4. Pruebas, documentación, capacitación y despliegue | S13–S14 | 23-nov → 04-dic-2026 | HT-06 … HT-09 | 21 |

**Pistas de trabajo:** Residente 1 — núcleo clínico (expediente, gráficas, adjuntos, cálculo dietético).
Residente 2 — agenda, Google Calendar, reservación en línea y panel. Residente 3 — IA (planes y WhatsApp)
y portal del paciente.

**Resumen MoSCoW:** Debe 32 HU / 167 pts (73 %) · Debería 10 HU / 33 pts (23 %) · Podría 2 HU / 8 pts (5 %).

## 9. Definición de terminado (por historia)

1. Cumple **todos** sus criterios de aceptación.
2. Respeta las reglas de la sección 0 (capas, `javax.*`, Ant, español, filtrado por consultorio).
3. Entidades nuevas registradas en `persistence.xml`; datos de ejemplo en `InicioDatos` cuando aplique.
4. **Clean and Build** de `sistema_nutricion` sin errores y despliegue correcto en Payara.
5. Probada manualmente con los roles afectados (ADMIN, NUTRIOLOGO, RECEPCION, paciente).
6. Ningún secreto ni clave de API en el repositorio.

## 10. Supuestos y riesgos

- Depende de terceros (proveedor de IA, WhatsApp Business API, Google Calendar): demoras en cuentas o
  aprobaciones pueden afectar los sprints 2–4. Cada cliente externo va detrás de una interfaz con una
  implementación simulada (`*Simulado`) para desarrollar y probar sin credenciales.
- **Todo plan generado por IA requiere aprobación del nutriólogo** antes de llegar al paciente.
- El asistente de WhatsApp **no** responde temas clínicos: los deriva a un humano (HU-35).
- Los sprints 3 y 4 concentran la mayor incertidumbre técnica (LLM y asistente conversacional).
