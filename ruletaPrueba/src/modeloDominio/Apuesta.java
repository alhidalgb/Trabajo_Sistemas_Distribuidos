package modeloDominio;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase Apuesta
 * -------------
 * Representa una apuesta realizada por un jugador en la ruleta.
 * Contiene el tipo de apuesta, el valor apostado y la cantidad de dinero.
 * Esta clase es inmutable y SERIALIZABLE para poder viajar por la red.
 */
public class Apuesta implements Serializable {

    // Identificador de versión para la serialización (recomendado)
    private static final long serialVersionUID = 1L;

    // --- ATRIBUTOS ---
    private final Jugador jugador; 
    private final TipoApuesta tipo;
    private final String valor;
    private final double cantidad;

    // --- CONSTRUCTOR ---
    /**
     * Constructor que inicializa todos los parámetros de la apuesta.
     * * @param j Jugador que realiza la apuesta (no null).
     * @param t Tipo de apuesta (no null).
     * @param v Valor apostado (no null).
     * @param cantidad Cantidad de dinero apostada (> 0).
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
    public Jugador getJugador() { return jugador; }
    public TipoApuesta getTipo() { return tipo; }
    public String getValor() { return valor; }
    public double getCantidad() { return cantidad; }

    // --- MÉTODOS DE OBJETO --- 

    @Override
    public String toString() {
        return "Apuesta [Jugador=" + jugador.getID() + 
               ", Tipo=" + tipo + 
               ", Valor='" + valor + '\'' + 
               ", Cantidad=" + cantidad + "]";
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(jugador, tipo, valor, cantidad);
    }
}