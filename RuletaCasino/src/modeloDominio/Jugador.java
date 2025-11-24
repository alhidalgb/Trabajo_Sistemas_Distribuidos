package modeloDominio;

import java.net.Socket;

public class Jugador {

	private String ID;
	private double saldo;
	
	//Yo los jugadores los voy a guardar como objetos pero no quiero guardar su Socket.
	private transient Socket conexion;
	
	//Es un estado para saber si la sesion esta ya iniciada y no se pueda volver a iniciar sesion desde otro dispositivo.
	private transient boolean isSesionIniciada;
	
	public Jugador() {
		
		this.conexion=null;
		this.ID=null;
		this.saldo=0;
	}
	
	public Jugador(String id, double saldo) {
		
		this.ID=id;
		this.conexion=null;
		this.saldo=saldo;
		
		
	}
	
	public Jugador(String id, double saldo,Socket cliente) {
		
		
		this.conexion=cliente;
		this.ID=id;
		this.saldo=saldo;
		
		
	}
	
	
	public  double getSaldo() {
		
		return saldo;
	}
	
	public void setSaldo(double saldo) {
		
		this.saldo=saldo;
		
	}
	
	public String getID() {
		
		return this.ID;
		
	}
	
	public void setID(String id) {
		
		this.ID=id;
	}
	
	public Socket getConexion() {
		
		return this.conexion;
		
	}
	
	
	public void setConexion(Socket cliente) {
		
		this.conexion=cliente;
		
	}
	
	
	
	public boolean equals(Jugador jug) {
		
		return this.ID.equals(jug.getID());
	
	}
	
	
	public boolean isSesionIniciada() {
		
		return this.isSesionIniciada;
	}
	
	public void setSesionIniciada(boolean is) {
		
		this.isSesionIniciada=is;
	}
	
}
