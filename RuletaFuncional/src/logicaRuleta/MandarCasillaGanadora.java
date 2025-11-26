package logicaRuleta;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;

import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class MandarCasillaGanadora implements Runnable{

	
	private List<Jugador> jugadores;
	private CyclicBarrier starter;
	private Casilla ganadora;
	private int inicio;
	private int fin;
	private ExecutorService pool;
	
	public MandarCasillaGanadora(List<Jugador> jugadores, CyclicBarrier starter, Casilla ganadora, int inicio, int fin, ExecutorService pool) {
		
		this.jugadores=jugadores;
		this.starter=starter;
		this.ganadora=ganadora;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		for(int i = inicio;i<fin;i++) {
			
			
			Jugador jug = this.jugadores.get(i);
			pool.execute(new mandarMSG(jug.getConexion(),"\u001b[32m"+"--- NO VA MAS ---"+"\u001b[0m"+"\r\n"+"\u001b[32m"+"CASILLA GANDORA: " + this.ganadora.toString()+"\u001b[0m", starter));
			
		}
		
		
	}

}
