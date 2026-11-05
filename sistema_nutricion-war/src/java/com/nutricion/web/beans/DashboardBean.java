package com.nutricion.web.beans;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consulta;
import com.nutricion.servicios.CitaFacade;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.PacienteFacade;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
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

    private long totalPacientes;
    private long consultasMes;
    private BigDecimal ingresosMes;
    private List<Cita> citasHoy;
    private List<Cita> proximas;
    private List<Consulta> recientes;

    @PostConstruct
    public void cargar() {
        totalPacientes = pacienteFacade.contar();
        consultasMes = consultaFacade.consultasDelMes();
        ingresosMes = consultaFacade.ingresosDelMes();
        citasHoy = citaFacade.delDia(new Date());
        proximas = citaFacade.proximas(6);
        recientes = consultaFacade.recientes(5);
    }

    public long getTotalPacientes() { return totalPacientes; }
    public long getConsultasMes() { return consultasMes; }
    public BigDecimal getIngresosMes() { return ingresosMes; }
    public List<Cita> getCitasHoy() { return citasHoy; }
    public List<Cita> getProximas() { return proximas; }
    public List<Consulta> getRecientes() { return recientes; }
}
