package com.nutricion.servicios;

import com.nutricion.entidades.Alergia;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PorcionPlan;
import com.nutricion.entidades.TiempoComida;
import static com.nutricion.entidades.TiempoComida.*;
import com.nutricion.util.ReglaNegocioException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class CatalogoSmae {

    // IMSS: 225GRR, tabla 1 (aportes), y Cartera de Alimentación 2024, pp. 9–11 (medidas).
    enum Grupo {
        VERDURA("Verduras", 25, 2, 0, 4, 8, 6, COMIDA, CENA, DESAYUNO),
        FRUTA("Frutas", 60, 0, 0, 15, 8, 4, COLACION_MATUTINA, COLACION_VESPERTINA, DESAYUNO, COMIDA, CENA),
        LEGUMINOSA("Leguminosas", 120, 8, 1, 20, 4, 2, COMIDA, CENA),
        LECHE("Leche descremada", 95, 9, 2, 12, 4, 2, COLACION_MATUTINA, DESAYUNO, COLACION_VESPERTINA),
        ANIMAL_MODERADO("Origen animal · grasa moderada", 75, 7, 5, 0, 4, 2, DESAYUNO, CENA),
        CEREAL("Cereales sin grasa", 70, 2, 0, 15, 0, 0, DESAYUNO, COMIDA, CENA),
        ANIMAL_MAGRO("Origen animal · muy bajo en grasa", 40, 7, 1, 0, 0, 0, COMIDA, CENA, DESAYUNO),
        GRASA("Grasas sin proteína", 45, 0, 5, 0, 0, 0, DESAYUNO, COMIDA, CENA);

        final String etiqueta;
        final int kcal, proteinas, lipidos, carbohidratos;
        final int limiteBase, preferenciaBase;
        final TiempoComida[] tiempos;

        Grupo(String etiqueta, int kcal, int proteinas, int lipidos, int carbohidratos,
                int limiteBase, int preferenciaBase, TiempoComida... tiempos) {
            this.etiqueta = etiqueta;
            this.kcal = kcal;
            this.proteinas = proteinas;
            this.lipidos = lipidos;
            this.carbohidratos = carbohidratos;
            this.limiteBase = limiteBase;
            this.preferenciaBase = preferenciaBase;
            this.tiempos = tiempos;
        }
    }

    enum Alimento {
        NOPAL(Grupo.VERDURA, "Nopal cocido", 1, 1, "taza", "nopal", "nopales"),
        BROCOLI(Grupo.VERDURA, "Brócoli cocido", 1, 2, "taza", "brócoli"),
        CALABACITA(Grupo.VERDURA, "Calabacita", 1, 1, "pieza", "calabaza"),
        MANZANA(Grupo.FRUTA, "Manzana", 1, 1, "pieza"),
        PAPAYA(Grupo.FRUTA, "Papaya", 1, 1, "taza"),
        MELON(Grupo.FRUTA, "Melón", 1, 1, "taza"),
        FRIJOL(Grupo.LEGUMINOSA, "Frijol entero cocido", 1, 2, "taza", "frijol", "frijoles"),
        LENTEJA(Grupo.LEGUMINOSA, "Lenteja cocida", 1, 2, "taza", "lenteja", "lentejas"),
        LECHE(Grupo.LECHE, "Leche descremada", 1, 1, "taza"),
        HUEVO(Grupo.ANIMAL_MODERADO, "Huevo", 1, 1, "pieza"),
        TORTILLA(Grupo.CEREAL, "Tortilla de maíz", 1, 1, "pieza", "tortilla", "maíz"),
        ARROZ(Grupo.CEREAL, "Arroz cocido", 1, 4, "taza", "arroz"),
        POLLO(Grupo.ANIMAL_MAGRO, "Pechuga de pollo sin piel", 30, 1, "g", "pollo", "pechuga de pollo"),
        ATUN(Grupo.ANIMAL_MAGRO, "Atún fresco", 30, 1, "g", "atún"),
        OLIVA(Grupo.GRASA, "Aceite de oliva", 1, 1, "cucharadita", "oliva", "aceituna"),
        AGUACATE(Grupo.GRASA, "Aguacate", 1, 3, "pieza");

        final Grupo grupo;
        final String etiqueta;
        private final int numerador, denominador;
        private final String unidad;
        private final String[] alias;

        Alimento(Grupo grupo, String etiqueta, int numerador, int denominador, String unidad, String... alias) {
            this.grupo = grupo;
            this.etiqueta = etiqueta;
            this.numerador = numerador;
            this.denominador = denominador;
            this.unidad = unidad;
            this.alias = alias;
        }

        PorcionPlan porcion(TiempoComida tiempo, int medios) {
            int cantidad = numerador * medios;
            int divisor = denominador * 2;
            int enteros = cantidad / divisor;
            int resto = cantidad % divisor;
            String medida = Integer.toString(enteros);
            if (resto != 0) {
                int comun = mcd(resto, divisor);
                medida = (enteros == 0 ? "" : enteros + " ") + resto / comun + "/" + divisor / comun;
            }
            String unidades = enteros > 0 && cantidad > divisor && !"g".equals(unidad) ? unidad + "s" : unidad;
            return new PorcionPlan(tiempo, name(), etiqueta, grupo.etiqueta, medios, medida + " " + unidades);
        }
    }

    private static final Pattern TILDES = Pattern.compile("\\p{M}+");
    private static final Map<String, Set<Alimento>> RESTRICCIONES = restricciones();

    private CatalogoSmae() { }

    static Set<Alimento> permitidos(Paciente paciente, List<Alergia> alergias) {
        Set<Alimento> permitidos = EnumSet.allOf(Alimento.class);
        for (Alergia alergia : alergias) {
            excluir(permitidos, alergia.getNombre());
        }
        for (String campo : Arrays.asList(paciente.getAlergias(), paciente.getIntolerancias(), paciente.getAlergiasOtras())) {
            if (campo == null || campo.trim().isEmpty()) {
                continue;
            }
            String valor = normalizar(campo);
            if (Arrays.asList("ninguna", "ninguno", "sin alergias", "sin intolerancias",
                    "sin alergias ni intolerancias").contains(valor)) {
                continue;
            }
            if (RESTRICCIONES.containsKey(valor)) {
                excluir(permitidos, campo);
            } else {
                for (String parte : campo.split("[,;\\r\\n]+(?![^()]*\\))")) {
                    if (!parte.trim().isEmpty()) {
                        excluir(permitidos, parte);
                    }
                }
                if (valor.matches("[,; ]+")) {
                    excluir(permitidos, campo);
                }
            }
        }
        return permitidos;
    }

    private static void excluir(Set<Alimento> permitidos, String nombre) {
        Set<Alimento> excluir = RESTRICCIONES.get(normalizar(nombre));
        if (excluir == null) {
            throw new ReglaNegocioException("El catálogo SMAE inicial no puede verificar la restricción «" + nombre.trim()
                    + "». Revisa su registro en Alergias e intolerancias antes de continuar.");
        }
        permitidos.removeAll(excluir);
    }

    private static Map<String, Set<Alimento>> restricciones() {
        Map<String, Set<Alimento>> mapa = new HashMap<>();
        for (Alimento alimento : Alimento.values()) {
            registrar(mapa, EnumSet.of(alimento), alimento.alias);
            registrar(mapa, EnumSet.of(alimento), alimento.etiqueta);
        }
        registrar(mapa, EnumSet.of(Alimento.LECHE), "lácteos", "leche", "lactosa", "proteína de leche de vaca");
        registrar(mapa, EnumSet.of(Alimento.ATUN), "pescado", "pescados");
        registrar(mapa, EnumSet.of(Alimento.NOPAL, Alimento.BROCOLI, Alimento.CALABACITA), "verduras");
        registrar(mapa, EnumSet.of(Alimento.MANZANA, Alimento.PAPAYA, Alimento.MELON), "frutas");
        registrar(mapa, EnumSet.of(Alimento.FRIJOL, Alimento.LENTEJA), "leguminosas");
        registrar(mapa, EnumSet.of(Alimento.TORTILLA, Alimento.ARROZ), "cereales");
        registrar(mapa, Collections.emptySet(), "cacahuate", "frutos secos (nuez, almendra)", "frutos secos", "nuez",
                "almendra", "mariscos y crustáceos", "mariscos", "crustáceos", "soya", "soja", "gluten / trigo", "gluten", "trigo");
        return mapa;
    }

    private static void registrar(Map<String, Set<Alimento>> mapa, Set<Alimento> alimentos, String... nombres) {
        for (String nombre : nombres) {
            mapa.put(normalizar(nombre), alimentos);
        }
    }

    private static String normalizar(String texto) {
        String descompuesto = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return TILDES.matcher(descompuesto).replaceAll("").replaceAll("\\s+", " ").replaceAll(" ?/ ?", " / ");
    }

    private static int mcd(int a, int b) {
        while (b != 0) {
            int resto = a % b;
            a = b;
            b = resto;
        }
        return a;
    }
}
