package com.nutricion.web.filtros;

import com.nutricion.entidades.Rol;
import com.nutricion.servicios.UsuarioFacade;
import com.nutricion.web.beans.LoginBean;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Regresión de la expiración de sesión con el sondeo automático de recordatorios. */
public class FiltroAccesoTest {

    public static void main(String[] args) throws Exception {
        FiltroAcceso filtro = new FiltroAcceso();
        Field usuarios = FiltroAcceso.class.getDeclaredField("usuarioFacade");
        usuarios.setAccessible(true);
        usuarios.set(filtro, new UsuarioFacade() {
            @Override
            public boolean sesionVigente(Long id, Rol rol, Long consultorioId) {
                return true;
            }
        });

        long inicio = System.currentTimeMillis() - 10_000;
        Solicitud automatico = new Solicitud(inicio);
        automatico.invocar(filtro, "frmRecordatorios:pollRecordatorios");
        verificar("El sondeo de avisos conserva el instante de actividad",
                automatico.permitida && !automatico.invalidada && automatico.actividad() == inicio);

        Solicitud manual = new Solicitud(inicio);
        manual.invocar(filtro, "frmPanel:actualizar");
        verificar("Una acción de la interfaz registra actividad",
                manual.permitida && !manual.invalidada && manual.actividad() > inicio);

        Solicitud expirada = new Solicitud(System.currentTimeMillis() - 1_800_001);
        expirada.invocar(filtro, "frmRecordatorios:pollRecordatorios");
        verificar("El sondeo redirige a acceso después de 30 minutos de inactividad",
                !expirada.permitida && expirada.invalidada
                && expirada.respuesta.toString().contains("<redirect url=\"/nutricion/acceso.xhtml\"/>"));

        Solicitud previa = new Solicitud(null);
        previa.invocar(filtro, "frmPanel:actualizar");
        verificar("Una sesión previa registra actividad al abrir la interfaz",
                previa.permitida && !previa.invalidada && previa.actividad() > inicio);
        System.out.println("RESULTADO 4 comprobaciones de actividad de sesion correctas");
    }

    private static void verificar(String nombre, boolean correcta) {
        if (!correcta) {
            throw new AssertionError(nombre);
        }
        System.out.println("PASS " + nombre);
    }

    private static <T> T simular(Class<T> tipo, InvocationHandler manejador) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, manejador));
    }

    private static class Solicitud {
        private final Map<String, Object> atributos = new HashMap<>();
        private final StringWriter respuesta = new StringWriter();
        private boolean permitida;
        private boolean invalidada;

        Solicitud(Long ultimaActividad) {
            atributos.put(LoginBean.ATRIBUTO_USUARIO, 1L);
            atributos.put(LoginBean.ATRIBUTO_ROL, Rol.NUTRIOLOGO);
            atributos.put(LoginBean.ATRIBUTO_CONSULTORIO, 1L);
            atributos.put(LoginBean.ATRIBUTO_ULTIMA_ACTIVIDAD, ultimaActividad);
        }

        long actividad() {
            return (Long) atributos.get(LoginBean.ATRIBUTO_ULTIMA_ACTIVIDAD);
        }

        void invocar(FiltroAcceso filtro, String origen) throws Exception {
            HttpSession sesion = simular(HttpSession.class, (proxy, metodo, args) -> {
                switch (metodo.getName()) {
                    case "getAttribute": return atributos.get(args[0]);
                    case "setAttribute": atributos.put((String) args[0], args[1]); return null;
                    case "getMaxInactiveInterval": return 1800;
                    case "invalidate": invalidada = true; return null;
                    default: throw new AssertionError(metodo);
                }
            });
            HttpServletRequest peticion = simular(HttpServletRequest.class, (proxy, metodo, args) -> {
                switch (metodo.getName()) {
                    case "getSession": return sesion;
                    case "getContextPath": return "/nutricion";
                    case "getServletPath": return "/vistas/dashboard.xhtml";
                    case "getHeader": return "Faces-Request".equals(args[0]) ? "partial/ajax" : null;
                    case "getParameter": return "javax.faces.source".equals(args[0]) ? origen : null;
                    default: throw new AssertionError(metodo);
                }
            });
            HttpServletResponse resultado = simular(HttpServletResponse.class, (proxy, metodo, args) -> {
                switch (metodo.getName()) {
                    case "setHeader":
                    case "setContentType": return null;
                    case "getWriter": return new PrintWriter(respuesta);
                    default: throw new AssertionError(metodo);
                }
            });
            filtro.doFilter(peticion, resultado, (req, res) -> permitida = true);
        }
    }
}
