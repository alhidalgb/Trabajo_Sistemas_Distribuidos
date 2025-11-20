package Principal;

import java.net.Socket;

public class Jugador {

	private String ID;
	private double saldo;
	
	//Yo los jugadores los voy a guardar como objetos pero no quiero guardar su Socket.
	private transient Socket conexion;
	
	
	public Jugador(String id, double saldo,Socket cliente) {
		
		
		this.conexion=cliente;
		
		this.ID=id;
		this.saldo=saldo;
		
		
	}
	
	
	public  double getSaldo() {
		
		return saldo;
	}
	
	public String getID() {
		
		return this.ID;
		
	}
	
	public Socket getConexion() {
		
		return this.conexion;
		
	}
	
	public boolean equals(Jugador jug) {
		
		return this.ID.equals(jug.getID());
	
	}
	
	
}
