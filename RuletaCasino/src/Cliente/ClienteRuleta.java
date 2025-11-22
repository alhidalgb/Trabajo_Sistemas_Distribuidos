package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ClienteRuleta {

	
	Socket cliente;
	
	public ClienteRuleta(int puerto) {
		
		try {
			this.cliente= new Socket("LOCALHOST",puerto);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	public void IniciarCliente() {
		
		
		try(BufferedReader bis = new BufferedReader(new InputStreamReader(this.cliente.getInputStream()));
				OutputStream os = cliente.getOutputStream();
				BufferedReader bff = new BufferedReader(new InputStreamReader(System.in))){
			
			
			
			while(true) {
				
												
				String msgServidor = bis.readLine();
				
				if(msgServidor.equals("NECESITO RESPUESTA\r\n")) {
										
					os.write((bff.readLine()+"\r\n").getBytes());
					os.flush();
					
				}else {
					
					System.out.println(msgServidor);
					
				}
				
									
				
			}
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	
}
