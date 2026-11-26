package com.nutricion.web.beans;

import com.nutricion.entidades.VinculoGoogleCalendar;
import com.nutricion.servicios.CalendarioGoogleFacade;
import com.nutricion.servicios.GoogleCalendarClient;
import com.nutricion.servicios.SincronizadorCalendario;
import com.nutricion.util.ConfiguracionGoogle;
import com.nutricion.web.util.Mensajes;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class GoogleCalendarBean {

    @EJB
    private CalendarioGoogleFacade calendario;

    @EJB
    private SincronizadorCalendario sincronizador;

    @Inject
    private LoginBean login;

    private ConfiguracionGoogle configuracion;
    private VinculoGoogleCalendar vinculo;

    @PostConstruct
    public void cargar() {
        configuracion = ConfiguracionGoogle.cargar();
        if (login.isPuedeVerExpedientes()) {
            vinculo = calendario.vinculo(login.getConsultorioActivo());
        }
    }

    public void conectar() throws IOException {
        if (configuracion == null || !login.isPuedeVerExpedientes()) {
            return;
        }
        Solicitud solicitud = new Solicitud(login.getUsuario().getId(), login.getConsultorioActivo().getId());
        ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
        contexto.getSessionMap().put(Solicitud.ATRIBUTO, solicitud);
        contexto.redirect(GoogleCalendarClient.autorizacion(configuracion, solicitud.estado).toASCIIString());
    }

    public void desvincular() {
        if (login.isPuedeVerExpedientes()) {
            sincronizador.desvincular(login.getConsultorioActivo());
            cargar();
            Mensajes.info("Google Calendar se desvinculó de este consultorio.");
        }
    }

    public boolean isConfigurado() { return configuracion != null; }
    public boolean isVinculado() { return vinculo != null && vinculo.getTokenCifrado() != null; }
    public Date getUltimaSincronizacion() { return vinculo == null ? null : vinculo.getSincronizadoEn(); }
    public boolean isErrorSincronizacion() { return vinculo != null && vinculo.isErrorSincronizacion(); }

    public static final class Solicitud implements Serializable {
        private static final long serialVersionUID = 1L;
        public static final String ATRIBUTO = "nutricionSolicitudGoogle";
        private final String estado = UUID.randomUUID().toString();
        private final long vence = System.currentTimeMillis() + 10 * 60 * 1000L;
        private final Long usuario;
        private final Long consultorio;

        private Solicitud(Long usuario, Long consultorio) {
            this.usuario = usuario;
            this.consultorio = consultorio;
        }

        public boolean coincide(String recibido, Long usuarioActual, Long consultorioActual) {
            return recibido != null && recibido.length() == estado.length() && System.currentTimeMillis() < vence
                    && Objects.equals(usuario, usuarioActual) && Objects.equals(consultorio, consultorioActual)
                    && MessageDigest.isEqual(estado.getBytes(StandardCharsets.UTF_8),
                            recibido.getBytes(StandardCharsets.UTF_8));
        }
    }
}
