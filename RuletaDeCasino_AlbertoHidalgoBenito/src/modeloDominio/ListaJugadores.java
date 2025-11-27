package modeloDominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.*;

/**
 * Clase ListaJugadores
 * --------------------
 * Representa una colección de jugadores, utilizada como raíz en la persistencia XML.
 * Compatible con JAXB para serialización y deserialización.
 *
 * PRECONDICIONES:
 *  - La lista puede estar vacía, pero nunca debe ser nula.
 *
 * POSTCONDICIONES:
 *  - Se obtiene un objeto contenedor de jugadores.
 *  - JAXB serializa la lista bajo la etiqueta raíz <jugadores> y cada jugador bajo <jugador>.
 */
@XmlRootElement(name = "jugadores")
public class ListaJugadores {

    // --- ATRIBUTOS ---
    private List<Jugador> lista = new ArrayList<>();

    // --- CONSTRUCTORES ---
    /**
     * Constructor por defecto requerido por JAXB.
     * Inicializa la lista como vacía.
     */
    public ListaJugadores() {
        this.lista = new ArrayList<>();
    }

    /**
     * Constructor que inicializa la lista con jugadores existentes.
     *
     * @param lista Lista de jugadores (no nula).
     */
    public ListaJugadores(List<Jugador> lista) {
        this.lista = (lista != null) ? lista : new ArrayList<>();
    }

    // --- GETTERS Y SETTERS ---
    /**
     * Devuelve la lista de jugadores.
     *
     * @return Lista de jugadores.
     */
    @XmlElement(name = "jugador")
    public List<Jugador> getLista() { 
        return lista; 
    }

    /**
     * Establece la lista de jugadores.
     *
     * @param lista Nueva lista de jugadores (no nula).
     */
    public void setLista(List<Jugador> lista) { 
        this.lista = (lista != null) ? lista : new ArrayList<>(); 
    }

    // --- MÉTODOS DE OBJETO ---
    /**
     * Devuelve una representación en cadena de la lista de jugadores.
     *
     * @return Cadena con los jugadores contenidos.
     */
    @Override
    public String toString() {
        return "ListaJugadores [lista=" + lista + "]";
    }

    /**
     * Comprueba si dos listas de jugadores son iguales.
     *
     * @param obj Objeto a comparar.
     * @return true si las listas son iguales, false en caso contrario.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ListaJugadores)) return false;
        ListaJugadores other = (ListaJugadores) obj;
        return Objects.equals(lista, other.lista);
    }

    /**
     * Devuelve un valor hash consistente con equals().
     *
     * @return hash calculado en base a la lista de jugadores.
     */
    @Override
    public int hashCode() {
        return Objects.hash(lista);
    }
}
