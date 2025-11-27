package logicaRuleta;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

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
 *
 * POSTCONDICIONES:
 *  - Se calcula la ganancia total del jugador en la ronda.
 *  - Se suma la ganancia al saldo del jugador.
 *  - Si la conexión está activa, se envía un mensaje al cliente con la ganancia.
 *  - El hilo espera en la barrera para sincronizarse con los demás jugadores.
 */
public class MandarPremios implements Runnable {

    private final List<Apuesta> listApuesta;
    private final Casilla ganadora;
    private final CyclicBarrier starter;
    private final Jugador jugador;

    /**
     * Constructor de MandarPremios.
     *
     * PRECONDICIONES:
     *  - jug != null
     *  - listApuesta != null
     *  - ganadora != null
     *  - starter != null
     *
     * POSTCONDICIONES:
     *  - Se crea una tarea lista para calcular y mandar premios a un jugador.
     */
    public MandarPremios(Jugador jug, List<Apuesta> listApuesta, Casilla ganadora, CyclicBarrier starter) {
        this.ganadora = ganadora;
        this.listApuesta = listApuesta;
        this.starter = starter;
        this.jugador = jug;
    }

    @Override
    public void run() {
        double ganancia = 0.0;
        PrintWriter os = null;

        // 1. Calcular las ganancias de todas las apuestas
        for (Apuesta ap : this.listApuesta) {
            ganancia += this.calcularPremio(ganadora, ap);
        }

        // 2. Intentar establecer conexión con el jugador
        try {
            os = new PrintWriter(new OutputStreamWriter(this.jugador.getConexion().getOutputStream()), true);
        } catch (IOException e) {
            // Si se ha caído la conexión, aun así se guarda la ganancia en el saldo
            os = null;
        }

        
        // 3. Sincronizar con otros hilos mediante la barrera
        try {
            starter.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            // Si falla la barrera, continuamos igualmente
        } finally {
            // 4. Actualizar saldo y mandar mensaje al jugador
            jugador.sumarGanancia(ganancia);
            if (os != null) {
                os.println("\u001b[1m\u001b[33mHAS GANADO: " + ganancia + "€\u001b[0m");
            }
        }
    }

    /**
     * Calcula el premio de una apuesta concreta según la casilla ganadora.
     *
     * PRECONDICIONES:
     *  - La apuesta debe estar inicializada y tener tipo y valor válidos.
     *  - La casilla ganadora debe estar correctamente creada.
     *
     * POSTCONDICIONES:
     *  - Devuelve el importe ganado por la apuesta (0 si perdió).
     */
    public double calcularPremio(Casilla ganadora, Apuesta apuesta) {
        double cantidad = apuesta.getCantidad();
        String valorApostado = apuesta.getValor();

        switch (apuesta.getTipo()) {
            case NUMERO:
                try {
                    int numeroApostado = Integer.parseInt(valorApostado);
                    if (numeroApostado == ganadora.getNumero()) {
                        return cantidad * 36;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Error formato número: " + valorApostado);
                }
                break;

            case COLOR:
                if (ganadora.getNumero() != 0 &&
                    valorApostado.equalsIgnoreCase(ganadora.getColor())) {
                    return cantidad * 2;
                }
                break;

            case PAR_IMPAR:
                if (ganadora.getNumero() != 0) {
                    boolean apostoPar = valorApostado.equalsIgnoreCase("PAR");
                    if ((apostoPar && ganadora.getNumero() % 2 == 0) ||
                        (!apostoPar && ganadora.getNumero() % 2 != 0)) {
                        return cantidad * 2;
                    }
                }
                break;

            case DOCENA:
                try {
                    int docenaApostada = Integer.parseInt(valorApostado);
                    if (docenaApostada == ganadora.getDocena()) {
                        return cantidad * 3;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Error formato docena: " + valorApostado);
                }
                break;

            default:
                return 0;
        }

        return 0;
    }
}
