package servidor.persistencia;

import java.io.File;
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


//Desde esta clase se actuliza la BBDD, pero es llamada por el ServidroPrincipal cada X tiempo y por el ServicioRuleta cuando se desconecta un jugador.
public class ActualizarBD implements Runnable {

    private final List<Jugador> jugadores;
    private final File BBDD;

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
    public ActualizarBD(List<Jugador> jugadores, File BBDD) {
        this.jugadores = jugadores;
        this.BBDD = BBDD;
    }

    @Override
    public void run() {
        try {
        	
        	
        	
        	//MEJORA: este synchronizdd esta bien? o tiene que ir en BDJugadores.
        		synchronized(BBDD) {
        			
        			 BDJugadores.MarshallingJugadores(jugadores, BBDD);
        		}
        		
           
            System.out.println("✅ Base de datos de jugadores actualizada en: " + BBDD.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("⚠️ Error actualizando base de datos de jugadores: " + e.getMessage());
        }
    }
}
