package logicaRuleta.concurrencia;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
 * Tarea (Worker) encargada de calcular y notificar los resultados de la ronda a un jugador específico.
 * Utiliza una barrera (CyclicBarrier) para asegurar que el cálculo termine antes de notificar.
 * * PRECONDICIONES:
 * - El jugador y la lista de apuestas no deben ser null.
 * - La barrera debe estar configurada correctamente (N jugadores + 1).
 * * POSTCONDICIONES:
 * - El saldo del jugador se actualiza (incluso si está desconectado).
 * - Si hay conexión, se envía el mensaje visual y el comando de actualización.
 */
public class mandarPremios implements Runnable {

    // --- ATRIBUTOS ---
    private final List<Apuesta> listApuesta;
    private final Casilla ganadora;
    private final CyclicBarrier starter;
    private final Jugador jugador;

    // --- CONSTRUCTOR ---
    /**
     * @param jug         Jugador al que se procesan los premios.
     * @param listApuesta Lista de apuestas realizadas en esta ronda.
     * @param ganadora    La casilla ganadora generada por el servidor.
     * @param starter     Barrera para sincronizar el fin del cálculo con el resto de hilos.
     */
    public mandarPremios(Jugador jug, List<Apuesta> listApuesta, Casilla ganadora, CyclicBarrier starter) {
        this.ganadora = ganadora;
        this.listApuesta = listApuesta;
        this.starter = starter;
        this.jugador = jug;
    }

    // --- LÓGICA DE NEGOCIO ---
    @Override
    public void run() {
        double ganancia = 0.0;

        // 1. Calcular las ganancias (Operación local, sin bloqueos)
        for (Apuesta ap : listApuesta) {
            ganancia += RuletaUtils.calcularPremio(ganadora, ap);
        }

        // 2. Sincronización: Esperar a que todos los hilos terminen de calcular
        try {
            starter.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            // Si la barrera se rompe, marcamos interrupción pero CONTINUAMOS al finally
            // para asegurar que el jugador reciba su dinero.
            Thread.currentThread().interrupt();
        } finally {
            
            // 3. Sección Crítica: Actualización y Envío
            // IMPRESCINDIBLE: synchronized(jugador) para evitar colisión con AtenderJugador
            synchronized (jugador) {
                
                // A) Actualización segura del modelo
                jugador.sumaRestaSaldo(ganancia);

                // B) Notificación al cliente (si sigue conectado)
                try {
                    ObjectOutputStream out = jugador.getOutputStream();
                    
                    if (out != null) {
                        // Protocolo técnico: Actualizar variable local saldo en cliente
                        out.writeObject("actualizar saldo:" + ganancia);
                        
                        // Protocolo visual: Mensaje de felicitación
                        if (ganancia > 0) {
                            out.writeObject("\u001b[1m\u001b[33m🎉 ¡HAS GANADO: " + ganancia + "€! 🎉\u001b[0m");
                        } else {
                             out.writeObject("\u001b[1m\u001b[33m No ha habido suerte. Sigue minando!!! \u001b[0m");
                        }
                        
                        out.flush();
                        out.reset(); // Limpieza de caché del stream
                    }
                } catch (IOException e) {
                    // El jugador se desconectó justo ahora. 
                    // No hacemos nada, el saldo ya se actualizó en el servidor.
                }
            }
        }
    }
}