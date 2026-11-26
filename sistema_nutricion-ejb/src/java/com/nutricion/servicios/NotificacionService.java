package com.nutricion.servicios;

import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class.getName());
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String PREFIJO = "nutricion.whatsapp.";

    public boolean whatsappDisponible() {
        return endpointWhatsapp() != null && !propiedad("token").isEmpty()
                && !propiedad("plantilla").isEmpty() && !propiedad("idioma").isEmpty();
    }

    public boolean telefonoWhatsappValido(String telefono) {
        String normalizado = PacienteFacade.normalizarTelefono(telefono);
        return normalizado != null && normalizado.matches("\\+[1-9][0-9]{9,14}");
    }

    /**
     * Notifica después de guardar la cita. El resultado indica la aceptación
     * del mensaje por el servidor que lo recibe.
     */
    public boolean enviarConfirmacion(Cita cita, boolean porWhatsapp) {
        Consultorio consultorio = cita.getPaciente().getConsultorio();
        String direccion = consultorio.getDireccion();
        if (direccion == null || direccion.trim().isEmpty()) {
            return false;
        }
        String ubicacion = consultorio.getNombre() + ", " + direccion.trim();
        ZonedDateTime fechaHora = cita.getFechaHora().toInstant().atZone(ZoneId.systemDefault());
        String dia = DIA.format(fechaHora);
        String hora = HORA.format(fechaHora);
        try {
            if (porWhatsapp) {
                if (!whatsappDisponible() || !telefonoWhatsappValido(cita.getPaciente().getTelefono())) {
                    return false;
                }
                String numero = PacienteFacade.normalizarTelefono(cita.getPaciente().getTelefono()).substring(1);
                JsonObject mensaje = crearWhatsapp(numero, propiedad("plantilla"), propiedad("idioma"),
                        dia, hora, ubicacion, cita.getCodigo());
                return enviarWhatsapp(endpointWhatsapp(), propiedad("token"), mensaje);
            }
            // La búsqueda diferida permite reservar también en dominios sin correo.
            Session sesion = InitialContext.doLookup("mail/nutricion");
            Transport.send(crearCorreo(sesion, cita.getPaciente().getEmail(), dia, hora, ubicacion, cita.getCodigo()));
            return true;
        } catch (NameNotFoundException e) {
            return false;
        } catch (NamingException | MessagingException | RuntimeException e) {
            LOG.log(Level.WARNING, "Falló la notificación de reserva para la cita {0} ({1}).",
                    new Object[]{cita.getId(), e.getClass().getSimpleName()});
            return false;
        }
    }

    static MimeMessage crearCorreo(Session sesion, String destinatario, String dia,
            String hora, String ubicacion, String codigo) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom();
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario, true));
        mensaje.setSubject("Confirmación de reserva de NutriVida", StandardCharsets.UTF_8.name());
        mensaje.setText("Tu reserva quedó registrada.\n\nFecha: " + dia + "\nHora: " + hora
                + "\nUbicación: " + ubicacion + "\nCódigo de cita: " + codigo
                + "\n\nUsa tu código para consultar o cancelar la cita en NutriVida.",
                StandardCharsets.UTF_8.name());
        return mensaje;
    }

    static JsonObject crearWhatsapp(String numero, String plantilla, String idioma,
            String dia, String hora, String ubicacion, String codigo) {
        JsonArrayBuilder parametros = Json.createArrayBuilder();
        for (String texto : new String[]{dia, hora, ubicacion, codigo}) {
            parametros.add(Json.createObjectBuilder().add("type", "text").add("text", texto));
        }
        return Json.createObjectBuilder()
                .add("messaging_product", "whatsapp")
                .add("to", numero)
                .add("type", "template")
                .add("template", Json.createObjectBuilder()
                        .add("name", plantilla)
                        .add("language", Json.createObjectBuilder().add("code", idioma))
                        .add("components", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("type", "body").add("parameters", parametros))))
                .build();
    }

    static boolean enviarWhatsapp(URI endpoint, String token, JsonObject mensaje) {
        Client cliente = ClientBuilder.newBuilder().connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS).property("jersey.config.client.followRedirects", false).build();
        try (Response respuesta = cliente.target(endpoint).request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + token).post(Entity.json(mensaje.toString()))) {
            if (respuesta.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL || !respuesta.hasEntity()) {
                return false;
            }
            try (JsonReader json = Json.createReader(new StringReader(respuesta.readEntity(String.class)))) {
                JsonArray mensajes = json.readObject().getJsonArray("messages");
                return mensajes != null && !mensajes.isEmpty()
                        && !mensajes.getJsonObject(0).getString("id", "").isEmpty();
            }
        } finally {
            cliente.close();
        }
    }

    private static URI endpointWhatsapp() {
        String version = propiedad("version");
        String numeroId = propiedad("numeroId");
        if (!version.matches("v[0-9]+\\.[0-9]+") || !numeroId.matches("[0-9]+")) {
            return null;
        }
        return URI.create("https://graph.facebook.com/" + version + "/" + numeroId + "/messages");
    }

    private static String propiedad(String nombre) {
        return System.getProperty(PREFIJO + nombre, "").trim();
    }
}
