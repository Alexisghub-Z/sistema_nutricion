package com.nutricion.servicios;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Rol;
import com.nutricion.entidades.Usuario;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.util.Seguridad;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class UsuarioFacade extends AbstractFacade<Usuario> {

    public static final int MAX_INTENTOS = 3;
    public static final int MINUTOS_BLOQUEO = 15;
    public static final int MINUTOS_RECUPERACION = 30;
    private static final int MINUTOS_ENTRE_ENLACES = 2;

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    public UsuarioFacade() {
        super(Usuario.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Usuario> porConsultorio(Consultorio c) {
        return em.createNamedQuery("Usuario.porConsultorio", Usuario.class)
                .setParameter("consultorio", c)
                .getResultList();
    }

    public boolean sesionVigente(Long id, Rol rol, Long consultorioId, long revision) {
        return em.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.id = :id AND u.activo = true "
                + "AND u.rol = :rol AND u.consultorio.id = :consultorio "
                + "AND COALESCE(u.revisionCredencial, 0) = :revision", Long.class)
                .setParameter("id", id)
                .setParameter("rol", rol)
                .setParameter("consultorio", consultorioId)
                .setParameter("revision", revision)
                .getSingleResult() == 1L;
    }

    public Usuario porEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        List<Usuario> r = em.createNamedQuery("Usuario.porEmail", Usuario.class)
                .setParameter("email", email.trim().toLowerCase(Locale.ROOT))
                .getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Autentica aplicando control de cuenta activa y bloqueo temporal (HU-02).
     * Devuelve el usuario si es válido, o null si las credenciales son incorrectas.
     * Lanza ReglaNegocioException si la cuenta está desactivada o bloqueada.
     */
    public Usuario autenticar(String email, String password) {
        if (email == null || password == null) {
            return null;
        }
        Usuario u = porEmail(email);
        if (u == null) {
            return null;
        }
        em.refresh(u, LockModeType.PESSIMISTIC_WRITE);
        if (!u.isActivo() || u.getConsultorio() == null) {
            throw new ReglaNegocioException("Tu usuario está desactivado. Contacta al administrador.");
        }
        Date ahora = new Date();
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().after(ahora)) {
            long min = Math.max(1L, (long) Math.ceil((u.getBloqueadoHasta().getTime() - ahora.getTime()) / 60000.0));
            throw new ReglaNegocioException("Cuenta bloqueada temporalmente por intentos fallidos. Intenta en " + min + " min.");
        }
        if (!Seguridad.verificar(password, u.getPasswordHash())) {
            int intentos = u.getBloqueadoHasta() == null ? u.getIntentosFallidos() + 1 : 1;
            u.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS) {
                u.setBloqueadoHasta(Fechas.sumarMinutos(ahora, MINUTOS_BLOQUEO));
            } else {
                u.setBloqueadoHasta(null);
            }
            // El retorno normal confirma el contador y el bloqueo en MySQL.
            return null;
        }
        if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
        return u;
    }

    public Usuario registrar(String nombre, String email, String password, Rol rol, Consultorio consultorio) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol == null ? Rol.ADMIN : rol);
        u.setConsultorio(consultorio);
        u.setActivo(true);
        return guardarUsuario(u, password, consultorio);
    }

    public String emitirEnlaceRecuperacion(String correo) {
        Usuario u = porEmail(correo);
        if (u == null) return null;
        em.refresh(u, LockModeType.PESSIMISTIC_WRITE);
        Date ahora = new Date();
        if (!u.isActivo() || u.getConsultorio() == null || !u.getEmail().equals(correo)
                || (u.getRecuperacionVence() != null && u.getRecuperacionVence().after(
                        Fechas.sumarMinutos(ahora, MINUTOS_RECUPERACION - MINUTOS_ENTRE_ENLACES)))) {
            return null;
        }
        String token = Seguridad.generarTokenRecuperacion();
        u.setRecuperacionHuella(Seguridad.huellaTokenRecuperacion(token));
        u.setRecuperacionVence(Fechas.sumarMinutos(ahora, MINUTOS_RECUPERACION));
        return token;
    }

    public String restablecerPassword(String token, String password, String confirmacion) {
        if (password == null || password.length() < 6 || password.length() > 128 || password.trim().isEmpty()) {
            throw new ReglaNegocioException("Escribe una contraseña de 6 a 128 caracteres.");
        }
        if (!password.equals(confirmacion)) {
            throw new ReglaNegocioException("Las contraseñas deben coincidir.");
        }
        String huella = Seguridad.huellaTokenRecuperacion(token);
        if (huella == null) throw enlaceInvalido();
        Usuario u = em.createQuery("SELECT u FROM Usuario u WHERE u.recuperacionHuella = :huella", Usuario.class)
                .setParameter("huella", huella).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().orElse(null);
        if (u != null) em.refresh(u, LockModeType.PESSIMISTIC_WRITE);
        if (u == null || !u.isActivo() || u.getConsultorio() == null || !huella.equals(u.getRecuperacionHuella())
                || u.getRecuperacionVence() == null || !u.getRecuperacionVence().after(new Date())) {
            throw enlaceInvalido();
        }
        cambiarPassword(u, password);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        return u.getEmail();
    }

    private ReglaNegocioException enlaceInvalido() {
        return new ReglaNegocioException("El enlace ya no está disponible. Solicita uno nuevo.");
    }

    private void limpiarRecuperacion(Usuario usuario) {
        usuario.setRecuperacionHuella(null);
        usuario.setRecuperacionVence(null);
    }

    private void cambiarPassword(Usuario usuario, String password) {
        usuario.setPasswordHash(Seguridad.hash(password));
        usuario.setRevisionCredencial(usuario.getRevisionCredencial() + 1L);
        limpiarRecuperacion(usuario);
    }

    /** Crea o actualiza un usuario con rol, consultorio y cambio opcional de contraseña (HU-03). */
    public Usuario guardarUsuario(Usuario u, String nuevaPassword, Consultorio consultorio) {
        if (u == null) {
            throw new ReglaNegocioException("Indica los datos del usuario.");
        }
        if (consultorio == null) {
            throw new ReglaNegocioException("El usuario debe pertenecer a un consultorio.");
        }
        String nombre = u.getNombre() == null ? null : u.getNombre().trim();
        if (nombre == null || nombre.isEmpty() || nombre.length() > 120) {
            throw new ReglaNegocioException("Escribe el nombre del usuario (1 a 120 caracteres).");
        }

        String mail = u.getEmail() == null ? null : u.getEmail().trim().toLowerCase(Locale.ROOT);
        if (mail == null || mail.length() > 120 || !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ReglaNegocioException("Escribe un correo válido de hasta 120 caracteres.");
        }

        if (u.getRol() == null) {
            throw new ReglaNegocioException("Selecciona un rol.");
        }

        boolean esNuevo = u.getId() == null;
        if (esNuevo && (nuevaPassword == null || nuevaPassword.length() < 6)) {
            throw new ReglaNegocioException("La contraseña inicial debe tener al menos 6 caracteres.");
        }
        if (nuevaPassword != null && !nuevaPassword.isEmpty() && nuevaPassword.length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario anterior = u;
        if (!esNuevo) {
            em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE);
            anterior = em.find(Usuario.class, u.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (anterior == null || !consultorio.equals(anterior.getConsultorio())) {
                throw new ReglaNegocioException("El usuario no pertenece a este consultorio.");
            }
        }
        Usuario duplicado = porEmail(mail);
        if (duplicado != null && !duplicado.getId().equals(u.getId())) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo.");
        }
        if (!esNuevo && anterior.getRol() == Rol.ADMIN && anterior.isActivo()
                && (!u.isActivo() || u.getRol() != Rol.ADMIN)) {
            Long otrosAdmins = em.createQuery(
                    "SELECT COUNT(x) FROM Usuario x WHERE x.consultorio = :consultorio "
                    + "AND x.rol = :rol AND x.activo = true AND x.id <> :id",
                    Long.class)
                    .setParameter("consultorio", consultorio)
                    .setParameter("rol", Rol.ADMIN)
                    .setParameter("id", u.getId())
                    .getSingleResult();
            if (otrosAdmins == 0L) {
                throw new ReglaNegocioException("Debe quedar al menos un administrador activo en el consultorio.");
            }
        }

        if (!esNuevo && (!mail.equals(anterior.getEmail()) || !u.isActivo())) {
            limpiarRecuperacion(anterior);
        }
        anterior.setNombre(nombre);
        anterior.setEmail(mail);
        anterior.setRol(u.getRol());
        anterior.setActivo(u.isActivo());
        anterior.setConsultorio(consultorio);
        if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
            cambiarPassword(anterior, nuevaPassword);
        }
        if (esNuevo) {
            em.persist(anterior);
        }
        return anterior;
    }

    /** Desbloquea manualmente un usuario bloqueado por intentos fallidos. */
    public void desbloquear(Long id, Consultorio consultorio) {
        Usuario u = em.find(Usuario.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (u == null || consultorio == null || !consultorio.equals(u.getConsultorio())) {
            throw new ReglaNegocioException("El usuario no pertenece a este consultorio.");
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
    }

    public long contar() {
        return em.createNamedQuery("Usuario.contar", Long.class).getSingleResult();
    }
}
