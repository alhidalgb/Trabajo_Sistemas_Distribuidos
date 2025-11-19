package Principal;

import java.io.Serializable;

public class Casilla implements Serializable{
	
	private static final long serialVersionUID = 1L; 
	
	private int numero;
	private COLOR color;
	
	public Casilla(int num) {
		
		this.numero=num;
		
		if(num==0) {
			
			this.color= COLOR.VERDE;
		}else {
			
			if((num%2)==0) {
				
				this.color=COLOR.NEGRO;
				
			}else {
				
				this.color=COLOR.ROJO;
			}
			
			
		}
		
		
	}
	

}
