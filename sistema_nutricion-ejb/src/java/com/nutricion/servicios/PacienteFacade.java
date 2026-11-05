package com.nutricion.servicios;

import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PacienteFacade extends AbstractFacade<Paciente> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public PacienteFacade() {
        super(Paciente.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Paciente> todos() {
        return em.createNamedQuery("Paciente.todos", Paciente.class).getResultList();
    }

    public List<Paciente> buscar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return todos();
        }
        return em.createNamedQuery("Paciente.buscar", Paciente.class)
                .setParameter("texto", "%" + texto.trim().toLowerCase() + "%")
                .getResultList();
    }

    public Paciente porEmail(String email) {
        List<Paciente> r = em.createNamedQuery("Paciente.porEmail", Paciente.class)
                .setParameter("email", normalizarEmail(email)).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    public Paciente porTelefono(String telefono) {
        List<Paciente> r = em.createNamedQuery("Paciente.porTelefono", Paciente.class)
                .setParameter("telefono", normalizarTelefono(telefono)).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Crea o actualiza validando que el correo y el teléfono no estén repetidos. */
    public Paciente guardar(Paciente p) {
        p.setEmail(normalizarEmail(p.getEmail()));
        p.setTelefono(normalizarTelefono(p.getTelefono()));
        validar(p);
        if (p.getId() == null) {
            em.persist(p);
            return p;
        }
        return em.merge(p);
    }

    public void eliminar(Long id) {
        Paciente p = em.find(Paciente.class, id);
        if (p != null) {
            em.remove(p);
        }
    }

    /** Carga el paciente con sus citas y consultas (para el expediente). */
    public Paciente expediente(Long id) {
        Paciente p = em.find(Paciente.class, id);
        if (p != null) {
            p.getCitas().size();
            p.getConsultas().size();
        }
        return p;
    }

    public long contar() {
        return em.createNamedQuery("Paciente.contar", Long.class).getSingleResult();
    }

    private void validar(Paciente p) {
        if (p.getFechaNacimiento() != null && p.getFechaNacimiento().after(new Date())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser futura.");
        }
        if (p.getTelefono().length() < 10) {
            throw new ReglaNegocioException("El teléfono debe tener al menos 10 dígitos.");
        }
        Paciente mismoEmail = porEmail(p.getEmail());
        if (mismoEmail != null && !mismoEmail.getId().equals(p.getId())) {
            throw new ReglaNegocioException("Ya existe un paciente con el correo " + p.getEmail() + ".");
        }
        Paciente mismoTel = porTelefono(p.getTelefono());
        if (mismoTel != null && !mismoTel.getId().equals(p.getId())) {
            throw new ReglaNegocioException("Ya existe un paciente con el teléfono " + p.getTelefono() + ".");
        }
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Deja solo dígitos (y el + inicial si lo hay). */
    static String normalizarTelefono(String tel) {
        if (tel == null) {
            return null;
        }
        String t = tel.trim();
        String digitos = t.replaceAll("[^0-9]", "");
        return t.startsWith("+") ? "+" + digitos : digitos;
    }
}
