package com.nutricion.servicios;

import com.nutricion.entidades.Usuario;
import com.nutricion.util.Seguridad;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class UsuarioFacade extends AbstractFacade<Usuario> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public UsuarioFacade() {
        super(Usuario.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Devuelve el usuario si el correo y la contraseña son correctos; si no, null. */
    public Usuario autenticar(String email, String password) {
        if (email == null || password == null) {
            return null;
        }
        List<Usuario> r = em.createNamedQuery("Usuario.porEmail", Usuario.class)
                .setParameter("email", email.trim().toLowerCase())
                .getResultList();
        if (r.isEmpty() || !Seguridad.verificar(password, r.get(0).getPasswordHash())) {
            return null;
        }
        return r.get(0);
    }

    public Usuario registrar(String nombre, String email, String password) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(Seguridad.hash(password));
        em.persist(u);
        return u;
    }

    public long contar() {
        return em.createNamedQuery("Usuario.contar", Long.class).getSingleResult();
    }
}
