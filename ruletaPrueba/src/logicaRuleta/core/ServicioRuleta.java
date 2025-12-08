package logicaRuleta.core;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.*;

import logicaRuleta.concurrencia.GetIDHilos;
import logicaRuleta.concurrencia.MandarPremios;
import logicaRuleta.concurrencia.cancelarFuture;
import logicaRuleta.concurrencia.mandarMensaje;
import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;
import servidor.persistencia.ActualizarBD;

public class ServicioRuleta {

    // --- ATRIBUTOS ---
    private final ExecutorService poolServer;
    
    // Listas thread-safe
    private final List<Jugador> jugadoresSesion;
    private final List<Jugador> jugadoresConexion;
    
    // Mapa de apuestas (ConcurrentHashMap para seguridad en escritura)
    private Map<Jugador, List<Apuesta>> jugadorApuestas;

    // ESTADO DE LA MESA
    // Solo necesitamos el booleano. La sincronización de espera la hace el cliente.
    private volatile boolean isNoVaMas; 
    
    private final File BBDD;

    // --- CONSTRUCTORES ---

    public ServicioRuleta(List<Jugador> jugadoresSesion, ExecutorService pool, File BBDD) {
        this.isNoVaMas = false; // La mesa empieza abierta

        this.jugadoresSesion = Collections.synchronizedList(jugadoresSesion);
        this.jugadorApuestas = new ConcurrentHashMap<>();
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        
        this.poolServer = pool;
        this.BBDD = BBDD;
    }
    
    public ServicioRuleta() {
        this(new ArrayList<>(), Executors.newCachedThreadPool(), null);
    }

    // --- GETTERS / SETTERS ---

    public List<Jugador> getListJugadoresSesion() { return this.jugadoresSesion; }
    public Map<Jugador, List<Apuesta>> getJugadorApuestas() { return this.jugadorApuestas; }
    public boolean isNoVaMas() { return this.isNoVaMas; }
    
    // Copia defensiva para persistencia (XML) o cálculos seguros sin bloquear el mapa original
    public Map<Jugador, List<Apuesta>> getCopiaJugadorApuestas() {
        Map<Jugador, List<Apuesta>> foto;
        // Sincronizamos para obtener la "foto" del mapa
        synchronized (jugadorApuestas) {
            foto = new HashMap<>(jugadorApuestas);
        }
        Map<Jugador, List<Apuesta>> copia = new HashMap<>(foto.size());
        for (Map.Entry<Jugador, List<Apuesta>> entry : foto.entrySet()) {
            Jugador jugador = entry.getKey();
            // Sincronizamos la lista específica del jugador para copiarla
            synchronized (entry.getValue()) {
                copia.put(jugador, new ArrayList<>(entry.getValue()));
            }
        }
        return copia;
    }

    // --- CONTROL DE RONDAS Y MENSAJERÍA ---

    /**
     * Cierra la mesa (NO VA MAS).
     * Cambia estado a cerrado y notifica a clientes.
     */
    public void NoVaMas(CountDownLatch latchFinBroadcast) {
        this.isNoVaMas = true;
        
        String msg = "\u001b[31m--- ⛔ NO VA MÁS ⛔ ---\u001b[0m";
        
        // Reutilizamos la lógica común
        enviarBroadcastConcurrente(msg, latchFinBroadcast);
    }

    /**
     * Abre la mesa (VA MAS / ABRIR MESA).
     * Limpia apuestas, cambia estado a abierto y notifica.
     */
    public void resetNoVaMas(CountDownLatch latchFinBroadcast) {
        this.jugadorApuestas.clear();
        this.isNoVaMas = false;

        String msg = "\u001b[32m--- 🟢 ¡HAGAN JUEGO! (ABRIR MESA) ---\u001b[0m";
        
        // La clave "ABRIR MESA" o "VA MAS" debe estar en el mensaje para que el cliente la detecte
        enviarBroadcastConcurrente("ABRIR MESA " + msg, latchFinBroadcast);
    }

