package Principal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;


public class GirarRueda implements Runnable {

	private List<OutputStream> clientesConectados;
	
	public GirarRueda(List<OutputStream> clientesConectados) {
		
		this.clientesConectados=clientesConectados;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		int numeroGanador = new Random().nextInt(37);
		
		for(OutputStream os : this.clientesConectados) {
			
			
			
			synchronized(this.clientesConectados) {
				
				
				try {
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(new Casilla(numeroGanador));
					os.flush();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
			}
			
			
			
		}
		
		
		
	}

}
