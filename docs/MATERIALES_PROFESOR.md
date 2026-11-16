# Material del profesor: índice y requisitos

Revisión: 15 de noviembre de 2026. Carpeta original: `C:\Users\yaxti_cm5rg3t\Documents\MI LAP 2\ESCUELA\NOVENO SEMESTRE\ENFOQUE AGIL\UNIDAD 4\ARCHIVOS`.

## Qué se revisó y cómo volver a encontrarlo

Hay **6 archivos: 4 PDF (1201 páginas en total) y 2 JAR**. Se inventariaron todos, se extrajo el texto disponible de los cuatro PDF, se aplicó OCR local a las 72 páginas del PDF de ingeniería y se consultaron las secciones pertinentes a la entrega. Se inspeccionaron los manifiestos y contenidos de los JAR sin ejecutarlos. Esto es una revisión de pertinencia y requisitos; no una lectura visual exhaustiva de las 1201 páginas ni una auditoría completa de las bibliotecas.

Los originales permanecen intactos. El texto extraído, el inventario con SHA-256 y las imágenes están en `../.cache/materiales-profesor/` dentro del proyecto. Es una caché local excluida de Git, no una dependencia de la aplicación. No subir libros o extractos completos al repositorio.

Todas las referencias siguientes indican **página del PDF, contando desde 1**, que puede diferir de la numeración impresa.

| Archivo dentro de ARCHIVOS | Contenido / utilidad | Relación con la entrega |
|---|---|---|
| `Head First Web Design - PDFDrive.com - head_first_web_design.pdf` | 553 páginas. Diseño centrado en usuarios, organización, navegación, diseño visual, accesibilidad y pruebas de uso. | Justificar las pantallas y las plantillas; preparar su navegación y presentación. |
| `Microsoft Word - configuracion servidor.docx - configuracion del servidor.pdf` | 3 páginas. Conexión de BD, servidor, pool JDBC, Ping y recurso JNDI. Es un PDF aunque el nombre mencione DOCX. | Demostrar que el modelo de datos llega a la BD mediante Payara. |
| `primefaces_user_guide_5_0.pdf` | 573 páginas. Manual de PrimeFaces **5.0**, la versión del JAR del proyecto. | Consultar atributos y ejemplos compatibles, validadores, tablas, mensajes y exportaciones. |
| `web engineering.pdf - web Engineering.pdf` | 72 páginas. Selección de un libro de ingeniería web: introducción, modelos, arquitectura y diseño. Es un PDF compuesto por imágenes, no el libro completo. | Base conceptual para conectar modelos de datos, comportamiento, navegación y presentación con la implementación. |
| `itext-2.1.7-20261115/itext-2.1.7.jar` | Biblioteca Java con clases `com.lowagie.*`, para producir PDF. | Opcional si se añade exportación PDF mediante `p:dataExporter`. |
| `itext-2.1.7-20261115/poi-3.16.jar` | Apache POI, manifiesto confirma **3.16**, para formatos de Microsoft Office. | Opcional para exportación Excel XLS; no equivale por sí sola al conjunto de componentes para XLSX. |

## Guía corta de configuración del servidor

Leer sus **3 páginas completas**, incluidas las capturas:

1. Obtener las propiedades de la conexión de la BD en NetBeans.
2. Iniciar Payara y abrir `http://localhost:4848/`.
3. Crear el pool en **Resources > JDBC > JDBC Connection Pools**.
4. Configurar el controlador y las propiedades de conexión y comprobar **Ping**.
5. Crear **JDBC Resources**, indicar el nombre JNDI y asociar el pool.

Los nombres del ejemplo del profesor son de otra base. Para este proyecto se usan **`NutricionMySQLPool`**, **`jdbc/nutricion`** y **`sistema_nutricion`**, según el README. No copiar sus credenciales ni reemplazar la configuración local funcional. El proyecto ya exige SSL mediante Connector/J; conservarlo aunque la captura histórica muestre otra configuración.

## PrimeFaces: páginas para consultar

