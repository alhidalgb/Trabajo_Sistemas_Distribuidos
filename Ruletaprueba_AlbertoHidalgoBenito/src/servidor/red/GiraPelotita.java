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
 * Tarea Runnable que simula el giro de la bola en la ruleta.
 * Cierra apuestas, genera la casilla ganadora, reparte premios,
 * guarda resultados en XML y abre una nueva ronda.
 *
 * PRECONDICIONES:
 *  - El ServicioRuletaServidor debe estar inicializado.
 *  - El ExecutorService debe estar activo para ejecutar tareas concurrentes.
 *  - El XMLServidor debe estar disponible para persistencia.
 *
 * POSTCONDICIONES:
 *  - Se cierran las apuestas de la ronda actual.
 *  - Se genera una casilla ganadora aleatoria.
 *  - Se reparten premios a los jugadores.
 *  - Se guardan las apuestas y resultados en XML.
 *  - Se abre una nueva ronda tras un breve intervalo.
 */
public class GiraPelotita implements Runnable {

    // --- ATRIBUTOS ---
    private final ServicioRuleta rule;
    private final ExecutorService pool;
    private final XMLServidor xml;

    // --- CONSTRUCTOR ---
    /**
     * Inicializa la tarea de giro de la bola.
     *
     * @param rule Servicio de ruleta compartido.
     * @param pool ExecutorService para tareas concurrentes.
     * @param xml  Manejador de persistencia XML.
     */
    public GiraPelotita(ServicioRuleta rule, ExecutorService pool, XMLServidor xml) {
        this.rule = rule;
        this.pool = pool;
        this.xml = xml;
    }

    // --- LÓGICA DE NEGOCIO ---
    /**
     * Ejecuta la simulación del giro de la bola:
     * - Cierra apuestas.
     * - Espera un tiempo de gracia.
     * - Genera número ganador.
     * - Reparte premios y comunica la casilla.
     * - Guarda resultados en XML.
     * - Espera y abre nueva ronda.
     */
    @Override
    public void run() {
    	
    		
    		//MEJORAS: queiro que antes de que se reseete el NO VA MAS, se envie el mensaje. Lo que me ocurre ahora es que si estoy esperando a que se abra la mesa,
    		// se muestra primero el menu. Yo quiero que aparezca la casilla y las ganancias.
    		//Basicamente quiero mandar las Casillas y los Premiso antes de volver al menu.
    	
    		
    	
        // 1. CERRAR APUESTAS
        this.rule.NoVaMas();
        
        
        //Si ha ocurrido algun error, cerramos la mesa y paramos.        
        if(Thread.currentThread().isInterrupted()){return;}
        
        
        // 2. ESPERAR 2 segundos para que los hilos de apuestas terminen
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
        	
        		//MEJORA:Aqui tendria que cerrar el hilo??
        	
            Thread.currentThread().interrupt(); // restaurar estado 
            System.err.println("⚠️ GiraPelotita interrumpida durante la espera de cierre de apuestas");
        }
        
        
        
        // 3. GENERAR NÚMERO GANADOR
        int numeroGanador = new Random().nextInt(37); // 0-36
        Casilla ganadora = new Casilla(numeroGanador);

        
        // 4. REPARTIR PREMIOS Y COMUNICAR CASILLA
        
        //MEJORA:esta sincronizacion no me va bien.
        CountDownLatch count = new CountDownLatch(2);
        this.rule.mandarCasilla(ganadora,count);
        this.rule.repartirPremio(ganadora,count);

        // 5. GUARDAR EN XML (en paralelo)
        this.pool.execute(new guardarApuestas(this.rule.getCopiaJugadorApuestas(), ganadora, this.xml));

        // 6. ESPERAR 2 SEGUNDOS ANTES DE ABRIR NUEVA RONDA
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ GiraPelotita interrumpida durante la espera antes de resetear");
        }

        /*
        try {
			count.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
        
        // 7. RESETEAR Y ABRIR NUEVA RONDA
        this.rule.resetNoVaMas();
    }
}
