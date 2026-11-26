package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.BloqueoCalendario;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.OrigenCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.DisponibilidadService;
import com.nutricion.servicios.Fechas;
import com.nutricion.servicios.PacienteFacade;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.context.RequestContext;
import org.primefaces.event.ScheduleEntryMoveEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.LazyScheduleModel;
import org.primefaces.model.ScheduleEvent;
import org.primefaces.model.ScheduleModel;

@Named
@ViewScoped
public class AgendaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private CitaFacade citaFacade;

    @EJB
    private PacienteFacade pacienteFacade;

    @EJB
    private DisponibilidadService disponibilidadService;

    @Inject
    private LoginBean loginBean;

    private ScheduleModel scheduleModel;
    private Date fechaSeleccionada = new Date();
    private EstadoCita filtroEstado;
    private List<Cita> citasDelDia = new ArrayList<>();
    private List<Paciente> pacientes = new ArrayList<>();

    private Cita citaActual = new Cita();
    private Long pacienteIdSeleccionado;

    @PostConstruct
    public void iniciar() {
        scheduleModel = new LazyScheduleModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void loadEvents(Date start, Date end) {
                List<Cita> citas = citaFacade.entreFechas(start, end, loginBean.getConsultorioActivo());
                for (Cita c : citas) {
                    Date fin = Fechas.sumarMinutos(c.getFechaHora(), c.getDuracionMinutos());
                    String titulo = c.getPaciente().getNombre() + " · " + c.getEstado().getEtiqueta();
                    DefaultScheduleEvent ev = new DefaultScheduleEvent(titulo, c.getFechaHora(), fin, c.getId());
                    ev.setStyleClass(estiloEvento(c.getEstado()));
                    ev.setEditable(c.getEstado() != EstadoCita.COMPLETADA && c.getEstado() != EstadoCita.CANCELADA);
                    addEvent(ev);
                }
                for (BloqueoCalendario bloqueo : disponibilidadService.bloqueos(start, end, loginBean.getConsultorioActivo())) {
                    DefaultScheduleEvent ocupado = new DefaultScheduleEvent(
                            "Ocupado · Google Calendar", bloqueo.getInicio(), bloqueo.getFin());
                    ocupado.setEditable(false);
                    ocupado.setStyleClass("cita-bloqueo");
                    addEvent(ocupado);
                }
            }
        };
        cargarDia();
    }

    public void cargarDia() {
        List<Cita> delDia = citaFacade.delDia(fechaSeleccionada == null ? new Date() : fechaSeleccionada,
                loginBean.getConsultorioActivo());
        if (filtroEstado == null) {
            citasDelDia = delDia;
        } else {
            citasDelDia = new ArrayList<>();
            for (Cita c : delDia) {
                if (c.getEstado() == filtroEstado) {
                    citasDelDia.add(c);
                }
            }
        }
    }

    public void nuevaCita() {
        prepararCita(Fechas.inicioDelDia(fechaSeleccionada == null ? new Date() : fechaSeleccionada));
    }

    private void prepararCita(Date fecha) {
        pacientes = pacienteFacade.todos(loginBean.getConsultorioActivo());
        citaActual = new Cita();
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 10);
        }
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        citaActual.setFechaHora(cal.getTime());
        citaActual.setDuracionMinutos(disponibilidadService.obtenerConfiguracion(loginBean.getConsultorioActivo()).getDuracionConsultaMin());
        citaActual.setOrigen(OrigenCita.PANEL);
        pacienteIdSeleccionado = null;
    }

    public void editarCita(Cita c) {
        if (c == null || c.getId() == null) return;
        citaActual = citaFacade.delConsultorio(c.getId(), loginBean.getConsultorioActivo());
        if (citaActual == null) {
            return;
        }
        pacientes = pacienteFacade.todos(loginBean.getConsultorioActivo());
        pacienteIdSeleccionado = citaActual.getPaciente() != null ? citaActual.getPaciente().getId() : null;
    }

    public void onDateSelect(SelectEvent event) {
        fechaSeleccionada = (Date) event.getObject();
        prepararCita(fechaSeleccionada);
        cargarDia();
    }

    public void onEventSelect(SelectEvent event) {
        boolean abrir = false;
        ScheduleEvent ev = (ScheduleEvent) event.getObject();
        if (ev != null && ev.getData() instanceof Long) {
            citaActual = citaFacade.delConsultorio((Long) ev.getData(), loginBean.getConsultorioActivo());
            if (citaActual != null) {
                pacientes = pacienteFacade.todos(loginBean.getConsultorioActivo());
                pacienteIdSeleccionado = citaActual.getPaciente().getId();
                fechaSeleccionada = citaActual.getFechaHora();
                cargarDia();
                abrir = true;
            }
        }
        RequestContext.getCurrentInstance().addCallbackParam("abrirCita", abrir);
    }

    public void onEventMove(ScheduleEntryMoveEvent event) {
        try {
            ScheduleEvent ev = event.getScheduleEvent();
            if (ev != null && ev.getData() instanceof Long) {
                Cita c = citaFacade.delConsultorio((Long) ev.getData(), loginBean.getConsultorioActivo());
                if (c != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(c.getFechaHora());
                    cal.add(Calendar.DAY_OF_MONTH, event.getDayDelta());
                    cal.add(Calendar.MINUTE, event.getMinuteDelta());
                    c.setFechaHora(cal.getTime());
                    fechaSeleccionada = citaFacade.guardar(c, loginBean.getConsultorioActivo()).getFechaHora();
                    cargarDia();
                    Mensajes.info("Cita reprogramada correctamente.");
                }
            }
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public void guardarCita() {
        try {
            if (pacienteIdSeleccionado != null) {
                citaActual.setPaciente(pacienteFacade.delConsultorio(pacienteIdSeleccionado, loginBean.getConsultorioActivo()));
            }
            boolean nueva = citaActual.getId() == null;
            citaActual = citaFacade.guardar(citaActual, loginBean.getConsultorioActivo());
            fechaSeleccionada = citaActual.getFechaHora();
            cargarDia();
            Mensajes.info(nueva ? "Cita agendada." : "Cita actualizada.");
            RequestContext.getCurrentInstance().execute("PF('dlgCita').hide()");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public void cambiarEstado(Cita c, EstadoCita nuevoEstado) {
        try {
            citaFacade.cambiarEstado(c.getId(), nuevoEstado, loginBean.getConsultorioActivo());
            cargarDia();
            Mensajes.info("Estado actualizado a " + nuevoEstado.getEtiqueta() + ".");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public String getBadgeEstado(EstadoCita estado) {
        if (estado == null) return "badge badge-neutro";
        switch (estado) {
            case CONFIRMADA:
            case COMPLETADA:
                return "badge badge-exito";
            case PENDIENTE:
            case APARTADA:
                return "badge badge-alerta";
            case CANCELADA:
            case NO_ASISTIO:
                return "badge badge-peligro";
            default:
                return "badge badge-neutro";
        }
    }

    private String estiloEvento(EstadoCita estado) {
        if (estado == null) return "cita-pendiente";
        switch (estado) {
            case CONFIRMADA: return "cita-confirmada";
            case COMPLETADA: return "cita-completada";
            case CANCELADA:
            case NO_ASISTIO: return "cita-cancelada";
            default: return "cita-pendiente";
        }
    }

    public ScheduleModel getScheduleModel() { return scheduleModel; }
    public Date getFechaSeleccionada() { return fechaSeleccionada; }
    public void setFechaSeleccionada(Date fechaSeleccionada) { this.fechaSeleccionada = fechaSeleccionada; }
    public EstadoCita getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(EstadoCita filtroEstado) { this.filtroEstado = filtroEstado; }
    public List<Cita> getCitasDelDia() { return citasDelDia; }
    public List<Paciente> getPacientes() { return pacientes; }
    public Cita getCitaActual() { return citaActual; }
    public void setCitaActual(Cita citaActual) { this.citaActual = citaActual; }
    public Long getPacienteIdSeleccionado() { return pacienteIdSeleccionado; }
    public void setPacienteIdSeleccionado(Long pacienteIdSeleccionado) { this.pacienteIdSeleccionado = pacienteIdSeleccionado; }
}
