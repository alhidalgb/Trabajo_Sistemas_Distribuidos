package logicaRuleta.concurrencia;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import modeloDominio.Jugador;

public class mandarMensaje implements Runnable{

	
	private final String msg;
	private final List<Jugador> jugadores;
	private final CyclicBarrier starter;
	
	
	public mandarMensaje(String msg, List<Jugador> jugadores, CyclicBarrier starter) {
		super();
		this.msg = msg;
		this.jugadores = jugadores;
		this.starter = starter;
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		ExecutorService pool = Executors.newFixedThreadPool(jugadores.size());
			
		
		for(Jugador j : jugadores) {
			
			if(!Thread.currentThread().isInterrupted()) {return;}
			
			pool.execute(new Runnable() {
				
								
				@Override
				public void run() {
					// TODO Auto-generated method stub
					ObjectOutputStream out = j.getOutputStream();
					try {
						
						
						out.writeObject(msg);
						starter.await();
						
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally {
						try {
							out.flush();
							out.reset();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					
					
					
				}
				
				
				
			});
					
					
					
					
			
			
		}
		
		
		
		pool.shutdown();		
		
		
	}

}
