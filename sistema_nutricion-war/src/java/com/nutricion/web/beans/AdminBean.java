package com.nutricion.web.beans;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Rol;
import com.nutricion.entidades.Usuario;
import com.nutricion.servicios.ConsultorioFacade;
import com.nutricion.servicios.UsuarioFacade;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

/** Administración de consultorios y usuarios/roles (HU-01, HU-03, HU-05). */
@Named
@ViewScoped
public class AdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConsultorioFacade consultorioFacade;

    @EJB
    private UsuarioFacade usuarioFacade;

    @Inject
    private LoginBean loginBean;

    private List<Consultorio> consultorios;
    private List<Consultorio> consultoriosPublicos;
    private List<Usuario> usuarios;

    private Consultorio consultorioForm = new Consultorio();
    private Usuario administradorNuevo = new Usuario();
    private String passwordAdministrador;
    private Usuario usuarioForm = new Usuario();
    private String passwordNueva;

    @PostConstruct
    public void cargar() {
        Consultorio consultorio = consultorioFacade.find(loginBean.getConsultorioActivo().getId());
        consultorios = Collections.singletonList(consultorio);
        usuarios = usuarioFacade.porConsultorio(consultorio);
        actualizarConsultoriosPublicos();
    }

    public void actualizarConsultoriosPublicos() {
        consultoriosPublicos = consultorioFacade.activos();
    }

    public void nuevoConsultorio() {
        consultorioForm = new Consultorio();
        administradorNuevo = new Usuario();
        passwordAdministrador = null;
    }

    public void editarConsultorio(Consultorio c) {
        if (loginBean.getConsultorioActivo().equals(c)) {
            consultorioForm = consultorioFacade.find(c.getId());
        }
    }

    public void guardarConsultorio() {
        try {
            boolean nuevo = consultorioForm.getId() == null;
            if (nuevo) {
                consultorioFacade.registrar(consultorioForm, administradorNuevo, passwordAdministrador);
                Mensajes.info("Consultorio creado. Su administrador ya puede iniciar sesión.");
            } else if (loginBean.getConsultorioActivo().equals(consultorioForm)) {
                consultorioFacade.guardar(consultorioForm);
                Mensajes.info("Consultorio actualizado.");
            } else {
                throw new ReglaNegocioException("Selecciona tu consultorio.");
            }
            cargar();
            RequestContext.getCurrentInstance().execute("PF('dlgConsultorio').hide()");
        } catch (Exception e) {
            Mensajes.error(e);
        } finally {
            passwordAdministrador = null;
        }
    }

    public void nuevoUsuario() {
        usuarioForm = new Usuario();
        usuarioForm.setRol(Rol.NUTRIOLOGO);
        usuarioForm.setActivo(true);
        passwordNueva = null;
    }

    public void editarUsuario(Usuario u) {
        if (loginBean.getConsultorioActivo().equals(u.getConsultorio())) {
            usuarioForm = usuarioFacade.find(u.getId());
        }
        passwordNueva = null;
    }

    public void guardarUsuario() {
        try {
            boolean nuevo = usuarioForm.getId() == null;
            usuarioFacade.guardarUsuario(usuarioForm, passwordNueva, loginBean.getConsultorioActivo());
            Mensajes.info(nuevo ? "Usuario creado." : "Usuario actualizado.");
            cargar();
            RequestContext.getCurrentInstance().execute("PF('dlgUsuario').hide()");
        } catch (Exception e) {
            Mensajes.error(e);
        } finally {
            passwordNueva = null;
        }
    }

    public void desbloquearUsuario(Usuario u) {
        try {
            usuarioFacade.desbloquear(u.getId(), loginBean.getConsultorioActivo());
            Mensajes.info("Usuario desbloqueado.");
            cargar();
        } catch (Exception e) {
            Mensajes.error(e);
        }
    }

    public List<Consultorio> getConsultorios() { return consultorios; }
    public List<Consultorio> getConsultoriosPublicos() { return consultoriosPublicos; }
    public List<Usuario> getUsuarios() { return usuarios; }
    public Consultorio getConsultorioForm() { return consultorioForm; }
    public void setConsultorioForm(Consultorio consultorioForm) { this.consultorioForm = consultorioForm; }
    public Usuario getUsuarioForm() { return usuarioForm; }
    public void setUsuarioForm(Usuario usuarioForm) { this.usuarioForm = usuarioForm; }
    public Usuario getAdministradorNuevo() { return administradorNuevo; }
    public String getPasswordAdministrador() { return passwordAdministrador; }
    public void setPasswordAdministrador(String passwordAdministrador) { this.passwordAdministrador = passwordAdministrador; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
}
