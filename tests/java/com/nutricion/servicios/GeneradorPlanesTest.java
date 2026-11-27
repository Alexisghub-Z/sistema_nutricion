package com.nutricion.servicios;

import com.nutricion.entidades.Alergia;
import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PlanAlimenticio;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.entidades.PorcionPlan;
import com.nutricion.entidades.TiempoComida;
import com.nutricion.entidades.TipoAlergia;
import com.nutricion.servicios.CatalogoSmae.Alimento;
import com.nutricion.util.ReglaNegocioException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneradorPlanesTest {

    private static int comprobaciones;

    public static void main(String[] args) {
        PlanTratamiento tratamiento = tratamiento(1800, 20, 30, 50);
        Set<Alimento> catalogo = EnumSet.allOf(Alimento.class);
        PlanAlimenticio plan = GeneradorPlanes.generar(tratamiento, catalogo);
        comprobar(plan.getId() == null && plan.getFechaAprobacion() == null && plan.getFechaEntrega() == null,
                "El generador devuelve un borrador sin estado de entrega ni persistencia.");
        comprobar(plan.coincideCon(tratamiento), "El borrador conserva el cálculo guardado.");
        comprobar(Arrays.stream(TiempoComida.values()).allMatch(t -> !plan.porcionesDe(t).isEmpty()),
                "Los cinco tiempos tienen porciones.");
        comprobar(Math.abs(plan.getKcal() - 1800) <= 90 && Math.abs(plan.getProteinas() - 90) <= 9
                && Math.abs(plan.getLipidos() - 60) <= 6 && Math.abs(plan.getCarbohidratos() - 225) <= 22.5,
                "La energía y los tres macros quedan en sus tolerancias.");
        comprobar(plan.getMargenKcal() == 90 && plan.getMargenProteinas() == 9
                && plan.getMargenLipidos() == 6 && plan.getMargenCarbohidratos() == 22.5,
                "Los márgenes de revisión corresponden a la meta guardada.");
        comprobar(PlanAlimenticio.margenMacro(0) == 0 && PlanAlimenticio.margenMacro(15) == 2
                && PlanAlimenticio.margenMacro(30) == 3,
                "Para macros se conserva cero estricto, el mínimo de 2 g y el margen proporcional.");
        comprobar(plan.getPorciones().stream().allMatch(p -> p.getMediosEquivalentes() > 0),
                "Las porciones son múltiplos positivos de medio equivalente.");
        comprobar(Alimento.ARROZ.porcion(TiempoComida.COMIDA, 1).getMedida().equals("1/8 taza")
                && Alimento.POLLO.porcion(TiempoComida.COMIDA, 3).getMedida().equals("45 g")
                && Alimento.AGUACATE.porcion(TiempoComida.COMIDA, 3).getMedida().equals("1/2 pieza"),
                "Las medidas fraccionarias y en gramos son exactas.");

        Paciente paciente = new Paciente();
        paciente.setAlergias(" PÓLLO ");
        paciente.setIntolerancias("Lactosa");
        paciente.setAlergiasOtras("Frutos secos (nuez, almendra); Cacahuate");
        Alergia huevo = new Alergia("Huevo", TipoAlergia.ALERGIA, "AOA");
        Set<Alimento> permitidos = CatalogoSmae.permitidos(paciente, Collections.singletonList(huevo));
        comprobar(!permitidos.contains(Alimento.POLLO) && !permitidos.contains(Alimento.LECHE)
                && !permitidos.contains(Alimento.HUEVO), "Se combinan catálogo y los tres campos de texto.");
        PlanAlimenticio alternativo = GeneradorPlanes.generar(tratamiento, permitidos);
        Set<String> codigos = alternativo.getPorciones().stream().map(PorcionPlan::getCodigoAlimento).collect(Collectors.toSet());
        comprobar(codigos.contains(Alimento.ATUN.name()) && !codigos.contains(Alimento.POLLO.name())
                && !codigos.contains(Alimento.LECHE.name()) && !codigos.contains(Alimento.HUEVO.name()),
                "Preserva los objetivos con alimentos alternativos.");
        rechaza(() -> GeneradorPlanes.validar(plan, permitidos), "alimento restringido");
        paciente.setAlergiasOtras("Fructosa o alguna fruta pendiente de identificar");
        rechaza(() -> CatalogoSmae.permitidos(paciente, Collections.emptyList()), "no puede verificar la restricción");

        paciente.setAlergiasOtras("Frutos secos (nuez, almendra); Fructosa; Sulfitos");
        RevisionCatalogoSmae revision = CatalogoSmae.revisar(paciente, Collections.singletonList(huevo));
        comprobar(revision.getAlimentos().size() == catalogo.size()
                && revision.getAlimentos().stream().anyMatch(ficha ->
                    ficha.getNombre().equals("Arroz cocido") && ficha.getMedida().equals("1/4 taza")),
                "La revisión incluye el catálogo completo con la medida por equivalente.");
        comprobar(revision.getSinVerificar().equals(Arrays.asList("Fructosa", "Sulfitos"))
                && revision.getSinCoincidencia().equals(Collections.singletonList("Frutos secos (nuez, almendra)")),
                "Las restricciones pendientes se distinguen de los ingredientes ausentes.");
        comprobar(revision.getAlimentos().stream().anyMatch(ficha ->
                    ficha.getNombre().equals("Leche descremada") && ficha.getMotivoExclusion().equals("Lactosa"))
                && revision.getAlimentos().stream().anyMatch(ficha ->
                    ficha.getNombre().equals("Huevo") && ficha.getMotivoExclusion().equals("Huevo")),
                "La consulta refleja exclusiones guardadas por catálogo y texto.");
        rechaza(() -> CatalogoSmae.permitidos(paciente, Collections.emptyList()), "«Fructosa»");

        rechaza(() -> GeneradorPlanes.generar(tratamiento, EnumSet.of(Alimento.OLIVA)), "No se encontró una combinación");
        rechaza(() -> GeneradorPlanes.generar(tratamiento(1800, 0, 0, 100), catalogo), "No se encontró una combinación");
        rechaza(() -> GeneradorPlanes.generar(new PlanTratamiento(), catalogo), "Guarda una meta");
        tratamiento.setGramosProteinas(91.0);
        comprobar(!plan.coincideCon(tratamiento), "La revisión detecta una modificación de los macros guardados.");

        System.out.println("RESULTADO " + comprobaciones + " comprobaciones del plan alimenticio correctas");
    }

    private static PlanTratamiento tratamiento(double kcal, int proteinas, int lipidos, int carbohidratos) {
        PlanTratamiento tratamiento = new PlanTratamiento();
        tratamiento.setId(1L);
        tratamiento.setMetaKcal(kcal);
        tratamiento.setPctProteinas(proteinas);
        tratamiento.setPctLipidos(lipidos);
        tratamiento.setPctCarbohidratos(carbohidratos);
        tratamiento.recalcularMacros();
        return tratamiento;
    }

    private static void comprobar(boolean correcto, String caso) {
        if (!correcto) {
            throw new AssertionError(caso);
        }
        comprobaciones++;
    }

    private static void rechaza(Runnable accion, String mensaje) {
        try {
            accion.run();
            throw new AssertionError("Se esperaba el rechazo: " + mensaje);
        } catch (ReglaNegocioException esperado) {
            comprobar(esperado.getMessage().contains(mensaje), esperado.getMessage());
        }
    }
}
