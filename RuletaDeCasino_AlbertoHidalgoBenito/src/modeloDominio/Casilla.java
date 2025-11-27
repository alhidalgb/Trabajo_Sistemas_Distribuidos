package modeloDominio;

import java.util.Objects;

/**
 * Clase Casilla
 * -------------
 * Representa una casilla de la ruleta, definida por un número y un color asociado.
 * La clase es inmutable: todos los campos son finales y no existen setters.
 *
 * PRECONDICIONES:
 *  - El número debe estar en el rango [0, 36].
 *
 * POSTCONDICIONES:
 *  - Se crea una casilla con número y color asignados según las reglas de la ruleta.
 *  - El método getDocena() devuelve la docena a la que pertenece el número (1, 2, 3) o 0 si es el 0.
 *  - equals() y hashCode() permiten comparar casillas de forma coherente.
 *  - toString() devuelve una representación legible de la casilla.
 */
public class Casilla {

    // --- ATRIBUTOS ---
    private final int numero;
    private final COLOR color;

    // --- CONSTRUCTOR ---
    /**
     * Constructor: Inicializa la casilla y asigna su color basado en las reglas de la ruleta.
     *
     * PRECONDICIONES:
     *  - num >= 0 y num <= 36.
     *
     * POSTCONDICIONES:
     *  - Se asigna el color correcto según el número:
     *      - 0 → VERDE
     *      - 1-10 y 19-28 → Impares ROJO, Pares NEGRO
     *      - 11-18 y 29-36 → Impares NEGRO, Pares ROJO
     */
    public Casilla(int num) {
        this.numero = num;

        if (num == 0) {
            this.color = COLOR.VERDE;
        } else if ((num >= 1 && num <= 10) || (num >= 19 && num <= 28)) {
            this.color = (num % 2 != 0) ? COLOR.ROJO : COLOR.NEGRO;
        } else if ((num >= 11 && num <= 18) || (num >= 29 && num <= 36)) {
            this.color = (num % 2 != 0) ? COLOR.NEGRO : COLOR.ROJO;
        } else {
            // Fallback defensivo: aunque los números válidos son 0-36
            this.color = COLOR.VERDE;
        }
    }

    // --- GETTERS ---
    /**
     * @return Número de la casilla.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * @return Color de la casilla como cadena.
     */
    public String getColor() {
        return color.toString();
    }

    /**
     * Devuelve la docena a la que pertenece el número.
     *
     * POSTCONDICIONES:
     *  - Si el número está entre 1 y 12 → devuelve 1.
     *  - Si el número está entre 13 y 24 → devuelve 2.
     *  - Si el número está entre 25 y 36 → devuelve 3.
     *  - Si el número es 0 → devuelve 0.
     */
    public int getDocena() {
        if (this.numero >= 1 && this.numero <= 12) {
            return 1;
        }
        if (this.numero >= 13 && this.numero <= 24) {
            return 2;
        }
        if (this.numero >= 25 && this.numero <= 36) {
            return 3;
        }
        return 0;
    }

    // --- MÉTODOS DE OBJETO ---
    /**
     * Devuelve una representación en cadena del objeto Casilla.
     */
    @Override
    public String toString() {
        return "[Número=" + numero + ", Color=" + color.toString() + ", Docena=" + this.getDocena() + "]";
    }

    /**
     * Comprueba si dos objetos Casilla son iguales, basándose en número y color.
     *
     * @param obj Objeto a comparar.
     * @return true si son iguales, false en caso contrario.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Casilla)) return false;
        Casilla otraCasilla = (Casilla) obj;
        return numero == otraCasilla.numero && color == otraCasilla.color;
    }

    /**
     * Devuelve un valor hash consistente con equals().
     *
     * @return hash calculado en base a número y color.
     */
    @Override
    public int hashCode() {
        return Objects.hash(numero, color);
    }
}
