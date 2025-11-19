package Principal;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServicioRuleta {

	
	
	private List<Apuesta> apuestas = new ArrayList<>();
	private List<Socket> clientesConectados;
	private List<Jugador> jugadores;
	
	private Casilla casillaGanadora;
	
	
	
	
	//BLOQUEADOR APUESTAS CADA MARCADO POR EL GIRAR RULETITA

	public void setCasilla(Casilla casilla) {
		
		this.casillaGanadora=casilla;
	}
	
	
	public int anadirJugador(Jugador jug){
		
		if(jugadores.contains(jug)) {
			
			return 0;
		}
		
		if(jugadores.add(jug)) {
			
			return 1;
			
		}
		
		//No ha podido añadir al jugador
		return 2;
	}
	
	public boolean anadirApuesta(Apuesta apuesta) {
		
		return apuestas.add(apuesta);
		
	}


	public Jugador getJugador(String iD) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
