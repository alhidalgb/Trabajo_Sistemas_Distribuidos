package modeloDominio;

import java.io.Serializable;
import java.util.Objects;


/**
 * Clase Apuesta
 * -------------
 * Clase inmutable que representa una transacción de juego en la ruleta.
 * Encapsula quién apuesta, qué tipo de apuesta realiza y cuánto dinero arriesga.
 * * Implementa Serializable para poder ser transmitida por red (Cliente -> Servidor).
 */
public class Apuesta implements Serializable {

    // Identificador de versión para la serialización (Buena práctica para evitar errores de versión)
    private static final long serialVersionUID = 1L;

    // --- ATRIBUTOS ---
    // Son final para garantizar la inmutabilidad del objeto una vez creado
    private final Jugador jugador; 
    private final TipoApuesta tipo;
    private final String valor;     // Ej: "ROJO", "14", "PAR"
    private final double cantidad;

    // --- CONSTRUCTOR ---
    /**
     * Crea una nueva apuesta validando los datos de entrada.
     *
     * @param j        Jugador que realiza la apuesta (No puede ser null).
     * @param t        Tipo de apuesta (NUMERO, COLOR, etc. - No puede ser null).
     * @param v        Valor específico apostado (Ej: "14", "ROJO" - No puede ser null).
     * @param cantidad Dinero a apostar (Debe ser mayor que 0).
     * @throws IllegalArgumentException Si algún parámetro es nulo o la cantidad es <= 0.
     */
    public Apuesta(Jugador j, TipoApuesta t, String v, double cantidad) {
        if (j == null) throw new IllegalArgumentException("El jugador no puede ser nulo");
        if (t == null) throw new IllegalArgumentException("El tipo de apuesta no puede ser nulo");
        if (v == null) throw new IllegalArgumentException("El valor apostado no puede ser nulo");
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        
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

    // --- MÉTODOS DE OBJETO (Overrides) --- 
    
    // toString: Útil para logs y depuración en consola
    @Override
    public String toString() {
        return "Apuesta [Jugador=" + jugador.getID() + 
               ", Tipo=" + tipo + 
               ", Valor='" + valor + '\'' + 
               ", Cantidad=" + cantidad + "€]";
    }

    // equals y hashCode: Necesarios si guardamos apuestas en Sets o las comparamos en tests
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