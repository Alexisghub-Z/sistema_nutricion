package com.nutricion.util;

/**
 * Cálculos antropométricos usados en las consultas.
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
        if (pesoKg == null || tallaM == null) {
            return null;
        }
        double cm = tallaM > 3 ? tallaM : tallaM * 100;
        double geb = 10 * pesoKg + 6.25 * cm - 5 * edad + (masculino ? 5 : -161);
        return redondear(geb, 0);
    }

    public static double redondear(double valor, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(valor * f) / f;
    }
}
