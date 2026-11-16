# Preparación del cuestionario: Unidad 4

Ejemplos referidos al código de este proyecto, revisado el 15-11-2026. Estudiar junto con [los modelos y la implementación](UNIDAD_4_REVISION_Y_ENTREGA.md) y las páginas del [material docente](MATERIALES_PROFESOR.md).

## Preguntas con respuestas breves

1. **¿Qué muestra el modelo de datos?** Qué información se conserva, sus tipos, claves y relaciones. Aquí hay Usuario, Paciente, Cita y Consulta; no confundirlo con todas las entidades futuras del README.
2. **¿Qué es una entidad JPA y qué es una tabla?** Una clase `@Entity` representa objetos persistentes; `@Table` indica su tabla. `Paciente` se corresponde con `pacientes`.
3. **¿Qué son PK y FK?** La PK identifica una fila; una FK referencia una fila de otra tabla. `pacientes.ID` es PK y `citas.paciente_id` es FK.
4. **¿Cuál es la cardinalidad Paciente-Cita?** Un paciente puede tener cero o muchas citas; cada cita exige un paciente. Se implementa con `@OneToMany(mappedBy="paciente")` y `@ManyToOne(optional=false)`.
5. **¿Por qué la relación Consulta-Cita es opcional?** Se admiten consultas históricas sin cita. `cita_id` permite NULL y es único, de modo que una cita no tenga dos consultas asociadas.
6. **¿Existe una relación de usuario con consultorio?** Todavía no. Falta la entidad Consultorio y el aislamiento del README; no dibujar una FK ficticia.
7. **¿Dónde se define la conexión?** `persistence.xml` usa `NutricionPU` y `jdbc/nutricion`; Payara resuelve el recurso hacia `NutricionMySQLPool`, que conecta a MySQL. Las credenciales quedan en el servidor.
8. **¿Qué diferencia hay entre pool y recurso JNDI?** El pool administra conexiones físicas reutilizables. El recurso JNDI es el nombre lógico que busca la aplicación y se asocia con un pool.
9. **¿Qué prueba el Ping?** Que el servidor obtiene una conexión del pool. No prueba login, CRUD, relaciones, pantallas ni permisos.
10. **¿Cómo se crean las tablas?** EclipseLink, a partir de las entidades registradas y `create-or-extend-tables`, al desplegar. No es una migración versionada ni borra automáticamente todo lo anterior.
11. **¿Cuál es el recorrido de una petición?** XHTML/JSF → bean WAR → fachada EJB → EntityManager/JPA → MySQL. El bean web no hace SQL ni administra directamente EntityManager.
12. **¿Qué representa el modelo de comportamiento?** Acciones, decisiones, cambios de estado e interacciones en respuesta a eventos. Ejemplo: Guardar paciente puede terminar en error JSF, error de negocio o registro persistido.
13. **¿Para qué sirve un diagrama de actividades?** Para explicar el flujo y sus decisiones. Ejemplo: capturar → validar → guardar o corregir. Un rombo representa una condición, no una tabla.
14. **¿Qué muestra un diagrama de estados?** Los estados y transiciones disparadas por eventos. Los estados Captura/Error/Listado del documento son de la interfaz; Paciente no tiene una columna `estado`.
15. **¿Qué muestra un diagrama de secuencia?** Quién llama a quién y en qué orden temporal. En el alta, `lista.xhtml` llama a `PacienteBean.guardar`, este a `PacienteFacade.guardar` y la fachada usa EntityManager.
16. **¿Qué es una historia de usuario?** Una necesidad de un actor con un beneficio y criterios verificables. HU-06 incluye datos y antecedentes; un CRUD básico sin antecedentes es un avance parcial.
17. **¿Cómo se comprueba la trazabilidad?** Elegir una historia y localizar sus criterios, entidad/tabla, modelo, pantalla, método y prueba. Si falta una pieza, no declararla terminada.
18. **¿Qué es Facelets?** El sistema de vistas y plantillas XHTML de JSF. `ui:insert` reserva zonas; `ui:composition` selecciona plantilla y `ui:define` aporta contenido.
19. **¿Por qué hay dos plantillas?** La pública organiza Inicio/Acceso; la privada añade menú y sesión a Panel/Pacientes. Un cambio común de cabecera se hace en la plantilla correspondiente.
20. **¿Para qué sirve `required`?** Para rechazar una entrada vacía. No comprueba por sí solo formato, duplicados o una fecha futura.
21. **¿Qué hacen `f:validateRegex` y `f:validateLength`?** Validan patrón y longitud en JSF del servidor. La fachada vuelve a comprobar las reglas antes de persistir; MySQL aplica sus restricciones.
22. **¿Por qué validar también en la fachada?** Porque puede ser invocada desde distintos clientes y debe proteger reglas de negocio sin depender de una pantalla. Un `maxlength` HTML puede eludirse.
23. **¿Cuándo se ejecuta la acción de Guardar?** Después de conversión/validación y actualización de modelo en el ciclo normal de JSF. Si la validación falla, la acción no se ejecuta y se muestran mensajes.
24. **¿Qué son `process` y `update` en AJAX?** `process` indica qué componentes procesar en la petición; `update` cuáles volver a renderizar al responder. Actualizar una tabla no equivale por sí solo a guardar en la BD.
25. **¿Por qué usar `p:resetInput`?** Un componente JSF puede conservar el valor local inválido aunque el bean ya tenga un objeto nuevo. Resetear Nuevo/Editar evita mostrar datos cancelados del formulario anterior.
26. **¿Cómo se impiden duplicados?** Normalización, consultas parametrizadas por email/teléfono excluyendo el propio ID y restricciones UNIQUE en MySQL. La prueba realizada es secuencial, no de concurrencia.
27. **¿Cómo se protege una vista privada?** `FiltroAcceso` comprueba el atributo de sesión y redirige al acceso si falta. Esto no implementa aún la matriz de roles del objetivo.
28. **¿Qué pasa al salir?** `LoginBean.salir` invalida la sesión. Al intentar otra vez `/vistas/*`, el filtro exige iniciar sesión.
29. **¿Qué diferencia hay entre persist y merge?** `persist` introduce una entidad nueva; `merge` copia el estado al contexto de persistencia y devuelve la entidad administrada. La transacción sincroniza con la BD.
30. **¿Qué significa rollback?** Revertir la transacción. `ReglaNegocioException` tiene `@ApplicationException(rollback=true)`; el mensaje se presenta sin mostrar detalles internos de una excepción inesperada.
31. **¿La edad se guarda?** No. Se calcula desde `fecha_nacimiento`. El IMC sí tiene columna y se recalcula al persistir/actualizar Consulta.
32. **¿Borrar un paciente hace CASCADE en SQL?** El código usa cascada de eliminación en JPA hacia citas/consultas. No es lo mismo que declarar `ON DELETE CASCADE` en MySQL; revisar `SHOW CREATE TABLE`.
33. **¿Se necesitan iText y POI para usar JSF?** No. Según el manual de PrimeFaces 5, p. 12 y 128-130, son opcionales para exportar PDF y XLS. No requieren un programa instalado mediante doble clic.
34. **¿Compilar o ver “deploy successful” prueba que funciona?** No. En esta revisión se reprodujo un despliegue anunciado como correcto con HTTP 404. Hay que abrir las vistas y probar acceso y persistencia.
35. **¿Qué falta para declarar acabado el sistema completo?** Cumplir todos los criterios del README, roles/aislamiento, módulos restantes, pruebas funcionales y aceptación real. Las muestras y servicios sin interfaz no sustituyen esos criterios.

