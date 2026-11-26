package com.nutricion.servicios;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

/** Reemplaza los índices de contacto del esquema anterior sin reescribir pacientes. */
@Singleton
@Startup
@DependsOn("InicioDatos")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MigracionContactosPacientes {

    private static final Logger LOG = Logger.getLogger(MigracionContactosPacientes.class.getName());

    @Resource(lookup = "jdbc/nutricion")
    private DataSource base;

    @PostConstruct
    void actualizarIndices() {
        try (Connection conexion = base.getConnection(); Statement sql = conexion.createStatement()) {
            Map<String, String> unicos = new LinkedHashMap<>();
            try (ResultSet filas = sql.executeQuery(
                    "SELECT INDEX_NAME, GROUP_CONCAT(LOWER(COLUMN_NAME) ORDER BY SEQ_IN_INDEX) "
                    + "FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = 'pacientes' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME")) {
                while (filas.next()) {
                    unicos.put(filas.getString(1), filas.getString(2));
                }
            }

            List<String> cambios = new ArrayList<>();
            if (!unicos.containsValue("consultorio_id,email")) {
                cambios.add("ADD UNIQUE INDEX uq_pacientes_consultorio_email (consultorio_id, email)");
            }
            if (!unicos.containsValue("consultorio_id,telefono")) {
                cambios.add("ADD UNIQUE INDEX uq_pacientes_consultorio_tel (consultorio_id, telefono)");
            }
            for (Map.Entry<String, String> indice : unicos.entrySet()) {
                if ("email".equals(indice.getValue()) || "telefono".equals(indice.getValue())) {
                    cambios.add("DROP INDEX `" + indice.getKey().replace("`", "``") + "`");
                }
            }
            if (!cambios.isEmpty()) {
                // MySQL aplica el conjunto en un ALTER para conservar la protección contra duplicados.
                sql.executeUpdate("ALTER TABLE pacientes " + String.join(", ", cambios));
                LOG.info("Índices de correo y teléfono de pacientes actualizados por consultorio.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron actualizar los índices de contacto de pacientes.", e);
        }
    }
}
