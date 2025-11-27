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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class ServicioRuletaServidor {

	private final ExecutorService poolServer;
	private final List<Jugador> jugadoresSesion;
    private Map<Jugador, List<Apuesta>> jugadorApuestas;
    
    //De eseta lista solo voy a añadir y borrar, entoces con haccerla threadSafe ya no me tengo que preocupar de hacer synchronized los metodos.
    private List<Jugador> jugadoresConexion;
    
    //private ExecutorService pool;
    
    private CountDownLatch noVaMas;
    private CountDownLatch VaMas;
    
    private boolean isNoVaMas; 
    
    
    public ServicioRuletaServidor(List<Jugador> jugadoresSesion, ExecutorService pool) {
        this.noVaMas = new CountDownLatch(1);
        this.VaMas = new CountDownLatch(1);
        this.isNoVaMas = false;

        this.jugadoresSesion = Collections.synchronizedList(jugadoresSesion);
        this.jugadorApuestas = new ConcurrentHashMap<>();
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        this.poolServer=pool;
    }
    
    public ServicioRuletaServidor() {
        this.noVaMas = new CountDownLatch(1);
        this.VaMas = new CountDownLatch(1);
        this.isNoVaMas = false;

        this.jugadoresSesion = Collections.synchronizedList(new ArrayList<>());
        this.jugadorApuestas = new ConcurrentHashMap<>();
        this.jugadoresConexion = Collections.synchronizedList(new ArrayList<>());
        this.poolServer=Executors.newCachedThreadPool();
    }

    
    // Getters/Setters
    
    //public ExecutorService getPool() { return this.pool; }    
    //public void setPool(ExecutorService pool) { this.pool = pool; }
    
    public List<Jugador> getListJugadoresSesion() { return this.jugadoresSesion; }
    
    
    public Map<Jugador, List<Apuesta>> getJugadorApuestas() { return this.jugadorApuestas; }
    public void setJugadorApuestas(Map<Jugador, List<Apuesta>> m) { this.jugadorApuestas = m; }
    
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

    
    // CONTROL DE RONDAS
    public void resetNoVaMas() {
        // Limpiar apuestas de la ronda
        this.jugadorApuestas.clear();
        this.isNoVaMas = false;

        // Reiniciar latch de noVaMas
        this.noVaMas = new CountDownLatch(1);

        // Desbloquear jugadores que estaban esperando VaMas
        if (this.VaMas != null) {
            this.VaMas.countDown();
        }
    }

    public void NoVaMas() {
        this.isNoVaMas = true;

        // Desbloquear jugadores esperando resultado
        if (this.noVaMas != null) {
            this.noVaMas.countDown();
        }

        // Reiniciar latch de VaMas para la siguiente ronda
        this.VaMas = new CountDownLatch(1);
    }

    public void noVaMasAwait() throws InterruptedException {
        if (this.noVaMas != null) {
            this.noVaMas.await();
        }
    }

    public void VaMasAwait() throws InterruptedException {
        if (this.VaMas != null) {
            this.VaMas.await();
        }
    }

    public boolean isNoVaMas() {
        return this.isNoVaMas;
    }

    
    // GESTIÓN DE JUGADORES
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Jugador getJugador(String iD) {
        // Caso de entrada inválida: id nulo o vacío
        if (iD == null || iD.trim().isEmpty()) {
            
            // Devolvemos null silencioso → el llamador abortará la conexión
            return null;
        }

        
        
        synchronized (jugadoresSesion) {
        	     	
        	int N = this.jugadoresSesion.size();
        	int particiones = 30;
        	
        	    	
        	int in=0;
        	int fin = 2;  
        	
        	if(30<N) {fin=N/30;}
        	int suma = fin;
        	
        	List<Future<Jugador>> lfj=new ArrayList<>(particiones);
        	
			for(int i=0;i<particiones;i++) {
				
				
				//Super importante mandar una copia, si mando la referencia original al estar en un bloque synchronize se bloquea todo.
				//Mando un copia, como esta en un bloque sincrono de la lista no hya porblema, no se cambiara mientras la utilizo,
				
				lfj.add(this.poolServer.submit(new GetIDHilos(new ArrayList<>(this.jugadoresSesion),in,fin,iD)));
				
				
				in +=suma;
				fin += suma;
				
				if(fin>N) {fin=N;}
				
				
			}
			
			Jugador sesion =null;
			
			for(Future<Jugador> jug : lfj) {
				
				try {
					sesion=jug.get();
					if(sesion!=null) {
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
							
				
			}
        	
			//Cerramos los future para ahorra CPU.
			this.poolServer.execute(new cancelarFuture(lfj));		
			return sesion;
        	      
        }
    }


    
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
                this.desconectarJugador(jug); // aquí llamamos al método de la rule
            }
        }
    }

    
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

    
    // GESTIÓN DE APUESTAS
    
    public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
        // NO ACEPTAR APUESTAS SI LA MESA ESTÁ CERRADA
        if (this.isNoVaMas) {
            return false;
        }

        if (apuesta == null) {
            return false;
        }

        //Hace falta que sea synchronized, ¿es logica esta siincronizacion?
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


    public void repartirPremio(Casilla ganadora) {
        if (this.jugadorApuestas.isEmpty()) {
            //System.out.println("ℹ️ No hay apuestas para repartir.");
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

    
    
    public void mandarCasilla(Casilla ganadora) {
        if (this.jugadoresConexion.isEmpty()) {
            return;
        }

        synchronized (this.jugadoresConexion) {
            // Snapshot del tamaño actual → si alguien se conecta después, no participa en esta ronda
            final CyclicBarrier starter = new CyclicBarrier(this.jugadoresConexion.size() + 1);
            ExecutorService poolCasilla = Executors.newFixedThreadPool(this.jugadoresConexion.size());

            try {
                for (Jugador j : this.jugadoresConexion) {
                    // Mantengo tu clase MandarCasillaGanadora
                    poolCasilla.execute(new MandarCasillaGanadora(j.getConexion(), starter, ganadora));
                }

                // Esperar a que todos los hilos lleguen a la barrera
                starter.await();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restaurar estado de interrupción
            } catch (BrokenBarrierException e) {
                System.err.println("⚠️ Error en la barrera de casillas: " + e.getMessage());
            } finally {
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
        this.poolServer.execute(new ActualizarBD(new ArrayList<>(this.jugadoresSesion),"jugadores.xml"));        
       
    }
    
    
    public void desconectarTodo() {
    	
    	
    	//Pendiente de pensar, hay que teneer en cuenta los flags, isInterrupt().
    	
    	
    }

    
    
}