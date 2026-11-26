package com.nutricion.servicios;

import com.nutricion.entidades.BloqueoCalendario;
import com.nutricion.entidades.Cita;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.EstadoCita;
import com.nutricion.entidades.VinculoGoogleCalendar;
import com.nutricion.util.ConfiguracionGoogle;
import com.nutricion.util.ReglaNegocioException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

/** Prueba de protocolo y sincronización con un proveedor HTTP ficticio en loopback. */
public final class SincronizadorCalendarioTest {

    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        System.setProperty("nutricion.google.clientId", "cliente-ficticio");
        System.setProperty("nutricion.google.clientSecret", "secreto-ficticio");
        System.setProperty("nutricion.google.redirectUri", "http://localhost:8080/demo/api/google/callback");
        System.setProperty("nutricion.google.clave", Base64.getEncoder().encodeToString(new byte[32]));
        ConfiguracionGoogle configuracion = ConfiguracionGoogle.cargar();
        String autorizacion = GoogleCalendarClient.autorizacion(configuracion, "estado-ficticio").toASCIIString();
        verificar(autorizacion.startsWith("https://accounts.google.com/")
                && autorizacion.contains("state=estado-ficticio") && autorizacion.contains("access_type=offline")
                && !autorizacion.contains("secreto-ficticio"), "Autorización offline con estado sin incluir el secreto");

        Consultorio consultorio = new Consultorio();
        consultorio.setId(1L);
        VinculoGoogleCalendar vinculo = new VinculoGoogleCalendar(consultorio);
        AgendaFicticia citas = new AgendaFicticia();
        citas.cita.setId(42L);
        citas.cita.setFechaHora(Date.from(Instant.parse("2026-11-27T16:00:00Z")));
        citas.cita.setDuracionMinutos(30);
        citas.cita.setEstado(EstadoCita.PENDIENTE);
        CalendarioFicticio bloques = new CalendarioFicticio();
        SincronizadorCalendario sincronizador = new SincronizadorCalendario();
        inyectar(sincronizador, "calendario", bloques);
        inyectar(sincronizador, "citas", citas);

