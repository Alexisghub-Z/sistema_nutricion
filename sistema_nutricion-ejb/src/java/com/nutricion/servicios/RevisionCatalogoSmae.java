package com.nutricion.servicios;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public final class RevisionCatalogoSmae implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Ficha> alimentos;
    private final List<String> sinCoincidencia;
    private final List<String> sinVerificar;

    RevisionCatalogoSmae(List<Ficha> alimentos, List<String> sinCoincidencia, List<String> sinVerificar) {
        this.alimentos = Collections.unmodifiableList(alimentos);
        this.sinCoincidencia = Collections.unmodifiableList(sinCoincidencia);
        this.sinVerificar = Collections.unmodifiableList(sinVerificar);
    }

    public List<Ficha> getAlimentos() { return alimentos; }
    public List<String> getSinCoincidencia() { return sinCoincidencia; }
    public List<String> getSinVerificar() { return sinVerificar; }

    public static final class Ficha implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String nombre;
        private final String grupo;
        private final String medida;
        private final String motivoExclusion;

        Ficha(String nombre, String grupo, String medida, String motivoExclusion) {
            this.nombre = nombre;
            this.grupo = grupo;
            this.medida = medida;
            this.motivoExclusion = motivoExclusion;
        }

        public String getNombre() { return nombre; }
        public String getGrupo() { return grupo; }
        public String getMedida() { return medida; }
        public String getMotivoExclusion() { return motivoExclusion; }
    }
}
