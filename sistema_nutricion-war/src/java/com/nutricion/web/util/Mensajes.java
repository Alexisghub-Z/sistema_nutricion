package com.nutricion.web.util;

import com.nutricion.util.ReglaNegocioException;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/** Atajos para mostrar mensajes en p:growl / p:messages. */
public final class Mensajes {

    private Mensajes() {
    }

    public static void info(String texto) {
        agregar(FacesMessage.SEVERITY_INFO, texto);
    }

    public static void error(String texto) {
        agregar(FacesMessage.SEVERITY_ERROR, texto);
    }

    /** Muestra el mensaje de una regla de negocio o uno genérico si es un error inesperado. */
    public static void error(Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof ReglaNegocioException) {
                error(t.getMessage());
                return;
            }
            t = t instanceof EJBException && ((EJBException) t).getCausedByException() != null
                    ? ((EJBException) t).getCausedByException() : t.getCause();
        }
        error("Ocurrió un error inesperado. Inténtalo de nuevo.");
        FacesContext.getCurrentInstance().getExternalContext().log("Error en la aplicación", e);
    }

    /** Conserva los mensajes después de una redirección. */
    public static void conservarTrasRedireccion() {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
    }

    private static void agregar(FacesMessage.Severity s, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, texto, null));
    }
}
