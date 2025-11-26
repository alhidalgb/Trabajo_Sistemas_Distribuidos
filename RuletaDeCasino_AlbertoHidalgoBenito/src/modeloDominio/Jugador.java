package modeloDominio;

import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;
import javax.xml.bind.annotation.*;

/**
 * Representa a un jugador del casino. Almacena su identificación, saldo, estado de la conexión 
 * y su estado de sesión. La clase es serializable y compatible con JAXB para XML.
 */
@XmlRootElement(name = "jugador")
public class Jugador implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Campos de estado (persistencia)
    private String id;
    private double saldo;
    
    // Campos transitorios (no persisten en JAXB ni Serialización estándar)
    private transient Socket conexion;
    private transient boolean isSesionIniciada;

    // --- CONSTRUCTORES ---

    /**
     * Constructor por defecto (necesario para JAXB y serialización). Inicializa el estado a valores seguros.
     */
    public Jugador() {
        this.id = null;
        this.saldo = 0.0;
        this.conexion = null;
        this.isSesionIniciada = false;
    }
    
    /**
     * Constructor usado para inicializar un jugador con ID y saldo, sin conexión activa.
     */
    public Jugador(String id, double saldo) {
        this.id = id;
        this.saldo = saldo;
        this.conexion = null;
        this.isSesionIniciada = false;
    }
    
    /**
     * Constructor completo para inicializar un jugador con ID, saldo y conexión activa.
     */
    public Jugador(String id, double saldo, Socket cliente) {
        this.id = id;
        this.saldo = saldo;
        this.conexion = cliente;
        this.isSesionIniciada = false;
    }

    // --- LÓGICA DE NEGOCIO ---

    
    /**
     * Suma la ganancia al saldo actual de forma segura para hilos.
     */
    public synchronized void sumarGanancia(double ganancia) {
        // La operación es atómica gracias al método setSaldo.
        this.setSaldo(this.saldo + ganancia);
    }
    
    /**
     * Resta el monto de la apuesta del saldo actual de forma segura para hilos.
     */
    public synchronized void restarApuesta(double apuesta) {
        // La operación es atómica gracias al método setSaldo.
        this.setSaldo(this.saldo - apuesta);
    }

    // --- GETTERS Y SETTERS (JAXB MAPPINGS) ---
    
    @XmlAttribute(name = "id")
    public String getID() { return id; }
    public void setID(String id) { this.id = id; }
    
    @XmlElement(name = "saldo")
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    
    // --- GETTERS Y SETTERS (TRANSITORIOS / CONEXIÓN) ---

    // Los campos transitorios se excluyen de la persistencia JAXB
    @XmlTransient
    public Socket getConexion() { return conexion; }
    public void setConexion(Socket conex) { this.conexion = conex; }
    
    @XmlTransient
    public boolean isSesionIniciada() { return this.isSesionIniciada; }
    public void setSesionIniciada(boolean sesionIniciada) { this.isSesionIniciada = sesionIniciada; }
    
    // --- MÉTODOS DE OBJETO (UTILIDADES) ---

    /**
     * Dos jugadores son iguales si sus IDs coinciden.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Jugador other = (Jugador) obj;
        
        // Uso de Objects.equals para manejar correctamente los valores nulos.
        return Objects.equals(id, other.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return "Jugador [id=" + id + ", saldo=" + saldo + ", Sesión Iniciada=" + this.isSesionIniciada + "]";
    }
}