package Principal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.time.LocalDate;

import ModeloDominio.Jugador;

public class AtenderJugador implements Runnable{

	

	private Socket cliente;
	private ServicioRuletaServidor rule;

	
	//Este jugador es para hacer mejor las llamadas. No hay que modificarlo en esta clase, se modifica solo usando el "paso por referencia de java"
	private Jugador jugador;

	
	
	public AtenderJugador(Socket cliente, ServicioRuletaServidor rule) {
		
		this.cliente= cliente;
		this.rule=rule;
		this.jugador=null;
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
			
		boolean iniciado=false;
		
		
		
		//Si sale del trycatch se ha caido la conexion.
		
		try(InputStream is = cliente.getInputStream();
				OutputStream os = cliente.getOutputStream()){
			
			BufferedReader bis= new BufferedReader(new InputStreamReader(is));
			DataOutputStream dos = new DataOutputStream(os);

			
			dos.writeBytes("¿Que quieres hacer?");
			dos.writeBytes("\r\n");
			dos.writeBytes("1-Iniciar sesion:");
			dos.writeBytes("\r\n");
			dos.writeBytes("2-Registrar sesion:");
			dos.writeBytes("\r\n");
			dos.flush();
			
			String i = bis.readLine();
			
			if(i.equals("1")) {
				
				iniciado=this.iniciarSesion(is,os);
				
			}else {
				
				//Si no quiere iniciar sesion se regista, no dejamos escapar a nadie.Todo el mundo debe consumir.
				
				iniciado=this.registrarSesion(is,os);
			}
			
			
			
			if(iniciado) {
				
				
				while(true) {
					
					//Hacemos apuestas-----------------.
					
					this.empezarJugar();
					
				}
				
				
			}else {
				
				dos.writeBytes("-ERROR-");
				dos.writeBytes("\r\n");
				dos.writeBytes("Ha ocurrido un error en inicio de sesion, intentelo mas tarde.");
				dos.writeBytes("\r\n");
				dos.writeBytes("Disculpa las molestias");
				dos.writeBytes("\r\n");				
				dos.flush();
				
				
				
				//Desconectar
				
			}
			
			
			
		}catch(IOException e) {e.printStackTrace();}
				
	}
	
	
	private void empezarJugar() {
		
		try {
			
			//Falta añadir funcionalidades:
			
			//Añadir saldo-
			//Seguir jugando
			//Desconectar.
			
			
			//Funcionalidad jugar.
			//Espero a que se pueda apostar. 
			this.rule.VaMasWait();
						
			
			//Crea apuestas
			
			CrearApuestas apuesta = new CrearApuestas(this.rule,this.jugador);	
			apuesta.start();
			
			//Espera a girar la pelota
			this.rule.noVaMasWait();
			//Se cierran las apuestas
			
			apuesta.interrupt();
			//Cierro el hilo
			
			
			//Vuleve a empezar
			
			
			//Metodo desconectar
			
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			
			//Si salta la exception no hago nada, sigo jugando.
			
		}
		
		
	}
	
	
	
	private boolean iniciarSesion(InputStream is, OutputStream os) {
		
		
		try{
			
			BufferedReader bis= new BufferedReader(new InputStreamReader(is));
			DataOutputStream dos = new DataOutputStream(os);
			
			dos.writeBytes("---INICIANDO SESION---");
			dos.writeBytes("\r\n");
			dos.writeBytes("Nombre de usuario:");
			dos.writeBytes("\r\n");
			dos.flush();
			
			dos.writeBytes("NECESITO RESPUESTA\r\n");				
			dos.flush();
			
			String ID=bis.readLine();
			
			this.jugador = this.rule.getJugador(ID);
			
			if(jugador.equals(null)) {
				
				
				dos.writeBytes("Nombre de usuario no encontrado, ¿Quieres registrar sesion?");
				dos.writeBytes("\r\n");
				dos.flush();
				
				dos.writeBytes("NECESITO RESPUESTA\r\n");				
				dos.flush();
				String n = bis.readLine();
				
				if(n.equals("si")) {
					
					return this.registrarSesion(is,os);
					
					
				}else {
					
					return this.iniciarSesion(is,os);
					
				}
				
				
			}else {
				
				this.rule.establecerConexion(jugador, cliente);
				return true;
				
				
			}
			
			
			
		}catch(IOException e) {e.printStackTrace();}
		
		return false;
		
		
		
	}
	
	private boolean registrarSesion(InputStream is, OutputStream os) {
		
		
		
		try{
			
			BufferedReader bis= new BufferedReader(new InputStreamReader((is)));
			DataOutputStream dos = new DataOutputStream(os);

			dos.writeBytes("---REGISTRANDO SESION---");
			dos.writeBytes("\r\n");
			dos.writeBytes("Nombre de usuario:");
			dos.flush();
			
			
			//Si por algun casual no funciona bien el asignamiento de nombre de usuario, nos aseguramos de que ponemos uno que no se va repetir
			String ID="user834959347"+ LocalDate.now().toString();
			
			
			while(true) {
				
				
				dos.writeBytes("NECESITO RESPUESTA\r\n");				
				dos.flush();
				ID = bis.readLine();
				
				if(this.rule.getJugador(ID)==null) {
					
					
					break;
					
				}
				
				dos.writeBytes("Nombre de usuario en uso, prueba otra vez.");
				dos.flush();
				
			}
			
			
			
			
			
			
			dos.writeBytes("Saldo a introducir:");
			dos.flush();
				
			//Nos aseguramos de que introduca un valor valido.
			boolean correcto=true;
			double saldo =0;//Si no empieza siempre con 0€
			while(correcto) {
				
				try {
					
					dos.writeBytes("NECESITO RESPUESTA\r\n");				
					dos.flush();
					String sal = bis.readLine();
					saldo = Double.parseDouble(sal);
					correcto=false;
					
				}catch (NumberFormatException e) {
					dos.writeBytes("Introduce un valor valido:");
					dos.flush();
				}
			}
			
			
			
			
			int i=this.rule.anadirJugador(new Jugador(ID,saldo));
				
			//0-Jugador ya existe.
			//1-ha añadido el jugador con exito
			//2-no ha podido añadir el jugador
			
			switch(i) {
			
			
			case 0:
				
				dos.writeBytes("Tu sabras pero tu usuario esta en uso. Incia Sesion.");
				return this.iniciarSesion(is,os);
				
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
