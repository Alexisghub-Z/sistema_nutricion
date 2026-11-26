package com.nutricion.servicios;

import com.nutricion.entidades.Paciente;
import com.nutricion.entidades.PlanTratamiento;
import com.nutricion.util.ReglaNegocioException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManager;

/** Comprueba la distribución de macronutrientes antes de escribir el plan. */
public class PlanTratamientoFacadeTest {

    public static void main(String[] args) throws Exception {
        AtomicReference<PlanTratamiento> persistido = new AtomicReference<>();
        EntityManager em = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(), new Class<?>[]{EntityManager.class},
                (proxy, method, parametros) -> {
                    if (!"persist".equals(method.getName())) {
                        throw new AssertionError("Operación inesperada: " + method.getName());
                    }
                    persistido.set((PlanTratamiento) parametros[0]);
                    return null;
                });
        PlanTratamientoFacade facade = new PlanTratamientoFacade();
        Field campo = PlanTratamientoFacade.class.getDeclaredField("em");
        campo.setAccessible(true);
        campo.set(facade, em);

        rechaza(facade, plan(-5, 40, 65), "porcentaje entre 0 y 100");
        rechaza(facade, plan(101, 0, 0), "porcentaje entre 0 y 100");
        rechaza(facade, plan(null, 30, 70), "porcentaje entre 0 y 100");
        rechaza(facade, plan(20, 30, 49), "exactamente 100%");
        if (persistido.get() != null) {
            throw new AssertionError("Se escribió un plan inválido.");
        }

        PlanTratamiento valido = plan(0, 0, 100);
        facade.guardar(valido);
        if (persistido.get() != valido || !Double.valueOf(450).equals(valido.getGramosCarbohidratos())) {
            throw new AssertionError("Los límites válidos deben guardarse con sus gramos.");
        }
        System.out.println("RESULTADO 5 comprobaciones de macronutrientes correctas");
    }

    private static PlanTratamiento plan(Integer proteinas, Integer lipidos, Integer carbohidratos) {
        PlanTratamiento plan = new PlanTratamiento();
        plan.setPaciente(new Paciente());
        plan.setMetaKcal(1800.0);
        plan.setPctProteinas(proteinas);
        plan.setPctLipidos(lipidos);
        plan.setPctCarbohidratos(carbohidratos);
        return plan;
    }

    private static void rechaza(PlanTratamientoFacade facade, PlanTratamiento plan, String mensaje) {
        try {
            facade.guardar(plan);
            throw new AssertionError("El plan aceptó porcentajes inválidos.");
        } catch (ReglaNegocioException esperado) {
            if (!esperado.getMessage().contains(mensaje)) {
                throw new AssertionError("Mensaje inesperado: " + esperado.getMessage(), esperado);
            }
        }
    }
}
