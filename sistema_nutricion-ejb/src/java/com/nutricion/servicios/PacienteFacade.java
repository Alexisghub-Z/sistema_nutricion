package com.nutricion.servicios;

import com.nutricion.entidades.Consultorio;
import com.nutricion.entidades.Paciente;
import com.nutricion.util.ReglaNegocioException;
import com.nutricion.util.Seguridad;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class PacienteFacade extends AbstractFacade<Paciente> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @EJB
    private ArchivoFacade archivoFacade;

    @EJB
    private RecordatorioFacade recordatorioFacade;

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

    public List<Paciente> todos(Consultorio c) {
        return em.createNamedQuery("Paciente.porConsultorio", Paciente.class)
                .setParameter("consultorio", c)
                .getResultList();
    }

    public List<Paciente> buscar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return todos();
        }
        return em.createNamedQuery("Paciente.buscar", Paciente.class)
                .setParameter("texto", "%" + texto.trim().toLowerCase() + "%")
                .getResultList();
    }

    public List<Paciente> buscar(String texto, Consultorio c) {
        if (texto == null || texto.trim().isEmpty()) {
            return todos(c);
        }
        return em.createNamedQuery("Paciente.buscarPorConsultorio", Paciente.class)
                .setParameter("consultorio", c)
                .setParameter("texto", "%" + texto.trim().toLowerCase(Locale.ROOT) + "%")
                .getResultList();
    }

    public Paciente porCodigoAcceso(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        List<Paciente> r = em.createNamedQuery("Paciente.porCodigoAcceso", Paciente.class)
                .setParameter("codigo", codigo.trim().toUpperCase(Locale.ROOT))
                .getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Genera y guarda un código único de acceso al portal si el paciente no tiene uno. */
    public String asegurarCodigoAcceso(Long pacienteId) {
        Paciente p = em.find(Paciente.class, pacienteId);
        if (p == null) {
            throw new ReglaNegocioException("El paciente no existe.");
        }
        if (p.getCodigoAcceso() == null || p.getCodigoAcceso().trim().isEmpty()) {
            p.setCodigoAcceso(codigoAccesoUnico());
            em.merge(p);
        }
        return p.getCodigoAcceso();
    }

    private String codigoAccesoUnico() {
        String codigo;
        do {
            codigo = Seguridad.codigoPortal();
        } while (porCodigoAcceso(codigo) != null);
        return codigo;
    }

    public Paciente porEmail(String email, Consultorio consultorio) {
        List<Paciente> r = em.createNamedQuery("Paciente.porEmail", Paciente.class)
                .setParameter("consultorio", consultorio)
                .setParameter("email", normalizarEmail(email)).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    public Paciente porTelefono(String telefono, Consultorio consultorio) {
        List<Paciente> r = em.createNamedQuery("Paciente.porTelefono", Paciente.class)
                .setParameter("consultorio", consultorio)
                .setParameter("telefono", normalizarTelefono(telefono)).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Crea o actualiza validando los contactos dentro del consultorio. */
    public Paciente guardar(Paciente p) {
        if (p == null) {
            throw new ReglaNegocioException("Indica los datos del paciente.");
        }
        if (p.getConsultorio() == null) {
            throw new ReglaNegocioException("Indica el consultorio del paciente.");
        }
        p.setNombre(p.getNombre() == null ? null : p.getNombre().trim());
        if (p.getTelefono() != null && !p.getTelefono().trim().matches("\\+?[0-9() .-]+")) {
            throw new ReglaNegocioException("El teléfono solo admite dígitos, un + inicial y separadores.");
        }
        p.setEmail(normalizarEmail(p.getEmail()));
        p.setTelefono(normalizarTelefono(p.getTelefono()));
        validar(p);
        if (p.getId() == null) {
            if (p.getCodigoAcceso() == null || p.getCodigoAcceso().trim().isEmpty()) {
                p.setCodigoAcceso(codigoAccesoUnico());
            }
            em.persist(p);
            return p;
        }
        Paciente actual = pacienteParaEditar(p.getId(), p.getConsultorio());
        actual.setNombre(p.getNombre());
        actual.setEmail(p.getEmail());
        actual.setTelefono(p.getTelefono());
        actual.setFechaNacimiento(p.getFechaNacimiento());
        actual.setSexo(p.getSexo());
        actual.setAlergias(p.getAlergias());
        actual.setIntolerancias(p.getIntolerancias());
        return actual;
    }

    public Paciente guardar(Paciente p, Consultorio consultorio) {
        if (p == null) {
            throw new ReglaNegocioException("Indica los datos del paciente.");
        }
        p.setConsultorio(consultorio);
        return guardar(p);
    }

    public Paciente delConsultorio(Long id, Consultorio consultorio) {
        if (id == null || consultorio == null) {
            return null;
        }
        return em.createQuery("SELECT p FROM Paciente p WHERE p.id = :id AND p.consultorio = :consultorio", Paciente.class)
                .setParameter("id", id).setParameter("consultorio", consultorio)
                .getResultStream().findFirst().orElse(null);
    }

    public void eliminar(Long id, Consultorio consultorio) {
        if (consultorio == null
                || em.find(Consultorio.class, consultorio.getId(), LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ReglaNegocioException("Selecciona un consultorio.");
        }
        Paciente p = pacienteParaEditar(id, consultorio);
        recordatorioFacade.eliminarDelPaciente(p);
        archivoFacade.eliminarDelPaciente(p);
        em.remove(p);
    }

    private Paciente pacienteParaEditar(Long id, Consultorio consultorio) {
        Paciente p = delConsultorio(id, consultorio);
        if (p == null) {
            throw new ReglaNegocioException("El paciente no pertenece a este consultorio.");
        }
        em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
        return p;
    }

    /** Carga el paciente y su catálogo de alergias para el expediente. */
    public Paciente expediente(Long id, Consultorio consultorio) {
        Paciente p = delConsultorio(id, consultorio);
        if (p != null) {
            p.getAlergiasCatalogo().size();
        }
        return p;
    }

    public void guardarAntecedentes(Paciente datos, Consultorio consultorio) {
        Paciente p = pacienteParaEditar(datos.getId(), consultorio);
        validarLongitud("Motivo principal", datos.getMotivoPrincipal(), 500);
        validarLongitud("Antecedentes heredofamiliares", datos.getAntecedentesFamiliares(), 2000);
        validarLongitud("Antecedentes patológicos", datos.getAntecedentesPatologicos(), 2000);
        validarLongitud("Antecedentes no patológicos", datos.getAntecedentesNoPatologicos(), 2000);
        validarLongitud("Medicamentos actuales", datos.getMedicamentosActuales(), 1000);

        p.setMotivoPrincipal(datos.getMotivoPrincipal());
        p.setAntecedentesFamiliares(datos.getAntecedentesFamiliares());
        p.setAntecedentesPatologicos(datos.getAntecedentesPatologicos());
        p.setAntecedentesNoPatologicos(datos.getAntecedentesNoPatologicos());
        p.setMedicamentosActuales(datos.getMedicamentosActuales());
    }

    public void guardarAlergias(Paciente datos, List<Long> alergiaIds, Consultorio consultorio) {
        Paciente p = pacienteParaEditar(datos.getId(), consultorio);
        validarLongitud("Otras alergias", datos.getAlergiasOtras(), 1000);
        validarLongitud("Preferencias", datos.getPreferencias(), 1000);
        validarLongitud("Disgustos", datos.getDisgustos(), 1000);

        p.setAlergiasOtras(datos.getAlergiasOtras());
        p.setPreferencias(datos.getPreferencias());
        p.setDisgustos(datos.getDisgustos());

        p.getAlergiasCatalogo().clear();
        if (alergiaIds != null && !alergiaIds.isEmpty()) {
            List<com.nutricion.entidades.Alergia> seleccion = em.createQuery(
                    "SELECT a FROM Alergia a WHERE a.id IN :ids", com.nutricion.entidades.Alergia.class)
                    .setParameter("ids", alergiaIds)
                    .getResultList();
            p.getAlergiasCatalogo().addAll(seleccion);
        }
    }

    private void validarLongitud(String campo, String valor, int max) {
        if (valor != null && valor.length() > max) {
            throw new ReglaNegocioException(campo + " admite hasta " + max + " caracteres.");
        }
    }

    public long contar() {
        return em.createNamedQuery("Paciente.contar", Long.class).getSingleResult();
    }

    public long contar(Consultorio c) {
        return em.createNamedQuery("Paciente.contarPorConsultorio", Long.class)
                .setParameter("consultorio", c)
                .getSingleResult();
    }

    private void validar(Paciente p) {
        if (p.getNombre() == null || p.getNombre().isEmpty() || p.getNombre().length() > 120) {
            throw new ReglaNegocioException("Escribe un nombre de entre 1 y 120 caracteres.");
        }
        if (p.getEmail() == null || p.getEmail().length() > 120
                || !p.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ReglaNegocioException("Escribe un correo válido de hasta 120 caracteres.");
        }
        if (p.getFechaNacimiento() == null) {
            throw new ReglaNegocioException("Indica la fecha de nacimiento.");
        }
        if (p.getFechaNacimiento().after(new Date())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser futura.");
        }
        if (p.getTelefono() == null || !p.getTelefono().matches("\\+?[0-9]{10,15}")) {
            throw new ReglaNegocioException("El teléfono debe tener entre 10 y 15 dígitos, con + inicial opcional.");
        }
        for (String texto : new String[]{p.getAlergias(), p.getIntolerancias(), p.getPreferencias(), p.getDisgustos()}) {
            if (texto != null && texto.length() > 1000) {
                throw new ReglaNegocioException("Alergias, intolerancias, preferencias y disgustos admiten hasta 1000 caracteres cada uno.");
            }
        }
        Paciente mismoEmail = porEmail(p.getEmail(), p.getConsultorio());
        if (mismoEmail != null && !mismoEmail.getId().equals(p.getId())) {
            throw new ReglaNegocioException("Ya existe un paciente con el correo " + p.getEmail() + ".");
        }
        Paciente mismoTel = porTelefono(p.getTelefono(), p.getConsultorio());
        if (mismoTel != null && !mismoTel.getId().equals(p.getId())) {
            throw new ReglaNegocioException("Ya existe un paciente con el teléfono " + p.getTelefono() + ".");
        }
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
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
