package servidor;

import java.util.List;
import java.util.Map;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

/**
 * Clase guardarApuestas
 * ---------------------
 * Tarea Runnable que persiste en el historial XML las apuestas realizadas por los jugadores
 * y el resultado de la casilla ganadora.
 *
 * PRECONDICIONES:
 *  - El mapa de apuestas debe estar inicializado y no ser null.
 *  - La casilla ganadora debe estar correctamente creada (número válido entre 0 y 36).
 *  - El objeto XMLServidor debe estar inicializado y apuntar a un fichero XML válido.
 *
 * POSTCONDICIONES:
 *  - Se añade un nuevo bloque <listapuestas> al historial XML con:
 *      - Fecha de la ronda.
 *      - Número ganador.
 *      - Jugadores y sus apuestas.
 *  - Si ocurre un error, se informa por consola y el hilo continúa sin bloquear el servidor.
 */
public class guardarApuestas implements Runnable {

    private final Map<Jugador, List<Apuesta>> apuestas;
    private final Casilla ganadora;
    private final XMLServidor xml;

    /**
     * Constructor de guardarApuestas.
     *
     * PRECONDICIONES:
     *  - apuestas != null
     *  - ganadora != null
     *  - xml != null
     *
     * POSTCONDICIONES:
     *  - Se crea una tarea lista para persistir las apuestas en el historial.
     */
    public guardarApuestas(Map<Jugador, List<Apuesta>> apuestas, Casilla ganadora, XMLServidor xml) {
        this.apuestas = apuestas;
        this.ganadora = ganadora;
        this.xml = xml;
    }

    @Override
    public void run() {
        try {
            // Persistir apuestas y resultado en XML
            xml.guardarJugadorApuesta(apuestas, ganadora);
            //System.out.println("✅ Apuestas guardadas en historial con número ganador: " + ganadora.getNumero());
        } catch (Exception e) {
            System.err.println("⚠️ Error inesperado en guardarApuestas: " + e.getMessage());
        } finally {
            // Restaurar estado de interrupción si el hilo fue interrumpido
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
