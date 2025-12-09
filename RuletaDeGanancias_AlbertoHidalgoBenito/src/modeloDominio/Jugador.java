package modeloDominio;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.*;

/**
 * Clase Jugador
 * -------------
 * Entidad principal que representa al usuario en el sistema.
 * Es híbrida: sirve como DTO para la red (Serializable) y como entidad de persistencia (JAXB/XML).
 *
 * PRECONDICIONES:
 * - El ID no debe ser nulo.
 * - El saldo no debe ser negativo.
 */
@XmlRootElement(name = "jugador")
public class Jugador implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- ATRIBUTOS PERSISTENTES ---
    private String id;
    private double saldo;

    // --- ATRIBUTOS TRANSITORIOS (No se guardan en XML ni viajan por red) ---
    // 'transient' de Java evita serialización binaria (Red).
    // '@XmlTransient' en los getters evita serialización XML (Disco).
    private transient ObjectOutputStream conexionOut;
    private transient boolean isSesionIniciada;

    // --- CONSTRUCTORES ---

    /**
     * Constructor vacío requerido por JAXB y Serialización.
     */
    public Jugador() {
        this.id = "";
        this.saldo = 0.0;
        this.conexionOut = null;
        this.isSesionIniciada = false;
    }

    /**
     * Constructor para nuevos registros o carga de datos.
     * @param id Identificador del usuario.
     * @param saldo Saldo inicial.
     */
    public Jugador(String id, double saldo) {
        this.id = id;
        this.saldo = saldo;
        this.conexionOut = null;
        this.isSesionIniciada = false;
    }

    /**
     * Constructor completo (útil para pruebas o reconexiones).
     */
    public Jugador(String id, double saldo, ObjectOutputStream cliente) {
        this(id, saldo);
        this.conexionOut = cliente;
    }

    // --- LÓGICA DE NEGOCIO ---

    /**
     * Modifica el saldo del jugador de forma sincronizada (Thread-Safe).
     * @param cantidad Cantidad a sumar (positiva) o restar (negativa).
     */
    public synchronized void sumaRestaSaldo(double cantidad) {
        this.saldo += cantidad;
    }

    // --- GETTERS Y SETTERS (PERSISTENCIA) ---

    @XmlAttribute(name = "id", required = true)
    public String getID() { return id; }
    public void setID(String id) { this.id = id; }

    @XmlElement(name = "saldo", required = true)
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    // --- GETTERS Y SETTERS (TRANSITORIOS / CONEXIÓN) ---

    @XmlTransient
    public ObjectOutputStream getOutputStream() { return conexionOut; }
    public void setOutputStream(ObjectOutputStream conex) { this.conexionOut = conex; }

    @XmlTransient
    public boolean isSesionIniciada() { return this.isSesionIniciada; }
    public void setSesionIniciada(boolean sesionIniciada) { this.isSesionIniciada = sesionIniciada; }

    // --- MÉTODOS DE OBJETO ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Jugador)) return false;
        Jugador other = (Jugador) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return "Jugador [ID=" + id + ", Saldo=" + saldo + "]";
    }
}