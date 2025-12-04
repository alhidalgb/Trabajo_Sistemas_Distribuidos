package logicaRuleta.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import logicaRuleta.concurrencia.GetIDHilos;
import logicaRuleta.concurrencia.MandarCasillaGanadora;
import logicaRuleta.concurrencia.MandarPremios;
import logicaRuleta.concurrencia.cancelarFuture;
import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;
import servidor.persistencia.ActualizarBD;

/**
 * Clase ServicioRuletaServidor
 * ----------------------------
 * Servicio central que coordina toda la lógica del casino de ruleta.
 * Gestiona jugadores, sesiones, apuestas, rondas y sincronización entre hilos.
 *
 * RESPONSABILIDADES:
 *  - Gestión de jugadores: registro, inicio de sesión, desconexión.
 *  - Control de rondas: apertura/cierre de mesa (VaMas/NoVaMas).
 *  - Gestión de apuestas: validación, almacenamiento y reparto de premios.
 *  - Sincronización: coordinación entre múltiples jugadores usando latches y barriers.
 *  - Persistencia: actualización de datos en XML mediante ActualizarBD.
 *
 * CONCURRENCIA:
 *  - Thread-safe: utiliza ConcurrentHashMap y listas sincronizadas.
 *  - Pool de hilos: ejecuta tareas concurrentes (búsquedas, premios, comunicación).
 *  - Sincronización: CountDownLatch para VaMas/NoVaMas, CyclicBarrier para premios/casillas.
 *
 * FLUJO DE RONDA:
 *  1. resetNoVaMas() → Abre mesa, jugadores pueden apostar.
 *  2. NoVaMas() → Cierra mesa, se genera número ganador.
 *  3. repartirPremio() → Calcula y entrega ganancias.
 *  4. mandarCasilla() → Comunica resultado a todos los jugadores.
 *  5. Vuelta al paso 1.
 */
public class ServicioRuleta {

    // --- ATRIBUTOS ---
    
    /**
     * Pool de hilos del servidor para tareas concurrentes.
     * - Búsquedas de jugadores (GetIDHilos).
     * - Reparto de premios (MandarPremios).
     * - Comunicación de casilla ganadora (MandarCasillaGanadora).
     * - Actualización de BD en segundo plano (ActualizarBD).
     */
    private final ExecutorService poolServer;
    
    /**
     * Lista de todos los jugadores registrados en el sistema (con o sin sesión activa).
     * Sincronizada para acceso concurrente seguro.
     */
    private final List<Jugador> jugadoresSesion;
    
    /**
     * Mapa que asocia cada jugador con sus apuestas en la ronda actual.
     * ConcurrentHashMap para escrituras thread-safe.
     * Las listas de apuestas son synchronized para operaciones atómicas.
     */
    private Map<Jugador, List<Apuesta>> jugadorApuestas;
    
    /**
     * Lista de jugadores con conexión activa (socket abierto).
     * De esta lista solo se añade y borra, con hacerla threadSafe ya no me tengo que preocupar 
     * de hacer synchronized los métodos.
     */
    private List<Jugador> jugadoresConexion;
    
    /**
     * Latch que se desbloquea cuando la mesa cierra (NoVaMas).
     * Los jugadores esperan en este latch para recibir el resultado de la ronda.
     */
    private CountDownLatch noVaMas;
    
    /**
     * Latch que se desbloquea cuando la mesa abre (resetNoVaMas).
     * Los jugadores esperan en este latch para empezar a apostar.
     */
    private CountDownLatch VaMas;
    
    /**
     * Flag que indica si la mesa está cerrada (true = no se aceptan apuestas).
     * Consultado por los hilos de jugadores antes de añadir apuestas.
     */
    private boolean isNoVaMas;
    
    
    
    
    private final File BBDD;

    // --- CONSTRUCTORES ---
    
    /**
     * Constructor con lista de jugadores y pool personalizado.
     *
     * PRE:
     *  - jugadoresSesion != null
     *  - pool != null
     *
     * POST:
     *  - El servicio está listo para operar.
     *  - Los latches están inicializados en estado bloqueado.
     *  - La mesa está cerrada (isNoVaMas = false inicialmente, pero VaMas bloqueado).
     *
     * @param jugadoresSesion Lista de jugadores existentes (cargados de BD).
     * @param pool            Pool de hilos personalizado.
     */
    public ServicioRuleta(List<Jugador> jugadoresSesion, ExecutorService pool,File BBDD) {
        this.noVaMas = null;
        this.VaMas = null;
        this.isNoVaMas = false;

        this.jugadoresSesion = Collections.synchronizedList(jugadoresSesion);
        this.jugadorApuestas = new ConcurrentHashMap<>();
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        
        this.poolServer = pool;
        this.BBDD = BBDD;
    }
    
