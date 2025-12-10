package logicaRuleta.core;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.*;

import logicaRuleta.concurrencia.getIDHilos;
import logicaRuleta.concurrencia.mandarPremios;
import logicaRuleta.concurrencia.cancelarFuture;
import logicaRuleta.concurrencia.mandarMensaje;
import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;
import servidor.persistencia.ActualizarBD;

/**
 * Clase ServicioRuleta
 * --------------------
 * Gestor central del estado del juego y la comunicación con los clientes.
 * Maneja la concurrencia de conexiones, apuestas y reparto de premios.
 * * Responsabilidades:
 * 1. Mantener la lista de jugadores conectados y sus sesiones.
 * 2. Gestionar el estado de la mesa (Abierta/Cerrada).
 * 3. Coordinar el envío de mensajes masivos (Broadcast).
 * 4. Repartir premios de forma concurrente.
 */
public class ServicioRuleta {

    // --- ATRIBUTOS ---
    
    // Pool de hilos para tareas asíncronas (búsquedas, mensajes, premios)
    private final ExecutorService poolServer;
    
    // Listas thread-safe para gestión de usuarios
    private final List<Jugador> jugadoresSesion;   // Todos los registrados en memoria
    private final List<Jugador> jugadoresConexion; // Solo los que tienen socket activo
    
    // Mapa de apuestas (ConcurrentHashMap para permitir escrituras simultáneas rápidas)
    private final Map<Jugador, List<Apuesta>> jugadorApuestas;

    // Estado de la mesa (volatile para visibilidad inmediata entre hilos)
    private volatile boolean isNoVaMas; 
    
    // Referencia al archivo de persistencia
    private final File BBDD;

    // --- CONSTRUCTORES ---

    /**
     * Constructor principal.
     * @param jugadoresSesion Lista inicial de jugadores cargada de BD.
     * @param pool ExecutorService para gestión de hilos.
     * @param BBDD Archivo físico para persistencia.
     */
    public ServicioRuleta(List<Jugador> jugadoresSesion, ExecutorService pool, File BBDD) {
        this.isNoVaMas = false; // La mesa empieza abierta ("Hagan juego")

        // Usamos listas sincronizadas para evitar corrupciones básicas
        this.jugadoresSesion = Collections.synchronizedList(jugadoresSesion);
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        this.jugadorApuestas = new ConcurrentHashMap<>();
        
        this.poolServer = pool;
        this.BBDD = BBDD;
    }
    
    /**
     * Constructor por defecto (para pruebas).
     */
    public ServicioRuleta() {
        this(new ArrayList<>(), Executors.newCachedThreadPool(), null);
    }

    // --- GETTERS ---

    public List<Jugador> getListJugadoresSesion() { return this.jugadoresSesion; }
    public Map<Jugador, List<Apuesta>> getJugadorApuestas() { return this.jugadorApuestas; }
    public boolean isNoVaMas() { return this.isNoVaMas; }
    
    /**
     * Genera una instantánea segura (Snapshot) de las apuestas actuales.
     * Útil para guardar en XML o inspeccionar sin bloquear el mapa original.
     * @return Copia profunda del mapa de apuestas.
     */
    public Map<Jugador, List<Apuesta>> getCopiaJugadorApuestas() {
        Map<Jugador, List<Apuesta>> foto;
        // Bloqueo breve para copiar la estructura del mapa
        synchronized (jugadorApuestas) {
            foto = new HashMap<>(jugadorApuestas);
        }
        
        // Copia profunda de las listas de apuestas
        Map<Jugador, List<Apuesta>> copia = new HashMap<>(foto.size());
        for (Map.Entry<Jugador, List<Apuesta>> entry : foto.entrySet()) {
            Jugador jugador = entry.getKey();
            synchronized (entry.getValue()) {
                copia.put(jugador, new ArrayList<>(entry.getValue()));
            }
        }
        return copia;
    }

    // --- CONTROL DE RONDAS Y MENSAJERÍA ---

