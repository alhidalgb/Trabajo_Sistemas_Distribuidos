package Principal;

public class Apuesta {

	private Jugador id;
	private TipoApuesta tipo;
	private String valor;
	private double cantidad;
	
	
	public Apuesta(Jugador j,TipoApuesta t,String v,double cantidad) {
		this.id=j;
		this.tipo=t;
		this.valor=v;
		
		
	}
	
	public double getCantidad() {return cantidad;}

	public String getValor() {
		// TODO Auto-generated method stub
		return valor;
	}

	public TipoApuesta getApuesta() {
		// TODO Auto-generated method stub
		return tipo;
	}
	
	
	
}
