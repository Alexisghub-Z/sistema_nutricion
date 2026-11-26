package com.nutricion.servicios;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.sql.Date;
import javax.persistence.EntityManager;

/** Regresión de las reglas del expediente básico, sin servidor ni base real. */
public class PacienteFacadeTest {
    private static int correctas;
    private static int fallidas;

    private static class Fachada extends PacienteFacade {
        Paciente duplicadoEmail;
        Paciente duplicadoTelefono;
        Paciente persistido;
        int escrituras;

        Fachada() throws Exception {
            EntityManager em = (EntityManager) Proxy.newProxyInstance(
                    EntityManager.class.getClassLoader(), new Class<?>[]{EntityManager.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("persist")) {
                            escrituras++;
                            return null;
                        }
                        if (method.getName().equals("refresh") && args[0] == persistido) {
                            return null;
                        }
                        throw new AssertionError("Operación inesperada: " + method.getName());
                    });
            Field field = PacienteFacade.class.getDeclaredField("em");
            field.setAccessible(true);
            field.set(this, em);
        }

        @Override public Paciente porEmail(String email, Consultorio consultorio) { return duplicadoEmail; }
        @Override public Paciente porTelefono(String telefono, Consultorio consultorio) { return duplicadoTelefono; }
        @Override public Paciente porCodigoAcceso(String codigo) { return null; }
        @Override public Paciente delConsultorio(Long id, Consultorio consultorio) { return persistido; }
    }

    private static Paciente valido() {
        Paciente p = new Paciente();
        p.setNombre("Paciente de prueba");
        p.setEmail("qa@example.invalid");
        p.setTelefono("9511234567");
        p.setFechaNacimiento(Date.valueOf("2000-01-01"));
        p.setConsultorio(new Consultorio());
        return p;
    }

    private static void rechaza(String nombre, java.util.function.Consumer<Paciente> cambio) throws Exception {
        Fachada f = new Fachada();
        Paciente p = valido();
        cambio.accept(p);
        try {
            f.guardar(p);
            throw new AssertionError("Se aceptó un dato inválido");
        } catch (ReglaNegocioException esperado) {
            if (f.escrituras != 0) throw new AssertionError("Escribió antes de validar");
        }
    }

    private interface Caso { void ejecutar() throws Exception; }

    private static void prueba(String nombre, Caso caso) {
        try {
            caso.ejecutar();
            correctas++;
            System.out.println("PASS " + nombre);
        } catch (Throwable e) {
            fallidas++;
            System.out.println("FAIL " + nombre + ": " + e);
        }
    }

    public static void main(String[] args) {
        prueba("paciente nulo", () -> {
            Fachada f = new Fachada();
            try { f.guardar(null); throw new AssertionError("Aceptó nulo"); }
            catch (ReglaNegocioException esperado) { if (f.escrituras != 0) throw new AssertionError(); }
        });
        prueba("nombre vacío", () -> rechaza("nombre", p -> p.setNombre("  ")));
        prueba("nombre nulo", () -> rechaza("nombre", p -> p.setNombre(null)));
        prueba("nombre largo", () -> rechaza("nombre", p -> p.setNombre("x".repeat(121))));
        prueba("correo nulo", () -> rechaza("email", p -> p.setEmail(null)));
        prueba("correo sin dominio", () -> rechaza("email", p -> p.setEmail("no-es-correo")));
        prueba("correo con espacios", () -> rechaza("email", p -> p.setEmail("a b@example.invalid")));
        prueba("correo largo", () -> rechaza("email", p -> p.setEmail("a".repeat(110) + "@example.invalid")));
        prueba("teléfono nulo", () -> rechaza("teléfono", p -> p.setTelefono(null)));
        prueba("teléfono corto", () -> rechaza("teléfono", p -> p.setTelefono("123")));
        prueba("teléfono largo", () -> rechaza("teléfono", p -> p.setTelefono("1".repeat(16))));
        prueba("teléfono con letras", () -> rechaza("teléfono", p -> p.setTelefono("abc9511234567")));
        prueba("nacimiento obligatorio", () -> rechaza("fecha", p -> p.setFechaNacimiento(null)));
        prueba("nacimiento futuro", () -> rechaza("fecha", p -> p.setFechaNacimiento(Date.valueOf(LocalDate.now().plusDays(1)))));
        prueba("alergias largas", () -> rechaza("alergias", p -> p.setAlergias("x".repeat(1001))));
        prueba("intolerancias largas", () -> rechaza("intolerancias", p -> p.setIntolerancias("x".repeat(1001))));
        prueba("preferencias largas", () -> rechaza("preferencias", p -> p.setPreferencias("x".repeat(1001))));
        prueba("disgustos largos", () -> rechaza("disgustos", p -> p.setDisgustos("x".repeat(1001))));
        prueba("normaliza y crea", () -> {
            Fachada f = new Fachada(); Paciente p = valido();
            p.setNombre("  Paciente de prueba  "); p.setEmail("  QA@EXAMPLE.INVALID  "); p.setTelefono("+52 (951) 123-4567");
            if (f.guardar(p) != p || f.escrituras != 1 || !p.getNombre().equals("Paciente de prueba")
                    || !p.getEmail().equals("qa@example.invalid") || !p.getTelefono().equals("+529511234567")
                    || p.getCodigoAcceso().length() != 12)
                throw new AssertionError("No normalizó o persistió");
        });
        prueba("rechaza correo duplicado", () -> {
            Fachada f = new Fachada(); f.duplicadoEmail = valido(); f.duplicadoEmail.setId(1L);
            try { f.guardar(valido()); throw new AssertionError("Aceptó duplicado"); }
            catch (ReglaNegocioException esperado) { if (f.escrituras != 0) throw new AssertionError(); }
        });
        prueba("rechaza teléfono duplicado", () -> {
            Fachada f = new Fachada(); f.duplicadoTelefono = valido(); f.duplicadoTelefono.setId(1L);
            try { f.guardar(valido()); throw new AssertionError("Aceptó duplicado"); }
            catch (ReglaNegocioException esperado) { if (f.escrituras != 0) throw new AssertionError(); }
        });
        prueba("edita sin duplicarse a sí mismo", () -> {
            Fachada f = new Fachada(); Paciente p = valido(); p.setId(1L);
            Paciente actual = valido(); actual.setId(1L);
            actual.setAntecedentesFamiliares("Antecedente ficticio registrado en otra vista");
            f.duplicadoEmail = actual;
            f.duplicadoTelefono = actual;
            f.persistido = actual;
            p.setNombre("Paciente actualizado");
            if (f.guardar(p) != actual || !p.getNombre().equals(actual.getNombre())
                    || actual.getAntecedentesFamiliares() == null) {
                throw new AssertionError("La edición debe conservar los antecedentes ya guardados.");
            }
        });
        System.out.println("RESULTADO " + correctas + " correctas, " + fallidas + " fallidas");
        if (fallidas > 0) System.exit(1);
    }
}
