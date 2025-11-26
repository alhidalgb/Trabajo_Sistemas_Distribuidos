package servidor;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import logicaRuleta.ServicioRuletaServidor;
import modeloDominio.Casilla;

/**
 * Clase mandarMensaje
 * -------------------
 * Ejecuta una acción sobre la ruleta (mandar casilla o repartir premio) y
 * sincroniza con otros hilos mediante una CyclicBarrier.
 *
 * PRECONDICIONES:
 *  - rule != null
 *  - opcion debe ser 1 (mandarCasilla) o 2 (repartirPremio)
 *  - ganadora != null
 *  - pelotita != null
 *
 * POSTCONDICIONES:
 *  - Se ejecuta la acción indicada.
 *  - El hilo espera en la barrera hasta que todos los participantes lleguen,
 *    o hasta que expire el timeout.
 */
public class mandarMensaje implements Runnable {

    private final int opcion;
    private final ServicioRuletaServidor rule;
    private final Casilla ganadora;
    private final CyclicBarrier pelotita;

    public mandarMensaje(ServicioRuletaServidor rule, int opcion, Casilla ganadora, CyclicBarrier pelotita) {
        this.opcion = opcion;
        this.rule = rule;
        this.ganadora = ganadora;
        this.pelotita = pelotita;
    }

    @Override
    public void run() {
        if (opcion == 0 || this.rule == null || ganadora==null) {
            return;
        }

        try {
            switch (this.opcion) {
                case 1:
                    this.rule.mandarCasilla(ganadora);
                    break;
                case 2:
                    this.rule.repartirPremio(ganadora);
                    break;
                default:
                    // No hacer nada si la opción no es válida
                    return;
            }

            // Esperar en la barrera con timeout para evitar bloqueos indefinidos
            pelotita.await(30, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaurar estado de interrupción
            System.err.println("⚠️ mandarMensaje interrumpido: " + e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.println("⚠️ Barrera rota en mandarMensaje: " + e.getMessage());
        } catch (TimeoutException e) {
            System.err.println("⚠️ Timeout esperando en barrera en mandarMensaje");
        }
    }
}
