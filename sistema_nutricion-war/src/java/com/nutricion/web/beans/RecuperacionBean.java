package com.nutricion.web.beans;

import com.nutricion.servicios.RecuperacionCorreo;
import com.nutricion.servicios.UsuarioFacade;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.web.util.Mensajes;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@RequestScoped
public class RecuperacionBean {

    private static final String ULTIMA_SOLICITUD = "solicitudRecuperacion";

    @EJB
    private RecuperacionCorreo recuperacionCorreo;
    @EJB
    private UsuarioFacade usuarioFacade;

    private String correo;
    private String codigo;
    private String password;
    private String confirmacion;
    private boolean restablecida;

    public void solicitar() {
        Map<String, Object> sesion = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Long anterior = (Long) sesion.get(ULTIMA_SOLICITUD);
        long ahora = System.currentTimeMillis();
        if (anterior == null || ahora - anterior >= 10_000L) {
            sesion.put(ULTIMA_SOLICITUD, ahora);
            recuperacionCorreo.solicitar(correo);
        }
        Mensajes.info("Si la cuenta admite recuperación, recibirás un enlace en tu correo.");
        correo = null;
    }

    public void restablecer() {
        try {
            String destinatario = usuarioFacade.restablecerPassword(codigo, password, confirmacion);
            restablecida = true;
            codigo = null;
            recuperacionCorreo.notificarCambio(destinatario);
        } catch (ReglaNegocioException e) {
            Mensajes.error(e);
        } finally {
            password = null;
            confirmacion = null;
        }
    }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
    public boolean isRestablecida() { return restablecida; }
}
