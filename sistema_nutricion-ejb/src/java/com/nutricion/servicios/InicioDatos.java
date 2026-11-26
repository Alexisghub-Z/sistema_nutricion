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
    private ConsultorioFacade consultorioFacade;

    @EJB
    private UsuarioFacade usuarioFacade;

    @EJB
    private PacienteFacade pacienteFacade;

    @EJB
    private AlergiaFacade alergiaFacade;

    @PostConstruct
    public void iniciar() {
        Consultorio principal = consultorioFacade.asegurarPrincipal();
        asegurarCatalogoAlergias();
        migrarAgenda(principal);
        if (usuarioFacade.contar() > 0) {
            migrarDatosExistentes(principal);
            return;
        }
        usuarioFacade.registrar("Nutriólogo Administrador", "admin@nutricion.com", "admin123", Rol.ADMIN, principal);

        Paciente ana = paciente("Ana López Martínez", "ana.lopez@correo.com", "9511234567", 1995, 4, 12, Sexo.FEMENINO,
                "Cacahuate", "Lactosa", "Sin carne roja", "Brócoli", principal);
        Paciente luis = paciente("Luis Hernández Ruiz", "luis.hdz@correo.com", "9517654321", 1988, 9, 3, Sexo.MASCULINO,
                null, null, null, "Pescado", principal);
        Paciente sofia = paciente("Sofía Ramírez Cruz", "sofia.ramirez@correo.com", "9515550101", 2001, 1, 27, Sexo.FEMENINO,
                null, "Gluten", "Vegetariana", null, principal);

        // Historial de Ana: tres consultas pasadas para la gráfica de evolución.
        consulta(ana, cita(ana, -60, 10, EstadoCita.COMPLETADA, "Primera valoración"), 72.5, 1.62, 31.0, 84, 100, "350.00");
        consulta(ana, cita(ana, -30, 10, EstadoCita.COMPLETADA, "Seguimiento mensual"), 70.1, 1.62, 29.4, 81, 99, "350.00");
        consulta(ana, cita(ana, -2, 11, EstadoCita.COMPLETADA, "Seguimiento mensual"), 68.4, 1.62, 28.1, 79, 98, "350.00");
        consulta(luis, cita(luis, -15, 17, EstadoCita.COMPLETADA, "Aumentar masa muscular"), 81.0, 1.78, 22.5, 90, 102, "400.00");

        cita(ana, 28, 10, EstadoCita.PENDIENTE, "Seguimiento mensual");
        cita(luis, 1, 12, EstadoCita.PENDIENTE, "Revisión de plan");
        cita(sofia, 2, 16, EstadoCita.PENDIENTE, "Primera consulta: plan vegetariano");
        cita(sofia, -5, 9, EstadoCita.NO_ASISTIO, "Primera consulta");
        LOG.info("Datos iniciales del entorno de demostración creados.");
    }

    private void migrarAgenda(Consultorio principal) {
        em.createQuery("UPDATE ConfiguracionAgenda c SET c.consultorio = :principal WHERE c.consultorio IS NULL")
                .setParameter("principal", principal).executeUpdate();
        em.createQuery("UPDATE HorarioLaboral h SET h.consultorio = :principal WHERE h.consultorio IS NULL")
                .setParameter("principal", principal).executeUpdate();
        // Las citas creadas antes de incorporar @Version conservan una versión nula.
        em.createNativeQuery("UPDATE citas SET VERSION = 1 WHERE VERSION IS NULL").executeUpdate();
    }

    private void migrarDatosExistentes(Consultorio principal) {
        for (Usuario u : em.createQuery("SELECT u FROM Usuario u WHERE u.consultorio IS NULL", Usuario.class).getResultList()) {
            u.setConsultorio(principal);
            u.setActivo(true);
        }
        for (Paciente p : em.createQuery("SELECT p FROM Paciente p WHERE p.consultorio IS NULL OR p.codigoAcceso IS NULL", Paciente.class).getResultList()) {
            if (p.getConsultorio() == null) {
                p.setConsultorio(principal);
            }
            if (p.getCodigoAcceso() == null || p.getCodigoAcceso().trim().isEmpty()) {
                pacienteFacade.asegurarCodigoAcceso(p.getId());
            }
        }
    }

    private Paciente paciente(String nombre, String email, String tel, int anio, int mes, int dia, Sexo sexo,
            String alergias, String intolerancias, String preferencias, String disgustos, Consultorio consultorio) {
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
        p.setConsultorio(consultorio);
        p.setCodigoAcceso(com.nutricion.util.Seguridad.codigoPortal());
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

    private void asegurarCatalogoAlergias() {
        if (alergiaFacade.contar() > 0) {
            return;
        }
        em.persist(new Alergia("Cacahuate", TipoAlergia.ALERGIA, "Leguminosas"));
        em.persist(new Alergia("Frutos secos (nuez, almendra)", TipoAlergia.ALERGIA, "Oleaginosas"));
        em.persist(new Alergia("Mariscos y crustáceos", TipoAlergia.ALERGIA, "AOA"));
        em.persist(new Alergia("Pescado", TipoAlergia.ALERGIA, "AOA"));
        em.persist(new Alergia("Huevo", TipoAlergia.ALERGIA, "AOA"));
        em.persist(new Alergia("Soya", TipoAlergia.ALERGIA, "Leguminosas"));
        em.persist(new Alergia("Proteína de leche de vaca", TipoAlergia.ALERGIA, "Lácteos"));
        em.persist(new Alergia("Lactosa", TipoAlergia.INTOLERANCIA, "Lácteos"));
        em.persist(new Alergia("Gluten / Trigo", TipoAlergia.INTOLERANCIA, "Cereales"));
        em.persist(new Alergia("Fructosa", TipoAlergia.INTOLERANCIA, "Frutas y azúcares"));
        em.persist(new Alergia("Sulfitos", TipoAlergia.INTOLERANCIA, "Aditivos"));
        LOG.info("Catálogo inicial de alergias e intolerancias creado.");
    }
}
