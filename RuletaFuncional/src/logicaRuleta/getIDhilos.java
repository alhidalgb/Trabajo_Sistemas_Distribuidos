package logicaRuleta;

import java.util.List;
import java.util.concurrent.Callable;

import modeloDominio.Jugador;

public class getIDhilos implements Callable<Jugador>{

	private int byteIn, byteFin;
	private List<Jugador> jugadorsSesion;
	private String id;
	
	public getIDhilos(List<Jugador>jug,int in,int fin, String id){
		
		this.jugadorsSesion=jug;
		this.byteIn=in;
		this.byteFin=fin;
		this.id=id;
		
		
	}
	
	
	@Override
	public Jugador call() throws Exception {
		
		
		for(int i=this.byteIn;i<this.byteFin;i++) {
			
			
			if(id.equals(this.jugadorsSesion.get(i).getID())) {
				return this.jugadorsSesion.get(i);
			}
			
			
		}
		
		
		
		return null;
	}




}
