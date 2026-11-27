package com.nutricion.entidades;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.*;

@Entity
@Table(name = "planes_alimenticios")
public class PlanAlimenticio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_borrador", nullable = false, unique = true, length = 36)
    private String folioBorrador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private PlanTratamiento tratamiento;

    @Column(name = "meta_kcal", nullable = false)
    private double metaKcal;

    @Column(name = "meta_proteinas", nullable = false)
    private double metaProteinas;

    @Column(name = "meta_lipidos", nullable = false)
    private double metaLipidos;

    @Column(name = "meta_carbohidratos", nullable = false)
    private double metaCarbohidratos;

    private double kcal;
    private double proteinas;
    private double lipidos;
    private double carbohidratos;

    @ElementCollection
    @CollectionTable(name = "porciones_plan", joinColumns = @JoinColumn(name = "plan_id", nullable = false))
    @OrderColumn(name = "posicion", nullable = false)
    private List<PorcionPlan> porciones = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_aprobacion", nullable = false)
    private Date fechaAprobacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aprobado_por", nullable = false)
    private Usuario aprobadoPor;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_entrega")
    private Date fechaEntrega;

    protected PlanAlimenticio() { }

    public PlanAlimenticio(PlanTratamiento tratamiento, List<PorcionPlan> porciones,
            double kcal, double proteinas, double lipidos, double carbohidratos) {
        this.folioBorrador = UUID.randomUUID().toString();
        this.tratamiento = tratamiento;
        this.metaKcal = tratamiento.getMetaKcal();
        this.metaProteinas = tratamiento.getGramosProteinas();
        this.metaLipidos = tratamiento.getGramosLipidos();
        this.metaCarbohidratos = tratamiento.getGramosCarbohidratos();
        this.kcal = kcal;
        this.proteinas = proteinas;
        this.lipidos = lipidos;
        this.carbohidratos = carbohidratos;
        this.porciones.addAll(porciones);
    }

    public boolean coincideCon(PlanTratamiento vigente) {
        return tratamiento.getId().equals(vigente.getId())
                && Double.valueOf(metaKcal).equals(vigente.getMetaKcal())
                && Double.valueOf(metaProteinas).equals(vigente.getGramosProteinas())
                && Double.valueOf(metaLipidos).equals(vigente.getGramosLipidos())
                && Double.valueOf(metaCarbohidratos).equals(vigente.getGramosCarbohidratos());
    }

    public void aprobar(PlanTratamiento vigente, Usuario responsable) {
        this.tratamiento = vigente;
        this.aprobadoPor = responsable;
        this.fechaAprobacion = new Date();
    }

    public void entregar() { this.fechaEntrega = new Date(); }

    public List<PorcionPlan> porcionesDe(TiempoComida tiempo) {
        return porciones.stream().filter(p -> p.getTiempo() == tiempo).collect(Collectors.toList());
    }

    public static double margenEnergia(double meta) { return meta * 0.05; }
    public static double margenMacro(double meta) { return meta == 0 ? 0 : Math.max(2, meta * 0.10); }

    public Long getId() { return id; }
    public String getFolioBorrador() { return folioBorrador; }
    public double getMetaKcal() { return metaKcal; }
    public double getMetaProteinas() { return metaProteinas; }
    public double getMetaLipidos() { return metaLipidos; }
    public double getMetaCarbohidratos() { return metaCarbohidratos; }
    public double getMargenKcal() { return margenEnergia(metaKcal); }
    public double getMargenProteinas() { return margenMacro(metaProteinas); }
    public double getMargenLipidos() { return margenMacro(metaLipidos); }
    public double getMargenCarbohidratos() { return margenMacro(metaCarbohidratos); }
    public double getKcal() { return kcal; }
    public double getProteinas() { return proteinas; }
    public double getLipidos() { return lipidos; }
    public double getCarbohidratos() { return carbohidratos; }
    public List<PorcionPlan> getPorciones() { return Collections.unmodifiableList(porciones); }
    public Date getFechaAprobacion() { return fechaAprobacion; }
    public Date getFechaEntrega() { return fechaEntrega; }
}
