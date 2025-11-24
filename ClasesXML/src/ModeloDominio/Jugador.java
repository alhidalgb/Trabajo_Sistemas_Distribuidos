package ModeloDominio;

import java.io.Serializable;
import java.net.Socket;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "jugador") // Define la etiqueta para cada item
public class Jugador implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private double saldo; 
    
    private transient Socket conexion;

    
    //Es un estado para saber si la sesion esta ya iniciada y no se pueda volver a iniciar sesion desde otro dispositivo.
  	private transient boolean isSesionIniciada;
    
    // 1. Constructor vacío 
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
		
	}
	
	public Jugador(String id, double saldo,Socket cliente) {
		
		
		this.conexion=cliente;
		this.id=id;
		this.saldo=saldo;
		
		
	}

    // Getters y Setters
    @XmlAttribute 
    public String getID() { return id; }
    public void setId(String id) { this.id = id; }
    
    @XmlElement
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    
    @XmlTransient
    public Socket getConexion() {return conexion;}
    public void setConexion(Socket conex) {this.conexion=conex;}
    
    @XmlTransient
    public boolean isSesionIniciada() {return this.isSesionIniciada;}
	public void setSesionIniciada(boolean is) {this.isSesionIniciada=is;}
    
    
    // Equals para buscarlo fácilmente en la lista
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Jugador other = (Jugador) obj;
        return id != null && id.equals(other.id);
    }
}