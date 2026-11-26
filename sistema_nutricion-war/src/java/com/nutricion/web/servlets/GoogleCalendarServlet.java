package com.nutricion.web.servlets;

import com.nutricion.servicios.GoogleCalendarClient;
import com.nutricion.servicios.SincronizadorCalendario;
import com.nutricion.util.ConfiguracionGoogle;
import com.nutricion.web.beans.GoogleCalendarBean.Solicitud;
import com.nutricion.web.beans.LoginBean;
import java.io.IOException;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/api/google/callback")
public class GoogleCalendarServlet extends HttpServlet {

    @EJB
    private SincronizadorCalendario sincronizador;

    @Inject
    private LoginBean login;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        String destino = req.getContextPath() + "/vistas/agenda/configuracion.xhtml?google=";
        ConfiguracionGoogle config = ConfiguracionGoogle.cargar();
        if (config == null || !login.isPuedeVerExpedientes() || !consumirSolicitud(req)) {
            res.sendRedirect(destino + "error");
            return;
        }
        String codigo = req.getParameter("code");
        if (codigo == null || codigo.isEmpty() || req.getParameter("error") != null) {
            res.sendRedirect(destino + "cancelado");
            return;
        }

        String resultado = "error";
        try (GoogleCalendarClient cliente = new GoogleCalendarClient()) {
            String token = cliente.canjear(config, codigo);
            String cifrado = config.cifrar(token, login.getConsultorioActivo().getId());
            sincronizador.vincular(login.getConsultorioActivo(), cifrado);
            resultado = "ok";
        } catch (RuntimeException e) {
            log("La autorización de Google Calendar falló (" + e.getClass().getSimpleName() + ").");
        }
        res.sendRedirect(destino + resultado);
    }

    private boolean consumirSolicitud(HttpServletRequest req) {
        HttpSession sesion = req.getSession(false);
        if (sesion == null) {
            return false;
        }
        synchronized (sesion) {
            Solicitud solicitud = (Solicitud) sesion.getAttribute(Solicitud.ATRIBUTO);
            if (solicitud == null || !solicitud.coincide(req.getParameter("state"),
                    login.getUsuario().getId(), login.getConsultorioActivo().getId())) {
                return false;
            }
            sesion.removeAttribute(Solicitud.ATRIBUTO);
            return true;
        }
    }
}