    /**
     * Cierra la mesa para impedir nuevas apuestas.
     * @param latchFinBroadcast Latch para notificar cuando se termine de avisar a todos.
     */
    public void NoVaMas(CountDownLatch latchFinBroadcast) {
        this.isNoVaMas = true;
        String msg = "\u001b[31m--- ⛔ NO VA MÁS ⛔ ---\u001b[0m";
        enviarBroadcastConcurrente(msg, latchFinBroadcast);
    }

    /**
     * Abre la mesa y limpia las apuestas anteriores.
     * @param latchFinBroadcast Latch para notificar fin del broadcast.
     */
    public void resetNoVaMas(CountDownLatch latchFinBroadcast) {
        this.jugadorApuestas.clear();
        this.isNoVaMas = false;
        String msg = "\u001b[32m--- 🟢 ¡HAGAN JUEGO! (ABRIR MESA) ---\u001b[0m";
        enviarBroadcastConcurrente("ABRIR MESA " + msg, latchFinBroadcast);
    }

    /**
     * Envía un mensaje a todos los jugadores conectados de forma concurrente.
     * Divide la lista de jugadores en chunks y asigna hilos para el envío.
     * * @param mensaje Texto a enviar.
     * @param latchFinExterno Latch opcional para sincronizar con el hilo principal.
     */
    public void enviarBroadcastConcurrente(String mensaje, CountDownLatch latchFinExterno) {
        if (jugadoresConexion.isEmpty()) {
            if (latchFinExterno != null) latchFinExterno.countDown();
            return;
        }

        synchronized (this.jugadoresConexion) {
            int N = this.jugadoresConexion.size(); 
            
            // Limitamos a 30 hilos máximo para no saturar el pool
            int numHilos = Math.min(N, 30);
            
            // IMPORTANTE: La barrera espera a (Total Jugadores + 1).
            // Esto es porque 'mandarMensaje' lanza internamente un hilo por jugador.
            CyclicBarrier internalBarrier = new CyclicBarrier(N + 1); 

            int tamanoChunk = N / numHilos;
            int resto = N % numHilos;
            int indiceInicio = 0;

            for (int i = 0; i < numHilos; i++) {
                int indiceFin = indiceInicio + tamanoChunk + (i < resto ? 1 : 0);
                
                List<Jugador> sublista = new ArrayList<>(this.jugadoresConexion.subList(indiceInicio, indiceFin));
                
                // Lanzamos la tarea de distribución
                this.poolServer.execute(new mandarMensaje(mensaje, sublista, internalBarrier));

                indiceInicio = indiceFin;
            }

            try {
                // Esperamos a que todos los mensajes se hayan enviado
                internalBarrier.await(); 
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            } finally {
                if (latchFinExterno != null) latchFinExterno.countDown();
            }
        }
    }

    // --- GESTIÓN DE JUGADORES (BÚSQUEDA CONCURRENTE) ---

    /**
     * Busca un jugador por ID utilizando paralelismo.
     * @param iD Identificador del jugador.
     * @return Objeto Jugador si existe, null si no.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Jugador getJugador(String iD) {
        if (iD == null || iD.trim().isEmpty()) return null;
        
        List<Future<Jugador>> lfj;
        Jugador sesion = null;
        
        // FASE 1: Preparación (Sincronizada)
        // Solo bloqueamos para copiar la lista y lanzar tareas. Es muy rápido.
        synchronized (jugadoresSesion) {
            if (this.jugadoresSesion.isEmpty()) return null;
            
            int N = this.jugadoresSesion.size();
            int numHilos = Math.min(N, 30);
            
            int tamanoChunk = N / numHilos;
            int resto = N % numHilos;
            int indiceInicio = 0;
            
            lfj = new ArrayList<>(numHilos);
            
            for (int i = 0; i < numHilos; i++) {
                int indiceFin = indiceInicio + tamanoChunk + (i < resto ? 1 : 0);
                
                // Creamos copia segura del subsegmento para el hilo
                List<Jugador> copiaSublista = new ArrayList<>(this.jugadoresSesion.subList(indiceInicio, indiceFin));
                
                // Lanzamos búsqueda sin índices, pasamos la sublista directa
                lfj.add(this.poolServer.submit(new getIDHilos(copiaSublista, iD)));
                
                indiceInicio = indiceFin;
            }
        } // Fin synchronized: liberamos el servidor para otros usuarios
        
        // FASE 2: Recogida de resultados (Sin bloqueo global)
        for (Future<Jugador> jug : lfj) {
            try {
                sesion = jug.get(); // Esperamos resultado
                if (sesion != null) break; // Encontrado
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        // Cancelamos tareas pendientes para ahorrar recursos
        this.poolServer.execute(new cancelarFuture(lfj));
        return sesion;
    }

    // --- CONEXIÓN Y REGISTRO ---

    /**
     * Asocia un stream de salida a un jugador existente.
     * Evita dobles conexiones.
     */
    public void establecerConexion(Jugador jug, ObjectOutputStream out) throws IOException {
        synchronized (jug) {
            if (!jug.isSesionIniciada()) {
                jug.setSesionIniciada(true);
                jug.setOutputStream(out); 
                jugadoresConexion.add(jug);
            } else {
                // Usuario ya conectado: Rechazamos la NUEVA conexión.
                // NO desconectamos al usuario antiguo.
                try {
                    out.writeObject("El usuario ya ha iniciado sesion");
                    out.flush();
                } catch(IOException e) {
                    e.printStackTrace();
                } 
            }
        }
    }

