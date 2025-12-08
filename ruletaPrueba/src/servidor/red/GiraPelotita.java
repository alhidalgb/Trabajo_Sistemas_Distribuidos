package servidor.red;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import logicaRuleta.core.ServicioRuleta;
import modeloDominio.Casilla;
import servidor.persistencia.XMLServidor;

/**
 * Clase GiraPelotita
 * ------------------
 * Hilo director de la ronda. Orquesta la secuencia de estados de la mesa.
 * Garantiza que todos los clientes reciban los mensajes en el orden correcto
 * mediante el uso de CountDownLatch en cada paso crítico.
 */
public class GiraPelotita implements Runnable {

    private final ServicioRuleta rule;
    private final ExecutorService pool;
    private final XMLServidor xml;

    public GiraPelotita(ServicioRuleta rule, ExecutorService pool, XMLServidor xml) {
        this.rule = rule;
        this.pool = pool;
        this.xml = xml;
    }

    @Override
    public void run() {
        // Chequeo de seguridad por si el servidor se apaga
        if (Thread.currentThread().isInterrupted()) return;

        try {
            // =================================================================
            // 1. CERRAR APUESTAS (NO VA MÁS)
            // =================================================================
            // El servidor manda "NO_VA_MAS". Los clientes bloquean su input.
            //System.out.println("⛔ Cerrando mesa...");
            
            CountDownLatch latchCierre = new CountDownLatch(1);
            this.rule.NoVaMas(latchCierre);
            
            // Esperamos a que TODOS los clientes reciban el mensaje de cierre
            latchCierre.await(); 

            // =================================================================
            // 2. SIMULACIÓN DE GIRO (Suspense)
            // =================================================================
            // La mesa está cerrada. Nadie puede apostar. La bola gira.
            //System.out.println("... Girando la ruleta ...");
            TimeUnit.SECONDS.sleep(3); 

            // =================================================================
            // 3. GENERAR GANADOR
            // =================================================================
            int numero = new Random().nextInt(37); // 0 - 36
            Casilla ganadora = new Casilla(numero);
            
            // Mensaje bonito en amarillo para resaltar
            String mensajeGanador = "\u001b[33m🎲 ¡RESULTADO: " + numero + " " + ganadora.getColor() + "! 🎲\u001b[0m";
            System.out.println("Resultado: " + numero + " " + ganadora.getColor());

            // =================================================================
            // 4. COMUNICAR RESULTADO (Broadcast)
            // =================================================================
            // Enviamos el número ganador a todos.
            CountDownLatch latchResultado = new CountDownLatch(1);
            this.rule.broadcastMensaje(mensajeGanador, latchResultado);
            latchResultado.await();

            // =================================================================
            // 5. REPARTIR PREMIOS (Unicast)
            // =================================================================
            // Enviamos "actualizar saldo" y felicitaciones solo a los ganadores.
            CountDownLatch latchPremios = new CountDownLatch(1);
            this.rule.repartirPremio(ganadora, latchPremios);
            
            //AQUI SE BLOQUEA Y NUNCA AVANZA
            latchPremios.await(); 

            // =================================================================
            // 6. PERSISTENCIA (Async)
            // =================================================================
            // Guardamos el historial en XML. No bloqueamos el flujo principal.
            this.pool.execute(new guardarApuestas(this.rule.getCopiaJugadorApuestas(), ganadora, this.xml));

            // =================================================================
            // 7. TIEMPO DE LECTURA
            // =================================================================
            // Damos unos segundos para que los jugadores vean si han ganado o perdido
            // antes de limpiar la pantalla o habilitar la escritura de nuevo.
            System.out.println("⏳ Dejando tiempo para ver resultados...");
            TimeUnit.SECONDS.sleep(4);

            // =================================================================
            // 8. ABRIR NUEVA RONDA (ABRIR MESA)
            // =================================================================
            // El servidor limpia apuestas y manda "ABRIR_MESA".
            // Los clientes desbloquean su input.
            System.out.println("🟢 Abriendo nueva ronda...");
            
            CountDownLatch latchApertura = new CountDownLatch(1);
            this.rule.resetNoVaMas(latchApertura);
            latchApertura.await();

        } catch (InterruptedException e) {
            // Si el scheduler interrumpe el hilo durante un sleep/await
            Thread.currentThread().interrupt();
            System.out.println("GiraPelotita interrumpido (posible cierre de servidor).");
        } catch (Exception e) {
            System.err.println("Error en ciclo de ruleta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}