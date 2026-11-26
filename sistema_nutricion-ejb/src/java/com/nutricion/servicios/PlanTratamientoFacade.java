package com.nutricion.servicios;

import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.util.ReglaNegocioException;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PlanTratamientoFacade extends AbstractFacade<PlanTratamiento> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public PlanTratamientoFacade() {
        super(PlanTratamiento.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<PlanTratamiento> porPaciente(Paciente p) {
        return em.createNamedQuery("PlanTratamiento.porPaciente", PlanTratamiento.class)
                .setParameter("paciente", p)
                .getResultList();
    }

    public PlanTratamiento guardar(PlanTratamiento plan) {
        if (plan == null || plan.getPaciente() == null) {
            throw new ReglaNegocioException("El plan debe pertenecer a un paciente.");
        }
        if (plan.getMetaKcal() == null || plan.getMetaKcal() < 500 || plan.getMetaKcal() > 8000) {
            throw new ReglaNegocioException("La meta calórica debe estar entre 500 y 8000 kcal.");
        }
        int suma = (plan.getPctProteinas() == null ? 0 : plan.getPctProteinas())
                + (plan.getPctLipidos() == null ? 0 : plan.getPctLipidos())
                + (plan.getPctCarbohidratos() == null ? 0 : plan.getPctCarbohidratos());
        if (suma != 100) {
            throw new ReglaNegocioException("La suma de macronutrientes debe ser exactamente 100% (actual: " + suma + "%).");
        }
        plan.recalcularMacros();
        if (plan.getId() == null) {
            em.persist(plan);
            return plan;
        }
        return em.merge(plan);
    }

    public void eliminar(Long id) {
        PlanTratamiento p = em.find(PlanTratamiento.class, id);
        if (p != null) {
            em.remove(p);
        }
    }
}
