package Principal;

public class Jugador {

	private String ID;
	private double saldo;
	
	
	public Jugador(String id, double saldo) {
		
		this.ID=id;
		this.saldo=saldo;
		
		
	}
	
	
	public  double getSaldo() {
		
		return saldo;
	}
	
	public String getID() {
		
		return this.ID;
		
	}
	
	public boolean equals(Jugador jug) {
		
		return this.ID.equals(jug.getID());
	
	}
	
	
}
