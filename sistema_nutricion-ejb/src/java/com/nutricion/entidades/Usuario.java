package com.nutricion.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "usuarios")
@NamedQueries({
    @NamedQuery(name = "Usuario.todos", query = "SELECT u FROM Usuario u ORDER BY u.nombre"),
    @NamedQuery(name = "Usuario.porEmail", query = "SELECT u FROM Usuario u WHERE u.email = :email"),
    @NamedQuery(name = "Usuario.porConsultorio", query = "SELECT u FROM Usuario u WHERE u.consultorio = :consultorio ORDER BY u.nombre"),
    @NamedQuery(name = "Usuario.contar", query = "SELECT COUNT(u) FROM Usuario u")
})
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.ADMIN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "bloqueado_hasta")
    private Date bloqueadoHasta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_registro", nullable = false)
    private Date fechaRegistro;

    @Column(name = "recuperacion_huella", length = 43, unique = true)
    private String recuperacionHuella;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "recuperacion_vence")
    private Date recuperacionVence;

    @Column(name = "revision_credencial")
    private Long revisionCredencial;

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = new Date();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Consultorio getConsultorio() { return consultorio; }
    public void setConsultorio(Consultorio consultorio) { this.consultorio = consultorio; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public Date getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Date bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public boolean isBloqueadoAhora() { return bloqueadoHasta != null && bloqueadoHasta.after(new Date()); }
    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getRecuperacionHuella() { return recuperacionHuella; }
    public void setRecuperacionHuella(String huella) { this.recuperacionHuella = huella; }
    public Date getRecuperacionVence() { return recuperacionVence; }
    public void setRecuperacionVence(Date vence) { this.recuperacionVence = vence; }
    public long getRevisionCredencial() { return revisionCredencial == null ? 0L : revisionCredencial; }
    public void setRevisionCredencial(long revision) { this.revisionCredencial = revision; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Usuario && id != null && id.equals(((Usuario) o).id);
    }
}
