package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consulta;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.PacienteFacade;
import java.math.BigDecimal;
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

    @Inject
    private LoginBean loginBean;

    private long totalPacientes;
    private long consultasMes;
    private BigDecimal ingresosMes;
    private List<Cita> citasHoy;
    private List<Cita> proximas;
    private List<Consulta> recientes;

    @PostConstruct
    public void cargar() {
        Consultorio c = loginBean.getConsultorioActivo();
        totalPacientes = pacienteFacade.contar(c);
        consultasMes = consultaFacade.consultasDelMes(c);
        ingresosMes = consultaFacade.ingresosDelMes(c);
        citasHoy = citaFacade.delDia(new Date(), c);
        proximas = citaFacade.proximas(6, c);
        recientes = consultaFacade.recientes(5, c);
    }

    public long getTotalPacientes() { return totalPacientes; }
    public long getConsultasMes() { return consultasMes; }
    public BigDecimal getIngresosMes() { return ingresosMes; }
    public List<Cita> getCitasHoy() { return citasHoy; }
    public long getCitasPorAtenderHoy() {
        return citasHoy.stream().filter(cita -> cita.getEstado() == EstadoCita.PENDIENTE
                || cita.getEstado() == EstadoCita.CONFIRMADA || cita.getEstado() == EstadoCita.APARTADA).count();
    }
    public long getCitasCompletadasHoy() {
        return citasHoy.stream().filter(cita -> cita.getEstado() == EstadoCita.COMPLETADA).count();
    }
    public List<Cita> getProximas() { return proximas; }
    public List<Consulta> getRecientes() { return recientes; }
}