    /**
     * Constructor por defecto (sin jugadores previos).
     *
     * POST:
     *  - El servicio está listo para operar.
     *  - Crea un CachedThreadPool automáticamente.
     *  - Listas vacías de jugadores y apuestas.
     */
    public ServicioRuleta() {
    	
    		//Se inician con valores nulos y se inician en GirarPelotita, asi evitamos posibles bloqueas.
    		//Los bloqueos que pueden ocurrir es que un jugador si inicia la partida muy rapido, antes de que gire la pelotita mas concretamente, se quedara esperando para siempre en 
    		//this.rule.VaMasAwait();  porque se cambiara la referencia de ese VaMas en girarPelotita.class cuando llame a this.rule.NoVaMas();
    		this.noVaMas = null;
        this.VaMas = null;
        this.isNoVaMas = false;

        this.jugadoresSesion = Collections.synchronizedList(new ArrayList<>());
        this.jugadorApuestas = new ConcurrentHashMap<>();
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        this.poolServer = Executors.newCachedThreadPool();
		this.BBDD = null;
    }

    // --- GETTERS / SETTERS ---
    
    /**
     * Obtiene la lista de jugadores registrados.
     *
     * POST: Retorna lista sincronizada (thread-safe para iteración con synchronized).
     *
     * @return Lista sincronizada de jugadores.
     */
    public List<Jugador> getListJugadoresSesion() { 
        return this.jugadoresSesion; 
    }
    
    /**
     * Obtiene el mapa de apuestas de la ronda actual.
     *
     * POST: Retorna referencia directa al ConcurrentHashMap (thread-safe).
     *
     * @return Mapa jugador → lista de apuestas.
     */
    public Map<Jugador, List<Apuesta>> getJugadorApuestas() { 
        return this.jugadorApuestas; 
    }
    
    /**
     * Reemplaza el mapa de apuestas (uso interno).
     *
     * PRE: m != null
     * POST: El mapa interno se reemplaza por el nuevo.
     *
     * @param m Nuevo mapa de apuestas.
     */
    public void setJugadorApuestas(Map<Jugador, List<Apuesta>> m) { 
        this.jugadorApuestas = m; 
    }
    
    /**
     * Crea una copia defensiva del mapa de apuestas.
     * Útil para persistencia o cálculos sin afectar el original.
     *
     * PRE: Ninguna.
     * POST:
     *  - Retorna un HashMap nuevo con copias de las listas de apuestas.
     *  - Las modificaciones en la copia NO afectan al original.
     *
     * SINCRONIZACIÓN:
     *  - Toma un snapshot del mapa para evitar ConcurrentModificationException.
     *  - Copia cada lista de apuestas de forma atómica (synchronized).
     *
     * @return Copia profunda del mapa de apuestas.
     */
    public Map<Jugador, List<Apuesta>> getCopiaJugadorApuestas() {
        // Foto del mapa: referencias coherentes en un instante
        Map<Jugador, List<Apuesta>> foto;
        synchronized (jugadorApuestas) {
            foto = new HashMap<>(jugadorApuestas);
        }

        Map<Jugador, List<Apuesta>> copia = new HashMap<>(foto.size());
        for (Map.Entry<Jugador, List<Apuesta>> entry : foto.entrySet()) {
            Jugador jugador = entry.getKey();
            List<Apuesta> listaOriginal = entry.getValue();
            synchronized (listaOriginal) {
                copia.put(jugador, new ArrayList<>(listaOriginal));
            }
        }
        return copia;
    }

    // --- CONTROL DE RONDAS ---
    
    /**
     * Reinicia el estado de la mesa para una nueva ronda.
     * Abre la mesa y permite que los jugadores empiecen a apostar.
     *
     * PRE: Debe llamarse DESPUÉS de NoVaMas() y reparto de premios.
     * POST:
     *  - isNoVaMas = false → mesa abierta.
     *  - jugadorApuestas vacío → nuevas apuestas para la ronda.
     *  - noVaMas reiniciado → bloqueado hasta próximo cierre.
     *  - VaMas desbloqueado → jugadores pueden entrar a la mesa.
     *
     * SINCRONIZACIÓN:
     *  - Los hilos bloqueados en VaMasAwait() se desbloquean.
     */
    public void resetNoVaMas() {
        // Limpiar apuestas de la ronda anterior
        this.jugadorApuestas.clear();
        this.isNoVaMas = false;

        // Reiniciar latch de noVaMas para la próxima ronda
        this.noVaMas = new CountDownLatch(1);

        // Desbloquear jugadores que estaban esperando VaMas
        if (this.VaMas != null) {
            this.VaMas.countDown();
        }
    }

