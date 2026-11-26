import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Lee solo metadatos de la base local; las credenciales no salen del proceso. */
public class InspeccionarBase {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Indica el domain.xml local de Payara.");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(Path.of(args[0]).toFile());
        Properties pool = new Properties();
        NodeList pools = document.getElementsByTagName("jdbc-connection-pool");
        for (int i = 0; i < pools.getLength(); i++) {
            Element element = (Element) pools.item(i);
            if (!"NutricionMySQLPool".equals(element.getAttribute("name"))) continue;
            NodeList properties = element.getElementsByTagName("property");
            for (int j = 0; j < properties.getLength(); j++) {
                Element property = (Element) properties.item(j);
                pool.setProperty(property.getAttribute("name").toLowerCase(java.util.Locale.ROOT), property.getAttribute("value"));
            }
        }
        if (!"sistema_nutricion".equals(pool.getProperty("databasename"))) {
            throw new IllegalStateException("El pool no apunta a la base esperada.");
        }
        String host = pool.getProperty("servername");
        if (!"127.0.0.1".equals(host) && !"localhost".equals(host)) {
            throw new IllegalStateException("Esta revisión solo permite la base local.");
        }
        Properties connection = new Properties();
        connection.setProperty("user", pool.getProperty("user"));
        connection.setProperty("password", pool.getProperty("password"));
        connection.setProperty("sslMode", pool.getProperty("sslmode", "REQUIRED"));
        connection.setProperty("fallbackToSystemKeyStore", "false");
        String url = "jdbc:mysql://" + host + ":" + pool.getProperty("portnumber", "3306") + "/sistema_nutricion";
        try (Connection db = DriverManager.getConnection(url, connection)) {
            db.setReadOnly(true);
            DatabaseMetaData metadata = db.getMetaData();
            System.out.println("BASE " + db.getCatalog() + " MOTOR " + metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
            for (String table : new String[]{"consultorios", "usuarios", "pacientes", "alergias", "paciente_alergias",
                    "citas", "consultas", "horarios_laborales", "configuracion_agenda", "planes_tratamiento",
                    "archivos_adjuntos", "vinculos_google_calendar", "bloqueos_calendario", "recordatorios"}) {
                System.out.println("TABLA " + table);
                try (ResultSet columns = metadata.getColumns(db.getCatalog(), null, table, null)) {
                    while (columns.next()) System.out.println("  COLUMNA " + columns.getString("COLUMN_NAME") + " "
                            + columns.getString("TYPE_NAME") + "(" + columns.getInt("COLUMN_SIZE") + ") NULL=" + columns.getString("IS_NULLABLE"));
                }
                try (ResultSet keys = metadata.getImportedKeys(db.getCatalog(), null, table)) {
                    while (keys.next()) System.out.println("  FK " + keys.getString("FKCOLUMN_NAME") + " -> "
                            + keys.getString("PKTABLE_NAME") + "." + keys.getString("PKCOLUMN_NAME"));
                }
                try (ResultSet indexes = metadata.getIndexInfo(db.getCatalog(), null, table, true, false)) {
                    while (indexes.next()) {
                        String columna = indexes.getString("COLUMN_NAME");
                        if (columna != null) {
                            System.out.println("  UNIQUE " + indexes.getString("INDEX_NAME") + " "
                                    + indexes.getInt("ORDINAL_POSITION") + " " + columna);
                        }
                    }
                }
            }
        }
    }
}
