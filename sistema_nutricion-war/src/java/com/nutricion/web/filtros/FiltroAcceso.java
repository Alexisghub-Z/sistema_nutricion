package com.nutricion.web.filtros;

import com.nutricion.web.beans.LoginBean;
import java.io.IOException;
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

/** Protege todas las páginas de /vistas/*: sin sesión iniciada manda a acceso.xhtml. */
@WebFilter(urlPatterns = "/vistas/*")
public class FiltroAcceso implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession sesion = req.getSession(false);
        boolean conectado = sesion != null && sesion.getAttribute(LoginBean.ATRIBUTO_USUARIO) != null;
        if (conectado) {
            // Evita que el botón "atrás" muestre páginas privadas después de cerrar sesión.
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/acceso.xhtml");
        }
    }

    @Override
    public void destroy() {
    }
}
