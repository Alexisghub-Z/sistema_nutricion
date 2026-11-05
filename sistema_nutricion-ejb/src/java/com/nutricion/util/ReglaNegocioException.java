package com.nutricion.util;

import javax.ejb.ApplicationException;

/**
 * Error de validación de negocio. Revierte la transacción y llega tal cual
 * a la capa web para mostrarse como mensaje al usuario.
 */
@ApplicationException(rollback = true)
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