    /**
     * Cierra la mesa y detiene la aceptación de apuestas.
     * Se llama cuando el crupier decide girar la ruleta.
     *
     * PRE: Debe llamarse DESPUÉS de un período de apuestas abiertas.
     * POST:
     *  - isNoVaMas = true → no se aceptan más apuestas.
     *  - noVaMas desbloqueado → jugadores reciben resultado.
     *  - VaMas reiniciado → bloqueado hasta resetNoVaMas().
     *
     * SINCRONIZACIÓN:
     *  - Los hilos bloqueados en noVaMasAwait() se desbloquean.
     */
    public void NoVaMas() {
        this.isNoVaMas = true;

        // Desbloquear jugadores esperando resultado
        if (this.noVaMas != null) {
            this.noVaMas.countDown();
        }

        // Reiniciar latch de VaMas para la siguiente ronda
        this.VaMas = new CountDownLatch(1);
    }

    /**
     * Espera a que la mesa cierre (bloquea el hilo).
     * Los jugadores llaman esto después de terminar sus apuestas.
     *
     * PRE: El hilo debe estar en estado de juego (después de VaMasAwait).
     * POST: Se desbloquea cuando NoVaMas() es llamado.
     *
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public void noVaMasAwait() throws InterruptedException {
        if (this.noVaMas != null) {
            this.noVaMas.await();
        }
    }

    /**
     * Espera a que la mesa abra (bloquea el hilo).
     * Los jugadores llaman esto al entrar a la ruleta.
     *
     * PRE: El hilo debe haber solicitado jugar.
     * POST: Se desbloquea cuando resetNoVaMas() es llamado.
     *
     * @throws InterruptedException Si el hilo es interrumpido durante la espera.
     */
    public void VaMasAwait() throws InterruptedException {
        if (this.VaMas != null) {
            this.VaMas.await();
        }
    }

    /**
     * Consulta si la mesa está cerrada.
     *
     * POST: Retorna true si no se aceptan apuestas, false si la mesa está abierta.
     *
     * @return true si la mesa está cerrada, false en caso contrario.
     */
    public boolean isNoVaMas() {
        return this.isNoVaMas;
    }

    // --- GESTIÓN DE JUGADORES ---
    
    /**
     * Busca un jugador por su ID en la lista de sesiones.
     * Utiliza búsqueda paralela dividiendo la lista en 30 particiones.
     *
     * PRE:
     *  - iD != null && !iD.isEmpty()
     *
     * POST:
     *  - Retorna el jugador si existe, null si no se encuentra.
     *  - La búsqueda es thread-safe (copia defensiva de la lista).
     *
     * OPTIMIZACIÓN:
     *  - Si N < 30, cada hilo busca en un rango pequeño.
     *  - Si N >= 30, se divide en 30 particiones.
     *  - Los Future se cancelan automáticamente tras encontrar el jugador.
     *
     * @param iD Identificador del jugador.
     * @return Jugador encontrado o null.
     */
    
    
    
