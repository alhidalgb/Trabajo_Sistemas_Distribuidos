package Principal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.*;

public class ServidorRuleta {

	public static List<OutputStream> clientesConectados = Collections.synchronizedList(new ArrayList<>());
	
	
	public void IniciarServidor(int puerto) {
		
		
		
		try(ServerSocket server = new ServerSocket(puerto);
			ExecutorService pool = Executors.newCachedThreadPool();
				// 1. INICIAR EL TEMPORIZADOR GLOBAL (El crupier automático)
		    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);){
		
			
	        
	        // Programamos la tarea "RondaDeJuego" para que ocurra cada 30 segundos
	        // Parametros: Tarea, retardo inicial, periodo, unidad de tiempo
	        scheduler.scheduleAtFixedRate(new GirarRueda(clientesConectados), 0, 30, TimeUnit.SECONDS);
			
			
		
			
				
			
			while(true) {
				
				
				try {
					
					Socket cliente = server.accept();
					clientesConectados.add(cliente.getOutputStream());
					
					
					
					
					
				}catch(IOException e) {e.printStackTrace();}
				
				
				
			}
			
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	
	
	
}
