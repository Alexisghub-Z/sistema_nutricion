package com.nutricion.entidades;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "configuracion_agenda")
public class ConfiguracionAgenda implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consultorio_id", unique = true)
    private Consultorio consultorio;

    @Column(name = "duracion_consulta_min", nullable = false)
    private int duracionConsultaMin = 60;

    @Column(name = "anticipacion_minima_horas", nullable = false)
    private int anticipacionMinimaHoras = 2;

    @Column(name = "minutos_expiracion_apartado", nullable = false)
    private int minutosExpiracionApartado = 15;

    @Column(name = "dias_paciente_activo", nullable = false)
    private int diasPacienteActivo = 60;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setConsultorio(Consultorio consultorio) { this.consultorio = consultorio; }
    public int getDuracionConsultaMin() { return duracionConsultaMin; }
    public void setDuracionConsultaMin(int duracionConsultaMin) { this.duracionConsultaMin = duracionConsultaMin; }
    public int getAnticipacionMinimaHoras() { return anticipacionMinimaHoras; }
    public void setAnticipacionMinimaHoras(int anticipacionMinimaHoras) { this.anticipacionMinimaHoras = anticipacionMinimaHoras; }
    public int getMinutosExpiracionApartado() { return minutosExpiracionApartado; }
    public void setMinutosExpiracionApartado(int minutosExpiracionApartado) { this.minutosExpiracionApartado = minutosExpiracionApartado; }
    public int getDiasPacienteActivo() { return diasPacienteActivo; }
    public void setDiasPacienteActivo(int diasPacienteActivo) { this.diasPacienteActivo = diasPacienteActivo; }

    @Override
    public int hashCode() { return id == null ? 0 : id.hashCode(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConfiguracionAgenda && id != null && id.equals(((ConfiguracionAgenda) o).id);
    }
}
