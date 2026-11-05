package com.nutricion.entidades;

import com.nutricion.util.CalculosNutricion;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "consultas")
@NamedQueries({
    @NamedQuery(name = "Consulta.porPaciente", query = "SELECT c FROM Consulta c WHERE c.paciente = :paciente ORDER BY c.fecha DESC"),
    @NamedQuery(name = "Consulta.porPacienteAsc", query = "SELECT c FROM Consulta c WHERE c.paciente = :paciente ORDER BY c.fecha"),
    @NamedQuery(name = "Consulta.recientes", query = "SELECT c FROM Consulta c ORDER BY c.fecha DESC"),
    @NamedQuery(name = "Consulta.contarEntre", query = "SELECT COUNT(c) FROM Consulta c WHERE c.fecha >= :desde AND c.fecha < :hasta"),
    @NamedQuery(name = "Consulta.ingresosEntre", query = "SELECT SUM(c.montoConsulta) FROM Consulta c WHERE c.fecha >= :desde AND c.fecha < :hasta AND c.estadoPago = com.nutricion.entidades.EstadoPago.PAGADO")
})
public class Consulta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Opcional: las consultas históricas pueden no tener cita. */
    @OneToOne
    @JoinColumn(name = "cita_id", unique = true)
    private Cita cita;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date fecha;

    @Column(length = 500)
    private String motivo;

    // ---- Antropometría ----
    /** kg */
    private Double peso;
    /** metros */
    private Double talla;
    /** Calculado automáticamente a partir de peso y talla. */
    private Double imc;
    /** porcentaje */
    @Column(name = "grasa_corporal")
    private Double grasaCorporal;
    /** kg */
    @Column(name = "masa_muscular_kg")
    private Double masaMuscular;
    /** porcentaje */
    @Column(name = "porcentaje_agua")
    private Double porcentajeAgua;
    /** cm */
    private Double cintura;
    /** cm */
    private Double cadera;

    // ---- Notas clínicas ----
    @Column(length = 2000)
    private String notas;
    @Column(length = 2000)
    private String diagnostico;
    @Column(length = 2000)
    private String objetivo;
    @Column(name = "plan_nutricional", length = 4000)
    private String plan;

    @Temporal(TemporalType.DATE)
    @Column(name = "proxima_cita")
    private Date proximaCita;

    // ---- Pago ----
    @Column(name = "monto_consulta", precision = 10, scale = 2)
    private BigDecimal montoConsulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.PAGADO;

    @PrePersist
    @PreUpdate
    void calcular() {
        if (fecha == null) {
            fecha = new Date();
        }
        imc = CalculosNutricion.imc(peso, talla);
    }

    /** Clasificación OMS del IMC. */
    public String getClasificacionImc() {
        return CalculosNutricion.clasificarImc(imc);
    }

    /** Índice cintura/cadera. */
    public Double getIndiceCinturaCadera() {
        return CalculosNutricion.indiceCinturaCadera(cintura, cadera);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public Double getTalla() { return talla; }
    public void setTalla(Double talla) { this.talla = talla; }
    public Double getImc() { return imc; }
    public void setImc(Double imc) { this.imc = imc; }
    public Double getGrasaCorporal() { return grasaCorporal; }
    public void setGrasaCorporal(Double grasaCorporal) { this.grasaCorporal = grasaCorporal; }
    public Double getMasaMuscular() { return masaMuscular; }
    public void setMasaMuscular(Double masaMuscular) { this.masaMuscular = masaMuscular; }
    public Double getPorcentajeAgua() { return porcentajeAgua; }
    public void setPorcentajeAgua(Double porcentajeAgua) { this.porcentajeAgua = porcentajeAgua; }
    public Double getCintura() { return cintura; }
    public void setCintura(Double cintura) { this.cintura = cintura; }
    public Double getCadera() { return cadera; }
    public void setCadera(Double cadera) { this.cadera = cadera; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public Date getProximaCita() { return proximaCita; }
    public void setProximaCita(Date proximaCita) { this.proximaCita = proximaCita; }
    public BigDecimal getMontoConsulta() { return montoConsulta; }
    public void setMontoConsulta(BigDecimal montoConsulta) { this.montoConsulta = montoConsulta; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
    public EstadoPago getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPago estadoPago) { this.estadoPago = estadoPago; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Consulta && id != null && id.equals(((Consulta) o).id);
    }
}
