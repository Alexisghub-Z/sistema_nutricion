package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "citas")
@NamedQueries({
    @NamedQuery(name = "Cita.todas", query = "SELECT c FROM Cita c ORDER BY c.fechaHora DESC"),
    @NamedQuery(name = "Cita.porEstado", query = "SELECT c FROM Cita c WHERE c.estado = :estado ORDER BY c.fechaHora DESC"),
    @NamedQuery(name = "Cita.entreFechas", query = "SELECT c FROM Cita c WHERE c.fechaHora >= :desde AND c.fechaHora < :hasta ORDER BY c.fechaHora"),
    @NamedQuery(name = "Cita.proximas", query = "SELECT c FROM Cita c WHERE c.fechaHora >= :desde AND c.estado = com.nutricion.entidades.EstadoCita.PENDIENTE ORDER BY c.fechaHora"),
    @NamedQuery(name = "Cita.porPaciente", query = "SELECT c FROM Cita c WHERE c.paciente = :paciente ORDER BY c.fechaHora DESC"),
    @NamedQuery(name = "Cita.porCodigo", query = "SELECT c FROM Cita c WHERE c.codigo = :codigo"),
    @NamedQuery(name = "Cita.choques", query = "SELECT c FROM Cita c WHERE c.estado = com.nutricion.entidades.EstadoCita.PENDIENTE AND c.fechaHora > :desde AND c.fechaHora < :hasta")
})
public class Cita implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_hora", nullable = false)
    private Date fechaHora;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos = 60;

    @Column(name = "motivo_consulta", nullable = false, length = 1000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cita", nullable = false, length = 20)
    private TipoCita tipo = TipoCita.PRESENCIAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    /** Código único que recibe el paciente para consultar su cita. */
    @Column(name = "codigo_cita", unique = true, length = 12)
    private String codigo;

    @OneToOne(mappedBy = "cita")
    private Consulta consulta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_registro", nullable = false)
    private Date fechaRegistro;

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = new Date();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Date getFechaHora() { return fechaHora; }
    public void setFechaHora(Date fechaHora) { this.fechaHora = fechaHora; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public TipoCita getTipo() { return tipo; }
    public void setTipo(TipoCita tipo) { this.tipo = tipo; }
    public EstadoCita getEstado() { return estado; }
    public void setEstado(EstadoCita estado) { this.estado = estado; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Consulta getConsulta() { return consulta; }
    public void setConsulta(Consulta consulta) { this.consulta = consulta; }
    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cita && id != null && id.equals(((Cita) o).id);
    }
}
