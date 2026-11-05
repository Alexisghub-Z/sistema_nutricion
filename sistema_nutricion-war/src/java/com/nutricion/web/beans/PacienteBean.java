package com.nutricion.web.beans;

import com.nutricion.entidades.Paciente;
import com.nutricion.servicios.PacienteFacade;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

@Named
@ViewScoped
public class PacienteBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PacienteFacade pacienteFacade;

    private List<Paciente> pacientes;
    private String filtro;
    private Paciente seleccionado = new Paciente();

    @PostConstruct
    public void cargar() {
        pacientes = pacienteFacade.buscar(filtro);
    }

    public void buscar() {
        cargar();
    }

    public void nuevo() {
        seleccionado = new Paciente();
    }

    public void editar(Paciente p) {
        seleccionado = pacienteFacade.find(p.getId());
    }

    public void guardar() {
        try {
            boolean nuevo = seleccionado.getId() == null;
            pacienteFacade.guardar(seleccionado);
            Mensajes.info(nuevo ? "Paciente registrado." : "Paciente actualizado.");
            cargar();
            RequestContext.getCurrentInstance().execute("PF('dlgPaciente').hide()");
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public void eliminar(Paciente p) {
        try {
            pacienteFacade.eliminar(p.getId());
            Mensajes.info("Se eliminó a " + p.getNombre() + " junto con su historial.");
            cargar();
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public List<Paciente> getPacientes() { return pacientes; }
    public String getFiltro() { return filtro; }
    public void setFiltro(String filtro) { this.filtro = filtro; }
    public Paciente getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Paciente seleccionado) { this.seleccionado = seleccionado; }
}
