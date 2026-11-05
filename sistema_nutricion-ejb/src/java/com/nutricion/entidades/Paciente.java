package com.nutricion.entidades;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "pacientes")
@NamedQueries({
    @NamedQuery(name = "Paciente.todos", query = "SELECT p FROM Paciente p ORDER BY p.nombre"),
    @NamedQuery(name = "Paciente.buscar", query = "SELECT p FROM Paciente p WHERE LOWER(p.nombre) LIKE :texto OR LOWER(p.email) LIKE :texto OR p.telefono LIKE :texto ORDER BY p.nombre"),
    @NamedQuery(name = "Paciente.porEmail", query = "SELECT p FROM Paciente p WHERE p.email = :email"),
    @NamedQuery(name = "Paciente.porTelefono", query = "SELECT p FROM Paciente p WHERE p.telefono = :telefono"),
    @NamedQuery(name = "Paciente.contar", query = "SELECT COUNT(p) FROM Paciente p")
})
public class Paciente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String telefono;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_nacimiento", nullable = false)
    private Date fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Sexo sexo;

    /** Riesgo clínico: nunca proponerlos. */
    @Column(length = 1000)
    private String alergias;

    /** Causan malestar (lactosa, gluten...). */
    @Column(length = 1000)
    private String intolerancias;

    /** Decisión personal (vegetariano, sin cerdo...). */
    @Column(length = 1000)
    private String preferencias;

    /** Simplemente no le gustan. */
    @Column(length = 1000)
    private String disgustos;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_registro", nullable = false)
    private Date fechaRegistro;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("fechaHora DESC")
    private List<Cita> citas = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("fecha DESC")
    private List<Consulta> consultas = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = new Date();
        }
    }

    /** Edad en años cumplidos. */
    public int getEdad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        java.time.LocalDate nac = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
        return java.time.Period.between(nac, java.time.LocalDate.now()).getYears();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public String getIntolerancias() { return intolerancias; }
    public void setIntolerancias(String intolerancias) { this.intolerancias = intolerancias; }
    public String getPreferencias() { return preferencias; }
    public void setPreferencias(String preferencias) { this.preferencias = preferencias; }
    public String getDisgustos() { return disgustos; }
    public void setDisgustos(String disgustos) { this.disgustos = disgustos; }
    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public List<Cita> getCitas() { return citas; }
    public void setCitas(List<Cita> citas) { this.citas = citas; }
    public List<Consulta> getConsultas() { return consultas; }
    public void setConsultas(List<Consulta> consultas) { this.consultas = consultas; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Paciente && id != null && id.equals(((Paciente) o).id);
    }

    @Override
    public String toString() { return nombre; }
}
