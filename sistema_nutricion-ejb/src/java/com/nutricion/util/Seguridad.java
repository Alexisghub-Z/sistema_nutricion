package com.nutricion.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2 (incluido en el JDK, sin librerías extra).
 * Formato guardado: iteraciones:salBase64:hashBase64
 */
public final class Seguridad {

    private static final int ITERACIONES = 65536;
    private static final int LONGITUD = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private Seguridad() {
    }

    public static String hash(String password) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        byte[] h = pbkdf2(password.toCharArray(), sal, ITERACIONES);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean verificar(String password, String guardado) {
        if (password == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split(":");
        if (partes.length != 3) {
            return false;
        }
        int it = Integer.parseInt(partes[0]);
        byte[] sal = Base64.getDecoder().decode(partes[1]);
        byte[] esperado = Base64.getDecoder().decode(partes[2]);
        byte[] actual = pbkdf2(password.toCharArray(), sal, it);
        return java.security.MessageDigest.isEqual(esperado, actual);
    }

    /** Código corto y legible para que el paciente consulte su cita (sin 0/O ni 1/I). */
    public static String codigoCita() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CARACTERES_CODIGO.charAt(RANDOM.nextInt(CARACTERES_CODIGO.length())));
        }
        return sb.toString();
    }

    private static byte[] pbkdf2(char[] password, byte[] sal, int iteraciones) {
        try {
            KeySpec spec = new PBEKeySpec(password, sal, iteraciones, LONGITUD);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }
}
