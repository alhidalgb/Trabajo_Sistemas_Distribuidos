package logicaRuleta.concurrencia;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import modeloDominio.Jugador;

/**
 * Clase mandarMensaje
 * -------------------
 * Tarea concurrente encargada de difundir un mensaje a una sublista de jugadores.
 * Utiliza un ThreadPool interno para paralelizar el envío dentro de este grupo.
 * * PRECONDICIONES:
 * - La lista de jugadores no debe ser null.
 * - La barrera debe estar dimensionada para (Total Jugadores + 1).
 * * POSTCONDICIONES:
 * - Se envía el mensaje a todos los jugadores con conexión activa.
 * - Se sincroniza con la barrera para garantizar que el servidor no avance hasta terminar el envío.
 */
public class mandarMensaje implements Runnable {

    // --- ATRIBUTOS ---
    private final String msg;
    private final List<Jugador> jugadores;
    private final CyclicBarrier starter;

    // --- CONSTRUCTOR ---
    /**
     * @param msg       Mensaje a enviar (String).
     * @param jugadores Sublista de jugadores a los que notificar.
     * @param starter   Barrera global de sincronización.
     */
    public mandarMensaje(String msg, List<Jugador> jugadores, CyclicBarrier starter) {
        this.msg = msg;
        this.jugadores = jugadores;
        this.starter = starter;
    }

    // --- LÓGICA DE NEGOCIO ---
    @Override
    public void run() {
        // Validación rápida de interrupción
        if (Thread.currentThread().isInterrupted()) return;

        // Creamos un pool local para enviar a este grupo de jugadores en paralelo
        // Math.max(1, ...) evita error si la lista está vacía
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, jugadores.size()));

        for (final Jugador j : jugadores) {
            
            // Comprobación de seguridad por si el hilo principal se interrumpe
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            // Usamos Clase Anónima en lugar de Lambda
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    
                    // SECCIÓN CRÍTICA: Bloqueamos al jugador para escritura exclusiva
                    // Obligatorio para no chocar con AtenderJugador
                    synchronized (j) {
                        ObjectOutputStream out = null;
                        
                        // 1. Intento de escritura
                        try {
                            out = j.getOutputStream();
                            if (out != null) {
                                out.writeObject(msg);
                                // Nota: No hacemos flush todavía para sincronizar el efecto visual
                            }
                        } catch (IOException e) {
                            // Jugador desconectado o error de red. 
                            // Lo ignoramos aquí para que el código siga y llegue al await.
                        }

                        // 2. Sincronización (CRÍTICO)
                        // Este bloque debe ejecutarse SIEMPRE, haya fallado el writeObject o no.
                        // Si no se ejecuta, la barrera nunca llega al tope y el servidor se congela (Deadlock).
                        try {
                            starter.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            // 3. Limpieza final (Flush y Reset)
                            if (out != null) {
                                try {
                                    out.flush();
                                    out.reset(); // Evita fugas de memoria en la caché del stream
                                } catch (IOException e) {
                                    // Error final, no podemos hacer nada más
                                }
                            }
                        }
                    } // Fin synchronized
                }
            });
        }

        // Cerramos el pool local para liberar recursos
        pool.shutdown();
    }
}