        Proveedor proveedor = new Proveedor();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", proveedor::responder);
        servidor.start();
        String base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        Instant desde = Instant.parse("2026-11-26T00:00:00Z");
        try (GoogleCalendarClient cliente = new GoogleCalendarClient(base + "/token", base + "/events")) {
            String token = cliente.canjear(configuracion, "codigo-ficticio");
            vinculo.setTokenCifrado(configuracion.cifrar(token, consultorio.getId()));
            verificar("renovacion-ficticia".equals(token) && !vinculo.getTokenCifrado().contains(token),
                    "Canje y almacenamiento cifrado del token");
            boolean protegido = false;
            try {
                configuracion.descifrar(vinculo.getTokenCifrado(), 2L);
            } catch (IllegalStateException esperado) {
                protegido = true;
            }
            verificar(protegido, "El token cifrado está ligado al consultorio");

            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(bloques.lista.size() == 2 && proveedor.paginas == 2,
                    "Importa eventos ocupados de dos páginas y omite libres y cancelados");
            BloqueoCalendario parcial = bloques.lista.get(0);
            verificar(parcial.coincide(Date.from(Instant.parse("2026-11-27T15:30:00Z")),
                    Date.from(Instant.parse("2026-11-27T16:00:00Z")))
                    && !parcial.coincide(Date.from(Instant.parse("2026-11-27T16:00:00Z")),
                            Date.from(Instant.parse("2026-11-27T16:30:00Z"))),
                    "Detecta superposición y permite iniciar al terminar el bloqueo");
            verificar(bloques.lista.get(1).getInicio().toInstant().equals(Instant.parse("2026-11-28T06:00:00Z")),
                    "Los bloqueos de día completo usan la zona del calendario");
            JsonObject evento = proveedor.propios.values().iterator().next();
            verificar(proveedor.altas == 1 && "Consulta NutriVida".equals(evento.getString("summary"))
                    && "private".equals(evento.getString("visibility"))
                    && !evento.containsKey("attendees") && !evento.containsKey("description"),
                    "Publica la cita con título genérico y sin datos del paciente");

            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(proveedor.altas == 1 && proveedor.cambios == 0 && bloques.lista.size() == 2,
                    "El ciclo repetido no duplica citas ni importa sus propios eventos");
            citas.cita.setFechaHora(Date.from(Instant.parse("2026-11-27T17:00:00Z")));
            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(proveedor.cambios == 1 && proveedor.propios.values().iterator().next()
                    .getJsonObject("start").getString("dateTime").equals("2026-11-27T17:00:00Z"),
                    "Reprogramar actualiza el mismo evento");
            citas.cita.setEstado(EstadoCita.CANCELADA);
            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(proveedor.bajas == 1 && proveedor.propios.isEmpty(),
                    "La cancelación retira solo un evento publicado por la aplicación");

            proveedor.fallaListado = true;
            List<BloqueoCalendario> anterior = bloques.lista;
            boolean fallo = false;
            try {
                sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            } catch (ReglaNegocioException esperado) {
                fallo = esperado.getMessage().contains("503");
            }
            verificar(fallo && bloques.lista == anterior, "Una caída conserva los bloqueos de la última lectura válida");
            proveedor.fallaListado = false;
            proveedor.listadoVacio = true;
            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(bloques.lista.isEmpty(), "Una lectura válida vacía retira los bloqueos anteriores");

            proveedor.listadoVacio = false;
            citas.cita.setEstado(EstadoCita.PENDIENTE);
            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(proveedor.altas == 2 && proveedor.propios.size() == 1
                    && !proveedor.propios.containsKey(proveedor.borrado),
                    "Recupera una cita cuyo identificador sigue ocupado por un evento borrado");
            sincronizador.sincronizar(vinculo, configuracion, cliente, desde);
            verificar(proveedor.altas == 2 && proveedor.propios.size() == 1,
                    "El nuevo identificador también se reutiliza en el siguiente ciclo");
            verificar(proveedor.renovaciones == 8, "Cada ciclo renueva la credencial en el proveedor");
        } finally {
            servidor.stop(0);
        }
        System.out.println("RESULTADO " + comprobaciones + " comprobaciones de calendario correctas");
    }

    private static void inyectar(Object bean, String nombre, Object valor) throws Exception {
        Field campo = bean.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(bean, valor);
    }

    private static void verificar(boolean cumple, String caso) {
        if (!cumple) {
            throw new AssertionError(caso);
        }
        comprobaciones++;
        System.out.println("PASS " + caso);
    }

    private static final class AgendaFicticia extends CitaFacade {
        private final Cita cita = new Cita();

        @Override
        public List<Cita> entreFechas(Date inicio, Date fin, Consultorio consultorio) {
            return List.of(cita);
        }
    }

    private static final class CalendarioFicticio extends CalendarioGoogleFacade {
        private List<BloqueoCalendario> lista = new ArrayList<>();

        @Override
        public boolean guardarBloqueos(VinculoGoogleCalendar origen, List<BloqueoCalendario> nuevos) {
            lista = nuevos;
            return true;
        }
    }

    private static final class Proveedor {
        private final Map<String, JsonObject> propios = new LinkedHashMap<>();
        private int paginas;
        private int altas;
        private int cambios;
        private int bajas;
        private int renovaciones;
        private boolean fallaListado;
        private boolean listadoVacio;
        private String borrado;

        void responder(HttpExchange req) throws IOException {
            try {
                String ruta = req.getRequestURI().getPath();
                String metodo = req.getRequestMethod();
                if ("/token".equals(ruta) && "POST".equals(metodo)) {
                    String cuerpo = new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (!cuerpo.contains("client_secret=secreto-ficticio")) {
                        enviar(req, 403, "{}");
                    } else if (cuerpo.contains("grant_type=authorization_code&code=codigo-ficticio")) {
                        enviar(req, 200, "{\"refresh_token\":\"renovacion-ficticia\"}");
                    } else if (cuerpo.contains("grant_type=refresh_token&refresh_token=renovacion-ficticia")) {
                        renovaciones++;
                        enviar(req, 200, "{\"access_token\":\"acceso-ficticio\"}");
                    } else {
                        enviar(req, 400, "{}");
                    }
                    return;
                }
                if (!"Bearer acceso-ficticio".equals(req.getRequestHeaders().getFirst("Authorization"))) {
                    enviar(req, 401, "{}");
                    return;
                }
                JsonObject datos;
                switch (metodo) {
                    case "GET":
                        if (fallaListado) {
                            enviar(req, 503, "{}");
                            return;
                        }
                        if (ruta.equals("/events/" + borrado)) {
                            enviar(req, 410, "{}");
                            return;
                        }
                        enviar(req, 200, listadoVacio ? "{\"timeZone\":\"America/Mexico_City\"}" : listar(req).toString());
                        break;
                    case "POST":
                        datos = cuerpo(req);
                        if (datos.getString("id").equals(borrado)) {
                            enviar(req, 409, "{}");
                            return;
                        }
                        propios.put(datos.getString("id"), datos);
                        altas++;
                        enviar(req, 201, datos.toString());
                        break;
                    case "PUT":
                        String id = ruta.substring("/events/".length());
                        datos = Json.createObjectBuilder(cuerpo(req)).add("id", id).build();
                        propios.put(id, datos);
                        cambios++;
                        enviar(req, 200, datos.toString());
                        break;
                    case "DELETE":
                        String eliminado = ruta.substring("/events/".length());
                        if (propios.remove(eliminado) != null) {
                            borrado = eliminado;
                            bajas++;
                            req.sendResponseHeaders(204, -1);
                        } else {
                            enviar(req, 404, "{}");
                        }
                        break;
                    default:
                        enviar(req, 405, "{}");
                }
            } finally {
                req.close();
            }
        }

        private JsonObject listar(HttpExchange req) {
            paginas++;
            boolean segunda = req.getRequestURI().getQuery().contains("pageToken=segunda");
            JsonArrayBuilder items = Json.createArrayBuilder();
            if (segunda) {
                items.add(Json.createObjectBuilder().add("id", "dia")
                        .add("start", Json.createObjectBuilder().add("date", "2026-11-28"))
                        .add("end", Json.createObjectBuilder().add("date", "2026-11-29")));
                propios.values().forEach(items::add);
                return Json.createObjectBuilder().add("timeZone", "America/Mexico_City").add("items", items).build();
            }
            items.add(Json.createObjectBuilder(eventoExterno("ocupado")).add("transparency", "opaque"))
                    .add(Json.createObjectBuilder(eventoExterno("libre")).add("transparency", "transparent"))
                    .add(Json.createObjectBuilder().add("id", "cancelado").add("status", "cancelled"));
            return Json.createObjectBuilder().add("timeZone", "America/Mexico_City").add("items", items)
                    .add("nextPageToken", "segunda").build();
        }

        private JsonObject eventoExterno(String id) {
            return Json.createObjectBuilder().add("id", id)
                    .add("start", Json.createObjectBuilder().add("dateTime", "2026-11-27T15:00:00Z"))
                    .add("end", Json.createObjectBuilder().add("dateTime", "2026-11-27T16:00:00Z")).build();
        }

        private JsonObject cuerpo(HttpExchange req) throws IOException {
            try (JsonReader json = Json.createReader(new StringReader(
                    new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)))) {
                return json.readObject();
            }
        }

        private void enviar(HttpExchange req, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            req.getResponseHeaders().set("Content-Type", "application/json");
            req.sendResponseHeaders(status, bytes.length);
            req.getResponseBody().write(bytes);
        }
    }
}