    /**
     * Registra un nuevo jugador si no existe.
     */
    public Jugador registroSesionDefinitivo(String name, double saldo, ObjectOutputStream out) throws IOException {
        if (name == null || name.trim().isEmpty()) return null;

        // Bloqueamos la lista global porque vamos a escribir en ella
        synchronized (jugadoresSesion) {
            // Reutilizamos la búsqueda concurrente (seguro porque tenemos el lock)
            Jugador jug = this.getJugador(name); 

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

    /**
     * Inicia sesión de un jugador existente.
     */
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

    /**
     * Valida y registra una apuesta.
     * @return true si la apuesta fue aceptada, false si no (saldo insuficiente o mesa cerrada).
     */
    public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
        if (this.isNoVaMas) return false; 
        if (apuesta == null) return false;

        synchronized (jug) {
            if (jug.getSaldo() < apuesta.getCantidad()) return false;
            jug.sumaRestaSaldo(-apuesta.getCantidad());
        }

        this.jugadorApuestas.computeIfAbsent(jug, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(apuesta);
        
        return true;
    }

    // --- PREMIOS ---
    
    /**
     * Calcula y reparte los premios de la ronda.
     * Utiliza una barrera para sincronizar el envío de resultados.
     */
    public void repartirPremio(Casilla ganadora, CountDownLatch count) {
        Set<Map.Entry<Jugador, List<Apuesta>>> snapshot;
        
        // Snapshot seguro para iterar
        synchronized (this.jugadorApuestas) { 
            snapshot = new HashSet<>(this.jugadorApuestas.entrySet());
        }

        if (snapshot.isEmpty()) {
            if (count != null) count.countDown();
            return;
        }

        // Barrera: Espera a (Jugadores con apuestas + 1 hilo principal)
        final CyclicBarrier starter = new CyclicBarrier(snapshot.size() + 1);

        for (Map.Entry<Jugador, List<Apuesta>> entry : snapshot) {
            List<Apuesta> copiaApuestas;
            synchronized (entry.getValue()) {
                copiaApuestas = new ArrayList<>(entry.getValue());
            }
            this.poolServer.execute(new mandarPremios(entry.getKey(), copiaApuestas, ganadora, starter));
        }

        try {
            // Esperamos a que todos calculen sus premios
            starter.await(); 
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
            if (count != null) count.countDown();
        }
    }

    // --- DESCONEXIÓN ---

    /**
     * Cierra la sesión de un jugador y guarda estado en BD.
     */
    public void desconectarJugador(Jugador jug) {
        if (jug == null) return;

        synchronized (jug) {
            jug.setSesionIniciada(false);
            
            try {
                if (jug.getOutputStream() != null) jug.getOutputStream().close();
            } catch (IOException e) { /* Ignorar */ }
            
            jug.setOutputStream(null);
        }

        jugadoresConexion.remove(jug);
        
        if (BBDD != null) {
            this.poolServer.execute(new ActualizarBD(new ArrayList<>(this.jugadoresSesion), this.BBDD));
        }
    }
}