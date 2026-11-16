-- Solo lectura. No contiene credenciales ni modifica registros.
USE sistema_nutricion;
SHOW TABLES;
SHOW CREATE TABLE usuarios;
SHOW CREATE TABLE pacientes;
SHOW CREATE TABLE citas;
SHOW CREATE TABLE consultas;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Buscar exclusivamente pacientes ficticios de las pruebas de la unidad.
SELECT ID, NOMBRE, EMAIL, TELEFONO, fecha_nacimiento
FROM pacientes WHERE EMAIL LIKE 'u4.qa.%@example.invalid';

-- Las FK impiden las tres primeras clases de huérfanos.
SELECT COUNT(*) AS citas_sin_paciente
FROM citas c LEFT JOIN pacientes p ON p.ID = c.paciente_id WHERE p.ID IS NULL;
SELECT COUNT(*) AS consultas_sin_paciente
FROM consultas c LEFT JOIN pacientes p ON p.ID = c.paciente_id WHERE p.ID IS NULL;
SELECT COUNT(*) AS consultas_con_cita_inexistente
FROM consultas c LEFT JOIN citas a ON a.ID = c.cita_id
WHERE c.cita_id IS NOT NULL AND a.ID IS NULL;

-- Esta consistencia entre dos FK requiere además una regla de negocio.
SELECT COUNT(*) AS consulta_y_cita_de_distinto_paciente
FROM consultas c JOIN citas a ON a.ID = c.cita_id
WHERE c.paciente_id <> a.paciente_id;
