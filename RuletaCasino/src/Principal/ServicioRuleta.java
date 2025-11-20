package Principal;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ServicioRuleta {

	
	//Lista con todos los jugadores iniciados sesion.
	private List<Jugador> jugadores;
	
	private Map<Jugador, Socket> jugadorSocket = new ConcurrentHashMap<>();
	
	
	
	private List<Apuesta> apuestas = new ArrayList<>();
	private List<Socket> clientesConectados;
	
	
	private Casilla casillaGanadora;
	
	private CountDownLatch noVaMas = new CountDownLatch(1);
	private CountDownLatch VaMas = new CountDownLatch(1);
	
	
	
	
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
	
	public void setCasilla(Casilla casilla) {
		
		this.casillaGanadora=casilla;
		
	}
	
	
	public int anadirJugador(Jugador jug, Socket cliente){
		
		if(jugadores.contains(jug)) {		
			
			return 0;
		}
		
		if(jugadores.add(jug)) {
			
			return 1;
			
		}
		
		//No ha podido añadir al jugador
		return 2;
	}
	
	
	public void establecerConexion(Jugador jug, Socket cliente) {
		
		this.jugadorSocket.put(jug, cliente);
		
	}
	
	
	public boolean anadirApuesta(Apuesta apuesta) {
		
		return apuestas.add(apuesta);
		
	}


	public Jugador getJugador(String iD) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	public void repartirPremio(Casilla ganadora) {
		
		
		
		
		
	}
	
	public double calcularPremio(Casilla ganadora, Apuesta apuesta) {
		
	    double cantidad = apuesta.getCantidad();
	    String valorApostado = apuesta.getValor(); // Ej: "17", "ROJO", "PAR", "1" (1ª docena)

	    switch(apuesta.getApuesta()) { // Asumo que getApuesta() devuelve el Enum TIPO

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
