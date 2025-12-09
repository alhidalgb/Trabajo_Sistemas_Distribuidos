package servidor.red;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import logicaRuleta.core.AtenderJugador;
import logicaRuleta.core.ServicioRuleta;
import modeloDominio.Jugador;
import servidor.persistencia.ActualizarBD;
import servidor.persistencia.BDJugadores;
import servidor.persistencia.XMLServidor;

/**
 * Clase ServidorRuleta
 * --------------------
 * Punto de entrada (Main) del Servidor.
 * Orquestador principal que levanta la infraestructura de red, persistencia y lógica de juego.
 * * Responsabilidades:
 * 1. Cargar estado inicial (Jugadores) desde disco.
 * 2. Iniciar el ciclo de juego (GiraPelotita) y persistencia programada (ActualizarBD).
 * 3. Escuchar conexiones TCP entrantes y delegarlas a hilos trabajadores (AtenderJugador).
 */
public class ServidorRuleta {

    /**
     * Entry Point de la aplicación Servidor.
     * @param args Argumentos de consola (no usados).
     */
    public static void main(String[] args) {
        ServidorRuleta server = new ServidorRuleta();
        // Rutas relativas por defecto
        server.IniciarServidor(8000, "historial.xml", "jugadores.xml");
    }

    /**
     * Inicializa y ejecuta el servidor de ruleta.
     *
     * @param puerto    Puerto TCP de escucha (ej: 8000).
     * @param historial Ruta del archivo XML para historial de rondas.
     * @param bd        Ruta del archivo XML para base de datos de usuarios.
     */
    public void IniciarServidor(int puerto, String historial, String bd) {
        
        System.out.println("🚀 Iniciando Servidor Ruleta en puerto " + puerto + "...");
        
        // 1. PERSISTENCIA INICIAL
        XMLServidor xml = new XMLServidor(historial);
        File BBDD = new File(bd);

        // Cargar jugadores (o crear lista vacía si no existe fichero)
        List<Jugador> jugadoresConSesion = BDJugadores.UnmarshallingJugadores(BBDD);
        if (jugadoresConSesion == null) {
            System.out.println("ℹ️ No se encontró base de datos previa o estaba vacía. Iniciando desde cero.");
            jugadoresConSesion = new ArrayList<>();
        } else {
            System.out.println("✅ " + jugadoresConSesion.size() + " jugadores cargados desde BD.");
        }

        // 2. INFRAESTRUCTURA DE CONCURRENCIA
        // CachedThreadPool es ideal aquí: crea hilos bajo demanda y reutiliza los inactivos.
        // Si hay un pico de 100 usuarios, crea 100 hilos. Si bajan, los elimina.
        ExecutorService pool = Executors.newCachedThreadPool();

        // 3. LÓGICA DE NEGOCIO
        ServicioRuleta rule = new ServicioRuleta(jugadoresConSesion, pool, BBDD);

        // 4. TAREAS PROGRAMADAS (Scheduler)
        // Usamos un pool de 2 hilos para garantizar que la persistencia y el juego no se bloqueen mutuamente
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        try (ServerSocket server = new ServerSocket(puerto)) {
            
            System.out.println("🟢 Servidor ONLINE. Esperando conexiones...");

            // A) Tarea Cíclica: GiraPelotita (Ciclo de juego)
            // Ejecuta una ronda cada 20 segundos (Delay inicial de 10s para dar tiempo a conectar)
            scheduler.scheduleWithFixedDelay(new GiraPelotita(rule, pool, xml), 10, 20, TimeUnit.SECONDS);

            // B) Tarea Cíclica: Persistencia (Backup de seguridad)
            // Guarda el estado de los jugadores cada minuto
            scheduler.scheduleAtFixedRate(new ActualizarBD(jugadoresConSesion, BBDD), 1, 1, TimeUnit.MINUTES);

            // 5. BUCLE PRINCIPAL DE CONEXIÓN
            while (true) {
                try {
                    // Bloqueante hasta que llega un cliente
                    Socket cl = server.accept();
                    
                    // Configuración de Timeout (Opcional)
                    // Si un cliente no envía nada en 45s, se lanza SocketTimeoutException en AtenderJugador
                    cl.setSoTimeout(45000); 
                    
                    // Delegamos la gestión del cliente al Pool de Hilos
                    pool.execute(new AtenderJugador(cl, rule));
                    
                } catch (IOException e) {
                    System.err.println("⚠️ Error de conexión con cliente: " + e.getMessage());
                } catch (RejectedExecutionException e) {
                    System.err.println("⚠️ Servidor saturado. Conexión rechazada: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error crítico al iniciar servidor (Puerto ocupado?): " + e.getMessage());
        } finally {
            // CIERRE ORDENADO (Graceful Shutdown)
            System.out.println("⛔ Deteniendo servidor...");

            // Forzamos guardado final
            BDJugadores.MarshallingJugadores(jugadoresConSesion, BBDD);

            scheduler.shutdown();
            pool.shutdown();
            
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) scheduler.shutdownNow();
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) pool.shutdownNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.println("👋 Servidor detenido.");
        }
    }
}