    //MEJORAS: quiero saber si hay una forma mas eficiente de implementar este metodo, algo que en cuento encuentre el Jugador, se cancele todo al instante.
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Jugador getJugador(String iD) {
        // Caso de entrada inválida: id nulo o vacío
        if (iD == null || iD.trim().isEmpty()) {
            // Devolvemos null silencioso → el llamador abortará la conexión
            return null;
        }

        synchronized (jugadoresSesion) {
            int N = this.jugadoresSesion.size();
            
            //MEJORA:cual es la forma optima de sacar las particiones?
            
            int particiones = 30;
            
            int in = 0;
            int fin = 2;
            
            if (30 < N) {
                fin = N / 30;
            }
            int suma = fin;
            
            List<Future<Jugador>> lfj = new ArrayList<>(particiones);
            
            
            
            for (int i = 0; i < particiones; i++) {
                // Super importante mandar una copia, si mando la referencia original al estar en un bloque synchronize se bloquea todo.
                // Mando una copia, como esta en un bloque síncrono de la lista no hay problema, no se cambiará mientras la utilizo.
            	
            	
            		//MEJORA: usar subList?
            	
                lfj.add(this.poolServer.submit(new GetIDHilos(new ArrayList<>(this.jugadoresSesion), in, fin, iD)));
                
                in += suma;
                fin += suma;
                
                if (i==particiones-2) {
                    fin = N;
                }
            }
            
            Jugador sesion = null;
            
            //MEJORA: es mas eficiente hacerla con Future, o con un flag??
            
            for (Future<Jugador> jug : lfj) {
                try {
                    sesion = jug.get();
                    if (sesion != null) {
                        break;
                    }
                } catch (InterruptedException e) {
                	
                	
                		//MEJORA: esto no lo entiendo. Si el currente Thread, es el AtenderPeticion.
                	
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            
            // Cerramos los future para ahorrar CPU.
            this.poolServer.execute(new cancelarFuture(lfj));
            return sesion;
        }
    }

    /**
     * Establece la conexión de un jugador al servidor.
     * Si el jugador ya tiene sesión activa, rechaza la nueva conexión.
     *
     * PRE:
     *  - jug != null
     *  - cliente != null && !cliente.isClosed()
     *
     * POST:
     *  - Si el jugador NO tiene sesión activa:
     *    → Se marca como conectado (isSesionIniciada = true).
     *    → Se asocia el socket.
     *    → Se añade a jugadoresConexion.
     *  - Si el jugador YA tiene sesión activa:
     *    → Se envía mensaje de error al nuevo cliente.
     *    → Se cierra el socket duplicado.
     *    → Se desconecta al jugador original (seguridad).
     *
     * @param jug     Jugador a conectar.
     * @param cliente Socket del cliente.
     * @throws IOException Si hay error al escribir en el socket.
     */
    public void establecerConexion(Jugador jug, Socket cliente) throws IOException {
        synchronized (jug) { // Lock común por jugador
            if (!jug.isSesionIniciada()) {
                jug.setSesionIniciada(true);
                jug.setConexion(cliente);
                jugadoresConexion.add(jug); // suficiente, ya es thread-safe

            } else {
                // Jugador ya conectado → abortamos la nueva conexión
                try (OutputStream os = cliente.getOutputStream()) {
                    os.write("El usuario ya ha iniciado sesion\r\n".getBytes());
                    os.flush();
                }
                // Cerramos el socket duplicado
                cliente.close();

                // Desconexión controlada en el servidor
                this.desconectarJugador(jug);
            }
        }
    }

    /**
     * Registra un nuevo jugador en el sistema.
     *
     * PRE:
     *  - name != null && !name.isEmpty()
     *  - saldo >= 5 && saldo <= 10000
     *  - cliente != null && !cliente.isClosed()
     *
     * POST:
     *  - Si el nombre NO existe:
     *    → Se crea el jugador con el saldo inicial.
     *    → Se establece conexión.
     *    → Se añade a jugadoresSesion.
     *    → Retorna el jugador creado.
     *  - Si el nombre YA existe:
     *    → Retorna null.
     *
     * @param name    Nombre del jugador.
     * @param saldo   Saldo inicial.
     * @param cliente Socket del cliente.
     * @return Jugador registrado o null si el nombre ya existe.
     * @throws IOException Si hay error al establecer conexión.
     */
    public Jugador registroSesionDefinitivo(String name, double saldo, Socket cliente) throws IOException {
        // Validación de entrada: nombre inválido → abortamos registro
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        synchronized (jugadoresSesion) {
            Jugador jug = this.getJugador(name);

            if (jug == null) {
                jug = new Jugador(name, saldo);
                // Puede lanzar IOException → se propaga a AtenderJugador
                this.establecerConexion(jug, cliente);
                jugadoresSesion.add(jug);
                return jug;
            } else {
                // Nombre ya existente → devolvemos null silencioso
                return null;
            }
        }
    }

    /**
     * Inicia sesión de un jugador existente.
     *
     * PRE:
     *  - name != null && !name.isEmpty()
     *  - cliente != null && !cliente.isClosed()
     *
     * POST:
     *  - Si el jugador existe:
     *    → Se establece conexión (si no tiene sesión activa).
     *    → Retorna el jugador.
     *  - Si el jugador NO existe:
     *    → Retorna null.
     *
     * @param name    Nombre del jugador.
     * @param cliente Socket del cliente.
     * @return Jugador autenticado o null si no existe.
     * @throws IOException Si hay error al establecer conexión.
     */
    public Jugador inicioSesionDefinitivo(String name, Socket cliente) throws IOException {
        // Validación de entrada: nombre inválido → abortamos inicio de sesión
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        synchronized (jugadoresSesion) {
            Jugador jug = this.getJugador(name);

            if (jug != null) {
                // Puede lanzar IOException → se propaga a AtenderJugador
                this.establecerConexion(jug, cliente);
            }
            // Si no existe → devolvemos null silencioso
            return jug;
        }
    }

    // --- GESTIÓN DE APUESTAS ---
    
    /**
     * Añade una apuesta del jugador a la ronda actual.
     *
     * PRE:
     *  - jug != null
     *  - apuesta != null
     *  - jug.getSaldo() >= apuesta.getCantidad()
     *  - isNoVaMas == false (mesa abierta)
     *
     * POST:
     *  - Si la apuesta es válida:
     *    → Se añade a la lista del jugador.
     *    → Se resta el importe del saldo.
     *    → Retorna true.
     *  - Si la apuesta NO es válida:
     *    → Retorna false (no se modifica nada).
     *
     * VALIDACIONES:
     *  - Mesa cerrada (isNoVaMas = true) → rechaza apuesta.
     *  - Apuesta nula → rechaza apuesta.
     *  - Saldo insuficiente → rechaza apuesta.
     *
     * @param jug     Jugador que apuesta.
     * @param apuesta Apuesta a registrar.
     * @return true si la apuesta fue aceptada, false en caso contrario.
     */
    public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
        // NO ACEPTAR APUESTAS SI LA MESA ESTÁ CERRADA
        if (this.isNoVaMas) {
            return false;
        }

        if (apuesta == null) {
            return false;
        }

        // Validar saldo suficiente antes de añadir
        if (jug.getSaldo() < apuesta.getCantidad()) {
            return false;
        }

        List<Apuesta> listaDelJugador =
            this.jugadorApuestas.computeIfAbsent(
                jug,
                k -> Collections.synchronizedList(new ArrayList<>())
            );

        boolean add = listaDelJugador.add(apuesta);
        if (add) {
            jug.restarApuesta(apuesta.getCantidad()); // ya es synchronized en Jugador
        }

        return add;
    }

    /**
     * Reparte premios a todos los jugadores que apostaron en la ronda.
     * Utiliza un pool de hilos y CyclicBarrier para sincronizar el reparto.
     *
     * PRE:
     *  - ganadora != null
     *  - NoVaMas() ya fue llamado (mesa cerrada).
     *
     * POST:
     *  - Se calcula la ganancia de cada jugador según sus apuestas.
     *  - Se suma la ganancia al saldo del jugador.
     *  - Se envía mensaje al cliente con la ganancia (si está conectado).
     *  - Todos los hilos esperan en la barrera antes de actualizar saldos.
     *
     * CONCURRENCIA:
     *  - Crea un pool de hilos del tamaño del número de jugadores.
     *  - CyclicBarrier sincroniza el reparto (todos terminan al mismo tiempo).
     *  - Timeout de 3 segundos para shutdown del pool.
     *
     * @param ganadora Casilla ganadora de la ronda.
     */
    public void repartirPremio(Casilla ganadora,CountDownLatch count) {
        if (this.jugadorApuestas.isEmpty()) {
            // No hay apuestas para repartir (silencioso)
        		//count.countDown();
            return;
        }

        final CyclicBarrier starter = new CyclicBarrier(this.jugadorApuestas.size() + 1);
        ExecutorService poolPremios = Executors.newFixedThreadPool(this.jugadorApuestas.size());

        try {
            for (Map.Entry<Jugador, List<Apuesta>> entry : this.jugadorApuestas.entrySet()) {
                // Copia defensiva de la lista de apuestas
                List<Apuesta> copiaApuestas;
                synchronized (entry.getValue()) {
                    copiaApuestas = new ArrayList<>(entry.getValue());
                }
                poolPremios.execute(new MandarPremios(entry.getKey(), copiaApuestas, ganadora, starter));
            }

            // Esperar a que todos los hilos de premios lleguen a la barrera
            starter.await();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaurar estado de interrupción
        } catch (BrokenBarrierException e) {
            System.err.println("⚠️ Error en la barrera de premios: " + e.getMessage());
        } finally {
            //count.countDown();
            poolPremios.shutdown();
            try {
                if (!poolPremios.awaitTermination(3, TimeUnit.SECONDS)) {
                    poolPremios.shutdownNow();
                }
            } catch (InterruptedException e) {
                poolPremios.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
        }
    }

    /**
     * Comunica la casilla ganadora a todos los jugadores conectados.
     * Utiliza un pool de hilos y CyclicBarrier para sincronizar el envío.
     *
     * PRE:
     *  - ganadora != null
     *  - NoVaMas() ya fue llamado.
     *
     * POST:
     *  - Todos los jugadores conectados reciben un mensaje con la casilla ganadora.
     *  - Los jugadores desconectados son ignorados (no se interrumpe la ronda).
     *  - Todos los hilos esperan en la barrera antes de enviar el mensaje.
     *
     * CONCURRENCIA:
     *  - Snapshot de jugadoresConexion para evitar ConcurrentModificationException.
     *  - Si un jugador se conecta DESPUÉS del snapshot, no participa en esta ronda.
     *  - CyclicBarrier sincroniza el envío (todos envían al mismo tiempo).
     *  - Timeout de 3 segundos para shutdown del pool.
     *
     * @param ganadora Casilla ganadora de la ronda.
     */
    public void mandarCasilla(Casilla ganadora,CountDownLatch count) {
        if (this.jugadoresConexion.isEmpty()) {
        		//count.countDown();
            return;
        }

        synchronized (this.jugadoresConexion) {
            // Snapshot del tamaño actual → si alguien se conecta después, no participa en esta ronda
            final CyclicBarrier starter = new CyclicBarrier(this.jugadoresConexion.size() + 1);
            ExecutorService poolCasilla = Executors.newFixedThreadPool(this.jugadoresConexion.size());

            try {
                for (Jugador j : this.jugadoresConexion) {
                    poolCasilla.execute(new MandarCasillaGanadora(j.getConexion(), starter, ganadora));
                }

                // Esperar a que todos los hilos lleguen a la barrera
                starter.await();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restaurar estado de interrupción
            } catch (BrokenBarrierException e) {
                System.err.println("⚠️ Error en la barrera de casillas: " + e.getMessage());
            } finally {
            		//count.countDown();
                poolCasilla.shutdown();
                try {
                    if (!poolCasilla.awaitTermination(3, TimeUnit.SECONDS)) {
                        poolCasilla.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    poolCasilla.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                
            }
        }
    }

    // --- DESCONEXIÓN ---
    
    /**
     * Desconecta un jugador de forma limpia.
     *
     * PRE: jug puede ser null (se valida internamente).
     * POST:
     *  - Se marca la sesión como cerrada (isSesionIniciada = false).
     *  - Se cierra el socket del jugador.
     *  - Se elimina de jugadoresConexion.
     *  - Se actualiza la BD en segundo plano (ActualizarBD).
     *
     * CONCURRENCIA:
     *  - Sincroniza sobre el jugador para evitar race conditions.
     *  - No pasa nada por que se produzcan errores de carrera en la actualización de BD,
     *    en el sentido de estar todo el rato sobreescribiendo, ya que al final la lista
     *    jugadoresSesion siempre va a estar actualizada.
     *
     * @param jug Jugador a desconectar.
     */
    public void desconectarJugador(Jugador jug) {
        if (jug == null) {
            return;
        }

        synchronized (jug) {
            // Marcar sesión como cerrada
            jug.setSesionIniciada(false);

            // Cerrar socket si existe
            Socket conexion = jug.getConexion();
            if (conexion != null && !conexion.isClosed()) {
                try {
                    conexion.close();
                } catch (IOException e) {
                    // Log silencioso, no propagamos
                    System.err.println("⚠️ Error cerrando socket de jugador " + jug.getID() + ": " + e.getMessage());
                }
            }
            jug.setConexion(null);

        }
        
        jugadoresConexion.remove(jug);
        //No pasa nada por que se produzcan errores de carrera, en el sentido de estar todo el rato sobreescribiendo, ya que al final la lista        
        // jugadoresSesion siempre va a estar acutalizada.         
        this.poolServer.execute(new ActualizarBD(new ArrayList<>(this.jugadoresSesion),this.BBDD));        
       
    }
    
    
    public void desconectarTodo() {
    	
    	
    	//Pendiente de pensar, hay que teneer en cuenta los flags, isInterrupt().
    	
    	
    }

    
    
}