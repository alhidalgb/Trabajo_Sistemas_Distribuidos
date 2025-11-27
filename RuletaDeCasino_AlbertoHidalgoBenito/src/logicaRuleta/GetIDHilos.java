package logicaRuleta;

import java.util.List;
import java.util.concurrent.Callable;
import modeloDominio.Jugador;

/**
 * Clase GetIDHilos
 * ----------------
 * Tarea concurrente que busca un jugador en una lista de sesión, 
 * dentro de un rango de índices específico, comparando por su ID.
 * 
 * Se utiliza en escenarios donde la búsqueda se divide en varios hilos 
 * para mejorar el rendimiento en listas grandes.
 *
 * PRECONDICIONES:
 *  - jugadoresSesion != null
 *  - inicio >= 0
 *  - fin <= jugadoresSesion.size()
 *  - id != null
 *
 * POSTCONDICIONES:
 *  - Devuelve el jugador cuyo ID coincide con el buscado, si se encuentra en el rango.
 *  - Si no se encuentra o el hilo es interrumpido, devuelve null.
 */
public class GetIDHilos implements Callable<Jugador> {

    // --- ATRIBUTOS ---
    private final int inicio;
    private final int fin;
    private final List<Jugador> jugadoresSesion;
    private final String id;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea de búsqueda de jugador por ID en un rango de índices.
     *
     * @param jugadores Lista de jugadores en sesión.
     * @param inicio    Índice inicial de búsqueda (inclusive).
     * @param fin       Índice final de búsqueda (exclusive).
     * @param id        Identificador del jugador a buscar.
     */
    public GetIDHilos(List<Jugador> jugadores, int inicio, int fin, String id) {
        this.jugadoresSesion = jugadores;
        this.inicio = inicio;
        this.fin = fin;
        this.id = id;
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Busca un jugador en la lista dentro del rango indicado.
     * 
     * PRE: La lista y el ID no deben ser nulos.
     * POST:
     *  - Devuelve el jugador si se encuentra en el rango.
     *  - Devuelve null si no se encuentra o si el hilo es interrumpido.
     */
    @Override
    public Jugador call() {
        for (int i = inicio; i < fin; i++) {
        	
            // Comprobamos interrupción dentro del bucle, tenemos que hacer esta comprobacion por si se ha decidido cerrar el hilo antes de tiempo. Por ejemplo, con "cancelarFuture".
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Jugador j = jugadoresSesion.get(i);
            if (id.equals(j.getID())) {
                return j;
            }
        }
        return null;
    }
}
