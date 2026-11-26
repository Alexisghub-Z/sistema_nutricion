package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Publicación de un aviso dentro de NutriVida. */
@Entity
@Table(name = "recordatorios",
        uniqueConstraints = @UniqueConstraint(name = "uq_recordatorios_cita_tipo", columnNames = {"cita_id", "tipo"}))
public class Recordatorio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecordatorio tipo = TipoRecordatorio.NUTRIOLOGO;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "programado_para", nullable = false)
    private Date programadoPara;

    protected Recordatorio() {
    }

    public Recordatorio(Cita cita, Date programadoPara) {
        this.cita = cita;
        this.programadoPara = programadoPara;
    }

    public Cita getCita() { return cita; }
    public Date getProgramadoPara() { return programadoPara; }
    public void setProgramadoPara(Date programadoPara) { this.programadoPara = programadoPara; }
}