## Ejercicio práctico para ensayar

**Enunciado:** un administrador registra un paciente ficticio. Si el correo ya existe o la fecha de nacimiento es futura, el sistema debe rechazarlo. Si es válido, debe persistir y poder verse después de cerrar sesión y entrar de nuevo.

1. Identificar actor, precondiciones y resultado esperado.
2. Dibujar Paciente con su PK y restricciones únicas; ubicar la tabla real.
3. Dibujar la actividad con ramas válida, duplicado y fecha futura.
4. Dibujar Usuario → XHTML/JSF → PacienteBean → PacienteFacade → EntityManager → MySQL.
5. Ubicar los validadores en `vistas/pacientes/lista.xhtml` y `PacienteFacade.validar`.
6. Ejecutar los casos usando exclusivamente un correo `u4.qa.<marca>@example.invalid` y teléfono ficticio único.
7. Consultar la fila ficticia, recargar, editar y volver a entrar. Eliminar únicamente la prueba al terminar.
8. Explicar por qué esto cubre el registro básico de HU-06 pero **no** sus antecedentes ni el resto del sistema.

## Frase útil para exponer

«Este es el modelo del avance que voy a demostrar. Esta entidad corresponde a esta tabla; este paso del diagrama llama a este método; esta validación produce este mensaje y evita guardar. Aquí están los criterios cubiertos y aquí los pendientes de la historia completa».
