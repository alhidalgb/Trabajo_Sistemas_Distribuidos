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
 * Hilo director (Orquestador) de una ronda de juego.
 * Controla la secuencia de tiempos: Cierre de mesa -> Giro -> Resultado -> Premios -> Apertura.
 * * PRECONDICIONES:
 * - El servicio de ruleta y el pool de hilos deben estar inicializados.
 * * NOTA SOBRE CONCURRENCIA:
 * - Esta clase se ejecuta en un hilo separado.
 * - Utiliza CountDownLatch para bloquearse a sí misma hasta que el ServicioRuleta 
 * confirme que una operación masiva (Broadcast/Premios) ha terminado.
 */
public class GiraPelotita implements Runnable {

    // --- ATRIBUTOS ---
    private final ServicioRuleta rule;
    private final ExecutorService pool;
    private final XMLServidor xml;

    // --- CONSTRUCTOR ---
    public GiraPelotita(ServicioRuleta rule, ExecutorService pool, XMLServidor xml) {
        this.rule = rule;
        this.pool = pool;
        this.xml = xml;
    }

    // --- LÓGICA DE LA RONDA ---
    @Override
    public void run() {
        // Chequeo de seguridad inicial
        if (Thread.currentThread().isInterrupted()) return;

        try {
            // =================================================================
            // 1. CERRAR APUESTAS (NO VA MÁS)
            // =================================================================
            //System.out.println("⛔ Cerrando mesa...");
            
            CountDownLatch latchCierre = new CountDownLatch(1);
            // El servicio notifica a todos los clientes. Cuando termina, baja el latch.
            this.rule.NoVaMas(latchCierre);
            latchCierre.await(); 

            // =================================================================
            // 2. SIMULACIÓN DE GIRO (Suspenso)
            // =================================================================
            // System.out.println("... Girando la ruleta ...");
            TimeUnit.SECONDS.sleep(3); 

            // =================================================================
            // 3. GENERAR GANADOR
            // =================================================================
            // Generamos número aleatorio [0-36]
            int numero = new Random().nextInt(37); 
            Casilla ganadora = new Casilla(numero);
            
            String mensajeGanador = "\u001b[33m🎲 ¡RESULTADO: " + numero + " " + ganadora.getColor() + "! 🎲\u001b[0m";
            System.out.println("Resultado de la ronda: " + numero + " (" + ganadora.getColor() + ")");

            // =================================================================
            // 4. COMUNICAR RESULTADO (Broadcast)
            // =================================================================
            CountDownLatch latchResultado = new CountDownLatch(1);
            this.rule.enviarBroadcastConcurrente(mensajeGanador, latchResultado);
            latchResultado.await();

            // =================================================================
            // 5. REPARTIR PREMIOS (Unicast Paralelo)
            // =================================================================
            // Calculamos premios y actualizamos saldos.
            // NOTA: Si se bloquea aquí, revisa el tamaño del ThreadPool en el Main.
            CountDownLatch latchPremios = new CountDownLatch(1);
            this.rule.repartirPremio(ganadora, latchPremios);
            latchPremios.await(); 

            // =================================================================
            // 6. PERSISTENCIA (Asíncrona "Fire & Forget")
            // =================================================================
            // Guardamos el historial en XML en segundo plano para no frenar el juego.
            // Se pasa una COPIA de las apuestas para evitar ConcurrentModificationException.
            this.pool.execute(new guardarApuestas(this.rule.getCopiaJugadorApuestas(), ganadora, this.xml));

            // =================================================================
            // 7. TIEMPO DE LECTURA (Cooldown)
            // =================================================================
            System.out.println("⏳ Esperando para nueva ronda...");
            TimeUnit.SECONDS.sleep(4);

            // =================================================================
            // 8. ABRIR NUEVA RONDA (ABRIR MESA)
            // =================================================================
            System.out.println("🟢 Abriendo mesa...");
            
            CountDownLatch latchApertura = new CountDownLatch(1);
            this.rule.resetNoVaMas(latchApertura);
            latchApertura.await();

        } catch (InterruptedException e) {
            // Interrupción limpia (apagado del servidor)
            Thread.currentThread().interrupt();
            System.out.println("Ronda interrumpida. Cerrando hilo GiraPelotita.");
        } catch (Exception e) {
            // Captura de errores imprevistos para no tumbar el hilo silenciosamente
            System.err.println("❌ Error CRÍTICO en el ciclo de la ruleta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}