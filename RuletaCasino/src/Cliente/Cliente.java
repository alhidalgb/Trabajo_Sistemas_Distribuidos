package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class Cliente {

	
	Socket cliente;
	
	public Cliente(Socket cliente) {
		
		this.cliente=cliente;
		
		
	}
	
	
	public void IniciarCliente() {
		
		
		try(BufferedReader bis = new BufferedReader(new InputStreamReader(this.cliente.getInputStream()));
				OutputStream os = cliente.getOutputStream();
				BufferedReader bff = new BufferedReader(new InputStreamReader(System.in))){
			
			
			
			while(true) {
				
				
				System.out.println(bis.readLine());
				os.write((bff.readLine()+"\r\n").getBytes());
				os.flush();
				
				
			}
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	
}
