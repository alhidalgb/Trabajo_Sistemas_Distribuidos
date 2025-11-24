package modeloDominio;

import java.io.Serializable;
import java.net.Socket;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "jugador") // Define la etiqueta para cada item
public class Jugador {

    private String id;
    private double saldo; 
    
    private Socket conexion;

    
    //Es un estado para saber si la sesion esta ya iniciada y no se pueda volver a iniciar sesion desde otro dispositivo.
  	private boolean isSesionIniciada;
    
    // 1. Constructor vacÃ­o 
    public Jugador() {
		
		this.conexion=null;
		this.id=null;
		this.saldo=0;
		this.isSesionIniciada=false;

	}
	
	public Jugador(String id, double saldo) {
		
		this.id=id;
		this.conexion=null;
		this.saldo=saldo;
		this.isSesionIniciada=false;

	}
	
	public Jugador(String id, double saldo,Socket cliente) {
		
		
		this.conexion=cliente;
		this.id=id;
		this.saldo=saldo;
		this.isSesionIniciada=false;

		
	}

    // Getters y Setters
    @XmlAttribute(name = "id") 
    public String getID() { return id; }
    public void setId(String id) { this.id = id; }
    
    @XmlElement(name = "saldo")
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    
    @XmlTransient
    public Socket getConexion() {return conexion;}
    public void setConexion(Socket conex) {this.conexion=conex;}
    
    @XmlTransient
    public boolean isSesionIniciada() {return this.isSesionIniciada;}
	public void setSesionIniciada(boolean is) {this.isSesionIniciada=is;}
    
    
    // Equals para buscarlo fÃ¡cilmente en la lista
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Jugador other = (Jugador) obj;
        return id != null && id.equals(other.id);
    }
    
    @Override
    public String toString() {
        return "Jugador [id=" + id + ", saldo=" + saldo + "¿Ha iniciado sesion?="+this.isSesionIniciada+ "]";
    }
    
}