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
