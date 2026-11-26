package com.nutricion.util;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Configuración del servidor y protección de los tokens en MySQL. */
public final class ConfiguracionGoogle {

    private static final int LONGITUD_IV = 12;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final String cliente;
    private final String secreto;
    private final URI retorno;
    private final SecretKeySpec clave;

    private ConfiguracionGoogle(String cliente, String secreto, URI retorno, byte[] clave) {
        this.cliente = cliente;
        this.secreto = secreto;
        this.retorno = retorno;
        this.clave = new SecretKeySpec(clave, "AES");
    }

    public static ConfiguracionGoogle cargar() {
        String cliente = System.getProperty("nutricion.google.clientId", "").trim();
        String secreto = System.getProperty("nutricion.google.clientSecret", "").trim();
        try {
            URI retorno = URI.create(System.getProperty("nutricion.google.redirectUri", "").trim());
            byte[] clave = Base64.getDecoder().decode(System.getProperty("nutricion.google.clave", "").trim());
            boolean local = "localhost".equals(retorno.getHost()) || "127.0.0.1".equals(retorno.getHost());
            boolean valido = retorno.getHost() != null && retorno.getUserInfo() == null
                    && retorno.getQuery() == null && retorno.getFragment() == null
                    && retorno.getPath().endsWith("/api/google/callback")
                    && ("https".equals(retorno.getScheme()) || (local && "http".equals(retorno.getScheme())));
            return cliente.isEmpty() || secreto.isEmpty() || clave.length != 32 || !valido
                    ? null : new ConfiguracionGoogle(cliente, secreto, retorno, clave);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String cifrar(String token, long consultorio) {
        byte[] iv = new byte[LONGITUD_IV];
        ALEATORIO.nextBytes(iv);
        try {
            byte[] cifrado = cifrador(Cipher.ENCRYPT_MODE, iv, consultorio)
                    .doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo guardar la autorización de Calendar.");
        }
    }

    public String descifrar(String tokenCifrado, long consultorio) {
        ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(tokenCifrado));
        byte[] iv = new byte[LONGITUD_IV];
        buffer.get(iv);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);
        try {
            return new String(cifrador(Cipher.DECRYPT_MODE, iv, consultorio).doFinal(cifrado),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo leer la autorización de Calendar.");
        }
    }

    private Cipher cifrador(int modo, byte[] iv, long consultorio) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(modo, clave, new GCMParameterSpec(128, iv));
        cipher.updateAAD(ByteBuffer.allocate(Long.BYTES).putLong(consultorio).array());
        return cipher;
    }

    public String getCliente() { return cliente; }
    public String getSecreto() { return secreto; }
    public URI getRetorno() { return retorno; }
}
