package servidor;

import java.util.List;
import java.util.Map;

import modeloDominio.Apuesta;
import modeloDominio.Casilla;
import modeloDominio.Jugador;

public class guardarApuestas implements Runnable {

	
	private Map<Jugador,List<Apuesta>> map;
	private Casilla ganador;
	private XMLServidor xml;
	
	public guardarApuestas(Map<Jugador,List<Apuesta>> map, Casilla ganadora, XMLServidor xml) {
		this.map=map;
		this.ganador=ganadora;
		this.xml=xml;
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		xml.guardarJugadorApuesta(map, ganador);
		
	}

	
	
	
		
		
		
	
	
	
}
