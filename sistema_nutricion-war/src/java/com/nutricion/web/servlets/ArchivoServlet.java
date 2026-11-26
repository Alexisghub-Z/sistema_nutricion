package com.nutricion.web.servlets;

import com.nutricion.entidades.ArchivoAdjunto;
import com.nutricion.entidades.Rol;
import com.nutricion.servicios.ArchivoFacade;
import com.nutricion.util.AlmacenArchivos;
import com.nutricion.web.beans.LoginBean;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/archivo")
public class ArchivoServlet extends HttpServlet {

    @EJB
    private ArchivoFacade archivos;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession sesion = req.getSession(false);
        Rol rol = sesion == null ? null : (Rol) sesion.getAttribute(LoginBean.ATRIBUTO_ROL);
        Long consultorio = sesion == null ? null : (Long) sesion.getAttribute(LoginBean.ATRIBUTO_CONSULTORIO);
        if (consultorio == null || (rol != Rol.ADMIN && rol != Rol.NUTRIOLOGO)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long id;
        try {
            id = Long.valueOf(req.getParameter("id"));
        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ArchivoAdjunto archivo = archivos.paraDescarga(id, consultorio, rol);
        if (archivo == null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path ubicacion = AlmacenArchivos.ubicacion(archivo.getRutaDisco());
        try (InputStream contenido = Files.newInputStream(ubicacion, LinkOption.NOFOLLOW_LINKS)) {
            String nombre = URLEncoder.encode(archivo.getNombreOriginal(), StandardCharsets.UTF_8)
                    .replace("+", "%20").replace("*", "%2A");
            boolean imagen = archivo.getMimeType().startsWith("image/");
            String disposicion = imagen && "1".equals(req.getParameter("vista")) ? "inline" : "attachment";
            res.setContentType(archivo.getMimeType());
            res.setContentLengthLong(archivo.getTamano());
            res.setHeader("Content-Disposition", disposicion + "; filename*=UTF-8''" + nombre);
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
            contenido.transferTo(res.getOutputStream());
        } catch (NoSuchFileException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
