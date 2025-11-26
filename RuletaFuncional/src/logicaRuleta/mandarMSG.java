package logicaRuleta;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class mandarMSG implements Runnable{

	private Socket socket;
	private String msg;
	private CyclicBarrier starter;
	
	public mandarMSG(Socket s, String msg, CyclicBarrier starter) {
		
		this.socket=s;
		this.msg=msg;
		this.starter=starter;
		
		
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		PrintWriter os=null;
		
		
		
		try {
			
			os = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			starter.await(1,TimeUnit.MINUTES);
			
			
		}catch(IOException e) {
			
			System.err.println("No se pudo mandar el mensaje");
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			
			if(os!=null && !msg.trim().isEmpty()) {
				
				os.println(msg);
				
				
			}
			
		}
		
	}

}