    /**
     * Método público para enviar mensajes generales (como el resultado).
     */
    public void broadcastMensaje(String mensaje, CountDownLatch latchFinBroadcast) {
        enviarBroadcastConcurrente(mensaje, latchFinBroadcast);
    }

    /**
     * MÉTODO PRIVADO (HELPER) PARA EVITAR DUPLICIDAD
     * Se encarga de la división de hilos y la sincronización con Barrera.
     */
    private void enviarBroadcastConcurrente(String mensaje, CountDownLatch latchFinExterno) {
        if (jugadoresConexion.isEmpty()) {
            if (latchFinExterno != null) latchFinExterno.countDown();
            return;
        }

        synchronized (this.jugadoresConexion) {
            int N = this.jugadoresConexion.size(); // Total de jugadores conectados
            
            // --- CORRECCIÓN CRÍTICA ---
            // La barrera debe esperar a TODOS los hilos individuales de jugadores (N) 
            // + 1 (este hilo principal que espera al final).
            CyclicBarrier internalBarrier = new CyclicBarrier(N + 1); 

            // El resto de la lógica de partición se mantiene igual para organizar las tareas
            int numHilos = Math.min(N, 30);
            int tamanoChunk = N / numHilos;
            int resto = N % numHilos;
            int indiceInicio = 0;

            for (int i = 0; i < numHilos; i++) {
                int indiceFin = indiceInicio + tamanoChunk + (i < resto ? 1 : 0);
                
                List<Jugador> sublista = new ArrayList<>(this.jugadoresConexion.subList(indiceInicio, indiceFin));
                
                this.poolServer.execute(new mandarMensaje(mensaje, sublista, internalBarrier));

                indiceInicio = indiceFin;
            }

            try {
                // El hilo principal espera aquí hasta que los N hilos de jugadores estén listos para hacer flush
                internalBarrier.await(); 
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            } finally {
                if (latchFinExterno != null) latchFinExterno.countDown();
            }
        }
    }

