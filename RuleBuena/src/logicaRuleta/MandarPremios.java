package logicaRuleta;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class MandarPremios implements Runnable{

	private List<Apuesta> listApuesta;
	private Casilla ganadora;
	private CyclicBarrier starter;
	private Jugador jugador;
	
	public MandarPremios(Jugador jug, List<Apuesta> listApuesta, Casilla ganadora,CyclicBarrier starter) {
		
		this.ganadora=ganadora;
		this.listApuesta=listApuesta;
		this.starter=starter;
		this.jugador=jug;
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		double ganancia = 0.0;	
		PrintWriter os=null;
		
		
		
		//Calculo las ganacias a añadir.
		for(Apuesta ap:this.listApuesta) {
					
			ganancia = this.calcularPremio(ganadora, ap)+ganancia;
					
		}
		
		//Intento establcer conexion con el jugador.
		try {
			
			os= new PrintWriter(new OutputStreamWriter(this.jugador.getConexion().getOutputStream()),true);
			
		}catch(IOException e) {
			
			//Si se ha caido la conexion mientras se repartian los premios, aun asi estos se guardan.
			os=null;
			
		}
		
			
		//Hago que los hilos esperen y si se produce una excepcion no pasa nada, mandamos la gananciai y listo.
		try {
			
			starter.await();
			
		}catch(InterruptedException | BrokenBarrierException e){
			
		}finally {
			
			
			//Esto quiero que se haga siempre pase lo que pase.
			
			
			
			jugador.sumarGanancia(ganancia);			
			if(os!=null) {
				
				os.println("CASILLA GANADORA: "+ ganadora.toString());
				os.println("HAS GANADO: " +ganancia + "€");}	
			
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
