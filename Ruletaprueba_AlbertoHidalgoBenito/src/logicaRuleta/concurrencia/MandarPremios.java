package logicaRuleta.concurrencia;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import logicaRuleta.core.RuletaUtils;
import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

/**
 * Clase MandarPremios
 * -------------------
 * Tarea Runnable que calcula las ganancias de un jugador en una ronda y las comunica.
 * Utiliza una CyclicBarrier para sincronizar el reparto de premios entre varios jugadores.
 *
 * PRECONDICIONES:
 *  - El jugador debe estar inicializado y tener conexión activa (puede ser null si se cayó).
 *  - La lista de apuestas debe estar inicializada (no null).
 *  - La casilla ganadora debe estar correctamente creada (número válido entre 0 y 36).
 *  - La CyclicBarrier debe estar inicializada para coordinar los hilos.
 *  - El ServicioRuletaServidor debe estar disponible para calcular premios.
 *
 * POSTCONDICIONES:
 *  - Se calcula la ganancia total del jugador en la ronda.
 *  - Se suma la ganancia al saldo del jugador.
 *  - Si la conexión está activa, se envía un mensaje al cliente con la ganancia.
 *  - El hilo espera en la barrera para sincronizarse con los demás jugadores.
 */
public class MandarPremios implements Runnable {

    // --- ATRIBUTOS ---
    private final List<Apuesta> listApuesta;
    private final Casilla ganadora;
    private final CyclicBarrier starter;
    private final Jugador jugador;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea con el jugador, sus apuestas, la casilla ganadora y la barrera de sincronización.
     *
     * @param jug         Jugador al que se le reparten premios.
     * @param listApuesta Lista de apuestas del jugador.
     * @param ganadora    Casilla ganadora de la ronda.
     * @param starter     Barrera de sincronización entre hilos.
     */
    public MandarPremios(Jugador jug, List<Apuesta> listApuesta, Casilla ganadora, CyclicBarrier starter) {
        this.ganadora = ganadora;
        this.listApuesta = listApuesta;
        this.starter = starter;
        this.jugador = jug;
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Ejecuta la tarea: calcula la ganancia del jugador, espera en la barrera y envía el resultado.
     * Si el jugador está desconectado, se actualiza el saldo pero no se envía el mensaje.
     */
    @Override
    public void run() {
        double ganancia = 0.0;
        PrintWriter os = null;

        // 1. Calcular las ganancias de todas las apuestas
        for (Apuesta ap : listApuesta) {
            ganancia += RuletaUtils.calcularPremio(ganadora, ap);
        }

        // 2. Intentar establecer conexión con el jugador
        try {
            if (jugador.getConexion() != null && !jugador.getConexion().isClosed()) {
                os = new PrintWriter(new OutputStreamWriter(jugador.getConexion().getOutputStream()), true);
            }
        } catch (IOException e) {
            // Si se ha caído la conexión, no enviamos mensaje pero seguimos
            os = null;
        }

        // 3. Sincronizar con otros hilos mediante la barrera
        try {
            starter.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt(); // restaurar flag si fue interrupción
        }

        // 4. Actualizar saldo y mandar mensaje al jugador
        jugador.sumarGanancia(ganancia);
        if (os != null) {
            os.println("\u001b[1m\u001b[33mHAS GANADO: " + ganancia + "€\u001b[0m");
        }
    }
}
