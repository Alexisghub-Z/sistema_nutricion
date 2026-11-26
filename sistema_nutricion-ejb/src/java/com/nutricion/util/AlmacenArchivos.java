package com.nutricion.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

/** Archivos clínicos bajo un directorio privado de Payara. */
public final class AlmacenArchivos {

    public static final long TAMANO_MAXIMO = 10 * 1024 * 1024;
    private static final Logger LOG = Logger.getLogger(AlmacenArchivos.class.getName());
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private AlmacenArchivos() {
    }

    public static Path ubicacion(String clave) {
        if (!clave.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("Identificador de archivo inválido.");
        }
        String configurada = System.getProperty("nutricion.adjuntos.dir");
        Path raiz = configurada == null || configurada.trim().isEmpty()
                ? Paths.get(System.getProperty("com.sun.aas.instanceRoot"), "data", "nutricion-adjuntos")
                : Paths.get(configurada);
        return raiz.toAbsolutePath().normalize().resolve(clave);
    }

    public static String nombreValido(String enviado) {
        if (enviado == null) {
            throw new ReglaNegocioException("Selecciona un archivo.");
        }
        String nombre = enviado.substring(Math.max(enviado.lastIndexOf('/'), enviado.lastIndexOf('\\')) + 1).trim();
        if (nombre.isEmpty() || nombre.length() > 255 || nombre.chars().anyMatch(Character::isISOControl)) {
            throw new ReglaNegocioException("Escribe un nombre de archivo válido de hasta 255 caracteres.");
        }
        return nombre;
    }

    public static String guardar(Path destino, String nombre, InputStream origen, long tamano, boolean soloImagen)
            throws IOException {
        if (origen == null || tamano <= 0) {
            throw new ReglaNegocioException("Selecciona un archivo con contenido.");
        }
        if (tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("El archivo supera el límite de 10 MB.");
        }
        BufferedInputStream entrada = new BufferedInputStream(origen);
        entrada.mark(8);
        String tipo = tipoValido(nombre, entrada.readNBytes(8));
        entrada.reset();
        if (soloImagen && "application/pdf".equals(tipo)) {
            throw new ReglaNegocioException("Las fotografías de progreso deben ser JPG o PNG.");
        }

        Files.createDirectories(destino.getParent());
        OutputStream nuevo = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW);
        try (OutputStream salida = nuevo) {
            byte[] bloque = new byte[8192];
            long total = 0;
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                total += leidos;
                if (total > tamano) {
                    throw new IOException("Tamaño del adjunto inconsistente.");
                }
                salida.write(bloque, 0, leidos);
            }
            if (total != tamano) {
                throw new IOException("Carga de adjunto incompleta.");
            }
        } catch (IOException | RuntimeException e) {
            borrar(destino);
            throw e;
        }
        return tipo;
    }

    private static String tipoValido(String nombre, byte[] firma) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        if (minusculas.endsWith(".pdf") && new String(firma, StandardCharsets.US_ASCII).startsWith("%PDF-")) {
            return "application/pdf";
        }
        if (minusculas.endsWith(".png") && Arrays.equals(firma, FIRMA_PNG)) {
            return "image/png";
        }
        if ((minusculas.endsWith(".jpg") || minusculas.endsWith(".jpeg")) && firma.length >= 3
                && firma[0] == (byte) 0xff && firma[1] == (byte) 0xd8 && firma[2] == (byte) 0xff) {
            return "image/jpeg";
        }
        throw new ReglaNegocioException("Adjunta un PDF, JPG o PNG cuyo contenido coincida con la extensión.");
    }

    public static void borrar(Path destino) {
        try {
            Files.deleteIfExists(destino);
        } catch (IOException e) {
            LOG.warning("No se pudo borrar un archivo de adjuntos sin referencia.");
        }
    }
}