| Tema | Páginas PDF | Qué buscar |
|---|---|---|
| Incorporar PrimeFaces | 11 | Un único JAR de PrimeFaces; Maven es una alternativa del manual, no una obligación. El proyecto usa Ant. |
| Dependencias obligatorias y opcionales | **12** | iText 2.1.7 para PDF y POI para Excel son opcionales. El manual prueba POI 3.7; el archivo del profesor es 3.16. |
| Configuración | 13 | Tema, carga de archivos y opciones; no habilitar todas automáticamente. |
| AJAX | 18-19 | Diferencia entre `process`, `update`, `listener` y `oncomplete`. |
| Calendar | 44-52 | Fecha, patrón, idioma y selección. |
| CommandButton | 105-109 | `action`, AJAX y componentes procesados/actualizados. |
| DataExporter | **128-130** | `type="pdf"`/`"xls"`, JAR en classpath, `ajax="false"`, `target`, columnas excluidas. |
| Dialog | 172-176 | Formularios modales y apertura/cierre. |
| ResetInput | 365-366 | Restablecer valores locales después de cancelar o de una validación fallida. |
| Validación y transformación de metadatos | Capítulo 11; localizar desde el índice y p. 563 | Distinguir validación JSF del servidor, Bean Validation y validación opcional del navegador. |
| Idioma de componentes | 564 y siguientes | Calendario en español y mensajes. |

La extracción original de este PDF separó letras artificialmente. Para búsquedas automáticas fiables usar `extract_text(extraction_mode="layout")` con pypdf; también hay extractos legibles por página en la caché. La página 12 se verificó por extracción de tabla; el renderizador de Windows se quedó esperando al cargar este PDF y se detuvo ese proceso de análisis. Eso no afecta al proyecto ni invalida el contenido extraído.

## Ingeniería web: referencias centrales para los modelos

| Páginas PDF | Página impresa / figura | Contenido |
|---|---|---|
| 2-14 | 5-17 | Categorías y características de aplicaciones web. |
| **15** | 44, figuras **3-3 y 3-4** | Casos de uso y diagrama de actividades de un proceso de envío. |
| **16-17** | 45-46, figuras **3-5 y 3-6** | Modelo de contenido: clases/datos y comportamiento mediante estados. |
| **18-21** | 47-50 | Modelo de hipertexto o navegación; enlaces, índices y recorridos. |
| **22-23** | 51-52, figura **3-9** | Modelo de presentación; páginas, unidades y elementos repetidos. Relacionarlo con las plantillas. |
| **24** | 53, figuras **3-10 y 3-11** | Vista general de interacción y secuencia para obtener una lista. |
| **25** | 54, figura **3-12** | Secuencia para mostrar un elemento seleccionado. |
| 26-29 | 55-58 | Personalización y relación entre los modelos. |
| **30-35** | 59-64 | Métodos y herramientas: WebML, OOHDM, UWE, WebRatio, VisualWADE y OpenUWE. Son ejemplos explicativos. |
| 36-55 | 65-84 | Arquitecturas, capas, MVC y datos. |
| 56-72 | Selección de 85 y 95-110 | Diseño condicionado por tecnología, presentación, interacción, URL, funciones y servicios. Hay saltos de páginas en la selección. |

El OCR usó el idioma disponible `es-MX`, aunque el libro esté en inglés. Puede confundir palabras y etiquetas de diagramas; comprobar en la imagen cualquier cita literal, flecha o símbolo. Las 72 imágenes completas están en `.cache/materiales-profesor/web-engineering-ocr/pagina-NNN.png` y el OCR por página en `ocr.json`.

## Head First Web Design: mapa del libro

