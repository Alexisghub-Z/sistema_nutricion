package com.nutricion.web.beans;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Rol;
import com.nutricion.entidades.Usuario;
import com.nutricion.servicios.UsuarioFacade;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.web.util.Mensajes;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Named
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ATRIBUTO_USUARIO = "usuarioConectado";
    public static final String ATRIBUTO_ROL = "rolConectado";
    public static final String ATRIBUTO_CONSULTORIO = "consultorioConectado";

    @EJB
    private UsuarioFacade usuarioFacade;

    private String email;
    private String password;
    private Usuario usuario;

    public String entrar() {
        try {
            Usuario u = usuarioFacade.autenticar(email, password);
            password = null;
            if (u == null) {
                Mensajes.error("Correo o contraseña incorrectos.");
                return null;
            }
            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
            // Cambia el id de sesión al iniciar sesión (evita fijación de sesión).
            ((HttpServletRequest) ec.getRequest()).changeSessionId();
            usuario = u;
            ec.getSessionMap().put(ATRIBUTO_USUARIO, u.getId());
            ec.getSessionMap().put(ATRIBUTO_ROL, u.getRol());
            ec.getSessionMap().put(ATRIBUTO_CONSULTORIO, u.getConsultorio().getId());
            return (isRecepcion() ? "/vistas/agenda/agenda" : "/vistas/dashboard") + "?faces-redirect=true";
        } catch (ReglaNegocioException e) {
            password = null;
            Mensajes.error(e);
            return null;
        }
    }

    public String salir() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/acceso?faces-redirect=true";
    }

    public boolean isConectado() {
        return usuario != null;
    }

    public boolean isAdmin() {
        return usuario != null && usuario.getRol() == Rol.ADMIN;
    }

    public boolean isNutriologo() {
        return usuario != null && usuario.getRol() == Rol.NUTRIOLOGO;
    }

    public boolean isRecepcion() {
        return usuario != null && usuario.getRol() == Rol.RECEPCION;
    }

    /** Solo ADMIN y NUTRIOLOGO pueden consultar/editar expedientes clínicos e ingresos. */
    public boolean isPuedeVerExpedientes() {
        return isAdmin() || isNutriologo();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Usuario getUsuario() { return usuario; }
    public Consultorio getConsultorioActivo() { return usuario == null ? null : usuario.getConsultorio(); }
}
