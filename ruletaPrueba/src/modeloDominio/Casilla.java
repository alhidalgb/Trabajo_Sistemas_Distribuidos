package modeloDominio;

import java.util.Objects;

/**
 * Clase Casilla
 * -------------
 * Value Object inmutable que representa una celda del tablero de la ruleta.
 * Encapsula la lógica de negocio sobre qué color corresponde a cada número.
 *
 * PRECONDICIONES:
 * - El número debe estar estrictamente en el rango [0, 36].
 */
public class Casilla {

    /**
     * Enum COLOR
     * ----------
     * Propiedad intrínseca de una casilla.
     */
    public enum COLOR {
        VERDE, 
        ROJO, 
        NEGRO
    }

    // --- ATRIBUTOS ---
    private final int numero;
    private final COLOR color;

    // --- CONSTRUCTOR ---
    /**
     * Crea una casilla y calcula su color automáticamente según las reglas europeas.
     *
     * @param num Número de la casilla (0-36).
     * @throws IllegalArgumentException Si el número está fuera del rango permitido.
     */
    public Casilla(int num) {
        if (num < 0 || num > 36) {
            throw new IllegalArgumentException("El número de casilla debe estar entre 0 y 36. Recibido: " + num);
        }

        this.numero = num;
        this.color = determinarColor(num);
    }

    // --- LÓGICA PRIVADA (Auxiliar) ---
    /**
     * Aplica la lógica estándar de la Ruleta para asignar colores.
     * - 0: Verde
     * - Rangos 1-10 y 19-28: Impar=Rojo, Par=Negro
     * - Rangos 11-18 y 29-36: Impar=Negro, Par=Rojo
     */
    private COLOR determinarColor(int n) {
        if (n == 0) return COLOR.VERDE;

        boolean esImpar = (n % 2 != 0);

        if ((n >= 1 && n <= 10) || (n >= 19 && n <= 28)) {
            return esImpar ? COLOR.ROJO : COLOR.NEGRO;
        } else {
            // Rangos 11-18 y 29-36
            return esImpar ? COLOR.NEGRO : COLOR.ROJO;
        }
    }

    // --- GETTERS ---
    
    public int getNumero() {
        return numero;
    }

    /**
     * @return El ENUM del color (Mejor que String para comparaciones lógicas).
     */
    public COLOR getColor() {
        return color;
    }

    /**
     * Calcula la docena a la que pertenece el número.
     * @return 1 (1-12), 2 (13-24), 3 (25-36) o 0 (si es el 0).
     */
    public int getDocena() {
        if (this.numero >= 1 && this.numero <= 12) return 1;
        if (this.numero >= 13 && this.numero <= 24) return 2;
        if (this.numero >= 25 && this.numero <= 36) return 3;
        return 0; // Caso del 0
    }

    // --- MÉTODOS DE OBJETO ---

    @Override
    public String toString() {
        return "Casilla [" + numero + " | " + color + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Casilla)) return false;
        Casilla other = (Casilla) obj;
        return numero == other.numero && color == other.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, color);
    }
}