package com.nutricion.util;

import com.nutricion.entidades.FormulaEnergetica;

/**
 * Cálculos antropométricos y dietéticos usados en las consultas (HU-08, HU-22, HU-23, HU-24).
 */
public final class CalculosNutricion {

    private CalculosNutricion() {
    }

    /** IMC = peso (kg) / talla (m)². Devuelve null si falta algún dato. */
    public static Double imc(Double pesoKg, Double tallaM) {
        if (pesoKg == null || tallaM == null || pesoKg <= 0 || tallaM <= 0) {
            return null;
        }
        // Si capturaron la talla en centímetros (ej. 165) la convertimos a metros.
        double t = tallaM > 3 ? tallaM / 100.0 : tallaM;
        return redondear(pesoKg / (t * t), 2);
    }

    /** Clasificación de la OMS para adultos. */
    public static String clasificarImc(Double imc) {
        if (imc == null) {
            return "—";
        }
        if (imc < 18.5) {
            return "Bajo peso";
        }
        if (imc < 25) {
            return "Normal";
        }
        if (imc < 30) {
            return "Sobrepeso";
        }
        if (imc < 35) {
            return "Obesidad I";
        }
        if (imc < 40) {
            return "Obesidad II";
        }
        return "Obesidad III";
    }

    public static Double indiceCinturaCadera(Double cintura, Double cadera) {
        if (cintura == null || cadera == null || cadera <= 0) {
            return null;
        }
        return redondear(cintura / cadera, 2);
    }

    /**
     * Gasto energético basal (Mifflin-St Jeor), en kcal/día.
     *
     * @param masculino true si es hombre
     */
    public static Double gastoEnergeticoBasal(Double pesoKg, Double tallaM, int edad, boolean masculino) {
        return gastoEnergeticoBasal(FormulaEnergetica.MIFFLIN_ST_JEOR, pesoKg, tallaM, edad, masculino, null);
    }

    /**
     * Calcula el GEB según la fórmula seleccionada (HU-22).
     * Katch-McArdle y Cunningham requieren porcentaje de grasa corporal.
     */
    public static Double gastoEnergeticoBasal(FormulaEnergetica formula, Double pesoKg, Double tallaM,
            int edad, boolean masculino, Double porcentajeGrasa) {
        if (formula == null || pesoKg == null || pesoKg <= 0) {
            return null;
        }
        double cm = tallaM == null ? 0 : (tallaM > 3 ? tallaM : tallaM * 100.0);
        switch (formula) {
            case HARRIS_BENEDICT:
                if (tallaM == null || tallaM <= 0) return null;
                return redondear(masculino
                        ? 66.47 + 13.75 * pesoKg + 5.003 * cm - 6.755 * edad
                        : 655.1 + 9.563 * pesoKg + 1.850 * cm - 4.676 * edad, 0);
            case MIFFLIN_ST_JEOR:
                if (tallaM == null || tallaM <= 0) return null;
                return redondear(10.0 * pesoKg + 6.25 * cm - 5.0 * edad + (masculino ? 5 : -161), 0);
            case KATCH_MCARDLE: {
                if (porcentajeGrasa == null || porcentajeGrasa <= 0 || porcentajeGrasa >= 100) return null;
                double mlg = pesoKg * (1.0 - porcentajeGrasa / 100.0);
                return redondear(370.0 + 21.6 * mlg, 0);
            }
            case CUNNINGHAM: {
                if (porcentajeGrasa == null || porcentajeGrasa <= 0 || porcentajeGrasa >= 100) return null;
                double mlg = pesoKg * (1.0 - porcentajeGrasa / 100.0);
                return redondear(500.0 + 22.0 * mlg, 0);
            }
            case FAO_OMS: {
                if (edad < 30) {
                    return redondear(masculino ? 15.3 * pesoKg + 679 : 14.7 * pesoKg + 496, 0);
                }
                if (edad <= 60) {
                    return redondear(masculino ? 11.6 * pesoKg + 879 : 8.7 * pesoKg + 829, 0);
                }
                return redondear(masculino ? 13.5 * pesoKg + 487 : 10.5 * pesoKg + 596, 0);
            }
            default:
                return null;
        }
    }

    /** Gramos diarios de un macronutriente a partir de la meta calórica, porcentaje y kcal/gramo (4 o 9). */
    public static Double gramosMacro(Double metaKcal, Integer porcentaje, int kcalPorGramo) {
        if (metaKcal == null || porcentaje == null || kcalPorGramo <= 0) {
            return null;
        }
        return redondear((metaKcal * (porcentaje / 100.0)) / kcalPorGramo, 1);
    }

    /** Variación porcentual entre valor anterior y actual. */
    public static Double variacionPorcentual(Double actual, Double anterior) {
        if (actual == null || anterior == null || anterior == 0) {
            return null;
        }
        return redondear(((actual - anterior) / anterior) * 100.0, 1);
    }

    public static double redondear(double valor, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(valor * f) / f;
    }
}
