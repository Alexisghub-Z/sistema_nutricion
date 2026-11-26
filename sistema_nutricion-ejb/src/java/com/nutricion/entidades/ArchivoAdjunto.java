package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "archivos_adjuntos", indexes =
        @Index(name = "idx_adjuntos_expediente", columnList = "consultorio_id, paciente_id, fecha_carga"))
public class ArchivoAdjunto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false)
    private Consultorio consultorio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaArchivo categoria;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "ruta_disco", nullable = false, unique = true, length = 36)
    private String rutaDisco;

    @Column(name = "mime_type", nullable = false, length = 32)
    private String mimeType;

    @Column(nullable = false)
    private long tamano;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_carga", nullable = false)
    private Date fechaCarga;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_foto")
    private Date fechaFoto;

    protected ArchivoAdjunto() {
    }

    public ArchivoAdjunto(Paciente paciente, CategoriaArchivo categoria, String nombreOriginal,
            String rutaDisco, String mimeType, long tamano, Date fechaFoto) {
        this.paciente = paciente;
        this.consultorio = paciente.getConsultorio();
        this.categoria = categoria;
        this.nombreOriginal = nombreOriginal;
        this.rutaDisco = rutaDisco;
        this.mimeType = mimeType;
        this.tamano = tamano;
        this.fechaCarga = new Date();
        this.fechaFoto = fechaFoto;
    }

    public Long getId() { return id; }
    public CategoriaArchivo getCategoria() { return categoria; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getRutaDisco() { return rutaDisco; }
    public String getMimeType() { return mimeType; }
    public long getTamano() { return tamano; }
    public Date getFechaCarga() { return fechaCarga; }
    public Date getFechaFoto() { return fechaFoto; }
}
