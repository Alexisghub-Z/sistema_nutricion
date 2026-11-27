package com.nutricion.entidades;

import com.nutricion.util.CalculosNutricion;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "planes_tratamiento")
@NamedQueries({
    @NamedQuery(name = "PlanTratamiento.porPaciente", query = "SELECT p FROM PlanTratamiento p WHERE p.paciente = :paciente ORDER BY p.fecha DESC, p.id DESC")
})
public class PlanTratamiento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ObjetivoTratamiento objetivo = ObjetivoTratamiento.MANTENIMIENTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormulaEnergetica formula = FormulaEnergetica.MIFFLIN_ST_JEOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_actividad", nullable = false, length = 30)
    private FactorActividad factorActividad = FactorActividad.MODERADO;

    @Column(name = "peso_usado")
    private Double pesoUsado;

    @Column(name = "talla_usada")
    private Double tallaUsada;

    @Column(name = "grasa_usada")
    private Double grasaUsada;

    private Double geb;

    @Column(name = "gasto_total")
    private Double gastoTotal;

    @Column(name = "ajuste_kcal")
    private Integer ajusteKcal = 0;

    @Column(name = "meta_kcal")
    private Double metaKcal;

    @Column(name = "pct_proteinas", nullable = false)
    private Integer pctProteinas = 20;

    @Column(name = "pct_lipidos", nullable = false)
    private Integer pctLipidos = 30;

    @Column(name = "pct_carbohidratos", nullable = false)
    private Integer pctCarbohidratos = 50;

    @Column(name = "gramos_proteinas")
    private Double gramosProteinas;

    @Column(name = "gramos_lipidos")
    private Double gramosLipidos;

    @Column(name = "gramos_carbohidratos")
    private Double gramosCarbohidratos;

    @Lob
    private String observaciones;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = new Date();
        }
        recalcularMacros();
    }

    public void recalcularMacros() {
        gramosProteinas = CalculosNutricion.gramosMacro(metaKcal, pctProteinas, 4);
        gramosLipidos = CalculosNutricion.gramosMacro(metaKcal, pctLipidos, 9);
        gramosCarbohidratos = CalculosNutricion.gramosMacro(metaKcal, pctCarbohidratos, 4);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public ObjetivoTratamiento getObjetivo() { return objetivo; }
    public void setObjetivo(ObjetivoTratamiento objetivo) { this.objetivo = objetivo; }
    public FormulaEnergetica getFormula() { return formula; }
    public void setFormula(FormulaEnergetica formula) { this.formula = formula; }
    public FactorActividad getFactorActividad() { return factorActividad; }
    public void setFactorActividad(FactorActividad factorActividad) { this.factorActividad = factorActividad; }
    public Double getPesoUsado() { return pesoUsado; }
    public void setPesoUsado(Double pesoUsado) { this.pesoUsado = pesoUsado; }
    public Double getTallaUsada() { return tallaUsada; }
    public void setTallaUsada(Double tallaUsada) { this.tallaUsada = tallaUsada; }
    public Double getGrasaUsada() { return grasaUsada; }
    public void setGrasaUsada(Double grasaUsada) { this.grasaUsada = grasaUsada; }
    public Double getGeb() { return geb; }
    public void setGeb(Double geb) { this.geb = geb; }
    public Double getGastoTotal() { return gastoTotal; }
    public void setGastoTotal(Double gastoTotal) { this.gastoTotal = gastoTotal; }
    public Integer getAjusteKcal() { return ajusteKcal; }
    public void setAjusteKcal(Integer ajusteKcal) { this.ajusteKcal = ajusteKcal; }
    public Double getMetaKcal() { return metaKcal; }
    public void setMetaKcal(Double metaKcal) { this.metaKcal = metaKcal; }
    public Integer getPctProteinas() { return pctProteinas; }
    public void setPctProteinas(Integer pctProteinas) { this.pctProteinas = pctProteinas; }
    public Integer getPctLipidos() { return pctLipidos; }
    public void setPctLipidos(Integer pctLipidos) { this.pctLipidos = pctLipidos; }
    public Integer getPctCarbohidratos() { return pctCarbohidratos; }
    public void setPctCarbohidratos(Integer pctCarbohidratos) { this.pctCarbohidratos = pctCarbohidratos; }
    public Double getGramosProteinas() { return gramosProteinas; }
    public void setGramosProteinas(Double gramosProteinas) { this.gramosProteinas = gramosProteinas; }
    public Double getGramosLipidos() { return gramosLipidos; }
    public void setGramosLipidos(Double gramosLipidos) { this.gramosLipidos = gramosLipidos; }
    public Double getGramosCarbohidratos() { return gramosCarbohidratos; }
    public void setGramosCarbohidratos(Double gramosCarbohidratos) { this.gramosCarbohidratos = gramosCarbohidratos; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlanTratamiento && id != null && id.equals(((PlanTratamiento) o).id);
    }
}
