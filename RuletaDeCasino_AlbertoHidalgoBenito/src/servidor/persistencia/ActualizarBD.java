package servidor.persistencia;

import java.util.List;
import modeloDominio.Jugador;

/**
 * Clase ActualizarBD
 * ------------------
 * Tarea Runnable que se encarga de persistir la lista de jugadores en la base de datos XML.
 * Se puede programar periódicamente con un ExecutorService o invocar manualmente.
 *
 * PRECONDICIONES:
 *  - La lista de jugadores debe estar inicializada (no null).
 *  - La ruta bd debe ser válida y accesible para escritura.
 *
 * POSTCONDICIONES:
 *  - El fichero XML indicado por bd contendrá la lista actualizada de jugadores.
 *  - Si ocurre un error en el marshalling, se informa por consola pero no se interrumpe el hilo.
 */
public class ActualizarBD implements Runnable {

    private final List<Jugador> jugadores;
    private final String bd;

    /**
     * Constructor de ActualizarBD.
     *
     * PRECONDICIONES:
     *  - jugadores != null
     *  - bd != null y no vacío
     *
     * POSTCONDICIONES:
     *  - Se crea una tarea lista para ejecutar el guardado de jugadores.
     */
    public ActualizarBD(List<Jugador> jugadores, String bd) {
        this.jugadores = jugadores;
        this.bd = bd;
    }

    @Override
    public void run() {
        try {
            BDJugadores.MarshallingJugadores(jugadores, bd);
            System.out.println("✅ Base de datos de jugadores actualizada en: " + bd);
        } catch (Exception e) {
            System.err.println("⚠️ Error actualizando base de datos de jugadores: " + e.getMessage());
        }
    }
}
