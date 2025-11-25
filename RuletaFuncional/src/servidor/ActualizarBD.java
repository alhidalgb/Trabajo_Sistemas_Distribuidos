package servidor;

import java.util.List;

import modeloDominio.Jugador;

public class ActualizarBD implements Runnable{

	private List<Jugador> jugadores;
	private String bd;
	
	public ActualizarBD(List<Jugador> jugadores,String bd) {
		
		this.jugadores=jugadores;
		this.bd=bd;
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
	
		
		BDJugadores.MarshallingJugadores(jugadores, bd);
		
	}

}
