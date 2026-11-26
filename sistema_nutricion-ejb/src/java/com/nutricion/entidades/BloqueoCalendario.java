package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "bloqueos_calendario", indexes = @Index(
        name = "idx_bloqueos_consultorio_fecha", columnList = "consultorio_id, inicio, fin"))
public class BloqueoCalendario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false)
    private Consultorio consultorio;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date inicio;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date fin;

    protected BloqueoCalendario() {
    }

    public BloqueoCalendario(Consultorio consultorio, Date inicio, Date fin) {
        this.consultorio = consultorio;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Date getInicio() { return inicio; }
    public Date getFin() { return fin; }

    public boolean coincide(Date desde, Date hasta) {
        return inicio.before(hasta) && fin.after(desde);
    }
}
