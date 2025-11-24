package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import logicaRuleta.*;
import modeloDominio.Jugador;

import java.util.*;


public class ServidorRuleta {

	
	public void IniciarServidor(int puerto, String historial, String bd) {
		
		
		
		XMLServidor xml = new XMLServidor(historial);
		
		List<Jugador> jugadoresConSesion = BDJugadores.UnmarshallingJugadores(bd); //Aqui se hace el unmarshalling
		
		System.out.println("hola");

		for(Jugador j : jugadoresConSesion) {
			
			System.out.println(j.toString());
			System.out.println("hola1");

		}
		
		
		ServicioRuletaServidor rule = new ServicioRuletaServidor();
		rule.setListJugadoresSesion(jugadoresConSesion);
		
		try(ServerSocket server = new ServerSocket(puerto);){
			
			
			
			ExecutorService pool = Executors.newCachedThreadPool();
			
			// 1. INICIAR EL TEMPORIZADOR GLOBAL (El crupier automático)
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	        
	        // Programamos la tarea "RondaDeJuego" para que ocurra cada 30 segundos
	        // Parametros: Tarea, retardo inicial, periodo, unidad de tiempo
	        scheduler.scheduleAtFixedRate(new GiraPelotita(rule,pool,xml), 0, 60, TimeUnit.SECONDS);
			
			
			
			
			while(true) {
				
				
				try {
					
					Socket cliente = server.accept();
					pool.execute(new AtenderJugador(cliente,rule));
					
					
					
					
					
					
				}catch(IOException e) {e.printStackTrace();}
				
				
				
			}
			
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			
			BDJugadores.MarshallingJugadores(jugadoresConSesion, bd);
			
		}
		
		
	}
	
	
	
	
	
}
