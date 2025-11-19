package Principal;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Jugar implements Runnable{

	

	private Socket cliente;
	private Jugador jugador;
	private ServicioRuleta rule;
	
	
	public Jugar(Socket cliente, ServicioRuleta rule) {
		
		this.cliente= cliente;
		this.rule=rule;
		this.jugador=null;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
			
		boolean iniciado=false;
		
		
		try(BufferedReader bff= new BufferedReader(new InputStreamReader((System.in)))){
			
			System.out.println("¿Que quieres hacer?");
			System.out.println("1-Iniciar sesion:");
			System.out.println("2-Registrar sesion:");
			
			String i = bff.readLine();
			
			
			
			if(i.equals("1")) {
				
				iniciado=this.iniciarSesion();
			}else {
				
				//Si no quiere iniciar sesion se registar, no dejamos escapar a nadie.
				
				iniciado=this.registrarSesion();
			}
			
			
		}catch(IOException e) {e.printStackTrace();}
		
		
		if(iniciado) {
			
			
			boolean seguir=true;
			
			while(seguir) {
							
				
				Apuesta apuesta = this.hacerApuesta();
				
				if(rule.anadirApuesta(apuesta)) {
					
					
					System.out.println("Apuesta añadida correctamente");
					
				}else {
					
					System.out.println("No se ha podido realizar la apuesta");
					
					
				}
				
				System.out.println("¿Quieres hacer otra?");
				
				
						
				
			}
			
		}
		
		
		
				
	}
	
	
	private Apuesta hacerApuesta() {
		
		TipoApuesta tipoapuesta = null;
		String valor = null;
		double cantidad = 0;
		
		
		
		try(BufferedReader bff= new BufferedReader(new InputStreamReader((System.in)))){
			
			
			//Tipo apuesta.
			
			System.out.println("¿Que tipo de apuesta quieres hacer?");
			System.out.println("1-Numero.");
			System.out.println("2-Color.");
			System.out.println("3-Par o impar.");
			System.out.println("4-Docena.");
			
			
			
			//Nos aseguramos de que introduca un valor valido.
			
			int op =0;
			
			while(op<1 || op>4) {
				
				try {					
					
					String sal = bff.readLine();
					
					op = Integer.parseInt(sal);
					
					if(op<1 || op>4) {
						
						System.out.println("Introduce un valor valido:");
						
						
					}
					
				}catch (NumberFormatException e) {
					System.out.println("Introduce un valor valido:");
				}
			}
			
			
			//Valor apuesta
			
			
			switch(op) {
			
			case 1:
				
				tipoapuesta=TipoApuesta.NUMERO;
				
				System.out.println("¿A que numero quieres apostar?");
				System.out.println("Elige entre el 0 y el 36");

				//Nos aseguramos de que introduca un valor valido.
				
				String val = "0";
				int i =-1;			
				while(i<0 || i>36) {
					
					try {					
						
						val = bff.readLine();						
						i = Integer.parseInt(val);
						
						if(i<0 || i>36) {System.out.println("Introduce un valor valido:");}
						
					}catch (NumberFormatException e) {System.out.println("Introduce un valor valido:");}
					
				}
				
				valor = val;
				
				break;
				
			
			case 2:
				tipoapuesta=TipoApuesta.COLOR;
				
				
				System.out.println("¿A que color quieres apostar?");
				System.out.println("1-rojo");
				System.out.println("2-negro");

				
				//Nos aseguramos de que introduca un valor valido.
				
				String val1;
				int i1 =-1;			
				while(i1<1 || i1>2) {
					
					try {					
						
						val1 = bff.readLine();						
						i1 = Integer.parseInt(val1);
						
						if(i1<0 || i1>36) {System.out.println("Introduce un valor valido:");}
						
					}catch (NumberFormatException e) {System.out.println("Introduce un valor valido:");}
					
				}
				

				if(i1==1) {
					
					valor="rojo";
				}else {
					
					valor="negro";
				}
				
				break;
				
			case 3:
				tipoapuesta=TipoApuesta.PAR_IMPAR;
				
				System.out.println("¿A que par quieres apostar?");
				System.out.println("1-par");
				System.out.println("2-impar");

				
				//Nos aseguramos de que introduca un valor valido.
				
				String val11;
				int i11 =-1;			
				while(i11<1 || i11>2) {
					
					try {					
						
						val11 = bff.readLine();						
						i11 = Integer.parseInt(val11);
						
						if(i11<0 || i11>36) {System.out.println("Introduce un valor valido:");}
						
					}catch (NumberFormatException e) {System.out.println("Introduce un valor valido:");}
					
				}
				

				if(i11==1) {
					
					valor="par";
				}else {
					
					valor="impar";
				}
				
				
				break;
			case 4:
				tipoapuesta=TipoApuesta.DOCENA;
				
				System.out.println("¿A que docena quieres apostar?");
				System.out.println("1-primera");
				System.out.println("2-segunda");
				System.out.println("2-tercera");


				
				//Nos aseguramos de que introduca un valor valido.
				
				String val111 = null;
				int i111 =-1;			
				while(i111<1 || i111>3) {
					
					try {					
						
						val111 = bff.readLine();						
						i111 = Integer.parseInt(val111);
						
						if(i111<0 || i111>36) {System.out.println("Introduce un valor valido:");}
						
					}catch (NumberFormatException e) {System.out.println("Introduce un valor valido:");}
					
				}
				

				valor=val111;
				
				
				break;
				
			
			
			}
			
			
			System.out.println("¿Cuanto quieres apostar?");	
			System.out.println("La apuesta minima son 5€");			
			System.out.println("Tu saldo es: " + jugador.getSaldo());	
			//Nos aseguramos de que introduca un valor valido.
			
			String val;
			double i = 0;			
			while(i<5 || i>jugador.getSaldo()) {
				
				try {					
					
					val = bff.readLine();						
					i = Double.parseDouble(val);
					
					if(i<5 || i>jugador.getSaldo()) {System.out.println("Introduce un valor valido:");}
					
				}catch (NumberFormatException e) {System.out.println("Introduce un valor valido:");}
				
			}
			
			cantidad=i;
			
		}catch(IOException e) {e.printStackTrace();}
		
		
		
		return new Apuesta(this.jugador, tipoapuesta,valor,cantidad);
		
	}
	
	
	private boolean iniciarSesion() {
		
		
		try(BufferedReader bff= new BufferedReader(new InputStreamReader(System.in))){
			
			
			System.out.println("Nombre de usuario:");
			String ID = bff.readLine();
			
			this.jugador = this.rule.getJugador(ID);
			
			if(jugador.equals(null)) {
				
				System.out.println("Nombre de usuario no encontrado, ¿Quieres registrar sesion?");
				
				String n = bff.readLine();

				
				if(n.equals("si")) {
					
					return this.registrarSesion();
					
					
				}else {
					
					return this.iniciarSesion();
					
				}
				
				
			}
			
			
			
		}catch(IOException e) {e.printStackTrace();}
		
		return false;
		
		
		
	}
	
	private boolean registrarSesion() {
		
		
		
		try(BufferedReader bff= new BufferedReader(new InputStreamReader((System.in)))){
			
			
			System.out.println("Nombre de usuario:");
			String ID = bff.readLine();
			
			
			System.out.println("Saldo a introducir:");
			
			
			
			//Nos aseguramos de que introduca un valor valido.
			boolean correcto=true;
			double saldo =0;//Si no empieza siempre con 0€
			while(correcto) {
				
				try {
					String sal = bff.readLine();
					saldo = Double.parseDouble(sal);
					correcto=false;
					
				}catch (NumberFormatException e) {
					System.out.println("Introduce un valor valido:");
				}
			}
			
			int i=this.rule.anadirJugador(new Jugador(ID,saldo));
				
			//0-Jugador ya existe.
			//1-ha añadido el jugador con exito
			//2-no ha podido añadir el jugador
			
			switch(i) {
			
			
			case 0:
				
				return this.iniciarSesion();
				
			case 1:
				
				
				this.jugador=rule.getJugador(ID);
				
				if(jugador.equals(null)) {
					return false;
				}else {
					
					return true;
				}
				
				
			case 2:
				
				return false;
			
			}		
			
			
		}catch(IOException e) {e.printStackTrace();} 
		 catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
		
	}
	

}
