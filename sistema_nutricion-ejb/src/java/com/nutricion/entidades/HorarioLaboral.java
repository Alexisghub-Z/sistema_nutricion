package com.nutricion.entidades;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "horarios_laborales", uniqueConstraints = @UniqueConstraint(columnNames = {"consultorio_id", "dia_semana"}))
@NamedQueries({
    @NamedQuery(name = "HorarioLaboral.todos", query = "SELECT h FROM HorarioLaboral h WHERE h.consultorio = :consultorio ORDER BY h.diaSemana"),
    @NamedQuery(name = "HorarioLaboral.porDia", query = "SELECT h FROM HorarioLaboral h WHERE h.consultorio = :consultorio AND h.diaSemana = :dia")
})
public class HorarioLaboral implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    /** 1 = Lunes ... 7 = Domingo (ISO-8601). */
    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_inicio", nullable = false, length = 5)
    private String horaInicio = "09:00";

    @Column(name = "hora_fin", nullable = false, length = 5)
    private String horaFin = "19:00";

    @Column(nullable = false)
    private boolean activo = true;

    public HorarioLaboral() {
    }

    public HorarioLaboral(int diaSemana, String horaInicio, String horaFin, boolean activo) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.activo = activo;
    }

    public String getNombreDia() {
        switch (diaSemana) {
            case 1: return "Lunes";
            case 2: return "Martes";
            case 3: return "Miércoles";
            case 4: return "Jueves";
            case 5: return "Viernes";
            case 6: return "Sábado";
            case 7: return "Domingo";
            default: return "Día " + diaSemana;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setConsultorio(Consultorio consultorio) { this.consultorio = consultorio; }
    public int getDiaSemana() { return diaSemana; }
    public void setDiaSemana(int diaSemana) { this.diaSemana = diaSemana; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof HorarioLaboral && id != null && id.equals(((HorarioLaboral) o).id);
    }
}
