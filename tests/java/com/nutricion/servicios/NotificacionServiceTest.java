package com.nutricion.servicios;

import com.sun.net.httpserver.HttpServer;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class NotificacionServiceTest {

    private static int total;

    public static void main(String[] args) throws Exception {
        String fecha = "27/11/2026";
        String hora = "09:30";
        String ubicacion = "Consultorio QA, Calle Ficticia 12, Oaxaca";
        String codigo = "NUQA1234";
        Properties propiedades = new Properties();
        propiedades.setProperty("mail.from", "nutrivida@example.invalid");
        MimeMessage correo = NotificacionService.crearCorreo(Session.getInstance(propiedades),
                "paciente@example.invalid", fecha, hora, ubicacion, codigo);
        comprobar(correo.getRecipients(Message.RecipientType.TO).length == 1
                && correo.getRecipients(Message.RecipientType.TO)[0].toString().equals("paciente@example.invalid"),
                "Correo al destinatario de la reserva");
        String cuerpo = (String) correo.getContent();
        comprobar(cuerpo.contains(fecha) && cuerpo.contains(hora)
                && cuerpo.contains(ubicacion) && cuerpo.contains(codigo), "Correo con datos de la cita");

        NotificacionService servicio = new NotificacionService();
        comprobar(servicio.telefonoWhatsappValido("+52 (000) 000-0000"), "WhatsApp con número internacional");
        comprobar(!servicio.telefonoWhatsappValido("000 000 0000"), "WhatsApp requiere código de país");

        JsonObject mensaje = NotificacionService.crearWhatsapp("520000000000", "reserva_qa", "es_MX",
                fecha, hora, ubicacion, codigo);
        JsonObject plantilla = mensaje.getJsonObject("template");
        JsonArray parametros = plantilla.getJsonArray("components").getJsonObject(0).getJsonArray("parameters");
        comprobar(mensaje.getString("to").equals("520000000000") && parametros.size() == 4
                && parametros.getJsonObject(0).getString("text").equals(fecha)
                && parametros.getJsonObject(1).getString("text").equals(hora)
                && parametros.getJsonObject(2).getString("text").equals(ubicacion)
                && parametros.getJsonObject(3).getString("text").equals(codigo), "Plantilla con los cuatro parámetros");

        AtomicInteger estado = new AtomicInteger(200);
        AtomicReference<String> respuesta = new AtomicReference<>("{\"messages\":[{\"id\":\"qa-1\"}]}");
        AtomicReference<JsonObject> recibido = new AtomicReference<>();
        AtomicReference<String> autorizacion = new AtomicReference<>();
        AtomicReference<String> metodo = new AtomicReference<>();
        HttpServer proveedor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proveedor.createContext("/messages", solicitud -> {
            metodo.set(solicitud.getRequestMethod());
            autorizacion.set(solicitud.getRequestHeaders().getFirst("Authorization"));
            String entrada = new String(solicitud.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try (javax.json.JsonReader lector = Json.createReader(new StringReader(entrada))) {
                recibido.set(lector.readObject());
            }
            byte[] datos = respuesta.get().getBytes(StandardCharsets.UTF_8);
            solicitud.getResponseHeaders().set("Content-Type", "application/json");
            solicitud.sendResponseHeaders(estado.get(), datos.length);
            solicitud.getResponseBody().write(datos);
            solicitud.close();
        });
        proveedor.start();
        try {
            URI url = URI.create("http://127.0.0.1:" + proveedor.getAddress().getPort() + "/messages");
            comprobar(NotificacionService.enviarWhatsapp(url, "credencial-ficticia", mensaje),
                    "Confirmación aceptada por el proveedor HTTP ficticio");
            comprobar("POST".equals(metodo.get()) && "Bearer credencial-ficticia".equals(autorizacion.get())
                    && mensaje.equals(recibido.get()), "Solicitud HTTP con credencial y plantilla correctas");
            respuesta.set("{}");
            comprobar(!NotificacionService.enviarWhatsapp(url, "credencial-ficticia", mensaje),
                    "Respuesta sin identificador no cuenta como envío");
            estado.set(400);
            comprobar(!NotificacionService.enviarWhatsapp(url, "credencial-ficticia", mensaje),
                    "Rechazo HTTP no cuenta como envío");
        } finally {
            proveedor.stop(0);
        }
        System.out.println("RESULTADO " + total + " comprobaciones de confirmaciones correctas");
    }

    private static void comprobar(boolean cumple, String descripcion) {
        if (!cumple) {
            throw new AssertionError(descripcion);
        }
        total++;
        System.out.println("PASS " + descripcion);
    }
}
