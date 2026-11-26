package com.nutricion.servicios;

import com.nutricion.entidades.ArchivoAdjunto;
import com.nutricion.entidades.CategoriaArchivo;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.Rol;
import com.nutricion.util.AlmacenArchivos;
import com.nutricion.util.ReglaNegocioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

@Stateless
public class ArchivoFacade extends AbstractFacade<ArchivoAdjunto> {

    @PersistenceContext(unitName = "NutricionPU")
    private EntityManager em;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry transacciones;

    public ArchivoFacade() {
        super(ArchivoAdjunto.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ArchivoAdjunto> porExpediente(Long paciente, Long consultorio, Rol rol) {
        validarAcceso(consultorio, rol);
        return metadatosDelPaciente(paciente, consultorio);
    }

    public ArchivoAdjunto paraDescarga(Long id, Long consultorio, Rol rol) {
        validarAcceso(consultorio, rol);
        return em.createQuery("SELECT a FROM ArchivoAdjunto a WHERE a.id = :id AND a.consultorio.id = :consultorio", ArchivoAdjunto.class)
                .setParameter("id", id).setParameter("consultorio", consultorio)
                .getResultStream().findFirst().orElse(null);
    }

    public void guardar(Long pacienteId, Long consultorio, Rol rol, CategoriaArchivo categoria,
            Date fechaFoto, String nombreOriginal, long tamano, InputStream datos) throws IOException {
        validarAcceso(consultorio, rol);
        if (categoria == null) {
            throw new ReglaNegocioException("Selecciona la categoría del adjunto.");
        }

        Date fechaProgreso = null;
        if (categoria == CategoriaArchivo.FOTO_PROGRESO) {
            LocalDate hoy = LocalDate.now();
            LocalDate fecha = fechaFoto == null ? hoy : new java.sql.Date(fechaFoto.getTime()).toLocalDate();
            if (fecha.isAfter(hoy)) {
                throw new ReglaNegocioException("La fecha de la foto no puede ser futura.");
            }
            fechaProgreso = java.sql.Date.valueOf(fecha);
        }

        Paciente paciente = em.createQuery("SELECT p FROM Paciente p WHERE p.id = :paciente AND p.consultorio.id = :consultorio", Paciente.class)
                .setParameter("paciente", pacienteId).setParameter("consultorio", consultorio)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultStream().findFirst().orElse(null);
        if (paciente == null) {
            throw new ReglaNegocioException("El expediente no pertenece a este consultorio.");
        }

        String nombre = AlmacenArchivos.nombreValido(nombreOriginal);
        String clave = UUID.randomUUID().toString();
        Path destino = AlmacenArchivos.ubicacion(clave);
        String mime = AlmacenArchivos.guardar(destino, nombre, datos, tamano, categoria == CategoriaArchivo.FOTO_PROGRESO);
        try {
            limpiarAlFinalizar(Collections.singletonList(destino), Status.STATUS_ROLLEDBACK);
        } catch (RuntimeException e) {
            AlmacenArchivos.borrar(destino);
            throw e;
        }
        em.persist(new ArchivoAdjunto(paciente, categoria, nombre, clave, mime, tamano, fechaProgreso));
    }

    /** Se ejecuta dentro de la misma transacción que elimina al paciente. */
    public void eliminarDelPaciente(Paciente paciente) {
        List<ArchivoAdjunto> adjuntos = metadatosDelPaciente(paciente.getId(), paciente.getConsultorio().getId());
        if (adjuntos.isEmpty()) {
            return;
        }
        List<Path> rutas = adjuntos.stream().map(a -> AlmacenArchivos.ubicacion(a.getRutaDisco()))
                .collect(Collectors.toList());
        adjuntos.forEach(em::remove);
        limpiarAlFinalizar(rutas, Status.STATUS_COMMITTED);
    }

    private List<ArchivoAdjunto> metadatosDelPaciente(Long paciente, Long consultorio) {
        return em.createQuery("SELECT a FROM ArchivoAdjunto a WHERE a.paciente.id = :paciente "
                + "AND a.consultorio.id = :consultorio ORDER BY a.fechaCarga DESC, a.id DESC", ArchivoAdjunto.class)
                .setParameter("paciente", paciente).setParameter("consultorio", consultorio).getResultList();
    }

    private void validarAcceso(Long consultorio, Rol rol) {
        if (consultorio == null || (rol != Rol.ADMIN && rol != Rol.NUTRIOLOGO)) {
            throw new ReglaNegocioException("El acceso a archivos requiere un usuario clínico.");
        }
    }

    private void limpiarAlFinalizar(List<Path> rutas, int estadoEsperado) {
        transacciones.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int estado) {
                if (estado == estadoEsperado) {
                    rutas.forEach(AlmacenArchivos::borrar);
                }
            }
        });
    }
}
