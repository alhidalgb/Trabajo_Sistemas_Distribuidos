package logicaRuleta;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class ServicioRuletaServidor {

	
	//Lista con todos los jugadores iniciados sesion. Yo cargo esta lista y mientras el servidor siga vivo esta sigue funcionando.
	//Cuando el servidor se desconecte hara todos los cambios necesarios en la BD (archivo xml).
	private List<Jugador> jugadoresSesion;
	
	
	//Los jugadores que han hecho apuestas.
	private Map<Jugador,List<Apuesta>> jugadorApuestas;
	
	private ExecutorService pool;
	
	private CountDownLatch noVaMas;
	private CountDownLatch VaMas;
	
	
	public ServicioRuletaServidor() {
		
		this.noVaMas=new CountDownLatch(1);
		this.VaMas = new CountDownLatch(1);
		
		this.jugadoresSesion=new ArrayList<>();
		this.jugadorApuestas = new ConcurrentHashMap<>();
		
		this.pool=Executors.newCachedThreadPool();
		
		
	}
	
	//Getters/Setters
	
	public ExecutorService getPool() {return this.pool;}	
	public void setPool(ExecutorService pool) {this.pool=pool;}
	
	public List<Jugador> getListJugadoresSesion(){return this.jugadoresSesion;}
	public void setListJugadoresSesion(List<Jugador> lj) {this.jugadoresSesion= Collections.synchronizedList(lj);}
	
	public Map<Jugador,List<Apuesta>> getJugadorApuestas(){return this.jugadorApuestas;}
	public void setJugadorApuestas(Map<Jugador,List<Apuesta>> m) {this.jugadorApuestas=m;}
	
	public Map<Jugador, List<Apuesta>> getCopiaJugadorApuestas( ) {
	    Map<Jugador, List<Apuesta>> copia = new HashMap<>();

	    // Recorremos el mapa original (thread-safe gracias a ConcurrentHashMap)
	    for (Map.Entry<Jugador, List<Apuesta>> entry : this.jugadorApuestas.entrySet()) {
	        
	        Jugador jugador = entry.getKey();
	        List<Apuesta> listaOriginal = entry.getValue();
	        
	        // IMPORTANTE: Si la lista original es una synchronizedList (como sugerí antes),
	        // debes sincronizar para copiarla atómicamente y evitar errores.
	        synchronized (listaOriginal) {
	        	
	            // Creamos una NUEVA ArrayList con los contenidos de la vieja
	            copia.put(new Jugador(jugador.getID(),jugador.getSaldo()), new ArrayList<>(listaOriginal));
	        }
	    }

	    return copia;
	}
	
	//BLOQUEADOR APUESTAS CADA MARCADO POR EL GIRAR RULETITA

	public void resetNoVaMas() {
		
		this.jugadorApuestas.clear();
		this.pool=Executors.newCachedThreadPool();
		this.noVaMas=new CountDownLatch(1);
		this.VaMas.countDown();
		
	}
	
	public void NoVaMas() {
		
		this.pool.shutdownNow();
		this.noVaMas.countDown();
		this.VaMas=new CountDownLatch(1);
		
	}
	
	public void noVaMasAwait() throws InterruptedException {
		this.noVaMas.await();
		
	}
	
	public void VaMasAwait() throws InterruptedException {
		
		this.VaMas.await();
	}
	
	
	//------------------------
	
	
	
	//METODOS USADOS PARA INICIAR/REGISTRAR SESION, (necesario un cuidado con synchronized)
	
	
	public Jugador getJugador(String iD) {
		
		
		//Hacerlo en varios hilos
		
		for(Jugador j : this.jugadoresSesion) {
			
			if(j.getID().equals(iD)) {
				
				return j;
				
			}
			
			
		}
		
		return null;
		
		
	}
	
	

	
	
	public void establecerConexion(Jugador jug, Socket cliente) {
		
		
		if(jug.isSesionIniciada()==false) { 
			
			jug.setSesionIniciada(true);
			jug.setConexion(cliente);
			
		}else {
			
			jug=null;
			
			try {
				OutputStream os = cliente.getOutputStream();
				os.write("El usuario ya ha iniciado sesion".getBytes());
				os.write("\r\n".getBytes());
				os.flush();
			}catch(IOException e) {
				
				//Propagar excepcion??
				
			}
			
		}
				
		
	}
	

	
	public Jugador registroSesionDefinitivo(String name,double saldo, Socket cliente) {
		
		
		Jugador jug = null;
		
		synchronized (this.jugadoresSesion) {
			
			
			jug = this.getJugador(name);
			
			if(jug==null) {
				
				jug = new Jugador(name,saldo);
				this.establecerConexion(jug, cliente);
				this.jugadoresSesion.add(jug);
				return jug;
				
			}else {return null;}
				   
		}
		
		
		
		
	}
	
	public Jugador inicioSesionDefinitivo(String name, Socket cliente) {
		
		Jugador jug = null;
		
		synchronized (this.jugadoresSesion) {
			
			
			jug = this.getJugador(name);
			
			if(jug!=null) {
				
				this.establecerConexion(jug, cliente);
				
			}
				   
		}
		
		
		
		return jug;
	
		
	}
	
	
	//--------------------------------------------------------------
	
	
	public boolean anadirApuesta(Jugador jug, Apuesta apuesta) {
		
		boolean add = false;
		
		if(apuesta!=null) {
			
			List<Apuesta> listaDelJugador = 
					this.jugadorApuestas.computeIfAbsent(jug, k -> Collections.synchronizedList(new ArrayList<>()));

			    // 3. Añadimos a la lista (ahora es seguro hacerlo desde varios hilos)
			    add = listaDelJugador.add(apuesta);
			    
			if(add) {
				
				jug.setSaldo(jug.getSaldo()-apuesta.getCantidad());
				
			}
			    
			
		}
		
		return add;
		
		    
		    
		/*
		if(this.jugadorApuestas.get(apuesta.getJugador())==null) {
			
			List<Apuesta> t = new ArrayList<>();
			
			if(t.add(apuesta)) {
				
				this.jugadorApuestas.put(apuesta.getJugador(),t);
				return true;
			}else {
				
				return false;
			}
			
			
			
		}else {
			
			List<Apuesta> t = this.jugadorApuestas.get(apuesta.getJugador());
			
			return t.add(apuesta);
			
		}
		
		/*/
		
		
	}


	

	public void repartirPremio(Casilla ganadora) {
		
		
		
		final CyclicBarrier starter = new CyclicBarrier(this.jugadorApuestas.size()+1);
		ExecutorService pool = Executors.newFixedThreadPool(this.jugadorApuestas.size());
		try {
			
			 for (Map.Entry<Jugador, List<Apuesta>> entry : this.jugadorApuestas.entrySet()) {
			    	
				    
			    	pool.execute(new MandarPremios(entry.getKey().getConexion().getOutputStream(),new ArrayList<>(entry.getValue()), ganadora, starter));
		    		 
			    		    	
			    	
			    }
			 
				starter.await();

			
		}catch(IOException e) {e.printStackTrace();} 
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	   
	    
	   
		
		
		
	}
	

	
	
}
