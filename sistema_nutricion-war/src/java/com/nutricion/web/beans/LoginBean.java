package com.nutricion.web.beans;

import com.nutricion.entidades.Usuario;
import com.nutricion.servicios.UsuarioFacade;
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

    @EJB
    private UsuarioFacade usuarioFacade;

    private String email;
    private String password;
    private Usuario usuario;

    public String entrar() {
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
        return "/vistas/dashboard?faces-redirect=true";
    }

    public String salir() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/acceso?faces-redirect=true";
    }

    public boolean isConectado() {
        return usuario != null;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Usuario getUsuario() { return usuario; }
}
