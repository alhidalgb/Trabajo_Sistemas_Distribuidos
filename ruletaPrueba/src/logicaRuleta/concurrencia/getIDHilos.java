package logicaRuleta.concurrencia;

import java.util.List;
import java.util.concurrent.Callable;
import modeloDominio.Jugador;

/**
 * Clase GetIDHilos
 * ----------------
 * Tarea concurrente (Callable) diseñada para buscar un jugador por su ID 
 * dentro de una sublista o fragmento de la lista de sesión.
 * * Se utiliza en ServicioRuleta para paralelizar la búsqueda (Divide y Vencerás).
 *
 * PRECONDICIONES:
 * - La lista de jugadores no debe ser null.
 * - El ID a buscar no debe ser null.
 *
 * POSTCONDICIONES:
 * - Devuelve el objeto Jugador si se encuentra en este fragmento de lista.
 * - Devuelve null si no se encuentra o si el hilo es interrumpido.
 */
public class getIDHilos implements Callable<Jugador> {

    // --- ATRIBUTOS ---
    private final List<Jugador> jugadoresSesion;
    private final String id;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea de búsqueda.
     *
     * @param jugadores Sublista o fragmento de jugadores donde buscar.
     * @param id        Identificador del jugador objetivo.
     */
    public getIDHilos(List<Jugador> jugadores, String id) {
        this.jugadoresSesion = jugadores;
        this.id = id;
    }

    // --- LÓGICA DE NEGOCIO ---
    @Override
    public Jugador call() {
        // Usamos for-each para mayor limpieza y legibilidad
        for (Jugador j : this.jugadoresSesion) {
            
            // Comprobación de seguridad: 
            // Permite que 'cancelarFuture' detenga este hilo si otro hilo ya encontró al jugador.
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            
            // Comparación de ID (Case-sensitive)
            if (id.equals(j.getID())) {
                return j;
            }
        }
        
        // No encontrado en este fragmento
        return null;
    }
}