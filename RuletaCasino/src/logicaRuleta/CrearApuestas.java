package logicaRuleta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import modeloDominio.Apuesta;
import modeloDominio.Jugador;
import modeloDominio.TipoApuesta;

public class CrearApuestas extends Thread {

	
	private ServicioRuletaServidor rule;
	private Jugador jugador;
	
	
	public CrearApuestas(ServicioRuletaServidor rule,Jugador jug) {
		
		this.jugador=jug;
		this.rule=rule;
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		
		try{
			
			OutputStream os = this.jugador.getConexion().getOutputStream();
		
			while(true) {
				
				
				if(this.rule.anadirApuesta(this.crearApuesta(this.jugador.getConexion().getInputStream(),os))) {
					
					os.write("Apuesta añadida con exito".getBytes());
					os.write("\r\n".getBytes());
					os.flush();
					
				}else {
					
					os.write("No se ha podido añadir la apuesta. Lo sentimos.".getBytes());
					os.write("\r\n".getBytes());
					os.flush();
				}
				
				
				
			}
			
			
			
			
			
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		
		
		
	}
	
	
	
    public  Apuesta crearApuesta(InputStream is, OutputStream os) throws IOException {
        
        
   	
        	BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        	
        	os.write("\n--- NUEVA APUESTA ---".getBytes());
			os.write("\r\n".getBytes());
            os.write(("Saldo actual: " + jugador.getSaldo() + "€").getBytes());
			os.write("\r\n".getBytes());
            
        	
            // 1. PEDIR CANTIDAD (Validando saldo)
            double cantidad = 5;
            boolean cantidadValida = false;
            
            while (!cantidadValida) {
                os.write("¿Cuánto quieres apostar? ".getBytes());
				os.write("\r\n".getBytes());

                try {
                	os.flush();
                	
                	os.write("NECESITO RESPUESTA\r\n".getBytes());				
					os.flush();
                    String entrada = bis.readLine();
                    cantidad = Double.parseDouble(entrada);
                    
                    if (cantidad < 5) {
                        os.write("❌ La cantidad debe ser mayor a 5.".getBytes());
    					os.write("\r\n".getBytes());

                    } else if (cantidad > jugador.getSaldo()) {
                        os.write(("❌ No tienes suficiente saldo (Tienes: " + jugador.getSaldo() + ")").getBytes());
    					os.write("\r\n".getBytes());

                    } else {
                        cantidadValida = true;
                    }
                } catch (NumberFormatException e) {
                    os.write("❌ Introduce un número válido (ej: 10.5).".getBytes());
					os.write("\r\n".getBytes());
					

                }
            }

            // 2. PEDIR TIPO DE APUESTA
            os.write("¿Qué tipo de apuesta quieres hacer?".getBytes());
			os.write("\r\n".getBytes());
            os.write("1- NUMERO (Pleno)".getBytes());
            os.write("\r\n".getBytes());
            os.write("2- COLOR".getBytes());
            os.write("\r\n".getBytes());
            os.write("3- PAR / IMPAR".getBytes());
            os.write("\r\n".getBytes());
            os.write("4- DOCENA".getBytes());
            os.write("\r\n".getBytes());
            
            TipoApuesta tipoSeleccionado = null;
            while (tipoSeleccionado == null) {
                try {
                	os.flush();
                	
                	os.write("NECESITO RESPUESTA\r\n".getBytes());				
					os.flush();
                    String s = bis.readLine();
                    int op = Integer.parseInt(s);
                    // Asumiendo que tu Enum tiene este orden
                    if (op >= 1 && op <= 4) {
                        tipoSeleccionado = TipoApuesta.values()[op - 1];
                    } else {
                        os.write("❌ Elige entre 1 y 4.".getBytes());
                        os.write("\r\n".getBytes());
                    }
                } catch (NumberFormatException e) {
                    os.write("❌ Introduce un número.".getBytes());
                    os.write("\r\n".getBytes());
                }
            }

            // 3. PEDIR VALOR ESPECÍFICO (Depende del tipo)
            String valorApostado = "";
            boolean valorValido = false;

            while (!valorValido) {
                switch (tipoSeleccionado) {
                    case NUMERO:
                        os.write("Elige número (0-36): ".getBytes());
                        os.write("\r\n".getBytes());
                        try {
                        	os.flush();
                        	
                        	os.write("NECESITO RESPUESTA\r\n".getBytes());				
        					os.flush();
                            int num = Integer.parseInt(bis.readLine());
                            if (num >= 0 && num <= 36) {
                                valorApostado = String.valueOf(num);
                                valorValido = true;
                            } else os.write("❌ Número fuera de rango.".getBytes());
                            os.write("\r\n".getBytes());
                        } catch (NumberFormatException e) { os.write("❌ Error de formato.".getBytes());
                        os.write("\r\n".getBytes());}
                        break;

                    case COLOR:
                        os.write("Elige color (ROJO / NEGRO): ".getBytes());
                        os.write("\r\n".getBytes());
                        
                        os.write("NECESITO RESPUESTA\r\n".getBytes());				
    					os.flush();
                        String color = bis.readLine().toUpperCase().trim();
                        if (color.equals("ROJO") || color.equals("NEGRO")) {
                            valorApostado = color;
                            valorValido = true;
                        } else os.write("❌ Escribe ROJO o NEGRO.".getBytes());
                        	os.write("\r\n".getBytes());
                        break;

                    case PAR_IMPAR:
                        os.write("Elige paridad (PAR / IMPAR): ".getBytes());
                        os.write("\r\n".getBytes());
                        os.flush();
                        
                        os.write("NECESITO RESPUESTA\r\n".getBytes());				
    					os.flush();
                        String paridad = bis.readLine().toUpperCase().trim();
                        if (paridad.equals("PAR") || paridad.equals("IMPAR")) {
                            valorApostado = paridad;
                            valorValido = true;
                        } else os.write("❌ Escribe PAR o IMPAR.".getBytes());os.write("\r\n".getBytes());
                        break;

                    case DOCENA:
                        os.write("Elige docena (1, 2 o 3): ".getBytes());
                        os.write("\r\n".getBytes());
                        os.flush();
                        
                        os.write("NECESITO RESPUESTA\r\n".getBytes());				
    					os.flush();
                        String docena = bis.readLine().trim();
                        if (docena.equals("1") || docena.equals("2") || docena.equals("3")) {
                            valorApostado = docena;
                            valorValido = true;
                        } else os.write("❌ Escribe 1, 2 o 3.".getBytes());os.write("\r\n".getBytes());
                        break;
                }
            }

            // 4. CONSTRUIR Y DEVOLVER EL OBJETO
            // Usamos el ID del jugador que pasamos por parámetro
            return new Apuesta(jugador, tipoSeleccionado, valorApostado, cantidad);

       
    }
	
	

}
