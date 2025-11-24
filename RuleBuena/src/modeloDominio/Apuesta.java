package modeloDominio;

import java.io.Reader;
import java.io.Serializable;

public class Apuesta  {

	

	private Jugador jugador; 
	private TipoApuesta tipo;
	private String valor;
	private double cantidad;
	
	
	
	// 2. Constructor con parámetros
	public Apuesta(Jugador j, TipoApuesta t, String v, double cantidad) {
		this.jugador = j;
		this.tipo = t;
		this.valor = v;
		this.cantidad = cantidad; 
	}
	
	
	//--- GETTERS ---
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
	
	
	// --- SETTERS ---

	//Una vez creada esta clase es inmutable.
	
	/*

	public void setJugador(Jugador jugador) {
		this.jugador = jugador;
	}

	

	public void setTipo(TipoApuesta tipo) {
		this.tipo = tipo;
	}

	

	public void setValor(String valor) {
		this.valor = valor;
	}

	

	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	
	*/
}