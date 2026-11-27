package com.nutricion.servicios;

import com.nutricion.entidades.PlanAlimenticio;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.entidades.PorcionPlan;
import com.nutricion.entidades.TiempoComida;
import com.nutricion.servicios.CatalogoSmae.Alimento;
import com.nutricion.servicios.CatalogoSmae.Grupo;
import com.nutricion.util.ReglaNegocioException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class GeneradorPlanes {

    private static final Grupo[] GRUPOS = Grupo.values();
    private static final Grupo[] BASE = {Grupo.VERDURA, Grupo.FRUTA, Grupo.LEGUMINOSA, Grupo.LECHE, Grupo.ANIMAL_MODERADO};
    private static final TiempoComida[] TIEMPOS = TiempoComida.values();

    private GeneradorPlanes() { }

    static PlanAlimenticio generar(PlanTratamiento tratamiento, Set<Alimento> permitidos) {
        if (tratamiento == null || !valido(tratamiento.getMetaKcal()) || tratamiento.getMetaKcal() < 500
                || tratamiento.getMetaKcal() > 8000
                || !valido(tratamiento.getGramosProteinas()) || !valido(tratamiento.getGramosLipidos())
                || !valido(tratamiento.getGramosCarbohidratos())) {
            throw new ReglaNegocioException("Guarda una meta y los tres macronutrientes en Cálculo dietético para generar el borrador.");
        }
        Busqueda busqueda = new Busqueda(tratamiento, permitidos);
        busqueda.recorrer(0, 0, 0, 0, 0);
        if (busqueda.mejor == null) {
            throw new ReglaNegocioException("No se encontró una combinación que cumpla la meta y los macros con el catálogo SMAE inicial "
                    + "y las restricciones registradas. Revisa la meta y las alternativas del paciente.");
        }
        List<PorcionPlan> porciones = distribuir(busqueda.mejor, permitidos);
        PlanAlimenticio plan = new PlanAlimenticio(tratamiento, porciones, busqueda.kcal, busqueda.proteinas,
                busqueda.lipidos, busqueda.carbohidratos);
        validar(plan, permitidos);
        return plan;
    }

    static void validar(PlanAlimenticio plan, Set<Alimento> permitidos) {
        double kcal = 0;
        double proteinas = 0;
        double lipidos = 0;
        double carbohidratos = 0;
        Set<TiempoComida> tiempos = EnumSet.noneOf(TiempoComida.class);
        for (PorcionPlan porcion : plan.getPorciones()) {
            Alimento alimento;
            try {
                alimento = Alimento.valueOf(porcion.getCodigoAlimento());
            } catch (IllegalArgumentException e) {
                throw new ReglaNegocioException("Un alimento del plan ya no está en el catálogo SMAE. Genera otro borrador.");
            }
            if (!permitidos.contains(alimento)) {
                throw new ReglaNegocioException("El plan incluye un alimento restringido (" + alimento.etiqueta
                        + "). Genera otro borrador con las alergias vigentes.");
            }
            int medios = porcion.getMediosEquivalentes();
            PorcionPlan esperada = alimento.porcion(porcion.getTiempo(), medios);
            if (medios <= 0 || !esperada.getMedida().equals(porcion.getMedida())
                    || !esperada.getGrupo().equals(porcion.getGrupo()) || !esperada.getAlimento().equals(porcion.getAlimento())) {
                throw new ReglaNegocioException("Una porción ya no coincide con el catálogo SMAE. Genera otro borrador.");
            }
            tiempos.add(porcion.getTiempo());
            double equivalentes = porcion.getEquivalentes();
            Grupo grupo = alimento.grupo;
            kcal += equivalentes * grupo.kcal;
            proteinas += equivalentes * grupo.proteinas;
            lipidos += equivalentes * grupo.lipidos;
            carbohidratos += equivalentes * grupo.carbohidratos;
        }
        if (tiempos.size() != TIEMPOS.length || kcal != plan.getKcal() || proteinas != plan.getProteinas()
                || lipidos != plan.getLipidos() || carbohidratos != plan.getCarbohidratos()
                || !enTolerancia(kcal, plan.getMetaKcal(), plan.getMetaKcal() * 0.05)
                || !enTolerancia(proteinas, plan.getMetaProteinas(), toleranciaMacro(plan.getMetaProteinas()))
                || !enTolerancia(lipidos, plan.getMetaLipidos(), toleranciaMacro(plan.getMetaLipidos()))
                || !enTolerancia(carbohidratos, plan.getMetaCarbohidratos(), toleranciaMacro(plan.getMetaCarbohidratos()))) {
            throw new ReglaNegocioException("Las porciones no cumplen la meta guardada. Genera otro borrador.");
        }
    }

    private static List<PorcionPlan> distribuir(int[] cantidades, Set<Alimento> permitidos) {
        int[][] distribucion = new int[TIEMPOS.length][GRUPOS.length];
        int[] totales = new int[TIEMPOS.length];
        for (Grupo grupo : GRUPOS) {
            int medios = cantidades[grupo.ordinal()];
            for (int i = 0; i < medios; i += 2) {
                int tiempo = grupo.tiempos[(i / 2) % grupo.tiempos.length].ordinal();
                int cantidad = Math.min(2, medios - i);
                distribucion[tiempo][grupo.ordinal()] += cantidad;
                totales[tiempo] += cantidad;
            }
        }
        for (int tiempo = 0; tiempo < TIEMPOS.length; tiempo++) {
            if (totales[tiempo] > 0) {
                continue;
            }
            int donante = 0;
            for (int i = 1; i < TIEMPOS.length; i++) {
                if (totales[i] > totales[donante]) {
                    donante = i;
                }
            }
            int grupo = 0;
            for (int i = 1; i < GRUPOS.length; i++) {
                if (distribucion[donante][i] > distribucion[donante][grupo]) {
                    grupo = i;
                }
            }
            distribucion[donante][grupo]--;
            distribucion[tiempo][grupo]++;
            totales[donante]--;
            totales[tiempo]++;
        }
        List<PorcionPlan> porciones = new ArrayList<>();
        for (Grupo grupo : GRUPOS) {
            List<Alimento> opciones = permitidos.stream().filter(a -> a.grupo == grupo).collect(Collectors.toList());
            int seleccion = 0;
            for (TiempoComida tiempo : TIEMPOS) {
                int medios = distribucion[tiempo.ordinal()][grupo.ordinal()];
                if (medios > 0) {
                    porciones.add(opciones.get(seleccion++ % opciones.size()).porcion(tiempo, medios));
                }
            }
        }
        return porciones;
    }

    private static boolean valido(Double valor) { return valor != null && Double.isFinite(valor) && valor >= 0; }

    private static boolean enTolerancia(double valor, double meta, double tolerancia) {
        return Math.abs(valor - meta) <= tolerancia;
    }

    private static double toleranciaMacro(double meta) { return meta == 0 ? 0 : Math.max(2, meta * 0.10); }

    private static final class Busqueda {
        private final double metaKcal, metaProteinas, metaLipidos, metaCarbohidratos;
        private final double margenProteinas, margenLipidos, margenCarbohidratos;
        private final Set<Grupo> disponibles = EnumSet.noneOf(Grupo.class);
        private final int[] cantidades = new int[GRUPOS.length];
        private double mejorPuntuacion = Double.POSITIVE_INFINITY;
        private int[] mejor;
        private double kcal, proteinas, lipidos, carbohidratos;

        Busqueda(PlanTratamiento tratamiento, Set<Alimento> permitidos) {
            metaKcal = tratamiento.getMetaKcal();
            metaProteinas = tratamiento.getGramosProteinas();
            metaLipidos = tratamiento.getGramosLipidos();
            metaCarbohidratos = tratamiento.getGramosCarbohidratos();
            margenProteinas = toleranciaMacro(metaProteinas);
            margenLipidos = toleranciaMacro(metaLipidos);
            margenCarbohidratos = toleranciaMacro(metaCarbohidratos);
            permitidos.forEach(alimento -> disponibles.add(alimento.grupo));
        }

        void recorrer(int indice, double energia, double p, double l, double hc) {
            if (indice == BASE.length) {
                completar(energia, p, l, hc);
                return;
            }
            Grupo grupo = BASE[indice];
            int maximo = disponibles.contains(grupo) ? grupo.limiteBase : 0;
            for (int medios = 0; medios <= maximo; medios++) {
                cantidades[grupo.ordinal()] = medios;
                double eq = medios / 2.0;
                recorrer(indice + 1, energia + eq * grupo.kcal, p + eq * grupo.proteinas,
                        l + eq * grupo.lipidos, hc + eq * grupo.carbohidratos);
            }
        }

        // Los tres grupos restantes completan carbohidratos, proteína y grasa, en ese orden.
        private void completar(double energia, double p, double l, double hc) {
            Grupo cereal = Grupo.CEREAL;
            Grupo animal = Grupo.ANIMAL_MAGRO;
            Grupo grasa = Grupo.GRASA;
            double hcRestantes = metaCarbohidratos - hc;
            int cerealMaximo = maximo(cereal, hcRestantes + margenCarbohidratos, cereal.carbohidratos);
            for (int c = minimo(cereal, hcRestantes - margenCarbohidratos, cereal.carbohidratos); c <= cerealMaximo; c++) {
                cantidades[cereal.ordinal()] = c;
                double nuevosHc = hc + c * cereal.carbohidratos / 2.0;
                double baseProteinas = p + c * cereal.proteinas / 2.0;
                double pRestantes = metaProteinas - baseProteinas;
                int animalMaximo = maximo(animal, pRestantes + margenProteinas, animal.proteinas);
                for (int a = minimo(animal, pRestantes - margenProteinas, animal.proteinas); a <= animalMaximo; a++) {
                    cantidades[animal.ordinal()] = a;
                    double nuevasProteinas = baseProteinas + a * animal.proteinas / 2.0;
                    double baseLipidos = l + a * animal.lipidos / 2.0;
                    double baseKcal = energia + (c * cereal.kcal + a * animal.kcal) / 2.0;
                    double lRestantes = metaLipidos - baseLipidos;
                    double kcalRestantes = metaKcal - baseKcal;
                    int grasaMinima = Math.max(minimo(grasa, lRestantes - margenLipidos, grasa.lipidos),
                            minimo(grasa, kcalRestantes - metaKcal * 0.05, grasa.kcal));
                    int grasaMaxima = Math.min(maximo(grasa, lRestantes + margenLipidos, grasa.lipidos),
                            maximo(grasa, kcalRestantes + metaKcal * 0.05, grasa.kcal));
                    for (int g = grasaMinima; g <= grasaMaxima; g++) {
                        cantidades[grasa.ordinal()] = g;
                        double nuevasKcal = baseKcal + g * grasa.kcal / 2.0;
                        evaluar(nuevasKcal, nuevasProteinas, baseLipidos + g * grasa.lipidos / 2.0, nuevosHc);
                    }
                }
            }
        }

        private int minimo(Grupo grupo, double aporteMinimo, int porEquivalente) {
            return disponibles.contains(grupo) ? Math.max(0, (int) Math.ceil(2 * aporteMinimo / porEquivalente)) : 0;
        }

        private int maximo(Grupo grupo, double aporteMaximo, int porEquivalente) {
            return disponibles.contains(grupo) ? (int) Math.floor(2 * aporteMaximo / porEquivalente) : 0;
        }

        private void evaluar(double energia, double p, double l, double hc) {
            if (!enTolerancia(energia, metaKcal, metaKcal * 0.05) || !enTolerancia(p, metaProteinas, margenProteinas)
                    || !enTolerancia(l, metaLipidos, margenLipidos) || !enTolerancia(hc, metaCarbohidratos, margenCarbohidratos)) {
                return;
            }
            double puntuacion = Math.abs(energia - metaKcal) / metaKcal
                    + Math.abs(p - metaProteinas) / Math.max(1, metaProteinas)
                    + Math.abs(l - metaLipidos) / Math.max(1, metaLipidos)
                    + Math.abs(hc - metaCarbohidratos) / Math.max(1, metaCarbohidratos);
            int medios = 0;
            for (Grupo grupo : GRUPOS) {
                int cantidad = cantidades[grupo.ordinal()];
                medios += cantidad;
                if (grupo.limiteBase > 0 && disponibles.contains(grupo)) {
                    puntuacion += 0.015 * Math.abs(cantidad - grupo.preferenciaBase);
                }
            }
            if (medios >= TIEMPOS.length && puntuacion < mejorPuntuacion) {
                mejorPuntuacion = puntuacion;
                mejor = Arrays.copyOf(cantidades, cantidades.length);
                kcal = energia;
                proteinas = p;
                lipidos = l;
                carbohidratos = hc;
            }
        }
    }
}
