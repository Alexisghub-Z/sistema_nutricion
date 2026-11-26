package com.nutricion.servicios;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Rol;
import com.nutricion.entidades.Usuario;
import com.nutricion.util.ReglaNegocioException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/** Fachada de gestión de consultorios (multiconsultorio / HU-01). */
@Stateless
public class ConsultorioFacade extends AbstractFacade<Consultorio> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private UsuarioFacade usuarioFacade;

    public ConsultorioFacade() {
        super(Consultorio.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Consultorio> todos() {
        return em.createNamedQuery("Consultorio.todos", Consultorio.class).getResultList();
    }

    public List<Consultorio> activos() {
        return em.createQuery("SELECT c FROM Consultorio c WHERE c.activo = true ORDER BY c.nombre", Consultorio.class)
                .getResultList();
    }

    public Consultorio porSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return null;
        }
        List<Consultorio> r = em.createNamedQuery("Consultorio.porSlug", Consultorio.class)
                .setParameter("slug", slug.trim().toLowerCase(Locale.ROOT))
                .getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    public long contar() {
        return em.createNamedQuery("Consultorio.contar", Long.class).getSingleResult();
    }

    /** Crea el consultorio principal si no existe ninguno y lo devuelve. */
    public Consultorio asegurarPrincipal() {
        List<Consultorio> lista = em.createQuery("SELECT c FROM Consultorio c ORDER BY c.id", Consultorio.class)
                .setMaxResults(1).getResultList();
        if (!lista.isEmpty()) {
            return lista.get(0);
        }
        Consultorio c = new Consultorio();
        c.setNombre("Consultorio NutriVida Principal");
        c.setSlug("principal");
        c.setEmail("contacto@nutrivida.local");
        c.setTelefono("9510000000");
        c.setDireccion("Consultorio central");
        c.setActivo(true);
        em.persist(c);
        return c;
    }

    public Consultorio registrar(Consultorio nuevo, Usuario administrador, String password) {
        if (nuevo.getId() != null) {
            throw new ReglaNegocioException("Selecciona un consultorio nuevo.");
        }
        Consultorio guardado = guardar(nuevo);
        usuarioFacade.registrar(administrador.getNombre(), administrador.getEmail(), password, Rol.ADMIN, guardado);
        return guardado;
    }

    public Consultorio guardar(Consultorio c) {
        if (c == null) {
            throw new ReglaNegocioException("Indica los datos del consultorio.");
        }
        String nombre = c.getNombre() == null ? null : c.getNombre().trim();
        if (nombre == null || nombre.isEmpty() || nombre.length() > 120) {
            throw new ReglaNegocioException("Escribe el nombre del consultorio (1 a 120 caracteres).");
        }
        c.setNombre(nombre);

        String slug = normalizarSlug(c.getSlug() == null || c.getSlug().trim().isEmpty() ? nombre : c.getSlug());
        if (slug.length() < 2 || slug.length() > 60) {
            throw new ReglaNegocioException("El identificador (slug) debe tener entre 2 y 60 caracteres.");
        }
        c.setSlug(slug);

        Consultorio existente = porSlug(slug);
        if (existente != null && !existente.getId().equals(c.getId())) {
            throw new ReglaNegocioException("Ya existe un consultorio con el identificador '" + slug + "'.");
        }

        if (c.getEmail() != null && !c.getEmail().trim().isEmpty()) {
            String mail = c.getEmail().trim().toLowerCase(Locale.ROOT);
            if (mail.length() > 120 || !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                throw new ReglaNegocioException("Escribe un correo válido de hasta 120 caracteres.");
            }
            c.setEmail(mail);
        } else {
            c.setEmail(null);
        }

        if (c.getTelefono() != null && c.getTelefono().trim().length() > 20) {
            throw new ReglaNegocioException("El teléfono admite hasta 20 caracteres.");
        }
        if (c.getDireccion() != null && c.getDireccion().trim().length() > 300) {
            throw new ReglaNegocioException("La dirección admite hasta 300 caracteres.");
        }

        if (c.getId() == null) {
            em.persist(c);
            return c;
        }
        return em.merge(c);
    }

    private String normalizarSlug(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
