package prueba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class clienteAA {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Socket socket = null;
		try {
			socket = new Socket("localhost",8000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		
		 try (
				 
				 
		            // Flujo de ENTRADA del servidor (para leer mensajes)
		            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		            // Flujo de SALIDA al servidor (para enviar respuestas) - autoFlush=true
		            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		            // Flujo de ENTRADA de teclado (para que el usuario escriba)
		            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
		        ) {
		            
		            System.out.println("✅ Conectado al Casino. Esperando instrucciones...");
		            
		            String msgServidor;
		            // Leemos continuamente lo que dice el servidor
		            while ((msgServidor = in.readLine()) != null) {
		                
		                // PROTOCOLO: El servidor nos pide datos explícitamente
		                if (msgServidor.equals("NECESITO RESPUESTA")) {
		                    System.out.print("> "); // Prompt visual para el usuario
		                    String respuesta = teclado.readLine();
		                    
		                    // Enviamos la respuesta al servidor
		                    // println añade el salto de línea correcto automáticamente
		                    out.println(respuesta); 
		                    
		                } else {
		                    // Es un mensaje normal (informativo), solo lo mostramos
		                    System.out.println(msgServidor);
		                }
		            }
		            
		        } catch (IOException e) {
		            System.err.println("❌ Se ha perdido la conexión con el servidor.");
		        } finally {
		            try { if (socket != null) socket.close(); } catch (IOException e) {}
		        }
		
		
	}

}
