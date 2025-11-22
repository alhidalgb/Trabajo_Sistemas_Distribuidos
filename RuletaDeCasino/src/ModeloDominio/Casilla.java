package ModeloDominio;

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

	public int getNumero() {
		// TODO Auto-generated method stub
		return numero;
	}

	public String getColor() {
		// TODO Auto-generated method stub
		return color.toString();
	}

	public int getDocena() {
		// TODO Auto-generated method stub
		
		if(this.numero>=1 && this.numero<=12) {return 1;}
		if(this.numero>=13 && this.numero<=24) {return 2;}
		if(this.numero>=25 && this.numero<=36) {return 3;}


		
		
		return 0;
	}
	

}
