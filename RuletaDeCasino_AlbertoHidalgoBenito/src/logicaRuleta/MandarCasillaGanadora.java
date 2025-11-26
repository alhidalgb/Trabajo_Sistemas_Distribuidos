
package logicaRuleta;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import modeloDominio.Casilla;

public class MandarCasillaGanadora implements Runnable{

	
	private Socket jug;
	private CyclicBarrier starter;
	private Casilla ganadora;
	
	public MandarCasillaGanadora(Socket s, CyclicBarrier starter, Casilla ganadora) {
		
		this.jug=s;
		this.starter=starter;
		this.ganadora=ganadora;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		PrintWriter os = null;
		try {
			
			 os = new PrintWriter(new OutputStreamWriter(jug.getOutputStream()),true);
			starter.await();
			
			
			
		}catch(IOException | InterruptedException | BrokenBarrierException e) {
			
			//No hacer nada.
			
		}finally {
			
			if(os!=null) {
			os.println("\u001b[32m"+"--- NO VA MAS ---"+"\u001b[0m");
			os.println("\u001b[32m"+"CASILLA GANDORA: " + this.ganadora.toString()+"\u001b[0m");
			}
		}
		
		
	}

}
