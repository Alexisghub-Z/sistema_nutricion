package com.nutricion.web.beans;

import com.nutricion.entidades.ConfiguracionAgenda;
import com.nutricion.entidades.HorarioLaboral;
import com.nutricion.servicios.DisponibilidadService;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class ConfigAgendaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private DisponibilidadService disponibilidadService;

    @Inject
    private LoginBean loginBean;

    private ConfiguracionAgenda configuracion;
    private List<HorarioLaboral> horarios;

    @PostConstruct
    public void cargar() {
        configuracion = disponibilidadService.obtenerConfiguracion(loginBean.getConsultorioActivo());
        horarios = disponibilidadService.listarHorarios(loginBean.getConsultorioActivo());
    }

    public void guardar() {
        try {
            disponibilidadService.guardar(loginBean.getConsultorioActivo(), configuracion, horarios);
            cargar();
            Mensajes.info("Horarios laborales y parámetros de agenda guardados.");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public ConfiguracionAgenda getConfiguracion() { return configuracion; }
    public void setConfiguracion(ConfiguracionAgenda configuracion) { this.configuracion = configuracion; }
    public List<HorarioLaboral> getHorarios() { return horarios; }
    public void setHorarios(List<HorarioLaboral> horarios) { this.horarios = horarios; }
}
