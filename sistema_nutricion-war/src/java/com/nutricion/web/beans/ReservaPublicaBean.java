package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.TipoCita;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.ConsultorioFacade;
import com.nutricion.servicios.DisponibilidadService;
import com.nutricion.servicios.Fechas;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/** Flujo público de consulta de disponibilidad y reserva en línea (HU-19, HU-20). */
@Named
@ViewScoped
public class ReservaPublicaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultorioFacade consultorioFacade;

    @EJB
    private DisponibilidadService disponibilidadService;

    @EJB
    private CitaFacade citaFacade;

    private List<Consultorio> consultorios = new ArrayList<>();
    private Long consultorioId;
    private Date fecha;
    private List<Date> slots = new ArrayList<>();
    private Long slotSeleccionadoMs;

    private Paciente paciente = new Paciente();
    private Cita cita = new Cita();
    private Cita citaConfirmada;

    private String codigoBusqueda;
    private Cita citaEncontrada;

    @PostConstruct
    public void iniciar() {
        consultorios = consultorioFacade.activos();
        String slug = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("c");
        if (slug != null) {
            Consultorio elegido = consultorioFacade.porSlug(slug);
            if (elegido != null && elegido.isActivo()) {
                consultorioId = elegido.getId();
            } else {
                Mensajes.error("Este consultorio no está disponible. Elige uno de la lista.");
            }
        } else if (!consultorios.isEmpty()) {
            consultorioId = consultorios.get(0).getId();
        }
        fecha = Fechas.sumarDias(new Date(), 1);
        cita.setTipo(TipoCita.PRESENCIAL);
        cargarHorarios();
    }

    public void cargarHorarios() {
        Consultorio c = consultorioSeleccionado();
        slots = disponibilidadService.slotsDisponibles(fecha, c);
        slotSeleccionadoMs = null;
    }

    public void seleccionarSlot(Date slot) {
        if (slot != null) {
            this.slotSeleccionadoMs = slot.getTime();
        }
    }

    public String horaSlot(Date slot) {
        return new SimpleDateFormat("HH:mm", new Locale("es", "MX")).format(slot);
    }

    public void reservar() {
        try {
            if (slotSeleccionadoMs == null) {
                Mensajes.error("Selecciona un horario disponible.");
                return;
            }
            Consultorio c = consultorioSeleccionado();
            cita.setFechaHora(new Date(slotSeleccionadoMs));
            citaConfirmada = citaFacade.agendarPublica(paciente, cita, c);
            Mensajes.info("Tu cita quedó agendada con código " + citaConfirmada.getCodigo() + ".");
            paciente = new Paciente();
            cita = new Cita();
            cita.setTipo(TipoCita.PRESENCIAL);
            cargarHorarios();
        } catch (Exception e) {
            Mensajes.error(e);
            cargarHorarios();
        }
    }

    public void nuevaReserva() {
        citaConfirmada = null;
        cargarHorarios();
    }

    public void consultarPorCodigo() {
        citaEncontrada = citaFacade.porCodigo(codigoBusqueda);
        if (citaEncontrada == null) {
            Mensajes.error("No encontramos ninguna cita con ese código.");
        }
    }

    public void cancelarPorCodigo() {
        try {
            if (citaEncontrada == null) {
                return;
            }
            citaEncontrada = citaFacade.cancelarPorCodigo(citaEncontrada.getCodigo());
            Mensajes.info("La cita " + citaEncontrada.getCodigo() + " fue cancelada.");
            cargarHorarios();
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    private Consultorio consultorioSeleccionado() {
        if (consultorioId == null) {
            return null;
        }
        return consultorios.stream().filter(c -> consultorioId.equals(c.getId())).findFirst().orElse(null);
    }

    public Date getFechaMinima() { return new Date(); }
    public List<Consultorio> getConsultorios() { return consultorios; }
    public Long getConsultorioId() { return consultorioId; }
    public void setConsultorioId(Long consultorioId) { this.consultorioId = consultorioId; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public List<Date> getSlots() { return slots; }
    public Long getSlotSeleccionadoMs() { return slotSeleccionadoMs; }
    public void setSlotSeleccionadoMs(Long slotSeleccionadoMs) { this.slotSeleccionadoMs = slotSeleccionadoMs; }
    public Paciente getPaciente() { return paciente; }
    public Cita getCita() { return cita; }
    public Cita getCitaConfirmada() { return citaConfirmada; }
    public String getCodigoBusqueda() { return codigoBusqueda; }
    public void setCodigoBusqueda(String codigoBusqueda) { this.codigoBusqueda = codigoBusqueda; }
    public Cita getCitaEncontrada() { return citaEncontrada; }
}
