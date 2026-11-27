package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.DisponibilidadService;
import com.nutricion.servicios.Fechas;
import com.nutricion.servicios.PacienteFacade;
import com.nutricion.servicios.ResumenAsistencia;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class DashboardBean {

    @EJB
    private PacienteFacade pacienteFacade;
    @EJB
    private CitaFacade citaFacade;
    @EJB
    private ConsultaFacade consultaFacade;
    @EJB
    private DisponibilidadService disponibilidadService;

    @Inject
    private LoginBean loginBean;

    private long totalPacientes;
    private long pacientesActivos;
    private int diasPacienteActivo;
    private long consultasMes;
    private BigDecimal ingresosMes;
    private BigDecimal ingresosMesAnterior;
    private BigDecimal variacionIngresos;
    private List<Cita> citasHoy;
    private List<Cita> proximas;
    private List<ResumenAsistencia> asistencia;

    @PostConstruct
    public void cargar() {
        Consultorio c = loginBean.getConsultorioActivo();
        Date ahora = new Date();
        Date mes = Fechas.inicioDelMes(ahora);
        diasPacienteActivo = disponibilidadService.obtenerConfiguracion(c).getDiasPacienteActivo();
        totalPacientes = pacienteFacade.contar(c);
        pacientesActivos = consultaFacade.contarPacientesActivos(c, ahora, diasPacienteActivo);
        consultasMes = consultaFacade.consultasDelMes(c);
        ingresosMes = consultaFacade.ingresosEntre(c, mes, Fechas.inicioMesSiguiente(ahora));
        ingresosMesAnterior = consultaFacade.ingresosEntre(c, Fechas.inicioMesAnterior(ahora), mes);
        variacionIngresos = ingresosMesAnterior.signum() == 0 ? null : ingresosMes.subtract(ingresosMesAnterior)
                .movePointRight(2).divide(ingresosMesAnterior, 1, RoundingMode.HALF_UP);
        citasHoy = citaFacade.delDia(ahora, c);
        proximas = citaFacade.proximas(6, c);
    }

    public long getTotalPacientes() { return totalPacientes; }
    public long getPacientesActivos() { return pacientesActivos; }
    public int getDiasPacienteActivo() { return diasPacienteActivo; }
    public long getConsultasMes() { return consultasMes; }
    public BigDecimal getIngresosMes() { return ingresosMes; }
    public BigDecimal getIngresosMesAnterior() { return ingresosMesAnterior; }
    public BigDecimal getVariacionIngresos() { return variacionIngresos; }
    public List<Cita> getCitasHoy() { return citasHoy; }
    public long getCitasPorAtenderHoy() {
        return citasHoy.stream().filter(cita -> cita.getEstado() == EstadoCita.PENDIENTE
                || cita.getEstado() == EstadoCita.CONFIRMADA || cita.getEstado() == EstadoCita.APARTADA).count();
    }
    public long getCitasCompletadasHoy() {
        return citasHoy.stream().filter(cita -> cita.getEstado() == EstadoCita.COMPLETADA).count();
    }
    public List<Cita> getProximas() { return proximas; }
    public List<ResumenAsistencia> getAsistencia() {
        if (asistencia == null) {
            Date ahora = new Date();
            Date desde = Fechas.sumarDias(Fechas.inicioDelDia(ahora), 1 - diasPacienteActivo);
            asistencia = citaFacade.asistenciaEntre(loginBean.getConsultorioActivo(), desde, ahora);
        }
        return asistencia;
    }
    public int getUmbralAsistenciaBaja() { return ResumenAsistencia.UMBRAL_ASISTENCIA_BAJA; }
}
