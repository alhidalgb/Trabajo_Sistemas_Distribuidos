package modeloDominio;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;
import javax.xml.bind.annotation.*;

/**
 * Clase Jugador
 * -------------
 * Representa a un jugador del casino. 
 * Almacena su identificación, saldo, estado de la conexión y su estado de sesión.
 * La clase es serializable y compatible con JAXB para persistencia en XML.
 *
 * PRECONDICIONES:
 *  - El ID del jugador debe ser único y no nulo.
 *  - El saldo inicial debe ser >= 0.
 *  - La conexión (Socket) puede ser nula si el jugador no está conectado.
 *
 * POSTCONDICIONES:
 *  - Se crea un objeto Jugador con ID, saldo y estado de sesión inicializado.
 *  - Los métodos sincronizados garantizan operaciones seguras sobre el saldo en entornos concurrentes.
 *  - equals() y hashCode() permiten comparar jugadores por su ID.
 *  - toString() devuelve una representación legible del jugador.
 */
@XmlRootElement(name = "jugador")
public class Jugador implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- ATRIBUTOS (Persistencia) ---
    private String id;
    private double saldo;

    // --- ATRIBUTOS TRANSITORIOS (no persisten en JAXB ni serialización estándar) ---
    private transient ObjectOutputStream conexionOut;
    private transient boolean isSesionIniciada;

    // --- CONSTRUCTORES ---

    /**
     * Constructor por defecto (necesario para JAXB y serialización).
     * Inicializa el estado a valores seguros.
     */
    public Jugador() {
        this.id = null;
        this.saldo = 0.0;
        this.conexionOut = null;
        this.isSesionIniciada = false;
    }

    /**
     * Constructor usado para inicializar un jugador con ID y saldo, sin conexión activa.
     *
     * @param id    Identificador único del jugador.
     * @param saldo Saldo inicial del jugador (>= 0).
     */
    public Jugador(String id, double saldo) {
        this.id = id;
        this.saldo = saldo;
        this.conexionOut = null;
        this.isSesionIniciada = false;
    }

    /**
     * Constructor completo para inicializar un jugador con ID, saldo y conexión activa.
     *
     * @param id      Identificador único del jugador.
     * @param saldo   Saldo inicial del jugador (>= 0).
     * @param cliente Socket de conexión activo.
     */
    public Jugador(String id, double saldo, ObjectOutputStream cliente) {
        this.id = id;
        this.saldo = saldo;
        this.conexionOut = cliente;
        this.isSesionIniciada = false;
    }

    // --- LÓGICA DE NEGOCIO ---

    /**
     * Suma la ganancia al saldo actual de forma segura para hilos.
     *
     * @param ganancia Monto a añadir al saldo.
     * POST: El saldo se incrementa en la cantidad indicada.
     */
    public synchronized void sumaRestaSaldo(double saldo) {
        this.setSaldo(this.saldo + saldo);
    }

  

    // --- GETTERS Y SETTERS (Persistencia / JAXB) ---

    @XmlAttribute(name = "id", required = true)
    public String getID() { return id; }
    public void setID(String id) { this.id = id; }

    @XmlElement(name = "saldo", required = true)
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    // --- GETTERS Y SETTERS (Transitorios / Conexión) ---

    @XmlTransient
    public ObjectOutputStream getOutputStream() { return conexionOut; }
    public void setOutputStream(ObjectOutputStream conex) { this.conexionOut = conex; }

    @XmlTransient
    public boolean isSesionIniciada() { return this.isSesionIniciada; }
    public void setSesionIniciada(boolean sesionIniciada) { this.isSesionIniciada = sesionIniciada; }

    // --- MÉTODOS DE OBJETO ---

    /**
     * Dos jugadores son iguales si sus IDs coinciden.
     *
     * @param obj Objeto a comparar.
     * @return true si los IDs son iguales, false en caso contrario.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Jugador)) return false;
        Jugador other = (Jugador) obj;
        return Objects.equals(id, other.id);
    }

    /**
     * Devuelve un valor hash consistente con equals().
     *
     * @return hash calculado en base al ID del jugador.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    /**
     * Devuelve una representación en cadena del objeto Jugador.
     *
     * @return Cadena con ID, saldo y estado de sesión.
     */
    @Override
    public String toString() {
        return "Jugador [id=" + id + ", saldo=" + saldo + ", Sesión Iniciada=" + this.isSesionIniciada + "]";
    }

	
}
