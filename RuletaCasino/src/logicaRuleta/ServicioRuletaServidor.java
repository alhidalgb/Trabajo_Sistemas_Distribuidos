package logicaRuleta;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class ServicioRuletaServidor {

	
	//Lista con todos los jugadores iniciados sesion. Yo cargo esta lista y mientras el servidor siga vivo esta sigue funcionando.
	//Cuando el servidor se desconecte hara todos los cambios necesarios en la BD (archivo xml).
	private List<Jugador> jugadoresSesion;
	
	
	//Los jugadores que han hecho apuestas.
	private Map<Jugador,List<Apuesta>> jugadorApuestas;
	
	
	//private List<Socket> clientesConectados;
	
	
	//private Casilla casillaGanadora;
	
	private CountDownLatch noVaMas;
	private CountDownLatch VaMas;
	
	
	public ServicioRuletaServidor() {
		
		this.noVaMas=new CountDownLatch(1);
		this.VaMas = new CountDownLatch(1);
		
		this.jugadoresSesion=new ArrayList<>();
		this.jugadorApuestas = new ConcurrentHashMap<>();
		
		
		
	}
	
	//Getters/Setters
	
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
		
		this.noVaMas=new CountDownLatch(1);
		this.VaMas.countDown();
		
	}
	
	public void NoVaMas() {
		
		this.noVaMas.countDown();
		this.VaMas=new CountDownLatch(1);
		
	}
	
	public void noVaMasWait() throws InterruptedException {
		this.noVaMas.wait();
	}
	
	public void VaMasWait() throws InterruptedException {
		
		this.VaMas.wait();
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
	
	
	
//Metodo inutil.
	public int anadirJugador(Jugador jug){
		
		if(jugadoresSesion.contains(jug)) {		
			
			return 0;
		}
		
		if(jugadoresSesion.add(jug)) {
			
			return 1;
			
		}
		
		//No ha podido añadir al jugador
		return 2;
	}
	
	
	public void establecerConexion(Jugador jug, Socket cliente) {
		
		
		if(!jug.isSesionIniciada()) { 
			
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
	
	
	public boolean anadirApuesta(Apuesta apuesta) {
		
		
		List<Apuesta> listaDelJugador = 
				this.jugadorApuestas.computeIfAbsent(apuesta.getJugador(), k -> Collections.synchronizedList(new ArrayList<>()));

		    // 3. Añadimos a la lista (ahora es seguro hacerlo desde varios hilos)
		    return listaDelJugador.add(apuesta);
		    
		    
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
		
	
		OutputStream os;
		double ganancia=0.0;

		for (Map.Entry<Jugador, List<Apuesta>> entrada : this.jugadorApuestas.entrySet()) {
			
			ganancia=0.0;
			
			try {
				
				//Obtenemos el outputstream del jugador.
				
				os = entrada.getKey().getConexion().getOutputStream();
			
		

			
				for(Apuesta t : entrada.getValue()) {
					
					 ganancia = this.calcularPremio(ganadora, t) + ganancia;
					
					
			}
			
			
			
					
				os.write(("HAS GANADO: " + ganancia + "€").getBytes());
				os.flush();
				
			}catch(IOException e) {
				
				//Manejar la desconexion
				
			}finally {
				
				
				this.jugadorApuestas.clear();
				
				
			}
			
			
		    
		}
		
		
	}
	
	public double calcularPremio(Casilla ganadora, Apuesta apuesta) {
		
	    double cantidad = apuesta.getCantidad();
	    String valorApostado = apuesta.getValor(); // Ej: "17", "ROJO", "PAR", "1" (1ª docena)

	    switch(apuesta.getTipo()) { // Asumo que getApuesta() devuelve el Enum TIPO

	        case NUMERO:
	            // Convertimos el valor de la apuesta (String) a entero para comparar
	            try {
	                int numeroApostado = Integer.parseInt(valorApostado);
	                if (numeroApostado == ganadora.getNumero()) {
	                    return cantidad * 36; 
	                }
	            } catch (NumberFormatException e) {
	                System.err.println("Error formato numero: " + valorApostado);
	            }
	            break;

	        case COLOR:
	            // Si sale el 0 (VERDE), las apuestas a color suelen perder
	            if (ganadora.getNumero() != 0) {
	                // Comparamos ignorando mayúsculas/minúsculas (ej: "ROJO" vs "Rojo")
	                if (valorApostado.equalsIgnoreCase(ganadora.getColor())) {
	                    return cantidad * 2;
	                }
	            }
	            break;

	        case PAR_IMPAR:
	            // El 0 no se considera ni par ni impar para pagar apuestas (la banca gana)
	            if (ganadora.getNumero() != 0) {
	                boolean apostoPar = valorApostado.equalsIgnoreCase("PAR");
	                // Si apostó PAR y salió PAR, o si apostó IMPAR y salió IMPAR (no par)
	                if ((apostoPar && ganadora.getNumero()%2==0) || (!apostoPar && ganadora.getNumero()%2!=0)) {
	                    return cantidad * 2;
	                }
	            }
	            break;

	        case DOCENA:
	            // Asumimos que valorApostado es "1", "2" o "3"
	            // O que la Casilla tiene un método getDocena() que devuelve 1, 2, 3 (y 0 si es el cero)
	            try {
	                int docenaApostada = Integer.parseInt(valorApostado);
	                if (docenaApostada == ganadora.getDocena()) {
	                    return cantidad * 3;
	                }
	            } catch (NumberFormatException e) {
	                 System.err.println("Error formato docena: " + valorApostado);
	            }
	            break;

	        default: 
	            return 0;
	    }

	    // Si llega aquí es que no entró en ningún IF de ganar, por tanto perdió.
	    return 0;
	}
	
	
}
