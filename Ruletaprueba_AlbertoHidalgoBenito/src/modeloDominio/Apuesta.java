package modeloDominio;

import java.util.Objects;

/**
 * Clase Apuesta
 * -------------
 * Representa una apuesta realizada por un jugador en la ruleta.
 * Contiene el tipo de apuesta, el valor apostado y la cantidad de dinero.
 * Esta clase es inmutable: todos los campos son finales y no existen setters.
 *
 * PRECONDICIONES:
 *  - jugador != null
 *  - tipo != null
 *  - valor debe ser válido para el tipo de apuesta
 *  - cantidad > 0
 *
 * POSTCONDICIONES:
 *  - Se crea un objeto Apuesta inmutable con los valores proporcionados.
 *  - equals() y hashCode() permiten comparar apuestas de forma coherente.
 *  - toString() devuelve una representación legible de la apuesta.
 */
public class Apuesta {

    // --- ATRIBUTOS ---
    private final Jugador jugador; 
    private final TipoApuesta tipo;
    private final String valor;
    private final double cantidad;

    // --- CONSTRUCTOR ---
    /**
     * Constructor que inicializa todos los parámetros de la apuesta.
     * 
     * @param j Jugador que realiza la apuesta (no null).
     * @param t Tipo de apuesta (no null).
     * @param v Valor apostado (no null, debe ser válido para el tipo).
     * @param cantidad Cantidad de dinero apostada (> 0).
     * 
     * PRECONDICIONES:
     *  - j, t y v no pueden ser nulos.
     *  - cantidad > 0.
     * 
     * POSTCONDICIONES:
     *  - Se crea un objeto Apuesta inmutable con los valores proporcionados.
     */
    public Apuesta(Jugador j, TipoApuesta t, String v, double cantidad) {
        if (j == null || t == null || v == null) {
            throw new IllegalArgumentException("Jugador, tipo y valor no pueden ser nulos");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        this.jugador = j;
        this.tipo = t;
        this.valor = v;
        this.cantidad = cantidad; 
    }

    // --- GETTERS ---
    /**
     * @return Jugador que realizó la apuesta.
     */
    public Jugador getJugador() { return jugador; }

    /**
     * @return Tipo de la apuesta.
     */
    public TipoApuesta getTipo() { return tipo; }

    /**
     * @return Valor apostado (ej: "17", "ROJO", "PAR").
     */
    public String getValor() { return valor; }

    /**
     * @return Cantidad de dinero apostada.
     */
    public double getCantidad() { return cantidad; }

    // --- MÉTODOS DE OBJETO --- 

    /**
     * Devuelve una representación en cadena del objeto Apuesta.
     * Incluye el ID del jugador, tipo, valor y cantidad.
     */
    @Override
    public String toString() {
        return "Apuesta [Jugador=" + jugador.getID() + 
               ", Tipo=" + tipo + 
               ", Valor='" + valor + '\'' + 
               ", Cantidad=" + cantidad + "]";
    }

    /**
     * Compara dos objetos Apuesta basándose en todos sus campos.
     * 
     * @param o Objeto a comparar.
     * @return true si son iguales, false en caso contrario.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Apuesta)) return false;
        Apuesta apuesta = (Apuesta) o;
        return Double.compare(apuesta.cantidad, cantidad) == 0 &&
               Objects.equals(jugador, apuesta.jugador) &&
               tipo == apuesta.tipo &&
               Objects.equals(valor, apuesta.valor);
    }

    /**
     * Devuelve un valor hash consistente con equals().
     * 
     * @return hash calculado en base a todos los campos.
     */
    @Override
    public int hashCode() {
        return Objects.hash(jugador, tipo, valor, cantidad);
    }
}
