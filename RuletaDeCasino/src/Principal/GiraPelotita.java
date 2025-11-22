package Principal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import ModeloDominio.Casilla;


public class GiraPelotita implements Runnable {

	private ServicioRuletaServidor rule;
	
	//SOLO LE PASA EL SERVICIORULETA Y CADA X SEGUNDOS HACE LAS COSAS.
	public GiraPelotita(ServicioRuletaServidor rule) {
		
		this.rule=rule;
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
		
		this.rule.repartirPremio(new Casilla(numeroGanador));
		this.rule.resetNoVaMas();
		
	}
	

	
	
	

}
