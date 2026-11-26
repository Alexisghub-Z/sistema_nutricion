package com.nutricion.web.filtros;

import com.nutricion.entidades.Rol;
import com.nutricion.servicios.UsuarioFacade;
import com.nutricion.web.beans.LoginBean;
import java.io.IOException;
import javax.ejb.EJB;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Protege las vistas privadas y sus descargas con una sesión activa y el rol vigente. */
@WebFilter(urlPatterns = {"/vistas/*", "/archivo", "/api/google/callback"})
public class FiltroAcceso implements Filter {

    @EJB
    private UsuarioFacade usuarioFacade;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession sesion = req.getSession(false);
        Long id = sesion == null ? null : (Long) sesion.getAttribute(LoginBean.ATRIBUTO_USUARIO);
        Rol rol = sesion == null ? null : (Rol) sesion.getAttribute(LoginBean.ATRIBUTO_ROL);
        Long consultorio = sesion == null ? null : (Long) sesion.getAttribute(LoginBean.ATRIBUTO_CONSULTORIO);
        if (id == null || rol == null || consultorio == null || !usuarioFacade.sesionVigente(id, rol, consultorio)) {
            if (sesion != null && id != null) {
                sesion.invalidate();
            }
            redirigir(req, res, "/acceso.xhtml");
            return;
        }

        String ruta = req.getServletPath();

        if (rol == Rol.RECEPCION && !"/vistas/agenda/agenda.xhtml".equals(ruta)) {
            redirigir(req, res, "/vistas/agenda/agenda.xhtml?denegado=1");
            return;
        }
        if (ruta.startsWith("/vistas/admin") && rol != Rol.ADMIN) {
            redirigir(req, res, "/vistas/dashboard.xhtml?denegado=1");
            return;
        }
        // Evita que el botón "atrás" muestre páginas privadas después de cerrar sesión.
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        chain.doFilter(request, response);
    }

    private void redirigir(HttpServletRequest req, HttpServletResponse res, String destino) throws IOException {
        String url = req.getContextPath() + destino;
        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            res.setContentType("text/xml;charset=UTF-8");
            res.setHeader("Cache-Control", "no-store");
            res.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<partial-response><redirect url=\"" + url + "\"/></partial-response>");
        } else {
            res.sendRedirect(url);
        }
    }

    @Override
    public void destroy() {
    }
}
