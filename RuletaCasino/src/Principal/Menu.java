package Principal;

import java.io.DataInputStream;
import java.io.IOException;

public class Menu {

	
	
	public void Menu()  {
		
		
		try(DataInputStream dis = new DataInputStream(System.in)){
			
			System.out.println("¿Que apuesta quieres realizar?");
			System.out.println("1. Par o impar");
			System.out.println("2. Color");
			System.out.println("1. Par o impar");
			System.out.println("1. Par o impar");
			
			
			int n = System.in.read();
			
			switch(n) {
			
			case 1:
				
			
			
			
			}
			
			
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		
		
		
	}
	
	
	
	
}
