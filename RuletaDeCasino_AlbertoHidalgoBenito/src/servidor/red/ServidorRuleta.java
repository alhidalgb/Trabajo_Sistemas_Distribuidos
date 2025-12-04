package servidor.red;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import logicaRuleta.core.AtenderJugador;
import logicaRuleta.core.ServicioRuleta;
import modeloDominio.Jugador;
import servidor.persistencia.ActualizarBD;
import servidor.persistencia.BDJugadores;
import servidor.persistencia.XMLServidor;

import java.util.*;

/**
 * Clase ServidorRuleta
 * --------------------
 * Representa el servidor principal de la ruleta. Se encarga de:
 *  - Cargar jugadores desde la base de datos XML.
 *  - Iniciar el servicio de ruleta.
 *  - Aceptar conexiones de clientes y atenderlas en paralelo.
 *  - Lanzar tareas periódicas: crupier automático y actualización de la base de datos.
 *
 * PRECONDICIONES:
 *  - El puerto debe estar libre y accesible.
 *  - Los ficheros historial y bd deben ser rutas válidas en el sistema de archivos.
 *
 * POSTCONDICIONES:
 *  - El servidor queda escuchando en el puerto indicado.
 *  - Los clientes pueden conectarse y ser atendidos en paralelo.
 *  - Cada 20 segundos se ejecuta el ciclo de la ruleta (GiraPelotita).
 *  - Cada minuto se guarda la base de datos de jugadores (ActualizarBD).
 *  - Al cerrar, se persiste el estado de los jugadores y se cierran los pools de hilos.
 */
public class ServidorRuleta {

    /**
     * Inicia el servidor de ruleta en el puerto indicado.
     *
     * @param puerto    Puerto TCP donde escuchar conexiones de clientes.
     * @param historial Ruta del fichero XML donde se guarda el historial de apuestas.
     * @param bd        Ruta del fichero XML donde se guarda la base de datos de jugadores.
     */
    public void IniciarServidor(int puerto, String historial, String bd) {
        // Inicializar historial XML
        XMLServidor xml = new XMLServidor(historial);
        
        File BBDD= new File(bd);
        

        // Cargar jugadores desde la base de datos
        List<Jugador> jugadoresConSesion = BDJugadores.UnmarshallingJugadores(BBDD);

        for (Jugador j : jugadoresConSesion) {
            System.out.println("Jugador cargado: " + j);
        }
        
       
        
        System.out.println(jugadoresConSesion.size());

        // Pool de hilos para atender clientes
        
        //MEJORA: ¿es mejor .newCachedThreadPool() o .newFixedThreadPool(Runtime.getRuntime().availableProcessors()) ?
        //Es bueno pasarle esta pool a otras clases?
        
        
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        // Inicializar lógica de ruleta con jugadores cargados
        ServicioRuleta rule = new ServicioRuleta(jugadoresConSesion, pool,BBDD);

        // Scheduler para tareas periódicas (crupier y actualización BD)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        try (ServerSocket server = new ServerSocket(puerto)) {
        	
        	
            // Crupier automático cada 20 segundos
            scheduler.scheduleWithFixedDelay(new GiraPelotita(rule, pool, xml),10, 20, TimeUnit.SECONDS);

            // Guardar BD de jugadores cada minuto
            scheduler.scheduleAtFixedRate(new ActualizarBD(jugadoresConSesion, BBDD),1, 1, TimeUnit.MINUTES);

            // Bucle principal: aceptar clientes
            while (true) {
            	
                try {
                	
                    Socket cliente = server.accept();
                    pool.execute(new AtenderJugador(cliente, rule));
                    
                    
                } catch (IOException e) {
                    System.err.println("⚠️ Error aceptando cliente: " + e.getMessage());
                } catch (RejectedExecutionException e) {
                    System.err.println("⚠️ Pool saturado: " + e.getMessage());
                }
            }
            
            
            
        } catch (IOException e) {
            System.err.println("⚠️ Error iniciando servidor: " + e.getMessage());
        } finally {
            // Guardar estado de jugadores al cerrar
            BDJugadores.MarshallingJugadores(jugadoresConSesion, BBDD);

            scheduler.shutdown();
            pool.shutdown();
            try {
                scheduler.awaitTermination(3, TimeUnit.SECONDS);
                pool.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String [ ] args) {
		
		ServidorRuleta server = new ServidorRuleta();
		server.IniciarServidor(8000,"historial.xml","jugadores.xml");

	}
    
}
