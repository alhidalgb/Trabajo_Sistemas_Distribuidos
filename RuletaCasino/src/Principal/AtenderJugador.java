package Principal;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDate;

public class AtenderJugador implements Runnable{

	

	private Socket cliente;
	private ServicioRuleta rule;

	
	//Seguramente hay que eliminar.
	private Jugador jugador;

	
	
	public AtenderJugador(Socket cliente, ServicioRuleta rule) {
		
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
		
		
		
		//Hacemos apuestas.
		
		
		if(iniciado) {
			
			try {
				
				//¿Quieres hacer apuestas?
				
				//Espero a que se pueda apostar. 
				this.rule.VaMasWait();
				//Crea apuestas
				this.rule.anadirApuesta(null);//Hilo distinto
				//Espera a girar la pelota
				this.rule.noVaMasWait();
				//Se cierran las apuestas
				
				//Cierro el hilo
				
				
				//Vuleve a empezar
				
				
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}else {
			
			System.out.println("Ha ocurrido un error en inicio de sesion, intentelo mas tarde.");
			System.out.println("Disculpa las molestias");
			
			
			//Desconectar
			
		}
		
		
		
				
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
				
				
			}else {
				
				this.rule.establecerConexion(jugador, cliente);
				return true;
				
				
			}
			
			
			
		}catch(IOException e) {e.printStackTrace();}
		
		return false;
		
		
		
	}
	
	private boolean registrarSesion() {
		
		
		
		try(BufferedReader bff= new BufferedReader(new InputStreamReader((System.in)))){
			
			//Si por algun casual no funciona bien el asignamiento de nombre de usuario, nos aseguramos de que ponemos uno que no se va repetir
			String ID="user834959347"+ LocalDate.now().toString();
			
			System.out.println("Nombre de usuario:");
			
			
			while(true) {
				
				ID = bff.readLine();
				
				if(this.rule.getJugador(ID)==null) {
					
					
					break;
					
				}
				
				System.out.println("Nombre de usuario en uso, prueba otra vez.");
				
				
			}
			
			
			
			
			
			
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
			
			int i=this.rule.anadirJugador(new Jugador(ID,saldo),cliente);
				
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
					
					
					this.rule.establecerConexion(jugador, cliente);
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
