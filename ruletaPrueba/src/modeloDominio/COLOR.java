package modeloDominio;

/**
 * Enum COLOR
 * ----------
 * Representa los posibles colores de una casilla en la ruleta.
 * Los valores están definidos según las reglas estándar:
 *  - VERDE → corresponde al número 0.
 *  - ROJO  → corresponde a ciertos números según la paridad y rango.
 *  - NEGRO → corresponde a ciertos números según la paridad y rango.
 *
 * PRECONDICIONES:
 *  - El color se asigna únicamente a través de la lógica de la clase Casilla.
 *
 * POSTCONDICIONES:
 *  - Se obtiene un valor de color válido (VERDE, ROJO o NEGRO).
 *  - El método toString() devuelve el nombre del color en mayúsculas.
 */
public enum COLOR {
    VERDE, 
    ROJO, 
    NEGRO;
}
