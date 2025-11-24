package prueba;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class jale implements Runnable {

	private Socket cliente;
	private CountDownLatch count;
	
	public jale(Socket s,CountDownLatch count) {
		
		this.count=count;
		this.cliente=s;
		
	}
	
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			
			BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true); 
				
				
			
			while(true) {
				
				out.println("NECESITO RESPUESTA");	
				
				String d = in.readLine();
				System.out.print(d);
				
				if(d.equals("f")) {
					this.count.countDown();
				}
				
			}
			
			
			
			
		}catch(IOException e) {e.printStackTrace();}
		
		
		
	}

}
