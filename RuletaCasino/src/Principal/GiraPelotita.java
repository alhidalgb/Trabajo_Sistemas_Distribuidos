package Principal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class GiraPelotita implements Runnable {

	private List<Socket> clientesConectados;
	
	private Map<Jugador,Socket> jugadoresConectados = new ConcurrentHashMap<>();
	
	
	private ServicioRuleta rule;
	
	
	
	
	//SOLO LE PASA EL SERVICIORULETA Y CADA X SEGUNDOS HACE LAS COSAS.
	public GiraPelotita(List<Socket> clientesConectados, ServicioRuleta rule) {
		
		this.clientesConectados=clientesConectados;
		this.rule=rule;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		int numeroGanador = new Random().nextInt(37);
		
		for(Socket cl : this.clientesConectados) {
				
			synchronized(this.clientesConectados) {
				
				
				try {
					
					OutputStream os = cl.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(new Casilla(numeroGanador));
					oos.flush();
					oos.reset();
					
				} catch (IOException e) {
					clientesConectados.remove(cl);
				}
				
				
				
			}
			
			
			
		}
	
		
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
