package com.nutricion.web.beans;

import com.nutricion.entidades.*;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/** Listas de valores para los select de las vistas. */
@Named
@ApplicationScoped
public class CatalogosBean {

    public Sexo[] getSexos() { return Sexo.values(); }
    public TipoCita[] getTiposCita() { return TipoCita.values(); }
    public EstadoCita[] getEstadosCita() { return EstadoCita.values(); }
    public MetodoPago[] getMetodosPago() { return MetodoPago.values(); }
    public EstadoPago[] getEstadosPago() { return EstadoPago.values(); }
    public FormulaEnergetica[] getFormulas() { return FormulaEnergetica.values(); }
    public FactorActividad[] getFactoresActividad() { return FactorActividad.values(); }
    public ObjetivoTratamiento[] getObjetivosTratamiento() { return ObjetivoTratamiento.values(); }
    public TipoAlergia[] getTiposAlergia() { return TipoAlergia.values(); }
    public Rol[] getRoles() { return Rol.values(); }
    public int[] getDuraciones() { return new int[]{30, 45, 60, 90}; }
}
