package com.nutricion.servicios;

import com.nutricion.util.ConfiguracionGoogle;
import com.nutricion.util.ReglaNegocioException;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/** OAuth de servidor y Calendar v3. La agenda se limita al calendario principal. */
public final class GoogleCalendarClient implements AutoCloseable {

    private static final String ALCANCE = "https://www.googleapis.com/auth/calendar.events.owned";
    private static final String MARCA_PRIVADA = "nutrivida";

    private final Client http;
    private final WebTarget tokenUrl;
    private final WebTarget eventosUrl;

    public GoogleCalendarClient() {
        this("https://oauth2.googleapis.com/token",
                "https://www.googleapis.com/calendar/v3/calendars/primary/events");
    }

    // El constructor de paquete permite probar el protocolo con un servidor de prueba local.
    GoogleCalendarClient(String tokenUrl, String eventosUrl) {
        http = ClientBuilder.newBuilder().connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS).property("jersey.config.client.followRedirects", false).build();
        this.tokenUrl = http.target(tokenUrl);
        this.eventosUrl = http.target(eventosUrl);
    }

    // https://developers.google.com/identity/protocols/oauth2/web-server
    public static URI autorizacion(ConfiguracionGoogle config, String estado) {
        return UriBuilder.fromUri("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", config.getCliente()).queryParam("redirect_uri", config.getRetorno())
                .queryParam("response_type", "code").queryParam("scope", ALCANCE)
                .queryParam("access_type", "offline").queryParam("prompt", "consent")
                .queryParam("state", estado).build();
    }

    public String canjear(ConfiguracionGoogle config, String codigo) {
        return pedirToken(config, new Form("grant_type", "authorization_code")
                .param("code", codigo).param("redirect_uri", config.getRetorno().toASCIIString()), "refresh_token");
    }

    String refrescar(ConfiguracionGoogle config, String token) {
        return pedirToken(config, new Form("grant_type", "refresh_token")
                .param("refresh_token", token), "access_token");
    }

    private String pedirToken(ConfiguracionGoogle config, Form solicitud, String campo) {
        solicitud.param("client_id", config.getCliente()).param("client_secret", config.getSecreto());
        try (Response response = tokenUrl.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.form(solicitud))) {
            String token = json(response).getString(campo, "");
            if (token.isEmpty()) {
                throw new ReglaNegocioException("Google no entregó una autorización válida para sincronizar.");
            }
            return token;
        }
    }

    // https://developers.google.com/workspace/calendar/api/v3/reference/events/list
    List<Evento> eventos(String token, Instant desde, Instant hasta) {
        WebTarget consulta = eventosUrl.queryParam("singleEvents", true).queryParam("timeMin", desde.toString())
                .queryParam("timeMax", hasta.toString()).queryParam("maxResults", 250)
                .queryParam("fields", "nextPageToken,timeZone,items(id,status,transparency,eventType,start,end,extendedProperties(private))");
        List<Evento> eventos = new ArrayList<>();
        String pagina = "";
        do {
            WebTarget target = pagina.isEmpty() ? consulta : consulta.queryParam("pageToken", pagina);
            try (Response response = peticion(target, token).get()) {
                JsonObject datos = json(response);
                ZoneId zona = ZoneId.of(datos.getString("timeZone", "America/Mexico_City"));
                JsonArray items = datos.getJsonArray("items");
                for (JsonValue valor : items == null ? JsonValue.EMPTY_JSON_ARRAY : items) {
                    JsonObject item = (JsonObject) valor;
                    if ("cancelled".equals(item.getString("status", ""))) {
                        continue;
                    }
                    Evento evento = new Evento(item, zona);
                    if (evento.inicio.isBefore(evento.fin)) {
                        eventos.add(evento);
                    }
                }
                pagina = datos.getString("nextPageToken", "");
            }
        } while (!pagina.isEmpty());
        return eventos;
    }

    // https://developers.google.com/workspace/calendar/api/v3/reference/events/insert
    void publicar(String token, String id, String marca, Instant inicio, Instant fin, Evento existente) {
        JsonObjectBuilder contenido = Json.createObjectBuilder().add("summary", "Consulta NutriVida")
                .add("visibility", "private")
                .add("start", Json.createObjectBuilder().add("dateTime", inicio.toString()))
                .add("end", Json.createObjectBuilder().add("dateTime", fin.toString()))
                .add("extendedProperties", Json.createObjectBuilder()
                        .add("private", Json.createObjectBuilder().add(MARCA_PRIVADA, marca)));
        if (existente != null) {
            if (!existente.inicio.equals(inicio) || !existente.fin.equals(fin)) {
                actualizar(token, existente.id, contenido.build());
            }
            return;
        }
        JsonObject datos = contenido.add("id", id).build();
        if (insertar(token, datos)) {
            return;
        }
        // Un alta anterior puede haber llegado a Google antes de cortar la conexión.
        boolean eliminado;
        try (Response response = peticion(eventosUrl.path(id), token).get()) {
            eliminado = response.getStatus() == 404 || response.getStatus() == 410;
            if (!eliminado) {
                JsonObject remoto = json(response);
                eliminado = "cancelled".equals(remoto.getString("status", ""));
                if (!eliminado && !marca.equals(marcaPrivada(remoto))) {
                    throw new ReglaNegocioException("Google devolvió un identificador de evento en uso.");
                }
            }
        }
        if (eliminado) {
            String nuevoId = id + UUID.randomUUID().toString().replace("-", "");
            if (!insertar(token, Json.createObjectBuilder(datos).add("id", nuevoId).build())) {
                throw new ReglaNegocioException("Google mantiene ocupado el identificador de la cita.");
            }
        } else {
            actualizar(token, id, datos);
        }
    }

    private boolean insertar(String token, JsonObject datos) {
        try (Response response = peticion(eventosUrl.queryParam("sendUpdates", "none"), token)
                .post(Entity.entity(datos.toString(), MediaType.APPLICATION_JSON_TYPE))) {
            if (response.getStatus() == 409) {
                return false;
            }
            verificar(response);
            return true;
        }
    }

    private void actualizar(String token, String id, JsonObject datos) {
        try (Response response = peticion(eventosUrl.path(id).queryParam("sendUpdates", "none"), token)
                .put(Entity.entity(datos.toString(), MediaType.APPLICATION_JSON_TYPE))) {
            verificar(response);
        }
    }

    void cancelar(String token, String id) {
        try (Response response = peticion(eventosUrl.path(id).queryParam("sendUpdates", "none"), token).delete()) {
            if (response.getStatus() != 404 && response.getStatus() != 410) {
                verificar(response);
            }
        }
    }

    private Invocation.Builder peticion(WebTarget target, String token) {
        return target.request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + token);
    }

    private static JsonObject json(Response response) {
        verificar(response);
        try (JsonReader reader = Json.createReader(new StringReader(response.readEntity(String.class)))) {
            return reader.readObject();
        }
    }

    private static void verificar(Response response) {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new ReglaNegocioException("Google Calendar respondió HTTP " + response.getStatus() + ".");
        }
    }

    private static String marcaPrivada(JsonObject item) {
        JsonObject extensiones = item.getJsonObject("extendedProperties");
        JsonObject valores = extensiones == null ? null : extensiones.getJsonObject("private");
        return valores == null ? "" : valores.getString(MARCA_PRIVADA, "");
    }

    private static Instant instante(JsonObject fecha, ZoneId zona) {
        String dateTime = fecha.getString("dateTime", "");
        return dateTime.isEmpty()
                ? LocalDate.parse(fecha.getString("date")).atStartOfDay(zona).toInstant()
                : OffsetDateTime.parse(dateTime).toInstant();
    }

    static final class Evento {
        final String id;
        final String marca;
        final Instant inicio;
        final Instant fin;
        final boolean ocupado;

        Evento(JsonObject item, ZoneId zona) {
            id = item.getString("id");
            marca = marcaPrivada(item);
            inicio = instante(item.getJsonObject("start"), zona);
            fin = instante(item.getJsonObject("end"), zona);
            ocupado = !"transparent".equals(item.getString("transparency", "opaque"))
                    && !"workingLocation".equals(item.getString("eventType", ""));
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
