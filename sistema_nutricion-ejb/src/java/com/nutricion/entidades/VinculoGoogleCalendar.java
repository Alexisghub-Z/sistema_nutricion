package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.*;

@Entity
@Table(name = "vinculos_google_calendar")
public class VinculoGoogleCalendar implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false, unique = true)
    private Consultorio consultorio;

    @Column(nullable = false, unique = true, length = 32)
    private String marca;

    @Lob
    @Column(name = "token_cifrado")
    private String tokenCifrado;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sincronizado_en")
    private Date sincronizadoEn;

    @Column(name = "error_sincronizacion", nullable = false)
    private boolean errorSincronizacion;

    protected VinculoGoogleCalendar() {
    }

    public VinculoGoogleCalendar(Consultorio consultorio) {
        this.consultorio = consultorio;
        this.marca = UUID.randomUUID().toString().replace("-", "");
    }

    public Consultorio getConsultorio() { return consultorio; }
    public String getMarca() { return marca; }
    public String getTokenCifrado() { return tokenCifrado; }
    public void setTokenCifrado(String tokenCifrado) { this.tokenCifrado = tokenCifrado; }
    public Date getSincronizadoEn() { return sincronizadoEn; }
    public void setSincronizadoEn(Date sincronizadoEn) { this.sincronizadoEn = sincronizadoEn; }
    public boolean isErrorSincronizacion() { return errorSincronizacion; }
    public void setErrorSincronizacion(boolean errorSincronizacion) { this.errorSincronizacion = errorSincronizacion; }
}
