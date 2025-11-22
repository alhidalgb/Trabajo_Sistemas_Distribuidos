package Cliente;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ClienteRuleta {

	
	private Socket cliente;
	
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
				DataOutputStream os = new DataOutputStream(cliente.getOutputStream());
				BufferedReader bff = new BufferedReader(new InputStreamReader(System.in))) {
			
			
			//SI USO OUTPUTSTREAM NO FUNCIONA, TENGO QUE USAR UN DATAINPUTSTERAM ¿PORQUE?
			
			while(true) {
				
				
				String msgServer = bis.readLine();
				
				if(msgServer.equals("NECESITO RESPUESTA\r\n")){
					
					
					os.writeBytes(bff.readLine());
					os.flush();
					
				}else {
					
				}
				
				
				
			}
			
			
			
		}catch(IOException e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	
	
}
