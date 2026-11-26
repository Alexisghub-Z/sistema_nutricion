package com.nutricion.entidades;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "alergias")
@NamedQueries({
    @NamedQuery(name = "Alergia.todas", query = "SELECT a FROM Alergia a ORDER BY a.tipo, a.nombre"),
    @NamedQuery(name = "Alergia.porNombre", query = "SELECT a FROM Alergia a WHERE LOWER(a.nombre) = :nombre"),
    @NamedQuery(name = "Alergia.contar", query = "SELECT COUNT(a) FROM Alergia a")
})
public class Alergia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAlergia tipo = TipoAlergia.ALERGIA;

    @Column(name = "grupo_alimento", length = 100)
    private String grupoAlimento;

    public Alergia() {
    }

    public Alergia(String nombre, TipoAlergia tipo, String grupoAlimento) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.grupoAlimento = grupoAlimento;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoAlergia getTipo() { return tipo; }
    public void setTipo(TipoAlergia tipo) { this.tipo = tipo; }
    public String getGrupoAlimento() { return grupoAlimento; }
    public void setGrupoAlimento(String grupoAlimento) { this.grupoAlimento = grupoAlimento; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Alergia && id != null && id.equals(((Alergia) o).id);
    }

    @Override
    public String toString() { return nombre; }
}
