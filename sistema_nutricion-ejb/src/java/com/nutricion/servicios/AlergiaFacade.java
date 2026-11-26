package com.nutricion.servicios;

import com.nutricion.entidades.Alergia;
import com.nutricion.entidades.TipoAlergia;
import com.nutricion.util.ReglaNegocioException;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AlergiaFacade extends AbstractFacade<Alergia> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public AlergiaFacade() {
        super(Alergia.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Alergia> todas() {
        return em.createNamedQuery("Alergia.todas", Alergia.class).getResultList();
    }

    public long contar() {
        return em.createNamedQuery("Alergia.contar", Long.class).getSingleResult();
    }

    public Alergia guardar(Alergia a) {
        if (a == null || a.getNombre() == null || a.getNombre().trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre de la alergia o intolerancia es obligatorio.");
        }
        String nombre = a.getNombre().trim();
        if (nombre.length() > 100) {
            throw new ReglaNegocioException("El nombre no puede superar 100 caracteres.");
        }
        a.setNombre(nombre);
        if (a.getTipo() == null) {
            a.setTipo(TipoAlergia.ALERGIA);
        }
        List<Alergia> existentes = em.createNamedQuery("Alergia.porNombre", Alergia.class)
                .setParameter("nombre", nombre.toLowerCase())
                .getResultList();
        if (!existentes.isEmpty() && !existentes.get(0).getId().equals(a.getId())) {
            throw new ReglaNegocioException("Ya existe en el catálogo: " + nombre + ".");
        }
        if (a.getId() == null) {
            em.persist(a);
            return a;
        }
        return em.merge(a);
    }
}
