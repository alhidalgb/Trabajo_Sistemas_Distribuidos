package logicaRuleta.concurrencia;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import modeloDominio.Casilla;

/**
 * Clase MandarCasillaGanadora
 * ---------------------------
 * Tarea Runnable que comunica al jugador la casilla ganadora al final de la ronda.
 * Utiliza una CyclicBarrier para sincronizar el envío con otros jugadores.
 *
 * PRECONDICIONES:
 *  - El socket del jugador debe estar activo (no cerrado).
 *  - La casilla ganadora debe estar correctamente creada.
 *  - La CyclicBarrier debe estar inicializada y compartida entre los hilos.
 *
 * POSTCONDICIONES:
 *  - El jugador recibe un mensaje con la casilla ganadora.
 *  - Si el socket está cerrado o hay error, el mensaje no se envía pero no se interrumpe la ronda.
 */
public class MandarCasillaGanadora implements Runnable {

    // --- ATRIBUTOS ---
    private final Socket jug;
    private final CyclicBarrier starter;
    private final Casilla ganadora;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea con el socket del jugador, la barrera de sincronización y la casilla ganadora.
     *
     * @param s        Socket del jugador.
     * @param starter  Barrera de sincronización.
     * @param ganadora Casilla ganadora de la ronda.
     */
    public MandarCasillaGanadora(Socket s, CyclicBarrier starter, Casilla ganadora) {
        this.jug = s;
        this.starter = starter;
        this.ganadora = ganadora;
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Ejecuta la tarea: espera en la barrera y luego envía la casilla ganadora al jugador.
     * Si hay error de conexión o interrupción, el mensaje no se envía pero el hilo continúa.
     */
    @Override
    public void run() {
        PrintWriter os = null;

        try {
            if (jug != null && !jug.isClosed()) {
                os = new PrintWriter(new OutputStreamWriter(jug.getOutputStream()), true);
            }

            // Esperamos a que todos los hilos lleguen a la barrera
            starter.await();

        } catch (IOException | InterruptedException | BrokenBarrierException e) {
            // Silencioso: si el jugador se desconectó o la barrera se rompió, no interrumpimos la ronda
        } finally {
            // Enviamos el mensaje si la conexión estaba activa
            if (os != null) {
                os.println("\u001b[32m--- NO VA MÁS ---\u001b[0m");
                os.println("\u001b[32mCASILLA GANADORA: " + ganadora + "\u001b[0m");
            }
        }
    }
}
