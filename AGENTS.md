# Instrucciones del proyecto sistema_nutricion

## Al comenzar una conversación o tarea

- Leer `CONTINUIDAD_UNIDAD_4.md` y `README.md` antes de explorar o modificar el proyecto. Consultar después los documentos enlazados que correspondan a la tarea, sin pedir al usuario que repita sus rutas.
- Para preguntas sobre el material del profesor, empezar por `docs/MATERIALES_PROFESOR.md` y la caché local indicada allí. No volver a extraer todos los PDF si el índice o las páginas ya disponibles resuelven la consulta.
- Revisar las skills y plugins disponibles y aplicar los pertinentes; leer sus instrucciones y explicar brevemente su propósito. No pedir al usuario que elija herramientas ni usar todas por estar instaladas.
- Verificar HEAD, cambios locales, procesos y servidor antes de asumir que el estado de una revisión anterior sigue vigente. Conservar los cambios del usuario.

## Contrato de implementación y evidencia

- Mantener Java EE 8 con `javax.*`, NetBeans/Ant, JDK 17, Payara 5.2022.3, JSF 2.3 y PrimeFaces 5.0. El README contiene los requisitos funcionales objetivo.
- Ejecutar la aplicación empresarial raíz `sistema_nutricion` (EAR), que integra los módulos EJB y WAR. Las entradas bajo Java EE Modules y los proyectos abiertos separados representan los mismos módulos.
- Mantener alineadas historias, modelos, validadores, código y pruebas. Diferenciar el avance básico probado de las historias completas pendientes.
- Actualizar la documentación de continuidad cuando cambie el estado del proyecto o aparezca evidencia relevante. No registrar secretos ni datos clínicos personales.
- Actualizar el mismo informe de Word en Descargas después de cada avance del proyecto. Consultar su ruta en CONTINUIDAD_UNIDAD_4.md, añadir el cambio fechado con sus pruebas antes de «Resumen para exponer» y actualizar ese resumen al final.
- Para QA, usar únicamente registros ficticios identificables y limpiar solo los creados por la prueba. No borrar muestras existentes, dominio, configuración JDBC ni despliegues activos para repetir una demostración.
- Separar compilación, despliegue, conexión JDBC y recorrido funcional en navegador: cada comprobación aporta evidencia distinta.
