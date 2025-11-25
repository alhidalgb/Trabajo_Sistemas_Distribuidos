package modeloDominio;

import java.util.Objects;

/**
 * Representa una apuesta realizada por un jugador, conteniendo el tipo de apuesta, 
 * el valor apostado y la cantidad de dinero. 
 * Esta clase es inmutable.
 */
public class Apuesta {

	// Hacemos los campos 'final' para garantizar la inmutabilidad.
	private final Jugador jugador; 
	private final TipoApuesta tipo;
	private final String valor;
	private final double cantidad;
	
	
	/**
	 * Constructor que inicializa todos los parámetros de la apuesta.
	 * PRE: La cantidad debe ser positiva.
	 */
	public Apuesta(Jugador j, TipoApuesta t, String v, double cantidad) {
		this.jugador = j;
		this.tipo = t;
		this.valor = v;
		this.cantidad = cantidad; 
	}
	
	
	// --- GETTERS ---
	
	public Jugador getJugador() {
		return jugador;
	}
	
	public TipoApuesta getTipo() { 
		return tipo;
	}
	
	public String getValor() {
		return valor;
	}
	
	public double getCantidad() {
		return cantidad;
	}
	
	
	// --- MÉTODOS DE OBJETO ---

    /**
     * Devuelve una representación en cadena del objeto Apuesta.
     */
    @Override
    public String toString() {
        return "Apuesta [Jugador=" + jugador.getID() + 
               ", Tipo=" + tipo + 
               ", Valor='" + valor + '\'' + 
               ", Cantidad=" + cantidad + "]";
    }

    /**
     * Comprueba si dos objetos Apuesta son iguales, basándose en todos sus campos.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Apuesta apuesta = (Apuesta) o;
        
        // Se comparan todos los campos finales para la igualdad.
        return Double.compare(apuesta.cantidad, cantidad) == 0 &&
               Objects.equals(jugador, apuesta.jugador) &&
               tipo == apuesta.tipo &&
               Objects.equals(valor, apuesta.valor);
    }

    /**
     * Devuelve un valor hash consistente con el método equals().
     */
    @Override
    public int hashCode() {
        return Objects.hash(jugador, tipo, valor, cantidad);
    }
}