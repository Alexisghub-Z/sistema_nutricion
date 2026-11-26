package com.nutricion.servicios;

import com.nutricion.entidades.BloqueoCalendario;
import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.VinculoGoogleCalendar;
import com.nutricion.util.ReglaNegocioException;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class CalendarioGoogleFacade extends AbstractFacade<VinculoGoogleCalendar> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public CalendarioGoogleFacade() {
        super(VinculoGoogleCalendar.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public VinculoGoogleCalendar vinculo(Consultorio consultorio) {
        return em.createQuery("SELECT v FROM VinculoGoogleCalendar v WHERE v.consultorio = :consultorio",
                VinculoGoogleCalendar.class).setParameter("consultorio", consultorio)
                .getResultStream().findFirst().orElse(null);
    }

    public List<VinculoGoogleCalendar> vinculados() {
        return em.createQuery("SELECT v FROM VinculoGoogleCalendar v "
                + "WHERE v.consultorio.activo = true AND v.tokenCifrado IS NOT NULL", VinculoGoogleCalendar.class)
                .getResultList();
    }

    public void vincular(Consultorio c, String cifrado) {
        Consultorio consultorio = bloquear(c);
        VinculoGoogleCalendar vinculo = vinculo(consultorio);
        if (vinculo == null) {
            vinculo = new VinculoGoogleCalendar(consultorio);
            em.persist(vinculo);
        }
        vinculo.setTokenCifrado(cifrado);
        vinculo.setSincronizadoEn(null);
        vinculo.setErrorSincronizacion(false);
        limpiarBloqueos(consultorio);
    }

    public void desvincular(Consultorio c) {
        Consultorio consultorio = bloquear(c);
        VinculoGoogleCalendar vinculo = vinculo(consultorio);
        if (vinculo != null) {
            vinculo.setTokenCifrado(null);
            vinculo.setSincronizadoEn(null);
            vinculo.setErrorSincronizacion(false);
            limpiarBloqueos(consultorio);
        }
    }

    public boolean guardarBloqueos(VinculoGoogleCalendar origen, List<BloqueoCalendario> bloqueos) {
        Consultorio consultorio = bloquear(origen.getConsultorio());
        if (vigente(origen, consultorio) == null) {
            return false;
        }
        limpiarBloqueos(consultorio);
        bloqueos.forEach(em::persist);
        return true;
    }

    public void registrarResultado(VinculoGoogleCalendar origen, boolean correcto) {
        Consultorio consultorio = bloquear(origen.getConsultorio());
        VinculoGoogleCalendar actual = vigente(origen, consultorio);
        if (actual != null) {
            actual.setErrorSincronizacion(!correcto);
            if (correcto) {
                actual.setSincronizadoEn(new Date());
            }
        }
    }

    private VinculoGoogleCalendar vigente(VinculoGoogleCalendar origen, Consultorio consultorio) {
        VinculoGoogleCalendar actual = vinculo(consultorio);
        return actual != null && consultorio.isActivo() && origen.getTokenCifrado().equals(actual.getTokenCifrado())
                ? actual : null;
    }

    private Consultorio bloquear(Consultorio consultorio) {
        Consultorio actual = consultorio == null ? null
                : em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (actual == null) {
            throw new ReglaNegocioException("El consultorio no está disponible.");
        }
        return actual;
    }

    private void limpiarBloqueos(Consultorio consultorio) {
        em.createQuery("DELETE FROM BloqueoCalendario b WHERE b.consultorio = :consultorio")
                .setParameter("consultorio", consultorio).executeUpdate();
    }
}
