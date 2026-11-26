package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consulta;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.Paciente;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.PacienteFacade;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

/** Próxima cita y mediciones propias mediante código único. */
@Named
@ViewScoped
public class PortalPacienteBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PacienteFacade pacienteFacade;

    @EJB
    private ConsultaFacade consultaFacade;

    @EJB
    private CitaFacade citaFacade;

    private String codigoAcceso;
    private Paciente paciente;
    private List<Consulta> consultas = new ArrayList<>();
    private Cita siguienteCita;
    private LineChartModel graficaPeso;
    private LineChartModel graficaImc;

    public void ingresar() {
        FacesContext.getCurrentInstance().getExternalContext().setResponseHeader("Cache-Control", "no-store");
        if (codigoAcceso == null || codigoAcceso.trim().isEmpty()) {
            Mensajes.error("Escribe tu código de acceso.");
            return;
        }
        Paciente p = pacienteFacade.porCodigoAcceso(codigoAcceso);
        if (p == null) {
            Mensajes.error("Código no válido. Verifica con tu nutriólogo.");
            return;
        }
        codigoAcceso = null;
        cargar(p);
    }

    public void actualizar() {
        FacesContext.getCurrentInstance().getExternalContext().setResponseHeader("Cache-Control", "no-store");
        if (paciente == null) {
            return;
        }
        Paciente actual = pacienteFacade.porCodigoAcceso(paciente.getCodigoAcceso());
        if (actual == null) {
            salir();
            Mensajes.error("El acceso al portal ya no está disponible. Verifica tu código con el consultorio.");
            return;
        }
        cargar(actual);
    }

    private void cargar(Paciente actual) {
        paciente = actual;
        consultas = consultaFacade.medicionesParaPortal(paciente);
        siguienteCita = citaFacade.siguienteDePaciente(paciente);
        construirGraficas();
    }

    public void salir() {
        paciente = null;
        codigoAcceso = null;
        consultas.clear();
        siguienteCita = null;
        graficaPeso = null;
        graficaImc = null;
    }

    public void actualizarCita(EstadoCita estado) {
        if (paciente == null || siguienteCita == null) {
            return;
        }
        try {
            citaFacade.actualizarDesdePortal(siguienteCita.getId(), paciente, estado);
            Mensajes.info(estado == EstadoCita.CONFIRMADA ? "Tu cita quedó confirmada." : "Tu cita quedó cancelada.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
        actualizar();
    }

    private void construirGraficas() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy", new Locale("es", "MX"));
        graficaPeso = crearModeloBase("Evolución de peso (kg)");
        graficaImc = crearModeloBase("Evolución de IMC");

        LineChartSeries sPeso = new LineChartSeries();
        sPeso.setLabel("Peso (kg)");
        LineChartSeries sImc = new LineChartSeries();
        sImc.setLabel("IMC");

        Map<String, Integer> consultasPorFecha = new HashMap<>();
        for (Consulta c : consultas) {
            String et = fmt.format(c.getFecha());
            int numero = consultasPorFecha.merge(et, 1, Integer::sum);
            if (numero > 1) {
                et += " (" + numero + ")";
            }
            if (c.getPeso() != null) {
                sPeso.set(et, c.getPeso());
            }
            if (c.getImc() != null) {
                sImc.set(et, c.getImc());
            }
        }
        if (!sPeso.getData().isEmpty()) {
            graficaPeso.addSeries(sPeso);
        }
        if (!sImc.getData().isEmpty()) {
            graficaImc.addSeries(sImc);
        }
    }

    private LineChartModel crearModeloBase(String titulo) {
        LineChartModel m = new LineChartModel();
        m.setTitle(titulo);
        m.setLegendPosition("ne");
        m.setShowPointLabels(true);
        m.getAxes().put(AxisType.X, new CategoryAxis("Fecha"));
        return m;
    }

    public Consulta getUltimaConsulta() {
        return consultas.isEmpty() ? null : consultas.get(consultas.size() - 1);
    }

    public String getCodigoAcceso() { return codigoAcceso; }
    public void setCodigoAcceso(String codigoAcceso) { this.codigoAcceso = codigoAcceso; }
    public Paciente getPaciente() { return paciente; }
    public List<Consulta> getConsultas() { return consultas; }
    public Cita getSiguienteCita() { return siguienteCita; }
    public LineChartModel getGraficaPeso() { return graficaPeso; }
    public LineChartModel getGraficaImc() { return graficaImc; }
}
