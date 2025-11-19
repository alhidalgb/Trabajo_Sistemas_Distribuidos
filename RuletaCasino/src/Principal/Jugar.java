package Principal;

import java.net.Socket;

public class Jugar implements Runnable{

	
	private Socket cliente;
	
	
	public Jugar(Socket cliente) {
		
		this.cliente= cliente;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		boolean seguir=true;
		
		while(seguir) {
			
			
			//LLamada al menu para hacer apuestas (Callable).
			
			
			if(seguir) {
				
				//Obtener Casilla ganadora
				// .readObject - espera a la casilla ganadora
				
				
				//Obtener premio.
				// obtiene las apuestas del jugador y le da los premios.
				
				
			}
			
			
					
			
		}
		
		
		
		
	}

}
