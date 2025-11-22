package ModeloDominio;

import java.io.Serializable;

public class Apuesta implements Serializable {

	private static final long serialVersionUID = 1L; // Recomendado para Serializable

	private Jugador jugador; 
	private TipoApuesta tipo;
	private String valor;
	private double cantidad;
	
	// 1. Constructor Vacío (OBLIGATORIO para JAXB)
	public Apuesta() {
	}
	
	// 2. Constructor con parámetros
	public Apuesta(Jugador j, TipoApuesta t, String v, double cantidad) {
		this.jugador = j;
		this.tipo = t;
		this.valor = v;
		this.cantidad = cantidad; 
	}
	
	// --- GETTERS Y SETTERS ---

	public Jugador getJugador() {
		return jugador;
	}

	public void setJugador(Jugador jugador) {
		this.jugador = jugador;
	}

	public TipoApuesta getTipo() { 
		return tipo;
	}

	public void setTipo(TipoApuesta tipo) {
		this.tipo = tipo;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public double getCantidad() {
		return cantidad;
	}

	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
}