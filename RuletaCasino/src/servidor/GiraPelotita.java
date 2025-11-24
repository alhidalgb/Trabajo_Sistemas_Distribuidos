package servidor;


import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import logicaRuleta.ServicioRuletaServidor;
import modeloDominio.Casilla;


public class GiraPelotita implements Runnable {

	private ServicioRuletaServidor rule;
	private ExecutorService pool;
	private XMLServidor xml;
	
	
	public GiraPelotita(ServicioRuletaServidor rule, ExecutorService pool, XMLServidor xml) {
		
		this.rule=rule;
		this.pool=pool;
		this.xml=xml;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		int numeroGanador = new Random().nextInt(37);
		
		
		//Cieroo apuestas
		this.rule.NoVaMas();
		
		
		//Espero 1 segundo por  si hay apuestas que estan llegando.
		try {
		    TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		    e.printStackTrace();
		}
		
		Casilla ganadora=new Casilla(numeroGanador);
		
		pool.execute(new guardarApuestas(this.rule.getCopiaJugadorApuestas(),ganadora,xml) );
		this.rule.repartirPremio(ganadora);
		this.rule.resetNoVaMas();
		
	}
	

	
	
	

}
