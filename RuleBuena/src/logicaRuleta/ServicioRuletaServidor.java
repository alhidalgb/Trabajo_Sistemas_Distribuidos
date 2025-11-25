package logicaRuleta;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class ServicioRuletaServidor {

    private List<Jugador> jugadoresSesion;
    private Map<Jugador, List<Apuesta>> jugadorApuestas;
    private ExecutorService pool;
    
    private CountDownLatch noVaMas;
    private CountDownLatch VaMas;
    
    private volatile boolean isNoVaMas; // IMPORTANTE: volatile para visibilidad entre hilos
    
    public ServicioRuletaServidor() {
        this.noVaMas = new CountDownLatch(1);
        this.VaMas = new CountDownLatch(1);
        this.isNoVaMas = false;
        
        this.jugadoresSesion = new ArrayList<>();
        this.jugadorApuestas = new ConcurrentHashMap<>();
        
        this.pool = Executors.newCachedThreadPool();
    }
    
    // Getters/Setters
    
    public ExecutorService getPool() { return this.pool; }    
    public void setPool(ExecutorService pool) { this.pool = pool; }
    
    public List<Jugador> getListJugadoresSesion() { return this.jugadoresSesion; }
    public void setListJugadoresSesion(List<Jugador> lj) { 
        this.jugadoresSesion = Collections.synchronizedList(lj);
    }
    
    public Map<Jugador, List<Apuesta>> getJugadorApuestas() { return this.jugadorApuestas; }
    public void setJugadorApuestas(Map<Jugador, List<Apuesta>> m) { this.jugadorApuestas = m; }
    
    public Map<Jugador, List<Apuesta>> getCopiaJugadorApuestas() {
        Map<Jugador, List<Apuesta>> copia = new HashMap<>();

        for (Map.Entry<Jugador, List<Apuesta>> entry : this.jugadorApuestas.entrySet()) {
            Jugador jugador = entry.getKey();
            List<Apuesta> listaOriginal = entry.getValue();
            
            synchronized (listaOriginal) {
                copia.put(
                    new Jugador(jugador.getID(), jugador.getSaldo()), 
                    new ArrayList<>(listaOriginal)
                );
            }
        }

        return copia;
    }
    
    // CONTROL DE RONDAS
    
    public void resetNoVaMas() {
    	
        this.jugadorApuestas.clear();
    	this.isNoVaMas = false;
        
        // Crear nuevo pool para la siguiente ronda
        this.pool = Executors.newCachedThreadPool();
        
        this.noVaMas = new CountDownLatch(1);
        this.VaMas.countDown(); // Desbloquear jugadores esperando
    }
    
    public void NoVaMas() {
        this.isNoVaMas = true;
        
        // Apagar el pool de forma ordenada
        this.pool.shutdown();
        
        try {
            // Esperar máximo 3 segundos a que terminen las tareas
            if (!this.pool.awaitTermination(3, TimeUnit.SECONDS)) {
                // Si no terminan, forzar cierre
                this.pool.shutdownNow();
                
                // Dar otra oportunidad
                if (!this.pool.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("⚠️ Pool no se cerró correctamente");
                }
            }
        } catch (InterruptedException e) {
            this.pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        this.noVaMas.countDown(); // Desbloquear jugadores esperando resultado
        this.VaMas = new CountDownLatch(1);
    }
    
    public void noVaMasAwait() throws InterruptedException {
        this.noVaMas.await();
    }
    
    public void VaMasAwait() throws InterruptedException {
        this.VaMas.await();
    }
    
    public boolean isNoVaMas() {
        return isNoVaMas;
    }
    
    // GESTIÓN DE JUGADORES
    
    public Jugador getJugador(String iD) {
    	
        for (Jugador j : this.jugadoresSesion) {
            if (j.getID().equals(iD)) {
                return j;
            }
        }
        return null;
    }
    
    public void establecerConexion(Jugador jug, Socket cliente) {
    	
        if (jug.isSesionIniciada() == false) { 
            jug.setSesionIniciada(true);
            jug.setConexion(cliente);
        } else {
            jug = null;
            
            try {
                OutputStream os = cliente.getOutputStream();
                os.write("El usuario ya ha iniciado sesion\r\n".getBytes());
                os.flush();
            } catch (IOException e) {
            	
            	System.err.println("⚠️ No se ha podido establcer conexion con el cliente");
                e.printStackTrace();
            }
        }
    }
    
    public Jugador registroSesionDefinitivo(String name, double saldo, Socket cliente) {
        Jugador jug = null;
        
        synchronized (this.jugadoresSesion) {
            jug = this.getJugador(name);
            
            if (jug == null) {
                jug = new Jugador(name, saldo);
                this.establecerConexion(jug, cliente);
                this.jugadoresSesion.add(jug);
                return jug;
            } else {
                return null;
            }
        }
    }
    
    public Jugador inicioSesionDefinitivo(String name, Socket cliente) {
        Jugador jug = null;
        
        synchronized (this.jugadoresSesion) {
            jug = this.getJugador(name);
            
            if (jug != null) {
                this.establecerConexion(jug, cliente);
            }
        }
        
        return jug;
    }
    
    // GESTIÓN DE APUESTAS
    
    public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
        
        // NO ACEPTAR APUESTAS SI LA MESA ESTÁ CERRADA
        if (this.isNoVaMas) {
            return false;
        }
        
        boolean add = false;
        
        if (apuesta != null) {
            List<Apuesta> listaDelJugador = 
                this.jugadorApuestas.computeIfAbsent(
                    jug, 
                    k -> Collections.synchronizedList(new ArrayList<>())
                );

            add = listaDelJugador.add(apuesta);
            
            if (add) {
                jug.restarApuesta(apuesta.getCantidad());;
            }
        }
        
        return add;
    }

    public void repartirPremio(Casilla ganadora) {
        
        if (this.jugadorApuestas.isEmpty()) {
            System.out.println("ℹ️ No hay apuestas para repartir.");
            return;
        }
        
        final CyclicBarrier starter = new CyclicBarrier(this.jugadorApuestas.size() + 1);
        ExecutorService poolPremios = Executors.newFixedThreadPool(this.jugadorApuestas.size());
        
        try {
            for (Map.Entry<Jugador, List<Apuesta>> entry : this.jugadorApuestas.entrySet()) {
                poolPremios.execute(new MandarPremios(entry.getKey(),new ArrayList<>(entry.getValue()), ganadora, starter));
            }
            
            // Esperar a que todos terminen
            starter.await();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
            poolPremios.shutdown();
        }
    }
}