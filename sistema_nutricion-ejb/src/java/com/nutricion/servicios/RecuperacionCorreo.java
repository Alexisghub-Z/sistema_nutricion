package com.nutricion.servicios;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RecuperacionCorreo {

    private static final Logger LOG = Logger.getLogger(RecuperacionCorreo.class.getName());

    @EJB
    private UsuarioFacade usuarioFacade;

    @Asynchronous
    public void solicitar(String correo) {
        if (correo == null) return;
        String destinatario = correo.trim().toLowerCase(Locale.ROOT);
        if (destinatario.length() > 120 || !destinatario.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return;
        try {
            String base = urlBase();
            if (base == null) return;
            Session sesion = sesionCorreo();
            String token = usuarioFacade.emitirEnlaceRecuperacion(destinatario);
            if (token != null) {
                enviar(sesion, destinatario, "Recupera tu acceso a NutriVida",
                        "Abre este enlace para cambiar tu contraseña:\n\n"
                        + base + "/restablecer.xhtml#codigo=" + token
                        + "\n\nEl enlace caduca en " + UsuarioFacade.MINUTOS_RECUPERACION
                        + " minutos y admite un cambio de contraseña. Si no solicitaste el cambio, omite este correo.");
            }
        } catch (NamingException | MessagingException | RuntimeException e) {
            LOG.warning("No se pudo enviar el correo de recuperación (" + e.getClass().getSimpleName() + ").");
        }
    }

    @Asynchronous
    public void notificarCambio(String destinatario) {
        try {
            enviar(sesionCorreo(), destinatario, "Cambió tu contraseña de NutriVida",
                    "Tu contraseña se actualizó. Si no realizaste este cambio, contacta al administrador de tu consultorio.");
        } catch (NamingException | MessagingException | RuntimeException e) {
            LOG.warning("No se pudo enviar el aviso de cambio de contraseña (" + e.getClass().getSimpleName() + ").");
        }
    }

    private Session sesionCorreo() throws NamingException {
        return InitialContext.doLookup(System.getProperty("nutricion.recuperacion.recursoCorreo", "mail/nutricion"));
    }

    private void enviar(Session sesion, String destinatario, String asunto, String cuerpo) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom();
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario, true));
        mensaje.setSubject(asunto, StandardCharsets.UTF_8.name());
        mensaje.setText(cuerpo, StandardCharsets.UTF_8.name());
        Transport.send(mensaje);
    }

    private String urlBase() {
        String valor = System.getProperty("nutricion.recuperacion.urlBase", "").trim();
        if (valor.isEmpty()) return null;
        URI uri = URI.create(valor);
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            return null;
        }
        boolean loopback = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !(loopback && "http".equalsIgnoreCase(uri.getScheme()))) {
            return null;
        }
        return valor.replaceFirst("/+$", "");
    }
}