    // --- GESTIÓN DE JUGADORES (Tu Búsqueda Concurrente Didáctica) ---

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Jugador getJugador(String iD) {
        if (iD == null || iD.trim().isEmpty()) return null;

        synchronized (jugadoresSesion) {
            int N = this.jugadoresSesion.size();
            int particiones = 30;
            int in = 0;
            int fin = 2; // Default pequeño
            
            if (30 < N) fin = N / 30;
            
            int suma = fin;
            
            List<Future<Jugador>> lfj = new ArrayList<>(particiones);
            
            // Lanzamos hilos de búsqueda
            for (int i = 0; i < particiones; i++) {
                // Pasamos copia de la lista para evitar problemas de concurrencia
                lfj.add(this.poolServer.submit(new GetIDHilos(new ArrayList<>(this.jugadoresSesion), in, fin, iD)));
                
                in += suma;
                fin += suma;
                
                if (i == particiones - 2) fin = N; // Ajuste último tramo
            }
            
            Jugador sesion = null;
            
            // Recogemos resultados
            for (Future<Jugador> jug : lfj) {
                try {
                    sesion = jug.get();
                    if (sesion != null) break; // Encontrado
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            
            // Cancelar el resto de tareas si ya lo encontramos para ahorrar recursos
            this.poolServer.execute(new cancelarFuture(lfj));
            return sesion;
        }
    }

    // --- CONEXIÓN Y REGISTRO ---

    /**
     * Establece la conexión guardando el Stream en el Jugador.
     */
    public void establecerConexion(Jugador jug, ObjectOutputStream out) throws IOException {
        synchronized (jug) {
            if (!jug.isSesionIniciada()) {
                jug.setSesionIniciada(true);
                jug.setOutputStream(out); // Guardamos el stream para broadcast
                jugadoresConexion.add(jug);
            } else {
                // Usuario ya conectado -> Rechazamos
                try {
                    out.writeObject("El usuario ya ha iniciado sesion");
                    out.flush();
                } catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    // No cerramos el stream aquí, dejamos que AtenderJugador maneje el cierre
                }
            }
        }
    }

    public Jugador registroSesionDefinitivo(String name, double saldo, ObjectOutputStream out) throws IOException {
        if (name == null || name.trim().isEmpty()) return null;

        synchronized (jugadoresSesion) {
            Jugador jug = this.getJugador(name); // Tu búsqueda concurrente

            if (jug == null) {
                jug = new Jugador(name, saldo);
                this.establecerConexion(jug, out);
                jugadoresSesion.add(jug);
                return jug;
            } else {
                return null; // Ya existe
            }
        }
    }

    public Jugador inicioSesionDefinitivo(String name, ObjectOutputStream out) throws IOException {
        if (name == null || name.trim().isEmpty()) return null;

        synchronized (jugadoresSesion) {
            Jugador jug = this.getJugador(name);

            if (jug != null) {
                this.establecerConexion(jug, out);
            }
            return jug;
        }
    }

    // --- GESTIÓN DE APUESTAS ---

    public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
        // Validación 1: Mesa Cerrada
        if (this.isNoVaMas) return false; 
        
        // Validación 2: Datos inválidos
        if (apuesta == null) return false;

        // Validación 3 y Operación Atómica: Saldo
        synchronized (jug) {
            if (jug.getSaldo() < apuesta.getCantidad()) return false;
            // Restamos saldo inmediatamente
            jug.sumaRestaSaldo(-apuesta.getCantidad());
        }

        // Añadir apuesta al mapa thread-safe
        this.jugadorApuestas.computeIfAbsent(jug, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(apuesta);
        
        return true;
    }

    // --- PREMIOS ---
    public void repartirPremio(Casilla ganadora, CountDownLatch count) {
        // 1. CREAR SNAPSHOT: Copiamos las entradas a un Set seguro.
        // Esto garantiza que el tamaño (.size()) coincida exactamente con los elementos que vamos a recorrer.
        Set<Map.Entry<Jugador, List<Apuesta>>> snapshot;
        
        // Aunque ConcurrentHashMap es thread-safe, hacer esto asegura coherencia entre size e iteración
        synchronized (this.jugadorApuestas) { 
            snapshot = new HashSet<>(this.jugadorApuestas.entrySet());
        }

        if (snapshot.isEmpty()) {
            if (count != null) count.countDown();
            return;
        }

        // 2. Usamos el tamaño del SNAPSHOT para la barrera
        final CyclicBarrier starter = new CyclicBarrier(snapshot.size() + 1);

        // 3. Iteramos sobre el SNAPSHOT (no sobre el mapa original)
        for (Map.Entry<Jugador, List<Apuesta>> entry : snapshot) {
            List<Apuesta> copiaApuestas;
            // Sincronizamos la lista de apuestas individual por seguridad
            synchronized (entry.getValue()) {
                copiaApuestas = new ArrayList<>(entry.getValue());
            }
            // Usamos el pool global
            this.poolServer.execute(new MandarPremios(entry.getKey(), copiaApuestas, ganadora, starter));
        }

        try {
            starter.await(); // Ahora seguro que coinciden
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
            if (count != null) count.countDown();
        }
    }
    // --- DESCONEXIÓN ---

    public void desconectarJugador(Jugador jug) {
        if (jug == null) return;

        synchronized (jug) {
            jug.setSesionIniciada(false);
            
            // Intentamos cerrar recursos asociados al jugador
            try {
                if (jug.getOutputStream() != null) jug.getOutputStream().close();
            } catch (IOException e) { /* Ignorar */ }
            
            // Limpiamos referencias
            jug.setOutputStream(null);
            // Si tuvieras socket en jugador: jug.setConexion(null);
        }

        jugadoresConexion.remove(jug);
        
        // Persistencia en segundo plano
        if (BBDD != null) {
            this.poolServer.execute(new ActualizarBD(new ArrayList<>(this.jugadoresSesion), this.BBDD));
        }
    }
}