package com.nutricion.servicios;

import com.nutricion.entidades.*;
import java.math.BigDecimal;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Al desplegar por primera vez crea el usuario administrador y datos de ejemplo.
 * Usuario: admin@nutricion.com / admin123
 */
@Singleton
@Startup
public class InicioDatos {

    private static final Logger LOG = Logger.getLogger(InicioDatos.class.getName());

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private UsuarioFacade usuarioFacade;

    @PostConstruct
    public void iniciar() {
        if (usuarioFacade.contar() > 0) {
            return;
        }
        usuarioFacade.registrar("Nutriólogo Administrador", "admin@nutricion.com", "admin123");

        Paciente ana = paciente("Ana López Martínez", "ana.lopez@correo.com", "9511234567", 1995, 4, 12, Sexo.FEMENINO,
                "Cacahuate", "Lactosa", "Sin carne roja", "Brócoli");
        Paciente luis = paciente("Luis Hernández Ruiz", "luis.hdz@correo.com", "9517654321", 1988, 9, 3, Sexo.MASCULINO,
                null, null, null, "Pescado");
        Paciente sofia = paciente("Sofía Ramírez Cruz", "sofia.ramirez@correo.com", "9515550101", 2001, 1, 27, Sexo.FEMENINO,
                null, "Gluten", "Vegetariana", null);

        // Historial de Ana: tres consultas pasadas para la gráfica de evolución.
        consulta(ana, cita(ana, -60, 10, EstadoCita.COMPLETADA, "Primera valoración"), 72.5, 1.62, 31.0, 84, 100, "350.00");
        consulta(ana, cita(ana, -30, 10, EstadoCita.COMPLETADA, "Seguimiento mensual"), 70.1, 1.62, 29.4, 81, 99, "350.00");
        consulta(ana, cita(ana, -2, 11, EstadoCita.COMPLETADA, "Seguimiento mensual"), 68.4, 1.62, 28.1, 79, 98, "350.00");
        consulta(luis, cita(luis, -15, 17, EstadoCita.COMPLETADA, "Aumentar masa muscular"), 81.0, 1.78, 22.5, 90, 102, "400.00");

        cita(ana, 28, 10, EstadoCita.PENDIENTE, "Seguimiento mensual");
        cita(luis, 1, 12, EstadoCita.PENDIENTE, "Revisión de plan");
        cita(sofia, 2, 16, EstadoCita.PENDIENTE, "Primera consulta: plan vegetariano");
        cita(sofia, -5, 9, EstadoCita.NO_ASISTIO, "Primera consulta");
        LOG.info("Datos iniciales creados. Usuario: admin@nutricion.com / admin123");
    }

    private Paciente paciente(String nombre, String email, String tel, int anio, int mes, int dia, Sexo sexo,
            String alergias, String intolerancias, String preferencias, String disgustos) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        p.setEmail(email);
        p.setTelefono(tel);
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia);
        p.setFechaNacimiento(c.getTime());
        p.setSexo(sexo);
        p.setAlergias(alergias);
        p.setIntolerancias(intolerancias);
        p.setPreferencias(preferencias);
        p.setDisgustos(disgustos);
        em.persist(p);
        return p;
    }

    private Cita cita(Paciente p, int dias, int hora, EstadoCita estado, String motivo) {
        // Evita domingos en los datos de ejemplo.
        java.util.Date f = Fechas.en(dias, hora, 0);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(f);
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY) {
            f = Fechas.sumarDias(f, 1);
        }
        Cita c = new Cita();
        c.setPaciente(p);
        c.setFechaHora(f);
        c.setMotivo(motivo);
        c.setEstado(estado);
        c.setCodigo(com.nutricion.util.Seguridad.codigoCita());
        em.persist(c);
        return c;
    }

    private void consulta(Paciente p, Cita cita, double peso, double talla, double grasa, double cintura, double cadera, String monto) {
        Consulta c = new Consulta();
        c.setPaciente(p);
        c.setCita(cita);
        c.setFecha(cita.getFechaHora());
        c.setMotivo(cita.getMotivo());
        c.setPeso(peso);
        c.setTalla(talla);
        c.setGrasaCorporal(grasa);
        c.setCintura(cintura);
        c.setCadera(cadera);
        c.setObjetivo("Reducir porcentaje de grasa manteniendo masa muscular.");
        c.setPlan("Dieta de 1,700 kcal: 5 tiempos de comida, 2 L de agua al día, actividad física 3 veces por semana.");
        c.setMontoConsulta(new BigDecimal(monto));
        c.setMetodoPago(MetodoPago.EFECTIVO);
        c.setEstadoPago(EstadoPago.PAGADO);
        em.persist(c);
        cita.setConsulta(c);
    }
}
