package com.nutricion.web.beans;

import com.nutricion.entidades.Alergia;
import com.nutricion.entidades.ArchivoAdjunto;
import com.nutricion.entidades.CategoriaArchivo;
import com.nutricion.entidades.Consulta;
import com.nutricion.entidades.FactorActividad;
import com.nutricion.entidades.FormulaEnergetica;
import com.nutricion.entidades.ObjetivoTratamiento;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.entidades.Sexo;
import com.nutricion.servicios.AlergiaFacade;
import com.nutricion.servicios.ArchivoFacade;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.PacienteFacade;
import com.nutricion.servicios.PlanTratamientoFacade;
import com.nutricion.util.CalculosNutricion;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.web.util.Mensajes;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

@Named
@ViewScoped
public class ExpedienteBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int PESTANA_ARCHIVOS = 6;

    @EJB
    private PacienteFacade pacienteFacade;

    @EJB
    private ConsultaFacade consultaFacade;

    @EJB
    private AlergiaFacade alergiaFacade;

    @EJB
    private PlanTratamientoFacade planTratamientoFacade;

    @EJB
    private ArchivoFacade archivoFacade;

    @Inject
    private LoginBean loginBean;

    private Long pacienteId;
    private Paciente paciente;
    private List<Consulta> historial = new ArrayList<>();
    private List<Alergia> catalogoAlergias = new ArrayList<>();
    private List<Long> alergiasSeleccionadas = new ArrayList<>();
    private Consulta consultaActual = new Consulta();
    private int tabActivo = 0;

    // Evolución (HU-09, HU-11)
    private Date evolucionDesde;
    private Date evolucionHasta;
    private LineChartModel graficaPeso;
    private LineChartModel graficaImc;
    private LineChartModel graficaGrasa;
    private LineChartModel graficaMusculo;
    private Consulta consultaAnterior;
    private boolean hayComparativo;

    // Cálculo dietético (HU-22, HU-23, HU-24)
    private PlanTratamiento planActual = new PlanTratamiento();
    private List<PlanTratamiento> planesGuardados = new ArrayList<>();

    private List<ArchivoAdjunto> adjuntos = Collections.emptyList();
    private CategoriaArchivo categoriaAdjunto;
    private CategoriaArchivo filtroAdjuntos;
    private Date fechaFoto;
    private Long primeraFotoId;
    private Long segundaFotoId;
    private List<ArchivoAdjunto> fotosComparadas = Collections.emptyList();

    public void inicializar() {
        FacesContext contexto = FacesContext.getCurrentInstance();
        if (contexto.isPostback()) {
            return;
        }
        String id = contexto.getExternalContext().getRequestParameterMap().get("id");
        if (id == null) {
            return;
        }
        try {
            pacienteId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            return;
        }
        catalogoAlergias = alergiaFacade.todas();
        recargarExpediente();
        if (paciente != null && consultaActual.getPaciente() == null) {
            prepararNuevaConsulta();
        }
        if (paciente != null) {
            adjuntos = archivoFacade.porExpediente(pacienteId, loginBean.getConsultorioActivo().getId(),
                    loginBean.getUsuario().getRol());
            if ("1".equals(contexto.getExternalContext().getRequestParameterMap().get("archivos"))) {
                tabActivo = PESTANA_ARCHIVOS;
            }
        }
    }

    private void recargarExpediente() {
        if (!loginBean.isPuedeVerExpedientes()) {
            paciente = null;
            return;
        }
        cargarDatosPaciente();
        historial = paciente == null ? new ArrayList<>() : consultaFacade.historial(paciente);
        alergiasSeleccionadas.clear();
        if (paciente != null) {
            if (paciente.getAlergiasCatalogo() != null) {
                for (Alergia a : paciente.getAlergiasCatalogo()) {
                    alergiasSeleccionadas.add(a.getId());
                }
            }
            planesGuardados = planTratamientoFacade.porPaciente(paciente);
            prepararComparativo();
            actualizarGraficas();
            prepararCalculoDietetico();
        }
    }

    private void cargarDatosPaciente() {
        paciente = pacienteFacade.expediente(pacienteId, loginBean.getConsultorioActivo());
        if (paciente != null && (paciente.getCodigoAcceso() == null || paciente.getCodigoAcceso().trim().isEmpty())) {
            paciente.setCodigoAcceso(pacienteFacade.asegurarCodigoAcceso(paciente.getId()));
        }
    }

    public String actualizarDatos() {
        cargarDatosPaciente();
        if (paciente == null) {
            return "/vistas/pacientes/lista?faces-redirect=true";
        }
        recalcularPlan();
        return null;
    }

    private void prepararNuevaConsulta() {
        consultaActual = new Consulta();
        consultaActual.setPaciente(paciente);
        consultaActual.setFecha(new Date());
        Consulta ultima = getUltimaConsulta();
        if (ultima != null && ultima.getTalla() != null) {
            consultaActual.setTalla(ultima.getTalla());
        }
    }

    private void prepararComparativo() {
        hayComparativo = historial.size() >= 2;
        consultaAnterior = hayComparativo ? historial.get(1) : null;
    }

    public void actualizarGraficas() {
        if (paciente == null) {
            return;
        }
        List<Consulta> serie = consultaFacade.evolucion(paciente, evolucionDesde, evolucionHasta);
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy", new Locale("es", "MX"));

        graficaPeso = crearModeloLinea("Evolución de Peso (kg)", "kg", "#2f855a");
        graficaImc = crearModeloLinea("Evolución de IMC", "kg/m²", "#2b6cb0");
        graficaGrasa = crearModeloLinea("% Grasa Corporal", "%", "#d97706");
        graficaMusculo = crearModeloLinea("Masa Muscular (kg)", "kg", "#6b46c1");

        LineChartSeries sPeso = new LineChartSeries("Peso (kg)");
        LineChartSeries sImc = new LineChartSeries("IMC");
        LineChartSeries sGrasa = new LineChartSeries("% Grasa");
        LineChartSeries sMusc = new LineChartSeries("Masa Muscular (kg)");

        Map<String, Integer> consultasPorFecha = new HashMap<>();
        for (Consulta c : serie) {
            String etiqueta = fmt.format(c.getFecha());
            int numero = consultasPorFecha.merge(etiqueta, 1, Integer::sum);
            if (numero > 1) {
                etiqueta += " (" + numero + ")";
            }
            if (c.getPeso() != null) sPeso.set(etiqueta, c.getPeso());
            if (c.getImc() != null) sImc.set(etiqueta, c.getImc());
            if (c.getGrasaCorporal() != null) sGrasa.set(etiqueta, c.getGrasaCorporal());
            if (c.getMasaMuscular() != null) sMusc.set(etiqueta, c.getMasaMuscular());
        }
        if (!sPeso.getData().isEmpty()) graficaPeso.addSeries(sPeso); else graficaPeso = null;
        if (!sImc.getData().isEmpty()) graficaImc.addSeries(sImc); else graficaImc = null;
        if (!sGrasa.getData().isEmpty()) graficaGrasa.addSeries(sGrasa); else graficaGrasa = null;
        if (!sMusc.getData().isEmpty()) graficaMusculo.addSeries(sMusc); else graficaMusculo = null;
    }

    private LineChartModel crearModeloLinea(String titulo, String unidadY, String colorHex) {
        LineChartModel m = new LineChartModel();
        m.setTitle(titulo);
        m.setAnimate(true);
        m.setShowPointLabels(true);
        m.setSeriesColors(colorHex.replace("#", ""));
        CategoryAxis xAxis = new CategoryAxis("Fecha");
        xAxis.setTickAngle(-25);
        m.getAxes().put(AxisType.X, xAxis);
        m.getAxis(AxisType.Y).setLabel(unidadY);
        return m;
    }

    public void limpiarFiltroGraficas() {
        evolucionDesde = null;
        evolucionHasta = null;
        actualizarGraficas();
    }

    private void prepararCalculoDietetico() {
        planActual = new PlanTratamiento();
        planActual.setPaciente(paciente);
        planActual.setFormula(FormulaEnergetica.MIFFLIN_ST_JEOR);
        planActual.setFactorActividad(FactorActividad.MODERADO);
        planActual.setObjetivo(ObjetivoTratamiento.MANTENIMIENTO);
        Consulta ultima = getUltimaConsulta();
        if (ultima != null) {
            planActual.setPesoUsado(ultima.getPeso());
            planActual.setTallaUsada(ultima.getTalla());
            planActual.setGrasaUsada(ultima.getGrasaCorporal());
        }
        recalcularPlan();
    }

    public void alCambiarObjetivo() {
        if (planActual.getObjetivo() != null) {
            planActual.setAjusteKcal(planActual.getObjetivo().getAjusteSugerido());
            planActual.setPctProteinas(planActual.getObjetivo().getPctProteinasSugerido());
            planActual.setPctLipidos(planActual.getObjetivo().getPctLipidosSugerido());
            planActual.setPctCarbohidratos(planActual.getObjetivo().getPctCarbohidratosSugerido());
        }
        recalcularPlan();
    }

    public void recalcularPlan() {
        if (paciente == null || planActual == null) {
            return;
        }
        boolean masculino = paciente.getSexo() == Sexo.MASCULINO;
        Double geb = CalculosNutricion.gastoEnergeticoBasal(
                planActual.getFormula(),
                planActual.getPesoUsado(),
                planActual.getTallaUsada(),
                paciente.getEdad(),
                masculino,
                planActual.getGrasaUsada());
        planActual.setGeb(geb);
        if (geb != null && planActual.getFactorActividad() != null) {
            double get = CalculosNutricion.redondear(geb * planActual.getFactorActividad().getFactor(), 0);
            planActual.setGastoTotal(get);
            int ajuste = planActual.getAjusteKcal() == null ? 0 : planActual.getAjusteKcal();
            planActual.setMetaKcal(Math.max(500.0, get + ajuste));
        } else {
            planActual.setGastoTotal(null);
            planActual.setMetaKcal(null);
        }
        planActual.recalcularMacros();
    }

    public void guardarPlanTratamiento() {
        try {
            recalcularPlan();
            planActual.setPaciente(paciente);
            planActual = planTratamientoFacade.guardar(planActual);
            planesGuardados = planTratamientoFacade.porPaciente(paciente);
            Mensajes.info("Plan de tratamiento guardado en el expediente.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public boolean isFormulaFaltaGrasa() {
        return planActual != null && planActual.getFormula() != null
                && planActual.getFormula().isRequiereGrasa()
                && (planActual.getGrasaUsada() == null || planActual.getGrasaUsada() <= 0);
    }

    public int getSumaMacros() {
        if (planActual == null) return 0;
        return (planActual.getPctProteinas() == null ? 0 : planActual.getPctProteinas())
                + (planActual.getPctLipidos() == null ? 0 : planActual.getPctLipidos())
                + (planActual.getPctCarbohidratos() == null ? 0 : planActual.getPctCarbohidratos());
    }

    public String formatDiferencia(Double actual, Double anterior, String unidad) {
        if (actual == null || anterior == null) return "—";
        double diff = CalculosNutricion.redondear(actual - anterior, 2);
        Double pct = CalculosNutricion.variacionPorcentual(actual, anterior);
        String signo = diff > 0 ? "+" : "";
        String pctTxt = pct == null ? "" : " (" + signo + pct + "%)";
        return signo + diff + " " + unidad + pctTxt;
    }

    public void nuevaConsulta() {
        prepararNuevaConsulta();
        tabActivo = 1;
    }

    public void verConsulta(Consulta c) {
        if (c == null || c.getId() == null || paciente == null) {
            return;
        }
        Consulta detalle = consultaFacade.detalle(c.getId(), paciente);
        if (detalle != null) {
            consultaActual = detalle;
            tabActivo = 1;
        }
    }

    public void recalcularImc() {
        if (consultaActual != null) {
            consultaActual.setImc(CalculosNutricion.imc(consultaActual.getPeso(), consultaActual.getTalla()));
        }
    }

    public void guardarConsulta(boolean cerrar) {
        try {
            if (paciente == null) {
                Mensajes.error("No se ha cargado ningún paciente.");
                return;
            }
            consultaActual.setPaciente(paciente);
            Consulta guardada = consultaFacade.guardar(consultaActual, cerrar, loginBean.getConsultorioActivo());
            consultaActual = guardada;
            recargarExpediente();
            Mensajes.info(cerrar
                    ? "Consulta cerrada y bloqueada en modo solo lectura."
                    : "Consulta guardada como borrador.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public void guardarAntecedentes() {
        try {
            pacienteFacade.guardarAntecedentes(paciente, loginBean.getConsultorioActivo());
            cargarDatosPaciente();
            Mensajes.info("Antecedentes clínicos actualizados.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public void guardarAlergias() {
        try {
            pacienteFacade.guardarAlergias(paciente, alergiasSeleccionadas, loginBean.getConsultorioActivo());
            cargarDatosPaciente();
            Mensajes.info("Alergias e intolerancias actualizadas.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public String getImcBadgeClass(Double imc) {
        if (imc == null) return "badge badge-neutro";
        if (imc < 18.5) return "badge badge-alerta";
        if (imc < 25) return "badge badge-exito";
        if (imc < 30) return "badge badge-alerta";
        return "badge badge-peligro";
    }

    public String subirAdjunto() {
        tabActivo = PESTANA_ARCHIVOS;
        try {
            if (paciente == null || !loginBean.isPuedeVerExpedientes()) {
                throw new ReglaNegocioException("El expediente no está disponible.");
            }
            HttpServletRequest peticion = (HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest();
            Part archivo = peticion.getPart("tabsExpediente:frmCargaAdjunto:archivoAdjunto");
            if (archivo == null || archivo.getSize() == 0) {
                throw new ReglaNegocioException("Selecciona un archivo con contenido.");
            }
            try (InputStream datos = archivo.getInputStream()) {
                archivoFacade.guardar(pacienteId, loginBean.getConsultorioActivo().getId(),
                        loginBean.getUsuario().getRol(), categoriaAdjunto, fechaFoto,
                        archivo.getSubmittedFileName(), archivo.getSize(), datos);
            }
            Mensajes.info("Archivo guardado en el expediente.");
            Mensajes.conservarTrasRedireccion();
            return "/vistas/pacientes/expediente?faces-redirect=true&id=" + pacienteId + "&archivos=1";
        } catch (Exception e) {
            Mensajes.error(e);
            return null;
        }
    }

    public List<ArchivoAdjunto> getAdjuntosFiltrados() {
        if (filtroAdjuntos == null) {
            return adjuntos;
        }
        return adjuntos.stream().filter(archivo -> archivo.getCategoria() == filtroAdjuntos)
                .collect(Collectors.toList());
    }

    public List<ArchivoAdjunto> getFotosProgreso() {
        return adjuntos.stream().filter(archivo -> archivo.getCategoria() == CategoriaArchivo.FOTO_PROGRESO)
                .sorted(Comparator.comparing(ArchivoAdjunto::getFechaFoto).thenComparing(ArchivoAdjunto::getId))
                .collect(Collectors.toList());
    }

    public String etiquetaFoto(ArchivoAdjunto foto) {
        return new SimpleDateFormat("dd/MM/yyyy").format(foto.getFechaFoto()) + " · " + foto.getNombreOriginal();
    }

    public void compararFotos() {
        List<ArchivoAdjunto> seleccion = getFotosProgreso().stream()
                .filter(foto -> foto.getId().equals(primeraFotoId) || foto.getId().equals(segundaFotoId))
                .collect(Collectors.toList());
        if (seleccion.size() != 2 || seleccion.get(0).getFechaFoto().equals(seleccion.get(1).getFechaFoto())) {
            fotosComparadas = Collections.emptyList();
            Mensajes.error("Selecciona dos fotografías de fechas distintas.");
            return;
        }
        fotosComparadas = seleccion;
    }

    public CategoriaArchivo[] getCategoriasAdjunto() { return CategoriaArchivo.values(); }
    public CategoriaArchivo getCategoriaAdjunto() { return categoriaAdjunto; }
    public void setCategoriaAdjunto(CategoriaArchivo categoriaAdjunto) { this.categoriaAdjunto = categoriaAdjunto; }
    public CategoriaArchivo getFiltroAdjuntos() { return filtroAdjuntos; }
    public void setFiltroAdjuntos(CategoriaArchivo filtroAdjuntos) { this.filtroAdjuntos = filtroAdjuntos; }
    public Date getFechaFoto() { return fechaFoto; }
    public void setFechaFoto(Date fechaFoto) { this.fechaFoto = fechaFoto; }
    public Long getPrimeraFotoId() { return primeraFotoId; }
    public void setPrimeraFotoId(Long primeraFotoId) { this.primeraFotoId = primeraFotoId; }
    public Long getSegundaFotoId() { return segundaFotoId; }
    public void setSegundaFotoId(Long segundaFotoId) { this.segundaFotoId = segundaFotoId; }
    public List<ArchivoAdjunto> getFotosComparadas() { return fotosComparadas; }

    public Paciente getPaciente() { return paciente; }
    public List<Consulta> getHistorial() { return historial; }
    public Consulta getUltimaConsulta() { return historial.isEmpty() ? null : historial.get(0); }
    public List<Alergia> getCatalogoAlergias() { return catalogoAlergias; }
    public List<Long> getAlergiasSeleccionadas() { return alergiasSeleccionadas; }
    public void setAlergiasSeleccionadas(List<Long> alergiasSeleccionadas) { this.alergiasSeleccionadas = alergiasSeleccionadas; }
    public Consulta getConsultaActual() { return consultaActual; }
    public void setConsultaActual(Consulta consultaActual) { this.consultaActual = consultaActual; }
    public int getTabActivo() { return tabActivo; }
    public void setTabActivo(int tabActivo) { this.tabActivo = tabActivo; }
    public Date getEvolucionDesde() { return evolucionDesde; }
    public void setEvolucionDesde(Date evolucionDesde) { this.evolucionDesde = evolucionDesde; }
    public Date getEvolucionHasta() { return evolucionHasta; }
    public void setEvolucionHasta(Date evolucionHasta) { this.evolucionHasta = evolucionHasta; }
    public LineChartModel getGraficaPeso() { return graficaPeso; }
    public LineChartModel getGraficaImc() { return graficaImc; }
    public LineChartModel getGraficaGrasa() { return graficaGrasa; }
    public LineChartModel getGraficaMusculo() { return graficaMusculo; }
    public Consulta getConsultaAnterior() { return consultaAnterior; }
    public boolean isHayComparativo() { return hayComparativo; }
    public PlanTratamiento getPlanActual() { return planActual; }
    public void setPlanActual(PlanTratamiento planActual) { this.planActual = planActual; }
    public List<PlanTratamiento> getPlanesGuardados() { return planesGuardados; }
}
