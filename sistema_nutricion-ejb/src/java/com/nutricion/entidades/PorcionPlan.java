package com.nutricion.entidades;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class PorcionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TiempoComida tiempo;

    @Column(name = "codigo_alimento", nullable = false, length = 24)
    private String codigoAlimento;

    @Column(nullable = false, length = 80)
    private String alimento;

    @Column(nullable = false, length = 70)
    private String grupo;

    @Column(name = "medios_equivalentes", nullable = false)
    private int mediosEquivalentes;

    @Column(nullable = false, length = 50)
    private String medida;

    protected PorcionPlan() { }

    public PorcionPlan(TiempoComida tiempo, String codigoAlimento, String alimento, String grupo,
            int mediosEquivalentes, String medida) {
        this.tiempo = tiempo;
        this.codigoAlimento = codigoAlimento;
        this.alimento = alimento;
        this.grupo = grupo;
        this.mediosEquivalentes = mediosEquivalentes;
        this.medida = medida;
    }

    public TiempoComida getTiempo() { return tiempo; }
    public String getCodigoAlimento() { return codigoAlimento; }
    public String getAlimento() { return alimento; }
    public String getGrupo() { return grupo; }
    public int getMediosEquivalentes() { return mediosEquivalentes; }
    public double getEquivalentes() { return mediosEquivalentes / 2.0; }
    public String getMedida() { return medida; }
}
