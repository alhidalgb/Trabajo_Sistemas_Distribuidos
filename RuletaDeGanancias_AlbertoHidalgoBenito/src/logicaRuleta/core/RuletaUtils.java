package logicaRuleta.core;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;

/**
 * Clase RuletaUtils
 * -----------------
 * Métodos utilitarios estáticos para cálculos de la ruleta.
 * No dependen de estado de instancia, por lo que son thread-safe.
 */
public final class RuletaUtils {

    // Constructor privado para evitar instanciación
    private RuletaUtils() {}

    /**
     * Calcula el premio de una apuesta concreta según la casilla ganadora.
     *
     * @param ganadora Casilla ganadora.
     * @param apuesta  Apuesta del jugador.
     * @return Importe ganado (0 si perdió).
     */
    public static double calcularPremio(Casilla ganadora, Apuesta apuesta) {
        double cantidad = apuesta.getCantidad();
        String valorApostado = apuesta.getValor();

        switch (apuesta.getTipo()) {
            case NUMERO:
                try {
                    int numeroApostado = Integer.parseInt(valorApostado);
                    
                   
                    
                    if (numeroApostado == ganadora.getNumero()) {
                    	
                    	 if(numeroApostado == 0) {
                         	return cantidad*300;//Te haces rico
                         }
                    	
                        return cantidad * 36;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Error formato número: " + valorApostado);
                }
                break;

            case COLOR:
                if (ganadora.getNumero() != 0 &&
                    valorApostado.equalsIgnoreCase(ganadora.getColor().toString())) {
                    return cantidad * 2;
                }
                break;

            case PAR_IMPAR:
                if (ganadora.getNumero() != 0) {
                    boolean apostoPar = valorApostado.equalsIgnoreCase("PAR");
                    if ((apostoPar && ganadora.getNumero() % 2 == 0) ||
                        (!apostoPar && ganadora.getNumero() % 2 != 0)) {
                        return cantidad * 2;
                    }
                }
                break;

            case DOCENA:
                try {
                    int docenaApostada = Integer.parseInt(valorApostado);
                    if (docenaApostada == ganadora.getDocena()) {
                        return cantidad * 3;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Error formato docena: " + valorApostado);
                }
                break;

            default:
                return 0;
        }

        return 0;
    }
}
