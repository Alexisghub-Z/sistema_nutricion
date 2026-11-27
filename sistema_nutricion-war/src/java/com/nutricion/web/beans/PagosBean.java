package com.nutricion.web.beans;

import com.nutricion.entidades.MetodoPago;
import com.nutricion.servicios.ConsultaFacade;
import com.nutricion.servicios.PagoPendiente;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class PagosBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultaFacade consultaFacade;
    @Inject
    private LoginBean loginBean;

    private String paciente;
    private Date desde;
    private Date hasta;
    private MetodoPago metodo = MetodoPago.EFECTIVO;
    private List<PagoPendiente> pendientes = Collections.emptyList();

    @PostConstruct
    public void filtrar() {
        try {
            pendientes = consultaFacade.pagosPendientes(loginBean.getConsultorioActivo(), paciente, desde, hasta);
        } catch (ReglaNegocioException e) {
            pendientes = Collections.emptyList();
            Mensajes.error(e);
        }
    }

    public void limpiar() {
        paciente = null;
        desde = null;
        hasta = null;
        filtrar();
    }

    public void liquidar() {
        String consultaId = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("consultaCobro");
        try {
            boolean registrado = consultaFacade.liquidarPago(Long.valueOf(consultaId), loginBean.getConsultorioActivo(), metodo);
            filtrar();
            Mensajes.info(registrado ? "Pago registrado." : "El pago ya estaba registrado.");
        } catch (NumberFormatException e) {
            Mensajes.error("Selecciona un pago pendiente.");
        } catch (ReglaNegocioException e) {
            Mensajes.error(e);
        }
    }

    public String getPaciente() { return paciente; }
    public void setPaciente(String paciente) { this.paciente = paciente; }
    public Date getDesde() { return desde; }
    public void setDesde(Date desde) { this.desde = desde; }
    public Date getHasta() { return hasta; }
    public void setHasta(Date hasta) { this.hasta = hasta; }
    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }
    public List<PagoPendiente> getPendientes() { return pendientes; }
}
