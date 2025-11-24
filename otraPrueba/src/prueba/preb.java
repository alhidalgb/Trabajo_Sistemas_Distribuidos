package prueba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class preb {

	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		try(ServerSocket server = new ServerSocket(8000)){
			
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			ExecutorService pool = Executors.newCachedThreadPool();
		        
			CountDownLatch count = new CountDownLatch(1);
			
		     //scheduler.scheduleAtFixedRate(new bajarContador(count), 0, 30, TimeUnit.SECONDS);

				
				
				
				
				while(true) {
					
					
					try {
						
						Socket cliente = server.accept();
						pool.execute(new jale(cliente, count));
						
						BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
			            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true); 
						
						
						count.wait();
						
						pool.shutdownNow();
						
						out.println("Matraca?");
						in.readLine();
						
						
						
					}catch(IOException e) {e.printStackTrace();} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					
				}
				
				
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
	}

}