| Capítulo | Inicio, página PDF | Para qué sirve aquí |
|---|---:|---|
| 1. Building Beautiful Web Pages | 32 | Usuarios, objetivos y estructura del sitio. |
| 2. Pre-Production | 72 | Bocetos y storyboard antes de programar; no exige software de pago. |
| 3. Organizing Your Site | 103 | Agrupación de información y arquitectura de navegación. |
| 4. Layout and Design | 142 | Distribución visual según tareas del usuario. |
| 5. Designing With Color | 195 | Uso consistente del color. |
| 6. Smart Navigation | 230 | Nombres de enlaces y orientación. |
| 7. Writing for the Web | 265 | Texto fácil de explorar y mensajes comprensibles. |
| 8. Accessibility | 310 | Formularios etiquetados, contraste y alternativas textuales. |
| 9. Listen to Your Users | 355 | Encuestas y pruebas de usabilidad; no confundirlas con compilar. |
| 10. Evolutionary Design | 403 | Mejoras iterativas y adaptación. |
| 11. The Business of Web Design | 443 | Alcance, clientes y entrega. |

El libro menciona herramientas y tecnologías como Photoshop, Dreamweaver, WordPress o ejemplos descargables. Esas menciones no constituyen una instrucción del profesor para instalarlas ni cambian el stack del README.

## ¿Hay que instalar algo?

**Para el avance actual no se encontró una instalación adicional necesaria.** La carpeta contiene bibliografía, una guía de configuración y dos bibliotecas opcionales; no contiene un instalador ni una rúbrica que ordene adoptar las herramientas citadas por los libros.

| Componente | Estado comprobado en este equipo | Acción |
|---|---|---|
| NetBeans 25 y Ant | `C:\Program Files\NetBeans-25\netbeans`; Ant 1.10.14 disponible en su distribución. | Usar lo existente. |
| JDK 17 | `C:\Users\yaxti_cm5rg3t\AppData\Local\Programs\EclipseAdoptium\jdk-17.0.21+9`. | Seleccionarlo para Payara y la compilación. `java` del PATH apunta a JDK 23; no asumir que es el del proyecto. |
| Payara 5.2022.3 | Accesible mediante `C:\Users\yaxti_cm5rg3t\payara5-nutricion`; dominio `domain1`. | Conservar instalación, dominio y configuración JDBC. |
| MySQL | Servicio `MySQL80`, puerto 3306, motor comprobado 8.0.41. | Ya instalado; no reinstalar ni crear otra base sobre la existente. |
| Connector/J | `glassfish/domains/domain1/lib/mysql-connector-j-8.3.0.jar`. | Ya instalado. El pool respondió al Ping. |
| PrimeFaces 5.0 | `sistema_nutricion-war/lib/primefaces-5.0.jar`. | Ya registrado. |
| iText y POI del profesor | Permanecen en ARCHIVOS; no incorporados al sistema. | Agregarlos al classpath del WAR solo si se acuerda implementar exportaciones. No se instalan mediante doble clic. |
| WebRatio, VisualWADE, OpenUWE, ArgoUML, Photoshop, Dreamweaver | Menciones de la bibliografía. | No requeridos por la evidencia recibida. |
| Herramienta de diagramas | Se observó una instalación de Visual Paradigm 18.1. Los modelos entregados están en Mermaid/Markdown. | No hace falta instalar otra para leer los documentos; confirmar formato de entrega si el profesor pide un archivo UML editable concreto. |

Las bibliotecas son antiguas. Apache declara sin soporte las versiones de POI 4.x y anteriores en su [política oficial de versiones](https://poi.apache.org/versioning.html). Esto justifica no incorporar POI 3.16 por mera presencia en la carpeta, sin probar compatibilidad y necesidad. No se hizo una migración de PrimeFaces/Java EE ni una auditoría integral de CVE.

## Búsqueda local rápida

Desde la raíz del proyecto:

```powershell
Get-ChildItem -LiteralPath '..\ARCHIVOS' -Recurse -File
python scripts\buscar-materiales.py 'sequence'
python scripts\buscar-materiales.py 'DataExporter'
python scripts\buscar-materiales.py 'Presentation Modeling'
```

Para regenerar la extracción nativa: `python scripts\indexar_materiales.py` (requiere el pypdf ya disponible en el Python local; no es dependencia Java). El PDF de ingeniería exige OCR aparte. Usar primero la caché existente y las referencias de esta guía; no repetir la extracción completa para cada pregunta.
